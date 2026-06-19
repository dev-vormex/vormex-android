package com.kyant.backdrop.catalog.ads

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.catalog.MainActivity
import com.kyant.backdrop.catalog.deeplink.VormexDeepLinks
import com.kyant.backdrop.catalog.network.models.ManagedAdPlacement
import com.kyant.backdrop.catalog.ui.BasicText

@Composable
fun ManagedFeedAdCard(
    ad: ManagedAdPlacement,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    onImpression: (ManagedAdPlacement) -> Unit,
    onClick: (ManagedAdPlacement) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backgroundColor = if (isLightTheme) Color.White.copy(alpha = 0.95f) else Color(0xFF171717).copy(alpha = 0.96f)
    val borderColor = if (isLightTheme) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
    val title = ad.feedTitle?.takeIf { it.isNotBlank() } ?: ad.sponsorName
    val body = ad.feedBody?.takeIf { it.isNotBlank() }
    val ctaText = ad.ctaText?.takeIf { it.isNotBlank() } ?: "Learn more"

    LaunchedEffect(ad.campaignId, ad.slotKey) {
        onImpression(ad)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    ad.sponsorName,
                    style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    "Sponsored",
                    style = TextStyle(contentColor.copy(alpha = 0.58f), 11.sp, FontWeight.Medium),
                    maxLines = 1
                )
            }
        }

        ad.feedImageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(contentColor.copy(alpha = 0.06f))
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicText(
                title,
                style = TextStyle(contentColor, 18.sp, FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!body.isNullOrBlank()) {
                BasicText(
                    body,
                    style = TextStyle(contentColor.copy(alpha = 0.72f), 13.sp, FontWeight.Normal),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(accentColor)
                .clickable {
                    onClick(ad)
                    openManagedAdCta(context, ad)
                }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                ctaText,
                style = TextStyle(Color.White, 14.sp, FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ManagedReelsAdPage(
    ad: ManagedAdPlacement,
    modifier: Modifier = Modifier,
    onImpression: (ManagedAdPlacement) -> Unit,
    onClick: (ManagedAdPlacement) -> Unit
) {
    val context = LocalContext.current
    val videoUrl = ad.reelsHlsUrl?.takeIf { it.isNotBlank() }
        ?: ad.reelsVideoUrl?.takeIf { it.isNotBlank() }
    val player = remember(videoUrl) {
        videoUrl?.let { url ->
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
                prepare()
            }
        }
    }
    val ctaText = ad.ctaText?.takeIf { it.isNotBlank() } ?: "Learn more"
    val caption = ad.reelCaption?.takeIf { it.isNotBlank() } ?: ad.sponsorName

    LaunchedEffect(ad.campaignId, ad.slotKey) {
        onImpression(ad)
    }

    DisposableEffect(player) {
        onDispose {
            player?.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view -> view.player = player },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(ad.reelsThumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.82f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BasicText(
                ad.sponsorName,
                style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                caption,
                style = TextStyle(Color.White, 24.sp, FontWeight.Bold),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .clickable {
                        onClick(ad)
                        openManagedAdCta(context, ad)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicText(
                    ctaText,
                    style = TextStyle(Color.Black, 14.sp, FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        BasicText(
            "Ad",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            style = TextStyle(Color.White, 12.sp, FontWeight.SemiBold)
        )
    }
}

fun openManagedAdCta(context: Context, ad: ManagedAdPlacement): Boolean {
    val rawUrl = ad.ctaUrl?.trim()?.takeIf { it.isNotBlank() } ?: return false
    val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return false
    val isInternal = ad.ctaKind == "vormex_deeplink"

    val intent = if (isInternal && VormexDeepLinks.parse(uri) != null) {
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    } else {
        Intent(Intent.ACTION_VIEW, uri).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    return runCatching {
        context.startActivity(intent)
    }.isSuccess
}
