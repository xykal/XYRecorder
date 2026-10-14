package com.xyverse.xyrecorder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xyverse.xyrecorder.data.NoiseReductionPreset
import com.xyverse.xyrecorder.data.NoiseReductionSettings
import com.xyverse.xyrecorder.service.RecordingService
import com.xyverse.xyrecorder.ui.components.NoiseReductionPanel
import com.xyverse.xyrecorder.ui.screens.*
import com.xyverse.xyrecorder.ui.theme.XYRecorderTheme

class MainActivity : ComponentActivity() {

    private lateinit var projectionManager: MediaProjectionManager

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Start the recording service with the projection data
            startRecordingService(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Screen recording permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "Audio permission required for recording", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        setContent {
            XYRecorderTheme {
                MainScreen(
                    onRequestScreenCapture = {
                        val intent = projectionManager.createScreenCaptureIntent()
                        screenCaptureLauncher.launch(intent)
                    },
                    onRequestPermissions = {
                        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        audioPermissionLauncher.launch(perms.toTypedArray())
                    },
                    checkPermissions = {
                        ContextCompat.checkSelfPermission(
                            this, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    },
                    onCheckOverlayPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
                            Toast.makeText(this, "Enable overlay permission for floating bubble", Toast.LENGTH_LONG).show()
                            false
                        } else {
                            true
                        }
                    }
                )
            }
        }
    }

    private fun startRecordingService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_RESULT_CODE, resultCode)
            putExtra(RecordingService.EXTRA_RESULT_DATA, data)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MainScreen(
    onRequestScreenCapture: () -> Unit,
    onRequestPermissions: () -> Unit,
    checkPermissions: () -> Boolean,
    onCheckOverlayPermission: () -> Boolean,
    viewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val status by viewModel.status.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.Home) }
    val hasPermissions = checkPermissions()

    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            onRequestPermissions()
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            Screen.Home -> HomeScreen(
                settings = settings,
                status = status,
                canStart = hasPermissions,
                onStartRecording = {
                    if (hasPermissions) {
                        // Check overlay permission for floating bubble
                        if (onCheckOverlayPermission()) {
                            onRequestScreenCapture()
                        }
                    } else {
                        onRequestPermissions()
                    }
                },
                onStopRecording = viewModel::stopRecording,
                onPauseRecording = viewModel::pauseRecording,
                onResumeRecording = viewModel::resumeRecording,
                onOpenSettings = { currentScreen = Screen.Settings },
                onOpenNoiseReduction = { currentScreen = Screen.NoiseReduction },
                modifier = Modifier.fillMaxSize()
            )
            Screen.Settings -> SettingsScreen(
                settings = settings,
                onBack = { currentScreen = Screen.Home },
                onUpdateVideo = viewModel::updateVideoSettings,
                onUpdateAudio = viewModel::updateAudioSettings,
                onUpdateGeneral = viewModel::updateGeneral,
                modifier = Modifier.fillMaxSize()
            )
            Screen.NoiseReduction -> NoiseReductionScreen(
                noiseSettings = settings.noiseReduction,
                micLevelDb = status.micLevelDb,
                processedLevelDb = status.processedLevelDb,
                onBack = { currentScreen = Screen.Home },
                onUpdate = viewModel::updateNoiseReduction,
                onApplyPreset = viewModel::applyNoisePreset,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

enum class Screen { Home, Settings, NoiseReduction }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoiseReductionScreen(
    noiseSettings: NoiseReductionSettings,
    micLevelDb: Float,
    processedLevelDb: Float,
    onBack: () -> Unit,
    onUpdate: (NoiseReductionSettings.() -> NoiseReductionSettings) -> Unit,
    onApplyPreset: (NoiseReductionPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        TopAppBar(
            title = {
                Text("Noise Reduction", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        NoiseReductionPanel(
            settings = noiseSettings,
            onUpdate = onUpdate,
            onApplyPreset = onApplyPreset,
            modifier = Modifier.weight(1f)
        )
    }
}
