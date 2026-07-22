package com.kyant.backdrop.catalog.linkedin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.*
import com.kyant.backdrop.catalog.linkedin.crossedpaths.CrossedPathsScreen
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField
import com.kyant.backdrop.catalog.ui.blockTouchPassthrough
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.ads.ManagedFeedAdCard
import com.kyant.backdrop.catalog.ads.VormexAdsManager
import com.kyant.backdrop.catalog.ads.VormexNativeFeedAd
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.components.LiquidToggle
import com.kyant.backdrop.catalog.network.AgentApiService
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.Author
import com.kyant.backdrop.catalog.network.models.Comment
import com.kyant.backdrop.catalog.network.models.FullProfileResponse
import com.kyant.backdrop.catalog.network.models.PendingConnectionRequest
import com.kyant.backdrop.catalog.network.models.ProfileUpdateRequest
import com.kyant.backdrop.catalog.network.models.CelebrationType
import com.kyant.backdrop.catalog.network.models.PollOption
import com.kyant.backdrop.catalog.network.models.Post
import com.kyant.backdrop.catalog.network.models.ManagedAdPlacement
import com.kyant.backdrop.catalog.network.models.CreatorProResponse
import com.kyant.backdrop.catalog.network.models.CreatorProSettingsRequest
import com.kyant.backdrop.catalog.network.models.PremiumPlanOption
import com.kyant.backdrop.catalog.network.models.PremiumSubscriptionResponse
import com.kyant.backdrop.catalog.network.models.StoryGroup
import com.kyant.backdrop.catalog.payments.PremiumCheckoutManager
import com.kyant.backdrop.catalog.payments.findComponentActivity
import com.kyant.backdrop.catalog.chat.ChatTabContent
import com.kyant.backdrop.catalog.linkedin.posts.SharePostModal
import com.kyant.backdrop.catalog.linkedin.posts.FormattedContent
import com.kyant.backdrop.catalog.linkedin.posts.MentionProfilePreviewPopup
import com.kyant.backdrop.catalog.network.PostsApiService
import com.kyant.backdrop.catalog.network.RecommendationApiService
import com.kyant.backdrop.catalog.network.models.PostBoostCampaign
import com.kyant.backdrop.catalog.network.models.PostBoostCredits
import com.kyant.backdrop.catalog.linkedin.groups.GroupsScreen
import com.kyant.backdrop.catalog.linkedin.groups.CreateGroupModal
import com.kyant.backdrop.catalog.linkedin.groups.GroupDetailScreen
import com.kyant.backdrop.catalog.linkedin.groups.GroupInviteLinkScreen
import com.kyant.backdrop.catalog.linkedin.groups.GroupSettingsScreen
import com.kyant.backdrop.catalog.linkedin.groups.GroupChatScreen
import com.kyant.backdrop.catalog.linkedin.groups.CirclesScreen
import com.kyant.backdrop.catalog.game.GamesHubScreen
import com.kyant.backdrop.catalog.linkedin.groups.CircleDetailScreen
import com.kyant.backdrop.catalog.network.GroupsApiService
import com.kyant.backdrop.catalog.linkedin.reels.ReelsPreviewSection
import com.kyant.backdrop.catalog.linkedin.reels.ReelsFeedScreen
import com.kyant.backdrop.catalog.linkedin.reels.ReelCommentsSheet
import com.kyant.backdrop.catalog.linkedin.reels.ReelCreateSheet
import com.kyant.backdrop.catalog.linkedin.reels.ReelsViewModel
import com.kyant.backdrop.catalog.network.models.Reel
import com.kyant.backdrop.catalog.notifications.MessageNotificationManager
import com.kyant.backdrop.catalog.notifications.VormexMessagingService
import com.kyant.backdrop.catalog.onboarding.ProfileSetupWizard
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.deeplink.VormexDeepLinks
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import com.kyant.backdrop.catalog.chat.ChatViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Pacifico font family
private val PacificoFontFamily = FontFamily(
    Font(R.font.pacifico)
)

// Kaushan Script font family for vormeX branding
private val KaushanScriptFontFamily = FontFamily(
    Font(R.font.kaushan_script)
)

internal fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick
    )
}
internal class MenuAnchorBoundsHolder {
    var coordinates: LayoutCoordinates? = null

    fun currentBounds(): Rect? = coordinates?.takeIf { it.isAttached }?.boundsInWindow()
}

// Shimmer effect for skeleton loading
@Composable
private fun shimmerBrush(isLightTheme: Boolean): Brush {
    val shimmerColors = if (isLightTheme) {
        listOf(
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.5f),
            Color.LightGray.copy(alpha = 0.3f)
        )
    } else {
        listOf(
            Color.DarkGray.copy(alpha = 0.3f),
            Color.DarkGray.copy(alpha = 0.5f),
            Color.DarkGray.copy(alpha = 0.3f)
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation.value - 300f, translateAnimation.value - 300f),
        end = Offset(translateAnimation.value, translateAnimation.value)
    )
}

// Skeleton loading card for posts — avoid full glass blur on placeholders (much cheaper GPU work).
@Composable
private fun PostSkeletonCard(
    shimmerBrush: Brush,
    isLightTheme: Boolean
) {
    val appearance = currentVormexAppearance()
    val surfaceColor = if (appearance.isGlassTheme) {
        if (isLightTheme) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)
    } else {
        appearance.cardColor
    }
    val borderColor = if (appearance.isGlassTheme) {
        if (isLightTheme) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f)
    } else {
        appearance.cardBorderColor
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Author skeleton
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar skeleton
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    // Name skeleton
                    Box(
                        Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(Modifier.height(6.dp))
                    // Headline skeleton
                    Box(
                        Modifier
                            .width(180.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(Modifier.height(4.dp))
                    // Time skeleton
                    Box(
                        Modifier
                            .width(60.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }
            }

            // Content skeleton - multiple lines
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Box(
                Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )

            // Image skeleton
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerBrush)
            )

            // Stats skeleton
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Box(
                    Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }

            // Divider
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (isLightTheme) Color.LightGray.copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.2f))
            )

            // Action buttons skeleton
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(3) {
                    Box(
                        Modifier
                            .width(60.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionOverlayContainer(
    backdrop: LayerBackdrop,
    themeMode: String,
    glassBackgroundKey: String,
    accentColor: Color,
    glassMotionStyleKey: String,
    reduceAnimations: Boolean,
    applyStatusBarPadding: Boolean = true,
    content: @Composable () -> Unit
) {
    val appearance = currentVormexAppearance(themeMode)
    val isGlassTheme = appearance.isGlassTheme

    Box(
        Modifier
            .fillMaxSize()
            .blockTouchPassthrough()
    ) {
        if (isGlassTheme) {
            GlassBackgroundLayer(
                modifier = Modifier
                    .layerBackdrop(backdrop)
                    .fillMaxSize(),
                backgroundKey = glassBackgroundKey,
                accentColor = accentColor,
                motionStyleKey = glassMotionStyleKey,
                reduceAnimations = reduceAnimations
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(appearance.backgroundColor)
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
        ) {
            content()
        }
    }
}

enum class LinkedInTab {
    Home, Network, Post, Notifications, Jobs
}

// Helper to get Activity from Context
private fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedInContent(
    deepLink: com.kyant.backdrop.catalog.NotificationDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val appScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    val startGoogleSignIn = {
        val hostActivity = activity
        if (hostActivity != null) {
            viewModel.googleSignIn(hostActivity)
        } else {
            Toast.makeText(context, "Google Sign-In is unavailable here.", Toast.LENGTH_SHORT).show()
        }
    }
    val sessionUserKey = uiState.currentUserId
        ?.takeIf { uiState.isLoggedIn && it.isNotBlank() }
        ?: "signed-out"
    val pendingGroupConversationKeys by MessageNotificationManager.pendingGroupConversationKeys.collectAsState()

    // Theme preference: "glass", "light", "dark"
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val glassBackgroundKey by SettingsPreferences.glassBackgroundPreset(context)
        .collectAsState(initial = DefaultGlassBackgroundPresetKey)
    val accentPaletteKey by SettingsPreferences.accentPalette(context)
        .collectAsState(initial = DefaultAccentPaletteKey)
    val glassMotionStyleKey by SettingsPreferences.glassMotionStyle(context)
        .collectAsState(initial = DefaultGlassMotionStyleKey)
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    val showReelsOnHome by SettingsPreferences.showReelsOnHome(context).collectAsState(initial = false)
    val appearance = rememberVormexAppearance(themeMode)
    val isGlassTheme = appearance.isGlassTheme
    val isLightTheme = appearance.isLightTheme
    val isDarkTheme = appearance.isDarkTheme
    val contentColor = appearance.contentColor
    val accentColor = vormexAccentColor(themeMode, accentPaletteKey)
    val hasPendingGroupMessages = pendingGroupConversationKeys.isNotEmpty()
    val footerIconSize = 28.dp
    val footerTextStyle = TextStyle(contentColor, 12.sp)

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showFullMoreScreen by rememberSaveable { mutableStateOf(false) }
    var showCrossedPathsFullScreen by rememberSaveable { mutableStateOf(false) }
    var premiumDetailsRequestKey by rememberSaveable { mutableIntStateOf(0) }
    var crossedPathsRequestKey by rememberSaveable { mutableIntStateOf(0) }
    var moreHubAnimationKey by rememberSaveable { mutableIntStateOf(0) }
    var viewingProfileUserId by remember { mutableStateOf<String?>(null) }
    var pendingSmartMatchDeepLinkUserId by remember { mutableStateOf<String?>(null) }
    var profileOpenedFromReels by remember { mutableStateOf(false) }
    var openChatWithUserId by remember { mutableStateOf<String?>(null) }
    var openChatInitialDraft by remember { mutableStateOf<String?>(null) }
    var preparingConversationStarterForUserId by remember { mutableStateOf<String?>(null) }
    var profileConversationHasMessagesByUserId by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    // Track if user is viewing a personal chat thread (for hiding bottom nav)
    var isInChatThread by remember { mutableStateOf(false) }
    val backdrop = rememberLayerBackdrop()

    // Messages screen state
    var showMessagesScreen by remember { mutableStateOf(false) }
    var showMessagesCreateGroupMenu by remember { mutableStateOf(false) }
    var showMessagesCreateGroupSheet by remember { mutableStateOf(false) }
    var isCreatingGroupFromMessages by remember { mutableStateOf(false) }

    // Groups & Circles navigation state
    var showGroupsScreen by remember { mutableStateOf(false) }
    var showCirclesScreen by remember { mutableStateOf(false) }
    var openCreateGroupRequestKey by rememberSaveable { mutableIntStateOf(0) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var selectedGroupInviteCode by remember { mutableStateOf<String?>(null) }
    var selectedCircleId by remember { mutableStateOf<String?>(null) }
    var showGroupChat by remember { mutableStateOf(false) }
    var showGroupSettingsScreen by remember { mutableStateOf(false) }

    // Retention features navigation state
    var showWeeklyGoalsScreen by remember { mutableStateOf(false) }
    var showStreakDetailsScreen by remember { mutableStateOf(false) }
    var showTopNetworkersScreen by remember { mutableStateOf(false) }
    var showOnboardingScreen by remember { mutableStateOf(false) }
    var showSessionSummary by remember { mutableStateOf(false) }
    var showConnectionCelebration by remember { mutableStateOf(false) }
    var celebrationConnectionId by remember { mutableStateOf<String?>(null) }

    // Deep link navigation state - for opening specific post/reel from notification
    var deepLinkPostId by remember { mutableStateOf<String?>(null) }
    var deepLinkPostShouldOpenComments by remember { mutableStateOf(false) }
    var deepLinkReelId by remember { mutableStateOf<String?>(null) }
    var deepLinkReelCommentId by remember { mutableStateOf<String?>(null) }
    var deepLinkReelParentCommentId by remember { mutableStateOf<String?>(null) }
    var deepLinkConversationId by remember { mutableStateOf<String?>(null) }

    // Shared post detail/comments state (used by feed and profile screens)
    var showCommentsSheet by remember { mutableStateOf(false) }
    var selectedPostForComments by remember { mutableStateOf<String?>(null) }
    var reportingPostId by remember { mutableStateOf<String?>(null) }
    var isSubmittingPostReport by remember { mutableStateOf(false) }

    // Settings & More screen navigation state
    var showProfileScreen by remember { mutableStateOf(false) }
    var showSavedPostsScreen by remember { mutableStateOf(false) }
    var showConnectionRequestsScreen by remember { mutableStateOf(false) }
    var showProfileCustomizationsScreen by remember { mutableStateOf(false) }
    var showNotificationsInbox by remember { mutableStateOf(false) }
    var showProfileViewersScreen by remember { mutableStateOf(false) }
    var showNotificationSettingsScreen by remember { mutableStateOf(false) }
    var showPrivacySettingsScreen by remember { mutableStateOf(false) }
    var showAppearanceSettingsScreen by remember { mutableStateOf(false) }
    var showHelpScreen by remember { mutableStateOf(false) }
    var showInviteFriendsScreen by remember { mutableStateOf(false) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var showContactScreen by remember { mutableStateOf(false) }
    var showGrowthHubScreen by remember { mutableStateOf(false) }
    var showGamesScreen by remember { mutableStateOf(false) }
    var showTalentEngineScreen by remember { mutableStateOf(false) }
    var showSkillPassportScreen by remember { mutableStateOf(false) }
    var showSkillSwapScreen by remember { mutableStateOf(false) }
    var skillSwapInitialTab by remember { mutableStateOf("discover") }
    var showHackathonBoardScreen by remember { mutableStateOf(false) }
    var showAgentSheet by remember { mutableStateOf(false) }
    var minimizeAgentSheetForVoice by remember { mutableStateOf(false) }
    var autoMinimizedAgentSheetForActiveVoice by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAccountSwitcherDialog by rememberSaveable { mutableStateOf(false) }
    var logoutDialogTitle by remember { mutableStateOf("Log Out") }
    var logoutDialogMessage by remember {
        mutableStateOf("Are you sure you want to log out? You'll need to sign in again to access your account.")
    }
    var logoutDialogConfirmText by remember { mutableStateOf("Log Out") }
    var notificationUnreadCount by remember { mutableIntStateOf(0) }
    var shareAnimationTrigger by remember { mutableIntStateOf(0) }
    val hasOpenPostDetail = uiState.openedPost != null ||
        uiState.isLoadingOpenedPost ||
        uiState.openedPostError != null

    val hasOverlayBackNavigation = hasOpenPostDetail ||
            viewingProfileUserId != null ||
            showMessagesScreen ||
            showGroupChat ||
            showGroupSettingsScreen ||
            selectedGroupId != null ||
            selectedGroupInviteCode != null ||
            selectedCircleId != null ||
            showGroupsScreen ||
            showCirclesScreen ||
            showWeeklyGoalsScreen ||
            showStreakDetailsScreen ||
            showTopNetworkersScreen ||
            showSessionSummary ||
            showConnectionCelebration ||
            showProfileScreen ||
            showSavedPostsScreen ||
            showConnectionRequestsScreen ||
            showProfileCustomizationsScreen ||
            showNotificationsInbox ||
            showProfileViewersScreen ||
            showNotificationSettingsScreen ||
            showPrivacySettingsScreen ||
            showAppearanceSettingsScreen ||
            showHelpScreen ||
            showInviteFriendsScreen ||
            showAboutScreen ||
            showContactScreen ||
            showGrowthHubScreen ||
            showGamesScreen ||
            showTalentEngineScreen ||
            showSkillPassportScreen ||
            showSkillSwapScreen ||
            showHackathonBoardScreen ||
            showAgentSheet

    fun resetAccountScopedNavigation() {
        selectedTab = 0
        showFullMoreScreen = false
        viewingProfileUserId = null
        pendingSmartMatchDeepLinkUserId = null
        profileOpenedFromReels = false
        openChatWithUserId = null
        openChatInitialDraft = null
        preparingConversationStarterForUserId = null
        profileConversationHasMessagesByUserId = emptyMap()
        isInChatThread = false
        showMessagesScreen = false
        showGroupsScreen = false
        showCirclesScreen = false
        selectedGroupId = null
        selectedGroupInviteCode = null
        selectedCircleId = null
        showGroupChat = false
        showGroupSettingsScreen = false
        showWeeklyGoalsScreen = false
        showStreakDetailsScreen = false
        showTopNetworkersScreen = false
        showOnboardingScreen = false
        showSessionSummary = false
        showConnectionCelebration = false
        celebrationConnectionId = null
        deepLinkPostId = null
        deepLinkPostShouldOpenComments = false
        deepLinkReelId = null
        deepLinkReelCommentId = null
        deepLinkReelParentCommentId = null
        deepLinkConversationId = null
        showCommentsSheet = false
        selectedPostForComments = null
        showProfileScreen = false
        showSavedPostsScreen = false
        showConnectionRequestsScreen = false
        showProfileCustomizationsScreen = false
        showNotificationsInbox = false
        showProfileViewersScreen = false
        showNotificationSettingsScreen = false
        showPrivacySettingsScreen = false
        showAppearanceSettingsScreen = false
        showHelpScreen = false
        showInviteFriendsScreen = false
        showAboutScreen = false
        showContactScreen = false
        showGrowthHubScreen = false
        showGamesScreen = false
        showTalentEngineScreen = false
        showSkillPassportScreen = false
        showSkillSwapScreen = false
        skillSwapInitialTab = "discover"
        showHackathonBoardScreen = false
        showAgentSheet = false
        minimizeAgentSheetForVoice = false
        autoMinimizedAgentSheetForActiveVoice = false
        showAccountSwitcherDialog = false
    }

    fun openPremiumDetails() {
        resetAccountScopedNavigation()
        selectedTab = 3
        showFullMoreScreen = false
        premiumDetailsRequestKey += 1
    }

    fun showAccountExitDialog(switchAccount: Boolean) {
        if (switchAccount) {
            showAccountSwitcherDialog = true
            return
        }
        logoutDialogTitle = if (switchAccount) "Switch Account" else "Log Out"
        logoutDialogMessage = if (switchAccount) {
            "Sign out of this account so you can choose another one."
        } else {
            "Are you sure you want to log out? You'll need to sign in again to access your account."
        }
        logoutDialogConfirmText = if (switchAccount) "Switch Account" else "Log Out"
        showLogoutDialog = true
    }

    // Handle system back button for all overlay screens
    // Priority: innermost overlays first, then outer overlays
    BackHandler(enabled = hasOverlayBackNavigation) {
        when {
            showAgentSheet -> {
                minimizeAgentSheetForVoice = false
                showAgentSheet = false
            }

            hasOpenPostDetail -> viewModel.closePostDetail()

            // Profile viewing (highest priority - innermost overlay)
            viewingProfileUserId != null -> viewingProfileUserId = null

            // Messages screen
            showMessagesScreen -> showMessagesScreen = false

            // Group chat
            showGroupChat -> showGroupChat = false
            showGroupSettingsScreen -> showGroupSettingsScreen = false

            // Group/Circle detail screens
            selectedGroupInviteCode != null -> {
                selectedGroupInviteCode = null
                showGroupsScreen = true
            }
            selectedGroupId != null -> {
                showGroupSettingsScreen = false
                selectedGroupId = null
            }
            selectedCircleId != null -> selectedCircleId = null

            // Groups/Circles list screens
            showGroupsScreen -> showGroupsScreen = false
            showCirclesScreen -> showCirclesScreen = false

            // Retention feature screens
            showWeeklyGoalsScreen -> showWeeklyGoalsScreen = false
            showStreakDetailsScreen -> showStreakDetailsScreen = false
            showTopNetworkersScreen -> showTopNetworkersScreen = false
            showSessionSummary -> showSessionSummary = false
            showConnectionCelebration -> showConnectionCelebration = false

            // Settings screens
            showProfileScreen -> showProfileScreen = false
            showSavedPostsScreen -> showSavedPostsScreen = false
            showConnectionRequestsScreen -> showConnectionRequestsScreen = false
            showProfileCustomizationsScreen -> showProfileCustomizationsScreen = false
            showNotificationsInbox -> showNotificationsInbox = false
            showProfileViewersScreen -> showProfileViewersScreen = false
            showNotificationSettingsScreen -> showNotificationSettingsScreen = false
            showPrivacySettingsScreen -> showPrivacySettingsScreen = false
            showAppearanceSettingsScreen -> showAppearanceSettingsScreen = false
            showHelpScreen -> showHelpScreen = false
            showInviteFriendsScreen -> showInviteFriendsScreen = false
            showAboutScreen -> showAboutScreen = false
            showContactScreen -> showContactScreen = false
            showGrowthHubScreen -> showGrowthHubScreen = false
            showGamesScreen -> showGamesScreen = false
            showTalentEngineScreen -> showTalentEngineScreen = false
            showSkillPassportScreen -> showSkillPassportScreen = false
            showSkillSwapScreen -> showSkillSwapScreen = false
            showHackathonBoardScreen -> showHackathonBoardScreen = false
        }
    }

    BackHandler(enabled = !hasOverlayBackNavigation && selectedTab == 3 && showFullMoreScreen) {
        showFullMoreScreen = false
    }

    // Android back gesture: from non-Home bottom tabs, go back to Home first.
    BackHandler(
        enabled = !hasOverlayBackNavigation &&
            selectedTab != 0 &&
            !(selectedTab == 3 && showFullMoreScreen)
    ) {
        selectedTab = 0
    }

    val handlePostMenuAction: (String, String) -> Unit = { postId, action ->
        when (action) {
            "save" -> viewModel.toggleSave(postId)
            "copy_link" -> viewModel.copyPostLink(postId)
            "not_interested" -> viewModel.markPostNotInterested(postId)
            "report" -> reportingPostId = postId
        }
    }

    var postIdForBoost by rememberSaveable { mutableStateOf<String?>(null) }
    var postBoostCredits by remember { mutableStateOf<PostBoostCredits?>(null) }
    var postBoostCampaign by remember { mutableStateOf<PostBoostCampaign?>(null) }
    var isUpdatingPostBoost by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(postIdForBoost) {
        val postId = postIdForBoost ?: return@LaunchedEffect
        postBoostCredits = RecommendationApiService.getPostBoostCredits(context).getOrNull()
        postBoostCampaign = RecommendationApiService.listPostBoosts(context).getOrNull()
            ?.campaigns
            ?.firstOrNull { it.postId == postId && it.status.equals("active", ignoreCase = true) }
    }

    postIdForBoost?.let { postId ->
        val activeCampaign = postBoostCampaign
        AlertDialog(
            onDismissRequest = { if (!isUpdatingPostBoost) postIdForBoost = null },
            title = { BasicText(if (activeCampaign == null) "Boost this post?" else "Post boost status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (activeCampaign == null) {
                        BasicText("Use one 24-hour Premium credit. Targeting is automatic and every placement is labeled Boosted.")
                        BasicText("Credits remaining: ${postBoostCredits?.creditsRemaining ?: "—"}")
                    } else {
                        BasicText("Status: ${activeCampaign.status}")
                        BasicText("${activeCampaign.impressions} impressions • ${activeCampaign.clicks} clicks • ${activeCampaign.meaningfulActions} meaningful actions")
                        activeCampaign.pauseReason?.let { BasicText("Pause reason: $it") }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isUpdatingPostBoost && (activeCampaign != null || (postBoostCredits?.creditsRemaining ?: 0) > 0),
                    onClick = {
                        appScope.launch {
                            isUpdatingPostBoost = true
                            val result = if (activeCampaign == null) {
                                RecommendationApiService.createPostBoost(context, postId)
                            } else {
                                RecommendationApiService.cancelPostBoost(context, activeCampaign.id)
                            }
                            result.onSuccess {
                                Toast.makeText(
                                    context,
                                    if (activeCampaign == null) "Post boost started" else "Post boost cancelled",
                                    Toast.LENGTH_SHORT
                                ).show()
                                postIdForBoost = null
                            }.onFailure { error ->
                                Toast.makeText(context, error.message ?: "Could not update post boost", Toast.LENGTH_SHORT).show()
                            }
                            isUpdatingPostBoost = false
                        }
                    }
                ) { BasicText(if (activeCampaign == null) "Confirm boost" else "Cancel campaign") }
            },
            dismissButton = {
                TextButton(onClick = { postIdForBoost = null }, enabled = !isUpdatingPostBoost) {
                    BasicText("Close")
                }
            }
        )
    }

    fun submitPostReport(reason: String, details: String, blockAuthor: Boolean) {
        val postId = reportingPostId ?: return
        if (isSubmittingPostReport) return
        appScope.launch {
            isSubmittingPostReport = true
            PostsApiService.reportPost(
                context = context,
                postId = postId,
                reason = reason,
                description = details.ifBlank { null },
                blockUser = blockAuthor
            ).onSuccess {
                Toast.makeText(
                    context,
                    if (blockAuthor) {
                        "Report sent and author blocked."
                    } else {
                        "Thanks - we received your report."
                    },
                    Toast.LENGTH_LONG
                ).show()
                reportingPostId = null
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: "Could not submit report",
                    Toast.LENGTH_LONG
                ).show()
            }
            isSubmittingPostReport = false
        }
    }

    val openPostFromLink: (String) -> Unit = { postId ->
        selectedTab = 0
        showFullMoreScreen = false
        showMessagesScreen = false
        isInChatThread = false
        showGroupChat = false
        showGroupSettingsScreen = false
        selectedGroupId = null
        selectedGroupInviteCode = null
        selectedCircleId = null
        showSavedPostsScreen = false
        showNotificationsInbox = false
        showCommentsSheet = false
        selectedPostForComments = null
        viewModel.openPostDetail(postId)
    }

    val ownProfileViewModel: ProfileViewModel = viewModel(
        key = "profile:me:$sessionUserKey",
        factory = ProfileViewModel.Factory(context)
    )

    LaunchedEffect(uiState.isLoggedIn, uiState.currentUserId) {
        notificationUnreadCount = if (uiState.isLoggedIn) {
            ApiClient.getNotificationUnreadCount(context).getOrDefault(0)
        } else {
            0
        }
    }

    // Handle deep links from push notifications
    LaunchedEffect(deepLink, uiState.isLoggedIn) {
        deepLink?.let { link ->
            if (link.action != "auth_flow" && !uiState.isLoggedIn) {
                return@LaunchedEffect
            }

            when (link.action) {
                "auth_flow" -> {
                    if (!uiState.isLoggedIn) {
                        viewModel.setPendingReferralCode(link.referralCode)
                        if (link.authMode.equals("signup", ignoreCase = true) || !link.referralCode.isNullOrBlank()) {
                            viewModel.showSignUp()
                        }
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_STREAK_REMINDER -> {
                    showStreakDetailsScreen = true
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_WEEKLY_GOAL -> {
                    showWeeklyGoalsScreen = true
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_LEADERBOARD -> {
                    showTopNetworkersScreen = true
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_CONNECTION_CELEBRATION -> {
                    link.connectionId?.let { connectionId ->
                        celebrationConnectionId = connectionId
                        showConnectionCelebration = true
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_SESSION_SUMMARY -> {
                    showSessionSummary = true
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_PROFILE -> {
                    link.userId?.let { userId ->
                        viewingProfileUserId = userId
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_PROFILE_VIEWS -> {
                    showProfileViewersScreen = true
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_CHAT -> {
                    // Open the messages overlay instead of the Post tab.
                    showMessagesScreen = true
                    if (!link.conversationId.isNullOrBlank()) {
                        val convId = link.conversationId
                        deepLinkConversationId = convId
                        openChatWithUserId = null
                        openChatInitialDraft = null
                    } else if (!link.userId.isNullOrBlank()) {
                        deepLinkConversationId = null
                        openChatInitialDraft = null
                        openChatWithUserId = link.userId
                    } else {
                        deepLinkConversationId = null
                        openChatWithUserId = null
                        openChatInitialDraft = null
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_GROUP_CHAT -> {
                    link.groupId?.let { groupId ->
                        selectedTab = 3
                        showGroupsScreen = true
                        selectedGroupId = groupId
                        selectedGroupInviteCode = null
                        showGroupChat = true
                    }
                }
                VormexMessagingService.ACTION_HACKATHONS -> {
                    selectedTab = 3
                    showFullMoreScreen = false
                    showHackathonBoardScreen = true
                }
                VormexMessagingService.ACTION_SKILL_SWAP -> {
                    selectedTab = 3
                    showFullMoreScreen = false
                    skillSwapInitialTab = link.tab ?: "requests"
                    showSkillSwapScreen = true
                }
                VormexDeepLinks.ACTION_GROUP_INVITE_LINK -> {
                    link.groupInviteCode?.let { inviteCode ->
                        selectedTab = 3
                        showGroupsScreen = true
                        selectedGroupId = null
                        selectedGroupInviteCode = inviteCode
                        showGroupChat = false
                        showGroupSettingsScreen = false
                    }
                }
                VormexDeepLinks.ACTION_POST_LINK -> {
                    link.postId?.let { postId ->
                        openPostFromLink(postId)
                    }
                }
                VormexDeepLinks.ACTION_REEL_LINK -> {
                    link.reelId?.let { reelId ->
                        selectedTab = 0
                        deepLinkReelId = reelId
                        deepLinkReelCommentId = link.commentId
                        deepLinkReelParentCommentId = link.parentCommentId
                    }
                }
                VormexDeepLinks.ACTION_GITHUB_INTEGRATION -> {
                    selectedTab = 4
                    showFullMoreScreen = false
                    showProfileScreen = false
                    ownProfileViewModel.handleGitHubOAuthResult(
                        status = link.githubStatus,
                        message = link.githubMessage
                    )
                    Toast.makeText(
                        context,
                        if (link.githubStatus == "connected") {
                            "GitHub connected"
                        } else {
                            "GitHub connection failed"
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_POST -> {
                    // Navigate to specific post (like/share/post mention notifications)
                    link.postId?.let { postId ->
                        selectedTab = 0 // Feed tab
                        deepLinkPostId = postId
                        deepLinkPostShouldOpenComments = false
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_POST_COMMENTS -> {
                    // Navigate directly to the discussion for comment/reply notifications.
                    link.postId?.let { postId ->
                        selectedTab = 0 // Feed tab
                        deepLinkPostId = postId
                        deepLinkPostShouldOpenComments = true
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_REEL -> {
                    // Navigate to specific reel
                    link.reelId?.let { reelId ->
                        selectedTab = 0 // Feed tab
                        deepLinkReelId = reelId
                        deepLinkReelCommentId = link.commentId
                        deepLinkReelParentCommentId = link.parentCommentId
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_CONNECTIONS -> {
                    // Navigate to connections screen
                    selectedTab = 1 // Find People tab (connections)
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_FIND_PEOPLE -> {
                    selectedTab = 1 // Find People tab
                    showFullMoreScreen = false
                    link.userId?.takeIf { it.isNotBlank() }?.let { matchedUserId ->
                        pendingSmartMatchDeepLinkUserId = matchedUserId
                        viewingProfileUserId = matchedUserId
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_STREAK -> {
                    showStreakDetailsScreen = true
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_ENGAGEMENT -> {
                    // Show engagement/rewards
                    selectedTab = 0 // Feed tab
                }
                "crossed_paths" -> {
                    selectedTab = 3
                    showFullMoreScreen = true
                    crossedPathsRequestKey += 1
                }
            }
            onDeepLinkConsumed()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != 2 && isInChatThread) {
            isInChatThread = false
        }
        if (selectedTab != 3 && showFullMoreScreen) {
            showFullMoreScreen = false
        }
    }

    // Find/Profile/Reels state shared across tab switches so those sections stay warm.
    val findPeopleViewModel: FindPeopleViewModel = viewModel(
        key = "find-people",
        factory = FindPeopleViewModel.Factory(context)
    )
    val rewardsState by findPeopleViewModel.uiState.collectAsState()
    LaunchedEffect(pendingSmartMatchDeepLinkUserId) {
        pendingSmartMatchDeepLinkUserId ?: return@LaunchedEffect
        findPeopleViewModel.selectTab(FindPeopleTab.SMART_MATCHES)
        findPeopleViewModel.setSmartMatchFilter(SmartMatchFilter.ALL)
        findPeopleViewModel.loadSmartMatches(forceRefresh = true)
        pendingSmartMatchDeepLinkUserId = null
    }
    val premiumRefreshSignal by PremiumCheckoutManager.refreshSignal.collectAsState()
    val rewardCardsViewModel: RewardCardsViewModel = viewModel(factory = RewardCardsViewModel.Factory(context))
    val rewardCardsState by rewardCardsViewModel.uiState.collectAsState()

    // Reels state
    val reelsViewModel: ReelsViewModel = viewModel(factory = ReelsViewModel.Factory(context))
    val reelsState by reelsViewModel.uiState.collectAsState()
    var showReelCreateSheet by rememberSaveable { mutableStateOf(false) }
    var reopenReelDraftLibrary by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewingProfileUserId, profileOpenedFromReels, reelsState.isViewerOpen) {
        if (profileOpenedFromReels && viewingProfileUserId == null && reelsState.isViewerOpen) {
            reelsViewModel.resumePlayback(reelsState.currentReelIndex)
            profileOpenedFromReels = false
        }
    }
    val openReelCreateSheet: () -> Unit = {
        reelsViewModel.pausePlayback(false)
        reelsViewModel.clearReelUploadState()
        reopenReelDraftLibrary = false
        showReelCreateSheet = true
    }
    val closeReelCreateSheet: () -> Unit = {
        showReelCreateSheet = false
        reopenReelDraftLibrary = false
        reelsViewModel.clearReelUploadState()
        if (reelsState.isViewerOpen) {
            reelsViewModel.resumePlayback(reelsState.currentReelIndex)
        }
    }
    LaunchedEffect(showReelsOnHome, uiState.isLoggedIn) {
        if (showReelsOnHome && uiState.isLoggedIn) {
            reelsViewModel.loadPreviewReels()
        }
    }

    // Retention features state (Weekly Goals, Leaderboard, Session Summary)
    val retentionViewModel: RetentionViewModel = viewModel(factory = RetentionViewModel.Factory(context))
    val retentionState by retentionViewModel.uiState.collectAsState()

    // Chat state for unread message indicator
    val chatViewModel: ChatViewModel = viewModel(
        key = "chat:$sessionUserKey",
        factory = ChatViewModel.Factory(context)
    )
    val chatState by chatViewModel.uiState.collectAsState()
    LaunchedEffect(uiState.isLoggedIn, uiState.currentUserId, chatViewModel) {
        chatViewModel.onSessionChanged(if (uiState.isLoggedIn) uiState.currentUserId else null)
    }
    LaunchedEffect(viewingProfileUserId, uiState.isLoggedIn, uiState.currentUserId) {
        val profileUserId = viewingProfileUserId ?: return@LaunchedEffect
        if (!uiState.isLoggedIn || profileUserId == uiState.currentUserId) return@LaunchedEffect

        val cachedConversation = chatState.conversations.firstOrNull { it.otherParticipant.id == profileUserId }
        if (cachedConversation?.lastMessage != null) {
            profileConversationHasMessagesByUserId =
                profileConversationHasMessagesByUserId + (profileUserId to true)
            return@LaunchedEffect
        }

        ApiClient.getConversationStatusWithUser(context, profileUserId)
            .onSuccess { status ->
                profileConversationHasMessagesByUserId =
                    profileConversationHasMessagesByUserId + (profileUserId to status.hasMessages)
            }
    }
    val agentViewModel: AgentViewModel = viewModel(factory = AgentViewModel.Factory(context))
    val agentState by agentViewModel.uiState.collectAsState()
    val canUseAgent = uiState.currentUser?.canUseAgent == true
    val canAccessProfileCustomization = uiState.currentUser?.canAccessProfileCustomization == true
    val canOpenAgent = uiState.isLoggedIn
    val isAgentVoiceLive =
        agentState.isVoiceSessionConnecting ||
            agentState.isRecordingVoice ||
            agentState.isVoiceListening ||
            agentState.isVoiceThinking ||
            agentState.isPlayingAudio
    val visibleInlinePeople =
        agentState.activeInlineResults?.visiblePeople(agentState.dismissedInlineResultIds).orEmpty()
    val visibleInlinePeopleIds = visibleInlinePeople.map { it.id }
    val agentSheetAnimationDuration = if (reduceAnimations) 0 else 220
    val agentSheetAlpha by animateFloatAsState(
        targetValue = if (minimizeAgentSheetForVoice) 0f else 1f,
        animationSpec = tween(durationMillis = agentSheetAnimationDuration, easing = FastOutSlowInEasing),
        label = "agentSheetAlpha"
    )

    val agentSurface = when {
        showTalentEngineScreen -> "talent_engine"
        showGrowthHubScreen -> "growth_hub"
        showNotificationsInbox -> "notifications"
        showProfileViewersScreen -> "profile_views"
        showHackathonBoardScreen -> "hackathons"
        showMessagesScreen || isInChatThread -> "chat"
        showGroupsScreen || selectedGroupId != null || selectedGroupInviteCode != null || showGroupChat || showGroupSettingsScreen -> "groups"
        viewingProfileUserId != null || selectedTab == 4 -> "profile"
        selectedTab == 1 -> "find_people"
        else -> "feed"
    }
    val agentSurfaceContext = remember(
        agentSurface,
        selectedTab,
        viewingProfileUserId,
        selectedGroupId,
        openChatWithUserId,
        chatState.selectedConversation?.id,
        uiState.currentUser?.id,
        visibleInlinePeopleIds,
        agentState.activeInlineResults?.source
    ) {
        buildMap<String, String> {
            put("surface", agentSurface)
            put("selectedTab", selectedTab.toString())
            uiState.currentUser?.id?.let { put("currentUserId", it) }
            viewingProfileUserId?.let { put("viewingProfileUserId", it) }
            selectedGroupId?.let { put("selectedGroupId", it) }
            openChatWithUserId?.let { put("openChatWithUserId", it) }
            chatState.selectedConversation?.id?.let { put("conversationId", it) }
            if (visibleInlinePeopleIds.isNotEmpty()) {
                put("inlineResultUserIds", visibleInlinePeopleIds.joinToString(","))
            }
            agentState.activeInlineResults?.source
                ?.takeIf { it.isNotBlank() }
                ?.let { put("inlineResultSource", it) }
        }
    }
    val shouldHideInlineResultsOverlay =
        viewingProfileUserId != null ||
            selectedTab == 4 ||
            showMessagesScreen ||
            isInChatThread ||
            showGroupsScreen ||
            selectedGroupId != null ||
            selectedGroupInviteCode != null ||
            showGroupChat ||
            showGroupSettingsScreen ||
            showNotificationsInbox ||
            showProfileViewersScreen ||
            showHackathonBoardScreen ||
            showTalentEngineScreen ||
            showGrowthHubScreen
    LaunchedEffect(selectedTab, uiState.isLoggedIn) {
        if (uiState.isLoggedIn && selectedTab == 3) {
            findPeopleViewModel.loadPendingConnectionRequests()
        }
    }

    LaunchedEffect(shouldHideInlineResultsOverlay, agentState.activeInlineResults) {
        if (shouldHideInlineResultsOverlay && agentState.activeInlineResults != null) {
            agentViewModel.dismissInlineResults()
        }
    }

    // Own profile should paint from memory/disk cache immediately, then refresh in the background.
    LaunchedEffect(selectedTab, uiState.isLoggedIn) {
        if (uiState.isLoggedIn && selectedTab == 4) {
            ownProfileViewModel.loadProfile(userId = null, forceRefresh = false)
        }
    }

    LaunchedEffect(showConnectionRequestsScreen, uiState.isLoggedIn) {
        if (uiState.isLoggedIn && showConnectionRequestsScreen) {
            findPeopleViewModel.loadPendingConnectionRequests(forceRefresh = true)
        }
    }

    LaunchedEffect(premiumRefreshSignal, uiState.isLoggedIn) {
        if (uiState.isLoggedIn && premiumRefreshSignal != 0L) {
            viewModel.refreshCurrentUser()
            findPeopleViewModel.refreshDiscoveryPremiumState()
            ownProfileViewModel.loadProfile(userId = null, forceRefresh = true)
        }
    }

    LaunchedEffect(
        canAccessProfileCustomization,
        showProfileCustomizationsScreen
    ) {
        if (!canAccessProfileCustomization) {
            if (showProfileCustomizationsScreen) {
                showProfileCustomizationsScreen = false
            }
        }
    }

    // Stagger background prefetches so cold start is not a thundering herd competing with
    // FeedViewModel (feed + user + socket). Reels metadata + poster warmup runs last.
    LaunchedEffect(uiState.isLoggedIn) {
        if (!uiState.isLoggedIn) {
            rewardCardsViewModel.maybeLoadOnAppOpen(false)
            return@LaunchedEffect
        }
        delay(120)
        chatViewModel.preloadChats()
        delay(120)
        ownProfileViewModel.prefetchOwnProfile()
        delay(120)
        findPeopleViewModel.prefetchInitialData()
        delay(280)
        retentionViewModel.ensureRetentionLoaded()
        delay(220)
        reelsViewModel.prefetchAppStartData(includeHomePreview = showReelsOnHome)
        rewardCardsViewModel.maybeLoadOnAppOpen(true)
    }

    LaunchedEffect(showAgentSheet, agentSurface, uiState.isLoggedIn, canUseAgent) {
        if (showAgentSheet && uiState.isLoggedIn && canUseAgent) {
            agentViewModel.ensureSession(surface = agentSurface)
        }
    }

    LaunchedEffect(uiState.isLoggedIn, agentSurface, agentSurfaceContext, canUseAgent) {
        if (uiState.isLoggedIn && canUseAgent) {
            agentViewModel.ensureSession(surface = agentSurface)
            agentViewModel.syncSurface(
                surface = agentSurface,
                surfaceContext = agentSurfaceContext
            )
        }
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (!uiState.isLoggedIn) {
            autoMinimizedAgentSheetForActiveVoice = false
            minimizeAgentSheetForVoice = false
            showAgentSheet = false
            agentViewModel.dismissInlineResults()
        }
    }

    val openAgentPanel: () -> Unit = {
        if (!uiState.isLoggedIn) {
            Toast.makeText(context, "Please sign in to use vormex.", Toast.LENGTH_SHORT).show()
        } else {
            showFullMoreScreen = false
            minimizeAgentSheetForVoice = false
            showAgentSheet = true
        }
    }

    LaunchedEffect(isAgentVoiceLive) {
        if (!isAgentVoiceLive) {
            autoMinimizedAgentSheetForActiveVoice = false
            minimizeAgentSheetForVoice = false
        }
    }

    LaunchedEffect(showAgentSheet, isAgentVoiceLive, agentState.activeInlineResults, reduceAnimations) {
        if (
            showAgentSheet &&
            isAgentVoiceLive &&
            agentState.activeInlineResults == null &&
            !autoMinimizedAgentSheetForActiveVoice
        ) {
            autoMinimizedAgentSheetForActiveVoice = true
            minimizeAgentSheetForVoice = true
            if (!reduceAnimations) {
                delay(agentSheetAnimationDuration.toLong())
            }
            showAgentSheet = false
            minimizeAgentSheetForVoice = false
        }
    }

    LaunchedEffect(agentState.pendingUiIntents, reduceAnimations) {
        val pendingIntents = agentState.pendingUiIntents
        if (pendingIntents.isEmpty()) return@LaunchedEffect
        val previewDelayMs = if (reduceAnimations) 0L else 170L
        val transitionDelayMs = if (reduceAnimations) 0L else 140L

        suspend fun resetOverlayNavigation() {
            val hadOverlay =
                showGrowthHubScreen ||
                    showTalentEngineScreen ||
                    showNotificationsInbox ||
                    showProfileViewersScreen ||
                    showMessagesScreen ||
                    isInChatThread ||
                    openChatWithUserId != null ||
                    deepLinkConversationId != null ||
                    showGroupsScreen ||
                    showHackathonBoardScreen ||
                    selectedGroupId != null ||
                    selectedGroupInviteCode != null ||
                    showGroupChat ||
                    showGroupSettingsScreen ||
                    viewingProfileUserId != null
            showGrowthHubScreen = false
            showTalentEngineScreen = false
            showNotificationsInbox = false
            showProfileViewersScreen = false
            showMessagesScreen = false
            isInChatThread = false
            openChatWithUserId = null
            openChatInitialDraft = null
            deepLinkConversationId = null
            showGroupsScreen = false
            showHackathonBoardScreen = false
            selectedGroupId = null
            selectedGroupInviteCode = null
            showGroupChat = false
            showGroupSettingsScreen = false
            viewingProfileUserId = null
            if (hadOverlay && transitionDelayMs > 0) {
                delay(transitionDelayMs)
            }
        }

        pendingIntents.forEach { intent ->
            agentViewModel.previewUiIntent(intent)
            if (previewDelayMs > 0) {
                delay(previewDelayMs)
            }
            when (intent.type) {
                "switch_tab" -> {
                    resetOverlayNavigation()
                    when (intent.tab?.lowercase()) {
                        "feed", "home" -> selectedTab = 0
                        "find", "find_people", "network" -> selectedTab = 1
                        "post", "create_post" -> selectedTab = 2
                        "more" -> selectedTab = 3
                        "profile" -> selectedTab = 4
                        "groups" -> {
                            selectedTab = 3
                            showGroupsScreen = true
                        }
                    }
                }
                "open_profile" -> {
                    resetOverlayNavigation()
                    selectedTab = 4
                    intent.userId?.let { viewingProfileUserId = it }
                }
                "open_chat" -> {
                    resetOverlayNavigation()
                    showMessagesScreen = true
                    intent.conversationId?.let { deepLinkConversationId = it }
                    if (intent.conversationId.isNullOrBlank()) {
                        openChatInitialDraft = null
                        openChatWithUserId = intent.userId
                    }
                }
                "open_group" -> {
                    resetOverlayNavigation()
                    selectedTab = 3
                    showGroupsScreen = true
                    intent.groupId?.let { selectedGroupId = it }
                }
                "open_groups" -> {
                    resetOverlayNavigation()
                    selectedTab = 3
                    showGroupsScreen = true
                }
                "open_notifications" -> {
                    resetOverlayNavigation()
                    showNotificationsInbox = true
                }
                "open_growth_task" -> {
                    resetOverlayNavigation()
                    showGrowthHubScreen = true
                }
                "show_match_stack" -> {
                    resetOverlayNavigation()
                    selectedTab = 1
                }
            }
            if (transitionDelayMs > 0) {
                delay(transitionDelayMs / 2)
            }
        }

        agentViewModel.consumeUiIntents()
    }

    CompositionLocalProvider(LocalVormexAppearance provides appearance) {
    Box(Modifier.fillMaxSize()) {
        // Background based on theme
        if (isGlassTheme) {
            GlassBackgroundLayer(
                modifier = Modifier
                    .layerBackdrop(backdrop)
                    .fillMaxSize(),
                backgroundKey = glassBackgroundKey,
                accentColor = accentColor,
                motionStyleKey = glassMotionStyleKey,
                reduceAnimations = reduceAnimations
            )
        } else {
            // Solid color background for White/Dark themes
            Box(
                Modifier
                    .layerBackdrop(backdrop)
                    .fillMaxSize()
                    .background(appearance.backgroundColor)
            )
        }

        if (!uiState.isLoggedIn && !uiState.isRestoringSession) {
            val authScreenError = if (uiState.pendingVerificationEmail == null) uiState.error else null
            when (uiState.authScreen) {
                AuthScreen.LOGIN -> LoginScreen(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isLoading = uiState.isLoading,
                    isGoogleLoading = uiState.isGoogleLoading,
                    error = authScreenError,
                    savedAccountsCount = uiState.savedAccounts.size,
                    onLogin = { email, password -> viewModel.login(email, password) },
                    onGoogleSignIn = startGoogleSignIn,
                    onForgotPassword = { email ->
                        appScope.launch {
                            ApiClient.forgotPassword(email)
                                .onSuccess { response ->
                                    Toast.makeText(
                                        context,
                                        response.message.ifBlank {
                                            "Password reset link sent. Check your email."
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "Could not send reset email",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    },
                    onSignUpClick = { viewModel.showSignUp() },
                    onOpenSavedAccounts = { showAccountSwitcherDialog = true },
                    onClearError = { viewModel.clearError() }
                )
                AuthScreen.SIGNUP,
                AuthScreen.EMAIL_VERIFICATION -> SignUpScreen(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isLoading = uiState.isLoading,
                    isGoogleLoading = uiState.isGoogleLoading,
                    error = authScreenError,
                    savedAccountsCount = uiState.savedAccounts.size,
                    onSignUp = { email, password, name, username -> viewModel.register(email, password, name, username) },
                    onGoogleSignIn = startGoogleSignIn,
                    onLoginClick = { viewModel.showLogin() },
                    onOpenSavedAccounts = { showAccountSwitcherDialog = true },
                    onClearError = { viewModel.clearError() }
                )
            }
        } else if (!uiState.isRestoringSession && uiState.showOnboarding) {
            // Show onboarding wizard for new users
            ProfileSetupWizard(
                onComplete = {
                    viewModel.completeOnboarding()
                },
                onSkip = {
                    viewModel.skipOnboarding()
                }
            )
        } else {
            // Content
            Column(
                Modifier
                    .fillMaxSize()
                    .then(
                        // Only add status bar padding when NOT on profile tab (to allow banner to extend to top)
                        if (selectedTab != 4 && !showCrossedPathsFullScreen) Modifier.statusBarsPadding() else Modifier
                    )
                    .then(if (!showCrossedPathsFullScreen) Modifier.displayCutoutPadding() else Modifier)
            ) {
                // Top bar (hidden when in chat thread or on profile tab)
                if (!isInChatThread && selectedTab != 4 && !showCrossedPathsFullScreen) {
                    LinkedInTopBar(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        userInitials = uiState.currentUser?.name?.firstOrNull()?.toString() ?: "U",
                        loginStreak = uiState.loginStreak,
                        isStreakAtRisk = uiState.isStreakAtRisk,
                        hasUnreadNotifications = notificationUnreadCount > 0,
                        unreadNotificationCount = notificationUnreadCount,
                        hasUnreadMessages = chatState.unreadCount > 0,
                        onStreakClick = { showStreakDetailsScreen = true },
                        onNotificationsClick = {
                            showNotificationsInbox = true
                        },
                        onMessagesClick = {
                            openChatWithUserId = null
                            showMessagesScreen = true
                        }
                    )
                }

                // Main content based on selected tab
                Box(
                    Modifier
                        .fillMaxSize()
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Handle deep link to specific post (from notification)
                            LaunchedEffect(deepLinkPostId, deepLinkPostShouldOpenComments) {
                                deepLinkPostId?.let { postId ->
                                    if (deepLinkPostShouldOpenComments) {
                                        selectedPostForComments = postId
                                        viewModel.loadComments(postId)
                                        showCommentsSheet = true
                                    } else {
                                        openPostFromLink(postId)
                                    }
                                    deepLinkPostId = null // Clear after handling
                                    deepLinkPostShouldOpenComments = false
                                }
                            }

                            // Handle deep link to specific reel (from notification)
                            LaunchedEffect(deepLinkReelId, deepLinkReelCommentId, deepLinkReelParentCommentId) {
                                deepLinkReelId?.let { reelId ->
                                    // Open the reel viewer
                                    reelsViewModel.loadReelById(reelId)
                                    if (!deepLinkReelCommentId.isNullOrBlank()) {
                                        reelsViewModel.openComments(
                                            reelId = reelId,
                                            highlightCommentId = deepLinkReelCommentId,
                                            parentCommentId = deepLinkReelParentCommentId
                                        )
                                    }
                                    deepLinkReelId = null // Clear after handling
                                    deepLinkReelCommentId = null
                                    deepLinkReelParentCommentId = null
                                }
                            }

                            Box(Modifier.fillMaxSize()) {
                                FeedScreen(
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    glassBackgroundKey = glassBackgroundKey,
                                    posts = uiState.posts,
                                    managedAdPlacements = uiState.feedAdPlacements,
                                    modulePlacements = uiState.modulePlacements,
                                    recommendationSessionId = uiState.recommendationSessionId,
                                    recommendationRequestId = uiState.recommendationRequestId,
                                    storyGroups = uiState.storyGroups,
                                    // Reels data
                                    showReelsOnHome = showReelsOnHome,
                                    reels = if (showReelsOnHome) reelsState.previewReels else emptyList(),
                                    isLoadingReels = showReelsOnHome && reelsState.isLoadingPreview,
                                    onReelClick = { index ->
                                        reelsViewModel.openPreviewReelsViewer(index)
                                    },
                                    onSeeAllReelsClick = {
                                        reelsViewModel.loadAndOpenReels()
                                    },
                                    onCreateReelClick = openReelCreateSheet,
                                isLoading = uiState.isLoading,
                                isRefreshing = uiState.isRefreshingFeed,
                                error = uiState.error,
                                currentUserInitials = uiState.currentUser?.name?.split(" ")?.mapNotNull { it.firstOrNull()?.uppercase() }?.take(2)?.joinToString("") ?: "U",
                                currentUserProfileImage = uiState.currentUser?.profileImage,
                                currentUserName = uiState.currentUser?.name ?: "You",
                                currentUserId = uiState.currentUserId,
                                isCurrentUserPremium = uiState.currentUser?.isPremium == true,
                                isLightTheme = isLightTheme,
                                // Streak data (Duolingo Effect)
                                connectionStreak = uiState.connectionStreak,
                                loginStreak = uiState.loginStreak,
                                isStreakAtRisk = uiState.isStreakAtRisk,
                                showStreakReminder = uiState.showStreakReminder,
                                showLoginStreakBadge = uiState.showLoginStreakBadge,
                                onDismissStreakReminder = { viewModel.dismissStreakReminder() },
                                onDismissLoginStreakBadge = { viewModel.dismissLoginStreakBadge() },
                                onNavigateToFindPeople = {
                                    viewModel.clearError() // Clear any error when navigating
                                    selectedTab = 1
                                },
                                onRefresh = {
                                    viewModel.loadFeed(forceRefresh = true)
                                    viewModel.loadStories(forceRefresh = true)
                                    if (showReelsOnHome) {
                                        reelsViewModel.loadPreviewReels(forceRefresh = true)
                                    }
                                    findPeopleViewModel.refreshAllVariableRewards()
                                    retentionViewModel.loadAllRetentionData(forceRefresh = true)
                                },
                                onLike = { postId -> viewModel.toggleLike(postId) },
                                onComment = { postId ->
                                    selectedPostForComments = postId
                                    viewModel.loadComments(postId)
                                    showCommentsSheet = true
                                },
                                onShare = { postId ->
                                    viewModel.showShareModal(postId)
                                },
                                onVotePoll = { postId, optionId ->
                                    viewModel.votePoll(postId, optionId)
                                },
                                onProfileClick = { userId ->
                                    viewingProfileUserId = userId
                                },
                                onMenuAction = handlePostMenuAction,
                                onNotInterested = { postId -> viewModel.markPostNotInterested(postId) },
                                onBoostPost = { postId -> postIdForBoost = postId },
                                showFeedbackUndo = uiState.lastRecommendationFeedbackPost != null,
                                onUndoFeedback = { viewModel.undoLastRecommendationFeedback() },
                                onManagedAdImpression = { ad -> viewModel.trackManagedAdImpression(ad) },
                                onManagedAdClick = { ad -> viewModel.trackManagedAdClick(ad) },
                                onStoryClick = { groupIndex ->
                                    viewModel.openStoryViewer(groupIndex)
                                },
                                onAddStoryClick = {
                                    viewModel.openStoryCreator()
                                },
                                onMyStoryClick = {
                                    // Find own story group index and open viewer
                                    val myStoryIndex = uiState.storyGroups.indexOfFirst { it.isOwnStory }
                                    if (myStoryIndex >= 0) {
                                        viewModel.openStoryViewer(myStoryIndex)
                                    }
                                },
                                // Onboarding prompt - show for users who skipped but didn't complete
                                showOnboarding = !uiState.onboardingCompleted && !uiState.showOnboarding,
                                onNavigateToOnboarding = { viewModel.showOnboardingAgain() },
                                // Retention features data
                                retentionState = retentionState,
                                onWeeklyGoalsClick = { showWeeklyGoalsScreen = true },
                                onStreakDetailsClick = { showStreakDetailsScreen = true },
                                onTopNetworkersClick = { showTopNetworkersScreen = true },
                                reduceAnimations = reduceAnimations,
                                hasMore = uiState.hasMore,
                                isLoadingMore = uiState.isLoadingMore,
                                onLoadMore = { viewModel.loadMorePosts() }
                            )

                            // Share modal
                            if (uiState.showShareModal && uiState.sharePostId != null) {
                                SharePostModal(
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isLightTheme = isLightTheme,
                                    shareTargets = uiState.shareTargets,
                                    isLoading = uiState.isLoadingShareTargets,
                                    isSharing = uiState.isSharing,
                                    error = uiState.shareError,
                                    onDismiss = { viewModel.hideShareModal() },
                                    onCopyLink = {
                                        uiState.sharePostId?.let { postId ->
                                            viewModel.copyPostLink(postId)
                                        }
                                    },
                                    onShareToTargets = { targetIds, message ->
                                        viewModel.sharePostInApp(targetIds, message)
                                    },
                                    onClearError = { viewModel.clearShareError() },
                                    onShareAnimation = { shareAnimationTrigger++ }
                                )
                            }

                            // Story Viewer Dialog
                            if (uiState.isStoryViewerOpen && uiState.storyGroups.isNotEmpty()) {
                                StoryViewerDialog(
                                    storyGroups = uiState.storyGroups,
                                    accentColor = accentColor,
                                    initialGroupIndex = uiState.currentStoryGroupIndex,
                                    initialStoryIndex = uiState.currentStoryIndex,
                                    onDismiss = { viewModel.closeStoryViewer() },
                                    onStoryViewed = { storyId -> viewModel.viewStory(storyId) },
                                    onReact = { storyId, reaction -> viewModel.reactToStory(storyId, reaction) },
                                    onReply = { storyId, content -> viewModel.replyToStory(storyId, content) },
                                    onGetViewers = { storyId, callback ->
                                        viewModel.getStoryViewers(storyId, callback)
                                    },
                                    onAddStory = {
                                        viewModel.closeStoryViewer()
                                        viewModel.openStoryCreator()
                                    }
                                )
                            }

                            // Story Creator Dialog
                            if (uiState.isStoryCreatorOpen) {
                                StoryCreatorDialog(
                                    onDismiss = { viewModel.closeStoryCreator() },
                                    onCreateStory = { mediaType, mediaBytes, textContent, backgroundColor, category, visibility, linkUrl, linkTitle ->
                                        viewModel.createStory(
                                            mediaType = mediaType,
                                            mediaBytes = mediaBytes,
                                            textContent = textContent,
                                            backgroundColor = backgroundColor,
                                            category = category,
                                            visibility = visibility,
                                            linkUrl = linkUrl,
                                            linkTitle = linkTitle,
                                            onSuccess = { viewModel.closeStoryCreator() }
                                        )
                                    },
                                    isCreating = uiState.isCreatingStory
                                )
                            }

                                val activeUploadProgress =
                                    if (reelsState.uploadProgress.status != UploadStatus.IDLE) {
                                        reelsState.uploadProgress
                                    } else {
                                        uiState.uploadProgress
                                    }

                                // Upload Progress Bar (Instagram-style)
                                GlassUploadProgressBar(
                                    uploadProgress = activeUploadProgress,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    onDismiss = {
                                        if (reelsState.uploadProgress.status != UploadStatus.IDLE) {
                                            reelsViewModel.dismissReelUploadProgress()
                                        } else {
                                            viewModel.dismissUploadError()
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 8.dp)
                                        .statusBarsPadding()
                                )

                                // Trending Banner (auto-hide after 2 seconds)
                                TrendingBannerAutoHide(
                                    isTrending = rewardsState.isTrending,
                                    rank = rewardsState.trendingRank,
                                    viewsToday = rewardsState.trendingViewsToday,
                                    message = rewardsState.trendingMessage,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 8.dp)
                                )
                            } // Close the Box wrapping FeedScreen
                        }
                        1 -> FindPeopleScreenNew(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            findPeopleViewModel = findPeopleViewModel,
                            onNavigateToProfile = { userId -> viewingProfileUserId = userId },
                            reduceAnimations = reduceAnimations
                        )
                        2 -> com.kyant.backdrop.catalog.linkedin.posts.CreatePostScreen(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isCreating = uiState.isCreatingPost,
                            error = uiState.error,
                            userName = listOf(
                                uiState.currentUser?.name,
                                uiState.currentUser?.username,
                                uiState.currentUser?.email?.substringBefore("@")
                            ).firstOrNull { !it.isNullOrBlank() } ?: "User",
                            userUsername = uiState.currentUser?.username,
                            userAvatar = uiState.currentUser?.profileImage,
                            mentionSearchResults = uiState.mentionSearchResults,
                            isSearchingMentions = uiState.isSearchingMentions,
                            onCreateTextPost = { content, visibility, mentions, collaboratorIds, defaultVideoId ->
                                viewModel.createTextPost(content, visibility, mentions, defaultVideoId, collaboratorIds) { selectedTab = 0 }
                            },
                            onCreateImagePost = { content, visibility, images, mentions, collaboratorIds, defaultVideoId ->
                                viewModel.createImagePost(content, visibility, images, mentions, defaultVideoId, collaboratorIds) { selectedTab = 0 }
                            },
                            onCreateVideoPost = { content, visibility, videoBytes, videoFilename, mentions, collaboratorIds, defaultVideoId ->
                                viewModel.createVideoPost(content, visibility, videoBytes, videoFilename, mentions, defaultVideoId, collaboratorIds) { selectedTab = 0 }
                            },
                            onCreateLinkPost = { linkUrl, content, visibility, mentions, collaboratorIds, defaultVideoId ->
                                viewModel.createLinkPost(linkUrl, content, visibility, mentions, defaultVideoId, collaboratorIds) { selectedTab = 0 }
                            },
                            onCreatePollPost = { pollOptions, pollDurationHours, content, visibility, showResultsBeforeVote, mentions, collaboratorIds, defaultVideoId ->
                                viewModel.createPollPost(pollOptions, pollDurationHours, content, visibility, showResultsBeforeVote, mentions, defaultVideoId, collaboratorIds) { selectedTab = 0 }
                            },
                            onCreateArticlePost = { articleTitle, content, visibility, coverImage, articleTags, mentions, collaboratorIds, defaultVideoId ->
                                viewModel.createArticlePost(articleTitle, content, visibility, coverImage, articleTags, mentions, defaultVideoId, collaboratorIds) { selectedTab = 0 }
                            },
                            onCreateCelebrationPost = { celebrationType, content, visibility, mentions, collaboratorIds, celebrationGif, defaultVideoId ->
                                viewModel.createCelebrationPost(
                                    celebrationType,
                                    content,
                                    visibility,
                                    mentions,
                                    celebrationGif,
                                    defaultVideoId,
                                    collaboratorIds
                                ) { selectedTab = 0 }
                            },
                            onSearchMentions = { query -> viewModel.searchMentions(query) },
                            onClearMentionSearch = { viewModel.clearMentionSearch() },
                            onClearError = { viewModel.clearError() },
                            onPostCreated = { selectedTab = 0 }
                        )
                        3 -> MoreScreen(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isGlassTheme = isGlassTheme,
                            retentionState = retentionState,
                            currentUser = uiState.currentUser,
                            connectionRequests = rewardsState.pendingConnectionRequests,
                            isLoadingConnectionRequests = rewardsState.isLoadingPendingConnectionRequests,
                            connectionRequestsError = rewardsState.pendingConnectionRequestsError,
                            hasPendingGroupMessages = hasPendingGroupMessages,
                            showFullMoreScreen = showFullMoreScreen,
                            premiumDetailsRequestKey = premiumDetailsRequestKey,
                            crossedPathsRequestKey = crossedPathsRequestKey,
                            onCrossedPathsVisibilityChanged = { showCrossedPathsFullScreen = it },
                            quickHubAnimationKey = moreHubAnimationKey,
                            onOpenFullMoreScreen = { showFullMoreScreen = true },
                            onCloseFullMoreScreen = { showFullMoreScreen = false },
                            onNavigateToProfile = { selectedTab = 4 },
                            onNavigateToUserProfile = { userId -> viewingProfileUserId = userId },
                            onNavigateToConnectionRequests = { showConnectionRequestsScreen = true },
                            onNavigateToProfileCustomizations = {
                                if (canAccessProfileCustomization) {
                                    showProfileCustomizationsScreen = true
                                } else {
                                    openPremiumDetails()
                                }
                            },
                            onNavigateToGroups = { showGroupsScreen = true },
                            onNavigateToCircles = { showCirclesScreen = true },
                            onNavigateToReels = {
                                reelsViewModel.loadAndOpenReels()
                            },
                            onNavigateToWeeklyGoals = { showWeeklyGoalsScreen = true },
                            onNavigateToStreakDetails = { showStreakDetailsScreen = true },
                            onNavigateToTopNetworkers = { showTopNetworkersScreen = true },
                            onNavigateToOnboarding = { showOnboardingScreen = true },
                            onNavigateToSavedPosts = { showSavedPostsScreen = true },
                            onNavigateToProfileInsights = { showProfileViewersScreen = true },
                            onNavigateToGrowthHub = { showGrowthHubScreen = true },
                            onNavigateToGames = { showGamesScreen = true },
                            onNavigateToTalentEngine = { showTalentEngineScreen = true },
                            onNavigateToSkillPassport = { showSkillPassportScreen = true },
                            onNavigateToSkillSwap = {
                                skillSwapInitialTab = "discover"
                                showSkillSwapScreen = true
                            },
                            onNavigateToHackathons = { showHackathonBoardScreen = true },
                            onOpenAiChat = openAgentPanel,
                            onOpenAgent = openAgentPanel,

                            onNavigateToNotificationSettings = { showNotificationSettingsScreen = true },
                            onNavigateToPrivacySettings = { showPrivacySettingsScreen = true },
                            onNavigateToIdentitySafety = {
                                context.startActivity(Intent(context, IdentitySafetyActivity::class.java))
                            },
                            onNavigateToAppearanceSettings = { showAppearanceSettingsScreen = true },
                            onNavigateToHelp = { showHelpScreen = true },
                            onNavigateToInviteFriends = { showInviteFriendsScreen = true },
                            onNavigateToAbout = { showAboutScreen = true },
                            onNavigateToContact = { showContactScreen = true },
                            onSwitchAccount = { showAccountSwitcherDialog = true },
                            onLogout = { showAccountExitDialog(switchAccount = false) }
                        )
                        4 -> {
                            // Use the new comprehensive ProfileScreen with its own ViewModel
                            ProfileScreen(
                                userId = null, // null means current user's profile
                                backdrop = backdrop,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                profileViewModel = ownProfileViewModel,
                                onNavigateBack = { selectedTab = 0 },
                                onEditProfile = {},
                                onOpenProfile = { nextUserId -> viewingProfileUserId = nextUserId },
                                onOpenFeedItem = { item ->
                                    when (item.entityType?.lowercase()) {
                                        "reel" -> {
                                            selectedTab = 0
                                            reelsViewModel.loadReelById(item.id)
                                        }
                                        "post" -> {
                                            selectedTab = 0
                                            viewModel.openPostDetail(item.id)
                                        }
                                    }
                                }
                            )
                        }
                    }

                    selectedPostForComments?.let { selectedPostId ->
                        if (showCommentsSheet) {
                            com.kyant.backdrop.catalog.linkedin.posts.CommentsBottomSheet(
                                backdrop = backdrop,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                isLightTheme = isLightTheme,
                                postId = selectedPostId,
                                comments = uiState.comments,
                                isLoading = uiState.isLoadingComments,
                                isLoadingMore = uiState.isLoadingMoreComments,
                                isSendingComment = uiState.isSubmittingComment,
                                hasMoreComments = uiState.hasMoreComments,
                                currentUserAvatar = uiState.currentUser?.profileImage,
                                currentUserName = uiState.currentUser?.name ?: "You",
                                mentionSearchResults = uiState.mentionSearchResults,
                                isSearchingMentions = uiState.isSearchingMentions,
                                error = uiState.commentsError,
                                onDismiss = {
                                    showCommentsSheet = false
                                    selectedPostForComments = null
                                    viewModel.clearComments()
                                },
                                onLoadMore = { viewModel.loadMoreComments() },
                                onSendComment = { content, parentId ->
                                    selectedPostForComments?.let { postId ->
                                        viewModel.submitComment(postId, content, parentId)
                                    }
                                },
                                onLikeComment = { commentId ->
                                    viewModel.toggleCommentLike(commentId)
                                },
                                onDeleteComment = { commentId ->
                                    viewModel.deleteComment(commentId)
                                },
                                onSearchMentions = { query ->
                                    viewModel.searchMentions(query)
                                },
                                onClearMentionSearch = {
                                    viewModel.clearMentionSearch()
                                },
                                onClearError = {
                                    viewModel.clearCommentsError()
                                },
                                onProfileClick = { userId ->
                                    showCommentsSheet = false
                                    selectedPostForComments = null
                                    viewModel.clearComments()
                                    viewingProfileUserId = userId
                                }
                            )
                        }
                    }
                }
            }

            // Bottom navigation - floating over content, hidden when in chat thread
            AnimatedVisibility(
                visible = !isInChatThread && !showCrossedPathsFullScreen,
                enter = fadeIn() + slideInHorizontally { it / 2 },
                exit = fadeOut() + slideOutHorizontally { it / 2 },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LiquidBottomTabs(
                        selectedTabIndex = { selectedTab },
                        onTabSelected = {
                            selectedTab = it
                            if (it == 3) {
                                showFullMoreScreen = false
                            }
                        },
                        backdrop = backdrop,
                        tabsCount = 5,
                        useGlassEffects = true,
                        lightTheme = isLightTheme,
                        accentColor = accentColor,
                        modifier = Modifier
                            .padding(horizontal = 36.dp)
                            .fillMaxWidth()
                    ) {
                        LiquidBottomTab(onClick = { selectedTab = 0 }) {
                            FooterHomeIcon(
                                color = contentColor,
                                size = footerIconSize
                            )
                            BasicText("Home", style = footerTextStyle)
                        }
                        LiquidBottomTab(onClick = {
                            viewModel.clearError() // Clear feed errors when switching tabs
                            selectedTab = 1
                        }) {
                            // Find tab - only show badge when streak is at risk
                            Box {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    FooterFindIcon(
                                        color = contentColor,
                                        size = footerIconSize
                                    )
                                    BasicText("Find", style = footerTextStyle)
                                }

                                // Only show badge when streak is AT RISK (not always)
                                if (uiState.isStreakAtRisk && uiState.connectionStreak > 0) {
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 8.dp, y = (-4).dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF6B6B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BasicText(
                                            "!",
                                            style = TextStyle(
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        LiquidBottomTab(onClick = { selectedTab = 2 }) {
                            FooterCreateIcon(
                                color = contentColor,
                                size = footerIconSize
                            )
                            BasicText("Post", style = footerTextStyle)
                        }
                        LiquidBottomTab(onClick = {
                            moreHubAnimationKey += 1
                            showFullMoreScreen = false
                            selectedTab = 3
                        }) {
                            Box {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    FooterMoreIcon(
                                        color = contentColor,
                                        size = footerIconSize
                                    )
                                    BasicText("More", style = footerTextStyle)
                                }

                                if (hasPendingGroupMessages) {
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 7.dp, y = (-3).dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(accentColor)
                                    )
                                }
                            }
                        }
                        LiquidBottomTab(
                            onClick = { selectedTab = 4 },
                            onLongClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                showAccountSwitcherDialog = true
                            },
                            onDoubleClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                showAccountSwitcherDialog = true
                            }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                FooterProfileIcon(
                                    color = contentColor,
                                    size = footerIconSize
                                )
                                BasicText("Profile", style = footerTextStyle)
                            }
                        }
                    } // End LiquidBottomTabs
                }
            } // End AnimatedVisibility

            val openInlineProfile: (String) -> Unit = { userId ->
                minimizeAgentSheetForVoice = false
                showAgentSheet = false
                showGrowthHubScreen = false
                showNotificationsInbox = false
                showMessagesScreen = false
                isInChatThread = false
                openChatWithUserId = null
                openChatInitialDraft = null
                deepLinkConversationId = null
                showGroupsScreen = false
                showHackathonBoardScreen = false
                selectedGroupId = null
                selectedGroupInviteCode = null
                showGroupChat = false
                showGroupSettingsScreen = false
                selectedTab = 4
                viewingProfileUserId = userId
            }
            fun openInlineChat(userId: String, initialDraft: String? = null) {
                minimizeAgentSheetForVoice = false
                showAgentSheet = false
                showGrowthHubScreen = false
                showNotificationsInbox = false
                showGroupsScreen = false
                showHackathonBoardScreen = false
                selectedGroupId = null
                selectedGroupInviteCode = null
                showGroupChat = false
                showGroupSettingsScreen = false
                viewingProfileUserId = null
                showMessagesScreen = true
                isInChatThread = false
                deepLinkConversationId = null
                openChatInitialDraft = initialDraft
                openChatWithUserId = userId
            }
            fun startAiConversationFromProfile(userId: String) {
                if (preparingConversationStarterForUserId != null) return
                appScope.launch {
                    preparingConversationStarterForUserId = userId
                    val fallbackDraft =
                        "Hey, I saw your profile and think there may be a useful overlap between what you're working on and what I'm exploring. I'd love to trade ideas and see if there is a small way we can help each other."
                    val starter = AgentApiService.getConversationStarters(
                        context = context,
                        otherUserId = userId,
                        goal = "Write one first message that makes the recipient feel the connection could be useful for collaboration, learning, or helping each other. Do not send a generic greeting."
                    ).getOrNull()
                        ?.firstOrNull { it.isNotBlank() }
                        ?.trim()
                        ?: fallbackDraft
                    preparingConversationStarterForUserId = null
                    if (profileOpenedFromReels && reelsState.isViewerOpen) {
                        profileOpenedFromReels = false
                        reelsViewModel.closeReelsViewer()
                    }
                    viewingProfileUserId = null
                    openInlineChat(userId, starter)
                }
            }
            val openInlineFind: () -> Unit = {
                agentViewModel.dismissInlineResults()
                minimizeAgentSheetForVoice = false
                showAgentSheet = false
                showGrowthHubScreen = false
                showNotificationsInbox = false
                showMessagesScreen = false
                isInChatThread = false
                openChatWithUserId = null
                openChatInitialDraft = null
                deepLinkConversationId = null
                showGroupsScreen = false
                showHackathonBoardScreen = false
                selectedGroupId = null
                selectedGroupInviteCode = null
                showGroupChat = false
                showGroupSettingsScreen = false
                viewingProfileUserId = null
                selectedTab = 1
            }

            AnimatedVisibility(
                visible = uiState.isLoggedIn && canOpenAgent,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(end = 20.dp, bottom = if (isInChatThread) 20.dp else 84.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .noRippleClickable {
                                minimizeAgentSheetForVoice = false
                                showAgentSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        RoboAgentLottie(
                            modifier = Modifier
                                .size(58.dp)
                                .graphicsLayer {
                                    scaleX = 1.04f
                                    scaleY = 1.04f
                                }
                        )
                    }

                }
            }

            if (showAgentSheet && canOpenAgent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize()
                        .blockTouchPassthrough()
                        .graphicsLayer { alpha = agentSheetAlpha }
                ) {
                    if (isGlassTheme) {
                        GlassBackgroundLayer(
                            modifier = Modifier
                                .layerBackdrop(backdrop)
                                .fillMaxSize(),
                            backgroundKey = glassBackgroundKey,
                            accentColor = accentColor,
                            motionStyleKey = glassMotionStyleKey,
                            reduceAnimations = reduceAnimations
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .layerBackdrop(backdrop)
                                .fillMaxSize()
                                .background(appearance.backgroundColor)
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AgentSheetContent(
                            viewModel = agentViewModel,
                            surface = "talk_with_vormex",
                            surfaceContext = mapOf(
                                "surface" to "talk_with_vormex",
                                "entry" to "more_chat_ai"
                            ),
                            userDisplayName = uiState.currentUser?.name,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            backdrop = backdrop,
                            reduceAnimations = reduceAnimations,
                            isDarkTheme = isDarkTheme,
                            isPremiumUser = uiState.currentUser?.isPremium,
                            headerTitle = "vormex",
                            sendButtonLabel = "Send",
                            placeholderText = "Message vormex...",
                            enableVoiceControls = false,
                            enableInlineNavigationActions = true,
                            onOpenInlineProfile = { userId ->
                                agentViewModel.dismissInlineResults()
                                openInlineProfile(userId)
                            },
                            onOpenInlineChat = { userId ->
                                agentViewModel.dismissInlineResults()
                                openInlineChat(userId)
                            },
                            onSeeMoreInlineResults = openInlineFind,
                            onDismiss = {
                                minimizeAgentSheetForVoice = false
                                showAgentSheet = false
                            },
                            isFullScreen = true
                        )
                    }
                }
            }

            // Reels Full-Screen Viewer Dialog (shown from any tab)
            if (reelsState.isViewerOpen) {
                val reelsToShow = if (reelsState.feedReels.isNotEmpty())
                    reelsState.feedReels
                else
                    reelsState.previewReels

                if (reelsToShow.isNotEmpty()) {
                    val reelsManagedAdPlacements = if (reelsState.feedReels.isNotEmpty()) {
                        reelsState.feedAdPlacements
                    } else {
                        emptyList()
                    }
                    ReelsFeedScreen(
                        reels = reelsToShow,
                        managedAdPlacements = reelsManagedAdPlacements,
                        initialIndex = reelsState.currentReelIndex,
                        onDismiss = {
                            val wasDraftPreview = reelsState.activeDraftPreviewId != null
                            reelsViewModel.closeReelsViewer()
                            if (wasDraftPreview) {
                                reopenReelDraftLibrary = true
                                showReelCreateSheet = true
                            }
                        },
                        onLike = { reelId -> reelsViewModel.toggleLike(reelId) },
                        onSave = { reelId -> reelsViewModel.toggleSave(reelId) },
                        onComment = { reelId -> reelsViewModel.openComments(reelId) },
                        onShare = { reelId -> reelsViewModel.showShareModal(reelId) },
                        onProfileClick = { userId ->
                            profileOpenedFromReels = true
                            reelsViewModel.pausePlayback(false)
                            viewingProfileUserId = userId
                        },
                        onTrackView = { reelId, watchTime, completed ->
                            reelsViewModel.trackView(reelId, watchTime, completed)
                        },
                        onReelChanged = { index -> reelsViewModel.onReelChanged(index) },
                        onFirstFrameRendered = { reelId -> reelsViewModel.recordFirstFrameRendered(reelId) },
                        playerForIndex = { index -> reelsViewModel.playerForIndex(index) },
                        onPlaybackError = { index -> reelsViewModel.handlePlaybackError(index) },
                        onRetryPlayback = { index -> reelsViewModel.retryPlayback(index) },
                        onPausePlayback = { reset -> reelsViewModel.pausePlayback(reset) },
                        onResumePlayback = { index -> reelsViewModel.resumePlayback(index) },
                        onReleasePlayback = { reelsViewModel.releasePlayback() },
                        onLoadMore = { reelsViewModel.loadMoreReels() },
                        onManagedAdImpression = { ad -> reelsViewModel.trackManagedAdImpression(ad) },
                        onManagedAdClick = { ad -> reelsViewModel.trackManagedAdClick(ad) },
                        isDraftPreview = reelsState.activeDraftPreviewId != null,
                        isPublishingDraft = reelsState.publishingDraftId != null,
                        onPublishDraftPreview = { reelId ->
                            reelsViewModel.publishDraftReel(reelId) {
                                reopenReelDraftLibrary = false
                                showReelCreateSheet = false
                                selectedTab = 0
                                reelsViewModel.closeReelsViewer()
                            }
                        },
                        onCreateClick = openReelCreateSheet,
                        showNativeAds = true
                    )

                    if (reelsState.showCommentsSheet) {
                        ReelCommentsSheet(
                            backdrop = backdrop,
                            isGlassTheme = isGlassTheme,
                            isDarkTheme = isDarkTheme,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            comments = reelsState.reelComments,
                            repliesByParent = reelsState.replyCommentsByParent,
                            expandedParents = reelsState.expandedReplyParents,
                            loadingReplyParents = reelsState.loadingReplyParents,
                            hasMoreRepliesByParent = reelsState.hasMoreRepliesByParent,
                            replyTarget = reelsState.replyToComment,
                            isLoading = reelsState.isLoadingComments,
                            isLoadingMore = reelsState.isLoadingMoreComments,
                            hasMore = reelsState.hasMoreComments,
                            isSubmitting = reelsState.isSubmittingComment,
                            error = reelsState.commentsError,
                            highlightedCommentId = reelsState.highlightedCommentId,
                            mentionSearchResults = reelsState.mentionSearchResults,
                            isSearchingMentions = reelsState.isSearchingMentions,
                            onDismiss = { reelsViewModel.closeComments() },
                            onLoadMore = { reelsViewModel.loadReelComments(refresh = false) },
                            onToggleReplies = { parentId -> reelsViewModel.loadReplies(parentId) },
                            onLoadMoreReplies = { parentId -> reelsViewModel.loadMoreReplies(parentId) },
                            onReplyTo = { comment -> reelsViewModel.setReplyTarget(comment) },
                            onMentionQueryChanged = { query -> reelsViewModel.searchMentions(query) },
                            onMentionSearchClear = { reelsViewModel.clearMentionSearch() },
                            onSubmitComment = { content -> reelsViewModel.submitComment(content) }
                        )
                    }
                } else {
                    // Loading or empty state dialog
                    Dialog(
                        onDismissRequest = { reelsViewModel.closeReelsViewer() },
                        properties = DialogProperties(
                            usePlatformDefaultWidth = false,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            // Close button
                            Box(
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(16.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { reelsViewModel.closeReelsViewer() }
                                    .align(Alignment.TopStart),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    "✕",
                                    style = TextStyle(Color.White, 18.sp, FontWeight.Bold)
                                )
                            }

                            if (reelsState.isLoadingFeed) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(color = Color.White)
                                    BasicText(
                                        "Loading Reels...",
                                        style = TextStyle(Color.White, 16.sp)
                                    )
                                }
                            } else if (reelsState.feedError != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    BasicText(
                                        "🎬",
                                        style = TextStyle(fontSize = 48.sp)
                                    )
                                    BasicText(
                                        reelsState.feedError ?: "No reels available",
                                        style = TextStyle(Color.White, 16.sp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Color.White.copy(alpha = 0.2f))
                                            .clickable { reelsViewModel.loadAndOpenReels() }
                                            .padding(horizontal = 24.dp, vertical = 12.dp)
                                    ) {
                                        BasicText(
                                            "Try Again",
                                            style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    BasicText(
                                        "🎬",
                                        style = TextStyle(fontSize = 48.sp)
                                    )
                                    BasicText(
                                        "No reels yet",
                                        style = TextStyle(Color.White, 16.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (reelsState.showShareModal && reelsState.shareReelId != null) {
                SharePostModal(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isLightTheme = isLightTheme,
                    shareTargets = reelsState.shareTargets,
                    isLoading = reelsState.isLoadingShareTargets,
                    isSharing = reelsState.isSharing,
                    error = reelsState.shareError,
                    onDismiss = { reelsViewModel.hideShareModal() },
                    onCopyLink = {
                        reelsState.shareReelId?.let { reelId ->
                            reelsViewModel.copyReelLink(reelId)
                        }
                    },
                    onShareToTargets = { targetIds, message ->
                        reelsViewModel.shareReelInApp(targetIds, message)
                    },
                    onClearError = { reelsViewModel.clearShareError() },
                    subjectLabel = "reel",
                    showMessageInput = false,
                    onShareAnimation = { shareAnimationTrigger++ }
                )
            }

            ShareLottieEffect(
                trigger = shareAnimationTrigger,
                modifier = Modifier
                    .align(Alignment.Center)
                    .requiredSize(260.dp)
            )

            if (showReelCreateSheet) {
                ReelCreateSheet(
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isUploading = reelsState.isUploadingReel,
                    error = reelsState.reelUploadError,
                    drafts = reelsState.draftReels,
                    isLoadingDrafts = reelsState.isLoadingDrafts,
                    draftsError = reelsState.draftError,
                    publishingDraftId = reelsState.publishingDraftId,
                    startWithDraftLibrary = reopenReelDraftLibrary,
                    onDismiss = closeReelCreateSheet,
                    onLoadDrafts = { reelsViewModel.loadDraftReels(forceRefresh = true) },
                    onPreviewDraft = { draft ->
                        reopenReelDraftLibrary = false
                        showReelCreateSheet = false
                        selectedTab = 0
                        reelsViewModel.openDraftPreview(draft)
                    },
                    onPublishDraft = { reelId ->
                        reelsViewModel.publishDraftReel(reelId) {
                            showReelCreateSheet = false
                            selectedTab = 0
                            reelsViewModel.closeReelsViewer()
                        }
                    },
                    onSubmit = { form ->
                        reelsViewModel.createReel(
                            videoUri = form.video.uri,
                            videoFileName = form.video.fileName,
                            videoMimeType = form.video.mimeType,
                            videoSize = form.video.sizeBytes,
                            thumbnailUri = form.thumbnail?.uri,
                            thumbnailFileName = form.thumbnail?.fileName,
                            thumbnailMimeType = form.thumbnail?.mimeType,
                            thumbnailSize = form.thumbnail?.sizeBytes,
                            title = form.title,
                            caption = form.caption,
                            hashtags = form.hashtags,
                            category = form.category,
                            visibility = form.visibility,
                            allowComments = form.allowComments,
                            allowDuets = form.allowDuets,
                            allowStitch = form.allowStitch,
                            allowDownload = form.allowDownload,
                            allowSharing = form.allowSharing,
                            muteOriginalAudio = form.muteOriginalAudio,
                            saveAsDraft = form.saveAsDraft
                        )
                        showReelCreateSheet = false
                        selectedTab = 0
                        reelsViewModel.closeReelsViewer()
                    }
                )
            }

            // Swipeable Reward Cards Overlay (shown when app opens)
            if (
                rewardCardsState.shouldShowOverlay &&
                rewardCardsState.cards.isNotEmpty() &&
                rewardCardsState.sessionId != null
            ) {
                SwipeableRewardCardsOverlay(
                    sessionId = rewardCardsState.sessionId!!,
                    cards = rewardCardsState.cards,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    currentTheme = themeMode,
                    reduceAnimations = reduceAnimations,
                    onCardShown = {
                        rewardCardsViewModel.trackShownCardsOnce()
                    },
                    onSkip = { card ->
                        rewardCardsViewModel.trackSkipped(card)
                    },
                    onOpenProfile = { card ->
                        rewardCardsViewModel.openProfile(card)
                        viewingProfileUserId = card.id
                    },
                    onConnect = { card ->
                        rewardCardsViewModel.connect(card)
                    },
                    onDismissAll = {
                        rewardCardsViewModel.dismissAll()
                    }
                )
            }

            // Messages Screen Overlay
            AnimatedVisibility(
                visible = showMessagesScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                // Same shell as Groups / Appearance: glass uses GlassBackgroundLayer (wallpaper + motion),
                // light/dark use solid surfaces — avoids feed bleed-through and keeps chat aligned with theme.
                val messagesContentColor = contentColor
                val messagesAccentColor = accentColor

                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations,
                    applyStatusBarPadding = false
                ) {
                    Column(Modifier.fillMaxSize()) {
                        // Chat content
                        ChatTabContent(
                            backdrop = backdrop,
                            contentColor = messagesContentColor,
                            accentColor = messagesAccentColor,
                            viewModel = chatViewModel,
                            isGlassTheme = isGlassTheme,
                            openConversationId = deepLinkConversationId,
                            openChatWithUserId = openChatWithUserId,
                            openChatInitialDraft = openChatInitialDraft,
                            onConsumedOpenConversation = { deepLinkConversationId = null },
                            onConsumedOpenChat = {
                                openChatWithUserId = null
                                openChatInitialDraft = null
                            },
                            onInChatThread = { inThread -> isInChatThread = inThread },
                            onNavigateToProfile = { userId ->
                                viewingProfileUserId = userId
                            },
                            onNavigateToReel = { sharedReel ->
                                showMessagesScreen = false
                                isInChatThread = false
                                chatViewModel.selectConversation(null)
                                selectedTab = 0
                                reelsViewModel.openSharedReel(sharedReel)
                            },
                            onOpenGroupShortcut = { groupId ->
                                showMessagesScreen = false
                                isInChatThread = false
                                chatViewModel.selectConversation(null)
                                openChatWithUserId = null
                                openChatInitialDraft = null
                                deepLinkConversationId = null
                                viewingProfileUserId = null
                                selectedTab = 3
                                showGroupsScreen = false
                                selectedGroupInviteCode = null
                                selectedGroupId = groupId
                                showGroupSettingsScreen = false
                                showGroupChat = true
                            },
                            onCreateGroup = {
                                showMessagesCreateGroupSheet = true
                            }
                        )

                        if (showMessagesCreateGroupSheet) {
                            CreateGroupModal(
                                backdrop = backdrop,
                                contentColor = messagesContentColor,
                                accentColor = messagesAccentColor,
                                isCreating = isCreatingGroupFromMessages,
                                onDismiss = {
                                    if (!isCreatingGroupFromMessages) {
                                        showMessagesCreateGroupSheet = false
                                    }
                                },
                                onCreate = { name, description, privacy, category, rules ->
                                    if (isCreatingGroupFromMessages) return@CreateGroupModal
                                    appScope.launch {
                                        isCreatingGroupFromMessages = true
                                        GroupsApiService.createGroup(
                                            context = context,
                                            name = name,
                                            description = description,
                                            privacy = privacy,
                                            category = category,
                                            rules = rules
                                        ).fold(
                                            onSuccess = {
                                                isCreatingGroupFromMessages = false
                                                showMessagesCreateGroupSheet = false
                                                showMessagesScreen = false
                                                isInChatThread = false
                                                chatViewModel.selectConversation(null)
                                                openChatWithUserId = null
                                                openChatInitialDraft = null
                                                deepLinkConversationId = null
                                                viewingProfileUserId = null
                                                selectedTab = 3
                                                selectedGroupId = null
                                                selectedGroupInviteCode = null
                                                showGroupChat = false
                                                showGroupSettingsScreen = false
                                                showGroupsScreen = true
                                            },
                                            onFailure = { error ->
                                                isCreatingGroupFromMessages = false
                                                Toast.makeText(
                                                    context,
                                                    error.message ?: "Failed to create group",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Profile page when viewing another user's profile (above Messages so it opens on top of chat)
            AnimatedVisibility(
                visible = viewingProfileUserId != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                viewingProfileUserId?.let { userId ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .blockTouchPassthrough()
                            .then(
                                when {
                                    isGlassTheme -> Modifier.drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedRectangle(0f.dp) },
                                        effects = {
                                            vibrancy()
                                            blur(24f.dp.toPx())
                                            lens(12f.dp.toPx(), 24f.dp.toPx())
                                        },
                                        onDrawSurface = {
                                            drawRect(Color.White.copy(alpha = 0.08f))
                                        }
                                    )
                                    isDarkTheme -> Modifier.background(Color(0xFF121212))
                                    else -> Modifier.background(Color(0xFFF5F5F5)) // Light theme
                                }
                            )
                            .statusBarsPadding()
                    ) {
	                        ProfileScreen(
	                            userId = userId,
	                            backdrop = backdrop,
	                            contentColor = contentColor,
	                            accentColor = accentColor,
	                            onNavigateBack = { viewingProfileUserId = null },
	                            onOpenProfile = { nextUserId -> viewingProfileUserId = nextUserId },
	                            showStartConversation = profileConversationHasMessagesByUserId[userId] == false,
	                            isPreparingConversationStarter = preparingConversationStarterForUserId == userId,
	                            onStartConversation = { otherUserId ->
	                                startAiConversationFromProfile(otherUserId)
	                            },
	                            onMessage = { otherUserId ->
	                                if (profileOpenedFromReels && reelsState.isViewerOpen) {
	                                    profileOpenedFromReels = false
	                                    reelsViewModel.closeReelsViewer()
	                                }
	                                openInlineChat(otherUserId)
	                            },
                            onOpenFeedItem = { item ->
                                when (item.entityType?.lowercase()) {
                                    "reel" -> {
                                        if (profileOpenedFromReels && reelsState.isViewerOpen) {
                                            profileOpenedFromReels = false
                                            reelsViewModel.closeReelsViewer()
                                        }
                                        viewingProfileUserId = null
                                        selectedTab = 0
                                        reelsViewModel.loadReelById(item.id)
                                    }
                                    "post" -> {
                                        if (profileOpenedFromReels && reelsState.isViewerOpen) {
                                            profileOpenedFromReels = false
                                            reelsViewModel.closeReelsViewer()
                                        }
                                        viewingProfileUserId = null
                                        selectedTab = 0
                                        viewModel.openPostDetail(item.id)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Groups Screen Overlay
            AnimatedVisibility(
                visible = showGroupsScreen && selectedGroupId == null && selectedGroupInviteCode == null && !showGroupChat && !showGroupSettingsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    GroupsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        openCreateRequestKey = openCreateGroupRequestKey,
                        onMessageShortcutChanged = { chatViewModel.refreshGroupShortcuts() },
                        onNavigateBack = { showGroupsScreen = false },
                        onNavigateToGroupDetail = { groupId -> selectedGroupId = groupId },
                        onNavigateToGroupChat = { groupId ->
                            selectedGroupId = groupId
                            showGroupChat = true
                        }
                    )
                }
            }

            // Group Invite Link Overlay
            AnimatedVisibility(
                visible = selectedGroupInviteCode != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                selectedGroupInviteCode?.let { inviteCode ->
                    SectionOverlayContainer(
                        backdrop = backdrop,
                        themeMode = themeMode,
                        glassBackgroundKey = glassBackgroundKey,
                        accentColor = accentColor,
                        glassMotionStyleKey = glassMotionStyleKey,
                        reduceAnimations = reduceAnimations
                    ) {
                        GroupInviteLinkScreen(
                            inviteCode = inviteCode,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onNavigateBack = {
                                selectedGroupInviteCode = null
                                showGroupsScreen = true
                            },
                            onOpenGroup = { groupId ->
                                selectedGroupInviteCode = null
                                selectedGroupId = groupId
                                showGroupsScreen = true
                                showGroupChat = false
                                showGroupSettingsScreen = false
                            }
                        )
                    }
                }
            }

            // Group Detail Screen Overlay
            AnimatedVisibility(
                visible = selectedGroupId != null && selectedGroupInviteCode == null && !showGroupChat && !showGroupSettingsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                selectedGroupId?.let { groupId ->
                    SectionOverlayContainer(
                        backdrop = backdrop,
                        themeMode = themeMode,
                        glassBackgroundKey = glassBackgroundKey,
                        accentColor = accentColor,
                        glassMotionStyleKey = glassMotionStyleKey,
                        reduceAnimations = reduceAnimations
                    ) {
                        GroupDetailScreen(
                            groupId = groupId,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onNavigateBack = {
                                showGroupSettingsScreen = false
                                selectedGroupId = null
                            },
                            onNavigateToChat = { showGroupChat = true },
                            onNavigateToSettings = { showGroupSettingsScreen = true },
                            onNavigateToProfile = { userId -> viewingProfileUserId = userId },
                            onMessageShortcutChanged = { chatViewModel.refreshGroupShortcuts() }
                        )
                    }
                }
            }

            // Group Settings Screen Overlay
            AnimatedVisibility(
                visible = showGroupSettingsScreen && selectedGroupId != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                selectedGroupId?.let { groupId ->
                    SectionOverlayContainer(
                        backdrop = backdrop,
                        themeMode = themeMode,
                        glassBackgroundKey = glassBackgroundKey,
                        accentColor = accentColor,
                        glassMotionStyleKey = glassMotionStyleKey,
                        reduceAnimations = reduceAnimations
                    ) {
                        GroupSettingsScreen(
                            groupId = groupId,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onNavigateBack = { showGroupSettingsScreen = false },
                            onDeleted = {
                                showGroupSettingsScreen = false
                                showGroupChat = false
                                selectedGroupId = null
                                showGroupsScreen = true
                            }
                        )
                    }
                }
            }

            // Group Chat Screen Overlay
            AnimatedVisibility(
                visible = showGroupChat && selectedGroupId != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                selectedGroupId?.let { groupId ->
                    SectionOverlayContainer(
                        backdrop = backdrop,
                        themeMode = themeMode,
                        glassBackgroundKey = glassBackgroundKey,
                        accentColor = accentColor,
                        glassMotionStyleKey = glassMotionStyleKey,
                        reduceAnimations = reduceAnimations
                    ) {
                        GroupChatScreen(
                            groupId = groupId,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            currentUserId = uiState.currentUser?.id,
                            onNavigateBack = { showGroupChat = false },
                            onNavigateToProfile = { userId -> viewingProfileUserId = userId },
                            onNavigateToGroupDetail = {
                                showGroupSettingsScreen = false
                                showGroupChat = false
                            },
                            onNavigateToSettings = {
                                showGroupChat = false
                                showGroupSettingsScreen = true
                            }
                        )
                    }
                }
            }

            // Circles Screen Overlay
            AnimatedVisibility(
                visible = showCirclesScreen && selectedCircleId == null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    CirclesScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showCirclesScreen = false },
                        onNavigateToCircle = { circleId -> selectedCircleId = circleId },
                        onNavigateToUpgrade = { openPremiumDetails() }
                    )
                }
            }

            // Circle Detail Screen Overlay
            AnimatedVisibility(
                visible = selectedCircleId != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                selectedCircleId?.let { circleId ->
                    SectionOverlayContainer(
                        backdrop = backdrop,
                        themeMode = themeMode,
                        glassBackgroundKey = glassBackgroundKey,
                        accentColor = accentColor,
                        glassMotionStyleKey = glassMotionStyleKey,
                        reduceAnimations = reduceAnimations
                    ) {
                        CircleDetailScreen(
                            circleId = circleId,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            currentUserId = uiState.currentUser?.id,
                            onNavigateBack = { selectedCircleId = null },
                            onNavigateToProfile = { userId -> viewingProfileUserId = userId },
                            onInviteMember = { /* TODO: Show invite modal */ }
                        )
                    }
                }
            }

            // ==================== RETENTION FEATURE SCREENS ====================

            // Weekly Goals Screen Overlay
            AnimatedVisibility(
                visible = showWeeklyGoalsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    WeeklyGoalsDetailScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showWeeklyGoalsScreen = false },
                        onNavigateToFindPeople = {
                            showWeeklyGoalsScreen = false
                            selectedTab = 1
                        }
                    )
                }
            }

            // Streak Details Screen Overlay
            AnimatedVisibility(
                visible = showStreakDetailsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    StreakDetailsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showStreakDetailsScreen = false },
                        onNavigateToFindPeople = {
                            showStreakDetailsScreen = false
                            selectedTab = 1
                        },
                        onNavigateToCreatePost = {
                            showStreakDetailsScreen = false
                            selectedTab = 2
                        }
                    )
                }
            }

            // Top Networkers Leaderboard Screen Overlay
            AnimatedVisibility(
                visible = showTopNetworkersScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    TopNetworkersScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showTopNetworkersScreen = false },
                        onNavigateToProfile = { userId ->
                            showTopNetworkersScreen = false
                            viewingProfileUserId = userId
                        }
                    )
                }
            }

            // Onboarding / Profile Preferences Screen Overlay
            AnimatedVisibility(
                visible = showOnboardingScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .blockTouchPassthrough()
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                ) {
                    com.kyant.backdrop.catalog.onboarding.ProfileSetupWizard(
                        onComplete = { showOnboardingScreen = false },
                        onSkip = { showOnboardingScreen = false }
                    )
                }
            }

            // Saved Posts Screen Overlay
            AnimatedVisibility(
                visible = showSavedPostsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    SavedPostsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showSavedPostsScreen = false },
                        onNavigateToPost = { postId ->
                            showSavedPostsScreen = false
                            selectedTab = 0
                            viewModel.openPostDetail(postId)
                        },
                        onNavigateToReel = { reelId ->
                            showSavedPostsScreen = false
                            selectedTab = 0
                            reelsViewModel.loadReelById(reelId)
                        },
                        onNavigateToProfile = { userId ->
                            showSavedPostsScreen = false
                            viewingProfileUserId = userId
                        }
                    )
                }
            }

            // Saved Post Detail Overlay
            AnimatedVisibility(
                visible = hasOpenPostDetail,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    SavedPostDetailOverlay(
                        post = uiState.openedPost,
                        isLoading = uiState.isLoadingOpenedPost,
                        error = uiState.openedPostError,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        glassBackgroundKey = glassBackgroundKey,
                        reduceAnimations = reduceAnimations,
                        onNavigateBack = { viewModel.closePostDetail() },
                        onLike = { postId -> viewModel.toggleLike(postId) },
                        onComment = { postId ->
                            selectedPostForComments = postId
                            viewModel.loadComments(postId)
                            showCommentsSheet = true
                        },
                        onShare = { postId -> viewModel.showShareModal(postId) },
                        onVotePoll = { postId, optionId -> viewModel.votePoll(postId, optionId) },
                        onProfileClick = { userId ->
                            viewModel.closePostDetail()
                            viewingProfileUserId = userId
                        },
                        onMenuAction = handlePostMenuAction
                    )
                }
            }

            // Connection Requests Screen Overlay
            AnimatedVisibility(
                visible = showConnectionRequestsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    ConnectionRequestsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGlassTheme = isGlassTheme,
                        requests = rewardsState.pendingConnectionRequests,
                        isLoading = rewardsState.isLoadingPendingConnectionRequests,
                        error = rewardsState.pendingConnectionRequestsError,
                        actionInProgress = rewardsState.connectionActionInProgress,
                        onNavigateBack = { showConnectionRequestsScreen = false },
                        onOpenProfile = { userId ->
                            showConnectionRequestsScreen = false
                            viewingProfileUserId = userId
                        },
                        onAccept = { userId, connectionId ->
                            findPeopleViewModel.acceptConnection(userId, connectionId)
                        },
                        onReject = { userId, connectionId ->
                            findPeopleViewModel.rejectConnection(userId, connectionId)
                        }
                    )
                }
            }

            // Profile Customizations Screen Overlay
            AnimatedVisibility(
                visible = showProfileCustomizationsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    ProfileCustomizationsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGlassTheme = isGlassTheme,
                        currentUser = uiState.currentUser,
                        onNavigateBack = { showProfileCustomizationsScreen = false }
                    )
                }
            }

            // Notifications Inbox Overlay
            AnimatedVisibility(
                visible = showNotificationsInbox,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    NotificationsInboxScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showNotificationsInbox = false },
                        onUnreadCountChanged = { notificationUnreadCount = it },
                        onOpenProfileViews = {
                            showNotificationsInbox = false
                            showProfileViewersScreen = true
                        },
                        onOpenProfile = { userId ->
                            showNotificationsInbox = false
                            viewingProfileUserId = userId
                        },
                        onOpenPost = { postId, openComments ->
                            showNotificationsInbox = false
                            selectedTab = 0
                            if (openComments) {
                                viewModel.closePostDetail()
                                selectedPostForComments = postId
                                viewModel.loadComments(postId)
                                showCommentsSheet = true
                            } else {
                                openPostFromLink(postId)
                            }
                        },
                        onOpenReel = { reelId, commentId, parentCommentId ->
                            showNotificationsInbox = false
                            selectedTab = 0
                            reelsViewModel.loadReelById(reelId)
                            if (!commentId.isNullOrBlank()) {
                                reelsViewModel.openComments(
                                    reelId = reelId,
                                    highlightCommentId = commentId,
                                    parentCommentId = parentCommentId
                                )
                            }
                        },
                        onOpenConversation = { conversationId ->
                            showNotificationsInbox = false
                            openChatWithUserId = null
                            deepLinkConversationId = conversationId
                            showMessagesScreen = true
                        },
                        onOpenNetwork = {
                            showNotificationsInbox = false
                            selectedTab = 1
                        },
                        onOpenGrowthHub = {
                            showNotificationsInbox = false
                            showGrowthHubScreen = true
                        },
                        onOpenSkillSwap = { tab ->
                            showNotificationsInbox = false
                            selectedTab = 3
                            showFullMoreScreen = false
                            skillSwapInitialTab = tab ?: "requests"
                            showSkillSwapScreen = true
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = showProfileViewersScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    ProfileViewersScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showProfileViewersScreen = false },
                        onOpenProfile = { userId ->
                            showProfileViewersScreen = false
                            viewingProfileUserId = userId
                        }
                    )
                }
            }

            // Games Overlay
            AnimatedVisibility(
                visible = showGamesScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    GamesHubScreen(
                        contentColor = contentColor,
                        onNavigateBack = { showGamesScreen = false }
                    )
                }
            }

            // Growth Hub Overlay
            AnimatedVisibility(
                visible = showGrowthHubScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    GrowthHubScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showGrowthHubScreen = false }
                    )
                }
            }

            // Talent Engine Overlay
            AnimatedVisibility(
                visible = showTalentEngineScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    TalentEngineScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showTalentEngineScreen = false }
                    )
                }
            }

            // Skill Passport Overlay
            AnimatedVisibility(
                visible = showSkillPassportScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    SkillPassportScreen(
                        userId = "me",
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showSkillPassportScreen = false },
                        onOpenSkillSwap = {
                            showSkillPassportScreen = false
                            skillSwapInitialTab = "discover"
                            showSkillSwapScreen = true
                        },
                        onOpenProfile = { userId ->
                            showSkillPassportScreen = false
                            viewingProfileUserId = userId
                        }
                    )
                }
            }

            // Skill Swap Overlay
            AnimatedVisibility(
                visible = showSkillSwapScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    SkillSwapScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        initialTab = skillSwapInitialTab,
                        onNavigateBack = { showSkillSwapScreen = false },
                        onOpenProfile = { userId ->
                            showSkillSwapScreen = false
                            viewingProfileUserId = userId
                        }
                    )
                }
            }

            // Hackathon Board Overlay
            AnimatedVisibility(
                visible = showHackathonBoardScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    HackathonBoardScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showHackathonBoardScreen = false },
                        onOpenGroupChat = { groupId ->
                            showHackathonBoardScreen = false
                            showGroupSettingsScreen = false
                            selectedGroupId = groupId
                            showGroupChat = true
                        }
                    )
                }
            }

            // Notification Settings Screen Overlay
            AnimatedVisibility(
                visible = showNotificationSettingsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    NotificationSettingsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showNotificationSettingsScreen = false }
                    )
                }
            }

            // Privacy Settings Screen Overlay
            AnimatedVisibility(
                visible = showPrivacySettingsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    PrivacySettingsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showPrivacySettingsScreen = false }
                    )
                }
            }

            // Appearance Settings Screen Overlay
            AnimatedVisibility(
                visible = showAppearanceSettingsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    AppearanceSettingsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        canAccessProfileCustomization = canAccessProfileCustomization,
                        onNavigateBack = { showAppearanceSettingsScreen = false }
                    )
                }
            }

            // Help Screen Overlay
            AnimatedVisibility(
                visible = showHelpScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    HelpScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showHelpScreen = false }
                    )
                }
            }

            // About Screen Overlay
            AnimatedVisibility(
                visible = showAboutScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    AboutScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showAboutScreen = false }
                    )
                }
            }

            // Invite Friends Screen Overlay
            AnimatedVisibility(
                visible = showInviteFriendsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    InviteFriendsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showInviteFriendsScreen = false }
                    )
                }
            }

            // Contact Screen Overlay
            AnimatedVisibility(
                visible = showContactScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    ContactScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showContactScreen = false }
                    )
                }
            }

            // Logout Confirmation Dialog
            if (showLogoutDialog) {
                LogoutConfirmationDialog(
                    contentColor = Color.White,
                    accentColor = accentColor,
                    title = logoutDialogTitle,
                    message = logoutDialogMessage,
                    confirmText = logoutDialogConfirmText,
                    onConfirm = {
                        showLogoutDialog = false
                        resetAccountScopedNavigation()
                        viewModel.logout()
                    },
                    onDismiss = { showLogoutDialog = false }
                )
            }

            // Session Summary Overlay (Peak-End Rule)
            SessionSummaryOverlay(
                isVisible = showSessionSummary,
                sessionData = retentionState.sessionSummary,
                backdrop = backdrop,
                contentColor = Color.White,
                accentColor = accentColor,
                onDismiss = { showSessionSummary = false }
            )
        }

        uiState.pendingVerificationEmail?.takeIf { it.isNotBlank() }?.let { verificationEmail ->
            EmailVerificationSheet(
                contentColor = contentColor,
                accentColor = accentColor,
                email = verificationEmail,
                isLoading = uiState.isLoading,
                isSuccess = uiState.isEmailVerificationSuccess,
                error = uiState.error,
                onVerify = { code -> viewModel.verifyEmailOtp(code) },
                onResend = { viewModel.resendVerificationCode() },
                onLoginClick = { viewModel.showLogin() },
                onDismiss = { viewModel.dismissEmailVerification() },
                onClearError = { viewModel.clearError() },
                onSuccessAnimationFinished = { viewModel.completeEmailVerificationAnimation() }
            )
        }

        if (reportingPostId != null) {
            SafetyReportDialog(
                title = "Report post",
                subtitle = "Tell Trust & Safety what is wrong with this post.",
                contentColor = contentColor,
                accentColor = accentColor,
                blockLabel = "Also block this author",
                isSubmitting = isSubmittingPostReport,
                onDismiss = {
                    if (!isSubmittingPostReport) reportingPostId = null
                },
                onSubmit = ::submitPostReport
            )
        }

        if (showAccountSwitcherDialog) {
            AccountSwitcherDialog(
                accounts = uiState.savedAccounts,
                currentUserId = uiState.currentUserId,
                contentColor = contentColor,
                accentColor = accentColor,
                onSwitchAccount = { userId ->
                    resetAccountScopedNavigation()
                    viewModel.switchToSavedAccount(userId)
                },
                onAddExistingAccount = {
                    resetAccountScopedNavigation()
                    viewModel.addExistingAccount()
                },
                onAddNewAccount = {
                    resetAccountScopedNavigation()
                    viewModel.addNewAccount()
                },
                onDismiss = { showAccountSwitcherDialog = false }
            )
        }
    }
    }
}

@Composable
private fun AccountSwitcherDialog(
    accounts: List<ApiClient.SavedAccountSession>,
    currentUserId: String?,
    contentColor: Color,
    accentColor: Color,
    onSwitchAccount: (String) -> Unit,
    onAddExistingAccount: () -> Unit,
    onAddNewAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDarkDialog = contentColor.luminance() > 0.5f
    val surfaceColor = if (isDarkDialog) Color(0xFF111827) else Color.White
    val textColor = if (isDarkDialog) Color.White else Color(0xFF101828)
    val secondaryTextColor = textColor.copy(alpha = 0.62f)
    val dividerColor = textColor.copy(alpha = if (isDarkDialog) 0.12f else 0.08f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 430.dp)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(26.dp))
                    .background(surfaceColor.copy(alpha = 0.98f))
                    .border(1.dp, dividerColor, RoundedCornerShape(26.dp))
                    .padding(18.dp)
            ) {
                BasicText(
                    "Switch account",
                    style = TextStyle(
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    "Choose a saved account on this device.",
                    style = TextStyle(
                        color = secondaryTextColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )

                Spacer(Modifier.height(16.dp))

                if (accounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(textColor.copy(alpha = if (isDarkDialog) 0.06f else 0.04f))
                            .padding(14.dp)
                    ) {
                        BasicText(
                            "No saved accounts yet. Add one below to start switching faster.",
                            style = TextStyle(
                                color = secondaryTextColor,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 292.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(accounts, key = { it.userId }) { account ->
                            AccountSwitcherAccountRow(
                                account = account,
                                isCurrent = account.userId == currentUserId,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                accentColor = accentColor,
                                isDarkDialog = isDarkDialog,
                                onClick = { onSwitchAccount(account.userId) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(dividerColor)
                )
                Spacer(Modifier.height(10.dp))

                AccountSwitcherActionRow(
                    title = "Add existing account",
                    subtitle = "Sign in to another Vormex account",
                    icon = Icons.AutoMirrored.Outlined.Login,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    accentColor = accentColor,
                    onClick = onAddExistingAccount
                )
                AccountSwitcherActionRow(
                    title = "Add new account",
                    subtitle = "Create a fresh Vormex account",
                    icon = Icons.Outlined.PersonAddAlt1,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    accentColor = accentColor,
                    onClick = onAddNewAccount
                )
            }
        }
    }
}

@Composable
private fun AccountSwitcherAccountRow(
    account: ApiClient.SavedAccountSession,
    isCurrent: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    isDarkDialog: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val displayName = savedAccountDisplayName(account)
    val subtitle = savedAccountSubtitle(account)
    val profileImage = account.profileImage?.takeIf { it.isNotBlank() }
    val rowColor = if (isCurrent) {
        accentColor.copy(alpha = if (isDarkDialog) 0.22f else 0.14f)
    } else {
        textColor.copy(alpha = if (isDarkDialog) 0.06f else 0.04f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowColor)
            .clickable(enabled = !isCurrent, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            if (profileImage != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(profileImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                BasicText(
                    savedAccountInitials(displayName),
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                displayName,
                style = TextStyle(
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            BasicText(
                subtitle,
                style = TextStyle(
                    color = secondaryTextColor,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isCurrent) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(17.dp)
                )
                BasicText(
                    "Current",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = secondaryTextColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AccountSwitcherActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    textColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                title,
                style = TextStyle(
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                subtitle,
                style = TextStyle(
                    color = secondaryTextColor,
                    fontSize = 12.sp
                )
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = secondaryTextColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun savedAccountDisplayName(account: ApiClient.SavedAccountSession): String =
    account.name?.takeIf { it.isNotBlank() }
        ?: account.username?.takeIf { it.isNotBlank() }
        ?: account.email?.takeIf { it.isNotBlank() }
        ?: "Vormex account"

private fun savedAccountSubtitle(account: ApiClient.SavedAccountSession): String {
    val username = account.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
    val email = account.email?.takeIf { it.isNotBlank() }
    return when {
        username != null && email != null -> "$username - $email"
        username != null -> username
        email != null -> email
        else -> "Saved on this device"
    }
}

private fun savedAccountInitials(displayName: String): String =
    displayName
        .split(Regex("\\s+"))
        .mapNotNull { token -> token.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifBlank { "V" }

@Composable
private fun LinkedInTopBar(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    userInitials: String = "U",
    loginStreak: Int = 0,
    isStreakAtRisk: Boolean = false,
    hasUnreadNotifications: Boolean = false,
    unreadNotificationCount: Int = 0,
    hasUnreadMessages: Boolean = false,
    onStreakClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: login streak (tappable → streak details)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .noRippleClickable(onClick = onStreakClick)
                    .padding(horizontal = 2.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                StreakFireLottie(modifier = Modifier.size(22.dp))
                BasicText(
                    loginStreak.coerceAtLeast(0).toString(),
                    style = TextStyle(
                        color = if (isStreakAtRisk) Color(0xFFFF9500) else contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        // Center: vormeX
        Box(
            Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                "vormeX",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 32.sp,
                    fontFamily = PacificoFontFamily,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            )
        }

        // Right: notification + message (vector icons)
        Box(
            Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .noRippleClickable { onNotificationsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    NotificationBellIcon(
                        color = contentColor,
                        size = 22.dp
                    )
                    if (hasUnreadNotifications) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFFF3B30))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            BasicText(
                                if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    Modifier
                        .size(40.dp)
                        .noRippleClickable { onMessagesClick() },
                    contentAlignment = Alignment.Center
                ) {
                    HeaderMessageIcon(
                        color = contentColor,
                        size = 22.dp
                    )
                    if (hasUnreadMessages) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .size(10.dp)
                                .background(Color(0xFFFF3B30), CircleShape)
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    glassBackgroundKey: String = DefaultGlassBackgroundPresetKey,
    posts: List<Post> = emptyList(),
    managedAdPlacements: List<ManagedAdPlacement> = emptyList(),
    modulePlacements: List<com.kyant.backdrop.catalog.network.models.HomeModulePlacement> = emptyList(),
    recommendationSessionId: String? = null,
    recommendationRequestId: String? = null,
    storyGroups: List<StoryGroup> = emptyList(),
    // Reels data
    showReelsOnHome: Boolean = false,
    reels: List<Reel> = emptyList(),
    isLoadingReels: Boolean = false,
    onReelClick: (Int) -> Unit = {},
    onSeeAllReelsClick: () -> Unit = {},
    onCreateReelClick: () -> Unit = {},
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    error: String? = null,
    currentUserInitials: String = "U",
    currentUserProfileImage: String? = null,
    currentUserName: String = "You",
    currentUserId: String? = null,
    isCurrentUserPremium: Boolean = false,
    isLightTheme: Boolean = true,
    // Streak data (Duolingo Effect)
    connectionStreak: Int = 0,
    loginStreak: Int = 0,
    isStreakAtRisk: Boolean = false,
    showStreakReminder: Boolean = false,
    showLoginStreakBadge: Boolean = false,
    onDismissStreakReminder: () -> Unit = {},
    onDismissLoginStreakBadge: () -> Unit = {},
    onNavigateToFindPeople: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onLike: (String) -> Unit = {},
    onComment: (String) -> Unit = {},
    onShare: (String) -> Unit = {},
    onVotePoll: (String, String) -> Unit = { _, _ -> },
    onProfileClick: (String) -> Unit = {},
    onMenuAction: (String, String) -> Unit = { _, _ -> },
    onNotInterested: (String) -> Unit = {},
    onBoostPost: (String) -> Unit = {},
    showFeedbackUndo: Boolean = false,
    onUndoFeedback: () -> Unit = {},
    onManagedAdImpression: (ManagedAdPlacement) -> Unit = {},
    onManagedAdClick: (ManagedAdPlacement) -> Unit = {},
    // Story callbacks
    onStoryClick: (Int) -> Unit = {},
    onAddStoryClick: () -> Unit = {},
    onMyStoryClick: () -> Unit = {},
    // Onboarding prompt
    showOnboarding: Boolean = false,
    onNavigateToOnboarding: () -> Unit = {},
    // Retention features
    retentionState: RetentionUiState? = null,
    onWeeklyGoalsClick: () -> Unit = {},
    onStreakDetailsClick: () -> Unit = {},
    onTopNetworkersClick: () -> Unit = {},
    reduceAnimations: Boolean = false,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val pullToRefreshState = rememberPullToRefreshState()
    val showSkeletons = isLoading && posts.isEmpty()
    val skeletonShimmer = if (showSkeletons) shimmerBrush(isLightTheme) else null
    val isFeedScrolling by remember {
        derivedStateOf { listState.isScrollInProgress }
    }
    val reduceFeedCardMotion = reduceAnimations || isFeedScrolling
    val canRequestNativeAds by VormexAdsManager.canRequestAds.collectAsState()
    val showNativeAds = BuildConfig.ADS_ENABLED && canRequestNativeAds
    val activeManagedAdPlacements = if (BuildConfig.ADS_ENABLED) managedAdPlacements else emptyList()

    val widgetPositions = emptyMap<Int, String>()

    val feedRows = remember(posts, retentionState, modulePlacements, activeManagedAdPlacements, showNativeAds) {
        buildHomeFeedRows(
            posts = posts,
            retentionState = retentionState,
            widgetPositions = widgetPositions,
            modulePlacements = modulePlacements,
            managedAdPlacements = activeManagedAdPlacements,
            includeNativeAds = showNativeAds
        )
    }

    TrackHomeRecommendationVisibility(
        listState = listState,
        rows = feedRows,
        recommendationSessionId = recommendationSessionId,
        requestId = recommendationRequestId
    )

    // System default fling matches native RecyclerView-style physics and avoids custom decay work during scroll.
    val listFlingBehavior = ScrollableDefaults.flingBehavior()

    LaunchedEffect(listState, feedRows.size, hasMore, isLoadingMore) {
        if (!hasMore || isLoadingMore || feedRows.isEmpty()) return@LaunchedEffect

        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            totalItems > 0 &&
                lastVisibleItem >= (totalItems - HomeFeedPrefetchRemainingPosts).coerceAtLeast(0)
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore && hasMore && !isLoadingMore) {
                    onLoadMore()
                }
            }
    }

    // Pull-to-refresh with haptic feedback and minimal line indicator
    // Twitter/X style - thin animated gradient line at top
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onRefresh()
        },
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        indicator = {
            // Minimal line loader - Twitter/X style
            MinimalLineRefreshIndicator(
                isRefreshing = isRefreshing,
                pullProgress = pullToRefreshState.distanceFraction,
                accentColor = accentColor,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            flingBehavior = listFlingBehavior
        ) {
            item { Spacer(Modifier.height(4.dp)) }

        // Streak reminder banner at top (urgency driver) - ONLY when at risk
        if (showStreakReminder && connectionStreak > 0) {
            item {
                StreakReminderCard(
                    connectionStreak = connectionStreak,
                    isAtRisk = isStreakAtRisk,
                    backdrop = backdrop,
                    onDismiss = onDismissStreakReminder,
                    onAction = {
                        onDismissStreakReminder() // Also dismiss reminder when navigating
                        onNavigateToFindPeople()
                    }
                )
            }
        }

        // Login streak celebration - controlled by ViewModel (milestones only, 24hr cooldown)
        if (showLoginStreakBadge && !isLoading && posts.isNotEmpty()) {
            item {
                DismissableLoginStreakBadge(
                    loginStreak = loginStreak,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    onDismiss = onDismissLoginStreakBadge
                )
            }
        }

        // Onboarding prompt banner - show if user hasn't completed onboarding
        if (showOnboarding) {
            item {
                OnboardingPromptBanner(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onGetStarted = onNavigateToOnboarding
                )
            }
        }

        // Stories section - always show
        item {
            StoriesRow(
                storyGroups = storyGroups,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                currentUserProfileImage = currentUserProfileImage,
                currentUserInitials = currentUserInitials,
                onStoryClick = onStoryClick,
                onAddStoryClick = onAddStoryClick,
                onMyStoryClick = onMyStoryClick
            )
        }

        if (showReelsOnHome) {
            // Reels Preview Section - Instagram-like horizontal scrollable reels
            item {
                ReelsPreviewSection(
                    reels = reels,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isLoading = isLoadingReels,
                    onReelClick = onReelClick,
                    onSeeAllClick = onSeeAllReelsClick,
                    onCreateClick = onCreateReelClick,
                    reduceAnimations = reduceFeedCardMotion
                )
            }
        }

        // ==================== RETENTION FEATURES SECTION ====================

        // Stay Active Banner (like web's "Stay active – check your feed and connect with someone today")
        retentionState?.let { state ->
            item {
                StayActiveBanner(
                    liveActivity = state.liveActivity,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onViewFeed = { /* Already on feed */ },
                    onConnect = onNavigateToFindPeople
                )
            }

        }

        // Loading state - Skeleton loading
        if (showSkeletons && skeletonShimmer != null) {
            items(count = 3, key = { "feed_skeleton_$it" }) {
                PostSkeletonCard(
                    shimmerBrush = skeletonShimmer,
                    isLightTheme = isLightTheme
                )
            }
        }

        // Error state
        error?.let { errorMsg ->
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BasicText(errorMsg, style = TextStyle(contentColor, 14.sp))
                        Spacer(Modifier.height(8.dp))
                        LiquidButton(
                            onClick = onRefresh,
                            backdrop = backdrop
                        ) {
                            BasicText("Retry", style = TextStyle(contentColor, 14.sp))
                        }
                    }
                }
            }
        }

        // Empty state
        if (!isLoading && posts.isEmpty() && error == null) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "No posts yet. Be the first to share!",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                }
            }
        }

        // Posts + widgets: flattened list with stable keys and contentType for recycling
        if (showFeedbackUndo) {
            item(key = "recommendation_feedback_undo") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(contentColor.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText("Post hidden", style = TextStyle(contentColor, 14.sp))
                    BasicText(
                        "Undo",
                        style = TextStyle(accentColor, 14.sp, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.clickable(onClick = onUndoFeedback)
                    )
                }
            }
        }

        items(
            items = feedRows,
            key = { it.itemKey },
            contentType = { it.contentType }
        ) { row ->
            when (row) {
                is FeedListRow.PostItem -> {
                    Column {
                        if (
                            row.post.isBoosted ||
                            !row.post.reasonText.isNullOrBlank() ||
                            (isCurrentUserPremium && row.post.authorId == currentUserId)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicText(
                                    when {
                                        row.post.isBoosted -> "Boosted"
                                        !row.post.reasonText.isNullOrBlank() -> "Why this?  ${row.post.reasonText}"
                                        else -> "Your post"
                                    },
                                    style = TextStyle(contentColor.copy(alpha = 0.66f), 12.sp),
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCurrentUserPremium && row.post.authorId == currentUserId) {
                                    BasicText(
                                        "Boost",
                                        style = TextStyle(accentColor, 12.sp, fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .clickable { onBoostPost(row.post.id) }
                                    )
                                } else {
                                    BasicText(
                                        "Not interested",
                                        style = TextStyle(accentColor, 12.sp),
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .clickable { onNotInterested(row.post.id) }
                                    )
                                }
                            }
                        }
                        ApiPostCard(
                            post = row.post,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            glassBackgroundKey = glassBackgroundKey,
                            onLike = onLike,
                            onComment = onComment,
                            onShare = onShare,
                            onVotePoll = onVotePoll,
                            onProfileClick = { onProfileClick(row.post.author.id) },
                            onMentionClick = { username -> onProfileClick(username) },
                            onMenuAction = onMenuAction,
                            reduceAnimations = reduceFeedCardMotion,
                            playDefaultVideos = !reduceAnimations
                        )
                    }
                }
                is FeedListRow.NativeAdItem -> {
                    VormexNativeFeedAd(
                        slotKey = row.slotKey,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isLightTheme = isLightTheme
                    )
                }
                is FeedListRow.ManagedAdItem -> {
                    ManagedFeedAdCard(
                        ad = row.ad,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isLightTheme = isLightTheme,
                        onImpression = onManagedAdImpression,
                        onClick = onManagedAdClick
                    )
                }
                is FeedListRow.ServerModuleItem -> {
                    val placement = row.placement
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(contentColor.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        BasicText(
                            placement.label ?: when (placement.type.uppercase()) {
                                "REELS" -> "Reels for you"
                                "JOBS" -> "Jobs for you"
                                "EVENTS" -> "Events for you"
                                "BOOSTED_POST" -> "Boosted"
                                else -> "Recommended for you"
                            },
                            style = TextStyle(contentColor, 16.sp, fontWeight = FontWeight.SemiBold)
                        )
                        placement.reasonText?.let { reason ->
                            Spacer(Modifier.height(4.dp))
                            BasicText(reason, style = TextStyle(contentColor.copy(alpha = 0.65f), 13.sp))
                        }
                        val boostedPost = if (placement.type.equals("BOOSTED_POST", ignoreCase = true)) {
                            remember(placement.items) {
                                placement.items.firstOrNull()?.let { element ->
                                    runCatching {
                                        Json { ignoreUnknownKeys = true; coerceInputValues = true }
                                            .decodeFromJsonElement<Post>(element)
                                    }.getOrNull()
                                }
                            }
                        } else null
                        if (boostedPost != null) {
                            Spacer(Modifier.height(8.dp))
                            ApiPostCard(
                                post = boostedPost,
                                backdrop = backdrop,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                glassBackgroundKey = glassBackgroundKey,
                                onLike = onLike,
                                onComment = onComment,
                                onShare = onShare,
                                onVotePoll = onVotePoll,
                                onProfileClick = { onProfileClick(boostedPost.author.id) },
                                onMentionClick = { username -> onProfileClick(username) },
                                onMenuAction = onMenuAction,
                                reduceAnimations = reduceFeedCardMotion,
                                playDefaultVideos = !reduceAnimations
                            )
                        } else placement.items.take(4).forEach { item ->
                            val value = item as? JsonObject
                            val itemLabel = listOf("name", "title", "caption", "content")
                                .firstNotNullOfOrNull { key -> value?.get(key)?.jsonPrimitive?.contentOrNull }
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                            itemLabel?.let {
                                Spacer(Modifier.height(6.dp))
                                BasicText("• $it", style = TextStyle(contentColor.copy(alpha = 0.82f), 13.sp), maxLines = 2)
                            }
                        }
                    }
                }
                FeedListRow.WidgetPeopleLikeYou,
                FeedListRow.WidgetPeopleLikeYouFallback -> {
                    retentionState?.let { state ->
                        PeopleLikeYouSection(
                            people = state.peopleLikeYou,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onPersonClick = { userId -> onProfileClick(userId) },
                            onSeeAll = onNavigateToFindPeople
                        )
                    }
                }
                FeedListRow.WidgetTodaysMatches,
                FeedListRow.WidgetTodaysMatchesFallback -> {
                    retentionState?.let { state ->
                        TodaysMatchesSection(
                            matches = state.todaysMatches,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onMatchClick = { userId -> onProfileClick(userId) },
                            onConnect = { userId ->
                                scope.launch {
                                    ApiClient.sendConnectionRequest(context, userId)
                                        .onSuccess {
                                            Toast.makeText(
                                                context,
                                                "Connection request sent",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .onFailure { error ->
                                            Toast.makeText(
                                                context,
                                                error.message ?: "Could not send request",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            },
                            onSeeAll = onNavigateToFindPeople
                        )
                    }
                }
                FeedListRow.WidgetWeeklyGoals,
                FeedListRow.WidgetWeeklyGoalsFallback -> {
                    retentionState?.let { state ->
                        EngagementDashboardCard(
                            weeklyGoals = state.weeklyGoals,
                            streakData = state.streakData,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onWeeklyGoalsClick = onWeeklyGoalsClick,
                            onStreakDetailsClick = onStreakDetailsClick
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
    } // Close PullToRefreshBox
}

// Minimal Line Refresh Indicator - Twitter/X style
@Composable
private fun MinimalLineRefreshIndicator(
    isRefreshing: Boolean,
    pullProgress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    // Only show when pulling or refreshing
    val showIndicator = pullProgress > 0f || isRefreshing

    AnimatedVisibility(
        visible = showIndicator,
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .statusBarsPadding()
        ) {
            if (isRefreshing) {
                val infiniteTransition = rememberInfiniteTransition(label = "line_loader")
                val animatedOffset = infiniteTransition.animateFloat(
                    initialValue = -1f,
                    targetValue = 2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "gradient_offset"
                )
                // Animated gradient line during refresh
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    accentColor.copy(alpha = 0.3f),
                                    accentColor,
                                    accentColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                startX = animatedOffset.value * 500f,
                                endX = (animatedOffset.value + 1f) * 500f
                            )
                        )
                )
            } else {
                // Static progress line based on pull distance
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pullProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.5f),
                                    accentColor
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun StoriesRow(
    storyGroups: List<StoryGroup>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    currentUserProfileImage: String? = null,
    currentUserInitials: String = "U",
    onStoryClick: (Int) -> Unit = {},
    onAddStoryClick: () -> Unit = {},
    onMyStoryClick: () -> Unit = {}
) {
    // Find user's own story group
    val myStoryGroup = remember(storyGroups) { storyGroups.find { it.isOwnStory } }
    val hasMyStory = myStoryGroup != null && myStoryGroup.stories.isNotEmpty()

    // Filter out user's own story from the list (will be shown separately)
    val otherStoryGroups = remember(storyGroups) { storyGroups.filter { !it.isOwnStory } }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Your Story button - always shown first
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable {
                if (hasMyStory) {
                    onMyStoryClick()
                } else {
                    onAddStoryClick()
                }
            }
        ) {
            Box(
                Modifier.size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                // Profile image with story ring (if has story)
                Box(
                    Modifier
                        .size(if (hasMyStory) 76.dp else 72.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(38f.dp) },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                            },
                            onDrawSurface = {
                                if (hasMyStory && myStoryGroup.hasUnviewed) {
                                    drawRect(accentColor.copy(alpha = 0.4f))
                                } else if (hasMyStory) {
                                    drawRect(Color.Gray.copy(alpha = 0.3f))
                                } else {
                                    drawRect(Color.White.copy(alpha = 0.2f))
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner profile image
                    Box(
                        Modifier
                            .size(if (hasMyStory) 68.dp else 64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentUserProfileImage.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(currentUserProfileImage)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Your story",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(if (hasMyStory) 64.dp else 60.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                Modifier
                                    .size(if (hasMyStory) 64.dp else 60.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    currentUserInitials,
                                    style = TextStyle(Color.White, 20.sp, FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // "+" badge in bottom-right corner
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable { onAddStoryClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "+",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            BasicText(
                "Your story",
                style = TextStyle(contentColor.copy(alpha = 0.7f), 11.sp)
            )
        }

        // Other story groups
        otherStoryGroups.forEachIndexed { index, storyGroup ->
            // Find the original index in the full list for proper callback
            val originalIndex = storyGroups.indexOf(storyGroup)
            StoryItem(
                storyGroup = storyGroup,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = { onStoryClick(originalIndex) }
            )
        }
    }
}

@Composable
private fun StoryItem(
    storyGroup: StoryGroup,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit = {}
) {
    val userName = storyGroup.user.name ?: storyGroup.user.username ?: "User"
    val firstName = userName.split(" ").firstOrNull() ?: userName
    val displayName = if (firstName.length > 8) firstName.take(7) + "…" else firstName

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        // Story ring with glass effect
        Box(
            Modifier
                .size(76.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(38f.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                    },
                    onDrawSurface = {
                        if (storyGroup.hasUnviewed) {
                            drawRect(accentColor.copy(alpha = 0.4f))
                        } else {
                            drawRect(Color.Gray.copy(alpha = 0.3f))
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner profile image
            Box(
                Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                val profileImage = storyGroup.user.profileImage
                if (!profileImage.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profileImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = "$userName's story",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                } else {
                    val initials = userName.split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .take(2)
                        .joinToString("")
                        .ifEmpty { "U" }
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            initials,
                            style = TextStyle(Color.White, 20.sp, FontWeight.Bold)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        BasicText(
            displayName,
            style = TextStyle(contentColor.copy(alpha = 0.8f), 11.sp),
            maxLines = 1
        )
    }
}

@Composable
private fun MockPostCard(
    post: com.kyant.backdrop.catalog.linkedin.Post,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24f.dp) },
                effects = {
                    vibrancy()
                    blur(16f.dp.toPx())
                    lens(8f.dp.toPx(), 16f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.12f))
                }
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Author info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        post.author.avatarInitials,
                        style = TextStyle(Color.White, 16.sp, FontWeight.Bold)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    BasicText(
                        post.author.name,
                        style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                    )
                    BasicText(
                        post.author.headline,
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        post.timeAgo,
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                    )
                }
            }

            // Post content
            FormattedContent(
                content = post.content,
                contentColor = contentColor,
                accentColor = accentColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            // Image placeholder if has image
            if (post.hasImage) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("📷 Image", style = TextStyle(contentColor.copy(alpha = 0.5f), 16.sp))
                }
            }

            // Engagement stats
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicText(
                    "👍 ${post.likes}",
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                )
                BasicText(
                    "${post.comments} comments",
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                )
            }

            // Divider
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(contentColor.copy(alpha = 0.1f))
            )

            // Action buttons - removed Repost
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton("👍", "Like", contentColor)
                ActionButton("💬", "Comment", contentColor)
                ActionButton("📤", "Share", contentColor)
            }
        }
    }
}

@Composable
private fun ActionButton(icon: String, label: String, contentColor: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(icon, style = TextStyle(fontSize = 16.sp))
        Spacer(Modifier.width(4.dp))
        BasicText(label, style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp))
    }
}

@Composable
private fun FindPeopleScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    retentionViewModel: RetentionViewModel? = null
) {
    val retentionState = retentionViewModel?.uiState?.collectAsState()?.value

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Search Header
        Box(
            Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(24f.dp) },
                    effects = {
                        vibrancy()
                        blur(12f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.15f))
                    }
                )
                .padding(16.dp)
        ) {
            Column {
                BasicText(
                    "Find People",
                    style = TextStyle(contentColor, 20.sp, FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    "Discover and connect with others",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp)
                )
            }
        }

        // Connection limit indicator (Scarcity feature)
        retentionState?.connectionLimit?.let { limit ->
            ConnectionLimitIndicator(
                limitData = limit,
                contentColor = contentColor,
                accentColor = accentColor
            )
        }

        // Suggested connections
        BasicText(
            "Suggested for you",
            Modifier.padding(start = 4.dp, top = 8.dp),
            style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
        )

        MockData.users.filter { !it.isConnected }.forEach { user ->
            ConnectionCard(user, backdrop, contentColor, accentColor)
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ConnectionCard(
    user: User,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24f.dp) },
                effects = {
                    vibrancy()
                    blur(12f.dp.toPx())
                    lens(6f.dp.toPx(), 12f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.12f))
                }
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    user.avatarInitials,
                    style = TextStyle(Color.White, 14.sp, FontWeight.Bold)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                BasicText(
                    user.name,
                    style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold)
                )
                BasicText(
                    user.headline,
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 11.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    "${user.connections} connections",
                    style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp)
                )
            }
            Spacer(Modifier.width(8.dp))
            LiquidButton(
                onClick = { },
                backdrop = backdrop,
                modifier = Modifier.height(32.dp),
                tint = accentColor
            ) {
                BasicText(
                    "Connect",
                    Modifier.padding(horizontal = 10.dp),
                    style = TextStyle(Color.White, 12.sp, FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
private fun PostScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    viewModel: FeedViewModel,
    isCreatingPost: Boolean,
    createError: String?,
    onPostCreated: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var postType by remember { mutableIntStateOf(0) }
    var imageBytes by remember { mutableStateOf<List<Pair<ByteArray, String>>>(emptyList()) }
    var videoBytes by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    var imagePreviewUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var videoPreviewUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                val filename = it.lastPathSegment ?: "image.jpg"
                imageBytes = imageBytes + (bytes to filename)
                imagePreviewUris = imagePreviewUris + it
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                val filename = it.lastPathSegment ?: "video.mp4"
                videoBytes = bytes to filename
                videoPreviewUri = it
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp, bottom = 100.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BasicText(
                    "Create a post",
                    style = TextStyle(contentColor, 24.sp, FontWeight.Bold)
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Text" to "TEXT", "Image" to "IMAGE", "Video" to "VIDEO").forEachIndexed { index, (label, _) ->
                        val selected = postType == index
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) accentColor.copy(alpha = 0.3f) else Color.Transparent)
                                .clickable { postType = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                label,
                                style = TextStyle(
                                    color = if (selected) accentColor else contentColor.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }

                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                        .padding(16.dp),
                    textStyle = TextStyle(contentColor, 16.sp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (content.isEmpty()) {
                                BasicText(
                                    "What do you want to talk about?",
                                    style = TextStyle(contentColor.copy(alpha = 0.5f), 16.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (imagePreviewUris.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        imagePreviewUris.forEachIndexed { index, uri ->
                            Box(
                                Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(contentColor.copy(alpha = 0.1f))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(uri).build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable {
                                            imageBytes = imageBytes.filterIndexed { i, _ -> i != index }
                                            imagePreviewUris = imagePreviewUris.filterIndexed { i, _ -> i != index }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText("×", style = TextStyle(Color.White, 16.sp, FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                if (videoPreviewUri != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(contentColor.copy(alpha = 0.1f))
                    ) {
                        BasicText(
                            "Video attached",
                            Modifier
                                .align(Alignment.Center)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            style = TextStyle(Color.White, 12.sp)
                        )
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable {
                                    videoBytes = null
                                    videoPreviewUri = null
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("×", style = TextStyle(Color.White, 14.sp, FontWeight.Bold))
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PostOption("📷", "Photo", contentColor) { imagePicker.launch("image/*") }
                    PostOption("🎥", "Video", contentColor) { videoPicker.launch("video/*") }
                }

                if (createError != null) {
                    BasicText(
                        createError,
                        style = TextStyle(Color(0xFFE53935), 14.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                LiquidButton(
                    onClick = {
                        val type = when (postType) {
                            1 -> "IMAGE"
                            2 -> "VIDEO"
                            else -> "TEXT"
                        }
                        if (type == "IMAGE" && imageBytes.isEmpty()) return@LiquidButton
                        if (type == "VIDEO" && videoBytes == null) return@LiquidButton
                        if (type == "TEXT" && content.isBlank()) return@LiquidButton
                        viewModel.createPost(
                            type = type,
                            content = content.ifBlank { " " },
                            imageBytes = imageBytes,
                            videoBytes = videoBytes,
                            onSuccess = {
                                content = ""
                                imageBytes = emptyList()
                                imagePreviewUris = emptyList()
                                videoBytes = null
                                videoPreviewUri = null
                                onPostCreated()
                            }
                        )
                    },
                    backdrop = backdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    tint = accentColor
                ) {
                    if (isCreatingPost) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        BasicText(
                            "Post",
                            style = TextStyle(Color.White, 16.sp, FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostOption(
    icon: String,
    label: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        BasicText(icon, style = TextStyle(fontSize = 24.sp))
        BasicText(label, style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp))
    }
}

@Composable
private fun MoreScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    retentionState: RetentionUiState,
    currentUser: com.kyant.backdrop.catalog.network.models.User? = null,
    connectionRequests: List<PendingConnectionRequest> = emptyList(),
    isLoadingConnectionRequests: Boolean = false,
    connectionRequestsError: String? = null,
    hasPendingGroupMessages: Boolean = false,
    showFullMoreScreen: Boolean = false,
    premiumDetailsRequestKey: Int = 0,
    crossedPathsRequestKey: Int = 0,
    onCrossedPathsVisibilityChanged: (Boolean) -> Unit = {},
    quickHubAnimationKey: Int = 0,
    onOpenFullMoreScreen: () -> Unit = {},
    onCloseFullMoreScreen: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToConnectionRequests: () -> Unit = {},
    onNavigateToProfileCustomizations: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToCircles: () -> Unit = {},
    onNavigateToReels: () -> Unit = {},
    onNavigateToWeeklyGoals: () -> Unit = {},
    onNavigateToStreakDetails: () -> Unit = {},
    onNavigateToTopNetworkers: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToSavedPosts: () -> Unit = {},
    onNavigateToProfileInsights: () -> Unit = {},
    onNavigateToGrowthHub: () -> Unit = {},
    onNavigateToGames: () -> Unit = {},
    onNavigateToTalentEngine: () -> Unit = {},
    onNavigateToSkillPassport: () -> Unit = {},
    onNavigateToSkillSwap: () -> Unit = {},
    onNavigateToHackathons: () -> Unit = {},
    onOpenAiChat: () -> Unit = {},
    onOpenAgent: () -> Unit = {},

    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToPrivacySettings: () -> Unit = {},
    onNavigateToIdentitySafety: () -> Unit = {},
    onNavigateToAppearanceSettings: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToInviteFriends: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToContact: () -> Unit = {},
    onSwitchAccount: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var showPremiumDetailsScreen by rememberSaveable { mutableStateOf(false) }
    var showCreatorProDetailsScreen by rememberSaveable { mutableStateOf(false) }
    var showCrossedPathsScreen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showCrossedPathsScreen) {
        onCrossedPathsVisibilityChanged(showCrossedPathsScreen)
    }
    DisposableEffect(Unit) {
        onDispose { onCrossedPathsVisibilityChanged(false) }
    }

    LaunchedEffect(crossedPathsRequestKey) {
        if (crossedPathsRequestKey > 0) showCrossedPathsScreen = true
    }

    val pendingRequestsCount = connectionRequests.size
    val pendingRequestsBadge = when {
        pendingRequestsCount <= 0 -> null
        pendingRequestsCount > 99 -> "99+"
        else -> pendingRequestsCount.toString()
    }
    val quickActions = buildList {
        add(
            MoreQuickAction(
                title = "Connection requests",
                label = "Requests",
                icon = Icons.Outlined.PersonAddAlt1,
                badgeLabel = pendingRequestsBadge,
                showIndicatorDot = pendingRequestsCount > 0,
                onClick = onNavigateToConnectionRequests
            )
        )
        add(
            MoreQuickAction(
                title = "Saved",
                label = "Saved",
                icon = Icons.Outlined.BookmarkBorder,
                onClick = onNavigateToSavedPosts
            )
        )
        add(
            MoreQuickAction(
                title = "Reels",
                label = "Reels",
                icon = Icons.Outlined.SmartDisplay,
                onClick = onNavigateToReels
            )
        )
        add(
            MoreQuickAction(
                title = "Groups",
                label = "Groups",
                icon = Icons.Outlined.Groups,
                showIndicatorDot = hasPendingGroupMessages,
                onClick = onNavigateToGroups
            )
        )
        add(
            MoreQuickAction(
                title = "Hackathons",
                label = "Hacks",
                icon = Icons.Outlined.EmojiEvents,
                onClick = onNavigateToHackathons
            )
        )
        add(
            MoreQuickAction(
                title = "Growth hub",
                label = "Soon",
                icon = Icons.Outlined.School,
                onClick = onNavigateToGrowthHub
            )
        )
    }

    BackHandler(enabled = showPremiumDetailsScreen) {
        showPremiumDetailsScreen = false
    }

    BackHandler(enabled = showCreatorProDetailsScreen) {
        showCreatorProDetailsScreen = false
    }

    BackHandler(enabled = showCrossedPathsScreen) { showCrossedPathsScreen = false }

    LaunchedEffect(premiumDetailsRequestKey) {
        if (premiumDetailsRequestKey > 0) {
            showPremiumDetailsScreen = true
        }
    }

    if (showCrossedPathsScreen) {
        CrossedPathsScreen(
            onBack = { showCrossedPathsScreen = false },
            onOpenProfile = { userId ->
                onNavigateToUserProfile(userId)
            },
        )
    } else if (showCreatorProDetailsScreen) {
        MoreCreatorProDetailsScreen(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            currentUserId = currentUser?.id,
            onNavigateBack = { showCreatorProDetailsScreen = false }
        )
    } else if (showPremiumDetailsScreen) {
        MorePremiumDetailsScreen(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            currentUserId = currentUser?.id,
            onNavigateBack = { showPremiumDetailsScreen = false },
            onOpenAgent = onOpenAgent,

        )
    } else if (showFullMoreScreen) {
        MoreFullScreen(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            retentionState = retentionState,
            currentUser = currentUser,
            connectionRequests = connectionRequests,
            isLoadingConnectionRequests = isLoadingConnectionRequests,
            connectionRequestsError = connectionRequestsError,
            hasPendingGroupMessages = hasPendingGroupMessages,
            hiddenQuickActionTitles = emptySet(),
            onNavigateBack = onCloseFullMoreScreen,
            onOpenPremiumDetails = { showPremiumDetailsScreen = true },
            onOpenCreatorProDetails = { showCreatorProDetailsScreen = true },
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToConnectionRequests = onNavigateToConnectionRequests,
            onNavigateToProfileCustomizations = onNavigateToProfileCustomizations,
            onNavigateToGroups = onNavigateToGroups,
            onNavigateToCircles = onNavigateToCircles,
            onNavigateToReels = onNavigateToReels,
            onNavigateToWeeklyGoals = onNavigateToWeeklyGoals,
            onNavigateToStreakDetails = onNavigateToStreakDetails,
            onNavigateToCrossedPaths = { showCrossedPathsScreen = true },
            onNavigateToTopNetworkers = onNavigateToTopNetworkers,
            onNavigateToOnboarding = onNavigateToOnboarding,
            onNavigateToSavedPosts = onNavigateToSavedPosts,
            onNavigateToProfileInsights = onNavigateToProfileInsights,
            onNavigateToGrowthHub = onNavigateToGrowthHub,
            onNavigateToGames = onNavigateToGames,
            onNavigateToTalentEngine = onNavigateToTalentEngine,
            onNavigateToSkillPassport = onNavigateToSkillPassport,
            onNavigateToSkillSwap = onNavigateToSkillSwap,
            onNavigateToHackathons = onNavigateToHackathons,
            onOpenAiChat = onOpenAiChat,
            onOpenAgent = onOpenAgent,
            onNavigateToNotificationSettings = onNavigateToNotificationSettings,
            onNavigateToPrivacySettings = onNavigateToPrivacySettings,
            onNavigateToIdentitySafety = onNavigateToIdentitySafety,
            onNavigateToAppearanceSettings = onNavigateToAppearanceSettings,
            onNavigateToHelp = onNavigateToHelp,
            onNavigateToInviteFriends = onNavigateToInviteFriends,
            onNavigateToAbout = onNavigateToAbout,
            onNavigateToContact = onNavigateToContact,
            onSwitchAccount = onSwitchAccount,
            onLogout = onLogout
        )
    } else {
        MoreQuickAccessHub(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            quickActions = quickActions,
            animationKey = quickHubAnimationKey,
            onOpenFullMoreScreen = onOpenFullMoreScreen
        )
    }
}

@Composable
private fun MoreFullScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    retentionState: RetentionUiState,
    currentUser: com.kyant.backdrop.catalog.network.models.User? = null,
    connectionRequests: List<PendingConnectionRequest> = emptyList(),
    isLoadingConnectionRequests: Boolean = false,
    connectionRequestsError: String? = null,
    hasPendingGroupMessages: Boolean = false,
    hiddenQuickActionTitles: Set<String> = emptySet(),
    onNavigateBack: () -> Unit = {},
    onOpenPremiumDetails: () -> Unit = {},
    onOpenCreatorProDetails: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToConnectionRequests: () -> Unit = {},
    onNavigateToProfileCustomizations: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToCircles: () -> Unit = {},
    onNavigateToReels: () -> Unit = {},
    onNavigateToWeeklyGoals: () -> Unit = {},
    onNavigateToStreakDetails: () -> Unit = {},
    onNavigateToCrossedPaths: () -> Unit = {},
    onNavigateToTopNetworkers: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToSavedPosts: () -> Unit = {},
    onNavigateToProfileInsights: () -> Unit = {},
    onNavigateToGrowthHub: () -> Unit = {},
    onNavigateToGames: () -> Unit = {},
    onNavigateToTalentEngine: () -> Unit = {},
    onNavigateToSkillPassport: () -> Unit = {},
    onNavigateToSkillSwap: () -> Unit = {},
    onNavigateToHackathons: () -> Unit = {},
    onOpenAiChat: () -> Unit = {},
    onOpenAgent: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToPrivacySettings: () -> Unit = {},
    onNavigateToIdentitySafety: () -> Unit = {},
    onNavigateToAppearanceSettings: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToInviteFriends: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToContact: () -> Unit = {},
    onSwitchAccount: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val goalsData = retentionState.weeklyGoals
    val goalsProgressText = if (goalsData.goals.isNotEmpty()) {
        "${(goalsData.totalProgress * 100).toInt()}% complete"
    } else {
        "Start tracking"
    }
    val isDarkSurface = rememberMoreUsesDarkSurface(isGlassTheme = isGlassTheme)
    val pageBackground = Color.Transparent
    val sectionSurfaceColor = if (isDarkSurface) {
        Color.White.copy(alpha = if (isGlassTheme) 0.03f else 0.02f)
    } else {
        Color.White.copy(alpha = if (isGlassTheme) 0.08f else 0.18f)
    }
    val dividerColor = if (isDarkSurface) {
        Color.White.copy(alpha = if (isGlassTheme) 0.10f else 0.08f)
    } else {
        Color.Black.copy(alpha = if (isGlassTheme) 0.10f else 0.07f)
    }
    val groupDividerColor = if (isDarkSurface) {
        Color.White.copy(alpha = if (isGlassTheme) 0.12f else 0.08f)
    } else {
        Color.Black.copy(alpha = if (isGlassTheme) 0.10f else 0.06f)
    }
    val secondaryTextColor = contentColor.copy(alpha = if (isDarkSurface) 0.66f else 0.58f)
    val sectionHeaderColor = contentColor.copy(alpha = if (isDarkSurface) 0.68f else 0.56f)
    val destructiveColor = if (isDarkSurface) Color(0xFFFF7A7A) else Color(0xFFD33A3A)
    val pendingRequestsCount = connectionRequests.size
    val hasPremiumAccess = currentUser?.isPremium == true
    val canAccessProfileCustomization = currentUser?.canAccessProfileCustomization == true
    val connectionRequestsSubtitle = when {
        isLoadingConnectionRequests && pendingRequestsCount == 0 -> "Checking for incoming requests"
        pendingRequestsCount == 0 && connectionRequestsError != null -> "Open to retry loading requests"
        pendingRequestsCount == 0 -> "People who send requests will show up here"
        pendingRequestsCount == 1 -> "1 person is waiting for your reply"
        else -> "$pendingRequestsCount people are waiting for your reply"
    }
    val accountCenterSubtitle = currentUser?.let { user ->
        listOfNotNull(
            user.name?.takeIf { it.isNotBlank() },
            user.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
        ).joinToString(" • ").ifBlank { "Profile and account details" }
    } ?: "Profile, security, saved accounts and preferences"

    val sections = listOf(
        MoreSettingsSection(
            title = "Your account",
            items = buildList {
                add(
                    MoreSettingsItem(
                        title = "Account center",
                        subtitle = accountCenterSubtitle,
                        icon = Icons.Outlined.PersonOutline,
                        onClick = onNavigateToProfile,
                        searchTerms = listOf("profile", "account", "security", "details", "saved accounts")
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Profile preferences",
                        subtitle = "Goals, interests and matching settings",
                        icon = Icons.Outlined.Description,
                        onClick = onNavigateToOnboarding
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Customize your profile",
                        subtitle = "Frames, avatar preview and profile loaders",
                        icon = Icons.Outlined.Palette,
                        onClick = onNavigateToProfileCustomizations,
                        trailingLabel = if (canAccessProfileCustomization) null else "Premium",
                        searchTerms = listOf(
                            "profile customization",
                            "custom",
                            "customizations",
                            "loader",
                            "loaders",
                            "visitor loader",
                            "visitor",
                            "frame",
                            "avatar",
                            "gift",
                            "wolf",
                            "wolfie",
                            "morty",
                            "dance"
                        )
                    )
                )
            }
        ),
        MoreSettingsSection(
            title = "How you use Vormex",
            items = listOfNotNull(
                MoreSettingsItem(
                    title = "Saved",
                    subtitle = "Profiles, posts and reels you've bookmarked",
                    icon = Icons.Outlined.BookmarkBorder,
                    onClick = onNavigateToSavedPosts
                ),
                MoreSettingsItem(
                    title = "Connection requests",
                    subtitle = connectionRequestsSubtitle,
                    icon = Icons.Outlined.PersonAddAlt1,
                    onClick = onNavigateToConnectionRequests,
                    trailingLabel = pendingRequestsCount.takeIf { it > 0 }?.toString(),
                    showIndicatorDot = pendingRequestsCount > 0
                ),
                MoreSettingsItem(
                    title = "Streaks & activity",
                    subtitle = "Networking, login, posting, messaging",
                    icon = Icons.Outlined.Schedule,
                    onClick = onNavigateToStreakDetails
                ),
                MoreSettingsItem(
                    title = "Crossed Paths",
                    subtitle = "People you encountered nearby in the last 7 days",
                    icon = Icons.Outlined.PersonPinCircle,
                    onClick = onNavigateToCrossedPaths,
                    searchTerms = listOf("nearby", "event", "meetup", "location", "people")
                ),
                MoreSettingsItem(
                    title = "Permissions & notifications",
                    subtitle = "System access, push alerts, location",
                    icon = Icons.Outlined.AdminPanelSettings,
                    onClick = onNavigateToNotificationSettings
                ),
                MoreSettingsItem(
                    title = "Weekly goals",
                    subtitle = goalsProgressText,
                    icon = Icons.Outlined.TrackChanges,
                    onClick = onNavigateToWeeklyGoals,
                    trailingLabel = if (goalsData.streakAtRisk) "At risk" else null,
                    showIndicatorDot = goalsData.streakAtRisk
                ),
                MoreSettingsItem(
                    title = "Reels",
                    subtitle = "Watch short videos",
                    icon = Icons.Outlined.SmartDisplay,
                    onClick = onNavigateToReels
                ),
                MoreSettingsItem(
                    title = "Games",
                    subtitle = "Games section",
                    icon = Icons.Outlined.SportsEsports,
                    onClick = onNavigateToGames
                )
            )
        ),
        MoreSettingsSection(
            title = "For professionals",
            items = buildList {
                add(
                    MoreSettingsItem(
                        title = "Premium",
                        subtitle = "Priority reach, profile tools and AI assistance",
                        icon = Icons.Outlined.WorkspacePremium,
                        onClick = onOpenPremiumDetails,
                        trailingLabel = if (hasPremiumAccess) "Active" else "Not subscribed",
                        searchTerms = listOf("premium", "pro", "upgrade", "subscription", "member", "gift")
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Creator Pro",
                        subtitle = "Audience analytics, paid sessions and creator amplification",
                        icon = Icons.Outlined.WorkspacePremium,
                        onClick = onOpenCreatorProDetails,
                        searchTerms = listOf(
                            "creator",
                            "creator pro",
                            "monetized",
                            "paid dm",
                            "paid sessions",
                            "analytics",
                            "portfolio",
                            "showcase"
                        )
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Profile insights",
                        subtitle = "Who viewed you, saved you and matched with you",
                        icon = Icons.Outlined.Insights,
                        onClick = if (hasPremiumAccess) onNavigateToProfileInsights else onOpenPremiumDetails,
                        trailingLabel = if (hasPremiumAccess) null else "Premium",
                        searchTerms = listOf("insights", "analytics", "profile views", "saved", "bookmarked", "match rate")
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "vormex",
                        subtitle = "Ask vormex to help inside the app",
                        icon = Icons.Outlined.ChatBubbleOutline,
                        onClick = onOpenAiChat,
                        searchTerms = listOf("ai", "agent", "vormex", "chat", "assistant")
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Talent Engine",
                        subtitle = "Selected -> trained -> tasked -> placed",
                        icon = Icons.Outlined.TrackChanges,
                        onClick = onNavigateToTalentEngine,
                        trailingLabel = "New",
                        showIndicatorDot = true,
                        searchTerms = listOf(
                            "talent",
                            "talent engine",
                            "training",
                            "tasks",
                            "opportunities",
                            "placement",
                            "cohort",
                            "vormex team"
                        )
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Hackathons",
                        subtitle = "Find events, form teams, and open team chats",
                        icon = Icons.Outlined.EmojiEvents,
                        onClick = onNavigateToHackathons,
                        searchTerms = listOf("hackathon", "hackathons", "devfolio", "mlh", "team", "college fest")
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Growth hub",
                        subtitle = "Career growth tools open soon",
                        icon = Icons.Outlined.School,
                        onClick = onNavigateToGrowthHub,
                        trailingLabel = "Locked",
                        searchTerms = listOf("growth", "career", "jobs", "learning", "ai coach", "rewards", "opens soon")
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Skill Passport",
                        subtitle = "Verified skills and proof of work",
                        icon = Icons.Outlined.Verified,
                        onClick = onNavigateToSkillPassport,
                        searchTerms = listOf("skill", "passport", "verified", "proof", "portfolio")
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Skill Swap",
                        subtitle = "Trade skills and learn with others",
                        icon = Icons.Outlined.Groups,
                        onClick = onNavigateToSkillSwap,
                        searchTerms = listOf("skill", "swap", "learn", "mentor", "teach")
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Top networkers",
                        subtitle = "Weekly and monthly leaderboard",
                        icon = Icons.Outlined.EmojiEvents,
                        onClick = onNavigateToTopNetworkers
                    )
                )
            }
        ),
        MoreSettingsSection(
            title = "Communities",
            items = listOf(
                MoreSettingsItem(
                    title = "Groups",
                    subtitle = "Connect with communities",
                    icon = Icons.Outlined.Groups,
                    showIndicatorDot = hasPendingGroupMessages,
                    onClick = onNavigateToGroups
                ),
                MoreSettingsItem(
                    title = "Circles",
                    subtitle = "Share with close friends",
                    icon = Icons.Default.FavoriteBorder,
                    onClick = onNavigateToCircles
                )
            )
        ),
        MoreSettingsSection(
            title = "Who can see your content",
            items = buildList {
                add(
                    MoreSettingsItem(
                        title = "Privacy",
                        subtitle = "Profile visibility and messaging",
                        icon = Icons.Outlined.Lock,
                        onClick = onNavigateToPrivacySettings
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Identity & Safety",
                        subtitle = "Student verification, ID review, reports and blocks",
                        icon = Icons.Outlined.Shield,
                        onClick = onNavigateToIdentitySafety,
                        searchTerms = listOf("identity", "safety", "student", "verify", "verification", "id", "block", "report")
                    )
                )
            }
        ),
        MoreSettingsSection(
            title = "App settings",
            items = listOf(
                MoreSettingsItem(
                    title = "Appearance",
                    subtitle = "Theme, font and accessibility",
                    icon = Icons.Outlined.Palette,
                    onClick = onNavigateToAppearanceSettings
                )
            )
        ),
        MoreSettingsSection(
            title = "Support and legal",
            items = listOf(
                MoreSettingsItem(
                    title = "Help & FAQ",
                    subtitle = "Getting started and troubleshooting",
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    onClick = onNavigateToHelp
                ),
                MoreSettingsItem(
                    title = "Invite friends",
                    subtitle = "Share Vormex with others",
                    icon = Icons.Outlined.CardGiftcard,
                    onClick = onNavigateToInviteFriends
                ),
                MoreSettingsItem(
                    title = "About",
                    subtitle = "Version, terms and privacy policy",
                    icon = Icons.Outlined.Info,
                    onClick = onNavigateToAbout
                ),
                MoreSettingsItem(
                    title = "Contact us",
                    subtitle = "Support and feedback",
                    icon = Icons.Outlined.AlternateEmail,
                    onClick = onNavigateToContact
                )
            )
        ),
        MoreSettingsSection(
            title = "Account actions",
            items = listOf(
                MoreSettingsItem(
                    title = "Switch account",
                    subtitle = "Choose a saved account on this device",
                    icon = Icons.Outlined.PersonOutline,
                    onClick = onSwitchAccount
                ),
                MoreSettingsItem(
                    title = "Log out",
                    subtitle = "Sign out of your account on this device",
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    onClick = onLogout,
                    isDestructive = true
                )
            )
        )
    )

    val visibleSections = sections.mapNotNull { section ->
        val visibleItems = section.items.filterNot { it.title in hiddenQuickActionTitles }
        if (visibleItems.isNotEmpty()) {
            section.copy(items = visibleItems)
        } else {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
    ) {
        MoreFullScreenTopBar(
            title = "Settings and activity",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(dividerColor)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp),
        ) {
            visibleSections.forEachIndexed { index, section ->
                if (index > 0) {
                    MoreSettingsGroupDivider(groupDividerColor)
                }

                MoreSettingsListSectionHeader(
                    title = section.title,
                    contentColor = sectionHeaderColor,
                    trailingLabel = if (section.title == "Your account") "Vormex" else null
                )
                MoreSettingsSectionCard(
                    items = section.items,
                    backdrop = backdrop,
                    isGlassTheme = isGlassTheme,
                    surfaceColor = sectionSurfaceColor,
                    borderColor = dividerColor,
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    accentColor = accentColor,
                    destructiveColor = destructiveColor
                )
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

private data class MoreQuickAction(
    val title: String,
    val label: String,
    val icon: ImageVector,
    val badgeLabel: String? = null,
    val showIndicatorDot: Boolean = false,
    val onClick: () -> Unit
)

private data class PremiumHighlight(
    val title: String,
    val detail: String,
    val icon: ImageVector
)

private data class PremiumMembershipPalette(
    val pageBackground: Color,
    val contentColor: Color,
    val secondaryTextColor: Color,
    val metaTextColor: Color,
    val accentColor: Color,
    val featureAccentColor: Color,
    val dividerColor: Color,
    val closeBackgroundColor: Color,
    val closeIconColor: Color,
    val headerTextColor: Color,
    val glassPanelSurfaceColor: Color,
    val glassPanelBorderColor: Color,
    val planSurfaceColor: Color,
    val planSelectedSurfaceColor: Color,
    val planSelectedTextColor: Color,
    val planUnselectedTextColor: Color,
    val reachCardSurfaceColor: Color,
    val reachCardLabelSurfaceColor: Color,
    val reachCardLabelTextColor: Color,
    val reachCardTitleColor: Color,
    val reachCardDetailColor: Color,
    val aiCardSurfaceColor: Color,
    val aiCardLabelSurfaceColor: Color,
    val aiCardLabelTextColor: Color,
    val aiCardTitleColor: Color,
    val aiCardDetailColor: Color,
    val ctaSurfaceColor: Color,
    val ctaContentColor: Color,
    val activeCtaSurfaceColor: Color,
    val activeCtaContentColor: Color,
    val statusSurfaceColor: Color,
    val statusErrorTextColor: Color
)

@Composable
private fun rememberPremiumMembershipPalette(
    isGlassTheme: Boolean,
    baseContentColor: Color,
    baseAccentColor: Color
): PremiumMembershipPalette {
    val appearance = currentVormexAppearance()
    val usesDarkSurface = rememberMoreUsesDarkSurface(isGlassTheme = isGlassTheme)
    val useDarkPremium = appearance.isDarkTheme || usesDarkSurface
    val neonAccent = Color(0xFFCDFF00)
    val editorialAccent = Color(0xFFFF6240)
    val resolvedAccent = when {
        useDarkPremium -> neonAccent
        isGlassTheme -> baseAccentColor
        else -> editorialAccent
    }
    val useLightTextOnAccent = !useDarkPremium && resolvedAccent.luminance() < 0.42f
    val pageBackground = when {
        isGlassTheme -> Color.Transparent
        useDarkPremium -> appearance.backgroundColor
        appearance.mode == VormexThemeMode.WarmPaper -> Color(0xFFF5F1EA)
        else -> appearance.backgroundColor
    }
    val content = when {
        useDarkPremium -> baseContentColor.takeIf { it.luminance() > 0.45f } ?: Color.White.copy(alpha = 0.94f)
        isGlassTheme -> baseContentColor
        else -> Color(0xFF1E1C18)
    }
    val secondary = if (useDarkPremium) {
        content.copy(alpha = 0.62f)
    } else {
        Color(0xFF736F68)
    }
    val meta = if (useDarkPremium) {
        content.copy(alpha = 0.42f)
    } else {
        Color(0xFFA9A198)
    }
    val divider = when {
        isGlassTheme && useDarkPremium -> Color.White.copy(alpha = 0.14f)
        isGlassTheme -> Color.White.copy(alpha = 0.30f)
        useDarkPremium -> appearance.dividerColor.copy(alpha = 0.95f)
        else -> Color(0xFFE5DED2)
    }

    return PremiumMembershipPalette(
        pageBackground = pageBackground,
        contentColor = content,
        secondaryTextColor = secondary,
        metaTextColor = meta,
        accentColor = resolvedAccent,
        featureAccentColor = if (useDarkPremium) neonAccent else Color(0xFF8FC900),
        dividerColor = divider,
        closeBackgroundColor = if (useDarkPremium) {
            Color.White.copy(alpha = if (isGlassTheme) 0.12f else 0.08f)
        } else {
            Color(0xFFE9E4DA).copy(alpha = if (isGlassTheme) 0.72f else 1f)
        },
        closeIconColor = if (useDarkPremium) content.copy(alpha = 0.74f) else Color(0xFF77716A),
        headerTextColor = meta,
        glassPanelSurfaceColor = if (useDarkPremium) {
            Color.Black.copy(alpha = 0.34f)
        } else {
            Color.White.copy(alpha = 0.36f)
        },
        glassPanelBorderColor = if (useDarkPremium) {
            Color.White.copy(alpha = 0.14f)
        } else {
            Color.White.copy(alpha = 0.34f)
        },
        planSurfaceColor = if (useDarkPremium) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.92f),
        planSelectedSurfaceColor = if (useDarkPremium) neonAccent else Color(0xFF1F1C18),
        planSelectedTextColor = if (useDarkPremium) Color(0xFF111111) else Color.White,
        planUnselectedTextColor = secondary,
        reachCardSurfaceColor = if (useDarkPremium) Color(0xFF101010) else Color(0xFF1F1C18),
        reachCardLabelSurfaceColor = Color.White.copy(alpha = if (useDarkPremium) 0.09f else 0.10f),
        reachCardLabelTextColor = Color.White.copy(alpha = 0.74f),
        reachCardTitleColor = Color.White,
        reachCardDetailColor = Color.White.copy(alpha = 0.68f),
        aiCardSurfaceColor = resolvedAccent,
        aiCardLabelSurfaceColor = Color.Black.copy(alpha = if (useDarkPremium) 0.14f else 0.10f),
        aiCardLabelTextColor = when {
            useDarkPremium -> Color(0xFF284000)
            useLightTextOnAccent -> Color.White.copy(alpha = 0.78f)
            else -> Color(0xFF5D2619)
        },
        aiCardTitleColor = if (useLightTextOnAccent) Color.White else Color(0xFF111111),
        aiCardDetailColor = when {
            useDarkPremium -> Color(0xFF334500)
            useLightTextOnAccent -> Color.White.copy(alpha = 0.74f)
            else -> Color(0xFF6A291C)
        },
        ctaSurfaceColor = if (useDarkPremium) neonAccent else Color(0xFF1F1C18),
        ctaContentColor = if (useDarkPremium) Color(0xFF111111) else Color.White,
        activeCtaSurfaceColor = neonAccent,
        activeCtaContentColor = Color(0xFF111111),
        statusSurfaceColor = if (useDarkPremium) {
            Color.White.copy(alpha = if (isGlassTheme) 0.08f else 0.06f)
        } else {
            Color.White.copy(alpha = if (isGlassTheme) 0.34f else 0.54f)
        },
        statusErrorTextColor = if (useDarkPremium) Color(0xFFFF947A) else Color(0xFFB33A25)
    )
}

@Composable
private fun MorePremiumSection(
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    sectionSurfaceColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    sectionHeaderColor: Color,
    currentUser: com.kyant.backdrop.catalog.network.models.User?,
    onOpenPremiumDetails: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MoreSectionHeader("Premium", sectionHeaderColor)
        MorePremiumEntryCard(
            backdrop = backdrop,
            isGlassTheme = isGlassTheme,
            sectionSurfaceColor = sectionSurfaceColor,
            dividerColor = dividerColor,
            accentColor = accentColor,
            onOpenPremiumDetails = onOpenPremiumDetails
        )
    }
}

@Composable
private fun MoreCreatorProDetailsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    currentUserId: String?,
    onNavigateBack: () -> Unit
) {
    val palette = rememberPremiumMembershipPalette(
        isGlassTheme = isGlassTheme,
        baseContentColor = contentColor,
        baseAccentColor = accentColor
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val premiumRefreshSignal by PremiumCheckoutManager.refreshSignal.collectAsState()
    val checkoutState by PremiumCheckoutManager.checkoutState.collectAsState()
    var premiumState by remember { mutableStateOf<PremiumSubscriptionResponse?>(null) }
    var creatorProState by remember { mutableStateOf<CreatorProResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdatingOverride by remember { mutableStateOf(false) }
    var isUpdatingSettings by remember { mutableStateOf(false) }
    var selectedBillingCycle by rememberSaveable { mutableStateOf("monthly") }

    fun launchCreatorProCheckout() {
        val activity = context.findComponentActivity()
        if (activity == null) {
            Toast.makeText(
                context,
                "Creator Pro checkout needs an activity context.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        PremiumCheckoutManager.startCheckout(
            activity = activity,
            billingCycle = selectedBillingCycle,
            userId = currentUserId,
            plan = "creator_pro"
        )
    }

    fun setCreatorProOverride(enabled: Boolean) {
        if (isUpdatingOverride) return
        scope.launch {
            isUpdatingOverride = true
            ApiClient.setDeveloperCreatorProOverride(context, enabled)
                .onSuccess { response ->
                    creatorProState = response
                    response.subscription?.let { premiumState = it }
                    PremiumCheckoutManager.notifyPremiumStateChanged()
                    Toast.makeText(
                        context,
                        response.message.ifBlank {
                            if (enabled) "Creator Pro test mode is on." else "Creator Pro test mode is off."
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Unable to update Creator Pro test mode.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            isUpdatingOverride = false
        }
    }

    fun updateCreatorProSettings(request: CreatorProSettingsRequest) {
        if (isUpdatingSettings) return
        scope.launch {
            isUpdatingSettings = true
            ApiClient.updateCreatorProSettings(context, request)
                .onSuccess { response ->
                    creatorProState = response
                    response.subscription?.let { premiumState = it }
                }
                .onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Unable to update Creator Pro settings.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            isUpdatingSettings = false
        }
    }

    LaunchedEffect(premiumRefreshSignal) {
        isLoading = true
        ApiClient.getPremiumSubscription(context)
            .onSuccess { premiumState = it }
            .onFailure { premiumState = null }
        ApiClient.getCreatorPro(context)
            .onSuccess { response ->
                creatorProState = response
                response.subscription?.let { premiumState = it }
            }
            .onFailure { creatorProState = null }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        PremiumCheckoutManager.preload(context)
    }

    LaunchedEffect(creatorProState?.access?.planOptions) {
        val availableCycles = creatorProState?.access?.planOptions
            ?.map { it.billingCycle }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        if (availableCycles.isNotEmpty() && selectedBillingCycle !in availableCycles) {
            selectedBillingCycle = availableCycles.first()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .padding(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(palette.closeBackgroundColor)
                        .clickable(onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = palette.closeIconColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                BasicText(
                    "CREATOR PRO",
                    modifier = Modifier.align(Alignment.Center),
                    style = TextStyle(
                        color = palette.headerTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                )
            }

            MoreCreatorProHeroCard(
                isActive = creatorProState?.access?.canUseCreatorPro == true || premiumState?.isCreatorPro == true,
                isLoading = isLoading || checkoutState.isProcessingPurchase,
                contentColor = palette.contentColor,
                secondaryTextColor = palette.secondaryTextColor,
                accentColor = palette.accentColor,
                borderColor = palette.dividerColor
            )

            MoreCreatorProPanel(
                backdrop = backdrop,
                isGlassTheme = isGlassTheme,
                palette = palette,
                contentColor = palette.contentColor,
                secondaryTextColor = palette.secondaryTextColor,
                accentColor = palette.accentColor,
                dividerColor = palette.dividerColor,
                creatorProState = creatorProState,
                premiumState = premiumState,
                selectedBillingCycle = selectedBillingCycle,
                isLoading = isLoading || checkoutState.isProcessingPurchase,
                isUpdatingOverride = isUpdatingOverride,
                isUpdatingSettings = isUpdatingSettings,
                onBillingCycleChange = { selectedBillingCycle = it },
                onStartCheckout = ::launchCreatorProCheckout,
                onToggleCreatorProTest = ::setCreatorProOverride,
                onUpdateSettings = ::updateCreatorProSettings
            )

            creatorProState?.analytics?.let { analytics ->
                CreatorProAnalyticsBoard(
                    analytics = analytics,
                    settings = creatorProState?.settings,
                    contentColor = palette.contentColor,
                    secondaryTextColor = palette.secondaryTextColor,
                    accentColor = palette.accentColor,
                    borderColor = palette.dividerColor,
                    surfaceColor = palette.planSurfaceColor
                )
            }
        }
    }
}

@Composable
private fun MoreCreatorProHeroCard(
    isActive: Boolean,
    isLoading: Boolean,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    borderColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .border(1.dp, accentColor.copy(alpha = 0.34f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                BasicText(
                    if (isActive) "Creator Pro is live" else "Creator Pro",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 25.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                BasicText(
                    if (isLoading) {
                        "Checking creator access..."
                    } else {
                        "Audience analytics, collab priority, paid DMs, paid 1:1 sessions, and portfolio amplification."
                    },
                    style = TextStyle(
                        color = secondaryTextColor,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CreatorProCapabilityPill("Analytics", contentColor, borderColor, Modifier.weight(1f))
            CreatorProCapabilityPill("Paid DMs", contentColor, borderColor, Modifier.weight(1f))
            CreatorProCapabilityPill("Amplify", contentColor, borderColor, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CreatorProCapabilityPill(
    label: String,
    contentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = contentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CreatorProAnalyticsBoard(
    analytics: com.kyant.backdrop.catalog.network.models.CreatorProAnalytics,
    settings: com.kyant.backdrop.catalog.network.models.CreatorProSettings?,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    borderColor: Color,
    surfaceColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BasicText(
            "Creator dashboard",
            style = TextStyle(
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        )

        CreatorProDashboardSection(
            title = "Audience",
            contentColor = contentColor,
            secondaryTextColor = secondaryTextColor,
            borderColor = borderColor,
            items = listOf(
                "Views" to analytics.audience.profileViewsTotal.toString(),
                "Unique" to analytics.audience.uniqueViewers.toString(),
                "Saved" to analytics.audience.profileSavesTotal.toString(),
                "Requests" to analytics.audience.connectionRequestsLast30Days.toString()
            )
        )

        CreatorProDashboardSection(
            title = "Content",
            contentColor = contentColor,
            secondaryTextColor = secondaryTextColor,
            borderColor = borderColor,
            items = listOf(
                "Reels" to analytics.content.reels.count.toString(),
                "Reel views" to analytics.content.reels.views.toString(),
                "Posts" to analytics.content.posts.count.toString(),
                "Collabs" to analytics.content.collaborations.accepted.toString()
            )
        )

        CreatorProDashboardSection(
            title = "Monetization",
            contentColor = contentColor,
            secondaryTextColor = secondaryTextColor,
            borderColor = borderColor,
            items = listOf(
                "DMs" to if (settings?.monetizedDmEnabled == true) settings.dmDisplayPrice else "Off",
                "Sessions" to if (settings?.sessionBookingEnabled == true) settings.sessionDisplayPrice else "Off",
                "You keep" to (settings?.sessionCreatorReceivesDisplay ?: "-"),
                "Fee" to "${analytics.monetization.platformFeeBps / 100.0}%"
            )
        )

        val tags = analytics.showcase.topTags.take(4)
        if (tags.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(
                    "Top match tags",
                    style = TextStyle(
                        color = secondaryTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .border(1.dp, accentColor.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                tag.label,
                                style = TextStyle(
                                    color = contentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorProDashboardSection(
    title: String,
    contentColor: Color,
    secondaryTextColor: Color,
    borderColor: Color,
    items: List<Pair<String, String>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            title,
            style = TextStyle(
                color = secondaryTextColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.take(4).forEach { (label, value) ->
                CreatorProMetricPill(
                    label = label,
                    value = value,
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    borderColor = borderColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MorePremiumDetailsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    currentUserId: String?,
    onNavigateBack: () -> Unit,
    onOpenAgent: () -> Unit
) {
    val premiumPalette = rememberPremiumMembershipPalette(
        isGlassTheme = isGlassTheme,
        baseContentColor = contentColor,
        baseAccentColor = accentColor
    )
    val sectionSurfaceColor = Color.Transparent
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val premiumRefreshSignal by PremiumCheckoutManager.refreshSignal.collectAsState()
    val celebrationSignal by PremiumCheckoutManager.celebrationSignal.collectAsState()
    val checkoutState by PremiumCheckoutManager.checkoutState.collectAsState()
    var premiumState by remember { mutableStateOf<PremiumSubscriptionResponse?>(null) }
    var creatorProState by remember { mutableStateOf<CreatorProResponse?>(null) }
    var isLoadingPremiumState by remember { mutableStateOf(true) }
    var isLoadingCreatorProState by remember { mutableStateOf(true) }
    var isCancellingPremium by remember { mutableStateOf(false) }
    var isActivatingBoost by remember { mutableStateOf(false) }
    var isUpdatingCreatorPro by remember { mutableStateOf(false) }
    var isUpdatingCreatorProSettings by remember { mutableStateOf(false) }

    var showCelebration by remember { mutableStateOf(false) }
    var selectedBillingCycle by rememberSaveable { mutableStateOf("yearly") }
    var selectedCreatorProBillingCycle by rememberSaveable { mutableStateOf("monthly") }
    val checkoutPlanOptions = premiumState?.planOptions.orEmpty()

    fun launchPremiumCheckout() {
        val activity = context.findComponentActivity()
        if (activity == null) {
            Toast.makeText(
                context,
                "Premium checkout needs an activity context.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!isLoadingPremiumState && premiumState?.checkoutEnabled == false) {
            Toast.makeText(
                context,
                "Google Play premium verification is not configured on the server yet.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        PremiumCheckoutManager.startCheckout(
            activity = activity,
            billingCycle = selectedBillingCycle,
            userId = currentUserId
        )
    }

    fun launchCreatorProCheckout() {
        val activity = context.findComponentActivity()
        if (activity == null) {
            Toast.makeText(
                context,
                "Creator Pro checkout needs an activity context.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        PremiumCheckoutManager.startCheckout(
            activity = activity,
            billingCycle = selectedCreatorProBillingCycle,
            userId = currentUserId,
            plan = "creator_pro"
        )
    }

    fun cancelPremiumAccess() {
        scope.launch {
            isCancellingPremium = true
            val cancelResult = ApiClient.cancelPremiumSubscription(context)
            isCancellingPremium = false

            cancelResult
                .onSuccess { response ->
                    premiumState = response.subscription ?: premiumState
                    PremiumCheckoutManager.notifyPremiumStateChanged()
                    Toast.makeText(
                        context,
                        response.message.ifBlank { "Premium cancelled. Buy again anytime." },
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Unable to cancel premium right now.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    fun activateProfileBoost() {
        scope.launch {
            isActivatingBoost = true
            val boostResult = ApiClient.activateProfileBoost(context)
            isActivatingBoost = false

            boostResult
                .onSuccess { response ->
                    premiumState = response.subscription
                        ?: premiumState?.copy(profileBoost = response.profileBoost)
                    PremiumCheckoutManager.notifyPremiumStateChanged()
                    Toast.makeText(
                        context,
                        response.message.ifBlank { "Profile boost is live." },
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Unable to activate profile boost right now.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    fun setCreatorProOverride(enabled: Boolean) {
        if (isUpdatingCreatorPro) return
        scope.launch {
            isUpdatingCreatorPro = true
            ApiClient.setDeveloperCreatorProOverride(context, enabled)
                .onSuccess { response ->
                    creatorProState = response
                    response.subscription?.let { premiumState = it }
                    PremiumCheckoutManager.notifyPremiumStateChanged()
                    Toast.makeText(
                        context,
                        response.message.ifBlank {
                            if (enabled) "Creator Pro test mode is on." else "Creator Pro test mode is off."
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Unable to update Creator Pro test mode.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            isUpdatingCreatorPro = false
        }
    }

    fun updateCreatorProSettings(request: CreatorProSettingsRequest) {
        if (isUpdatingCreatorProSettings) return
        scope.launch {
            isUpdatingCreatorProSettings = true
            ApiClient.updateCreatorProSettings(context, request)
                .onSuccess { response ->
                    creatorProState = response
                    response.subscription?.let { premiumState = it }
                }
                .onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Unable to update Creator Pro settings.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            isUpdatingCreatorProSettings = false
        }
    }



    LaunchedEffect(premiumRefreshSignal) {
        isLoadingPremiumState = true
        ApiClient.getPremiumSubscription(context)
            .onSuccess { premiumState = it }
            .onFailure { premiumState = null }
        isLoadingPremiumState = false

        isLoadingCreatorProState = true
        ApiClient.getCreatorPro(context)
            .onSuccess { response ->
                creatorProState = response
                response.subscription?.let { premiumState = it }
            }
            .onFailure { creatorProState = null }
        isLoadingCreatorProState = false
    }

    LaunchedEffect(Unit) {
        PremiumCheckoutManager.preload(context)
    }

    LaunchedEffect(checkoutPlanOptions) {
        val availableCycles = checkoutPlanOptions
            .map { it.billingCycle }
            .filter { it.isNotBlank() }
        if (availableCycles.isNotEmpty() && selectedBillingCycle !in availableCycles) {
            selectedBillingCycle = availableCycles.first()
        }
    }

    LaunchedEffect(creatorProState?.access?.planOptions) {
        val availableCycles = creatorProState?.access?.planOptions
            ?.map { it.billingCycle }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        if (availableCycles.isNotEmpty() && selectedCreatorProBillingCycle !in availableCycles) {
            selectedCreatorProBillingCycle = availableCycles.first()
        }
    }

    LaunchedEffect(celebrationSignal) {
        if (celebrationSignal == 0L) return@LaunchedEffect
        showCelebration = true
        delay(2800)
        showCelebration = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(premiumPalette.pageBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp)
                .padding(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(premiumPalette.closeBackgroundColor)
                        .clickable(onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = premiumPalette.closeIconColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                BasicText(
                    "VORMEX · MEMBERSHIP",
                    modifier = Modifier.align(Alignment.Center),
                    style = TextStyle(
                        color = premiumPalette.headerTextColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            MorePremiumPromoCard(
                backdrop = backdrop,
                isGlassTheme = isGlassTheme,
                sectionSurfaceColor = sectionSurfaceColor,
                dividerColor = premiumPalette.dividerColor,
                contentColor = premiumPalette.contentColor,
                secondaryTextColor = premiumPalette.secondaryTextColor,
                accentColor = premiumPalette.accentColor,
                palette = premiumPalette,
                premiumState = premiumState,
                checkoutPlanOptions = checkoutPlanOptions,
                selectedBillingCycle = selectedBillingCycle,
                isLoadingPremiumState = isLoadingPremiumState,
                isLaunchingCheckout = checkoutState.isProcessingPurchase,
                isCancellingPremium = isCancellingPremium,
                isActivatingBoost = isActivatingBoost,
                checkoutErrorMessage = checkoutState.errorMessage,
                checkoutPendingMessage = checkoutState.pendingMessage,
                showCelebration = showCelebration,
                onBillingCycleChange = { selectedBillingCycle = it },
                onStartCheckout = ::launchPremiumCheckout,
                onCancelPremium = ::cancelPremiumAccess,
                onActivateBoost = ::activateProfileBoost,
                onOpenAgent = onOpenAgent
            )

            MoreCreatorProPanel(
                backdrop = backdrop,
                isGlassTheme = isGlassTheme,
                palette = premiumPalette,
                contentColor = premiumPalette.contentColor,
                secondaryTextColor = premiumPalette.secondaryTextColor,
                accentColor = premiumPalette.accentColor,
                dividerColor = premiumPalette.dividerColor,
                creatorProState = creatorProState,
                premiumState = premiumState,
                selectedBillingCycle = selectedCreatorProBillingCycle,
                isLoading = isLoadingCreatorProState,
                isUpdatingOverride = isUpdatingCreatorPro,
                isUpdatingSettings = isUpdatingCreatorProSettings,
                onBillingCycleChange = { selectedCreatorProBillingCycle = it },
                onStartCheckout = ::launchCreatorProCheckout,
                onToggleCreatorProTest = ::setCreatorProOverride,
                onUpdateSettings = ::updateCreatorProSettings
            )
        }
    }
}

@Composable
private fun MoreCreatorProPanel(
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    palette: PremiumMembershipPalette,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    dividerColor: Color,
    creatorProState: CreatorProResponse?,
    premiumState: PremiumSubscriptionResponse?,
    selectedBillingCycle: String,
    isLoading: Boolean,
    isUpdatingOverride: Boolean,
    isUpdatingSettings: Boolean,
    onBillingCycleChange: (String) -> Unit,
    onStartCheckout: () -> Unit,
    onToggleCreatorProTest: (Boolean) -> Unit,
    onUpdateSettings: (CreatorProSettingsRequest) -> Unit
) {
    val access = creatorProState?.access
    val settings = creatorProState?.settings
    val analytics = creatorProState?.analytics
    val isCreatorProActive = access?.canUseCreatorPro == true || premiumState?.isCreatorPro == true
    val canUseDebugToggle = premiumState?.developerPremiumOverrideAvailable == true
    val planOptions = access?.planOptions?.takeIf { it.isNotEmpty() }
        ?: premiumState?.creatorPro?.planOptions.orEmpty()
    val orderedPlanOptions = planOptions.takeIf { it.isNotEmpty() }?.let(::premiumOrderedPlanOptions).orEmpty()
    val selectedPlan = orderedPlanOptions.firstOrNull { it.billingCycle == selectedBillingCycle }
        ?: orderedPlanOptions.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isGlassTheme) {
                    Modifier
                        .vormexSurface(
                            backdrop = backdrop,
                            cornerRadius = 24.dp,
                            blurRadius = 18.dp,
                            lensRadius = 6.dp,
                            lensDepth = 14.dp,
                            surfaceColor = palette.glassPanelSurfaceColor,
                            borderColor = palette.glassPanelBorderColor
                        )
                        .padding(16.dp)
                } else {
                    Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(palette.planSurfaceColor)
                        .border(1.dp, dividerColor, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                }
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                BasicText(
                    if (isCreatorProActive) "Creator Pro active" else "Creator Pro",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                BasicText(
                    when {
                        isLoading -> "Loading creator tools..."
                        isCreatorProActive -> "Analytics, monetization, and amplification are unlocked."
                        else -> access?.description?.takeIf { it.isNotBlank() }
                            ?: "Audience analytics, collab priority, paid sessions, and portfolio amplification."
                    },
                    style = TextStyle(
                        color = secondaryTextColor,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            if (isLoading || isUpdatingOverride || isUpdatingSettings) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = accentColor
                )
            }
        }

        if (analytics != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CreatorProMetricPill(
                    label = "Views 30d",
                    value = analytics.audience.profileViewsLast30Days.toString(),
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    borderColor = dividerColor,
                    modifier = Modifier.weight(1f)
                )
                CreatorProMetricPill(
                    label = "Search",
                    value = analytics.audience.searchAppearancesLast30Days.toString(),
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    borderColor = dividerColor,
                    modifier = Modifier.weight(1f)
                )
                CreatorProMetricPill(
                    label = "Match",
                    value = analytics.audience.matchRateDisplay,
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    borderColor = dividerColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (isCreatorProActive && settings != null) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                CreatorProSettingRow(
                    title = "Collab priority",
                    detail = "Rank higher for creator/team matches",
                    checked = settings.collabPriorityEnabled,
                    enabled = !isUpdatingSettings,
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    accentColor = accentColor,
                    borderColor = dividerColor,
                    backdrop = backdrop,
                    onChange = {
                        onUpdateSettings(CreatorProSettingsRequest(collabPriorityEnabled = it))
                    }
                )
                CreatorProSettingRow(
                    title = "Showcase amplify",
                    detail = "Boost portfolio and showcase surfaces",
                    checked = settings.showcaseAmplificationEnabled,
                    enabled = !isUpdatingSettings,
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    accentColor = accentColor,
                    borderColor = dividerColor,
                    backdrop = backdrop,
                    onChange = {
                        onUpdateSettings(CreatorProSettingsRequest(showcaseAmplificationEnabled = it))
                    }
                )
                CreatorProSettingRow(
                    title = "Portfolio amplify",
                    detail = "Push portfolio links and proof-of-work higher",
                    checked = settings.portfolioAmplificationEnabled,
                    enabled = !isUpdatingSettings,
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    accentColor = accentColor,
                    borderColor = dividerColor,
                    backdrop = backdrop,
                    onChange = {
                        onUpdateSettings(CreatorProSettingsRequest(portfolioAmplificationEnabled = it))
                    }
                )
                CreatorProSettingRow(
                    title = "Monetized DMs",
                    detail = if (settings.monetizedDmEnabled) {
                        "${settings.dmDisplayPrice} DM · you get ${settings.dmCreatorReceivesDisplay}"
                    } else {
                        "Set a paid DM intro offer"
                    },
                    checked = settings.monetizedDmEnabled,
                    enabled = !isUpdatingSettings,
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    accentColor = accentColor,
                    borderColor = dividerColor,
                    backdrop = backdrop,
                    onChange = { enabled ->
                        onUpdateSettings(
                            CreatorProSettingsRequest(
                                monetizedDmEnabled = enabled,
                                dmPriceMinor = if (enabled && settings.dmPriceMinor <= 0) 9900 else settings.dmPriceMinor
                            )
                        )
                    }
                )
                if (settings.monetizedDmEnabled) {
                    CreatorProPresetRow(
                        title = "DM price",
                        currentLabel = settings.dmDisplayPrice,
                        selectedValue = settings.dmPriceMinor,
                        options = listOf(
                            "₹99" to 9900,
                            "₹199" to 19900,
                            "₹499" to 49900
                        ),
                        enabled = !isUpdatingSettings,
                        contentColor = contentColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = accentColor,
                        borderColor = dividerColor,
                        onSelect = {
                            onUpdateSettings(CreatorProSettingsRequest(dmPriceMinor = it))
                        }
                    )
                }
                CreatorProSettingRow(
                    title = "Paid 1:1 sessions",
                    detail = if (settings.sessionBookingEnabled) {
                        "${settings.sessionDisplayPrice} · ${settings.sessionDurationMinutes} min"
                    } else {
                        "Offer paid creator calls"
                    },
                    checked = settings.sessionBookingEnabled,
                    enabled = !isUpdatingSettings,
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    accentColor = accentColor,
                    borderColor = dividerColor,
                    backdrop = backdrop,
                    onChange = { enabled ->
                        onUpdateSettings(
                            CreatorProSettingsRequest(
                                sessionBookingEnabled = enabled,
                                sessionPriceMinor = if (enabled && settings.sessionPriceMinor <= 0) 24900 else settings.sessionPriceMinor,
                                sessionDurationMinutes = settings.sessionDurationMinutes
                            )
                        )
                    }
                )
                if (settings.sessionBookingEnabled) {
                    CreatorProPresetRow(
                        title = "Session price",
                        currentLabel = settings.sessionDisplayPrice,
                        selectedValue = settings.sessionPriceMinor,
                        options = listOf(
                            "₹249" to 24900,
                            "₹499" to 49900,
                            "₹999" to 99900
                        ),
                        enabled = !isUpdatingSettings,
                        contentColor = contentColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = accentColor,
                        borderColor = dividerColor,
                        onSelect = {
                            onUpdateSettings(CreatorProSettingsRequest(sessionPriceMinor = it))
                        }
                    )
                    CreatorProPresetRow(
                        title = "Session length",
                        currentLabel = "${settings.sessionDurationMinutes} min",
                        selectedValue = settings.sessionDurationMinutes,
                        options = listOf(
                            "30m" to 30,
                            "45m" to 45,
                            "60m" to 60
                        ),
                        enabled = !isUpdatingSettings,
                        contentColor = contentColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = accentColor,
                        borderColor = dividerColor,
                        onSelect = {
                            onUpdateSettings(CreatorProSettingsRequest(sessionDurationMinutes = it))
                        }
                    )
                    CreatorProAvailabilityEditor(
                        initialNote = settings.availabilityNote.orEmpty(),
                        enabled = !isUpdatingSettings,
                        contentColor = contentColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = accentColor,
                        borderColor = dividerColor,
                        onSave = {
                            onUpdateSettings(CreatorProSettingsRequest(availabilityNote = it))
                        }
                    )
                }
            }
        } else {
            val features = access?.features?.takeIf { it.isNotEmpty() }
                ?: premiumState?.creatorPro?.features.orEmpty()
            if (features.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    features.take(4).forEach { feature ->
                        BasicText(
                            feature,
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
            if (orderedPlanOptions.isNotEmpty()) {
                MorePremiumEditorialPlanSelector(
                    planOptions = orderedPlanOptions,
                    selectedBillingCycle = selectedPlan?.billingCycle ?: selectedBillingCycle,
                    onBillingCycleChange = onBillingCycleChange,
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    accentColor = accentColor,
                    borderColor = dividerColor,
                    surfaceColor = palette.planSurfaceColor,
                    selectedSurfaceColor = palette.planSelectedSurfaceColor,
                    selectedTextColor = palette.planSelectedTextColor,
                    unselectedTextColor = palette.planUnselectedTextColor,
                    fontFamily = FontFamily.SansSerif
                )
            }
            MorePremiumPrimaryCta(
                label = if (isLoading) "Checking Creator Pro" else "Upgrade to Creator Pro",
                priceLabel = selectedPlan?.let { premiumCompactAmountLabel(it) } ?: "",
                enabled = !isLoading,
                onClick = onStartCheckout,
                backgroundColor = palette.ctaSurfaceColor,
                contentColor = palette.ctaContentColor,
                fontFamily = FontFamily.SansSerif
            )
        }

        if (canUseDebugToggle) {
            CreatorProSettingRow(
                title = "Test Creator Pro",
                detail = if (isCreatorProActive) "Creator Pro is enabled for this account" else "Enable Creator Pro without payment",
                checked = isCreatorProActive,
                enabled = !isUpdatingOverride,
                contentColor = contentColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                borderColor = dividerColor,
                backdrop = backdrop,
                onChange = onToggleCreatorProTest
            )
        }
    }
}

@Composable
private fun CreatorProMetricPill(
    label: String,
    value: String,
    contentColor: Color,
    secondaryTextColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        BasicText(
            value,
            style = TextStyle(
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        BasicText(
            label,
            style = TextStyle(
                color = secondaryTextColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CreatorProPresetRow(
    title: String,
    currentLabel: String,
    selectedValue: Int,
    options: List<Pair<String, Int>>,
    enabled: Boolean,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    borderColor: Color,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                title,
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                currentLabel,
                style = TextStyle(
                    color = secondaryTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (label, value) ->
                val selected = selectedValue == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) accentColor else Color.Transparent)
                        .border(
                            1.dp,
                            if (selected) accentColor else borderColor,
                            RoundedCornerShape(999.dp)
                        )
                        .clickable(enabled = enabled && !selected) { onSelect(value) }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        label,
                        style = TextStyle(
                            color = if (selected && accentColor.luminance() > 0.55f) Color.Black else if (selected) Color.White else contentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatorProAvailabilityEditor(
    initialNote: String,
    enabled: Boolean,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    borderColor: Color,
    onSave: (String) -> Unit
) {
    var note by remember(initialNote) { mutableStateOf(initialNote) }
    val changed = note.trim() != initialNote.trim()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BasicText(
                "Availability note",
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (changed && enabled) accentColor else Color.Transparent)
                    .border(
                        1.dp,
                        if (changed && enabled) accentColor else borderColor,
                        RoundedCornerShape(999.dp)
                    )
                    .clickable(enabled = enabled && changed) { onSave(note.trim()) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "Save",
                    style = TextStyle(
                        color = if (changed && enabled && accentColor.luminance() > 0.55f) Color.Black else if (changed && enabled) Color.White else secondaryTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        BasicTextField(
            value = note,
            onValueChange = { value ->
                note = value.take(180)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(contentColor.copy(alpha = 0.06f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            textStyle = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(accentColor),
            decorationBox = { innerTextField ->
                Box {
                    if (note.isBlank()) {
                        BasicText(
                            "Weekdays after 7 PM, 30 min calls preferred",
                            style = TextStyle(
                                color = secondaryTextColor,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun CreatorProSettingRow(
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    borderColor: Color,
    backdrop: LayerBackdrop,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            BasicText(
                title,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                detail,
                style = TextStyle(
                    color = secondaryTextColor,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        LiquidToggle(
            selected = { checked },
            onSelect = { if (enabled) onChange(it) },
            backdrop = backdrop,
            accentColor = accentColor
        )
    }
}

@Composable
private fun MorePremiumEntryCard(
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    sectionSurfaceColor: Color,
    dividerColor: Color,
    accentColor: Color,
    onOpenPremiumDetails: () -> Unit
) {
    val context = LocalContext.current
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    val ctaComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.go_premium))
    val ctaProgress by animateLottieCompositionAsState(
        composition = ctaComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = !reduceAnimations
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(22.dp) },
                        effects = {
                            vibrancy()
                            blur(14f.dp.toPx())
                            lens(8f.dp.toPx(), 16f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(sectionSurfaceColor)
                        }
                    )
                } else {
                    Modifier.background(sectionSurfaceColor)
                }
            )
            .border(1.dp, dividerColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenPremiumDetails)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isGlassTheme) 0.22f else 0.14f),
                            Color(0xFFFFC857).copy(alpha = if (isGlassTheme) 0.14f else 0.08f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(900f, 460f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(520f, 92f),
                        radius = 240f
                    )
                )
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (ctaComposition != null) {
                LottieAnimation(
                    composition = ctaComposition,
                    progress = { ctaProgress },
                    modifier = Modifier.size(156.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}



@Composable
private fun MorePremiumPromoCard(
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    sectionSurfaceColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    palette: PremiumMembershipPalette,
    premiumState: PremiumSubscriptionResponse?,
    checkoutPlanOptions: List<PremiumPlanOption>,
    selectedBillingCycle: String,
    isLoadingPremiumState: Boolean,
    isLaunchingCheckout: Boolean,
    isCancellingPremium: Boolean,
    isActivatingBoost: Boolean,
    checkoutErrorMessage: String?,
    checkoutPendingMessage: String?,
    showCelebration: Boolean,
    onBillingCycleChange: (String) -> Unit,
    onStartCheckout: () -> Unit,
    onCancelPremium: () -> Unit,
    onActivateBoost: () -> Unit,
    onOpenAgent: () -> Unit
) {
    val context = LocalContext.current
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    val confettiComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.confetti_animation_01)
    )
    val confettiProgress by animateLottieCompositionAsState(
        composition = confettiComposition,
        iterations = if (reduceAnimations) 1 else LottieConstants.IterateForever,
        isPlaying = showCelebration
    )
    val backendPlanOptions = premiumState?.planOptions?.takeIf { it.isNotEmpty() }
    val planOptions = checkoutPlanOptions.takeIf { it.isNotEmpty() } ?: backendPlanOptions ?: premiumDefaultPlanOptions()
    val orderedPlanOptions = premiumOrderedPlanOptions(planOptions)
    val selectedPlan = orderedPlanOptions.firstOrNull { it.billingCycle == selectedBillingCycle }
        ?: orderedPlanOptions.first()
    val monthlyPlan = orderedPlanOptions.firstOrNull { it.billingCycle == "monthly" }
    val isPremiumActive = premiumState?.isPremium == true
    val profileBoost = premiumState?.profileBoost
    val isProfileBoostActive = profileBoost?.active == true
    val customPriceApplied = premiumState?.customPriceApplied == true
    val checkoutEnabled = premiumState?.checkoutEnabled != false
    val canStartCheckout =
        !isLoadingPremiumState &&
            !isLaunchingCheckout &&
            !isCancellingPremium &&
            checkoutEnabled &&
            !isPremiumActive
    val primaryCtaLabel = when {
        isLoadingPremiumState -> "Checking access"
        isLaunchingCheckout -> "Opening checkout"
        isActivatingBoost -> "Activating boost"
        isCancellingPremium -> "Updating premium"
        isPremiumActive && premiumState.canUseAgent -> "Open AI Agent"
        isPremiumActive -> "Premium active"
        !checkoutEnabled -> "Premium unavailable"
        else -> "Start 7-day free trial"
    }
    val ctaPriceLabel = when {
        isPremiumActive -> premiumState.displayAmount.takeIf { it.isNotBlank() }
            ?: premiumCompactAmountLabel(selectedPlan)
        selectedPlan.billingCycle == "yearly" -> "${premiumCompactAmountLabel(selectedPlan)}/yr"
        selectedPlan.billingCycle == "monthly" -> "${premiumCompactAmountLabel(selectedPlan)}/mo"
        else -> premiumCompactAmountLabel(selectedPlan)
    }
    val statusMessage = when {
        !checkoutPendingMessage.isNullOrBlank() -> checkoutPendingMessage
        !checkoutErrorMessage.isNullOrBlank() && !isPremiumActive -> checkoutErrorMessage
        isLoadingPremiumState -> "Checking your premium access..."
        isLaunchingCheckout -> "Opening Google Play checkout..."
        isActivatingBoost -> "Activating your profile boost..."
        isProfileBoostActive -> "Your profile boost is active and ranking higher in discovery."
        isCancellingPremium -> "Updating premium access..."
        !checkoutEnabled && !isPremiumActive -> "Google Play checkout is not ready yet. Tap to retry."
        customPriceApplied -> "A special premium price is active for this account."
        else -> null
    }
    val includedFeatures = premiumState?.features
        ?.takeIf { it.isNotEmpty() }
        ?: listOf(
            "Priority discovery placement",
            "Premium verified badge",
            "Profile reach boost - up to 3x",
            "Better chance of profile views",
            "Recommended to top creators",
            "Unlimited AI Agent prompts",
            "Teammate finder and intro help",
            "Profile frames and visitor polish"
        )
    val priceHeader = when (selectedPlan.billingCycle) {
        "yearly" -> "YEARLY · PER MONTH"
        "monthly" -> "MONTHLY"
        else -> selectedPlan.billingCycle.uppercase()
    }
    val priceLabel = if (selectedPlan.billingCycle == "yearly") {
        premiumPerMonthLabel(selectedPlan, monthlyPlan)
    } else {
        premiumCompactAmountLabel(selectedPlan)
    }
    val billingBase = premiumBillingBaseLabel(selectedPlan)
    val billingSavings = premiumSavingsLabel(selectedPlan)
    val premiumFontFamily = FontFamily.SansSerif

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isGlassTheme) {
                    Modifier
                        .vormexSurface(
                            backdrop = backdrop,
                            cornerRadius = 24.dp,
                            blurRadius = 18.dp,
                            lensRadius = 6.dp,
                            lensDepth = 14.dp,
                            surfaceColor = palette.glassPanelSurfaceColor,
                            borderColor = palette.glassPanelBorderColor
                        )
                        .padding(16.dp)
                } else {
                    Modifier
                }
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                BasicText(
                    if (isPremiumActive) "VORMEX+ ACTIVE" else "VORMEX+ MEMBERSHIP",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = premiumFontFamily
                    )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                BasicText(
                    "Grow louder.",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 34.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = premiumFontFamily
                    )
                )
                BasicText(
                    "Build faster.",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 32.sp,
                        lineHeight = 34.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        fontFamily = premiumFontFamily
                    )
                )
            }

            BasicText(
                "Everything top creators on Vormex use to ship projects, get discovered, and find their next team.",
                style = TextStyle(
                    color = secondaryTextColor,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = premiumFontFamily
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(dividerColor)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    BasicText(
                        priceHeader,
                        style = TextStyle(
                            color = palette.metaTextColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = premiumFontFamily
                        )
                    )
                    BasicText(
                        priceLabel,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 39.sp,
                            lineHeight = 41.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = premiumFontFamily
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            billingBase,
                            style = TextStyle(
                                color = secondaryTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = premiumFontFamily
                            )
                        )
                        billingSavings?.let { savings ->
                            BasicText(
                                " · $savings",
                                style = TextStyle(
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = premiumFontFamily
                                )
                            )
                        }
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    BasicText(
                        "7-DAY",
                        style = TextStyle(
                            color = palette.metaTextColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = premiumFontFamily
                        )
                    )
                    BasicText(
                        "FREE TRIAL",
                        style = TextStyle(
                            color = palette.metaTextColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = premiumFontFamily
                        )
                    )
                }
            }

            MorePremiumEditorialPlanSelector(
                planOptions = orderedPlanOptions,
                selectedBillingCycle = selectedPlan.billingCycle,
                onBillingCycleChange = onBillingCycleChange,
                contentColor = contentColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                borderColor = dividerColor,
                surfaceColor = palette.planSurfaceColor,
                selectedSurfaceColor = palette.planSelectedSurfaceColor,
                selectedTextColor = palette.planSelectedTextColor,
                unselectedTextColor = palette.planUnselectedTextColor,
                fontFamily = premiumFontFamily
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(dividerColor)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MorePremiumSpotlightCard(
                label = "REACH",
                title = "Land on more profiles",
                detail = "Priority discovery placement, 3x reach boost, and a premium verified badge that builds instant trust.",
                backgroundColor = palette.reachCardSurfaceColor,
                labelBackgroundColor = palette.reachCardLabelSurfaceColor,
                labelColor = palette.reachCardLabelTextColor,
                titleColor = palette.reachCardTitleColor,
                detailColor = palette.reachCardDetailColor,
                fontFamily = premiumFontFamily
            )
            MorePremiumSpotlightCard(
                label = "AI",
                title = "An AI co-pilot for your profile",
                detail = "Unlimited Agent prompts, teammate finder, and a sharper profile assistant for shipping faster.",
                backgroundColor = palette.aiCardSurfaceColor,
                labelBackgroundColor = palette.aiCardLabelSurfaceColor,
                labelColor = palette.aiCardLabelTextColor,
                titleColor = palette.aiCardTitleColor,
                detailColor = palette.aiCardDetailColor,
                fontFamily = premiumFontFamily
            )
        }

        MorePremiumPrimaryCta(
            label = primaryCtaLabel,
            priceLabel = ctaPriceLabel,
            enabled = if (isPremiumActive) premiumState.canUseAgent else canStartCheckout,
            onClick = if (isPremiumActive) onOpenAgent else onStartCheckout,
            backgroundColor = if (isPremiumActive && premiumState.canUseAgent) {
                palette.activeCtaSurfaceColor
            } else {
                palette.ctaSurfaceColor
            },
            contentColor = if (isPremiumActive && premiumState.canUseAgent) {
                palette.activeCtaContentColor
            } else {
                palette.ctaContentColor
            },
            fontFamily = premiumFontFamily
        )



        statusMessage?.let { message ->
            MorePremiumStatusLine(
                message = message,
                contentColor = contentColor,
                secondaryTextColor = secondaryTextColor,
                borderColor = dividerColor,
                surfaceColor = palette.statusSurfaceColor,
                errorColor = palette.statusErrorTextColor,
                fontFamily = premiumFontFamily
            )
        }

        MorePremiumIncludedFeatureList(
            features = includedFeatures,
            contentColor = contentColor,
            secondaryTextColor = secondaryTextColor,
            borderColor = dividerColor,
            metaTextColor = palette.metaTextColor,
            accentColor = palette.featureAccentColor,
            fontFamily = premiumFontFamily
        )

        if (isPremiumActive) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MorePremiumSecondaryButton(
                    label = when {
                        isActivatingBoost -> "Activating boost..."
                        isProfileBoostActive -> "Profile boost active"
                        else -> "Activate ${profileBoost?.durationHours ?: 4}h profile boost"
                    },
                    contentColor = contentColor,
                    dividerColor = dividerColor,
                    fontFamily = premiumFontFamily,
                    enabled = !isActivatingBoost,
                    onClick = onActivateBoost
                )
                if (premiumState.canCancel) {
                    MorePremiumSecondaryButton(
                        label = if (isCancellingPremium) "Cancelling..." else "Cancel premium now",
                        contentColor = contentColor,
                        dividerColor = dividerColor,
                        fontFamily = premiumFontFamily,
                        enabled = !isCancellingPremium,
                        onClick = onCancelPremium
                    )
                }
            }
        }
    }

    if (showCelebration) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.20f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF1F1C18))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (confettiComposition != null) {
                            LottieAnimation(
                                composition = confettiComposition,
                                progress = { confettiProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        BasicText(
                            "Premium unlocked",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = premiumFontFamily
                            )
                        )
                        BasicText(
                            "Your AI Agent access and premium styling are now active for this account.",
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.80f),
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                textAlign = TextAlign.Center,
                                fontFamily = premiumFontFamily
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun premiumDefaultPlanOptions(): List<PremiumPlanOption> {
    return listOf(
        PremiumPlanOption(
            billingCycle = "monthly",
            amountMinor = 19900,
            currency = "INR",
            displayAmount = "₹199",
            durationDays = 31,
            label = "Monthly"
        ),
        PremiumPlanOption(
            billingCycle = "yearly",
            amountMinor = 149900,
            currency = "INR",
            displayAmount = "₹1499",
            durationDays = 365,
            label = "Yearly",
            savingsLabel = "save 37%"
        )
    )
}

private fun premiumOrderedPlanOptions(planOptions: List<PremiumPlanOption>): List<PremiumPlanOption> {
    val orderedCycles = listOf("monthly", "yearly")
    val ordered = orderedCycles.mapNotNull { cycle ->
        planOptions.firstOrNull { it.billingCycle == cycle }
    }
    return (ordered + planOptions.filterNot { plan -> ordered.any { it.billingCycle == plan.billingCycle } })
        .ifEmpty { premiumDefaultPlanOptions() }
}

private fun premiumCompactAmountLabel(plan: PremiumPlanOption): String {
    val amountFromMinor = premiumFormatAmountMinor(plan.amountMinor, plan.currency)
    return amountFromMinor.ifBlank {
        plan.displayAmount
            .replace(".00", "")
            .replace(",00", "")
            .ifBlank { "₹0" }
    }
}

private fun premiumPerMonthLabel(yearlyPlan: PremiumPlanOption, monthlyPlan: PremiumPlanOption?): String {
    if (yearlyPlan.billingCycle != "yearly") return premiumCompactAmountLabel(yearlyPlan)
    if (yearlyPlan.amountMinor > 0) {
        val monthlyMinor = (yearlyPlan.amountMinor / 12f).roundToInt()
        return premiumFormatAmountMinor(monthlyMinor, yearlyPlan.currency)
    }
    return monthlyPlan?.let { premiumCompactAmountLabel(it) } ?: premiumCompactAmountLabel(yearlyPlan)
}

private fun premiumFormatAmountMinor(amountMinor: Int, currency: String): String {
    if (amountMinor <= 0) return ""
    val amount = (amountMinor / 100f).roundToInt()
    val symbol = when (currency.uppercase()) {
        "INR" -> "₹"
        "USD" -> "$"
        else -> "$currency "
    }
    return "$symbol$amount"
}

private fun premiumBillingBaseLabel(plan: PremiumPlanOption): String {
    val amount = premiumCompactAmountLabel(plan)
    return when (plan.billingCycle) {
        "yearly" -> "Billed $amount per year"
        "monthly" -> "Billed $amount monthly"
        else -> "Billed $amount"
    }
}

private fun premiumSavingsLabel(plan: PremiumPlanOption): String? {
    return plan.savingsLabel
        ?.takeIf { it.isNotBlank() }
        ?: if (plan.billingCycle == "yearly") "save 37%" else null
}

@Composable
private fun MorePremiumEditorialPlanSelector(
    planOptions: List<PremiumPlanOption>,
    selectedBillingCycle: String,
    onBillingCycleChange: (String) -> Unit,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    borderColor: Color,
    surfaceColor: Color,
    selectedSurfaceColor: Color,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    fontFamily: FontFamily
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(9.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        planOptions.take(2).forEach { plan ->
            val selected = plan.billingCycle == selectedBillingCycle
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) selectedSurfaceColor else Color.Transparent)
                    .clickable { onBillingCycleChange(plan.billingCycle) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        plan.label.ifBlank { plan.billingCycle.replaceFirstChar { it.uppercase() } },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = if (selected) selectedTextColor else unselectedTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily
                        )
                    )
                    if (plan.billingCycle == "yearly") {
                        BasicText(
                            "-37%",
                            maxLines = 1,
                            style = TextStyle(
                                color = if (selected) {
                                    selectedTextColor.copy(alpha = 0.82f)
                                } else {
                                    accentColor.copy(alpha = 0.86f)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = fontFamily
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MorePremiumSpotlightCard(
    label: String,
    title: String,
    detail: String,
    backgroundColor: Color,
    labelBackgroundColor: Color,
    labelColor: Color,
    titleColor: Color,
    detailColor: Color,
    fontFamily: FontFamily
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(labelBackgroundColor)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            BasicText(
                label,
                style = TextStyle(
                    color = labelColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily
                )
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicText(
                title,
                style = TextStyle(
                    color = titleColor,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = fontFamily
                )
            )
            BasicText(
                detail,
                style = TextStyle(
                    color = detailColor,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = fontFamily
                )
            )
        }
    }
}

@Composable
private fun MorePremiumPrimaryCta(
    label: String,
    priceLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    fontFamily: FontFamily
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor.copy(alpha = if (enabled) 1f else 0.46f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        BasicText(
            label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = fontFamily
            )
        )
        BasicText(
            "  →  ",
            style = TextStyle(
                color = contentColor.copy(alpha = 0.72f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily
            )
        )
        BasicText(
            priceLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.76f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = fontFamily
            )
        )
    }
}

@Composable
private fun MorePremiumStatusLine(
    message: String,
    contentColor: Color,
    secondaryTextColor: Color,
    borderColor: Color,
    surfaceColor: Color,
    errorColor: Color,
    fontFamily: FontFamily
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        BasicText(
            message,
            style = TextStyle(
                color = if (message.contains("not ready", ignoreCase = true) ||
                    message.contains("unavailable", ignoreCase = true) ||
                    message.contains("could not", ignoreCase = true)
                ) {
                    errorColor
                } else {
                    secondaryTextColor
                },
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = fontFamily
            )
        )
    }
}

@Composable
private fun MorePremiumIncludedFeatureList(
    features: List<String>,
    contentColor: Color,
    secondaryTextColor: Color,
    borderColor: Color,
    metaTextColor: Color,
    accentColor: Color,
    fontFamily: FontFamily
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                "WHAT'S INCLUDED",
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    color = metaTextColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = fontFamily
                )
            )
            BasicText(
                "${features.size.coerceAtLeast(18)} FEATURES",
                style = TextStyle(
                    color = metaTextColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = fontFamily
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(borderColor)
        )
        BasicText(
            "DISCOVERY",
            style = TextStyle(
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = fontFamily
            )
        )
        features.take(8).forEachIndexed { index, feature ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 31.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    (index + 1).toString().padStart(2, '0'),
                    modifier = Modifier.width(36.dp),
                    style = TextStyle(
                        color = secondaryTextColor.copy(alpha = 0.42f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = fontFamily
                    )
                )
                BasicText(
                    feature,
                    modifier = Modifier.weight(1f),
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = fontFamily
                    )
                )
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (index != features.take(8).lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(borderColor.copy(alpha = 0.72f))
                )
            }
        }
    }
}

private fun premiumBillingLabel(billingCycle: String?, durationDays: Int = 31): String {
    return when (billingCycle?.lowercase()) {
        "monthly" -> "monthly access"
        "yearly" -> "yearly access"
        "weekly" -> "weekly access"
        else -> "$durationDays-day access"
    }
}

@Composable
private fun MorePremiumPlanSelector(
    planOptions: List<PremiumPlanOption>,
    selectedBillingCycle: String,
    onBillingCycleChange: (String) -> Unit,
    accentColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    fontFamily: FontFamily
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        planOptions.take(2).forEach { plan ->
            val selected = plan.billingCycle == selectedBillingCycle
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (selected) accentColor.copy(alpha = 0.16f)
                        else Color.White.copy(alpha = 0.12f)
                    )
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) accentColor else dividerColor,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onBillingCycleChange(plan.billingCycle) }
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicText(
                    plan.label.ifBlank { plan.billingCycle.replaceFirstChar { it.uppercase() } },
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = fontFamily
                    )
                )
                BasicText(
                    plan.displayAmount,
                    style = TextStyle(
                        color = if (selected) accentColor else contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily
                    )
                )
                BasicText(
                    plan.savingsLabel ?: premiumBillingLabel(plan.billingCycle, plan.durationDays),
                    style = TextStyle(
                        color = secondaryTextColor,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontFamily = fontFamily
                    )
                )
            }
        }
    }
}

@Composable
private fun MorePremiumMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    supporting: String,
    accentColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    isGlassTheme: Boolean,
    fontFamily: FontFamily
) {
    Column(
        modifier = modifier
            .widthIn(min = 152.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = if (isGlassTheme) 0.14f else 0.48f))
            .border(1.dp, dividerColor.copy(alpha = 0.82f), RoundedCornerShape(20.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = secondaryTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = fontFamily
            )
        )
        BasicText(
            value,
            style = TextStyle(
                color = contentColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily
            )
        )
        BasicText(
            supporting,
            style = TextStyle(
                color = secondaryTextColor.copy(alpha = 0.92f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontFamily = fontFamily
            )
        )
    }
}

@Composable
private fun MorePremiumFeatureCard(
    modifier: Modifier = Modifier,
    item: PremiumHighlight,
    accentColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    isGlassTheme: Boolean,
    fontFamily: FontFamily
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = if (isGlassTheme) 0.11f else 0.34f))
            .border(1.dp, dividerColor.copy(alpha = 0.66f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        BasicText(
            item.title,
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily
            )
        )
        BasicText(
            item.detail,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = secondaryTextColor,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontFamily = fontFamily
            )
        )
    }
}

@Composable
private fun MorePremiumAnimatedButton(
    label: String,
    supporting: String,
    accentColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    composition: com.airbnb.lottie.LottieComposition?,
    progress: Float,
    isGlassTheme: Boolean,
    fontFamily: FontFamily
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(
                        listOf(
                            accentColor,
                            Color(0xFFEAA22D)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.40f),
                            accentColor.copy(alpha = 0.28f)
                        )
                    )
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicText(
                    label,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily
                    )
                )
                BasicText(
                    supporting,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontFamily = fontFamily
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = if (isGlassTheme) 0.16f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (composition != null) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.size(44.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MorePremiumAnimationCta(
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    composition: com.airbnb.lottie.LottieComposition?,
    progress: Float
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.46f }
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(136.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = "Go Premium",
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun MorePremiumSecondaryButton(
    label: String,
    contentColor: Color,
    dividerColor: Color,
    fontFamily: FontFamily,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Transparent)
            .border(1.dp, dividerColor.copy(alpha = 0.72f), RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily
            )
        )
    }
}

@Composable
private fun MorePremiumFeaturePill(
    label: String,
    accentColor: Color,
    contentColor: Color,
    fontFamily: FontFamily
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = fontFamily
            )
        )
    }
}

@Composable
private fun MoreQuickAccessHub(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    quickActions: List<MoreQuickAction>,
    animationKey: Int,
    onOpenFullMoreScreen: () -> Unit
) {
    val pinnedAiChatAction = quickActions.firstOrNull {
        it.label == "vormex" || it.title == "vormex"
    }
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.free_interactive_radial_menu)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    val scope = rememberCoroutineScope()
    var expanded by remember(animationKey) { mutableStateOf(false) }
    var isTransitioning by remember(animationKey) { mutableStateOf(false) }
    val isDarkSurface = rememberMoreUsesDarkSurface(isGlassTheme = isGlassTheme)
    val actionSurfaceColor = if (isDarkSurface) {
        Color.White.copy(alpha = if (isGlassTheme) 0.08f else 0.06f)
    } else {
        Color.White.copy(alpha = if (isGlassTheme) 0.22f else 0.72f)
    }
    val actionBorderColor = if (isDarkSurface) {
        Color.White.copy(alpha = if (isGlassTheme) 0.12f else 0.10f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val aiChatCardSurfaceColor = if (isDarkSurface) Color(0xFF181A20) else Color.White
    val aiChatCardBorderColor = if (isDarkSurface) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
    val radius = 122f
    val angleStep = 360f / quickActions.size.coerceAtLeast(1)
    val lottieScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.82f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "more_hub_lottie_scale"
    )
    val lottieAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.55f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "more_hub_lottie_alpha"
    )
    val centerScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.82f,
        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
        label = "more_hub_center_scale"
    )
    val centerRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -135f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "more_hub_center_rotation"
    )

    LaunchedEffect(animationKey) {
        isTransitioning = false
        expanded = false
        delay(90)
        expanded = true
    }

    fun launchHubAction(openFullMore: Boolean, action: () -> Unit) {
        if (isTransitioning) return
        isTransitioning = true
        expanded = false
        scope.launch {
            delay(180)
            action()
            if (!openFullMore) {
                delay(240)
                expanded = true
                isTransitioning = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        pinnedAiChatAction?.let { action ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .widthIn(max = 330.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(aiChatCardSurfaceColor)
                    .border(1.dp, aiChatCardBorderColor, RoundedCornerShape(12.dp))
                    .noRippleClickable {
                        launchHubAction(openFullMore = false) {
                            action.onClick()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.10f))
                            .border(1.dp, aiChatCardBorderColor, CircleShape)
                            .padding(7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.vormex_logo),
                            contentDescription = "Vormex logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BasicText(
                            "vormex",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        BasicText(
                            "Open vormex",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.62f),
                                fontSize = 11.sp
                            )
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.13f))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            BasicText(
                                "Hi",
                                style = TextStyle(
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(contentColor.copy(alpha = 0.10f))
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(360.dp)
                .padding(top = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier
                        .size(282.dp)
                        .graphicsLayer {
                            alpha = lottieAlpha
                            scaleX = lottieScale
                            scaleY = lottieScale
                        },
                    contentScale = ContentScale.Fit,
                    clipToCompositionBounds = true
                )
            }

            quickActions.forEachIndexed { index, action ->
                val itemProgress by animateFloatAsState(
                    targetValue = if (expanded) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 420,
                        delayMillis = 70 + (index * 45),
                        easing = FastOutSlowInEasing
                    ),
                    label = "more_hub_item_$index"
                )
                val angleDegrees = -90f + (index * angleStep)
                val angleRadians = angleDegrees * (PI / 180.0)
                val targetX = (radius * cos(angleRadians)).toFloat()
                val targetY = (radius * sin(angleRadians)).toFloat()

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(
                            x = (targetX * itemProgress).dp,
                            y = (targetY * itemProgress).dp
                        )
                        .graphicsLayer {
                            alpha = itemProgress
                            val scale = 0.52f + (itemProgress * 0.48f)
                            scaleX = scale
                            scaleY = scale
                        }
                ) {
                    MoreRadialShortcutBubble(
                        action = action,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGlassTheme = isGlassTheme,
                        surfaceColor = actionSurfaceColor,
                        borderColor = actionBorderColor,
                        onClick = {
                            launchHubAction(openFullMore = false) {
                                action.onClick()
                            }
                        }
                    )
                }
            }

            MoreRadialCenterButton(
                modifier = Modifier.graphicsLayer {
                    scaleX = centerScale
                    scaleY = centerScale
                    rotationZ = centerRotation
                },
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isDarkSurface = isDarkSurface,
                onClick = {
                    launchHubAction(openFullMore = true) {
                        onOpenFullMoreScreen()
                    }
                }
            )
        }
    }
}

@Composable
private fun MoreRadialShortcutBubble(
    action: MoreQuickAction,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(width = 88.dp, height = 96.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .then(
                        if (isGlassTheme) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { Capsule() },
                                effects = {
                                    vibrancy()
                                    blur(16f.dp.toPx())
                                    lens(8f.dp.toPx(), 14f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(surfaceColor)
                                }
                            )
                        } else {
                            Modifier.background(surfaceColor)
                        }
                    )
                    .border(1.dp, borderColor, CircleShape)
                    .noRippleClickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            BasicText(
                action.label,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                ),
                maxLines = 2
            )
        }

        action.badgeLabel?.let { badge ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                BasicText(
                    badge,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        if (action.showIndicatorDot && action.badgeLabel == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}

@Composable
private fun MoreRadialCenterButton(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isDarkSurface: Boolean,
    onClick: () -> Unit
) {
    val centerSurfaceColor = accentColor.copy(alpha = if (isDarkSurface) 0.20f else 0.18f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = modifier
                .size(92.dp)
                .clip(CircleShape)
                .then(
                    if (isGlassTheme) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                vibrancy()
                                blur(18f.dp.toPx())
                                lens(10f.dp.toPx(), 18f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(centerSurfaceColor)
                            }
                        )
                    } else {
                        Modifier.background(centerSurfaceColor)
                    }
                )
                .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape)
                .noRippleClickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "More",
                tint = contentColor,
                modifier = Modifier.size(34.dp)
            )
        }
        BasicText(
            "More",
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

private data class MoreSettingsItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val trailingLabel: String? = null,
    val showIndicatorDot: Boolean = false,
    val searchTerms: List<String> = emptyList(),
    val isDestructive: Boolean = false,
    val isSpotlight: Boolean = false,
    val spotlightLabel: String? = null
)

private data class MoreSettingsSection(
    val title: String,
    val items: List<MoreSettingsItem>
)

@Composable
private fun MoreProfileCustomizationsSection(
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    currentUser: com.kyant.backdrop.catalog.network.models.User? = null,
    sectionSurfaceColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    sectionHeaderColor: Color,
    showSectionHeader: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val equipped by SettingsPreferences.equippedProfileLoaderGiftId(context).collectAsState(initial = null)
    val profileFrameEnabled by SettingsPreferences.profileFrameEnabled(context).collectAsState(initial = false)
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    val activeLoaderGiftId = equipped?.takeIf { ProfileLoaderGifts.rawResForGiftId(it) != null }
    val isWolfieEquipped = equipped == ProfileLoaderGifts.BIG_BAD_WOLFIE
    val isMortyEquipped = equipped == ProfileLoaderGifts.MORTY_DANCE

    fun syncLoaderGift(nextGiftId: String?) {
        scope.launch {
            val previousGiftId = equipped
            SettingsPreferences.setEquippedProfileLoaderGiftId(context, nextGiftId)
            ApiClient.getCurrentUserId(context)?.let { userId ->
                ProfileLoaderGiftMemory.put(userId, nextGiftId)
            }
            ApiClient.updateProfile(
                context,
                ProfileUpdateRequest(visitLoaderGiftId = nextGiftId),
                explicitNullFields = if (nextGiftId == null) setOf("visitLoaderGiftId") else emptySet()
            ).onFailure { e ->
                SettingsPreferences.setEquippedProfileLoaderGiftId(context, previousGiftId)
                ApiClient.getCurrentUserId(context)?.let { userId ->
                    ProfileLoaderGiftMemory.put(userId, previousGiftId)
                }
                Toast.makeText(
                    context,
                    e.message ?: "Couldn't sync gift",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showSectionHeader) {
            MoreSectionHeader("Profile customizations", sectionHeaderColor)
        }

        MoreProfileCustomizationPreviewCard(
            user = currentUser,
            activeLoaderGiftId = activeLoaderGiftId,
            profileFrameEnabled = profileFrameEnabled,
            reduceAnimations = reduceAnimations,
            backdrop = backdrop,
            isGlassTheme = isGlassTheme,
            sectionSurfaceColor = sectionSurfaceColor,
            dividerColor = dividerColor,
            contentColor = contentColor,
            secondaryTextColor = secondaryTextColor,
            accentColor = accentColor
        )

        MoreProfileCustomizationCard(
            title = "Profile Frame",
            subtitle = if (reduceAnimations && profileFrameEnabled) {
                "Shows around your avatar on your profile. Equipped, but paused while Reduce Animations is on."
            } else {
                "Shows an animated frame around your avatar on your profile."
            },
            isEquipped = profileFrameEnabled,
            backdrop = backdrop,
            isGlassTheme = isGlassTheme,
            sectionSurfaceColor = sectionSurfaceColor,
            dividerColor = dividerColor,
            contentColor = contentColor,
            secondaryTextColor = secondaryTextColor,
            accentColor = accentColor,
            preview = {
                MoreProfileAvatarPreview(
                    user = currentUser,
                    showFrame = true,
                    reduceAnimations = reduceAnimations,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    modifier = Modifier.size(82.dp)
                )
            },
            onToggle = {
                scope.launch {
                    val nextEnabled = !profileFrameEnabled
                    SettingsPreferences.setProfileFrameEnabled(context, nextEnabled)
                    ApiClient.updateProfile(
                        context,
                        ProfileUpdateRequest(
                            profileRing = if (nextEnabled) PROFILE_FRAME_RING_ID else null
                        ),
                        explicitNullFields = if (nextEnabled) emptySet() else setOf("profileRing")
                    ).onFailure { e ->
                        SettingsPreferences.setProfileFrameEnabled(context, profileFrameEnabled)
                        Toast.makeText(
                            context,
                            e.message ?: "Couldn't sync profile frame",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        MoreProfileCustomizationCard(
            title = "Big Bad Wolfie",
            subtitle = "Visitors see this animation while your profile loads. Equip to use it.",
            isEquipped = isWolfieEquipped,
            backdrop = backdrop,
            isGlassTheme = isGlassTheme,
            sectionSurfaceColor = sectionSurfaceColor,
            dividerColor = dividerColor,
            contentColor = contentColor,
            secondaryTextColor = secondaryTextColor,
            accentColor = accentColor,
            preview = {
                MoreProfileLoaderAnimationPreview(
                    giftId = ProfileLoaderGifts.BIG_BAD_WOLFIE,
                    reduceAnimations = reduceAnimations,
                    accentColor = accentColor,
                    modifier = Modifier.size(74.dp)
                )
            },
            onToggle = {
                syncLoaderGift(
                    if (isWolfieEquipped) null else ProfileLoaderGifts.BIG_BAD_WOLFIE
                )
            }
        )

        MoreProfileCustomizationCard(
            title = "Morty Dance Loader",
            subtitle = "Visitors see this dance animation while your profile opens. Equip to use it.",
            isEquipped = isMortyEquipped,
            backdrop = backdrop,
            isGlassTheme = isGlassTheme,
            sectionSurfaceColor = sectionSurfaceColor,
            dividerColor = dividerColor,
            contentColor = contentColor,
            secondaryTextColor = secondaryTextColor,
            accentColor = accentColor,
            preview = {
                MoreProfileLoaderAnimationPreview(
                    giftId = ProfileLoaderGifts.MORTY_DANCE,
                    reduceAnimations = reduceAnimations,
                    accentColor = accentColor,
                    modifier = Modifier.size(74.dp)
                )
            },
            onToggle = {
                syncLoaderGift(
                    if (isMortyEquipped) null else ProfileLoaderGifts.MORTY_DANCE
                )
            }
        )
    }
}

@Composable
private fun MoreProfileCustomizationPreviewCard(
    user: com.kyant.backdrop.catalog.network.models.User?,
    activeLoaderGiftId: String?,
    profileFrameEnabled: Boolean,
    reduceAnimations: Boolean,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    sectionSurfaceColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color
) {
    val activeLoaderLabel = ProfileLoaderGifts.resolvedVisitorLabel(activeLoaderGiftId)
    val summaryText = when {
        profileFrameEnabled && !activeLoaderGiftId.isNullOrBlank() && reduceAnimations ->
            "Your frame is active, and $activeLoaderLabel is selected for visitors. Reduce Animations pauses the motion preview."
        profileFrameEnabled && !activeLoaderGiftId.isNullOrBlank() ->
            "Your frame is active, and visitors will see $activeLoaderLabel while your profile opens."
        profileFrameEnabled ->
            "Your profile frame is active. Visitors still see $activeLoaderLabel while your profile opens."
        !activeLoaderGiftId.isNullOrBlank() && reduceAnimations ->
            "$activeLoaderLabel is selected for profile visits. Reduce Animations pauses the live motion."
        !activeLoaderGiftId.isNullOrBlank() ->
            "$activeLoaderLabel is ready for profile visits."
        else ->
            "$activeLoaderLabel is your default visitor loader until you equip something else."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(20.dp) },
                        effects = {
                            vibrancy()
                            blur(14f.dp.toPx())
                            lens(6f.dp.toPx(), 12f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(sectionSurfaceColor)
                        }
                    )
                } else {
                    Modifier.background(sectionSurfaceColor)
                }
            )
            .border(1.dp, dividerColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        "Live preview",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    BasicText(
                        summaryText,
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 12.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        "3 options",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MoreCustomizationPreviewTile(
                    title = "Profile look",
                    subtitle = if (profileFrameEnabled) "Frame on" else "Frame off",
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    borderColor = dividerColor
                ) {
                    MoreProfileAvatarPreview(
                        user = user,
                        showFrame = profileFrameEnabled,
                        reduceAnimations = reduceAnimations,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        modifier = Modifier.size(96.dp)
                    )
                }

                MoreCustomizationPreviewTile(
                    title = "Visitor loader",
                    subtitle = if (reduceAnimations && !activeLoaderGiftId.isNullOrBlank()) {
                        "$activeLoaderLabel selected"
                    } else {
                        activeLoaderLabel
                    },
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    borderColor = dividerColor
                ) {
                    MoreProfileLoaderAnimationPreview(
                        giftId = activeLoaderGiftId,
                        reduceAnimations = reduceAnimations,
                        accentColor = accentColor,
                        modifier = Modifier.size(82.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.MoreCustomizationPreviewTile(
    title: String,
    subtitle: String,
    contentColor: Color,
    secondaryTextColor: Color,
    borderColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(contentColor.copy(alpha = 0.04f))
            .border(1.dp, borderColor.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp),
            contentAlignment = Alignment.Center,
            content = content
        )
        BasicText(
            title,
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        BasicText(
            subtitle,
            style = TextStyle(
                color = secondaryTextColor,
                fontSize = 11.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MoreProfileAvatarPreview(
    user: com.kyant.backdrop.catalog.network.models.User?,
    showFrame: Boolean,
    reduceAnimations: Boolean,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val displayName = user?.name?.takeIf { it.isNotBlank() } ?: user?.username ?: "You"
    val profileImage = user?.profileImage
    val initials = displayName
        .split(" ")
        .mapNotNull { token -> token.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifBlank { "Y" }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            if (!profileImage.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profileImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                BasicText(
                    initials,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        if (showFrame) {
            ProfileFrameLottie(
                modifier = Modifier.fillMaxSize(),
                isPlaying = !reduceAnimations
            )
        }
    }
}

@Composable
private fun MoreProfileLoaderAnimationPreview(
    giftId: String?,
    reduceAnimations: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val rawRes = ProfileLoaderGifts.resolvedVisitorRawRes(giftId)

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = !reduceAnimations
    )

    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier,
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,
            clipToCompositionBounds = true
        )
        return
    }

    CircularProgressIndicator(
        modifier = modifier.size(38.dp),
        color = accentColor,
        strokeWidth = 3.dp
    )
}

@Composable
private fun MoreProfileCustomizationCard(
    title: String,
    subtitle: String,
    isEquipped: Boolean,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    sectionSurfaceColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    preview: (@Composable BoxScope.() -> Unit)? = null,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(18.dp) },
                        effects = {
                            vibrancy()
                            blur(14f.dp.toPx())
                            lens(6f.dp.toPx(), 12f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(sectionSurfaceColor)
                        }
                    )
                } else {
                    Modifier.background(sectionSurfaceColor)
                }
            )
            .border(1.dp, dividerColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (preview != null) 0.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        title,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    BasicText(
                        subtitle,
                        style = TextStyle(secondaryTextColor, 12.sp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (preview != null) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(contentColor.copy(alpha = 0.04f))
                            .border(1.dp, dividerColor.copy(alpha = 0.78f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center,
                        content = preview
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isEquipped) contentColor.copy(alpha = 0.12f) else accentColor
                        )
                        .clickable(onClick = onToggle)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        if (isEquipped) "Unequip" else "Equip",
                        style = TextStyle(
                            color = if (isEquipped) contentColor else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreFullScreenTopBar(
    title: String,
    contentColor: Color,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .clip(CircleShape)
                .noRippleClickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = contentColor,
                modifier = Modifier.size(25.dp)
            )
        }

        BasicText(
            title,
            modifier = Modifier.align(Alignment.Center),
            style = TextStyle(
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MoreSettingsGroupDivider(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .background(color)
    )
}

@Composable
private fun MoreSettingsListSectionHeader(
    title: String,
    contentColor: Color,
    trailingLabel: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BasicText(
            title,
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        trailingLabel?.let { label ->
            BasicText(
                label,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MoreSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    surfaceColor: Color,
    contentColor: Color,
    placeholderColor: Color,
    cursorColor: Color
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(14.dp) },
                        effects = {
                            vibrancy()
                            blur(12f.dp.toPx())
                            lens(5f.dp.toPx(), 10f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(surfaceColor)
                        }
                    )
                } else {
                    Modifier.background(surfaceColor)
                }
            )
            .padding(horizontal = 14.dp),
        textStyle = TextStyle(
            color = contentColor,
            fontSize = 13.sp
        ),
        cursorBrush = SolidColor(cursorColor),
        singleLine = true,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = placeholderColor,
                    modifier = Modifier.size(18.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        BasicText(
                            "Search",
                            style = TextStyle(
                                color = placeholderColor,
                                fontSize = 13.sp
                            )
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun MoreCurrentUserCard(
    user: com.kyant.backdrop.catalog.network.models.User,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(18.dp) },
                        effects = {
                            vibrancy()
                            blur(14f.dp.toPx())
                            lens(6f.dp.toPx(), 12f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(surfaceColor)
                        }
                    )
                } else {
                    Modifier.background(surfaceColor)
                }
            )
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .noRippleClickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (!user.profileImage.isNullOrEmpty()) {
                    AsyncImage(
                        model = user.profileImage,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    BasicText(
                        user.name?.firstOrNull()?.uppercase() ?: "U",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                BasicText(
                    user.name ?: "User",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!user.username.isNullOrEmpty()) {
                    BasicText(
                        "@${user.username}",
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicText(
                    user.headline?.takeIf { it.isNotBlank() } ?: "Profile and account details",
                    style = TextStyle(
                        color = secondaryTextColor,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            BasicText(
                "›",
                style = TextStyle(
                    color = secondaryTextColor,
                    fontSize = 20.sp
                )
            )
        }
    }
}

@Composable
private fun MoreSettingsSectionCard(
    items: List<MoreSettingsItem>,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    destructiveColor: Color
) {
    val sectionBackground = if (isGlassTheme) surfaceColor else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(sectionBackground)
    ) {
        items.forEachIndexed { index, item ->
            MoreSettingsRow(
                item = item,
                contentColor = contentColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                destructiveColor = destructiveColor
            )

            if (index != items.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 58.dp, end = 18.dp)
                        .height(1.dp)
                        .background(borderColor)
                )
            }
        }
    }
}

@Composable
private fun MoreConnectionRequestsCard(
    requests: List<PendingConnectionRequest>,
    isLoading: Boolean,
    error: String?,
    actionInProgress: Set<String>,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    onOpenProfile: (String) -> Unit,
    onAccept: (String, String) -> Unit,
    onReject: (String, String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        when {
            isLoading && requests.isEmpty() -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        BasicText(
                            "Checking for new requests",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        BasicText(
                            "People who want to connect will show up here.",
                            style = TextStyle(
                                color = secondaryTextColor,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            error != null && requests.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        "Could not load connection requests",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    BasicText(
                        error,
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            requests.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        "No connection requests right now",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    BasicText(
                        "When someone sends you a request, you’ll see them here.",
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            else -> {
                requests.forEachIndexed { index, request ->
                    MoreConnectionRequestRow(
                        request = request,
                        isActionInProgress = actionInProgress.contains(request.user.id),
                        contentColor = contentColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onOpenProfile = onOpenProfile,
                        onAccept = onAccept,
                        onReject = onReject
                    )

                    if (index != requests.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 64.dp)
                                .height(1.dp)
                                .background(borderColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreConnectionRequestRow(
    request: PendingConnectionRequest,
    isActionInProgress: Boolean,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    borderColor: Color,
    onOpenProfile: (String) -> Unit,
    onAccept: (String, String) -> Unit,
    onReject: (String, String) -> Unit
) {
    val user = request.user
    val displayName = user.name?.takeIf { it.isNotBlank() } ?: user.username ?: "Vormex user"
    val subtitle = listOfNotNull(
        user.headline?.takeIf { it.isNotBlank() },
        user.college?.takeIf { it.isNotBlank() }
    ).joinToString(" • ").ifBlank { "Sent you a connection request" }
    val avatarLetter = (
        user.name?.firstOrNull()?.toString()
            ?: user.username?.firstOrNull()?.toString()
            ?: "U"
        ).uppercase()
    val priorityLabel = request.priorityLabel
        ?: when {
            user.profileBoostActive -> "Boosted request"
            user.isPremium -> "Premium request"
            else -> null
        }
    val showActionsBeside = displayName.length <= 16

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (!user.profileImage.isNullOrEmpty()) {
                AsyncImage(
                    model = user.profileImage,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                BasicText(
                    avatarLetter,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Column(
                modifier = Modifier.clickable { onOpenProfile(user.id) },
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                BasicText(
                    displayName,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!user.username.isNullOrBlank()) {
                    BasicText(
                        "@${user.username}",
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 10.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                priorityLabel?.let { label ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(accentColor.copy(alpha = 0.13f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText(
                            label,
                            style = TextStyle(
                                color = accentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                BasicText(
                    subtitle,
                    style = TextStyle(
                        color = secondaryTextColor,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!showActionsBeside) {
                MoreConnectionRequestActions(
                    isActionInProgress = isActionInProgress,
                    secondaryTextColor = secondaryTextColor,
                    accentColor = accentColor,
                    contentColor = contentColor,
                    borderColor = borderColor,
                    onAccept = { onAccept(user.id, request.id) },
                    onReject = { onReject(user.id, request.id) }
                )
            }
        }

        if (showActionsBeside) {
            MoreConnectionRequestActions(
                isActionInProgress = isActionInProgress,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                contentColor = contentColor,
                borderColor = borderColor,
                onAccept = { onAccept(user.id, request.id) },
                onReject = { onReject(user.id, request.id) },
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

@Composable
private fun MoreConnectionRequestActions(
    isActionInProgress: Boolean,
    secondaryTextColor: Color,
    accentColor: Color,
    contentColor: Color,
    borderColor: Color,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isActionInProgress) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = accentColor
            )
            BasicText(
                "Updating",
                style = TextStyle(
                    color = secondaryTextColor,
                    fontSize = 10.sp
                )
            )
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MoreConnectionRequestActionButton(
                label = "Accept",
                filled = true,
                accentColor = accentColor,
                contentColor = contentColor,
                borderColor = borderColor,
                onClick = onAccept
            )
            MoreConnectionRequestActionButton(
                label = "Ignore",
                filled = false,
                accentColor = accentColor,
                contentColor = contentColor,
                borderColor = borderColor,
                onClick = onReject
            )
        }
    }
}

@Composable
private fun MoreConnectionRequestActionButton(
    label: String,
    filled: Boolean,
    accentColor: Color,
    contentColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (filled) accentColor else accentColor.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = if (filled) accentColor else borderColor,
                shape = RoundedCornerShape(999.dp)
            )
            .noRippleClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = if (filled) Color.White else contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun MoreSettingsRow(
    item: MoreSettingsItem,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    destructiveColor: Color
) {
    val rowColor = if (item.isDestructive) destructiveColor else contentColor
    val rowSecondaryColor = if (item.isDestructive) {
        destructiveColor.copy(alpha = 0.7f)
    } else {
        secondaryTextColor
    }
    val spotlightTransition = if (item.isSpotlight) {
        rememberInfiniteTransition(label = "more_talent_engine_spotlight")
    } else {
        null
    }
    val spotlightPulse = spotlightTransition?.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "more_talent_engine_spotlight_pulse"
    )?.value ?: 0f
    val rowShape = RoundedCornerShape(if (item.isSpotlight) 18.dp else 0.dp)
    val spotlightBackground = Brush.linearGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.24f + spotlightPulse * 0.08f),
            Color(0xFF22C55E).copy(alpha = 0.14f + spotlightPulse * 0.05f),
            Color(0xFFF59E0B).copy(alpha = 0.12f + spotlightPulse * 0.04f)
        )
    )
    val rowModifier = if (item.isSpotlight) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(rowShape)
            .background(spotlightBackground)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.34f + spotlightPulse * 0.24f),
                shape = rowShape
            )
            .noRippleClickable(onClick = item.onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .noRippleClickable(onClick = item.onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (item.isSpotlight) 40.dp else 26.dp)
                    .then(
                        if (item.isSpotlight) {
                            Modifier
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.18f + spotlightPulse * 0.08f))
                                .graphicsLayer {
                                    scaleX = 1f + spotlightPulse * 0.04f
                                    scaleY = 1f + spotlightPulse * 0.04f
                                }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (item.isSpotlight) accentColor else rowColor.copy(alpha = if (item.isDestructive) 1f else 0.9f),
                    modifier = Modifier.size(if (item.isSpotlight) 23.dp else 24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BasicText(
                        item.title,
                        style = TextStyle(
                            color = rowColor,
                            fontSize = if (item.isSpotlight) 15.sp else 16.sp,
                            fontWeight = if (item.isSpotlight) FontWeight.Bold else FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    item.spotlightLabel?.let { label ->
                        BasicText(
                            label,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(accentColor.copy(alpha = 0.18f + spotlightPulse * 0.08f))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            style = TextStyle(
                                color = accentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                BasicText(
                    item.subtitle,
                    style = TextStyle(
                        color = if (item.isSpotlight) contentColor.copy(alpha = 0.74f) else rowSecondaryColor,
                        fontSize = if (item.isSpotlight) 12.sp else 13.sp,
                        lineHeight = if (item.isSpotlight) 16.sp else 17.sp,
                        fontWeight = if (item.isSpotlight) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item.trailingLabel?.let { label ->
                BasicText(
                    label,
                    modifier = Modifier.widthIn(max = 118.dp),
                    style = TextStyle(
                        color = if (item.isDestructive) destructiveColor else secondaryTextColor,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.showIndicatorDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }

            BasicText(
                "›",
                style = TextStyle(
                    color = rowSecondaryColor,
                    fontSize = 24.sp
                )
            )
        }
    }
}

@Composable
private fun MoreSectionHeader(
    title: String,
    contentColor: Color
) {
    BasicText(
        title,
        style = TextStyle(
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        ),
        modifier = Modifier.padding(start = 2.dp)
    )
}

@Composable
private fun ConnectionRequestsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    requests: List<PendingConnectionRequest>,
    isLoading: Boolean,
    error: String?,
    actionInProgress: Set<String>,
    onNavigateBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onAccept: (String, String) -> Unit,
    onReject: (String, String) -> Unit
) {
    val isDarkSurface = rememberMoreUsesDarkSurface(isGlassTheme = isGlassTheme)
    val sectionSurfaceColor = if (isDarkSurface) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.White.copy(alpha = 0.22f)
    }
    val dividerColor = if (isDarkSurface) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    val secondaryTextColor = contentColor.copy(alpha = if (isDarkSurface) 0.66f else 0.58f)
    val requestCountText = when (requests.size) {
        0 -> "No pending requests"
        1 -> "1 pending request"
        else -> "${requests.size} pending requests"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        SettingsHeader(
            title = "Connection requests",
            contentColor = contentColor,
            onBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 2.dp, end = 2.dp, bottom = 2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                BasicText(
                    requestCountText,
                    style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                )
                BasicText(
                    "Review requests and respond from here.",
                    style = TextStyle(secondaryTextColor, 12.sp)
                )
            }

            MoreConnectionRequestsCard(
                requests = requests,
                isLoading = isLoading,
                error = error,
                actionInProgress = actionInProgress,
                backdrop = backdrop,
                isGlassTheme = isGlassTheme,
                surfaceColor = sectionSurfaceColor,
                borderColor = dividerColor,
                contentColor = contentColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                onOpenProfile = onOpenProfile,
                onAccept = onAccept,
                onReject = onReject
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ProfileCustomizationsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    currentUser: com.kyant.backdrop.catalog.network.models.User? = null,
    onNavigateBack: () -> Unit
) {
    val isDarkSurface = rememberMoreUsesDarkSurface(isGlassTheme = isGlassTheme)
    val sectionSurfaceColor = if (isDarkSurface) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.White.copy(alpha = 0.22f)
    }
    val dividerColor = if (isDarkSurface) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    val secondaryTextColor = contentColor.copy(alpha = if (isDarkSurface) 0.66f else 0.58f)
    val sectionHeaderColor = contentColor.copy(alpha = if (isDarkSurface) 0.72f else 0.48f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        SettingsHeader(
            title = "Profile customizations",
            contentColor = contentColor,
            onBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MoreProfileCustomizationsSection(
                backdrop = backdrop,
                isGlassTheme = isGlassTheme,
                currentUser = currentUser,
                sectionSurfaceColor = sectionSurfaceColor,
                dividerColor = dividerColor,
                contentColor = contentColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                sectionHeaderColor = sectionHeaderColor,
                showSectionHeader = false
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun MoreWorkspaceCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    goalsProgressText: String,
    connectionStreak: Int,
    liveNow: Int,
    remainingConnections: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(22.dp) },
                        effects = {
                            vibrancy()
                            blur(14f.dp.toPx())
                            lens(8f.dp.toPx(), 16f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(accentColor.copy(alpha = 0.14f))
                        }
                    )
                } else {
                    Modifier.background(contentColor.copy(alpha = 0.08f))
                }
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    "Your workspace",
                    style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
                )
                BasicText(
                    "Everything you revisit often should feel instant and easy to scan.",
                    style = TextStyle(contentColor.copy(alpha = 0.65f), 12.sp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoreWorkspaceMetric(
                    modifier = Modifier.weight(1f),
                    label = "Goals",
                    value = goalsProgressText,
                    accentColor = accentColor,
                    contentColor = contentColor
                )
                MoreWorkspaceMetric(
                    modifier = Modifier.weight(1f),
                    label = "Streak",
                    value = "${connectionStreak.coerceAtLeast(0)} days",
                    accentColor = accentColor,
                    contentColor = contentColor
                )
                MoreWorkspaceMetric(
                    modifier = Modifier.weight(1f),
                    label = "Room",
                    value = "${remainingConnections.coerceAtLeast(0)} today",
                    accentColor = accentColor,
                    contentColor = contentColor
                )
            }

            if (liveNow > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = if (isGlassTheme) 0.12f else 0.06f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicText(
                        "$liveNow people are networking right now. Good moment to reply, connect, or post.",
                        style = TextStyle(contentColor.copy(alpha = 0.72f), 12.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberMoreUsesDarkSurface(isGlassTheme: Boolean): Boolean {
    val appearance = currentVormexAppearance()
    if (appearance.isDarkTheme) return true
    if (!isGlassTheme) return false

    val context = LocalContext.current
    val glassBackgroundKey by SettingsPreferences.glassBackgroundPreset(context)
        .collectAsState(initial = DefaultGlassBackgroundPresetKey)

    return remember(glassBackgroundKey) {
        isDarkGlassBackgroundPreset(glassBackgroundKey)
    }
}

private fun isDarkGlassBackgroundPreset(key: String): Boolean {
    val averageLuminance = glassBackgroundPreset(key)
        .baseColors
        .map { it.luminance() }
        .average()
    return averageLuminance < 0.32
}

@Composable
private fun MoreWorkspaceMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color,
    contentColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(
                label,
                style = TextStyle(contentColor.copy(alpha = 0.58f), 11.sp, FontWeight.Medium)
            )
            BasicText(
                value,
                style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun MoreQuickActionChip(
    label: String,
    icon: String,
    accentColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BasicText(icon, style = TextStyle(fontSize = 14.sp))
            BasicText(
                label,
                style = TextStyle(contentColor, 12.sp, FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun MoreMenuItemWithSubtitle(
    title: String,
    subtitle: String,
    iconResId: Int,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    trailingIconResId: Int? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isGlassTheme) Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(16.dp) },
                    effects = {
                        vibrancy()
                        blur(10f.dp.toPx())
                        lens(4f.dp.toPx(), 8f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.08f))
                    }
                ) else Modifier.background(contentColor.copy(alpha = 0.08f))
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(contentColor)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 16.sp, FontWeight.Medium)
                    )
                    BasicText(
                        subtitle,
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                trailingIconResId?.let { resId ->
                    Image(
                        painter = painterResource(resId),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.7f))
                    )
                    Spacer(Modifier.width(8.dp))
                }
                BasicText(
                    "›",
                    style = TextStyle(contentColor.copy(alpha = 0.3f), 24.sp)
                )
            }
        }
    }
}

@Composable
private fun MoreMenuItem(
    title: String,
    iconResId: Int,
    backdrop: LayerBackdrop,
    contentColor: Color,
    isGlassTheme: Boolean
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isGlassTheme) Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(16.dp) },
                    effects = {
                        vibrancy()
                        blur(10f.dp.toPx())
                        lens(4f.dp.toPx(), 8f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.08f))
                    }
                ) else Modifier.background(contentColor.copy(alpha = 0.08f))
            )
            .clickable { }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(iconResId),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(contentColor)
            )
            Spacer(Modifier.width(16.dp))
            BasicText(
                title,
                style = TextStyle(contentColor, 16.sp, FontWeight.Medium)
            )
        }
    }
}

/**
 * Onboarding Prompt Banner - Shows when user hasn't completed profile setup
 * Encourages users to complete their profile to unlock collaborations
 */
@Composable
private fun OnboardingPromptBanner(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onGetStarted: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(20.dp) },
                effects = {
                    vibrancy()
                    blur(14f.dp.toPx())
                    lens(6f.dp.toPx(), 12f.dp.toPx())
                },
                onDrawSurface = {
                    // Gradient background for attention
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.25f),
                                Color(0xFF9C27B0).copy(alpha = 0.2f)
                            )
                        )
                    )
                }
            )
            .clickable(onClick = onGetStarted)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon/Emoji section
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(accentColor, Color(0xFF9C27B0))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "✨",
                    style = TextStyle(fontSize = 28.sp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    "Complete Your Profile",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(Modifier.height(4.dp))

                BasicText(
                    "Fill in your details to unlock collaborations and connect with like-minded people",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Get Started button
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(accentColor, Color(0xFF9C27B0))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    BasicText(
                        "Get Started →",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreMenuItemWithAction(
    title: String,
    icon: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24f.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(accentColor.copy(alpha = 0.15f))
                }
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(icon, style = TextStyle(fontSize = 24.sp))
                Spacer(Modifier.width(16.dp))
                Column {
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 16.sp, FontWeight.Medium)
                    )
                    BasicText(
                        when (title) {
                            "Groups" -> "Connect with communities"
                            "Circles" -> "Share with close friends"
                            "Reels" -> "Watch short videos"
                            "Edit Profile Preferences" -> "Update goals, interests & more"
                            else -> ""
                        },
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                    )
                }
            }

            // Arrow indicator
            BasicText(
                "→",
                style = TextStyle(contentColor.copy(alpha = 0.5f), 20.sp)
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    val icon = when (notification.type) {
        NotificationType.LIKE -> "👍"
        NotificationType.COMMENT -> "💬"
        NotificationType.CONNECTION -> "👤"
        NotificationType.JOB -> "💼"
        NotificationType.MENTION -> "📢"
        NotificationType.VIEW -> "👁️"
    }

    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24f.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.1f))
                }
            )
            .clickable { }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText(icon, style = TextStyle(fontSize = 20.sp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText(
                    notification.title,
                    style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                )
                BasicText(
                    notification.description,
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            BasicText(
                notification.timeAgo,
                style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
            )
        }
    }
}

// Renamed to avoid conflict with new ProfileScreen from ProfileScreen.kt
@Composable
private fun OldProfileScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    profile: FullProfileResponse? = null,
    isLoading: Boolean = false,
    error: String? = null,
    onRefresh: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Loading state
        if (isLoading && profile == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
            return@Column
        }

        // Error state
        error?.let { errorMsg ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText(errorMsg, style = TextStyle(Color.Red, 14.sp))
                    Spacer(Modifier.height(8.dp))
                    LiquidButton(onClick = onRefresh, backdrop = backdrop) {
                        BasicText("Retry", style = TextStyle(contentColor, 14.sp))
                    }
                }
            }
            return@Column
        }

        val user = profile?.user ?: return@Column
        val stats = profile.stats

        // Banner Image
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            if (!user.bannerImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.bannerImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Default gradient banner
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1e3a5f),
                                    Color(0xFF2d5a87),
                                    Color(0xFF1e3a5f)
                                )
                            )
                        )
                )
            }
        }

        // Profile Card overlapping banner
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .offset(y = (-50).dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(24f.dp) },
                        effects = {
                            vibrancy()
                            blur(16f.dp.toPx())
                            lens(8f.dp.toPx(), 16f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.15f))
                        }
                    )
                    .padding(20.dp)
            ) {
                Column {
                    // Profile Avatar and Name
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        // Avatar with ring
                        Box(
                            Modifier
                                .size(100.dp)
                                .offset(y = (-30).dp)
                        ) {
                            val hasRing = !user.profileRing.isNullOrEmpty()
                            Box(
                                Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (hasRing) Modifier.background(
                                            brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                                colors = listOf(
                                                    Color(0xFFdd8448),
                                                    Color(0xFFf59e0b),
                                                    Color(0xFFdd8448),
                                                    Color(0xFFb45309),
                                                    Color(0xFFdd8448)
                                                )
                                            )
                                        )
                                        else Modifier.background(Color.White)
                                    )
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!user.avatar.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(user.avatar)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Profile picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(accentColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val initials = user.name.split(" ")
                                            .mapNotNull { it.firstOrNull()?.uppercase() }
                                            .take(2)
                                            .joinToString("")
                                        BasicText(
                                            initials.ifEmpty { "?" },
                                            style = TextStyle(Color.White, 32.sp, FontWeight.Bold)
                                        )
                                    }
                                }
                            }

                            VerificationBadge(
                                verified = user.hasVerificationBadge(),
                                badgeStyle = user.verificationBadgeStyle(),
                                modifier = Modifier.align(Alignment.BottomEnd),
                                size = VerificationBadgeSize.Large
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        // Name, headline, location
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText(
                                    user.name,
                                    style = TextStyle(contentColor, 22.sp, FontWeight.Bold)
                                )
                                if (user.isOpenToOpportunities) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF10b981).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        BasicText(
                                            "Open to work",
                                            style = TextStyle(Color(0xFF10b981), 10.sp, FontWeight.Medium)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            user.headline?.let { headline ->
                                BasicText(
                                    headline,
                                    style = TextStyle(contentColor.copy(alpha = 0.8f), 14.sp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // Location + College
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                user.location?.let { loc ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BasicText("📍 ", style = TextStyle(fontSize = 12.sp))
                                        BasicText(loc, style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                                    }
                                }
                                user.college?.let { college ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BasicText("🎓 ", style = TextStyle(fontSize = 12.sp))
                                        BasicText(college, style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // XP Level Bar
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                "Level ${stats.level}",
                                style = TextStyle(accentColor, 14.sp, FontWeight.SemiBold)
                            )
                            BasicText(
                                "${stats.xp} / ${stats.xp + stats.xpToNextLevel} XP",
                                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Gray.copy(alpha = 0.2f))
                        ) {
                            val progress = stats.xp.toFloat() / (stats.xp + stats.xpToNextLevel)
                            Box(
                                Modifier
                                    .fillMaxWidth(progress)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(accentColor, Color(0xFF60a5fa))
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Stats Row with Public Streak Badge
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        ProfileStat("Connections", stats.connectionsCount, contentColor)

                        // Prominent Public Streak Badge (Duolingo Effect)
                        PublicStreakBadge(
                            currentStreak = stats.currentStreak,
                            longestStreak = stats.longestStreak,
                            contentColor = contentColor
                        )

                        ProfileStat("Followers", stats.followersCount, contentColor)
                    }
                }
            }
        }

        // Content sections
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .offset(y = (-40).dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // About section
            user.bio?.let { bio ->
                ProfileSection(
                    title = "About",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    BasicText(
                        bio,
                        style = TextStyle(contentColor.copy(alpha = 0.9f), 14.sp, lineHeight = 20.sp)
                    )
                }
            }

            // Skills section
            if (profile.skills.isNotEmpty()) {
                ProfileSection(
                    title = "Skills (${profile.skills.size})",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profile.skills.forEach { skill ->
                            SkillChip(skill.skill.name, skill.proficiency, contentColor)
                        }
                    }
                }
            }

            // Experience section
            if (profile.experiences.isNotEmpty()) {
                ProfileSection(
                    title = "Experience",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        profile.experiences.forEach { exp ->
                            ExperienceItem(exp, contentColor)
                        }
                    }
                }
            }

            // Education section
            if (profile.education.isNotEmpty()) {
                ProfileSection(
                    title = "Education",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        profile.education.forEach { edu ->
                            EducationItem(edu, contentColor)
                        }
                    }
                }
            }

            // Projects section
            if (profile.projects.isNotEmpty()) {
                ProfileSection(
                    title = "Projects (${profile.projects.size})",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        profile.projects.take(3).forEach { project ->
                            ProjectItem(project, contentColor, accentColor)
                        }
                    }
                }
            }

            // Achievements section
            if (profile.achievements.isNotEmpty()) {
                ProfileSection(
                    title = "Achievements",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        profile.achievements.forEach { achievement ->
                            AchievementItem(achievement, contentColor)
                        }
                    }
                }
            }

            // Certificates section
            if (profile.certificates.isNotEmpty()) {
                ProfileSection(
                    title = "Certificates",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        profile.certificates.forEach { cert ->
                            CertificateItem(cert, contentColor)
                        }
                    }
                }
            }

            // Logout button
            LiquidButton(
                onClick = onLogout,
                backdrop = backdrop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                tint = Color(0xFFe53935)
            ) {
                BasicText(
                    "Logout",
                    style = TextStyle(Color.White, 15.sp, FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: Int, contentColor: Color, emoji: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            emoji?.let {
                BasicText(it, style = TextStyle(fontSize = 14.sp))
                Spacer(Modifier.width(4.dp))
            }
            BasicText(
                formatNumber(value),
                style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
            )
        }
        BasicText(
            label,
            style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(20f.dp) },
                effects = {
                    vibrancy()
                    blur(12f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.12f))
                }
            )
            .padding(16.dp)
    ) {
        Column {
            BasicText(
                title,
                style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SkillChip(name: String, proficiency: String?, contentColor: Color) {
    val bgColor = when (proficiency) {
        "Expert" -> Color(0xFF10b981).copy(alpha = 0.15f)
        "Advanced" -> Color(0xFF3b82f6).copy(alpha = 0.15f)
        "Intermediate" -> Color(0xFFf59e0b).copy(alpha = 0.15f)
        else -> Color.Gray.copy(alpha = 0.15f)
    }
    val textColor = when (proficiency) {
        "Expert" -> Color(0xFF10b981)
        "Advanced" -> Color(0xFF3b82f6)
        "Intermediate" -> Color(0xFFf59e0b)
        else -> contentColor.copy(alpha = 0.8f)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        BasicText(name, style = TextStyle(textColor, 13.sp, FontWeight.Medium))
    }
}

@Composable
private fun ExperienceItem(exp: com.kyant.backdrop.catalog.network.models.Experience, contentColor: Color) {
    Row {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            BasicText("💼", style = TextStyle(fontSize = 24.sp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(exp.title, style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold))
            BasicText(exp.company, style = TextStyle(contentColor.copy(alpha = 0.8f), 13.sp))
            Row {
                exp.type?.let {
                    BasicText(it, style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                    BasicText(" • ", style = TextStyle(contentColor.copy(alpha = 0.4f), 12.sp))
                }
                BasicText(
                    if (exp.isCurrent) "Present" else exp.endDate?.take(7) ?: "",
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                )
            }
        }
    }
}

@Composable
private fun EducationItem(edu: com.kyant.backdrop.catalog.network.models.Education, contentColor: Color) {
    Row {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            BasicText("🎓", style = TextStyle(fontSize = 24.sp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(edu.school, style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold))
            BasicText(
                "${edu.degree}${edu.fieldOfStudy?.let { " in $it" } ?: ""}",
                style = TextStyle(contentColor.copy(alpha = 0.8f), 13.sp)
            )
            edu.grade?.let {
                BasicText("Grade: $it", style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
            }
        }
    }
}

@Composable
private fun ProjectItem(project: com.kyant.backdrop.catalog.network.models.Project, contentColor: Color, accentColor: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (project.featured) {
                BasicText("⭐ ", style = TextStyle(fontSize = 14.sp))
            }
            BasicText(project.name, style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold))
        }
        project.description?.let { desc ->
            Spacer(Modifier.height(4.dp))
            BasicText(
                desc,
                style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (project.techStack.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                project.techStack.take(4).forEach { tech ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText(tech, style = TextStyle(accentColor, 11.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementItem(achievement: com.kyant.backdrop.catalog.network.models.Achievement, contentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val emoji = when (achievement.type) {
            "Hackathon" -> "🏆"
            "Competition" -> "🥇"
            "Award" -> "🏅"
            "Scholarship" -> "📚"
            else -> "✨"
        }
        BasicText(emoji, style = TextStyle(fontSize = 24.sp))
        Spacer(Modifier.width(12.dp))
        Column {
            BasicText(achievement.title, style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold))
            BasicText(
                "${achievement.organization} • ${achievement.date.take(4)}",
                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
            )
        }
    }
}

@Composable
private fun CertificateItem(cert: com.kyant.backdrop.catalog.network.models.Certificate, contentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText("📜", style = TextStyle(fontSize = 24.sp))
        Spacer(Modifier.width(12.dp))
        Column {
            BasicText(cert.name, style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold))
            BasicText(
                "${cert.issuingOrg} • ${cert.issueDate.take(7)}",
                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
            )
        }
    }
}

private fun formatNumber(num: Int): String {
    return when {
        num >= 1000000 -> "${(num / 1000000.0).let { if (it == it.toLong().toDouble()) it.toLong() else String.format("%.1f", it) }}M"
        num >= 1000 -> "${(num / 1000.0).let { if (it == it.toLong().toDouble()) it.toLong() else String.format("%.1f", it) }}K"
        else -> num.toString()
    }
}

@Composable
private fun ProfileStatCard(
    label: String,
    value: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(20f.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.1f))
                }
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                value,
                style = TextStyle(contentColor, 20.sp, FontWeight.Bold)
            )
            BasicText(
                label,
                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
            )
        }
    }
}

@Composable
private fun JobCard(
    job: Job,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24f.dp) },
                effects = {
                    vibrancy()
                    blur(14f.dp.toPx())
                    lens(6f.dp.toPx(), 12f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.12f))
                }
            )
            .clickable { }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("🏢", style = TextStyle(fontSize = 24.sp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        job.title,
                        style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                    )
                    BasicText(
                        job.company,
                        style = TextStyle(contentColor.copy(alpha = 0.8f), 13.sp)
                    )
                    BasicText(
                        job.location,
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    job.salary?.let {
                        BasicText(
                            it,
                            style = TextStyle(Color(0xFF00A86B), 13.sp, FontWeight.Medium)
                        )
                    }
                    BasicText(
                        "Posted ${job.postedAgo}",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                    )
                }

                if (job.isEasyApply) {
                    LiquidButton(
                        onClick = { },
                        backdrop = backdrop,
                        modifier = Modifier.height(36.dp),
                        tint = accentColor
                    ) {
                        BasicText(
                            "Easy Apply",
                            Modifier.padding(horizontal = 12.dp),
                            style = TextStyle(Color.White, 12.sp, FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLoading: Boolean,
    isGoogleLoading: Boolean,
    error: String?,
    savedAccountsCount: Int,
    onLogin: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onOpenSavedAccounts: () -> Unit,
    onClearError: () -> Unit
) {
    LiquidGlassLoginScreen(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isLoading = isLoading,
        isGoogleLoading = isGoogleLoading,
        error = error,
        savedAccountsCount = savedAccountsCount,
        onLogin = onLogin,
        onGoogleSignIn = onGoogleSignIn,
        onForgotPassword = onForgotPassword,
        onSignUpClick = onSignUpClick,
        onOpenSavedAccounts = onOpenSavedAccounts,
        onClearError = onClearError
    )
}

@Composable
private fun SignUpScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLoading: Boolean,
    isGoogleLoading: Boolean,
    error: String?,
    savedAccountsCount: Int,
    onSignUp: (email: String, password: String, name: String, username: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onLoginClick: () -> Unit,
    onOpenSavedAccounts: () -> Unit,
    onClearError: () -> Unit
) {
    LiquidGlassSignUpScreen(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isLoading = isLoading,
        isGoogleLoading = isGoogleLoading,
        error = error,
        savedAccountsCount = savedAccountsCount,
        onSignUp = onSignUp,
        onGoogleSignIn = onGoogleSignIn,
        onLoginClick = onLoginClick,
        onOpenSavedAccounts = onOpenSavedAccounts,
        onClearError = onClearError
    )
}

@Composable
private fun EmailVerificationScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    email: String,
    isLoading: Boolean,
    error: String?,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onLoginClick: () -> Unit,
    onClearError: () -> Unit
) {
    LiquidGlassEmailVerificationScreen(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        email = email,
        isLoading = isLoading,
        error = error,
        onVerify = onVerify,
        onResend = onResend,
        onLoginClick = onLoginClick,
        onClearError = onClearError
    )
}

@Composable
private fun EmailVerificationSheet(
    contentColor: Color,
    accentColor: Color,
    email: String,
    isLoading: Boolean,
    isSuccess: Boolean,
    error: String?,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onLoginClick: () -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
    onSuccessAnimationFinished: () -> Unit
) {
    LiquidGlassEmailVerificationSheet(
        contentColor = contentColor,
        accentColor = accentColor,
        email = email,
        isLoading = isLoading,
        isSuccess = isSuccess,
        error = error,
        onVerify = onVerify,
        onResend = onResend,
        onLoginClick = onLoginClick,
        onDismiss = onDismiss,
        onClearError = onClearError,
        onSuccessAnimationFinished = onSuccessAnimationFinished
    )
}

@Composable
private fun SavedPostDetailOverlay(
    post: Post?,
    isLoading: Boolean,
    error: String?,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    glassBackgroundKey: String,
    reduceAnimations: Boolean,
    onNavigateBack: () -> Unit,
    onLike: (String) -> Unit,
    onComment: (String) -> Unit,
    onShare: (String) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onProfileClick: (String) -> Unit,
    onMenuAction: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        SettingsHeader(
            title = "Saved Post",
            contentColor = contentColor,
            onBack = onNavigateBack
        )

        when {
            isLoading && post == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            }

            post == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        error ?: "Post unavailable",
                        style = TextStyle(contentColor.copy(alpha = 0.72f), 14.sp)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ApiPostCard(
                            post = post,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            glassBackgroundKey = glassBackgroundKey,
                            onLike = onLike,
                            onComment = onComment,
                            onShare = onShare,
                            onVotePoll = onVotePoll,
                            onProfileClick = { onProfileClick(post.author.id) },
                            onMentionClick = { username -> onProfileClick(username) },
                            onMenuAction = onMenuAction,
                            reduceAnimations = reduceAnimations
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

internal fun Author.displayName(): String =
    name?.takeIf { it.isNotBlank() }
        ?: username?.takeIf { it.isNotBlank() }
        ?: "User"

private fun authorInitials(author: Author): String =
    author.displayName()
        .split(Regex("\\s+"))
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifEmpty { "U" }

@Composable
internal fun PostAuthorAvatarStack(
    authors: List<Author>,
    accentColor: Color,
    surfaceColor: Color,
    imageCrossfadeMs: Int
) {
    val visibleAuthors = authors.take(2).ifEmpty { listOf(Author(id = "", name = "User")) }
    Box(
        modifier = Modifier
            .width(if (visibleAuthors.size > 1) 68.dp else 44.dp)
            .height(44.dp)
    ) {
        visibleAuthors.forEachIndexed { index, author ->
            val authorName = author.displayName()
            val profileImageUrl = author.profileImage
            Box(
                Modifier
                    .offset(x = (index * 24).dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                accentColor,
                                accentColor.copy(alpha = 0.5f),
                                Color(0xFF16A34A),
                                accentColor
                            )
                        )
                    )
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(surfaceColor),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profileImageUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profileImageUrl)
                                    .crossfade(imageCrossfadeMs)
                                    .build(),
                                contentDescription = "Profile picture of $authorName",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            BasicText(
                                authorInitials(author),
                                style = TextStyle(Color.White, 14.sp, FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
