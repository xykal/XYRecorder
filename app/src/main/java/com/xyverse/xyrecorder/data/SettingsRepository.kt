package com.xyverse.xyrecorder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.xyverse.xyrecorder.data.*

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xyrecorder_settings")

class SettingsRepository(private val context: Context) {

    private val ds = context.dataStore

    // Keys
    private object Keys {
        val RESOLUTION = stringPreferencesKey("video_resolution")
        val FPS = intPreferencesKey("video_fps")
        val BITRATE = intPreferencesKey("video_bitrate")
        val CODEC = stringPreferencesKey("video_codec")
        val QUALITY = stringPreferencesKey("video_quality")

        val MIC_ENABLED = booleanPreferencesKey("mic_enabled")
        val SYS_AUDIO_ENABLED = booleanPreferencesKey("sys_audio_enabled")
        val MIC_VOLUME = floatPreferencesKey("mic_volume")
        val SYS_VOLUME = floatPreferencesKey("sys_volume")

        val NR_ENABLED = booleanPreferencesKey("nr_enabled")
        val NR_PRESET = stringPreferencesKey("nr_preset")
        val NR_HPF_ENABLED = booleanPreferencesKey("nr_hpf_enabled")
        val NR_HPF_FREQ = intPreferencesKey("nr_hpf_freq")
        val NR_GATE_ENABLED = booleanPreferencesKey("nr_gate_enabled")
        val NR_GATE_THRESH = floatPreferencesKey("nr_gate_thresh")
        val NR_SUP_ENABLED = booleanPreferencesKey("nr_sup_enabled")
        val NR_SUP_STRENGTH = floatPreferencesKey("nr_sup_strength")
        val NR_COMP_ENABLED = booleanPreferencesKey("nr_comp_enabled")
        val NR_COMP_THRESH = floatPreferencesKey("nr_comp_thresh")
        val NR_COMP_RATIO = floatPreferencesKey("nr_comp_ratio")
        val NR_COMP_MAKEUP = floatPreferencesKey("nr_comp_makeup")
        val NR_DEESS_ENABLED = booleanPreferencesKey("nr_deess_enabled")
        val NR_LIMIT_ENABLED = booleanPreferencesKey("nr_limit_enabled")
        val NR_LIMIT_CEIL = floatPreferencesKey("nr_limit_ceil")

        val SHOW_INDICATOR = booleanPreferencesKey("show_indicator")
        val STOP_ON_SCREEN_OFF = booleanPreferencesKey("stop_on_screen_off")
        val COUNTDOWN = intPreferencesKey("countdown")
        val SAVE_TO_GALLERY = booleanPreferencesKey("save_to_gallery")
    }

    val settingsFlow: Flow<AppSettings> = ds.data.map { prefs ->
        AppSettings(
            video = VideoSettings(
                resolution = prefs[Keys.RESOLUTION]?.let {
                    runCatching { VideoResolution.valueOf(it) }.getOrNull()
                } ?: VideoResolution.HD_720P,
                fps = prefs[Keys.FPS] ?: 30,
                bitrate = prefs[Keys.BITRATE] ?: 5_000_000,
                codec = prefs[Keys.CODEC]?.let {
                    runCatching { VideoCodec.valueOf(it) }.getOrNull()
                } ?: VideoCodec.H264,
                quality = prefs[Keys.QUALITY]?.let {
                    runCatching { RecordingQuality.valueOf(it) }.getOrNull()
                } ?: RecordingQuality.HIGH
            ),
            audio = AudioSettings(
                microphoneEnabled = prefs[Keys.MIC_ENABLED] ?: true,
                systemAudioEnabled = prefs[Keys.SYS_AUDIO_ENABLED] ?: true,
                microphoneVolume = prefs[Keys.MIC_VOLUME] ?: 1.0f,
                systemAudioVolume = prefs[Keys.SYS_VOLUME] ?: 0.8f
            ),
            noiseReduction = NoiseReductionSettings(
                enabled = prefs[Keys.NR_ENABLED] ?: true,
                preset = prefs[Keys.NR_PRESET]?.let {
                    runCatching { NoiseReductionPreset.valueOf(it) }.getOrNull()
                } ?: NoiseReductionPreset.MEDIUM,
                highPassEnabled = prefs[Keys.NR_HPF_ENABLED] ?: true,
                highPassFrequency = prefs[Keys.NR_HPF_FREQ] ?: 80,
                noiseGateEnabled = prefs[Keys.NR_GATE_ENABLED] ?: true,
                noiseGateThresholdDb = prefs[Keys.NR_GATE_THRESH] ?: -45f,
                noiseSuppressionEnabled = prefs[Keys.NR_SUP_ENABLED] ?: true,
                noiseSuppressionStrength = prefs[Keys.NR_SUP_STRENGTH] ?: 0.6f,
                compressorEnabled = prefs[Keys.NR_COMP_ENABLED] ?: true,
                compressorThresholdDb = prefs[Keys.NR_COMP_THRESH] ?: -24f,
                compressorRatio = prefs[Keys.NR_COMP_RATIO] ?: 4f,
                compressorMakeupGainDb = prefs[Keys.NR_COMP_MAKEUP] ?: 6f,
                deEsserEnabled = prefs[Keys.NR_DEESS_ENABLED] ?: true,
                limiterEnabled = prefs[Keys.NR_LIMIT_ENABLED] ?: true,
                limiterCeilingDb = prefs[Keys.NR_LIMIT_CEIL] ?: -1f
            ),
            showRecordingIndicator = prefs[Keys.SHOW_INDICATOR] ?: true,
            stopOnScreenOff = prefs[Keys.STOP_ON_SCREEN_OFF] ?: true,
            countDownBeforeStart = prefs[Keys.COUNTDOWN] ?: 3,
            saveToGallery = prefs[Keys.SAVE_TO_GALLERY] ?: true
        )
    }

    suspend fun updateVideo(block: VideoSettings.() -> VideoSettings) {
        ds.edit { prefs ->
            val current = getCurrent(prefs).video.block()
            prefs[Keys.RESOLUTION] = current.resolution.name
            prefs[Keys.FPS] = current.fps
            prefs[Keys.BITRATE] = current.bitrate
            prefs[Keys.CODEC] = current.codec.name
            prefs[Keys.QUALITY] = current.quality.name
        }
    }

    suspend fun updateAudio(block: AudioSettings.() -> AudioSettings) {
        ds.edit { prefs ->
            val current = getCurrent(prefs).audio.block()
            prefs[Keys.MIC_ENABLED] = current.microphoneEnabled
            prefs[Keys.SYS_AUDIO_ENABLED] = current.systemAudioEnabled
            prefs[Keys.MIC_VOLUME] = current.microphoneVolume
            prefs[Keys.SYS_VOLUME] = current.systemAudioVolume
        }
    }

    suspend fun updateNoiseReduction(block: NoiseReductionSettings.() -> NoiseReductionSettings) {
        ds.edit { prefs ->
            val current = getCurrent(prefs).noiseReduction.block()
            prefs[Keys.NR_ENABLED] = current.enabled
            prefs[Keys.NR_PRESET] = current.preset.name
            prefs[Keys.NR_HPF_ENABLED] = current.highPassEnabled
            prefs[Keys.NR_HPF_FREQ] = current.highPassFrequency
            prefs[Keys.NR_GATE_ENABLED] = current.noiseGateEnabled
            prefs[Keys.NR_GATE_THRESH] = current.noiseGateThresholdDb
            prefs[Keys.NR_SUP_ENABLED] = current.noiseSuppressionEnabled
            prefs[Keys.NR_SUP_STRENGTH] = current.noiseSuppressionStrength
            prefs[Keys.NR_COMP_ENABLED] = current.compressorEnabled
            prefs[Keys.NR_COMP_THRESH] = current.compressorThresholdDb
            prefs[Keys.NR_COMP_RATIO] = current.compressorRatio
            prefs[Keys.NR_COMP_MAKEUP] = current.compressorMakeupGainDb
            prefs[Keys.NR_DEESS_ENABLED] = current.deEsserEnabled
            prefs[Keys.NR_LIMIT_ENABLED] = current.limiterEnabled
            prefs[Keys.NR_LIMIT_CEIL] = current.limiterCeilingDb
        }
    }

    suspend fun updateGeneral(
        showIndicator: Boolean? = null,
        stopOnScreenOff: Boolean? = null,
        countdown: Int? = null,
        saveToGallery: Boolean? = null
    ) {
        ds.edit { prefs ->
            showIndicator?.let { prefs[Keys.SHOW_INDICATOR] = it }
            stopOnScreenOff?.let { prefs[Keys.STOP_ON_SCREEN_OFF] = it }
            countdown?.let { prefs[Keys.COUNTDOWN] = it }
            saveToGallery?.let { prefs[Keys.SAVE_TO_GALLERY] = it }
        }
    }

    private fun getCurrent(prefs: Preferences): AppSettings = AppSettings(
        video = VideoSettings(
            resolution = prefs[Keys.RESOLUTION]?.let {
                runCatching { VideoResolution.valueOf(it) }.getOrNull()
            } ?: VideoResolution.HD_720P,
            fps = prefs[Keys.FPS] ?: 30,
            bitrate = prefs[Keys.BITRATE] ?: 5_000_000,
            codec = prefs[Keys.CODEC]?.let {
                runCatching { VideoCodec.valueOf(it) }.getOrNull()
            } ?: VideoCodec.H264,
            quality = prefs[Keys.QUALITY]?.let {
                runCatching { RecordingQuality.valueOf(it) }.getOrNull()
            } ?: RecordingQuality.HIGH
        ),
        audio = AudioSettings(
            microphoneEnabled = prefs[Keys.MIC_ENABLED] ?: true,
            systemAudioEnabled = prefs[Keys.SYS_AUDIO_ENABLED] ?: true,
            microphoneVolume = prefs[Keys.MIC_VOLUME] ?: 1.0f,
            systemAudioVolume = prefs[Keys.SYS_VOLUME] ?: 0.8f
        ),
        noiseReduction = NoiseReductionSettings(
            enabled = prefs[Keys.NR_ENABLED] ?: true,
            preset = prefs[Keys.NR_PRESET]?.let {
                runCatching { NoiseReductionPreset.valueOf(it) }.getOrNull()
            } ?: NoiseReductionPreset.MEDIUM,
            highPassEnabled = prefs[Keys.NR_HPF_ENABLED] ?: true,
            highPassFrequency = prefs[Keys.NR_HPF_FREQ] ?: 80,
            noiseGateEnabled = prefs[Keys.NR_GATE_ENABLED] ?: true,
            noiseGateThresholdDb = prefs[Keys.NR_GATE_THRESH] ?: -45f,
            noiseSuppressionEnabled = prefs[Keys.NR_SUP_ENABLED] ?: true,
            noiseSuppressionStrength = prefs[Keys.NR_SUP_STRENGTH] ?: 0.6f,
            compressorEnabled = prefs[Keys.NR_COMP_ENABLED] ?: true,
            compressorThresholdDb = prefs[Keys.NR_COMP_THRESH] ?: -24f,
            compressorRatio = prefs[Keys.NR_COMP_RATIO] ?: 4f,
            compressorMakeupGainDb = prefs[Keys.NR_COMP_MAKEUP] ?: 6f,
            deEsserEnabled = prefs[Keys.NR_DEESS_ENABLED] ?: true,
            limiterEnabled = prefs[Keys.NR_LIMIT_ENABLED] ?: true,
            limiterCeilingDb = prefs[Keys.NR_LIMIT_CEIL] ?: -1f
        )
    )
}
