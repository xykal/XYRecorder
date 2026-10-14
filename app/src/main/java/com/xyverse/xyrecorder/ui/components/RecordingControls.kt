package com.xyverse.xyrecorder.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xyverse.xyrecorder.ui.theme.*
import java.util.concurrent.TimeUnit

@Composable
fun RecordingControlButton(
    isRecording: Boolean,
    isPaused: Boolean,
    durationMs: Long,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    canStart: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isRecording) {
            // Recording controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause/Resume button
                IconButton(
                    onClick = if (isPaused) onResumeRecording else onPauseRecording,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Stop button (main action)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Danger, Danger.copy(alpha = 0.8f))
                            )
                        )
                        .clickable { onStopRecording() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                    )
                }
            }

            // Duration display
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isPaused) Warning.copy(alpha = 0.15f) else Danger.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isPaused) Warning else Danger)
                    )
                    Text(
                        text = if (isPaused) "PAUSED" else "REC",
                        color = if (isPaused) Warning else Danger,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = formatDuration(durationMs),
                        color = if (isPaused) Warning.copy(alpha = 0.7f) else Danger.copy(alpha = 0.7f),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Start button
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (canStart) Danger else Danger.copy(alpha = 0.3f),
                                if (canStart) Danger.copy(alpha = 0.8f) else Danger.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .clickable(enabled = canStart) { onStartRecording() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            Text(
                text = if (canStart) "Tap to start recording" else "Grant permission to start",
                color = TextMuted,
                fontSize = 12.sp
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

@Composable
fun AudioLevelMeter(
    levelDb: Float,
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val levelPercent = dbToPercent(levelDb)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()
                Text(text = label, color = TextSecondary, fontSize = 13.sp)
            }
            Text(
                text = "${String.format("%.0f", levelDb)} dB",
                color = TextMuted,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Level bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(levelPercent / 100f)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Accent, Accent, Warning, Danger)
                        )
                    )
            )
        }
    }
}

fun dbToPercent(db: Float): Float {
    // Map -60dB..0dB to 0..100%
    return ((db + 60f) / 60f * 100f).coerceIn(0f, 100f)
}
