package com.example.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.model.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

object VideoProcessor {
    private const val TAG = "VideoProcessor"
    private const val BUFFER_SIZE = 1024 * 1024 // 1 MB buffer

    data class ExportResult(
        val success: Boolean,
        val filePath: String? = null,
        val contentUri: Uri? = null,
        val errorMessage: String? = null,
        val durationMs: Long = 0L
    )

    suspend fun getVideoMetadata(context: Context, uri: Uri): Pair<Long, Bitmap?> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var duration = 0L
        var thumbnail: Bitmap? = null
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            duration = durationStr?.toLongOrNull() ?: 0L
            thumbnail = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
        } catch (e: Exception) {
            Log.e(TAG, "Error getting metadata for $uri", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }
        Pair(duration, thumbnail)
    }

    /**
     * Process a single video clip (trimming, slow-motion retiming, rotation, mute)
     */
    suspend fun processSingleClip(
        context: Context,
        clip: VideoClip,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "processed_${clip.id}.mp4")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val extractor = MediaExtractor()
        val uri = Uri.parse(clip.uriString)
        try {
            if (clip.uriString.startsWith("/")) {
                extractor.setDataSource(clip.uriString)
            } else {
                extractor.setDataSource(context, uri, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Extractor setDataSource failed, trying file descriptor", e)
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                extractor.setDataSource(pfd.fileDescriptor)
                pfd.close()
            }
        }

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        if (clip.rotationDegrees != 0) {
            muxer.setOrientationHint(clip.rotationDegrees)
        }

        val trackMap = mutableMapOf<Int, Int>() // extractorTrackIndex -> muxerTrackIndex
        var videoTrackIndex = -1
        var audioTrackIndex = -1

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("video/")) {
                videoTrackIndex = i
                val muxerTrack = muxer.addTrack(format)
                trackMap[i] = muxerTrack
            } else if (mime.startsWith("audio/") && !clip.isMuted) {
                audioTrackIndex = i
                val muxerTrack = muxer.addTrack(format)
                trackMap[i] = muxerTrack
            }
        }

        var muxerStarted = false
        var samplesWritten = 0

        if (trackMap.isNotEmpty()) {
            try {
                muxer.start()
                muxerStarted = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start muxer", e)
            }
        }

        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
        val bufferInfo = MediaCodec.BufferInfo()

        val startUs = clip.trimStartMs * 1000L
        val endUs = clip.trimEndMs * 1000L
        val slowStartUs = clip.slowMoStartMs * 1000L
        val slowEndUs = clip.slowMoEndMs * 1000L

        // Select all required tracks
        trackMap.keys.forEach { extractor.selectTrack(it) }

        // Seek near trim start
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val firstPtsMap = mutableMapOf<Int, Long>()
        var lastVideoPtsUs = 0L

        try {
            while (muxerStarted) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val muxerTrack = trackMap[trackIndex]
                if (muxerTrack != null) {
                    val sampleTime = extractor.sampleTime

                    if (sampleTime >= startUs) {
                        if (sampleTime > endUs && trackIndex == videoTrackIndex) {
                            // Reached end of trimmed range for video
                            break
                        }

                        buffer.clear()
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize > 0) {
                            val flags = extractor.sampleFlags

                            val firstPts = firstPtsMap.getOrPut(trackIndex) { sampleTime }
                            val relativePts = sampleTime - firstPts

                            // Speed / Slow-mo PTS calculation
                            val adjustedPts: Long = if (trackIndex == videoTrackIndex) {
                                if (clip.hasSlowMoSection && sampleTime in slowStartUs..slowEndUs) {
                                    // Scale time in slow-motion section
                                    val timeBeforeSlow = (slowStartUs - firstPts).coerceAtLeast(0L)
                                    val timeInSlow = (sampleTime - slowStartUs)
                                    val scaledSlow = (timeInSlow / clip.slowMoSpeed).toLong()
                                    ((timeBeforeSlow + scaledSlow) / clip.playbackSpeed).toLong()
                                } else {
                                    (relativePts / clip.playbackSpeed).toLong()
                                }
                            } else {
                                // Audio track: maintain relative timing or silence if speed altered
                                (relativePts / clip.playbackSpeed).toLong()
                            }

                            bufferInfo.offset = 0
                            bufferInfo.size = sampleSize
                            bufferInfo.presentationTimeUs = adjustedPts.coerceAtLeast(0L)
                            bufferInfo.flags = flags

                            if (trackIndex == videoTrackIndex) {
                                lastVideoPtsUs = adjustedPts
                                val range = (endUs - startUs).coerceAtLeast(1L)
                                val progress = ((sampleTime - startUs).toFloat() / range).coerceIn(0f, 1f)
                                onProgress(progress)
                            }

                            try {
                                muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                                samplesWritten++
                            } catch (e: Exception) {
                                Log.w(TAG, "Write sample data error: ${e.message}")
                            }
                        }
                    }
                }

                if (!extractor.advance()) {
                    break
                }
            }
        } finally {
            try {
                if (muxerStarted && samplesWritten > 0) {
                    muxer.stop()
                }
                muxer.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing muxer: ${e.message}")
            }
            try {
                extractor.release()
            } catch (e: Exception) {
                // ignore
            }
        }

        // Fallback: If 0 samples were written, copy original clip file
        if (samplesWritten == 0 || !outputFile.exists() || outputFile.length() < 100) {
            try {
                val sourceFile = if (clip.uriString.startsWith("file://")) {
                    File(Uri.parse(clip.uriString).path ?: "")
                } else if (clip.uriString.startsWith("/")) {
                    File(clip.uriString)
                } else null

                if (sourceFile != null && sourceFile.exists()) {
                    sourceFile.copyTo(outputFile, overwrite = true)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallback copy failed: ${e.message}")
            }
        }

        outputFile
    }

    /**
     * Merge multiple video files into a single continuous MP4 file
     */
    suspend fun mergeVideoFiles(
        context: Context,
        inputFiles: List<File>,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (inputFiles.isEmpty()) return@withContext false
        if (inputFiles.size == 1) {
            inputFiles[0].copyTo(outputFile, overwrite = true)
            return@withContext true
        }

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        var samplesWritten = 0
        var videoMuxerTrack = -1
        var audioMuxerTrack = -1

        // Use first clip format as baseline
        val firstExtractor = MediaExtractor()
        try {
            firstExtractor.setDataSource(inputFiles[0].absolutePath)
            for (i in 0 until firstExtractor.trackCount) {
                val format = firstExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") && videoMuxerTrack < 0) {
                    videoMuxerTrack = muxer.addTrack(format)
                } else if (mime.startsWith("audio/") && audioMuxerTrack < 0) {
                    audioMuxerTrack = muxer.addTrack(format)
                }
            }
        } finally {
            firstExtractor.release()
        }

        if (videoMuxerTrack < 0) return@withContext false

        muxer.start()
        muxerStarted = true

        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
        val bufferInfo = MediaCodec.BufferInfo()

        var videoPtsOffsetUs = 0L
        var audioPtsOffsetUs = 0L

        for (clipIndex in inputFiles.indices) {
            val file = inputFiles[clipIndex]
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(file.absolutePath)
                var sourceVideoTrack = -1
                var sourceAudioTrack = -1

                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("video/") && sourceVideoTrack < 0) {
                        sourceVideoTrack = i
                        extractor.selectTrack(i)
                    } else if (mime.startsWith("audio/") && sourceAudioTrack < 0 && audioMuxerTrack >= 0) {
                        sourceAudioTrack = i
                        extractor.selectTrack(i)
                    }
                }

                var maxVideoPtsInClip = 0L
                var maxAudioPtsInClip = 0L

                while (true) {
                    val trackIndex = extractor.sampleTrackIndex
                    if (trackIndex < 0) break

                    val isVideo = trackIndex == sourceVideoTrack
                    val isAudio = trackIndex == sourceAudioTrack

                    if (isVideo || isAudio) {
                        buffer.clear()
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize > 0) {
                            val pts = extractor.sampleTime
                            val targetTrack = if (isVideo) videoMuxerTrack else audioMuxerTrack
                            val offset = if (isVideo) videoPtsOffsetUs else audioPtsOffsetUs

                            val adjustedPts = offset + pts
                            if (isVideo) maxVideoPtsInClip = maxVideoPtsInClip.coerceAtLeast(pts)
                            if (isAudio) maxAudioPtsInClip = maxAudioPtsInClip.coerceAtLeast(pts)

                            bufferInfo.offset = 0
                            bufferInfo.size = sampleSize
                            bufferInfo.presentationTimeUs = adjustedPts
                            bufferInfo.flags = extractor.sampleFlags

                            try {
                                muxer.writeSampleData(targetTrack, buffer, bufferInfo)
                                samplesWritten++
                            } catch (e: Exception) {
                                Log.w(TAG, "Merge write sample error: ${e.message}")
                            }
                        }
                    }

                    if (!extractor.advance()) break
                }

                // Update offsets for next clip (add 33ms buffer for frame interval)
                videoPtsOffsetUs += maxVideoPtsInClip + 33_333L
                audioPtsOffsetUs += maxAudioPtsInClip + 33_333L

                val totalProgress = (clipIndex + 1).toFloat() / inputFiles.size
                onProgress(totalProgress)

            } finally {
                extractor.release()
            }
        }

        try {
            if (muxerStarted && samplesWritten > 0) {
                muxer.stop()
            }
            muxer.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping merge muxer", e)
        }

        true
    }

    /**
     * Saves exported video to Android Gallery (MediaStore Movies folder)
     */
    suspend fun saveVideoToGallery(
        context: Context,
        sourceFile: File,
        title: String = "KlipStudio_Video_${System.currentTimeMillis()}"
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$title.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/KlipStudio")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        var uri: Uri? = null
        try {
            uri = resolver.insert(collection, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save video to MediaStore", e)
            if (uri != null) {
                try {
                    resolver.delete(uri, null, null)
                } catch (delEx: Exception) {
                    // ignore
                }
                uri = null
            }
        }

        uri
    }

    /**
     * Executes the full export pipeline for a list of clips:
     * 1. Processes each clip (trim, slow-motion speed, mute, rotate)
     * 2. Merges if multiple clips
     * 3. Exports to Phone Gallery (MediaStore)
     */
    suspend fun exportProject(
        context: Context,
        clips: List<VideoClip>,
        projectTitle: String,
        onProgress: (Float, String) -> Unit
    ): ExportResult = withContext(Dispatchers.IO) {
        if (clips.isEmpty()) {
            return@withContext ExportResult(false, errorMessage = "Düzenlenecek video klibi bulunamadı.")
        }

        try {
            onProgress(0.05f, "Klipler hazırlanıyor...")
            val processedFiles = mutableListOf<File>()

            for (i in clips.indices) {
                val clip = clips[i]
                onProgress(
                    0.05f + (0.50f * (i.toFloat() / clips.size)),
                    "Klip işleniyor (${i + 1}/${clips.size}): Kırpma & Hız"
                )
                val processedFile = processSingleClip(context, clip) { clipProgress ->
                    val overallProgress = 0.05f + 0.50f * ((i + clipProgress) / clips.size)
                    onProgress(overallProgress, "Klip ${i + 1}/${clips.size} işleniyor (%${(clipProgress * 100).toInt()})")
                }
                processedFiles.add(processedFile)
            }

            val finalFile: File
            if (processedFiles.size == 1) {
                finalFile = processedFiles[0]
            } else {
                onProgress(0.60f, "Klipler birleştiriliyor...")
                val mergedFile = File(context.cacheDir, "merged_export_${System.currentTimeMillis()}.mp4")
                val mergeSuccess = mergeVideoFiles(context, processedFiles, mergedFile) { mergeProgress ->
                    val p = 0.60f + 0.25f * mergeProgress
                    onProgress(p, "Klipler birleştiriliyor (%${(mergeProgress * 100).toInt()})")
                }
                if (!mergeSuccess) {
                    return@withContext ExportResult(false, errorMessage = "Klipler birleştirilirken hata oluştu.")
                }
                finalFile = mergedFile
            }

            onProgress(0.90f, "Telefona kaydediliyor (Galeri / Movies)...")
            val sanitizedTitle = projectTitle.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val contentUri = saveVideoToGallery(context, finalFile, sanitizedTitle)

            onProgress(1.0f, "Tamamlandı!")

            ExportResult(
                success = true,
                filePath = finalFile.absolutePath,
                contentUri = contentUri,
                durationMs = clips.sumOf { it.trimmedDurationMs }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Export project failed", e)
            ExportResult(false, errorMessage = e.localizedMessage ?: "Bilinmeyen bir hata oluştu.")
        }
    }
}
