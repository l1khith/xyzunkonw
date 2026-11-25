package com.splitsignal.app.di

import com.splitsignal.app.presentation.SplitSignalStateHolder
import com.splitsignal.core.domain.Fnv1a
import com.splitsignal.core.domain.LocalIpProvider
import com.splitsignal.core.domain.ProgressionStore
import com.splitsignal.core.domain.UserIdentityStore
import com.splitsignal.core.network.networkModule
import kotlinx.coroutines.CoroutineScope
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
val commonAppModule: Module = module {
    includes(networkModule)
    single { ProgressionStore() }
    single {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val ipHint = runCatching { get<LocalIpProvider>().getLocalIpv4Address() }.getOrDefault("")
        val nonce = Random.nextInt()
        val deviceSeed = Fnv1a.hash32("SPLIT_ID:$nowMs:$ipHint:$nonce")
        UserIdentityStore(storage = get(), initialSeed = deviceSeed)
    }
    factory { (scope: CoroutineScope) ->
        SplitSignalStateHolder(
            transportFactory = get(),
            lanDiscovery = get(),
            progressionStore = get(),
            identityStore = get(),
            fileTransferTransport = getOrNull(),
            scope = scope,
        )
    }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(commonAppModule)
    }
