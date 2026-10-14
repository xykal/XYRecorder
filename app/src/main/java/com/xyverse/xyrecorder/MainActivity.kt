package com.xyverse.xyrecorder

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xyverse.xyrecorder.service.AudioRecordingService
import com.xyverse.xyrecorder.ui.screens.MainScreen
import com.xyverse.xyrecorder.ui.screens.MainViewModel
import com.xyverse.xyrecorder.ui.theme.XYRecorderTheme

class MainActivity : ComponentActivity() {
    
    private var audioService: AudioRecordingService? = null
    private var bound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioRecordingService.LocalBinder
            audioService = binder.getService()
            bound = true
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            bound = false
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "✅ Permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "⚠️ Microphone permission required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request permissions on start
        checkPermissions()
        
        // Bind to service
        val intent = Intent(this, AudioRecordingService::class.java)
        startService(intent) // Start service first
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            XYRecorderTheme {
                val viewModel: MainViewModel = viewModel()
                
                val settings by viewModel.settings.collectAsState()
                val status by viewModel.status.collectAsState()
                val recordings by viewModel.recordings.collectAsState()
                
                // Connect service callbacks
                LaunchedEffect(bound) {
                    if (bound) {
                        audioService?.onAudioLevelUpdate = { level ->
                            viewModel.updateMicLevel(level)
                        }
                        audioService?.onRecordingComplete = { file ->
                            viewModel.onRecordingComplete(file)
                        }
                        audioService?.onError = { error ->
                            viewModel.setError(error)
                        }
                    }
                }
                
                MainScreen(
                    settings = settings,
                    status = status.copy(isRecording = bound && (audioService?.isRecording() ?: false)),
                    recordings = recordings,
                    onStartRecording = { 
                        if (bound) {
                            audioService?.startRecording()
                            viewModel.setRecordingState(true)
                        }
                    },
                    onStopRecording = { 
                        if (bound) {
                            audioService?.stopRecording()
                            viewModel.setRecordingState(false)
                        }
                    },
                    onToggleNoiseReduction = { enabled ->
                        viewModel.toggleNoiseReduction(enabled)
                        audioService?.updateNoiseSettings(enabled)
                    },
                    onUpdateNoisePreset = { preset ->
                        viewModel.updateNoisePreset(preset)
                        audioService?.updatePreset(preset)
                    },
                    onRequestPermission = { checkPermissions() },
                    onClearError = { viewModel.clearError() }
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        
        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val granted = permissions.all { 
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
        }
        
        if (!granted) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
