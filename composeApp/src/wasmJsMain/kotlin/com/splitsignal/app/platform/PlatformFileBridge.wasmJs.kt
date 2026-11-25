package com.splitsignal.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.splitsignal.core.domain.PlatformFileHandle

/**
 * Wasm/browser stub [PlatformFileBridge] where local streaming file picking is unsupported.
 */
actual class PlatformFileBridge {
    actual val canPickFiles: Boolean = false

    actual fun pickFile(onPicked: (PlatformFileHandle?) -> Unit) {
        onPicked(null)
    }
}

/**
 * Remembers a no-op [PlatformFileBridge] for browser targets.
 */
@Composable
actual fun rememberPlatformFileBridge(): PlatformFileBridge = remember { PlatformFileBridge() }
