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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.linkedin.posts.formatTimeAgo
import com.kyant.backdrop.catalog.network.models.Reel
import com.kyant.backdrop.catalog.network.models.ReelAuthor
import com.kyant.backdrop.catalog.network.models.ReelComment
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                    "Reels",
                    style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .width(120.dp)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        // Thumbnail
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(reel.thumbnailUrl ?: reel.videoUrl)
                .crossfade(true)
                .build(),
            contentDescription = reel.title ?: "Reel",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
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
    onLoadMore: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hostActivity = remember(context) { context.findActivity() }

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

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { reels.size }
    )
    
    // Track current visible reel for autoplay
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    
    LaunchedEffect(currentPage) {
        onReelChanged(currentPage)

        // Load more when near the end
        if (currentPage >= reels.size - 8) {
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

        // Close button
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() }
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                "✕",
                style = TextStyle(Color.White, 18.sp, FontWeight.Bold)
            )
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
    val coroutineScope = rememberCoroutineScope()
    val mediaPulseScale = remember(reel.id) { Animatable(1f) }
    val likeBurstScale = remember(reel.id) { Animatable(0.82f) }
    
    // Watch time tracking
    var watchStartTime by remember(reel.id) { mutableLongStateOf(0L) }
    var viewTracked by remember(reel.id) { mutableStateOf(false) }
    val posterUrl = remember(reel.id, reel.thumbnailUrl, reel.previewGifUrl) {
        reel.thumbnailUrl ?: reel.previewGifUrl
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

            if (!hasRenderedFirstFrame && posterUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = reel.title ?: "Reel poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
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
                .padding(start = 16.dp, bottom = 100.dp, end = 80.dp)
                .navigationBarsPadding()
        ) {
            // Author info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onProfileClick() }
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
                    BasicText(
                        reel.author.name ?: "User",
                        style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                    )
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
                icon = if (isLiked) "❤" else "♡",
                label = formatCount(likesCount),
                isActive = isLiked,
                activeColor = Color.Red,
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
                icon = "💬",
                label = formatCount(reel.commentsCount),
                onClick = onComment
            )
            
            // Save button
            ActionButton(
                icon = if (isSaved) "🔖" else "📑",
                label = formatCount(reel.savesCount),
                isActive = isSaved,
                activeColor = Color.Yellow,
                onClick = {
                    isSaved = !isSaved
                    onSave()
                }
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
    icon: String,
    label: String,
    isActive: Boolean = false,
    activeColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        BasicText(
            icon,
            style = TextStyle(
                color = if (isActive) activeColor else Color.White,
                fontSize = 28.sp
            )
        )
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
    replyTarget: ReelComment?,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    isSubmitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleReplies: (String) -> Unit,
    onReplyTo: (ReelComment?) -> Unit,
    onSubmitComment: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    val sheetBackground = if (isDarkTheme) Color(0xFF101215) else Color(0xFFFFFFFF)
    val sheetTextColor = if (isDarkTheme) Color(0xFFF3F4F6) else Color(0xFF111111)
    val subTextColor = if (isDarkTheme) Color(0xFF9AA1AC) else Color(0xFF667085)
    val inputBg = if (isDarkTheme) Color(0xFF1B1F25) else Color(0xFFF2F4F7)
    val avatarBg = if (isDarkTheme) Color(0xFF232933) else Color(0xFFE4E7EC)
    val threadLineColor = if (isDarkTheme) Color(0xFF313844) else Color(0xFFD0D5DD)
    val overlayScrim = Color.Black.copy(alpha = 0.34f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(overlayScrim)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.64f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(sheetBackground)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(top = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
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
                    BasicText("Loading comments...", style = TextStyle(subTextColor, 13.sp))
                }
            } else {
                LazyColumn(
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

                    items(comments, key = { it.id }) { comment ->
                        ReelCommentThreadItem(
                            comment = comment,
                            replies = repliesByParent[comment.id].orEmpty(),
                            repliesExpanded = expandedParents.contains(comment.id),
                            textColor = sheetTextColor,
                            subTextColor = subTextColor,
                            accentColor = accentColor,
                            avatarBg = avatarBg,
                            threadLineColor = threadLineColor,
                            onShowReplies = { onToggleReplies(comment.id) },
                            onReply = { onReplyTo(comment) }
                        )
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
                            if (draft.isBlank()) {
                                BasicText(
                                    if (replyTarget == null) "Write a comment..." else "Write a reply...",
                                    style = TextStyle(subTextColor.copy(alpha = 0.8f), 14.sp)
                                )
                            }
                            inner()
                        }
                    )

                    val canSend = draft.trim().isNotEmpty() && !isSubmitting
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (canSend) accentColor else accentColor.copy(alpha = 0.35f))
                            .clickable(enabled = canSend) {
                                val content = draft.trim()
                                if (content.isNotEmpty()) {
                                    onSubmitComment(content)
                                    draft = ""
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
private fun ReelCommentThreadItem(
    comment: ReelComment,
    replies: List<ReelComment>,
    repliesExpanded: Boolean,
    textColor: Color,
    subTextColor: Color,
    accentColor: Color,
    avatarBg: Color,
    threadLineColor: Color,
    onShowReplies: () -> Unit,
    onReply: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                BasicText(
                    comment.author.name ?: "@${comment.author.username ?: "user"}",
                    style = TextStyle(textColor, 11.sp, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

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
                        val label = if (repliesExpanded) "Hide replies" else "View replies (${comment.repliesCount})"
                        BasicText(
                            label,
                            style = TextStyle(
                                if (repliesExpanded) accentColor else subTextColor,
                                10.sp,
                                FontWeight.Medium
                            ),
                            modifier = Modifier.clickable { onShowReplies() }
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
                    Row(
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
                                BasicText(
                                    reply.author.name ?: "@${reply.author.username ?: "user"}",
                                    style = TextStyle(textColor, 9.sp, FontWeight.SemiBold)
                                )
                                BasicText(
                                    reply.content,
                                    style = TextStyle(textColor.copy(alpha = 0.9f), 9.sp)
                                )
                            }
                        }
                    }
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
