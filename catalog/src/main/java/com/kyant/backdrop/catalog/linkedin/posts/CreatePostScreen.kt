package com.kyant.backdrop.catalog.linkedin.posts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImagePainter
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.ai.VormexAiGateway
import com.kyant.backdrop.catalog.ai.VormexAiRewriteStyle
import com.kyant.backdrop.catalog.ai.VormexAiStatusCard
import com.kyant.backdrop.catalog.ai.VormexAiSurface
import com.kyant.backdrop.catalog.ai.VormexAiTextResult
import com.kyant.backdrop.catalog.linkedin.DefaultPostVideo
import com.kyant.backdrop.catalog.linkedin.DefaultPostVideoPlayer
import com.kyant.backdrop.catalog.linkedin.MentionSearchPolicy
import com.kyant.backdrop.catalog.linkedin.VerificationBadge
import com.kyant.backdrop.catalog.linkedin.VerificationBadgeSize
import com.kyant.backdrop.catalog.linkedin.currentVormexAppearance
import com.kyant.backdrop.catalog.linkedin.defaultPostVideos
import com.kyant.backdrop.catalog.linkedin.findDefaultPostVideo
import com.kyant.backdrop.catalog.linkedin.hasVerificationBadge
import com.kyant.backdrop.catalog.linkedin.verificationBadgeStyle
import com.kyant.backdrop.catalog.media.MediaReadSafety
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ==================== UI Styling Constants ====================
private val PostButtonGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0A66C2), Color(0xFF004182))
)

private const val CreatePostDraftPrefs = "vormex_create_post_draft"
private const val DraftHasDraft = "hasDraft"
private const val DraftPostType = "postType"
private const val DraftContent = "content"
private const val DraftVisibility = "visibility"
private const val DraftLinkUrl = "linkUrl"
private const val DraftPollOptions = "pollOptions"
private const val DraftPollDurationHours = "pollDurationHours"
private const val DraftShowResultsBeforeVote = "showResultsBeforeVote"
private const val DraftArticleTitle = "articleTitle"
private const val DraftArticleTags = "articleTags"
private const val DraftCelebrationType = "celebrationType"
private const val DraftDefaultVideoId = "defaultVideoId"
private const val DraftImageUris = "imageUris"
private const val DraftVideoUri = "videoUri"
private const val DraftArticleCoverUri = "articleCoverUri"
private const val DraftCelebrationGifUri = "celebrationGifUri"
private const val DraftRestoreOnOpen = "restoreOnOpen"

private fun postTypeSubtitle(type: PostType): String = when (type) {
    PostType.TEXT -> "Share a thought, update, or quick insight."
    PostType.IMAGE -> "Tell the story with a gallery and a strong caption."
    PostType.VIDEO -> "Drop a clip that gets people to stop scrolling."
    PostType.LINK -> "Recommend something worth opening."
    PostType.POLL -> "Ask a sharp question and collect answers."
    PostType.ARTICLE -> "Publish a longer take with depth and structure."
    PostType.CELEBRATION -> "Mark a win and bring people into the moment."
    else -> "Share something worth opening."
}

private fun postTypeSectionTitle(type: PostType): String = when (type) {
    PostType.IMAGE -> "Visuals"
    PostType.VIDEO -> "Video"
    PostType.LINK -> "Link Details"
    PostType.POLL -> "Poll Setup"
    PostType.ARTICLE -> "Article Details"
    PostType.CELEBRATION -> "Celebration"
    else -> "Content"
}

// ==================== Post Type Icons (using drawable resources) ====================
private data class PostTypeConfig(
    val type: PostType,
    val label: String,
    val icon: ImageVector
)

private val postTypeConfigs = listOf(
    PostTypeConfig(PostType.TEXT, "Text", Icons.AutoMirrored.Outlined.Subject),
    PostTypeConfig(PostType.IMAGE, "Image", Icons.Outlined.Image),
    PostTypeConfig(PostType.VIDEO, "Video", Icons.Outlined.Videocam),
    PostTypeConfig(PostType.LINK, "Link", Icons.Outlined.Link),
    PostTypeConfig(PostType.POLL, "Poll", Icons.Outlined.Poll),
    PostTypeConfig(PostType.ARTICLE, "Article", Icons.Outlined.Description),
    PostTypeConfig(PostType.CELEBRATION, "Celebrate", Icons.Outlined.EmojiEvents)
)

private data class VisibilityOption(
    val value: String,
    val label: String,
    val detail: String,
    val icon: ImageVector
)

private enum class PostMentionMode {
    TAG,
    COLLAB
}

private data class SelectedPostMention(
    val user: MentionUser,
    val mode: PostMentionMode = PostMentionMode.TAG
)

private val visibilityOptions = listOf(
    VisibilityOption("PUBLIC", "Anyone", "Visible to everyone", Icons.Outlined.Public),
    VisibilityOption("CONNECTIONS", "Connections", "Only your network", Icons.Outlined.Groups),
    VisibilityOption("PRIVATE", "Only me", "Keep this private", Icons.Outlined.Lock)
)

private fun normalizePostVisibility(value: String?): String {
    return when (value?.trim()?.uppercase()?.replace(" ", "_")) {
        "PUBLIC", "ANYONE", "EVERYONE" -> "PUBLIC"
        "CONNECTIONS", "CONNECTION", "NETWORK" -> "CONNECTIONS"
        "PRIVATE", "ONLY_ME", "ME" -> "PRIVATE"
        else -> "PUBLIC"
    }
}

private fun resolveProfileImageModel(value: String?): String? {
    val image = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return when {
        image.startsWith("http://", ignoreCase = true) ||
            image.startsWith("https://", ignoreCase = true) ||
            image.startsWith("content://", ignoreCase = true) ||
            image.startsWith("file://", ignoreCase = true) -> image
        image.startsWith("/") -> {
            val apiRoot = BuildConfig.API_BASE_URL.removeSuffix("/api").removeSuffix("/")
            "$apiRoot$image"
        }
        else -> image
    }
}

private fun mentionHandleFor(user: MentionUser): String {
    val username = user.username
        ?.trim()
        ?.removePrefix("@")
        ?.takeIf { it.isNotBlank() }
    if (username != null) return username

    return user.name
        ?.replace(Regex("[^A-Za-z0-9_]"), "")
        ?.takeIf { it.isNotBlank() }
        ?: "user"
}

private fun celebrationTypeIcon(type: CelebrationType): ImageVector = when (type) {
    CelebrationType.NEW_JOB -> Icons.Outlined.Work
    CelebrationType.PROMOTION -> Icons.AutoMirrored.Outlined.TrendingUp
    CelebrationType.GRADUATION -> Icons.Outlined.School
    CelebrationType.CERTIFICATION -> Icons.Outlined.Verified
    CelebrationType.WORK_ANNIVERSARY -> Icons.Outlined.StarOutline
    CelebrationType.BIRTHDAY -> Icons.Outlined.Cake
}

// ==================== Color Presets ====================
private val colorPresets = listOf(
    "#ef4444" to "Red",
    "#f97316" to "Orange",
    "#eab308" to "Yellow",
    "#22c55e" to "Green",
    "#14b8a6" to "Teal",
    "#3b82f6" to "Blue",
    "#a855f7" to "Purple",
    "#ec4899" to "Pink"
)

// ==================== Poll Duration Options ====================
private val pollDurations = listOf(
    1 to "1 hour",
    6 to "6 hours",
    12 to "12 hours",
    24 to "1 day",
    72 to "3 days",
    168 to "1 week"
)

private fun parseDraftPostType(value: String?): PostType {
    return runCatching {
        value?.let(PostType::valueOf)
    }.getOrNull() ?: PostType.TEXT
}

private fun parseDraftCelebrationType(value: String?): CelebrationType? {
    return runCatching {
        value?.takeIf { it.isNotBlank() }?.let(CelebrationType::valueOf)
    }.getOrNull()
}

private fun persistReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

private fun readDraftFile(
    context: Context,
    uri: Uri,
    fallbackFilename: String,
    maxBytes: Long = MediaReadSafety.MaxPostImageBytes,
    label: String = "Media"
): Pair<ByteArray, String>? {
    return runCatching {
        MediaReadSafety.readMediaBytes(context, uri, fallbackFilename, maxBytes, label)
    }.getOrNull()
}

private data class RestoredDraftMedia(
    val imageUris: List<Uri> = emptyList(),
    val imageBytes: List<Pair<ByteArray, String>> = emptyList(),
    val videoUri: Uri? = null,
    val videoBytes: Pair<ByteArray, String>? = null,
    val articleCoverUri: Uri? = null,
    val articleCoverBytes: Pair<ByteArray, String>? = null,
    val celebrationGifUri: Uri? = null,
    val celebrationGifBytes: Pair<ByteArray, String>? = null
)

private data class CreatePostDraftSnapshot(
    val selectedPostType: PostType,
    val content: String,
    val visibility: String,
    val linkUrl: String,
    val pollOptions: List<String>,
    val pollDurationHours: Int,
    val showResultsBeforeVote: Boolean,
    val articleTitle: String,
    val articleTags: List<String>,
    val selectedCelebrationType: CelebrationType?,
    val selectedDefaultVideoId: String?,
    val imageUris: List<Uri>,
    val videoUri: Uri?,
    val articleCoverUri: Uri?,
    val celebrationGifUri: Uri?
) {
    val hasDraftContent: Boolean =
        content.isNotBlank() ||
            linkUrl.isNotBlank() ||
            pollOptions.any { it.isNotBlank() } ||
            articleTitle.isNotBlank() ||
            articleTags.isNotEmpty() ||
            selectedCelebrationType != null ||
            selectedDefaultVideoId != null ||
            imageUris.isNotEmpty() ||
            videoUri != null ||
            articleCoverUri != null ||
            celebrationGifUri != null

    val hasRestorableComposerState: Boolean =
        hasDraftContent ||
            selectedPostType != PostType.TEXT ||
            normalizePostVisibility(visibility) != "PUBLIC" ||
            pollDurationHours != 24 ||
            showResultsBeforeVote

    fun preview(): String =
        createPostDraftPreview(
            content = content,
            articleTitle = articleTitle,
            linkUrl = linkUrl,
            celebrationType = selectedCelebrationType?.name,
            defaultVideoId = selectedDefaultVideoId,
            imageCount = imageUris.size,
            hasVideo = videoUri != null
        )
}

private fun createPostDraftPreview(
    content: String?,
    articleTitle: String?,
    linkUrl: String?,
    celebrationType: String?,
    defaultVideoId: String?,
    imageCount: Int,
    hasVideo: Boolean
): String {
    return listOf(
        content?.trim(),
        articleTitle?.trim(),
        linkUrl?.trim(),
        celebrationType
            ?.replace("_", " ")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() },
        defaultVideoId
            ?.takeIf { it.isNotBlank() }
            ?.let { findDefaultPostVideo(it)?.title },
        when {
            imageCount > 0 -> "$imageCount image${if (imageCount == 1) "" else "s"} attached"
            hasVideo -> "Video attached"
            else -> null
        }
    ).firstOrNull { !it.isNullOrBlank() } ?: "Saved composer draft"
}

private fun PostType.draftLabel(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

/**
 * Create Post Screen - Full-featured post creation with all 7 post types
 * Instagram/LinkedIn style with glass theme
 */
@Composable
fun CreatePostScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isCreating: Boolean,
    error: String?,
    userName: String,
    userUsername: String? = null,
    userAvatar: String?,
    mentionSearchResults: List<MentionUser>,
    isSearchingMentions: Boolean,
    onCreateTextPost: (content: String, visibility: String, mentions: List<String>, collaboratorIds: List<String>, defaultVideoId: String?) -> Unit,
    onCreateImagePost: (content: String?, visibility: String, images: List<Pair<ByteArray, String>>, mentions: List<String>, collaboratorIds: List<String>, defaultVideoId: String?) -> Unit,
    onCreateVideoPost: (content: String?, visibility: String, videoBytes: ByteArray, videoFilename: String, mentions: List<String>, collaboratorIds: List<String>, defaultVideoId: String?) -> Unit,
    onCreateLinkPost: (linkUrl: String, content: String?, visibility: String, mentions: List<String>, collaboratorIds: List<String>, defaultVideoId: String?) -> Unit,
    onCreatePollPost: (pollOptions: List<String>, pollDurationHours: Int, content: String?, visibility: String, showResultsBeforeVote: Boolean, mentions: List<String>, collaboratorIds: List<String>, defaultVideoId: String?) -> Unit,
    onCreateArticlePost: (articleTitle: String, content: String?, visibility: String, coverImage: Pair<ByteArray, String>?, articleTags: List<String>, mentions: List<String>, collaboratorIds: List<String>, defaultVideoId: String?) -> Unit,
    onCreateCelebrationPost: (celebrationType: String, content: String?, visibility: String, mentions: List<String>, collaboratorIds: List<String>, celebrationGif: Pair<ByteArray, String>?, defaultVideoId: String?) -> Unit,
    onSearchMentions: (String) -> Unit,
    onClearMentionSearch: () -> Unit,
    onClearError: () -> Unit,
    onPostCreated: () -> Unit
) {
    val context = LocalContext.current
    val appearance = currentVormexAppearance()
    val aiGateway = remember { VormexAiGateway(context.applicationContext) }
    val aiScope = rememberCoroutineScope()
    
    // State
    var selectedPostType by remember { mutableStateOf(PostType.TEXT) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var visibility by remember { mutableStateOf("PUBLIC") }
    var showVisibilityDropdown by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showPostAiMenu by remember { mutableStateOf(false) }
    var showDefaultVideoPicker by remember { mutableStateOf(false) }
    var aiStatus by remember { mutableStateOf<String?>(null) }
    var aiBusyLabel by remember { mutableStateOf<String?>(null) }
    var selectedDefaultVideoId by remember { mutableStateOf<String?>(null) }
    val draftPrefs = remember(context) {
        context.getSharedPreferences(CreatePostDraftPrefs, Context.MODE_PRIVATE)
    }
    var hasSavedDraft by remember { mutableStateOf(false) }
    var draftStatus by remember { mutableStateOf<String?>(null) }
    var savedDraftType by remember { mutableStateOf<PostType?>(null) }
    var savedDraftPreview by remember { mutableStateOf<String?>(null) }
    var isEditingSavedDraft by remember { mutableStateOf(false) }
    var wasCreatingPost by remember { mutableStateOf(false) }
    val postSubmissionStartedState = remember { mutableStateOf(false) }
    var postSubmissionStarted by postSubmissionStartedState
    var refreshedCurrentUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        ApiClient.getCurrentUser(context)
            .onSuccess { user -> refreshedCurrentUser = user }
    }
    
    // Image state
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imageBytes by remember { mutableStateOf<List<Pair<ByteArray, String>>>(emptyList()) }
    
    // Video state
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoBytes by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    
    // Link state
    var linkUrl by remember { mutableStateOf("") }
    
    // Poll state
    var pollOptions by remember { mutableStateOf(listOf("", "")) }
    var pollDurationHours by remember { mutableIntStateOf(24) }
    var showResultsBeforeVote by remember { mutableStateOf(false) }
    
    // Article state
    var articleTitle by remember { mutableStateOf("") }
    var articleTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var articleTagInput by remember { mutableStateOf("") }
    var articleCoverUri by remember { mutableStateOf<Uri?>(null) }
    var articleCoverBytes by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    
    // Celebration state
    var selectedCelebrationType by remember { mutableStateOf<CelebrationType?>(null) }
    var celebrationGifUri by remember { mutableStateOf<Uri?>(null) }
    var celebrationGifBytes by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    
    // Mention state
    var selectedMentions by remember { mutableStateOf<List<SelectedPostMention>>(emptyList()) }
    var showMentionDropdown by remember { mutableStateOf(false) }
    var activeMention by remember { mutableStateOf<MentionSearchPolicy.ActiveMention?>(null) }
    val composerUserName = listOf(
        refreshedCurrentUser?.name,
        refreshedCurrentUser?.username,
        userUsername,
        refreshedCurrentUser?.email?.substringBefore("@"),
        userName
    ).firstOrNull { !it.isNullOrBlank() } ?: "Your profile"
    val composerUsername = refreshedCurrentUser?.username ?: userUsername
    val composerAvatar = refreshedCurrentUser?.profileImage ?: userAvatar

    fun draftPreviewFromPrefs(): String {
        val imageCount = draftPrefs.getString(DraftImageUris, "").orEmpty()
            .split('\n')
            .count { it.isNotBlank() }
        return createPostDraftPreview(
            content = draftPrefs.getString(DraftContent, ""),
            articleTitle = draftPrefs.getString(DraftArticleTitle, ""),
            linkUrl = draftPrefs.getString(DraftLinkUrl, ""),
            celebrationType = draftPrefs.getString(DraftCelebrationType, ""),
            defaultVideoId = draftPrefs.getString(DraftDefaultVideoId, ""),
            imageCount = imageCount,
            hasVideo = draftPrefs.getString(DraftVideoUri, "").orEmpty().isNotBlank()
        )
    }

    fun refreshDraftSummaryFromPrefs() {
        hasSavedDraft = draftPrefs.getBoolean(DraftHasDraft, false)
        savedDraftType = if (hasSavedDraft) {
            parseDraftPostType(draftPrefs.getString(DraftPostType, null))
        } else {
            null
        }
        savedDraftPreview = if (hasSavedDraft) draftPreviewFromPrefs() else null
    }

    fun resetComposerForNewPost() {
        selectedPostType = PostType.TEXT
        textFieldValue = TextFieldValue("")
        visibility = "PUBLIC"
        showVisibilityDropdown = false
        showColorPicker = false
        showPostAiMenu = false
        showDefaultVideoPicker = false
        aiStatus = null
        aiBusyLabel = null
        selectedDefaultVideoId = null
        imageUris = emptyList()
        imageBytes = emptyList()
        videoUri = null
        videoBytes = null
        linkUrl = ""
        pollOptions = listOf("", "")
        pollDurationHours = 24
        showResultsBeforeVote = false
        articleTitle = ""
        articleTags = emptyList()
        articleTagInput = ""
        articleCoverUri = null
        articleCoverBytes = null
        selectedCelebrationType = null
        celebrationGifUri = null
        celebrationGifBytes = null
        selectedMentions = emptyList()
        showMentionDropdown = false
        activeMention = null
        postSubmissionStarted = false
        isEditingSavedDraft = false
        onClearMentionSearch()
    }

    fun currentDraftSnapshot(): CreatePostDraftSnapshot =
        CreatePostDraftSnapshot(
            selectedPostType = selectedPostType,
            content = textFieldValue.text,
            visibility = normalizePostVisibility(visibility),
            linkUrl = linkUrl,
            pollOptions = pollOptions,
            pollDurationHours = pollDurationHours,
            showResultsBeforeVote = showResultsBeforeVote,
            articleTitle = articleTitle,
            articleTags = articleTags,
            selectedCelebrationType = selectedCelebrationType,
            selectedDefaultVideoId = selectedDefaultVideoId,
            imageUris = imageUris,
            videoUri = videoUri,
            articleCoverUri = articleCoverUri,
            celebrationGifUri = celebrationGifUri
        )

    fun persistDraftSnapshot(
        snapshot: CreatePostDraftSnapshot,
        restoreOnOpen: Boolean,
        updateSummaryState: Boolean
    ) {
        draftPrefs.edit()
            .putBoolean(DraftHasDraft, true)
            .putBoolean(DraftRestoreOnOpen, restoreOnOpen)
            .putString(DraftPostType, snapshot.selectedPostType.name)
            .putString(DraftContent, snapshot.content)
            .putString(DraftVisibility, normalizePostVisibility(snapshot.visibility))
            .putString(DraftLinkUrl, snapshot.linkUrl)
            .putString(DraftPollOptions, snapshot.pollOptions.joinToString("\n"))
            .putInt(DraftPollDurationHours, snapshot.pollDurationHours)
            .putBoolean(DraftShowResultsBeforeVote, snapshot.showResultsBeforeVote)
            .putString(DraftArticleTitle, snapshot.articleTitle)
            .putString(DraftArticleTags, snapshot.articleTags.joinToString("\n"))
            .putString(DraftCelebrationType, snapshot.selectedCelebrationType?.name.orEmpty())
            .putString(DraftDefaultVideoId, snapshot.selectedDefaultVideoId.orEmpty())
            .putString(DraftImageUris, snapshot.imageUris.joinToString("\n") { it.toString() })
            .putString(DraftVideoUri, snapshot.videoUri?.toString().orEmpty())
            .putString(DraftArticleCoverUri, snapshot.articleCoverUri?.toString().orEmpty())
            .putString(DraftCelebrationGifUri, snapshot.celebrationGifUri?.toString().orEmpty())
            .apply()

        if (updateSummaryState) {
            hasSavedDraft = true
            savedDraftType = snapshot.selectedPostType
            savedDraftPreview = snapshot.preview()
        }
    }

    fun saveDraft(showToast: Boolean = true) {
        val snapshot = currentDraftSnapshot()
        persistDraftSnapshot(
            snapshot = snapshot,
            restoreOnOpen = false,
            updateSummaryState = true
        )
        resetComposerForNewPost()
        draftStatus = "Draft saved. Start a new post when you're ready."
        if (showToast) {
            Toast.makeText(context, "Draft saved. You can start another post now.", Toast.LENGTH_LONG).show()
        }
    }

    fun clearDraft(showToast: Boolean = true) {
        draftPrefs.edit().clear().apply()
        hasSavedDraft = false
        savedDraftType = null
        savedDraftPreview = null
        isEditingSavedDraft = false
        draftStatus = null
        if (showToast) {
            Toast.makeText(context, "Draft cleared", Toast.LENGTH_SHORT).show()
        }
    }

    fun restoreSavedDraft() {
        if (!draftPrefs.getBoolean(DraftHasDraft, false)) return

        aiScope.launch {
            selectedPostType = parseDraftPostType(draftPrefs.getString(DraftPostType, null))
            textFieldValue = TextFieldValue(
                text = draftPrefs.getString(DraftContent, "").orEmpty(),
                selection = TextRange(draftPrefs.getString(DraftContent, "").orEmpty().length)
            )
            visibility = normalizePostVisibility(draftPrefs.getString(DraftVisibility, "PUBLIC"))
            linkUrl = draftPrefs.getString(DraftLinkUrl, "").orEmpty()
            pollOptions = draftPrefs.getString(DraftPollOptions, "").orEmpty()
                .split('\n')
                .filter { it.isNotBlank() }
                .takeIf { it.size >= 2 }
                ?: listOf("", "")
            pollDurationHours = draftPrefs.getInt(DraftPollDurationHours, 24)
            showResultsBeforeVote = draftPrefs.getBoolean(DraftShowResultsBeforeVote, false)
            articleTitle = draftPrefs.getString(DraftArticleTitle, "").orEmpty()
            articleTags = draftPrefs.getString(DraftArticleTags, "").orEmpty()
                .split('\n')
                .filter { it.isNotBlank() }
            selectedCelebrationType = parseDraftCelebrationType(draftPrefs.getString(DraftCelebrationType, null))
            selectedDefaultVideoId = draftPrefs.getString(DraftDefaultVideoId, "").orEmpty()
                .takeIf { it.isNotBlank() }
            val restoredMedia = withContext(Dispatchers.IO) {
                val restoredImageFiles = draftPrefs.getString(DraftImageUris, "").orEmpty()
                    .split('\n')
                    .filter { it.isNotBlank() }
                    .map(Uri::parse)
                    .mapNotNull { uri ->
                        readDraftFile(
                            context = context,
                            uri = uri,
                            fallbackFilename = "image.jpg",
                            maxBytes = MediaReadSafety.MaxPostImageBytes,
                            label = "Image"
                        )?.let { uri to it }
                    }
                    .take(10)
                val restoredVideoUri = draftPrefs.getString(DraftVideoUri, "").orEmpty()
                    .takeIf { it.isNotBlank() }
                    ?.let(Uri::parse)
                val restoredVideoBytes = restoredVideoUri?.let {
                    readDraftFile(
                        context = context,
                        uri = it,
                        fallbackFilename = "video.mp4",
                        maxBytes = MediaReadSafety.MaxPostVideoBytes,
                        label = "Video"
                    )
                }
                val restoredArticleCoverUri = draftPrefs.getString(DraftArticleCoverUri, "").orEmpty()
                    .takeIf { it.isNotBlank() }
                    ?.let(Uri::parse)
                val restoredArticleCoverBytes =
                    restoredArticleCoverUri?.let {
                        readDraftFile(
                            context = context,
                            uri = it,
                            fallbackFilename = "cover.jpg",
                            maxBytes = MediaReadSafety.MaxPostImageBytes,
                            label = "Cover image"
                        )
                    }
                val restoredCelebrationGifUri = draftPrefs.getString(DraftCelebrationGifUri, "").orEmpty()
                    .takeIf { it.isNotBlank() }
                    ?.let(Uri::parse)
                val restoredCelebrationGifBytes =
                    restoredCelebrationGifUri?.let {
                        readDraftFile(
                            context = context,
                            uri = it,
                            fallbackFilename = "celebration.gif",
                            maxBytes = MediaReadSafety.MaxPostImageBytes,
                            label = "Celebration image"
                        )
                    }

                RestoredDraftMedia(
                    imageUris = restoredImageFiles.map { it.first },
                    imageBytes = restoredImageFiles.map { it.second },
                    videoUri = restoredVideoUri.takeIf { restoredVideoBytes != null },
                    videoBytes = restoredVideoBytes,
                    articleCoverUri = restoredArticleCoverUri.takeIf { restoredArticleCoverBytes != null },
                    articleCoverBytes = restoredArticleCoverBytes,
                    celebrationGifUri = restoredCelebrationGifUri.takeIf { restoredCelebrationGifBytes != null },
                    celebrationGifBytes = restoredCelebrationGifBytes
                )
            }
            imageUris = restoredMedia.imageUris
            imageBytes = restoredMedia.imageBytes
            videoUri = restoredMedia.videoUri
            videoBytes = restoredMedia.videoBytes
            articleCoverUri = restoredMedia.articleCoverUri
            articleCoverBytes = restoredMedia.articleCoverBytes
            celebrationGifUri = restoredMedia.celebrationGifUri
            celebrationGifBytes = restoredMedia.celebrationGifBytes
            isEditingSavedDraft = true
            draftStatus = "Draft resumed. Publish it or save it again when you're ready."
            draftPrefs.edit().putBoolean(DraftRestoreOnOpen, false).apply()
            refreshDraftSummaryFromPrefs()
        }
    }

    LaunchedEffect(Unit) {
        refreshDraftSummaryFromPrefs()
        if (draftPrefs.getBoolean(DraftRestoreOnOpen, false)) {
            restoreSavedDraft()
        }
    }

    val autoSaveCurrentDraft by rememberUpdatedState {
        if (!postSubmissionStartedState.value) {
            val snapshot = currentDraftSnapshot()
            if (snapshot.hasRestorableComposerState) {
                persistDraftSnapshot(
                    snapshot = snapshot,
                    restoreOnOpen = true,
                    updateSummaryState = false
                )
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            autoSaveCurrentDraft()
        }
    }
    
    // File pickers
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val pickedUris = uris.take(10)
            aiScope.launch {
                val newBytes = withContext(Dispatchers.IO) {
                    pickedUris.mapNotNull { uri ->
                        persistReadPermission(context, uri)
                        readDraftFile(
                            context = context,
                            uri = uri,
                            fallbackFilename = "image.jpg",
                            maxBytes = MediaReadSafety.MaxPostImageBytes,
                            label = "Image"
                        )
                    }
                }
                imageUris = pickedUris
                imageBytes = newBytes
            }
        }
    }
    
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            aiScope.launch {
                val file = withContext(Dispatchers.IO) {
                    persistReadPermission(context, it)
                    readDraftFile(
                        context = context,
                        uri = it,
                        fallbackFilename = "video.mp4",
                        maxBytes = MediaReadSafety.MaxPostVideoBytes,
                        label = "Video"
                    )
                }
                file?.let { restoredFile ->
                    videoUri = it
                    videoBytes = restoredFile
                }
            }
        }
    }
    
    val articleCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            aiScope.launch {
                val file = withContext(Dispatchers.IO) {
                    persistReadPermission(context, it)
                    readDraftFile(
                        context = context,
                        uri = it,
                        fallbackFilename = "cover.jpg",
                        maxBytes = MediaReadSafety.MaxPostImageBytes,
                        label = "Cover image"
                    )
                }
                file?.let { restoredFile ->
                    articleCoverUri = it
                    articleCoverBytes = restoredFile
                }
            }
        }
    }

    val celebrationGifPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            aiScope.launch {
                val file = withContext(Dispatchers.IO) {
                    persistReadPermission(context, it)
                    readDraftFile(
                        context = context,
                        uri = it,
                        fallbackFilename = "celebration.gif",
                        maxBytes = MediaReadSafety.MaxPostImageBytes,
                        label = "Celebration image"
                    )
                }
                file?.let { restoredFile ->
                    celebrationGifUri = it
                    celebrationGifBytes = restoredFile
                }
            }
        }
    }
    
    // Detect @mention in content
    LaunchedEffect(textFieldValue.text, textFieldValue.selection) {
        val text = textFieldValue.text
        val cursorPos = textFieldValue.selection.start
        if (text.isBlank()) {
            showPostAiMenu = false
        }

        val mention = MentionSearchPolicy.findActiveMention(text, cursorPos)
        activeMention = mention
        if (mention != null) {
            showMentionDropdown = true
            val query = MentionSearchPolicy.normalize(mention.query)
            if (MentionSearchPolicy.shouldSearch(query)) {
                onSearchMentions(query)
            } else {
                onClearMentionSearch()
            }
        } else {
            showMentionDropdown = false
            onClearMentionSearch()
        }
    }

    fun markComposerSubmitted() {
        postSubmissionStarted = true
        if (isEditingSavedDraft) {
            clearDraft(showToast = false)
        }
    }
    
    // Handle create post
    val handleCreatePost: () -> Unit = {
        val content = textFieldValue.text
        val postVisibility = normalizePostVisibility(visibility)
        val tagMentionIds = selectedMentions
            .filter { it.mode == PostMentionMode.TAG }
            .map { it.user.id }
            .distinct()
        val collaboratorIds = selectedMentions
            .filter { it.mode == PostMentionMode.COLLAB }
            .map { it.user.id }
            .distinct()
        when (selectedPostType) {
            PostType.TEXT -> {
                if (content.isNotBlank()) {
                    markComposerSubmitted()
                    onCreateTextPost(content, postVisibility, tagMentionIds, collaboratorIds, selectedDefaultVideoId)
                }
            }
            PostType.IMAGE -> {
                if (imageBytes.isNotEmpty()) {
                    markComposerSubmitted()
                    onCreateImagePost(content.ifBlank { null }, postVisibility, imageBytes, tagMentionIds, collaboratorIds, selectedDefaultVideoId)
                }
            }
            PostType.VIDEO -> {
                videoBytes?.let { (bytes, filename) ->
                    markComposerSubmitted()
                    onCreateVideoPost(content.ifBlank { null }, postVisibility, bytes, filename, tagMentionIds, collaboratorIds, selectedDefaultVideoId)
                }
            }
            PostType.LINK -> {
                if (linkUrl.isNotBlank()) {
                    markComposerSubmitted()
                    onCreateLinkPost(linkUrl, content.ifBlank { null }, postVisibility, tagMentionIds, collaboratorIds, selectedDefaultVideoId)
                }
            }
            PostType.POLL -> {
                val validOptions = pollOptions.filter { it.isNotBlank() }
                if (validOptions.size >= 2) {
                    markComposerSubmitted()
                    onCreatePollPost(validOptions, pollDurationHours, content.ifBlank { null }, postVisibility, showResultsBeforeVote, tagMentionIds, collaboratorIds, selectedDefaultVideoId)
                }
            }
            PostType.ARTICLE -> {
                if (articleTitle.isNotBlank()) {
                    markComposerSubmitted()
                    onCreateArticlePost(articleTitle, content.ifBlank { null }, postVisibility, articleCoverBytes, articleTags, tagMentionIds, collaboratorIds, selectedDefaultVideoId)
                }
            }
            PostType.CELEBRATION -> {
                selectedCelebrationType?.let { type ->
                    markComposerSubmitted()
                    onCreateCelebrationPost(
                        type.name,
                        content.ifBlank { null },
                        postVisibility,
                        tagMentionIds,
                        collaboratorIds,
                        celebrationGifBytes,
                        selectedDefaultVideoId
                    )
                }
            }
            else -> {}
        }
    }
    
    // Validation
    val canPost = when (selectedPostType) {
        PostType.TEXT -> textFieldValue.text.isNotBlank()
        PostType.IMAGE -> imageBytes.isNotEmpty()
        PostType.VIDEO -> videoBytes != null
        PostType.LINK -> linkUrl.isNotBlank()
        PostType.POLL -> pollOptions.filter { it.isNotBlank() }.size >= 2
        PostType.ARTICLE -> articleTitle.isNotBlank()
        PostType.CELEBRATION -> selectedCelebrationType != null
        else -> false
    }
    val canSaveDraft =
        textFieldValue.text.isNotBlank() ||
            linkUrl.isNotBlank() ||
            pollOptions.any { it.isNotBlank() } ||
            articleTitle.isNotBlank() ||
            articleTags.isNotEmpty() ||
            selectedCelebrationType != null ||
            selectedDefaultVideoId != null ||
            imageUris.isNotEmpty() ||
            videoUri != null ||
            articleCoverUri != null ||
            celebrationGifUri != null
    val savedDraftTypeLabel = savedDraftType?.draftLabel() ?: "Draft"
    val savedDraftPreviewText = savedDraftPreview ?: "Saved composer draft"

    val applyPostAiText: (String, String) -> Unit = { label, value ->
        textFieldValue = TextFieldValue(
            text = value,
            selection = TextRange(value.length)
        )
        aiStatus = label
    }

    fun runPostAi(label: String, block: suspend () -> VormexAiTextResult) {
        aiScope.launch {
            aiBusyLabel = "$label…"
            aiStatus = null
            when (val result = block()) {
                is VormexAiTextResult.Success -> {
                    val status = when (result.source) {
                        com.kyant.backdrop.catalog.ai.VormexAiSource.LOCAL -> "$label updated on-device."
                        com.kyant.backdrop.catalog.ai.VormexAiSource.CLOUD -> "$label updated with cloud AI."
                    }
                    applyPostAiText(status, result.text)
                }
                is VormexAiTextResult.NeedsDownload -> aiStatus = result.message
                is VormexAiTextResult.Blocked -> aiStatus = result.message
                is VormexAiTextResult.Failure -> aiStatus = result.message
            }
            aiBusyLabel = null
        }
    }
    
    LaunchedEffect(isCreating, error) {
        if (wasCreatingPost && !isCreating && error == null && isEditingSavedDraft) {
            clearDraft(showToast = false)
            resetComposerForNewPost()
        }
        wasCreatingPost = isCreating
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appearance.backgroundColor)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CreatePostHeader(
                selectedType = selectedPostType,
                contentColor = contentColor,
                accentColor = accentColor,
                isCreating = isCreating,
                canPost = canPost,
                canSaveDraft = canSaveDraft,
                hasSavedDraft = hasSavedDraft,
                onSaveDraft = { saveDraft() },
                onClearDraft = { clearDraft() },
                onClose = onPostCreated,
                onPost = handleCreatePost
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                UserInfoRow(
                    userName = composerUserName,
                    userUsername = composerUsername,
                    userAvatar = composerAvatar,
                    visibility = visibility,
                    showVisibilityDropdown = showVisibilityDropdown,
                    onVisibilityDropdownToggle = { showVisibilityDropdown = it },
                    onVisibilityChange = { visibility = normalizePostVisibility(it) },
                    contentColor = contentColor,
                    accentColor = accentColor
                )

                ContentTextArea(
                    textFieldValue = textFieldValue,
                    onTextFieldValueChange = { textFieldValue = it },
                    placeholder = getPlaceholder(selectedPostType),
                    contentColor = contentColor,
                    accentColor = accentColor,
                    mentionSearchResults = mentionSearchResults,
                    isSearchingMentions = isSearchingMentions,
                    showMentionDropdown = showMentionDropdown,
                    onMentionSelected = { user ->
                        val text = textFieldValue.text
                        val username = mentionHandleFor(user)
                        val mention = activeMention
                            ?: MentionSearchPolicy.findActiveMention(text, textFieldValue.selection.start)
                        if (mention != null) {
                            val startIndex = mention.start.coerceIn(0, text.length)
                            val endIndex = mention.end.coerceIn(startIndex, text.length)
                            val replacement = "@$username "
                            val newText = text.replaceRange(startIndex, endIndex, replacement)
                            textFieldValue = TextFieldValue(
                                text = newText,
                                selection = TextRange(startIndex + replacement.length)
                            )
                            selectedMentions = if (selectedMentions.any { it.user.id == user.id }) {
                                selectedMentions
                            } else {
                                selectedMentions + SelectedPostMention(user)
                            }
                        }
                        activeMention = null
                        showMentionDropdown = false
                        onClearMentionSearch()
                    },
                    onDismissMentionDropdown = {
                        showMentionDropdown = false
                        onClearMentionSearch()
                    },
                    showAiButton = textFieldValue.text.isNotBlank(),
                    aiMenuExpanded = showPostAiMenu,
                    aiBusy = aiBusyLabel != null,
                    onAiMenuExpandedChange = { showPostAiMenu = it },
                    onProfessionalRewrite = {
                        showPostAiMenu = false
                        runPostAi("Professional rewrite") {
                            aiGateway.rewrite(
                                text = textFieldValue.text,
                                style = VormexAiRewriteStyle.PROFESSIONAL,
                                surface = VormexAiSurface.POST,
                                allowCloudFallback = true
                            )
                        }
                    },
                    onShorterRewrite = {
                        showPostAiMenu = false
                        runPostAi("Shorter rewrite") {
                            aiGateway.rewrite(
                                text = textFieldValue.text,
                                style = VormexAiRewriteStyle.SHORTER,
                                surface = VormexAiSurface.POST,
                                allowCloudFallback = true
                            )
                        }
                    },
                    onClearerRewrite = {
                        showPostAiMenu = false
                        runPostAi("Clearer rewrite") {
                            aiGateway.rewrite(
                                text = textFieldValue.text,
                                style = VormexAiRewriteStyle.CLEARER,
                                surface = VormexAiSurface.POST,
                                allowCloudFallback = true
                            )
                        }
                    },
                    onProofread = {
                        showPostAiMenu = false
                        runPostAi("Proofread") {
                            aiGateway.proofread(
                                text = textFieldValue.text,
                                surface = VormexAiSurface.POST,
                                allowCloudFallback = true
                            )
                        }
                    },
                    backdrop = backdrop
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

                draftStatus?.let { status ->
                    VormexAiStatusCard(
                        message = status,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                }

                AnimatedVisibility(visible = hasSavedDraft) {
                    SavedDraftCard(
                        draftType = savedDraftTypeLabel,
                        preview = savedDraftPreviewText,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onResumeDraft = { restoreSavedDraft() },
                        onClearDraft = { clearDraft() }
                    )
                }

                AnimatedVisibility(visible = selectedMentions.isNotEmpty()) {
                    MentionModeSection(
                        mentions = selectedMentions,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onModeChange = { userId, mode ->
                            selectedMentions = selectedMentions.map { mention ->
                                if (mention.user.id == userId) mention.copy(mode = mode) else mention
                            }
                        },
                        onRemove = { userId ->
                            selectedMentions = selectedMentions.filterNot { it.user.id == userId }
                        }
                    )
                }

                CreatePostSection {
                    SectionCaption(
                        title = "Format",
                        subtitle = "Bold, italic, center, mention, and color controls."
                    )
                    RichTextToolbar(
                        textFieldValue = textFieldValue,
                        onTextFieldValueChange = { textFieldValue = it },
                        showColorPicker = showColorPicker,
                        onColorPickerToggle = { showColorPicker = it },
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                }

                AnimatedVisibility(visible = showColorPicker) {
                    CreatePostSection {
                        SectionCaption(
                            title = "Text Color",
                            subtitle = "Pick a highlight color for the selected text."
                        )
                        ColorPickerRow(
                            onColorSelected = { hex ->
                                val selection = textFieldValue.selection
                                val text = textFieldValue.text
                                if (selection.length > 0) {
                                    val selectedText = text.substring(selection.start, selection.end)
                                    val newText = text.substring(0, selection.start) +
                                        "[color:$hex]$selectedText[/color]" +
                                        text.substring(selection.end)
                                    textFieldValue = TextFieldValue(
                                        text = newText,
                                        selection = TextRange(selection.start + hex.length + 8 + selectedText.length)
                                    )
                                } else {
                                    val newText = text.substring(0, selection.start) +
                                        "[color:$hex][/color]" +
                                        text.substring(selection.start)
                                    textFieldValue = TextFieldValue(
                                        text = newText,
                                        selection = TextRange(selection.start + hex.length + 8)
                                    )
                                }
                                showColorPicker = false
                            },
                            contentColor = contentColor
                        )
                    }
                }

                CreatePostToolDock(
                    selectedType = selectedPostType,
                    textLength = textFieldValue.text.length,
                    hasDefaultVideo = selectedDefaultVideoId != null,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onText = { selectedPostType = PostType.TEXT },
                    onPickImages = {
                        selectedPostType = PostType.IMAGE
                        imagePicker.launch(arrayOf("image/*"))
                    },
                    onPickVideo = {
                        selectedPostType = PostType.VIDEO
                        videoPicker.launch(arrayOf("video/*"))
                    },
                    onDefaultVideo = {
                        showDefaultVideoPicker = true
                    },
                    onLink = { selectedPostType = PostType.LINK },
                    onPoll = { selectedPostType = PostType.POLL },
                    onArticle = { selectedPostType = PostType.ARTICLE },
                    onCelebration = { selectedPostType = PostType.CELEBRATION },
                    onMention = {
                        val text = textFieldValue.text
                        val cursor = textFieldValue.selection.start.coerceIn(0, text.length)
                        textFieldValue = TextFieldValue(
                            text = text.substring(0, cursor) + "@" + text.substring(cursor),
                            selection = TextRange(cursor + 1)
                        )
                        activeMention = MentionSearchPolicy.ActiveMention(cursor, cursor + 1, "")
                        showMentionDropdown = true
                        onClearMentionSearch()
                    },
                    onVoice = {
                        Toast.makeText(context, "Voice notes are coming soon", Toast.LENGTH_SHORT).show()
                    }
                )

                CreatePostSection {
                    SectionCaption(
                        title = "Default Videos",
                        subtitle = "Add a built-in Vormex motion video to this post."
                    )
                    DefaultVideoDropdown(
                        selectedVideoId = selectedDefaultVideoId,
                        expanded = showDefaultVideoPicker,
                        onExpandedChange = { showDefaultVideoPicker = it },
                        onVideoSelected = { videoId ->
                            selectedDefaultVideoId = if (selectedDefaultVideoId == videoId) null else videoId
                            showDefaultVideoPicker = false
                        },
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    if (selectedDefaultVideoId != null) {
                        ComposerActionChip(
                            icon = Icons.Outlined.Close,
                            label = "Remove default video",
                            enabled = true,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onClick = {
                                selectedDefaultVideoId = null
                                showDefaultVideoPicker = false
                            }
                        )
                    }
                }

                when (selectedPostType) {
                    PostType.IMAGE -> CreatePostSection {
                        SectionCaption(
                            title = postTypeSectionTitle(selectedPostType),
                            subtitle = "Add up to 10 images and arrange the story visually."
                        )
                        ImagePicker(
                            imageUris = imageUris,
                            onPickImages = { imagePicker.launch(arrayOf("image/*")) },
                            onRemoveImage = { index ->
                                imageUris = imageUris.filterIndexed { i, _ -> i != index }
                                imageBytes = imageBytes.filterIndexed { i, _ -> i != index }
                            },
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }
                    PostType.VIDEO -> CreatePostSection {
                        SectionCaption(
                            title = postTypeSectionTitle(selectedPostType),
                            subtitle = "Attach one strong clip and give it context."
                        )
                        VideoPicker(
                            videoUri = videoUri,
                            onPickVideo = { videoPicker.launch(arrayOf("video/*")) },
                            onRemoveVideo = {
                                videoUri = null
                                videoBytes = null
                            },
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }
                    PostType.LINK -> CreatePostSection {
                        SectionCaption(
                            title = postTypeSectionTitle(selectedPostType),
                            subtitle = "Paste the URL and add a little framing."
                        )
                        LinkInput(
                            linkUrl = linkUrl,
                            onLinkUrlChange = { linkUrl = it },
                            contentColor = contentColor
                        )
                    }
                    PostType.POLL -> CreatePostSection {
                        SectionCaption(
                            title = postTypeSectionTitle(selectedPostType),
                            subtitle = "Write a sharp question and keep the choices simple."
                        )
                        PollEditor(
                            options = pollOptions,
                            onOptionsChange = { pollOptions = it },
                            durationHours = pollDurationHours,
                            onDurationChange = { pollDurationHours = it },
                            showResultsBeforeVote = showResultsBeforeVote,
                            onShowResultsChange = { showResultsBeforeVote = it },
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }
                    PostType.ARTICLE -> CreatePostSection {
                        SectionCaption(
                            title = postTypeSectionTitle(selectedPostType),
                            subtitle = "Shape the title, cover, and tags before you publish."
                        )
                        ArticleEditor(
                            title = articleTitle,
                            onTitleChange = { articleTitle = it },
                            coverUri = articleCoverUri,
                            onPickCover = { articleCoverPicker.launch(arrayOf("image/*")) },
                            onRemoveCover = {
                                articleCoverUri = null
                                articleCoverBytes = null
                            },
                            tags = articleTags,
                            tagInput = articleTagInput,
                            onTagInputChange = { articleTagInput = it },
                            onAddTag = {
                                if (articleTagInput.isNotBlank() && articleTags.size < 5) {
                                    articleTags = articleTags + articleTagInput.trim()
                                    articleTagInput = ""
                                }
                            },
                            onRemoveTag = { tag -> articleTags = articleTags - tag },
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }
                    PostType.CELEBRATION -> CreatePostSection {
                        SectionCaption(
                            title = postTypeSectionTitle(selectedPostType),
                            subtitle = "Pick the win, then add an optional local GIF."
                        )
                        CelebrationPicker(
                            selectedType = selectedCelebrationType,
                            onTypeSelected = { selectedCelebrationType = it },
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                        CelebrationGifPicker(
                            gifUri = celebrationGifUri,
                            onPickGif = { celebrationGifPicker.launch(arrayOf("image/*")) },
                            onRemoveGif = {
                                celebrationGifUri = null
                                celebrationGifBytes = null
                            },
                            contentColor = contentColor,
                            accentColor = accentColor
                        )
                    }
                    else -> Unit
                }

                AnimatedVisibility(visible = error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFE53935).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFE53935).copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            text = error ?: "",
                            style = TextStyle(Color(0xFFE53935), 14.sp),
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onClearError() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Dismiss error",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(180.dp))
            }
        }
    }
}

@Composable
private fun CelebrationGifPicker(
    gifUri: Uri?,
    onPickGif: () -> Unit,
    onRemoveGif: () -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current
    val appearance = currentVormexAppearance()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            text = "GIF sticker (optional)",
            style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
        )
        if (gifUri == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accentColor.copy(alpha = 0.14f),
                                appearance.inputColor
                            )
                        )
                    )
                    .border(1.dp, appearance.inputBorderColor, RoundedCornerShape(20.dp))
                    .clickable(onClick = onPickGif),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "Pick GIF",
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                    BasicText(
                        text = "Choose a GIF from your gallery",
                        style = TextStyle(contentColor.copy(alpha = 0.76f), 13.sp, FontWeight.Medium)
                    )
                    BasicText(
                        text = "Animated GIF or still image",
                        style = TextStyle(contentColor.copy(alpha = 0.45f), 11.sp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(gifUri).crossfade(true).build(),
                    contentDescription = "Celebration GIF preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onRemoveGif),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Remove GIF",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==================== Subcomponents ====================

@Composable
private fun CreatePostHeader(
    selectedType: PostType,
    contentColor: Color,
    accentColor: Color,
    isCreating: Boolean,
    canPost: Boolean,
    canSaveDraft: Boolean,
    hasSavedDraft: Boolean,
    onSaveDraft: () -> Unit,
    onClearDraft: () -> Unit,
    onClose: () -> Unit,
    onPost: () -> Unit
) {
    val canInteract = !isCreating
    val appearance = currentVormexAppearance()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val draftActionLabel = if (hasSavedDraft && !canSaveDraft) "DRAFT SAVED" else "SAVE DRAFT"
            BasicText(
                text = draftActionLabel,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = canSaveDraft && canInteract, onClick = onSaveDraft)
                    .padding(horizontal = 2.dp, vertical = 6.dp),
                style = TextStyle(
                    color = if (canSaveDraft || hasSavedDraft) appearance.mutedContentColor else appearance.disabledContentColor,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            if (hasSavedDraft) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Clear draft",
                    tint = appearance.mutedContentColor,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canInteract, onClick = onClearDraft)
                        .padding(2.dp)
                )
            }

            PrimaryPostButton(
                isCreating = isCreating,
                enabled = canPost && canInteract,
                contentColor = contentColor,
                accentColor = accentColor,
                onPost = onPost
            )
        }
    }
}

@Composable
private fun VormexAiLogoMenu(
    expanded: Boolean,
    aiEnabled: Boolean,
    aiBusy: Boolean,
    contentColor: Color,
    accentColor: Color,
    onExpandedChange: (Boolean) -> Unit,
    onProfessionalRewrite: () -> Unit,
    onShorterRewrite: () -> Unit,
    onClearerRewrite: () -> Unit,
    onProofread: () -> Unit
) {
    val appearance = currentVormexAppearance()
    val canUseActions = aiEnabled && !aiBusy

    Box {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(appearance.controlColor)
                .border(1.dp, appearance.controlBorderColor, CircleShape)
                .clickable(enabled = !aiBusy) { onExpandedChange(true) }
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            if (aiBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = accentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.vormex_logo),
                    contentDescription = "vormex",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .background(appearance.sheetColor)
                .border(1.dp, appearance.sheetBorderColor, RoundedCornerShape(14.dp))
        ) {
            VormexAiMenuItem(
                title = "Professional",
                subtitle = "Make it polished",
                icon = Icons.Outlined.Work,
                contentColor = contentColor,
                accentColor = accentColor,
                enabled = canUseActions,
                onClick = onProfessionalRewrite
            )
            VormexAiMenuItem(
                title = "Shorter",
                subtitle = "Trim extra words",
                icon = Icons.AutoMirrored.Outlined.Subject,
                contentColor = contentColor,
                accentColor = accentColor,
                enabled = canUseActions,
                onClick = onShorterRewrite
            )
            VormexAiMenuItem(
                title = "Clearer",
                subtitle = "Improve readability",
                icon = Icons.Outlined.AutoAwesome,
                contentColor = contentColor,
                accentColor = accentColor,
                enabled = canUseActions,
                onClick = onClearerRewrite
            )
            VormexAiMenuItem(
                title = "Proofread",
                subtitle = "Fix grammar",
                icon = Icons.Outlined.Check,
                contentColor = contentColor,
                accentColor = accentColor,
                enabled = canUseActions,
                onClick = onProofread
            )
        }
    }
}

@Composable
private fun PrimaryPostButton(
    isCreating: Boolean,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onPost: () -> Unit
) {
    val appearance = currentVormexAppearance()
    val showActive = enabled || isCreating

    Box(
        modifier = Modifier
            .height(34.dp)
            .widthIn(min = 52.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (showActive) accentColor else appearance.controlColor,
                RoundedCornerShape(9.dp)
            )
            .border(1.dp, if (showActive) accentColor.copy(alpha = 0.40f) else appearance.controlBorderColor, RoundedCornerShape(9.dp))
            .clickable(enabled = enabled, onClick = onPost)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isCreating) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            BasicText(
                text = "Post",
                style = TextStyle(
                    color = if (showActive) Color.White else contentColor.copy(alpha = 0.46f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun CreatePostToolDock(
    selectedType: PostType,
    textLength: Int,
    hasDefaultVideo: Boolean,
    contentColor: Color,
    accentColor: Color,
    onText: () -> Unit,
    onPickImages: () -> Unit,
    onPickVideo: () -> Unit,
    onDefaultVideo: () -> Unit,
    onLink: () -> Unit,
    onPoll: () -> Unit,
    onArticle: () -> Unit,
    onCelebration: () -> Unit,
    onMention: () -> Unit,
    onVoice: () -> Unit
) {
    val appearance = currentVormexAppearance()

    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(appearance.navigationColor)
            .border(1.dp, appearance.navigationBorderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CreatePostDockButton(
                icon = Icons.AutoMirrored.Outlined.Subject,
                label = "Text post",
                selected = selectedType == PostType.TEXT,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = onText
            )
            CreatePostDockButton(
                icon = Icons.Outlined.Image,
                label = "Add image",
                selected = selectedType == PostType.IMAGE,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = onPickImages
            )
            CreatePostDockButton(
                icon = Icons.Outlined.Videocam,
                label = "Add video",
                selected = selectedType == PostType.VIDEO,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = onPickVideo
            )
            CreatePostDockButton(
                icon = Icons.Outlined.VideoLibrary,
                label = "Default video",
                selected = hasDefaultVideo,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = onDefaultVideo
            )
            CreatePostDockButton(
                icon = Icons.Outlined.Link,
                label = "Add link",
                selected = selectedType == PostType.LINK,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = onLink
            )
            CreatePostDockButton(
                icon = Icons.Outlined.Poll,
                label = "Create poll",
                selected = selectedType == PostType.POLL,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = onPoll
            )
            CreatePostDockButton(
                icon = Icons.Outlined.Description,
                label = "Write article",
                selected = selectedType == PostType.ARTICLE,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = onArticle
            )
            CreatePostDockButton(
                icon = Icons.Outlined.EmojiEvents,
                label = "Celebrate",
                selected = selectedType == PostType.CELEBRATION,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = onCelebration
            )
            CreatePostDockButton(
                icon = Icons.Outlined.AlternateEmail,
                label = "Mention",
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = onMention
            )
            CreatePostDockButton(
                icon = Icons.Outlined.Mic,
                label = "Voice note",
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = onVoice
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        BasicText(
            text = "$textLength / 800",
            style = TextStyle(
                color = appearance.mutedContentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun CreatePostDockButton(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val appearance = currentVormexAppearance()
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(shape)
            .background(if (selected) accentColor.copy(alpha = 0.16f) else appearance.controlColor)
            .border(
                1.dp,
                if (selected) accentColor.copy(alpha = 0.34f) else appearance.controlBorderColor,
                shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accentColor else contentColor.copy(alpha = 0.72f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ComposerActionChip(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val appearance = currentVormexAppearance()
    val labelColor = if (enabled) contentColor.copy(alpha = 0.82f) else appearance.disabledContentColor

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(appearance.controlColor)
            .border(1.dp, appearance.controlBorderColor, RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) accentColor else appearance.disabledContentColor,
            modifier = Modifier.size(15.dp)
        )
        BasicText(
            text = label,
            style = TextStyle(labelColor, 12.sp, FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CreatePostSection(
    horizontalPadding: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val appearance = currentVormexAppearance()
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (horizontalPadding) 0.dp else 0.dp,
                vertical = 2.dp
            )
            .clip(shape)
            .background(appearance.cardColor)
            .border(1.dp, appearance.cardBorderColor, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        content()
    }
}

@Composable
private fun MentionModeSection(
    mentions: List<SelectedPostMention>,
    contentColor: Color,
    accentColor: Color,
    onModeChange: (String, PostMentionMode) -> Unit,
    onRemove: (String) -> Unit
) {
    CreatePostSection {
        SectionCaption(
            title = "People",
            subtitle = "Choose Tag for a normal mention, or Collab Invite when you want them to collaborate."
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            mentions.forEach { selected ->
                MentionModeRow(
                    mention = selected,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onModeChange = { mode -> onModeChange(selected.user.id, mode) },
                    onRemove = { onRemove(selected.user.id) }
                )
            }
        }
    }
}

@Composable
private fun MentionModeRow(
    mention: SelectedPostMention,
    contentColor: Color,
    accentColor: Color,
    onModeChange: (PostMentionMode) -> Unit,
    onRemove: () -> Unit
) {
    val appearance = currentVormexAppearance()
    val handle = mentionHandleFor(mention.user)
    val displayName = mention.user.name?.takeIf { it.isNotBlank() } ?: "@$handle"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(appearance.inputColor)
            .border(1.dp, appearance.inputBorderColor, RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(accentColor, Color(0xFF22C55E), Color(0xFFF59E0B))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = displayName.firstOrNull()?.uppercase() ?: "@",
                style = TextStyle(Color.White, 13.sp, FontWeight.Bold)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            BasicText(
                text = displayName,
                style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                text = "@$handle",
                style = TextStyle(contentColor.copy(alpha = 0.56f), 11.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        MentionModePill(
            label = "Tag",
            selected = mention.mode == PostMentionMode.TAG,
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = { onModeChange(PostMentionMode.TAG) }
        )
        MentionModePill(
            label = "Collab",
            selected = mention.mode == PostMentionMode.COLLAB,
            contentColor = contentColor,
            accentColor = Color(0xFF16A34A),
            onClick = { onModeChange(PostMentionMode.COLLAB) }
        )

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove person",
                tint = contentColor.copy(alpha = 0.55f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MentionModePill(
    label: String,
    selected: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val appearance = currentVormexAppearance()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) accentColor.copy(alpha = 0.16f) else appearance.controlColor)
            .border(
                1.dp,
                if (selected) accentColor.copy(alpha = 0.48f) else appearance.controlBorderColor,
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = if (selected) accentColor else contentColor.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionCaption(
    title: String,
    subtitle: String
) {
    val appearance = currentVormexAppearance()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(
            text = title,
            style = TextStyle(appearance.contentColor.copy(alpha = 0.92f), 15.sp, FontWeight.SemiBold)
        )
        BasicText(
            text = subtitle,
            style = TextStyle(appearance.mutedContentColor, 12.sp, lineHeight = 18.sp)
        )
    }
}

@Composable
private fun SavedDraftCard(
    draftType: String,
    preview: String,
    contentColor: Color,
    accentColor: Color,
    onResumeDraft: () -> Unit,
    onClearDraft: () -> Unit
) {
    CreatePostSection {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    imageVector = Icons.Outlined.Save,
                    contentDescription = "Saved draft",
                    tint = accentColor,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicText(
                    text = "Saved Draft",
                    style = TextStyle(contentColor.copy(alpha = 0.92f), 15.sp, FontWeight.Bold)
                )
                BasicText(
                    text = "$draftType - $preview",
                    style = TextStyle(contentColor.copy(alpha = 0.58f), 12.sp, lineHeight = 17.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    text = "Saved separately from this composer. Resume it when you want to finish.",
                    style = TextStyle(contentColor.copy(alpha = 0.46f), 11.sp, lineHeight = 16.sp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ComposerActionChip(
                icon = Icons.Outlined.Description,
                label = "Resume draft",
                enabled = true,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = onResumeDraft
            )
            ComposerActionChip(
                icon = Icons.Outlined.DeleteOutline,
                label = "Clear draft",
                enabled = true,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = onClearDraft
            )
        }
    }
}

@Composable
private fun PostTypeTabs(
    selectedType: PostType,
    onTypeSelected: (PostType) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        postTypeConfigs.forEach { config ->
            val isSelected = config.type == selectedType
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isSelected) {
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.18f),
                                    accentColor.copy(alpha = 0.1f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.03f)
                                )
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (isSelected) accentColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onTypeSelected(config.type) }
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = config.icon,
                        contentDescription = config.label,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected) accentColor else contentColor.copy(alpha = 0.72f)
                    )
                    BasicText(
                        text = config.label,
                        style = TextStyle(
                            color = if (isSelected) accentColor else contentColor.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun UserInfoRow(
    userName: String,
    userUsername: String?,
    userAvatar: String?,
    visibility: String,
    showVisibilityDropdown: Boolean,
    onVisibilityDropdownToggle: (Boolean) -> Unit,
    onVisibilityChange: (String) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current
    val appearance = currentVormexAppearance()
    val normalizedVisibility = normalizePostVisibility(visibility)
    val selectedVisibility = visibilityOptions.firstOrNull { it.value == normalizedVisibility } ?: visibilityOptions.first()
    val displayName = userName.takeIf { it.isNotBlank() } ?: "User"
    val usernameLabel = userUsername
        ?.trim()
        ?.removePrefix("@")
        ?.takeIf { it.isNotBlank() }
        ?.let { "@$it" }
    val avatarModel = resolveProfileImageModel(userAvatar)
    val initials = displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank {
            usernameLabel
                ?.removePrefix("@")
                ?.firstOrNull()
                ?.uppercase()
                ?: "Y"
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(appearance.controlColor)
                .border(1.dp, appearance.controlBorderColor, CircleShape)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (avatarModel == null) accentColor else appearance.inputColor),
                contentAlignment = Alignment.Center
            ) {
                if (avatarModel != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = "$displayName profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                            else -> UserAvatarInitials(initials)
                        }
                    }
                } else {
                    UserAvatarInitials(initials)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            BasicText(
                text = displayName,
                style = TextStyle(contentColor, 15.sp, FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            usernameLabel?.let { username ->
                BasicText(
                    text = username,
                    style = TextStyle(contentColor.copy(alpha = 0.58f), 12.sp, FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(appearance.controlColor)
                        .border(1.dp, appearance.controlBorderColor, RoundedCornerShape(999.dp))
                        .clickable { onVisibilityDropdownToggle(!showVisibilityDropdown) }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = selectedVisibility.icon,
                        contentDescription = selectedVisibility.label,
                        tint = contentColor.copy(alpha = 0.76f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    BasicText(
                        text = selectedVisibility.label.uppercase(),
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.76f),
                            fontSize = 10.sp,
                            letterSpacing = 0.7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (showVisibilityDropdown) "Hide visibility options" else "Show visibility options",
                        tint = contentColor.copy(alpha = 0.62f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = showVisibilityDropdown,
                    onDismissRequest = { onVisibilityDropdownToggle(false) },
                    modifier = Modifier.background(appearance.sheetColor)
                ) {
                    visibilityOptions.forEach { option ->
                        val isSelected = option.value == normalizedVisibility
                        DropdownMenuItem(
                            text = {
                                Column {
                                    BasicText(
                                        text = option.label,
                                        style = TextStyle(
                                            if (isSelected) accentColor else contentColor,
                                            14.sp,
                                            FontWeight.SemiBold
                                        )
                                    )
                                    BasicText(
                                        text = option.detail,
                                        style = TextStyle(contentColor.copy(alpha = 0.55f), 11.sp)
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.label,
                                    tint = if (isSelected) accentColor else contentColor.copy(alpha = 0.72f),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = {
                                onVisibilityChange(normalizePostVisibility(option.value))
                                onVisibilityDropdownToggle(false)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserAvatarInitials(initials: String) {
    BasicText(
        text = initials,
        style = TextStyle(
            color = Color.White,
            fontSize = 16.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium
        )
    )
}

@Composable
private fun RichTextToolbar(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    showColorPicker: Boolean,
    onColorPickerToggle: (Boolean) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolbarButton(
            icon = Icons.Outlined.FormatBold,
            label = "Bold",
            contentColor = contentColor,
            accentColor = accentColor
        ) {
            wrapSelection(textFieldValue, onTextFieldValueChange, "**", "**", "bold text")
        }
        
        ToolbarButton(
            icon = Icons.Outlined.FormatItalic,
            label = "Italic",
            contentColor = contentColor,
            accentColor = accentColor
        ) {
            wrapSelection(textFieldValue, onTextFieldValueChange, "*", "*", "italic text")
        }

        ToolbarButton(
            icon = Icons.Outlined.FormatStrikethrough,
            label = "Strikethrough",
            contentColor = contentColor,
            accentColor = accentColor
        ) {
            wrapSelection(textFieldValue, onTextFieldValueChange, "~~", "~~", "crossed text")
        }
        
        ToolbarButton(
            icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
            label = "List",
            contentColor = contentColor,
            accentColor = accentColor
        ) {
            applyListFormatting(textFieldValue, onTextFieldValueChange)
        }
        
        ToolbarButton(
            icon = Icons.Outlined.Code,
            label = "Code",
            contentColor = contentColor,
            accentColor = accentColor
        ) {
            wrapSelection(textFieldValue, onTextFieldValueChange, "`", "`", "code")
        }

        ToolbarButton(
            icon = Icons.Outlined.FormatAlignCenter,
            label = "Center",
            contentColor = contentColor,
            accentColor = accentColor
        ) {
            wrapSelection(textFieldValue, onTextFieldValueChange, "[center]", "[/center]", "centered text")
        }
        
        ToolbarButton(
            icon = Icons.Outlined.AlternateEmail,
            label = "Mention",
            contentColor = contentColor,
            accentColor = accentColor
        ) {
            val text = textFieldValue.text
            val cursor = textFieldValue.selection.start
            val newText = text.substring(0, cursor) + "@" + text.substring(cursor)
            onTextFieldValueChange(TextFieldValue(
                text = newText,
                selection = TextRange(cursor + 1)
            ))
        }
        
        ToolbarButton(
            icon = Icons.Outlined.Palette,
            label = "Color",
            contentColor = contentColor,
            accentColor = accentColor,
            isActive = showColorPicker
        ) {
            onColorPickerToggle(!showColorPicker)
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    label: String,
    contentColor: Color,
    accentColor: Color,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) accentColor else contentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun wrapSelection(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    prefix: String,
    suffix: String,
    placeholder: String
) {
    val selection = textFieldValue.selection
    val text = textFieldValue.text
    
    if (selection.length > 0) {
        val selectedText = text.substring(selection.start, selection.end)
        val newText = text.substring(0, selection.start) + 
            prefix + selectedText + suffix + 
            text.substring(selection.end)
        onTextFieldValueChange(TextFieldValue(
            text = newText,
            selection = TextRange(selection.start + prefix.length, selection.end + prefix.length)
        ))
    } else {
        val insertedText = prefix + placeholder + suffix
        val newText = text.substring(0, selection.start) +
            insertedText +
            text.substring(selection.start)
        onTextFieldValueChange(TextFieldValue(
            text = newText,
            selection = TextRange(
                selection.start + prefix.length,
                selection.start + prefix.length + placeholder.length
            )
        ))
    }
}

private fun applyListFormatting(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit
) {
    val text = textFieldValue.text
    val selection = textFieldValue.selection
    val lineStart = text.lastIndexOf('\n', (selection.start - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', selection.end).let { if (it == -1) text.length else it }
    val block = text.substring(lineStart, lineEnd)
    val lines = block.split('\n')
    val allListed = lines.filter { it.isNotBlank() }.all { it.trimStart().startsWith("- ") }
    val formattedBlock = lines.joinToString("\n") { line ->
        when {
            line.isBlank() -> line
            allListed -> line.replaceFirst(Regex("^(\\s*)-\\s"), "$1")
            else -> {
                val indentLength = line.indexOfFirst { !it.isWhitespace() }.let { if (it == -1) 0 else it }
                line.substring(0, indentLength) + "- " + line.substring(indentLength)
            }
        }
    }
    val newText = text.substring(0, lineStart) + formattedBlock + text.substring(lineEnd)
    onTextFieldValueChange(
        TextFieldValue(
            text = newText,
            selection = TextRange(
                lineStart,
                lineStart + formattedBlock.length
            )
        )
    )
}

@Composable
private fun ColorPickerRow(
    onColorSelected: (String) -> Unit,
    contentColor: Color
) {
    val appearance = currentVormexAppearance()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(appearance.inputColor)
            .border(1.dp, appearance.inputBorderColor, RoundedCornerShape(18.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        colorPresets.forEach { (hex, name) ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(hex)))
                    .clickable { onColorSelected(hex) }
            )
        }
    }
}

private data class RichTextSpanRange(
    val start: Int,
    var end: Int,
    val style: SpanStyle
)

private data class RichTextParagraphRange(
    val start: Int,
    var end: Int
)

private data class RenderedPostRichText(
    val visibleText: String,
    val originalToTransformed: IntArray,
    val transformedToOriginal: IntArray,
    val spans: List<RichTextSpanRange>,
    val centeredParagraphs: List<RichTextParagraphRange>
)

private class PostRichTextVisualTransformation(
    private val contentColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rendered = renderPostRichText(text.text, contentColor)
        val builder = AnnotatedString.Builder(rendered.visibleText)
        rendered.spans.forEach { span ->
            if (span.start < span.end) {
                builder.addStyle(span.style, span.start, span.end)
            }
        }
        rendered.centeredParagraphs.forEach { paragraph ->
            if (paragraph.start < paragraph.end) {
                builder.addStyle(
                    ParagraphStyle(textAlign = TextAlign.Center),
                    paragraph.start,
                    paragraph.end
                )
            }
        }
        return TransformedText(
            text = builder.toAnnotatedString(),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    val safeOffset = offset.coerceIn(0, rendered.originalToTransformed.lastIndex)
                    return rendered.originalToTransformed[safeOffset]
                }

                override fun transformedToOriginal(offset: Int): Int {
                    val safeOffset = offset.coerceIn(0, rendered.transformedToOriginal.lastIndex)
                    return rendered.transformedToOriginal[safeOffset]
                }
            }
        )
    }
}

private fun renderPostRichText(
    source: String,
    contentColor: Color
): RenderedPostRichText {
    val visible = StringBuilder()
    val originalToTransformed = IntArray(source.length + 1)
    val transformedToOriginal = mutableListOf<Int>()
    val spans = mutableListOf<RichTextSpanRange>()
    val centeredParagraphs = mutableListOf<RichTextParagraphRange>()
    var bold = false
    var italic = false
    var strike = false
    var code = false
    var center = false
    var currentColor: Color? = null
    var index = 0

    fun markHidden(length: Int) {
        repeat(length) { offset ->
            originalToTransformed[(index + offset).coerceAtMost(source.length)] = visible.length
        }
        index += length
    }

    fun addSpan(start: Int, end: Int, style: SpanStyle) {
        val last = spans.lastOrNull()
        if (last != null && last.end == start && last.style == style) {
            last.end = end
        } else {
            spans += RichTextSpanRange(start, end, style)
        }
    }

    fun addCenteredParagraph(start: Int, end: Int) {
        val last = centeredParagraphs.lastOrNull()
        if (last != null && last.end == start) {
            last.end = end
        } else {
            centeredParagraphs += RichTextParagraphRange(start, end)
        }
    }

    fun currentStyle(): SpanStyle {
        return SpanStyle(
            color = currentColor ?: Color.Unspecified,
            fontWeight = if (bold) FontWeight.Bold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
            textDecoration = if (strike) TextDecoration.LineThrough else null,
            fontFamily = if (code) FontFamily.Monospace else null,
            background = if (code) contentColor.copy(alpha = 0.08f) else Color.Unspecified
        )
    }

    fun hasStyle(): Boolean = bold || italic || strike || code || currentColor != null

    while (index < source.length) {
        when {
            source.startsWith("[center]", index) -> {
                markHidden("[center]".length)
                center = true
            }
            source.startsWith("[/center]", index) -> {
                markHidden("[/center]".length)
                center = false
            }
            source.startsWith("[/color]", index) -> {
                markHidden("[/color]".length)
                currentColor = null
            }
            source.startsWith("[color:", index) -> {
                val tokenEnd = source.indexOf(']', startIndex = index)
                val rawColor = if (tokenEnd > index) source.substring(index + "[color:".length, tokenEnd) else ""
                val parsedColor = runCatching {
                    Color(android.graphics.Color.parseColor(rawColor))
                }.getOrNull()
                if (tokenEnd > index && parsedColor != null) {
                    markHidden(tokenEnd - index + 1)
                    currentColor = parsedColor
                } else {
                    originalToTransformed[index] = visible.length
                    transformedToOriginal += index
                    visible.append(source[index])
                    index++
                }
            }
            source.startsWith("**", index) -> {
                markHidden(2)
                bold = !bold
            }
            source.startsWith("~~", index) -> {
                markHidden(2)
                strike = !strike
            }
            source[index] == '*' -> {
                markHidden(1)
                italic = !italic
            }
            source[index] == '`' -> {
                markHidden(1)
                code = !code
            }
            else -> {
                val transformedStart = visible.length
                originalToTransformed[index] = transformedStart
                transformedToOriginal += index
                visible.append(source[index])
                val transformedEnd = visible.length
                if (hasStyle()) {
                    addSpan(transformedStart, transformedEnd, currentStyle())
                }
                if (center) {
                    addCenteredParagraph(transformedStart, transformedEnd)
                }
                index++
            }
        }
    }

    originalToTransformed[source.length] = visible.length
    transformedToOriginal += source.length

    return RenderedPostRichText(
        visibleText = visible.toString(),
        originalToTransformed = originalToTransformed,
        transformedToOriginal = transformedToOriginal.toIntArray(),
        spans = spans,
        centeredParagraphs = centeredParagraphs
    )
}

@Composable
private fun ContentTextArea(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    contentColor: Color,
    accentColor: Color,
    mentionSearchResults: List<MentionUser>,
    isSearchingMentions: Boolean,
    showMentionDropdown: Boolean,
    onMentionSelected: (MentionUser) -> Unit,
    onDismissMentionDropdown: () -> Unit,
    showAiButton: Boolean,
    aiMenuExpanded: Boolean,
    aiBusy: Boolean,
    onAiMenuExpandedChange: (Boolean) -> Unit,
    onProfessionalRewrite: () -> Unit,
    onShorterRewrite: () -> Unit,
    onClearerRewrite: () -> Unit,
    onProofread: () -> Unit,
    backdrop: LayerBackdrop? = null
) {
    val appearance = currentVormexAppearance()
    val density = LocalDensity.current
    val richTextVisualTransformation = remember(contentColor) {
        PostRichTextVisualTransformation(contentColor)
    }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val mentionDropdownOffsetY = remember(textFieldValue.selection, textLayoutResult, showMentionDropdown) {
        val layoutResult = textLayoutResult
        if (!showMentionDropdown || layoutResult == null) {
            0.dp
        } else {
            val selection = textFieldValue.selection.end
                .coerceIn(0, layoutResult.layoutInput.text.length)
            val cursorBottom = with(density) { layoutResult.getCursorRect(selection).bottom.toDp() }
            (cursorBottom + 8.dp).coerceAtLeast(28.dp)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Text field
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 430.dp)
                .padding(top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 390.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = onTextFieldValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 390.dp),
                    textStyle = TextStyle(
                        color = contentColor,
                        fontSize = 17.sp,
                        lineHeight = 25.sp,
                        fontFamily = FontFamily.Serif
                    ),
                    visualTransformation = richTextVisualTransformation,
                    cursorBrush = SolidColor(contentColor),
                    onTextLayout = { textLayoutResult = it },
                    decorationBox = { innerTextField ->
                        Box {
                            if (textFieldValue.text.isEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    BasicText(
                                        text = placeholder,
                                        style = TextStyle(
                                            color = contentColor.copy(alpha = 0.78f),
                                            fontSize = 17.sp,
                                            lineHeight = 25.sp,
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    BasicText(
                                        text = "Share something thoughtful...",
                                        style = TextStyle(
                                            color = appearance.mutedContentColor,
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            fontFamily = FontFamily.Serif
                                        )
                                    )
                                }
                            }
                            innerTextField()
                        }
                    }
                )

                MentionSuggestionsPanel(
                    visible = showMentionDropdown,
                    suggestions = mentionSearchResults,
                    isLoading = isSearchingMentions,
                    contentColor = contentColor,
                    backdrop = backdrop,
                    onSelect = onMentionSelected,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = mentionDropdownOffsetY)
                        .fillMaxWidth()
                        .zIndex(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !aiBusy) {
                                onAiMenuExpandedChange(true)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (aiBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.vormex_logo),
                                contentDescription = "vormex",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = aiMenuExpanded,
                        onDismissRequest = { onAiMenuExpandedChange(false) },
                        modifier = Modifier
                            .background(appearance.sheetColor)
                            .border(1.dp, appearance.sheetBorderColor, RoundedCornerShape(14.dp))
                    ) {
                        VormexAiMenuItem(
                            title = "Professional",
                            subtitle = "Make it polished",
                            icon = Icons.Outlined.Work,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            enabled = showAiButton && !aiBusy,
                            onClick = onProfessionalRewrite
                        )
                        VormexAiMenuItem(
                            title = "Shorter",
                            subtitle = "Trim extra words",
                            icon = Icons.AutoMirrored.Outlined.Subject,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            enabled = showAiButton && !aiBusy,
                            onClick = onShorterRewrite
                        )
                        VormexAiMenuItem(
                            title = "Clearer",
                            subtitle = "Improve readability",
                            icon = Icons.Outlined.AutoAwesome,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            enabled = showAiButton && !aiBusy,
                            onClick = onClearerRewrite
                        )
                        VormexAiMenuItem(
                            title = "Proofread",
                            subtitle = "Fix grammar",
                            icon = Icons.Outlined.Check,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            enabled = showAiButton && !aiBusy,
                            onClick = onProofread
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MentionSuggestionsPanel(
    visible: Boolean,
    suggestions: List<MentionUser>,
    isLoading: Boolean,
    contentColor: Color,
    backdrop: LayerBackdrop?,
    onSelect: (MentionUser) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(20f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.25f),
                                            Color.White.copy(alpha = 0.12f),
                                            Color.White.copy(alpha = 0.08f)
                                        )
                                    )
                                )
                            }
                        )
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.2f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            )
                    }
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicText(
                    text = "Tag someone",
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp, FontWeight.Medium),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = contentColor.copy(alpha = 0.5f)
                        )
                    }
                } else if (suggestions.isEmpty()) {
                    BasicText(
                        text = "Type a name or username to search.",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.52f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)
                    )
                } else {
                    suggestions.take(5).forEach { user ->
                        MentionSuggestionRow(
                            user = user,
                            context = context,
                            contentColor = contentColor,
                            backdrop = backdrop,
                            onSelect = onSelect
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MentionSuggestionRow(
    user: MentionUser,
    context: Context,
    contentColor: Color,
    backdrop: LayerBackdrop?,
    onSelect: (MentionUser) -> Unit
) {
    val mentionAvatarModel = resolveProfileImageModel(user.profileImage ?: user.avatar)
    val mentionInitial = user.name
        ?.firstOrNull()
        ?.uppercase()
        ?: user.username?.firstOrNull()?.uppercase()
        ?: "U"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(14f.dp) },
                        effects = {
                            vibrancy()
                            blur(16f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.18f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            )
                        }
                    )
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.12f))
                }
            )
            .clickable { onSelect(user) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (mentionAvatarModel != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(mentionAvatarModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                            else -> BasicText(
                                text = mentionInitial,
                                style = TextStyle(contentColor, 16.sp, FontWeight.Bold)
                            )
                        }
                    }
                } else {
                    BasicText(
                        text = mentionInitial,
                        style = TextStyle(contentColor, 16.sp, FontWeight.Bold)
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicText(
                    text = user.name ?: user.username ?: "User",
                    style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                VerificationBadge(
                    verified = user.hasVerificationBadge(),
                    badgeStyle = user.verificationBadgeStyle(),
                    size = VerificationBadgeSize.Small
                )
            }
            BasicText(
                text = user.username?.let { "@$it" } ?: user.headline.orEmpty(),
                style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
            )
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Add mention",
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun VormexAiMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    contentColor: Color,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        enabled = enabled,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = title,
                    style = TextStyle(
                        color = contentColor.copy(alpha = if (enabled) 0.92f else 0.38f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    text = subtitle,
                    style = TextStyle(
                        color = contentColor.copy(alpha = if (enabled) 0.58f else 0.32f),
                        fontSize = 11.sp
                    )
                )
            }
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) accentColor else contentColor.copy(alpha = 0.34f),
                modifier = Modifier.size(18.dp)
            )
        },
        onClick = onClick
    )
}

private fun getPlaceholder(postType: PostType): String {
    return when (postType) {
        PostType.TEXT -> "What's the small craft detail you noticed today?"
        PostType.IMAGE -> "Add a caption for your images"
        PostType.VIDEO -> "Describe your video"
        PostType.LINK -> "Add a comment about this link"
        PostType.POLL -> "Ask a question"
        PostType.ARTICLE -> "Write your article content"
        PostType.CELEBRATION -> "Share more about your achievement"
        else -> "What's on your mind?"
    }
}

// ==================== Type-Specific Components ====================

@Composable
private fun ImagePicker(
    imageUris: List<Uri>,
    onPickImages: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current
    val appearance = currentVormexAppearance()
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (imageUris.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accentColor.copy(alpha = 0.12f),
                                appearance.inputColor
                            )
                        )
                    )
                    .border(1.dp, appearance.inputBorderColor, RoundedCornerShape(24.dp))
                    .clickable(onClick = onPickImages),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = "Add images",
                        tint = accentColor,
                        modifier = Modifier.size(30.dp)
                    )
                    BasicText(
                        text = "Add images (max 10)",
                        style = TextStyle(contentColor.copy(alpha = 0.76f), 14.sp, FontWeight.Medium)
                    )
                    BasicText(
                        text = "Bring the visual story in here",
                        style = TextStyle(contentColor.copy(alpha = 0.48f), 12.sp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                imageUris.forEachIndexed { index, uri ->
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(uri).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { onRemoveImage(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                
                if (imageUris.size < 10) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(appearance.controlColor)
                            .border(1.dp, appearance.controlBorderColor, RoundedCornerShape(18.dp))
                            .clickable(onClick = onPickImages),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add more images",
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPicker(
    videoUri: Uri?,
    onPickVideo: () -> Unit,
    onRemoveVideo: () -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    val appearance = currentVormexAppearance()

    if (videoUri == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.12f),
                            appearance.inputColor
                        )
                    )
                )
                .border(1.dp, appearance.inputBorderColor, RoundedCornerShape(24.dp))
                .clickable(onClick = onPickVideo),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.VideoLibrary,
                    contentDescription = "Add video",
                    tint = accentColor,
                    modifier = Modifier.size(30.dp)
                )
                BasicText(
                    text = "Add video (max 500MB)",
                    style = TextStyle(contentColor.copy(alpha = 0.76f), 14.sp, FontWeight.Medium)
                )
                BasicText(
                    text = "One strong clip works best here",
                    style = TextStyle(contentColor.copy(alpha = 0.48f), 12.sp)
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(appearance.inputColor)
                .border(1.dp, appearance.inputBorderColor, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartDisplay,
                    contentDescription = "Video attached",
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = "Video attached",
                        style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                    )
                    BasicText(
                        text = videoUri.lastPathSegment ?: "video.mp4",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.1f))
                        .clickable(onClick = onRemoveVideo),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Remove video",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultVideoDropdown(
    selectedVideoId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onVideoSelected: (String) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    val appearance = currentVormexAppearance()
    val selectedVideo = findDefaultPostVideo(selectedVideoId)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                BasicText(
                    text = "Default Video",
                    style = TextStyle(contentColor.copy(alpha = 0.92f), 15.sp, FontWeight.SemiBold)
                )
                BasicText(
                    text = selectedVideo?.title ?: "Tap to pick an animation",
                    style = TextStyle(
                        color = if (selectedVideo != null) accentColor else appearance.mutedContentColor,
                        fontSize = 12.sp,
                        fontWeight = if (selectedVideo != null) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Hide default videos" else "Show default videos",
                tint = contentColor.copy(alpha = 0.64f),
                modifier = Modifier.size(22.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            DefaultPostVideoPicker(
                selectedVideoId = selectedVideoId,
                onVideoSelected = onVideoSelected,
                contentColor = contentColor,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun DefaultPostVideoPicker(
    selectedVideoId: String?,
    onVideoSelected: (String) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    val appearance = currentVormexAppearance()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(
            items = defaultPostVideos,
            key = { _, video -> video.id },
            contentType = { _, _ -> "default_video_choice" }
        ) { index, video ->
            DefaultPostVideoChoice(
                video = video,
                isSelected = selectedVideoId == video.id,
                onClick = { onVideoSelected(video.id) },
                contentColor = contentColor,
                accentColor = accentColor
            )
            if (index != defaultPostVideos.lastIndex) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = appearance.dividerColor
                )
            }
        }
    }
}

@Composable
private fun DefaultPostVideoChoice(
    video: DefaultPostVideo,
    isSelected: Boolean,
    onClick: () -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(82.dp)
                .height(58.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(contentColor.copy(alpha = 0.05f))
        ) {
            DefaultPostVideoPlayer(
                video = video,
                reduceAnimations = true,
                accentColor = accentColor,
                height = 58.dp,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            BasicText(
                text = video.title,
                style = TextStyle(
                    color = if (isSelected) accentColor else contentColor.copy(alpha = 0.86f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                text = video.description,
                style = TextStyle(contentColor.copy(alpha = 0.48f), 12.sp, lineHeight = 15.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        AnimatedVisibility(visible = isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LinkInput(
    linkUrl: String,
    onLinkUrlChange: (String) -> Unit,
    contentColor: Color
) {
    val appearance = currentVormexAppearance()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            text = "Link URL",
            style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
        )
        BasicTextField(
            value = linkUrl,
            onValueChange = onLinkUrlChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(appearance.inputColor)
                .border(1.dp, appearance.inputBorderColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
            textStyle = TextStyle(contentColor, 14.sp),
            cursorBrush = SolidColor(contentColor),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box {
                    if (linkUrl.isEmpty()) {
                        BasicText(
                            text = "https://example.com",
                            style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun PollEditor(
    options: List<String>,
    onOptionsChange: (List<String>) -> Unit,
    durationHours: Int,
    onDurationChange: (Int) -> Unit,
    showResultsBeforeVote: Boolean,
    onShowResultsChange: (Boolean) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    var showDurationDropdown by remember { mutableStateOf(false) }
    val appearance = currentVormexAppearance()
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(
            text = "Poll Options (2-6)",
            style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
        )
        
        options.forEachIndexed { index, option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = option,
                    onValueChange = { newValue ->
                        onOptionsChange(options.toMutableList().apply { set(index, newValue) })
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(appearance.inputColor)
                        .border(1.dp, appearance.inputBorderColor, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(contentColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (option.isEmpty()) {
                                BasicText(
                                    text = "Option ${index + 1}",
                                    style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                if (options.size > 2) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = 0.1f))
                            .clickable {
                                onOptionsChange(options.filterIndexed { i, _ -> i != index })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove option",
                            tint = Color.Red,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        
        if (options.size < 6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                    .clickable { onOptionsChange(options + "") },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Add option",
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                    BasicText(
                        text = "Add option",
                        style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                    )
                }
            }
        }
        
        // Duration selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = "Duration:",
                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(appearance.controlColor)
                        .border(1.dp, appearance.controlBorderColor, RoundedCornerShape(8.dp))
                        .clickable { showDurationDropdown = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        text = pollDurations.find { it.first == durationHours }?.second ?: "1 day",
                        style = TextStyle(contentColor, 14.sp)
                    )
                }
                
                DropdownMenu(
                    expanded = showDurationDropdown,
                    onDismissRequest = { showDurationDropdown = false }
                ) {
                    pollDurations.forEach { (hours, label) ->
                        DropdownMenuItem(
                            text = { BasicText(label, style = TextStyle(contentColor, 14.sp)) },
                            onClick = {
                                onDurationChange(hours)
                                showDurationDropdown = false
                            }
                        )
                    }
                }
            }
        }
        
        // Show results before vote toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowResultsChange(!showResultsBeforeVote) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = showResultsBeforeVote,
                onCheckedChange = onShowResultsChange
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicText(
                text = "Show results before voting",
                style = TextStyle(contentColor, 14.sp)
            )
        }
    }
}

@Composable
private fun ArticleEditor(
    title: String,
    onTitleChange: (String) -> Unit,
    coverUri: Uri?,
    onPickCover: () -> Unit,
    onRemoveCover: () -> Unit,
    tags: List<String>,
    tagInput: String,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current
    val appearance = currentVormexAppearance()
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Title input
        Column {
            BasicText(
                text = "Article Title *",
                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            BasicTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(appearance.inputColor)
                    .border(1.dp, appearance.inputBorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                textStyle = TextStyle(contentColor, 16.sp, FontWeight.SemiBold),
                cursorBrush = SolidColor(contentColor),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (title.isEmpty()) {
                            BasicText(
                                text = "Enter article title...",
                                style = TextStyle(contentColor.copy(alpha = 0.4f), 16.sp)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        
        // Cover image
        Column {
            BasicText(
                text = "Cover Image (optional)",
                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            if (coverUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(appearance.inputColor)
                        .border(1.dp, appearance.inputBorderColor, RoundedCornerShape(18.dp))
                        .clickable(onClick = onPickCover),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "Add cover image",
                            tint = contentColor.copy(alpha = 0.58f),
                            modifier = Modifier.size(20.dp)
                        )
                        BasicText(
                            text = "Add cover image",
                            style = TextStyle(contentColor.copy(alpha = 0.58f), 14.sp, FontWeight.Medium)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(coverUri).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable(onClick = onRemoveCover),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove cover",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        // Tags
        Column {
            BasicText(
                text = "Tags (max 5)",
                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = tagInput,
                    onValueChange = onTagInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(appearance.inputColor)
                        .border(1.dp, appearance.inputBorderColor, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(contentColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (tagInput.isEmpty()) {
                                BasicText(
                                    text = "Add a tag",
                                    style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                if (tags.size < 5 && tagInput.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor)
                            .clickable(onClick = onAddTag)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add tag",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(accentColor.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                text = tag,
                                style = TextStyle(accentColor, 12.sp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable { onRemoveTag(tag) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Remove tag",
                                    tint = accentColor,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CelebrationPicker(
    selectedType: CelebrationType?,
    onTypeSelected: (CelebrationType) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    val appearance = currentVormexAppearance()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(
            text = "What are you celebrating?",
            style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
        )
        
        // Grid of celebration types (2 columns)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CelebrationType.entries.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { type ->
                        val isSelected = type == selectedType
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.2f)
                                    else appearance.inputColor
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) accentColor.copy(alpha = 0.28f) else appearance.inputBorderColor,
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable { onTypeSelected(type) }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = celebrationTypeIcon(type),
                                    contentDescription = type.label,
                                    tint = if (isSelected) accentColor else contentColor.copy(alpha = 0.72f),
                                    modifier = Modifier.size(24.dp)
                                )
                                BasicText(
                                    text = type.label,
                                    style = TextStyle(
                                        color = if (isSelected) accentColor else contentColor,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    // Fill empty space if odd number of items
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
