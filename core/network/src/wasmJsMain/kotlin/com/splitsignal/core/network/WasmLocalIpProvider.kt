package com.splitsignal.core.network

import com.splitsignal.core.domain.LocalIpProvider

/**
 * Browser sandbox [LocalIpProvider] returning the loopback IPv4 address.
 */
class WasmLocalIpProvider : LocalIpProvider {
    override fun getLocalIpv4Address(): String = "127.0.0.1"
}
