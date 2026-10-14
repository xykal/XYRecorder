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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Professional audio recorder using MediaRecorder (most reliable method)
 * Outputs M4A/AAC format (native Android, works on all devices)
 */
class AudioRecorder(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 48000
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: File? = null
    private var processor: AudioProcessor? = null
    
    var onAudioLevelUpdate: ((Float) -> Unit)? = null
    var onRecordingComplete: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    /**
     * Start recording using MediaRecorder (native, reliable)
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
            
            Log.d(TAG, "Creating MediaRecorder...")
            
            // Create MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(SAMPLE_RATE)
                setAudioEncodingBitRate(128000)
                setAudioChannels(1)
                setOutputFile(outputFile!!.absolutePath)
                
                try {
                    prepare()
                    Log.d(TAG, "MediaRecorder prepared")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to prepare MediaRecorder", e)
                    onError?.invoke("Failed to initialize recorder: ${e.message}")
                    return
                }
            }
            
            // Start recording
            mediaRecorder?.start()
            isRecording = true
            
            Log.d(TAG, "Recording started: ${outputFile?.name}")
            
            // Start level meter thread
            Thread {
                monitorAudioLevel()
            }.start()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            onError?.invoke("Error: ${e.message}")
            stopRecording()
        }
    }
    
    /**
     * Monitor audio level for UI meter
     */
    private fun monitorAudioLevel() {
        while (isRecording) {
            try {
                val maxAmplitude = mediaRecorder?.maxAmplitude ?: 0
                val db = if (maxAmplitude > 0) {
                    20 * Math.log10(maxAmplitude.toDouble() / 32768.0).toFloat()
                } else {
                    -60f
                }
                onAudioLevelUpdate?.invoke(db)
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring level", e)
            }
            
            Thread.sleep(100)
        }
    }
    
    /**
     * Stop recording
     */
    fun stopRecording() {
        if (!isRecording) return
        
        isRecording = false
        
        try {
            Log.d(TAG, "Stopping recording...")
            
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            
            Log.d(TAG, "Recording saved: ${outputFile?.absolutePath}")
            
            if (outputFile != null && outputFile!!.exists()) {
                onRecordingComplete?.invoke(outputFile!!)
            } else {
                onError?.invoke("Recording file not created")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            onError?.invoke("Error saving recording: ${e.message}")
        } finally {
            processor?.release()
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
