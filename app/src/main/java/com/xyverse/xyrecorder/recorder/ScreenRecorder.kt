package com.xyverse.xyrecorder.recorder

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import com.xyverse.xyrecorder.data.*
import com.xyverse.xyrecorder.audio.AudioProcessor
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Core screen recorder.
 *
 * Uses MediaProjection for screen capture, MediaCodec for H.264 encoding,
 * and MediaMuxer to multiplex video + audio into MP4.
 *
 * Supports:
 * - 720p/1080p/1440p/4K @ 30/60 FPS
 * - H.264 / H.265 / VP8 / VP9 codecs
 * - Microphone audio with studio-quality noise reduction
 * - System/internal audio capture (Android 10+)
 */
class ScreenRecorder(private val context: Context) {

    companion object {
        private const val TAG = "ScreenRecorder"
        private const val TIMEOUT_US = 10_000L
    }

    // State
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var videoEncoder: MediaCodec? = null
    private var audioEncoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var videoInputSurface: Surface? = null
    private var audioProcessor: AudioProcessor? = null

    // Audio capture
    private var micAudioRecord: AudioRecord? = null
    private var systemAudioRecord: AudioRecord? = null
    private var micRecordingThread: Thread? = null
    private var systemRecordingThread: Thread? = null

    // Muxer track indices
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var muxerStarted = false

    // Buffers
    private val bufferInfo = MediaCodec.BufferInfo()
    private var audioBufferInfo = MediaCodec.BufferInfo()

    // Recording state
    @Volatile
    var isRecording = false
        private set

    @Volatile
    var isPaused = false
        private set

    var durationMs: Long = 0L
        private set

    var currentOutputPath: String? = null
        private set

    var micLevelDb: Float = -96f
        private set

    var processedLevelDb: Float = -96f
        private set

    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Callbacks
    var onDurationUpdate: ((Long) -> Unit)? = null
    var onLevelsUpdate: ((Float, Float) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onComplete: ((String) -> Unit)? = null

    /**
     * Start recording.
     *
     * @param resultCode  resultCode from MediaProjection permission dialog
     * @param data        Intent from MediaProjection permission dialog
     * @param videoSettings Video recording settings
     * @param audioSettings Audio settings
     * @param nrSettings  Noise reduction settings
     */
    fun startRecording(
        resultCode: Int,
        data: Intent,
        videoSettings: VideoSettings,
        audioSettings: AudioSettings,
        nrSettings: NoiseReductionSettings
    ) {
        if (isRecording) return

        try {
            // Get MediaProjection
            val projectionManager =
                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection stopped")
                    stopRecording()
                }
            }, null)

            // Prepare output file
            val outputDir = getOutputDirectory()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFile = File(outputDir, "XYRec_$timestamp.mp4")
            currentOutputPath = outputFile.absolutePath

            // Setup video encoder
            setupVideoEncoder(videoSettings, mediaProjection!!)

            // Setup audio encoder
            setupAudioEncoder(audioSettings, nrSettings)

            // Setup muxer
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            videoTrackIndex = -1
            audioTrackIndex = -1
            muxerStarted = false

            // Create virtual display
            val metrics = getDisplayMetrics()
            val density = metrics.densityDpi

            virtualDisplay = mediaProjection!!.createVirtualDisplay(
                "XYRecorder",
                videoSettings.resolution.width,
                videoSettings.resolution.height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                videoInputSurface,
                null, null
            )

            isRecording = true
            isPaused = false
            durationMs = 0L

            // Start encoding loops
            startVideoEncodingLoop()
            startAudioEncodingLoop()
            startDurationTracker()

            Log.i(TAG, "Recording started: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            onError?.invoke("Failed to start: ${e.message}")
            cleanup()
        }
    }

    private fun setupVideoEncoder(settings: VideoSettings, projection: MediaProjection) {
        val format = MediaFormat.createVideoFormat(settings.codec.mimeType, settings.resolution.width, settings.resolution.height)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
            android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, settings.bitrate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, settings.fps)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

        // Try to enable hardware acceleration
        try {
            format.setInteger(MediaFormat.KEY_PROFILE,
                if (settings.codec == VideoCodec.H265)
                    android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain
                else
                    android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
            format.setInteger(MediaFormat.KEY_LEVEL,
                if (settings.codec == VideoCodec.H265)
                    android.media.MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel51
                else
                    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel52)
        } catch (_: Exception) {}

        videoEncoder = MediaCodec.createEncoderByType(settings.codec.mimeType)
        videoEncoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoInputSurface = videoEncoder!!.createInputSurface()
        videoEncoder!!.start()
    }

    private fun setupAudioEncoder(audioSettings: AudioSettings, nrSettings: NoiseReductionSettings) {
        // Create AudioProcessor for mic
        audioProcessor = AudioProcessor(nrSettings)

        // Mic AudioRecord
        if (audioSettings.microphoneEnabled) {
            val micBufferSize = AudioRecord.getMinBufferSize(
                audioSettings.sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ) * 2

            micAudioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(AudioFormat.Builder()
                    .setSampleRate(audioSettings.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build())
                .setBufferSizeInBytes(micBufferSize)
                .build()

            // Attach native noise suppression
            audioProcessor?.attachNativeEffects(micAudioRecord!!.audioSessionId)
            micAudioRecord?.startRecording()
        }

        // System audio (Android 10+)
        if (audioSettings.systemAudioEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val sysBufferSize = AudioRecord.getMinBufferSize(
                    audioSettings.sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                ) * 2

                systemAudioRecord = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX)
                    .setAudioFormat(AudioFormat.Builder()
                        .setSampleRate(audioSettings.sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build())
                    .setBufferSizeInBytes(sysBufferSize)
                    .build()
                systemAudioRecord?.startRecording()
            } catch (e: Exception) {
                Log.w(TAG, "System audio not available: ${e.message}")
            }
        }

        // Audio encoder (AAC)
        val audioFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            audioSettings.sampleRate,
            if (systemAudioRecord != null) 2 else 1
        )
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioSettings.outputBitrate)
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)

        audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        audioEncoder!!.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioEncoder!!.start()

        // Start capture threads
        startMicCaptureThread(audioSettings)
        startSystemAudioCaptureThread(audioSettings)
    }

    private fun startMicCaptureThread(settings: AudioSettings) {
        val record = micAudioRecord ?: return
        val processor = audioProcessor ?: return
        val encoder = audioEncoder ?: return

        val bufferSize = record.bufferSizeInFrames * 2
        val shortBuffer = ShortArray(bufferSize / 2)
        val floatBuffer = FloatArray(bufferSize / 2)

        micRecordingThread = Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            while (isRecording) {
                val read = record.read(shortBuffer, 0, shortBuffer.size)
                if (read > 0 && !isPaused) {
                    // Convert & process through the studio-quality chain
                    processor.processPcm16(shortBuffer, floatBuffer)

                    // Apply volume
                    val vol = settings.microphoneVolume
                    if (vol != 1f) {
                        for (i in 0 until read) floatBuffer[i] *= vol
                    }

                    // Compute level
                    micLevelDb = processor.computeRmsDb(floatBuffer)
                    onLevelsUpdate?.invoke(micLevelDb, processedLevelDb)

                    // Convert back to PCM16 and feed to encoder
                    val pcmBuffer = ShortArray(read)
                    processor.floatToPcm16(floatBuffer, pcmBuffer, read)

                    // Write to audio encoder
                    writeAudioToEncoder(pcmBuffer, read * 2)
                }
            }
        }.apply {
            name = "XYRecorder-MicThread"
            isDaemon = true
            start()
        }
    }

    private fun startSystemAudioCaptureThread(settings: AudioSettings) {
        val record = systemAudioRecord ?: return
        val encoder = audioEncoder ?: return

        val bufferSize = record.bufferSizeInFrames * 4
        val shortBuffer = ShortArray(bufferSize / 2)

        systemRecordingThread = Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            while (isRecording) {
                val read = record.read(shortBuffer, 0, shortBuffer.size)
                if (read > 0 && !isPaused) {
                    // Apply volume
                    val vol = settings.systemAudioVolume
                    if (vol != 1f) {
                        for (i in 0 until read) {
                            shortBuffer[i] = (shortBuffer[i] * vol).toInt().toShort()
                        }
                    }

                    // Feed directly (system audio doesn't need noise reduction)
                    writeAudioToEncoder(shortBuffer, read * 2)
                }
            }
        }.apply {
            name = "XYRecorder-SysThread"
            isDaemon = true
            start()
        }
    }

    @Synchronized
    private fun writeAudioToEncoder(pcmData: ShortArray, dataSize: Int) {
        val encoder = audioEncoder ?: return
        try {
            val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = encoder.getInputBuffer(inputBufferIndex) ?: return
                inputBuffer.clear()
                // Convert short[] to ByteBuffer
                val bb = ByteBuffer.allocate(dataSize)
                for (i in 0 until dataSize / 2) {
                    bb.putShort(pcmData[i])
                }
                bb.flip()
                inputBuffer.put(bb)
                encoder.queueInputBuffer(inputBufferIndex, 0, dataSize, 0, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio encoder write error", e)
        }
    }

    private fun startVideoEncodingLoop() {
        scope.launch {
            while (isRecording) {
                try {
                    drainVideoEncoder()
                } catch (e: Exception) {
                    Log.e(TAG, "Video encoding error", e)
                }
                delay(1) // ~60fps polling
            }
        }
    }

    private fun startAudioEncodingLoop() {
        scope.launch {
            while (isRecording) {
                try {
                    drainAudioEncoder()
                } catch (e: Exception) {
                    Log.w(TAG, "Audio drain error", e)
                }
                delay(10)
            }
        }
    }

    private fun startDurationTracker() {
        scope.launch {
            val startTime = System.currentTimeMillis()
            while (isRecording) {
                if (!isPaused) {
                    durationMs = System.currentTimeMillis() - startTime
                    onDurationUpdate?.invoke(durationMs)
                }
                delay(100)
            }
        }
    }

    private fun drainVideoEncoder() {
        val encoder = videoEncoder ?: return
        val mux = muxer ?: return

        while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            when {
                outputIndex >= 0 -> {
                    val outputBuffer = encoder.getOutputBuffer(outputIndex) ?: continue

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size > 0) {
                        if (!muxerStarted && videoTrackIndex >= 0 && audioTrackIndex >= 0) {
                            mux.start()
                            muxerStarted = true
                        }
                        if (muxerStarted) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            try {
                                mux.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                            } catch (e: Exception) {
                                Log.w(TAG, "Video mux write error", e)
                            }
                        }
                    }

                    encoder.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val format = encoder.outputFormat
                    videoTrackIndex = mux.addTrack(format)
                }
                else -> break
            }
        }
    }

    private fun drainAudioEncoder() {
        val encoder = audioEncoder ?: return
        val mux = muxer ?: return

        audioBufferInfo = MediaCodec.BufferInfo()
        while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(audioBufferInfo, TIMEOUT_US)
            when {
                outputIndex >= 0 -> {
                    val outputBuffer = encoder.getOutputBuffer(outputIndex) ?: continue

                    if (audioBufferInfo.size > 0) {
                        if (!muxerStarted && videoTrackIndex >= 0 && audioTrackIndex >= 0) {
                            mux.start()
                            muxerStarted = true
                        }
                        if (muxerStarted) {
                            outputBuffer.position(audioBufferInfo.offset)
                            outputBuffer.limit(audioBufferInfo.offset + audioBufferInfo.size)
                            try {
                                mux.writeSampleData(audioTrackIndex, outputBuffer, audioBufferInfo)
                            } catch (e: Exception) {
                                Log.w(TAG, "Audio mux write error", e)
                            }
                        }
                    }

                    encoder.releaseOutputBuffer(outputIndex, false)

                    if (audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val format = encoder.outputFormat
                    audioTrackIndex = mux.addTrack(format)
                }
                else -> break
            }
        }
    }

    fun pauseRecording() {
        if (!isRecording || isPaused) return
        isPaused = true
        virtualDisplay?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Pause by releasing the surface temporarily
            }
        }
    }

    fun resumeRecording() {
        if (!isRecording || !isPaused) return
        isPaused = false
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false

        scope.launch {
            try {
                // Send EOS to encoders
                videoEncoder?.signalEndOfInputStream()
                delay(500)

                // Release in order
                virtualDisplay?.release()
                videoEncoder?.stop()
                videoEncoder?.release()
                audioEncoder?.stop()
                audioEncoder?.release()

                if (muxerStarted) {
                    muxer?.stop()
                }
                muxer?.release()

                micAudioRecord?.stop()
                micAudioRecord?.release()
                systemAudioRecord?.stop()
                systemAudioRecord?.release()

                mediaProjection?.stop()
                audioProcessor?.release()

                val path = currentOutputPath
                if (path != null) {
                    Log.i(TAG, "Recording saved: $path (${durationMs}ms)")
                    onComplete?.invoke(path)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording", e)
                onError?.invoke("Error saving: ${e.message}")
            } finally {
                cleanup()
            }
        }
    }

    private fun cleanup() {
        try {
            virtualDisplay?.release()
            videoEncoder?.stop()
            videoEncoder?.release()
            audioEncoder?.stop()
            audioEncoder?.release()
            muxer?.stop()
            muxer?.release()
            micAudioRecord?.stop()
            micAudioRecord?.release()
            systemAudioRecord?.stop()
            systemAudioRecord?.release()
            mediaProjection?.stop()
            audioProcessor?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup error", e)
        }

        videoEncoder = null
        audioEncoder = null
        muxer = null
        virtualDisplay = null
        mediaProjection = null
        videoInputSurface = null
        micAudioRecord = null
        systemAudioRecord = null
        audioProcessor = null
        videoTrackIndex = -1
        audioTrackIndex = -1
        muxerStarted = false
    }

    private fun getOutputDirectory(): File {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "XYRecorder"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }
}
