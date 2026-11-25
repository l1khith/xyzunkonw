package com.splitsignal.core.network

import com.splitsignal.core.domain.HostSyncTransport
import com.splitsignal.core.domain.LocalIpProvider
import com.splitsignal.core.domain.MAX_FILE_TRANSFER_BYTES
import com.splitsignal.core.domain.SyncPacket
import com.splitsignal.core.domain.TransportConnectionState
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.readAvailable
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

/**
 * Embedded Ktor CIO WebSocket [HostSyncTransport] for JVM and Android LAN hosting.
 */
class KtorSyncHost(
    private val roomCode: String,
    private val localIpProvider: LocalIpProvider,
    scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val fileTransferTransport: JvmFileTransferTransport? = null,
) : HostSyncTransport {

    private val syncJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val transportJob = SupervisorJob(scope.coroutineContext[Job])
    private val transportScope = CoroutineScope(scope.coroutineContext + transportJob)

    private val _incomingPackets = MutableSharedFlow<SyncPacket>(extraBufferCapacity = 64)
    override val incomingPackets: SharedFlow<SyncPacket> = _incomingPackets.asSharedFlow()

    private val _connectionState = MutableStateFlow(TransportConnectionState.CONNECTING)
    override val connectionState: StateFlow<TransportConnectionState> = _connectionState.asStateFlow()

    private val _boundAddress = MutableStateFlow<String?>(null)
    override val boundAddress: StateFlow<String?> = _boundAddress.asStateFlow()

    private val _connectedClients = MutableStateFlow(0)
    override val connectedClients: StateFlow<Int> = _connectedClients.asStateFlow()

    private val sessionsMutex = Mutex()
    private val activeSessions = mutableSetOf<DefaultWebSocketServerSession>()

    @Volatile
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    @Volatile
    private var closed = false

    init {
        transportScope.launch(ioDispatcher) {
            startServer()
        }
    }

    private suspend fun startServer() {
        var createdServer: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
        try {
            createdServer = embeddedServer(CIO, port = 0, host = "0.0.0.0") {
                install(WebSockets)
                routing {
                    webSocket("/ws") {
                        handleClientSession(this)
                    }
                    get("/api/transfer/{transferId}") {
                        val transferId = call.parameters["transferId"]
                        val handle = transferId?.let { fileTransferTransport?.getOutgoingFile(it) }
                        if (handle == null) {
                            call.respond(HttpStatusCode.NotFound, "Transfer not found")
                            return@get
                        }
                        if (handle.sizeBytes > MAX_FILE_TRANSFER_BYTES) {
                            call.respond(HttpStatusCode.PayloadTooLarge, "File exceeds 1 GiB limit")
                            return@get
                        }
                        if (handle.sizeBytes >= 0L) {
                            call.response.header(HttpHeaders.ContentLength, handle.sizeBytes.toString())
                        }
                        call.respondOutputStream(ContentType.Application.OctetStream) {
                            val buffer = ByteArray(JvmFileTransferTransport.CHUNK_SIZE_BYTES)
                            handle.openInputStream().use { input ->
                                while (true) {
                                    val bytesRead = input.read(buffer)
                                    if (bytesRead < 0) break
                                    if (bytesRead > 0) {
                                        write(buffer, 0, bytesRead)
                                    }
                                }
                                flush()
                            }
                        }
                    }
                    post("/api/transfer/{transferId}") {
                        val transferId = call.parameters["transferId"]
                        val transport = fileTransferTransport
                        if (transferId.isNullOrBlank() || transport == null) {
                            call.respond(HttpStatusCode.NotFound, "Transfer endpoint unavailable")
                            return@post
                        }
                        val rawFileName = call.request.headers["X-File-Name"]
                            ?: call.request.queryParameters["fileName"]
                            ?: "$transferId.bin"
                        val targetFile = transport.createIncomingFile(rawFileName)
                        val channel = call.receiveChannel()
                        val buffer = ByteArray(JvmFileTransferTransport.CHUNK_SIZE_BYTES)
                        var totalReceived = 0L

                        try {
                            targetFile.outputStream().use { out ->
                                while (true) {
                                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                                    if (bytesRead < 0) break
                                    if (bytesRead > 0) {
                                        totalReceived += bytesRead
                                        if (totalReceived > MAX_FILE_TRANSFER_BYTES) {
                                            throw IllegalStateException("Transfer exceeds 1 GiB limit")
                                        }
                                        out.write(buffer, 0, bytesRead)
                                    }
                                }
                                out.flush()
                            }
                            call.respond(HttpStatusCode.OK)
                        } catch (t: Throwable) {
                            runCatching { if (targetFile.exists()) targetFile.delete() }
                            call.respond(
                                HttpStatusCode.BadRequest,
                                t.message ?: "Failed to receive file stream",
                            )
                        }
                    }
                }
            }
            createdServer.start(wait = false)

            if (closed || !currentCoroutineContext().isActive) {
                runCatching { createdServer.stop(gracePeriodMillis = 50L, timeoutMillis = 250L) }
                return
            }

            server = createdServer
            val resolvedPort = createdServer.engine.resolvedConnectors().first().port
            val localIp = localIpProvider.getLocalIpv4Address()
            _boundAddress.value = "$localIp:$resolvedPort"
            _connectionState.value = TransportConnectionState.CONNECTED
        } catch (e: CancellationException) {
            runCatching { createdServer?.stop(gracePeriodMillis = 50L, timeoutMillis = 250L) }
            if (!closed) {
                _connectionState.value = TransportConnectionState.DISCONNECTED
            }
            throw e
        } catch (_: Throwable) {
            runCatching { createdServer?.stop(gracePeriodMillis = 50L, timeoutMillis = 250L) }
            if (!closed) {
                _connectionState.value = TransportConnectionState.ERROR
            }
        }
    }

    private suspend fun handleClientSession(session: DefaultWebSocketServerSession) {
        addSession(session)
        try {
            for (frame in session.incoming) {
                if (!currentCoroutineContext().isActive || closed) break
                if (frame is Frame.Text) {
                    val packet = runCatching {
                        syncJson.decodeFromString(SyncPacket.serializer(), frame.readText())
                    }.getOrNull() ?: continue

                    if (packet is SyncPacket.Handshake &&
                        !packet.roomCode.trim().equals(roomCode.trim(), ignoreCase = true)
                    ) {
                        runCatching {
                            session.close(
                                CloseReason(
                                    CloseReason.Codes.VIOLATED_POLICY,
                                    "Invalid room code",
                                ),
                            )
                        }
                        break
                    }

                    _incomingPackets.emit(packet)
                }
            }
        } catch (_: ClosedReceiveChannelException) {
            // Client closed the WebSocket connection cleanly.
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Connection dropped or errored; session cleanup happens in finally.
        } finally {
            removeSession(session)
        }
    }

    private suspend fun addSession(session: DefaultWebSocketServerSession) {
        sessionsMutex.withLock {
            activeSessions.add(session)
            _connectedClients.value = activeSessions.size
        }
    }

    private suspend fun removeSession(session: DefaultWebSocketServerSession) {
        sessionsMutex.withLock {
            activeSessions.remove(session)
            _connectedClients.value = activeSessions.size
        }
    }

    override suspend fun send(packet: SyncPacket) {
        if (closed || _connectionState.value != TransportConnectionState.CONNECTED) return

        val sessionsSnapshot = sessionsMutex.withLock { activeSessions.toList() }
        if (sessionsSnapshot.isEmpty()) return

        val payload = syncJson.encodeToString(SyncPacket.serializer(), packet)
        val failedSessions = mutableListOf<DefaultWebSocketServerSession>()

        for (session in sessionsSnapshot) {
            try {
                session.send(Frame.Text(payload))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                failedSessions.add(session)
            }
        }

        if (failedSessions.isNotEmpty()) {
            sessionsMutex.withLock {
                activeSessions.removeAll(failedSessions.toSet())
                _connectedClients.value = activeSessions.size
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        _connectionState.value = TransportConnectionState.DISCONNECTED
        _boundAddress.value = null
        _connectedClients.value = 0

        val serverToStop = server
        server = null
        transportJob.cancel()

        CoroutineScope(SupervisorJob() + ioDispatcher).launch {
            val sessionsToClose = sessionsMutex.withLock {
                val snapshot = activeSessions.toList()
                activeSessions.clear()
                snapshot
            }
            for (session in sessionsToClose) {
                runCatching {
                    session.close(CloseReason(CloseReason.Codes.NORMAL, "Host closed"))
                }
            }
            runCatching {
                serverToStop?.stop(gracePeriodMillis = 50L, timeoutMillis = 250L)
            }
        }
    }
}
