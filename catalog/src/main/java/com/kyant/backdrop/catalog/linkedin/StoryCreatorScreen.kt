package com.kyant.backdrop.catalog.linkedin

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.catalog.network.models.StoryCategory
import com.kyant.backdrop.catalog.network.models.StoryVisibility
import java.io.ByteArrayOutputStream

@Composable
fun StoryCreatorDialog(
    onDismiss: () -> Unit,
    onCreateStory: (
        mediaType: String,
        mediaBytes: Pair<ByteArray, String>?,
        textContent: String?,
        backgroundColor: String?,
        category: String,
        visibility: String,
        linkUrl: String?,
        linkTitle: String?
    ) -> Unit,
    isCreating: Boolean = false
) {
    Dialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isCreating,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        StoryCreator(
            onDismiss = onDismiss,
            onCreateStory = onCreateStory,
            isCreating = isCreating
        )
    }
}

@Composable
private fun StoryCreator(
    onDismiss: () -> Unit,
    onCreateStory: (
        mediaType: String,
        mediaBytes: Pair<ByteArray, String>?,
        textContent: String?,
        backgroundColor: String?,
        category: String,
        visibility: String,
        linkUrl: String?,
        linkTitle: String?
    ) -> Unit,
    isCreating: Boolean
) {
    val context = LocalContext.current
    
    // State
    var storyType by remember { mutableStateOf("TEXT") } // TEXT, IMAGE, VIDEO
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var textContent by remember { mutableStateOf("") }
    var selectedBackgroundColor by remember { mutableStateOf("#1a1a2e") }
    var selectedCategory by remember { mutableStateOf("GENERAL") }
    var selectedVisibility by remember { mutableStateOf("PUBLIC") }
    var linkUrl by remember { mutableStateOf("") }
    var linkTitle by remember { mutableStateOf("") }
    var showLinkInput by remember { mutableStateOf(false) }
    
    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            selectedVideoUri = null
            storyType = "IMAGE"
        }
    }
    
    // Video picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedVideoUri = it
            selectedImageUri = null
            storyType = "VIDEO"
        }
    }
    
    // Background colors for text stories
    val backgroundColors = listOf(
        "#1a1a2e", "#16213e", "#0f3460", "#e94560",
        "#533483", "#2c061f", "#374045", "#ff6b6b",
        "#4ecdc4", "#45b7d1", "#96ceb4", "#ffeaa7"
    )
    
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 24.dp)
        ) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { if (!isCreating) onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("✕", style = TextStyle(Color.White, 18.sp, FontWeight.Bold))
                }
                
                BasicText(
                    "Create Story",
                    style = TextStyle(Color.White, 18.sp, FontWeight.SemiBold)
                )
                
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isCreating) Color.Gray else Color(0xFF0077B5))
                        .clickable(enabled = !isCreating) {
                            // Create story
                            val mediaBytes: Pair<ByteArray, String>? = when (storyType) {
                                "IMAGE" -> selectedImageUri?.let { uri ->
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        val bytes = inputStream?.readBytes()
                                        inputStream?.close()
                                        bytes?.let { Pair(it, "image/jpeg") }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                "VIDEO" -> selectedVideoUri?.let { uri ->
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        val bytes = inputStream?.readBytes()
                                        inputStream?.close()
                                        bytes?.let { Pair(it, "video/mp4") }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                else -> null
                            }
                            
                            onCreateStory(
                                storyType,
                                mediaBytes,
                                if (storyType == "TEXT" || textContent.isNotBlank()) textContent else null,
                                if (storyType == "TEXT") selectedBackgroundColor else null,
                                selectedCategory,
                                selectedVisibility,
                                linkUrl.takeIf { it.isNotBlank() },
                                linkTitle.takeIf { it.isNotBlank() }
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        if (isCreating) "Posting..." else "Share",
                        style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Story type tabs
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("TEXT" to "Text", "IMAGE" to "Photo", "VIDEO" to "Video").forEach { (type, label) ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (storyType == type) Color.White.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                storyType = type
                                if (type == "IMAGE") {
                                    imagePickerLauncher.launch("image/*")
                                } else if (type == "VIDEO") {
                                    videoPickerLauncher.launch("video/*")
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            label,
                            style = TextStyle(Color.White, 14.sp)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Preview area
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (storyType) {
                    "TEXT" -> {
                        // Text story preview
                        val bgColor = try {
                            Color(android.graphics.Color.parseColor(selectedBackgroundColor))
                        } catch (e: Exception) {
                            Color(0xFF1a1a2e)
                        }
                        
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = textContent,
                                onValueChange = { textContent = it },
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (textContent.isEmpty()) {
                                            BasicText(
                                                "Type your story...",
                                                style = TextStyle(
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                    "IMAGE" -> {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(selectedImageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                            )
                        } else {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.DarkGray)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    BasicText("📷", style = TextStyle(fontSize = 48.sp))
                                    Spacer(Modifier.height(8.dp))
                                    BasicText(
                                        "Tap to select a photo",
                                        style = TextStyle(Color.White.copy(alpha = 0.7f), 14.sp)
                                    )
                                }
                            }
                        }
                    }
                    "VIDEO" -> {
                        if (selectedVideoUri != null) {
                            // Show video thumbnail or placeholder
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.DarkGray)
                                    .clickable { videoPickerLauncher.launch("video/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    BasicText("🎬", style = TextStyle(fontSize = 48.sp))
                                    Spacer(Modifier.height(8.dp))
                                    BasicText(
                                        "Video selected",
                                        style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                                    )
                                    BasicText(
                                        "Tap to change",
                                        style = TextStyle(Color.White.copy(alpha = 0.5f), 12.sp)
                                    )
                                }
                            }
                        } else {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.DarkGray)
                                    .clickable { videoPickerLauncher.launch("video/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    BasicText("🎥", style = TextStyle(fontSize = 48.sp))
                                    Spacer(Modifier.height(8.dp))
                                    BasicText(
                                        "Tap to select a video",
                                        style = TextStyle(Color.White.copy(alpha = 0.7f), 14.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Background color picker (for text stories)
            if (storyType == "TEXT") {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    BasicText(
                        "Background",
                        style = TextStyle(Color.White.copy(alpha = 0.7f), 12.sp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        backgroundColors.forEach { colorHex ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(colorHex))
                            } catch (e: Exception) {
                                Color.Gray
                            }
                            
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        2.dp,
                                        if (selectedBackgroundColor == colorHex) Color.White
                                        else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { selectedBackgroundColor = colorHex }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
            }
            
            // Category picker
            Column(Modifier.padding(horizontal = 16.dp)) {
                BasicText(
                    "Category",
                    style = TextStyle(Color.White.copy(alpha = 0.7f), 12.sp)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StoryCategory.ALL_CATEGORIES.forEach { category ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selectedCategory == category.id) Color(0xFF0077B5)
                                    else Color.White.copy(alpha = 0.1f)
                                )
                                .clickable { selectedCategory = category.id }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            BasicText(
                                category.label,
                                style = TextStyle(Color.White, 12.sp)
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Visibility picker
            Column(Modifier.padding(horizontal = 16.dp)) {
                BasicText(
                    "Who can see this",
                    style = TextStyle(Color.White.copy(alpha = 0.7f), 12.sp)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StoryVisibility.ALL_OPTIONS.forEach { visibility ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selectedVisibility == visibility.id) Color(0xFF0077B5)
                                    else Color.White.copy(alpha = 0.1f)
                                )
                                .clickable { selectedVisibility = visibility.id }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            BasicText(
                                visibility.label,
                                style = TextStyle(Color.White, 12.sp)
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Link input toggle
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { showLinkInput = !showLinkInput }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Add a link",
                        style = TextStyle(Color.White, 14.sp)
                    )
                    BasicText(
                        if (showLinkInput) "▼" else "▶",
                        style = TextStyle(Color.White.copy(alpha = 0.5f), 12.sp)
                    )
                }
                
                if (showLinkInput) {
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        textStyle = TextStyle(Color.White, 14.sp),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(12.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (linkUrl.isEmpty()) {
                                    BasicText(
                                        "https://...",
                                        style = TextStyle(Color.White.copy(alpha = 0.5f), 14.sp)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = linkTitle,
                        onValueChange = { linkTitle = it },
                        textStyle = TextStyle(Color.White, 14.sp),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(12.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (linkTitle.isEmpty()) {
                                    BasicText(
                                        "Link title (optional)",
                                        style = TextStyle(Color.White.copy(alpha = 0.5f), 14.sp)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
    }
}
