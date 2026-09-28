package com.example.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.AspectRatioType
import com.example.data.model.FilterType
import com.example.data.model.TextPosition
import com.example.data.model.VideoClip
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerView(
    clip: VideoClip?,
    aspectRatio: AspectRatioType,
    isPlaying: Boolean,
    onPlayPauseToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var currentPosMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }
    var showControls by remember { mutableStateOf(true) }

    // Periodic time update
    LaunchedEffect(isPlaying, clip) {
        while (true) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    val pos = vv.currentPosition
                    currentPosMs = pos

                    // Trimming loop check
                    clip?.let { c ->
                        if (pos >= c.trimEndMs) {
                            vv.seekTo(c.trimStartMs.toInt())
                        }
                    }
                }
            }
            delay(100)
        }
    }

    // Auto hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3500)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable { showControls = !showControls },
        contentAlignment = Alignment.Center
    ) {
        if (clip == null) {
            Text(
                text = "Lütfen bir video ekleyin veya örnek video ile başlayın",
                color = Color.Gray,
                fontSize = 14.sp
            )
            return@Box
        }

        // Aspect ratio container
        val ratioModifier = if (aspectRatio.ratio != null) {
            Modifier.aspectRatio(aspectRatio.ratio)
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = ratioModifier
                .rotate(clip.rotationDegrees.toFloat())
                .scale(scaleX = if (clip.isMirrored) -1f else 1f, scaleY = 1f)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.key(clip.id) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("video_player_surface"),
                    factory = { context ->
                        val frameLayout = FrameLayout(context)
                        val videoView = VideoView(context).apply {
                            val uri = Uri.parse(clip.uriString)
                            setVideoURI(uri)
                            setOnErrorListener { _, what, extra ->
                                android.util.Log.w("VideoPlayerView", "VideoView playback error: $what, $extra")
                                true
                            }
                            setOnPreparedListener { mp ->
                                durationMs = mp.duration
                                seekTo(clip.trimStartMs.toInt())
                                mp.isLooping = true
                                if (clip.isMuted) {
                                    mp.setVolume(0f, 0f)
                                } else {
                                    mp.setVolume(clip.volume, clip.volume)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    try {
                                        val params = mp.playbackParams
                                        params.speed = clip.playbackSpeed
                                        mp.playbackParams = params
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                }
                                if (isPlaying) {
                                    start()
                                }
                            }
                        }
                        videoViewRef = videoView
                        frameLayout.addView(
                            videoView,
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        )
                        frameLayout
                    },
                    update = {
                        videoViewRef?.let { vv ->
                            if (isPlaying && !vv.isPlaying) {
                                vv.start()
                            } else if (!isPlaying && vv.isPlaying) {
                                vv.pause()
                            }
                        }
                    }
                )
            }

            // Live Filter Tint Overlay
            FilterOverlay(filter = clip.filterType)

            // Live Text Watermark Overlay
            if (clip.overlayText.isNotBlank()) {
                val alignment = when (clip.overlayPosition) {
                    TextPosition.TOP -> Alignment.TopCenter
                    TextPosition.CENTER -> Alignment.Center
                    TextPosition.BOTTOM -> Alignment.BottomCenter
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = alignment
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = clip.overlayText,
                            color = Color(clip.overlayTextColor),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Overlay Player Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Play / Pause center button
                IconButton(
                    onClick = {
                        val nextState = !isPlaying
                        onPlayPauseToggle(nextState)
                        videoViewRef?.let { vv ->
                            if (nextState) vv.start() else vv.pause()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Replay button on left
                IconButton(
                    onClick = {
                        videoViewRef?.seekTo(clip.trimStartMs.toInt())
                        currentPosMs = clip.trimStartMs.toInt()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Başa Sar",
                        tint = Color.White
                    )
                }

                // Bottom time bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(currentPosMs.toLong()),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Slider(
                        value = currentPosMs.toFloat(),
                        onValueChange = { newPos ->
                            currentPosMs = newPos.toInt()
                            videoViewRef?.seekTo(newPos.toInt())
                        },
                        valueRange = clip.trimStartMs.toFloat()..clip.trimEndMs.toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_scrub_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatTime(clip.trimEndMs),
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    DisposableEffect(clip) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }
}

@Composable
fun FilterOverlay(filter: FilterType) {
    val overlayColor = when (filter) {
        FilterType.NONE -> Color.Transparent
        FilterType.BW -> Color(0x33555555)
        FilterType.SEPIA -> Color(0x44D97706) // Vintage Amber
        FilterType.VIVID -> Color(0x228B5CF6) // Neon saturation
        FilterType.COOL -> Color(0x330284C7) // Cinematic Teal
        FilterType.WARM -> Color(0x33F97316) // Sunset Orange
    }
    if (overlayColor != Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
        )
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 100
    return String.format("%02d:%02d.%d", minutes, seconds, millis)
}
