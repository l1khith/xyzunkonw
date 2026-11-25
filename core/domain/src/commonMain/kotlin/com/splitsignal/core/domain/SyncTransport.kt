package com.splitsignal.core.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Connection lifecycle states for [SyncTransport].
 */
enum class TransportConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

/**
 * Bidirectional packet transport abstraction used by [ReactorEngine].
 */
interface SyncTransport {
    val incomingPackets: SharedFlow<SyncPacket>
    val connectionState: StateFlow<TransportConnectionState>
    suspend fun send(packet: SyncPacket)
    fun close()
}

/**
 * Host-capable [SyncTransport] exposing its bound network address and active client count.
 */
interface HostSyncTransport : SyncTransport {
    val boundAddress: StateFlow<String?>
    val connectedClients: StateFlow<Int>
}

/**
 * Client-capable [SyncTransport] able to initiate a connection to a remote host.
 */
interface ClientSyncTransport : SyncTransport {
    fun connect(host: String, port: Int, roomCode: String, role: PlayerRole)
}

/**
 * Platform factory creating loopback, LAN host, and LAN client transports.
 */
interface TransportFactory {
    val canHostLan: Boolean
    fun createInMemoryLoopback(): SyncTransport
    fun createHost(roomCode: String, scope: CoroutineScope): HostSyncTransport
    fun createClient(scope: CoroutineScope): ClientSyncTransport
}

/**
 * In-memory transport pipe providing paired [host] and [client] endpoints plus a [localLoopback].
 */
class InMemorySyncPipe {
    private val hostToClient = MutableSharedFlow<SyncPacket>(extraBufferCapacity = 64)
    private val clientToHost = MutableSharedFlow<SyncPacket>(extraBufferCapacity = 64)
    private val loopbackFlow = MutableSharedFlow<SyncPacket>(extraBufferCapacity = 64)

    private val hostState = MutableStateFlow(TransportConnectionState.CONNECTED)
    private val clientState = MutableStateFlow(TransportConnectionState.CONNECTED)
    private val loopbackState = MutableStateFlow(TransportConnectionState.CONNECTED)

    /**
     * Host-side endpoint; packets sent here arrive on [client].
     */
    val host: SyncTransport = Endpoint(
        incoming = clientToHost,
        outgoing = hostToClient,
        state = hostState,
        onClose = {
            hostState.value = TransportConnectionState.DISCONNECTED
            clientState.value = TransportConnectionState.DISCONNECTED
        },
    )

    /**
     * Client-side endpoint; packets sent here arrive on [host].
     */
    val client: SyncTransport = Endpoint(
        incoming = hostToClient,
        outgoing = clientToHost,
        state = clientState,
        onClose = {
            hostState.value = TransportConnectionState.DISCONNECTED
            clientState.value = TransportConnectionState.DISCONNECTED
        },
    )

    /**
     * Single-process loopback endpoint for solo split-screen play.
     */
    val localLoopback: SyncTransport = Endpoint(
        incoming = loopbackFlow,
        outgoing = loopbackFlow,
        state = loopbackState,
        onClose = {
            loopbackState.value = TransportConnectionState.DISCONNECTED
        },
    )

    private class Endpoint(
        incoming: MutableSharedFlow<SyncPacket>,
        private val outgoing: MutableSharedFlow<SyncPacket>,
        private val state: MutableStateFlow<TransportConnectionState>,
        private val onClose: () -> Unit,
    ) : SyncTransport {
        override val incomingPackets: SharedFlow<SyncPacket> = incoming.asSharedFlow()
        override val connectionState: StateFlow<TransportConnectionState> = state.asStateFlow()

        override suspend fun send(packet: SyncPacket) {
            if (state.value == TransportConnectionState.CONNECTED) {
                outgoing.emit(packet)
            }
        }

        override fun close() {
            onClose()
        }
    }

    companion object {
        /**
         * Creates a connected `(host, client)` transport pair.
         */
        fun createPair(): Pair<SyncTransport, SyncTransport> {
            val pipe = InMemorySyncPipe()
            return pipe.host to pipe.client
        }
    }
}
