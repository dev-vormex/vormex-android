package com.kyant.backdrop.catalog.linkedin

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.kyant.backdrop.catalog.ui.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * A composable video player that uses Media3 ExoPlayer
 * Supports inline playback and full-screen mode
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false,
    showControls: Boolean = true,
    contentColor: Color = Color.White,
    onFullScreenClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isPlaying by remember(videoUrl) { mutableStateOf(autoPlay) }
    var isLoading by remember(videoUrl) { mutableStateOf(true) }
    var hasError by remember(videoUrl) { mutableStateOf(false) }
    var currentPosition by remember(videoUrl) { mutableLongStateOf(0L) }
    var duration by remember(videoUrl) { mutableLongStateOf(0L) }
    var videoAspectRatio by remember(videoUrl) { mutableFloatStateOf(16f / 9f) }
    
    // One player per videoUrl; avoids stale media when URL changes and releases on leave.
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = autoPlay
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isLoading = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) {
                        val detectedDuration = this@apply.duration
                        if (detectedDuration > 0L) duration = detectedDuration
                        // Get actual video aspect ratio
                        val format = this@apply.videoFormat
                        if (format != null && format.height > 0) {
                            videoAspectRatio = format.width.toFloat() / format.height.toFloat()
                        }
                    } else if (playbackState == Player.STATE_ENDED) {
                        currentPosition = duration.coerceAtLeast(currentPosition)
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

    // Lifecycle only — release happens after PlayerView detaches (see AndroidView.onRelease).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) exoPlayer.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            val detectedDuration = exoPlayer.duration
            if (detectedDuration > 0L) duration = detectedDuration
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(if (exoPlayer.isPlaying) 250L else 500L)
        }
    }

    fun togglePlayback() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0L)
            currentPosition = 0L
        }
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(videoAspectRatio.coerceIn(0.5f, 2.5f))
                .background(Color.Black)
        ) {
            // Player must be set in [update] so LazyColumn reuse / URL changes attach the right instance.
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                    view.useController = false
                },
                onRelease = { view ->
                    view.player = null
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isLoading && !hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (showControls) {
                                togglePlayback()
                            } else if (onFullScreenClick != null) {
                                onFullScreenClick()
                            } else {
                                togglePlayback()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.58f)),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayIcon(color = Color.White, size = 30.dp)
                        }
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingSpinner(color = contentColor)
                }
            }

            // Error state
            if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicText(
                            "Failed to load video",
                            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(contentColor.copy(alpha = 0.2f))
                                .clickable {
                                    hasError = false
                                    isLoading = true
                                    exoPlayer.prepare()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            BasicText(
                                "Retry",
                                style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }

        if (showControls) {
            InlineVideoControls(
                isPlaying = isPlaying,
                positionMs = currentPosition,
                durationMs = duration,
                onTogglePlay = ::togglePlayback,
                onSeekTo = { positionMs ->
                    currentPosition = positionMs
                    exoPlayer.seekTo(positionMs)
                },
                onFullScreenClick = onFullScreenClick
            )
        }
    }
}

@Composable
private fun InlineVideoControls(
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onFullScreenClick: (() -> Unit)?
) {
    val controlsColor = Color.White
    val inactiveColor = Color.White.copy(alpha = 0.28f)
    val safeDuration = durationMs.coerceAtLeast(0L)
    val maxValue = safeDuration.coerceAtLeast(1L).toFloat()
    val safePosition = positionMs.coerceIn(0L, safeDuration.coerceAtLeast(1L)).toFloat()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VideoControlButton(onClick = onTogglePlay) {
            if (isPlaying) {
                PauseIcon(color = controlsColor, size = 18.dp)
            } else {
                PlayIcon(color = controlsColor, size = 18.dp)
            }
        }

        BasicText(
            text = formatVideoTime(positionMs),
            style = TextStyle(
                color = controlsColor.copy(alpha = 0.82f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Slider(
            value = safePosition,
            onValueChange = { value -> onSeekTo(value.toLong()) },
            valueRange = 0f..maxValue,
            enabled = durationMs > 0L,
            colors = SliderDefaults.colors(
                thumbColor = controlsColor,
                activeTrackColor = controlsColor,
                inactiveTrackColor = inactiveColor,
                disabledThumbColor = controlsColor.copy(alpha = 0.45f),
                disabledActiveTrackColor = inactiveColor,
                disabledInactiveTrackColor = inactiveColor.copy(alpha = 0.55f)
            ),
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
        )

        BasicText(
            text = if (durationMs > 0L) formatVideoTime(durationMs) else "--:--",
            style = TextStyle(
                color = controlsColor.copy(alpha = 0.66f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )

        if (onFullScreenClick != null) {
            VideoControlButton(onClick = onFullScreenClick) {
                FullscreenIcon(color = controlsColor, size = 18.dp)
            }
        }
    }
}

@Composable
private fun VideoControlButton(
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Simple loading spinner composable
 */
@Composable
private fun LoadingSpinner(
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    // Simple loading text for now - can be replaced with actual spinner animation
    BasicText(
        "Loading...",
        style = TextStyle(color.copy(alpha = 0.8f), 12.sp),
        modifier = modifier
    )
}

/**
 * Play icon using Canvas
 */
@Composable
private fun PlayIcon(
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(size)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            val triangleSize = size.toPx() * 0.6f
            val startX = (size.toPx() - triangleSize * 0.866f) / 2 + triangleSize * 0.1f
            val centerY = size.toPx() / 2
            
            moveTo(startX, centerY - triangleSize / 2)
            lineTo(startX + triangleSize * 0.866f, centerY)
            lineTo(startX, centerY + triangleSize / 2)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun PauseIcon(
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(size)) {
        val barWidth = size.toPx() * 0.22f
        val barHeight = size.toPx() * 0.62f
        val gap = size.toPx() * 0.16f
        val startX = (size.toPx() - (barWidth * 2f + gap)) / 2f
        val topY = (size.toPx() - barHeight) / 2f
        val corner = barWidth * 0.32f

        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(startX, topY),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
        )
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(startX + barWidth + gap, topY),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
        )
    }
}

@Composable
private fun FullscreenIcon(
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(size)) {
        val inset = size.toPx() * 0.18f
        val corner = size.toPx() * 0.28f
        val stroke = size.toPx() * 0.1f
        val max = size.toPx() - inset

        fun line(startX: Float, startY: Float, endX: Float, endY: Float) {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(startX, startY),
                end = androidx.compose.ui.geometry.Offset(endX, endY),
                strokeWidth = stroke
            )
        }

        line(inset, inset, inset + corner, inset)
        line(inset, inset, inset, inset + corner)
        line(max, inset, max - corner, inset)
        line(max, inset, max, inset + corner)
        line(inset, max, inset + corner, max)
        line(inset, max, inset, max - corner)
        line(max, max, max - corner, max)
        line(max, max, max, max - corner)
    }
}

private fun formatVideoTime(timeMs: Long): String {
    val totalSeconds = (timeMs.coerceAtLeast(0L) / 1000L).coerceAtMost(99L * 60L * 60L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0L) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

/**
 * Full screen video player dialog
 */
@OptIn(UnstableApi::class)
@Composable
fun FullScreenVideoPlayer(
    videoUrl: String,
    onDismiss: () -> Unit,
    backgroundStatusText: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isLoading by remember(videoUrl) { mutableStateOf(true) }
    var hasError by remember(videoUrl) { mutableStateOf(false) }
    
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isLoading = playbackState == Player.STATE_BUFFERING
                }
                
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    hasError = true
                    isLoading = false
                }
            })
        }
    }
    
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full screen video player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    controllerShowTimeoutMs = 3000
                    controllerHideOnTouch = true
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.useController = true
                view.showController()
            },
            onRelease = { view ->
                view.player = null
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Close button
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "✕",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "Loading...",
                    style = TextStyle(Color.White.copy(alpha = 0.8f), 14.sp)
                )
            }
        }
        
        // Error state
        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText(
                        "Failed to load video",
                        style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable {
                                hasError = false
                                isLoading = true
                                exoPlayer.prepare()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            "Retry",
                            style = TextStyle(Color.White, 13.sp, FontWeight.Medium)
                        )
                    }
                }
            }
        }

        backgroundStatusText?.let { status ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!status.equals("Saved locally", ignoreCase = true)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    }
                    BasicText(
                        text = status,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
