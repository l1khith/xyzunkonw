package com.splitsignal.core.network

import org.koin.core.module.Module

/**
 * Platform Koin module providing [com.splitsignal.core.domain.LocalIpProvider] and
 * [com.splitsignal.core.domain.TransportFactory].
 */
expect val networkModule: Module
