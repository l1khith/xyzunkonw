package com.splitsignal.app.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.splitsignal.core.domain.PlatformFileHandle
import com.splitsignal.core.network.StreamableFileHandle
import java.io.InputStream

private class AndroidCallbackHolder {
    var onPicked: ((PlatformFileHandle?) -> Unit)? = null
}

/**
 * Android SAF-backed [PlatformFileBridge] using [ActivityResultContracts.OpenDocument].
 */
actual class PlatformFileBridge internal constructor(
    private val launchPicker: ((PlatformFileHandle?) -> Unit) -> Unit,
) {
    actual val canPickFiles: Boolean = true

    actual fun pickFile(onPicked: (PlatformFileHandle?) -> Unit) {
        launchPicker(onPicked)
    }
}

/**
 * Remembers an Android [PlatformFileBridge] backed by [rememberLauncherForActivityResult].
 */
@Composable
actual fun rememberPlatformFileBridge(): PlatformFileBridge {
    val context = LocalContext.current
    val callbackHolder = remember { AndroidCallbackHolder() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        val callback = callbackHolder.onPicked
        callbackHolder.onPicked = null
        val handle = uri?.let { resolveStreamableHandle(context, it) }
        callback?.invoke(handle)
    }

    return remember(launcher) {
        PlatformFileBridge { onPicked ->
            callbackHolder.onPicked = onPicked
            launcher.launch(arrayOf("*/*"))
        }
    }
}

private fun resolveStreamableHandle(context: Context, uri: Uri): StreamableFileHandle? {
    val resolver = context.contentResolver
    var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
    var sizeBytes = 0L

    resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIdx >= 0) {
                cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
            }
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                sizeBytes = cursor.getLong(sizeIdx).coerceAtLeast(0L)
            }
        }
    }

    val mimeType = resolver.getType(uri) ?: "application/octet-stream"
    return object : StreamableFileHandle {
        override val id: String = uri.toString()
        override val name: String = displayName
        override val sizeBytes: Long = sizeBytes
        override val mimeType: String = mimeType

        override fun openInputStream(): InputStream {
            return resolver.openInputStream(uri)
                ?: throw IllegalStateException("Unable to open input stream for $uri")
        }
    }
}
