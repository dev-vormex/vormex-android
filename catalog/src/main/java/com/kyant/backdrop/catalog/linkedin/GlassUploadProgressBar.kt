package com.kyant.backdrop.catalog.linkedin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

/**
 * Instagram-style glass upload progress bar
 * Shows at the top of the feed during post upload with smooth animations
 */
@Composable
fun GlassUploadProgressBar(
    uploadProgress: UploadProgress,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated visibility for the entire component
    AnimatedVisibility(
        visible = uploadProgress.status != UploadStatus.IDLE,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        val isSuccess = uploadProgress.status == UploadStatus.SUCCESS
        val isFailed = uploadProgress.status == UploadStatus.FAILED
        val isProcessing = uploadProgress.status == UploadStatus.PROCESSING
        
        // Animated progress value
        val animatedProgress by animateFloatAsState(
            targetValue = uploadProgress.progress,
            animationSpec = tween(300, easing = LinearEasing),
            label = "progress"
        )
        
        // Pulsing animation for processing state
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        
        // Progress bar gradient colors based on status
        val progressGradient = when {
            isSuccess -> Brush.horizontalGradient(
                colors = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
            )
            isFailed -> Brush.horizontalGradient(
                colors = listOf(Color(0xFFE53935), Color(0xFFFF5252))
            )
            else -> Brush.horizontalGradient(
                colors = listOf(accentColor, accentColor.copy(alpha = 0.8f))
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(16f.dp) },
                    effects = {
                        vibrancy()
                        blur(20f.dp.toPx())
                        lens(8f.dp.toPx(), 16f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(
                            color = when {
                                isSuccess -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                isFailed -> Color(0xFFE53935).copy(alpha = 0.1f)
                                else -> Color.White.copy(alpha = 0.15f)
                            }
                        )
                    }
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status row with icon and message
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status icon
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSuccess -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                        isFailed -> Color(0xFFE53935).copy(alpha = 0.2f)
                                        else -> accentColor.copy(alpha = 0.2f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = when {
                                    isSuccess -> "✓"
                                    isFailed -> "!"
                                    isProcessing -> "⟳"
                                    else -> getPostTypeIcon(uploadProgress.postType)
                                },
                                style = TextStyle(
                                    color = when {
                                        isSuccess -> Color(0xFF4CAF50)
                                        isFailed -> Color(0xFFE53935)
                                        else -> accentColor
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        
                        // Status message
                        BasicText(
                            text = uploadProgress.message,
                            style = TextStyle(
                                color = contentColor.copy(
                                    alpha = if (isProcessing) pulseAlpha else 1f
                                ),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    
                    // Dismiss button (only for error or success)
                    if (isSuccess || isFailed) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(contentColor.copy(alpha = 0.1f))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = "×",
                                style = TextStyle(
                                    color = contentColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
                
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(contentColor.copy(alpha = 0.1f))
                ) {
                    // Animated progress fill
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(2.dp))
                            .background(progressGradient)
                    )
                    
                    // Shimmer effect for uploading state
                    if (!isSuccess && !isFailed) {
                        val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
                        val shimmerOffset by shimmerTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "shimmerOffset"
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.3f),
                                            Color.Transparent
                                        ),
                                        startX = shimmerOffset * 500f - 200f,
                                        endX = shimmerOffset * 500f + 200f
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact version of the progress bar - just shows the linear progress
 */
@Composable
fun CompactUploadProgressBar(
    uploadProgress: UploadProgress,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = uploadProgress.status != UploadStatus.IDLE && uploadProgress.status != UploadStatus.SUCCESS,
        enter = fadeIn(animationSpec = tween(200)) + expandVertically(),
        exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(),
        modifier = modifier
    ) {
        val animatedProgress by animateFloatAsState(
            targetValue = uploadProgress.progress,
            animationSpec = tween(300, easing = LinearEasing),
            label = "compactProgress"
        )
        
        val isFailed = uploadProgress.status == UploadStatus.FAILED
        val progressColor = if (isFailed) Color(0xFFE53935) else accentColor
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(progressColor.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(progressColor)
            )
        }
    }
}

private fun getPostTypeIcon(postType: String): String {
    return when (postType) {
        "TEXT" -> "📝"
        "IMAGE" -> "🖼️"
        "VIDEO" -> "🎥"
        "LINK" -> "🔗"
        "POLL" -> "📊"
        "ARTICLE" -> "📰"
        "CELEBRATION" -> "🎉"
        else -> "📤"
    }
}
