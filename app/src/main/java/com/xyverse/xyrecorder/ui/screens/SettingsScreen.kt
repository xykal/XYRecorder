package com.xyverse.xyrecorder.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xyverse.xyrecorder.data.*
import com.xyverse.xyrecorder.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onUpdateVideo: (VideoSettings.() -> VideoSettings) -> Unit,
    onUpdateAudio: (AudioSettings.() -> AudioSettings) -> Unit,
    onUpdateGeneral: (showIndicator: Boolean?, stopOnScreenOff: Boolean?, countdown: Int?, saveToGallery: Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = modifier,
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Background,
                contentColor = Purple40,
                edgePadding = 16.dp,
                divider = {}
            ) {
                listOf("Video", "Audio", "General").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab content
            when (selectedTab) {
                0 -> VideoSettingsTab(settings.video, onUpdateVideo)
                1 -> AudioSettingsTab(settings.audio, onUpdateAudio)
                2 -> GeneralSettingsTab(settings, onUpdateGeneral)
            }
        }
    }
}

@Composable
private fun VideoSettingsTab(
    video: VideoSettings,
    onUpdate: (VideoSettings.() -> VideoSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Resolution
        SettingsSection(title = "Resolution") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoResolution.values().forEach { res ->
                    OutlinedButton(
                        onClick = { onUpdate { copy(resolution = res) } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (video.resolution == res) Purple40.copy(alpha = 0.2f) else Surface,
                            contentColor = if (video.resolution == res) Purple40 else TextSecondary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (video.resolution == res) Purple40 else Border
                        )
                    ) {
                        Text(res.label, fontSize = 12.sp)
                    }
                }
            }
        }

        // FPS
        SettingsSection(title = "Frame Rate") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(30, 60).forEach { fps ->
                    OutlinedButton(
                        onClick = { onUpdate { copy(fps = fps) } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (video.fps == fps) Purple40.copy(alpha = 0.2f) else Surface,
                            contentColor = if (video.fps == fps) Purple40 else TextSecondary
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (video.fps == fps) Purple40 else Border
                        )
                    ) {
                        Text("$fps FPS", fontSize = 12.sp)
                    }
                }
            }
        }

        // Codec
        SettingsSection(title = "Codec") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VideoCodec.values().forEach { codec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (video.codec == codec) Purple40.copy(alpha = 0.15f) else Surface)
                            .clickable { onUpdate { copy(codec = codec) } }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(codec.label, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text(codec.mimeType, color = TextMuted, fontSize = 11.sp)
                        }
                        RadioButton(
                            selected = video.codec == codec,
                            onClick = { onUpdate { copy(codec = codec) } },
                            colors = RadioButtonDefaults.colors(selectedColor = Purple40)
                        )
                    }
                }
            }
        }

        // Bitrate
        SettingsSection(title = "Bitrate") {
            val bitrates = listOf(3_000_000, 5_000_000, 8_000_000, 12_000_000, 20_000_000)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                bitrates.forEach { bitrate ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onUpdate { copy(bitrate = bitrate) } }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = video.bitrate == bitrate,
                            onClick = { onUpdate { copy(bitrate = bitrate) } },
                            colors = RadioButtonDefaults.colors(selectedColor = Purple40)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${bitrate / 1_000_000} Mbps", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioSettingsTab(
    audio: AudioSettings,
    onUpdate: (AudioSettings.() -> AudioSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Microphone toggle
        SettingsSection(title = "Microphone") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Record Microphone", color = TextPrimary, fontSize = 14.sp)
                    Text("Capture your voice through the mic", color = TextMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = audio.microphoneEnabled,
                    onCheckedChange = { onUpdate { copy(microphoneEnabled = it) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = Purple40, checkedTrackColor = Purple40.copy(alpha = 0.3f))
                )
            }
            if (audio.microphoneEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Volume: ${(audio.microphoneVolume * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = audio.microphoneVolume,
                        onValueChange = { onUpdate { copy(microphoneVolume = it) } },
                        valueRange = 0f..2f,
                        colors = SliderDefaults.colors(thumbColor = Purple40, activeTrackColor = Purple40)
                    )
                }
            }
        }

        // System audio toggle
        SettingsSection(title = "System Audio") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Record Internal Audio", color = TextPrimary, fontSize = 14.sp)
                    Text("Capture app/device sounds", color = TextMuted, fontSize = 11.sp)
                    Text("(Android 10+ required)", color = TextMuted, fontSize = 10.sp)
                }
                Switch(
                    checked = audio.systemAudioEnabled,
                    onCheckedChange = { onUpdate { copy(systemAudioEnabled = it) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = Purple40, checkedTrackColor = Purple40.copy(alpha = 0.3f))
                )
            }
            if (audio.systemAudioEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Volume: ${(audio.systemAudioVolume * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = audio.systemAudioVolume,
                        onValueChange = { onUpdate { copy(systemAudioVolume = it) } },
                        valueRange = 0f..2f,
                        colors = SliderDefaults.colors(thumbColor = Purple40, activeTrackColor = Purple40)
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneralSettingsTab(
    settings: AppSettings,
    onUpdate: (Boolean?, Boolean?, Int?, Boolean?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSection(title = "Behavior") {
            SettingsToggle(
                title = "Show recording indicator",
                description = "Show floating dot while recording",
                checked = settings.showRecordingIndicator,
                onCheckedChange = { onUpdate(it, null, null, null) }
            )
            HorizontalDivider(color = Border.copy(alpha = 0.5f))
            SettingsToggle(
                title = "Stop on screen off",
                description = "Automatically stop recording when screen turns off",
                checked = settings.stopOnScreenOff,
                onCheckedChange = { onUpdate(null, it, null, null) }
            )
            HorizontalDivider(color = Border.copy(alpha = 0.5f))
            SettingsToggle(
                title = "Save to Gallery",
                description = "Make recordings visible in Photos/Gallery app",
                checked = settings.saveToGallery,
                onCheckedChange = { onUpdate(null, null, null, it) }
            )
        }

        SettingsSection(title = "Countdown") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 3, 5, 10).forEach { seconds ->
                    OutlinedButton(
                        onClick = { onUpdate(null, null, seconds, null) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (settings.countDownBeforeStart == seconds) Purple40.copy(alpha = 0.2f) else Surface,
                            contentColor = if (settings.countDownBeforeStart == seconds) Purple40 else TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (settings.countDownBeforeStart == seconds) Purple40 else Border)
                    ) {
                        Text(if (seconds == 0) "None" else "${seconds}s", fontSize = 12.sp)
                    }
                }
            }
        }

        // App info
        SettingsSection(title = "About") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Version", "1.0.0")
                InfoRow("Developer", "xykal")
                InfoRow("Website", "xyverse.my.id")
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = TextSecondary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            content()
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp)
            Text(description, color = TextMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Purple40, checkedTrackColor = Purple40.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
