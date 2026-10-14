package com.xyverse.xyrecorder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xyverse.xyrecorder.data.*
import com.xyverse.xyrecorder.ui.theme.*

@Composable
fun NoiseReductionPanel(
    settings: NoiseReductionSettings,
    onUpdate: (NoiseReductionSettings.() -> NoiseReductionSettings) -> Unit,
    onApplyPreset: (NoiseReductionPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Purple40.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = Purple40,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Noise Reduction", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Studio-quality processing", color = TextMuted, fontSize = 12.sp)
                }
            }
            Switch(
                checked = settings.enabled,
                onCheckedChange = { onUpdate { copy(enabled = it) } },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Purple40,
                    checkedTrackColor = Purple40.copy(alpha = 0.3f)
                )
            )
        }

        // Presets
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Preset", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NoiseReductionPreset.values().filter { it != NoiseReductionPreset.CUSTOM }.forEach { preset ->
                        PresetChip(
                            preset = preset,
                            selected = settings.preset == preset,
                            onClick = { onApplyPreset(preset) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Processing chain
        if (settings.enabled) {
            ProcessingChainSection(settings, onUpdate)
        }
    }
}

@Composable
private fun PresetChip(
    preset: NoiseReductionPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emoji = when (preset) {
        NoiseReductionPreset.NONE -> "🔇"
        NoiseReductionPreset.LIGHT -> "🌤️"
        NoiseReductionPreset.MEDIUM -> "☀️"
        NoiseReductionPreset.HEAVY -> "🔥"
        NoiseReductionPreset.STUDIO -> "🎙️"
        NoiseReductionPreset.CUSTOM -> "⚙️"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) Purple40.copy(alpha = 0.2f) else SurfaceVariant
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                preset.label,
                color = if (selected) Purple40 else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ProcessingChainSection(
    settings: NoiseReductionSettings,
    onUpdate: (NoiseReductionSettings.() -> NoiseReductionSettings) -> Unit
) {
    // High-pass filter
    ExpandableProcessingCard(
        title = "High-Pass Filter",
        icon = Icons.Default.Waves,
        description = "Remove low-frequency rumble",
        enabled = settings.highPassEnabled,
        onToggle = { onUpdate { copy(highPassEnabled = it) } }
    ) {
        SliderRow(
            label = "Frequency",
            value = settings.highPassFrequency.toFloat(),
            range = 20f..300f,
            unit = "Hz",
            onValueChange = { onUpdate { copy(highPassFrequency = it.toInt()) } }
        )
    }

    // Noise gate
    ExpandableProcessingCard(
        title = "Noise Gate",
        icon = Icons.Default.Shield,
        description = "Silence background noise",
        enabled = settings.noiseGateEnabled,
        onToggle = { onUpdate { copy(noiseGateEnabled = it) } }
    ) {
        SliderRow(
            label = "Threshold",
            value = settings.noiseGateThresholdDb,
            range = -60f..-10f,
            unit = "dB",
            onValueChange = { onUpdate { copy(noiseGateThresholdDb = it) } }
        )
        SliderRow(
            label = "Attack",
            value = settings.noiseGateAttackMs,
            range = 1f..20f,
            unit = "ms",
            onValueChange = { onUpdate { copy(noiseGateAttackMs = it) } }
        )
        SliderRow(
            label = "Release",
            value = settings.noiseGateReleaseMs,
            range = 20f..500f,
            unit = "ms",
            onValueChange = { onUpdate { copy(noiseGateReleaseMs = it) } }
        )
    }

    // Noise suppression
    ExpandableProcessingCard(
        title = "Noise Suppression",
        icon = Icons.Default.Bolt,
        description = "AI-based noise reduction",
        enabled = settings.noiseSuppressionEnabled,
        onToggle = { onUpdate { copy(noiseSuppressionEnabled = it) } }
    ) {
        SliderRow(
            label = "Strength",
            value = settings.noiseSuppressionStrength,
            range = 0f..1f,
            unit = "",
            displayMultiplier = 100f,
            displaySuffix = "%",
            onValueChange = { onUpdate { copy(noiseSuppressionStrength = it) } }
        )
    }

    // Compressor
    ExpandableProcessingCard(
        title = "Compressor",
        icon = Icons.Default.VolumeUp,
        description = "Even out volume levels",
        enabled = settings.compressorEnabled,
        onToggle = { onUpdate { copy(compressorEnabled = it) } }
    ) {
        SliderRow(
            label = "Threshold",
            value = settings.compressorThresholdDb,
            range = -40f..-6f,
            unit = "dB",
            onValueChange = { onUpdate { copy(compressorThresholdDb = it) } }
        )
        SliderRow(
            label = "Ratio",
            value = settings.compressorRatio,
            range = 1f..10f,
            unit = ":1",
            onValueChange = { onUpdate { copy(compressorRatio = it) } }
        )
        SliderRow(
            label = "Makeup Gain",
            value = settings.compressorMakeupGainDb,
            range = 0f..18f,
            unit = "dB",
            onValueChange = { onUpdate { copy(compressorMakeupGainDb = it) } }
        )
    }

    // De-esser
    ExpandableProcessingCard(
        title = "De-Esser",
        icon = Icons.Default.GraphicEq,
        description = "Reduce harsh S/Sh sounds",
        enabled = settings.deEsserEnabled,
        onToggle = { onUpdate { copy(deEsserEnabled = it) } }
    ) {
        SliderRow(
            label = "Frequency",
            value = settings.deEsserFrequency.toFloat(),
            range = 3000f..10000f,
            unit = "Hz",
            onValueChange = { onUpdate { copy(deEsserFrequency = it.toInt()) } }
        )
        SliderRow(
            label = "Threshold",
            value = settings.deEsserThresholdDb,
            range = -40f..-6f,
            unit = "dB",
            onValueChange = { onUpdate { copy(deEsserThresholdDb = it) } }
        )
    }

    // Limiter
    ExpandableProcessingCard(
        title = "Limiter",
        icon = Icons.Default.Speed,
        description = "Prevent clipping",
        enabled = settings.limiterEnabled,
        onToggle = { onUpdate { copy(limiterEnabled = it) } }
    ) {
        SliderRow(
            label = "Ceiling",
            value = settings.limiterCeilingDb,
            range = -6f..0f,
            unit = "dB",
            onValueChange = { onUpdate { copy(limiterCeilingDb = it) } }
        )
        SliderRow(
            label = "Release",
            value = settings.limiterReleaseMs,
            range = 10f..200f,
            unit = "ms",
            onValueChange = { onUpdate { copy(limiterReleaseMs = it) } }
        )
    }
}

@Composable
private fun ExpandableProcessingCard(
    title: String,
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) Surface else Surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (enabled) Purple40 else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            title,
                            color = if (enabled) TextPrimary else TextMuted,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Text(description, color = TextMuted, fontSize = 10.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (enabled) Accent else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 0.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    displayMultiplier: Float = 1f,
    displaySuffix: String = unit,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextSecondary, fontSize = 11.sp)
            Text(
                "${String.format("%.1f", value * displayMultiplier)}$displaySuffix",
                color = Purple40,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = Purple40,
                activeTrackColor = Purple40,
                inactiveTrackColor = SurfaceVariant
            )
        )
    }
}
