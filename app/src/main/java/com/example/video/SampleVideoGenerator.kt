package com.example.video

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SampleVideoGenerator {
    private const val TAG = "SampleVideoGen"

    suspend fun createSampleVideo(
        context: Context,
        fileName: String = "sample_clip_1.mp4",
        themeColor: Int = 0,
        titleText: String = "Klip 1: Aksiyon"
    ): File = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, fileName)
        if (outputFile.exists() && outputFile.length() > 5000) {
            return@withContext outputFile
        }

        try {
            val assetName = if (fileName.contains("2")) {
                "sample_clip_2.mp4"
            } else {
                "sample_clip_1.mp4"
            }

            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(TAG, "Sample video extracted from assets: ${outputFile.length()} bytes")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting sample video from assets", e)
        }

        outputFile
    }
}
