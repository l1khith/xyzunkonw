package com.splitsignal.core.network

import com.splitsignal.core.domain.ClientSyncTransport
import com.splitsignal.core.domain.HostSyncTransport
import com.splitsignal.core.domain.InMemorySyncPipe
import com.splitsignal.core.domain.LocalIpProvider
import com.splitsignal.core.domain.SyncTransport
import com.splitsignal.core.domain.TransportFactory
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * JVM and Android [TransportFactory] supporting loopback, LAN hosting, and CIO WebSocket clients.
 */
class JvmTransportFactory(
    private val localIpProvider: LocalIpProvider,
    private val fileTransferTransport: JvmFileTransferTransport? = null,
) : TransportFactory {
    override val canHostLan: Boolean = true

    override fun createInMemoryLoopback(): SyncTransport {
        return InMemorySyncPipe().localLoopback
    }

    override fun createHost(roomCode: String, scope: CoroutineScope): HostSyncTransport {
        return KtorSyncHost(
            roomCode = roomCode,
            localIpProvider = localIpProvider,
            scope = scope,
            fileTransferTransport = fileTransferTransport,
        )
    }

    override fun createClient(scope: CoroutineScope): ClientSyncTransport {
        return KtorSyncClient(
            httpClientEngineFactory = CIO,
            scope = scope,
            dispatcher = Dispatchers.IO,
        )
    }
}
