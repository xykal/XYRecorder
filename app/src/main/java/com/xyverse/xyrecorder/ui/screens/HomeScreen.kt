package com.xyverse.xyrecorder.ui.screens

import android.os.Environment
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
import com.xyverse.xyrecorder.data.AppSettings
import com.xyverse.xyrecorder.data.RecordingStatus
import com.xyverse.xyrecorder.ui.theme.XYVerseColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    settings: AppSettings,
    status: RecordingStatus,
    recordings: List<File>,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onToggleNoiseReduction: (Boolean) -> Unit,
    onUpdateNoisePreset: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        XYVerseColors.BackgroundDark,
                        XYVerseColors.BackgroundDark.copy(alpha = 0.95f),
                        XYVerseColors.BackgroundDark
                    )
                )
            )
    ) {
        // Header
        HeaderSection()
        
        // Main content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Status card
            StatusCard(
                isRecording = status.isRecording,
                micLevel = status.micLevelDb,
                settings = settings
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Audio level meter
            LevelMeter(
                levelDb = status.micLevelDb,
                isRecording = status.isRecording
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Recording button
            RecordingButton(
                isRecording = status.isRecording,
                onStart = onStartRecording,
                onStop = onStopRecording
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Noise reduction card
            NoiseReductionCard(
                enabled = settings.noiseReduction.enabled,
                preset = settings.noiseReduction.preset,
                onToggle = onToggleNoiseReduction,
                onPresetChange = onUpdateNoisePreset
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Recordings list
            RecordingsList(recordings = recordings)
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Error dialog
    status.error?.let { error ->
        AlertDialog(
            onDismissRequest = onClearError,
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                Button(onClick = onClearError) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
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
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Column {
                Text(
                    "XYRecorder",
                    color = XYVerseColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Studio Audio Processor",
                    color = XYVerseColors.TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    isRecording: Boolean,
    micLevel: Float,
    settings: AppSettings
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = XYVerseColors.BackgroundCard
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusItem(
                    icon = Icons.Default.Mic,
                    label = "Status",
                    value = if (isRecording) "Recording" else "Ready",
                    color = if (isRecording) XYVerseColors.RecordingRed else XYVerseColors.Success
                )
                
                StatusItem(
                    icon = Icons.Default.Waves,
                    label = "Noise Reduction",
                    value = if (settings.noiseReduction.enabled) "ON" else "OFF",
                    color = if (settings.noiseReduction.enabled) XYVerseColors.PrimaryStart else XYVerseColors.TextMuted
                )
                
                StatusItem(
                    icon = Icons.Default.FormatAudioDescription,
                    label = "Format",
                    value = "WAV",
                    color = XYVerseColors.ElectricBlue
                )
            }
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = XYVerseColors.TextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun LevelMeter(
    levelDb: Float,
    isRecording: Boolean
) {
    // Convert dB to 0-100 scale
    val level = ((levelDb + 60) / 60 * 100).coerceIn(0f, 100f)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = XYVerseColors.BackgroundCard
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Audio Level",
                    color = XYVerseColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${String.format("%.1f", levelDb)} dB",
                    color = XYVerseColors.TextMuted,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Level bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(XYVerseColors.BackgroundElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(level / 100f)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    XYVerseColors.Success,
                                    XYVerseColors.Warning,
                                    XYVerseColors.RecordingRed
                                )
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Scale markers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("-60", color = XYVerseColors.TextMuted, fontSize = 10.sp)
                Text("-30", color = XYVerseColors.TextMuted, fontSize = 10.sp)
                Text("0", color = XYVerseColors.TextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun RecordingButton(
    isRecording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            // Stop button
            Box(
                modifier = Modifier
                    .size(120.dp)
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
                    modifier = Modifier.size(120.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop Recording",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        } else {
            // Start button
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
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
                IconButton(
                    onClick = onStart,
                    modifier = Modifier.size(140.dp)
                ) {
                    Icon(
                        Icons.Default.FiberManualRecord,
                        contentDescription = "Start Recording",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoiseReductionCard(
    enabled: Boolean,
    preset: String,
    onToggle: (Boolean) -> Unit,
    onPresetChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = XYVerseColors.BackgroundCard
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            "Noise Reduction",
                            color = XYVerseColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Studio-quality audio processing",
                            color = XYVerseColors.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = XYVerseColors.PrimaryStart,
                        checkedTrackColor = XYVerseColors.PrimaryStart.copy(alpha = 0.3f)
                    )
                )
            }
            
            if (enabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Preset chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("light", "medium", "heavy", "studio").forEach { presetName ->
                        val emoji = when (presetName) {
                            "light" -> "🌤️"
                            "medium" -> "☀️"
                            "heavy" -> "🔥"
                            "studio" -> "🎙️"
                            else -> ""
                        }
                        
                        FilterChip(
                            selected = preset == presetName,
                            onClick = { onPresetChange(presetName) },
                            label = {
                                Text(
                                    "$emoji ${presetName.replaceFirstChar { it.uppercase() }}",
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = XYVerseColors.PrimaryStart.copy(alpha = 0.2f),
                                selectedLabelColor = XYVerseColors.PrimaryStart
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingsList(recordings: List<File>) {
    if (recordings.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = XYVerseColors.BackgroundCard
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Recent Recordings",
                color = XYVerseColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            recordings.take(3).forEach { file ->
                RecordingItem(file = file)
            }
        }
    }
}

@Composable
private fun RecordingItem(file: File) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val sizeMb = file.length() / (1024.0 * 1024.0)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(XYVerseColors.ElectricBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AudioFile,
                contentDescription = null,
                tint = XYVerseColors.ElectricBlue,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                file.nameWithoutExtension,
                color = XYVerseColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${dateFormat.format(Date(file.lastModified()))} • ${String.format("%.1f", sizeMb)} MB",
                color = XYVerseColors.TextMuted,
                fontSize = 11.sp
            )
        }
    }
}
