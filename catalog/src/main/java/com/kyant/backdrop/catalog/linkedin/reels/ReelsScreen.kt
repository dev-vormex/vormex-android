package com.kyant.backdrop.catalog.linkedin.reels

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.linkedin.CommentIcon
import com.kyant.backdrop.catalog.linkedin.SaveLottieEffect
import com.kyant.backdrop.catalog.linkedin.VerificationBadge
import com.kyant.backdrop.catalog.linkedin.VerificationBadgeSize
import com.kyant.backdrop.catalog.linkedin.hasVerificationBadge
import com.kyant.backdrop.catalog.linkedin.posts.formatTimeAgo
import com.kyant.backdrop.catalog.network.models.Reel
import com.kyant.backdrop.catalog.network.models.ReelAuthor
import com.kyant.backdrop.catalog.network.models.ReelComment
import com.kyant.backdrop.catalog.network.models.MentionUser
import com.kyant.backdrop.catalog.ui.blockTouchPassthrough
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REELS_LOAD_MORE_THRESHOLD_PAGES = 5

/**
 * Reels Preview Section for Home Feed
 * Horizontal scrollable row of reel thumbnails with preview on hover/long-press
 */
@Composable
fun ReelsPreviewSection(
    reels: List<Reel>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLoading: Boolean = false,
    onReelClick: (Int) -> Unit = {},
    onSeeAllClick: () -> Unit = {},
    onCreateClick: () -> Unit = {},
    reduceAnimations: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Reels icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "▶",
                        style = TextStyle(Color.White, 12.sp, FontWeight.Bold)
                    )
                }
                Spacer(Modifier.width(8.dp))
                BasicText(
                    "Trending Reels",
                    style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = 0.14f))
                        .clickable { onCreateClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        "+ Create",
                        style = TextStyle(accentColor, 13.sp, FontWeight.SemiBold)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSeeAllClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        "See all →",
                        style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                    )
                }
            }
        }
        
        // Loading state
        if (isLoading) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) {
                    ReelThumbnailSkeleton()
                }
            }
        } else if (reels.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "No reels yet",
                    style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                )
            }
        } else {
            // Reels thumbnails
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(reels, key = { _, reel -> reel.id }) { index, reel ->
                    ReelThumbnailCard(
                        reel = reel,
                        backdrop = backdrop,
                        reduceAnimations = reduceAnimations,
                        onClick = { onReelClick(index) }
                    )
                }
                
                // "Explore More" card at the end
                item {
                    ExploreMoreCard(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        onClick = onSeeAllClick
                    )
                }
            }
        }
    }
}

/**
 * Individual reel thumbnail card for the preview section
 */
@Composable
private fun ReelThumbnailCard(
    reel: Reel,
    backdrop: LayerBackdrop,
    reduceAnimations: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val posterUrl = remember(reel.thumbnailUrl, reel.previewGifUrl) {
        reel.thumbnailUrl?.takeIf { it.isNotBlank() }
            ?: reel.previewGifUrl?.takeIf { it.isNotBlank() }
    }
    val thumbnailRequest = remember(context, posterUrl, reduceAnimations) {
        posterUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(if (reduceAnimations) 0 else 160)
                .build()
        }
    }
    
    Box(
        modifier = modifier
            .width(120.dp)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        ReelThumbnailFallback()

        thumbnailRequest?.let { request ->
            AsyncImage(
                model = request,
                contentDescription = reel.title ?: "Reel",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Gradient overlay at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )
        
        // Stats overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            // Views count
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText("▶", style = TextStyle(Color.White, 10.sp))
                Spacer(Modifier.width(4.dp))
                BasicText(
                    formatCount(reel.viewsCount),
                    style = TextStyle(Color.White, 11.sp, FontWeight.Medium)
                )
            }
            
            Spacer(Modifier.height(2.dp))
            
            // Author
            BasicText(
                "@${reel.author.username ?: reel.author.name}",
                style = TextStyle(Color.White.copy(alpha = 0.9f), 10.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Duration badge
        reel.durationSeconds.takeIf { it > 0 }?.let { duration ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                BasicText(
                    formatDuration(duration),
                    style = TextStyle(Color.White, 9.sp, FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
private fun ReelThumbnailFallback() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF111827),
                        Color(0xFF312E81),
                        Color(0xFFE11D48)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.vormex_logo),
            contentDescription = "Vormex",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.58f)
                .aspectRatio(1f)
        )
    }
}

/**
 * Skeleton loader for reel thumbnail
 */
@Composable
private fun ReelThumbnailSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(120.dp)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Gray.copy(alpha = 0.2f))
    )
}

/**
 * "Explore More" card at the end of reels row
 */
@Composable
private fun ExploreMoreCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(120.dp)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0))
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText("▶", style = TextStyle(Color.White, 18.sp, FontWeight.Bold))
            }
            Spacer(Modifier.height(8.dp))
            BasicText(
                "Explore\nReels",
                style = TextStyle(Color.White, 12.sp, FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// ==================== Full Screen Reels Feed ====================

/**
 * Full-screen reels feed with vertical paging (Instagram-like)
 * Features:
 * - Vertical snap-to-item paging
 * - Video preloading for next 2-3 reels
 * - Smooth playback transitions
 * - Double-tap to like
 */
@kotlin.OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsFeedScreen(
    reels: List<Reel>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onLike: (String) -> Unit = {},
    onSave: (String) -> Unit = {},
    onComment: (String) -> Unit = {},
    onShare: (String) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onTrackView: (String, Long, Boolean) -> Unit = { _, _, _ -> },
    onReelChanged: (Int) -> Unit = {},
    playerForIndex: (Int) -> ExoPlayer? = { null },
    onPlaybackError: (Int) -> Boolean = { false },
    onRetryPlayback: (Int) -> Unit = {},
    onPausePlayback: (Boolean) -> Unit = {},
    onResumePlayback: (Int) -> Unit = {},
    onReleasePlayback: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    isDraftPreview: Boolean = false,
    isPublishingDraft: Boolean = false,
    onPublishDraftPreview: (String) -> Unit = {},
    onCreateClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hostActivity = remember(context) { context.findActivity() }
    val openProgress = remember { Animatable(0f) }

    DisposableEffect(hostActivity) {
        hostActivity?.let { activity ->
            val window = activity.window
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        } ?: onDispose { }
    }

    LaunchedEffect(Unit) {
        openProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { reels.size }
    )
    
    // Track current visible reel for autoplay
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    
    LaunchedEffect(currentPage) {
        onReelChanged(currentPage)

        // Load more when near the end
        if (currentPage >= reels.size - REELS_LOAD_MORE_THRESHOLD_PAGES) {
            onLoadMore()
        }
    }

    LaunchedEffect(reels) {
        if (reels.isNotEmpty()) {
            onResumePlayback(currentPage.coerceIn(0, reels.lastIndex))
        }
    }

    DisposableEffect(lifecycleOwner, currentPage) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> onPausePlayback(false)
                Lifecycle.Event.ON_RESUME -> onResumePlayback(currentPage)
                Lifecycle.Event.ON_STOP -> onPausePlayback(true)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onReleasePlayback()
        }
    }
    
    BackHandler(enabled = true, onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blockTouchPassthrough()
            .graphicsLayer {
                alpha = openProgress.value
                val scale = 0.96f + (openProgress.value * 0.04f)
                scaleX = scale
                scaleY = scale
            }
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { index -> reels[index].id },
            pageSize = PageSize.Fill,
            pageSpacing = 0.dp
        ) { page ->
            val reel = reels[page]
            val isActive = page == currentPage
            val player = if (isActive) playerForIndex(page) else null

            ReelCard(
                pageIndex = page,
                reel = reel,
                player = player,
                isActive = isActive,
                onLike = { onLike(reel.id) },
                onSave = { onSave(reel.id) },
                onComment = { onComment(reel.id) },
                onShare = { onShare(reel.id) },
                onProfileClick = { onProfileClick(reel.author.id) },
                onPlaybackError = { onPlaybackError(page) },
                onRetryPlayback = { onRetryPlayback(page) },
                onTrackView = { watchTime, completed ->
                    onTrackView(reel.id, watchTime, completed)
                }
            )
        }

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.34f))
                .clickable(onClick = onDismiss)
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(23.dp)
            )
        }

        val currentReelForActions = reels.getOrNull(currentPage)
        if (isDraftPreview && currentReelForActions != null) {
            val canPublish = !isPublishingDraft
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (canPublish) Color(0xFF0095F6) else Color.White.copy(alpha = 0.18f))
                    .clickable(enabled = canPublish) {
                        onPublishDraftPreview(currentReelForActions.id)
                    }
                    .padding(horizontal = 14.dp)
                    .align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.UploadFile,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
                BasicText(
                    if (isPublishingDraft) "Publishing" else "Publish",
                    style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onCreateClick() }
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "+",
                    style = TextStyle(Color.White, 26.sp, FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * Individual Reel Card with video player
 */
@OptIn(UnstableApi::class)
@Composable
private fun ReelCard(
    pageIndex: Int,
    reel: Reel,
    player: ExoPlayer?,
    isActive: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onProfileClick: () -> Unit,
    onPlaybackError: () -> Boolean,
    onRetryPlayback: () -> Unit,
    onTrackView: (Long, Boolean) -> Unit
) {
    val context = LocalContext.current
    val networkAvailable by rememberNetworkAvailable()
    
    var isPlaying by remember(reel.id) { mutableStateOf(false) }
    var isLoading by remember(reel.id) { mutableStateOf(true) }
    var hasRenderedFirstFrame by remember(reel.id) { mutableStateOf(false) }
    var playbackErrorMessage by remember(reel.id) { mutableStateOf<String?>(null) }
    var waitingForNetwork by remember(reel.id) { mutableStateOf(false) }
    var showLikeAnimation by remember(reel.id) { mutableStateOf(false) }
    var likeAnimationTrigger by remember(reel.id) { mutableIntStateOf(0) }
    var isLiked by remember(reel.id) { mutableStateOf(reel.isLiked) }
    var likesCount by remember(reel.id) { mutableIntStateOf(reel.likesCount) }
    var isSaved by remember(reel.id) { mutableStateOf(reel.isSaved) }
    var saveEffectTrigger by remember(reel.id) { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val mediaPulseScale = remember(reel.id) { Animatable(1f) }
    val likeBurstScale = remember(reel.id) { Animatable(0.82f) }
    var horizontalDragDistance by remember(reel.id) { mutableFloatStateOf(0f) }
    
    // Watch time tracking
    var watchStartTime by remember(reel.id) { mutableLongStateOf(0L) }
    var viewTracked by remember(reel.id) { mutableStateOf(false) }
    val posterUrl = remember(reel.id, reel.thumbnailUrl, reel.previewGifUrl) {
        reel.thumbnailUrl?.takeIf { it.isNotBlank() }
            ?: reel.previewGifUrl?.takeIf { it.isNotBlank() }
    }

    LaunchedEffect(reel.id, reel.isLiked, reel.likesCount, reel.isSaved) {
        isLiked = reel.isLiked
        likesCount = reel.likesCount
        isSaved = reel.isSaved
    }

    LaunchedEffect(likeAnimationTrigger) {
        if (likeAnimationTrigger == 0) return@LaunchedEffect
        likeBurstScale.stop()
        likeBurstScale.snapTo(0.82f)
        likeBurstScale.animateTo(
            targetValue = 1.32f,
            animationSpec = tween(durationMillis = 180)
        )
        likeBurstScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(900)
        showLikeAnimation = false
    }

    LaunchedEffect(player, reel.id) {
        isPlaying = player?.isPlaying == true
        isLoading = player == null || player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_BUFFERING
        hasRenderedFirstFrame = player?.videoSize?.height?.let { it > 0 } == true
        playbackErrorMessage = null
        waitingForNetwork = false
    }
    
    LaunchedEffect(isActive, player, reel.id) {
        if (isActive && player != null) {
            watchStartTime = System.currentTimeMillis()
            viewTracked = false
            isLoading = !hasRenderedFirstFrame
            
            // Track view after 3 seconds
            delay(3000)
            if (!viewTracked && isActive && player.isPlaying) {
                viewTracked = true
                val watchTime = System.currentTimeMillis() - watchStartTime
                onTrackView(watchTime, false)
            }
        }
    }

    DisposableEffect(player, reel.id) {
        val exoPlayer = player
        if (exoPlayer == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isLoading =
                        playbackState == Player.STATE_IDLE ||
                            playbackState == Player.STATE_BUFFERING

                    if (playbackState == Player.STATE_READY) {
                        playbackErrorMessage = null
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onRenderedFirstFrame() {
                    hasRenderedFirstFrame = true
                    isLoading = false
                    playbackErrorMessage = null
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    hasRenderedFirstFrame = false
                    if (onPlaybackError()) {
                        isLoading = true
                        playbackErrorMessage = null
                        waitingForNetwork = false
                    } else {
                        waitingForNetwork = !networkAvailable
                        playbackErrorMessage = if (!networkAvailable) {
                            "Waiting for network..."
                        } else {
                            error.message ?: "Failed to load reel"
                        }
                        isLoading = false
                    }
                }
            }

            exoPlayer.addListener(listener)
            onDispose {
                exoPlayer.removeListener(listener)
            }
        }
    }

    LaunchedEffect(networkAvailable, waitingForNetwork, isActive, pageIndex) {
        if (networkAvailable && waitingForNetwork && isActive) {
            waitingForNetwork = false
            playbackErrorMessage = null
            onRetryPlayback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(pageIndex, isPlaying, player) {
                detectTapGestures(
                    onTap = {
                        val exoPlayer = player ?: return@detectTapGestures
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    onDoubleTap = {
                        coroutineScope.launch {
                            mediaPulseScale.stop()
                            mediaPulseScale.snapTo(1f)
                            mediaPulseScale.animateTo(
                                targetValue = 1.14f,
                                animationSpec = tween(durationMillis = 160)
                            )
                            mediaPulseScale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                        likeAnimationTrigger++
                        showLikeAnimation = true
                        if (!isLiked) {
                            isLiked = true
                            likesCount++
                            onLike()
                        }
                    }
                )
            }
            .pointerInput(reel.author.id) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragDistance = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        horizontalDragDistance += dragAmount
                    },
                    onDragEnd = {
                        if (horizontalDragDistance < -72f) {
                            onProfileClick()
                        }
                        horizontalDragDistance = 0f
                    },
                    onDragCancel = { horizontalDragDistance = 0f }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = mediaPulseScale.value
                    scaleY = mediaPulseScale.value
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setKeepContentOnPlayerReset(true)
                        setEnableComposeSurfaceSyncWorkaround(true)
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
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
                },
                onRelease = { view ->
                    if (view.player != null) {
                        view.player = null
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            AnimatedVisibility(
                visible = !hasRenderedFirstFrame,
                enter = fadeIn(tween(90)),
                exit = fadeOut(tween(120)),
                modifier = Modifier.fillMaxSize()
            ) {
                if (posterUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(posterUrl)
                            .crossfade(false)
                            .build(),
                        contentDescription = reel.title ?: "Reel poster",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ReelThumbnailFallback()
                }
            }
        }

        if (isLoading && hasRenderedFirstFrame) {
            BufferingChip(
                text = "Buffering...",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }

        if (playbackErrorMessage != null) {
            PlaybackStatusOverlay(
                message = playbackErrorMessage ?: "Failed to load reel",
                showRetry = networkAvailable,
                onRetry = {
                    playbackErrorMessage = null
                    isLoading = true
                    onRetryPlayback()
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Double-tap heart animation
        AnimatedVisibility(
            visible = showLikeAnimation,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            key(likeAnimationTrigger) {
                ReelLikeBurstAnimation(
                    modifier = Modifier
                        .size(260.dp)
                        .graphicsLayer {
                            scaleX = likeBurstScale.value
                            scaleY = likeBurstScale.value
                        }
                )
            }
        }
        
        // Bottom gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )
        
        // Reel info (bottom left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 76.dp, end = 80.dp)
                .navigationBarsPadding()
        ) {
            // Author info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onProfileClick
                )
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                ) {
                    reel.author.profileImage?.let { img ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(img)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                reel.author.name?.firstOrNull()?.uppercase() ?: "U",
                                style = TextStyle(Color.White, 16.sp, FontWeight.Bold)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.width(10.dp))
                
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BasicText(
                            reel.author.name ?: "User",
                            style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 160.dp)
                        )
                        VerificationBadge(
                            verified = reel.author.hasVerificationBadge(),
                            size = VerificationBadgeSize.Small
                        )
                    }
                    BasicText(
                        "@${reel.author.username ?: ""}",
                        style = TextStyle(Color.White.copy(alpha = 0.7f), 12.sp)
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                // Follow button (if not following)
                if (!reel.author.isFollowing) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .clickable { /* Follow action */ }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        BasicText(
                            "Follow",
                            style = TextStyle(Color.Black, 12.sp, FontWeight.SemiBold)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Caption
            reel.caption?.let { caption ->
                BasicText(
                    caption,
                    style = TextStyle(Color.White, 13.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Hashtags
            if (reel.hashtags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                BasicText(
                    reel.hashtags.take(3).joinToString(" ") { "#$it" },
                    style = TextStyle(Color.White.copy(alpha = 0.9f), 12.sp, FontWeight.Medium)
                )
            }
            
            // Audio (if any)
            reel.audio?.let { audio ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText("♪", style = TextStyle(Color.White, 12.sp))
                    Spacer(Modifier.width(6.dp))
                    BasicText(
                        "${audio.title} • ${audio.artist ?: "Original"}",
                        style = TextStyle(Color.White.copy(alpha = 0.8f), 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        // Action buttons (right side)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 100.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Like button
            ActionButton(
                icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = formatCount(likesCount),
                isActive = isLiked,
                activeColor = Color(0xFFFF3040),
                onClick = {
                    val nextLiked = !isLiked
                    isLiked = nextLiked
                    likesCount = (likesCount + if (nextLiked) 1 else -1).coerceAtLeast(0)
                    if (nextLiked) {
                        likeAnimationTrigger++
                        showLikeAnimation = true
                    }
                    onLike()
                }
            )
            
            // Comment button
            ActionButton(
                label = formatCount(reel.commentsCount),
                customIcon = { tint ->
                    CommentIcon(
                        color = tint,
                        size = 31.dp
                    )
                },
                onClick = onComment
            )
            
            // Save button
            ActionButton(
                icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                label = formatCount(reel.savesCount),
                isActive = isSaved,
                activeColor = Color(0xFFA855F7),
                saveEffectTrigger = saveEffectTrigger,
                onClick = {
                    if (!isSaved) saveEffectTrigger++
                    isSaved = !isSaved
                    onSave()
                }
            )

            // Share button
            ActionButton(
                icon = Icons.AutoMirrored.Outlined.Send,
                label = formatCount(reel.sharesCount),
                onClick = onShare
            )
        }
    }
}

@Composable
private fun BufferingChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.56f))
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(Color.White, 13.sp, FontWeight.Medium)
        )
    }
}

@Composable
private fun PlaybackStatusOverlay(
    message: String,
    showRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.68f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BasicText(
            text = message,
            style = TextStyle(Color.White, 13.sp, FontWeight.Medium)
        )

        if (showRetry) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable { onRetry() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                BasicText(
                    text = "Retry",
                    style = TextStyle(Color.White, 13.sp, FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun rememberNetworkAvailable(): State<Boolean> {
    val context = LocalContext.current
    return produceState(initialValue = context.isNetworkAvailable()) {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        if (connectivityManager == null) {
            value = true
            return@produceState
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                value = true
            }

            override fun onLost(network: Network) {
                value = context.isNetworkAvailable()
            }

            override fun onUnavailable() {
                value = false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } else {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
        }
        awaitDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}

private fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(ConnectivityManager::class.java) ?: return true
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Action button for reels (like, comment, share, etc.)
 */
@Composable
private fun ActionButton(
    icon: ImageVector? = null,
    label: String,
    isActive: Boolean = false,
    activeColor: Color = Color.White,
    saveEffectTrigger: Int = 0,
    customIcon: (@Composable (Color) -> Unit)? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            val iconTint = if (isActive) activeColor else Color.White
            if (customIcon != null) {
                customIcon(iconTint)
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(30.dp)
                )
            }
            SaveLottieEffect(
                trigger = saveEffectTrigger,
                tintColor = activeColor,
                modifier = Modifier.requiredSize(86.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        BasicText(
            label,
            style = TextStyle(Color.White, 11.sp, FontWeight.Medium)
        )
    }
}

@Composable
fun ReelCommentsSheet(
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    isDarkTheme: Boolean,
    contentColor: Color,
    accentColor: Color,
    comments: List<ReelComment>,
    repliesByParent: Map<String, List<ReelComment>>,
    expandedParents: Set<String>,
    loadingReplyParents: Set<String>,
    hasMoreRepliesByParent: Map<String, Boolean>,
    replyTarget: ReelComment?,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    isSubmitting: Boolean,
    error: String?,
    highlightedCommentId: String?,
    mentionSearchResults: List<MentionUser>,
    isSearchingMentions: Boolean,
    onDismiss: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleReplies: (String) -> Unit,
    onLoadMoreReplies: (String) -> Unit,
    onReplyTo: (ReelComment?) -> Unit,
    onMentionQueryChanged: (String) -> Unit,
    onMentionSearchClear: () -> Unit,
    onSubmitComment: (String) -> Unit
) {
    var draft by remember { mutableStateOf(TextFieldValue("")) }
    val activeMention = remember(draft) { findActiveReelMention(draft.text, draft.selection.start) }
    LaunchedEffect(activeMention?.query) {
        val query = activeMention?.query.orEmpty()
        if (query.length >= 2) {
            onMentionQueryChanged(query)
        } else {
            onMentionSearchClear()
        }
    }
    val sheetBackground = if (isDarkTheme) Color(0xFF101215) else Color(0xFFFFFFFF)
    val sheetTextColor = if (isDarkTheme) Color(0xFFF3F4F6) else Color(0xFF111111)
    val subTextColor = if (isDarkTheme) Color(0xFF9AA1AC) else Color(0xFF667085)
    val inputBg = if (isDarkTheme) Color(0xFF1B1F25) else Color(0xFFF2F4F7)
    val avatarBg = if (isDarkTheme) Color(0xFF232933) else Color(0xFFE4E7EC)
    val threadLineColor = if (isDarkTheme) Color(0xFF313844) else Color(0xFFD0D5DD)
    val overlayScrim = Color.Black.copy(alpha = 0.34f)
    var isExpanded by remember { mutableStateOf(false) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    val commentsListState = rememberLazyListState()

    LaunchedEffect(highlightedCommentId, comments, repliesByParent, expandedParents) {
        val targetId = highlightedCommentId ?: return@LaunchedEffect
        val index = comments.indexOfFirst { comment ->
            comment.id == targetId || repliesByParent[comment.id].orEmpty().any { it.id == targetId }
        }
        if (index >= 0) {
            commentsListState.animateScrollToItem(index)
        }
    }

    LaunchedEffect(commentsListState, comments.size, hasMore, isLoadingMore, isLoading) {
        snapshotFlow {
            val layout = commentsListState.layoutInfo
            val lastVisibleIndex = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex to layout.totalItemsCount
        }.collect { (lastVisibleIndex, totalItems) ->
            if (!isLoading && !isLoadingMore && hasMore && totalItems > 0 && lastVisibleIndex >= totalItems - 4) {
                onLoadMore()
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(overlayScrim)
            .clickable { onDismiss() }
    ) {
        val collapsedHeight = maxHeight * 0.64f
        val expandedHeight = maxHeight * 0.9f
        val sheetHeight by animateDpAsState(
            targetValue = if (isExpanded) expandedHeight else collapsedHeight,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "commentsSheetHeight"
        )
        val sheetDragModifier = Modifier.pointerInput(isExpanded) {
            detectVerticalDragGestures(
                onDragStart = { totalDragY = 0f },
                onVerticalDrag = { _, dragAmount ->
                    totalDragY += dragAmount
                },
                onDragCancel = { totalDragY = 0f },
                onDragEnd = {
                    when {
                        totalDragY <= -56f -> isExpanded = true
                        totalDragY >= 80f && isExpanded -> isExpanded = false
                        totalDragY >= 80f -> onDismiss()
                    }
                    totalDragY = 0f
                }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeight)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(sheetBackground)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(top = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(sheetDragModifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(subTextColor.copy(alpha = 0.35f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BasicText(
                        "Comments",
                        style = TextStyle(sheetTextColor, 16.sp, FontWeight.SemiBold)
                    )
                    BasicText(
                        "Close",
                        style = TextStyle(subTextColor, 13.sp),
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }
            }

            if (error != null) {
                BasicText(
                    error,
                    style = TextStyle(Color(0xFFFF8A80), 12.sp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (isLoading && comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    ReelCommentsLoadingAnimation()
                }
            } else {
                LazyColumn(
                    state = commentsListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (comments.isEmpty()) {
                        item {
                            BasicText(
                                "No comments yet. Start the conversation.",
                                style = TextStyle(subTextColor, 13.sp)
                            )
                        }
                    }

                    itemsIndexed(comments, key = { _, comment -> comment.id }) { index, comment ->
                        var commentVisible by remember(comment.id) { mutableStateOf(false) }

                        LaunchedEffect(comment.id) {
                            commentVisible = false
                            delay((index * 55L).coerceAtMost(440L))
                            commentVisible = true
                        }

                        AnimatedVisibility(
                            visible = commentVisible,
                            enter = fadeIn(animationSpec = tween(180)) +
                                slideInVertically(animationSpec = tween(220)) { it / 3 }
                        ) {
                            ReelCommentThreadItem(
                                comment = comment,
                                replies = repliesByParent[comment.id].orEmpty(),
                                repliesExpanded = expandedParents.contains(comment.id),
                                isLoadingReplies = loadingReplyParents.contains(comment.id),
                                hasMoreReplies = hasMoreRepliesByParent[comment.id] == true,
                                textColor = sheetTextColor,
                                subTextColor = subTextColor,
                                accentColor = accentColor,
                                avatarBg = avatarBg,
                                threadLineColor = threadLineColor,
                                onShowReplies = { onToggleReplies(comment.id) },
                                onLoadMoreReplies = { onLoadMoreReplies(comment.id) },
                                onReply = { onReplyTo(comment) },
                                highlightedCommentId = highlightedCommentId
                            )
                        }
                    }

                    if (hasMore) {
                        item {
                            BasicText(
                                if (isLoadingMore) "Loading more..." else "Load more comments",
                                style = TextStyle(accentColor, 13.sp, FontWeight.Medium),
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .clickable(enabled = !isLoadingMore) { onLoadMore() }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                        .background(if (isDarkTheme) Color(0xFF14181D) else Color(0xFFFFFFFF))
                    .padding(12.dp)
            ) {
                if (replyTarget != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicText(
                            "Replying to @${replyTarget.author.username ?: replyTarget.author.name}",
                            style = TextStyle(accentColor, 12.sp)
                        )
                        BasicText(
                            "Cancel",
                            style = TextStyle(subTextColor, 12.sp),
                            modifier = Modifier.clickable { onReplyTo(null) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                AnimatedVisibility(
                    visible = activeMention != null && (mentionSearchResults.isNotEmpty() || isSearchingMentions)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(inputBg)
                            .padding(vertical = 6.dp)
                    ) {
                        if (isSearchingMentions && mentionSearchResults.isEmpty()) {
                            BasicText(
                                "Searching...",
                                style = TextStyle(subTextColor, 12.sp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                        mentionSearchResults.take(5).forEach { user ->
                            val username = user.username?.takeIf { it.isNotBlank() }
                            if (username != null) {
                                ReelMentionSuggestionRow(
                                    user = user,
                                    textColor = sheetTextColor,
                                    subTextColor = subTextColor,
                                    avatarBg = avatarBg,
                                    onClick = {
                                        activeMention?.let { mention ->
                                            draft = draft.insertMention(mention, username)
                                            onMentionSearchClear()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        textStyle = TextStyle(sheetTextColor, 14.sp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(inputBg)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        decorationBox = { inner ->
                            if (draft.text.isBlank()) {
                                BasicText(
                                    if (replyTarget == null) "Write a comment..." else "Write a reply...",
                                    style = TextStyle(subTextColor.copy(alpha = 0.8f), 14.sp)
                                )
                            }
                            inner()
                        }
                    )

                    val canSend = draft.text.trim().isNotEmpty() && !isSubmitting
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (canSend) accentColor else accentColor.copy(alpha = 0.35f))
                            .clickable(enabled = canSend) {
                                val content = draft.text.trim()
                                if (content.isNotEmpty()) {
                                    onSubmitComment(content)
                                    draft = TextFieldValue("")
                                    onMentionSearchClear()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            if (isSubmitting) "..." else "Send",
                            style = TextStyle(Color.White, 13.sp, FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelCommentsLoadingAnimation(
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.card_vertical_scroll_animation)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = true
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(260.dp),
        alignment = Alignment.Center,
        contentScale = ContentScale.Fit
    )
}

private data class ReelActiveMention(
    val start: Int,
    val end: Int,
    val query: String
)

private fun findActiveReelMention(text: String, cursor: Int): ReelActiveMention? {
    val safeCursor = cursor.coerceIn(0, text.length)
    val beforeCursor = text.substring(0, safeCursor)
    val atIndex = beforeCursor.lastIndexOf('@')
    if (atIndex < 0) return null

    if (atIndex > 0) {
        val previous = text[atIndex - 1]
        if (previous.isLetterOrDigit() || previous == '_' || previous == '.') return null
    }

    val query = beforeCursor.substring(atIndex + 1)
    if (query.any { it.isWhitespace() || it in ",;:!?()[]{}" }) return null
    if (query.length > 30) return null

    return ReelActiveMention(atIndex, safeCursor, query)
}

private fun TextFieldValue.insertMention(activeMention: ReelActiveMention, username: String): TextFieldValue {
    val replacement = "@$username "
    val nextText = text.replaceRange(activeMention.start, activeMention.end, replacement)
    val nextCursor = activeMention.start + replacement.length
    return TextFieldValue(
        text = nextText,
        selection = TextRange(nextCursor)
    )
}

@Composable
private fun ReelMentionSuggestionRow(
    user: MentionUser,
    textColor: Color,
    subTextColor: Color,
    avatarBg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            val profileImage = user.profileImage ?: user.avatar
            if (!profileImage.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profileImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = user.name ?: user.username ?: "Mention avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                BasicText(
                    (user.name ?: user.username ?: "?").take(1).uppercase(),
                    style = TextStyle(textColor, 11.sp, FontWeight.Bold)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                user.name ?: user.username ?: "User",
                style = TextStyle(textColor, 13.sp, FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                "@${user.username.orEmpty()}",
                style = TextStyle(subTextColor, 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReelCommentThreadItem(
    comment: ReelComment,
    replies: List<ReelComment>,
    repliesExpanded: Boolean,
    isLoadingReplies: Boolean,
    hasMoreReplies: Boolean,
    textColor: Color,
    subTextColor: Color,
    accentColor: Color,
    avatarBg: Color,
    threadLineColor: Color,
    onShowReplies: () -> Unit,
    onLoadMoreReplies: () -> Unit,
    onReply: () -> Unit,
    highlightedCommentId: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val isHighlighted = comment.id == highlightedCommentId
        val commentHighlight by animateColorAsState(
            targetValue = if (isHighlighted) accentColor.copy(alpha = 0.28f) else Color.Transparent,
            label = "reelCommentHighlight"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(commentHighlight)
                .padding(if (isHighlighted) 8.dp else 0.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReelCommentAvatar(
                author = comment.author,
                avatarBg = avatarBg,
                textColor = textColor,
                size = 24.dp,
                fontSize = 9.sp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    BasicText(
                        comment.author.name ?: "@${comment.author.username ?: "user"}",
                        style = TextStyle(textColor, 11.sp, FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    VerificationBadge(
                        verified = comment.author.hasVerificationBadge(),
                        size = VerificationBadgeSize.Micro
                    )
                }

                BasicText(
                    comment.content,
                    style = TextStyle(textColor.copy(alpha = 0.94f), 11.sp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Reply",
                        style = TextStyle(accentColor, 10.sp, FontWeight.Medium),
                        modifier = Modifier.clickable { onReply() }
                    )
                    if (comment.repliesCount > 0) {
                        val label = when {
                            isLoadingReplies && !repliesExpanded -> "Loading replies..."
                            repliesExpanded -> "Hide replies"
                            else -> "View replies (${comment.repliesCount})"
                        }
                        BasicText(
                            label,
                            style = TextStyle(
                                if (repliesExpanded) accentColor else subTextColor,
                                10.sp,
                                FontWeight.Medium
                            ),
                            modifier = Modifier.clickable(enabled = !isLoadingReplies) { onShowReplies() }
                        )
                    }
                }
            }
        }

        if (repliesExpanded && replies.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(start = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                replies.forEach { reply ->
                    val isReplyHighlighted = reply.id == highlightedCommentId
                    val replyHighlight by animateColorAsState(
                        targetValue = if (isReplyHighlighted) accentColor.copy(alpha = 0.30f) else Color.Transparent,
                        label = "reelReplyHighlight"
                    )
                    val replyShape = RoundedCornerShape(10.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(replyShape)
                            .background(replyHighlight)
                            .border(
                                width = if (isReplyHighlighted) 1.dp else 0.dp,
                                color = if (isReplyHighlighted) accentColor.copy(alpha = 0.75f) else Color.Transparent,
                                shape = replyShape
                            )
                            .padding(if (isReplyHighlighted) 6.dp else 0.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .width(10.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(22.dp)
                                    .background(threadLineColor)
                            )
                        }

                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReelCommentAvatar(
                                author = reply.author,
                                avatarBg = avatarBg,
                                textColor = textColor,
                                size = 18.dp,
                                fontSize = 8.sp
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    BasicText(
                                        reply.author.name ?: "@${reply.author.username ?: "user"}",
                                        style = TextStyle(textColor, 9.sp, FontWeight.SemiBold),
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    VerificationBadge(
                                        verified = reply.author.hasVerificationBadge(),
                                        size = VerificationBadgeSize.Micro
                                    )
                                }
                                BasicText(
                                    reply.content,
                                    style = TextStyle(textColor.copy(alpha = 0.9f), 9.sp)
                                )
                            }
                        }
                    }
                }
                if (hasMoreReplies || isLoadingReplies) {
                    BasicText(
                        if (isLoadingReplies) "Loading replies..." else "Load more replies",
                        style = TextStyle(accentColor, 10.sp, FontWeight.Medium),
                        modifier = Modifier
                            .padding(start = 16.dp, top = 2.dp)
                            .clickable(enabled = !isLoadingReplies) { onLoadMoreReplies() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReelCommentAvatar(
    author: ReelAuthor,
    avatarBg: Color,
    textColor: Color,
    size: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val imageUrl = author.profileImage?.takeIf { it.isNotBlank() }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarBg),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = author.name ?: author.username ?: "User avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            BasicText(
                ((author.name?.firstOrNull() ?: author.username?.firstOrNull() ?: 'U').uppercase()),
                style = TextStyle(textColor, fontSize, FontWeight.Bold)
            )
        }
    }
}

// ==================== Utility Functions ====================

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${(count / 1_000_000.0).format(1)}M"
        count >= 1_000 -> "${(count / 1_000.0).format(1)}K"
        else -> count.toString()
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}:${secs.toString().padStart(2, '0')}" else "0:${secs.toString().padStart(2, '0')}"
}

private fun Double.format(decimals: Int): String {
    return "%.${decimals}f".format(this).trimEnd('0').trimEnd('.')
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
