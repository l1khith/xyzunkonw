package com.splitsignal.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.splitsignal.core.domain.PlatformFileHandle
import com.splitsignal.core.network.StreamableFileHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files

/**
 * Desktop AWT [PlatformFileBridge] using [FileDialog] and [FileInputStream].
 */
actual class PlatformFileBridge internal constructor(
    private val scope: CoroutineScope,
) {
    actual val canPickFiles: Boolean = true

    actual fun pickFile(onPicked: (PlatformFileHandle?) -> Unit) {
        scope.launch {
            val handle = withContext(Dispatchers.IO) {
                val dialog = FileDialog(null as Frame?, "Select File to Send", FileDialog.LOAD)
                dialog.isVisible = true
                val directory = dialog.directory
                val fileName = dialog.file
                if (directory == null || fileName == null) {
                    null
                } else {
                    val file = File(directory, fileName)
                    if (file.exists() && file.isFile) {
                        toStreamableHandle(file)
                    } else {
                        null
                    }
                }
            }
            onPicked(handle)
        }
    }
}

/**
 * Remembers a Desktop [PlatformFileBridge] bound to the composition coroutine scope.
 */
@Composable
actual fun rememberPlatformFileBridge(): PlatformFileBridge {
    val scope = rememberCoroutineScope()
    return remember(scope) { PlatformFileBridge(scope) }
}

private fun toStreamableHandle(file: File): StreamableFileHandle {
    val detectedMime = runCatching { Files.probeContentType(file.toPath()) }.getOrNull()
        ?: "application/octet-stream"
    return object : StreamableFileHandle {
        override val id: String = file.absolutePath
        override val name: String = file.name
        override val sizeBytes: Long = file.length()
        override val mimeType: String = detectedMime

        override fun openInputStream(): InputStream = FileInputStream(file)
    }
}
