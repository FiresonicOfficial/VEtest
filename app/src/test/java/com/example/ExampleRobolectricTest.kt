package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.VideoClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KlipStudio", appName)
    }

    @Test
    fun `test video clip duration calculation with trimming and speed`() {
        val clip = VideoClip(
            uriString = "content://test/video.mp4",
            title = "Test Clip",
            durationMs = 10000L,
            trimStartMs = 2000L,
            trimEndMs = 6000L,
            playbackSpeed = 2.0f
        )
        // Trimmed duration = (6000 - 2000) / 2.0 = 2000 ms
        assertEquals(2000L, clip.trimmedDurationMs)
    }

    @Test
    fun `test video clip duration calculation with slow motion section`() {
        val clip = VideoClip(
            uriString = "content://test/video.mp4",
            title = "Slow Mo Clip",
            durationMs = 10000L,
            trimStartMs = 0L,
            trimEndMs = 10000L,
            hasSlowMoSection = true,
            slowMoStartMs = 2000L,
            slowMoEndMs = 4000L,
            slowMoSpeed = 0.5f,
            playbackSpeed = 1.0f
        )
        // Before slow: 2000ms
        // Slow section: (4000 - 2000) / 0.5 = 4000ms
        // After slow: 6000ms
        // Total = 2000 + 4000 + 6000 = 12000ms
        assertEquals(12000L, clip.trimmedDurationMs)
    }

    @Test
    fun `test clip split into two contiguous clips`() {
        val originalClip = VideoClip(
            uriString = "content://test/video.mp4",
            title = "Aksiyon Klip",
            durationMs = 8000L,
            trimStartMs = 1000L,
            trimEndMs = 7000L
        )
        val splitTime = 4000L

        val part1 = originalClip.copy(
            trimStartMs = originalClip.trimStartMs,
            trimEndMs = splitTime
        )
        val part2 = originalClip.copy(
            trimStartMs = splitTime,
            trimEndMs = originalClip.trimEndMs
        )

        assertEquals(3000L, part1.trimmedDurationMs)
        assertEquals(3000L, part2.trimmedDurationMs)
        assertEquals(6000L, part1.trimmedDurationMs + part2.trimmedDurationMs)
        assertEquals(originalClip.trimmedDurationMs, part1.trimmedDurationMs + part2.trimmedDurationMs)
    }

    @Test
    fun `test slow-motion section separation produces accurate slow speed duration`() {
        val originalClip = VideoClip(
            uriString = "content://test/video.mp4",
            title = "Ana Klip",
            durationMs = 10000L,
            trimStartMs = 0L,
            trimEndMs = 10000L
        )

        val sStart = 3000L
        val sEnd = 5000L
        val slowSpeed = 0.5f

        val beforeClip = originalClip.copy(trimStartMs = 0L, trimEndMs = sStart)
        val slowClip = originalClip.copy(
            trimStartMs = sStart,
            trimEndMs = sEnd,
            playbackSpeed = slowSpeed
        )
        val afterClip = originalClip.copy(trimStartMs = sEnd, trimEndMs = 10000L)

        assertEquals(3000L, beforeClip.trimmedDurationMs)
        // (5000 - 3000) / 0.5 = 4000ms
        assertEquals(4000L, slowClip.trimmedDurationMs)
        assertEquals(5000L, afterClip.trimmedDurationMs)

        val mergedTotal = listOf(beforeClip, slowClip, afterClip).sumOf { it.trimmedDurationMs }
        assertEquals(12000L, mergedTotal)
    }

    @Test
    fun `test reordering clips in custom sequence`() {
        val clip1 = VideoClip(uriString = "1", title = "Klip 1", durationMs = 3000L)
        val clip2 = VideoClip(uriString = "2", title = "Klip 2", durationMs = 5000L)
        val clip3 = VideoClip(uriString = "3", title = "Klip 3", durationMs = 2000L)

        val list = mutableListOf(clip1, clip2, clip3)
        // Move clip 3 to first position
        val moved = list.removeAt(2)
        list.add(0, moved)

        assertEquals("Klip 3", list[0].title)
        assertEquals("Klip 1", list[1].title)
        assertEquals("Klip 2", list[2].title)
        assertEquals(10000L, list.sumOf { it.trimmedDurationMs })
    }
}
