package com.splitsignal.core.network

import com.splitsignal.core.domain.ClientSyncTransport
import com.splitsignal.core.domain.HostSyncTransport
import com.splitsignal.core.domain.InMemorySyncPipe
import com.splitsignal.core.domain.SyncTransport
import com.splitsignal.core.domain.TransportFactory
import io.ktor.client.engine.js.Js
import kotlinx.coroutines.CoroutineScope

/**
 * Wasm/browser [TransportFactory] supporting loopback and JS WebSocket clients without LAN hosting.
 */
class WasmTransportFactory : TransportFactory {
    override val canHostLan: Boolean = false

    override fun createInMemoryLoopback(): SyncTransport {
        return InMemorySyncPipe().localLoopback
    }

    override fun createHost(roomCode: String, scope: CoroutineScope): HostSyncTransport {
        throw UnsupportedOperationException("LAN hosting is not supported in browser sandbox")
    }

    override fun createClient(scope: CoroutineScope): ClientSyncTransport {
        return KtorSyncClient(
            httpClientEngineFactory = Js,
            scope = scope,
        )
    }
}
