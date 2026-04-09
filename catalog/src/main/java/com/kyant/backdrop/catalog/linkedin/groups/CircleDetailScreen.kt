package com.kyant.backdrop.catalog.linkedin.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import java.text.SimpleDateFormat
import java.util.Locale

// ==================== Tab Enum ====================

enum class CircleDetailTab {
    POSTS, MEMBERS, ABOUT
}

// ==================== Circle Detail Screen ====================

@Composable
fun CircleDetailScreen(
    circleId: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    currentUserId: String?,
    onNavigateBack: () -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onInviteMember: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: CirclesViewModel = viewModel(factory = CirclesViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedTab by remember { mutableStateOf(CircleDetailTab.POSTS) }
    var showMenu by remember { mutableStateOf(false) }
    
    LaunchedEffect(circleId) {
        viewModel.loadCircleDetail(circleId)
    }
    
    val circle = uiState.currentCircle
    
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header with back button and menu
        item {
            CircleDetailHeader(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                circle = circle,
                showMenu = showMenu,
                isOwner = circle?.myRole == "creator" || circle?.myRole == "owner",
                isMember = circle?.isMember == true,
                onBackClick = onNavigateBack,
                onMenuClick = { showMenu = !showMenu },
                onMenuDismiss = { showMenu = false },
                onLeaveCircle = { circle?.let { viewModel.leaveCircle(it.id) } },
                onEditCircle = { /* TODO: Navigate to edit */ },
                onInviteMember = onInviteMember
            )
        }
        
        // Circle Avatar and Info
        item {
            CircleAvatarSection(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                circle = circle
            )
        }
        
        // Action buttons
        item {
            CircleActionButtons(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                circle = circle,
                isLoading = uiState.isJoining,
                onJoin = { circle?.let { viewModel.joinCircle(it.id) } },
                onLeave = { circle?.let { viewModel.leaveCircle(it.id) } },
                onInvite = onInviteMember
            )
        }
        
        // Tabs
        item {
            CircleDetailTabs(
                contentColor = contentColor,
                accentColor = accentColor,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
        
        // Content based on selected tab
        when {
            uiState.isLoading -> {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }
            }
            selectedTab == CircleDetailTab.POSTS -> {
                if (uiState.circlePosts.isEmpty()) {
                    item {
                        EmptyPostsState(contentColor = contentColor, accentColor = accentColor)
                    }
                } else {
                    items(uiState.circlePosts, key = { it.id }) { post ->
                        CirclePostCard(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            post = post,
                            onAuthorClick = { onNavigateToProfile(post.authorId) },
                            onLikeClick = { /* TODO: Like post */ }
                        )
                    }
                }
            }
            selectedTab == CircleDetailTab.MEMBERS -> {
                if (uiState.circleMembers.isEmpty()) {
                    item {
                        EmptyMembersState(contentColor = contentColor)
                    }
                } else {
                    items(uiState.circleMembers, key = { it.id }) { member ->
                        CircleMemberCard(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            member = member,
                            isCreator = member.role == "creator" || member.role == "owner",
                            onClick = { onNavigateToProfile(member.id) }
                        )
                    }
                }
            }
            else -> {
                item {
                    CircleAboutSection(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        circle = circle
                    )
                }
            }
        }
    }
}

// ==================== Header ====================

@Composable
private fun CircleDetailHeader(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    circle: Circle?,
    showMenu: Boolean,
    isOwner: Boolean,
    isMember: Boolean,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onLeaveCircle: () -> Unit,
    onEditCircle: () -> Unit,
    onInviteMember: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .glassBackground(backdrop)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(contentColor.copy(alpha = 0.1f), CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(Modifier.weight(1f))
        
        if (isMember) {
            Box {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(contentColor.copy(alpha = 0.1f), CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onMenuClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = onMenuDismiss,
                    containerColor = contentColor.copy(alpha = 0.1f)
                ) {
                    // Invite option
                    Row(
                        Modifier
                            .clickable {
                                onInviteMember()
                                onMenuDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        BasicText(
                            "Invite Members",
                            style = TextStyle(color = contentColor, fontSize = 14.sp)
                        )
                    }
                    
                    // Edit option (owner only)
                    if (isOwner) {
                        Row(
                            Modifier
                                .clickable {
                                    onEditCircle()
                                    onMenuDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicText(
                                "Edit Circle",
                                style = TextStyle(color = contentColor, fontSize = 14.sp)
                            )
                        }
                    }
                    
                    // Leave option (non-owners)
                    if (!isOwner) {
                        Row(
                            Modifier
                                .clickable {
                                    onLeaveCircle()
                                    onMenuDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicText(
                                "Leave Circle",
                                style = TextStyle(color = Color(0xFFF44336), fontSize = 14.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Avatar Section ====================

@Composable
private fun CircleAvatarSection(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    circle: Circle?
) {
    val context = LocalContext.current
    
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large avatar with gradient ring
        Box(
            Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Gradient ring
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFF6B6B),
                                    Color(0xFFFFE66D),
                                    Color(0xFF4ECDC4),
                                    Color(0xFFFF6B6B)
                                )
                            ),
                            radius = size.minDimension / 2
                        )
                    }
                    .padding(5.dp)
                    .background(contentColor.copy(alpha = 0.05f), CircleShape)
                    .clip(CircleShape)
            ) {
                if (circle?.imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(circle.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BasicText(
                            circle?.name?.take(2)?.uppercase() ?: "?",
                            style = TextStyle(
                                color = accentColor,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Name
        BasicText(
            circle?.name ?: "Loading...",
            style = TextStyle(
                color = contentColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
        
        Spacer(Modifier.height(4.dp))
        
        // Privacy badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                if (circle?.isPrivate == true) Icons.Default.Lock else Icons.Default.Public,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            BasicText(
                if (circle?.isPrivate == true) "Private Circle" else "Public Circle",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Stats row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            StatItem(
                contentColor = contentColor,
                value = "${circle?.memberCount ?: 0}",
                label = "Members"
            )
            
            Box(
                Modifier
                    .padding(horizontal = 24.dp)
                    .width(1.dp)
                    .height(30.dp)
                    .background(contentColor.copy(alpha = 0.2f))
            )
            
            StatItem(
                contentColor = contentColor,
                value = "${circle?.postsCount ?: 0}",
                label = "Posts"
            )
        }
        
        // Description
        if (!circle?.description.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            BasicText(
                circle?.description ?: "",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun StatItem(
    contentColor: Color,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            value,
            style = TextStyle(
                color = contentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        )
        BasicText(
            label,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        )
    }
}

// ==================== Action Buttons ====================

@Composable
private fun CircleActionButtons(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    circle: Circle?,
    isLoading: Boolean,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onInvite: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (circle?.isMember == true) {
            // Member button (shows joined status)
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    BasicText(
                        "Member",
                        style = TextStyle(
                            color = Color(0xFF4CAF50),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            // Invite button
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(accentColor, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onInvite),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    BasicText(
                        "Invite",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        } else {
            // Join button
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        if (isLoading) contentColor.copy(alpha = 0.3f) else accentColor,
                        RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isLoading, onClick = onJoin),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            "Join Circle",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

// ==================== Tabs ====================

@Composable
private fun CircleDetailTabs(
    contentColor: Color,
    accentColor: Color,
    selectedTab: CircleDetailTab,
    onTabSelected: (CircleDetailTab) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircleDetailTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) accentColor else contentColor.copy(alpha = 0.08f),
                        RoundedCornerShape(10.dp)
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    when (tab) {
                        CircleDetailTab.POSTS -> "Posts"
                        CircleDetailTab.MEMBERS -> "Members"
                        CircleDetailTab.ABOUT -> "About"
                    },
                    style = TextStyle(
                        color = if (isSelected) Color.White else contentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

// ==================== Post Card ====================

@Composable
private fun CirclePostCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    post: CirclePost,
    onAuthorClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = contentColor.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .glassBackground(backdrop, blurRadius = 15f, vibrancyAlpha = 0.08f)
                .padding(16.dp)
        ) {
            // Author row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onAuthorClick)
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(accentColor.copy(alpha = 0.1f), CircleShape)
                        .clip(CircleShape)
                ) {
                    if (post.author?.profileImage != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(post.author.profileImage)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                Spacer(Modifier.width(10.dp))
                
                Column {
                    BasicText(
                        post.author?.name ?: post.author?.username ?: "Unknown",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    BasicText(
                        formatPostTime(post.createdAt ?: ""),
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
            
            // Content
            if (post.content.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                BasicText(
                    post.content,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                )
            }
            
            // Media
            val mediaUrl = post.mediaUrls.firstOrNull()
            if (mediaUrl != null) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(mediaUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Actions
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like button
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onLikeClick)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = contentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                    if (post.likesCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        BasicText(
                            "${post.likesCount}",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        )
                    }
                }
                
                // Comment count
                if (post.commentsCount > 0) {
                    Spacer(Modifier.width(16.dp))
                    BasicText(
                        "${post.commentsCount} comments",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

// ==================== Member Card ====================

@Composable
private fun CircleMemberCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    member: CircleMember,
    isCreator: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .background(accentColor.copy(alpha = 0.1f), CircleShape)
                .clip(CircleShape)
        ) {
            if (member.profileImage != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(member.profileImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    member.name ?: member.username ?: "Unknown",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                
                if (isCreator) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            BasicText(
                                "Creator",
                                style = TextStyle(
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
            
            if (member.username != null) {
                BasicText(
                    "@${member.username}",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                )
            }
        }
        
        // Joined date
        BasicText(
            "Joined ${formatJoinDate(member.joinedAt ?: "")}",
            style = TextStyle(
                color = contentColor.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
        )
    }
}

// ==================== About Section ====================

@Composable
private fun CircleAboutSection(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    circle: Circle?
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Description
        if (!circle?.description.isNullOrBlank()) {
            AboutCard(
                backdrop = backdrop,
                contentColor = contentColor,
                title = "About",
                content = circle?.description ?: ""
            )
            
            Spacer(Modifier.height(12.dp))
        }
        
        // Details
        Card(
            colors = CardDefaults.cardColors(
                containerColor = contentColor.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier
                    .glassBackground(backdrop, blurRadius = 15f, vibrancyAlpha = 0.08f)
                    .padding(16.dp)
            ) {
                BasicText(
                    "Details",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                Spacer(Modifier.height(12.dp))
                
                DetailRow(
                    contentColor = contentColor,
                    label = "Privacy",
                    value = if (circle?.isPrivate == true) "Private" else "Public"
                )
                
                DetailRow(
                    contentColor = contentColor,
                    label = "Members",
                    value = "${circle?.memberCount ?: 0}"
                )
                
                DetailRow(
                    contentColor = contentColor,
                    label = "Posts",
                    value = "${circle?.postsCount ?: 0}"
                )
                
                if (circle?.createdAt != null) {
                    DetailRow(
                        contentColor = contentColor,
                        label = "Created",
                        value = formatCreatedDate(circle.createdAt)
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    title: String,
    content: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = contentColor.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .glassBackground(backdrop, blurRadius = 15f, vibrancyAlpha = 0.08f)
                .padding(16.dp)
        ) {
            BasicText(
                title,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            Spacer(Modifier.height(8.dp))
            
            BasicText(
                content,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            )
        }
    }
}

@Composable
private fun DetailRow(
    contentColor: Color,
    label: String,
    value: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        )
        BasicText(
            value,
            style = TextStyle(
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

// ==================== Empty States ====================

@Composable
private fun EmptyPostsState(
    contentColor: Color,
    accentColor: Color
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(60.dp)
                    .background(accentColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            BasicText(
                "No posts yet",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            BasicText(
                "Be the first to share something!",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            )
        }
    }
}

@Composable
private fun EmptyMembersState(contentColor: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            "No members to show",
            style = TextStyle(
                color = contentColor.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        )
    }
}

// ==================== Utility Functions ====================

private fun formatPostTime(isoDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(isoDate.take(19)) ?: return isoDate
        val now = System.currentTimeMillis()
        val diff = now - date.time
        
        when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

private fun formatJoinDate(isoDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(isoDate.take(19)) ?: return isoDate
        SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

private fun formatCreatedDate(isoDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(isoDate.take(19)) ?: return isoDate
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        isoDate.take(10)
    }
}
