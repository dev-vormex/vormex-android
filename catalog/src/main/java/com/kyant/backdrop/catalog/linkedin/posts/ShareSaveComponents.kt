package com.kyant.backdrop.catalog.linkedin.posts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.linkedin.PostShareTarget
import com.kyant.backdrop.catalog.linkedin.VormexSurfaceTone
import com.kyant.backdrop.catalog.linkedin.currentVormexAppearance
import com.kyant.backdrop.catalog.linkedin.vormexSurface
import kotlinx.coroutines.launch

private val ShareButtonGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4B70E2), Color(0xFF3a5bc7))
)

/**
 * Share Post Modal - In-app people picker plus copy link (Glass Theme)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostModal(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean = true,
    shareTargets: List<PostShareTarget>,
    isLoading: Boolean,
    isSharing: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCopyLink: () -> Unit,
    onShareToTargets: (List<String>, String?) -> Unit,
    onClearError: () -> Unit,
    subjectLabel: String = "post",
    showMessageInput: Boolean = true,
    onShareAnimation: () -> Unit = {}
) {
    val appearance = currentVormexAppearance(
        fallbackThemeMode = if (isLightTheme) "light" else "dark"
    )
    val glassBackground = appearance.sheetColor
    val glassBubbleBackground = appearance.inputColor
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var shareMessage by remember { mutableStateOf("") }
    var selectedTargets by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isClosingAfterShare by remember { mutableStateOf(false) }
    val visibleTargets = remember(shareTargets, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isBlank()) {
            shareTargets
        } else {
            shareTargets.filter { target ->
                listOfNotNull(
                    target.name,
                    target.username,
                    target.headline,
                    target.reason
                ).any { it.lowercase().contains(query) }
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0.dp) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .vormexSurface(
                    backdrop = backdrop,
                    tone = VormexSurfaceTone.Sheet,
                    cornerRadius = 28.dp,
                    blurRadius = 32.dp,
                    lensRadius = 16.dp,
                    lensDepth = 32.dp,
                    surfaceColor = glassBackground,
                    borderColor = appearance.sheetBorderColor
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = "Share",
                        style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(glassBubbleBackground.copy(alpha = 0.5f))
                                .clickable(onClick = onCopyLink),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_link),
                                contentDescription = "Copy link",
                                modifier = Modifier.size(19.dp),
                                colorFilter = ColorFilter.tint(contentColor)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(glassBubbleBackground.copy(alpha = 0.5f))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "Close",
                                modifier = Modifier.size(20.dp),
                                colorFilter = ColorFilter.tint(contentColor)
                            )
                        }
                    }
                }
                
                // Error message
                error?.let { errorMsg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Red.copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                text = errorMsg,
                                style = TextStyle(Color.Red.copy(alpha = 0.8f), 13.sp),
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onClearError() }
                                    .padding(4.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = "Dismiss error",
                                    modifier = Modifier.size(14.dp),
                                    colorFilter = ColorFilter.tint(Color.Red)
                                )
                            }
                        }
                    }
                }
                
                // Divider
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = contentColor.copy(alpha = 0.1f)
                )
                
                if (showMessageInput) {
                    // Share message
                    BasicText(
                        text = "Add a message (optional)",
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    BasicTextField(
                        value = shareMessage,
                        onValueChange = { shareMessage = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(glassBubbleBackground.copy(alpha = 0.5f))
                            .padding(12.dp),
                        textStyle = TextStyle(contentColor, 14.sp),
                        cursorBrush = SolidColor(contentColor),
                        maxLines = 3,
                        decorationBox = { innerTextField ->
                            Box {
                                if (shareMessage.isEmpty()) {
                                    BasicText(
                                        text = "Say something about this $subjectLabel...",
                                        style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Send to people
                BasicText(
                    text = "Send in Vormex",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                
                // People search
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(glassBubbleBackground.copy(alpha = 0.5f))
                        .padding(12.dp),
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(contentColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.5f))
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    BasicText(
                                        text = "Search people...",
                                        style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                )
                
                // Selected people chips
                if (selectedTargets.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedTargets.take(3).forEach { targetId ->
                            val target = shareTargets.find { it.id == targetId }
                            target?.let {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(accentColor.copy(alpha = 0.15f))
                                        .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BasicText(
                                            text = it.name?.split(" ")?.firstOrNull() ?: it.username ?: "User",
                                            style = TextStyle(accentColor, 12.sp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    selectedTargets = selectedTargets - targetId
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_close),
                                                contentDescription = "Remove",
                                                modifier = Modifier.size(10.dp),
                                                colorFilter = ColorFilter.tint(accentColor.copy(alpha = 0.7f))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (selectedTargets.size > 3) {
                            BasicText(
                                text = "+${selectedTargets.size - 3} more",
                                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                            )
                        }
                    }
                }
                
                // People list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isLoading) {
                        items(3) {
                            ConnectionSkeleton(contentColor)
                        }
                    } else {
                        items(visibleTargets, key = { it.id }) { target ->
                            ShareTargetItem(
                                target = target,
                                isSelected = target.id in selectedTargets,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                glassBubbleBackground = glassBubbleBackground,
                                onClick = {
                                    selectedTargets = if (target.id in selectedTargets) {
                                        selectedTargets - target.id
                                    } else {
                                        selectedTargets + target.id
                                    }
                                }
                            )
                        }
                        
                        if (visibleTargets.isEmpty() && !isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText(
                                        text = if (searchQuery.isBlank()) {
                                            "No share suggestions yet"
                                        } else {
                                            "No people found"
                                        },
                                        style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Share button
                if (selectedTargets.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ShareButtonGradient)
                            .clickable(enabled = !isSharing && !isClosingAfterShare) {
                                isClosingAfterShare = true
                                onShareToTargets(
                                    selectedTargets.toList(),
                                    shareMessage.ifBlank { null }
                                )
                                scope.launch {
                                    sheetState.hide()
                                    onDismiss()
                                    onShareAnimation()
                                }
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSharing || isClosingAfterShare) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            BasicText(
                                text = "Send to ${selectedTargets.size} ${if (selectedTargets.size == 1) "person" else "people"}",
                                style = TextStyle(Color.White, 15.sp, FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareTargetItem(
    target: PostShareTarget,
    isSelected: Boolean,
    contentColor: Color,
    accentColor: Color,
    glassBubbleBackground: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.15f)
                else glassBubbleBackground.copy(alpha = 0.4f)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (!target.avatar.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(target.avatar).build(),
                    contentDescription = "${target.name ?: target.username ?: "User"}'s avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val initials = target.name?.split(" ")
                    ?.mapNotNull { it.firstOrNull()?.uppercase() }
                    ?.take(2)
                    ?.joinToString("") ?: "?"
                BasicText(
                    text = initials,
                    style = TextStyle(Color.White, 14.sp, FontWeight.Bold)
                )
            }
        }
        
        // User info
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = target.name ?: target.username ?: "Vormex user",
                style = TextStyle(contentColor, 15.sp, FontWeight.Medium)
            )
            target.headline?.let { headline ->
                BasicText(
                    text = headline,
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 13.sp),
                    maxLines = 1
                )
            }
            BasicText(
                text = target.reason,
                style = TextStyle(accentColor.copy(alpha = 0.86f), 12.sp, FontWeight.Medium),
                maxLines = 1
            )
        }
        
        // Selection indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) accentColor
                    else contentColor.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Image(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "Selected",
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }
    }
}

@Composable
private fun ConnectionSkeleton(contentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.1f))
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(contentColor.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(contentColor.copy(alpha = 0.08f))
            )
        }
    }
}

/**
 * Save/Bookmark Indicator - Visual feedback for saved state
 */
@Composable
fun SaveIndicator(
    isSaved: Boolean,
    contentColor: Color,
    accentColor: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isSaved) accentColor.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable(onClick = onToggle)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                if (isSaved) R.drawable.ic_bookmark
                else R.drawable.ic_bookmark_outline
            ),
            contentDescription = if (isSaved) "Saved" else "Save",
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(
                if (isSaved) accentColor else contentColor
            )
        )
    }
}

/**
 * Saved Posts Empty State
 */
@Composable
fun SavedPostsEmptyState(
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_bookmark_outline),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.3f))
            )
            BasicText(
                text = "No saved posts",
                style = TextStyle(contentColor, 18.sp, FontWeight.SemiBold)
            )
            BasicText(
                text = "Save posts to read them later",
                style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
            )
        }
    }
}
