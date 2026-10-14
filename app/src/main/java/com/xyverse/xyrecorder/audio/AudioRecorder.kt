package com.xyverse.xyrecorder.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Professional audio recorder with MP3 output using LAME encoder
 */
class AudioRecorder(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 44100  // Standard MP3 sample rate
        const val BIT_RATE = 128       // 128 kbps
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var outputFile: File? = null
    private var processor: AudioProcessor? = null
    private var mp3Encoder: MP3Encoder? = null
    
    var onAudioLevelUpdate: ((Float) -> Unit)? = null
    var onRecordingComplete: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    /**
     * Start recording with MP3 output
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
            
            // Initialize LAME MP3 encoder
            mp3Encoder = MP3Encoder().apply {
                init(SAMPLE_RATE, 1, BIT_RATE)  // Mono, 128kbps
            }
            
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
            
            // Create output file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "XYRecorder"
            )
            if (!outputDir.exists()) outputDir.mkdirs()
            
            outputFile = File(outputDir, "XYRec_$timestamp.mp3")
            
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
            
            // Start recording
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
     * Record audio in background thread and encode to MP3
     */
    private fun recordAudio(bufferSize: Int) {
        val pcmBuffer = ShortArray(bufferSize / 2)
        val processedBuffer = FloatArray(pcmBuffer.size)
        val mp3BufferSize = mp3Encoder?.getBufferSize(pcmBuffer.size) ?: 8192
        val mp3Buffer = ByteArray(mp3BufferSize)
        
        val outputStream = try {
            FileOutputStream(outputFile)
        } catch (e: Exception) {
            onError?.invoke("Cannot create output file: ${e.message}")
            return
        }
        
        try {
            while (isRecording) {
                val readCount = audioRecord?.read(pcmBuffer, 0, pcmBuffer.size) ?: -1
                
                if (readCount > 0) {
                    // Convert to float for processing
                    for (i in 0 until readCount) {
                        processedBuffer[i] = pcmBuffer[i] / 32768.0f
                    }
                    
                    // Apply audio processing (noise reduction, compression, etc.)
                    processor?.process(processedBuffer)
                    
                    // Calculate RMS level for UI meter
                    var sum = 0.0
                    for (i in 0 until readCount) {
                        sum += processedBuffer[i] * processedBuffer[i]
                    }
                    val rms = Math.sqrt(sum / readCount).toFloat()
                    val db = 20 * Math.log10(rms.toDouble()).toFloat()
                    onAudioLevelUpdate?.invoke(db)
                    
                    // Convert back to PCM shorts for MP3 encoding
                    for (i in 0 until readCount) {
                        pcmBuffer[i] = (processedBuffer[i] * 32767.0f).toInt().toShort()
                    }
                    
                    // Encode to MP3
                    val mp3Size = mp3Encoder?.encode(pcmBuffer, readCount, mp3Buffer) ?: -1
                    
                    if (mp3Size > 0) {
                        outputStream.write(mp3Buffer, 0, mp3Size)
                    } else if (mp3Size < 0) {
                        Log.e(TAG, "MP3 encoding failed")
                    }
                }
            }
            
            // Flush remaining MP3 data
            val remainingSize = mp3Encoder?.flush(mp3Buffer) ?: 0
            if (remainingSize > 0) {
                outputStream.write(mp3Buffer, 0, remainingSize)
            }
            
            outputStream.flush()
            Log.d(TAG, "Recording saved: ${outputFile?.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during recording", e)
            onError?.invoke("Recording error: ${e.message}")
        } finally {
            try {
                outputStream.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing file", e)
            }
            
            // Notify completion
            if (outputFile?.exists() == true && (outputFile?.length() ?: 0) > 0) {
                onRecordingComplete?.invoke(outputFile!!)
            } else {
                onError?.invoke("Recording file is empty or missing")
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
            audioRecord?.stop()
            audioRecord?.release()
            mp3Encoder?.close()
            processor?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        } finally {
            audioRecord = null
            recordingThread = null
            mp3Encoder = null
            processor = null
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
