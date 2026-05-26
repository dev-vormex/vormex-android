@file:androidx.media3.common.util.UnstableApi

package com.kyant.backdrop.catalog.linkedin.reels

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.catalog.network.models.Reel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.Locale
import kotlin.math.roundToLong

private const val MaxHashtags = 10
private const val MaxReelBytes = 150L * 1024L * 1024L
private const val MinTrimWindowFraction = 0.05f
private val ReelCreateBlack = Color(0xFF050505)
private val ReelCreateSurface = Color(0xFF121212)
private val ReelCreateSurfaceHigh = Color(0xFF1C1C1E)
private val ReelCreateDivider = Color.White.copy(alpha = 0.12f)
private val ReelCreateBlue = Color(0xFF0095F6)
private val ReelCreateMuted = Color.White.copy(alpha = 0.62f)

data class ReelUploadMedia(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long?
)

data class ReelUploadForm(
    val video: ReelUploadMedia,
    val thumbnail: ReelUploadMedia?,
    val title: String,
    val caption: String,
    val hashtags: List<String>,
    val category: String?,
    val visibility: String,
    val allowComments: Boolean,
    val allowDuets: Boolean,
    val allowStitch: Boolean,
    val allowDownload: Boolean,
    val allowSharing: Boolean,
    val muteOriginalAudio: Boolean,
    val saveAsDraft: Boolean
)

private data class ReelVideoEditState(
    val trimStartFraction: Float = 0f,
    val trimEndFraction: Float = 1f,
    val coverFrameFraction: Float = 0.18f
) {
    val hasTrimEdit: Boolean
        get() = trimStartFraction > 0.01f || trimEndFraction < 0.99f

    fun normalized(): ReelVideoEditState {
        val start = trimStartFraction.coerceIn(0f, 1f - MinTrimWindowFraction)
        val end = trimEndFraction.coerceIn(start + MinTrimWindowFraction, 1f)
        return copy(
            trimStartFraction = start,
            trimEndFraction = end,
            coverFrameFraction = coverFrameFraction.coerceIn(0f, 1f)
        )
    }
}

private enum class ReelEditorTool(
    val label: String,
    val icon: ImageVector
) {
    Trim("Split", Icons.Outlined.ContentCut),
    Crop("Crop", Icons.Outlined.Crop),
    Audio("Audio", Icons.Outlined.MusicNote),
    Text("Text", Icons.Outlined.TextFields),
    Voice("Voice", Icons.Outlined.Mic),
    Captions("Captions", Icons.Outlined.ClosedCaption),
    Overlay("Overlay", Icons.Outlined.Layers),
    Adjust("Adjust", Icons.Outlined.Tune),
    Cover("Cover", Icons.Outlined.Image)
}

private enum class ReelCropMode(val label: String) {
    Fit("Fit"),
    Fill("Fill"),
    Square("1:1")
}

private data class ReelTimelineEditorState(
    val splitFractions: List<Float> = emptyList(),
    val cropMode: ReelCropMode = ReelCropMode.Fit,
    val mirrored: Boolean = false,
    val textOverlay: String = "",
    val voiceoverEnabled: Boolean = false,
    val captionsEnabled: Boolean = false,
    val overlayEnabled: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 1f
)

private val ReelTimelineEditorState.hasTextOverlay: Boolean
    get() = textOverlay.isNotBlank()

private val ReelTimelineEditorState.hasVisualOverlay: Boolean
    get() = hasTextOverlay || captionsEnabled || overlayEnabled || brightness != 0f || contrast != 1f || mirrored || cropMode != ReelCropMode.Fit

private val ReelEditorTool.isAvailableForDock: Boolean
    get() = this != ReelEditorTool.Cover

private fun ReelTimelineEditorState.withSplitAt(fraction: Float): ReelTimelineEditorState {
    val safeFraction = fraction.coerceIn(0.08f, 0.92f)
    if (splitFractions.any { kotlin.math.abs(it - safeFraction) < 0.025f }) {
        return this
    }
    return copy(splitFractions = (splitFractions + safeFraction).sorted())
}

private data class ReelDockAction(
    val label: String,
    val icon: ImageVector,
    val tool: ReelEditorTool
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelCreateSheet(
    contentColor: Color,
    accentColor: Color,
    isUploading: Boolean,
    error: String?,
    drafts: List<Reel> = emptyList(),
    isLoadingDrafts: Boolean = false,
    draftsError: String? = null,
    publishingDraftId: String? = null,
    startWithDraftLibrary: Boolean = false,
    onDismiss: () -> Unit,
    onLoadDrafts: () -> Unit = {},
    onPreviewDraft: (Reel) -> Unit = {},
    onPublishDraft: (String) -> Unit = {},
    onSubmit: (ReelUploadForm) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedVideo by remember { mutableStateOf<ReelUploadMedia?>(null) }
    var selectedThumbnail by remember { mutableStateOf<ReelUploadMedia?>(null) }
    var videoEditState by remember { mutableStateOf(ReelVideoEditState()) }
    var timelineEditorState by remember { mutableStateOf(ReelTimelineEditorState()) }
    var videoDurationMs by remember { mutableLongStateOf(0L) }
    var isRenderingVideoEdit by remember { mutableStateOf(false) }
    var editorError by remember { mutableStateOf<String?>(null) }
    var showVideoEditor by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var hashtagInput by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf<List<String>>(emptyList()) }
    var category by remember { mutableStateOf<String?>(null) }
    var visibility by remember { mutableStateOf("public") }
    var allowComments by remember { mutableStateOf(true) }
    var allowDuets by remember { mutableStateOf(true) }
    var allowStitch by remember { mutableStateOf(true) }
    var allowDownload by remember { mutableStateOf(true) }
    var allowSharing by remember { mutableStateOf(true) }
    var muteOriginalAudio by remember { mutableStateOf(false) }
    var saveAsDraft by remember { mutableStateOf(false) }
    var showDraftLibrary by remember(startWithDraftLibrary) { mutableStateOf(startWithDraftLibrary) }

    LaunchedEffect(showDraftLibrary) {
        if (showDraftLibrary) {
            onLoadDrafts()
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedVideo = uri?.toReelUploadMedia(context, "reel.mp4", "video/mp4")
        selectedThumbnail = null
        videoEditState = ReelVideoEditState()
        timelineEditorState = ReelTimelineEditorState()
        videoDurationMs = 0L
        editorError = null
        showVideoEditor = false
    }
    val thumbnailPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedThumbnail = uri?.toReelUploadMedia(context, "thumbnail.jpg", "image/jpeg")
        editorError = null
    }

    val videoTooLarge = selectedVideo?.sizeBytes?.let { it > MaxReelBytes } == true
    val canSubmit = selectedVideo != null &&
        (!videoTooLarge || videoEditState.hasTrimEdit) &&
        !isUploading &&
        !isRenderingVideoEdit
    val publishLabel = if (saveAsDraft) "Save draft" else "Share"
    val statusLabel = when {
        isUploading -> "Uploading"
        isRenderingVideoEdit -> "Preparing edit"
        videoTooLarge && !videoEditState.hasTrimEdit -> "File too large"
        videoTooLarge -> "Trim to share"
        selectedVideo != null -> "Ready to share"
        else -> "Select a video"
    }

    fun addHashtag() {
        val normalized = hashtagInput
            .trim()
            .removePrefix("#")
            .lowercase()
            .filter { it.isLetterOrDigit() || it == '_' }
        if (normalized.isNotEmpty() && normalized !in hashtags && hashtags.size < MaxHashtags) {
            hashtags = hashtags + normalized
            hashtagInput = ""
        }
    }

    fun useSelectedCoverFrame(video: ReelUploadMedia) {
        coroutineScope.launch {
            editorError = null
            isRenderingVideoEdit = true
            val result = runCatching {
                createReelCoverFromFrame(
                    context = context,
                    video = video,
                    editState = videoEditState.normalized()
                )
            }
            isRenderingVideoEdit = false
            result
                .onSuccess { selectedThumbnail = it }
                .onFailure { throwable ->
                    editorError = throwable.message ?: "Could not create cover frame"
                }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isUploading) onDismiss() },
        containerColor = ReelCreateBlack,
        contentColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .background(ReelCreateBlack)
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReelComposerHeader(
                status = statusLabel,
                contentColor = Color.White,
                accentColor = ReelCreateBlue,
                onDismiss = onDismiss,
                isUploading = isUploading
            )

            ReelMediaStudio(
                video = selectedVideo,
                thumbnail = selectedThumbnail,
                editState = videoEditState,
                videoTooLarge = videoTooLarge,
                contentColor = Color.White,
                accentColor = ReelCreateBlue,
                muteOriginalAudio = muteOriginalAudio || showVideoEditor,
                enabled = !isUploading,
                onPickVideo = { videoPicker.launch("video/*") },
                onPickThumbnail = { thumbnailPicker.launch("image/*") },
                onOpenEditor = { showVideoEditor = true },
                onDurationAvailable = { durationMs ->
                    if (durationMs > 0L) {
                        videoDurationMs = durationMs
                    }
                }
            )

            ReelComposerSection(
                title = "Details",
                contentColor = Color.White
            ) {
                ReelTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = "Title",
                    helper = "${title.length}/120",
                    enabled = !isUploading,
                    singleLine = true,
                    contentColor = Color.White,
                    accentColor = ReelCreateBlue
                )

                HashtagComposer(
                    hashtagInput = hashtagInput,
                    hashtags = hashtags,
                    enabled = !isUploading,
                    accentColor = ReelCreateBlue,
                    contentColor = Color.White,
                    onInputChange = { hashtagInput = it.take(32) },
                    onAdd = { addHashtag() },
                    onRemove = { tag -> hashtags = hashtags - tag }
                )
            }

            ReelComposerSection(
                title = "Share to",
                contentColor = Color.White
            ) {
                ChoiceSection(
                    title = "Visibility",
                    options = listOf("public", "connections", "private"),
                    selected = visibility,
                    contentColor = Color.White,
                    accentColor = ReelCreateBlue,
                    enabled = !isUploading,
                    onSelected = { visibility = it }
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReelSwitchRow("Comments", "Thread replies", allowComments, !isUploading, Color.White, ReelCreateBlue) { allowComments = it }
                    ReelSwitchRow("Duet", "Side-by-side responses", allowDuets, !isUploading, Color.White, ReelCreateBlue) { allowDuets = it }
                    ReelSwitchRow("Stitch", "Clip reuse", allowStitch, !isUploading, Color.White, ReelCreateBlue) { allowStitch = it }
                    ReelSwitchRow("Downloads", "Video saves", allowDownload, !isUploading, Color.White, ReelCreateBlue) { allowDownload = it }
                    ReelSwitchRow("Sharing", "In-app shares", allowSharing, !isUploading, Color.White, ReelCreateBlue) { allowSharing = it }
                }
            }

            DraftLibraryRow(
                draftCount = drafts.size,
                isExpanded = showDraftLibrary,
                isLoading = isLoadingDrafts,
                enabled = !isUploading && publishingDraftId == null,
                contentColor = Color.White,
                accentColor = ReelCreateBlue,
                onClick = {
                    showDraftLibrary = !showDraftLibrary
                }
            )

            if (showDraftLibrary) {
                ReelDraftsPanel(
                    drafts = drafts,
                    isLoading = isLoadingDrafts,
                    error = draftsError,
                    publishingDraftId = publishingDraftId,
                    enabled = !isUploading,
                    contentColor = Color.White,
                    accentColor = ReelCreateBlue,
                    onRefresh = onLoadDrafts,
                    onPreview = onPreviewDraft,
                    onPublish = onPublishDraft
                )
            }

            DraftPreferenceRow(
                saveAsDraft = saveAsDraft,
                enabled = !isUploading,
                contentColor = Color.White,
                accentColor = ReelCreateBlue,
                onCheckedChange = { saveAsDraft = it }
            )

            val composerError = editorError ?: error
            if (composerError != null) {
                Text(
                    text = composerError,
                    color = Color(0xFFDC2626),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFEBEE))
                        .padding(12.dp)
                )
            }

            ReelPublishBar(
                label = publishLabel,
                enabled = canSubmit,
                isUploading = isUploading,
                contentColor = Color.White,
                accentColor = ReelCreateBlue,
                onClick = {
                    val video = selectedVideo ?: return@ReelPublishBar
                    coroutineScope.launch {
                        editorError = null
                        val uploadVideo = if (videoEditState.hasTrimEdit) {
                            isRenderingVideoEdit = true
                            val rendered = runCatching {
                                renderTrimmedReelVideo(
                                    context = context,
                                    media = video,
                                    editState = videoEditState.normalized(),
                                    muteOriginalAudio = muteOriginalAudio
                                )
                            }
                            isRenderingVideoEdit = false
                            rendered.getOrElse { throwable ->
                                editorError = throwable.message ?: "Could not prepare edited video"
                                return@launch
                            }
                        } else {
                            video
                        }

                        if (uploadVideo.sizeBytes?.let { it > MaxReelBytes } == true) {
                            editorError = "Edited video is still larger than 150 MB"
                            return@launch
                        }

                        onSubmit(
                            ReelUploadForm(
                                video = uploadVideo,
                                thumbnail = selectedThumbnail,
                                title = title,
                                caption = caption,
                                hashtags = hashtags,
                                category = category,
                                visibility = visibility,
                                allowComments = allowComments,
                                allowDuets = allowDuets,
                                allowStitch = allowStitch,
                                allowDownload = allowDownload,
                                allowSharing = allowSharing,
                                muteOriginalAudio = muteOriginalAudio,
                                saveAsDraft = saveAsDraft
                            )
                        )
                    }
                }
            )
        }
    }

    selectedVideo?.let { video ->
        if (showVideoEditor) {
            ReelFullScreenVideoEditor(
                video = video,
                editState = videoEditState,
                editorState = timelineEditorState,
                durationMs = videoDurationMs,
                muteOriginalAudio = muteOriginalAudio,
                isRendering = isRenderingVideoEdit,
                enabled = !isUploading,
                contentColor = Color.White,
                accentColor = ReelCreateBlue,
                onDismiss = { showVideoEditor = false },
                onDone = { showVideoEditor = false },
                onEditChange = {
                    videoEditState = it.normalized()
                    editorError = null
                },
                onEditorStateChange = {
                    timelineEditorState = it
                    editorError = null
                },
                onMuteOriginalAudioChange = {
                    muteOriginalAudio = it
                    editorError = null
                },
                onUseCoverFrame = { useSelectedCoverFrame(video) },
                onDurationAvailable = { durationMs ->
                    if (durationMs > 0L) {
                        videoDurationMs = durationMs
                    }
                }
            )
        }
    }
}

@Composable
private fun ReelComposerHeader(
    status: String,
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    isUploading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onDismiss, enabled = !isUploading) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close",
                tint = contentColor.copy(alpha = if (isUploading) 0.34f else 0.92f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "New reel",
                color = contentColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = status,
                color = if (status == "File too large") Color(0xFFFF453A) else ReelCreateMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun ReelMediaStudio(
    video: ReelUploadMedia?,
    thumbnail: ReelUploadMedia?,
    editState: ReelVideoEditState,
    videoTooLarge: Boolean,
    contentColor: Color,
    accentColor: Color,
    muteOriginalAudio: Boolean,
    enabled: Boolean,
    onPickVideo: () -> Unit,
    onPickThumbnail: () -> Unit,
    onOpenEditor: () -> Unit,
    onDurationAvailable: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ReelPreviewFrame(
            video = video,
            thumbnail = thumbnail,
            editState = editState,
            videoTooLarge = videoTooLarge,
            accentColor = accentColor,
            muteOriginalAudio = muteOriginalAudio,
            enabled = enabled,
            onPickVideo = onPickVideo,
            onOpenEditor = onOpenEditor,
            onDurationAvailable = onDurationAvailable
        )

        Text(
            text = video?.fileName ?: "Select a video from your gallery",
            color = if (video == null) ReelCreateMuted else contentColor.copy(alpha = 0.82f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (video != null) {
            ReelMediaAction(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.Image,
                label = if (thumbnail == null) "Add thumbnail" else "Change thumbnail",
                detail = thumbnail?.let { mediaDetail(it) } ?: "Optional cover image",
                accentColor = accentColor,
                contentColor = contentColor,
                enabled = enabled,
                onClick = onPickThumbnail
            )
        }

        if (thumbnail != null) {
            ReelThumbnailPreviewCard(
                thumbnail = thumbnail,
                contentColor = contentColor,
                accentColor = accentColor,
                enabled = enabled,
                onChangeThumbnail = onPickThumbnail
            )
        }

        if (videoTooLarge) {
            ReelWarningText("Maximum video size is 150 MB")
        }
    }
}

@Composable
private fun ReelThumbnailPreviewCard(
    thumbnail: ReelUploadMedia,
    contentColor: Color,
    accentColor: Color,
    enabled: Boolean,
    onChangeThumbnail: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ReelCreateSurface)
            .border(1.dp, ReelCreateDivider, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onChangeThumbnail)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(thumbnail.uri).crossfade(true).build(),
            contentDescription = "Thumbnail preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(72.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Thumbnail preview",
                color = contentColor.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = thumbnail.fileName,
                color = contentColor.copy(alpha = 0.54f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Tap to change",
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ReelPreviewFrame(
    video: ReelUploadMedia?,
    thumbnail: ReelUploadMedia?,
    editState: ReelVideoEditState,
    videoTooLarge: Boolean,
    accentColor: Color,
    muteOriginalAudio: Boolean,
    enabled: Boolean,
    onPickVideo: () -> Unit,
    onOpenEditor: () -> Unit,
    onDurationAvailable: (Long) -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(22.dp)
    val previewGestureModifier = if (video == null) {
        Modifier.clickable(enabled = enabled, onClick = onPickVideo)
    } else {
        Modifier.pointerInput(enabled, video.uri) {
            var dragDistance = 0f
            detectVerticalDragGestures(
                onDragStart = { dragDistance = 0f },
                onVerticalDrag = { _, dragAmount ->
                    if (!enabled) return@detectVerticalDragGestures
                    dragDistance += dragAmount
                    if (dragDistance > 72f) {
                        dragDistance = 0f
                        onOpenEditor()
                    }
                },
                onDragEnd = { dragDistance = 0f },
                onDragCancel = { dragDistance = 0f }
            )
        }
    }

    Box(
        modifier = Modifier
            .width(198.dp)
            .height(352.dp)
            .clip(shape)
            .background(Color(0xFF10131A))
            .border(
                width = 1.dp,
                color = if (videoTooLarge) Color(0xFFDC2626).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.18f),
                shape = shape
            )
            .then(previewGestureModifier),
        contentAlignment = Alignment.Center
    ) {
        if (video != null) {
            LocalReelVideoPreview(
                media = video,
                muted = muteOriginalAudio,
                editState = editState,
                onDurationAvailable = onDurationAvailable,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (thumbnail != null && video == null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(thumbnail.uri).crossfade(true).build(),
                contentDescription = "Cover preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
            )
        } else if (video == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E293B),
                                accentColor.copy(alpha = 0.62f)
                            )
                        )
                    )
            )
        }

        if (video == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Text(
                    text = "9:16",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (video != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.48f))
                    .clickable(enabled = enabled, onClick = onOpenEditor)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCut,
                    contentDescription = "Edit video",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Edit video",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = video.fileName,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (videoTooLarge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFFDC2626))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Too large",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun LocalReelVideoPreview(
    media: ReelUploadMedia,
    muted: Boolean,
    editState: ReelVideoEditState,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    onDurationAvailable: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLoading by remember(media.uri) { mutableStateOf(true) }
    var hasError by remember(media.uri) { mutableStateOf(false) }
    var isPlaying by remember(media.uri) { mutableStateOf(true) }
    var durationMs by remember(media.uri) { mutableLongStateOf(0L) }

    val player = remember(media.uri) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(media.uri))
            prepare()
            playWhenReady = true
            volume = if (muted) 0f else 1f
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isLoading = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                    if (playbackState == Player.STATE_READY) {
                        hasError = false
                        val mediaDuration = this@apply.duration.takeIf {
                            it > 0L && it != androidx.media3.common.C.TIME_UNSET
                        } ?: 0L
                        if (mediaDuration > 0L) {
                            durationMs = mediaDuration
                            onDurationAvailable(mediaDuration)
                        }
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    hasError = true
                    isLoading = false
                }
            })
        }
    }

    LaunchedEffect(player, muted) {
        player.volume = if (muted) 0f else 1f
    }

    LaunchedEffect(player, media.uri, editState.trimStartFraction, editState.trimEndFraction, durationMs) {
        val knownDuration = durationMs.takeIf { it > 0L } ?: player.duration.takeIf {
            it > 0L && it != androidx.media3.common.C.TIME_UNSET
        } ?: 0L
        if (knownDuration <= 0L) return@LaunchedEffect
        val trim = editState.normalized()
        val startMs = (knownDuration * trim.trimStartFraction).roundToLong()
        player.seekTo(startMs)
    }

    LaunchedEffect(player, media.uri, editState.trimStartFraction, editState.trimEndFraction, durationMs) {
        while (true) {
            delay(180)
            val knownDuration = durationMs.takeIf { it > 0L } ?: player.duration.takeIf {
                it > 0L && it != androidx.media3.common.C.TIME_UNSET
            } ?: continue
            val trim = editState.normalized()
            val startMs = (knownDuration * trim.trimStartFraction).roundToLong()
            val endMs = (knownDuration * trim.trimEndFraction).roundToLong()
            if (endMs > startMs && player.currentPosition >= endMs) {
                player.seekTo(startMs)
                if (isPlaying) player.play()
            }
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> if (isPlaying) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.resizeMode = resizeMode
                    setKeepContentOnPlayerReset(true)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                if (view.player !== player) {
                    view.player = player
                }
                view.resizeMode = resizeMode
            },
            onRelease = { view ->
                view.player = null
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        }

        if (!isPlaying && !isLoading && !hasError) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.52f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = "Play preview",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        if (hasError) {
            Text(
                text = "Preview unavailable",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun ReelEditorPreviewOverlays(
    editorState: ReelTimelineEditorState,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (editorState.brightness != 0f) {
            val overlayColor = if (editorState.brightness > 0f) Color.White else Color.Black
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor.copy(alpha = kotlin.math.abs(editorState.brightness) * 0.28f))
            )
        }

        if (editorState.contrast > 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (editorState.contrast - 1f) * 0.12f))
            )
        }

        if (editorState.overlayEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFFFD400), accentColor)))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "VORMEX",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        if (editorState.captionsEnabled) {
            Text(
                text = "Auto caption preview",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.56f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        if (editorState.hasTextOverlay) {
            Text(
                text = editorState.textOverlay,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.34f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        if (editorState.voiceoverEnabled) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFFF2FC3).copy(alpha = 0.9f))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Voice",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReelFullScreenVideoEditor(
    video: ReelUploadMedia,
    editState: ReelVideoEditState,
    editorState: ReelTimelineEditorState,
    durationMs: Long,
    muteOriginalAudio: Boolean,
    isRendering: Boolean,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
    onEditChange: (ReelVideoEditState) -> Unit,
    onEditorStateChange: (ReelTimelineEditorState) -> Unit,
    onMuteOriginalAudioChange: (Boolean) -> Unit,
    onUseCoverFrame: () -> Unit,
    onDurationAvailable: (Long) -> Unit
) {
    var selectedTool by remember(video.uri) { mutableStateOf(ReelEditorTool.Trim) }
    val controlsEnabled = enabled && !isRendering
    val trim = editState.normalized()
    val trimStartMs = editorPositionMs(durationMs, trim.trimStartFraction)
    val trimEndMs = editorPositionMs(durationMs, trim.trimEndFraction)
    val coverMs = editorPositionMs(
        durationMs,
        trim.trimStartFraction + ((trim.trimEndFraction - trim.trimStartFraction) * trim.coverFrameFraction)
    )
    val previewAspect = if (editorState.cropMode == ReelCropMode.Square) 1f else 9f / 16f
    val previewWidth = if (editorState.cropMode == ReelCropMode.Square) 236.dp else 210.dp
    val previewResizeMode = when (editorState.cropMode) {
        ReelCropMode.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        ReelCropMode.Fill,
        ReelCropMode.Square -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }
    val projectTitle = remember(video.fileName) {
        video.fileName.substringBeforeLast('.').ifBlank { "Vormex edit" }.take(18)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B10))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            ReelEditorTopBar(
                title = projectTitle,
                enabled = controlsEnabled,
                contentColor = contentColor,
                accentColor = accentColor,
                onDismiss = onDismiss,
                onDone = onDone
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(previewWidth)
                        .aspectRatio(previewAspect)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                        .border(
                            width = if (selectedTool == ReelEditorTool.Crop || selectedTool == ReelEditorTool.Cover) 2.dp else 1.dp,
                            color = if (selectedTool == ReelEditorTool.Crop || selectedTool == ReelEditorTool.Cover) Color(0xFFFFD400) else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    LocalReelVideoPreview(
                        media = video,
                        muted = muteOriginalAudio,
                        editState = trim,
                        resizeMode = previewResizeMode,
                        onDurationAvailable = onDurationAvailable,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = if (editorState.mirrored) -1f else 1f
                            }
                    )

                    ReelEditorPreviewOverlays(
                        editorState = editorState,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            ReelEditorTransportRow(
                durationMs = durationMs,
                playheadMs = trimStartMs,
                contentColor = contentColor,
                accentColor = accentColor,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            ReelInstagramTimelinePanel(
                selectedTool = selectedTool,
                editState = trim,
                durationMs = durationMs,
                startMs = trimStartMs,
                endMs = trimEndMs,
                coverMs = coverMs,
                editorState = editorState,
                muteOriginalAudio = muteOriginalAudio,
                isRendering = isRendering,
                enabled = controlsEnabled,
                contentColor = contentColor,
                accentColor = accentColor,
                onEditChange = onEditChange,
                onEditorStateChange = onEditorStateChange,
                onMuteOriginalAudioChange = onMuteOriginalAudioChange,
                onUseCoverFrame = onUseCoverFrame
            )

            ReelEditorDock(
                selectedTool = selectedTool,
                enabled = controlsEnabled,
                contentColor = contentColor,
                accentColor = accentColor,
                onSelectedTool = { tool ->
                    selectedTool = tool
                    if (tool == ReelEditorTool.Trim) {
                        onEditorStateChange(editorState.withSplitAt(0.5f))
                    }
                }
            )
        }
    }
}

@Composable
private fun ReelEditorTopBar(
    title: String,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onDismiss, enabled = enabled, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close editor",
                    tint = contentColor.copy(alpha = if (enabled) 0.92f else 0.34f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "$title v",
                color = contentColor.copy(alpha = if (enabled) 0.92f else 0.34f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "4K",
            color = contentColor.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1B2028))
                .clickable(enabled = enabled, onClick = onDone)
                .padding(horizontal = 13.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Export",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ReelEditorTransportRow(
    durationMs: Long,
    playheadMs: Long,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = Icons.Outlined.PlayArrow,
            contentDescription = "Play",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = editorTimecodeLabel(playheadMs),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = editorTimecodeLabel(durationMs),
                    color = contentColor.copy(alpha = 0.44f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "1",
                color = contentColor.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Undo,
                contentDescription = "Undo",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Redo,
                contentDescription = "Redo",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReelInstagramTimelinePanel(
    selectedTool: ReelEditorTool,
    editState: ReelVideoEditState,
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    coverMs: Long,
    editorState: ReelTimelineEditorState,
    muteOriginalAudio: Boolean,
    isRendering: Boolean,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onEditChange: (ReelVideoEditState) -> Unit,
    onEditorStateChange: (ReelTimelineEditorState) -> Unit,
    onMuteOriginalAudioChange: (Boolean) -> Unit,
    onUseCoverFrame: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 248.dp, max = 302.dp)
            .background(Color(0xFF0E1318)),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(182.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ReelTrackRow(
                    icon = Icons.Outlined.MusicNote,
                    clipLabel = "Dance break",
                    clipColor = Color(0xFF5368FF),
                    clipStartWeight = 0.62f,
                    clipWeight = 0.3f,
                    contentColor = contentColor
                )
                ReelTrackRow(
                    icon = Icons.Outlined.Mic,
                    clipLabel = "Voiceover",
                    clipColor = Color(0xFFFF2FC3),
                    clipStartWeight = 0.48f,
                    clipWeight = 0.42f,
                    contentColor = contentColor,
                    waveform = true,
                    active = editorState.voiceoverEnabled
                )
                ReelTrackRow(
                    icon = Icons.Outlined.TextFields,
                    clipLabel = when {
                        editorState.hasTextOverlay -> editorState.textOverlay
                        editorState.captionsEnabled -> "Captions"
                        editorState.overlayEnabled -> "Overlay"
                        else -> "Text / overlay"
                    },
                    clipColor = Color(0xFFD800C8),
                    clipStartWeight = 0.48f,
                    clipWeight = 0.42f,
                    contentColor = contentColor,
                    waveform = true,
                    active = editorState.hasTextOverlay || editorState.captionsEnabled || editorState.overlayEnabled
                )
                ReelVideoTimelineRow(
                    trimStartFraction = editState.trimStartFraction,
                    trimEndFraction = editState.trimEndFraction,
                    splitFractions = editorState.splitFractions,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }

        RangeSlider(
            value = editState.trimStartFraction..editState.trimEndFraction,
            onValueChange = { range ->
                val start = range.start.coerceIn(0f, 1f - MinTrimWindowFraction)
                val end = range.endInclusive.coerceIn(start + MinTrimWindowFraction, 1f)
                onEditChange(editState.copy(trimStartFraction = start, trimEndFraction = end))
            },
            valueRange = 0f..1f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFD400),
                activeTrackColor = Color(0xFFFFD400),
                inactiveTrackColor = ReelCreateDivider,
                disabledThumbColor = contentColor.copy(alpha = 0.28f),
                disabledActiveTrackColor = contentColor.copy(alpha = 0.22f),
                disabledInactiveTrackColor = ReelCreateDivider.copy(alpha = 0.42f)
            ),
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        when (selectedTool) {
            ReelEditorTool.Trim -> ReelQuickTrimActions(
                durationMs = durationMs,
                editState = editState,
                editorState = editorState,
                startMs = startMs,
                endMs = endMs,
                enabled = enabled,
                contentColor = contentColor,
                accentColor = accentColor,
                onEditChange = onEditChange,
                onEditorStateChange = onEditorStateChange
            )

            ReelEditorTool.Crop -> ReelCropControls(
                editorState = editorState,
                enabled = enabled,
                contentColor = contentColor,
                accentColor = accentColor,
                onEditorStateChange = onEditorStateChange
            )

            ReelEditorTool.Audio -> ReelSwitchRow(
                "Mute original audio",
                "Silent reel playback",
                muteOriginalAudio,
                enabled,
                contentColor,
                accentColor,
                onMuteOriginalAudioChange
            )

            ReelEditorTool.Text -> ReelTextOverlayControls(
                editorState = editorState,
                enabled = enabled,
                contentColor = contentColor,
                accentColor = accentColor,
                onEditorStateChange = onEditorStateChange
            )

            ReelEditorTool.Voice -> ReelVoiceControls(
                editorState = editorState,
                enabled = enabled,
                contentColor = contentColor,
                accentColor = accentColor,
                onEditorStateChange = onEditorStateChange
            )

            ReelEditorTool.Captions -> ReelCaptionsControls(
                editorState = editorState,
                enabled = enabled,
                contentColor = contentColor,
                accentColor = accentColor,
                onEditorStateChange = onEditorStateChange
            )

            ReelEditorTool.Overlay -> ReelOverlayControls(
                editorState = editorState,
                enabled = enabled,
                contentColor = contentColor,
                accentColor = accentColor,
                onEditorStateChange = onEditorStateChange
            )

            ReelEditorTool.Adjust -> ReelAdjustControls(
                editorState = editorState,
                enabled = enabled,
                contentColor = contentColor,
                accentColor = accentColor,
                onEditorStateChange = onEditorStateChange
            )

            ReelEditorTool.Cover -> ReelCoverControls(
                editState = editState,
                coverMs = coverMs,
                enabled = enabled && !isRendering,
                contentColor = contentColor,
                accentColor = accentColor,
                onEditChange = onEditChange,
                onUseCoverFrame = onUseCoverFrame
            )
        }
    }
}

@Composable
private fun ReelTrackRow(
    icon: ImageVector,
    clipLabel: String,
    clipColor: Color,
    clipStartWeight: Float,
    clipWeight: Float,
    contentColor: Color,
    waveform: Boolean = false,
    active: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color(0xFF151A1F)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.62f),
                modifier = Modifier.size(17.dp)
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(clipStartWeight.coerceAtLeast(0.01f)))
            Box(
                modifier = Modifier
                    .weight(clipWeight.coerceAtLeast(0.05f))
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (active) clipColor else Color(0xFF222830))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (waveform) {
                    MiniWaveform(clipColor = Color.Black.copy(alpha = 0.18f))
                }
                Text(
                    text = clipLabel,
                    color = if (active) Color.White else contentColor.copy(alpha = 0.46f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.weight((1f - clipStartWeight - clipWeight).coerceAtLeast(0.01f)))
        }
    }
}

@Composable
private fun ReelVideoTimelineRow(
    trimStartFraction: Float,
    trimEndFraction: Float,
    splitFractions: List<Float>,
    contentColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .background(Color(0xFF151A1F)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.VideoLibrary,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.62f),
                modifier = Modifier.size(17.dp)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .padding(end = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(12) { index ->
                    val center = (index + 0.5f) / 12f
                    val active = center in trimStartFraction..trimEndFraction
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        if (active) Color(0xFFE0B46B) else Color(0xFF6E493D),
                                        if (active) Color(0xFF356EA2) else Color(0xFF263846)
                                    )
                                )
                            )
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(3.dp, Color(0xFFFFD400), RoundedCornerShape(7.dp))
            )
            splitFractions.forEach { fraction ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                ) {
                    Spacer(Modifier.weight(fraction.coerceIn(0.01f, 0.99f)))
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.92f))
                    )
                    Spacer(Modifier.weight((1f - fraction).coerceIn(0.01f, 0.99f)))
                }
            }
            TimelineHandle(Color(0xFFFFD400))
            TimelineHandle(Color(0xFFFFD400), modifier = Modifier.align(Alignment.CenterEnd))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add clip",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniWaveform(clipColor: Color) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(18) { index ->
            val height = when (index % 5) {
                0 -> 8.dp
                1 -> 18.dp
                2 -> 12.dp
                3 -> 22.dp
                else -> 14.dp
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(height)
                    .clip(RoundedCornerShape(999.dp))
                    .background(clipColor)
            )
        }
    }
}

@Composable
private fun ReelQuickTrimActions(
    durationMs: Long,
    editState: ReelVideoEditState,
    editorState: ReelTimelineEditorState,
    startMs: Long,
    endMs: Long,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onEditChange: (ReelVideoEditState) -> Unit,
    onEditorStateChange: (ReelTimelineEditorState) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${editorTimeLabel(startMs)} - ${editorTimeLabel(endMs)}",
            color = contentColor.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        QuickCutChip("Reset", enabled, contentColor, accentColor) {
            onEditChange(editState.copy(trimStartFraction = 0f, trimEndFraction = 1f))
            onEditorStateChange(editorState.copy(splitFractions = emptyList()))
        }
        QuickCutChip("Split", enabled, contentColor, accentColor) {
            onEditorStateChange(editorState.withSplitAt(0.5f))
        }
        QuickCutChip("Clear splits", enabled && editorState.splitFractions.isNotEmpty(), contentColor, accentColor) {
            onEditorStateChange(editorState.copy(splitFractions = emptyList()))
        }
        QuickCutChip("First 15s", enabled && durationMs > 15_000L, contentColor, accentColor) {
            val window = (15_000f / durationMs).coerceIn(MinTrimWindowFraction, 1f)
            onEditChange(editState.copy(trimStartFraction = 0f, trimEndFraction = window))
        }
        QuickCutChip("Last 15s", enabled && durationMs > 15_000L, contentColor, accentColor) {
            val window = (15_000f / durationMs).coerceIn(MinTrimWindowFraction, 1f)
            onEditChange(editState.copy(trimStartFraction = 1f - window, trimEndFraction = 1f))
        }
    }
}

@Composable
private fun ReelCropControls(
    editorState: ReelTimelineEditorState,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onEditorStateChange: (ReelTimelineEditorState) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReelCropMode.entries.forEach { mode ->
            SelectablePill(
                label = mode.label,
                icon = null,
                selected = editorState.cropMode == mode,
                enabled = enabled,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = { onEditorStateChange(editorState.copy(cropMode = mode)) }
            )
        }
        QuickCutChip(
            label = if (editorState.mirrored) "Mirror on" else "Mirror",
            enabled = enabled,
            contentColor = contentColor,
            accentColor = accentColor
        ) {
            onEditorStateChange(editorState.copy(mirrored = !editorState.mirrored))
        }
    }
}

@Composable
private fun ReelTextOverlayControls(
    editorState: ReelTimelineEditorState,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onEditorStateChange: (ReelTimelineEditorState) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReelTextField(
            value = editorState.textOverlay,
            onValueChange = { onEditorStateChange(editorState.copy(textOverlay = it.take(80))) },
            label = "Overlay text",
            helper = "${editorState.textOverlay.length}/80",
            enabled = enabled,
            contentColor = contentColor,
            accentColor = accentColor,
            singleLine = true
        )
        QuickCutChip("Clear text", enabled && editorState.hasTextOverlay, contentColor, accentColor) {
            onEditorStateChange(editorState.copy(textOverlay = ""))
        }
    }
}

@Composable
private fun ReelVoiceControls(
    editorState: ReelTimelineEditorState,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onEditorStateChange: (ReelTimelineEditorState) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickCutChip(
            label = if (editorState.voiceoverEnabled) "Voice layer on" else "Add voice layer",
            enabled = enabled,
            contentColor = contentColor,
            accentColor = accentColor
        ) {
            onEditorStateChange(editorState.copy(voiceoverEnabled = !editorState.voiceoverEnabled))
        }
        Text(
            text = if (editorState.voiceoverEnabled) "Voiceover track added" else "Tap to add a voice track",
            color = contentColor.copy(alpha = 0.62f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ReelCaptionsControls(
    editorState: ReelTimelineEditorState,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onEditorStateChange: (ReelTimelineEditorState) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickCutChip(
            label = if (editorState.captionsEnabled) "Captions on" else "Auto captions",
            enabled = enabled,
            contentColor = contentColor,
            accentColor = accentColor
        ) {
            onEditorStateChange(editorState.copy(captionsEnabled = !editorState.captionsEnabled))
        }
        Text(
            text = if (editorState.captionsEnabled) "Caption layer visible" else "Adds a caption layer to preview",
            color = contentColor.copy(alpha = 0.62f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ReelOverlayControls(
    editorState: ReelTimelineEditorState,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onEditorStateChange: (ReelTimelineEditorState) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickCutChip(
            label = if (editorState.overlayEnabled) "Overlay on" else "Add overlay",
            enabled = enabled,
            contentColor = contentColor,
            accentColor = accentColor
        ) {
            onEditorStateChange(editorState.copy(overlayEnabled = !editorState.overlayEnabled))
        }
        QuickCutChip(
            label = "Cover frame",
            enabled = enabled,
            contentColor = contentColor,
            accentColor = accentColor
        ) {
            onEditorStateChange(editorState.copy(overlayEnabled = true))
        }
    }
}

@Composable
private fun ReelAdjustControls(
    editorState: ReelTimelineEditorState,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onEditorStateChange: (ReelTimelineEditorState) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        EditorSliderRow(
            label = "Brightness",
            value = editorState.brightness,
            valueRange = -1f..1f,
            enabled = enabled,
            contentColor = contentColor,
            accentColor = accentColor,
            onValueChange = { onEditorStateChange(editorState.copy(brightness = it.coerceIn(-1f, 1f))) }
        )
        EditorSliderRow(
            label = "Contrast",
            value = editorState.contrast,
            valueRange = 0.75f..1.35f,
            enabled = enabled,
            contentColor = contentColor,
            accentColor = accentColor,
            onValueChange = { onEditorStateChange(editorState.copy(contrast = it.coerceIn(0.75f, 1.35f))) }
        )
    }
}

@Composable
private fun ReelEditorDock(
    selectedTool: ReelEditorTool,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onSelectedTool: (ReelEditorTool) -> Unit
) {
    val actions = remember {
        listOf(
            ReelDockAction("Split", Icons.Outlined.ContentCut, ReelEditorTool.Trim),
            ReelDockAction("Crop", Icons.Outlined.Crop, ReelEditorTool.Crop),
            ReelDockAction("Audio", Icons.Outlined.MusicNote, ReelEditorTool.Audio),
            ReelDockAction("Text", Icons.Outlined.TextFields, ReelEditorTool.Text),
            ReelDockAction("Voice", Icons.Outlined.Mic, ReelEditorTool.Voice),
            ReelDockAction("Captions", Icons.Outlined.ClosedCaption, ReelEditorTool.Captions),
            ReelDockAction("Overlay", Icons.Outlined.Layers, ReelEditorTool.Overlay),
            ReelDockAction("Adjust", Icons.Outlined.Tune, ReelEditorTool.Adjust)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .background(Color(0xFF070B10))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { action ->
            val selected = action.tool == selectedTool
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.clickable(enabled = enabled) {
                    onSelectedTool(action.tool)
                }
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = when {
                        !enabled -> contentColor.copy(alpha = 0.28f)
                        selected -> accentColor
                        else -> Color.White
                    },
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = action.label,
                    color = when {
                        !enabled -> contentColor.copy(alpha = 0.28f)
                        selected -> accentColor
                        else -> contentColor.copy(alpha = 0.78f)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ReelEditorToolButton(
    tool: ReelEditorTool,
    selected: Boolean,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White else Color.Black.copy(alpha = 0.46f))
                .border(1.dp, Color.White.copy(alpha = if (selected) 0f else 0.18f), CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.label,
                tint = when {
                    !enabled -> contentColor.copy(alpha = 0.28f)
                    selected -> Color.Black
                    else -> Color.White
                },
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = tool.label,
            color = if (selected) accentColor else contentColor.copy(alpha = 0.78f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReelTimelineTrimControls(
    editState: ReelVideoEditState,
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onEditChange: (ReelVideoEditState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorMetric("Start", editorTimeLabel(startMs), contentColor, accentColor)
            EditorMetric("Clip", editorTimeLabel((endMs - startMs).coerceAtLeast(0L)), contentColor, accentColor)
            EditorMetric("End", editorTimeLabel(endMs), contentColor, accentColor)
        }

        ReelCutTimelineStrip(
            trimStartFraction = editState.trimStartFraction,
            trimEndFraction = editState.trimEndFraction,
            contentColor = contentColor,
            accentColor = accentColor
        )

        RangeSlider(
            value = editState.trimStartFraction..editState.trimEndFraction,
            onValueChange = { range ->
                val start = range.start.coerceIn(0f, 1f - MinTrimWindowFraction)
                val end = range.endInclusive.coerceIn(start + MinTrimWindowFraction, 1f)
                onEditChange(editState.copy(trimStartFraction = start, trimEndFraction = end))
            },
            valueRange = 0f..1f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accentColor,
                inactiveTrackColor = ReelCreateDivider,
                disabledThumbColor = contentColor.copy(alpha = 0.28f),
                disabledActiveTrackColor = contentColor.copy(alpha = 0.22f),
                disabledInactiveTrackColor = ReelCreateDivider.copy(alpha = 0.42f)
            )
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickCutChip(
                label = "Reset",
                enabled = enabled,
                contentColor = contentColor,
                accentColor = accentColor
            ) {
                onEditChange(editState.copy(trimStartFraction = 0f, trimEndFraction = 1f))
            }
            QuickCutChip(
                label = "First 15s",
                enabled = enabled && durationMs > 15_000L,
                contentColor = contentColor,
                accentColor = accentColor
            ) {
                val window = (15_000f / durationMs).coerceIn(MinTrimWindowFraction, 1f)
                onEditChange(editState.copy(trimStartFraction = 0f, trimEndFraction = window))
            }
            QuickCutChip(
                label = "Last 15s",
                enabled = enabled && durationMs > 15_000L,
                contentColor = contentColor,
                accentColor = accentColor
            ) {
                val window = (15_000f / durationMs).coerceIn(MinTrimWindowFraction, 1f)
                onEditChange(editState.copy(trimStartFraction = 1f - window, trimEndFraction = 1f))
            }
        }
    }
}

@Composable
private fun ReelCutTimelineStrip(
    trimStartFraction: Float,
    trimEndFraction: Float,
    contentColor: Color,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.46f))
            .border(1.dp, ReelCreateDivider, RoundedCornerShape(10.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(14) { index ->
                val center = (index + 0.5f) / 14f
                val selected = center in trimStartFraction..trimEndFraction
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (selected) accentColor.copy(alpha = 0.72f) else contentColor.copy(alpha = 0.16f),
                                    if (selected) Color.White.copy(alpha = 0.78f) else contentColor.copy(alpha = 0.08f)
                                )
                            )
                        )
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TimelineHandle(accentColor)
            TimelineHandle(accentColor)
        }
    }
}

@Composable
private fun TimelineHandle(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(6.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor)
    )
}

@Composable
private fun QuickCutChip(
    label: String,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) ReelCreateSurfaceHigh else ReelCreateSurfaceHigh.copy(alpha = 0.45f))
            .border(1.dp, if (enabled) ReelCreateDivider else ReelCreateDivider.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) contentColor.copy(alpha = 0.88f) else contentColor.copy(alpha = 0.32f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReelClipEditor(
    video: ReelUploadMedia,
    editState: ReelVideoEditState,
    durationMs: Long,
    muteOriginalAudio: Boolean,
    isRendering: Boolean,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onEditChange: (ReelVideoEditState) -> Unit,
    onMuteOriginalAudioChange: (Boolean) -> Unit,
    onUseCoverFrame: () -> Unit
) {
    var selectedTool by remember(video.uri) { mutableStateOf(ReelEditorTool.Trim) }
    val controlsEnabled = enabled && !isRendering
    val trim = editState.normalized()
    val trimStartMs = editorPositionMs(durationMs, trim.trimStartFraction)
    val trimEndMs = editorPositionMs(durationMs, trim.trimEndFraction)
    val coverMs = editorPositionMs(
        durationMs,
        trim.trimStartFraction + ((trim.trimEndFraction - trim.trimStartFraction) * trim.coverFrameFraction)
    )

    ReelComposerSection(
        title = "Studio",
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ReelCreateSurface)
                .border(1.dp, ReelCreateDivider, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = video.fileName,
                    color = contentColor.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = editorTimeLabel(durationMs),
                    color = contentColor.copy(alpha = 0.52f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReelEditorTool.entries.forEach { tool ->
                    SelectablePill(
                        label = tool.label,
                        icon = tool.icon,
                        selected = selectedTool == tool,
                        enabled = controlsEnabled,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { selectedTool = tool }
                    )
                }
            }

            when (selectedTool) {
                ReelEditorTool.Trim -> ReelTrimControls(
                    editState = trim,
                    durationMs = durationMs,
                    startMs = trimStartMs,
                    endMs = trimEndMs,
                    enabled = controlsEnabled,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onEditChange = onEditChange
                )

                ReelEditorTool.Audio -> ReelSwitchRow(
                    "Mute original audio",
                    "Silent reel playback",
                    muteOriginalAudio,
                    controlsEnabled,
                    contentColor,
                    accentColor,
                    onMuteOriginalAudioChange
                )

                ReelEditorTool.Cover -> ReelCoverControls(
                    editState = trim,
                    coverMs = coverMs,
                    enabled = controlsEnabled,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onEditChange = onEditChange,
                    onUseCoverFrame = onUseCoverFrame
                )

                ReelEditorTool.Crop,
                ReelEditorTool.Text,
                ReelEditorTool.Voice,
                ReelEditorTool.Captions,
                ReelEditorTool.Overlay,
                ReelEditorTool.Adjust -> Unit
            }

            if (isRendering) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                    Text(
                        text = "Preparing edit",
                        color = contentColor.copy(alpha = 0.68f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ReelTrimControls(
    editState: ReelVideoEditState,
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onEditChange: (ReelVideoEditState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorMetric("Start", editorTimeLabel(startMs), contentColor, accentColor)
            EditorMetric("End", editorTimeLabel(endMs), contentColor, accentColor)
            EditorMetric("Clip", editorTimeLabel((endMs - startMs).coerceAtLeast(0L)), contentColor, accentColor)
        }

        EditorSliderRow(
            label = "Start",
            value = editState.trimStartFraction,
            valueRange = 0f..(editState.trimEndFraction - MinTrimWindowFraction).coerceAtLeast(0.001f),
            enabled = enabled,
            contentColor = contentColor,
            accentColor = accentColor,
            onValueChange = { value ->
                onEditChange(
                    editState.copy(
                        trimStartFraction = value.coerceIn(0f, editState.trimEndFraction - MinTrimWindowFraction)
                    )
                )
            }
        )
        EditorSliderRow(
            label = "End",
            value = editState.trimEndFraction,
            valueRange = (editState.trimStartFraction + MinTrimWindowFraction).coerceAtMost(0.999f)..1f,
            enabled = enabled,
            contentColor = contentColor,
            accentColor = accentColor,
            onValueChange = { value ->
                onEditChange(
                    editState.copy(
                        trimEndFraction = value.coerceIn(editState.trimStartFraction + MinTrimWindowFraction, 1f)
                    )
                )
            }
        )

        if (durationMs <= 0L) {
            Text(
                text = "Reading duration",
                color = contentColor.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ReelCoverControls(
    editState: ReelVideoEditState,
    coverMs: Long,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onEditChange: (ReelVideoEditState) -> Unit,
    onUseCoverFrame: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        EditorSliderRow(
            label = "Frame",
            value = editState.coverFrameFraction,
            valueRange = 0f..1f,
            enabled = enabled,
            contentColor = contentColor,
            accentColor = accentColor,
            onValueChange = { onEditChange(editState.copy(coverFrameFraction = it)) }
        )
        ReelMediaAction(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Outlined.Image,
            label = "Use frame",
            detail = editorTimeLabel(coverMs),
            accentColor = accentColor,
            contentColor = contentColor,
            enabled = enabled,
            onClick = onUseCoverFrame
        )
    }
}

@Composable
private fun EditorSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = contentColor.copy(alpha = if (enabled) 0.68f else 0.34f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = ReelCreateDivider,
                disabledThumbColor = contentColor.copy(alpha = 0.28f),
                disabledActiveTrackColor = contentColor.copy(alpha = 0.22f),
                disabledInactiveTrackColor = ReelCreateDivider.copy(alpha = 0.42f)
            )
        )
    }
}

@Composable
private fun EditorMetric(
    label: String,
    value: String,
    contentColor: Color,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = contentColor.copy(alpha = 0.46f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReelMediaAction(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    detail: String,
    accentColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ReelCreateSurface)
            .border(1.dp, ReelCreateDivider, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) accentColor else contentColor.copy(alpha = 0.34f),
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = contentColor.copy(alpha = if (enabled) 0.9f else 0.38f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                color = contentColor.copy(alpha = if (enabled) 0.52f else 0.28f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReelComposerSection(
    title: String,
    contentColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun ReelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    helper: String,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        label = { Text(label) },
        supportingText = {
            Text(
                text = helper,
                color = contentColor.copy(alpha = 0.45f),
                fontSize = 11.sp
            )
        },
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = contentColor,
            unfocusedTextColor = contentColor,
            disabledTextColor = contentColor.copy(alpha = 0.42f),
            focusedContainerColor = ReelCreateSurface,
            unfocusedContainerColor = ReelCreateSurface,
            disabledContainerColor = ReelCreateSurface.copy(alpha = 0.62f),
            cursorColor = accentColor,
            focusedBorderColor = accentColor,
            unfocusedBorderColor = ReelCreateDivider,
            disabledBorderColor = ReelCreateDivider.copy(alpha = 0.58f),
            focusedLabelColor = accentColor,
            unfocusedLabelColor = ReelCreateMuted,
            disabledLabelColor = contentColor.copy(alpha = 0.34f)
        )
    )
}

@Composable
private fun HashtagComposer(
    hashtagInput: String,
    hashtags: List<String>,
    enabled: Boolean,
    accentColor: Color,
    contentColor: Color,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    val canAdd = enabled && hashtagInput.isNotBlank() && hashtags.size < MaxHashtags

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = hashtagInput,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                enabled = enabled && hashtags.size < MaxHashtags,
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Tag, contentDescription = null) },
                label = { Text("Hashtag") },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = contentColor,
                    unfocusedTextColor = contentColor,
                    focusedContainerColor = ReelCreateSurface,
                    unfocusedContainerColor = ReelCreateSurface,
                    disabledContainerColor = ReelCreateSurface.copy(alpha = 0.62f),
                    cursorColor = accentColor,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = ReelCreateDivider,
                    focusedLeadingIconColor = accentColor,
                    unfocusedLeadingIconColor = ReelCreateMuted,
                    focusedLabelColor = accentColor,
                    unfocusedLabelColor = ReelCreateMuted
                )
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (canAdd) accentColor else ReelCreateSurface)
                    .border(
                        width = 1.dp,
                        color = if (canAdd) accentColor.copy(alpha = 0.35f) else ReelCreateDivider,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = canAdd, onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Add hashtag",
                    tint = if (canAdd) Color.White else contentColor.copy(alpha = 0.38f)
                )
            }
        }

        if (hashtags.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                hashtags.forEach { tag ->
                    AssistChip(
                        onClick = { if (enabled) onRemove(tag) },
                        enabled = enabled,
                        label = { Text("#$tag") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = ReelCreateSurfaceHigh,
                            labelColor = Color.White,
                            disabledContainerColor = ReelCreateSurface,
                            disabledLabelColor = ReelCreateMuted
                        ),
                        trailingIcon = {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Remove hashtag",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceSection(
    title: String,
    options: List<String>,
    selected: String?,
    contentColor: Color,
    accentColor: Color,
    enabled: Boolean,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            selected?.let {
                Text(
                    text = it.replaceFirstChar { char -> char.titlecase(Locale.US) },
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = selected == option
                SelectablePill(
                    label = option.replaceFirstChar { it.titlecase(Locale.US) },
                    icon = visibilityIconFor(option).takeIf { title == "Visibility" },
                    selected = isSelected,
                    enabled = enabled,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { onSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun SelectablePill(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bgColor = when {
        selected -> Color.White
        else -> ReelCreateSurface
    }
    val borderColor = when {
        selected -> Color.White
        else -> ReelCreateDivider
    }
    val fgColor = when {
        !enabled -> contentColor.copy(alpha = 0.34f)
        selected -> Color.Black
        else -> ReelCreateMuted
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = fgColor,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = label,
            color = fgColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReelSwitchRow(
    label: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ReelCreateSurface)
            .border(1.dp, ReelCreateDivider, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                color = contentColor.copy(alpha = if (enabled) 0.92f else 0.36f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                detail,
                color = contentColor.copy(alpha = if (enabled) 0.52f else 0.28f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                checkedBorderColor = accentColor,
                uncheckedThumbColor = contentColor.copy(alpha = 0.48f),
                uncheckedTrackColor = ReelCreateSurfaceHigh,
                uncheckedBorderColor = ReelCreateDivider
            )
        )
    }
}

@Composable
private fun DraftLibraryRow(
    draftCount: Int,
    isExpanded: Boolean,
    isLoading: Boolean,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val detail = when {
        isLoading -> "Loading drafts"
        draftCount == 1 -> "1 saved draft"
        draftCount > 1 -> "$draftCount saved drafts"
        else -> "Open saved drafts"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ReelCreateSurface)
            .border(1.dp, ReelCreateDivider, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ReelCreateSurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                } else {
                    Icon(Icons.Outlined.VideoLibrary, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Saved drafts",
                    color = contentColor.copy(alpha = if (enabled) 1f else 0.42f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = detail,
                    color = contentColor.copy(alpha = if (enabled) 0.54f else 0.28f),
                    fontSize = 11.sp
                )
            }
        }
        Text(
            text = if (isExpanded) "Hide" else "View",
            color = if (enabled) accentColor else contentColor.copy(alpha = 0.3f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReelDraftsPanel(
    drafts: List<Reel>,
    isLoading: Boolean,
    error: String?,
    publishingDraftId: String?,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onRefresh: () -> Unit,
    onPreview: (Reel) -> Unit,
    onPublish: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ReelCreateSurface)
            .border(1.dp, ReelCreateDivider, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Drafts",
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Refresh",
                color = if (enabled && !isLoading) accentColor else contentColor.copy(alpha = 0.32f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(enabled = enabled && !isLoading, onClick = onRefresh)
            )
        }

        error?.let {
            Text(
                text = it,
                color = Color(0xFFFFCDD2),
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF3B1418))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }

        when {
            isLoading && drafts.isEmpty() -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Loading drafts", color = contentColor.copy(alpha = 0.62f), fontSize = 12.sp)
                }
            }
            drafts.isEmpty() -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ReelCreateSurfaceHigh)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null, tint = contentColor.copy(alpha = 0.42f), modifier = Modifier.size(19.dp))
                    Text(
                        text = "No saved drafts yet",
                        color = contentColor.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 330.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    drafts.forEach { draft ->
                        ReelDraftListItem(
                            draft = draft,
                            enabled = enabled && publishingDraftId == null,
                            isPublishing = publishingDraftId == draft.id,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onPreview = { onPreview(draft) },
                            onPublish = { onPublish(draft.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelDraftListItem(
    draft: Reel,
    enabled: Boolean,
    isPublishing: Boolean,
    contentColor: Color,
    accentColor: Color,
    onPreview: () -> Unit,
    onPublish: () -> Unit
) {
    val context = LocalContext.current
    val thumbnail = draft.thumbnailUrl ?: draft.previewGifUrl ?: draft.videoUrl
    val active = enabled || isPublishing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ReelCreateSurfaceHigh)
            .clickable(enabled = enabled && !isPublishing, onClick = onPreview)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Icon(
                Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = draft.reelDraftTitle(),
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = draft.reelDraftSubtitle(),
                color = contentColor.copy(alpha = 0.56f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Draft",
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .width(82.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) accentColor else ReelCreateDivider)
                .clickable(enabled = enabled, onClick = onPublish),
            contentAlignment = Alignment.Center
        ) {
            if (isPublishing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(
                    text = "Publish",
                    color = if (enabled) Color.White else contentColor.copy(alpha = 0.34f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

private fun Reel.reelDraftTitle(): String =
    title?.takeIf { it.isNotBlank() }
        ?: caption?.takeIf { it.isNotBlank() }?.take(48)
        ?: "Untitled reel"

private fun Reel.reelDraftSubtitle(): String {
    val firstHashtag = hashtags.firstOrNull()?.let { "#$it" }
    return caption?.takeIf { it.isNotBlank() }
        ?: firstHashtag
        ?: updatedAt?.takeIf { it.isNotBlank() }
        ?: createdAt.takeIf { it.isNotBlank() }
        ?: "Ready to publish"
}

@Composable
private fun DraftPreferenceRow(
    saveAsDraft: Boolean,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ReelCreateSurface)
            .border(1.dp, ReelCreateDivider, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ReelCreateSurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Save as draft",
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (saveAsDraft) "Publish later" else "Post when submitted",
                    color = contentColor.copy(alpha = 0.54f),
                    fontSize = 11.sp
                )
            }
        }
        Switch(
            checked = saveAsDraft,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                checkedBorderColor = accentColor
            )
        )
    }
}

@Composable
private fun ReelPublishBar(
    label: String,
    enabled: Boolean,
    isUploading: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val active = enabled || isUploading

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (active) {
                    Brush.linearGradient(listOf(accentColor, accentColor))
                } else {
                    Brush.linearGradient(listOf(ReelCreateSurfaceHigh, ReelCreateSurfaceHigh))
                }
            )
            .border(
                1.dp,
                if (active) accentColor else ReelCreateDivider,
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(19.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = if (enabled) Color.White else contentColor.copy(alpha = 0.36f),
                    modifier = Modifier.size(19.dp)
                )
            }
            Text(
                text = if (isUploading) "Uploading reel" else label,
                color = if (active) Color.White else contentColor.copy(alpha = 0.38f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ReelWarningText(text: String) {
    Text(
        text = text,
        color = Color(0xFFDC2626),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFEBEE))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

private fun editorPositionMs(durationMs: Long, fraction: Float): Long {
    if (durationMs <= 0L) return 0L
    return (durationMs * fraction.coerceIn(0f, 1f)).roundToLong()
}

private fun editorTimeLabel(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

private fun editorTimecodeLabel(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private suspend fun createReelCoverFromFrame(
    context: Context,
    video: ReelUploadMedia,
    editState: ReelVideoEditState
): ReelUploadMedia = withContext(Dispatchers.IO) {
    val outputDir = File(context.cacheDir, "reel_covers").apply { mkdirs() }
    val outputFile = File(outputDir, "vormex_cover_${System.currentTimeMillis()}.jpg")
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, video.uri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?.takeIf { it > 0L }
            ?: 0L
        val trim = editState.normalized()
        val frameFraction = trim.trimStartFraction +
            ((trim.trimEndFraction - trim.trimStartFraction) * trim.coverFrameFraction)
        val frameTimeUs = editorPositionMs(durationMs, frameFraction) * 1000L
        val bitmap = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: throw IllegalStateException("Could not read this video frame")

        FileOutputStream(outputFile).use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)) {
                throw IllegalStateException("Could not save cover frame")
            }
        }
        bitmap.recycle()
    } finally {
        retriever.release()
    }

    ReelUploadMedia(
        uri = Uri.fromFile(outputFile),
        fileName = outputFile.name,
        mimeType = "image/jpeg",
        sizeBytes = outputFile.length()
    )
}

private suspend fun renderTrimmedReelVideo(
    context: Context,
    media: ReelUploadMedia,
    editState: ReelVideoEditState,
    muteOriginalAudio: Boolean
): ReelUploadMedia = withContext(Dispatchers.IO) {
    val durationMs = readVideoDurationMs(context, media.uri)
    if (durationMs <= 0L) {
        throw IllegalStateException("Could not read video duration")
    }

    val trim = editState.normalized()
    val startUs = (durationMs * trim.trimStartFraction).roundToLong() * 1000L
    val endUs = (durationMs * trim.trimEndFraction).roundToLong() * 1000L
    if (endUs - startUs < 1_000_000L) {
        throw IllegalStateException("Clip must be at least 1 second")
    }

    val outputDir = File(context.cacheDir, "reel_edits").apply { mkdirs() }
    val outputFile = File(outputDir, "vormex_reel_${System.currentTimeMillis()}.mp4")
    muxTrimmedVideo(
        context = context,
        sourceUri = media.uri,
        outputFile = outputFile,
        startUs = startUs,
        endUs = endUs,
        muteOriginalAudio = muteOriginalAudio
    )

    ReelUploadMedia(
        uri = Uri.fromFile(outputFile),
        fileName = outputFile.name,
        mimeType = "video/mp4",
        sizeBytes = outputFile.length()
    )
}

private fun readVideoDurationMs(context: Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?.takeIf { it > 0L }
            ?: 0L
    } finally {
        retriever.release()
    }
}

private fun muxTrimmedVideo(
    context: Context,
    sourceUri: Uri,
    outputFile: File,
    startUs: Long,
    endUs: Long,
    muteOriginalAudio: Boolean
) {
    val extractor = MediaExtractor()
    var muxer: MediaMuxer? = null
    var muxerStarted = false
    var wroteSamples = false

    try {
        extractor.setDataSource(context, sourceUri, null)
        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        setVideoOrientationHint(context, sourceUri, muxer)

        val trackMap = mutableMapOf<Int, Int>()
        var maxBufferSize = 4 * 1024 * 1024
        var hasVideoTrack = false

        for (trackIndex in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            val isVideo = mime.startsWith("video/")
            val isAudio = mime.startsWith("audio/")
            if (!isVideo && !isAudio) continue
            if (muteOriginalAudio && isAudio) continue

            extractor.selectTrack(trackIndex)
            trackMap[trackIndex] = muxer.addTrack(format)
            hasVideoTrack = hasVideoTrack || isVideo

            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                maxBufferSize = maxOf(maxBufferSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
            }
        }

        if (!hasVideoTrack || trackMap.isEmpty()) {
            throw IllegalStateException("Could not prepare video tracks")
        }

        val buffer = ByteBuffer.allocateDirect(maxBufferSize)
        val bufferInfo = MediaCodec.BufferInfo()
        var firstSampleTimeUs = -1L

        muxer.start()
        muxerStarted = true
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        while (true) {
            val sourceTrackIndex = extractor.sampleTrackIndex
            if (sourceTrackIndex < 0) break

            val muxerTrackIndex = trackMap[sourceTrackIndex]
            if (muxerTrackIndex == null) {
                extractor.advance()
                continue
            }

            val sampleTimeUs = extractor.sampleTime
            if (sampleTimeUs < 0L || sampleTimeUs > endUs) break

            buffer.clear()
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break

            if (firstSampleTimeUs < 0L) {
                firstSampleTimeUs = sampleTimeUs
            }

            val bufferFlags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                MediaCodec.BUFFER_FLAG_KEY_FRAME
            } else {
                0
            }

            bufferInfo.set(
                0,
                sampleSize,
                (sampleTimeUs - firstSampleTimeUs).coerceAtLeast(0L),
                bufferFlags
            )
            muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
            wroteSamples = true
            extractor.advance()
        }

        if (!wroteSamples) {
            throw IllegalStateException("Could not write edited clip")
        }
    } finally {
        extractor.release()
        muxer?.let { activeMuxer ->
            if (muxerStarted) {
                runCatching { activeMuxer.stop() }
            }
            activeMuxer.release()
        }
    }
}

private fun setVideoOrientationHint(
    context: Context,
    sourceUri: Uri,
    muxer: MediaMuxer
) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, sourceUri)
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull()
            ?: 0
        if (rotation != 0) {
            muxer.setOrientationHint(rotation)
        }
    } finally {
        retriever.release()
    }
}

private fun visibilityIconFor(option: String): ImageVector = when (option) {
    "public" -> Icons.Outlined.Public
    "connections" -> Icons.Outlined.Groups
    "private" -> Icons.Outlined.Lock
    else -> Icons.Outlined.Public
}

private fun mediaDetail(media: ReelUploadMedia): String =
    listOfNotNull(media.mimeType, media.sizeBytes?.let(::formatFileSize)).joinToString(" / ")

private fun Uri.toReelUploadMedia(context: Context, fallbackName: String, fallbackMime: String): ReelUploadMedia {
    var name = fallbackName
    var size: Long? = null
    context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex) ?: fallbackName
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex).takeIf { it >= 0 }
                }
            }
        }

    return ReelUploadMedia(
        uri = this,
        fileName = name.ifBlank { fallbackName },
        mimeType = context.contentResolver.getType(this) ?: fallbackMime,
        sizeBytes = size
    )
}

private fun formatFileSize(bytes: Long): String {
    val mb = bytes / (1024f * 1024f)
    return if (mb >= 1f) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.0f KB", bytes / 1024f)
    }
}
