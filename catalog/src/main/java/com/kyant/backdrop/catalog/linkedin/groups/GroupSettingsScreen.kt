package com.kyant.backdrop.catalog.linkedin.groups

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.chat.readChatAttachment
import com.kyant.backdrop.catalog.network.models.GroupInviteLinkResponse
import com.kyant.backdrop.catalog.network.models.GroupMember
import com.kyant.backdrop.catalog.network.models.GroupJoinRequest
import com.kyant.backdrop.catalog.network.models.MentionUser
import com.kyant.backdrop.catalog.ui.BasicText
import kotlinx.coroutines.launch

@Composable
fun GroupSettingsScreen(
    groupId: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit = {},
    onDeleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: GroupsViewModel = viewModel(factory = GroupsViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var uploadTarget by remember { mutableStateOf<GroupSettingsUploadTarget?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetInviteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        if (uiState.selectedGroup?.id != groupId) {
            viewModel.loadGroupDetail(groupId)
        }
        viewModel.loadGroupMembers(groupId)
        viewModel.loadGroupInviteLink(groupId)
        viewModel.loadGroupJoinRequests(groupId)
    }

    val group = uiState.selectedGroup?.takeIf { it.id == groupId }
    var name by rememberSaveable(group?.id) { mutableStateOf(group?.name.orEmpty()) }
    var description by rememberSaveable(group?.id) { mutableStateOf(group?.description.orEmpty()) }
    var category by rememberSaveable(group?.id) { mutableStateOf(group?.category.orEmpty()) }
    var privacy by rememberSaveable(group?.id) { mutableStateOf(group?.privacy ?: "PUBLIC") }
    var rulesText by rememberSaveable(group?.id) { mutableStateOf(group?.rules?.joinToString("\n").orEmpty()) }
    var inviteSearchQuery by rememberSaveable(group?.id) { mutableStateOf("") }
    var inviteMessage by rememberSaveable(group?.id) { mutableStateOf("") }

    val canManage = canManageGroup(group?.memberRole)
    val canDelete = isGroupOwner(group?.memberRole)
    val canSave = canManage && !uiState.isSavingGroupSettings && name.trim().length >= 3

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val target = uploadTarget ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val attachment = context.readChatAttachment(
                    uri = uri,
                    fallbackFileName = if (target == GroupSettingsUploadTarget.ICON) "group-icon.jpg" else "group-cover.jpg",
                    fallbackMimeType = "image/jpeg"
                )
                if (!attachment.mimeType.startsWith("image/")) {
                    Toast.makeText(context, "Please choose an image", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                when (target) {
                    GroupSettingsUploadTarget.ICON -> viewModel.uploadGroupIcon(
                        groupId = group?.id ?: groupId,
                        uri = attachment.uri,
                        fileName = attachment.fileName,
                        mimeType = attachment.mimeType
                    )

                    GroupSettingsUploadTarget.COVER -> viewModel.uploadGroupCover(
                        groupId = group?.id ?: groupId,
                        uri = attachment.uri,
                        fileName = attachment.fileName,
                        mimeType = attachment.mimeType
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Could not open this image", Toast.LENGTH_SHORT).show()
            } finally {
                uploadTarget = null
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        EditorHeader(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            title = "Group Settings",
            subtitle = group?.name ?: "Manage group",
            actionLabel = if (uiState.isSavingGroupSettings) "Saving" else "Save",
            actionEnabled = canSave,
            onBackClick = onNavigateBack,
            onActionClick = {
                viewModel.updateGroupSettings(
                    groupId = group?.id ?: groupId,
                    name = name.trim(),
                    description = description.ifBlank { null },
                    privacy = privacy,
                    category = category.ifBlank { null },
                    rules = rulesText
                        .lines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                )
            }
        )

        when {
            uiState.isLoading && group == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            }

            group == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BasicText("Group not found", style = TextStyle(contentColor, 14.sp))
                }
            }

            !canManage -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EditorStatusBanner(
                        backgroundColor = Color(0xFFB3261E).copy(alpha = 0.18f),
                        contentColor = contentColor,
                        text = "Only group owners and admins can manage this group."
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
                            title = "Details"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                EditorTextField(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    label = "Name",
                                    value = name,
                                    placeholder = "Group name",
                                    onValueChange = { if (it.length <= 80) name = it }
                                )
                                EditorTextField(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    label = "Description",
                                    value = description,
                                    placeholder = "What is this group about?",
                                    singleLine = false,
                                    onValueChange = { if (it.length <= 1_000) description = it }
                                )
                                EditorTextField(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    label = "Category",
                                    value = category,
                                    placeholder = "Technology, Sports, Gaming",
                                    onValueChange = { if (it.length <= 80) category = it }
                                )
                                EditorTextField(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    label = "Rules",
                                    value = rulesText,
                                    placeholder = "One rule per line",
                                    singleLine = false,
                                    onValueChange = { rulesText = it }
                                )
                                GroupPrivacyPicker(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    selectedPrivacy = privacy,
                                    onPrivacySelected = { privacy = it }
                                )
                            }
                        }
                    }

                    item {
                        EditorSectionCard(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            title = "Appearance"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                GroupPreview(
                                    groupName = name.ifBlank { group.name },
                                    iconImage = group.iconImage,
                                    coverImage = group.coverImage,
                                    contentColor = contentColor,
                                    accentColor = accentColor
                                )
                                EditorActionTile(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    title = "Group icon",
                                    subtitle = group.iconImage?.let { "Current icon attached" } ?: "No icon uploaded",
                                    actionLabel = if (uiState.isUpdatingGroupAppearance) "Uploading" else "Change",
                                    onClick = {
                                        uploadTarget = GroupSettingsUploadTarget.ICON
                                        picker.launch("image/*")
                                    }
                                )
                                EditorActionTile(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    title = "Group cover",
                                    subtitle = group.coverImage?.let { "Current cover attached" } ?: "No cover uploaded",
                                    actionLabel = if (uiState.isUpdatingGroupAppearance) "Uploading" else "Change",
                                    onClick = {
                                        uploadTarget = GroupSettingsUploadTarget.COVER
                                        picker.launch("image/*")
                                    }
                                )
                            }
                        }
                    }

                    item {
                        GroupInviteLinkSection(
                            backdrop = backdrop,
                            groupId = group.id,
                            groupName = group.name,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            inviteLink = uiState.currentInviteLink?.takeIf { it.group.id == group.id },
                            isLoading = uiState.isLoadingInviteLink,
                            onCopy = { inviteUrl -> copyGroupInviteLink(context, inviteUrl) },
                            onShare = { inviteUrl -> launchGroupInviteShareSheet(context, group.name, inviteUrl) },
                            onReset = { showResetInviteDialog = true },
                            onVisibilityChange = { visibility ->
                                viewModel.updateInviteLinkVisibility(group.id, visibility)
                            }
                        )
                    }

                    item {
                        InviteUsersSection(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            searchQuery = inviteSearchQuery,
                            message = inviteMessage,
                            users = uiState.inviteUserResults,
                            isSearching = uiState.isSearchingInviteUsers,
                            invitingUserIds = uiState.invitingUserIds,
                            onSearchChange = { query ->
                                inviteSearchQuery = query
                                viewModel.searchInviteUsers(query)
                            },
                            onMessageChange = { if (it.length <= 500) inviteMessage = it },
                            onInvite = { user ->
                                viewModel.createGroupInvite(
                                    groupId = group.id,
                                    userId = user.id,
                                    message = inviteMessage.ifBlank { null }
                                )
                            }
                        )
                    }

                    if (group.privacy != "PUBLIC") {
                        item {
                            JoinRequestsSection(
                                backdrop = backdrop,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                requests = uiState.groupJoinRequests,
                                respondingIds = uiState.respondingJoinRequestIds,
                                onApprove = { requestId ->
                                    viewModel.respondToGroupJoinRequest(group.id, requestId, "accept")
                                },
                                onDecline = { requestId ->
                                    viewModel.respondToGroupJoinRequest(group.id, requestId, "decline")
                                }
                            )
                        }
                    }

                    item {
                        EditorSectionCard(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            title = "Members",
                            subtitle = "${uiState.groupMembers.size} loaded"
                        ) {
                            if (uiState.groupMembers.isEmpty()) {
                                BasicText(
                                    "No members loaded yet.",
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.62f),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    items(uiState.groupMembers, key = { it.id }) { member ->
                        GroupMemberManageRow(
                            member = member,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isUpdating = member.userId in uiState.updatingMemberIds,
                            isRemoving = member.userId in uiState.removingMemberIds,
                            onRoleSelected = { role -> viewModel.updateMemberRole(group.id, member.userId, role) },
                            onRemove = { viewModel.removeMember(group.id, member.userId) }
                        )
                    }

                    if (uiState.isSavingGroupSettings || uiState.isUpdatingGroupAppearance) {
                        item {
                            EditorStatusBanner(
                                backgroundColor = accentColor.copy(alpha = 0.18f),
                                contentColor = contentColor,
                                text = if (uiState.isSavingGroupSettings) "Saving group settings..." else "Uploading image..."
                            )
                        }
                    }

                    uiState.groupSettingsError?.let { error ->
                        item {
                            EditorStatusBanner(
                                backgroundColor = Color(0xFFB3261E).copy(alpha = 0.18f),
                                contentColor = contentColor,
                                text = error
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

                    if (canDelete) {
                        item {
                            EditorSectionCard(
                                backdrop = backdrop,
                                contentColor = contentColor,
                                title = "Danger"
                            ) {
                                EditorActionTile(
                                    contentColor = contentColor,
                                    accentColor = Color(0xFFB3261E),
                                    title = "Delete group",
                                    subtitle = "This removes the group, its memberships, and group chat history.",
                                    actionLabel = if (group.id in uiState.deletingGroupIds) "Deleting" else "Delete",
                                    onClick = { showDeleteDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && group != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                BasicText(
                    "Delete group?",
                    style = TextStyle(color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                BasicText(
                    "This action cannot be undone.",
                    style = TextStyle(color = contentColor.copy(alpha = 0.72f), fontSize = 14.sp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteGroup(group.id, onDeleted = onDeleted)
                    }
                ) {
                    BasicText("Delete", style = TextStyle(color = Color(0xFFB3261E), fontWeight = FontWeight.SemiBold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    BasicText("Cancel", style = TextStyle(color = contentColor))
                }
            }
        )
    }

    if (showResetInviteDialog && group != null) {
        AlertDialog(
            onDismissRequest = { showResetInviteDialog = false },
            title = {
                BasicText(
                    "Reset invite link?",
                    style = TextStyle(color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                BasicText(
                    "The old group link and QR code will stop working.",
                    style = TextStyle(color = contentColor.copy(alpha = 0.72f), fontSize = 14.sp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetInviteDialog = false
                        viewModel.resetInviteLink(group.id)
                    }
                ) {
                    BasicText("Reset", style = TextStyle(color = Color(0xFFB3261E), fontWeight = FontWeight.SemiBold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetInviteDialog = false }) {
                    BasicText("Cancel", style = TextStyle(color = contentColor))
                }
            }
        )
    }
}

@Composable
private fun GroupInviteLinkSection(
    backdrop: LayerBackdrop,
    groupId: String,
    groupName: String,
    contentColor: Color,
    accentColor: Color,
    inviteLink: GroupInviteLinkResponse?,
    isLoading: Boolean,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onReset: () -> Unit,
    onVisibilityChange: (String) -> Unit
) {
    val inviteUrl = inviteLink?.inviteUrl ?: inviteLink?.inviteCode?.let(::groupInviteUrl).orEmpty()
    val inviteVisibility = normalizeGroupInviteVisibility(inviteLink?.visibility)
    val qrBitmap = remember(inviteUrl) {
        inviteUrl.takeIf { it.isNotBlank() }?.let { generateGroupInviteQrBitmap(it, 420) }
    }

    EditorSectionCard(
        backdrop = backdrop,
        contentColor = contentColor,
        title = "Invite link",
        subtitle = "Share $groupName with a link or QR code"
    ) {
        when {
            isLoading && inviteLink == null -> {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }

            inviteUrl.isBlank() -> {
                EditorStatusBanner(
                    backgroundColor = Color(0xFFB3261E).copy(alpha = 0.18f),
                    contentColor = contentColor,
                    text = "Invite link is not available yet."
                )
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(contentColor.copy(alpha = 0.07f))
                            .padding(12.dp)
                    ) {
                        BasicText(
                            inviteUrl,
                            style = TextStyle(color = contentColor.copy(alpha = 0.74f), fontSize = 12.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectablePill(
                            label = "Copy",
                            selected = false,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            enabled = !isLoading,
                            onClick = { onCopy(inviteUrl) }
                        )
                        SelectablePill(
                            label = "Share",
                            selected = false,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            enabled = !isLoading,
                            onClick = { onShare(inviteUrl) }
                        )
                        SelectablePill(
                            label = if (isLoading) "Resetting" else "Reset",
                            selected = false,
                            contentColor = contentColor,
                            accentColor = Color(0xFFB3261E),
                            enabled = !isLoading,
                            onClick = onReset
                        )
                    }

                    EditorToggleRow(
                        contentColor = contentColor,
                        accentColor = accentColor,
                        title = "Members can share link",
                        subtitle = "Turning this off resets old shared links.",
                        checked = inviteVisibility == "MEMBERS",
                        enabled = !isLoading,
                        onCheckedChange = { checked ->
                            onVisibilityChange(if (checked) "MEMBERS" else "ADMINS")
                        }
                    )

                    qrBitmap?.let { bitmap ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(contentColor.copy(alpha = 0.06f))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Invite QR code",
                                modifier = Modifier
                                    .size(176.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteUsersSection(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    searchQuery: String,
    message: String,
    users: List<MentionUser>,
    isSearching: Boolean,
    invitingUserIds: Set<String>,
    onSearchChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onInvite: (MentionUser) -> Unit
) {
    EditorSectionCard(
        backdrop = backdrop,
        contentColor = contentColor,
        title = "Invite people",
        subtitle = "Send a direct invite they can accept or decline"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EditorTextField(
                contentColor = contentColor,
                accentColor = accentColor,
                label = "Search",
                value = searchQuery,
                placeholder = "Search people by name or username",
                onValueChange = onSearchChange
            )
            EditorTextField(
                contentColor = contentColor,
                accentColor = accentColor,
                label = "Message",
                value = message,
                placeholder = "Optional invite message",
                singleLine = false,
                onValueChange = onMessageChange
            )

            when {
                searchQuery.trim().length < 2 -> {
                    BasicText(
                        "Type at least 2 characters to find people.",
                        style = TextStyle(color = contentColor.copy(alpha = 0.58f), fontSize = 12.sp)
                    )
                }

                isSearching -> {
                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }

                users.isEmpty() -> {
                    BasicText(
                        "No people found.",
                        style = TextStyle(color = contentColor.copy(alpha = 0.58f), fontSize = 12.sp)
                    )
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        users.forEach { user ->
                            InviteUserRow(
                                user = user,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                isInviting = user.id in invitingUserIds,
                                onInvite = { onInvite(user) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteUserRow(
    user: MentionUser,
    contentColor: Color,
    accentColor: Color,
    isInviting: Boolean,
    onInvite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                userInitials(user.name ?: user.username ?: "?"),
                style = TextStyle(color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                user.name ?: user.username ?: "Unknown",
                style = TextStyle(color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            user.username?.let {
                BasicText(
                    "@$it",
                    style = TextStyle(color = contentColor.copy(alpha = 0.58f), fontSize = 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        SelectablePill(
            label = if (isInviting) "Sending" else "Invite",
            selected = true,
            contentColor = contentColor,
            accentColor = accentColor,
            enabled = !isInviting,
            onClick = onInvite
        )
    }
}

@Composable
private fun JoinRequestsSection(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    requests: List<GroupJoinRequest>,
    respondingIds: Set<String>,
    onApprove: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    EditorSectionCard(
        backdrop = backdrop,
        contentColor = contentColor,
        title = "Join requests",
        subtitle = "${requests.size} pending"
    ) {
        if (requests.isEmpty()) {
            BasicText(
                "No pending requests.",
                style = TextStyle(color = contentColor.copy(alpha = 0.58f), fontSize = 12.sp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                requests.forEach { request ->
                    JoinRequestRow(
                        request = request,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isResponding = request.id in respondingIds,
                        onApprove = { onApprove(request.id) },
                        onDecline = { onDecline(request.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun JoinRequestRow(
    request: GroupJoinRequest,
    contentColor: Color,
    accentColor: Color,
    isResponding: Boolean,
    onApprove: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                userInitials(request.requester.name ?: request.requester.username ?: "?"),
                style = TextStyle(color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                request.requester.name ?: request.requester.username ?: "Unknown",
                style = TextStyle(color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                "Requested from invite link",
                style = TextStyle(color = contentColor.copy(alpha = 0.58f), fontSize = 11.sp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SelectablePill(
                label = if (isResponding) "..." else "Approve",
                selected = true,
                contentColor = contentColor,
                accentColor = accentColor,
                enabled = !isResponding,
                onClick = onApprove
            )
            SelectablePill(
                label = "Decline",
                selected = false,
                contentColor = contentColor,
                accentColor = Color(0xFFB3261E),
                enabled = !isResponding,
                onClick = onDecline
            )
        }
    }
}

private fun userInitials(value: String): String {
    return value
        .split(' ', '.', '_', '-')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "?" }
}

@Composable
private fun GroupPrivacyPicker(
    contentColor: Color,
    accentColor: Color,
    selectedPrivacy: String,
    onPrivacySelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            "Privacy",
            style = TextStyle(
                color = contentColor.copy(alpha = 0.72f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("PUBLIC", "PRIVATE", "SECRET").forEach { privacy ->
                SelectablePill(
                    label = privacy.lowercase().replaceFirstChar { it.uppercase() },
                    selected = selectedPrivacy == privacy,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { onPrivacySelected(privacy) }
                )
            }
        }
    }
}

@Composable
private fun GroupPreview(
    groupName: String,
    iconImage: String?,
    coverImage: String?,
    contentColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(accentColor.copy(alpha = 0.12f))
    ) {
        if (coverImage != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(coverImage).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.32f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                if (iconImage != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(iconImage).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.size(10.dp))
            BasicText(
                groupName,
                style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GroupMemberManageRow(
    member: GroupMember,
    contentColor: Color,
    accentColor: Color,
    isUpdating: Boolean,
    isRemoving: Boolean,
    onRoleSelected: (String) -> Unit,
    onRemove: () -> Unit
) {
    val normalizedRole = normalizeGroupRole(member.role) ?: "member"
    val isOwner = normalizedRole == "owner"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BasicText(
                    member.user.name ?: member.user.username ?: "Unknown",
                    style = TextStyle(color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    groupRoleDisplayName(member.role),
                    style = TextStyle(color = contentColor.copy(alpha = 0.58f), fontSize = 12.sp)
                )
            }
            if (isUpdating || isRemoving) {
                CircularProgressIndicator(
                    color = accentColor,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        if (!isOwner) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "MEMBER" to "Member",
                    "MODERATOR" to "Mod",
                    "ADMIN" to "Admin"
                ).forEach { (role, label) ->
                    SelectablePill(
                        label = label,
                        selected = normalizedRole == role.lowercase(),
                        contentColor = contentColor,
                        accentColor = accentColor,
                        enabled = !isUpdating && !isRemoving,
                        onClick = { onRoleSelected(role) }
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFB3261E).copy(alpha = 0.14f))
                    .clickable(enabled = !isUpdating && !isRemoving, onClick = onRemove)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                BasicText(
                    if (isRemoving) "Removing" else "Remove member",
                    style = TextStyle(
                        color = Color(0xFFB3261E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun SelectablePill(
    label: String,
    selected: Boolean,
    contentColor: Color,
    accentColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) accentColor else contentColor.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = if (selected) Color.White else contentColor.copy(alpha = if (enabled) 0.78f else 0.38f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

private enum class GroupSettingsUploadTarget {
    ICON,
    COVER
}
