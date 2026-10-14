package com.xyverse.xyrecorder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xyverse.xyrecorder.data.NoiseReductionPreset
import com.xyverse.xyrecorder.data.NoiseReductionSettings
import com.xyverse.xyrecorder.ui.components.NoiseReductionPanel
import com.xyverse.xyrecorder.ui.screens.*
import com.xyverse.xyrecorder.ui.theme.XYRecorderTheme

class MainActivity : ComponentActivity() {

    private lateinit var projectionManager: MediaProjectionManager
    private var pendingProjectionData: Intent? = null
    private var pendingResultCode: Int = -1

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            pendingResultCode = result.resultCode
            pendingProjectionData = result.data
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions updated — recomposition will pick up the change
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
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    onRequestScreenCapture: () -> Unit,
    onRequestPermissions: () -> Unit,
    checkPermissions: () -> Boolean,
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
                    if (hasPermissions) onRequestScreenCapture()
                    else onRequestPermissions()
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
