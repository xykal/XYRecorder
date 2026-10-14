package com.xyverse.xyrecorder.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*

/**
 * Professional audio recorder with real-time processing
 * Outputs AAC/M4A format (playable on all devices)
 */
class AudioRecorder(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 48000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BIT_RATE = 128000 // 128 kbps AAC
    }
    
    private var audioRecord: AudioRecord? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var outputFile: File? = null
    private var processor: AudioProcessor? = null
    
    private var muxerStarted = false
    private var trackIndex = -1
    
    var onAudioLevelUpdate: ((Float) -> Unit)? = null
    var onRecordingComplete: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    /**
     * Start recording with processing
     */
    fun startRecording() {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }
        
        // Check permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            onError?.invoke("Microphone permission not granted")
            return
        }
        
        try {
            // Initialize processor
            processor = AudioProcessor(context).apply {
                initialize(SAMPLE_RATE)
            }
            
            // Create output file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "XYRecorder"
            )
            if (!outputDir.exists()) outputDir.mkdirs()
            
            outputFile = File(outputDir, "XYRec_$timestamp.m4a")
            
            // Setup MediaCodec for AAC encoding
            val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 1)
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)
            
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            mediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            
            // Setup MediaMuxer
            mediaMuxer = MediaMuxer(outputFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            // Get minimum buffer size
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                onError?.invoke("Invalid buffer size")
                return
            }
            
            // Create AudioRecord
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError?.invoke("Failed to initialize AudioRecord")
                return
            }
            
            // Attach native effects if available
            processor?.attachNativeEffects(audioRecord!!.audioSessionId)
            
            // Start everything
            mediaCodec?.start()
            audioRecord?.startRecording()
            isRecording = true
            
            // Start recording thread
            recordingThread = Thread {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
                recordAudio(bufferSize)
            }.apply {
                start()
            }
            
            Log.d(TAG, "Recording started: ${outputFile?.name}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            onError?.invoke("Error: ${e.message}")
            stopRecording()
        }
    }
    
    /**
     * Record audio in background thread
     */
    private fun recordAudio(bufferSize: Int) {
        val buffer = ShortArray(bufferSize / 2)
        val processedBuffer = FloatArray(buffer.size)
        val outputBuffer = ShortArray(buffer.size)
        
        val bufferInfo = MediaCodec.BufferInfo()
        
        try {
            while (isRecording) {
                val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                
                if (readCount > 0) {
                    // Convert to float
                    for (i in 0 until readCount) {
                        processedBuffer[i] = buffer[i] / 32768.0f
                    }
                    
                    // Process audio
                    processor?.process(processedBuffer)
                    
                    // Calculate RMS level
                    var sum = 0.0
                    for (i in 0 until readCount) {
                        sum += processedBuffer[i] * processedBuffer[i]
                    }
                    val rms = Math.sqrt(sum / readCount).toFloat()
                    val db = 20 * Math.log10(rms.toDouble()).toFloat()
                    onAudioLevelUpdate?.invoke(db)
                    
                    // Convert back to PCM
                    for (i in 0 until readCount) {
                        outputBuffer[i] = (processedBuffer[i] * 32767.0f).toInt().toShort()
                    }
                    
                    // Convert to ByteBuffer for MediaCodec
                    val byteBuffer = ByteBuffer.allocate(readCount * 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until readCount) {
                        byteBuffer.putShort(outputBuffer[i])
                    }
                    byteBuffer.flip()
                    
                    // Feed to encoder
                    encodeAndMux(byteBuffer, bufferInfo, false)
                }
            }
            
            // Signal end of stream
            encodeAndMux(null, bufferInfo, true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during recording", e)
            onError?.invoke("Recording error: ${e.message}")
        } finally {
            try {
                // Release resources
                mediaCodec?.stop()
                mediaCodec?.release()
                mediaMuxer?.stop()
                mediaMuxer?.release()
                audioRecord?.stop()
                audioRecord?.release()
                processor?.release()
                
                Log.d(TAG, "Recording saved: ${outputFile?.absolutePath}")
                onRecordingComplete?.invoke(outputFile!!)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error finalizing recording", e)
                onError?.invoke("Save error: ${e.message}")
            } finally {
                mediaCodec = null
                mediaMuxer = null
                audioRecord = null
                processor = null
                muxerStarted = false
                trackIndex = -1
            }
        }
    }
    
    /**
     * Encode PCM to AAC and mux to M4A
     */
    private fun encodeAndMux(inputBuffer: ByteBuffer?, bufferInfo: MediaCodec.BufferInfo, endOfStream: Boolean) {
        if (!endOfStream) {
            // Queue input
            val inputBufferIndex = mediaCodec?.dequeueInputBuffer(10000) ?: return
            if (inputBufferIndex >= 0) {
                val inputBuffer = mediaCodec?.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(inputBuffer)
                mediaCodec?.queueInputBuffer(inputBufferIndex, 0, inputBuffer?.limit() ?: 0, 0, 0)
            }
        } else {
            // Signal end of stream
            val inputBufferIndex = mediaCodec?.dequeueInputBuffer(10000) ?: return
            if (inputBufferIndex >= 0) {
                mediaCodec?.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
        }
        
        // Dequeue output
        while (true) {
            val outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 10000) ?: return
            
            if (outputBufferIndex >= 0) {
                val outputBuffer = mediaCodec?.getOutputBuffer(outputBufferIndex)
                val outputFormat = mediaCodec?.getOutputFormat(outputBufferIndex)
                
                // Add track if not done yet
                if (!muxerStarted && outputFormat != null) {
                    trackIndex = mediaMuxer?.addTrack(outputFormat) ?: -1
                    mediaMuxer?.start()
                    muxerStarted = true
                }
                
                // Write to muxer
                if (muxerStarted && outputBuffer != null && bufferInfo.size > 0) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    mediaMuxer?.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                }
                
                mediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
                
                // Check if end of stream
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Format changed, will add track on next buffer
                continue
            } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            }
        }
    }
    
    /**
     * Stop recording
     */
    fun stopRecording() {
        if (!isRecording) return
        
        isRecording = false
        
        try {
            recordingThread?.join(2000)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        } finally {
            recordingThread = null
        }
        
        Log.d(TAG, "Recording stopped")
    }
    
    fun isRecording(): Boolean = isRecording
    
    fun getOutputFile(): File? = outputFile
    
    /**
     * Update noise reduction settings
     */
    fun updateNoiseSettings(enabled: Boolean) {
        processor?.setEnabled(enabled)
    }
    
    /**
     * Update processing preset
     */
    fun updatePreset(preset: String) {
        processor?.setPreset(preset)
    }
}
