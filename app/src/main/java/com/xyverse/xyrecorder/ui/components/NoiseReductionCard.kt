package com.xyverse.xyrecorder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.xyverse.xyrecorder.ui.theme.XYVerseColors

/**
 * Noise reduction settings card
 */
@Composable
fun NoiseReductionCard(
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
                            "light" -> "\uD83C\uDF24" // 🌤
                            "medium" -> "\u2600" // ☀
                            "heavy" -> "\uD83D\uDD25" // 🔥
                            "studio" -> "\uD83C\uDF99" // 🎙
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
