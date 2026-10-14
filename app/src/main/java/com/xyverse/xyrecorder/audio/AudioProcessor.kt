package com.xyverse.xyrecorder.audio

import android.content.Context
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log

/**
 * Audio processor using native Android effects
 * Uses hardware-accelerated effects for best quality
 */
class AudioProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioProcessor"
    }
    
    private var noiseSuppressor: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null
    private var aec: AcousticEchoCanceler? = null
    
    private var enabled = true
    private var sampleRate = 48000
    private var preset = "medium"
    
    /**
     * Initialize with sample rate
     */
    fun initialize(sampleRate: Int) {
        this.sampleRate = sampleRate
        Log.d(TAG, "AudioProcessor initialized at ${sampleRate}Hz")
    }
    
    /**
     * Set enabled state
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        Log.d(TAG, "Processing ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Set processing preset
     */
    fun setPreset(preset: String) {
        this.preset = preset
        Log.d(TAG, "Preset set to: $preset")
    }
    
    /**
     * Process audio buffer in place
     * Applies noise reduction based on current settings
     */
    fun process(buffer: FloatArray) {
        if (!enabled) return
        
        when (preset) {
            "light" -> processLight(buffer)
            "medium" -> processMedium(buffer)
            "heavy" -> processHeavy(buffer)
            "studio" -> processStudio(buffer)
        }
    }
    
    /**
     * Light processing - basic noise floor reduction
     */
    private fun processLight(buffer: FloatArray) {
        // Simple noise gate
        val threshold = 0.01f
        for (i in buffer.indices) {
            if (Math.abs(buffer[i]) < threshold) {
                buffer[i] *= 0.1f
            }
        }
    }
    
    /**
     * Medium processing - noise reduction + light compression
     */
    private fun processMedium(buffer: FloatArray) {
        // Noise gate
        val threshold = 0.015f
        for (i in buffer.indices) {
            if (Math.abs(buffer[i]) < threshold) {
                buffer[i] *= 0.05f
            }
        }
        
        // Light compression
        val compThreshold = 0.3f
        val ratio = 2.0f
        for (i in buffer.indices) {
            if (Math.abs(buffer[i]) > compThreshold) {
                val sign = if (buffer[i] >= 0) 1f else -1f
                val excess = Math.abs(buffer[i]) - compThreshold
                buffer[i] = sign * (compThreshold + excess / ratio)
            }
        }
    }
    
    /**
     * Heavy processing - aggressive noise reduction + compression
     */
    private fun processHeavy(buffer: FloatArray) {
        // Aggressive noise gate
        val threshold = 0.02f
        for (i in buffer.indices) {
            if (Math.abs(buffer[i]) < threshold) {
                buffer[i] *= 0.02f
            }
        }
        
        // Strong compression
        val compThreshold = 0.2f
        val ratio = 4.0f
        for (i in buffer.indices) {
            if (Math.abs(buffer[i]) > compThreshold) {
                val sign = if (buffer[i] >= 0) 1f else -1f
                val excess = Math.abs(buffer[i]) - compThreshold
                buffer[i] = sign * (compThreshold + excess / ratio)
            }
        }
        
        // Makeup gain
        for (i in buffer.indices) {
            buffer[i] *= 1.5f
        }
    }
    
    /**
     * Studio processing - professional quality
     */
    private fun processStudio(buffer: FloatArray) {
        // Very aggressive noise gate
        val threshold = 0.025f
        for (i in buffer.indices) {
            if (Math.abs(buffer[i]) < threshold) {
                buffer[i] *= 0.01f
            }
        }
        
        // Professional compression with knee
        val compThreshold = 0.15f
        val knee = 0.05f
        val ratio = 3.0f
        
        for (i in buffer.indices) {
            val abs = Math.abs(buffer[i])
            if (abs > compThreshold - knee) {
                val sign = if (buffer[i] >= 0) 1f else -1f
                val excess = abs - compThreshold
                if (excess > 0) {
                    buffer[i] = sign * (compThreshold + excess / ratio)
                }
            }
        }
        
        // Makeup gain
        for (i in buffer.indices) {
            buffer[i] *= 2.0f
        }
        
        // Soft limiting
        for (i in buffer.indices) {
            if (Math.abs(buffer[i]) > 0.95f) {
                buffer[i] = if (buffer[i] > 0) 0.95f else -0.95f
            }
        }
    }
    
    /**
     * Attach native audio effects to an audio session
     */
    fun attachNativeEffects(audioSessionId: Int) {
        try {
            // Try to use native Android audio effects
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(audioSessionId)
                noiseSuppressor?.setEnabled(true)
                Log.d(TAG, "NoiseSuppressor attached")
            }
            
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(audioSessionId)
                aec?.setEnabled(true)
                Log.d(TAG, "AEC attached")
            }
            
            // Don't use AGC - we handle gain ourselves
            Log.d(TAG, "Native effects initialized for session $audioSessionId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach native effects", e)
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        try {
            noiseSuppressor?.release()
            agc?.release()
            aec?.release()
            Log.d(TAG, "AudioProcessor released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioProcessor", e)
        }
    }
}
