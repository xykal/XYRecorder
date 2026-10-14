package com.xyverse.xyrecorder.data

/**
 * Recording configuration - video settings
 */
data class VideoSettings(
    val resolution: VideoResolution = VideoResolution.HD_720P,
    val fps: Int = 30,
    val bitrate: Int = 5_000_000, // 5 Mbps
    val codec: VideoCodec = VideoCodec.H264,
    val quality: RecordingQuality = RecordingQuality.HIGH
)

enum class VideoResolution(val width: Int, val height: Int, val label: String) {
    HD_720P(1280, 720, "720p"),
    FHD_1080P(1920, 1080, "1080p"),
    QHD_1440P(2560, 1440, "1440p"),
    UHD_4K(3840, 2160, "4K")
}

enum class VideoCodec(val label: String, val mimeType: String) {
    H264("H.264", "video/avc"),
    H265("H.265", "video/hevc"),
    VP8("VP8", "video/x-vnd.on2.vp8"),
    VP9("VP9", "video/x-vnd.on2.vp9")
}

enum class RecordingQuality(val crf: Int, val label: String) {
    LOW(32, "Low"),
    MEDIUM(28, "Medium"),
    HIGH(23, "High"),
    ULTRA(18, "Ultra")
}

/**
 * Audio settings - microphone + system audio
 */
data class AudioSettings(
    val microphoneEnabled: Boolean = true,
    val systemAudioEnabled: Boolean = true,
    val microphoneVolume: Float = 1.0f,     // 0.0 - 2.0
    val systemAudioVolume: Float = 0.8f,    // 0.0 - 2.0
    val sampleRate: Int = 48000,
    val bitDepth: Int = 16,
    val channels: Int = 1,                  // mono for mic
    val outputBitrate: Int = 192_000        // AAC output
)

/**
 * Studio-quality noise reduction / audio processing chain
 */
data class NoiseReductionSettings(
    val enabled: Boolean = true,
    val preset: NoiseReductionPreset = NoiseReductionPreset.MEDIUM,

    // High-pass filter (removes low frequency rumble)
    val highPassEnabled: Boolean = true,
    val highPassFrequency: Int = 80,        // Hz

    // Noise gate (silences below threshold)
    val noiseGateEnabled: Boolean = true,
    val noiseGateThresholdDb: Float = -45f, // dBFS
    val noiseGateAttackMs: Float = 5f,
    val noiseGateReleaseMs: Float = 100f,
    val noiseGateHoldMs: Float = 50f,

    // Noise suppression (spectral)
    val noiseSuppressionEnabled: Boolean = true,
    val noiseSuppressionStrength: Float = 0.6f, // 0.0 - 1.0

    // Compressor (evens out levels)
    val compressorEnabled: Boolean = true,
    val compressorThresholdDb: Float = -24f,
    val compressorRatio: Float = 4f,        // 4:1
    val compressorAttackMs: Float = 10f,
    val compressorReleaseMs: Float = 100f,
    val compressorMakeupGainDb: Float = 6f,
    val compressorKneeDb: Float = 6f,

    // De-esser (reduces sibilance)
    val deEsserEnabled: Boolean = true,
    val deEsserFrequency: Int = 6000,       // Hz
    val deEsserThresholdDb: Float = -20f,
    val deEsserRatio: Float = 4f,

    // Limiter (prevents clipping)
    val limiterEnabled: Boolean = true,
    val limiterCeilingDb: Float = -1f,
    val limiterReleaseMs: Float = 50f,

    // Output normalization
    val normalizeOutput: Boolean = true,
    val targetLoudnessLufs: Float = -16f    // LUFS (broadcast standard)
)

enum class NoiseReductionPreset(val label: String, val description: String) {
    NONE("None", "No processing"),
    LIGHT("Light", "Basic cleanup"),
    MEDIUM("Medium", "Balanced (default)"),
    HEAVY("Heavy", "Aggressive noise removal"),
    STUDIO("Studio", "Broadcast-quality chain"),
    CUSTOM("Custom", "User-defined")
}

fun NoiseReductionPreset.toSettings(): NoiseReductionSettings = when (this) {
    NoiseReductionPreset.NONE -> NoiseReductionSettings(
        enabled = false,
        highPassEnabled = false,
        noiseGateEnabled = false,
        noiseSuppressionEnabled = false,
        compressorEnabled = false,
        deEsserEnabled = false,
        limiterEnabled = false
    )
    NoiseReductionPreset.LIGHT -> NoiseReductionSettings(
        enabled = true,
        highPassEnabled = true,
        highPassFrequency = 80,
        noiseGateEnabled = true,
        noiseGateThresholdDb = -50f,
        noiseSuppressionEnabled = true,
        noiseSuppressionStrength = 0.3f,
        compressorEnabled = false,
        deEsserEnabled = false,
        limiterEnabled = true,
        limiterCeilingDb = -1f
    )
    NoiseReductionPreset.MEDIUM -> NoiseReductionSettings(
        enabled = true,
        highPassEnabled = true,
        highPassFrequency = 80,
        noiseGateEnabled = true,
        noiseGateThresholdDb = -45f,
        noiseSuppressionEnabled = true,
        noiseSuppressionStrength = 0.6f,
        compressorEnabled = true,
        compressorThresholdDb = -24f,
        compressorRatio = 4f,
        deEsserEnabled = false,
        limiterEnabled = true,
        limiterCeilingDb = -1f
    )
    NoiseReductionPreset.HEAVY -> NoiseReductionSettings(
        enabled = true,
        highPassEnabled = true,
        highPassFrequency = 120,
        noiseGateEnabled = true,
        noiseGateThresholdDb = -35f,
        noiseGateAttackMs = 3f,
        noiseGateReleaseMs = 80f,
        noiseSuppressionEnabled = true,
        noiseSuppressionStrength = 0.8f,
        compressorEnabled = true,
        compressorThresholdDb = -20f,
        compressorRatio = 6f,
        compressorMakeupGainDb = 8f,
        deEsserEnabled = true,
        limiterEnabled = true,
        limiterCeilingDb = -1f
    )
    NoiseReductionPreset.STUDIO -> NoiseReductionSettings(
        enabled = true,
        highPassEnabled = true,
        highPassFrequency = 100,
        noiseGateEnabled = true,
        noiseGateThresholdDb = -40f,
        noiseGateAttackMs = 2f,
        noiseGateReleaseMs = 150f,
        noiseGateHoldMs = 40f,
        noiseSuppressionEnabled = true,
        noiseSuppressionStrength = 0.9f,
        compressorEnabled = true,
        compressorThresholdDb = -18f,
        compressorRatio = 3f,
        compressorAttackMs = 15f,
        compressorReleaseMs = 150f,
        compressorMakeupGainDb = 6f,
        compressorKneeDb = 4f,
        deEsserEnabled = true,
        deEsserFrequency = 7000,
        deEsserThresholdDb = -22f,
        limiterEnabled = true,
        limiterCeilingDb = -0.5f,
        limiterReleaseMs = 60f,
        normalizeOutput = true,
        targetLoudnessLufs = -16f
    )
    NoiseReductionPreset.CUSTOM -> NoiseReductionSettings()
}

/**
 * Complete app settings
 */
data class AppSettings(
    val video: VideoSettings = VideoSettings(),
    val audio: AudioSettings = AudioSettings(),
    val noiseReduction: NoiseReductionSettings = NoiseReductionSettings(),
    val showRecordingIndicator: Boolean = true,
    val stopOnScreenOff: Boolean = true,
    val countDownBeforeStart: Int = 3,
    val saveToGallery: Boolean = true
)

/**
 * Recording state exposed to UI
 */
data class RecordingStatus(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val durationMs: Long = 0L,
    val currentOutputPath: String? = null,
    val micLevelDb: Float = -60f,
    val systemLevelDb: Float = -60f,
    val processedLevelDb: Float = -60f,
    val error: String? = null
)
