package com.example.data.model

import java.util.UUID

data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val title: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val playbackSpeed: Float = 1.0f,
    val hasSlowMoSection: Boolean = false,
    val slowMoStartMs: Long = 0L,
    val slowMoEndMs: Long = durationMs,
    val slowMoSpeed: Float = 0.5f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val filterType: FilterType = FilterType.NONE,
    val rotationDegrees: Int = 0,
    val isMirrored: Boolean = false,
    val overlayText: String = "",
    val overlayTextColor: Long = 0xFFFFFFFF,
    val overlayPosition: TextPosition = TextPosition.BOTTOM
) {
    val trimmedDurationMs: Long
        get() {
            val rawDuration = (trimEndMs - trimStartMs).coerceAtLeast(100L)
            return if (hasSlowMoSection) {
                val beforeSlow = (slowMoStartMs - trimStartMs).coerceAtLeast(0L)
                val slowSection = ((slowMoEndMs - slowMoStartMs).coerceAtLeast(0L) / slowMoSpeed.coerceAtLeast(0.1f)).toLong()
                val afterSlow = (trimEndMs - slowMoEndMs).coerceAtLeast(0L)
                ((beforeSlow + slowSection + afterSlow) / playbackSpeed.coerceAtLeast(0.1f)).toLong()
            } else {
                (rawDuration / playbackSpeed.coerceAtLeast(0.1f)).toLong()
            }
        }
}
