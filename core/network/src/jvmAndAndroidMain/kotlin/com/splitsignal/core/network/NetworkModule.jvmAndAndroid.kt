package com.splitsignal.core.network

import com.splitsignal.core.domain.FileTransferTransport
import com.splitsignal.core.domain.LanDiscovery
import com.splitsignal.core.domain.LocalIpProvider
import com.splitsignal.core.domain.LocalStorageProvider
import com.splitsignal.core.domain.TransportFactory
import java.io.File
import org.koin.core.module.Module
import org.koin.dsl.module

actual val networkModule: Module = module {
    single<LocalIpProvider> { JvmLocalIpProvider() }
    single<LocalStorageProvider> { JvmLocalStorageProvider() }
    single { JvmFileTransferTransport(outputDir = File(resolveSplitSignalDataDir(), "transfers")) }
    single<FileTransferTransport> { get<JvmFileTransferTransport>() }
    single<TransportFactory> {
        JvmTransportFactory(
            localIpProvider = get(),
            fileTransferTransport = get(),
        )
    }
    single<LanDiscovery> { JvmLanDiscovery(get()) }
}
