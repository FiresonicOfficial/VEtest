package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.ProjectEntity
import com.example.data.model.AspectRatioType
import com.example.data.model.FilterType
import com.example.data.model.TextPosition
import com.example.data.model.VideoClip
import com.example.data.repository.ProjectRepository
import com.example.video.SampleVideoGenerator
import com.example.video.VideoProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ExportUiState(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val statusText: String = "",
    val isCompleted: Boolean = false,
    val exportedFileUri: Uri? = null,
    val exportedFilePath: String? = null,
    val errorMessage: String? = null
)

class VideoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository by lazy {
        val db = AppDatabase.getInstance(application)
        ProjectRepository(db.projectDao())
    }

    val savedProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exportedProjects: StateFlow<List<ProjectEntity>> = repository.exportedVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _clips = MutableStateFlow<List<VideoClip>>(emptyList())
    val clips: StateFlow<List<VideoClip>> = _clips.asStateFlow()

    private val _selectedClipIndex = MutableStateFlow(0)
    val selectedClipIndex: StateFlow<Int> = _selectedClipIndex.asStateFlow()

    private val _projectTitle = MutableStateFlow("Yeni Video Projesi")
    val projectTitle: StateFlow<String> = _projectTitle.asStateFlow()

    private val _aspectRatio = MutableStateFlow(AspectRatioType.ORIGINAL)
    val aspectRatio: StateFlow<AspectRatioType> = _aspectRatio.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isGeneratingSample = MutableStateFlow(false)
    val isGeneratingSample: StateFlow<Boolean> = _isGeneratingSample.asStateFlow()

    private val _exportState = MutableStateFlow(ExportUiState())
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    val currentClip: VideoClip?
        get() = _clips.value.getOrNull(_selectedClipIndex.value)

    fun setProjectTitle(title: String) {
        _projectTitle.value = title
    }

    fun selectClip(index: Int) {
        if (index in _clips.value.indices) {
            _selectedClipIndex.value = index
            _playbackPositionMs.value = _clips.value[index].trimStartMs
        }
    }

    fun setAspectRatio(ratio: AspectRatioType) {
        _aspectRatio.value = ratio
    }

    fun setPlaybackPosition(pos: Long) {
        _playbackPositionMs.value = pos
    }

    fun setIsPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun addClipFromUri(uri: Uri, context: Context) {
        viewModelScope.launch {
            val (duration, _) = VideoProcessor.getVideoMetadata(context, uri)
            val clipDuration = if (duration > 0) duration else 5000L
            val clipName = "Klip ${_clips.value.size + 1}"
            val newClip = VideoClip(
                uriString = uri.toString(),
                title = clipName,
                durationMs = clipDuration,
                trimStartMs = 0L,
                trimEndMs = clipDuration,
                slowMoStartMs = (clipDuration * 0.25f).toLong(),
                slowMoEndMs = (clipDuration * 0.75f).toLong()
            )
            _clips.value = _clips.value + newClip
            _selectedClipIndex.value = _clips.value.size - 1
        }
    }

    fun addSampleClip(context: Context, isSecondClip: Boolean = false) {
        viewModelScope.launch {
            _isGeneratingSample.value = true
            try {
                val fileName = if (isSecondClip) "sample_clip_2.mp4" else "sample_clip_1.mp4"
                val themeColor = if (isSecondClip) Color.parseColor("#059669") else Color.parseColor("#4F46E5")
                val title = if (isSecondClip) "Klip 2: Doğa & Aksiyon" else "Klip 1: Giriş Klipi"

                val sampleFile = SampleVideoGenerator.createSampleVideo(
                    context = context,
                    fileName = fileName,
                    themeColor = themeColor,
                    titleText = title
                )

                val uri = Uri.fromFile(sampleFile)
                val duration = 5000L
                val newClip = VideoClip(
                    uriString = uri.toString(),
                    title = title,
                    durationMs = duration,
                    trimStartMs = 0L,
                    trimEndMs = duration,
                    slowMoStartMs = 1500L,
                    slowMoEndMs = 3500L
                )
                _clips.value = _clips.value + newClip
                _selectedClipIndex.value = _clips.value.size - 1
            } finally {
                _isGeneratingSample.value = false
            }
        }
    }

    fun removeClip(index: Int) {
        val currentList = _clips.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _clips.value = currentList
            val newIndex = (index - 1).coerceAtLeast(0)
            _selectedClipIndex.value = if (currentList.isEmpty()) 0 else newIndex
        }
    }

    fun moveClip(fromIndex: Int, toIndex: Int) {
        val list = _clips.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _clips.value = list
            _selectedClipIndex.value = toIndex
        }
    }

    fun splitCurrentClipAtPlayhead(): Boolean {
        val idx = _selectedClipIndex.value
        val clip = _clips.value.getOrNull(idx) ?: return false
        val pos = _playbackPositionMs.value
        return splitClipAt(idx, pos)
    }

    fun splitClipAt(index: Int, splitTimeMs: Long): Boolean {
        val list = _clips.value.toMutableList()
        val clip = list.getOrNull(index) ?: return false

        // Ensure split point is inside trim range with at least 200ms padding on both sides
        val minSplit = clip.trimStartMs + 200L
        val maxSplit = clip.trimEndMs - 200L
        if (minSplit >= maxSplit) return false
        val actualSplit = splitTimeMs.coerceIn(minSplit, maxSplit)

        val part1 = clip.copy(
            id = UUID.randomUUID().toString(),
            title = "${clip.title} (1. Parça)",
            trimStartMs = clip.trimStartMs,
            trimEndMs = actualSplit
        )
        val part2 = clip.copy(
            id = UUID.randomUUID().toString(),
            title = "${clip.title} (2. Parça)",
            trimStartMs = actualSplit,
            trimEndMs = clip.trimEndMs
        )

        list.removeAt(index)
        list.add(index, part2)
        list.add(index, part1)
        _clips.value = list
        _selectedClipIndex.value = index
        _playbackPositionMs.value = part1.trimStartMs
        return true
    }

    fun extractAndSlowDownSegment(
        index: Int,
        segmentStartMs: Long,
        segmentEndMs: Long,
        slowSpeed: Float = 0.5f
    ): Boolean {
        val list = _clips.value.toMutableList()
        val clip = list.getOrNull(index) ?: return false

        val sStart = segmentStartMs.coerceIn(clip.trimStartMs, clip.trimEndMs)
        val sEnd = segmentEndMs.coerceIn(sStart + 200L, clip.trimEndMs)
        if (sEnd <= sStart) return false

        val newClips = mutableListOf<VideoClip>()

        // 1. Part before the slow-motion section (normal speed)
        if (sStart > clip.trimStartMs + 100L) {
            newClips.add(
                clip.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${clip.title} (Öncesi)",
                    trimStartMs = clip.trimStartMs,
                    trimEndMs = sStart
                )
            )
        }

        // 2. The isolated slow-motion segment
        val speedLabel = when (slowSpeed) {
            0.25f -> "0.25x Süper Yavaş"
            0.5f -> "0.5x Ağır Çekim"
            0.75f -> "0.75x Hafif Yavaş"
            else -> "${slowSpeed}x Ağır Çekim"
        }
        val slowClip = clip.copy(
            id = UUID.randomUUID().toString(),
            title = "${clip.title} [$speedLabel]",
            trimStartMs = sStart,
            trimEndMs = sEnd,
            playbackSpeed = slowSpeed
        )
        newClips.add(slowClip)

        // 3. Part after the slow-motion section (normal speed)
        if (clip.trimEndMs > sEnd + 100L) {
            newClips.add(
                clip.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${clip.title} (Sonrası)",
                    trimStartMs = sEnd,
                    trimEndMs = clip.trimEndMs
                )
            )
        }

        list.removeAt(index)
        list.addAll(index, newClips)
        _clips.value = list

        val slowIndex = list.indexOfFirst { it.id == slowClip.id }
        _selectedClipIndex.value = if (slowIndex >= 0) slowIndex else index
        _playbackPositionMs.value = slowClip.trimStartMs
        return true
    }

    fun duplicateClip(index: Int) {
        val list = _clips.value.toMutableList()
        val clip = list.getOrNull(index) ?: return
        val duplicate = clip.copy(
            id = UUID.randomUUID().toString(),
            title = "${clip.title} (Kopya)"
        )
        list.add(index + 1, duplicate)
        _clips.value = list
        _selectedClipIndex.value = index + 1
    }

    fun reorderClips(newOrder: List<VideoClip>) {
        _clips.value = newOrder
        if (_selectedClipIndex.value !in newOrder.indices) {
            _selectedClipIndex.value = 0
        }
    }

    fun updateTrim(startMs: Long, endMs: Long) {
        val idx = _selectedClipIndex.value
        val list = _clips.value.toMutableList()
        if (idx in list.indices) {
            val clip = list[idx]
            val safeStart = startMs.coerceIn(0L, (clip.durationMs - 100L).coerceAtLeast(0L))
            val safeEnd = endMs.coerceIn(safeStart + 100L, clip.durationMs)
            list[idx] = clip.copy(
                trimStartMs = safeStart,
                trimEndMs = safeEnd,
                slowMoStartMs = clip.slowMoStartMs.coerceIn(safeStart, safeEnd),
                slowMoEndMs = clip.slowMoEndMs.coerceIn(safeStart, safeEnd)
            )
            _clips.value = list
        }
    }

    fun updatePlaybackSpeed(speed: Float) {
        val idx = _selectedClipIndex.value
        val list = _clips.value.toMutableList()
        if (idx in list.indices) {
            list[idx] = list[idx].copy(playbackSpeed = speed)
            _clips.value = list
        }
    }

    fun updateSlowMotion(enabled: Boolean, startMs: Long? = null, endMs: Long? = null, speed: Float? = null) {
        val idx = _selectedClipIndex.value
        val list = _clips.value.toMutableList()
        if (idx in list.indices) {
            val clip = list[idx]
            list[idx] = clip.copy(
                hasSlowMoSection = enabled,
                slowMoStartMs = startMs ?: clip.slowMoStartMs,
                slowMoEndMs = endMs ?: clip.slowMoEndMs,
                slowMoSpeed = speed ?: clip.slowMoSpeed
            )
            _clips.value = list
        }
    }

    fun updateVolume(volume: Float, isMuted: Boolean) {
        val idx = _selectedClipIndex.value
        val list = _clips.value.toMutableList()
        if (idx in list.indices) {
            list[idx] = list[idx].copy(volume = volume, isMuted = isMuted)
            _clips.value = list
        }
    }

    fun updateFilter(filter: FilterType) {
        val idx = _selectedClipIndex.value
        val list = _clips.value.toMutableList()
        if (idx in list.indices) {
            list[idx] = list[idx].copy(filterType = filter)
            _clips.value = list
        }
    }

    fun rotateClip() {
        val idx = _selectedClipIndex.value
        val list = _clips.value.toMutableList()
        if (idx in list.indices) {
            val nextRotation = (list[idx].rotationDegrees + 90) % 360
            list[idx] = list[idx].copy(rotationDegrees = nextRotation)
            _clips.value = list
        }
    }

    fun toggleMirror() {
        val idx = _selectedClipIndex.value
        val list = _clips.value.toMutableList()
        if (idx in list.indices) {
            list[idx] = list[idx].copy(isMirrored = !list[idx].isMirrored)
            _clips.value = list
        }
    }

    fun updateTextOverlay(text: String, color: Long = 0xFFFFFFFF, position: TextPosition = TextPosition.BOTTOM) {
        val idx = _selectedClipIndex.value
        val list = _clips.value.toMutableList()
        if (idx in list.indices) {
            list[idx] = list[idx].copy(
                overlayText = text,
                overlayTextColor = color,
                overlayPosition = position
            )
            _clips.value = list
        }
    }

    fun startExport(context: Context) {
        val currentClips = _clips.value
        if (currentClips.isEmpty()) return

        _isPlaying.value = false
        _exportState.value = ExportUiState(
            isExporting = true,
            progress = 0f,
            statusText = "Dışa aktarma başlatılıyor..."
        )

        viewModelScope.launch {
            val title = _projectTitle.value.ifBlank { "KlipStudio_${System.currentTimeMillis()}" }
            val result = VideoProcessor.exportProject(
                context = context,
                clips = currentClips,
                projectTitle = title
            ) { progress, message ->
                _exportState.value = _exportState.value.copy(
                    progress = progress,
                    statusText = message
                )
            }

            if (result.success) {
                _exportState.value = ExportUiState(
                    isExporting = false,
                    isCompleted = true,
                    progress = 1.0f,
                    statusText = "Video başarıyla telefona kaydedildi! (Galeri / Movies)",
                    exportedFileUri = result.contentUri,
                    exportedFilePath = result.filePath
                )

                // Save to Room DB history
                val projectEntity = ProjectEntity(
                    title = title,
                    durationMs = result.durationMs,
                    clipCount = currentClips.size,
                    outputPath = result.filePath,
                    isExported = true,
                    thumbnailUri = currentClips.firstOrNull()?.uriString
                )
                repository.insertProject(projectEntity)
            } else {
                _exportState.value = ExportUiState(
                    isExporting = false,
                    isCompleted = false,
                    errorMessage = result.errorMessage ?: "Dışa aktarma işlemi başarısız oldu."
                )
            }
        }
    }

    fun dismissExport() {
        _exportState.value = ExportUiState()
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProjectById(id)
        }
    }
}
