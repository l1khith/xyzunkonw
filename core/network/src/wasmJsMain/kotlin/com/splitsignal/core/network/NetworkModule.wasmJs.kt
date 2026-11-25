package com.splitsignal.core.network

import com.splitsignal.core.domain.FileTransferTransport
import com.splitsignal.core.domain.LanDiscovery
import com.splitsignal.core.domain.LocalIpProvider
import com.splitsignal.core.domain.LocalStorageProvider
import com.splitsignal.core.domain.TransportFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val networkModule: Module = module {
    single<LocalIpProvider> { WasmLocalIpProvider() }
    single<LocalStorageProvider> { WasmLocalStorageProvider() }
    single<FileTransferTransport> { WasmFileTransferTransport() }
    single<TransportFactory> { WasmTransportFactory() }
    single<LanDiscovery> { WasmLanDiscovery() }
}
