package com.splitsignal.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.splitsignal.core.domain.PlatformFileHandle

/**
 * Wasm/browser no-op [PlatformMediaBridge] where directory picking and mic capture are unsupported.
 */
actual class PlatformMediaBridge {
    actual val canPickDirectory: Boolean = false
    actual val canRecordAudio: Boolean = false

    actual fun pickDirectory(onPicked: (String?) -> Unit) {
        onPicked(null)
    }

    actual fun shareFile(filePath: String, mimeType: String, onResult: (String) -> Unit) {
        onResult("Sharing not supported in browser")
    }

    actual fun startVoiceRecording(): Boolean = false

    actual fun stopVoiceRecording(): PlatformFileHandle? = null

    actual fun currentRecordingAmplitude(): Float = 0f

    actual fun playAudioFile(filePath: String, onFinished: () -> Unit) {
        onFinished()
    }

    actual fun stopAudioPlayback() = Unit
}

/**
 * Remembers a no-op [PlatformMediaBridge] for browser targets.
 */
@Composable
actual fun rememberPlatformMediaBridge(): PlatformMediaBridge = remember { PlatformMediaBridge() }
