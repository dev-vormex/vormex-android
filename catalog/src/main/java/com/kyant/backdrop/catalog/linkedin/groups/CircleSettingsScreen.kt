package com.kyant.backdrop.catalog.linkedin.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import com.kyant.backdrop.catalog.ui.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.LayerBackdrop

@Composable
fun CircleSettingsScreen(
    circleId: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: CirclesViewModel = viewModel(factory = CirclesViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var maxMembersInput by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var requiresApproval by remember { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(circleId) {
        if (uiState.selectedCircle?.id != circleId) {
            viewModel.loadCircleDetail(circleId)
        }
    }

    val circle = uiState.selectedCircle
    val canEdit = circle?.myRole in listOf("creator", "owner")

    LaunchedEffect(circle?.id) {
        circle?.let {
            name = it.name
            description = it.description.orEmpty()
            emoji = it.emoji.orEmpty()
            category = it.category.orEmpty()
            tagsInput = it.tags.joinToString(", ")
            maxMembersInput = it.maxMembers.toString()
            isPrivate = it.isPrivate
            requiresApproval = it.requiresApproval
            localValidationError = null
            viewModel.clearCircleSettingsError()
        }
    }

    Column(Modifier.fillMaxSize()) {
        EditorHeader(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            title = "Circle Settings",
            subtitle = circle?.name ?: "Owner controls",
            actionLabel = if (uiState.isSavingCircleSettings) "Saving" else "Save",
            actionEnabled = !uiState.isSavingCircleSettings && canEdit,
            onBackClick = onNavigateBack,
            onActionClick = {
                val parsedMaxMembers = maxMembersInput.trim().toIntOrNull()
                if (parsedMaxMembers == null) {
                    localValidationError = "Max members must be a valid number"
                    return@EditorHeader
                }
                localValidationError = null
                viewModel.updateCircle(
                    circleId = circle?.id ?: circleId,
                    name = name,
                    description = description,
                    emoji = emoji,
                    category = category,
                    tags = tagsInput
                        .split(',')
                        .map { it.trim() }
                        .filter { it.isNotBlank() },
                    isPrivate = isPrivate,
                    requiresApproval = requiresApproval,
                    maxMembers = parsedMaxMembers
                )
            }
        )

        when {
            uiState.isLoading && circle == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            }

            circle == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BasicText(
                        "Circle not found",
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
                        text = "Only the circle creator or owner can change these settings."
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
                            title = "Basics",
                            subtitle = "Update the public-facing metadata for this circle."
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                EditorTextField(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    label = "Name",
                                    value = name,
                                    placeholder = "Circle name",
                                    onValueChange = { name = it }
                                )
                                EditorTextField(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    label = "Description",
                                    value = description,
                                    placeholder = "What is this circle about?",
                                    singleLine = false,
                                    onValueChange = { description = it }
                                )
                                EditorTextField(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    label = "Emoji",
                                    value = emoji,
                                    placeholder = "e.g. 🚀",
                                    onValueChange = { emoji = it.take(4) }
                                )
                                EditorTextField(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    label = "Category",
                                    value = category,
                                    placeholder = "e.g. Coding, Mobile, Career",
                                    onValueChange = { category = it }
                                )
                                EditorTextField(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    label = "Tags",
                                    value = tagsInput,
                                    placeholder = "Comma-separated tags",
                                    singleLine = false,
                                    onValueChange = { tagsInput = it }
                                )
                            }
                        }
                    }

                    item {
                        EditorSectionCard(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            title = "Access Controls",
                            subtitle = "Private circles can require approval before members join."
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                EditorToggleRow(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    title = "Private circle",
                                    subtitle = "Hide this circle from public browsing and keep membership tighter.",
                                    checked = isPrivate,
                                    onCheckedChange = {
                                        isPrivate = it
                                        if (!it) {
                                            requiresApproval = false
                                        }
                                    }
                                )
                                EditorToggleRow(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    title = "Require approval",
                                    subtitle = if (isPrivate) {
                                        "Review join requests before new members enter."
                                    } else {
                                        "Only available for private circles."
                                    },
                                    checked = requiresApproval,
                                    enabled = isPrivate,
                                    onCheckedChange = { requiresApproval = it }
                                )
                                EditorTextField(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    label = "Max members",
                                    value = maxMembersInput,
                                    placeholder = "500",
                                    onValueChange = { maxMembersInput = it.filter { ch -> ch.isDigit() } }
                                )
                                BasicText(
                                    "Current members: ${circle.memberCount}",
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    item {
                        EditorSectionCard(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            title = "Read-only on Mobile",
                            subtitle = "These are intentionally out of scope for this pass."
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BasicText(
                                    "Campus: ${circle.campus ?: "Not set"}",
                                    style = TextStyle(contentColor.copy(alpha = 0.72f), 12.sp)
                                )
                                BasicText(
                                    "Image and cover uploads stay read-only here for now.",
                                    style = TextStyle(contentColor.copy(alpha = 0.72f), 12.sp)
                                )
                                BasicText(
                                    "Invite flows and member management remain unchanged in v1.",
                                    style = TextStyle(contentColor.copy(alpha = 0.72f), 12.sp)
                                )
                            }
                        }
                    }

                    if (uiState.isSavingCircleSettings) {
                        item {
                            EditorStatusBanner(
                                backgroundColor = accentColor.copy(alpha = 0.18f),
                                contentColor = contentColor,
                                text = "Saving circle settings and refreshing your lists..."
                            )
                        }
                    }

                    localValidationError?.let { error ->
                        item {
                            EditorStatusBanner(
                                backgroundColor = Color(0xFFB3261E).copy(alpha = 0.18f),
                                contentColor = contentColor,
                                text = error
                            )
                        }
                    }

                    uiState.circleSettingsError?.let { error ->
                        item {
                            EditorStatusBanner(
                                backgroundColor = Color(0xFFB3261E).copy(alpha = 0.18f),
                                contentColor = contentColor,
                                text = error
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
