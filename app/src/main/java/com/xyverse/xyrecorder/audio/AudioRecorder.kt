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
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*

/**
 * Professional audio recorder with real-time processing
 */
class AudioRecorder(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 48000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var outputFile: File? = null
    private var processor: AudioProcessor? = null
    
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
            
            // Create output file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "XYRecorder"
            )
            if (!outputDir.exists()) outputDir.mkdirs()
            
            outputFile = File(outputDir, "XYRec_$timestamp.wav")
            
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
     * Record audio in background thread
     */
    private fun recordAudio(bufferSize: Int) {
        val buffer = ShortArray(bufferSize / 2)
        val processedBuffer = FloatArray(buffer.size)
        val outputBuffer = ShortArray(buffer.size)
        
        val outputStream = try {
            FileOutputStream(outputFile)
        } catch (e: Exception) {
            onError?.invoke("Cannot create output file")
            return
        }
        
        // Write WAV header placeholder (will be updated later)
        outputStream.write(ByteArray(44))
        
        var totalBytesWritten = 0
        
        try {
            while (isRecording) {
                val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                
                if (readCount > 0) {
                    // Convert to float
                    for (i in 0 until readCount) {
                        processedBuffer[i] = buffer[i] / 32768.0f
                    }
                    
                    // Process audio
                    processor?.process(processedBuffer, readCount)
                    
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
                    
                    // Write to file
                    val byteBuffer = ByteBuffer.allocate(readCount * 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until readCount) {
                        byteBuffer.putShort(outputBuffer[i])
                    }
                    
                    outputStream.write(byteBuffer.array())
                    totalBytesWritten += byteBuffer.array().size
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during recording", e)
            onError?.invoke("Recording error: ${e.message}")
        } finally {
            try {
                outputStream.close()
                // Update WAV header with actual size
                updateWavHeader(outputFile!!, totalBytesWritten)
                Log.d(TAG, "Recording saved: ${outputFile?.absolutePath}")
                onRecordingComplete?.invoke(outputFile!!)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving file", e)
                onError?.invoke("Save error: ${e.message}")
            }
        }
    }
    
    /**
     * Update WAV header with actual file size
     */
    private fun updateWavHeader(file: File, audioDataSize: Int) {
        val randomAccessFile = RandomAccessFile(file, "rw")
        
        try {
            // RIFF header
            randomAccessFile.seek(4)
            randomAccessFile.write(intToByteArray(36 + audioDataSize))
            
            // fmt chunk
            randomAccessFile.seek(24)
            randomAccessFile.write(intToByteArray(SAMPLE_RATE))
            
            randomAccessFile.seek(28)
            randomAccessFile.write(intToByteArray(SAMPLE_RATE * 2)) // byte rate
            
            randomAccessFile.seek(32)
            randomAccessFile.write(shortToByteArray(2)) // block align
            
            randomAccessFile.seek(34)
            randomAccessFile.write(shortToByteArray(16)) // bits per sample
            
            // data chunk
            randomAccessFile.seek(40)
            randomAccessFile.write(intToByteArray(audioDataSize))
            
        } finally {
            randomAccessFile.close()
        }
    }
    
    private fun intToByteArray(value: Int): ByteArray {
        val bytes = ByteArray(4)
        bytes[0] = (value and 0xff).toByte()
        bytes[1] = ((value shr 8) and 0xff).toByte()
        bytes[2] = ((value shr 16) and 0xff).toByte()
        bytes[3] = ((value shr 24) and 0xff).toByte()
        return bytes
    }
    
    private fun shortToByteArray(value: Int): ByteArray {
        val bytes = ByteArray(2)
        bytes[0] = (value and 0xff).toByte()
        bytes[1] = ((value shr 8) and 0xff).toByte()
        return bytes
    }
    
    /**
     * Stop recording
     */
    fun stopRecording() {
        if (!isRecording) return
        
        isRecording = false
        
        try {
            recordingThread?.join(1000)
            audioRecord?.stop()
            audioRecord?.release()
            processor?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        } finally {
            audioRecord = null
            processor = null
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
}
