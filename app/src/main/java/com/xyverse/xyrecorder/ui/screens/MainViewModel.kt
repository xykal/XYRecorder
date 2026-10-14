package com.xyverse.xyrecorder.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xyverse.xyrecorder.XYRecorderApp
import com.xyverse.xyrecorder.audio.AudioRecorder
import com.xyverse.xyrecorder.data.AppSettings
import com.xyverse.xyrecorder.data.RecordingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for audio recording app
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {
    
    private val settingsRepo = (app as XYRecorderApp).settingsRepository
    private val audioRecorder = AudioRecorder(app)
    
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    private val _status = MutableStateFlow(RecordingStatus())
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()
    
    private val _recordings = MutableStateFlow<List<File>>(emptyList())
    val recordings: StateFlow<List<File>> = _recordings.asStateFlow()
    
    init {
        // Load settings
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { appSettings ->
                _settings.value = appSettings
                audioRecorder.updateNoiseSettings(appSettings.noiseReduction.enabled)
            }
        }
        
        // Setup audio recorder callbacks
        audioRecorder.onAudioLevelUpdate = { level ->
            _status.update { it.copy(micLevelDb = level) }
        }
        
        audioRecorder.onRecordingComplete = { file ->
            _status.update { it.copy(
                isRecording = false,
                currentOutputPath = file.absolutePath
            ) }
            loadRecordings()
        }
        
        audioRecorder.onError = { error ->
            _status.update { it.copy(error = error) }
        }
        
        // Load existing recordings
        loadRecordings()
    }
    
    /**
     * Start recording
     */
    fun startRecording() {
        audioRecorder.startRecording()
        _status.update { it.copy(isRecording = true) }
    }
    
    /**
     * Stop recording
     */
    fun stopRecording() {
        audioRecorder.stopRecording()
        _status.update { it.copy(isRecording = false) }
    }
    
    /**
     * Toggle noise reduction
     */
    fun toggleNoiseReduction(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateNoiseReduction { copy(enabled = enabled) }
        }
    }
    
    /**
     * Update noise reduction preset
     */
    fun updateNoisePreset(preset: String) {
        viewModelScope.launch {
            settingsRepo.updateNoiseReduction { copy(preset = preset) }
        }
    }
    
    /**
     * Load recorded files
     */
    private fun loadRecordings() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val dir = File(
                app.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC),
                "XYRecorder"
            )
            
            if (dir.exists()) {
                _recordings.value = dir.listFiles()
                    ?.filter { it.extension == "wav" }
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()
            }
        }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _status.update { it.copy(error = null) }
    }
}
