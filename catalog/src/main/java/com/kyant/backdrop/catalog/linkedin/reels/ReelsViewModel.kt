package com.kyant.backdrop.catalog.linkedin.reels

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.kyant.backdrop.catalog.deeplink.VormexDeepLinks
import com.kyant.backdrop.catalog.linkedin.PostShareTarget
import com.kyant.backdrop.catalog.linkedin.PostShareTargetSource
import com.kyant.backdrop.catalog.linkedin.UploadProgress
import com.kyant.backdrop.catalog.linkedin.UploadStatus
import com.kyant.backdrop.catalog.linkedin.reels.player.PlayerPool
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.PostsApiService
import com.kyant.backdrop.catalog.network.models.Conversation
import com.kyant.backdrop.catalog.network.models.MentionUser
import com.kyant.backdrop.catalog.network.models.PersonInfo
import com.kyant.backdrop.catalog.network.models.ProfileConnectionItem
import com.kyant.backdrop.catalog.network.models.Reel
import com.kyant.backdrop.catalog.network.models.ReelAuthor
import com.kyant.backdrop.catalog.network.models.ReelComment
import com.kyant.backdrop.catalog.network.models.SharedPostAuthor
import com.kyant.backdrop.catalog.network.models.SharedPostContent
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.LinkedHashMap

private const val REELS_PREVIEW_LIMIT = 10
private const val REELS_INITIAL_FEED_LIMIT = 12
private const val REELS_PAGE_LIMIT = 10
private const val REELS_THUMBNAIL_WARM_AHEAD_COUNT = 3

/**
 * UI State for Reels feature
 */
data class ReelsUiState(
    // Preview section state (for home feed)
    val previewReels: List<Reel> = emptyList(),
    val isLoadingPreview: Boolean = false,
    val previewError: String? = null,

    // Full feed state
    val feedReels: List<Reel> = emptyList(),
    val isLoadingFeed: Boolean = false,
    val feedError: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val activeDraftPreviewId: String? = null,

    // Current reel viewer state
    val isViewerOpen: Boolean = false,
    val currentReelIndex: Int = 0,

    // Comments state (supports nested replies)
    val showCommentsSheet: Boolean = false,
    val commentsReelId: String? = null,
    val reelComments: List<ReelComment> = emptyList(),
    val replyCommentsByParent: Map<String, List<ReelComment>> = emptyMap(),
    val replyCursorsByParent: Map<String, String?> = emptyMap(),
    val hasMoreRepliesByParent: Map<String, Boolean> = emptyMap(),
    val loadingReplyParents: Set<String> = emptySet(),
    val expandedReplyParents: Set<String> = emptySet(),
    val commentsCursor: String? = null,
    val hasMoreComments: Boolean = false,
    val isLoadingComments: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val isSubmittingComment: Boolean = false,
    val commentsError: String? = null,
    val replyToComment: ReelComment? = null,
    val highlightedCommentId: String? = null,
    val highlightedParentCommentId: String? = null,
    val mentionSearchResults: List<MentionUser> = emptyList(),
    val isSearchingMentions: Boolean = false,

    // Creation state
    val isUploadingReel: Boolean = false,
    val reelUploadError: String? = null,
    val lastCreatedReel: Reel? = null,
    val uploadProgress: UploadProgress = UploadProgress(),
    val draftReels: List<Reel> = emptyList(),
    val isLoadingDrafts: Boolean = false,
    val draftError: String? = null,
    val publishingDraftId: String? = null,

    // Share state (same picker algorithm as post sharing)
    val showShareModal: Boolean = false,
    val shareReelId: String? = null,
    val shareTargets: List<PostShareTarget> = emptyList(),
    val isLoadingShareTargets: Boolean = false,
    val isSharing: Boolean = false,
    val shareError: String? = null
)

private data class ReelCommentsCacheEntry(
    val comments: List<ReelComment>,
    val repliesByParent: Map<String, List<ReelComment>>,
    val replyCursorsByParent: Map<String, String?>,
    val hasMoreRepliesByParent: Map<String, Boolean>,
    val expandedParents: Set<String>,
    val commentsCursor: String?,
    val hasMoreComments: Boolean
)

/**
 * ViewModel for Reels with lightweight warm-up for thumbnails and feed data.
 */
class ReelsViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    // HTTP client for preloading
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // Track thumbnails we've already warmed so scroll-driven warmups stay cheap.
    private val warmedThumbnailUrls: MutableSet<String> = ConcurrentHashMap.newKeySet<String>()

    private val previewCacheTtlMillis = 5 * 60 * 1000L
    private val feedCacheTtlMillis = 5 * 60 * 1000L
    private var lastPreviewLoadedAt = 0L
    private var lastFeedLoadedAt = 0L
    private var isPreviewRequestInFlight = false
    private var isFeedRequestInFlight = false
    private var isDraftsRequestInFlight = false
    private val thumbnailWarmAheadCount = REELS_THUMBNAIL_WARM_AHEAD_COUNT
    private val sharePayloadJson = Json { encodeDefaults = true }
    private val likingReelIds = mutableSetOf<String>()
    private val savingReelIds = mutableSetOf<String>()
    private val sharingReelIds = mutableSetOf<String>()
    private val submittingCommentKeys = mutableSetOf<String>()
    private var shareTargetsLoaded = false
    private var cachedShareTargets: List<PostShareTarget> = emptyList()
    private val commentsCache = object : LinkedHashMap<String, ReelCommentsCacheEntry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ReelCommentsCacheEntry>?): Boolean {
            return size > 30
        }
    }

    private fun prefetchFeedSilently(mode: String = "foryou") {
        val now = System.currentTimeMillis()
        val isFeedFresh =
            _uiState.value.feedReels.isNotEmpty() &&
                (now - lastFeedLoadedAt) < feedCacheTtlMillis

        if (isFeedFresh || isFeedRequestInFlight) return

        isFeedRequestInFlight = true
        viewModelScope.launch {
            val result = ApiClient.getReelsFeed(context, limit = REELS_INITIAL_FEED_LIMIT, mode = mode)
            result.onSuccess { response ->
                lastFeedLoadedAt = System.currentTimeMillis()
                applyFreshFeed(response.reels, response.nextCursor, response.hasMore)
            }
            isFeedRequestInFlight = false
        }
    }

    fun prefetchAppStartData(includeHomePreview: Boolean = false) {
        if (includeHomePreview) {
            loadPreviewReels()
        }
        prefetchFeedSilently()
    }

    /**
     * Load trending reels for the home feed preview section
     */
    fun loadPreviewReels(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        val isPreviewFresh =
            !forceRefresh &&
                _uiState.value.previewReels.isNotEmpty() &&
                (now - lastPreviewLoadedAt) < previewCacheTtlMillis

        if (isPreviewFresh || isPreviewRequestInFlight) return

        isPreviewRequestInFlight = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingPreview = _uiState.value.previewReels.isEmpty() || forceRefresh,
                previewError = null
            )

            val trendingResult = ApiClient.getTrendingReels(context, hours = 48, limit = REELS_PREVIEW_LIMIT)
            val trendingResponse = trendingResult.getOrNull()
            val result = if (trendingResponse != null && trendingResponse.reels.isNotEmpty()) {
                trendingResult
            } else {
                ApiClient.getReelsFeed(context, limit = REELS_PREVIEW_LIMIT, mode = "foryou")
            }

            result.onSuccess { response ->
                lastPreviewLoadedAt = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    previewReels = response.reels,
                    isLoadingPreview = false
                )

                // Warm preview thumbnails so the entry point feels instant.
                preloadThumbnails(response.reels.take(REELS_THUMBNAIL_WARM_AHEAD_COUNT + 1))
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingPreview = false,
                    previewError = error.message
                )
            }
            isPreviewRequestInFlight = false
        }
    }

    /**
     * Load the main reels feed
     */
    fun loadReelsFeed(mode: String = "foryou", forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        val isFeedFresh =
            !forceRefresh &&
                _uiState.value.feedReels.isNotEmpty() &&
                (now - lastFeedLoadedAt) < feedCacheTtlMillis

        if (isFeedFresh || isFeedRequestInFlight) return

        isFeedRequestInFlight = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingFeed = true,
                feedError = null,
                nextCursor = null,
                hasMore = true
            )

            val result = ApiClient.getReelsFeed(context, limit = REELS_INITIAL_FEED_LIMIT, mode = mode)

            result.onSuccess { response ->
                lastFeedLoadedAt = System.currentTimeMillis()
                applyFreshFeed(response.reels, response.nextCursor, response.hasMore)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingFeed = false,
                    feedError = error.message
                )
            }
            isFeedRequestInFlight = false
        }
    }

    /**
     * Load more reels (pagination)
     */
    fun loadMoreReels(mode: String = "foryou") {
        val currentState = _uiState.value

        if (currentState.isLoadingMore || !currentState.hasMore || currentState.nextCursor == null) {
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoadingMore = true)

            val result = ApiClient.getReelsFeed(
                context,
                cursor = currentState.nextCursor,
                limit = REELS_PAGE_LIMIT,
                mode = mode
            )

            result.onSuccess { response ->
                val updatedFeed = (currentState.feedReels + response.reels).distinctBy { it.id }
                _uiState.value = _uiState.value.copy(
                    feedReels = updatedFeed,
                    isLoadingMore = false,
                    nextCursor = response.nextCursor,
                    hasMore = response.hasMore
                )
                preloadThumbnails(response.reels.take(thumbnailWarmAheadCount + 1))
                if (_uiState.value.isViewerOpen) {
                    syncPlaybackWindow(updatedFeed, _uiState.value.currentReelIndex)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    /**
     * Open the full-screen reels viewer at a specific index
     */
    fun openReelsViewer(reels: List<Reel>, startIndex: Int = 0) {
        val playbackReels = reels.ifEmpty { _uiState.value.feedReels }
        if (playbackReels.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, playbackReels.lastIndex)

        _uiState.value = _uiState.value.copy(
            isViewerOpen = true,
            currentReelIndex = safeIndex,
            feedReels = playbackReels
        )

        preloadUpcomingThumbnails(safeIndex)
        syncPlaybackWindow(playbackReels, safeIndex)
    }

    /**
     * Open from the 10-item home preview, then hydrate with the real paginated feed.
     */
    fun openPreviewReelsViewer(startIndex: Int = 0) {
        val state = _uiState.value
        val previewReels = state.previewReels
        if (previewReels.isEmpty()) {
            loadAndOpenReels()
            return
        }

        val safePreviewIndex = startIndex.coerceIn(0, previewReels.lastIndex)
        val targetReelId = previewReels.getOrNull(safePreviewIndex)?.id
        val feedIndex = targetReelId
            ?.let { id -> state.feedReels.indexOfFirst { it.id == id } }
            ?: -1
        val hasHydratedFeed = state.feedReels.size > previewReels.size &&
            (state.nextCursor != null || !state.hasMore)
        val playbackReels = if (feedIndex >= 0 && hasHydratedFeed) {
            state.feedReels
        } else {
            previewReels
        }
        val playbackIndex = if (playbackReels === state.feedReels && feedIndex >= 0) {
            feedIndex
        } else {
            safePreviewIndex
        }
        val shouldHydrate = playbackReels.size <= REELS_PREVIEW_LIMIT ||
            (state.hasMore && state.nextCursor == null)

        _uiState.value = state.copy(
            isViewerOpen = true,
            currentReelIndex = playbackIndex,
            feedReels = playbackReels,
            isLoadingFeed = shouldHydrate,
            feedError = null
        )

        preloadUpcomingThumbnails(playbackIndex)
        syncPlaybackWindow(playbackReels, playbackIndex)

        if (shouldHydrate) {
            loadReelsFeed(forceRefresh = true)
        }
    }

    /**
     * Close the reels viewer
     */
    fun closeReelsViewer() {
        val state = _uiState.value
        val draftPreviewId = state.activeDraftPreviewId
        val nextFeedReels = if (draftPreviewId != null) {
            state.feedReels.filterNot { it.id == draftPreviewId }
        } else {
            state.feedReels
        }
        _uiState.value = state.copy(
            isViewerOpen = false,
            feedReels = nextFeedReels,
            currentReelIndex = state.currentReelIndex.coerceAtMost((nextFeedReels.size - 1).coerceAtLeast(0)),
            activeDraftPreviewId = null
        )
        releasePlayback()
    }

    fun openDraftPreview(reel: Reel) {
        val state = _uiState.value
        val playbackReels = listOf(reel) + state.feedReels.filterNot { it.id == reel.id }

        _uiState.value = state.copy(
            feedReels = playbackReels,
            isViewerOpen = true,
            isLoadingFeed = false,
            feedError = null,
            currentReelIndex = 0,
            activeDraftPreviewId = reel.id
        )

        preloadUpcomingThumbnails(0)
        syncPlaybackWindow(playbackReels, 0)
    }

    /**
     * Load a specific reel by ID and open it in the viewer (for deep links from notifications)
     */
    fun loadReelById(reelId: String) {
        val normalizedReelId = reelId.trim()
        if (normalizedReelId.isBlank()) return

        val cachedReel = findReelById(normalizedReelId)
        if (cachedReel != null) {
            openExactReelAtTop(cachedReel, forceCurrentToStart = true)
            refreshExactReelWithRecommendations(
                reelId = normalizedReelId,
                fallbackReel = cachedReel,
                forceCurrentToStart = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isViewerOpen = true,
            feedReels = emptyList(),
            currentReelIndex = 0,
            isLoadingFeed = true,
            feedError = null,
            nextCursor = null,
            hasMore = true
        )
        refreshExactReelWithRecommendations(
            reelId = normalizedReelId,
            fallbackReel = null,
            forceCurrentToStart = true
        )
    }

    fun openSharedReel(sharedReel: SharedPostContent) {
        val reelId = sharedReel.resolvedReelId()
        if (reelId.isBlank()) return

        val cachedReel = findReelById(reelId)
        val instantReel = cachedReel ?: sharedReel.toInstantReelOrNull(reelId)

        if (instantReel != null) {
            openExactReelAtTop(instantReel, forceCurrentToStart = true)
            refreshExactReelWithRecommendations(
                reelId = reelId,
                fallbackReel = instantReel,
                forceCurrentToStart = false
            )
        } else {
            loadReelById(reelId)
        }
    }

    private fun refreshExactReelWithRecommendations(
        reelId: String,
        fallbackReel: Reel?,
        forceCurrentToStart: Boolean
    ) {
        viewModelScope.launch {
            val exactReelDeferred = async { ApiClient.getReel(context, reelId) }
            val recommendationsDeferred = async {
                ApiClient.getReelsFeed(context, limit = REELS_INITIAL_FEED_LIMIT, mode = "foryou")
            }

            val exactReelResult = exactReelDeferred.await()
            val recommendationsResult = recommendationsDeferred.await()

            val recommendedFeed = recommendationsResult.getOrNull()
            if (recommendedFeed != null) {
                lastFeedLoadedAt = System.currentTimeMillis()
            }

            val exactReel = exactReelResult.getOrNull() ?: fallbackReel
            if (exactReel != null) {
                openExactReelAtTop(
                    reel = exactReel,
                    recommendedReels = recommendedFeed?.reels.orEmpty(),
                    nextCursor = recommendedFeed?.nextCursor ?: _uiState.value.nextCursor,
                    hasMore = recommendedFeed?.hasMore ?: _uiState.value.hasMore,
                    forceCurrentToStart = forceCurrentToStart
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingFeed = false,
                    feedError = exactReelResult.exceptionOrNull()?.message ?: "Failed to load reel",
                    isViewerOpen = _uiState.value.feedReels.isNotEmpty()
                )
            }
        }
    }

    private fun openExactReelAtTop(
        reel: Reel,
        recommendedReels: List<Reel> = emptyList(),
        nextCursor: String? = _uiState.value.nextCursor,
        hasMore: Boolean = _uiState.value.hasMore,
        forceCurrentToStart: Boolean
    ) {
        val state = _uiState.value
        val currentReelId = state.feedReels.getOrNull(state.currentReelIndex)?.id
        val tail = (recommendedReels + state.feedReels + state.previewReels)
            .distinctBy { it.id }
            .filterNot { it.id == reel.id }
        val updatedFeed = listOf(reel) + tail
        val nextIndex = if (forceCurrentToStart) {
            0
        } else {
            currentReelId
                ?.let { id -> updatedFeed.indexOfFirst { it.id == id } }
                ?.takeIf { it >= 0 }
                ?: state.currentReelIndex.coerceIn(0, updatedFeed.lastIndex)
        }

        _uiState.value = state.copy(
            feedReels = updatedFeed,
            isViewerOpen = true,
            isLoadingFeed = false,
            feedError = null,
            currentReelIndex = nextIndex,
            nextCursor = nextCursor,
            hasMore = hasMore
        )

        preloadUpcomingThumbnails(nextIndex)
        syncPlaybackWindow(updatedFeed, nextIndex)
    }

    private fun SharedPostContent.resolvedReelId(): String {
        reelId.trim().takeIf { it.isNotBlank() }?.let { return it }
        return reelUrl
            .takeIf { it.isNotBlank() }
            ?.let { url -> runCatching { VormexDeepLinks.extractReelId(Uri.parse(url)) }.getOrNull() }
            .orEmpty()
    }

    private fun SharedPostContent.toInstantReelOrNull(reelId: String): Reel? {
        val playableUrl = videoUrl?.takeIf { it.isNotBlank() } ?: return null
        val sharedAuthor = author
        val previewText = preview
            .takeIf { it.isNotBlank() }
            ?: title
            ?: caption
            ?: "Shared reel"

        return Reel(
            id = reelId,
            author = ReelAuthor(
                id = sharedAuthor?.id?.takeIf { it.isNotBlank() } ?: "",
                username = sharedAuthor?.username,
                name = sharedAuthor?.name,
                profileImage = sharedAuthor?.profileImage
            ),
            videoUrl = playableUrl,
            hlsUrl = hlsUrl?.takeIf { it.isNotBlank() },
            thumbnailUrl = mediaUrl?.takeIf { it.isNotBlank() },
            previewGifUrl = previewGifUrl?.takeIf { it.isNotBlank() } ?: mediaUrl?.takeIf { it.isNotBlank() },
            title = title ?: previewText,
            caption = caption ?: previewText,
            durationSeconds = durationSeconds,
            width = width,
            height = height,
            hashtags = hashtags
        )
    }

    /**
     * Load reels and immediately open the viewer
     */
    fun loadAndOpenReels() {
        val state = _uiState.value
        if (state.feedReels.size > state.previewReels.size) {
            openReelsViewer(state.feedReels, 0)
            return
        }

        if (state.previewReels.isNotEmpty()) {
            openPreviewReelsViewer(0)
            return
        }

        if (state.feedReels.isNotEmpty()) {
            openReelsViewer(state.feedReels, 0)
            if (state.hasMore && state.nextCursor == null) {
                loadReelsFeed(forceRefresh = true)
            }
            return
        }

        _uiState.value = state.copy(
            isViewerOpen = true,
            isLoadingFeed = true,
            feedError = null
        )
        loadReelsFeed(forceRefresh = true)
    }

    /**
     * Update current reel index (called when user scrolls)
     */
    fun onReelChanged(newIndex: Int) {
        _uiState.value = _uiState.value.copy(currentReelIndex = newIndex)

        preloadUpcomingThumbnails(newIndex)
        syncPlaybackWindow(currentPlaybackReels(), newIndex)
    }

    fun playerForIndex(index: Int): ExoPlayer? {
        return PlayerPool.playerForIndex(context, index)
    }

    fun handlePlaybackError(index: Int): Boolean {
        return PlayerPool.handlePlaybackError(context, index)
    }

    fun retryPlayback(index: Int) {
        PlayerPool.retry(context, index)
    }

    fun pausePlayback(resetPosition: Boolean = false) {
        PlayerPool.pauseAll(resetPosition)
    }

    fun resumePlayback(currentIndex: Int = _uiState.value.currentReelIndex) {
        syncPlaybackWindow(currentPlaybackReels(), currentIndex)
    }

    fun releasePlayback() {
        PlayerPool.release()
    }

    /**
     * Toggle like on a reel
     */
    fun toggleLike(reelId: String) {
        if (!likingReelIds.add(reelId)) return

        viewModelScope.launch {
            try {
                val result = ApiClient.toggleReelLike(context, reelId)

                result.onSuccess { response ->
                    updateReelInLists(reelId) { reel ->
                        reel.copy(
                            isLiked = response.liked,
                            likesCount = response.likesCount
                        )
                    }
                }
            } finally {
                likingReelIds.remove(reelId)
            }
        }
    }

    /**
     * Toggle save on a reel
     */
    fun toggleSave(reelId: String) {
        if (!savingReelIds.add(reelId)) return

        viewModelScope.launch {
            try {
                val result = ApiClient.toggleReelSave(context, reelId)

                result.onSuccess { response ->
                    updateReelInLists(reelId) { reel ->
                        reel.copy(
                            isSaved = response.saved,
                            savesCount = response.savesCount
                        )
                    }
                }
            } finally {
                savingReelIds.remove(reelId)
            }
        }
    }

    fun showShareModal(reelId: String) {
        val hasCachedTargets = shareTargetsLoaded
        _uiState.value = _uiState.value.copy(
            showShareModal = true,
            shareReelId = reelId,
            shareTargets = if (hasCachedTargets) cachedShareTargets else _uiState.value.shareTargets,
            isLoadingShareTargets = false,
            shareError = null
        )
        if (!hasCachedTargets) {
            loadShareTargets()
        }
    }

    fun hideShareModal() {
        _uiState.value = _uiState.value.copy(
            showShareModal = false,
            shareReelId = null,
            isLoadingShareTargets = false,
            shareError = null,
            isSharing = false
        )
    }

    fun loadShareTargets() {
        if (_uiState.value.isLoadingShareTargets) return
        if (shareTargetsLoaded) {
            _uiState.value = _uiState.value.copy(
                shareTargets = cachedShareTargets,
                isLoadingShareTargets = false,
                shareError = if (cachedShareTargets.isEmpty()) {
                    "No people found to share with yet."
                } else {
                    null
                }
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingShareTargets = true,
                shareError = null
            )

            val currentUserId = ApiClient.getCurrentUserId(context)
            val recentConversationsDeferred = async {
                ApiClient.getConversations(context, limit = 10).getOrNull()?.conversations.orEmpty()
            }
            val connectionsDeferred = async {
                currentUserId?.let { userId ->
                    ApiClient.getUserConnections(context, userId, limit = 60).getOrNull()?.connections
                }.orEmpty()
            }
            val recommendationsDeferred = async {
                ApiClient.getPeopleSuggestions(context, limit = 24).getOrNull()?.suggestions.orEmpty()
            }

            val shareTargets = buildReelShareTargets(
                currentUserId = currentUserId,
                recentConversations = recentConversationsDeferred.await(),
                connections = connectionsDeferred.await(),
                recommendations = recommendationsDeferred.await()
            )

            cachedShareTargets = shareTargets
            shareTargetsLoaded = true
            _uiState.value = _uiState.value.copy(
                shareTargets = shareTargets,
                isLoadingShareTargets = false,
                shareError = if (shareTargets.isEmpty()) {
                    "No people found to share with yet."
                } else {
                    null
                }
            )
        }
    }

    fun copyReelLink(reelId: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val reelUrl = VormexDeepLinks.reelUrl(reelId)
        clipboard.setPrimaryClip(ClipData.newPlainText("Reel URL", reelUrl))
        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()

        val shareKey = "copy:$reelId"
        if (sharingReelIds.add(shareKey)) {
            viewModelScope.launch {
                try {
                    ApiClient.shareReel(context, reelId, shareType = "copy_link")
                        .onSuccess { response ->
                            updateReelInLists(reelId) { reel ->
                                reel.copy(sharesCount = response.sharesCount)
                            }
                        }
                        .onFailure { error ->
                            _uiState.value = _uiState.value.copy(
                                shareError = error.message ?: "Failed to record reel share"
                            )
                        }
                } finally {
                    sharingReelIds.remove(shareKey)
                }
            }
        }
    }

    fun shareReelInApp(targetUserIds: List<String>, message: String?) {
        val reelId = _uiState.value.shareReelId ?: return
        val shareKey = "chat:$reelId"
        if (_uiState.value.isSharing || !sharingReelIds.add(shareKey)) return

        val selectedTargets = targetUserIds.distinct().filter { it.isNotBlank() }
        if (selectedTargets.isEmpty()) {
            sharingReelIds.remove(shareKey)
            _uiState.value = _uiState.value.copy(shareError = "Select at least one person.")
            return
        }

        val reel = findReelById(reelId)
        val sharedReelMessage = buildSharedReelMessage(reelId, reel)

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSharing = true, shareError = null)

                var sentCount = 0
                var failedCount = 0

                for (targetUserId in selectedTargets) {
                    val conversationResult = ApiClient.getOrCreateConversation(context, targetUserId)
                    if (conversationResult.isFailure) {
                        failedCount += 1
                        continue
                    }

                    val conversation = conversationResult.getOrThrow()
                    ApiClient.sendMessage(
                        context = context,
                        conversationId = conversation.id,
                        content = sharedReelMessage,
                        contentType = "reel",
                        mediaUrl = reel?.thumbnailUrl ?: reel?.previewGifUrl,
                        mediaType = "reel"
                    ).onSuccess {
                        sentCount += 1
                    }.onFailure {
                        failedCount += 1
                    }
                }

                if (sentCount > 0) {
                    ApiClient.shareReel(
                        context = context,
                        reelId = reelId,
                        shareType = "chat"
                    ).onSuccess { response ->
                        updateReelInLists(reelId) { reelItem ->
                            reelItem.copy(sharesCount = response.sharesCount)
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        showShareModal = false,
                        shareReelId = null,
                        isSharing = false,
                        shareError = null
                    )
                    val suffix = if (failedCount > 0) " ($failedCount failed)" else ""
                    Toast.makeText(
                        context,
                        "Sent to $sentCount ${if (sentCount == 1) "person" else "people"}$suffix",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSharing = false,
                        shareError = "Could not send this reel. Please try again."
                    )
                }
            } finally {
                sharingReelIds.remove(shareKey)
            }
        }
    }

    fun clearShareError() {
        _uiState.value = _uiState.value.copy(shareError = null)
    }

    /**
     * Track a reel view
     */
    fun trackView(reelId: String, watchTimeMs: Long, completed: Boolean) {
        viewModelScope.launch {
            ApiClient.trackReelView(context, reelId, watchTimeMs, completed)
        }
    }

    fun loadDraftReels(forceRefresh: Boolean = false) {
        val state = _uiState.value
        if (isDraftsRequestInFlight) return
        if (!forceRefresh && state.draftReels.isNotEmpty()) return

        isDraftsRequestInFlight = true
        _uiState.value = state.copy(
            isLoadingDrafts = true,
            draftError = null
        )

        viewModelScope.launch {
            val result = ApiClient.getMyDraftReels(context, limit = 20)
            result.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    draftReels = response.reels,
                    isLoadingDrafts = false,
                    draftError = null
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingDrafts = false,
                    draftError = error.message ?: "Failed to load drafts"
                )
            }
            isDraftsRequestInFlight = false
        }
    }

    fun publishDraftReel(reelId: String, onSuccess: (Reel) -> Unit = {}) {
        val normalizedReelId = reelId.trim()
        if (normalizedReelId.isBlank() || _uiState.value.publishingDraftId != null) return

        _uiState.value = _uiState.value.copy(
            publishingDraftId = normalizedReelId,
            draftError = null,
            uploadProgress = UploadProgress(
                status = UploadStatus.UPLOADING,
                progress = 0.45f,
                message = "Publishing draft...",
                postType = "REEL"
            )
        )

        viewModelScope.launch {
            val result = ApiClient.publishDraftReel(context, normalizedReelId)
            result.onSuccess { reel ->
                val shouldSurfaceImmediately = reel.status.equals("ready", ignoreCase = true)
                _uiState.value = _uiState.value.copy(
                    publishingDraftId = null,
                    draftReels = _uiState.value.draftReels.filterNot { it.id == reel.id },
                    draftError = null,
                    uploadProgress = UploadProgress(
                        status = UploadStatus.SUCCESS,
                        progress = 1f,
                        message = "Draft published",
                        postType = "REEL"
                    ),
                    previewReels = if (shouldSurfaceImmediately) {
                        listOf(reel) + _uiState.value.previewReels.filterNot { it.id == reel.id }
                    } else {
                        _uiState.value.previewReels
                    },
                    feedReels = if (shouldSurfaceImmediately) {
                        listOf(reel) + _uiState.value.feedReels.filterNot { it.id == reel.id }
                    } else {
                        _uiState.value.feedReels
                    }
                )
                if (shouldSurfaceImmediately) {
                    loadPreviewReels(forceRefresh = true)
                    loadReelsFeed(forceRefresh = true)
                }
                onSuccess(reel)
                viewModelScope.launch {
                    delay(2500)
                    clearReelUploadProgress()
                }
            }.onFailure { error ->
                val message = error.message ?: "Failed to publish draft"
                _uiState.value = _uiState.value.copy(
                    publishingDraftId = null,
                    draftError = message,
                    uploadProgress = _uiState.value.uploadProgress.copy(
                        status = UploadStatus.FAILED,
                        message = message
                    )
                )
                viewModelScope.launch {
                    delay(4500)
                    clearReelUploadProgress()
                }
            }
        }
    }

    fun createReel(
        videoUri: Uri,
        videoFileName: String,
        videoMimeType: String,
        videoSize: Long?,
        thumbnailUri: Uri?,
        thumbnailFileName: String?,
        thumbnailMimeType: String?,
        thumbnailSize: Long?,
        title: String,
        caption: String,
        hashtags: List<String>,
        category: String?,
        visibility: String,
        allowComments: Boolean,
        allowDuets: Boolean,
        allowStitch: Boolean,
        allowDownload: Boolean,
        allowSharing: Boolean,
        muteOriginalAudio: Boolean,
        saveAsDraft: Boolean,
        onSuccess: (Reel) -> Unit = {}
    ) {
        if (_uiState.value.isUploadingReel) return

        _uiState.value = _uiState.value.copy(
            isUploadingReel = true,
            reelUploadError = null,
            lastCreatedReel = null,
            uploadProgress = UploadProgress(
                status = UploadStatus.UPLOADING,
                progress = 0.12f,
                message = "Uploading reel...",
                postType = "REEL"
            )
        )

        viewModelScope.launch {
            val progressJob = launch {
                var progress = 0.18f
                while (true) {
                    delay(850)
                    progress = (progress + 0.07f).coerceAtMost(0.82f)
                    val current = _uiState.value.uploadProgress
                    if (current.status == UploadStatus.UPLOADING) {
                        _uiState.value = _uiState.value.copy(
                            uploadProgress = current.copy(progress = progress)
                        )
                    }
                }
            }

            val result = ApiClient.createReel(
                context = context,
                videoUri = videoUri,
                videoFileName = videoFileName,
                videoMimeType = videoMimeType,
                videoSize = videoSize,
                thumbnailUri = thumbnailUri,
                thumbnailFileName = thumbnailFileName,
                thumbnailMimeType = thumbnailMimeType,
                thumbnailSize = thumbnailSize,
                title = title,
                caption = caption,
                hashtags = hashtags,
                category = category,
                visibility = visibility,
                allowComments = allowComments,
                allowDuets = allowDuets,
                allowStitch = allowStitch,
                allowDownload = allowDownload,
                allowSharing = allowSharing,
                muteOriginalAudio = muteOriginalAudio,
                saveAsDraft = saveAsDraft
            )
            progressJob.cancel()

            result.onSuccess { reel ->
                val shouldSurfaceImmediately = !saveAsDraft && reel.status.equals("ready", ignoreCase = true)
                val finishedProgress = when {
                    saveAsDraft -> UploadProgress(
                        status = UploadStatus.SUCCESS,
                        progress = 1f,
                        message = "Reel saved as draft",
                        postType = "REEL"
                    )
                    reel.status.equals("ready", ignoreCase = true) -> UploadProgress(
                        status = UploadStatus.SUCCESS,
                        progress = 1f,
                        message = "Reel posted",
                        postType = "REEL"
                    )
                    else -> UploadProgress(
                        status = UploadStatus.PROCESSING,
                        progress = 0.92f,
                        message = "Reel uploaded. Processing video...",
                        postType = "REEL"
                    )
                }
                _uiState.value = _uiState.value.copy(
                    isUploadingReel = false,
                    lastCreatedReel = reel,
                    uploadProgress = finishedProgress,
                    draftReels = if (saveAsDraft) {
                        listOf(reel) + _uiState.value.draftReels.filterNot { it.id == reel.id }
                    } else {
                        _uiState.value.draftReels
                    },
                    previewReels = if (shouldSurfaceImmediately) {
                        listOf(reel) + _uiState.value.previewReels.filterNot { it.id == reel.id }
                    } else {
                        _uiState.value.previewReels
                    },
                    feedReels = if (shouldSurfaceImmediately) {
                        listOf(reel) + _uiState.value.feedReels.filterNot { it.id == reel.id }
                    } else {
                        _uiState.value.feedReels
                    }
                )
                if (!saveAsDraft) {
                    loadPreviewReels(forceRefresh = true)
                    loadReelsFeed(forceRefresh = true)
                }
                onSuccess(reel)
                viewModelScope.launch {
                    delay(if (finishedProgress.status == UploadStatus.PROCESSING) 5000 else 2500)
                    clearReelUploadProgress()
                }
            }.onFailure { error ->
                val message = error.message ?: "Failed to upload reel"
                _uiState.value = _uiState.value.copy(
                    isUploadingReel = false,
                    reelUploadError = message,
                    uploadProgress = _uiState.value.uploadProgress.copy(
                        status = UploadStatus.FAILED,
                        message = message
                    )
                )
                viewModelScope.launch {
                    delay(4500)
                    clearReelUploadProgress()
                }
            }
        }
    }

    fun clearReelUploadState() {
        _uiState.value = _uiState.value.copy(
            reelUploadError = null,
            lastCreatedReel = null
        )
    }

    fun clearReelUploadProgress() {
        _uiState.value = _uiState.value.copy(uploadProgress = UploadProgress())
    }

    fun dismissReelUploadProgress() {
        clearReelUploadProgress()
    }

    // ==================== Comments (Nested) ====================

    fun openComments(
        reelId: String,
        highlightCommentId: String? = null,
        parentCommentId: String? = null
    ) {
        val cleanHighlightCommentId = highlightCommentId?.takeIf { it.isNotBlank() }
        val cleanParentCommentId = parentCommentId?.takeIf { it.isNotBlank() }
        val cached = commentsCache[reelId]

        _uiState.value = _uiState.value.copy(
            showCommentsSheet = true,
            commentsReelId = reelId,
            reelComments = cached?.comments ?: emptyList(),
            replyCommentsByParent = cached?.repliesByParent ?: emptyMap(),
            replyCursorsByParent = cached?.replyCursorsByParent ?: emptyMap(),
            hasMoreRepliesByParent = cached?.hasMoreRepliesByParent ?: emptyMap(),
            loadingReplyParents = emptySet(),
            expandedReplyParents = cached?.expandedParents ?: emptySet(),
            commentsCursor = cached?.commentsCursor,
            hasMoreComments = cached?.hasMoreComments ?: false,
            isLoadingComments = cached == null || cleanHighlightCommentId != null,
            isLoadingMoreComments = false,
            commentsError = null,
            replyToComment = null,
            highlightedCommentId = cleanHighlightCommentId,
            highlightedParentCommentId = cleanParentCommentId,
            mentionSearchResults = emptyList(),
            isSearchingMentions = false
        )

        if (cached == null || cleanHighlightCommentId != null) {
            loadReelComments(reelId, refresh = true)
        }
    }

    fun closeComments() {
        _uiState.value.commentsReelId?.let { cacheCurrentComments(it) }
        _uiState.value = _uiState.value.copy(
            showCommentsSheet = false,
            commentsReelId = null,
            reelComments = emptyList(),
            replyCommentsByParent = emptyMap(),
            replyCursorsByParent = emptyMap(),
            hasMoreRepliesByParent = emptyMap(),
            loadingReplyParents = emptySet(),
            expandedReplyParents = emptySet(),
            commentsCursor = null,
            hasMoreComments = false,
            isLoadingComments = false,
            isLoadingMoreComments = false,
            isSubmittingComment = false,
            commentsError = null,
            replyToComment = null,
            highlightedCommentId = null,
            highlightedParentCommentId = null,
            mentionSearchResults = emptyList(),
            isSearchingMentions = false
        )
    }

    private fun cacheCurrentComments(reelId: String) {
        val state = _uiState.value
        if (state.commentsReelId != reelId) return
        if (state.isLoadingComments || state.commentsError != null) return

        commentsCache[reelId] = ReelCommentsCacheEntry(
            comments = state.reelComments,
            repliesByParent = state.replyCommentsByParent,
            replyCursorsByParent = state.replyCursorsByParent,
            hasMoreRepliesByParent = state.hasMoreRepliesByParent,
            expandedParents = state.expandedReplyParents,
            commentsCursor = state.commentsCursor,
            hasMoreComments = state.hasMoreComments
        )
    }

    fun loadReelComments(reelId: String? = _uiState.value.commentsReelId, refresh: Boolean = false) {
        val targetReelId = reelId ?: return

        if (refresh) {
            _uiState.value = _uiState.value.copy(
                isLoadingComments = true,
                commentsError = null,
                commentsCursor = null,
                hasMoreComments = false
            )
        } else {
            if (_uiState.value.isLoadingMoreComments || !_uiState.value.hasMoreComments) return
            _uiState.value = _uiState.value.copy(isLoadingMoreComments = true)
        }

        viewModelScope.launch {
            val cursor = if (refresh) null else _uiState.value.commentsCursor
            val highlightCommentId = if (refresh) _uiState.value.highlightedCommentId else null
            ApiClient.getReelComments(
                context = context,
                reelId = targetReelId,
                cursor = cursor,
                limit = 20,
                highlightCommentId = highlightCommentId
            )
                .onSuccess { response ->
                    val merged = if (refresh) {
                        response.comments
                    } else {
                        (_uiState.value.reelComments + response.comments).distinctBy { it.id }
                    }

                    _uiState.value = _uiState.value.copy(
                        reelComments = merged,
                        commentsCursor = response.nextCursor,
                        hasMoreComments = response.hasMore,
                        isLoadingComments = false,
                        isLoadingMoreComments = false,
                        commentsError = null
                    )
                    cacheCurrentComments(targetReelId)

                    val highlightedCommentId = _uiState.value.highlightedCommentId
                    val inferredParentCommentId = if (
                        refresh &&
                        _uiState.value.highlightedParentCommentId.isNullOrBlank() &&
                        !highlightedCommentId.isNullOrBlank() &&
                        merged.none { it.id == highlightedCommentId }
                    ) {
                        merged.firstOrNull()?.id
                    } else {
                        null
                    }
                    val parentToExpand = _uiState.value.highlightedParentCommentId ?: inferredParentCommentId
                    if (refresh && !parentToExpand.isNullOrBlank()) {
                        loadReplies(
                            parentCommentId = parentToExpand,
                            highlightCommentId = highlightedCommentId,
                            forceExpand = true
                        )
                    } else if (refresh && !highlightCommentId.isNullOrBlank()) {
                        clearHighlightedCommentAfterDelay(highlightCommentId)
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingComments = false,
                        isLoadingMoreComments = false,
                        commentsError = e.message ?: "Failed to load comments"
                    )
                }
        }
    }

    fun loadReplies(
        parentCommentId: String,
        highlightCommentId: String? = null,
        forceExpand: Boolean = false
    ) {
        val reelId = _uiState.value.commentsReelId ?: return
        if (_uiState.value.replyCommentsByParent[parentCommentId] != null) {
            _uiState.value = _uiState.value.copy(
                expandedReplyParents = if (forceExpand) {
                    _uiState.value.expandedReplyParents + parentCommentId
                } else if (_uiState.value.expandedReplyParents.contains(parentCommentId)) {
                    _uiState.value.expandedReplyParents - parentCommentId
                } else {
                    _uiState.value.expandedReplyParents + parentCommentId
                }
            )
            cacheCurrentComments(reelId)
            if (!highlightCommentId.isNullOrBlank()) {
                clearHighlightedCommentAfterDelay(highlightCommentId)
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loadingReplyParents = _uiState.value.loadingReplyParents + parentCommentId
            )
            ApiClient.getReelComments(
                context = context,
                reelId = reelId,
                limit = 20,
                parentId = parentCommentId,
                highlightCommentId = highlightCommentId
            )
                .onSuccess { response ->
                    val replies = if (!highlightCommentId.isNullOrBlank()) {
                        response.comments.sortedBy { it.id != highlightCommentId }
                    } else {
                        response.comments
                    }
                    _uiState.value = _uiState.value.copy(
                        replyCommentsByParent = _uiState.value.replyCommentsByParent + (parentCommentId to replies),
                        replyCursorsByParent = _uiState.value.replyCursorsByParent + (parentCommentId to response.nextCursor),
                        hasMoreRepliesByParent = _uiState.value.hasMoreRepliesByParent + (parentCommentId to response.hasMore),
                        loadingReplyParents = _uiState.value.loadingReplyParents - parentCommentId,
                        expandedReplyParents = _uiState.value.expandedReplyParents + parentCommentId
                    )
                    cacheCurrentComments(reelId)
                    if (!highlightCommentId.isNullOrBlank()) {
                        clearHighlightedCommentAfterDelay(highlightCommentId)
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        loadingReplyParents = _uiState.value.loadingReplyParents - parentCommentId
                    )
                }
        }
    }

    fun loadMoreReplies(parentCommentId: String) {
        val reelId = _uiState.value.commentsReelId ?: return
        val state = _uiState.value
        val cursor = state.replyCursorsByParent[parentCommentId] ?: return
        if (
            state.loadingReplyParents.contains(parentCommentId) ||
            state.hasMoreRepliesByParent[parentCommentId] != true
        ) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loadingReplyParents = _uiState.value.loadingReplyParents + parentCommentId
            )

            ApiClient.getReelComments(
                context = context,
                reelId = reelId,
                cursor = cursor,
                limit = 20,
                parentId = parentCommentId
            )
                .onSuccess { response ->
                    val existing = _uiState.value.replyCommentsByParent[parentCommentId].orEmpty()
                    val merged = (existing + response.comments).distinctBy { it.id }
                    _uiState.value = _uiState.value.copy(
                        replyCommentsByParent = _uiState.value.replyCommentsByParent + (parentCommentId to merged),
                        replyCursorsByParent = _uiState.value.replyCursorsByParent + (parentCommentId to response.nextCursor),
                        hasMoreRepliesByParent = _uiState.value.hasMoreRepliesByParent + (parentCommentId to response.hasMore),
                        loadingReplyParents = _uiState.value.loadingReplyParents - parentCommentId,
                        expandedReplyParents = _uiState.value.expandedReplyParents + parentCommentId
                    )
                    cacheCurrentComments(reelId)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        loadingReplyParents = _uiState.value.loadingReplyParents - parentCommentId
                    )
                }
        }
    }

    fun setReplyTarget(comment: ReelComment?) {
        _uiState.value = _uiState.value.copy(replyToComment = comment)
    }

    fun searchMentions(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            clearMentionSearch()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingMentions = true)

            PostsApiService.searchMentions(context, trimmed, limit = 8)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        mentionSearchResults = response.users,
                        isSearchingMentions = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        mentionSearchResults = emptyList(),
                        isSearchingMentions = false
                    )
                }
        }
    }

    fun clearMentionSearch() {
        _uiState.value = _uiState.value.copy(
            mentionSearchResults = emptyList(),
            isSearchingMentions = false
        )
    }

    fun submitComment(content: String) {
        val reelId = _uiState.value.commentsReelId ?: return
        val trimmed = content.trim()
        if (trimmed.isEmpty() || _uiState.value.isSubmittingComment) return

        val parent = _uiState.value.replyToComment
        val submissionKey = listOf(reelId, parent?.id.orEmpty(), trimmed).joinToString("|")
        if (!submittingCommentKeys.add(submissionKey)) return

        _uiState.value = _uiState.value.copy(isSubmittingComment = true, commentsError = null)

        viewModelScope.launch {
            try {
                ApiClient.createReelComment(
                    context = context,
                    reelId = reelId,
                    content = trimmed,
                    parentId = parent?.id,
                    mentions = extractMentionUsernames(trimmed)
                )
                    .onSuccess { comment ->
                        if (parent == null) {
                            _uiState.value = _uiState.value.copy(
                                reelComments = listOf(comment) + _uiState.value.reelComments,
                                isSubmittingComment = false,
                                replyToComment = null,
                                mentionSearchResults = emptyList(),
                                isSearchingMentions = false
                            )
                            cacheCurrentComments(reelId)
                            updateReelInLists(reelId) { reel -> reel.copy(commentsCount = reel.commentsCount + 1) }
                        } else {
                            val existingReplies = _uiState.value.replyCommentsByParent[parent.id] ?: emptyList()
                            val updatedParents = _uiState.value.reelComments.map {
                                if (it.id == parent.id) it.copy(repliesCount = it.repliesCount + 1) else it
                            }
                            _uiState.value = _uiState.value.copy(
                                reelComments = updatedParents,
                                replyCommentsByParent = _uiState.value.replyCommentsByParent + (parent.id to (listOf(comment) + existingReplies)),
                                expandedReplyParents = _uiState.value.expandedReplyParents + parent.id,
                                isSubmittingComment = false,
                                replyToComment = null,
                                mentionSearchResults = emptyList(),
                                isSearchingMentions = false
                            )
                            cacheCurrentComments(reelId)
                        }
                    }
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isSubmittingComment = false,
                            commentsError = e.message ?: "Failed to post comment"
                        )
                    }
            } finally {
                submittingCommentKeys.remove(submissionKey)
            }
        }
    }

    private fun clearHighlightedCommentAfterDelay(commentId: String) {
        viewModelScope.launch {
            delay(2_000)
            if (_uiState.value.highlightedCommentId == commentId) {
                _uiState.value = _uiState.value.copy(
                    highlightedCommentId = null,
                    highlightedParentCommentId = null
                )
            }
        }
    }

    private fun extractMentionUsernames(content: String): List<String> {
        return Regex("(^|[^A-Za-z0-9_.])@([A-Za-z0-9_][A-Za-z0-9_.-]{1,29})")
            .findAll(content)
            .map { it.groupValues[2].trim().trim('.', '-').lowercase() }
            .filter { it.length >= 2 }
            .distinct()
            .take(30)
            .toList()
    }

    private fun buildReelShareTargets(
        currentUserId: String?,
        recentConversations: List<Conversation>,
        connections: List<ProfileConnectionItem>,
        recommendations: List<PersonInfo>
    ): List<PostShareTarget> {
        val targetsById = linkedMapOf<String, PostShareTarget>()

        recentConversations
            .sortedByDescending { it.lastMessageAt ?: it.lastMessage?.createdAt ?: it.updatedAt }
            .take(10)
            .forEach { conversation ->
                val user = conversation.otherParticipant
                if (user.id == currentUserId) return@forEach
                targetsById[user.id] = PostShareTarget(
                    id = user.id,
                    username = user.username,
                    name = user.name,
                    avatar = user.profileImage,
                    headline = if (user.lastActiveAt != null) "Recently active" else null,
                    reason = "Recent chat",
                    source = PostShareTargetSource.RecentChat
                )
            }

        val sortedConnections = connections.sortedByDescending { it.createdAt }
        val connectionLimit = if (sortedConnections.size >= 50) 50 else sortedConnections.size
        sortedConnections
            .take(connectionLimit)
            .forEachIndexed { index, connection ->
                val user = connection.user
                if (user.id == currentUserId || targetsById.containsKey(user.id)) return@forEachIndexed
                val isRecentConnection = index < 10
                targetsById[user.id] = PostShareTarget(
                    id = user.id,
                    username = user.username,
                    name = user.name,
                    avatar = user.profileImage,
                    headline = user.headline ?: user.college,
                    reason = if (isRecentConnection) "Connected recently" else "Connection",
                    source = if (isRecentConnection) {
                        PostShareTargetSource.RecentConnection
                    } else {
                        PostShareTargetSource.Connection
                    }
                )
            }

        recommendations
            .sortedWith(
                compareByDescending<PersonInfo> { it.mutualConnections }
                    .thenByDescending { it.isOnline }
            )
            .forEach { person ->
                if (person.id == currentUserId || targetsById.containsKey(person.id)) return@forEach
                targetsById[person.id] = PostShareTarget(
                    id = person.id,
                    username = person.username,
                    name = person.name,
                    avatar = person.profileImage,
                    headline = person.headline ?: person.college,
                    reason = when {
                        person.mutualConnections > 0 -> "${person.mutualConnections} mutual"
                        !person.college.isNullOrBlank() -> person.college
                        person.isOnline -> "Active now"
                        else -> "Recommended"
                    },
                    source = PostShareTargetSource.Recommended
                )
            }

        return targetsById.values.toList()
    }

    private fun buildSharedReelMessage(reelId: String, reel: Reel?): String {
        val preview = listOfNotNull(
            reel?.title,
            reel?.caption
        )
            .firstOrNull { it.isNotBlank() }
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.take(220)
            ?: "A reel on Vormex"

        val sharedReel = SharedPostContent(
            type = "shared_reel",
            reelId = reelId,
            reelUrl = VormexDeepLinks.reelUrl(reelId),
            preview = preview,
            author = reel?.author?.let { author ->
                SharedPostAuthor(
                    id = author.id,
                    name = author.name,
                    username = author.username,
                    profileImage = author.profileImage
                )
            },
            mediaUrl = reel?.thumbnailUrl ?: reel?.previewGifUrl,
            videoUrl = reel?.videoUrl,
            hlsUrl = reel?.hlsUrl,
            previewGifUrl = reel?.previewGifUrl,
            title = reel?.title,
            caption = reel?.caption,
            durationSeconds = reel?.durationSeconds ?: 0,
            width = reel?.width ?: 1080,
            height = reel?.height ?: 1920,
            hashtags = reel?.hashtags.orEmpty()
        )

        return sharePayloadJson.encodeToString(sharedReel)
    }

    // ==================== Poster Warm-Up ====================

    /**
     * Warm thumbnail requests so poster images appear immediately while video buffers.
     */
    private fun preloadThumbnails(reels: List<Reel>) {
        viewModelScope.launch(Dispatchers.IO) {
            reels.forEach { reel ->
                val posterUrl = reel.thumbnailUrl ?: reel.previewGifUrl
                posterUrl?.let { url ->
                    if (!warmedThumbnailUrls.add(url)) {
                        return@let
                    }
                    try {
                        val request = Request.Builder()
                            .url(url)
                            .head() // Just warm up the connection and cache
                            .build()
                        httpClient.newCall(request).execute().close()
                    } catch (e: Exception) {
                        // Ignore errors in preloading
                    }
                }
            }
        }
    }

    private fun applyFreshFeed(
        freshReels: List<Reel>,
        nextCursor: String?,
        hasMore: Boolean
    ) {
        val state = _uiState.value
        val currentReelId = state.feedReels.getOrNull(state.currentReelIndex)?.id
        val updatedFeed = if (state.isViewerOpen && state.feedReels.isNotEmpty()) {
            (state.feedReels + freshReels).distinctBy { it.id }
        } else {
            freshReels
        }
        val updatedIndex = when {
            updatedFeed.isEmpty() -> 0
            state.isViewerOpen && currentReelId != null -> {
                updatedFeed.indexOfFirst { it.id == currentReelId }
                    .takeIf { it >= 0 }
                    ?: state.currentReelIndex.coerceIn(0, updatedFeed.lastIndex)
            }
            else -> state.currentReelIndex.coerceIn(0, updatedFeed.lastIndex)
        }

        _uiState.value = state.copy(
            feedReels = updatedFeed,
            isLoadingFeed = false,
            feedError = if (updatedFeed.isEmpty() && state.isViewerOpen) {
                "No reels available yet"
            } else {
                null
            },
            nextCursor = nextCursor,
            hasMore = hasMore,
            currentReelIndex = updatedIndex
        )

        preloadUpcomingThumbnails(updatedIndex)
        if (_uiState.value.isViewerOpen) {
            syncPlaybackWindow(updatedFeed, updatedIndex)
        }
    }

    private fun preloadUpcomingThumbnails(currentIndex: Int) {
        val reels = _uiState.value.feedReels.ifEmpty { _uiState.value.previewReels }
        if (reels.isEmpty()) return

        val safeStart = currentIndex.coerceIn(0, reels.lastIndex)
        val postersToWarm = (safeStart..(safeStart + thumbnailWarmAheadCount))
            .mapNotNull(reels::getOrNull)

        preloadThumbnails(postersToWarm)
    }

    private fun syncPlaybackWindow(reels: List<Reel>, currentIndex: Int) {
        if (reels.isEmpty()) return
        PlayerPool.syncWindow(context, reels, currentIndex.coerceIn(0, reels.lastIndex))
    }

    private fun warmPlaybackWindow(reels: List<Reel>, currentIndex: Int) {
        if (reels.isEmpty()) return
        PlayerPool.warmWindow(context, reels, currentIndex.coerceIn(0, reels.lastIndex))
    }

    /**
     * Update a reel in both preview and feed lists
     */
    private fun updateReelInLists(reelId: String, update: (Reel) -> Reel) {
        _uiState.value = _uiState.value.copy(
            previewReels = _uiState.value.previewReels.map {
                if (it.id == reelId) update(it) else it
            },
            feedReels = _uiState.value.feedReels.map {
                if (it.id == reelId) update(it) else it
            }
        )
    }

    /**
     * Clean up preload cache when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        releasePlayback()
        warmedThumbnailUrls.clear()
    }

    private fun currentPlaybackReels(): List<Reel> {
        return _uiState.value.feedReels.ifEmpty { _uiState.value.previewReels }
    }

    private fun findReelById(reelId: String): Reel? {
        return _uiState.value.feedReels.firstOrNull { it.id == reelId }
            ?: _uiState.value.previewReels.firstOrNull { it.id == reelId }
            ?: _uiState.value.lastCreatedReel?.takeIf { it.id == reelId }
    }

    companion object {
        fun Factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReelsViewModel(context) as T
                }
            }
        }
    }
}

/**
 * Extension to copy Reel with updated fields
 */
private fun Reel.copy(
    isLiked: Boolean = this.isLiked,
    likesCount: Int = this.likesCount,
    isSaved: Boolean = this.isSaved,
    savesCount: Int = this.savesCount,
    commentsCount: Int = this.commentsCount,
    sharesCount: Int = this.sharesCount
): Reel {
    return Reel(
        id = this.id,
        author = this.author,
        videoId = this.videoId,
        videoUrl = this.videoUrl,
        hlsUrl = this.hlsUrl,
        thumbnailUrl = this.thumbnailUrl,
        previewGifUrl = this.previewGifUrl,
        title = this.title,
        caption = this.caption,
        durationSeconds = this.durationSeconds,
        width = this.width,
        height = this.height,
        aspectRatio = this.aspectRatio,
        audio = this.audio,
        hashtags = this.hashtags,
        mentions = this.mentions,
        skills = this.skills,
        topics = this.topics,
        category = this.category,
        locationName = this.locationName,
        isResponse = this.isResponse,
        responseType = this.responseType,
        originalReelId = this.originalReelId,
        pollQuestion = this.pollQuestion,
        pollOptions = this.pollOptions,
        pollEndsAt = this.pollEndsAt,
        userVotedOption = this.userVotedOption,
        quizQuestion = this.quizQuestion,
        quizOptions = this.quizOptions,
        codeSnippet = this.codeSnippet,
        codeLanguage = this.codeLanguage,
        codeFileName = this.codeFileName,
        repoUrl = this.repoUrl,
        visibility = this.visibility,
        allowComments = this.allowComments,
        allowDuets = this.allowDuets,
        allowStitch = this.allowStitch,
        allowDownload = this.allowDownload,
        allowSharing = this.allowSharing,
        status = this.status,
        viewsCount = this.viewsCount,
        likesCount = likesCount,
        commentsCount = commentsCount,
        sharesCount = sharesCount,
        savesCount = savesCount,
        isLiked = isLiked,
        isSaved = isSaved,
        publishedAt = this.publishedAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
