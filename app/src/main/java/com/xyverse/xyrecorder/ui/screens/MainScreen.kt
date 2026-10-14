package com.xyverse.xyrecorder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Main screen container
 */
@Composable
fun MainScreen(
    onRequestPermission: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val status by viewModel.status.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HomeScreen(
                settings = settings,
                status = status,
                recordings = recordings,
                onStartRecording = viewModel::startRecording,
                onStopRecording = viewModel::stopRecording,
                onToggleNoiseReduction = viewModel::toggleNoiseReduction,
                onUpdateNoisePreset = viewModel::updateNoisePreset,
                onRequestPermission = onRequestPermission,
                onClearError = viewModel::clearError
            )
        }
    }
}
