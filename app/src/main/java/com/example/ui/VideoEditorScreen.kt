package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AspectRatioType
import com.example.ui.components.EditorToolPanels
import com.example.ui.components.ExportDialog
import com.example.ui.components.FilterGalleryPanel
import com.example.ui.components.MediaLibraryPanel
import com.example.ui.components.ProjectHistorySheet
import com.example.ui.components.ReorderAndMergeDialog
import com.example.ui.components.StudioTimeline
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.PurpleBackdropEnd
import com.example.ui.theme.PurpleBackdropMid
import com.example.ui.theme.PurpleBackdropStart
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.StudioCornerBracket
import com.example.ui.theme.StudioLilacBorder
import com.example.ui.theme.StudioPurplePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    viewModel: VideoEditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clips by viewModel.clips.collectAsState()
    val selectedIndex by viewModel.selectedClipIndex.collectAsState()
    val projectTitle by viewModel.projectTitle.collectAsState()
    val aspectRatio by viewModel.aspectRatio.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackPosition by viewModel.playbackPositionMs.collectAsState()
    val isGeneratingSample by viewModel.isGeneratingSample.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val exportedProjects by viewModel.exportedProjects.collectAsState()

    var showTitleDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showReorderSheet by remember { mutableStateOf(false) }
    val reorderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Android standard zero-permission Photo/Video Picker (Multiple)
    val multipleVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            viewModel.addClipFromUri(uri, context)
        }
    }

    // Single video picker fallback
    val singleVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.addClipFromUri(it, context) }
    }

    val activeClip = viewModel.currentClip

    // Outer Purple Ambient Glow Backdrop (Matching image.png)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    listOf(PurpleBackdropStart, PurpleBackdropMid, PurpleBackdropEnd)
                )
            )
            .padding(6.dp)
    ) {
        // Rounded Studio Editor Window Container
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Purple Studio App Logo Tile (from image.png)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(StudioPurplePrimary, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Project Title with inline edit
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showTitleDialog = true }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = projectTitle,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Düzenle",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Undo / Redo Icons (from image.png)
                                Row(
                                    modifier = Modifier.padding(start = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = { /* undo */ },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Undo,
                                            contentDescription = "Geri Al",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { /* redo */ },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Redo,
                                            contentDescription = "Yinele",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            // History button with count badge
                            IconButton(
                                onClick = { showHistorySheet = true },
                                modifier = Modifier.testTag("history_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (exportedProjects.isNotEmpty()) {
                                            Badge(containerColor = StudioPurplePrimary) {
                                                Text("${exportedProjects.size}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = "Kaydedilenler",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Primary Signature Purple "Dışa Aktar" Export Button (from image.png)
                            if (clips.isNotEmpty()) {
                                Button(
                                    onClick = { viewModel.startExport(context) },
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .height(34.dp)
                                        .testTag("export_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StudioPurplePrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dışa Aktar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (clips.isEmpty()) {
                        // Empty state: Hero view with quick add options
                        EmptyStateView(
                            isGenerating = isGeneratingSample,
                            onPickVideos = {
                                multipleVideoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            onAddSample = {
                                viewModel.addSampleClip(context, isSecondClip = false)
                            }
                        )
                    } else {
                        // Responsive Layout
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val isWideScreen = maxWidth > 720.dp

                            if (isWideScreen) {
                                // 3-Column Studio Layout matching image.png exactly
                                Row(modifier = Modifier.fillMaxSize()) {
                                    // Left: Media Library Drawer
                                    Box(
                                        modifier = Modifier
                                            .width(220.dp)
                                            .fillMaxHeight()
                                            .border(1.dp, StudioLilacBorder)
                                    ) {
                                        MediaLibraryPanel(
                                            clips = clips,
                                            selectedIndex = selectedIndex,
                                            onSelectClip = { viewModel.selectClip(it) },
                                            onAddVideoClick = {
                                                singleVideoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                )
                                            },
                                            onAddSampleClick = {
                                                viewModel.addSampleClip(context, isSecondClip = true)
                                            }
                                        )
                                    }

                                    // Center: Video Player Preview & Multi-track Timeline
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            VideoPlayerView(
                                                clip = activeClip,
                                                aspectRatio = aspectRatio,
                                                isPlaying = isPlaying,
                                                onPlayPauseToggle = { viewModel.setIsPlaying(it) },
                                                onQuickCutClick = { viewModel.splitCurrentClipAtPlayhead() },
                                                onQuickSpeedClick = { /* speed toggle */ },
                                                onQuickAspectClick = {
                                                    val nextRatio = when (aspectRatio) {
                                                        AspectRatioType.ORIGINAL -> AspectRatioType.RATIO_16_9
                                                        AspectRatioType.RATIO_16_9 -> AspectRatioType.RATIO_9_16
                                                        AspectRatioType.RATIO_9_16 -> AspectRatioType.RATIO_1_1
                                                        AspectRatioType.RATIO_1_1 -> AspectRatioType.ORIGINAL
                                                    }
                                                    viewModel.setAspectRatio(nextRatio)
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        StudioTimeline(
                                            clips = clips,
                                            selectedIndex = selectedIndex,
                                            playbackPositionMs = playbackPosition,
                                            isPlaying = isPlaying,
                                            onPlayPauseToggle = { viewModel.setIsPlaying(it) },
                                            onSelectClip = { viewModel.selectClip(it) },
                                            onMoveClip = { from, to -> viewModel.moveClip(from, to) },
                                            onDeleteClip = { viewModel.removeClip(it) },
                                            onDuplicateClip = { viewModel.duplicateClip(it) },
                                            onSplitClip = { viewModel.splitCurrentClipAtPlayhead() },
                                            onOpenReorderDialog = { showReorderSheet = true },
                                            onAddVideoClick = {
                                                singleVideoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                )
                                            }
                                        )
                                    }

                                    // Right: Filter Presets Gallery (from image.png)
                                    Box(
                                        modifier = Modifier
                                            .width(230.dp)
                                            .fillMaxHeight()
                                            .border(1.dp, StudioLilacBorder)
                                    ) {
                                        FilterGalleryPanel(
                                            currentFilter = activeClip?.filterType ?: com.example.data.model.FilterType.NONE,
                                            onFilterSelect = { viewModel.updateFilter(it) }
                                        )
                                    }
                                }
                            } else {
                                // Phone Vertical Studio Layout
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // 1. Video Player Viewport with signature corner handles & floating toolbar
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        VideoPlayerView(
                                            clip = activeClip,
                                            aspectRatio = aspectRatio,
                                            isPlaying = isPlaying,
                                            onPlayPauseToggle = { viewModel.setIsPlaying(it) },
                                            onQuickCutClick = { viewModel.splitCurrentClipAtPlayhead() },
                                            onQuickSpeedClick = { /* speed */ },
                                            onQuickAspectClick = {
                                                val nextRatio = when (aspectRatio) {
                                                    AspectRatioType.ORIGINAL -> AspectRatioType.RATIO_16_9
                                                    AspectRatioType.RATIO_16_9 -> AspectRatioType.RATIO_9_16
                                                    AspectRatioType.RATIO_9_16 -> AspectRatioType.RATIO_1_1
                                                    AspectRatioType.RATIO_1_1 -> AspectRatioType.ORIGINAL
                                                }
                                                viewModel.setAspectRatio(nextRatio)
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // 2. Multi-track Timeline (ruler, text track, filmstrip clips with slow-mo badges, waveform)
                                    StudioTimeline(
                                        clips = clips,
                                        selectedIndex = selectedIndex,
                                        playbackPositionMs = playbackPosition,
                                        isPlaying = isPlaying,
                                        onPlayPauseToggle = { viewModel.setIsPlaying(it) },
                                        onSelectClip = { viewModel.selectClip(it) },
                                        onMoveClip = { from, to -> viewModel.moveClip(from, to) },
                                        onDeleteClip = { viewModel.removeClip(it) },
                                        onDuplicateClip = { viewModel.duplicateClip(it) },
                                        onSplitClip = { viewModel.splitCurrentClipAtPlayhead() },
                                        onOpenReorderDialog = { showReorderSheet = true },
                                        onAddVideoClick = {
                                            singleVideoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        }
                                    )

                                    // 3. Tabbed Tools Panel (Filtreler 2-column grid, Medya, Hız & Yavaşlat, Kırp, vb.)
                                    activeClip?.let { current ->
                                        EditorToolPanels(
                                            clip = current,
                                            aspectRatio = aspectRatio,
                                            clips = clips,
                                            selectedIndex = selectedIndex,
                                            onSelectClip = { viewModel.selectClip(it) },
                                            onAddVideoClick = {
                                                singleVideoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                )
                                            },
                                            onAddSampleClick = {
                                                viewModel.addSampleClip(context, isSecondClip = true)
                                            },
                                            onTrimChange = { start, end -> viewModel.updateTrim(start, end) },
                                            onSpeedChange = { speed -> viewModel.updatePlaybackSpeed(speed) },
                                            onSlowMoChange = { enabled, start, end, speed ->
                                                viewModel.updateSlowMotion(enabled, start, end, speed)
                                            },
                                            onVolumeChange = { vol, muted -> viewModel.updateVolume(vol, muted) },
                                            onFilterChange = { viewModel.updateFilter(it) },
                                            onRotateClick = { viewModel.rotateClip() },
                                            onMirrorClick = { viewModel.toggleMirror() },
                                            onTextChange = { text, color, pos -> viewModel.updateTextOverlay(text, color, pos) },
                                            onAspectRatioChange = { viewModel.setAspectRatio(it) },
                                            onSplitClick = { viewModel.splitCurrentClipAtPlayhead() },
                                            onExtractSlowMoSegment = { start, end, speed ->
                                                viewModel.extractAndSlowDownSegment(selectedIndex, start, end, speed)
                                            },
                                            onDuplicateClick = { viewModel.duplicateClip(selectedIndex) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Export Dialog (when exporting or completed)
                    if (exportState.isExporting || exportState.isCompleted || exportState.errorMessage != null) {
                        ExportDialog(
                            exportState = exportState,
                            onDismiss = { viewModel.dismissExport() }
                        )
                    }

                    // Reorder and Merge Dialog
                    if (showReorderSheet) {
                        ReorderAndMergeDialog(
                            clips = clips,
                            sheetState = reorderSheetState,
                            onDismiss = { showReorderSheet = false },
                            onMoveClip = { from, to -> viewModel.moveClip(from, to) },
                            onDuplicateClip = { viewModel.duplicateClip(it) },
                            onDeleteClip = { viewModel.removeClip(it) },
                            onStartExport = { viewModel.startExport(context) }
                        )
                    }

                    // Title Edit Dialog
                    if (showTitleDialog) {
                        EditTitleDialog(
                            currentTitle = projectTitle,
                            onConfirm = {
                                viewModel.setProjectTitle(it)
                                showTitleDialog = false
                            },
                            onDismiss = { showTitleDialog = false }
                        )
                    }

                    // Saved Projects & Export History BottomSheet
                    if (showHistorySheet) {
                        ProjectHistorySheet(
                            projects = exportedProjects,
                            sheetState = historySheetState,
                            onDismiss = { showHistorySheet = false },
                            onDeleteProject = { viewModel.deleteProject(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    isGenerating: Boolean,
    onPickVideos: () -> Unit,
    onAddSample: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hero visual banner with signature purple glow
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(StudioPurplePrimary.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(StudioPurplePrimary.copy(alpha = 0.15f), CircleShape)
                    .border(1.5.dp, StudioPurplePrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = StudioPurplePrimary,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "KlipStudio Video Editör",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Videoları kırpın, kesin, dilediğiniz sırada birleştirin,\nistediğiniz kısımları ayırarak ağır çekim yapın!",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPickVideos,
                modifier = Modifier
                    .height(44.dp)
                    .testTag("pick_videos_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioPurplePrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Video Seç", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = onAddSample,
                modifier = Modifier
                    .height(44.dp)
                    .testTag("add_sample_video_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(10.dp),
                enabled = !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = StudioPurplePrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = StudioPurplePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isGenerating) "Yükleniyor..." else "Örnek Video",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun EditTitleDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Proje Başlığını Düzenle",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Proje Adı") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text.trim().ifBlank { currentTitle }) },
                colors = ButtonDefaults.buttonColors(containerColor = StudioPurplePrimary)
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
