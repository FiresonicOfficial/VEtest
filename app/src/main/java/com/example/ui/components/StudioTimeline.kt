package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VideoClip
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRose
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.StudioLilacBorder
import com.example.ui.theme.StudioLilacCard
import com.example.ui.theme.StudioLilacHover
import com.example.ui.theme.StudioPurpleDark
import com.example.ui.theme.StudioPurpleLight
import com.example.ui.theme.StudioPurplePrimary
import com.example.ui.theme.WaveformTrack

@Composable
fun StudioTimeline(
    clips: List<VideoClip>,
    selectedIndex: Int,
    playbackPositionMs: Long,
    isPlaying: Boolean,
    onPlayPauseToggle: (Boolean) -> Unit,
    onSelectClip: (Int) -> Unit,
    onMoveClip: (from: Int, to: Int) -> Unit,
    onDeleteClip: (Int) -> Unit,
    onDuplicateClip: (Int) -> Unit,
    onSplitClip: () -> Unit,
    onOpenReorderDialog: () -> Unit,
    onAddVideoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalProjectDurationMs = clips.sumOf { it.trimmedDurationMs }.coerceAtLeast(1000L)
    val currentClip = clips.getOrNull(selectedIndex)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, StudioLilacBorder)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Timeline Action Bar: Controls, Timecode & Quick Action Buttons (matching image.png)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause & Timecode
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play/Pause pill button
                Surface(
                    color = StudioPurplePrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onPlayPauseToggle(!isPlaying) }
                        .testTag("timeline_play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Timecode: 00:02 / 00:10
                val curSec = playbackPositionMs / 1000f
                val totSec = totalProjectDurationMs / 1000f
                Text(
                    text = String.format("%.1fs / %.1fs", curSec, totSec),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick Split, Duplicate & Reorder Toolbar Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Split button (Scissors)
                FilledTonalButton(
                    onClick = onSplitClip,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StudioPurplePrimary.copy(alpha = 0.12f),
                        contentColor = StudioPurplePrimary
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("timeline_quick_split_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Kes",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kes / Böl", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Reorder / Merge button
                FilledTonalButton(
                    onClick = onOpenReorderDialog,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StudioPurplePrimary.copy(alpha = 0.12f),
                        contentColor = StudioPurplePrimary
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("timeline_reorder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Sırala",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sırala (${clips.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Timeline Ruler Bar (0s, 1s, 2s, 3s... matching image.png)
        TimelineRulerBar(
            totalDurationMs = totalProjectDurationMs,
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Multi-Track Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioLilacHover.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .border(1.dp, StudioLilacBorder, RoundedCornerShape(8.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // TRACK 1: Text Overlay Layer (Pill element if active clip has overlay)
            val activeText = currentClip?.overlayText
            if (!activeText.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(StudioPurplePrimary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, StudioPurplePrimary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = null,
                        tint = StudioPurplePrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Metin: \"$activeText\"",
                        color = StudioPurplePrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // TRACK 2: Video Filmstrip Clips (The core track from image.png)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                itemsIndexed(clips) { index, clip ->
                    val isSelected = index == selectedIndex
                    FilmstripClipCard(
                        clip = clip,
                        index = index,
                        totalCount = clips.size,
                        isSelected = isSelected,
                        onClick = { onSelectClip(index) },
                        onMoveLeft = { if (index > 0) onMoveClip(index, index - 1) },
                        onMoveRight = { if (index < clips.size - 1) onMoveClip(index, index + 1) },
                        onDuplicate = { onDuplicateClip(index) },
                        onDelete = { onDeleteClip(index) }
                    )
                }

                // Add Clip button at end of timeline track
                item {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, StudioPurplePrimary, RoundedCornerShape(6.dp))
                            .background(StudioPurplePrimary.copy(alpha = 0.08f))
                            .clickable { onAddVideoClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Ekle",
                                tint = StudioPurplePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Ekle",
                                color = StudioPurplePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // TRACK 3: Audio Waveform Track (Matching waveform strip in image.png)
            AudioWaveformStrip(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            )
        }
    }
}

@Composable
fun FilmstripClipCard(
    clip: VideoClip,
    index: Int,
    totalCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) StudioPurplePrimary else StudioLilacBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val clipDurationSec = clip.trimmedDurationMs / 1000f

    // Dynamic width proportional to clip duration (e.g. 5s = 140dp)
    val cardWidth = (110.dp + (clipDurationSec * 10f).dp).coerceIn(120.dp, 240.dp)

    // Gradient mimicking video frames from image.png
    val frameGradient = when (index % 3) {
        0 -> listOf(Color(0xFF38BDF8), Color(0xFF0284C7)) // Mountain blue
        1 -> listOf(Color(0xFFF97316), Color(0xFFC2410C)) // Sunset orange
        else -> listOf(Color(0xFFA855F7), Color(0xFF6B21A8)) // Purple dusk
    }

    Card(
        modifier = modifier
            .width(cardWidth)
            .height(64.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .testTag("filmstrip_clip_$index"),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Trim Handle (Purple bracket)
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(StudioPurplePrimary)
                )
            }

            // Central Clip Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                frameGradient[0].copy(alpha = 0.25f),
                                frameGradient[1].copy(alpha = 0.15f)
                            )
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: Title and Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}. ${clip.title}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Slow-motion badge
                    if (clip.playbackSpeed < 1.0f) {
                        Surface(
                            color = AccentAmber,
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                text = "${clip.playbackSpeed}x",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                            )
                        }
                    }
                }

                // Bottom row: Duration and quick reorder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%.1fs", clipDurationSec),
                        color = StudioPurplePrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Mini Action Icons
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (index > 0) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Sola",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onMoveLeft() }
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Çoğalt",
                            tint = StudioPurplePrimary,
                            modifier = Modifier
                                .size(12.dp)
                                .clickable { onDuplicate() }
                        )
                        if (index < totalCount - 1) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Sağa",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onMoveRight() }
                            )
                        }
                    }
                }
            }

            // Right Trim Handle (Purple bracket)
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(StudioPurplePrimary)
                )
            }
        }
    }
}

@Composable
fun TimelineRulerBar(
    totalDurationMs: Long,
    modifier: Modifier = Modifier
) {
    val totalSeconds = (totalDurationMs / 1000f).toInt().coerceAtLeast(1)
    val step = (totalSeconds / 5).coerceAtLeast(1)

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (sec in 0..totalSeconds step step) {
            Text(
                text = "${sec}s",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AudioWaveformStrip(
    modifier: Modifier = Modifier
) {
    // Draws rhythmic sound waveform bars matching the audio track in image.png
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(WaveformTrack.copy(alpha = 0.2f))
    ) {
        val width = size.width
        val height = size.height
        val barCount = (width / 5f).toInt().coerceAtLeast(10)
        val barWidth = 2.5f

        for (i in 0 until barCount) {
            val x = i * (width / barCount)
            // Simulated rhythmic audio waveform heights
            val factor = kotlin.math.sin(i * 0.4).toFloat().coerceIn(0.15f, 0.95f)
            val barHeight = height * factor
            val top = (height - barHeight) / 2f

            drawLine(
                color = WaveformTrack,
                start = Offset(x, top),
                end = Offset(x, top + barHeight),
                strokeWidth = barWidth
            )
        }
    }
}
