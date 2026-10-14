package com.xyverse.xyrecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xyverse.xyrecorder.ui.screens.MainScreen
import com.xyverse.xyrecorder.ui.screens.MainViewModel
import com.xyverse.xyrecorder.ui.theme.XYRecorderTheme

class MainActivity : ComponentActivity() {

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

        setContent {
            XYRecorderTheme {
                val viewModel: MainViewModel = viewModel()
                
                val settings by viewModel.settings.collectAsState()
                val status by viewModel.status.collectAsState()
                val recordings by viewModel.recordings.collectAsState()
                
                MainScreen(
                    settings = settings,
                    status = status,
                    recordings = recordings,
                    onStartRecording = { viewModel.startRecording() },
                    onStopRecording = { viewModel.stopRecording() },
                    onToggleNoiseReduction = { viewModel.toggleNoiseReduction(it) },
                    onUpdateNoisePreset = { viewModel.updateNoisePreset(it) },
                    onRequestPermission = { checkPermissions() },
                    onClearError = { viewModel.clearError() }
                )
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        
        val granted = permissions.all { 
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
        }
        
        if (!granted) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
