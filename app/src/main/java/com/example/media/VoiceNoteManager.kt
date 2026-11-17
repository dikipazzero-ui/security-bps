package com.example.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

data class VoiceRecordingState(
    val isRecording: Boolean = false,
    val elapsedSeconds: Int = 0,
    val formattedTimer: String = "00:00",
    val amplitudeNormalized: Float = 0.2f
)

data class VoicePlaybackState(
    val activeMessageId: String? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f
)

class VoiceNoteManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingTimerJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null

    private val _recordingState = MutableStateFlow(VoiceRecordingState())
    val recordingState: StateFlow<VoiceRecordingState> = _recordingState.asStateFlow()

    private val _playbackState = MutableStateFlow(VoicePlaybackState())
    val playbackState: StateFlow<VoicePlaybackState> = _playbackState.asStateFlow()

    fun startRecording(): Boolean {
        if (_recordingState.value.isRecording) return false
        stopPlayback()

        return try {
            val dir = File(context.filesDir, "voice_notes").apply { mkdirs() }
            val outFile = File(dir, "vn_${System.currentTimeMillis()}.m4a")
            recordingFile = outFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(24000)
                setMaxDuration(60_000)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder

            _recordingState.value = VoiceRecordingState(
                isRecording = true,
                elapsedSeconds = 0,
                formattedTimer = "00:00",
                amplitudeNormalized = 0.25f
            )

            recordingTimerJob?.cancel()
            recordingTimerJob = scope.launch {
                val startMs = System.currentTimeMillis()
                while (isActive && _recordingState.value.isRecording) {
                    val elapsedSec = ((System.currentTimeMillis() - startMs) / 1000L).toInt()
                    val mins = elapsedSec / 60
                    val secs = elapsedSec % 60
                    val amp = try {
                        val raw = mediaRecorder?.maxAmplitude ?: 0
                        (raw / 32767f).coerceIn(0.15f, 1.0f)
                    } catch (_: Exception) {
                        0.3f
                    }
                    _recordingState.value = VoiceRecordingState(
                        isRecording = true,
                        elapsedSeconds = elapsedSec,
                        formattedTimer = String.format("%02d:%02d", mins, secs),
                        amplitudeNormalized = amp
                    )
                    delay(200L)
                }
            }
            true
        } catch (_: Exception) {
            releaseRecorder()
            _recordingState.value = VoiceRecordingState()
            false
        }
    }

    fun cancelRecording() {
        recordingTimerJob?.cancel()
        releaseRecorder()
        recordingFile?.delete()
        recordingFile = null
        _recordingState.value = VoiceRecordingState()
    }

    fun stopAndExtractBase64(): Pair<String, String>? {
        recordingTimerJob?.cancel()
        val duration = _recordingState.value.formattedTimer.let {
            if (it == "00:00") "00:01" else it
        }
        val file = recordingFile
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        } finally {
            releaseRecorder()
            _recordingState.value = VoiceRecordingState()
        }

        if (file == null || !file.exists() || file.length() == 0L) return null
        return try {
            val bytes = file.readBytes()
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (encoded.length > 240_000) {
                null
            } else {
                encoded to duration
            }
        } catch (_: Exception) {
            null
        }
    }

    fun togglePlayVoiceNote(
        messageId: String,
        audioType: String,
        audioData: String?
    ) {
        val current = _playbackState.value
        if (current.activeMessageId == messageId && current.isPlaying) {
            stopPlayback()
            return
        }
        stopPlayback()

        if (audioType == "simulated" || audioData.isNullOrBlank()) {
            playSimulatedTone(messageId)
        } else {
            playRecordedAudio(messageId, audioData)
        }
    }

    private fun playRecordedAudio(messageId: String, audioData: String) {
        try {
            val bytes = Base64.decode(audioData, Base64.DEFAULT)
            val tempFile = File(context.cacheDir, "play_${messageId.hashCode()}.m4a")
            tempFile.writeBytes(bytes)

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                }
                start()
            }
            mediaPlayer = player
            _playbackState.value = VoicePlaybackState(
                activeMessageId = messageId,
                isPlaying = true,
                progress = 0f
            )
            playbackJob = scope.launch {
                while (isActive && mediaPlayer?.isPlaying == true) {
                    val dur = mediaPlayer?.duration?.coerceAtLeast(1) ?: 1
                    val pos = mediaPlayer?.currentPosition ?: 0
                    _playbackState.value = VoicePlaybackState(
                        activeMessageId = messageId,
                        isPlaying = true,
                        progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                    )
                    delay(100L)
                }
            }
        } catch (_: Exception) {
            playSimulatedTone(messageId)
        }
    }

    private fun playSimulatedTone(messageId: String) {
        _playbackState.value = VoicePlaybackState(
            activeMessageId = messageId,
            isPlaying = true,
            progress = 0f
        )

        playbackJob = scope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 16000
                val durationMs = 450
                val count = sampleRate * durationMs / 1000
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val freq = 520.0 + (160.0 * (i.toDouble() / count))
                    val env = 1.0 - (i.toDouble() / count)
                    samples[i] = (sin(2.0 * PI * freq * t) * 5000.0 * env).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                delay(500L)
                track.stop()
                track.release()
            } catch (_: Exception) {
            }

            for (step in 1..15) {
                if (!isActive) break
                _playbackState.value = VoicePlaybackState(
                    activeMessageId = messageId,
                    isPlaying = true,
                    progress = (step / 15f).coerceIn(0f, 1f)
                )
                delay(120L)
            }
            _playbackState.value = VoicePlaybackState()
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        _playbackState.value = VoicePlaybackState()
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }
}
