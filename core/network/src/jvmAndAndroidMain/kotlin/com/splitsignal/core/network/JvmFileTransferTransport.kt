package com.splitsignal.core.network

import com.splitsignal.core.domain.FileTransferTransport
import com.splitsignal.core.domain.MAX_FILE_TRANSFER_BYTES
import com.splitsignal.core.domain.PlatformFileHandle
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * JVM/Android extension of [PlatformFileHandle] capable of opening a streaming [InputStream]
 * so files up to 1 GB can be served in 64 KB chunks without loading them into memory.
 */
interface StreamableFileHandle : PlatformFileHandle {
    fun openInputStream(): InputStream
}

/**
 * Ktor CIO chunked HTTP [FileTransferTransport] supporting 64 KB streaming transfers up to 1 GiB.
 */
class JvmFileTransferTransport(
    private val outputDir: File = File(System.getProperty("user.home"), ".splitsignal/transfers"),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val httpClient: HttpClient = createDefaultClient(),
) : FileTransferTransport {

    private val outgoingFiles = ConcurrentHashMap<String, StreamableFileHandle>()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    /**
     * Looks up a registered outgoing [StreamableFileHandle] by [transferId].
     */
    fun getOutgoingFile(transferId: String): StreamableFileHandle? = outgoingFiles[transferId]

    /**
     * Resolves a safe target [File] inside [outputDir] for an incoming transfer.
     */
    fun createIncomingFile(fileName: String): File {
        outputDir.mkdirs()
        val safeName = sanitizeFileName(fileName)
        return File(outputDir, safeName)
    }

    override fun registerOutgoingFile(transferId: String, file: PlatformFileHandle) {
        if (transferId.isBlank()) return
        if (file.sizeBytes > MAX_FILE_TRANSFER_BYTES) return
        if (file is StreamableFileHandle) {
            outgoingFiles[transferId] = file
        }
    }

    override fun unregisterOutgoingFile(transferId: String) {
        outgoingFiles.remove(transferId)
    }

    override suspend fun sendOfferAndStream(
        peerIp: String,
        peerPort: Int,
        transferId: String,
        file: PlatformFileHandle,
        onProgress: (Long, Long) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (file.sizeBytes > MAX_FILE_TRANSFER_BYTES) {
            onError("File exceeds maximum transfer size of $MAX_FILE_TRANSFER_BYTES bytes")
            return
        }
        val streamable = file as? StreamableFileHandle
        if (streamable == null) {
            onError("Selected file handle does not support streaming")
            return
        }

        registerOutgoingFile(transferId, streamable)

        if (peerIp.isBlank() || peerPort !in 1..65535) {
            onError("Invalid peer endpoint")
            return
        }

        withContext(ioDispatcher) {
            val job = currentCoroutineContext()[Job]
            if (job != null) {
                activeJobs[transferId] = job
            }
            try {
                val url = "http://${peerIp.trim()}:$peerPort/api/transfer/$transferId"
                val startMs = System.currentTimeMillis()
                var lastProgressMs = 0L

                val bodyContent = object : OutgoingContent.WriteChannelContent() {
                    override val contentType: ContentType = ContentType.Application.OctetStream
                    override val contentLength: Long? = streamable.sizeBytes.takeIf { it >= 0L }

                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        val buffer = ByteArray(CHUNK_SIZE_BYTES)
                        var bytesSent = 0L
                        streamable.openInputStream().use { input ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                if (read > 0) {
                                    channel.writeFully(buffer, 0, read)
                                    bytesSent += read
                                    val now = System.currentTimeMillis()
                                    if (now - lastProgressMs >= PROGRESS_THROTTLE_MS) {
                                        lastProgressMs = now
                                        val elapsedMs = (now - startMs).coerceAtLeast(1L)
                                        val speed = (bytesSent * 1000L) / elapsedMs
                                        onProgress(bytesSent, speed)
                                    }
                                }
                            }
                        }
                        val elapsedMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
                        onProgress(bytesSent, (bytesSent * 1000L) / elapsedMs)
                    }
                }

                httpClient.preparePost(url) {
                    header("X-File-Name", sanitizeFileName(streamable.name))
                    setBody(bodyContent)
                }.execute { response ->
                    if (response.status.isSuccess()) {
                        onComplete()
                    } else {
                        onError("Upload failed with HTTP ${response.status.value}")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                onError(t.message ?: "File upload failed")
            } finally {
                if (job != null) {
                    activeJobs.remove(transferId, job)
                }
            }
        }
    }

    override suspend fun downloadOfferedFile(
        peerIp: String,
        peerPort: Int,
        transferId: String,
        fileName: String,
        totalBytes: Long,
        onProgress: (Long, Long) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (totalBytes > MAX_FILE_TRANSFER_BYTES) {
            onError("File exceeds maximum transfer size of $MAX_FILE_TRANSFER_BYTES bytes")
            return
        }
        if (peerIp.isBlank() || peerPort !in 1..65535 || transferId.isBlank()) {
            onError("Invalid transfer endpoint")
            return
        }

        withContext(ioDispatcher) {
            val job = currentCoroutineContext()[Job]
            if (job != null) {
                activeJobs[transferId] = job
            }
            val targetFile = createIncomingFile(fileName)
            try {
                val url = "http://${peerIp.trim()}:$peerPort/api/transfer/$transferId"
                httpClient.prepareGet(url).execute { response ->
                    if (!response.status.isSuccess()) {
                        onError("Download failed with HTTP ${response.status.value}")
                        return@execute
                    }

                    val channel: ByteReadChannel = response.bodyAsChannel()
                    val buffer = ByteArray(CHUNK_SIZE_BYTES)
                    var bytesTransferred = 0L
                    val startMs = System.currentTimeMillis()
                    var lastProgressMs = 0L

                    targetFile.outputStream().use { out ->
                        while (true) {
                            val read = channel.readAvailable(buffer, 0, buffer.size)
                            if (read < 0) break
                            if (read > 0) {
                                bytesTransferred += read
                                if (bytesTransferred > MAX_FILE_TRANSFER_BYTES) {
                                    throw IllegalStateException("Transfer exceeded 1 GiB limit")
                                }
                                out.write(buffer, 0, read)
                                val now = System.currentTimeMillis()
                                if (now - lastProgressMs >= PROGRESS_THROTTLE_MS) {
                                    lastProgressMs = now
                                    val elapsedMs = (now - startMs).coerceAtLeast(1L)
                                    val speed = (bytesTransferred * 1000L) / elapsedMs
                                    onProgress(bytesTransferred, speed)
                                }
                            }
                        }
                        out.flush()
                    }

                    val elapsedMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
                    onProgress(bytesTransferred, (bytesTransferred * 1000L) / elapsedMs)
                    onComplete(targetFile.absolutePath)
                }
            } catch (e: CancellationException) {
                runCatching { if (targetFile.exists()) targetFile.delete() }
                throw e
            } catch (t: Throwable) {
                runCatching { if (targetFile.exists()) targetFile.delete() }
                onError(t.message ?: "File download failed")
            } finally {
                if (job != null) {
                    activeJobs.remove(transferId, job)
                }
            }
        }
    }

    override fun cancelTransfer(transferId: String) {
        activeJobs.remove(transferId)?.cancel()
        outgoingFiles.remove(transferId)
    }

    private fun sanitizeFileName(rawName: String): String {
        val base = rawName.substringAfterLast('/').substringAfterLast('\\').trim()
        val cleaned = base.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return cleaned.ifEmpty { "transfer.bin" }
    }

    companion object {
        const val CHUNK_SIZE_BYTES: Int = 65_536
        const val PROGRESS_THROTTLE_MS: Long = 250L

        private fun createDefaultClient(): HttpClient = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = null
                socketTimeoutMillis = 60_000L
                connectTimeoutMillis = 10_000L
            }
        }
    }
}
