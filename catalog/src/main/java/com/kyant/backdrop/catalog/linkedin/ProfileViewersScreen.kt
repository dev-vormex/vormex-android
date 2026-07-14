package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.kyant.backdrop.catalog.ui.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.ProfileViewHistory
import com.kyant.backdrop.catalog.network.models.ProfileViewHistoryItem
import com.kyant.backdrop.catalog.network.models.ProfileViewStats
import com.kyant.backdrop.catalog.network.models.ProfileInsights
import com.kyant.backdrop.catalog.network.models.ProfileSaverItem
import com.kyant.backdrop.catalog.network.models.ProfileSavers
import com.kyant.backdrop.catalog.network.models.RecentViewedProfileItem
import com.kyant.backdrop.catalog.network.models.RecentViewedProfiles
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class ProfileViewersUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val stats: ProfileViewStats? = null,
    val insights: ProfileInsights? = null,
    val history: ProfileViewHistory = ProfileViewHistory(),
    val savers: ProfileSavers = ProfileSavers(),
    val recentlyViewed: RecentViewedProfiles = RecentViewedProfiles(),
    val isPremiumRequired: Boolean = false,
    val error: String? = null
)

private class ProfileViewersViewModel(
    private val context: Context
) : ViewModel() {
    companion object {
        private const val HISTORY_LIMIT = 100
    }

    private val _uiState = MutableStateFlow(ProfileViewersUiState())
    val uiState: StateFlow<ProfileViewersUiState> = _uiState.asStateFlow()

    init {
        refresh(showLoader = true)
    }

    fun refresh(showLoader: Boolean = false) {
        viewModelScope.launch {
            val userId = ApiClient.getCurrentUserId(context)
            if (userId.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "Sign in to see who viewed your profile"
                )
                return@launch
            }

            val hasExistingContent =
                _uiState.value.history.viewers.isNotEmpty() ||
                    _uiState.value.savers.savers.isNotEmpty() ||
                    _uiState.value.recentlyViewed.profiles.isNotEmpty() ||
                    _uiState.value.stats != null ||
                    _uiState.value.insights != null
            _uiState.value = _uiState.value.copy(
                isLoading = showLoader || !hasExistingContent,
                isRefreshing = hasExistingContent,
                error = null
            )

            val statsDeferred = async { ApiClient.getProfileViewStats(context, userId) }
            val historyDeferred = async { ApiClient.getProfileViewHistory(context, userId, page = 1, limit = HISTORY_LIMIT) }
            val insightsDeferred = async { ApiClient.getProfileInsights(context, userId) }
            val saversDeferred = async { ApiClient.getProfileSavers(context, userId, page = 1, limit = HISTORY_LIMIT) }
            val recentlyViewedDeferred = async { ApiClient.getRecentViewedProfiles(context, page = 1, limit = HISTORY_LIMIT) }

            val statsResult = statsDeferred.await()
            val historyResult = historyDeferred.await()
            val insightsResult = insightsDeferred.await()
            val saversResult = saversDeferred.await()
            val recentlyViewedResult = recentlyViewedDeferred.await()

            val nextStats = statsResult.getOrNull() ?: _uiState.value.stats
            val nextHistory = historyResult.getOrNull() ?: _uiState.value.history
            val nextInsights = insightsResult.getOrNull() ?: _uiState.value.insights
            val nextSavers = saversResult.getOrNull() ?: _uiState.value.savers
            val nextRecentlyViewed = recentlyViewedResult.getOrNull() ?: _uiState.value.recentlyViewed
            val premiumRequiredError = listOf(
                statsResult,
                historyResult,
                insightsResult,
                saversResult
            ).mapNotNull { it.exceptionOrNull()?.message }
                .firstOrNull { message ->
                    message.contains("Premium is required", ignoreCase = true) ||
                        message.contains("premium_required", ignoreCase = true)
                }
            val nextError = statsResult.exceptionOrNull()?.message
                ?: historyResult.exceptionOrNull()?.message
                ?: insightsResult.exceptionOrNull()?.message
                ?: saversResult.exceptionOrNull()?.message
                ?: recentlyViewedResult.exceptionOrNull()?.message
            val hasLoadedPremiumContent =
                nextStats != null ||
                    nextInsights != null ||
                    nextHistory.viewers.isNotEmpty() ||
                    nextSavers.savers.isNotEmpty()
            val hasLoadedContent = hasLoadedPremiumContent || nextRecentlyViewed.profiles.isNotEmpty()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                stats = nextStats,
                history = nextHistory,
                insights = nextInsights,
                savers = nextSavers,
                recentlyViewed = nextRecentlyViewed,
                isPremiumRequired = premiumRequiredError != null && !hasLoadedPremiumContent,
                error = premiumRequiredError ?: if (!hasLoadedContent) {
                    nextError ?: "Failed to load profile viewers"
                } else null
            )
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewersViewModel(context.applicationContext) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileViewersScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    onOpenProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ProfileViewersViewModel =
        viewModel(factory = ProfileViewersViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    val refreshState = rememberPullToRefreshState()
    val isDarkSurface = contentColor == Color.White

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() },
        state = refreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            SettingsHeader(
                title = "Profile insights",
                contentColor = contentColor,
                onBack = onNavigateBack
            )

            when {
                uiState.isLoading && uiState.history.viewers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!uiState.isPremiumRequired) {
                            item {
                                ViewerSummaryCard(
                                    stats = uiState.stats,
                                    history = uiState.history,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isDarkSurface = isDarkSurface
                                )
                            }
                        }

                        if (uiState.recentlyViewed.profiles.isNotEmpty()) {
                            item {
                                BasicText(
                                    text = "Recently viewed by you",
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.56f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            items(uiState.recentlyViewed.profiles, key = { item -> item.viewedId }) { item ->
                                RecentViewedProfileCard(
                                    item = item,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isDarkSurface = isDarkSurface,
                                    onClick = { onOpenProfile(item.profile.id) }
                                )
                            }
                        }

                        uiState.insights?.let { insights ->
                            item {
                                ProfileAnalyticsCard(
                                    insights = insights,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isDarkSurface = isDarkSurface
                                )
                            }

                            item {
                                MatchInsightsCard(
                                    insights = insights,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isDarkSurface = isDarkSurface
                                )
                            }
                        }

                        if (!uiState.error.isNullOrBlank()) {
                            item {
                                InlineInfoCard(
                                    title = if (uiState.isPremiumRequired) "Premium required" else "Couldn’t refresh right now",
                                    body = uiState.error ?: "",
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isDarkSurface = isDarkSurface
                                )
                            }
                        }

                        if (uiState.savers.savers.isNotEmpty()) {
                            item {
                                BasicText(
                                    text = "Bookmarked you",
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.56f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            items(uiState.savers.savers, key = { item -> item.id }) { item ->
                                ProfileSaverCard(
                                    item = item,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isDarkSurface = isDarkSurface,
                                    onClick = { onOpenProfile(item.saver.id) }
                                )
                            }
                        }

                        if (uiState.history.viewers.isEmpty() && !uiState.isPremiumRequired) {
                            item {
                                EmptyViewersState(
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isDarkSurface = isDarkSurface
                                )
                            }
                        } else {
                            item {
                                BasicText(
                                    text = "Recent people",
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.56f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            items(uiState.history.viewers, key = { item -> item.viewerId }) { item ->
                                ViewerHistoryCard(
                                    item = item,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isDarkSurface = isDarkSurface,
                                    onClick = {
                                        item.viewer?.id?.let(onOpenProfile)
                                    }
                                )
                            }

                            item {
                                Spacer(Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerSummaryCard(
    stats: ProfileViewStats?,
    history: ProfileViewHistory,
    contentColor: Color,
    accentColor: Color,
    isDarkSurface: Boolean
) {
    SurfaceCard(
        contentColor = contentColor,
        isDarkSurface = isDarkSurface,
        accentColor = accentColor
    ) {
        BasicText(
            text = "Who checked your profile",
            style = TextStyle(
                color = contentColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(Modifier.height(6.dp))

        BasicText(
            text = if ((stats?.viewsLastHour ?: 0) > 0) {
                "${stats?.viewsLastHour ?: 0} new view${if ((stats?.viewsLastHour ?: 0) == 1) "" else "s"} in the last hour"
            } else {
                "Tap any viewer to open their profile and see the latest visit time."
            },
            style = TextStyle(
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryMetric(
                label = "Total views",
                value = (stats?.totalViews ?: history.totalViews).toString(),
                contentColor = contentColor,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
            SummaryMetric(
                label = "Today",
                value = (stats?.viewsToday ?: stats?.todayViews ?: 0).toString(),
                contentColor = contentColor,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
            SummaryMetric(
                label = "This week",
                value = (stats?.viewsThisWeek ?: stats?.weeklyViews ?: 0).toString(),
                contentColor = contentColor,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProfileAnalyticsCard(
    insights: ProfileInsights,
    contentColor: Color,
    accentColor: Color,
    isDarkSurface: Boolean
) {
    val analytics = insights.analytics
    SurfaceCard(
        contentColor = contentColor,
        isDarkSurface = isDarkSurface,
        accentColor = accentColor
    ) {
        BasicText(
            text = "Profile analytics",
            style = TextStyle(
                color = contentColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = "${analytics.views.last30Days} views and ${analytics.searchAppearances.last30Days} search appearances in the last 30 days",
            style = TextStyle(
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryMetric(
                label = "Searches",
                value = analytics.searchAppearances.total.toString(),
                contentColor = contentColor,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
            SummaryMetric(
                label = "Saved",
                value = analytics.profileSaves.total.toString(),
                contentColor = contentColor,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
            SummaryMetric(
                label = "Match rate",
                value = analytics.matchRate.display,
                contentColor = contentColor,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MatchInsightsCard(
    insights: ProfileInsights,
    contentColor: Color,
    accentColor: Color,
    isDarkSurface: Boolean
) {
    val tags = insights.matchInsights.topTags.take(8)
    val reasons = insights.matchInsights.reasons.take(3)
    SurfaceCard(
        contentColor = contentColor,
        isDarkSurface = isDarkSurface,
        accentColor = accentColor
    ) {
        BasicText(
            text = "Match insights",
            style = TextStyle(
                color = contentColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            BasicText(
                text = tags.joinToString(" • ") { it.label },
                style = TextStyle(
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp
                )
            )
        }

        Spacer(Modifier.height(10.dp))

        if (reasons.isEmpty()) {
            BasicText(
                text = "Add skills, interests, and intent to make your match reasons sharper.",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.68f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            )
        } else {
            reasons.forEach { reason ->
                BasicText(
                    text = "• $reason",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .border(1.dp, accentColor.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        BasicText(
            text = value,
            style = TextStyle(
                color = contentColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(4.dp))
        BasicText(
            text = label,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.65f),
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun ViewerHistoryCard(
    item: ProfileViewHistoryItem,
    contentColor: Color,
    accentColor: Color,
    isDarkSurface: Boolean,
    onClick: () -> Unit
) {
    val viewer = item.viewer
    val canOpenProfile = viewer != null
    val title = viewer?.name?.ifBlank { viewer.username } ?: "Someone"
    val subtitle = listOfNotNull(
        viewer?.headline?.takeIf { it.isNotBlank() },
        viewer?.college?.takeIf { it.isNotBlank() }
    ).joinToString(" • ")

    SurfaceCard(
        contentColor = contentColor,
        isDarkSurface = isDarkSurface,
        accentColor = accentColor,
        modifier = Modifier.then(
            if (canOpenProfile) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewerAvatar(
                imageUrl = viewer?.profileImage,
                label = title,
                accentColor = accentColor
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        text = title,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    VerificationBadge(
                        verified = viewer?.hasVerificationBadge() == true,
                        badgeStyle = viewer?.verificationBadgeStyle(),
                        size = VerificationBadgeSize.Small
                    )
                }

                if (subtitle.isNotBlank()) {
                    BasicText(
                        text = subtitle,
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.64f),
                            fontSize = 11.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.isSameCollege) {
                        InfoChip(
                            text = "Same college",
                            textColor = accentColor,
                            background = accentColor.copy(alpha = 0.12f),
                            borderColor = accentColor.copy(alpha = 0.2f)
                        )
                    }

                    if (item.viewCount > 1) {
                        InfoChip(
                            text = "${item.viewCount} views",
                            textColor = contentColor.copy(alpha = 0.75f),
                            background = contentColor.copy(alpha = if (isDarkSurface) 0.08f else 0.06f),
                            borderColor = contentColor.copy(alpha = 0.12f)
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicText(
                    text = relativeTime(item.lastViewedAt),
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    text = absoluteTime(item.lastViewedAt),
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.52f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ProfileSaverCard(
    item: ProfileSaverItem,
    contentColor: Color,
    accentColor: Color,
    isDarkSurface: Boolean,
    onClick: () -> Unit
) {
    val saver = item.saver
    val title = saver.name.ifBlank { saver.username }
    val subtitle = listOfNotNull(
        saver.headline?.takeIf { it.isNotBlank() },
        saver.college?.takeIf { it.isNotBlank() }
    ).joinToString(" • ")

    SurfaceCard(
        contentColor = contentColor,
        isDarkSurface = isDarkSurface,
        accentColor = accentColor,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewerAvatar(
                imageUrl = saver.profileImage,
                label = title,
                accentColor = accentColor
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        text = title,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    VerificationBadge(
                        verified = saver.hasVerificationBadge(),
                        badgeStyle = saver.verificationBadgeStyle(),
                        size = VerificationBadgeSize.Small
                    )
                }

                if (subtitle.isNotBlank()) {
                    BasicText(
                        text = subtitle,
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.64f),
                            fontSize = 11.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicText(
                    text = relativeTime(item.savedAt),
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    text = "Saved",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.52f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun RecentViewedProfileCard(
    item: RecentViewedProfileItem,
    contentColor: Color,
    accentColor: Color,
    isDarkSurface: Boolean,
    onClick: () -> Unit
) {
    val profile = item.profile
    val title = profile.name.ifBlank { profile.username }
    val subtitle = listOfNotNull(
        profile.headline?.takeIf { it.isNotBlank() },
        profile.college?.takeIf { it.isNotBlank() }
    ).joinToString(" • ")

    SurfaceCard(
        contentColor = contentColor,
        isDarkSurface = isDarkSurface,
        accentColor = accentColor,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewerAvatar(
                imageUrl = profile.profileImage,
                label = title,
                accentColor = accentColor
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        text = title,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    VerificationBadge(
                        verified = profile.hasVerificationBadge(),
                        badgeStyle = profile.verificationBadgeStyle(),
                        size = VerificationBadgeSize.Small
                    )
                }

                if (subtitle.isNotBlank()) {
                    BasicText(
                        text = subtitle,
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.64f),
                            fontSize = 11.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.viewCount > 1) {
                    InfoChip(
                        text = "${item.viewCount} visits",
                        textColor = contentColor.copy(alpha = 0.75f),
                        background = contentColor.copy(alpha = if (isDarkSurface) 0.08f else 0.06f),
                        borderColor = contentColor.copy(alpha = 0.12f)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicText(
                    text = relativeTime(item.lastViewedAt),
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    text = "Viewed",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.52f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ViewerAvatar(
    imageUrl: String?,
    label: String,
    accentColor: Color
) {
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = label,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = label.trim().take(1).uppercase(Locale.getDefault()).ifBlank { "?" },
                style = TextStyle(
                    color = accentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun EmptyViewersState(
    contentColor: Color,
    accentColor: Color,
    isDarkSurface: Boolean
) {
    SurfaceCard(
        contentColor = contentColor,
        isDarkSurface = isDarkSurface,
        accentColor = accentColor
    ) {
        BasicText(
            text = "No profile viewers yet",
            style = TextStyle(
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = "When people open your profile, they’ll show up here with their latest visit time.",
            style = TextStyle(
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        )
    }
}

@Composable
private fun InlineInfoCard(
    title: String,
    body: String,
    contentColor: Color,
    accentColor: Color,
    isDarkSurface: Boolean
) {
    SurfaceCard(
        contentColor = contentColor,
        isDarkSurface = isDarkSurface,
        accentColor = accentColor
    ) {
        BasicText(
            text = title,
            style = TextStyle(
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(Modifier.height(4.dp))
        BasicText(
            text = body,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.68f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        )
    }
}

@Composable
private fun SurfaceCard(
    contentColor: Color,
    isDarkSurface: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (isDarkSurface) {
                    Color.White.copy(alpha = 0.07f)
                } else {
                    Color.White.copy(alpha = 0.82f)
                }
            )
            .border(
                1.dp,
                if (isDarkSurface) {
                    Color.White.copy(alpha = 0.1f)
                } else {
                    accentColor.copy(alpha = 0.08f)
                },
                RoundedCornerShape(22.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
private fun InfoChip(
    text: String,
    textColor: Color,
    background: Color,
    borderColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

private fun relativeTime(timestamp: String): String {
    val instant = runCatching { Instant.parse(timestamp) }.getOrNull() ?: return "Just now"
    val now = Instant.now()
    val duration = Duration.between(instant, now)

    return when {
        duration.isNegative || duration.toMinutes() < 1 -> "Just now"
        duration.toHours() < 1 -> "${duration.toMinutes()}m ago"
        duration.toDays() < 1 -> "${duration.toHours()}h ago"
        duration.toDays() < 7 -> "${duration.toDays()}d ago"
        duration.toDays() < 30 -> "${duration.toDays() / 7}w ago"
        duration.toDays() < 365 -> "${duration.toDays() / 30}mo ago"
        else -> "${duration.toDays() / 365}y ago"
    }
}

private fun absoluteTime(timestamp: String): String {
    val instant = runCatching { Instant.parse(timestamp) }.getOrNull() ?: return timestamp
    return DateTimeFormatter.ofPattern("dd MMM, h:mm a")
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)
}
