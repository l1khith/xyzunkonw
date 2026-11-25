package com.splitsignal.core.domain

/**
 * Resolves the local device's IPv4 address for LAN session hosting.
 */
interface LocalIpProvider {
    fun getLocalIpv4Address(): String
}
