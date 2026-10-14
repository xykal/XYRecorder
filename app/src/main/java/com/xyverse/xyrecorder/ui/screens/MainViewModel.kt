package com.xyverse.xyrecorder.ui.screens

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xyverse.xyrecorder.XYRecorderApp
import com.xyverse.xyrecorder.data.*
import com.xyverse.xyrecorder.recorder.ScreenRecorder
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

    // MediaProjection data
    var projectionResultCode: Int = -1
        private set
    var projectionData: Intent? = null
        private set

    fun setProjectionData(resultCode: Int, data: Intent) {
        projectionResultCode = resultCode
        projectionData = data
    }

    fun startRecording() {
        val data = projectionData ?: return
        val s = settings.value

        _status.update { it.copy(isRecording = true, isPaused = false, durationMs = 0L) }

        // Start foreground service
        val serviceIntent = Intent(getApplication(), RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_RESULT_CODE, projectionResultCode)
            putExtra(RecordingService.EXTRA_RESULT_DATA, data)
        }
        getApplication<Application>().startForegroundService(serviceIntent)

        // Get the recorder from service and wire up callbacks
        val recorder = RecordingService.instance?.getRecorder() ?: return
        recorder.onDurationUpdate = { ms ->
            _status.update { it.copy(durationMs = ms) }
        }
        recorder.onLevelsUpdate = { micDb, processedDb ->
            _status.update { it.copy(micLevelDb = micDb, processedLevelDb = processedDb) }
        }
        recorder.onComplete = { path ->
            _status.update { it.copy(isRecording = false, currentOutputPath = path) }
        }
        recorder.onError = { error ->
            _status.update { it.copy(error = error) }
        }
    }

    fun stopRecording() {
        RecordingService.instance?.let { service ->
            val intent = Intent(getApplication(), RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
            getApplication<Application>().startService(intent)
            service.getRecorder().stopRecording()
        }
        _status.update { it.copy(isRecording = false, isPaused = false) }
    }

    fun pauseRecording() {
        RecordingService.instance?.let { service ->
            val intent = Intent(getApplication(), RecordingService::class.java).apply {
                action = RecordingService.ACTION_PAUSE
            }
            getApplication<Application>().startService(intent)
        }
        _status.update { it.copy(isPaused = true) }
    }

    fun resumeRecording() {
        RecordingService.instance?.let { service ->
            val intent = Intent(getApplication(), RecordingService::class.java).apply {
                action = RecordingService.ACTION_RESUME
            }
            getApplication<Application>().startService(intent)
        }
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
