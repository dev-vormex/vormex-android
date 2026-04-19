package com.kyant.backdrop.catalog.linkedin.groups

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.chat.readChatAttachment
import kotlinx.coroutines.launch

@Composable
fun GroupAppearanceEditorScreen(
    groupId: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: GroupsViewModel = viewModel(factory = GroupsViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var uploadTarget by remember { mutableStateOf<GroupAppearanceTarget?>(null) }

    LaunchedEffect(groupId) {
        if (uiState.selectedGroup?.id != groupId) {
            viewModel.loadGroupDetail(groupId)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val target = uploadTarget ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val attachment = context.readChatAttachment(
                    uri = uri,
                    fallbackFileName = if (target == GroupAppearanceTarget.ICON) "group-icon.jpg" else "group-cover.jpg",
                    fallbackMimeType = "image/jpeg"
                )
                if (!attachment.mimeType.startsWith("image/")) {
                    Toast.makeText(context, "Please choose an image", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                when (target) {
                    GroupAppearanceTarget.ICON -> viewModel.uploadGroupIcon(
                        groupId = uiState.selectedGroup?.id ?: groupId,
                        uri = attachment.uri,
                        fileName = attachment.fileName,
                        mimeType = attachment.mimeType
                    )

                    GroupAppearanceTarget.COVER -> viewModel.uploadGroupCover(
                        groupId = uiState.selectedGroup?.id ?: groupId,
                        uri = attachment.uri,
                        fileName = attachment.fileName,
                        mimeType = attachment.mimeType
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    e.message ?: "Could not open this image",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                uploadTarget = null
            }
        }
    }

    val group = uiState.selectedGroup
    val canEdit = group?.memberRole in listOf("owner", "admin")

    Column(Modifier.fillMaxSize()) {
        EditorHeader(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            title = "Group Appearance",
            subtitle = group?.name ?: "Update icon and cover",
            onBackClick = onNavigateBack
        )

        when {
            uiState.isLoading && group == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            }

            group == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BasicText(
                        "Group not found",
                        style = TextStyle(contentColor, 14.sp)
                    )
                }
            }

            !canEdit -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EditorStatusBanner(
                        backgroundColor = Color(0xFFB3261E).copy(alpha = 0.18f),
                        contentColor = contentColor,
                        text = "Only group owners and admins can change the icon or cover."
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        EditorSectionCard(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            title = "Live Preview",
                            subtitle = "Updates appear instantly in group detail, lists, and chat."
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(accentColor.copy(alpha = 0.12f))
                            ) {
                                if (group.coverImage != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(group.coverImage)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (group.iconImage != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(group.iconImage)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Group,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        EditorSectionCard(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            title = "Assets",
                            subtitle = "Upload square-ish art for the icon and a wide image for the cover."
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                EditorActionTile(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    title = "Group icon",
                                    subtitle = group.iconImage?.let { "Current icon attached" } ?: "No icon uploaded yet",
                                    actionLabel = if (uiState.isUpdatingGroupAppearance) "Uploading" else "Change",
                                    onClick = {
                                        uploadTarget = GroupAppearanceTarget.ICON
                                        picker.launch("image/*")
                                    }
                                )
                                EditorActionTile(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    title = "Group cover",
                                    subtitle = group.coverImage?.let { "Current cover attached" } ?: "No cover uploaded yet",
                                    actionLabel = if (uiState.isUpdatingGroupAppearance) "Uploading" else "Change",
                                    onClick = {
                                        uploadTarget = GroupAppearanceTarget.COVER
                                        picker.launch("image/*")
                                    }
                                )
                            }
                        }
                    }

                    if (uiState.isUpdatingGroupAppearance) {
                        item {
                            EditorStatusBanner(
                                backgroundColor = accentColor.copy(alpha = 0.18f),
                                contentColor = contentColor,
                                text = "Uploading image and refreshing group surfaces..."
                            )
                        }
                    }

                    uiState.groupAppearanceError?.let { error ->
                        item {
                            EditorStatusBanner(
                                backgroundColor = Color(0xFFB3261E).copy(alpha = 0.18f),
                                contentColor = contentColor,
                                text = error
                            )
                        }
                    }

                    item {
                        EditorSectionCard(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            title = "Scope",
                            subtitle = "This mobile pass only covers the icon and cover. Member management and invites stay in the detail view for now."
                        ) {
                            BasicText(
                                "Tip: the same updated artwork will show up in the group detail header and the group chat header without restarting the app.",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.74f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class GroupAppearanceTarget {
    ICON,
    COVER
}
