package com.xyverse.xyrecorder.data

/**
 * App settings for audio recorder
 */
data class AppSettings(
    val noiseReduction: NoiseReductionSettings = NoiseReductionSettings()
)

/**
 * Noise reduction settings
 */
data class NoiseReductionSettings(
    val enabled: Boolean = true,
    val preset: String = "medium"
)

/**
 * Recording status
 */
data class RecordingStatus(
    val isRecording: Boolean = false,
    val micLevelDb: Float = -60f,
    val currentOutputPath: String? = null,
    val error: String? = null
)
