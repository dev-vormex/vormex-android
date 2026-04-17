package com.kyant.backdrop.catalog.chat

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.ai.VormexAiChipAction
import com.kyant.backdrop.catalog.ai.VormexAiChipRow
import com.kyant.backdrop.catalog.ai.VormexAiGateway
import com.kyant.backdrop.catalog.ai.VormexAiRewriteStyle
import com.kyant.backdrop.catalog.ai.VormexAiStatusCard
import com.kyant.backdrop.catalog.ai.VormexAiSurface
import com.kyant.backdrop.catalog.ai.VormexAiTextResult
import com.kyant.backdrop.catalog.data.ChatMutePreferences
import com.kyant.backdrop.catalog.linkedin.FullScreenVideoPlayer
import com.kyant.backdrop.catalog.linkedin.VideoPlayer
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.Conversation
import com.kyant.backdrop.catalog.network.models.Message
import com.kyant.backdrop.catalog.network.models.SharedPostContent
import com.kyant.backdrop.catalog.linkedin.currentVormexAppearance
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import androidx.compose.runtime.DisposableEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private const val CHAT_ATTACHMENT_MAX_BYTES = 25 * 1024 * 1024
private const val CHAT_VIDEO_MAX_BYTES = 150 * 1024 * 1024
private const val CHAT_VIDEO_MAX_DURATION_MS = 90_000L
private val chatDocumentMimeTypes = arrayOf(
    "application/pdf",
    "text/plain",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
)

private data class ChatPickedAttachment(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long?,
    val durationMs: Long? = null
)

private data class ChatFullscreenMedia(
    val url: String,
    val type: String,
    val title: String?,
    val fileSize: Int?,
    val backgroundStatusText: (() -> String?)? = null
)

/**
 * Main entry point for the Chat tab.
 * @param onInChatThread Callback invoked when entering/leaving a chat thread.
 *        Use this to hide/show bottom navigation for an immersive chat experience.
 */
@Composable
fun ChatTabContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    viewModel: ChatViewModel,
    isGlassTheme: Boolean = true,
    openConversationId: String? = null,
    openChatWithUserId: String? = null,
    onConsumedOpenConversation: () -> Unit = {},
    onConsumedOpenChat: () -> Unit = {},
    onInChatThread: (Boolean) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    val isInThread = uiState.selectedConversation != null
    val isResolvingConversation = uiState.isResolvingConversationOpen && uiState.selectedConversation == null

    // Ensure socket stays connected when app resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.ensureSocketConnected()
                    viewModel.ensureConversationsLoaded()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Notify parent about chat thread state changes for bottom nav visibility
    LaunchedEffect(isInThread) {
        onInChatThread(isInThread)
    }

    // Handle back button: return to chat list instead of closing app
    BackHandler(enabled = isInThread) {
        viewModel.selectConversation(null)
    }

    LaunchedEffect(Unit) {
        viewModel.ensureConversationsLoaded()
    }

    LaunchedEffect(openChatWithUserId) {
        val userId = openChatWithUserId ?: return@LaunchedEffect
        viewModel.openChatWithUser(userId)
        onConsumedOpenChat()
    }

    LaunchedEffect(openConversationId) {
        val conversationId = openConversationId ?: return@LaunchedEffect
        viewModel.openConversationById(conversationId)
        onConsumedOpenConversation()
    }

    if (isInThread) {
        ChatThreadScreen(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            viewModel = viewModel,
            onNavigateToProfile = onNavigateToProfile
        )
    } else if (isResolvingConversation) {
        ChatConversationOpeningState(
            contentColor = contentColor,
            accentColor = accentColor
        )
    } else {
        ChatListScreen(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            viewModel = viewModel
        )
    }
}

@Composable
private fun ChatListScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    viewModel: ChatViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val isRefreshingConversations = uiState.isLoadingConversations && uiState.conversations.isNotEmpty()

    // Filter conversations based on search
    val searchedConversations = if (searchQuery.isNotBlank()) {
        uiState.conversations.filter { conv ->
            val name = conv.otherParticipant.name ?: conv.otherParticipant.username ?: ""
            name.contains(searchQuery, ignoreCase = true)
        }
    } else {
        uiState.conversations
    }
    val requestConversations = searchedConversations.filter { it.isMessageRequest }
    val unreadConversations = searchedConversations.filter { !it.isMessageRequest && it.unreadCount > 0 }
    val recentConversations = searchedConversations.filter { !it.isMessageRequest && it.unreadCount == 0 }
    val orderedConversations = buildList {
        addAll(requestConversations)
        addAll(unreadConversations)
        addAll(recentConversations)
    }
    val hasVisibleConversations = orderedConversations.isNotEmpty()

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Spacer(Modifier.height(4.dp))

        if (isRefreshingConversations || !uiState.socketConnected) {
            ChatSyncStatusPill(
                text = if (uiState.socketConnected) "Refreshing chats..." else "Reconnecting chat...",
                contentColor = contentColor,
                accentColor = accentColor
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(contentColor.copy(alpha = if (isGlassTheme) 0.09f else 0.07f))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search",
                modifier = Modifier.size(16.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.52f))
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(contentColor, 13.sp),
                cursorBrush = SolidColor(contentColor),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        BasicText(
                            "Search conversations...",
                            style = TextStyle(contentColor.copy(alpha = 0.42f), 13.sp)
                        )
                    }
                    innerTextField()
                }
            )
            if (searchQuery.isNotEmpty()) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Clear",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { searchQuery = "" },
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.52f))
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (uiState.isLoadingConversations && uiState.conversations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ChatConversationListLoadingState(contentColor = contentColor)
            }
        } else if (!hasVisibleConversations) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ChatInboxEmptyState(
                    searchQuery = searchQuery,
                    contentColor = contentColor
                )
            }
        } else if (uiState.error != null && uiState.conversations.isEmpty()) {
            BasicText(uiState.error!!, style = TextStyle(contentColor.copy(alpha = 0.8f), 14.sp))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 112.dp)
            ) {
                items(orderedConversations, key = { it.id }) { conv ->
                    SwipeableConversationRow(
                        conversation = conv,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        currentUserId = uiState.currentUserId,
                        onClick = { viewModel.selectConversation(conv) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableConversationRow(
    conversation: Conversation,
    contentColor: Color,
    accentColor: Color,
    currentUserId: String?,
    onClick: () -> Unit
) {
    val other = conversation.otherParticipant
    val lastMsg = conversation.lastMessage
    val previewText = conversationPreviewText(conversation, currentUserId)
    val rowTime = lastMsg?.createdAt ?: conversation.updatedAt
    val showUnreadAccent = conversation.unreadCount > 0
    val avatarInitial = (other.name ?: other.username ?: "?")
        .trim()
        .firstOrNull()
        ?.uppercaseChar()
        ?.toString()
        ?: "?"

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            conversation.isMessageRequest -> accentColor.copy(alpha = 0.16f)
                            showUnreadAccent -> accentColor.copy(alpha = 0.1f)
                            else -> contentColor.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!other.profileImage.isNullOrBlank()) {
                    AsyncImage(
                        model = other.profileImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    BasicText(
                        avatarInitial,
                        style = TextStyle(
                            color = if (conversation.isMessageRequest) accentColor else contentColor.copy(alpha = 0.82f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        other.name ?: other.username ?: "Unknown",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = if (showUnreadAccent) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        formatTime(rowTime),
                        style = TextStyle(
                            color = if (showUnreadAccent) accentColor else contentColor.copy(alpha = 0.44f),
                            fontSize = 10.sp,
                            fontWeight = if (showUnreadAccent) FontWeight.Medium else FontWeight.Normal
                        )
                    )
                }
                Spacer(Modifier.height(2.dp))
                BasicText(
                    previewText,
                    style = TextStyle(
                        color = contentColor.copy(alpha = if (showUnreadAccent) 0.72f else 0.54f),
                        fontSize = 11.sp,
                        fontWeight = if (showUnreadAccent) FontWeight.Medium else FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (conversation.unreadCount > 0) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                        style = TextStyle(Color.White, 9.sp, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 48.dp)
                .height(1.dp)
                .background(contentColor.copy(alpha = 0.08f))
        )
    }
}

@Composable
private fun ChatInboxEmptyState(
    searchQuery: String,
    contentColor: Color
) {
    val title: String
    val subtitle: String

    if (searchQuery.isNotBlank()) {
        title = "No chats match \"$searchQuery\""
        subtitle = "Try a different name or username."
    } else {
        title = "No conversations yet"
        subtitle = "Start a chat from a profile and it will appear here."
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicText(
            title,
            style = TextStyle(contentColor, 18.sp, fontWeight = FontWeight.SemiBold)
        )
        BasicText(
            subtitle,
            style = TextStyle(contentColor.copy(alpha = 0.58f), 13.sp)
        )
    }
}

private data class LoadMoreAnchor(
    val firstVisibleIndex: Int,
    val firstVisibleOffset: Int,
    val previousMessageCount: Int
)

@Composable
private fun ChatThreadScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    viewModel: ChatViewModel,
    onNavigateToProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val reportScope = rememberCoroutineScope()
    val composerAiScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val conv = uiState.selectedConversation ?: return
    var inputText by remember(conv.id) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val lastMessageId = uiState.messages.lastOrNull()?.id
    var pendingLoadMoreAnchor by remember(conv.id) { mutableStateOf<LoadMoreAnchor?>(null) }
    var initialBottomScrollDone by remember(conv.id) { mutableStateOf(false) }
    val connectionStatus = when {
        !uiState.socketConnected -> "reconnecting..."
        uiState.isLoadingMessages -> "syncing messages..."
        else -> "live chat"
    }
    val showLoadingWithoutCache = uiState.threadReadyState == ChatThreadReadyState.LOADING_WITHOUT_CACHE
    val showEmptyThread = uiState.threadReadyState == ChatThreadReadyState.EMPTY_THREAD

    // Chat menu state
    var showChatMenu by remember { mutableStateOf(false) }
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var muteUntilMillis by remember(conv.id) {
        mutableStateOf(ChatMutePreferences.getMuteUntilMillis(context, conv.id))
    }
    var showMuteDurationSheet by remember { mutableStateOf(false) }
    var showReportChatSheet by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showWallpaperSheet by remember { mutableStateOf(false) }
    var fullscreenMedia by remember(conv.id) { mutableStateOf<ChatFullscreenMedia?>(null) }
    var showAttachmentCard by remember(conv.id) { mutableStateOf(false) }
    var selectedWallpaperKey by remember(conv.id) {
        mutableStateOf(ChatWallpaperPreferences.getSelectedKey(context, conv.id))
    }
    val selectedWallpaper = remember(selectedWallpaperKey) {
        chatWallpapers.firstOrNull { it.key == selectedWallpaperKey }
    }
    val threadContentColor = if (selectedWallpaper != null) Color.White else contentColor
    val attachmentScope = rememberCoroutineScope()
    val aiGateway = remember { VormexAiGateway(context.applicationContext) }
    var composerAiStatus by remember(conv.id) { mutableStateOf<String?>(null) }
    var composerAiBusyLabel by remember(conv.id) { mutableStateOf<String?>(null) }

    fun applyComposerAiResult(label: String, result: VormexAiTextResult, onSuccess: (String) -> Unit) {
        composerAiBusyLabel = null
        when (result) {
            is VormexAiTextResult.Success -> {
                onSuccess(result.text)
                composerAiStatus = when (result.source) {
                    com.kyant.backdrop.catalog.ai.VormexAiSource.LOCAL -> "$label updated on-device."
                    com.kyant.backdrop.catalog.ai.VormexAiSource.CLOUD -> "$label updated with cloud AI."
                }
            }
            is VormexAiTextResult.NeedsDownload -> {
                composerAiStatus = result.message
            }
            is VormexAiTextResult.Blocked -> {
                composerAiStatus = result.message
            }
            is VormexAiTextResult.Failure -> {
                composerAiStatus = result.message
            }
        }
    }

    fun runComposerAi(label: String, block: suspend () -> VormexAiTextResult, onSuccess: (String) -> Unit) {
        composerAiScope.launch {
            composerAiBusyLabel = "$label…"
            composerAiStatus = null
            applyComposerAiResult(label, block(), onSuccess)
        }
    }

    val voicePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            showAttachmentCard = false
            viewModel.startVoiceRecording(context)
        } else {
            Toast.makeText(context, "Microphone permission is needed for voice messages", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVoiceMessage() {
        showAttachmentCard = false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startVoiceRecording(context)
        } else {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun handlePickedAttachment(
        uri: Uri?,
        fallbackFileName: String,
        fallbackMimeType: String
    ) {
        if (uri == null) return
        val replyToId = uiState.replyToMessage?.id
        showAttachmentCard = false
        attachmentScope.launch {
            try {
                val attachment = context.readChatAttachment(
                    uri = uri,
                    fallbackFileName = fallbackFileName,
                    fallbackMimeType = fallbackMimeType
                )
                if (attachment.fileSize != null && attachment.fileSize > maxChatAttachmentBytes(attachment.mimeType)) {
                    Toast.makeText(
                        context,
                        chatAttachmentSizeMessage(attachment.mimeType),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                viewModel.uploadAndSendMessage(
                    uri = attachment.uri,
                    fileName = attachment.fileName,
                    mimeType = attachment.mimeType,
                    fileSize = attachment.fileSize,
                    durationMs = attachment.durationMs,
                    localPreviewUrl = uri.toString(),
                    replyToId = replyToId
                )
                viewModel.clearAiSuggestions()
                viewModel.clearReplyTo()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    e.message ?: "Could not read this file",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        handlePickedAttachment(uri, "photo.jpg", "image/jpeg")
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        handlePickedAttachment(uri, "video.mp4", "video/mp4")
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        handlePickedAttachment(uri, "document", "application/octet-stream")
    }

    val isNotificationsMuted = muteUntilMillis > System.currentTimeMillis()

    BackHandler(enabled = fullscreenMedia != null) {
        fullscreenMedia = null
    }

    BackHandler(enabled = uiState.isRecordingVoice) {
        viewModel.cancelVoiceRecording()
    }

    LaunchedEffect(conv.id) {
        muteUntilMillis = ChatMutePreferences.getMuteUntilMillis(context, conv.id)
    }

    LaunchedEffect(conv.id, muteUntilMillis) {
        val until = muteUntilMillis
        if (until <= System.currentTimeMillis()) return@LaunchedEffect
        val wait = (until - System.currentTimeMillis()).coerceAtMost(365L * 24 * 60 * 60 * 1000)
        delay(wait)
        muteUntilMillis = ChatMutePreferences.getMuteUntilMillis(context, conv.id)
    }

    // Filtered messages for search
    val displayedMessages = if (isSearchMode && searchQuery.isNotBlank()) {
        uiState.messages.filter { it.content.contains(searchQuery, ignoreCase = true) }
    } else {
        uiState.messages
    }
    val showingFullThread = !isSearchMode || searchQuery.isBlank()

    // Load AI suggestions when messages change (after receiving a new message)
    LaunchedEffect(uiState.messages.lastOrNull()?.id) {
        val lastMsg = uiState.messages.lastOrNull()
        // Only load suggestions if the last message is from the other person
        if (lastMsg != null && lastMsg.senderId != uiState.currentUserId) {
            viewModel.loadAiSuggestions(explicitCloudFallback = false)
        }
    }

    DisposableEffect(conv.id) {
        onDispose {
            viewModel.sendTyping(false)
        }
    }

    LaunchedEffect(conv.id) {
        viewModel.markAsRead()
    }

    LaunchedEffect(conv.id, uiState.messages.size, uiState.isLoadingMoreMessages) {
        if (!initialBottomScrollDone && uiState.messages.isNotEmpty() && !uiState.isLoadingMoreMessages) {
            listState.scrollToItem(uiState.messages.lastIndex)
            initialBottomScrollDone = true
        }
    }

    LaunchedEffect(lastMessageId) {
        val lastMessage = uiState.messages.lastOrNull() ?: return@LaunchedEffect
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        val isNearBottom = lastVisibleIndex >= (uiState.messages.lastIndex - 2)
        val isMyMessage = lastMessage.senderId == uiState.currentUserId
        if (isNearBottom || isMyMessage) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(conv.id, uiState.messages.size, uiState.isLoadingMoreMessages) {
        val anchor = pendingLoadMoreAnchor ?: return@LaunchedEffect
        if (uiState.isLoadingMoreMessages) return@LaunchedEffect

        val addedCount = uiState.messages.size - anchor.previousMessageCount
        if (addedCount > 0 && uiState.messages.isNotEmpty()) {
            val targetIndex = (anchor.firstVisibleIndex + addedCount)
                .coerceAtMost(uiState.messages.lastIndex)
            listState.scrollToItem(targetIndex, anchor.firstVisibleOffset)
        }

        pendingLoadMoreAnchor = null
    }

    LaunchedEffect(
        listState,
        conv.id,
        showingFullThread,
        uiState.hasMoreMessages,
        uiState.isLoadingMessages,
        uiState.isLoadingMoreMessages,
        uiState.messages.size
    ) {
        if (!showingFullThread) return@LaunchedEffect

        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisibleIndex ->
                val shouldLoadMore =
                    uiState.hasMoreMessages &&
                        !uiState.isLoadingMessages &&
                        !uiState.isLoadingMoreMessages &&
                        uiState.messages.isNotEmpty() &&
                        firstVisibleIndex <= 2

                if (shouldLoadMore && pendingLoadMoreAnchor == null) {
                    pendingLoadMoreAnchor = LoadMoreAnchor(
                        firstVisibleIndex = listState.firstVisibleItemIndex,
                        firstVisibleOffset = listState.firstVisibleItemScrollOffset,
                        previousMessageCount = uiState.messages.size
                    )
                    viewModel.loadMoreMessages()
                }
            }
    }

    LaunchedEffect(listState, conv.id, uiState.messages.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (
                    lastVisibleIndex != null &&
                    uiState.messages.isNotEmpty() &&
                    lastVisibleIndex >= uiState.messages.lastIndex - 1
                ) {
                    viewModel.markAsRead()
                }
            }
    }

    Box(Modifier.fillMaxSize()) {
        selectedWallpaper?.let { wallpaper ->
            androidx.compose.foundation.Image(
                painter = painterResource(wallpaper.drawableRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f))
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
        ) {
            // Header (normal or search mode)
            if (isSearchMode) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "Close search",
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                isSearchMode = false
                                searchQuery = ""
                            },
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(threadContentColor)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = if (selectedWallpaper != null) 0.38f else 0f))
                            .background(threadContentColor.copy(alpha = if (selectedWallpaper != null) 0.12f else 0.08f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        textStyle = TextStyle(threadContentColor, 13.sp),
                        cursorBrush = SolidColor(threadContentColor),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                BasicText(
                                    "Search messages...",
                                    style = TextStyle(threadContentColor.copy(alpha = 0.5f), 13.sp)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            "${displayedMessages.size}",
                            style = TextStyle(threadContentColor.copy(alpha = 0.55f), 11.sp)
                        )
                    }
                }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { viewModel.selectConversation(null) },
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(threadContentColor)
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(threadContentColor.copy(alpha = 0.12f))
                            .clickable { onNavigateToProfile(conv.otherParticipant.id) }
                    ) {
                        AsyncImage(
                            model = conv.otherParticipant.profileImage,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { onNavigateToProfile(conv.otherParticipant.id) }
                    ) {
                        BasicText(
                            conv.otherParticipant.name ?: conv.otherParticipant.username ?: "Unknown",
                            style = TextStyle(threadContentColor, 14.sp, fontWeight = FontWeight.SemiBold)
                        )
                        BasicText(
                            connectionStatus,
                            style = TextStyle(threadContentColor.copy(alpha = 0.6f), 11.sp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedWallpaper != null) Color.Black.copy(alpha = 0.36f)
                                else threadContentColor.copy(alpha = 0.08f)
                            )
                            .clickable { showChatMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ic_more),
                            contentDescription = "Menu",
                            modifier = Modifier.size(20.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(threadContentColor)
                        )
                    }
                }
            }

            // Messages
            when {
                showLoadingWithoutCache -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        ChatThreadLoadingState(
                            contentColor = threadContentColor,
                            accentColor = accentColor
                        )
                    }
                }
                isSearchMode && searchQuery.isNotBlank() && displayedMessages.isEmpty() && uiState.messages.isNotEmpty() -> {
            // No search results
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BasicText("🔍", style = TextStyle(fontSize = 26.sp))
                            Spacer(Modifier.height(8.dp))
                            BasicText(
                                "No messages found",
                                style = TextStyle(threadContentColor, 14.sp, fontWeight = FontWeight.Medium)
                            )
                            BasicText(
                                "Try a different search term",
                                style = TextStyle(threadContentColor.copy(alpha = 0.6f), 12.sp)
                            )
                        }
                    }
                }
                showEmptyThread -> {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ChatThreadEmptyState()
                    }
                }
                else -> {
                    Column(Modifier.weight(1f)) {
                        if (uiState.isLoadingMessages && uiState.messages.isNotEmpty()) {
                            ChatSyncStatusPill(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                text = "syncing messages...",
                                contentColor = threadContentColor,
                                accentColor = accentColor
                            )
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 8.dp)
                        ) {
                            if (uiState.isLoadingMoreMessages && showingFullThread) {
                                item(key = "loading-more-history") {
                                    ChatSyncStatusPill(
                                        text = "Loading older messages...",
                                        contentColor = threadContentColor,
                                        accentColor = accentColor
                                    )
                                }
                            }

                            itemsIndexed(displayedMessages, key = { _, msg -> msg.id }) { index, msg ->
                                val isFromMe = msg.senderId == uiState.currentUserId
                                val nextMsg = displayedMessages.getOrNull(index + 1)
                                val showTimestamp = shouldShowClusterMetaForDm(msg, nextMsg)
                                val deliveryStateText = chatDeliveryStateText(msg, isFromMe)

                                MessageBubble(
                                    message = msg,
                                    isFromMe = isFromMe,
                                    contentColor = threadContentColor,
                                    accentColor = accentColor,
                                    backdrop = backdrop,
                                    isGlassTheme = isGlassTheme,
                                    showTimestamp = showTimestamp,
                                    onReply = { viewModel.setReplyTo(msg) },
                                    onReact = { emoji -> viewModel.reactToMessage(msg.id, emoji) },
                                    onDelete = { forEveryone -> viewModel.deleteMessage(msg.id, forEveryone) },
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Message", msg.content))
                                        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpenMedia = { media ->
                                        fullscreenMedia = media
                                    },
                                    currentUserId = uiState.currentUserId,
                                    deliveryStateText = deliveryStateText
                                )
                            }
                        }
                    }
                }
            }

            // Reply preview (shown when replying to a message)
            uiState.replyToMessage?.let { replyMsg ->
                ReplyPreview(
                    message = replyMsg,
                    contentColor = threadContentColor,
                    accentColor = accentColor,
                    backdrop = backdrop,
                    onDismiss = { viewModel.clearReplyTo() }
                )
            }

            // AI Smart Reply Suggestions (shown above input when available)
            if (uiState.aiSuggestions.isNotEmpty() && inputText.isEmpty() && !showEmptyThread) {
                AiSuggestionsRow(
                    suggestions = uiState.aiSuggestions,
                    contentColor = threadContentColor,
                    accentColor = accentColor,
                    backdrop = backdrop,
                    onSuggestionClick = { suggestion ->
                        inputText = suggestion
                        viewModel.clearAiSuggestions()
                        viewModel.sendTyping(true)
                    }
                )
            } else if (
                inputText.isEmpty() &&
                !showEmptyThread &&
                (
                    uiState.isLoadingAiSuggestions ||
                        uiState.isPreparingAiSuggestions ||
                        !uiState.aiStatusMessage.isNullOrBlank()
                    )
            ) {
                VormexAiStatusCard(
                    message = when {
                        uiState.isPreparingAiSuggestions -> "Preparing on-device AI for smart replies…"
                        uiState.isLoadingAiSuggestions -> "Generating smart replies…"
                        else -> uiState.aiStatusMessage ?: ""
                    },
                    contentColor = threadContentColor,
                    accentColor = accentColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    primaryAction = if (uiState.aiNeedsPreparation && !uiState.isPreparingAiSuggestions) {
                        VormexAiChipAction(
                            label = "Prepare on-device AI",
                            onClick = viewModel::prepareAiSuggestions
                        )
                    } else {
                        null
                    },
                    secondaryAction = if (uiState.aiCanUseCloudFallback && !uiState.isLoadingAiSuggestions) {
                        VormexAiChipAction(
                            label = "Use cloud smart replies",
                            onClick = { viewModel.loadAiSuggestions(explicitCloudFallback = true) }
                        )
                    } else {
                        null
                    }
                )
            }

            val sendCurrentMessage = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(inputText.trim(), uiState.replyToMessage?.id)
                    viewModel.sendTyping(false)
                    viewModel.clearAiSuggestions()
                    viewModel.clearReplyTo()
                    inputText = ""
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (showAttachmentCard) {
                    ChatAttachmentCard(
                        backdrop = backdrop,
                        contentColor = threadContentColor,
                        accentColor = accentColor,
                        isGlassTheme = isGlassTheme,
                        onPickImage = { imagePicker.launch("image/*") },
                        onPickVideo = { videoPicker.launch("video/*") },
                        onRecordVoice = { startVoiceMessage() },
                        onPickDocument = { documentPicker.launch(chatDocumentMimeTypes) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (uiState.isRecordingVoice) {
                    VoiceRecordingCard(
                        contentColor = threadContentColor,
                        accentColor = accentColor,
                        onCancel = { viewModel.cancelVoiceRecording() },
                        onSend = {
                            viewModel.stopVoiceRecordingAndSend(context)
                            viewModel.clearAiSuggestions()
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (uiState.isUploadingAttachment) {
                    ChatSyncStatusPill(
                        text = "uploading attachment...",
                        contentColor = threadContentColor,
                        accentColor = accentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                uiState.attachmentUploadError?.let { error ->
                    ChatAttachmentError(
                        message = error,
                        contentColor = threadContentColor,
                        onDismiss = { viewModel.clearAttachmentError() }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (uiState.typingUserId != null) {
                    VormexTypingPresence()
                }
                val threadSummaryInput = remember(uiState.messages, uiState.currentUserId) {
                    buildChatSummaryInput(uiState.messages, uiState.currentUserId)
                }
                val composerAiActions = buildList {
                    add(
                        VormexAiChipAction(
                            label = "Friendly",
                            enabled = inputText.isNotBlank(),
                            onClick = {
                                runComposerAi(
                                    label = "Friendly rewrite",
                                    block = {
                                        aiGateway.rewrite(
                                            text = inputText,
                                            style = VormexAiRewriteStyle.FRIENDLY,
                                            surface = VormexAiSurface.CHAT,
                                            allowCloudFallback = true
                                        )
                                    },
                                    onSuccess = {
                                        inputText = it
                                        viewModel.sendTyping(it.isNotEmpty())
                                        viewModel.clearAiSuggestions()
                                    }
                                )
                            }
                        )
                    )
                    add(
                        VormexAiChipAction(
                            label = "Professional",
                            enabled = inputText.isNotBlank(),
                            onClick = {
                                runComposerAi(
                                    label = "Professional rewrite",
                                    block = {
                                        aiGateway.rewrite(
                                            text = inputText,
                                            style = VormexAiRewriteStyle.PROFESSIONAL,
                                            surface = VormexAiSurface.CHAT,
                                            allowCloudFallback = true
                                        )
                                    },
                                    onSuccess = {
                                        inputText = it
                                        viewModel.sendTyping(it.isNotEmpty())
                                        viewModel.clearAiSuggestions()
                                    }
                                )
                            }
                        )
                    )
                    add(
                        VormexAiChipAction(
                            label = "Shorter",
                            enabled = inputText.isNotBlank(),
                            onClick = {
                                runComposerAi(
                                    label = "Shorter rewrite",
                                    block = {
                                        aiGateway.rewrite(
                                            text = inputText,
                                            style = VormexAiRewriteStyle.SHORTER,
                                            surface = VormexAiSurface.CHAT,
                                            allowCloudFallback = true
                                        )
                                    },
                                    onSuccess = {
                                        inputText = it
                                        viewModel.sendTyping(it.isNotEmpty())
                                        viewModel.clearAiSuggestions()
                                    }
                                )
                            }
                        )
                    )
                    add(
                        VormexAiChipAction(
                            label = "Proofread",
                            enabled = inputText.isNotBlank(),
                            onClick = {
                                runComposerAi(
                                    label = "Proofread",
                                    block = {
                                        aiGateway.proofread(
                                            text = inputText,
                                            surface = VormexAiSurface.CHAT,
                                            allowCloudFallback = true
                                        )
                                    },
                                    onSuccess = {
                                        inputText = it
                                        viewModel.sendTyping(it.isNotEmpty())
                                        viewModel.clearAiSuggestions()
                                    }
                                )
                            }
                        )
                    )
                    add(
                        VormexAiChipAction(
                            label = "Summarize thread",
                            enabled = threadSummaryInput.isNotBlank(),
                            onClick = {
                                runComposerAi(
                                    label = "Thread summary",
                                    block = {
                                        aiGateway.summarize(
                                            text = threadSummaryInput,
                                            surface = VormexAiSurface.CHAT,
                                            allowCloudFallback = true
                                        )
                                    },
                                    onSuccess = { summary ->
                                        inputText = summary
                                        viewModel.sendTyping(summary.isNotEmpty())
                                        viewModel.clearAiSuggestions()
                                    }
                                )
                            }
                        )
                    )
                }
                if (composerAiActions.isNotEmpty()) {
                    VormexAiChipRow(
                        actions = composerAiActions,
                        contentColor = threadContentColor,
                        accentColor = accentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                composerAiBusyLabel?.let { label ->
                    ChatSyncStatusPill(
                        text = label,
                        contentColor = threadContentColor,
                        accentColor = accentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                composerAiStatus?.let { message ->
                    VormexAiStatusCard(
                        message = message,
                        contentColor = threadContentColor,
                        accentColor = accentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black.copy(alpha = if (selectedWallpaper != null) 0.42f else 0f))
                        .background(threadContentColor.copy(alpha = if (selectedWallpaper != null) 0.1f else 0.08f))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(threadContentColor.copy(alpha = 0.08f))
                            .clickable {
                                showAttachmentCard = !showAttachmentCard
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ic_plus),
                            contentDescription = "Attach",
                            modifier = Modifier.size(18.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(threadContentColor.copy(alpha = 0.7f))
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    BasicTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            viewModel.sendTyping(it.isNotEmpty())
                            if (it.isNotEmpty()) {
                                viewModel.clearAiSuggestions()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(threadContentColor, 13.sp),
                        cursorBrush = SolidColor(threadContentColor),
                        singleLine = false,
                        maxLines = 3,
                        decorationBox = { innerTextField ->
                            Box(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                if (inputText.isEmpty()) {
                                    BasicText(
                                        "Message...",
                                        style = TextStyle(threadContentColor.copy(alpha = 0.5f), 13.sp)
                                    )
                                }
                                innerTextField()
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendCurrentMessage() })
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) accentColor else accentColor.copy(alpha = 0.45f))
                            .clickable(enabled = inputText.isNotBlank()) { sendCurrentMessage() },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ic_send),
                            contentDescription = "Send",
                            modifier = Modifier.size(16.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                        )
                    }
                }
                if (!uiState.socketConnected) {
                    BasicText(
                        "reconnecting...",
                        modifier = Modifier.padding(start = 10.dp, top = 4.dp),
                        style = TextStyle(threadContentColor.copy(alpha = 0.52f), 11.sp)
                    )
                }
            }
        } // End of Column

        // Glass-styled chat options menu (overlay)
        if (showChatMenu) {
            GlassChatMenu(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isMuted = isNotificationsMuted,
                muteStatusLine = formatChatMuteStatusLine(isNotificationsMuted, muteUntilMillis),
                wallpaperStatusLine = selectedWallpaper?.title ?: "Default background",
                isGlassTheme = isGlassTheme,
                onDismiss = { showChatMenu = false },
                onViewProfile = {
                    onNavigateToProfile(conv.otherParticipant.id)
                    showChatMenu = false
                },
                onSearchMessages = {
                    isSearchMode = true
                    showChatMenu = false
                },
                onChangeWallpaper = {
                    showWallpaperSheet = true
                    showChatMenu = false
                },
                onMuteNotifications = {
                    showChatMenu = false
                    if (isNotificationsMuted) {
                        ChatMutePreferences.clearMute(context, conv.id)
                        muteUntilMillis = 0L
                        Toast.makeText(context, "Notifications unmuted", Toast.LENGTH_SHORT).show()
                    } else {
                        showMuteDurationSheet = true
                    }
                },
                onClearChat = {
                    showClearConfirm = true
                    showChatMenu = false
                },
                onReport = {
                    showReportChatSheet = true
                    showChatMenu = false
                }
            )
        }

        fullscreenMedia?.let { media ->
            ChatFullscreenMediaViewer(
                media = media,
                accentColor = accentColor,
                onDismiss = { fullscreenMedia = null }
            )
        }

        if (showWallpaperSheet) {
            GlassWallpaperSheet(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                selectedWallpaperKey = selectedWallpaperKey,
                onDismiss = { showWallpaperSheet = false },
                onSelectWallpaper = { wallpaper ->
                    selectedWallpaperKey = wallpaper?.key
                    ChatWallpaperPreferences.setSelectedKey(context, conv.id, wallpaper?.key)
                    showWallpaperSheet = false
                }
            )
        }

        if (showMuteDurationSheet) {
            GlassMuteDurationSheet(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                peerName = conv.otherParticipant.name ?: conv.otherParticipant.username ?: "this chat",
                onDismiss = { showMuteDurationSheet = false },
                onConfirmDuration = { durationMs ->
                    val until = System.currentTimeMillis() + durationMs
                    ChatMutePreferences.setMuteUntilMillis(context, conv.id, until)
                    muteUntilMillis = until
                    showMuteDurationSheet = false
                    Toast.makeText(context, "Notifications muted", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showReportChatSheet) {
            GlassReportChatDialog(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                reportedLabel = conv.otherParticipant.name ?: conv.otherParticipant.username ?: "this person",
                onDismiss = { showReportChatSheet = false },
                onSubmit = { reason, details ->
                    reportScope.launch {
                        ApiClient.reportChat(
                            context,
                            conv.id,
                            reason,
                            details
                        ).onSuccess {
                            Toast.makeText(
                                context,
                                "Thanks — we received your report and will review it.",
                                Toast.LENGTH_LONG
                            ).show()
                            showReportChatSheet = false
                        }.onFailure { e ->
                            Toast.makeText(
                                context,
                                e.message ?: "Could not submit report",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
        }

        // Clear chat confirmation dialog (glass-styled overlay)
        if (showClearConfirm) {
            GlassConfirmDialog(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                title = "Delete Chat",
                message = "Are you sure you want to delete this conversation? All messages will be permanently removed from the database. This action cannot be undone.",
                confirmText = "Delete",
                confirmColor = Color(0xFFFF6B6B),
                onConfirm = {
                    viewModel.deleteCurrentConversation()
                    showClearConfirm = false
                },
                onDismiss = { showClearConfirm = false }
            )
        }


    } // End of Box
}

private fun buildChatSummaryInput(
    messages: List<Message>,
    currentUserId: String?
): String {
    return messages
        .takeLast(12)
        .joinToString(separator = "\n") { message ->
            val speaker = if (message.senderId == currentUserId) "You" else "Them"
            "$speaker: ${message.content}"
        }
        .trim()
}

@Composable
private fun ChatConversationOpeningState(
    contentColor: Color,
    accentColor: Color
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ChatThreadLoadingState(
            contentColor = contentColor,
            accentColor = accentColor
        )
    }
}

/**
 * Typing indicator: warm, brand-forward presence — not read receipts or “seen” lists.
 */
@Composable
private fun VormexTypingPresence(
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.hands_typing_on_keyboard)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    if (composition == null) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .width(92.dp)
                .aspectRatio(1080f / 568f),
            contentScale = ContentScale.Fit,
            clipToCompositionBounds = true
        )
    }
}

@Composable
private fun ChatSyncStatusPill(
    text: String,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val pulseAlpha = rememberLoaderPulseAlpha()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = pulseAlpha))
        )
        Spacer(Modifier.width(8.dp))
        BasicText(
            text,
            style = TextStyle(contentColor.copy(alpha = 0.78f), 12.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun ChatAttachmentCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onRecordVoice: () -> Unit,
    onPickDocument: () -> Unit
) {
    val appearance = currentVormexAppearance()
    val cardShape = RoundedCornerShape(24.dp)
    val cardContentColor = if (isGlassTheme) Color.White else appearance.contentColor
    val borderColor = if (isGlassTheme) {
        Color.White.copy(alpha = 0.1f)
    } else {
        appearance.overlayBorderColor
    }
    val surface = if (isGlassTheme) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(24f.dp) },
            effects = {
                vibrancy()
                blur(24f.dp.toPx())
            },
            onDrawSurface = { drawRect(Color(0xFF111B21).copy(alpha = 0.82f)) }
        )
    } else {
        Modifier.background(appearance.sheetColor, cardShape)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(surface)
            .clip(cardShape)
            .background(Color.Black.copy(alpha = if (isGlassTheme) 0.08f else 0f))
            .border(1.dp, borderColor, cardShape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(cardContentColor.copy(alpha = 0.18f))
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            ChatAttachmentChoiceButton(
                iconRes = R.drawable.ic_chat_gallery,
                title = "Photos",
                circleColor = Color(0xFF8B5CF6),
                contentColor = cardContentColor,
                modifier = Modifier.weight(1f),
                onClick = onPickImage
            )
            ChatAttachmentChoiceButton(
                iconRes = R.drawable.ic_chat_video,
                title = "Videos",
                circleColor = Color(0xFFEF4444),
                contentColor = cardContentColor,
                modifier = Modifier.weight(1f),
                onClick = onPickVideo
            )
            ChatAttachmentChoiceButton(
                iconRes = R.drawable.ic_chat_audio,
                title = "Voice",
                circleColor = Color(0xFFF59E0B),
                contentColor = cardContentColor,
                modifier = Modifier.weight(1f),
                onClick = onRecordVoice
            )
            ChatAttachmentChoiceButton(
                iconRes = R.drawable.ic_chat_document,
                title = "Docs",
                circleColor = Color(0xFF2563EB),
                contentColor = cardContentColor,
                modifier = Modifier.weight(1f),
                onClick = onPickDocument
            )
        }
    }
}

@Composable
private fun ChatAttachmentChoiceButton(
    iconRes: Int,
    title: String,
    circleColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(iconRes),
                contentDescription = title,
                modifier = Modifier.size(26.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
            )
        }
        Spacer(Modifier.height(9.dp))
        BasicText(
            title,
            style = TextStyle(contentColor.copy(alpha = 0.9f), 12.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VoiceRecordingCard(
    contentColor: Color,
    accentColor: Color,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "voice_recording_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voice_recording_phase"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.38f))
            .background(contentColor.copy(alpha = 0.1f))
            .border(1.dp, contentColor.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF4D67).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_chat_audio),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFFFF4D67))
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                "Recording voice",
                style = TextStyle(contentColor, 13.sp, fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(7.dp))
            VoiceWaveform(
                phase = phase,
                color = accentColor
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(contentColor.copy(alpha = 0.12f))
                .clickable { onCancel() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                "Cancel",
                style = TextStyle(contentColor.copy(alpha = 0.82f), 12.sp, fontWeight = FontWeight.Medium)
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accentColor)
                .clickable { onSend() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                "Send",
                style = TextStyle(Color.White, 12.sp, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun VoiceWaveform(
    phase: Float,
    color: Color
) {
    Row(
        modifier = Modifier.height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(12) { index ->
            val angle = phase * 2f * PI.toFloat() + index * 0.62f
            val wave = ((sin(angle.toDouble()) + 1.0) / 2.0).toFloat()
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((8f + wave * 22f).dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color.copy(alpha = 0.48f + wave * 0.52f))
            )
        }
    }
}

@Composable
private fun ChatAttachmentError(
    message: String,
    contentColor: Color,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFF6B6B).copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            message.ifBlank { "Attachment failed" },
            modifier = Modifier.weight(1f),
            style = TextStyle(contentColor.copy(alpha = 0.88f), 12.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = "Dismiss",
            modifier = Modifier
                .size(18.dp)
                .clickable { onDismiss() },
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.76f))
        )
    }
}

@Composable
private fun ChatConversationListLoadingState(contentColor: Color) {
    val pulseAlpha = rememberLoaderPulseAlpha()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        repeat(5) { index ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadingPlaceholder(
                    modifier = Modifier.size(52.dp),
                    contentColor = contentColor,
                    pulseAlpha = pulseAlpha,
                    shape = CircleShape
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoadingPlaceholder(
                        modifier = Modifier.fillMaxWidth(if (index % 2 == 0) 0.58f else 0.7f).height(14.dp),
                        contentColor = contentColor,
                        pulseAlpha = pulseAlpha
                    )
                    LoadingPlaceholder(
                        modifier = Modifier.fillMaxWidth(if (index % 2 == 0) 0.36f else 0.44f).height(11.dp),
                        contentColor = contentColor,
                        pulseAlpha = pulseAlpha
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatThreadLoadingState(
    contentColor: Color,
    accentColor: Color
) {
    val pulseAlpha = rememberLoaderPulseAlpha()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Top
    ) {
        ChatSyncStatusPill(
            text = "syncing messages...",
            contentColor = contentColor,
            accentColor = accentColor
        )
        Spacer(Modifier.height(12.dp))
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                LoadingPlaceholder(
                    modifier = Modifier
                        .align(if (index % 2 == 0) Alignment.CenterStart else Alignment.CenterEnd)
                        .fillMaxWidth(if (index % 2 == 0) 0.52f else 0.46f)
                        .height(if (index == 2) 44.dp else 34.dp),
                    contentColor = contentColor,
                    pulseAlpha = pulseAlpha
                )
            }
        }
    }
}

@Composable
private fun ChatThreadEmptyState() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.contact_us))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(220.dp),
                contentScale = ContentScale.Fit,
                clipToCompositionBounds = true
            )
        }
    }
}

@Composable
private fun LoadingPlaceholder(
    modifier: Modifier,
    contentColor: Color,
    pulseAlpha: Float,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(contentColor.copy(alpha = 0.08f + (pulseAlpha * 0.14f)))
    )
}

@Composable
private fun LoadingPlaceholder(
    modifier: Modifier,
    contentColor: Color,
    pulseAlpha: Float,
    shape: androidx.compose.ui.graphics.Shape
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(contentColor.copy(alpha = 0.08f + (pulseAlpha * 0.14f)))
    )
}

@Composable
private fun rememberLoaderPulseAlpha(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "chat_loader_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chat_loader_pulse_alpha"
    )
    return pulseAlpha
}

// Common emoji reactions
private val quickReactions = listOf("❤️", "👍", "😂", "😮", "😢", "🔥")
private val chatMediaContentTypes = setOf("image", "video", "audio", "document", "file")

@Composable
private fun ChatMediaMessageContent(
    message: Message,
    contentColor: Color,
    accentColor: Color,
    onOpenMedia: (ChatFullscreenMedia) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaUrl = message.mediaUrl ?: return
    val type = message.contentType.lowercase()
    val caption = message.mediaCaptionOrNull()
    val isUploading = message.isPendingUpload()
    val isRemoteMedia = mediaUrl.isRemoteChatMediaUrl()
    val cacheFile = remember(mediaUrl, message.fileName, type) {
        if (isRemoteMedia) context.chatMediaCacheFile(mediaUrl, message.fileName, type) else null
    }
    var localMediaUrl by remember(mediaUrl) {
        mutableStateOf(cacheFile?.takeIf { it.exists() }?.toURI()?.toString())
    }
    var isDownloading by remember(mediaUrl) { mutableStateOf(false) }
    var downloadProgress by remember(mediaUrl) { mutableStateOf(0f) }
    var downloadError by remember(mediaUrl) { mutableStateOf<String?>(null) }
    val displayMediaUrl = localMediaUrl ?: mediaUrl
    val idleRemoteStatusText = if (type == "video") "Tap to play" else "Tap to download"
    val mediaStatusText = when {
        isUploading -> "Sending..."
        isDownloading -> formatChatDownloadProgress(downloadProgress)
        downloadError != null -> "Tap to retry"
        isRemoteMedia && localMediaUrl == null -> idleRemoteStatusText
        isRemoteMedia && localMediaUrl != null -> "Saved locally"
        else -> null
    }

    fun openFullscreen(url: String) {
        val backgroundStatusProvider = if (type == "video") {
            {
                when {
                    url == mediaUrl && isDownloading -> formatChatLocalSaveProgress(downloadProgress)
                    url == mediaUrl && isRemoteMedia && localMediaUrl != null -> "Saved locally"
                    else -> null
                }
            }
        } else {
            null
        }
        onOpenMedia(
            message.toFullscreenMedia(
                mediaUrl = url,
                mediaType = type,
                backgroundStatusText = backgroundStatusProvider
            )
        )
    }

    fun downloadMedia(openAfterDownload: Boolean) {
        val target = cacheFile
        if (!isRemoteMedia || target == null) {
            if (openAfterDownload) openFullscreen(displayMediaUrl)
            return
        }
        if (isDownloading) return

        scope.launch {
            isDownloading = true
            downloadError = null
            downloadProgress = 0f
            runCatching {
                context.downloadChatMediaToCache(mediaUrl, target) { progress ->
                    downloadProgress = progress
                }
            }.onSuccess { downloadedFile ->
                val localUrl = downloadedFile.toURI().toString()
                localMediaUrl = localUrl
                downloadProgress = 1f
                if (openAfterDownload) {
                    openFullscreen(localUrl)
                }
            }.onFailure { error ->
                downloadError = error.message ?: "Download failed"
                Toast.makeText(context, downloadError, Toast.LENGTH_SHORT).show()
            }
            isDownloading = false
        }
    }

    fun openMedia() {
        if (isUploading) return
        if (type == "video" && isRemoteMedia && localMediaUrl == null) {
            downloadMedia(openAfterDownload = false)
            openFullscreen(displayMediaUrl)
        } else if (isRemoteMedia && localMediaUrl == null) {
            downloadMedia(openAfterDownload = true)
        } else {
            openFullscreen(displayMediaUrl)
        }
    }

    fun openDocument() {
        if (isUploading) return
        if (isRemoteMedia && localMediaUrl == null) {
            val target = cacheFile ?: return
            if (isDownloading) return
            scope.launch {
                isDownloading = true
                downloadError = null
                downloadProgress = 0f
                runCatching {
                    context.downloadChatMediaToCache(mediaUrl, target) { progress ->
                        downloadProgress = progress
                    }
                }.onSuccess { downloadedFile ->
                    val localUrl = downloadedFile.toURI().toString()
                    localMediaUrl = localUrl
                    downloadProgress = 1f
                    context.openChatDocument(localUrl, message.fileName ?: message.content)
                }.onFailure { error ->
                    downloadError = error.message ?: "Download failed"
                    Toast.makeText(context, downloadError, Toast.LENGTH_SHORT).show()
                }
                isDownloading = false
            }
        } else {
            context.openChatDocument(
                mediaUrl = displayMediaUrl,
                fileName = message.fileName ?: message.content
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (type) {
            "image" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                        .clickable { openMedia() }
                ) {
                    AsyncImage(
                        model = displayMediaUrl,
                        contentDescription = message.fileName ?: "Chat image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (isUploading) {
                        ChatMediaUploadingOverlay(contentColor = contentColor)
                    }
                    mediaStatusText?.let { status ->
                        ChatMediaStatusChip(
                            text = status,
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        )
                    }
                }
            }
            "video" -> {
                val shouldShowVideoDownloadCard = isRemoteMedia && localMediaUrl == null
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    if (shouldShowVideoDownloadCard) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = !isUploading && !isDownloading) { openMedia() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(R.drawable.ic_chat_video),
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                                )
                                BasicText(
                                    message.fileName?.takeIf { it.isNotBlank() } ?: "Video",
                                    style = TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                formatChatFileSize(message.fileSize)?.let { size ->
                                    BasicText(
                                        size,
                                        style = TextStyle(
                                            color = Color.White.copy(alpha = 0.68f),
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    } else {
                        VideoPlayer(
                            videoUrl = displayMediaUrl,
                            modifier = Modifier.fillMaxSize(),
                            autoPlay = false,
                            showControls = false,
                            contentColor = contentColor,
                            onFullScreenClick = { openMedia() }
                        )
                    }
                    if (!isUploading && !isDownloading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            ChatMediaStatusChip(
                                text = if (shouldShowVideoDownloadCard) "Play" else "Open",
                                contentColor = Color.White
                            )
                        }
                    }
                    if (isUploading) {
                        ChatMediaUploadingOverlay(contentColor = contentColor)
                    }
                    if (isDownloading) {
                        ChatMediaDownloadOverlay(
                            text = formatChatLocalSaveProgress(downloadProgress),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        mediaStatusText?.let { status ->
                            ChatMediaStatusChip(
                                text = status,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
            "audio" -> {
                ChatAudioMessageContent(
                    mediaUrl = displayMediaUrl,
                    fileName = message.fileName,
                    fileSize = message.fileSize,
                    mediaStatusText = mediaStatusText,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isUploading = isUploading,
                    onOpen = {
                        openMedia()
                    }
                )
            }
            else -> {
                ChatDocumentMessageContent(
                    fileName = message.fileName ?: message.content,
                    fileSize = message.fileSize,
                    mediaStatusText = mediaStatusText,
                    actionText = if (isRemoteMedia && localMediaUrl == null) "Download" else "Open",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onOpen = { openDocument() }
                )
            }
        }

        caption?.let {
            BasicText(
                it,
                style = TextStyle(contentColor, 13.sp),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChatMediaUploadingOverlay(contentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pulseAlpha = rememberLoaderPulseAlpha()
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = pulseAlpha))
            )
            Spacer(Modifier.width(8.dp))
            BasicText(
                "Uploading...",
                style = TextStyle(Color.White, 12.sp, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun ChatMediaStatusChip(
    text: String,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text,
            style = TextStyle(
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChatMediaDownloadOverlay(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.42f)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.68f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
            BasicText(
                text,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChatAudioMessageContent(
    mediaUrl: String,
    fileName: String?,
    fileSize: Int?,
    mediaStatusText: String?,
    contentColor: Color,
    accentColor: Color,
    isUploading: Boolean,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember(mediaUrl) { mutableStateOf(false) }
    var isEnded by remember(mediaUrl) { mutableStateOf(false) }
    var positionMs by remember(mediaUrl) { mutableStateOf(0L) }
    var durationMs by remember(mediaUrl) { mutableStateOf(0L) }
    val player = remember(mediaUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(mediaUrl))
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isEnded = playbackState == Player.STATE_ENDED
                val duration = player.duration
                if (duration > 0L) durationMs = duration
                positionMs = player.currentPosition.coerceAtLeast(0L)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, isPlaying) {
        while (isPlaying) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            val duration = player.duration
            if (duration > 0L) durationMs = duration
            delay(250)
        }
        positionMs = player.currentPosition.coerceAtLeast(0L)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .clickable { onOpen() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_chat_audio),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(accentColor)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                fileName?.takeIf { it.isNotBlank() } ?: "Audio message",
                style = TextStyle(contentColor, 13.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(7.dp))
            ChatAudioProgressBar(
                progress = chatAudioProgressFraction(positionMs, durationMs),
                trackColor = contentColor,
                progressColor = accentColor
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    formatChatPlaybackTime(positionMs, durationMs),
                    style = TextStyle(contentColor.copy(alpha = 0.55f), 11.sp),
                    maxLines = 1
                )
                val metaText = when {
                    !mediaStatusText.isNullOrBlank() -> mediaStatusText
                    isUploading -> "Sending..."
                    else -> formatChatFileSize(fileSize)
                }
                metaText?.let {
                    BasicText(
                        it,
                        style = TextStyle(contentColor.copy(alpha = 0.55f), 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accentColor.copy(alpha = if (isPlaying) 0.9f else 0.24f))
                .clickable(enabled = !isUploading) {
                    if (isPlaying) {
                        player.pause()
                    } else {
                        if (isEnded) {
                            player.seekTo(0L)
                        }
                        player.play()
                    }
                }
                .padding(horizontal = 11.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                if (isUploading) "Wait" else if (isPlaying) "Pause" else "Play",
                style = TextStyle(
                    color = if (isPlaying) Color.White else contentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun ChatAudioProgressBar(
    progress: Float,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor.copy(alpha = 0.16f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .background(progressColor.copy(alpha = 0.92f))
        )
    }
}

@Composable
private fun ChatDocumentMessageContent(
    fileName: String,
    fileSize: Int?,
    mediaStatusText: String?,
    actionText: String,
    contentColor: Color,
    accentColor: Color,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .clickable { onOpen() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_file_text),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(accentColor)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                fileName.ifBlank { "Attachment" },
                style = TextStyle(contentColor, 13.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            formatChatFileSize(fileSize)?.let { size ->
                Spacer(Modifier.height(2.dp))
                BasicText(
                    listOfNotNull(size, mediaStatusText).joinToString(" • "),
                    style = TextStyle(contentColor.copy(alpha = 0.55f), 11.sp)
                )
            } ?: run {
                Spacer(Modifier.height(2.dp))
                BasicText(
                    mediaStatusText ?: "Tap to open",
                    style = TextStyle(contentColor.copy(alpha = 0.55f), 11.sp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        BasicText(
            actionText,
            style = TextStyle(accentColor, 12.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChatFullscreenMediaViewer(
    media: ChatFullscreenMedia,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    when (media.type.lowercase()) {
        "video" -> FullScreenVideoPlayer(
            videoUrl = media.url,
            onDismiss = onDismiss,
            backgroundStatusText = media.backgroundStatusText?.invoke()
        )
        "audio" -> ChatFullscreenAudioPlayer(
            media = media,
            accentColor = accentColor,
            onDismiss = onDismiss
        )
        else -> ChatFullscreenImageViewer(media = media, onDismiss = onDismiss)
    }
}

@Composable
private fun ChatFullscreenImageViewer(
    media: ChatFullscreenMedia,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = media.url,
            contentDescription = media.title ?: "Chat media",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        ChatFullscreenCloseButton(onDismiss = onDismiss)
    }
}

@Composable
private fun ChatFullscreenAudioPlayer(
    media: ChatFullscreenMedia,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember(media.url) { mutableStateOf(false) }
    var isEnded by remember(media.url) { mutableStateOf(false) }
    var positionMs by remember(media.url) { mutableStateOf(0L) }
    var durationMs by remember(media.url) { mutableStateOf(0L) }
    val player = remember(media.url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(media.url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isEnded = playbackState == Player.STATE_ENDED
                val duration = player.duration
                if (duration > 0L) durationMs = duration
                positionMs = player.currentPosition.coerceAtLeast(0L)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, isPlaying) {
        while (isPlaying) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            val duration = player.duration
            if (duration > 0L) durationMs = duration
            delay(250)
        }
        positionMs = player.currentPosition.coerceAtLeast(0L)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_chat_audio),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                )
            }
            Spacer(Modifier.height(22.dp))
            BasicText(
                media.title?.takeIf { it.isNotBlank() } ?: "Audio message",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            formatChatFileSize(media.fileSize)?.let { size ->
                Spacer(Modifier.height(6.dp))
                BasicText(
                    size,
                    style = TextStyle(Color.White.copy(alpha = 0.62f), 13.sp)
                )
            }
            Spacer(Modifier.height(28.dp))
            ChatAudioProgressBar(
                progress = chatAudioProgressFraction(positionMs, durationMs),
                trackColor = Color.White,
                progressColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            BasicText(
                formatChatPlaybackTime(positionMs, durationMs),
                style = TextStyle(Color.White.copy(alpha = 0.68f), 13.sp)
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor)
                    .clickable {
                        if (isPlaying) {
                            player.pause()
                        } else {
                            if (isEnded) player.seekTo(0L)
                            player.play()
                        }
                    }
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    if (isPlaying) "Pause" else "Play",
                    style = TextStyle(Color.White, 16.sp, fontWeight = FontWeight.SemiBold)
                )
            }
        }
        ChatFullscreenCloseButton(onDismiss = onDismiss)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ChatFullscreenCloseButton(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .statusBarsPadding()
            .padding(16.dp)
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.56f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            "X",
            style = TextStyle(Color.White, 18.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isFromMe: Boolean,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean = true,
    showTimestamp: Boolean = true,
    onReply: () -> Unit = {},
    onReact: (String) -> Unit = {},
    onDelete: (Boolean) -> Unit = {},
    onCopy: () -> Unit = {},
    onOpenMedia: (ChatFullscreenMedia) -> Unit = {},
    currentUserId: String? = null,
    deliveryStateText: String? = null
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 80.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var showMenu by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        // Reply-to preview (if this message is a reply)
        message.replyTo?.let { reply ->
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .background(contentColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(accentColor, RoundedCornerShape(1.dp))
                )
                Spacer(Modifier.width(6.dp))
                BasicText(
                    reply.content.take(40) + if (reply.content.length > 40) "..." else "",
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp),
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (kotlin.math.abs(offsetX.value) > swipeThreshold) {
                                    onReply()
                                }
                                offsetX.animateTo(0f, tween(200))
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val newValue = offsetX.value + dragAmount
                                // Only allow swipe in one direction based on message owner
                                if (isFromMe) {
                                    offsetX.snapTo(newValue.coerceIn(-swipeThreshold * 1.2f, 0f))
                                } else {
                                    offsetX.snapTo(newValue.coerceIn(0f, swipeThreshold * 1.2f))
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showMenu = true },
                        onDoubleTap = { onReact("❤️") }
                    )
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                // Reply indicator (shown during swipe)
                if (!isFromMe && offsetX.value > 20f) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(accentColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("↩", style = TextStyle(contentColor, 14.sp))
                    }
                    Spacer(Modifier.width(4.dp))
                }

                Column(horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isFromMe) accentColor.copy(alpha = 0.18f)
                                else contentColor.copy(alpha = 0.08f)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .widthIn(max = 252.dp)
                    ) {
                        if (message.isDeleted) {
                            BasicText("Message deleted", style = TextStyle(contentColor.copy(alpha = 0.5f), 13.sp))
                        } else {
                            // Try to parse as shared post
                            val sharedPost = SharedPostContent.tryParse(message.content)
                            if (sharedPost != null && sharedPost.type == "shared_post") {
                                SharedPostCard(
                                    sharedPost = sharedPost,
                                    contentColor = contentColor,
                                    accentColor = accentColor
                                )
                            } else if (!message.mediaUrl.isNullOrBlank() && message.contentType.lowercase() in chatMediaContentTypes) {
                                ChatMediaMessageContent(
                                    message = message,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    onOpenMedia = onOpenMedia
                                )
                            } else {
                                BasicText(
                                    message.content,
                                    style = TextStyle(contentColor, 14.sp)
                                )
                            }
                        }
                    }

                    // Reactions display
                    if (message.reactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .background(contentColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Group reactions by emoji and show count
                            message.reactions.groupBy { it.emoji }.forEach { (emoji, reactions) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BasicText(emoji, style = TextStyle(fontSize = 12.sp))
                                    if (reactions.size > 1) {
                                        BasicText(
                                            "${reactions.size}",
                                            style = TextStyle(contentColor.copy(alpha = 0.7f), 10.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val metaParts = buildList {
                        if (showTimestamp) add(formatTime(message.createdAt))
                        if (!deliveryStateText.isNullOrBlank()) add(deliveryStateText)
                    }
                    if (metaParts.isNotEmpty()) {
                        BasicText(
                            metaParts.joinToString(" • "),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp)
                        )
                    }
                }

                // Reply indicator (shown during swipe for own messages)
                if (isFromMe && offsetX.value < -20f) {
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(accentColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("↩", style = TextStyle(contentColor, 14.sp))
                    }
                }
            }
        }

        // Glass-styled context menu for message
        if (showMenu) {
            Box(
                modifier = Modifier
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(12f.dp) },
                        effects = {
                            vibrancy()
                            blur(16f.dp.toPx())
                        },
                        onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.5f)) }
                    )
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .width(160.dp)
                        .padding(6.dp)
                ) {
                    GlassMessageMenuItem(
                        icon = "↩️",
                        text = "Reply",
                        contentColor = contentColor,
                        onClick = { onReply(); showMenu = false }
                    )
                    GlassMessageMenuItem(
                        icon = "😊",
                        text = "React",
                        contentColor = contentColor,
                        onClick = { showReactionPicker = true; showMenu = false }
                    )
                    GlassMessageMenuItem(
                        icon = "📋",
                        text = "Copy",
                        contentColor = contentColor,
                        onClick = { onCopy(); showMenu = false }
                    )
                    if (isFromMe) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .height(1.dp)
                                .background(contentColor.copy(alpha = 0.1f))
                        )
                        GlassMessageMenuItem(
                            icon = "🗑️",
                            text = "Delete for me",
                            contentColor = Color(0xFFFF8866),
                            onClick = { onDelete(false); showMenu = false }
                        )
                        GlassMessageMenuItem(
                            icon = "🗑️",
                            text = "Delete for all",
                            contentColor = Color(0xFFFF6B6B),
                            onClick = { onDelete(true); showMenu = false }
                        )
                    }
                }
            }
        }

        // Quick reaction picker (glass-styled)
        if (showReactionPicker) {
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(20f.dp) },
                        effects = {
                            vibrancy()
                            blur(16f.dp.toPx())
                        },
                        onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.5f)) }
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickReactions.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable {
                                onReact(emoji)
                                showReactionPicker = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            emoji,
                            style = TextStyle(fontSize = 20.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassMessageMenuItem(
    icon: String,
    text: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            icon,
            style = TextStyle(fontSize = 14.sp)
        )
        Spacer(Modifier.width(8.dp))
        BasicText(
            text,
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp
            )
        )
    }
}

/**
 * Reply preview bar shown above input when replying to a message.
 */
@Composable
private fun ReplyPreview(
    message: Message,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(12f.dp) },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                },
                onDrawSurface = { drawRect(accentColor.copy(alpha = 0.15f)) }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(32.dp)
                .background(accentColor, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                "Replying to message",
                style = TextStyle(accentColor, 11.sp, fontWeight = FontWeight.Medium)
            )
            BasicText(
                message.content.take(50) + if (message.content.length > 50) "..." else "",
                style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp),
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.1f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            BasicText("✕", style = TextStyle(contentColor, 14.sp))
        }
    }
}

private fun formatChatMuteStatusLine(isMuted: Boolean, muteUntilMillis: Long): String {
    if (!isMuted) return "Notifications on"
    return try {
        val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        "Muted until ${fmt.format(Date(muteUntilMillis))}"
    } catch (_: Exception) {
        "Muted"
    }
}

private fun formatTime(iso: String): String {
    return try {
        val t = iso.indexOf('T')
        if (t > 0) iso.substring(t + 1, minOf(t + 6, iso.length)) else iso.take(5)
    } catch (_: Exception) {
        iso.take(5)
    }
}

private suspend fun Context.readChatAttachment(
    uri: Uri,
    fallbackFileName: String,
    fallbackMimeType: String
): ChatPickedAttachment = withContext(Dispatchers.IO) {
    val resolver = contentResolver
    val mimeType = resolver.getType(uri)?.takeIf { it.isNotBlank() } ?: fallbackMimeType
    val fileName = resolveAttachmentName(uri)?.let(::cleanAttachmentFileName)
        ?: cleanAttachmentFileName(fallbackFileName)
    val knownSize = resolveAttachmentSize(uri)
    if (knownSize != null && knownSize > maxChatAttachmentBytes(mimeType)) {
        throw IllegalArgumentException(chatAttachmentSizeMessage(mimeType))
    }
    val durationMs = if (mimeType.startsWith("video/")) {
        resolveVideoDurationMs(uri).also { durationMs ->
            if (durationMs != null && durationMs > CHAT_VIDEO_MAX_DURATION_MS) {
                throw IllegalArgumentException("Videos must be 90 seconds or less")
            }
        }
    } else {
        null
    }
    if (mimeType.startsWith("video/") && knownSize == null) {
        throw IllegalArgumentException("Could not read this video's size. Please choose another video.")
    }

    ChatPickedAttachment(
        uri = uri,
        fileName = fileName,
        mimeType = mimeType,
        fileSize = knownSize,
        durationMs = durationMs
    )
}

private fun Context.resolveAttachmentName(uri: Uri): String? {
    return runCatching {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    }.getOrNull()
        ?: uri.lastPathSegment
}

private fun Context.resolveVideoDurationMs(uri: Uri): Long? {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

private fun Context.resolveAttachmentSize(uri: Uri): Long? {
    val sizeFromOpenableColumns = runCatching {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && cursor.moveToFirst()) {
                cursor.getLong(sizeIndex).takeIf { it > 0L }
            } else {
                null
            }
        }
    }.getOrNull()

    if (sizeFromOpenableColumns != null) return sizeFromOpenableColumns

    val sizeFromAssetDescriptor = runCatching {
        contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length.takeIf { it > 0L }
        }
    }.getOrNull()

    if (sizeFromAssetDescriptor != null) return sizeFromAssetDescriptor

    return runCatching {
        contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.statSize.takeIf { it > 0L }
        }
    }.getOrNull()
}

private fun maxChatAttachmentBytes(mimeType: String): Long {
    return if (mimeType.startsWith("video/")) {
        CHAT_VIDEO_MAX_BYTES.toLong()
    } else {
        CHAT_ATTACHMENT_MAX_BYTES.toLong()
    }
}

private fun chatAttachmentSizeMessage(mimeType: String): String {
    return if (mimeType.startsWith("video/")) {
        "Videos must be under 150 MB"
    } else {
        "File must be under 25 MB"
    }
}

private fun cleanAttachmentFileName(name: String): String {
    return name
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .ifBlank { "attachment" }
}

private fun Message.mediaCaptionOrNull(): String? {
    val value = content.trim()
    if (value.isBlank()) return null
    val defaultCaption = when (contentType.lowercase()) {
        "pending" -> value
        "image" -> "📷 Photo"
        "video" -> "🎬 Video"
        "audio" -> "🎤 Voice message"
        else -> null
    }
    return value.takeUnless { defaultCaption != null && it == defaultCaption }
}

private fun Message.isPendingUpload(): Boolean {
    return status.equals("SENDING", ignoreCase = true) || id.startsWith("pending-")
}

private fun chatDeliveryStateText(message: Message, isFromMe: Boolean): String? {
    if (message.isPendingUpload()) return "Sending"
    return if (isFromMe) {
        when {
            message.status.equals("READ", ignoreCase = true) || message.readAt != null -> "Read"
            message.status.equals("DELIVERED", ignoreCase = true) || message.deliveredAt != null -> "Delivered"
            message.status.equals("FAILED", ignoreCase = true) -> "Failed"
            else -> "Sent"
        }
    } else {
        null
    }
}

private fun Message.toFullscreenMedia(
    mediaUrl: String,
    mediaType: String,
    backgroundStatusText: (() -> String?)? = null
): ChatFullscreenMedia {
    return ChatFullscreenMedia(
        url = mediaUrl,
        type = mediaType,
        title = fileName ?: content.takeIf { it.isNotBlank() },
        fileSize = fileSize,
        backgroundStatusText = backgroundStatusText
    )
}

private fun String.isRemoteChatMediaUrl(): Boolean {
    return startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)
}

private fun Context.chatMediaCacheFile(
    mediaUrl: String,
    fileName: String?,
    mediaType: String
): File {
    val directory = File(filesDir, "chat_media/$mediaType").apply { mkdirs() }
    val extension = chatMediaExtension(fileName, mediaUrl, mediaType)
    return File(directory, "${sha256(mediaUrl).take(32)}.$extension")
}

private fun chatMediaExtension(
    fileName: String?,
    mediaUrl: String,
    mediaType: String
): String {
    val source = fileName?.takeIf { it.isNotBlank() }
        ?: Uri.decode(Uri.parse(mediaUrl).lastPathSegment.orEmpty())
    val extension = source
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase(Locale.US)
        .takeIf { it.length in 1..8 && it.all { ch -> ch.isLetterOrDigit() } }

    return extension ?: when (mediaType.lowercase(Locale.US)) {
        "image" -> "jpg"
        "video" -> "mp4"
        "audio" -> "m4a"
        else -> "bin"
    }
}

private fun sha256(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    return bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

private fun formatChatDownloadProgress(progress: Float): String {
    val percent = (progress.coerceIn(0f, 1f) * 100f).roundToInt()
    return if (percent > 0) "Downloading $percent%" else "Downloading..."
}

private fun formatChatLocalSaveProgress(progress: Float): String {
    val percent = (progress.coerceIn(0f, 1f) * 100f).roundToInt()
    return if (percent > 0) "Saving locally $percent%" else "Saving locally..."
}

private suspend fun Context.downloadChatMediaToCache(
    mediaUrl: String,
    targetFile: File,
    onProgress: suspend (Float) -> Unit
): File = withContext(Dispatchers.IO) {
    if (targetFile.exists() && targetFile.length() > 0L) {
        return@withContext targetFile
    }

    targetFile.parentFile?.mkdirs()
    val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
    val bufferSize = 256 * 1024
    val connection = (URL(mediaUrl).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 120_000
        instanceFollowRedirects = true
    }

    try {
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IllegalStateException("Download failed ($responseCode)")
        }

        val totalBytes = connection.contentLengthLong
        var downloadedBytes = 0L
        var lastReportedProgress = 0f
        var lastProgressDispatchNs = 0L
        connection.inputStream.buffered(bufferSize).use { input ->
            tempFile.outputStream().buffered(bufferSize).use { output ->
                val buffer = ByteArray(bufferSize)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    if (totalBytes > 0L) {
                        val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        val now = System.nanoTime()
                        val shouldDispatch = progress >= 1f ||
                            progress - lastReportedProgress >= 0.01f ||
                            now - lastProgressDispatchNs >= 150_000_000L
                        if (shouldDispatch) {
                            lastReportedProgress = progress
                            lastProgressDispatchNs = now
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                }
            }
        }

        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }

        withContext(Dispatchers.Main) { onProgress(1f) }
        targetFile
    } finally {
        connection.disconnect()
        if (tempFile.exists() && !targetFile.exists()) {
            tempFile.delete()
        }
    }
}

private fun Context.openChatDocument(
    mediaUrl: String,
    fileName: String?
) {
    val rawUri = Uri.parse(mediaUrl)
    val uri = if (rawUri.scheme.equals("file", ignoreCase = true)) {
        val file = File(rawUri.path.orEmpty())
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    } else {
        rawUri
    }
    val mimeType = resolveChatDocumentMimeType(uri, fileName)
    val typedIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        startActivity(Intent.createChooser(typedIntent, "Open attachment"))
    } catch (_: ActivityNotFoundException) {
        val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(fallbackIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun Context.resolveChatDocumentMimeType(
    uri: Uri,
    fileName: String?
): String {
    contentResolver.getType(uri)?.takeIf { it.isNotBlank() }?.let { return it }
    val source = fileName?.takeIf { it.isNotBlank() }
        ?: Uri.decode(uri.lastPathSegment.orEmpty())
    val extension = source
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase(Locale.US)
    return MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension)
        ?: "application/octet-stream"
}

private fun chatAudioProgressFraction(
    positionMs: Long,
    durationMs: Long
): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

private fun formatChatPlaybackTime(
    positionMs: Long,
    durationMs: Long
): String {
    val safeDuration = durationMs.coerceAtLeast(0L)
    val safePosition = if (safeDuration > 0L) {
        positionMs.coerceIn(0L, safeDuration)
    } else {
        positionMs.coerceAtLeast(0L)
    }
    return if (safeDuration > 0L) {
        "${formatChatDuration(safePosition)} / ${formatChatDuration(safeDuration)}"
    } else {
        formatChatDuration(safePosition)
    }
}

private fun formatChatDuration(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

private fun formatChatFileSize(fileSize: Int?): String? {
    val bytes = fileSize ?: return null
    if (bytes <= 0) return null
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    }
}

private fun conversationPreviewText(
    conversation: Conversation,
    currentUserId: String?
): String {
    val lastMessage = conversation.lastMessage ?: return when {
        conversation.isMessageRequest -> "Message request"
        else -> "No messages yet"
    }

    val prefix = if (lastMessage.senderId == currentUserId) "You: " else ""
    val body = when (lastMessage.contentType.lowercase()) {
        "image" -> "sent a photo"
        "video" -> "sent a video"
        "audio" -> "sent a voice note"
        "document", "file" -> "shared a file"
        "post" -> "shared a post"
        else -> lastMessage.content
    }

    return "$prefix$body".trim()
}

/**
 * AI-powered smart reply suggestion chips.
 * Displays contextual reply suggestions that users can tap to send instantly.
 */
@Composable
private fun AiSuggestionsRow(
    suggestions: List<String>,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // AI label with sparkle icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            BasicText(
                "✨ Smart replies",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // Horizontally scrollable suggestion chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(16f.dp) },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(accentColor.copy(alpha = 0.15f))
                            }
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        suggestion,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Glass-styled chat options menu with blur effects matching app theme.
 */
@Composable
private fun GlassChatMenu(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isMuted: Boolean,
    muteStatusLine: String,
    wallpaperStatusLine: String,
    isGlassTheme: Boolean,
    onDismiss: () -> Unit,
    onViewProfile: () -> Unit,
    onSearchMessages: () -> Unit,
    onChangeWallpaper: () -> Unit,
    onMuteNotifications: () -> Unit,
    onClearChat: () -> Unit,
    onReport: () -> Unit
) {
    val appearance = currentVormexAppearance()
    val menuShape = RoundedCornerShape(18.dp)
    val menuContentColor = if (isGlassTheme) Color.White else appearance.contentColor
    val menuBorderColor =
        if (isGlassTheme) Color.White.copy(alpha = 0.14f) else appearance.overlayBorderColor
    val menuSurface: Modifier = if (isGlassTheme) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(18f.dp) },
            effects = {
                vibrancy()
                blur(24f.dp.toPx())
            },
            onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.66f)) }
        )
    } else {
        Modifier.background(
            color = appearance.overlayColor,
            shape = menuShape
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 16.dp)
                .then(menuSurface)
                .clip(menuShape)
                .background(Color.Black.copy(alpha = if (isGlassTheme) 0.28f else 0f))
                .border(1.dp, menuBorderColor, menuShape)
                .clickable(enabled = false) { }
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 228.dp, max = 280.dp)
                    .padding(8.dp)
            ) {
                GlassMenuItem(
                    iconRes = R.drawable.ic_profile,
                    text = "View profile",
                    contentColor = menuContentColor,
                    onClick = onViewProfile
                )
                GlassMenuItem(
                    iconRes = R.drawable.ic_search,
                    text = "Search messages",
                    contentColor = menuContentColor,
                    onClick = onSearchMessages
                )
                GlassMenuItem(
                    iconRes = R.drawable.ic_wallpaper,
                    text = "Chat wallpaper",
                    subtitle = wallpaperStatusLine,
                    contentColor = menuContentColor,
                    onClick = onChangeWallpaper
                )
                GlassMenuItem(
                    iconRes = if (isMuted) R.drawable.ic_notifications_off else R.drawable.ic_notifications,
                    text = if (isMuted) "Unmute notifications" else "Mute notifications",
                    subtitle = muteStatusLine,
                    contentColor = menuContentColor,
                    onClick = onMuteNotifications
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .height(1.dp)
                        .background(menuContentColor.copy(alpha = 0.15f))
                )

                GlassMenuItem(
                    iconRes = R.drawable.ic_delete,
                    text = "Delete chat",
                    contentColor = Color(0xFFFF6B6B),
                    onClick = onClearChat
                )
                GlassMenuItem(
                    iconRes = R.drawable.ic_warning,
                    text = "Report",
                    subtitle = "Safety concerns",
                    contentColor = Color(0xFFFFAA33),
                    onClick = onReport
                )
            }
        }
    }
}

@Composable
private fun GlassMenuItem(
    iconRes: Int,
    text: String,
    contentColor: Color,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = if (subtitle != null) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(contentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 15.sp
                )
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                BasicText(
                    subtitle,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun GlassWallpaperSheet(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    selectedWallpaperKey: String?,
    onDismiss: () -> Unit,
    onSelectWallpaper: (ChatWallpaper?) -> Unit
) {
    val appearance = currentVormexAppearance()
    val cardShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val sheetContentColor = if (isGlassTheme) Color.White else appearance.contentColor
    val sheetBorderColor =
        if (isGlassTheme) Color.White.copy(alpha = 0.12f) else appearance.sheetBorderColor
    val cardSurface: Modifier = if (isGlassTheme) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(24f.dp) },
            effects = {
                vibrancy()
                blur(24f.dp.toPx())
            },
            onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.7f)) }
        )
    } else {
        Modifier.background(
            color = appearance.sheetColor,
            shape = cardShape
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appearance.scrimColor)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(cardSurface)
                .clip(cardShape)
                .background(Color.Black.copy(alpha = if (isGlassTheme) 0.22f else 0f))
                .border(1.dp, sheetBorderColor, cardShape)
                .clickable(enabled = false) { }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            "Chat wallpaper",
                            style = TextStyle(
                                color = sheetContentColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(Modifier.height(4.dp))
                        BasicText(
                            "Full screen, original quality",
                            style = TextStyle(sheetContentColor.copy(alpha = 0.62f), 12.sp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(sheetContentColor.copy(alpha = 0.12f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close",
                            modifier = Modifier.size(16.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(sheetContentColor)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp)
                ) {
                    item(key = "default-wallpaper") {
                        ChatWallpaperTile(
                            wallpaper = null,
                            title = "Default",
                            selected = selectedWallpaperKey == null,
                            contentColor = sheetContentColor,
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onSelectWallpaper(null) }
                        )
                    }

                    items(chatWallpapers.chunked(2)) { rowWallpapers ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowWallpapers.forEach { wallpaper ->
                                ChatWallpaperTile(
                                    wallpaper = wallpaper,
                                    title = wallpaper.title,
                                    selected = selectedWallpaperKey == wallpaper.key,
                                    contentColor = sheetContentColor,
                                    accentColor = accentColor,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectWallpaper(wallpaper) }
                                )
                            }
                            if (rowWallpapers.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatWallpaperTile(
    wallpaper: ChatWallpaper?,
    title: String,
    selected: Boolean,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tileShape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .aspectRatio(if (wallpaper == null) 2.4f else 0.58f)
            .clip(tileShape)
            .background(contentColor.copy(alpha = 0.1f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accentColor else Color.White.copy(alpha = 0.12f),
                shape = tileShape
            )
            .clickable { onClick() }
    ) {
        if (wallpaper != null) {
            androidx.compose.foundation.Image(
                painter = painterResource(wallpaper.drawableRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.42f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "Default chat background",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        BasicText(
            title,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            style = TextStyle(
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "Selected",
                    modifier = Modifier.size(16.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                )
            }
        }
    }
}

@Composable
private fun GlassMuteDurationSheet(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    peerName: String,
    onDismiss: () -> Unit,
    onConfirmDuration: (Long) -> Unit
) {
    val options = remember {
        listOf(
            "1 hour" to 60L * 60 * 1000,
            "8 hours" to 8L * 60 * 60 * 1000,
            "1 week" to 7L * 24 * 60 * 60 * 1000,
            "1 year" to 365L * 24 * 60 * 60 * 1000
        )
    }
    var selectedMs by remember { mutableStateOf(options[0].second) }
    val appearance = currentVormexAppearance()
    val dialogContentColor = if (isGlassTheme) Color.White else appearance.contentColor
    val dialogShape = RoundedCornerShape(20.dp)
    val dialogBorderColor =
        if (isGlassTheme) Color.White.copy(alpha = 0.12f) else appearance.sheetBorderColor

    val cardSurface: Modifier = if (isGlassTheme) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(20f.dp) },
            effects = {
                vibrancy()
                blur(24f.dp.toPx())
            },
            onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.5f)) }
        )
    } else {
        Modifier.background(
            color = appearance.sheetColor,
            shape = dialogShape
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appearance.scrimColor)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .then(cardSurface)
                .clip(dialogShape)
                .border(1.dp, dialogBorderColor, dialogShape)
                .clickable(enabled = false) { }
        ) {
            Column(Modifier.padding(22.dp)) {
                BasicText(
                    "Mute notifications",
                    style = TextStyle(
                        color = dialogContentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(Modifier.height(8.dp))
                BasicText(
                    "How long should we silence alerts from $peerName? You won’t get message notifications from this chat on your device until then.",
                    style = TextStyle(dialogContentColor.copy(alpha = 0.72f), 13.sp)
                )
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (label, ms) ->
                        val selected = selectedMs == ms
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) accentColor.copy(alpha = 0.18f)
                                    else dialogContentColor.copy(alpha = 0.07f)
                                )
                                .clickable { selectedMs = ms }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BasicText(
                                label,
                                style = TextStyle(dialogContentColor, 14.sp, FontWeight.Medium)
                            )
                            if (selected) {
                                BasicText("✓", style = TextStyle(accentColor, 14.sp, FontWeight.Bold))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(dialogContentColor.copy(alpha = 0.1f))
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "Cancel",
                            style = TextStyle(dialogContentColor, 15.sp, FontWeight.Medium)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.85f))
                            .clickable {
                                onConfirmDuration(selectedMs)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "Mute",
                            style = TextStyle(Color.White, 15.sp, FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassReportChatDialog(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    reportedLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    val reasons = remember {
        listOf("Harassment or hate", "Spam or scam", "Impersonation", "Other")
    }
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }
    val appearance = currentVormexAppearance()
    val dialogContentColor = if (isGlassTheme) Color.White else appearance.contentColor
    val dialogShape = RoundedCornerShape(20.dp)
    val dialogBorderColor =
        if (isGlassTheme) Color.White.copy(alpha = 0.12f) else appearance.sheetBorderColor

    val cardSurface: Modifier = if (isGlassTheme) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(20f.dp) },
            effects = {
                vibrancy()
                blur(24f.dp.toPx())
            },
            onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.5f)) }
        )
    } else {
        Modifier.background(
            color = appearance.sheetColor,
            shape = dialogShape
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appearance.scrimColor)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .then(cardSurface)
                .clip(dialogShape)
                .border(1.dp, dialogBorderColor, dialogShape)
                .clickable(enabled = false) { }
        ) {
            Column(Modifier.padding(22.dp)) {
                BasicText(
                    "Report conversation",
                    style = TextStyle(
                        color = dialogContentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(Modifier.height(8.dp))
                BasicText(
                    "Tell us what’s wrong about your chat with $reportedLabel.",
                    style = TextStyle(dialogContentColor.copy(alpha = 0.72f), 13.sp)
                )
                Spacer(Modifier.height(14.dp))
                reasons.forEach { reason ->
                    val selected = selectedReason == reason
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) accentColor.copy(alpha = 0.16f) else dialogContentColor.copy(alpha = 0.06f)
                            )
                            .clickable { selectedReason = reason }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            if (selected) "●" else "○",
                            style = TextStyle(accentColor, 12.sp),
                            modifier = Modifier.width(20.dp)
                        )
                        BasicText(
                            reason,
                            style = TextStyle(dialogContentColor, 14.sp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                BasicText(
                    "Details (optional)",
                    style = TextStyle(dialogContentColor.copy(alpha = 0.55f), 11.sp)
                )
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = details,
                    onValueChange = { details = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(dialogContentColor.copy(alpha = 0.08f))
                        .padding(12.dp),
                    textStyle = TextStyle(dialogContentColor, 13.sp),
                    cursorBrush = SolidColor(dialogContentColor),
                    maxLines = 4
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(dialogContentColor.copy(alpha = 0.1f))
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "Cancel",
                            style = TextStyle(dialogContentColor, 15.sp, FontWeight.Medium)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.85f))
                            .clickable {
                                onSubmit(selectedReason, details.trim())
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "Submit",
                            style = TextStyle(Color.White, 15.sp, FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Glass-styled confirmation dialog matching app theme.
 */
@Composable
private fun GlassConfirmDialog(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color = accentColor,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogContentColor = Color.White

    // Full screen dismissible overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Dialog box
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(20f.dp) },
                    effects = {
                        vibrancy()
                        blur(24f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.5f)) }
                )
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = false) { } // Prevent click-through
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                BasicText(
                    title,
                    style = TextStyle(
                        color = dialogContentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Message
                BasicText(
                    message,
                    style = TextStyle(
                        color = dialogContentColor.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                )

                Spacer(Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(dialogContentColor.copy(alpha = 0.1f))
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "Cancel",
                            style = TextStyle(
                                color = dialogContentColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Confirm button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(confirmColor.copy(alpha = 0.8f))
                            .clickable { onConfirm() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            confirmText,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders shared post content as a formatted card instead of raw JSON.
 */
@Composable
private fun SharedPostCard(
    sharedPost: SharedPostContent,
    contentColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .widthIn(min = 220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .clickable {
                // Open post URL when clicked
                if (sharedPost.postUrl.isNotEmpty()) {
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(sharedPost.postUrl)
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open post", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .padding(12.dp)
    ) {
        // Shared post label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            BasicText(
                "📤 Shared Post",
                style = TextStyle(
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        // Post preview image FIRST (if available) - more prominent
        if (!sharedPost.mediaUrl.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(contentColor.copy(alpha = 0.1f))
            ) {
                AsyncImage(
                    model = sharedPost.mediaUrl,
                    contentDescription = "Post media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        // Post preview text
        if (sharedPost.preview.isNotEmpty()) {
            // Clean up preview text - remove color/markdown formatting
            val cleanPreview = sharedPost.preview
                .replace(Regex("\\[color:#[0-9a-fA-F]+\\]"), "")
                .replace("[/color]", "")
                .replace("\\n", "\n")
                .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1") // Remove bold markers
                .trim()
                .take(120)

            BasicText(
                cleanPreview + if (sharedPost.preview.length > 120) "..." else "",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
        }

        // Author info row at bottom
        sharedPost.author?.let { author ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(contentColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                // Profile image
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.2f))
                ) {
                    if (!author.profileImage.isNullOrEmpty()) {
                        AsyncImage(
                            model = author.profileImage,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Author name
                    if (!author.name.isNullOrEmpty()) {
                        BasicText(
                            author.name,
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Author username
                    if (!author.username.isNullOrEmpty()) {
                        BasicText(
                            "@${author.username}",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
                // Arrow icon
                BasicText(
                    "→",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
