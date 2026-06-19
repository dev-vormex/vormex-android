@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

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
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
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
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.kyant.backdrop.catalog.deeplink.VormexDeepLinks
import com.kyant.backdrop.catalog.linkedin.FullScreenVideoPlayer
import com.kyant.backdrop.catalog.linkedin.VerificationBadge
import com.kyant.backdrop.catalog.linkedin.VerificationBadgeSize
import com.kyant.backdrop.catalog.linkedin.VideoPlayer
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.Conversation
import com.kyant.backdrop.catalog.network.models.GroupMessageShortcut
import com.kyant.backdrop.catalog.network.models.Message
import com.kyant.backdrop.catalog.network.models.MySafetyReport
import com.kyant.backdrop.catalog.network.models.SharedPostContent
import com.kyant.backdrop.catalog.linkedin.currentVormexAppearance
import com.kyant.backdrop.catalog.linkedin.hasVerificationBadge
import com.kyant.backdrop.catalog.linkedin.verificationBadgeStyle
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

private data class ChatFullscreenMedia(
    val url: String,
    val type: String,
    val title: String?,
    val fileSize: Int?,
    val backgroundStatusText: (() -> String?)? = null
)

private data class ChatPeerSafetyState(
    val openReport: MySafetyReport? = null,
    val isBlocked: Boolean = false,
    val isLoading: Boolean = false
)

private sealed class ChatInboxEntry(open val sortAt: String) {
    data class Direct(val conversation: Conversation) : ChatInboxEntry(
        conversation.lastMessage?.createdAt ?: conversation.updatedAt
    )

    data class GroupShortcut(val shortcut: GroupMessageShortcut) : ChatInboxEntry(
        shortcut.latestMessage?.createdAt ?: shortcut.lastActivityAt
    )
}

private fun MySafetyReport.isOpenSafetyReport(): Boolean {
    val normalizedStatus = status.uppercase(Locale.US)
    return normalizedStatus == "PENDING" || normalizedStatus == "UNDER_REVIEW"
}

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
    openChatInitialDraft: String? = null,
    onConsumedOpenConversation: () -> Unit = {},
    onConsumedOpenChat: () -> Unit = {},
    onInChatThread: (Boolean) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToReel: (SharedPostContent) -> Unit = {},
    onOpenGroupShortcut: (String) -> Unit = {},
    onCreateGroup: () -> Unit = {}
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
                    viewModel.ensureConversationsLoaded(forceRefresh = true)
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
        viewModel.ensureConversationsLoaded(forceRefresh = true)
    }

    LaunchedEffect(openChatWithUserId, openChatInitialDraft) {
        val userId = openChatWithUserId ?: return@LaunchedEffect
        viewModel.openChatWithUser(userId, openChatInitialDraft)
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
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToReel = onNavigateToReel
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
            viewModel = viewModel,
            onOpenGroupShortcut = onOpenGroupShortcut,
            onCreateGroup = onCreateGroup
        )
    }
}

@Composable
private fun ChatListScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    viewModel: ChatViewModel,
    onOpenGroupShortcut: (String) -> Unit,
    onCreateGroup: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val isRefreshingConversations =
        uiState.isLoadingConversations && (uiState.conversations.isNotEmpty() || uiState.groupShortcuts.isNotEmpty())

    val searchedConversations = if (searchQuery.isNotBlank()) {
        uiState.conversations.filter { conv ->
            val name = conv.otherParticipant.name ?: conv.otherParticipant.username ?: ""
            name.contains(searchQuery, ignoreCase = true)
        }
    } else {
        uiState.conversations
    }
    val searchedGroupShortcuts = if (searchQuery.isNotBlank()) {
        uiState.groupShortcuts.filter { shortcut ->
            shortcut.name.contains(searchQuery, ignoreCase = true) ||
                shortcut.description?.contains(searchQuery, ignoreCase = true) == true
        }
    } else {
        uiState.groupShortcuts
    }
    val requestConversations = searchedConversations.filter { it.isMessageRequest }
    val mixedEntries = buildList {
        searchedConversations
            .filterNot { it.isMessageRequest }
            .forEach { add(ChatInboxEntry.Direct(it)) }
        searchedGroupShortcuts.forEach { add(ChatInboxEntry.GroupShortcut(it)) }
    }.sortedByDescending { it.sortAt }
    val hasVisibleConversations = requestConversations.isNotEmpty() || mixedEntries.isNotEmpty()

    val inboxSurface = contentColor.copy(alpha = if (isGlassTheme) 0.028f else 0.02f)
    val cardColor = contentColor.copy(alpha = if (isGlassTheme) 0.032f else 0.024f)
    val cardBorder = contentColor.copy(alpha = if (isGlassTheme) 0.055f else 0.04f)
    val quickEntries = mixedEntries.take(10)
    val showQuickEntries = quickEntries.isNotEmpty() && !showSearchBar

    LaunchedEffect(showSearchBar) {
        if (showSearchBar) {
            searchFocusRequester.requestFocus()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 12.dp)
    ) {
        Spacer(Modifier.height(2.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                BasicText(
                    "Vormex",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                BasicText(
                    "Messages",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    ),
                    maxLines = 1
                )
            }
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onCreateGroup),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_compose_outline),
                    contentDescription = "New message",
                    modifier = Modifier.size(25.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.86f))
                )
            }
        }

        if (isRefreshingConversations) {
            ChatSyncStatusPill(
                text = "Refreshing chats...",
                contentColor = contentColor,
                accentColor = accentColor
            )
            Spacer(Modifier.height(8.dp))
        }

        if (showSearchBar) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(inboxSurface)
                    .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Search",
                    modifier = Modifier.size(18.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.52f))
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(searchFocusRequester),
                    textStyle = TextStyle(contentColor, 14.sp, FontWeight.Medium),
                    cursorBrush = SolidColor(contentColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            BasicText(
                                "Search",
                                style = TextStyle(contentColor.copy(alpha = 0.42f), 14.sp, FontWeight.Medium)
                            )
                        }
                        innerTextField()
                    }
                )
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Close search",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            searchQuery = ""
                            showSearchBar = false
                        },
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.52f))
                )
            }
        }

        if (showQuickEntries) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ChatQuickSearchChip(
                    contentColor = contentColor,
                    cardColor = cardColor,
                    borderColor = cardBorder,
                    onClick = { showSearchBar = true }
                )
                quickEntries.forEach { entry ->
                    when (entry) {
                        is ChatInboxEntry.Direct -> ChatQuickConversationChip(
                            name = entry.conversation.otherParticipant.name
                                ?: entry.conversation.otherParticipant.username
                                ?: "Unknown",
                            imageUrl = entry.conversation.otherParticipant.profileImage,
                            unreadCount = entry.conversation.unreadCount,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            cardColor = cardColor,
                            showFrame = false,
                            onClick = { viewModel.selectConversation(entry.conversation) }
                        )
                        is ChatInboxEntry.GroupShortcut -> ChatQuickConversationChip(
                            name = entry.shortcut.name,
                            imageUrl = entry.shortcut.iconImage ?: entry.shortcut.coverImage,
                            unreadCount = 0,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            cardColor = cardColor,
                            onClick = { onOpenGroupShortcut(entry.shortcut.groupId) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (uiState.isLoadingConversations && uiState.conversations.isEmpty() && uiState.groupShortcuts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ChatConversationListLoadingState(contentColor = contentColor)
            }
        } else if (uiState.error != null && uiState.conversations.isEmpty() && uiState.groupShortcuts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ChatInboxErrorState(
                    message = uiState.error!!,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onRetry = { viewModel.ensureConversationsLoaded(forceRefresh = true) }
                )
            }
        } else if (!hasVisibleConversations) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ChatInboxEmptyState(
                    searchQuery = searchQuery,
                    contentColor = contentColor
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 112.dp, top = 2.dp)
            ) {
                items(requestConversations, key = { "request_${it.id}" }) { conv ->
                    SwipeableConversationRow(
                        conversation = conv,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        cardColor = cardColor,
                        borderColor = cardBorder,
                        currentUserId = uiState.currentUserId,
                        onClick = { viewModel.selectConversation(conv) }
                    )
                }
                items(mixedEntries, key = { entry ->
                    when (entry) {
                        is ChatInboxEntry.Direct -> "direct_${entry.conversation.id}"
                        is ChatInboxEntry.GroupShortcut -> "group_${entry.shortcut.groupId}"
                    }
                }) { entry ->
                    when (entry) {
                        is ChatInboxEntry.Direct -> {
                            SwipeableConversationRow(
                                conversation = entry.conversation,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                cardColor = cardColor,
                                borderColor = cardBorder,
                                currentUserId = uiState.currentUserId,
                                onClick = { viewModel.selectConversation(entry.conversation) }
                            )
                        }
                        is ChatInboxEntry.GroupShortcut -> {
                            GroupShortcutConversationRow(
                                shortcut = entry.shortcut,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                cardColor = cardColor,
                                borderColor = cardBorder,
                                currentUserId = uiState.currentUserId,
                                onClick = { onOpenGroupShortcut(entry.shortcut.groupId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatQuickSearchChip(
    contentColor: Color,
    cardColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(58.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(cardColor)
                .border(1.dp, borderColor, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search",
                modifier = Modifier.size(20.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor.copy(alpha = 0.74f))
            )
        }
        Spacer(Modifier.height(7.dp))
        BasicText(
            "Search",
            style = TextStyle(contentColor.copy(alpha = 0.78f), 11.sp, FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChatQuickConversationChip(
    name: String,
    imageUrl: String?,
    unreadCount: Int,
    contentColor: Color,
    accentColor: Color,
    cardColor: Color,
    showFrame: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(58.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChatAvatar(
            name = name,
            imageUrl = imageUrl,
            contentColor = contentColor,
            accentColor = accentColor,
            highlighted = unreadCount > 0,
            showOnlineDot = false,
            size = 50,
            backgroundColor = if (showFrame) cardColor else Color.Transparent,
            showFrame = showFrame
        )
        Spacer(Modifier.height(7.dp))
        BasicText(
            firstChatName(name),
            style = TextStyle(contentColor.copy(alpha = 0.82f), 11.sp, FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChatAvatar(
    name: String,
    imageUrl: String?,
    contentColor: Color,
    accentColor: Color,
    highlighted: Boolean,
    showOnlineDot: Boolean,
    size: Int,
    backgroundColor: Color = Color.Unspecified,
    showFrame: Boolean = true
) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val avatarBackground = when {
        backgroundColor != Color.Unspecified -> backgroundColor
        highlighted -> accentColor.copy(alpha = 0.18f)
        else -> contentColor.copy(alpha = 0.1f)
    }

    Box(Modifier.size(size.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(avatarBackground)
                .border(
                    width = if (!showFrame) 0.dp else if (highlighted) 1.5.dp else 1.dp,
                    color = if (!showFrame) Color.Transparent else if (highlighted) accentColor.copy(alpha = 0.36f) else contentColor.copy(alpha = 0.08f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                BasicText(
                    initial,
                    style = TextStyle(
                        color = if (highlighted) accentColor else contentColor.copy(alpha = 0.82f),
                        fontSize = (size * 0.34f).sp,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        }
        if (showOnlineDot) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.22f).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF35D07F))
                    .border(2.dp, avatarBackground, CircleShape)
            )
        }
    }
}

@Composable
private fun SwipeableConversationRow(
    conversation: Conversation,
    contentColor: Color,
    accentColor: Color,
    cardColor: Color,
    borderColor: Color,
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

    Box(
            Modifier
                .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(26.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.015f), spotColor = Color.Black.copy(alpha = 0.02f))
            .clip(RoundedCornerShape(26.dp))
            .background(cardColor)
            .border(1.dp, borderColor, RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatAvatar(
                name = avatarInitial,
                imageUrl = other.profileImage,
                contentColor = contentColor,
                accentColor = accentColor,
                highlighted = showUnreadAccent || conversation.isMessageRequest,
                showOnlineDot = false,
                size = 48,
                backgroundColor = contentColor.copy(alpha = 0.018f),
                showFrame = false
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            other.name ?: other.username ?: "Unknown",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 15.sp,
                                fontWeight = if (showUnreadAccent) FontWeight.Black else FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (other.hasVerificationBadge()) {
                            Spacer(Modifier.width(4.dp))
                            VerificationBadge(
                                verified = true,
                                badgeStyle = other.profileBadgeStyle,
                                isPremium = other.isPremium,
                                size = VerificationBadgeSize.Small
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        formatTime(rowTime),
                        style = TextStyle(
                            color = if (showUnreadAccent) accentColor else contentColor.copy(alpha = 0.44f),
                            fontSize = 11.sp,
                            fontWeight = if (showUnreadAccent) FontWeight.SemiBold else FontWeight.Medium
                        )
                    )
                }
                Spacer(Modifier.height(5.dp))
                BasicText(
                    previewText,
                    style = TextStyle(
                        color = contentColor.copy(alpha = if (showUnreadAccent) 0.72f else 0.54f),
                        fontSize = 12.sp,
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
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                        style = TextStyle(Color.White, 10.sp, fontWeight = FontWeight.Black)
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupShortcutConversationRow(
    shortcut: GroupMessageShortcut,
    contentColor: Color,
    accentColor: Color,
    cardColor: Color,
    borderColor: Color,
    currentUserId: String?,
    onClick: () -> Unit
) {
    val imageUrl = shortcut.iconImage ?: shortcut.coverImage
    val avatarInitial = shortcut.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "G"
    val previewText = groupShortcutPreviewText(shortcut, currentUserId)

    Box(
            Modifier
                .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(26.dp), clip = false, ambientColor = Color.Black.copy(alpha = 0.015f), spotColor = Color.Black.copy(alpha = 0.02f))
            .clip(RoundedCornerShape(26.dp))
            .background(cardColor)
            .border(1.dp, borderColor, RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatAvatar(
                name = avatarInitial,
                imageUrl = imageUrl,
                contentColor = contentColor,
                accentColor = accentColor,
                highlighted = true,
                showOnlineDot = false,
                size = 48
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        shortcut.name,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        formatTime(shortcut.lastActivityAt),
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.44f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Spacer(Modifier.height(5.dp))
                BasicText(
                    previewText,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.54f),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
        subtitle = "Try a different name, username, or group."
    } else {
        title = "No conversations yet"
        subtitle = "Start a chat or add a group from Groups."
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

@Composable
private fun ChatInboxErrorState(
    message: String,
    contentColor: Color,
    accentColor: Color,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        BasicText(
            "Chats could not sync",
            style = TextStyle(
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        )
        BasicText(
            message,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        )
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accentColor.copy(alpha = 0.16f))
                .clickable(onClick = onRetry)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                "Retry",
                style = TextStyle(accentColor, 13.sp, fontWeight = FontWeight.SemiBold)
            )
        }
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
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToReel: (SharedPostContent) -> Unit = {}
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
    val threadStatusLine = when {
        uiState.typingUserId != null -> "typing..."
        uiState.isLoadingMessages && uiState.messages.isEmpty() -> "loading messages..."
        else -> null
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
    var showAttachmentAiActions by remember(conv.id) { mutableStateOf(false) }
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
    var peerSafetyState by remember(conv.id, conv.otherParticipant.id) {
        mutableStateOf(ChatPeerSafetyState())
    }

    suspend fun loadChatPeerSafetyState(): ChatPeerSafetyState {
        val reports = ApiClient.getMyReports(context, limit = 50)
            .getOrNull()
            ?.reports
            .orEmpty()
        val openReport = reports.firstOrNull { report ->
            report.reportType.equals("CHAT", ignoreCase = true) &&
                report.conversationId == conv.id &&
                report.isOpenSafetyReport()
        }
        val isBlocked = ApiClient.getBlockedUsers(context)
            .getOrNull()
            ?.blocks
            ?.any { it.blockedUserId == conv.otherParticipant.id }
            ?: false
        return ChatPeerSafetyState(
            openReport = openReport,
            isBlocked = isBlocked,
            isLoading = false
        )
    }

    suspend fun refreshChatPeerSafetyState() {
        peerSafetyState = peerSafetyState.copy(isLoading = true)
        runCatching { loadChatPeerSafetyState() }
            .onSuccess { peerSafetyState = it }
            .onFailure { peerSafetyState = peerSafetyState.copy(isLoading = false) }
    }

    LaunchedEffect(conv.id, conv.otherParticipant.id) {
        refreshChatPeerSafetyState()
    }

    LaunchedEffect(showReportChatSheet, conv.id, conv.otherParticipant.id) {
        if (showReportChatSheet) {
            refreshChatPeerSafetyState()
        }
    }

    LaunchedEffect(conv.id, uiState.initialDraftMessage) {
        val draft = uiState.initialDraftMessage?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (inputText.isBlank()) {
            inputText = draft
            viewModel.sendTyping(true)
            viewModel.clearAiSuggestions()
        }
        viewModel.clearInitialDraftMessage()
    }

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
        if (composerAiBusyLabel != null) return
        composerAiScope.launch {
            composerAiBusyLabel = "$label…"
            composerAiStatus = null
            val result = runCatching { block() }
                .getOrElse { VormexAiTextResult.Failure(it.message ?: "AI failed.") }
            applyComposerAiResult(label, result, onSuccess)
        }
    }

    fun updateComposerTextFromAi(text: String) {
        inputText = text
        viewModel.sendTyping(text.isNotEmpty())
        viewModel.clearAiSuggestions()
    }

    fun fixComposerGrammar() {
        if (inputText.isBlank()) return
        runComposerAi(
            label = "Grammar fix",
            block = {
                aiGateway.proofread(
                    text = inputText,
                    surface = VormexAiSurface.CHAT,
                    allowCloudFallback = true
                )
            },
            onSuccess = ::updateComposerTextFromAi
        )
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

    fun handleSystemKeyboardGif(uri: Uri, hintedMimeType: String?): Boolean {
        val resolvedMimeType = hintedMimeType
            ?.takeIf { it.isNotBlank() }
            ?: context.contentResolver.getType(uri)?.takeIf { it.isNotBlank() }
        val uriText = uri.toString().lowercase(Locale.US).substringBefore('?')
        val isGif = resolvedMimeType.equals("image/gif", ignoreCase = true) || uriText.endsWith(".gif")
        if (!isGif) return false

        handlePickedAttachment(
            uri = uri,
            fallbackFileName = "keyboard.gif",
            fallbackMimeType = "image/gif"
        )
        return true
    }

    val systemGifReceiver = object : ReceiveContentListener {
        override fun onReceive(transferableContent: TransferableContent): TransferableContent? {
            val clipData = transferableContent.clipEntry.clipData
            return transferableContent.consume { item ->
                val uri = item.uri ?: return@consume false
                handleSystemKeyboardGif(
                    uri = uri,
                    hintedMimeType = resolveChatClipMimeType(context, clipData, uri)
                )
            }
        }
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
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.selectConversation(null) },
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.ic_back),
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(threadContentColor.copy(alpha = 0.88f))
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(threadContentColor.copy(alpha = if (selectedWallpaper != null) 0.1f else 0.025f))
                                .border(1.dp, threadContentColor.copy(alpha = 0.045f), CircleShape)
                                .clickable { onNavigateToProfile(conv.otherParticipant.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!conv.otherParticipant.profileImage.isNullOrBlank()) {
                                AsyncImage(
                                    model = conv.otherParticipant.profileImage,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                BasicText(
                                    (conv.otherParticipant.name ?: conv.otherParticipant.username ?: "?")
                                        .trim()
                                        .firstOrNull()
                                        ?.uppercaseChar()
                                        ?.toString()
                                        ?: "?",
                                    style = TextStyle(
                                        threadContentColor.copy(alpha = 0.82f),
                                        14.sp,
                                        FontWeight.Black
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable { onNavigateToProfile(conv.otherParticipant.id) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                BasicText(
                                    conv.otherParticipant.name ?: conv.otherParticipant.username ?: "Unknown",
                                    style = TextStyle(threadContentColor, 16.sp, fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                VerificationBadge(
                                    verified = conv.otherParticipant.hasVerificationBadge(),
                                    badgeStyle = conv.otherParticipant.profileBadgeStyle,
                                    isPremium = conv.otherParticipant.isPremium,
                                    size = VerificationBadgeSize.Small
                                )
                            }
                            threadStatusLine?.let { statusLine ->
                                Spacer(Modifier.height(2.dp))
                                BasicText(
                                    statusLine,
                                    style = TextStyle(threadContentColor.copy(alpha = 0.52f), 11.sp, FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showChatMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.ic_more),
                                contentDescription = "Menu",
                                modifier = Modifier.size(22.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(threadContentColor.copy(alpha = 0.86f))
                            )
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(threadContentColor.copy(alpha = if (selectedWallpaper != null) 0.13f else 0.045f))
                    )
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
                                val isFromMe = msg.isFromCurrentUser(
                                    currentUserId = uiState.currentUserId,
                                    conversation = uiState.selectedConversation
                                )
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
                                    onRetry = { viewModel.retryMessage(msg) },
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Message", msg.content))
                                        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpenMedia = { media ->
                                        fullscreenMedia = media
                                    },
                                    onOpenReel = onNavigateToReel,
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
                ChatSmartReplyStatusBar(
                    message = when {
                        uiState.isPreparingAiSuggestions -> "Preparing on-device AI for smart replies…"
                        uiState.isLoadingAiSuggestions -> "Generating smart replies…"
                        else -> uiState.aiStatusMessage ?: ""
                    },
                    contentColor = threadContentColor,
                    accentColor = accentColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
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
                val threadSummaryInput = remember(uiState.messages, uiState.currentUserId) {
                    buildChatSummaryInput(uiState.messages, uiState.currentUserId)
                }
                val isFixingGrammar = composerAiBusyLabel == "Grammar fix…"
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
                                    onSuccess = ::updateComposerTextFromAi
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
                                    onSuccess = ::updateComposerTextFromAi
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
                                    onSuccess = ::updateComposerTextFromAi
                                )
                            }
                        )
                    )
                    add(
                        VormexAiChipAction(
                            label = "Grammar",
                            enabled = inputText.isNotBlank(),
                            onClick = ::fixComposerGrammar
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
                                    onSuccess = ::updateComposerTextFromAi
                                )
                            }
                        )
                    )
                }
                if (showAttachmentCard) {
                    ChatAttachmentCard(
                        backdrop = backdrop,
                        contentColor = threadContentColor,
                        accentColor = accentColor,
                        isGlassTheme = isGlassTheme,
                        aiActions = composerAiActions,
                        showAiActions = showAttachmentAiActions,
                        onToggleAiActions = { showAttachmentAiActions = !showAttachmentAiActions },
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
                val chatAppearance = currentVormexAppearance()
                val chatComposerAccent = accentColor
                val chatComposerSurface = when {
                    selectedWallpaper != null -> Color.Black.copy(alpha = 0.46f)
                    isGlassTheme -> chatAppearance.inputColor.copy(alpha = 0.68f)
                    else -> chatAppearance.inputColor
                }
                val chatComposerText = if (selectedWallpaper != null) Color.White else chatAppearance.contentColor
                val chatComposerBorder = when {
                    selectedWallpaper != null -> Color.White.copy(alpha = 0.18f)
                    else -> chatAppearance.inputBorderColor
                }
                val chatComposerShadow = if (chatAppearance.isDarkTheme || selectedWallpaper != null) 3.dp else 7.dp
                val chatComposerPlaceholder = chatComposerText.copy(
                    alpha = if (chatAppearance.isDarkTheme || selectedWallpaper != null) 0.6f else 0.54f
                )
                val canSendMessage = inputText.isNotBlank()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(threadContentColor.copy(alpha = if (selectedWallpaper != null) 0.22f else 0.08f))
                )
                Spacer(Modifier.height(7.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .shadow(chatComposerShadow, CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(chatComposerSurface)
                            .border(1.dp, chatComposerBorder, CircleShape)
                            .clickable {
                                val nextAttachmentState = !showAttachmentCard
                                showAttachmentCard = nextAttachmentState
                                if (!nextAttachmentState) {
                                    showAttachmentAiActions = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ic_plus),
                            contentDescription = "Attach",
                            modifier = Modifier.size(20.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(chatComposerText.copy(alpha = 0.86f))
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .shadow(chatComposerShadow, RoundedCornerShape(22.dp), clip = false)
                            .clip(RoundedCornerShape(22.dp))
                            .background(chatComposerSurface)
                            .border(1.dp, chatComposerBorder, RoundedCornerShape(22.dp))
                            .padding(start = 14.dp, top = 5.dp, end = 6.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                viewModel.sendTyping(it.isNotEmpty())
                                if (it.isNotEmpty()) {
                                    viewModel.clearAiSuggestions()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .contentReceiver(systemGifReceiver),
                            textStyle = TextStyle(chatComposerText, 14.sp),
                            cursorBrush = SolidColor(chatComposerAccent),
                            singleLine = false,
                            maxLines = 3,
                            decorationBox = { innerTextField ->
                                Box(Modifier.padding(vertical = 8.dp)) {
                                    if (inputText.isEmpty()) {
                                        BasicText(
                                            "Message...",
                                            style = TextStyle(chatComposerPlaceholder, 14.sp)
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendCurrentMessage() })
                        )
                        Spacer(Modifier.width(4.dp))
                        val inlineActionIsGrammar = inputText.isNotBlank()
                        val inlineActionEnabled = if (inlineActionIsGrammar) composerAiBusyLabel == null else true
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inlineActionIsGrammar) {
                                        chatComposerAccent.copy(alpha = 0.1f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable(
                                    enabled = inlineActionEnabled,
                                    onClick = {
                                        if (inlineActionIsGrammar) {
                                            fixComposerGrammar()
                                        } else {
                                            startVoiceMessage()
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFixingGrammar && inlineActionIsGrammar) {
                                CircularProgressIndicator(
                                    color = chatComposerAccent,
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(
                                        if (inlineActionIsGrammar) R.drawable.ic_sparkles else R.drawable.ic_mic
                                    ),
                                    contentDescription = if (inlineActionIsGrammar) "Fix grammar" else "Voice message",
                                    modifier = Modifier.size(if (inlineActionIsGrammar) 15.dp else 16.dp),
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                        if (inlineActionEnabled) chatComposerAccent else chatComposerAccent.copy(alpha = 0.42f)
                                    )
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSendMessage) chatComposerAccent else chatComposerAccent.copy(alpha = 0.44f)
                            )
                            .clickable(enabled = canSendMessage) { sendCurrentMessage() },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ic_send),
                            contentDescription = "Send",
                            modifier = Modifier
                                .size(17.dp)
                                .offset(x = 1.dp, y = (-1).dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                        )
                    }
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
                existingReport = peerSafetyState.openReport,
                isBlocked = peerSafetyState.isBlocked,
                isSafetyStateLoading = peerSafetyState.isLoading,
                onDismiss = { showReportChatSheet = false },
                onUnblock = {
                    reportScope.launch {
                        peerSafetyState = peerSafetyState.copy(isLoading = true)
                        ApiClient.unblockUser(context, conv.otherParticipant.id)
                            .onSuccess {
                                peerSafetyState = peerSafetyState.copy(
                                    isBlocked = false,
                                    isLoading = false
                                )
                                Toast.makeText(
                                    context,
                                    "Unblocked ${conv.otherParticipant.name ?: "this user"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .onFailure { e ->
                                peerSafetyState = peerSafetyState.copy(isLoading = false)
                                Toast.makeText(
                                    context,
                                    e.message ?: "Could not unblock user",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                },
                onSubmit = { reason, details, blockUser ->
                    reportScope.launch {
                        peerSafetyState = peerSafetyState.copy(isLoading = true)
                        ApiClient.reportChat(
                            context,
                            conv.id,
                            reason,
                            details,
                            blockUser = blockUser
                        ).onSuccess {
                            Toast.makeText(
                                context,
                                "Thanks — we received your report and will review it.",
                                Toast.LENGTH_LONG
                            ).show()
                            refreshChatPeerSafetyState()
                        }.onFailure { e ->
                            peerSafetyState = peerSafetyState.copy(isLoading = false)
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

private fun resolveChatClipMimeType(
    context: Context,
    clipData: ClipData,
    uri: Uri
): String? {
    context.contentResolver.getType(uri)?.takeIf { it.isNotBlank() }?.let { return it }

    val description = clipData.description
    for (index in 0 until description.mimeTypeCount) {
        val mimeType = description.getMimeType(index)
        if (mimeType.equals("image/gif", ignoreCase = true)) {
            return mimeType
        }
    }

    return null
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
    aiActions: List<VormexAiChipAction>,
    showAiActions: Boolean,
    onToggleAiActions: () -> Unit,
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
                circleColor = accentColor,
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
        Spacer(Modifier.height(14.dp))
        ChatAttachmentDivider(color = cardContentColor)
        Spacer(Modifier.height(12.dp))
        ChatAttachmentAiButton(
            contentColor = cardContentColor,
            accentColor = accentColor,
            expanded = showAiActions,
            onClick = onToggleAiActions
        )
        if (showAiActions) {
            Spacer(Modifier.height(12.dp))
            ChatAttachmentDivider(color = cardContentColor)
            Spacer(Modifier.height(10.dp))
            VormexAiChipRow(
                actions = aiActions,
                contentColor = cardContentColor,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChatAttachmentDivider(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color.copy(alpha = 0.12f))
    )
}

@Composable
private fun ChatAttachmentAiButton(
    contentColor: Color,
    accentColor: Color,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_sparkles),
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(accentColor)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                "AI",
                style = TextStyle(contentColor, 13.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
            BasicText(
                "Rewrite, grammar, summarize",
                style = TextStyle(contentColor.copy(alpha = 0.58f), 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        BasicText(
            if (expanded) "Hide" else "Choose",
            style = TextStyle(accentColor, 12.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
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
                .background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_chat_audio),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(accentColor)
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

    // For documents only — images use Coil directly, videos stream via ExoPlayer
    val isDocumentType = type != "image" && type != "video" && type != "audio"
    val cacheFile = remember(mediaUrl, message.fileName, type) {
        if (isRemoteMedia && (isDocumentType || type == "audio")) {
            context.chatMediaCacheFile(mediaUrl, message.fileName, type)
        } else {
            null
        }
    }
    var localDocumentUrl by remember(mediaUrl) {
        mutableStateOf(cacheFile?.takeIf { it.exists() }?.toURI()?.toString())
    }
    var isDownloading by remember(mediaUrl) { mutableStateOf(false) }
    var downloadProgress by remember(mediaUrl) { mutableStateOf(0f) }
    var downloadError by remember(mediaUrl) { mutableStateOf<String?>(null) }

    // Images: always use the CDN URL directly — Coil handles caching
    // Videos: always stream from CDN URL — ExoPlayer handles buffering
    // Documents/Audio: use local cache if available
    val displayMediaUrl = when (type) {
        "image", "video" -> mediaUrl
        else -> localDocumentUrl ?: mediaUrl
    }

    // Only show status for uploads or document downloads — NOT for images/videos
    val mediaStatusText = when {
        isUploading -> "Sending..."
        isDocumentType && isDownloading -> formatChatDownloadProgress(downloadProgress)
        isDocumentType && downloadError != null -> "Tap to retry"
        else -> null
    }

    fun openFullscreen(url: String) {
        onOpenMedia(
            message.toFullscreenMedia(
                mediaUrl = url,
                mediaType = type,
                backgroundStatusText = null
            )
        )
    }

    fun openMedia() {
        if (isUploading) return
        openFullscreen(displayMediaUrl)
    }

    fun openDocument() {
        if (isUploading) return
        if (isRemoteMedia && localDocumentUrl == null) {
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
                    localDocumentUrl = localUrl
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
                // Best practice: Coil's AsyncImage loads directly from CDN URL.
                // Coil handles disk cache, memory cache, and progressive loading natively.
                // No manual download needed — images appear instantly from cache on revisit.
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
                }
            }
            "video" -> {
                // Best practice: ExoPlayer streams video directly from CDN URL.
                // No manual download needed — ExoPlayer buffers and plays progressively.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    VideoPlayer(
                        videoUrl = displayMediaUrl,
                        modifier = Modifier.fillMaxSize(),
                        autoPlay = false,
                        showControls = false,
                        contentColor = contentColor,
                        onFullScreenClick = { openMedia() }
                    )
                    if (!isUploading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            ChatMediaStatusChip(
                                text = "Play",
                                contentColor = Color.White
                            )
                        }
                    }
                    if (isUploading) {
                        ChatMediaUploadingOverlay(contentColor = contentColor)
                    }
                }
            }
            "audio" -> {
                ChatAudioMessageContent(
                    mediaUrl = displayMediaUrl,
                    mediaStatusText = mediaStatusText,
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
                    actionText = if (isRemoteMedia && localDocumentUrl == null) "Download" else "Open",
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
    mediaStatusText: String?,
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

    val progress = chatAudioProgressFraction(positionMs, durationMs)
    val statusText: String = when {
        isUploading -> "Sending"
        mediaStatusText?.contains("download", ignoreCase = true) == true -> "Download"
        mediaStatusText?.contains("retry", ignoreCase = true) == true -> "Retry"
        mediaStatusText?.contains("%") == true -> mediaStatusText.orEmpty()
        durationMs > 0L -> formatChatDuration(durationMs)
        positionMs > 0L -> formatChatDuration(positionMs)
        else -> "0:00"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(23.dp))
            .background(Color(0xFF111218))
            .clickable { onOpen() }
            .padding(start = 9.dp, top = 8.dp, end = 11.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .clickable(enabled = !isUploading) {
                    if (
                        mediaStatusText?.contains("download", ignoreCase = true) == true ||
                        mediaStatusText?.contains("retry", ignoreCase = true) == true
                    ) {
                        onOpen()
                    } else if (isPlaying) {
                        player.pause()
                    } else {
                        if (isEnded) {
                            player.seekTo(0L)
                        }
                        player.play()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                contentDescription = if (isPlaying) "Pause voice message" else "Play voice message",
                modifier = Modifier.size(17.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
            )
        }
        Spacer(Modifier.width(10.dp))
        ChatVoiceWaveform(
            progress = progress,
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
        )
        Spacer(Modifier.width(10.dp))
        BasicText(
            statusText,
            style = TextStyle(
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChatVoiceWaveform(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val waveform = listOf(
        0.36f, 0.58f, 0.82f, 0.72f, 0.96f, 0.46f,
        0.66f, 0.86f, 0.52f, 0.42f, 0.72f, 0.34f,
        0.78f, 0.48f, 0.4f, 0.58f, 0.36f, 0.7f,
        0.42f, 0.54f, 0.32f, 0.64f
    )
    val normalizedProgress = progress.coerceIn(0f, 1f)
    val activeIndex = when {
        normalizedProgress > 0f -> (normalizedProgress * (waveform.size - 1)).roundToInt()
        else -> 8
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        waveform.forEachIndexed { index, level ->
            val isActive = index <= activeIndex
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((9f + level * 27f).dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Color.White.copy(alpha = if (isActive) 0.96f else 0.34f)
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
    onRetry: () -> Unit = {},
    onCopy: () -> Unit = {},
    onOpenMedia: (ChatFullscreenMedia) -> Unit = {},
    onOpenReel: (SharedPostContent) -> Unit = {},
    currentUserId: String? = null,
    deliveryStateText: String? = null
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 80.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var showMenu by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    val sharedPost = if (message.isDeleted) {
        null
    } else {
        SharedPostContent.tryParse(message.content) ?: message.asSharedReelContentOrNull()
    }
    val isSharedReel = sharedPost?.type == "shared_reel"
    val hasMediaContent = !message.mediaUrl.isNullOrBlank() &&
        message.contentType.lowercase() in chatMediaContentTypes
    val isAudioMessage = !message.isDeleted && hasMediaContent && message.contentType.lowercase() == "audio"
    val isEmojiOnlyMessage = !message.isDeleted &&
        sharedPost == null &&
        !hasMediaContent &&
        isSystemEmojiOnlyMessage(message.content)
    val emojiFontSizeSp = if (isEmojiOnlyMessage) systemEmojiMessageFontSizeSp(message.content) else 14
    val outgoingBubbleColor = Color(0xFF151A22)
    val bubbleTextColor = if (isFromMe && !isEmojiOnlyMessage) Color.White else contentColor

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
                    val messageBubbleModifier = if (isEmojiOnlyMessage) {
                        Modifier
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                            .widthIn(max = 252.dp)
                    } else if (isAudioMessage) {
                        Modifier
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                            .widthIn(min = 204.dp, max = 260.dp)
                    } else if (sharedPost != null) {
                        Modifier
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                            .widthIn(
                                min = if (isSharedReel) 148.dp else 220.dp,
                                max = if (isSharedReel) 170.dp else 268.dp
                            )
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isFromMe) outgoingBubbleColor
                                else contentColor.copy(alpha = 0.08f)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .widthIn(max = 252.dp)
                    }

                    Box(
                        modifier = messageBubbleModifier
                    ) {
                        if (message.isDeleted) {
                            BasicText("Message deleted", style = TextStyle(bubbleTextColor.copy(alpha = 0.5f), 13.sp))
                        } else {
                            if (sharedPost != null && (sharedPost.type == "shared_post" || sharedPost.type == "shared_reel")) {
                                SharedPostCard(
                                    sharedPost = sharedPost,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    onOpenReel = onOpenReel
                                )
                            } else if (hasMediaContent) {
                                ChatMediaMessageContent(
                                    message = message,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    onOpenMedia = onOpenMedia
                                )
                            } else {
                                val messageTextStyle = if (isEmojiOnlyMessage) {
                                    TextStyle(
                                        color = contentColor,
                                        fontSize = emojiFontSizeSp.sp,
                                        lineHeight = (emojiFontSizeSp + 4).sp
                                    )
                                } else {
                                    TextStyle(bubbleTextColor, 14.sp)
                                }

                                if (isEmojiOnlyMessage) {
                                    BasicText(
                                        message.content,
                                        style = messageTextStyle
                                    )
                                } else {
                                    val formattedMessageContent = remember(message.content, bubbleTextColor) {
                                        parseChatRichText(message.content, bubbleTextColor)
                                    }
                                    BasicText(
                                        formattedMessageContent,
                                        style = messageTextStyle
                                    )
                                }
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
                    if (isFromMe && message.status.equals("FAILED", ignoreCase = true)) {
                        GlassMessageMenuItem(
                            icon = "↻",
                            text = "Retry",
                            contentColor = accentColor,
                            onClick = { onRetry(); showMenu = false }
                        )
                    }
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
    if (status.equals("FAILED", ignoreCase = true)) return false
    return status.equals("SENDING", ignoreCase = true) || id.startsWith("pending-")
}

private fun chatDeliveryStateText(message: Message, isFromMe: Boolean): String? {
    if (message.status.equals("FAILED", ignoreCase = true)) return "Failed"
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

private fun conversationPreviewText(
    conversation: Conversation,
    currentUserId: String?
): String {
    val lastMessage = conversation.lastMessage ?: return when {
        conversation.isMessageRequest -> "Message request"
        else -> "No messages yet"
    }

    val prefix = if (conversation.isSenderCurrentUser(lastMessage.senderId, currentUserId)) "You: " else ""
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

private fun groupShortcutPreviewText(
    shortcut: GroupMessageShortcut,
    currentUserId: String?
): String {
    val latest = shortcut.latestMessage ?: return "${shortcut.memberCount} members"
    val body = latest.preview.ifBlank {
        when (latest.contentType.lowercase()) {
            "image" -> "sent a photo"
            "video" -> "sent a video"
            "audio" -> "sent a voice note"
            "document", "file" -> "shared a file"
            else -> "sent a message"
        }
    }
    val senderPrefix = if (latest.senderId == currentUserId) "You" else latest.senderName
    return "$senderPrefix: $body"
}

private fun firstChatName(name: String): String {
    return name
        .trim()
        .split(Regex("\\s+"))
        .firstOrNull()
        ?.take(12)
        ?.ifBlank { "Chat" }
        ?: "Chat"
}

private fun Message.isFromCurrentUser(
    currentUserId: String?,
    conversation: Conversation?
): Boolean {
    return conversation?.isSenderCurrentUser(senderId, currentUserId)
        ?: (!currentUserId.isNullOrBlank() && senderId == currentUserId)
}

@Composable
private fun ChatSmartReplyStatusBar(
    message: String,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    primaryAction: VormexAiChipAction? = null,
    secondaryAction: VormexAiChipAction? = null
) {
    val displayMessage = when {
        message.contains("unavailable", ignoreCase = true) && secondaryAction != null -> "On-device unavailable"
        message.contains("Preparing on-device AI", ignoreCase = true) -> "Preparing AI"
        message.contains("Generating smart replies", ignoreCase = true) -> "Generating replies"
        else -> message
    }
    val actions = listOfNotNull(primaryAction, secondaryAction)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(contentColor.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.9f))
            )
            Spacer(Modifier.width(6.dp))
            BasicText(
                displayMessage,
                modifier = Modifier.widthIn(max = 210.dp),
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        actions.forEach { action ->
            BasicText(
                text = chatSmartReplyActionLabel(action.label),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (action.enabled) accentColor.copy(alpha = 0.18f)
                        else contentColor.copy(alpha = 0.06f)
                    )
                    .clickable(enabled = action.enabled, onClick = action.onClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = TextStyle(
                    color = if (action.enabled) accentColor else contentColor.copy(alpha = 0.42f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun chatSmartReplyActionLabel(label: String): String {
    return when (label) {
        "Prepare on-device AI" -> "Prepare AI"
        "Use cloud smart replies" -> "Use cloud"
        else -> label
    }
}

private fun Conversation.isSenderCurrentUser(
    senderId: String,
    currentUserId: String?
): Boolean {
    return senderId == currentParticipantId(currentUserId)
}

private fun Conversation.currentParticipantId(currentUserId: String?): String? {
    if (!currentUserId.isNullOrBlank() && (currentUserId == participant1Id || currentUserId == participant2Id)) {
        return currentUserId
    }

    return when (otherParticipant.id) {
        participant1Id -> participant2Id
        participant2Id -> participant1Id
        else -> currentUserId?.takeIf { it.isNotBlank() }
    }
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
    existingReport: MySafetyReport?,
    isBlocked: Boolean,
    isSafetyStateLoading: Boolean,
    onDismiss: () -> Unit,
    onUnblock: () -> Unit,
    onSubmit: (reason: String, details: String, blockUser: Boolean) -> Unit
) {
    val reasons = remember {
        listOf("Harassment or hate", "Spam or scam", "Impersonation", "Other")
    }
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }
    var blockUser by remember { mutableStateOf(false) }
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
                if (existingReport != null || isBlocked || isSafetyStateLoading) {
                    ChatReportSafetyStateContent(
                        reportedLabel = reportedLabel,
                        existingReport = existingReport,
                        isBlocked = isBlocked,
                        isLoading = isSafetyStateLoading,
                        dialogContentColor = dialogContentColor,
                        accentColor = accentColor,
                        onDismiss = onDismiss,
                        onUnblock = onUnblock
                    )
                } else {
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
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(dialogContentColor.copy(alpha = if (blockUser) 0.14f else 0.07f))
                            .clickable { blockUser = !blockUser }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            if (blockUser) "●" else "○",
                            style = TextStyle(accentColor, 12.sp),
                            modifier = Modifier.width(20.dp)
                        )
                        BasicText(
                            "Also block this user",
                            style = TextStyle(dialogContentColor, 14.sp)
                        )
                    }
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
                                    onSubmit(selectedReason, details.trim(), blockUser)
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
}

@Composable
private fun ChatReportSafetyStateContent(
    reportedLabel: String,
    existingReport: MySafetyReport?,
    isBlocked: Boolean,
    isLoading: Boolean,
    dialogContentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onUnblock: () -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.shield_icon))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    val title = when {
        existingReport != null -> "Report under review"
        isBlocked -> "User blocked"
        else -> "Checking safety status"
    }
    val statusLine = existingReport?.let {
        "You already submitted this report. Trust & Safety is reviewing it now."
    } ?: if (isBlocked) {
        "$reportedLabel is blocked on your account."
    } else {
        "Checking whether this conversation already has a report or block."
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(108.dp)
        )
        Spacer(Modifier.height(4.dp))
        BasicText(
            title,
            style = TextStyle(
                color = dialogContentColor,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        BasicText(
            statusLine,
            style = TextStyle(
                color = dialogContentColor.copy(alpha = 0.72f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (isBlocked) {
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(dialogContentColor.copy(alpha = 0.07f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                BasicText(
                    "While blocked:",
                    style = TextStyle(dialogContentColor, 13.sp, FontWeight.SemiBold)
                )
                SafetyEffectLine("No messages from $reportedLabel will be received.", dialogContentColor, accentColor)
                SafetyEffectLine("Other accounts linked to the same app install or browser signal are blocked from reaching you too.", dialogContentColor, accentColor)
                SafetyEffectLine("They cannot start new DMs or message requests with you.", dialogContentColor, accentColor)
                SafetyEffectLine("Story replies or reactions that would create DMs are blocked.", dialogContentColor, accentColor)
                SafetyEffectLine("Follows, connection requests, mentions, notifications, people search, matching, and feed recommendations are filtered.", dialogContentColor, accentColor)
                SafetyEffectLine("Unblocking does not remove the report from admin review.", dialogContentColor, accentColor)
            }
        }

        if (isLoading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = accentColor,
                strokeWidth = 2.dp
            )
        }

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
                    "Close",
                    style = TextStyle(dialogContentColor, 15.sp, FontWeight.Medium)
                )
            }
            if (isBlocked) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = if (isLoading) 0.42f else 0.85f))
                        .clickable(enabled = !isLoading) { onUnblock() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "Unblock",
                        style = TextStyle(Color.White, 15.sp, FontWeight.Medium)
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetyEffectLine(
    text: String,
    dialogContentColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicText(
            "-",
            style = TextStyle(accentColor, 13.sp, FontWeight.SemiBold)
        )
        BasicText(
            text,
            style = TextStyle(dialogContentColor.copy(alpha = 0.72f), 12.sp),
            modifier = Modifier.weight(1f)
        )
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

private fun Message.asSharedReelContentOrNull(): SharedPostContent? {
    if (contentType.lowercase() != "reel") return null
    val reelId = Regex("\"reelId\"\\s*:\\s*\"([^\"]+)\"")
        .find(content)
        ?.groupValues
        ?.getOrNull(1)
        ?: content.trim().takeIf { it.isNotBlank() && !it.startsWith("{") }
    if (reelId.isNullOrBlank() && mediaUrl.isNullOrBlank()) return null

    return SharedPostContent(
        type = "shared_reel",
        reelId = reelId.orEmpty(),
        reelUrl = reelId?.let { VormexDeepLinks.reelUrl(it) }.orEmpty(),
        mediaUrl = mediaUrl
    )
}

/**
 * Renders shared post content as a formatted card instead of raw JSON.
 */
@Composable
private fun SharedPostCard(
    sharedPost: SharedPostContent,
    contentColor: Color,
    accentColor: Color,
    onOpenReel: (SharedPostContent) -> Unit = {}
) {
    val context = LocalContext.current
    val isSharedReel = sharedPost.type == "shared_reel"
    val cleanPreview = cleanSharedPreviewText(
        raw = sharedPost.preview.ifBlank { sharedPost.title ?: sharedPost.caption.orEmpty() },
        maxLength = if (isSharedReel) 88 else 180
    )
    val author = sharedPost.author
    val authorName = author?.name?.takeIf { it.isNotBlank() }
        ?: author?.username?.takeIf { it.isNotBlank() }
    val authorHandle = author?.username?.takeIf { it.isNotBlank() }
    val authorImage = author?.profileImage?.takeIf { it.isNotBlank() }
    val previewMediaUrl = if (isSharedReel) {
        sharedPost.previewGifUrl?.takeIf { it.isNotBlank() }
            ?: sharedPost.mediaUrl?.takeIf { it.isNotBlank() }
    } else {
        sharedPost.mediaUrl?.takeIf { it.isNotBlank() }
    }

    fun openSharedContent() {
        val reelId = if (isSharedReel) {
            sharedPost.reelId.ifBlank {
                sharedPost.reelUrl
                    .takeIf { it.isNotBlank() }
                    ?.let { url -> runCatching { VormexDeepLinks.extractReelId(Uri.parse(url)) }.getOrNull() }
                    .orEmpty()
            }
        } else {
            ""
        }
        if (reelId.isNotBlank()) {
            onOpenReel(sharedPost.copy(reelId = reelId))
            return
        }

        val targetUrl = if (isSharedReel) {
            sharedPost.reelUrl.ifBlank {
                sharedPost.reelId
                    .takeIf { it.isNotBlank() }
                    ?.let { VormexDeepLinks.reelUrl(it) }
                    .orEmpty()
            }
        } else {
            sharedPost.postUrl.ifBlank {
                sharedPost.postId
                    .takeIf { it.isNotBlank() }
                    ?.let(VormexDeepLinks::postUrl)
                    .orEmpty()
            }
        }
        if (targetUrl.isNotEmpty()) {
            try {
                val opened = if (isSharedReel) {
                    VormexDeepLinks.openReelUrl(context, targetUrl)
                } else {
                    VormexDeepLinks.openPostUrl(context, targetUrl)
                }
                if (!opened) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isSharedReel) "Could not open reel" else "Could not open post",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    if (isSharedReel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111218))
                .clickable { openSharedContent() }
        ) {
            if (!previewMediaUrl.isNullOrBlank()) {
                AsyncImage(
                    model = previewMediaUrl,
                    contentDescription = "Reel preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(contentColor.copy(alpha = 0.1f))
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.48f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_play_arrow),
                    contentDescription = "Open reel",
                    modifier = Modifier
                        .size(18.dp)
                        .offset(x = 1.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.46f))
                    .padding(horizontal = 9.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BasicText(
                        "Reel",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    formatSharedReelDuration(sharedPost.durationSeconds)?.let { duration ->
                        BasicText(
                            duration,
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                if (cleanPreview.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        cleanPreview,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 14.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!authorName.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText(
                            authorName,
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (author?.hasVerificationBadge() == true) {
                            Spacer(Modifier.width(4.dp))
                            VerificationBadge(
                                verified = true,
                                badgeStyle = author.verificationBadgeStyle(),
                                size = VerificationBadgeSize.Micro
                            )
                        }
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(contentColor.copy(alpha = 0.055f))
            .clickable { openSharedContent() }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(contentColor.copy(alpha = 0.075f))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (!authorImage.isNullOrBlank()) {
                    AsyncImage(
                        model = authorImage,
                        contentDescription = "Author profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    BasicText(
                        authorName?.firstOrNull()?.uppercaseChar()?.toString() ?: "P",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        authorName ?: "Vormex",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (author?.hasVerificationBadge() == true) {
                        Spacer(Modifier.width(4.dp))
                        VerificationBadge(
                            verified = true,
                            badgeStyle = author.verificationBadgeStyle(),
                            size = VerificationBadgeSize.Micro
                        )
                    }
                }
                BasicText(
                    authorHandle?.let { "@$it" } ?: "Shared from Vormex",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.52f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!previewMediaUrl.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            AsyncImage(
                model = previewMediaUrl,
                contentDescription = "Post media",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(contentColor.copy(alpha = 0.08f)),
                contentScale = ContentScale.Crop
            )
        }

        if (cleanPreview.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            BasicText(
                cleanPreview,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.94f),
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun cleanSharedPreviewText(raw: String, maxLength: Int): String {
    val clean = raw
        .replace(Regex("\\[color:#[0-9a-fA-F]+\\]"), "")
        .replace("[/color]", "")
        .replace("\\n", "\n")
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        .replace(Regex("\\s+"), " ")
        .trim()
    return if (clean.length > maxLength) {
        clean.take(maxLength).trimEnd() + "..."
    } else {
        clean
    }
}

private fun formatSharedReelDuration(durationSeconds: Int): String? {
    if (durationSeconds <= 0) return null
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
