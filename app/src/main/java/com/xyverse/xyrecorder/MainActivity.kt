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
import android.util.Log
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var projectionManager: MediaProjectionManager

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "📱 Screen capture result: ${result.resultCode}")
        
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            Log.d(TAG, "✅ Permission granted, starting recording service...")
            
            // Start recording service directly
            val serviceIntent = Intent(this, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START
                putExtra(RecordingService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(RecordingService.EXTRA_RESULT_DATA, result.data)
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                    Log.d(TAG, "✅ Foreground service started")
                } else {
                    startService(serviceIntent)
                    Log.d(TAG, "✅ Service started")
                }
                
                Toast.makeText(this, "🎬 Recording started!", Toast.LENGTH_SHORT).show()
                
                // Move app to background after short delay
                android.os.Handler(mainLooper).postDelayed({
                    moveTaskToBack(true)
                    Log.d(TAG, "📱 Moved to background")
                }, 500)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start service", e)
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.d(TAG, "❌ Permission denied or cancelled")
            Toast.makeText(this, "❌ Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d(TAG, "✅ All permissions granted")
            Toast.makeText(this, "✅ Permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Log.d(TAG, "❌ Some permissions denied")
            Toast.makeText(this, "⚠️ Some permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "🚀 MainActivity created")

        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        setContent {
            XYRecorderTheme {
                val context = LocalContext.current
                MainScreen(
                    onRequestScreenCapture = {
                        Log.d(TAG, "🎬 Requesting screen capture...")
                        try {
                            val intent = projectionManager.createScreenCaptureIntent()
                            screenCaptureLauncher.launch(intent)
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed to create screen capture intent", e)
                            Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    onRequestPermissions = {
                        Log.d(TAG, "🔐 Requesting permissions...")
                        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        audioPermissionLauncher.launch(perms.toTypedArray())
                    },
                    checkPermissions = {
                        val hasAudio = ContextCompat.checkSelfPermission(
                            this, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        
                        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Settings.canDrawOverlays(this)
                        } else {
                            true
                        }
                        
                        Log.d(TAG, "🔍 Permissions check - Audio: $hasAudio, Overlay: $hasOverlay")
                        hasAudio && hasOverlay
                    },
                    onRequestOverlayPermission = {
                        Log.d(TAG, "🫧 Requesting overlay permission...")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                                startActivity(intent)
                                Log.d(TAG, "✅ Overlay permission dialog opened")
                                false
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Failed to open overlay permission", e)
                                false
                            }
                        } else {
                            Log.d(TAG, "✅ Overlay permission already granted")
                            true
                        }
                    },
                    onStopRecording = {
                        Log.d(TAG, "⏹ Stopping recording...")
                        val serviceIntent = Intent(this, RecordingService::class.java).apply {
                            action = RecordingService.ACTION_STOP
                        }
                        startService(serviceIntent)
                    },
                    onPauseRecording = {
                        Log.d(TAG, "⏸ Pausing recording...")
                        val serviceIntent = Intent(this, RecordingService::class.java).apply {
                            action = RecordingService.ACTION_PAUSE
                        }
                        startService(serviceIntent)
                    },
                    onResumeRecording = {
                        Log.d(TAG, "▶ Resuming recording...")
                        val serviceIntent = Intent(this, RecordingService::class.java).apply {
                            action = RecordingService.ACTION_RESUME
                        }
                        startService(serviceIntent)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "📱 MainActivity resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "📱 MainActivity paused")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🗑️ MainActivity destroyed")
    }
}

@Composable
fun MainScreen(
    onRequestScreenCapture: () -> Unit,
    onRequestPermissions: () -> Unit,
    checkPermissions: () -> Boolean,
    onRequestOverlayPermission: () -> Boolean,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val status by viewModel.status.collectAsState()
    val context = LocalContext.current

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
                        if (onRequestOverlayPermission()) {
                            onRequestScreenCapture()
                        } else {
                            Toast.makeText(context, 
                                "⚠️ Enable overlay permission first!", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        onRequestPermissions()
                    }
                },
                onStopRecording = onStopRecording,
                onPauseRecording = onPauseRecording,
                onResumeRecording = onResumeRecording,
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
