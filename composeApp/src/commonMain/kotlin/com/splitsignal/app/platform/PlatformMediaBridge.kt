package com.splitsignal.app.platform

import androidx.compose.runtime.Composable
import com.splitsignal.core.domain.PlatformFileHandle

/**
 * Platform-specific media, directory picker, sharing, and voice-note bridge.
 */
expect class PlatformMediaBridge {
    val canPickDirectory: Boolean
    val canRecordAudio: Boolean

    fun pickDirectory(onPicked: (String?) -> Unit)

    fun shareFile(filePath: String, mimeType: String, onResult: (String) -> Unit)

    fun startVoiceRecording(): Boolean

    fun stopVoiceRecording(): PlatformFileHandle?

    fun currentRecordingAmplitude(): Float

    fun playAudioFile(filePath: String, onFinished: () -> Unit)

    fun stopAudioPlayback()
}

/**
 * Remembers a [PlatformMediaBridge] bound to the current Compose host.
 */
@Composable
expect fun rememberPlatformMediaBridge(): PlatformMediaBridge
