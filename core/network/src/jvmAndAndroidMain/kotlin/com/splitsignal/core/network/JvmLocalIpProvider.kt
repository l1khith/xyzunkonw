package com.splitsignal.core.network

import com.splitsignal.core.domain.LocalIpProvider
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * JVM and Android [LocalIpProvider] resolving the active site-local IPv4 address.
 */
class JvmLocalIpProvider : LocalIpProvider {
    override fun getLocalIpv4Address(): String {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching DEFAULT_FALLBACK_IP
            var fallbackIpv4: String? = null

            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val isEligible = runCatching {
                    networkInterface.isUp && !networkInterface.isLoopback
                }.getOrDefault(false)
                if (!isEligible) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val hostAddress = address.hostAddress
                        if (!hostAddress.isNullOrBlank()) {
                            if (address.isSiteLocalAddress) {
                                return@runCatching hostAddress
                            }
                            if (fallbackIpv4 == null) {
                                fallbackIpv4 = hostAddress
                            }
                        }
                    }
                }
            }

            fallbackIpv4 ?: DEFAULT_FALLBACK_IP
        }.getOrDefault(DEFAULT_FALLBACK_IP)
    }

    private companion object {
        const val DEFAULT_FALLBACK_IP = "127.0.0.1"
    }
}
