package com.kyant.backdrop.catalog.linkedin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.models.DailyMatchUser
import com.kyant.backdrop.catalog.network.models.HiddenGemUser
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay

// ==================== Daily Matches Banner (Variable Count 1-5) ====================

@Composable
fun DailyMatchesBanner(
    matches: List<DailyMatchUser>,
    matchCount: Int,
    surpriseMessage: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onMatchClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (matches.isEmpty()) return
    
    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300) // Slight delay for dramatic effect
        visible = true
    }
    
    // Pulse animation for the count badge
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
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(16f.dp) },
                    effects = {
                        vibrancy()
                        blur(24f.dp.toPx())
                    }
                )
                .background(accentColor.copy(alpha = 0.15f))
                .padding(16.dp)
        ) {
            Column {
                // Header with count badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Animated count badge
                    Box(
                        Modifier
                            .scale(scale)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF6B6B),
                                        Color(0xFFFFE66D)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = "$matchCount",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            text = if (matchCount == 1) "🎯 New Match Today!" else "🎯 $matchCount New Matches!",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        BasicText(
                            text = surpriseMessage,
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        )
                    }
                    
                    // Dismiss button
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.1f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = "✕",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Match avatars row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy((-12).dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(matches.take(5)) { match ->
                        DailyMatchAvatar(
                            user = match,
                            contentColor = contentColor,
                            onClick = { onMatchClick(match.id) }
                        )
                    }
                    
                    // "View All" if more than shown
                    if (matchCount > 5) {
                        item {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.3f))
                                    .clickable { /* Expand view */ },
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    text = "+${matchCount - 5}",
                                    style = TextStyle(
                                        color = contentColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
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
private fun DailyMatchAvatar(
    user: DailyMatchUser,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
                if (user.isOnline) 
                    Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)))
                else 
                    Brush.linearGradient(listOf(contentColor.copy(alpha = 0.2f), contentColor.copy(alpha = 0.1f)))
            )
            .padding(2.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(user.profileImage ?: "")
                .crossfade(true)
                .build(),
            contentDescription = user.name,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        // Online indicator
        if (user.isOnline) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
                    .padding(2.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }
}

// ==================== Hidden Gem Card (Weekly Surprise) ====================

@Composable
fun HiddenGemCard(
    user: HiddenGemUser,
    message: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onViewProfile: () -> Unit,
    onConnect: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Shimmer animation for the gem icon
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(500) // Dramatic reveal
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(20f.dp) },
                    effects = {
                        vibrancy()
                        blur(24f.dp.toPx())
                    }
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.15f),
                            Color(0xFFFFA500).copy(alpha = 0.1f),
                            Color(0xFFFF6B6B).copy(alpha = 0.08f)
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 400f, 200f)
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Gem icon with shimmer
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFFA500)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = "💎",
                            style = TextStyle(fontSize = 24.sp)
                        )
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            text = "This Week's Hidden Gem",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        BasicText(
                            text = message,
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Dismiss
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.1f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = "✕",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // User profile preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.05f))
                        .clickable { onViewProfile() }
                        .padding(12.dp)
                ) {
                    // Avatar
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user.profileImage ?: "")
                            .crossfade(true)
                            .build(),
                        contentDescription = user.name,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicText(
                                text = user.name ?: "Unknown",
                                style = TextStyle(
                                    color = contentColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            if (user.isOnline) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                            }
                        }
                        if (user.headline != null) {
                            BasicText(
                                text = user.headline,
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Reply rate badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            BasicText(
                                text = "⚡",
                                style = TextStyle(fontSize = 12.sp)
                            )
                            BasicText(
                                text = " ${user.replyRate}% reply rate",
                                style = TextStyle(
                                    color = Color(0xFF4CAF50),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Connect button
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(accentColor, accentColor.copy(alpha = 0.8f))
                            )
                        )
                        .clickable { onConnect() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "✨ Connect with Hidden Gem",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

// ==================== Trending Banner ("You're Trending Today!") ====================

@Composable
fun TrendingBanner(
    rank: Int?,
    viewsToday: Int,
    message: String?,
    backdrop: LayerBackdrop,
    contentColor: Color,
    onDismiss: () -> Unit,
    onViewStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Celebration animation
    val infiniteTransition = rememberInfiniteTransition(label = "celebrate")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200)
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn() + scaleIn(initialScale = 0.8f),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(16f.dp) },
                    effects = {
                        vibrancy()
                        blur(24f.dp.toPx())
                    }
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF6B6B).copy(alpha = 0.2f),
                            Color(0xFFFFE66D).copy(alpha = 0.15f),
                            Color(0xFF4ECDC4).copy(alpha = 0.1f)
                        )
                    )
                )
                .clickable { onViewStats() }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Trending icon with bounce
                Box(
                    Modifier
                        .offset(y = (-bounce).dp)
                ) {
                    BasicText(
                        text = "🔥",
                        style = TextStyle(fontSize = 32.sp)
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = "You're Trending Today!",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (rank != null) {
                            BasicText(
                                text = "#$rank",
                                style = TextStyle(
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            BasicText(
                                text = " • ",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            )
                        }
                        BasicText(
                            text = "$viewsToday profile views today",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        )
                    }
                    
                    if (message != null) {
                        BasicText(
                            text = message,
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Dismiss
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.1f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "✕",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}

// ==================== Combined Variable Rewards Section ====================

@Composable
fun VariableRewardsSection(
    // Daily Matches
    dailyMatches: List<DailyMatchUser>,
    dailyMatchCount: Int,
    surpriseMessage: String,
    showDailyMatchesBanner: Boolean,
    // Hidden Gem
    hiddenGem: HiddenGemUser?,
    hiddenGemMessage: String,
    showHiddenGemCard: Boolean,
    // Trending
    isTrending: Boolean,
    trendingRank: Int?,
    trendingViewsToday: Int,
    trendingMessage: String?,
    showTrendingBanner: Boolean,
    // Backdrop & styling
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    // Actions
    onMatchClick: (String) -> Unit,
    onHiddenGemViewProfile: () -> Unit,
    onHiddenGemConnect: () -> Unit,
    onViewTrendingStats: () -> Unit,
    onDismissDailyMatches: () -> Unit,
    onDismissHiddenGem: () -> Unit,
    onDismissTrending: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Trending Banner (highest priority - exciting news)
        if (showTrendingBanner && isTrending) {
            TrendingBanner(
                rank = trendingRank,
                viewsToday = trendingViewsToday,
                message = trendingMessage,
                backdrop = backdrop,
                contentColor = contentColor,
                onDismiss = onDismissTrending,
                onViewStats = onViewTrendingStats
            )
        }
        
        // Daily Matches Banner
        if (showDailyMatchesBanner && dailyMatches.isNotEmpty()) {
            DailyMatchesBanner(
                matches = dailyMatches,
                matchCount = dailyMatchCount,
                surpriseMessage = surpriseMessage,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onMatchClick = onMatchClick,
                onDismiss = onDismissDailyMatches
            )
        }
        
        // Hidden Gem Card (weekly special)
        if (showHiddenGemCard && hiddenGem != null) {
            HiddenGemCard(
                user = hiddenGem,
                message = hiddenGemMessage,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onViewProfile = onHiddenGemViewProfile,
                onConnect = onHiddenGemConnect,
                onDismiss = onDismissHiddenGem
            )
        }
    }
}
