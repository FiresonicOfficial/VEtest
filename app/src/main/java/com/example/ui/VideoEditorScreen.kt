package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.example.ui.components.ClipTimeline
import com.example.ui.components.EditorToolPanels
import com.example.ui.components.ExportDialog
import com.example.ui.components.ProjectHistorySheet
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg

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
    val isGeneratingSample by viewModel.isGeneratingSample.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val exportedProjects by viewModel.exportedProjects.collectAsState()

    var showTitleDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showTitleDialog = true }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = projectTitle,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Başlığı Değiştir",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                actions = {
                    // History button with saved videos count badge
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (exportedProjects.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("${exportedProjects.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = "Kaydedilenler",
                                tint = Color.White
                            )
                        }
                    }

                    // Primary Export Action Button
                    if (clips.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.startExport(context) },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(38.dp)
                                .testTag("export_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kaydet", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioDarkBg
                )
            )
        },
        containerColor = StudioDarkBg,
        modifier = modifier.fillMaxSize()
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
                // Editor Main Layout
                Column(modifier = Modifier.fillMaxSize()) {
                    // Video Player Preview Area
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
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Timeline row (Clips for merging & reordering)
                    ClipTimeline(
                        clips = clips,
                        selectedIndex = selectedIndex,
                        onSelectClip = { viewModel.selectClip(it) },
                        onMoveClip = { from, to -> viewModel.moveClip(from, to) },
                        onDeleteClip = { viewModel.removeClip(it) },
                        onAddVideoClick = {
                            singleVideoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        onAddSampleClick = {
                            viewModel.addSampleClip(context, isSecondClip = true)
                        }
                    )

                    // Specialized Editing Tools (Trim, Speed / Slow-Mo, Filters, Audio, Transform, Text, Ratio)
                    activeClip?.let { current ->
                        EditorToolPanels(
                            clip = current,
                            aspectRatio = aspectRatio,
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
                            onAspectRatioChange = { viewModel.setAspectRatio(it) }
                        )
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
        // Hero visual banner
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(PrimaryNeon.copy(alpha = 0.4f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "KlipStudio Video Düzenleyici",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Videolarını kırp, ağır çekim ile yavaşlat,\nbirden fazla klibi birleştir ve telefona kaydet.",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isGenerating) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Örnek video hazırlanıyor...", color = Color.LightGray, fontSize = 13.sp)
        } else {
            // Action 1: Pick from phone
            Button(
                onClick = onPickVideos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("pick_videos_main_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Galeriden Video Seç", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action 2: Start with sample video (perfect for emulators & immediate test!)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddSample() }
                    .testTag("sample_video_card"),
                colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(SecondaryCyan.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Örnek Video İle Başla",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Telefonda video yoksa hemen test et",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
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
    var title by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Proje Başlığı") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Video Adı") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(title.ifBlank { "KlipStudio_Proje" }) }) {
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
