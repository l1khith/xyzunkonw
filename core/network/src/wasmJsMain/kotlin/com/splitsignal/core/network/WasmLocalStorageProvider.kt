package com.splitsignal.core.network

import com.splitsignal.core.domain.LocalStorageProvider

/**
 * In-memory [LocalStorageProvider] implementation for WasmJs targets.
 */
class WasmLocalStorageProvider : LocalStorageProvider {
    private val entries = mutableMapOf<String, String>()

    override fun readText(key: String): String? = entries[key]

    override fun writeText(key: String, value: String) {
        entries[key] = value
    }
}
