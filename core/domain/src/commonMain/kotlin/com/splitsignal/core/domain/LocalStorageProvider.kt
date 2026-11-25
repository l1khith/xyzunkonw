package com.splitsignal.core.domain

/**
 * Maximum supported offline file transfer size (1 GiB).
 */
const val MAX_FILE_TRANSFER_BYTES: Long = 1_073_741_824L

/**
 * Platform-agnostic key-value text persistence abstraction.
 */
interface LocalStorageProvider {
    fun readText(key: String): String?
    fun writeText(key: String, value: String)
}

/**
 * In-memory [LocalStorageProvider] implementation for unit tests and ephemeral targets.
 */
class InMemoryLocalStorageProvider : LocalStorageProvider {
    private val entries = mutableMapOf<String, String>()

    override fun readText(key: String): String? = entries[key]

    override fun writeText(key: String, value: String) {
        entries[key] = value
    }
}

/**
 * Platform-neutral handle describing a locally selected file for transfer.
 */
interface PlatformFileHandle {
    val id: String
    val name: String
    val sizeBytes: Long
    val mimeType: String
}

/**
 * Streaming HTTP/socket transport for 1 GB-capable offline file transfers.
 */
interface FileTransferTransport {
    suspend fun sendOfferAndStream(
        peerIp: String,
        peerPort: Int,
        transferId: String,
        file: PlatformFileHandle,
        onProgress: (Long, Long) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
    )

    fun registerOutgoingFile(transferId: String, file: PlatformFileHandle)

    fun unregisterOutgoingFile(transferId: String)

    suspend fun downloadOfferedFile(
        peerIp: String,
        peerPort: Int,
        transferId: String,
        fileName: String,
        totalBytes: Long,
        onProgress: (Long, Long) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit,
    )

    fun cancelTransfer(transferId: String)
}

/**
 * Formats a byte count into a human-readable string such as `"842.5 MB"` or `"12.4 KB"`.
 */
fun formatByteSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    val tenths = ((value * 10.0) + 0.5).toLong()
    val whole = tenths / 10L
    val frac = tenths % 10L
    return "$whole.$frac ${units[unitIndex]}"
}
