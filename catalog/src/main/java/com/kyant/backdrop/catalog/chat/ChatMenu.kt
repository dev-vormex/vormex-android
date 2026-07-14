package com.kyant.backdrop.catalog.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.linkedin.currentVormexAppearance
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Glass-styled chat options menu with blur effects matching app theme.
 */
@Composable
internal fun GlassChatMenu(
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
    val coroutineScope = rememberCoroutineScope()
    val hiddenMenuOffset = with(LocalDensity.current) { -560.dp.toPx() }
    val menuShape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)
    val menuContentColor = if (isGlassTheme) Color.White else appearance.contentColor
    val menuBorderColor =
        if (isGlassTheme) Color.White.copy(alpha = 0.14f) else appearance.overlayBorderColor
    val dividerColor = menuContentColor.copy(alpha = if (isGlassTheme) 0.14f else 0.10f)
    val dangerColor = if (isGlassTheme) Color(0xFFFF8A8A) else Color(0xFFD83A3A)
    val warningColor = if (isGlassTheme) Color(0xFFFFC46B) else Color(0xFFC77700)
    val sheetOffsetY = remember(hiddenMenuOffset) { Animatable(hiddenMenuOffset) }
    var isClosing by remember { mutableStateOf(false) }

    fun closeWithMotion(afterClose: () -> Unit = onDismiss) {
        if (isClosing) return
        isClosing = true
        coroutineScope.launch {
            sheetOffsetY.animateTo(
                targetValue = hiddenMenuOffset,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
            )
            afterClose()
        }
    }

    LaunchedEffect(Unit) {
        sheetOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
        )
    }
    val menuSurface: Modifier = if (isGlassTheme) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(0f.dp) },
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
                interactionSource = remember { MutableInteractionSource() }
            ) { closeWithMotion() }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, sheetOffsetY.value.roundToInt()) }
                .fillMaxWidth()
                .then(menuSurface)
                .clip(menuShape)
                .background(Color.Black.copy(alpha = if (isGlassTheme) 0.24f else 0f))
                .border(1.dp, menuBorderColor, menuShape)
                .pointerInput(hiddenMenuOffset) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (sheetOffsetY.value < -120.dp.toPx()) {
                                closeWithMotion()
                            } else {
                                coroutineScope.launch {
                                    sheetOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                sheetOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                                )
                            }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            if (!isClosing) {
                                coroutineScope.launch {
                                    sheetOffsetY.snapTo(
                                        (sheetOffsetY.value + dragAmount).coerceIn(hiddenMenuOffset, 0f)
                                    )
                                }
                            }
                        }
                    )
                }
                .clickable(enabled = false) { }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 22.dp)
                    .padding(top = 14.dp, bottom = 12.dp)
            ) {
                GlassMenuItem(
                    iconRes = R.drawable.ic_chat_menu_profile,
                    text = "View profile",
                    contentColor = menuContentColor,
                    onClick = { closeWithMotion(onViewProfile) }
                )
                GlassMenuItem(
                    iconRes = R.drawable.ic_chat_menu_search,
                    text = "Search messages",
                    contentColor = menuContentColor,
                    onClick = { closeWithMotion(onSearchMessages) }
                )
                GlassMenuItem(
                    iconRes = R.drawable.ic_chat_menu_wallpaper,
                    text = "Chat wallpaper",
                    subtitle = wallpaperStatusLine,
                    contentColor = menuContentColor,
                    onClick = { closeWithMotion(onChangeWallpaper) }
                )
                GlassMenuItem(
                    iconRes = if (isMuted) R.drawable.ic_chat_menu_bell_off else R.drawable.ic_chat_menu_bell,
                    text = if (isMuted) "Unmute notifications" else "Mute notifications",
                    subtitle = muteStatusLine,
                    contentColor = menuContentColor,
                    onClick = { closeWithMotion(onMuteNotifications) }
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(1.dp)
                        .background(dividerColor)
                )

                GlassMenuItem(
                    iconRes = R.drawable.ic_chat_menu_delete,
                    text = "Delete chat",
                    contentColor = dangerColor,
                    onClick = { closeWithMotion(onClearChat) }
                )
                GlassMenuItem(
                    iconRes = R.drawable.ic_chat_menu_report,
                    text = "Report",
                    subtitle = "Safety concerns",
                    contentColor = warningColor,
                    onClick = { closeWithMotion(onReport) }
                )
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 6.dp)
                        .width(46.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(menuContentColor.copy(alpha = if (isGlassTheme) 0.24f else 0.18f))
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
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = if (subtitle != null) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(contentColor)
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
