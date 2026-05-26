package com.kyant.backdrop.catalog.linkedin

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.ai.VormexAiChipAction
import com.kyant.backdrop.catalog.ai.VormexAiChipRow
import com.kyant.backdrop.catalog.ai.VormexAiGateway
import com.kyant.backdrop.catalog.ai.VormexAiRewriteStyle
import com.kyant.backdrop.catalog.ai.VormexAiStatusCard
import com.kyant.backdrop.catalog.ai.VormexAiSurface
import com.kyant.backdrop.catalog.ai.VormexAiTextResult
import com.kyant.backdrop.catalog.components.LiquidToggle
import com.kyant.backdrop.catalog.network.models.AgentGoal
import com.kyant.backdrop.catalog.network.models.AgentPendingAction
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSheetContent(
    viewModel: AgentViewModel,
    surface: String,
    surfaceContext: Map<String, String>,
    userDisplayName: String? = null,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    reduceAnimations: Boolean = false,
    isDarkTheme: Boolean = contentColor == Color.White,
    isPremiumUser: Boolean? = null,
    headerTitle: String = "vormex",
    sendButtonLabel: String = "Send",
    placeholderText: String = "Ask vormex what you want done in the app…",
    enableVoiceControls: Boolean = true,
    enableInlineNavigationActions: Boolean = true,
    onOpenInlineProfile: (String) -> Unit = {},
    onOpenInlineChat: (String) -> Unit = {},
    onSeeMoreInlineResults: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    isFullScreen: Boolean = false
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val displaySurface = uiState.sessionState?.currentSurface?.takeIf { it.isNotBlank() } ?: surface
    val powerModeEligible = isPremiumUser == true || uiState.powerModeEligible || uiState.isPremium
    var draft by rememberSaveable { mutableStateOf("") }
    var showGoalDialog by remember { mutableStateOf(false) }
    var selectedApproval by remember { mutableStateOf<AgentPendingAction?>(null) }
    val aiGateway = remember { VormexAiGateway(context.applicationContext) }
    val aiScope = rememberCoroutineScope()
    var aiStatus by remember { mutableStateOf<String?>(null) }
    var aiBusyLabel by remember { mutableStateOf<String?>(null) }
    val openingVoiceGreeting = remember(userDisplayName) {
        buildOpeningVoiceGreetingPrompt(userDisplayName)
    }

    fun runDraftAi(label: String, block: suspend () -> VormexAiTextResult) {
        aiScope.launch {
            aiBusyLabel = "$label…"
            aiStatus = null
            when (val result = block()) {
                is VormexAiTextResult.Success -> {
                    draft = result.text
                    aiStatus = when (result.source) {
                        com.kyant.backdrop.catalog.ai.VormexAiSource.LOCAL -> "$label updated on-device."
                        com.kyant.backdrop.catalog.ai.VormexAiSource.CLOUD -> "$label updated with cloud AI."
                    }
                }
                is VormexAiTextResult.NeedsDownload -> aiStatus = result.message
                is VormexAiTextResult.Blocked -> aiStatus = result.message
                is VormexAiTextResult.Failure -> aiStatus = result.message
            }
            aiBusyLabel = null
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startRealtimeVoice(
                surface = surface,
                surfaceContext = surfaceContext,
                openingGreeting = openingVoiceGreeting
            )
        }
    }

    LaunchedEffect(surface) {
        viewModel.ensureSession(surface = surface)
    }

    LaunchedEffect(surface, surfaceContext) {
        viewModel.syncSurface(surface = surface, surfaceContext = surfaceContext)
    }

    if (!enableVoiceControls) {
        HumanizedAgentTextSheet(
            uiState = uiState,
            userDisplayName = userDisplayName,
            draft = draft,
            onDraftChange = { draft = it },
            headerTitle = headerTitle,
            placeholderText = placeholderText,
            surface = surface,
            surfaceContext = surfaceContext,
            viewModel = viewModel,
            powerModeEligible = powerModeEligible,
            enableInlineNavigationActions = enableInlineNavigationActions,
            onOpenInlineProfile = onOpenInlineProfile,
            onOpenInlineChat = onOpenInlineChat,
            onSeeMoreInlineResults = onSeeMoreInlineResults,
            onSelectApproval = {},
            onDismiss = onDismiss,
            isFullScreen = isFullScreen,
            accentColor = accentColor
        )
    } else {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFullScreen) Modifier.fillMaxHeight() else Modifier.heightIn(max = 720.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    BasicText(
                        text = headerTitle,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AgentDisplayFontFamily
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        text = if (isFullScreen) "Back" else "Close",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = AgentBodyFontFamily
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetaChip(
                    text = humanizeSurface(displaySurface),
                    contentColor = contentColor,
                    background = contentColor.copy(alpha = 0.08f)
                )
                MetaChip(
                    text = if (uiState.socketConnected) "Live sync on" else "Live sync off",
                    contentColor = contentColor,
                    background = accentColor.copy(alpha = if (uiState.socketConnected) 0.14f else 0.08f)
                )
                if (uiState.pendingApprovals.isNotEmpty()) {
                    MetaChip(
                        text = "${uiState.pendingApprovals.size} approvals",
                        contentColor = contentColor,
                        background = Color(0xFFFFE3E3)
                    )
                }
                if (uiState.goals.isNotEmpty()) {
                    MetaChip(
                        text = "${uiState.goals.size} goals",
                        contentColor = contentColor,
                        background = contentColor.copy(alpha = 0.08f)
                    )
                }
                ActionChip(
                    text = if (uiState.isSavingGoal) "Saving…" else "Add Goal",
                    contentColor = contentColor,
                    background = accentColor.copy(alpha = 0.14f),
                    onClick = { showGoalDialog = true }
                )
            }

            if (!uiState.liveStatus.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    BasicText(
                        text = uiState.liveStatus ?: "",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = AgentBodyFontFamily
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(contentColor.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            BasicText(
                                text = if (uiState.autoRunEnabled && powerModeEligible) "Power mode" else "Approval mode",
                                style = TextStyle(
                                    color = contentColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = AgentBodyFontFamily
                                )
                            )
                            BasicText(
                                text = if (uiState.autoRunEnabled && powerModeEligible) {
                                    "Low-risk actions can run automatically."
                                } else if (!powerModeEligible) {
                                    "Power mode is locked to Premium."
                                } else {
                                    "Actions wait for your approval."
                                },
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.62f),
                                    fontSize = 12.sp,
                                    fontFamily = AgentBodyFontFamily
                                )
                            )
                        }
                        LiquidToggle(
                            selected = { uiState.autoRunEnabled && powerModeEligible },
                            onSelect = { selected ->
                                viewModel.setPowerModeEnabled(selected, powerModeEligible)
                            },
                            backdrop = backdrop
                        )
                    }
                }
            }

            if (uiState.goals.isNotEmpty()) {
                BasicText(
                    text = "Goals",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AgentBodyFontFamily
                    )
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.goals.forEach { goal ->
                        GoalChip(
                            goal = goal,
                            contentColor = contentColor,
                            onDelete = { viewModel.deleteGoal(goal.id) }
                        )
                    }
                }
            }

            if (uiState.pendingApprovals.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicText(
                        text = "Pending Approvals",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AgentBodyFontFamily
                        )
                    )
                    uiState.pendingApprovals.take(3).forEach { action ->
                        PendingApprovalCard(
                            action = action,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onOpen = { selectedApproval = action }
                        )
                    }
                }
            }

            if (uiState.lastExecutedActions.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.lastExecutedActions.takeLast(3).forEach { action ->
                        MetaChip(
                            text = action.title,
                            contentColor = contentColor,
                            background = accentColor.copy(alpha = 0.14f)
                        )
                    }
                }
            }

            if (!uiState.error.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFFFE3E3))
                        .padding(12.dp)
                ) {
                    BasicText(
                        text = uiState.error ?: "",
                        style = TextStyle(
                            color = Color(0xFF8A1C1C),
                            fontSize = 13.sp,
                            fontFamily = AgentBodyFontFamily
                        )
                    )
                }
            }

            uiState.activeInlineResults?.let { inlineResults ->
                AgentInlineResultsSurface(
                    panel = inlineResults,
                    dismissedIds = uiState.dismissedInlineResultIds,
                    actionInProgress = uiState.inlineResultActionInProgress,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isDarkTheme = isDarkTheme,
                    enableNavigationActions = enableInlineNavigationActions,
                    onViewProfile = {
                        viewModel.dismissInlineResults()
                        onOpenInlineProfile(it)
                    },
                    onMessage = {
                        viewModel.dismissInlineResults()
                        onOpenInlineChat(it)
                    },
                    onConnect = viewModel::sendInlineConnectionRequest,
                    onCloseItem = viewModel::dismissInlineResultItem,
                    onClosePanel = viewModel::dismissInlineResults,
                    onSeeMore = onSeeMoreInlineResults
                )
            }

            if (uiState.messages.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = uiState.messages) { message ->
                        AgentMessageBubble(
                            message = message,
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }
                }
            } else {
                Spacer(Modifier.weight(1f, fill = true))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VormexAiChipRow(
                    actions = listOf(
                        VormexAiChipAction(
                            label = "Clarify",
                            enabled = draft.isNotBlank(),
                            onClick = {
                                runDraftAi("Clarify draft") {
                                    aiGateway.rewrite(
                                        text = draft,
                                        style = VormexAiRewriteStyle.CLEARER,
                                        surface = VormexAiSurface.AGENT,
                                        allowCloudFallback = true
                                    )
                                }
                            }
                        ),
                        VormexAiChipAction(
                            label = "Summarize",
                            enabled = draft.isNotBlank(),
                            onClick = {
                                runDraftAi("Summarize draft") {
                                    aiGateway.summarize(
                                        text = draft,
                                        surface = VormexAiSurface.AGENT,
                                        allowCloudFallback = true,
                                        asBullets = false
                                    )
                                }
                            }
                        ),
                        VormexAiChipAction(
                            label = "Professional",
                            enabled = draft.isNotBlank(),
                            onClick = {
                                runDraftAi("Professional rewrite") {
                                    aiGateway.rewrite(
                                        text = draft,
                                        style = VormexAiRewriteStyle.PROFESSIONAL,
                                        surface = VormexAiSurface.AGENT,
                                        allowCloudFallback = true
                                    )
                                }
                            }
                        )
                    ),
                    contentColor = contentColor,
                    accentColor = accentColor
                )

                aiBusyLabel?.let { busy ->
                    VormexAiStatusCard(
                        message = busy,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                }

                aiStatus?.let { status ->
                    VormexAiStatusCard(
                        message = status,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(contentColor.copy(alpha = 0.07f))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        textStyle = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontFamily = AgentBodyFontFamily
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (draft.isBlank()) {
                                BasicText(
                                    text = placeholderText,
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.45f),
                                        fontSize = 14.sp,
                                        fontFamily = AgentBodyFontFamily
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (enableVoiceControls) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.isRecordingVoice || uiState.isVoiceSessionConnecting) Color(0xFFFF6B6B)
                                    else accentColor.copy(alpha = 0.16f)
                                )
                                .clickable(enabled = !uiState.isSending) {
                                    if (uiState.isRecordingVoice || uiState.isVoiceSessionConnecting) {
                                        viewModel.stopRealtimeVoice()
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (granted) {
                                            viewModel.startRealtimeVoice(
                                                surface = surface,
                                                surfaceContext = surfaceContext,
                                                openingGreeting = openingVoiceGreeting
                                            )
                                        } else {
                                            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isVoiceSessionConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = com.kyant.backdrop.catalog.R.drawable.ic_mic),
                                    contentDescription = "Mic",
                                    tint = if (uiState.isRecordingVoice) Color.White else contentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(2.dp))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(accentColor)
                            .clickable(
                                enabled = !uiState.isSending &&
                                    !(enableVoiceControls && uiState.isVoiceSessionConnecting)
                            ) {
                                val message = draft
                                draft = ""
                                viewModel.sendMessage(
                                    message = message,
                                    surface = surface,
                                    surfaceContext = surfaceContext
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = when {
                                uiState.isSending -> "Thinking…"
                                enableVoiceControls && uiState.isVoiceSessionConnecting -> "Opening live voice…"
                                else -> sendButtonLabel
                            },
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = AgentBodyFontFamily
                            )
                        )
                    }
                }

                BasicText(
                    text = if (enableVoiceControls && (uiState.isRecordingVoice || uiState.isVoiceSessionConnecting)) {
                        "Live voice uses realtime Vormex data and can interrupt replies when you start speaking again."
                    } else if (uiState.autoRunEnabled && powerModeEligible) {
                        "Premium power mode can auto-run low-risk actions. DMs, destructive, illegal, or billing actions stay gated."
                    } else if (!powerModeEligible) {
                        "Approval mode is active. Premium unlocks power mode for low-risk actions."
                    } else {
                        "Approval mode is active. DMs, destructive, illegal, or billing actions stay gated."
                    },
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.62f),
                        fontSize = 11.sp,
                        fontFamily = AgentBodyFontFamily
                    )
                )
            }
    }
    }

    if (showGoalDialog) {
            GoalSettingDialog(
                contentColor = contentColor,
                accentColor = accentColor,
                onDismiss = { showGoalDialog = false },
                onSave = { goal, category ->
                    showGoalDialog = false
                    viewModel.createGoal(goal = goal, category = category)
                }
        )
    }

    if (enableVoiceControls) selectedApproval?.let { action ->
        ModalBottomSheet(
            onDismissRequest = { selectedApproval = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = if (contentColor == Color.White) Color(0xFF1A1A1A) else Color(0xFFF9F7F2)
        ) {
            ApprovalBottomSheetContent(
                action = action,
                contentColor = contentColor,
                accentColor = accentColor,
                isSubmitting = uiState.isResolvingApproval,
                onApprove = {
                    selectedApproval = null
                    viewModel.approvePendingAction(action.id)
                },
                onReject = {
                    selectedApproval = null
                    viewModel.rejectPendingAction(action.id)
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HumanizedAgentTextSheet(
    uiState: AgentUiState,
    userDisplayName: String?,
    draft: String,
    onDraftChange: (String) -> Unit,
    headerTitle: String,
    placeholderText: String,
    surface: String,
    surfaceContext: Map<String, String>,
    viewModel: AgentViewModel,
    powerModeEligible: Boolean,
    enableInlineNavigationActions: Boolean,
    onOpenInlineProfile: (String) -> Unit,
    onOpenInlineChat: (String) -> Unit,
    onSeeMoreInlineResults: (() -> Unit)?,
    onSelectApproval: (AgentPendingAction) -> Unit,
    onDismiss: () -> Unit,
    isFullScreen: Boolean,
    accentColor: Color
) {
    val appearance = currentVormexAppearance()
    val warmBackground = when {
        appearance.isGlassTheme -> appearance.sheetColor.copy(alpha = 0.92f)
        else -> appearance.backgroundColor
    }
    val ink = appearance.contentColor
    val muted = appearance.mutedContentColor
    val line = appearance.dividerColor
    val card = when {
        appearance.isGlassTheme -> appearance.cardColor.copy(alpha = 0.72f)
        else -> appearance.cardColor
    }
    val field = when {
        appearance.isGlassTheme -> appearance.inputColor.copy(alpha = 0.78f)
        else -> appearance.inputColor
    }
    val coral = accentColor
    val userBubble = if (appearance.isDarkTheme || ink.luminance() > 0.5f) {
        appearance.controlColor
    } else {
        Color(0xFF1F1B17)
    }
    val userBubbleText = if (userBubble.luminance() > 0.45f) Color(0xFF1F1B17) else Color.White
    val listState = rememberLazyListState()
    val firstName = remember(userDisplayName) { humanFirstName(userDisplayName) }

    fun sendPrompt(prompt: String) {
        val text = prompt.trim()
        if (text.isBlank() || uiState.isSending) return
        onDraftChange("")
        viewModel.sendMessage(
            message = text,
            surface = surface,
            surfaceContext = surfaceContext
        )
    }

    LaunchedEffect(
        uiState.messages.size,
        uiState.activeInlineResults?.people?.size,
        uiState.pendingApprovals.size
    ) {
        val itemCount = listState.layoutInfo.totalItemsCount
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFullScreen) {
                    Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                } else {
                    Modifier.heightIn(max = 720.dp)
                }
            )
            .background(warmBackground)
            .imePadding()
    ) {
        HumanAgentHeader(
            title = headerTitle,
            status = if (uiState.autoRunEnabled && powerModeEligible) "Power mode" else "Always-on",
            showPower = uiState.autoRunEnabled && powerModeEligible,
            ink = ink,
            muted = muted,
            line = line,
            coral = coral,
            onDismiss = onDismiss
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.messages.isEmpty()) {
                item {
                    HumanAgentIntro(
                        firstName = firstName,
                        ink = ink,
                        muted = muted,
                        line = line,
                        card = card,
                        coral = coral,
                        onPrompt = ::sendPrompt
                    )
                }
            } else {
                if (uiState.autoRunEnabled && powerModeEligible && uiState.lastExecutedActions.isNotEmpty()) {
                    item {
                        HumanAutoRunBanner(
                            text = "Auto-ran: ${uiState.lastExecutedActions.joinToString(", ") { it.title }}",
                            ink = ink,
                            coral = coral
                        )
                    }
                }

                items(uiState.messages) { message ->
                    HumanAgentMessageBubble(
                        message = message,
                        ink = ink,
                        muted = muted,
                        coral = coral,
                        userBubble = userBubble,
                        userBubbleText = userBubbleText
                    )
                }
            }

            uiState.activeInlineResults?.let { panel ->
                item {
                    HumanInlinePeoplePanel(
                        panel = panel,
                        dismissedIds = uiState.dismissedInlineResultIds,
                        actionInProgress = uiState.inlineResultActionInProgress,
                        enableNavigationActions = enableInlineNavigationActions,
                        ink = ink,
                        muted = muted,
                        line = line,
                        card = card,
                        field = field,
                        coral = coral,
                        onViewProfile = onOpenInlineProfile,
                        onMessage = onOpenInlineChat,
                        onConnect = viewModel::sendInlineConnectionRequest,
                        onCloseItem = viewModel::dismissInlineResultItem,
                        onSeeMore = onSeeMoreInlineResults
                    )
                }
            }

            if (uiState.pendingApprovals.isNotEmpty()) {
                items(uiState.pendingApprovals.take(3), key = { it.id }) { action ->
                    HumanPendingApprovalPanel(
                        action = action,
                        isSubmitting = uiState.isResolvingApproval,
                        ink = ink,
                        muted = muted,
                        line = line,
                        card = card,
                        field = field,
                        coral = coral,
                        onOpen = { onSelectApproval(action) },
                        onApprove = { viewModel.approvePendingAction(action.id) },
                        onReject = { viewModel.rejectPendingAction(action.id) }
                    )
                }
            }

            if (!uiState.error.isNullOrBlank()) {
                item {
                    HumanErrorPanel(
                        text = uiState.error,
                        ink = ink,
                        coral = coral
                    )
                }
            }
        }

        HumanComposer(
            draft = draft,
            placeholder = when {
                uiState.pendingApprovals.isNotEmpty() -> "Tell me what to change..."
                else -> placeholderText.ifBlank { "Message Vormex..." }
            },
            isSending = uiState.isSending,
            ink = ink,
            muted = muted,
            line = line,
            card = field,
            coral = coral,
            onDraftChange = onDraftChange,
            onSubmit = { sendPrompt(draft) }
        )
    }
}

@Composable
private fun HumanAgentHeader(
    title: String,
    status: String,
    showPower: Boolean,
    ink: Color,
    muted: Color,
    line: Color,
    coral: Color,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, line.copy(alpha = 0.7f))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "<",
                style = TextStyle(
                    color = ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = AgentBodyFontFamily
                )
            )
        }

        Spacer(Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.kyant.backdrop.catalog.R.drawable.vormex_logo),
                contentDescription = "vormex",
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = title.ifBlank { "vormex" },
                    style = TextStyle(
                        color = ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AgentBodyFontFamily
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showPower) {
                    Spacer(Modifier.width(6.dp))
                    HumanTinyPill(
                        text = "POWER",
                        textColor = coral,
                        background = coral.copy(alpha = 0.14f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF40B86A))
                )
                Spacer(Modifier.width(4.dp))
                BasicText(
                    text = status,
                    style = TextStyle(
                        color = muted,
                        fontSize = 11.sp,
                        fontFamily = AgentBodyFontFamily
                    )
                )
            }
        }

        BasicText(
            text = "...",
            style = TextStyle(
                color = ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = AgentBodyFontFamily
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HumanAgentIntro(
    firstName: String,
    ink: Color,
    muted: Color,
    line: Color,
    card: Color,
    coral: Color,
    onPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.kyant.backdrop.catalog.R.drawable.vormex_logo),
                contentDescription = "vormex",
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Fit
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(
                text = "Hi $firstName,",
                style = TextStyle(
                    color = ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AgentDisplayFontFamily
                )
            )
            BasicText(
                text = "what's the move today?",
                style = TextStyle(
                    color = coral,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AgentDisplayFontFamily
                )
            )
            BasicText(
                text = "Ask in plain English. I can find people, summarize your inbox, suggest groups, or help you grow your network.",
                style = TextStyle(
                    color = muted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontFamily = AgentBodyFontFamily
                )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HumanTaskCard(
                    modifier = Modifier.weight(1f),
                    icon = "people",
                    title = "Find AI people from my campus",
                    subtitle = "Search by skills + college",
                    ink = ink,
                    muted = muted,
                    line = line,
                    card = card,
                    coral = coral,
                    onClick = { onPrompt("Find AI people from my campus") }
                )
                HumanTaskCard(
                    modifier = Modifier.weight(1f),
                    icon = "bell",
                    title = "Catch me up on notifications",
                    subtitle = "Summarise the last 24h",
                    ink = ink,
                    muted = muted,
                    line = line,
                    card = card,
                    coral = coral,
                    onClick = { onPrompt("Catch me up on notifications") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HumanTaskCard(
                    modifier = Modifier.weight(1f),
                    icon = "groups",
                    title = "Suggest groups I'd like",
                    subtitle = "Based on your interests",
                    ink = ink,
                    muted = muted,
                    line = line,
                    card = card,
                    coral = coral,
                    onClick = { onPrompt("Suggest public groups for me") }
                )
                HumanTaskCard(
                    modifier = Modifier.weight(1f),
                    icon = "grow",
                    title = "Help me grow this week",
                    subtitle = "3-step plan from Growth Hub",
                    ink = ink,
                    muted = muted,
                    line = line,
                    card = card,
                    coral = coral,
                    onClick = { onPrompt("Help me grow this week") }
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HumanPromptChip("Find React devs", ink, line, card) { onPrompt("Find React developers") }
            HumanPromptChip("Open my chats", ink, line, card) { onPrompt("Open my chats") }
            HumanPromptChip("Who viewed me?", ink, line, card) { onPrompt("Who viewed me?") }
        }
    }
}

@Composable
private fun HumanTaskCard(
    modifier: Modifier,
    icon: String,
    title: String,
    subtitle: String,
    ink: Color,
    muted: Color,
    line: Color,
    card: Color,
    coral: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(98.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(card)
            .border(1.dp, line, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        HumanTinyIcon(icon = icon, coral = coral)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            BasicText(
                text = title,
                style = TextStyle(
                    color = ink,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AgentBodyFontFamily
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                text = subtitle,
                style = TextStyle(
                    color = muted,
                    fontSize = 10.sp,
                    fontFamily = AgentBodyFontFamily
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HumanTinyIcon(icon: String, coral: Color) {
    val label = when (icon) {
        "people" -> "p"
        "bell" -> "n"
        "groups" -> "g"
        "grow" -> "up"
        else -> "+"
    }
    Box(
        modifier = Modifier
            .size(23.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(coral.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = coral,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AgentBodyFontFamily
            )
        )
    }
}

@Composable
private fun HumanPromptChip(
    text: String,
    ink: Color,
    line: Color,
    card: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(card)
            .border(1.dp, line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            )
        )
    }
}

@Composable
private fun HumanAutoRunBanner(
    text: String,
    ink: Color,
    coral: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(coral.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = ink.copy(alpha = 0.78f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HumanAgentMessageBubble(
    message: AgentMessage,
    ink: Color,
    muted: Color,
    coral: Color,
    userBubble: Color,
    userBubbleText: Color
) {
    val isUser = message.role.equals("user", ignoreCase = true)

    if (isUser) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 230.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                            bottomStart = 14.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(userBubble)
                    .padding(horizontal = 13.dp, vertical = 11.dp)
            ) {
                BasicText(
                    text = message.content,
                    style = TextStyle(
                        color = userBubbleText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontFamily = AgentBodyFontFamily
                    )
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.Top
        ) {
            HumanAssistantAvatar(coral)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                HumanTinyPill(
                    text = "Vormex",
                    textColor = muted,
                    background = muted.copy(alpha = 0.10f)
                )
                BasicText(
                    text = message.content,
                    style = TextStyle(
                        color = ink,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontFamily = AgentBodyFontFamily
                    )
                )
            }
        }
    }
}

@Composable
private fun HumanAssistantAvatar(coral: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = com.kyant.backdrop.catalog.R.drawable.vormex_logo),
            contentDescription = "vormex",
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun HumanInlinePeoplePanel(
    panel: AgentInlineResultsPanel,
    dismissedIds: Set<String>,
    actionInProgress: Set<String>,
    enableNavigationActions: Boolean,
    ink: Color,
    muted: Color,
    line: Color,
    card: Color,
    field: Color,
    coral: Color,
    onViewProfile: (String) -> Unit,
    onMessage: (String) -> Unit,
    onConnect: (String) -> Unit,
    onCloseItem: (String) -> Unit,
    onSeeMore: (() -> Unit)?
) {
    val people = panel.visiblePeople(dismissedIds)
    if (people.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(card)
            .border(1.dp, line, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = panel.title.ifBlank { "People for you" }.uppercase(),
                style = TextStyle(
                    color = muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AgentBodyFontFamily
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                text = "${panel.totalCount.coerceAtLeast(people.size)} matches",
                style = TextStyle(
                    color = muted,
                    fontSize = 10.sp,
                    fontFamily = AgentBodyFontFamily
                )
            )
        }

        people.take(3).forEach { person ->
            HumanPersonCard(
                person = person,
                isActionInProgress = actionInProgress.contains(person.id),
                enableNavigationActions = enableNavigationActions,
                ink = ink,
                muted = muted,
                line = line,
                card = field,
                coral = coral,
                onViewProfile = { onViewProfile(person.id) },
                onMessage = { onMessage(person.id) },
                onConnect = { onConnect(person.id) },
                onHide = { onCloseItem(person.id) }
            )
        }

        if (panel.totalCount > panel.shownCount && onSeeMore != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onSeeMore)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                BasicText(
                    text = "See ${panel.totalCount - panel.shownCount} more matches",
                    style = TextStyle(
                        color = muted,
                        fontSize = 11.sp,
                        fontFamily = AgentBodyFontFamily
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HumanPersonCard(
    person: AgentInlinePerson,
    isActionInProgress: Boolean,
    enableNavigationActions: Boolean,
    ink: Color,
    muted: Color,
    line: Color,
    card: Color,
    coral: Color,
    onViewProfile: () -> Unit,
    onMessage: () -> Unit,
    onConnect: () -> Unit,
    onHide: () -> Unit
) {
    val connectEnabled = !isActionInProgress && person.connectionStatus == "none"
    val connectLabel = when (person.connectionStatus) {
        "connected" -> "Connected"
        "pending_sent" -> "Pending"
        "pending_received" -> "Accept"
        else -> "Connect"
    }
    val tags = (person.matchReasons + person.sharedInterests + person.skills + person.interests)
        .filter { it.isNotBlank() }
        .distinct()
        .take(5)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(card)
            .border(1.dp, line.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(coral.copy(alpha = 0.62f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = personInitials(person),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AgentBodyFontFamily
                    )
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = person.name ?: person.username ?: "Vormex member",
                    style = TextStyle(
                        color = ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AgentBodyFontFamily
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    text = compactPersonLine(person),
                    style = TextStyle(
                        color = muted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontFamily = AgentBodyFontFamily
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.forEach { tag ->
                    HumanTinyPill(
                        text = tag,
                        textColor = coral,
                        background = coral.copy(alpha = 0.12f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (enableNavigationActions) {
                HumanTextButton(
                    modifier = Modifier.weight(1f),
                    text = "Profile",
                    ink = ink,
                    line = line,
                    background = card,
                    onClick = onViewProfile
                )
                HumanTextButton(
                    modifier = Modifier.weight(1f),
                    text = "Message",
                    ink = ink,
                    line = line,
                    background = card,
                    onClick = onMessage
                )
            }
            HumanTextButton(
                modifier = Modifier.weight(0.75f),
                text = "Hide",
                ink = muted,
                line = line,
                background = card,
                onClick = onHide
            )
            HumanPrimaryButton(
                modifier = Modifier.weight(1f),
                text = if (isActionInProgress) "..." else connectLabel,
                coral = coral,
                enabled = connectEnabled,
                onClick = onConnect
            )
        }
    }
}

@Composable
private fun HumanPendingApprovalPanel(
    action: AgentPendingAction,
    isSubmitting: Boolean,
    ink: Color,
    muted: Color,
    line: Color,
    card: Color,
    field: Color,
    coral: Color,
    onOpen: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val preview = pendingActionPreview(action)
    val kind = pendingActionKind(action)
    val approveText = pendingApproveLabel(action)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(card)
            .border(1.dp, coral.copy(alpha = 0.38f), RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = "APPROVAL NEEDED",
                style = TextStyle(
                    color = coral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AgentBodyFontFamily
                )
            )
            BasicText(
                text = kind,
                style = TextStyle(
                    color = muted,
                    fontSize = 10.sp,
                    fontFamily = AgentBodyFontFamily
                )
            )
        }
        BasicText(
            text = action.title.ifBlank { "Review action" },
            style = TextStyle(
                color = ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AgentBodyFontFamily
            )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(field)
                .border(1.dp, line, RoundedCornerShape(10.dp))
                .padding(11.dp)
        ) {
            BasicText(
                text = preview,
                style = TextStyle(
                    color = ink,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontFamily = AgentBodyFontFamily
                ),
                maxLines = 7,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            HumanTextButton(
                modifier = Modifier.weight(1f),
                text = "Cancel",
                ink = muted,
                line = line,
                background = field,
                enabled = !isSubmitting,
                onClick = onReject
            )
            HumanPrimaryButton(
                modifier = Modifier.weight(1f),
                text = if (isSubmitting) "Working..." else approveText,
                coral = coral,
                enabled = !isSubmitting,
                onClick = onApprove
            )
        }
    }
}

@Composable
private fun HumanErrorPanel(
    text: String,
    ink: Color,
    coral: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(coral.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = ink,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontFamily = AgentBodyFontFamily
            )
        )
    }
}

@Composable
private fun HumanComposer(
    draft: String,
    placeholder: String,
    isSending: Boolean,
    ink: Color,
    muted: Color,
    line: Color,
    card: Color,
    coral: Color,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(card)
            .border(1.dp, line, RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BasicText(
            text = "+",
            style = TextStyle(
                color = ink.copy(alpha = 0.74f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                fontFamily = AgentBodyFontFamily
            )
        )
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                singleLine = false,
                maxLines = 4,
                textStyle = TextStyle(
                    color = ink,
                    fontSize = 13.sp,
                    fontFamily = AgentBodyFontFamily
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (draft.isBlank()) {
                        BasicText(
                            text = placeholder,
                            style = TextStyle(
                                color = muted.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontFamily = AgentBodyFontFamily
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            )
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (draft.isBlank()) Color.Transparent else coral)
                .clickable(enabled = draft.isNotBlank() && !isSending, onClick = onSubmit),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = if (isSending) "..." else if (draft.isBlank()) "" else ">",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AgentBodyFontFamily
                )
            )
        }
    }
}

@Composable
private fun HumanTinyPill(
    text: String,
    textColor: Color,
    background: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HumanTextButton(
    modifier: Modifier = Modifier,
    text: String,
    ink: Color,
    line: Color,
    background: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .border(1.dp, line, RoundedCornerShape(9.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = ink.copy(alpha = if (enabled) 0.92f else 0.45f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HumanPrimaryButton(
    modifier: Modifier = Modifier,
    text: String,
    coral: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(coral.copy(alpha = if (enabled) 1f else 0.28f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AgentBodyFontFamily
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AgentMessageBubble(
    message: AgentMessage,
    contentColor: Color,
    accentColor: Color
) {
    val isUser = message.role.equals("user", ignoreCase = true)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (isUser) 22.dp else 8.dp,
                        bottomEnd = if (isUser) 8.dp else 22.dp
                    )
                )
                .background(
                    if (isUser) accentColor.copy(alpha = 0.18f)
                    else contentColor.copy(alpha = 0.08f)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicText(
                text = message.content,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = AgentBodyFontFamily
                )
            )
        }
    }
}

private fun humanizeSurface(surface: String): String {
    return when (surface.trim().lowercase()) {
        "feed" -> "Home"
        "find_people" -> "Find People"
        "chat" -> "Chat"
        "groups" -> "Groups"
        "profile" -> "Profile"
        "notifications" -> "Notifications"
        "growth_hub" -> "Growth Hub"
        "talk_with_vormex" -> "vormex"
        else -> surface.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

private fun humanFirstName(name: String?): String {
    return name
        ?.trim()
        ?.split(Regex("\\s+"))
        ?.firstOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: "there"
}

private fun personInitials(person: AgentInlinePerson): String {
    val source = person.name?.takeIf { it.isNotBlank() } ?: person.username ?: "V"
    return source
        .split(Regex("\\s+|[._-]+"))
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifBlank { "V" }
}

private fun compactPersonLine(person: AgentInlinePerson): String {
    return listOfNotNull(
        person.headline?.takeIf { it.isNotBlank() },
        person.college?.takeIf { it.isNotBlank() },
        person.branch?.takeIf { it.isNotBlank() }
    )
        .joinToString(" · ")
        .ifBlank { person.bio?.takeIf { it.isNotBlank() } ?: "Vormex member" }
}

private fun AgentPendingAction.inputObject(): JsonObject? {
    return input as? JsonObject
}

private fun JsonObject.stringValue(key: String): String? {
    return this[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
}

private fun JsonObject.stringArrayValue(key: String): List<String> {
    return (this[key] as? JsonArray)
        ?.mapNotNull { item -> item.jsonPrimitive.contentOrNull?.trim()?.takeIf { it.isNotBlank() } }
        .orEmpty()
}

private fun pendingActionPreview(action: AgentPendingAction): String {
    val input = action.inputObject()
    return when (action.toolName) {
        "chat_send_message" -> input?.stringValue("message")
        "posts_create_text" -> input?.stringValue("content")
        "profile_update_summary" -> {
            val headline = input?.stringValue("headline")
            val bio = input?.stringValue("bio")
            val interests = input?.stringArrayValue("interests").orEmpty()
            buildString {
                if (!headline.isNullOrBlank()) append("Headline: ").append(headline)
                if (!bio.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append("Bio: ").append(bio)
                }
                if (interests.isNotEmpty()) {
                    if (isNotEmpty()) append("\n\n")
                    append("Interests: ").append(interests.joinToString(", "))
                }
            }.ifBlank { action.summary }
        }
        "connections_accept_request" -> action.summary
        else -> input?.stringValue("note") ?: action.summary
    } ?: action.summary
}

private fun pendingActionKind(action: AgentPendingAction): String {
    return when (action.toolName) {
        "chat_send_message" -> "Direct message"
        "posts_create_text" -> "Feed post"
        "profile_update_summary" -> "Profile field"
        "connections_accept_request" -> "Connection"
        else -> action.actionType.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

private fun pendingApproveLabel(action: AgentPendingAction): String {
    return when (action.toolName) {
        "chat_send_message" -> "Send DM"
        "posts_create_text" -> "Post now"
        "profile_update_summary" -> "Update"
        "connections_accept_request" -> "Accept"
        else -> "Approve"
    }
}

@Composable
private fun MetaChip(
    text: String,
    contentColor: Color,
    background: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            )
        )
    }
}

@Composable
private fun ActionChip(
    text: String,
    contentColor: Color,
    background: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            )
        )
    }
}

private fun buildOpeningVoiceGreetingPrompt(userDisplayName: String?): String {
    val firstName = userDisplayName
        ?.trim()
        ?.split(Regex("\\s+"))
        ?.firstOrNull()
        ?.takeIf { it.isNotBlank() }

    val greetingLine = if (firstName != null) {
        "Hey, $firstName, what's the plan for today?"
    } else {
        "Hey, what's the plan for today?"
    }

    return "Open the conversation first. Say exactly one short warm line: \"$greetingLine\" Then stop speaking and wait for the user's reply."
}

@Composable
private fun GoalChip(
    goal: AgentGoal,
    contentColor: Color,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = goal.goal,
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AgentBodyFontFamily
            )
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.12f))
                .clickable(onClick = onDelete)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            BasicText(
                text = "X",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AgentBodyFontFamily
                )
            )
        }
    }
}

@Composable
private fun PendingApprovalCard(
    action: AgentPendingAction,
    contentColor: Color,
    accentColor: Color,
    onOpen: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(accentColor.copy(alpha = 0.1f))
            .clickable(onClick = onOpen)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicText(
                text = action.title.ifBlank { "Pending approval" },
                style = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AgentBodyFontFamily
                )
            )
            BasicText(
                text = action.summary,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontFamily = AgentBodyFontFamily
                )
            )
        }
    }
}
