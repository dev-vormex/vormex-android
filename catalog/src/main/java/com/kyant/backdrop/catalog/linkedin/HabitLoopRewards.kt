package com.kyant.backdrop.catalog.linkedin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * HABIT LOOP REWARD SYSTEM
 * 
 * Implements the Cue → Action → Reward pattern:
 * - Cue: Push notification "New business-minded student matched"
 * - Action: User views profile, sends connection request
 * - Reward: Instant celebration animation + reply rate display
 * 
 * The dopamine hit from the reward creates a craving to repeat the action.
 * After 7-10 loops, it becomes a habit.
 */

// ===========================================
// CONFETTI PARTICLE SYSTEM
// ===========================================

data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val color: Color,
    val size: Float,
    val shape: ConfettiShape
)

enum class ConfettiShape {
    CIRCLE, SQUARE, STAR, HEART
}

@Composable
fun ConfettiOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 100,
    colors: List<Color> = listOf(
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFFFFE66D),
        Color(0xFF95E1D3),
        Color(0xFFA66CFF),
        Color(0xFFFF9F43)
    ),
    onAnimationEnd: () -> Unit = {}
) {
    var particles by remember { mutableStateOf(emptyList<ConfettiParticle>()) }
    var animationProgress by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // Generate particles
            particles = (0 until particleCount).map {
                ConfettiParticle(
                    x = Random.nextFloat(),
                    y = -Random.nextFloat() * 0.3f,
                    velocityX = (Random.nextFloat() - 0.5f) * 0.3f,
                    velocityY = Random.nextFloat() * 0.02f + 0.01f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = (Random.nextFloat() - 0.5f) * 10f,
                    color = colors.random(),
                    size = Random.nextFloat() * 12f + 6f,
                    shape = ConfettiShape.entries.random()
                )
            }
            
            // Animate
            val startTime = System.currentTimeMillis()
            val duration = 3000L
            while (System.currentTimeMillis() - startTime < duration) {
                animationProgress = (System.currentTimeMillis() - startTime).toFloat() / duration
                delay(16)
            }
            onAnimationEnd()
        } else {
            particles = emptyList()
            animationProgress = 0f
        }
    }
    
    if (isVisible && particles.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val gravity = 0.0015f
            
            particles.forEach { particle ->
                val time = animationProgress * 60 // frames
                val x = (particle.x + particle.velocityX * time) * size.width
                val y = (particle.y + particle.velocityY * time + gravity * time * time) * size.height
                val rotation = particle.rotation + particle.rotationSpeed * time
                val alpha = (1f - animationProgress).coerceIn(0f, 1f)
                
                if (y < size.height && y > -50) {
                    rotate(rotation, Offset(x, y)) {
                        when (particle.shape) {
                            ConfettiShape.CIRCLE -> {
                                drawCircle(
                                    color = particle.color.copy(alpha = alpha),
                                    radius = particle.size,
                                    center = Offset(x, y)
                                )
                            }
                            ConfettiShape.SQUARE -> {
                                drawRect(
                                    color = particle.color.copy(alpha = alpha),
                                    topLeft = Offset(x - particle.size / 2, y - particle.size / 2),
                                    size = androidx.compose.ui.geometry.Size(particle.size, particle.size)
                                )
                            }
                            ConfettiShape.STAR -> {
                                val path = Path().apply {
                                    val cx = x
                                    val cy = y
                                    val outer = particle.size
                                    val inner = particle.size * 0.4f
                                    moveTo(cx, cy - outer)
                                    for (i in 1..4) {
                                        val angle1 = (i * 72 - 90) * PI / 180
                                        val angle2 = (i * 72 - 54) * PI / 180
                                        lineTo(
                                            (cx + inner * cos(angle2)).toFloat(),
                                            (cy + inner * sin(angle2)).toFloat()
                                        )
                                        lineTo(
                                            (cx + outer * cos(angle1)).toFloat(),
                                            (cy + outer * sin(angle1)).toFloat()
                                        )
                                    }
                                    close()
                                }
                                drawPath(path, particle.color.copy(alpha = alpha))
                            }
                            ConfettiShape.HEART -> {
                                val path = Path().apply {
                                    val s = particle.size * 0.7f
                                    moveTo(x, y + s * 0.25f)
                                    cubicTo(
                                        x - s, y - s * 0.5f,
                                        x - s, y + s * 0.5f,
                                        x, y + s
                                    )
                                    cubicTo(
                                        x + s, y + s * 0.5f,
                                        x + s, y - s * 0.5f,
                                        x, y + s * 0.25f
                                    )
                                }
                                drawPath(path, particle.color.copy(alpha = alpha))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===========================================
// CONNECTION SENT CELEBRATION
// ===========================================

@Composable
fun ConnectionSentCelebration(
    isVisible: Boolean,
    recipientName: String,
    recipientImage: String?,
    replyRate: Int,
    connectionStreak: Int = 0,
    isNewStreakMilestone: Boolean = false,
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f, animationSpec = tween(200))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            // Main celebration card
            Box(
                Modifier
                    .padding(32.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(28f.dp) },
                        effects = {
                            vibrancy()
                            blur(24f.dp.toPx())
                            lens(12f.dp.toPx(), 24f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.95f))
                        }
                    )
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Success checkmark with pulse animation
                    PulsingCheckmark()
                    
                    // Title
                    BasicText(
                        "Connection Sent!",
                        style = TextStyle(
                            color = Color(0xFF1a1a2e),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                    
                    // Recipient info
                    BasicText(
                        "Request sent to $recipientName",
                        style = TextStyle(
                            color = Color(0xFF1a1a2e).copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    // Streak progress (Duolingo Effect)
                    if (connectionStreak > 0) {
                        StreakProgressIndicator(
                            currentStreak = connectionStreak,
                            isNewMilestone = isNewStreakMilestone
                        )
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    
                    // Reply rate indicator (builds anticipation)
                    ReplyRateIndicator(replyRate = replyRate)
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Tip text
                    BasicText(
                        "💡 Tip: Send a personalized message to stand out!",
                        style = TextStyle(
                            color = Color(0xFF6C5CE7),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
            
            // Confetti overlay
            ConfettiOverlay(
                isVisible = isVisible,
                particleCount = 80
            )
        }
    }
}

@Composable
private fun PulsingCheckmark() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF00C851), Color(0xFF4ECDC4))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            "✓",
            style = TextStyle(
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun ReplyRateIndicator(
    replyRate: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        replyRate >= 80 -> Color(0xFF00C851)
        replyRate >= 50 -> Color(0xFFFFBB33)
        else -> Color(0xFF8E8E93)
    }
    
    val label = when {
        replyRate >= 80 -> "High responder! 🔥"
        replyRate >= 50 -> "Usually responds"
        else -> "Building connections"
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Progress bar
        Box(
            Modifier
                .fillMaxWidth(0.8f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Gray.copy(alpha = 0.2f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(replyRate / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )
        }
        
        // Label
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                "$replyRate%",
                style = TextStyle(
                    color = color,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            BasicText(
                label,
                style = TextStyle(
                    color = Color(0xFF1a1a2e).copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            )
        }
    }
}

// ===========================================
// DAILY MATCH NOTIFICATION CARD
// ===========================================

@Composable
fun DailyMatchCard(
    matchCount: Int,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onViewMatches: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(20f.dp) },
                    effects = {
                        vibrancy()
                        blur(16f.dp.toPx())
                        lens(8f.dp.toPx(), 16f.dp.toPx())
                    },
                    onDrawSurface = {
                        // Gradient background with shimmer
                        drawRect(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6C5CE7).copy(alpha = 0.9f),
                                    Color(0xFFA66CFF).copy(alpha = 0.9f),
                                    Color(0xFF6C5CE7).copy(alpha = 0.9f)
                                ),
                                start = Offset(size.width * shimmerOffset, 0f),
                                end = Offset(size.width * (shimmerOffset + 1), size.height)
                            )
                        )
                    }
                )
                .clickable(onClick = onViewMatches)
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        "✨ New Matches Today!",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    BasicText(
                        "$matchCount business-minded student${if (matchCount > 1) "s" else ""} matched",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    )
                }
                
                // Animated badge
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "$matchCount",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

// ===========================================
// STREAK REMINDER CARD (Fear of Loss Driver)
// ===========================================

/**
 * StreakReminderCard - Urgent reminder shown on Feed when streak is at risk
 * 
 * Psychology: Loss aversion is 2x stronger than gain motivation.
 * Creates urgency to take action and preserve the streak.
 */
@Composable
fun StreakReminderCard(
    connectionStreak: Int,
    isAtRisk: Boolean,
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    var hasDismissed by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val animatedDragOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "streakReminderDragY"
    )
    val titleText = if (isAtRisk) {
        "${connectionStreak}-day streak at risk"
    } else {
        "${connectionStreak}-day streak active"
    }
    val supportingText = if (isAtRisk) {
        "Connect with someone today to keep it alive."
    } else {
        "Nice momentum. Keep building it today."
    }

    fun dismissWithSlideUp() {
        if (hasDismissed) return
        hasDismissed = true
        dragOffsetY = 0f
        isVisible = false
        scope.launch {
            delay(260)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        delay(5000)
        dismissWithSlideUp()
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(120)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(240, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(180))
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .graphicsLayer { translationY = animatedDragOffsetY }
                .pointerInput(Unit) {
                    var totalDragY = 0f
                    detectVerticalDragGestures(
                        onDragStart = { totalDragY = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            totalDragY += dragAmount
                            dragOffsetY = totalDragY.coerceAtMost(0f)
                        },
                        onDragCancel = { dragOffsetY = 0f },
                        onDragEnd = {
                            if (totalDragY <= -48f) {
                                dismissWithSlideUp()
                            } else {
                                dragOffsetY = 0f
                            }
                        }
                    )
                }
                .clip(RoundedCornerShape(22.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(22.dp) },
                    effects = {
                        vibrancy()
                        blur(16f.dp.toPx())
                        lens(8f.dp.toPx(), 16f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color(0xFF111318).copy(alpha = 0.88f))
                    }
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF171A20).copy(alpha = 0.96f),
                            Color(0xFF251811).copy(alpha = 0.95f),
                            Color(0xFF171A20).copy(alpha = 0.96f)
                        )
                    )
                )
                .padding(start = 12.dp, top = 12.dp, end = 10.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StreakFireLottie(modifier = Modifier.size(46.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    BasicText(
                        titleText,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                    BasicText(
                        supportingText,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp
                        ),
                        maxLines = 2
                    )
                }
                
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.94f))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        "Find",
                        style = TextStyle(
                            color = Color(0xFF1C1F25),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            dismissWithSlideUp()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "x",
                        style = TextStyle(Color.White.copy(alpha = 0.86f), 12.sp, FontWeight.Bold)
                    )
                }
            }
        }
    }
}

// ===========================================
// LOGIN STREAK BADGE (Daily Engagement)
// ===========================================

/**
 * DismissableLoginStreakBadge - Wrapper with dismiss callback
 * 
 * Only shows when ViewModel determines it's time (milestones, 24hr cooldown).
 * When auto-dismissed or user dismisses, calls onDismiss to persist the dismissal.
 */
@Composable
fun DismissableLoginStreakBadge(
    loginStreak: Int,
    backdrop: LayerBackdrop,
    contentColor: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (loginStreak == 0) return
    
    var showBadge by remember { mutableStateOf(true) }
    val scale = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // Pop in animation
        scale.animateTo(1.1f, tween(200, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(100))
        
        // Auto-dismiss after 4 seconds and persist dismissal
        delay(4000)
        showBadge = false
        onDismiss() // Persist the dismissal for 24hr cooldown
    }
    
    AnimatedVisibility(
        visible = showBadge,
        enter = fadeIn() + scaleIn(initialScale = 0.5f),
        exit = fadeOut() + scaleOut(targetScale = 0.5f)
    ) {
        Row(
            modifier = modifier
                .scale(scale.value)
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(12f.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50).copy(alpha = 0.15f),
                                    Color(0xFF8BC34A).copy(alpha = 0.1f)
                                )
                            )
                        )
                    }
                )
                .clickable { 
                    showBadge = false
                    onDismiss() // User dismissed manually
                }
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Check icon
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50)),
                contentAlignment = Alignment.Center
            ) {
                BasicText("✓", style = TextStyle(Color.White, 16.sp, FontWeight.Bold))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    "Welcome back! 🎉",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    "$loginStreak-day login streak",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                )
            }
            
            // Fire badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText("🔥", style = TextStyle(fontSize = 18.sp))
                BasicText(
                    "$loginStreak",
                    style = TextStyle(
                        color = Color(0xFFFF5722),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

/**
 * LoginStreakBadge - Small celebratory badge shown when user opens app
 * 
 * Psychology: Immediate reward reinforces the habit of opening the app daily.
 * Shows appreciation and creates a sense of progress.
 */
@Composable
fun LoginStreakBadge(
    loginStreak: Int,
    backdrop: LayerBackdrop,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    if (loginStreak == 0) return
    
    var showBadge by remember { mutableStateOf(true) }
    val scale = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // Pop in animation
        scale.animateTo(1.1f, tween(200, easing = FastOutSlowInEasing))
        scale.animateTo(1f, tween(100))
        
        // Auto-dismiss after 4 seconds
        delay(4000)
        showBadge = false
    }
    
    AnimatedVisibility(
        visible = showBadge,
        enter = fadeIn() + scaleIn(initialScale = 0.5f),
        exit = fadeOut() + scaleOut(targetScale = 0.5f)
    ) {
        Row(
            modifier = modifier
                .scale(scale.value)
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(12f.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50).copy(alpha = 0.15f),
                                    Color(0xFF8BC34A).copy(alpha = 0.1f)
                                )
                            )
                        )
                    }
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Check icon
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50)),
                contentAlignment = Alignment.Center
            ) {
                BasicText("✓", style = TextStyle(Color.White, 16.sp, FontWeight.Bold))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    "Welcome back! 🎉",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    "$loginStreak-day login streak",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                )
            }
            
            // Fire badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText("🔥", style = TextStyle(fontSize = 18.sp))
                BasicText(
                    "$loginStreak",
                    style = TextStyle(
                        color = Color(0xFFFF5722),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// ===========================================
// STREAK STATUS BANNER (The Duolingo Effect)
// ===========================================

/**
 * StreakStatusBanner - Shows current streak with loss aversion messaging
 * 
 * Psychology: Fear of loss is 2x more motivating than gaining something.
 * Examples:
 * - "You've connected with someone new for 5 days straight! 🔥 Keep going!"
 * - "3-day networking streak—don't break it!"
 */
@Composable
fun StreakStatusBanner(
    connectionStreak: Int,
    isAtRisk: Boolean,
    backdrop: LayerBackdrop,
    onTap: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (connectionStreak == 0) return
    
    var isVisible by remember { mutableStateOf(true) }
    var hasDismissed by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val animatedDragOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "findStreakReminderDragY"
    )
    val titleText = if (isAtRisk) {
        "${connectionStreak}-day streak at risk"
    } else {
        "${connectionStreak}-day streak active"
    }
    val supportingText = if (isAtRisk) {
        "Send a connection today to keep it alive."
    } else {
        "Keep the momentum going with one more connection."
    }

    fun dismissWithSlideUp() {
        if (hasDismissed) return
        hasDismissed = true
        dragOffsetY = 0f
        isVisible = false
        scope.launch {
            delay(260)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        delay(5000)
        dismissWithSlideUp()
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(120)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(240, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(180))
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .graphicsLayer { translationY = animatedDragOffsetY }
                .pointerInput(Unit) {
                    var totalDragY = 0f
                    detectVerticalDragGestures(
                        onDragStart = { totalDragY = 0f },
                        onVerticalDrag = { _, dragAmount ->
                            totalDragY += dragAmount
                            dragOffsetY = totalDragY.coerceAtMost(0f)
                        },
                        onDragCancel = { dragOffsetY = 0f },
                        onDragEnd = {
                            if (totalDragY <= -48f) {
                                dismissWithSlideUp()
                            } else {
                                dragOffsetY = 0f
                            }
                        }
                    )
                }
                .clip(RoundedCornerShape(22.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(22.dp) },
                    effects = {
                        vibrancy()
                        blur(16f.dp.toPx())
                        lens(8f.dp.toPx(), 16f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color(0xFF111318).copy(alpha = 0.88f))
                    }
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF171A20).copy(alpha = 0.96f),
                            Color(0xFF251811).copy(alpha = 0.95f),
                            Color(0xFF171A20).copy(alpha = 0.96f)
                        )
                    )
                )
                .clickable(onClick = onTap)
                .padding(start = 12.dp, top = 12.dp, end = 10.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StreakFireLottie(modifier = Modifier.size(46.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    BasicText(
                        titleText,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                    BasicText(
                        supportingText,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp
                        ),
                        maxLines = 2
                    )
                }

                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            dismissWithSlideUp()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "x",
                        style = TextStyle(
                            Color.White.copy(alpha = 0.86f),
                            12.sp,
                            FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakBadge(
    count: Int,
    isAtRisk: Boolean,
    accentColor: Color
) {
    val fireRotation = rememberInfiniteTransition(label = "fire")
    val angle by fireRotation.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireAngle"
    )
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            "🔥",
            style = TextStyle(fontSize = 24.sp),
            modifier = Modifier.rotate(angle)
        )
        BasicText(
            "$count",
            style = TextStyle(
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

// ===========================================
// STREAK PROGRESS IN CELEBRATION
// ===========================================

@Composable
fun StreakProgressIndicator(
    currentStreak: Int,
    isNewMilestone: Boolean,
    modifier: Modifier = Modifier
) {
    if (currentStreak == 0) return
    
    val milestones = listOf(3, 7, 14, 30)
    val nextMilestone = milestones.firstOrNull { it > currentStreak } ?: (currentStreak + 7)
    val prevMilestone = milestones.lastOrNull { it <= currentStreak } ?: 0
    val progress = (currentStreak - prevMilestone).toFloat() / (nextMilestone - prevMilestone)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF3E0))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Streak message
        val message = when {
            isNewMilestone && currentStreak == 3 -> "🎉 3-day streak unlocked!"
            isNewMilestone && currentStreak == 7 -> "🔥 Week streak achieved!"
            isNewMilestone && currentStreak == 14 -> "💎 Two-week streak!"
            isNewMilestone && currentStreak == 30 -> "🏆 Monthly champion!"
            currentStreak >= 7 -> "🔥 ${currentStreak} days—you're on fire!"
            else -> "🔥 ${currentStreak}-day networking streak!"
        }
        
        BasicText(
            message,
            style = TextStyle(
                color = Color(0xFFE65100),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
        
        // Progress bar to next milestone
        if (currentStreak < 30) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFFD180))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                                )
                            )
                    )
                }
                BasicText(
                    "${nextMilestone - currentStreak} more day${if (nextMilestone - currentStreak > 1) "s" else ""} to ${nextMilestone}-day badge!",
                    style = TextStyle(
                        color = Color(0xFF795548),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

// ===========================================
// XP GAIN ANIMATION
// ===========================================

@Composable
fun XpGainAnimation(
    amount: Int,
    reason: String,
    isVisible: Boolean,
    onAnimationEnd: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    val yOffset = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // Pop in
            scale.animateTo(1.2f, tween(150, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(100, easing = FastOutSlowInEasing))
            
            // Float up and fade
            delay(500)
            launch {
                yOffset.animateTo(-100f, tween(800, easing = FastOutSlowInEasing))
            }
            alpha.animateTo(0f, tween(800))
            onAnimationEnd()
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(100)),
        exit = fadeOut(tween(100))
    ) {
        Box(
            Modifier
                .offset(y = yOffset.value.dp)
                .scale(scale.value),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    "+$amount XP",
                    style = TextStyle(
                        color = Color(0xFFFFD700).copy(alpha = alpha.value),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                BasicText(
                    reason,
                    style = TextStyle(
                        color = Color.White.copy(alpha = alpha.value * 0.8f),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

// ===========================================
// PUBLIC STREAK BADGE (Social Pressure)
// ===========================================

/**
 * PublicStreakBadge - Prominent streak display for profiles
 * 
 * Psychology: Public visibility creates social pressure.
 * Users are more motivated to maintain streaks when others can see them.
 */
@Composable
fun PublicStreakBadge(
    currentStreak: Int,
    longestStreak: Int,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    if (currentStreak == 0 && longestStreak == 0) return
    
    val fireRotation = rememberInfiniteTransition(label = "fire")
    val angle by fireRotation.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireAngle"
    )
    
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    // Determine badge tier
    val (tierColor, tierLabel) = when {
        currentStreak >= 30 -> Color(0xFFFFD700) to "🏆 Champion"
        currentStreak >= 14 -> Color(0xFF9B59B6) to "💎 Dedicated"
        currentStreak >= 7 -> Color(0xFF00C851) to "🔥 On Fire"
        currentStreak >= 3 -> Color(0xFFFF9F43) to "⚡ Active"
        else -> Color(0xFF4ECDC4) to "✨ Starting"
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Main streak display with badge effect
        Box(
            Modifier
                .scale(pulseScale)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            tierColor.copy(alpha = 0.2f),
                            tierColor.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    "🔥",
                    style = TextStyle(fontSize = 20.sp),
                    modifier = Modifier.rotate(angle)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText(
                        "$currentStreak",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    BasicText(
                        "day streak",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
        
        // Tier badge
        if (currentStreak >= 3) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tierColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                BasicText(
                    tierLabel,
                    style = TextStyle(
                        color = tierColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
        
        // "Visible to everyone" indicator (social pressure)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                "👁 Visible to others",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.4f),
                    fontSize = 9.sp
                )
            )
        }
        
        // Longest streak
        if (longestStreak > currentStreak) {
            BasicText(
                "Best: $longestStreak days",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            )
        }
    }
}

// ===========================================
// STREAK MILESTONE CELEBRATION
// ===========================================

@Composable
fun StreakMilestoneCelebration(
    streakCount: Int,
    streakType: String,
    isVisible: Boolean,
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.5f),
        exit = fadeOut() + scaleOut(targetScale = 0.5f)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .padding(32.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(32f.dp) },
                        effects = {
                            vibrancy()
                            blur(32f.dp.toPx())
                            lens(16f.dp.toPx(), 32f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF6B6B),
                                        Color(0xFFFF8E53)
                                    )
                                )
                            )
                        }
                    )
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Fire emoji with animation
                val rotation = rememberInfiniteTransition(label = "fire")
                val angle by rotation.animateFloat(
                    initialValue = -5f,
                    targetValue = 5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(200),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "fireAngle"
                )
                
                BasicText(
                    "🔥",
                    style = TextStyle(fontSize = 64.sp),
                    modifier = Modifier.rotate(angle)
                )
                
                BasicText(
                    "$streakCount Day Streak!",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                )
                
                BasicText(
                    "Amazing $streakType streak!",
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Reward info
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    BasicText(
                        "+${streakCount * 10} XP Bonus!",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
            // Confetti
            ConfettiOverlay(
                isVisible = isVisible,
                particleCount = 120
            )
        }
    }
}

// ===========================================
// BADGE UNLOCK ANIMATION
// ===========================================

@Composable
fun BadgeUnlockAnimation(
    badgeName: String,
    badgeEmoji: String,
    isVisible: Boolean,
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // Spin in animation
            launch {
                rotation.animateTo(360f, tween(600, easing = FastOutSlowInEasing))
            }
            scale.animateTo(1.2f, tween(400, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(200))
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .scale(scale.value)
                    .padding(32.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(28f.dp) },
                        effects = {
                            vibrancy()
                            blur(24f.dp.toPx())
                            lens(12f.dp.toPx(), 24f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6C5CE7),
                                        Color(0xFFA66CFF)
                                    )
                                )
                            )
                        }
                    )
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Badge with rotation
                BasicText(
                    badgeEmoji,
                    style = TextStyle(fontSize = 80.sp),
                    modifier = Modifier.rotate(rotation.value)
                )
                
                BasicText(
                    "Badge Unlocked!",
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                
                BasicText(
                    badgeName,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }
            
            // Confetti
            ConfettiOverlay(
                isVisible = isVisible,
                particleCount = 60
            )
        }
    }
}
