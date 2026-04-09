package com.kyant.backdrop.catalog.linkedin.groups

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.SolidColor
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

// ==================== Main Circles Screen ====================

@Composable
fun CirclesScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit = {},
    onNavigateToCircle: (String) -> Unit = {},
    onNavigateToUpgrade: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: CirclesViewModel = viewModel(factory = CirclesViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedTab by remember { mutableStateOf(CirclesTab.MY_CIRCLES) }
    var showCreateModal by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadMyCircles()
        viewModel.loadDiscoverCircles()
    }
    
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Header
            CirclesHeader(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                myCirclesCount = uiState.myCircles.size,
                maxCircles = uiState.maxCircles,
                canCreateMore = uiState.canCreateMore,
                onBackClick = onNavigateBack,
                onCreateClick = {
                    if (uiState.canCreateMore) {
                        showCreateModal = true
                    } else {
                        onNavigateToUpgrade()
                    }
                }
            )
            
            // Free plan limit warning
            AnimatedVisibility(visible = !uiState.canCreateMore) {
                FreePlanLimitBanner(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    currentCount = uiState.myCircles.size,
                    maxCount = uiState.maxCircles,
                    onUpgradeClick = onNavigateToUpgrade
                )
            }
            
            // Tabs
            CirclesTabs(
                contentColor = contentColor,
                accentColor = accentColor,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
            
            // Search
            CirclesSearchBar(
                backdrop = backdrop,
                contentColor = contentColor,
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) }
            )
            
            // Content
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }
                selectedTab == CirclesTab.MY_CIRCLES -> {
                    MyCirclesContent(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        circles = uiState.filteredMyCircles,
                        onCircleClick = onNavigateToCircle
                    )
                }
                else -> {
                    DiscoverCirclesContent(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        circles = uiState.filteredDiscoverCircles,
                        onCircleClick = onNavigateToCircle,
                        onJoinCircle = { viewModel.joinCircle(it) }
                    )
                }
            }
        }
        
        // Create Circle Modal
        if (showCreateModal) {
            CreateCircleModal(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isLoading = uiState.isCreating,
                onDismiss = { showCreateModal = false },
                onCreate = { name, description, isPrivate ->
                    viewModel.createCircle(
                        name = name,
                        description = description,
                        category = null,
                        emoji = null,
                        tags = emptyList(),
                        isPrivate = isPrivate
                    )
                    showCreateModal = false
                }
            )
        }
    }
}

// ==================== Header ====================

@Composable
private fun CirclesHeader(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    myCirclesCount: Int,
    maxCircles: Int,
    canCreateMore: Boolean,
    onBackClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .glassBackground(backdrop)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
        
        Spacer(Modifier.width(12.dp))
        
        Column(Modifier.weight(1f)) {
            BasicText(
                "Circles",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            BasicText(
                "Intimate connections with close friends",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            )
        }
        
        // Circle count indicator
        if (maxCircles > 0) {
            Box(
                Modifier
                    .background(
                        if (canCreateMore) contentColor.copy(alpha = 0.1f)
                        else Color(0xFFFFA726).copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                BasicText(
                    "$myCirclesCount/$maxCircles",
                    style = TextStyle(
                        color = if (canCreateMore) contentColor else Color(0xFFFFA726),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            Spacer(Modifier.width(8.dp))
        }
        
        // Create button
        Box(
            Modifier
                .size(40.dp)
                .background(
                    if (canCreateMore) accentColor else contentColor.copy(alpha = 0.3f),
                    CircleShape
                )
                .clip(CircleShape)
                .clickable(onClick = onCreateClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (canCreateMore) Icons.Default.Add else Icons.Default.Lock,
                contentDescription = "Create",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== Free Plan Limit Banner ====================

@Composable
private fun FreePlanLimitBanner(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    currentCount: Int,
    maxCount: Int,
    onUpgradeClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = Color(0xFFF57C00),
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(1f)) {
                BasicText(
                    "Free Plan Limit Reached",
                    style = TextStyle(
                        color = Color(0xFFE65100),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    "You've created $currentCount of $maxCount circles. Upgrade for unlimited circles!",
                    style = TextStyle(
                        color = Color(0xFFF57C00),
                        fontSize = 12.sp
                    )
                )
            }
            
            Box(
                Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF9800), Color(0xFFF57C00))
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(onClick = onUpgradeClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                BasicText(
                    "Upgrade",
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

// ==================== Tabs ====================

@Composable
private fun CirclesTabs(
    contentColor: Color,
    accentColor: Color,
    selectedTab: CirclesTab,
    onTabSelected: (CirclesTab) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CirclesTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) accentColor else contentColor.copy(alpha = 0.08f),
                        RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    when (tab) {
                        CirclesTab.MY_CIRCLES -> "My Circles"
                        CirclesTab.DISCOVER -> "Discover"
                    },
                    style = TextStyle(
                        color = if (isSelected) Color.White else contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

// ==================== Search Bar ====================

@Composable
private fun CirclesSearchBar(
    backdrop: LayerBackdrop,
    contentColor: Color,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .glassBackground(backdrop, blurRadius = 15f, vibrancyAlpha = 0.08f)
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(Modifier.width(8.dp))
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(color = contentColor, fontSize = 14.sp),
            cursorBrush = SolidColor(contentColor),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        BasicText(
                            "Search circles...",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.4f),
                                fontSize = 14.sp
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        if (value.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Clear",
                tint = contentColor.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onValueChange("") }
            )
        }
    }
}

// ==================== My Circles Content ====================

@Composable
private fun MyCirclesContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    circles: List<Circle>,
    onCircleClick: (String) -> Unit
) {
    if (circles.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(80.dp)
                        .background(accentColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                BasicText(
                    "No circles yet",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    "Create a circle to share with close friends",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(circles, key = { it.id }) { circle ->
                CircleCard(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    circle = circle,
                    isMember = true,
                    onClick = { onCircleClick(circle.id) },
                    onAction = null
                )
            }
        }
    }
}

// ==================== Discover Circles Content ====================

@Composable
private fun DiscoverCirclesContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    circles: List<Circle>,
    onCircleClick: (String) -> Unit,
    onJoinCircle: (String) -> Unit
) {
    if (circles.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                BasicText(
                    "No circles to discover",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(circles, key = { it.id }) { circle ->
                CircleCard(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    circle = circle,
                    isMember = false,
                    onClick = { onCircleClick(circle.id) },
                    onAction = { onJoinCircle(circle.id) }
                )
            }
        }
    }
}

// ==================== Circle Card ====================

@Composable
private fun CircleCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    circle: Circle,
    isMember: Boolean,
    onClick: () -> Unit,
    onAction: (() -> Unit)?
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = contentColor.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            // Circle avatar with gradient ring
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Gradient ring
                Box(
                    Modifier
                        .size(100.dp)
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
                        .padding(4.dp)
                        .background(contentColor.copy(alpha = 0.05f), CircleShape)
                        .clip(CircleShape)
                ) {
                    if (circle.imageUrl != null) {
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
                                circle.name.take(2).uppercase(),
                                style = TextStyle(
                                    color = accentColor,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
                
                // Private indicator
                if (circle.isPrivate) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .background(contentColor.copy(alpha = 0.1f), CircleShape)
                            .padding(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Private",
                            tint = contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            
            // Info
            Column(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    circle.name,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                BasicText(
                    "${circle.memberCount} members",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                )
                
                // Join/Member button
                if (onAction != null) {
                    Spacer(Modifier.height(8.dp))
                    
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(accentColor, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onAction)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            BasicText(
                                "Join",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                } else if (isMember) {
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        BasicText(
                            "Member",
                            style = TextStyle(
                                color = Color(0xFF4CAF50),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ==================== Create Circle Modal ====================

@Composable
fun CreateCircleModal(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, isPrivate: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    
    val isValid = name.length >= 2
    
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {},
            colors = CardDefaults.cardColors(
                containerColor = contentColor.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                Modifier
                    .glassBackground(backdrop, blurRadius = 30f, vibrancyAlpha = 0.15f)
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Create Circle",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = contentColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(onClick = onDismiss)
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                BasicText(
                    "Circles are intimate spaces for close friends",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                )
                
                Spacer(Modifier.height(20.dp))
                
                // Circle Name
                Column {
                    BasicText(
                        "Circle Name *",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    BasicTextField(
                        value = name,
                        onValueChange = { if (it.length <= 50) name = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(contentColor.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        textStyle = TextStyle(color = contentColor, fontSize = 15.sp),
                        cursorBrush = SolidColor(accentColor),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box {
                                if (name.isEmpty()) {
                                    BasicText(
                                        "e.g., Close Friends, Study Group...",
                                        style = TextStyle(
                                            color = contentColor.copy(alpha = 0.4f),
                                            fontSize = 15.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Description
                Column {
                    BasicText(
                        "Description (optional)",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    BasicTextField(
                        value = description,
                        onValueChange = { if (it.length <= 200) description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(contentColor.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        textStyle = TextStyle(color = contentColor, fontSize = 15.sp),
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { innerTextField ->
                            Box {
                                if (description.isEmpty()) {
                                    BasicText(
                                        "What's this circle about?",
                                        style = TextStyle(
                                            color = contentColor.copy(alpha = 0.4f),
                                            fontSize = 15.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Privacy toggle
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(contentColor.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                        .clickable { isPrivate = !isPrivate }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isPrivate) accentColor else contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            "Private Circle",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        BasicText(
                            "Only invited members can see and join",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        )
                    }
                    
                    Box(
                        Modifier
                            .size(24.dp)
                            .border(
                                2.dp,
                                if (isPrivate) accentColor else contentColor.copy(alpha = 0.3f),
                                CircleShape
                            )
                            .background(
                                if (isPrivate) accentColor else Color.Transparent,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPrivate) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Create button
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            if (isValid && !isLoading) accentColor else contentColor.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = isValid && !isLoading) {
                            onCreate(name, description, isPrivate)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        BasicText(
                            "Create Circle",
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
