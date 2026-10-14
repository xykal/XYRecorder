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
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

/**
 * Core screen recorder with MP4 output.
 */
class ScreenRecorder(private val context: Context) {

    companion object {
        private const val TAG = "ScreenRecorder"
        private const val TIMEOUT_US = 10_000L
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var videoEncoder: MediaCodec? = null
    private var audioEncoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var videoInputSurface: Surface? = null
    private var audioProcessor: AudioProcessor? = null

    private var micAudioRecord: AudioRecord? = null
    private var systemAudioRecord: AudioRecord? = null
    private var micRecordingThread: Thread? = null

    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var muxerStarted = false

    private val bufferInfo = MediaCodec.BufferInfo()
    private var audioBufferInfo = MediaCodec.BufferInfo()

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

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    var onDurationUpdate: ((Long) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onComplete: ((String) -> Unit)? = null

    fun startRecording(resultCode: Int, data: Intent) {
        if (isRecording) return

        try {
            // Get MediaProjection
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection stopped")
                    stopRecording()
                }
            }, null)

            // Prepare output file (MP4 format)
            val outputDir = getOutputDirectory()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFile = File(outputDir, "XYRec_$timestamp.mp4")
            currentOutputPath = outputFile.absolutePath

            // Setup video encoder (H.264 for best compatibility)
            setupVideoEncoder()

            // Setup audio encoder (AAC)
            setupAudioEncoder()

            // Setup muxer (MP4)
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            videoTrackIndex = -1
            audioTrackIndex = -1
            muxerStarted = false

            // Create virtual display
            val metrics = getDisplayMetrics()
            val density = metrics.densityDpi

            virtualDisplay = mediaProjection!!.createVirtualDisplay(
                "XYRecorder",
                1280, // 720p width
                720,  // 720p height
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

    private fun setupVideoEncoder() {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
            android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 5_000_000) // 5 Mbps
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

        videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        videoEncoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoInputSurface = videoEncoder!!.createInputSurface()
        videoEncoder!!.start()
    }

    private fun setupAudioEncoder() {
        // Create AudioProcessor for mic
        audioProcessor = AudioProcessor(NoiseReductionSettings())

        // Mic AudioRecord
        val micBufferSize = AudioRecord.getMinBufferSize(
            48000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        micAudioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(48000)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build())
            .setBufferSizeInBytes(micBufferSize)
            .build()

        // Attach native noise suppression
        audioProcessor?.attachNativeEffects(micAudioRecord!!.audioSessionId)
        micAudioRecord?.startRecording()

        // Audio encoder (AAC)
        val audioFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            48000,
            1 // mono
        )
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 192_000)
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)

        audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        audioEncoder!!.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioEncoder!!.start()

        // Start capture thread
        startMicCaptureThread()
    }

    private fun startMicCaptureThread() {
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

    @Synchronized
    private fun writeAudioToEncoder(pcmData: ShortArray, dataSize: Int) {
        val encoder = audioEncoder ?: return
        try {
            val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = encoder.getInputBuffer(inputBufferIndex) ?: return
                inputBuffer.clear()
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
                delay(1)
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
                videoEncoder?.signalEndOfInputStream()
                delay(500)

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
