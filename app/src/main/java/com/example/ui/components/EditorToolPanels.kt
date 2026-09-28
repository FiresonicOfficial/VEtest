package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AspectRatioType
import com.example.data.model.FilterType
import com.example.data.model.TextPosition
import com.example.data.model.VideoClip
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg

enum class EditorTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TRIM("Kırp", Icons.Default.ContentCut),
    SPEED("Yavaşlat & Hız", Icons.Default.Speed),
    FILTERS("Filtreler", Icons.Default.ColorLens),
    AUDIO("Ses", Icons.Default.VolumeUp),
    TRANSFORM("Döndür", Icons.Default.RotateRight),
    TEXT("Yazı", Icons.Default.TextFields),
    RATIO("En-Boy", Icons.Default.AspectRatio)
}

@Composable
fun EditorToolPanels(
    clip: VideoClip,
    aspectRatio: AspectRatioType,
    onTrimChange: (startMs: Long, endMs: Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSlowMoChange: (enabled: Boolean, startMs: Long?, endMs: Long?, speed: Float?) -> Unit,
    onVolumeChange: (volume: Float, isMuted: Boolean) -> Unit,
    onFilterChange: (FilterType) -> Unit,
    onRotateClick: () -> Unit,
    onMirrorClick: () -> Unit,
    onTextChange: (text: String, color: Long, position: TextPosition) -> Unit,
    onAspectRatioChange: (AspectRatioType) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = EditorTab.values()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioCardBg)
    ) {
        // Tab Bar
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (tabs[selectedTab]) {
                EditorTab.TRIM -> {
                    TrimScrubber(
                        clip = clip,
                        onTrimChange = onTrimChange
                    )
                }

                EditorTab.SPEED -> {
                    SpeedAndSlowMoPanel(
                        clip = clip,
                        onSpeedChange = onSpeedChange,
                        onSlowMoChange = onSlowMoChange
                    )
                }

                EditorTab.FILTERS -> {
                    FiltersPanel(
                        currentFilter = clip.filterType,
                        onFilterSelect = onFilterChange
                    )
                }

                EditorTab.AUDIO -> {
                    AudioPanel(
                        clip = clip,
                        onVolumeChange = onVolumeChange
                    )
                }

                EditorTab.TRANSFORM -> {
                    TransformPanel(
                        rotation = clip.rotationDegrees,
                        isMirrored = clip.isMirrored,
                        onRotateClick = onRotateClick,
                        onMirrorClick = onMirrorClick
                    )
                }

                EditorTab.TEXT -> {
                    TextOverlayPanel(
                        text = clip.overlayText,
                        color = clip.overlayTextColor,
                        position = clip.overlayPosition,
                        onTextChange = onTextChange
                    )
                }

                EditorTab.RATIO -> {
                    AspectRatioPanel(
                        currentRatio = aspectRatio,
                        onSelectRatio = onAspectRatioChange
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeedAndSlowMoPanel(
    clip: VideoClip,
    onSpeedChange: (Float) -> Unit,
    onSlowMoChange: (enabled: Boolean, startMs: Long?, endMs: Long?, speed: Float?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Klip Hızı (Yavaşlatma / Hızlandırma)",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Speed preset chips
        val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            speedOptions.forEach { speed ->
                val isSelected = clip.playbackSpeed == speed && !clip.hasSlowMoSection
                val label = when (speed) {
                    0.25f -> "0.25x (Aşırı Ağır)"
                    0.5f -> "0.5x (Ağır Çekim)"
                    0.75f -> "0.75x"
                    1.0f -> "1.0x (Normal)"
                    1.5f -> "1.5x"
                    2.0f -> "2.0x (Hızlı)"
                    else -> "${speed}x"
                }
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSlowMoChange(false, null, null, null)
                        onSpeedChange(speed)
                    },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Section Slow-Motion feature
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Belirli Bölümü Yavaşlat (Ağır Çekim)",
                            color = AccentAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Videonun seçtiğin saniyeleri yavaşlar",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = clip.hasSlowMoSection,
                        onCheckedChange = { isChecked ->
                            val s = clip.trimStartMs + ((clip.trimEndMs - clip.trimStartMs) * 0.25f).toLong()
                            val e = clip.trimStartMs + ((clip.trimEndMs - clip.trimStartMs) * 0.75f).toLong()
                            onSlowMoChange(isChecked, s, e, 0.5f)
                        }
                    )
                }

                if (clip.hasSlowMoSection) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ağır Çekim Aralığı: ${formatTime(clip.slowMoStartMs)} - ${formatTime(clip.slowMoEndMs)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    val rangeStart = clip.trimStartMs.toFloat()
                    val rangeEnd = clip.trimEndMs.toFloat().coerceAtLeast(rangeStart + 100f)

                    RangeSlider(
                        value = clip.slowMoStartMs.toFloat()..clip.slowMoEndMs.toFloat(),
                        onValueChange = { range ->
                            onSlowMoChange(true, range.start.toLong(), range.endInclusive.toLong(), clip.slowMoSpeed)
                        },
                        valueRange = rangeStart..rangeEnd,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentAmber,
                            activeTrackColor = AccentAmber
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bölüm Hızı:", color = Color.LightGray, fontSize = 11.sp)
                        listOf(0.25f, 0.5f).forEach { s ->
                            FilterChip(
                                selected = clip.slowMoSpeed == s,
                                onClick = { onSlowMoChange(true, null, null, s) },
                                label = { Text("${s}x", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltersPanel(
    currentFilter: FilterType,
    onFilterSelect: (FilterType) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Video Renk Efekti / Filtresi",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterType.values().forEach { filter ->
                val isSelected = currentFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelect(filter) },
                    label = {
                        Column {
                            Text(filter.displayName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(filter.description, fontSize = 9.sp, color = Color.LightGray)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun AudioPanel(
    clip: VideoClip,
    onVolumeChange: (Float, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (clip.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = if (clip.isMuted) Color.Gray else SecondaryCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (clip.isMuted) "Video Sesi Kapalı (Sessiz)" else "Video Sesi Açık",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Orijinal video sesini koru veya kapat", color = Color.Gray, fontSize = 10.sp)
                }
            }

            Switch(
                checked = !clip.isMuted,
                onCheckedChange = { isEnabled ->
                    onVolumeChange(if (isEnabled) 1.0f else 0f, !isEnabled)
                }
            )
        }

        if (!clip.isMuted) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Ses Seviyesi: %${(clip.volume * 100).toInt()}",
                color = Color.LightGray,
                fontSize = 12.sp
            )
            Slider(
                value = clip.volume,
                onValueChange = { onVolumeChange(it, false) },
                valueRange = 0f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = SecondaryCyan,
                    activeTrackColor = SecondaryCyan
                )
            )
        }
    }
}

@Composable
fun TransformPanel(
    rotation: Int,
    isMirrored: Boolean,
    onRotateClick: () -> Unit,
    onMirrorClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Döndürme & Yansıtma", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rotate 90
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onRotateClick() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.RotateRight, contentDescription = "Döndür", tint = SecondaryCyan, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("90° Döndür", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("Şu an: $rotation°", color = Color.Gray, fontSize = 10.sp)
                }
            }

            // Mirror
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onMirrorClick() },
                colors = CardDefaults.cardColors(
                    containerColor = if (isMirrored) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.Flip, contentDescription = "Aynala", tint = SecondaryCyan, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Yatay Aynala", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(if (isMirrored) "Aynalanmış" else "Normal", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextOverlayPanel(
    text: String,
    color: Long,
    position: TextPosition,
    onTextChange: (String, Long, TextPosition) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Metin / Filigran Ekle", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { onTextChange(it, color, position) },
            placeholder = { Text("Video üzerine yazı yazın...", color = Color.Gray, fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Position & Colors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position selector
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextPosition.values().forEach { pos ->
                    FilterChip(
                        selected = position == pos,
                        onClick = { onTextChange(text, color, pos) },
                        label = { Text(pos.displayName, fontSize = 10.sp) }
                    )
                }
            }

            // Colors
            val colors = listOf(0xFFFFFFFF, 0xFFFACC15, 0xFF38BDF8, 0xFFF43F5E, 0xFF10B981)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                colors.forEach { c ->
                    val isSelected = color == c
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(c), CircleShape)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color.White else StudioBorder,
                                shape = CircleShape
                            )
                            .clickable { onTextChange(text, c, position) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AspectRatioPanel(
    currentRatio: AspectRatioType,
    onSelectRatio: (AspectRatioType) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Video En-Boy Formatı", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AspectRatioType.values().forEach { ratio ->
                FilterChip(
                    selected = currentRatio == ratio,
                    onClick = { onSelectRatio(ratio) },
                    label = { Text(ratio.displayName, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}
