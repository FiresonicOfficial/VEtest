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
}
