package com.kyant.backdrop.catalog.linkedin.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy

// ==================== Create Group Modal ====================

@Composable
fun CreateGroupModal(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String?, privacy: String, category: String?, rules: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPrivacy by remember { mutableStateOf("PUBLIC") }
    var category by remember { mutableStateOf("") }
    val rules = remember { mutableStateListOf<String>() }
    var newRule by remember { mutableStateOf("") }
    var showRulesSection by remember { mutableStateOf(false) }
    
    val nameError = name.length < 3
    val canCreate = name.length >= 3 && !isCreating
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth(0.9f)
                    .glassBackground(backdrop, blurRadius = 30f, vibrancyAlpha = 0.15f)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(enabled = false) { } // Prevent dismiss on content click
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Create Group",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Box(
                        Modifier
                            .size(32.dp)
                            .background(contentColor.copy(alpha = 0.1f), CircleShape)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Name field
                InputField(
                    contentColor = contentColor,
                    label = "Group Name *",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Enter group name",
                    error = if (name.isNotEmpty() && nameError) "Name must be at least 3 characters" else null
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Description field
                InputField(
                    contentColor = contentColor,
                    label = "Description",
                    value = description,
                    onValueChange = { if (it.length <= 500) description = it },
                    placeholder = "What's this group about?",
                    singleLine = false,
                    maxLines = 4,
                    helper = "${description.length}/500"
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Privacy selection
                BasicText(
                    "Privacy",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                
                Spacer(Modifier.height(8.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrivacyOption(
                        contentColor = contentColor,
                        accentColor = accentColor,
                        icon = Icons.Default.Public,
                        title = "Public",
                        description = "Anyone can find and join",
                        isSelected = selectedPrivacy == "PUBLIC",
                        onClick = { selectedPrivacy = "PUBLIC" }
                    )
                    
                    PrivacyOption(
                        contentColor = contentColor,
                        accentColor = accentColor,
                        icon = Icons.Default.Lock,
                        title = "Private",
                        description = "Request to join required",
                        isSelected = selectedPrivacy == "PRIVATE",
                        onClick = { selectedPrivacy = "PRIVATE" }
                    )
                    
                    PrivacyOption(
                        contentColor = contentColor,
                        accentColor = accentColor,
                        icon = Icons.Default.VisibilityOff,
                        title = "Secret",
                        description = "Invite only, not discoverable",
                        isSelected = selectedPrivacy == "SECRET",
                        onClick = { selectedPrivacy = "SECRET" }
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Category field
                InputField(
                    contentColor = contentColor,
                    label = "Category (optional)",
                    value = category,
                    onValueChange = { category = it },
                    placeholder = "e.g., Technology, Sports, Gaming"
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Rules section (collapsible)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showRulesSection = !showRulesSection }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BasicText(
                        "Group Rules (optional)",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    BasicText(
                        if (showRulesSection) "Hide" else "Add",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 13.sp
                        )
                    )
                }
                
                AnimatedVisibility(
                    visible = showRulesSection,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        // Existing rules
                        rules.forEachIndexed { index, rule ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicText(
                                    "${index + 1}. $rule",
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color.Red.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { rules.removeAt(index) }
                                )
                            }
                        }
                        
                        // Add new rule
                        if (rules.size < 10) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .background(contentColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    BasicTextField(
                                        value = newRule,
                                        onValueChange = { newRule = it },
                                        textStyle = TextStyle(color = contentColor, fontSize = 13.sp),
                                        cursorBrush = SolidColor(contentColor),
                                        singleLine = true,
                                        decorationBox = { innerTextField ->
                                            Box {
                                                if (newRule.isEmpty()) {
                                                    BasicText(
                                                        "Add a rule...",
                                                        style = TextStyle(
                                                            color = contentColor.copy(alpha = 0.4f),
                                                            fontSize = 13.sp
                                                        )
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                                }
                                
                                Spacer(Modifier.width(8.dp))
                                
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .background(
                                            if (newRule.isNotBlank()) accentColor else contentColor.copy(alpha = 0.1f),
                                            CircleShape
                                        )
                                        .clip(CircleShape)
                                        .clickable(enabled = newRule.isNotBlank()) {
                                            rules.add(newRule.trim())
                                            newRule = ""
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = if (newRule.isNotBlank()) Color.White else contentColor.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Create button
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (canCreate) accentColor else accentColor.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = canCreate) {
                            onCreate(
                                name,
                                description.ifBlank { null },
                                selectedPrivacy,
                                category.ifBlank { null },
                                rules.toList()
                            )
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        BasicText(
                            "Create Group",
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

// ==================== Helper Components ====================

@Composable
private fun InputField(
    contentColor: Color,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    error: String? = null,
    helper: String? = null
) {
    Column {
        BasicText(
            label,
            style = TextStyle(
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
        
        Spacer(Modifier.height(6.dp))
        
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    if (error != null) Color.Red.copy(alpha = 0.1f) else contentColor.copy(alpha = 0.05f),
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = contentColor, fontSize = 15.sp),
                cursorBrush = SolidColor(contentColor),
                singleLine = singleLine,
                maxLines = maxLines,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            BasicText(
                                placeholder,
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
        
        if (error != null || helper != null) {
            Spacer(Modifier.height(4.dp))
            BasicText(
                error ?: helper ?: "",
                style = TextStyle(
                    color = if (error != null) Color.Red else contentColor.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun PrivacyOption(
    contentColor: Color,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) accentColor.copy(alpha = 0.1f) else contentColor.copy(alpha = 0.03f),
                RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(
                    if (isSelected) accentColor.copy(alpha = 0.2f) else contentColor.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else contentColor.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(Modifier.weight(1f)) {
            BasicText(
                title,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            BasicText(
                description,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            )
        }
        
        // Selection indicator
        Box(
            Modifier
                .size(20.dp)
                .background(
                    if (isSelected) accentColor else Color.Transparent,
                    CircleShape
                )
                .then(
                    if (!isSelected) Modifier.background(
                        Color.Transparent,
                        CircleShape,
                    ) else Modifier
                )
        ) {
            if (!isSelected) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .background(Color.Transparent, CircleShape)
                        .then(
                            Modifier.background(
                                Color.Transparent,
                                CircleShape
                            )
                        )
                )
            }
        }
    }
}
