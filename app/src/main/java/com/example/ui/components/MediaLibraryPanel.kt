package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VideoClip
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.StudioLilacBorder
import com.example.ui.theme.StudioPurpleLight
import com.example.ui.theme.StudioPurplePrimary

@Composable
fun MediaLibraryPanel(
    clips: List<VideoClip>,
    selectedIndex: Int,
    onSelectClip: (Int) -> Unit,
    onAddVideoClick: () -> Unit,
    onAddSampleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        // Main "+ Video Ekle" Button (signature purple from image.png)
        Button(
            onClick = onAddVideoClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("media_panel_add_video_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = StudioPurplePrimary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Video Ekle",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Secondary "Örnek Klip Ekle" Button
        OutlinedButton(
            onClick = onAddSampleClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .testTag("media_panel_add_sample_button"),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = StudioPurplePrimary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Örnek Video Ekle",
                fontSize = 12.sp,
                color = StudioPurplePrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Header: Clips Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Medya Kütüphanesi",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${clips.size} klip",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of Media Thumbnails (matching 2-column tiles in image.png)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(clips) { index, clip ->
                val isSelected = index == selectedIndex
                MediaThumbnailTile(
                    clip = clip,
                    index = index,
                    isSelected = isSelected,
                    onClick = { onSelectClip(index) }
                )
            }
        }
    }
}

@Composable
fun MediaThumbnailTile(
    clip: VideoClip,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) StudioPurplePrimary else StudioLilacBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp

    // Decorative gradients mimicking thumbnail art from image.png
    val gradientColors = when (index % 4) {
        0 -> listOf(Color(0xFF38BDF8), Color(0xFF1E3A8A)) // Blue mountain
        1 -> listOf(Color(0xFFF97316), Color(0xFF7C2D12)) // Sunset
        2 -> listOf(Color(0xFFA855F7), Color(0xFF4C1D95)) // Purple neon
        else -> listOf(Color(0xFF10B981), Color(0xFF064E3B)) // Forest
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag("media_tile_$index"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(brush = Brush.verticalGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )

                // Slow-mo badge if applicable
                if (clip.playbackSpeed < 1.0f) {
                    Surface(
                        color = AccentAmber,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "${clip.playbackSpeed}x",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Duration badge
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    val sec = clip.trimmedDurationMs / 1000f
                    Text(
                        text = String.format("%.1fs", sec),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Clip Title
            Text(
                text = "${index + 1}. ${clip.title}",
                color = if (isSelected) StudioPurplePrimary else MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}
