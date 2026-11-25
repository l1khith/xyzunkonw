package com.splitsignal.core.network

import com.splitsignal.core.domain.LocalStorageProvider
import java.io.File

/**
 * Resolves the writable application data directory, preferring the Android-provided
 * `splitsignal.data.dir` system property before falling back to `~/.splitsignal` or temp.
 */
fun resolveSplitSignalDataDir(): File {
    val override = System.getProperty("splitsignal.data.dir")?.trim().orEmpty()
    if (override.isNotEmpty()) {
        return File(override)
    }
    val userHome = System.getProperty("user.home")?.trim().orEmpty()
    if (userHome.isNotEmpty() && userHome != "/" && userHome != "?") {
        return File(userHome, ".splitsignal")
    }
    return File(System.getProperty("java.io.tmpdir", "."), "splitsignal")
}

/**
 * File-backed [LocalStorageProvider] storing UTF-8 entries under [baseDir].
 */
class JvmLocalStorageProvider(
    private val baseDir: File = resolveSplitSignalDataDir(),
) : LocalStorageProvider {

    override fun readText(key: String): String? {
        val file = resolveFile(key)
        if (!file.exists() || !file.isFile) return null
        return runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
    }

    override fun writeText(key: String, value: String) {
        baseDir.mkdirs()
        val file = resolveFile(key)
        file.parentFile?.mkdirs()
        file.writeText(value, Charsets.UTF_8)
    }

    private fun resolveFile(key: String): File {
        val safeKey = key.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").ifEmpty { "default" }
        return File(baseDir, "$safeKey.dat")
    }
}
