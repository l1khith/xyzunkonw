package com.splitsignal.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.splitsignal.core.domain.PlatformFileHandle
import com.splitsignal.core.network.StreamableFileHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineEvent
import javax.sound.sampled.TargetDataLine
import javax.swing.JFileChooser
import kotlin.math.abs

/**
 * Desktop JVM implementation of [PlatformMediaBridge] using [JFileChooser], [Desktop],
 * [TargetDataLine], and [Clip].
 */
actual class PlatformMediaBridge internal constructor(
    private val scope: CoroutineScope,
) {
    actual val canPickDirectory: Boolean = true
    actual val canRecordAudio: Boolean = runCatching {
        AudioSystem.isLineSupported(DataLine.Info(TargetDataLine::class.java, RECORD_FORMAT))
    }.getOrDefault(false)

    private val isRecording = AtomicBoolean(false)
    private var targetLine: TargetDataLine? = null
    private var captureThread: Thread? = null
    private var capturedBytes: ByteArrayOutputStream? = null

    @Volatile
    private var latestAmplitude: Float = 0f

    private var activeClip: Clip? = null

    actual fun pickDirectory(onPicked: (String?) -> Unit) {
        scope.launch {
            val selectedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val chooser = JFileChooser().apply {
                        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                        dialogTitle = "Select Save Directory"
                        isAcceptAllFileFilterUsed = false
                    }
                    val result = chooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        chooser.selectedFile?.absolutePath
                    } else {
                        null
                    }
                }.getOrNull()
            }
            onPicked(selectedPath)
        }
    }

    actual fun shareFile(filePath: String, mimeType: String, onResult: (String) -> Unit) {
        val opened = runCatching {
            val file = File(filePath)
            val folder = if (file.exists()) (file.parentFile ?: file) else null
            if (folder != null && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(folder)
                folder.absolutePath
            } else {
                null
            }
        }.getOrNull()

        if (opened != null) {
            onResult("Opened folder: $opened")
            return
        }

        val copied = runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(filePath), null)
        }.isSuccess
        if (copied) {
            onResult("Path copied to clipboard: $filePath")
        } else {
            onResult("Unable to share file")
        }
    }

    actual fun startVoiceRecording(): Boolean {
        stopRecordingInternal()
        return runCatching {
            val info = DataLine.Info(TargetDataLine::class.java, RECORD_FORMAT)
            val line = AudioSystem.getLine(info) as TargetDataLine
            line.open(RECORD_FORMAT)
            line.start()

            val bufferStream = ByteArrayOutputStream()
            targetLine = line
            capturedBytes = bufferStream
            latestAmplitude = 0f
            isRecording.set(true)

            val thread = Thread {
                val readBuffer = ByteArray(2048)
                while (isRecording.get()) {
                    val bytesRead = runCatching { line.read(readBuffer, 0, readBuffer.size) }.getOrDefault(-1)
                    if (bytesRead > 0) {
                        bufferStream.write(readBuffer, 0, bytesRead)
                        latestAmplitude = computePeakAmplitude(readBuffer, bytesRead)
                    } else if (bytesRead < 0) {
                        break
                    }
                }
            }.apply {
                isDaemon = true
                name = "SplitSignal-VoiceCapture"
                start()
            }
            captureThread = thread
            true
        }.getOrElse {
            stopRecordingInternal()
            false
        }
    }

    actual fun stopVoiceRecording(): PlatformFileHandle? {
        if (!isRecording.getAndSet(false)) {
            stopRecordingInternal()
            return null
        }

        val line = targetLine
        val thread = captureThread
        val bufferStream = capturedBytes
        targetLine = null
        captureThread = null
        capturedBytes = null
        latestAmplitude = 0f

        runCatching { line?.stop() }
        runCatching { line?.close() }
        runCatching { thread?.join(1000L) }

        val pcmBytes = bufferStream?.toByteArray() ?: return null
        if (pcmBytes.isEmpty()) return null

        val outFile = runCatching {
            val file = File.createTempFile("voice_note_", ".wav")
            val frameCount = (pcmBytes.size / RECORD_FORMAT.frameSize).toLong()
            AudioInputStream(ByteArrayInputStream(pcmBytes), RECORD_FORMAT, frameCount).use { ais ->
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file)
            }
            file
        }.getOrNull() ?: return null

        if (!outFile.exists() || outFile.length() <= 0L) {
            runCatching { outFile.delete() }
            return null
        }

        return object : StreamableFileHandle {
            override val id: String = outFile.absolutePath
            override val name: String = outFile.name
            override val sizeBytes: Long = outFile.length()
            override val mimeType: String = "audio/wav"

            override fun openInputStream(): InputStream = FileInputStream(outFile)
        }
    }

    actual fun currentRecordingAmplitude(): Float = if (isRecording.get()) latestAmplitude else 0f

    actual fun playAudioFile(filePath: String, onFinished: () -> Unit) {
        stopAudioPlayback()
        val started = runCatching {
            val file = File(filePath)
            val audioStream = AudioSystem.getAudioInputStream(file)
            val clip = AudioSystem.getClip()
            clip.addLineListener { event ->
                if (event.type == LineEvent.Type.STOP) {
                    stopAudioPlayback()
                    onFinished()
                }
            }
            clip.open(audioStream)
            activeClip = clip
            clip.start()
        }.isSuccess

        if (!started) {
            stopAudioPlayback()
            onFinished()
        }
    }

    actual fun stopAudioPlayback() {
        val clip = activeClip ?: return
        activeClip = null
        runCatching { clip.stop() }
        runCatching { clip.close() }
    }

    internal fun releaseAll() {
        stopRecordingInternal()
        stopAudioPlayback()
    }

    private fun stopRecordingInternal() {
        isRecording.set(false)
        val line = targetLine
        val thread = captureThread
        targetLine = null
        captureThread = null
        capturedBytes = null
        latestAmplitude = 0f
        runCatching { line?.stop() }
        runCatching { line?.close() }
        runCatching { thread?.join(500L) }
    }

    private fun computePeakAmplitude(pcm16Le: ByteArray, length: Int): Float {
        var peak = 0
        var i = 0
        while (i + 1 < length) {
            val low = pcm16Le[i].toInt() and 0xFF
            val high = pcm16Le[i + 1].toInt()
            val sample = (high shl 8) or low
            val magnitude = abs(sample)
            if (magnitude > peak) peak = magnitude
            i += 2
        }
        return (peak / 32767f).coerceIn(0f, 1f)
    }

    private companion object {
        val RECORD_FORMAT = AudioFormat(16_000f, 16, 1, true, false)
    }
}

/**
 * Remembers a Desktop [PlatformMediaBridge] bound to the composition coroutine scope.
 */
@Composable
actual fun rememberPlatformMediaBridge(): PlatformMediaBridge {
    val scope = rememberCoroutineScope()
    val bridge = remember(scope) { PlatformMediaBridge(scope) }
    DisposableEffect(bridge) {
        onDispose {
            bridge.releaseAll()
        }
    }
    return bridge
}
