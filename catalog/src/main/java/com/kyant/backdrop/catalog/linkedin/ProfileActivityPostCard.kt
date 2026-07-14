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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Icon
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
import com.kyant.backdrop.catalog.chat.ChatViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun ApiPostCard(
    post: Post,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    glassBackgroundKey: String = DefaultGlassBackgroundPresetKey,
    onLike: (String) -> Unit,
    onComment: (String) -> Unit = {},
    onShare: (String) -> Unit = {},
    onVotePoll: (String, String) -> Unit = { _, _ -> },
    onProfileClick: () -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    onMenuAction: (String, String) -> Unit = { _, _ -> },
    reduceAnimations: Boolean = false,
    playDefaultVideos: Boolean = !reduceAnimations
) {
    var showMenu by remember { mutableStateOf(false) }
    val menuAnchorBoundsHolder = remember(post.id) { MenuAnchorBoundsHolder() }
    var menuAnchorBounds by remember(post.id) { mutableStateOf<Rect?>(null) }
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var showFullScreenVideo by remember { mutableStateOf(false) }
    var displayIsLiked by remember(post.id) { mutableStateOf(post.isLiked) }
    var displayLikesCount by remember(post.id) { mutableIntStateOf(post.likesCount) }
    var displayIsSaved by remember(post.id) { mutableStateOf(post.isSaved) }
    var likeEffectTrigger by remember(post.id) { mutableIntStateOf(0) }
    var saveEffectTrigger by remember(post.id) { mutableIntStateOf(0) }
    var isLikePending by remember(post.id) { mutableStateOf(false) }
    var isSavePending by remember(post.id) { mutableStateOf(false) }

    // Mention preview state
    var showMentionPreview by remember { mutableStateOf(false) }
    var mentionUsername by remember { mutableStateOf("") }
    val context = LocalContext.current
    val relativeTimeLabel by rememberRelativeTimeLabel(post.createdAt)

    LaunchedEffect(post.id, post.isLiked, post.likesCount, post.isSaved) {
        displayIsLiked = post.isLiked
        displayLikesCount = post.likesCount
        displayIsSaved = post.isSaved
        isLikePending = false
        isSavePending = false
    }

    // Red color for active likes
    val appearance = currentVormexAppearance()
    val likeActiveColor = Color(0xFFE53935)
    val useCrystalPureGlass = appearance.isGlassTheme && glassBackgroundKey == "crystal"
    val containerShape = RoundedCornerShape(0.dp)
    val innerSectionShape = RoundedCornerShape(0.dp)
    val subtleTextColor = appearance.mutedContentColor
    val hasDefaultVideo = findDefaultPostVideo(post.defaultVideoId) != null
    val hasMedia = !post.videoUrl.isNullOrEmpty() || post.mediaUrls.isNotEmpty() || hasDefaultVideo
    val hasLinkPreview = !post.linkUrl.isNullOrEmpty()
    val showContentBlock = !post.content.isNullOrBlank()
    val cardSurfaceColor = when {
        !appearance.isGlassTheme -> appearance.cardColor
        useCrystalPureGlass -> Color.White.copy(alpha = 0.08f)
        else -> Color.White.copy(alpha = 0.12f)
    }
    val cardBorderColor = when {
        !appearance.isGlassTheme -> appearance.cardBorderColor
        useCrystalPureGlass -> Color.White.copy(alpha = 0.22f)
        else -> Color.White.copy(alpha = 0.16f)
    }
    val mediaContainerColor =
        if (appearance.isGlassTheme) Color.Black.copy(alpha = 0.08f) else appearance.mediaSurfaceColor
    val metricChipColor =
        if (appearance.isGlassTheme) Color.White.copy(alpha = 0.08f) else appearance.subtleColor
    val privateMetricChipColor =
        if (appearance.isGlassTheme) Color.White.copy(alpha = 0.06f) else appearance.subtleColor
    val imageCrossfadeMs = if (reduceAnimations) 0 else 300

    Box(
        Modifier
            .fillMaxWidth()
            // Bottom hairline divider only — no card background
            .drawWithContent {
                drawContent()
                val strokeWidth = 0.5.dp.toPx()
                val c = cardBorderColor
                drawLine(
                    color = c,
                    start = Offset(0f, size.height - strokeWidth / 2f),
                    end = Offset(size.width, size.height - strokeWidth / 2f),
                    strokeWidth = strokeWidth
                )
            }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            // Author info with menu — premium header layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                val collabAuthors = remember(post.author, post.collaborators) {
                    (listOf(post.author) + post.collaborators)
                        .distinctBy { it.id }
                        .filter { it.id.isNotBlank() }
                }
                val isCollaborativePost = collabAuthors.size > 1
                val authorName = if (isCollaborativePost) {
                    val visibleNames = collabAuthors.take(2).joinToString(" & ") { collaborator ->
                        collaborator.displayName()
                    }
                    if (collabAuthors.size > 2) {
                        "$visibleNames +${collabAuthors.size - 2}"
                    } else {
                        visibleNames
                    }
                } else {
                    post.author.displayName()
                }
                val showAuthorVerified = if (isCollaborativePost) {
                    collabAuthors.any { it.hasVerificationBadge() }
                } else {
                    post.author.hasVerificationBadge()
                }
                val authorBadgeStyle = if (isCollaborativePost) {
                    collabAuthors.firstNotNullOfOrNull { it.verificationBadgeStyle() }
                } else {
                    post.author.verificationBadgeStyle()
                }

                // Clickable author section (avatar + name)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .noRippleClickable { onProfileClick() },
                    verticalAlignment = Alignment.Top
                ) {
                    PostAuthorAvatarStack(
                        authors = collabAuthors.ifEmpty { listOf(post.author) },
                        accentColor = accentColor,
                        surfaceColor = cardSurfaceColor,
                        imageCrossfadeMs = imageCrossfadeMs
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Name + dot + time on one line
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BasicText(
                                authorName,
                                style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            VerificationBadge(
                                verified = showAuthorVerified,
                                badgeStyle = authorBadgeStyle,
                                size = VerificationBadgeSize.Small
                            )
                            BasicText(
                                "·",
                                style = TextStyle(subtleTextColor, 13.sp)
                            )
                            BasicText(
                                relativeTimeLabel,
                                style = TextStyle(subtleTextColor, 12.sp)
                            )
                        }
                        // Headline as secondary text
                        val headline = if (isCollaborativePost) {
                            "Collaboration"
                        } else {
                            post.author.headline
                        }
                        headline?.let { headlineText ->
                            BasicText(
                                headlineText,
                                style = TextStyle(subtleTextColor, 12.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isCollaborativePost) {
                            ApiMetricChip(
                                label = "Collab",
                                contentColor = Color(0xFF16A34A),
                                containerColor = Color(0xFF16A34A).copy(alpha = 0.12f),
                                borderColor = Color(0xFF16A34A).copy(alpha = 0.2f)
                            )
                        }
                        // Visibility badge (only if not public)
                        when (post.visibility) {
                            "CONNECTIONS" -> ApiMetricChip(
                                label = "Connections",
                                contentColor = accentColor,
                                containerColor = accentColor.copy(alpha = 0.12f),
                                borderColor = accentColor.copy(alpha = 0.2f)
                            )
                            "PRIVATE" -> ApiMetricChip(
                                label = "Only me",
                                contentColor = subtleTextColor,
                                containerColor = privateMetricChipColor
                            )
                        }
                    }
                }

                // Menu button
                Box(
                    Modifier
                        .onGloballyPositioned { coordinates ->
                            menuAnchorBoundsHolder.coordinates = coordinates
                        }
                        .clip(CircleShape)
                        .clickable {
                            menuAnchorBounds = menuAnchorBoundsHolder.currentBounds()
                            showMenu = true
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MenuDotsIcon(
                        color = contentColor.copy(alpha = 0.6f),
                        size = 18.dp
                    )
                }
            }

            // Glass-themed dropdown menu
            if (showMenu) {
                GlassDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    backdrop = backdrop,
                    contentColor = contentColor,
                    anchorBounds = menuAnchorBounds,
                    useGlassBackdropEffects = false
                ) {
                    GlassMenuItem(
                        onClick = {
                            showMenu = false
                            if (!isSavePending) {
                                if (!displayIsSaved) saveEffectTrigger++
                                displayIsSaved = !displayIsSaved
                                isSavePending = true
                                onMenuAction(post.id, "save")
                            }
                        },
                        contentColor = contentColor,
                        leadingIcon = {
                            BookmarkIcon(
                                if (displayIsSaved) accentColor else contentColor,
                                size = 18.dp,
                                filled = displayIsSaved
                            )
                        },
                        text = if (displayIsSaved) "Unsave" else "Save"
                    )
                    GlassMenuItem(
                        onClick = {
                            showMenu = false
                            onMenuAction(post.id, "copy_link")
                        },
                        contentColor = contentColor,
                        leadingIcon = { LinkIcon(contentColor, size = 18.dp) },
                        text = "Copy Link"
                    )
                    GlassMenuDivider(contentColor)
                    GlassMenuItem(
                        onClick = {
                            showMenu = false
                            onMenuAction(post.id, "not_interested")
                        },
                        contentColor = contentColor,
                        leadingIcon = { BlockIcon(contentColor, size = 18.dp) },
                        text = "Not Interested"
                    )
                    GlassMenuItem(
                        onClick = {
                            showMenu = false
                            onMenuAction(post.id, "report")
                        },
                        contentColor = contentColor,
                        leadingIcon = { WarningIcon(Color.Red.copy(alpha = 0.8f), size = 18.dp) },
                        text = "Report",
                        textColor = Color.Red.copy(alpha = 0.8f)
                    )
                }
            }

            val normalizedPostType = post.type.uppercase()
            if (normalizedPostType == "ARTICLE" || !post.articleTitle.isNullOrBlank()) {
                ApiArticleCard(
                    articleTitle = post.articleTitle.orEmpty(),
                    articleCoverImage = post.articleCoverImage,
                    articleTags = post.articleTags,
                    articleReadTime = post.articleReadTime,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    backdrop = backdrop,
                    reduceAnimations = reduceAnimations
                )
            }

            val celebrationGifShown =
                normalizedPostType == "CELEBRATION" &&
                    (!post.celebrationGifUrl.isNullOrBlank() || post.mediaUrls.isNotEmpty())
            if (normalizedPostType == "CELEBRATION" || !post.celebrationType.isNullOrBlank()) {
                ApiCelebrationHero(
                    celebrationType = post.celebrationType,
                    celebrationGifUrl = post.celebrationGifUrl ?: post.mediaUrls.firstOrNull(),
                    celebrationBadge = post.celebrationBadge,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    backdrop = backdrop,
                    reduceAnimations = reduceAnimations
                )
            }

            // Post content with mention support
            if (showContentBlock) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FormattedContent(
                        content = post.content.orEmpty(),
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onMentionClick = { username -> onMentionClick(username) },
                        onMentionLongPress = { username ->
                            mentionUsername = username
                            showMentionPreview = true
                        }
                    )
                }
            }

            // Media: Video or Image
            val isVideoPost = normalizedPostType == "VIDEO" || !post.videoUrl.isNullOrEmpty()

            if (isVideoPost && !post.videoUrl.isNullOrEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                ) {
                    VideoPlayer(
                        videoUrl = post.videoUrl,
                        modifier = Modifier.fillMaxWidth(),
                        autoPlay = false,
                        showControls = true,
                        contentColor = contentColor,
                        onFullScreenClick = { showFullScreenVideo = true }
                    )
                }
            } else if (post.mediaUrls.isNotEmpty() && normalizedPostType != "ARTICLE" && !celebrationGifShown) {
                Box(
                    Modifier
                        .fillMaxWidth()
                ) {
                    ApiImagePostGrid(
                        images = post.mediaUrls,
                        reduceAnimations = reduceAnimations,
                        onImageClick = { index ->
                            selectedImageIndex = index
                            showImageViewer = true
                        }
                    )
                }
            }

            if (hasDefaultVideo) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(innerSectionShape)
                ) {
                    DefaultPostVideoPlayer(
                        defaultVideoId = post.defaultVideoId,
                        modifier = Modifier.fillMaxWidth(),
                        reduceAnimations = !playDefaultVideos,
                        accentColor = accentColor,
                        height = null,
                        contentScale = ContentScale.FillWidth
                    )
                }
            }

            if (!post.linkUrl.isNullOrEmpty()) {
                Box(Modifier.padding(horizontal = 12.dp, vertical = if (hasMedia) 14.dp else 0.dp)) {
                    ApiLinkPreview(
                        url = post.linkUrl,
                        title = post.linkTitle,
                        description = post.linkDescription,
                        domain = post.linkDomain,
                        linkImage = post.linkImage,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        reduceAnimations = reduceAnimations,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(post.linkUrl))
                                )
                            }
                        }
                    )
                }
            } else if (hasMedia) {
                Spacer(Modifier.height(14.dp))
            }

            if ((normalizedPostType == "POLL" || post.pollOptions.isNotEmpty()) && post.pollOptions.isNotEmpty()) {
                Box(Modifier.padding(horizontal = 12.dp, vertical = if (!hasLinkPreview) 0.dp else 2.dp)) {
                    ApiPollContent(
                        options = post.pollOptions,
                        endsAt = post.pollEndsAt,
                        userVotedOptionId = post.userVotedOptionId,
                        showResultsBeforeVote = post.showResultsBeforeVote,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        reduceAnimations = reduceAnimations,
                        onVote = { optionId -> onVotePoll(post.id, optionId) }
                    )
                }
            }

            // Thin divider before actions
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(0.5.dp)
                    .background(contentColor.copy(alpha = 0.08f))
            )

            // Instagram-style action row: Like/Comment/Share left, Save right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Like, Comment, Share with inline counts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like button with count
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .noRippleClickable {
                                if (!isLikePending) {
                                    val nextLiked = !displayIsLiked
                                    if (nextLiked) likeEffectTrigger++
                                    displayIsLiked = nextLiked
                                    displayLikesCount = if (nextLiked) displayLikesCount + 1 else (displayLikesCount - 1).coerceAtLeast(0)
                                    isLikePending = true
                                    onLike(post.id)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        LikeIcon(
                            color = if (displayIsLiked) likeActiveColor else contentColor.copy(alpha = 0.65f),
                            size = 20.dp,
                            filled = displayIsLiked
                        )
                        if (displayLikesCount > 0) {
                            BasicText(
                                "$displayLikesCount",
                                style = TextStyle(
                                    color = if (displayIsLiked) likeActiveColor else contentColor.copy(alpha = 0.65f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Comment button with count
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .noRippleClickable { onComment(post.id) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        CommentIcon(contentColor.copy(alpha = 0.65f), size = 20.dp)
                        if (post.commentsCount > 0) {
                            BasicText(
                                "${post.commentsCount}",
                                style = TextStyle(
                                    contentColor.copy(alpha = 0.65f),
                                    13.sp,
                                    FontWeight.Medium
                                )
                            )
                        }
                    }

                    // Share button with count
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .noRippleClickable { onShare(post.id) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        ShareIcon(contentColor.copy(alpha = 0.65f), size = 20.dp)
                        if (post.sharesCount > 0) {
                            BasicText(
                                "${post.sharesCount}",
                                style = TextStyle(
                                    contentColor.copy(alpha = 0.65f),
                                    13.sp,
                                    FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                // Right: Save/Bookmark
                Box(
                    modifier = Modifier
                        .size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .noRippleClickable {
                                if (!isSavePending) {
                                    if (!displayIsSaved) saveEffectTrigger++
                                    displayIsSaved = !displayIsSaved
                                    isSavePending = true
                                    onMenuAction(post.id, "save")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BookmarkIcon(
                            if (displayIsSaved) accentColor else contentColor.copy(alpha = 0.65f),
                            size = 20.dp,
                            filled = displayIsSaved
                        )
                    }
                    SaveLottieEffect(
                        trigger = saveEffectTrigger,
                        modifier = Modifier.requiredSize(82.dp)
                    )
                }
            }
        }

        LikeHeartsEffect(
            trigger = likeEffectTrigger,
            modifier = Modifier
                .align(Alignment.Center)
                .requiredSize(260.dp)
        )
    }

    // Full screen image viewer dialog
    if (showImageViewer && post.mediaUrls.isNotEmpty()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            com.kyant.backdrop.catalog.linkedin.posts.FullScreenImageViewer(
                images = post.mediaUrls,
                initialIndex = selectedImageIndex,
                onDismiss = { showImageViewer = false }
            )
        }
    }

    // Full screen video player dialog
    if (showFullScreenVideo && !post.videoUrl.isNullOrEmpty()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullScreenVideo = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            FullScreenVideoPlayer(
                videoUrl = post.videoUrl,
                onDismiss = { showFullScreenVideo = false }
            )
        }
    }

    // Mention profile preview popup (glass theme with animation)
    if (showMentionPreview && mentionUsername.isNotEmpty()) {
        MentionProfilePreviewPopup(
            username = mentionUsername,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            onDismiss = { showMentionPreview = false },
            onViewProfile = {
                showMentionPreview = false
                onMentionClick(mentionUsername)
            }
        )
    }
}
@Composable
private fun ApiActionButton(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier
            .noRippleClickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun ApiMetricChip(
    icon: (@Composable () -> Unit)? = null,
    label: String,
    contentColor: Color,
    containerColor: Color = Color.White.copy(alpha = 0.08f),
    borderColor: Color = Color.White.copy(alpha = 0.12f)
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.invoke()
        BasicText(
            label,
            style = TextStyle(contentColor.copy(alpha = 0.78f), 11.sp, FontWeight.Medium)
        )
    }
}

private fun celebrationTypeLabel(raw: String?): String {
    if (raw.isNullOrBlank()) return "Celebration"
    val match = CelebrationType.entries.find { it.name == raw }
    return match?.label
        ?: raw.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
}

@Composable
private fun ApiArticleCard(
    articleTitle: String,
    articleCoverImage: String?,
    articleTags: List<String>,
    articleReadTime: Int?,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    reduceAnimations: Boolean = false
) {
    val context = LocalContext.current
    val cardModifier = Modifier
        .padding(horizontal = 0.dp, vertical = 8.dp)
        .fillMaxWidth()

    Column(modifier = cardModifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
        ) {
            if (!articleCoverImage.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(articleCoverImage)
                        .crossfade(if (reduceAnimations) 0 else 400)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.55f),
                                    accentColor.copy(alpha = 0.2f)
                                )
                            )
                        )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
                        )
                    )
            )
            BasicText(
                text = articleTitle,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                style = TextStyle(Color.White, 18.sp, FontWeight.Bold, lineHeight = 22.sp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val readLabel = articleReadTime?.takeIf { it > 0 }?.let { "$it min read" }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ApiMetricChip(
                    label = "Long-form article",
                    contentColor = accentColor,
                    containerColor = accentColor.copy(alpha = 0.14f),
                    borderColor = accentColor.copy(alpha = 0.22f)
                )
                readLabel?.let {
                    ApiMetricChip(
                        label = it,
                        contentColor = contentColor.copy(alpha = 0.78f),
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )
                }
            }
            if (articleTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    articleTags.take(6).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            BasicText(
                                text = "#$tag",
                                style = TextStyle(accentColor.copy(alpha = 0.95f), 11.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiCelebrationHero(
    celebrationType: String?,
    celebrationGifUrl: String?,
    celebrationBadge: String?,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    reduceAnimations: Boolean = false
) {
    val context = LocalContext.current
    val scale = if (reduceAnimations) {
        1f
    } else {
        val pulse = rememberInfiniteTransition(label = "celebratePulse")
        val pulseScale by pulse.animateFloat(
            initialValue = 0.985f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "celebrateScale"
        )
        pulseScale
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "CELEBRATION",
                    style = TextStyle(accentColor, 10.sp, FontWeight.Bold)
                )
                BasicText(
                    text = celebrationTypeLabel(celebrationType),
                    style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!celebrationBadge.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        text = celebrationBadge,
                        style = TextStyle(accentColor, 11.sp, FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (!celebrationGifUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(celebrationGifUrl)
                    .crossfade(if (reduceAnimations) 0 else 320)
                    .build(),
                contentDescription = "Celebration animation",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 240.dp)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun ApiLinkPreview(
    url: String?,
    title: String?,
    description: String?,
    domain: String?,
    linkImage: String?,
    contentColor: Color,
    accentColor: Color,
    reduceAnimations: Boolean = false,
    onClick: () -> Unit
) {
    if (url.isNullOrBlank()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        if (!linkImage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(linkImage)
                        .crossfade(if (reduceAnimations) 0 else 400)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f))
                            )
                        )
                )
            }
        }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicText(
                text = domain ?: "Open link",
                style = TextStyle(accentColor.copy(alpha = 0.9f), 10.sp, FontWeight.SemiBold)
            )
            BasicText(
                text = title ?: url,
                style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!description.isNullOrBlank()) {
                BasicText(
                    text = description,
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicText(
                text = "Tap to open in browser",
                style = TextStyle(contentColor.copy(alpha = 0.52f), 11.sp)
            )
        }
    }
}

/** Non-finite poll % crashes [Double.toInt] and Compose [animateFloatAsState] / [fillMaxWidth]. */
private fun sanitizePollPercentForUi(raw: Double): Double =
    when {
        raw.isNaN() || raw.isInfinite() -> 0.0
        else -> raw.coerceIn(0.0, 100.0)
    }

@Composable
private fun ApiPollOptionRow(
    option: PollOption,
    showResults: Boolean,
    percentage: Double,
    hasVoted: Boolean,
    isPollEnded: Boolean,
    isSelected: Boolean,
    contentColor: Color,
    accentColor: Color,
    onVote: () -> Unit
) {
    val safePct = sanitizePollPercentForUi(percentage)
    val targetFrac = (safePct / 100.0).toFloat().let { f ->
        if (f.isFinite()) f.coerceIn(0f, 1f) else 0f
    }
    val animatedFrac by animateFloatAsState(
        targetValue = if (showResults) targetFrac else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f),
        label = "pollBar"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(enabled = !hasVoted && !isPollEnded) { onVote() }
    ) {
        if (showResults) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFrac)
                    .height(50.dp)
                    .background(
                        if (isSelected) accentColor.copy(alpha = 0.24f)
                        else Color.White.copy(alpha = 0.14f)
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = option.text,
                style = TextStyle(
                    color = if (isSelected) accentColor else contentColor,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
            if (showResults) {
                BasicText(
                    text = "${safePct.roundToInt()}%",
                    style = TextStyle(contentColor.copy(alpha = 0.65f), 12.sp, FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
private fun ApiPollContent(
    options: List<PollOption>,
    endsAt: String?,
    userVotedOptionId: String?,
    showResultsBeforeVote: Boolean,
    contentColor: Color,
    accentColor: Color,
    reduceAnimations: Boolean = false,
    onVote: (String) -> Unit
) {
    val hasVoted = userVotedOptionId != null
    val showResults = hasVoted || showResultsBeforeVote
    val isPollEnded = isPollExpired(endsAt)
    val totalVotes = options.sumOf { it.votes }
    val isLive = !isPollEnded && !endsAt.isNullOrBlank()
    val liveAlpha = if (reduceAnimations || !isLive) {
        0.9f
    } else {
        val livePulse = rememberInfiniteTransition(label = "pollLive")
        val animatedLiveAlpha by livePulse.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "liveA"
        )
        animatedLiveAlpha
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = "Poll",
                style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold)
            )
            if (isLive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = liveAlpha))
                    )
                    BasicText(
                        text = "Live",
                        style = TextStyle(accentColor.copy(alpha = 0.9f), 11.sp, FontWeight.Medium)
                    )
                }
            }
        }

        options.forEach { option ->
            val rawPercentage = option.percentage.takeIf { showResults } ?: if (totalVotes > 0) {
                (option.votes.toDouble() / totalVotes.toDouble()) * 100.0
            } else {
                0.0
            }
            val percentage = sanitizePollPercentForUi(rawPercentage)
            val isSelected = option.id == userVotedOptionId
            ApiPollOptionRow(
                option = option,
                showResults = showResults,
                percentage = percentage,
                hasVoted = hasVoted,
                isPollEnded = isPollEnded,
                isSelected = isSelected,
                contentColor = contentColor,
                accentColor = accentColor,
                onVote = { onVote(option.id) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicText(
                text = "$totalVotes vote${if (totalVotes == 1) "" else "s"}",
                style = TextStyle(contentColor.copy(alpha = 0.55f), 11.sp)
            )
            BasicText(
                text = when {
                    isPollEnded -> "Poll ended"
                    !endsAt.isNullOrBlank() -> "Ends soon"
                    else -> ""
                },
                style = TextStyle(contentColor.copy(alpha = 0.55f), 11.sp)
            )
        }
    }
}

private fun isPollExpired(endsAt: String?): Boolean {
    if (endsAt.isNullOrBlank()) return false

    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )

    for (pattern in formats) {
        val parser = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val parsed = runCatching { parser.parse(endsAt) }.getOrNull()
        if (parsed != null) {
            return parsed.time < System.currentTimeMillis()
        }
    }

    return false
}

private fun formatTimeAgo(dateString: String): String {
    return try {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )

        var parsedDate: java.util.Date? = null
        for (pattern in patterns) {
            val parser = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            parsedDate = runCatching { parser.parse(dateString) }.getOrNull()
            if (parsedDate != null) break
        }

        val date = parsedDate ?: return dateString
        val diffMillis = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
        val seconds = diffMillis / 1000L
        val minutes = seconds / 60L
        val hours = minutes / 60L
        val days = hours / 24L
        val weeks = days / 7L

        when {
            seconds < 60L -> "Just now"
            minutes < 60L -> "${minutes}m"
            hours < 24L -> "${hours}h"
            days < 7L -> "${days}d"
            weeks < 4L -> "${weeks}w"
            else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.US).format(date)
        }
    } catch (e: Exception) {
        dateString
    }
}

@Composable
private fun rememberRelativeTimeLabel(dateString: String) = produceState(
    initialValue = formatTimeAgo(dateString),
    key1 = dateString
) {
    while (true) {
        value = formatTimeAgo(dateString)
        delay(
            when (value) {
                "Just now" -> 15_000L
                else -> 60_000L
            }
        )
    }
}

/**
 * Image grid for API posts - adapts layout based on image count
 */
@Composable
private fun ApiImagePostGrid(
    images: List<String>,
    reduceAnimations: Boolean = false,
    onImageClick: (Int) -> Unit
) {
    val crossfadeMs = if (reduceAnimations) 0 else 300
    val spacing = 3.dp
    val displayImages = images.take(9)
    val extraCount = (images.size - 9).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
    ) {
        when (images.size) {
            1 -> {
                // Single image: full card width, height = natural aspect ratio (original proportions, not a fixed 4:3).
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(images[0])
                        .crossfade(crossfadeMs)
                        .build(),
                    contentDescription = "Post image 1",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.Top)
                        .heightIn(min = 120.dp)
                        .clip(RoundedCornerShape(0.dp))
                        .background(Color.Black.copy(alpha = 0.06f))
                        .clickable { onImageClick(0) }
                )
            }
            2 -> {
                // 2 images side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    images.forEachIndexed { index, url ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(url)
                                .crossfade(crossfadeMs)
                                .build(),
                            contentDescription = "Post image ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable { onImageClick(index) }
                        )
                    }
                }
            }
            3 -> {
                // 3 images: large left, 2 stacked right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(images[0])
                            .crossfade(crossfadeMs)
                            .build(),
                        contentDescription = "Post image 1",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onImageClick(0) }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(images[1])
                                .crossfade(crossfadeMs)
                                .build(),
                            contentDescription = "Post image 2",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable { onImageClick(1) }
                        )
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(images[2])
                                .crossfade(crossfadeMs)
                                .build(),
                            contentDescription = "Post image 3",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable { onImageClick(2) }
                        )
                    }
                }
            }
            4 -> {
                // 2x2 grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        for (i in 0..1) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(images[i])
                                    .crossfade(crossfadeMs)
                                    .build(),
                                contentDescription = "Post image ${i + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { onImageClick(i) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        for (i in 2..3) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(images[i])
                                    .crossfade(crossfadeMs)
                                    .build(),
                                contentDescription = "Post image ${i + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { onImageClick(i) }
                            )
                        }
                    }
                }
            }
            else -> {
                // 5+ images: Big first image (2x2), rest in grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(displayImages[0])
                                .crossfade(crossfadeMs)
                                .build(),
                            contentDescription = "Post image 1",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(2f)
                                .aspectRatio(1f)
                                .clickable { onImageClick(0) }
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            if (displayImages.size > 1) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(displayImages[1])
                                        .crossfade(crossfadeMs)
                                        .build(),
                                    contentDescription = "Post image 2",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clickable { onImageClick(1) }
                                )
                            }
                            if (displayImages.size > 2) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(displayImages[2])
                                        .crossfade(crossfadeMs)
                                        .build(),
                                    contentDescription = "Post image 3",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clickable { onImageClick(2) }
                                )
                            }
                        }
                    }

                    if (displayImages.size > 3) {
                        val remainingImages = displayImages.drop(3)
                        remainingImages.chunked(3).forEachIndexed { rowIndex, rowImages ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                rowImages.forEachIndexed { colIndex, url ->
                                    val imageIndex = 3 + rowIndex * 3 + colIndex
                                    val isLastVisibleImage = imageIndex == 8 && extraCount > 0

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(url)
                                                .crossfade(crossfadeMs)
                                                .build(),
                                            contentDescription = "Post image ${imageIndex + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable { onImageClick(imageIndex) }
                                        )

                                        if (isLastVisibleImage) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                BasicText(
                                                    text = "+$extraCount",
                                                    style = TextStyle(
                                                        color = Color.White,
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                                repeat(3 - rowImages.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
