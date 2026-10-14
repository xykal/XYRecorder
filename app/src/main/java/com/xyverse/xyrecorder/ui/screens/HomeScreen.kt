package com.xyverse.xyrecorder.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xyverse.xyrecorder.data.*
import com.xyverse.xyrecorder.ui.theme.XYVerseColors
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settings: AppSettings,
    status: RecordingStatus,
    canStart: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNoiseReduction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.background(XYVerseColors.BackgroundDark),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Custom Logo
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            XYVerseColors.PrimaryStart,
                                            XYVerseColors.PrimaryEnd
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                "XYRecorder",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = XYVerseColors.TextPrimary
                            )
                            Text(
                                "Professional Screen Recorder",
                                fontSize = 11.sp,
                                color = XYVerseColors.TextMuted
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = XYVerseColors.TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Status Card with Gradient
            StatusCard(status = status, videoSettings = settings.video)

            Spacer(modifier = Modifier.weight(1f))

            // Main Recording Button with Animation
            RecordButton(
                isRecording = status.isRecording,
                isPaused = status.isPaused,
                canStart = canStart,
                onStart = onStartRecording,
                onStop = onStopRecording,
                onPause = onPauseRecording,
                onResume = onResumeRecording
            )

            Spacer(modifier = Modifier.weight(1f))

            // Noise Reduction Quick Access
            NoiseReductionCard(
                preset = settings.noiseReduction.preset,
                onClick = onOpenNoiseReduction
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Settings
            QuickSettingsRow(videoSettings = settings.video)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusCard(status: RecordingStatus, videoSettings: VideoSettings) {
    var elevated by remember { mutableStateOf(false) }
    
    LaunchedEffect(status.isRecording) {
        elevated = status.isRecording
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = XYVerseColors.BackgroundCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (elevated) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusItem(
                icon = Icons.Default.HighQuality,
                label = "Quality",
                value = videoSettings.resolution.label,
                color = XYVerseColors.PrimaryStart
            )
            StatusItem(
                icon = Icons.Default.Speed,
                label = "FPS",
                value = "${videoSettings.fps}",
                color = XYVerseColors.ElectricBlue
            )
            StatusItem(
                icon = Icons.Default.Timer,
                label = "Duration",
                value = formatDuration(status.durationMs),
                color = if (status.isRecording) XYVerseColors.RecordingRed else XYVerseColors.Success
            )
        }
    }
}

@Composable
private fun StatusItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            value,
            color = XYVerseColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            label,
            color = XYVerseColors.TextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    isPaused: Boolean,
    canStart: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !isPaused) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    if (isRecording) {
        // Recording controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pause/Resume button
            FloatingActionButton(
                onClick = if (isPaused) onResume else onPause,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                containerColor = XYVerseColors.BackgroundElevated,
                contentColor = XYVerseColors.TextPrimary
            ) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    modifier = Modifier.size(28.dp)
                )
            }

            // Stop button with glow effect
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                XYVerseColors.RecordingRed,
                                XYVerseColors.RecordingRedDark
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                    )
                }
            }
        }
    } else {
        // Start button with gradient
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(if (canStart) 1f else 0.8f)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = if (canStart) {
                            listOf(XYVerseColors.PrimaryStart, XYVerseColors.PrimaryEnd)
                        } else {
                            listOf(
                                XYVerseColors.BackgroundElevated,
                                XYVerseColors.BackgroundElevated
                            )
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onStart,
                enabled = canStart,
                modifier = Modifier.size(120.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun NoiseReductionCard(preset: NoiseReductionPreset, onClick: () -> Unit) {
    val emoji = when (preset) {
        NoiseReductionPreset.NONE -> "🔇"
        NoiseReductionPreset.LIGHT -> "🌤️"
        NoiseReductionPreset.MEDIUM -> "☀️"
        NoiseReductionPreset.HEAVY -> "🔥"
        NoiseReductionPreset.STUDIO -> "🎙️"
        NoiseReductionPreset.CUSTOM -> "⚙️"
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = XYVerseColors.BackgroundCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    XYVerseColors.NeonPurple,
                                    XYVerseColors.ElectricBlue
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 28.sp)
                }
                Column {
                    Text(
                        "Noise Reduction",
                        color = XYVerseColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Preset: ${preset.label}",
                        color = XYVerseColors.TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = XYVerseColors.TextMuted,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun QuickSettingsRow(videoSettings: VideoSettings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickChip(
            label = videoSettings.codec.label,
            icon = Icons.Default.Memory,
            color = XYVerseColors.NeonPurple,
            modifier = Modifier.weight(1f)
        )
        QuickChip(
            label = "${videoSettings.bitrate / 1_000_000} Mbps",
            icon = Icons.Default.DataUsage,
            color = XYVerseColors.ElectricBlue,
            modifier = Modifier.weight(1f)
        )
        QuickChip(
            label = videoSettings.quality.label,
            icon = Icons.Default.Star,
            color = XYVerseColors.SunsetOrange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                color = color,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
