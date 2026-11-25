package com.splitsignal.app.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.splitsignal.core.domain.PlatformFileHandle
import com.splitsignal.core.network.StreamableFileHandle
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

private class AndroidDirectoryCallbackHolder {
    var onPicked: ((String?) -> Unit)? = null
}

/**
 * Android implementation of [PlatformMediaBridge] using SAF [ActivityResultContracts.OpenDocumentTree],
 * [Intent.ACTION_SEND], [MediaRecorder], and [MediaPlayer].
 */
actual class PlatformMediaBridge internal constructor(
    private val context: Context,
    private val launchDirectoryPicker: ((String?) -> Unit) -> Unit,
) {
    actual val canPickDirectory: Boolean = true
    actual val canRecordAudio: Boolean = true

    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var mediaPlayer: MediaPlayer? = null

    actual fun pickDirectory(onPicked: (String?) -> Unit) {
        launchDirectoryPicker(onPicked)
    }

    actual fun shareFile(filePath: String, mimeType: String, onResult: (String) -> Unit) {
        val shareOutcome = runCatching {
            val uri = if (filePath.startsWith("content://")) {
                Uri.parse(filePath)
            } else {
                val file = File(filePath)
                require(file.exists()) { "File does not exist: $filePath" }
                Uri.fromFile(file)
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share file").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
        if (shareOutcome.isSuccess) {
            onResult("Share sheet opened")
            return
        }

        val clipboardOutcome = runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("File path", filePath))
        }
        if (clipboardOutcome.isSuccess) {
            onResult("Path copied to clipboard: $filePath")
        } else {
            onResult("Unable to share file")
        }
    }

    actual fun startVoiceRecording(): Boolean {
        stopRecorderInternal(deleteFile = true)
        val outFile = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
        val started = runCatching {
            val instance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            instance.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recorder = instance
            recordingFile = outFile
        }.isSuccess

        if (!started) {
            stopRecorderInternal(deleteFile = true)
        }
        return started
    }

    actual fun stopVoiceRecording(): PlatformFileHandle? {
        val activeRecorder = recorder ?: return null
        val file = recordingFile
        recorder = null
        recordingFile = null

        val stoppedCleanly = runCatching {
            activeRecorder.stop()
        }.isSuccess
        runCatching { activeRecorder.reset() }
        runCatching { activeRecorder.release() }

        if (!stoppedCleanly || file == null || !file.exists() || file.length() <= 0L) {
            runCatching { file?.delete() }
            return null
        }

        return object : StreamableFileHandle {
            override val id: String = file.absolutePath
            override val name: String = file.name
            override val sizeBytes: Long = file.length()
            override val mimeType: String = "audio/mp4"

            override fun openInputStream(): InputStream = FileInputStream(file)
        }
    }

    actual fun currentRecordingAmplitude(): Float {
        val activeRecorder = recorder ?: return 0f
        return runCatching {
            (activeRecorder.maxAmplitude / 32767f).coerceIn(0f, 1f)
        }.getOrDefault(0f)
    }

    actual fun playAudioFile(filePath: String, onFinished: () -> Unit) {
        stopAudioPlayback()
        val started = runCatching {
            val player = MediaPlayer().apply {
                if (filePath.startsWith("content://")) {
                    setDataSource(context, Uri.parse(filePath))
                } else {
                    setDataSource(filePath)
                }
                setOnCompletionListener {
                    stopAudioPlayback()
                    onFinished()
                }
                setOnErrorListener { _, _, _ ->
                    stopAudioPlayback()
                    onFinished()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
        }.isSuccess

        if (!started) {
            stopAudioPlayback()
            onFinished()
        }
    }

    actual fun stopAudioPlayback() {
        val active = mediaPlayer ?: return
        mediaPlayer = null
        runCatching { active.stop() }
        runCatching { active.release() }
    }

    internal fun releaseAll() {
        stopRecorderInternal(deleteFile = true)
        stopAudioPlayback()
    }

    private fun stopRecorderInternal(deleteFile: Boolean) {
        val active = recorder
        val file = recordingFile
        recorder = null
        recordingFile = null
        if (active != null) {
            runCatching { active.stop() }
            runCatching { active.reset() }
            runCatching { active.release() }
        }
        if (deleteFile && file != null) {
            runCatching { file.delete() }
        }
    }
}

/**
 * Remembers an Android [PlatformMediaBridge] backed by [rememberLauncherForActivityResult].
 */
@Composable
actual fun rememberPlatformMediaBridge(): PlatformMediaBridge {
    val context = LocalContext.current
    val callbackHolder = remember { AndroidDirectoryCallbackHolder() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        val callback = callbackHolder.onPicked
        callbackHolder.onPicked = null
        if (uri != null) {
            runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
        }
        callback?.invoke(uri?.toString())
    }

    val bridge = remember(context, launcher) {
        PlatformMediaBridge(
            context = context.applicationContext,
            launchDirectoryPicker = { onPicked ->
                callbackHolder.onPicked = onPicked
                launcher.launch(null)
            },
        )
    }

    DisposableEffect(bridge) {
        onDispose {
            bridge.releaseAll()
        }
    }

    return bridge
}
