package com.splitsignal.app.platform

import androidx.compose.runtime.Composable
import com.splitsignal.core.domain.PlatformFileHandle

/**
 * Platform-specific file picker bridge returning [PlatformFileHandle] instances
 * suitable for 1 GB chunked streaming transfers.
 */
expect class PlatformFileBridge {
    val canPickFiles: Boolean

    fun pickFile(onPicked: (PlatformFileHandle?) -> Unit)
}

/**
 * Remembers a [PlatformFileBridge] bound to the current Compose host.
 */
@Composable
expect fun rememberPlatformFileBridge(): PlatformFileBridge
