package com.splitsignal.core.network

import com.splitsignal.core.domain.ClientSyncTransport
import com.splitsignal.core.domain.PlayerRole
import com.splitsignal.core.domain.SyncPacket
import com.splitsignal.core.domain.TransportConnectionState
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
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
 * Multiplatform Ktor WebSocket [ClientSyncTransport] using an injected [HttpClientEngineFactory].
 */
class KtorSyncClient(
    httpClientEngineFactory: HttpClientEngineFactory<*>,
    scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ClientSyncTransport {

    private val syncJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val transportJob = SupervisorJob(scope.coroutineContext[Job])
    private val transportScope = CoroutineScope(scope.coroutineContext + transportJob)

    private val _incomingPackets = MutableSharedFlow<SyncPacket>(extraBufferCapacity = 64)
    override val incomingPackets: SharedFlow<SyncPacket> = _incomingPackets.asSharedFlow()

    private val _connectionState = MutableStateFlow(TransportConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<TransportConnectionState> = _connectionState.asStateFlow()

    private val sessionMutex = Mutex()

    @Volatile
    private var activeSession: DefaultClientWebSocketSession? = null

    @Volatile
    private var connectJob: Job? = null

    @Volatile
    private var closed = false

    private val client: HttpClient = HttpClient(httpClientEngineFactory) {
        install(WebSockets)
    }

    override fun connect(host: String, port: Int, roomCode: String, role: PlayerRole) {
        if (closed) return

        connectJob?.cancel()
        _connectionState.value = TransportConnectionState.CONNECTING

        connectJob = transportScope.launch(dispatcher) {
            try {
                client.webSocket(urlString = "ws://${host.trim()}:$port/ws") {
                    sessionMutex.withLock {
                        activeSession = this
                    }
                    try {
                        val handshakePayload = syncJson.encodeToString(
                            SyncPacket.serializer(),
                            SyncPacket.Handshake(roomCode = roomCode, role = role),
                        )
                        send(Frame.Text(handshakePayload))
                        _connectionState.value = TransportConnectionState.CONNECTED

                        readIncomingFrames(this)
                    } finally {
                        sessionMutex.withLock {
                            if (activeSession === this) {
                                activeSession = null
                            }
                        }
                    }
                }
                if (!closed && _connectionState.value != TransportConnectionState.ERROR) {
                    _connectionState.value = TransportConnectionState.DISCONNECTED
                }
            } catch (e: CancellationException) {
                if (closed) {
                    _connectionState.value = TransportConnectionState.DISCONNECTED
                }
                throw e
            } catch (_: Throwable) {
                if (!closed) {
                    _connectionState.value = TransportConnectionState.ERROR
                }
            }
        }
    }

    private suspend fun readIncomingFrames(session: DefaultClientWebSocketSession) {
        try {
            for (frame in session.incoming) {
                if (!currentCoroutineContext().isActive || closed) break
                if (frame is Frame.Text) {
                    val packet = runCatching {
                        syncJson.decodeFromString(SyncPacket.serializer(), frame.readText())
                    }.getOrNull() ?: continue
                    _incomingPackets.emit(packet)
                }
            }
        } catch (_: ClosedReceiveChannelException) {
            // Host closed the WebSocket connection cleanly.
        }
    }

    override suspend fun send(packet: SyncPacket) {
        if (closed || _connectionState.value != TransportConnectionState.CONNECTED) return

        val session = sessionMutex.withLock { activeSession } ?: return
        val payload = syncJson.encodeToString(SyncPacket.serializer(), packet)
        try {
            session.send(Frame.Text(payload))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            if (!closed) {
                _connectionState.value = TransportConnectionState.ERROR
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        _connectionState.value = TransportConnectionState.DISCONNECTED

        val sessionToClose = activeSession
        activeSession = null
        connectJob?.cancel()
        connectJob = null
        transportJob.cancel()

        CoroutineScope(SupervisorJob() + dispatcher).launch {
            runCatching {
                sessionToClose?.close(CloseReason(CloseReason.Codes.NORMAL, "Client closed"))
            }
            runCatching {
                client.close()
            }
        }
    }
}
