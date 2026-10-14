package com.xyverse.xyrecorder.ui.screens

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xyverse.xyrecorder.XYRecorderApp
import com.xyverse.xyrecorder.data.*
import com.xyverse.xyrecorder.service.RecordingService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel managing the recording state and settings.
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = (app as XYRecorderApp).settingsRepository

    // Settings flow
    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    // Recording status
    private val _status = MutableStateFlow(RecordingStatus())
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()

    init {
        // Monitor recording service for status updates
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(100)
                RecordingService.instance?.let { service ->
                    val recorder = service.recorder
                    _status.update {
                        it.copy(
                            isRecording = recorder.isRecording,
                            isPaused = recorder.isPaused,
                            durationMs = recorder.durationMs,
                            currentOutputPath = recorder.currentOutputPath
                        )
                    }
                }
            }
        }
    }

    fun startRecording() {
        // Recording is started via MainActivity's screen capture flow
        // This is just a placeholder - actual start happens through service
    }

    fun stopRecording() {
        val context = getApplication<Application>()
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        context.startService(intent)
        _status.update { it.copy(isRecording = false, isPaused = false) }
    }

    fun pauseRecording() {
        val context = getApplication<Application>()
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_PAUSE
        }
        context.startService(intent)
        _status.update { it.copy(isPaused = true) }
    }

    fun resumeRecording() {
        val context = getApplication<Application>()
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_RESUME
        }
        context.startService(intent)
        _status.update { it.copy(isPaused = false) }
    }

    fun updateVideoSettings(block: VideoSettings.() -> VideoSettings) {
        viewModelScope.launch {
            settingsRepo.updateVideo(block)
        }
    }

    fun updateAudioSettings(block: AudioSettings.() -> AudioSettings) {
        viewModelScope.launch {
            settingsRepo.updateAudio(block)
        }
    }

    fun updateNoiseReduction(block: NoiseReductionSettings.() -> NoiseReductionSettings) {
        viewModelScope.launch {
            settingsRepo.updateNoiseReduction(block)
        }
    }

    fun applyNoisePreset(preset: NoiseReductionPreset) {
        viewModelScope.launch {
            val newSettings = preset.toSettings()
            settingsRepo.updateNoiseReduction { newSettings }
        }
    }

    fun updateGeneral(
        showIndicator: Boolean? = null,
        stopOnScreenOff: Boolean? = null,
        countdown: Int? = null,
        saveToGallery: Boolean? = null
    ) {
        viewModelScope.launch {
            settingsRepo.updateGeneral(showIndicator, stopOnScreenOff, countdown, saveToGallery)
        }
    }

    fun clearError() {
        _status.update { it.copy(error = null) }
    }
}
