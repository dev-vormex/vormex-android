package com.kyant.backdrop.catalog.linkedin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.catalog.R
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.models.CollegeInfo
import com.kyant.backdrop.catalog.network.models.NearbyUser
import com.kyant.backdrop.catalog.network.models.PersonInfo
import com.kyant.backdrop.catalog.network.models.SmartMatch
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ==================== Main FindPeople Screen ====================

@Composable
fun FindPeopleScreenNew(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    findPeopleViewModel: FindPeopleViewModel,
    onNavigateToProfile: (String) -> Unit = {},
    reduceAnimations: Boolean = false
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val retentionViewModel: RetentionViewModel = viewModel(factory = RetentionViewModel.Factory(context))
    val uiState by findPeopleViewModel.uiState.collectAsState()
    val retentionState by retentionViewModel.uiState.collectAsState()
    
    // Theme preference: "glass", "light", "dark"
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val appearance = currentVormexAppearance(themeMode)
    val isGlassTheme = appearance.isGlassTheme
    val isLightTheme = appearance.isLightTheme
    // One shimmer for all Find skeletons (avoids N× infinite transitions).
    val listShimmerBrush = findPeopleShimmerBrush(isLightTheme)
    
    val isScrolled = false
    val hideTabsForSearch =
        uiState.selectedTab == FindPeopleTab.ALL_PEOPLE && uiState.searchQuery.isNotBlank()
    val navigateToProfile: (String) -> Unit = { userId ->
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onNavigateToProfile(userId)
    }
    
    // Clear any existing errors when this screen is opened
    LaunchedEffect(Unit) {
        findPeopleViewModel.clearAllErrors()
        findPeopleViewModel.ensureFindSurfaceLoaded()
        retentionViewModel.ensureConnectionLimitLoaded()
    }

    LaunchedEffect(uiState.selectedTab) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }
    
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            FindPeopleControlsCard(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isLightTheme = isLightTheme,
                reduceAnimations = reduceAnimations,
                selectedTab = uiState.selectedTab,
                onTabSelected = { findPeopleViewModel.selectTab(it) },
                searchQuery = uiState.searchQuery,
                normalizedQuery = uiState.searchQuery.trim().lowercase(),
                isLoading = uiState.isLoadingAllPeople,
                displayMode = uiState.allPeopleDisplayMode,
                filterOptions = uiState.filterOptions,
                selectedCollege = uiState.selectedCollege,
                selectedBranch = uiState.selectedBranch,
                selectedGraduationYear = uiState.selectedGraduationYear,
                isFilterExpanded = uiState.isFilterExpanded,
                onSearchQueryChange = { findPeopleViewModel.updateSearchQuery(it) },
                onToggleFilter = { findPeopleViewModel.toggleFilterExpanded() },
                onDisplayModeSelected = { findPeopleViewModel.setAllPeopleDisplayMode(it) },
                onCollegeSelected = { findPeopleViewModel.setCollegeFilter(it) },
                onBranchSelected = { findPeopleViewModel.setBranchFilter(it) },
                onYearSelected = { findPeopleViewModel.setGraduationYearFilter(it) },
                onClearFilters = { findPeopleViewModel.clearFilters() }
            )

            Spacer(Modifier.height(8.dp))
            
            // Connection Limit Indicator (only show if not scrolled)
            AnimatedVisibility(visible = !isScrolled) {
                Column {
                    ConnectionLimitIndicator(
                        limitData = retentionState.connectionLimit,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            
            // Variable Rewards Section (collapse when scrolled)
            AnimatedVisibility(visible = !isScrolled) {
                VariableRewardsSection(
                    // Daily Matches
                    dailyMatches = uiState.dailyMatches,
                    dailyMatchCount = uiState.dailyMatchCount,
                    surpriseMessage = uiState.surpriseMessage,
                    showDailyMatchesBanner = uiState.showDailyMatchesBanner,
                    // Hidden Gem
                    hiddenGem = uiState.hiddenGem,
                    hiddenGemMessage = uiState.hiddenGemMessage,
                    showHiddenGemCard = uiState.showHiddenGemCard,
                    // Trending
                    isTrending = uiState.isTrending,
                    trendingRank = uiState.trendingRank,
                    trendingViewsToday = uiState.trendingViewsToday,
                    trendingMessage = uiState.trendingMessage,
                    showTrendingBanner = uiState.showTrendingBanner,
                    // Backdrop & styling
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    // Actions
                    onMatchClick = navigateToProfile,
                    onHiddenGemViewProfile = { 
                        uiState.hiddenGem?.id?.let(navigateToProfile)
                    },
                    onHiddenGemConnect = {
                        uiState.hiddenGem?.id?.let { findPeopleViewModel.sendConnectionRequest(it) }
                    },
                    onViewTrendingStats = { /* TODO: Navigate to profile stats */ },
                    onDismissDailyMatches = { findPeopleViewModel.dismissDailyMatchesBanner() },
                    onDismissHiddenGem = { findPeopleViewModel.dismissHiddenGemCard() },
                    onDismissTrending = { findPeopleViewModel.dismissTrendingBanner() }
                )
            }
            
            // Content based on selected tab
            when (uiState.selectedTab) {
                FindPeopleTab.PEOPLE_YOU_KNOW -> PeopleYouKnowContent(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isGlassTheme = isGlassTheme,
                    isLightTheme = isLightTheme,
                    uiState = uiState,
                    viewModel = findPeopleViewModel,
                    onNavigateToProfile = navigateToProfile,
                    reduceAnimations = reduceAnimations
                )

                FindPeopleTab.SMART_MATCHES -> SmartMatchesContent(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isGlassTheme = isGlassTheme,
                    isLightTheme = isLightTheme,
                    listShimmerBrush = listShimmerBrush,
                    reduceAnimations = reduceAnimations,
                    matches = uiState.smartMatches,
                    isLoading = uiState.isLoadingSmartMatches,
                    error = uiState.smartMatchError,
                    selectedFilter = uiState.smartMatchFilter,
                    onFilterSelected = { findPeopleViewModel.setSmartMatchFilter(it) },
                    onNavigateToProfile = navigateToProfile,
                    onRetry = { findPeopleViewModel.loadSmartMatches(forceRefresh = true) },
                    onDismissError = { findPeopleViewModel.dismissErrorsWithCooldown() }
                )
            
            FindPeopleTab.ALL_PEOPLE -> AllPeopleContent(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isLightTheme = isLightTheme,
                listShimmerBrush = listShimmerBrush,
                reduceAnimations = reduceAnimations,
                people = uiState.allPeople,
                isLoading = uiState.isLoadingAllPeople,
                error = uiState.allPeopleError,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { findPeopleViewModel.updateSearchQuery(it) },
                displayMode = uiState.allPeopleDisplayMode,
                filterOptions = uiState.filterOptions,
                selectedCollege = uiState.selectedCollege,
                selectedBranch = uiState.selectedBranch,
                selectedGraduationYear = uiState.selectedGraduationYear,
                isFilterExpanded = uiState.isFilterExpanded,
                onToggleFilter = { findPeopleViewModel.toggleFilterExpanded() },
                onDisplayModeSelected = { findPeopleViewModel.setAllPeopleDisplayMode(it) },
                onCollegeSelected = { findPeopleViewModel.setCollegeFilter(it) },
                onBranchSelected = { findPeopleViewModel.setBranchFilter(it) },
                onYearSelected = { findPeopleViewModel.setGraduationYearFilter(it) },
                onClearFilters = { findPeopleViewModel.clearFilters() },
                hasMore = uiState.hasMoreAllPeople,
                onLoadMore = { findPeopleViewModel.loadMorePeople() },
                connectionActionInProgress = uiState.connectionActionInProgress,
                onConnect = { findPeopleViewModel.sendConnectionRequest(it) },
                onNavigateToProfile = navigateToProfile,
                onRetry = { findPeopleViewModel.loadAllPeople(resetPage = true, forceRefresh = true) },
                onDismissError = { findPeopleViewModel.dismissErrorsWithCooldown() }
            )
            
            FindPeopleTab.FOR_YOU -> ForYouContent(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isLightTheme = isLightTheme,
                listShimmerBrush = listShimmerBrush,
                reduceAnimations = reduceAnimations,
                people = uiState.suggestions,
                isLoading = uiState.isLoadingSuggestions,
                error = uiState.suggestionsError,
                connectionActionInProgress = uiState.connectionActionInProgress,
                onConnect = { findPeopleViewModel.sendConnectionRequest(it) },
                onNavigateToProfile = navigateToProfile,
                onRetry = { findPeopleViewModel.loadSuggestions(forceRefresh = true) },
                onDismissError = { findPeopleViewModel.dismissErrorsWithCooldown() }
            )
            
            FindPeopleTab.SAME_CAMPUS -> SameCampusContent(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isLightTheme = isLightTheme,
                listShimmerBrush = listShimmerBrush,
                reduceAnimations = reduceAnimations,
                people = uiState.sameCampusPeople,
                isLoading = uiState.isLoadingSameCampus,
                error = uiState.sameCampusError,
                userCollege = uiState.userCollege,
                isSavingCollege = uiState.isSavingCollege,
                collegeSuggestions = uiState.collegeSuggestions,
                isSearchingColleges = uiState.isSearchingColleges,
                connectionActionInProgress = uiState.connectionActionInProgress,
                onConnect = { findPeopleViewModel.sendConnectionRequest(it) },
                onNavigateToProfile = navigateToProfile,
                onRetry = { findPeopleViewModel.loadSameCampus(forceRefresh = true) },
                onSaveCollege = { findPeopleViewModel.saveCollege(it) },
                onCollegeSearch = { findPeopleViewModel.searchColleges(it) },
                onDismissError = { findPeopleViewModel.dismissErrorsWithCooldown() }
            )
            
            FindPeopleTab.NEARBY -> NearbyContent(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isLightTheme = isLightTheme,
                listShimmerBrush = listShimmerBrush,
                reduceAnimations = reduceAnimations,
                nearbyPeople = uiState.nearbyPeople,
                isLoading = uiState.isLoadingNearby,
                error = uiState.nearbyError,
                currentLat = uiState.currentLat,
                currentLng = uiState.currentLng,
                currentCity = uiState.currentCity,
                selectedRadius = uiState.selectedRadius,
                hasLocationPermission = uiState.hasLocationPermission,
                onPermissionGranted = { findPeopleViewModel.setLocationPermission(true) },
                onLocationUpdate = { lat, lng, acc -> findPeopleViewModel.updateLocation(lat, lng, acc) },
                onRadiusChange = { findPeopleViewModel.setRadius(it) },
                onNavigateToProfile = navigateToProfile,
                onRefresh = { findPeopleViewModel.loadNearbyPeople(forceRefresh = true) }
            )
        }
        }
        
        // Streak status banner (Duolingo Effect: Fear of loss)
        // Only show when streak is at risk - not just for having a streak
        if (uiState.isStreakAtRisk && uiState.connectionStreak > 0) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp) // Below header
            ) {
                StreakStatusBanner(
                    connectionStreak = uiState.connectionStreak,
                    isAtRisk = uiState.isStreakAtRisk,
                    backdrop = backdrop,
                    onTap = { /* Navigate to streak details */ },
                    onDismiss = { findPeopleViewModel.dismissErrorsWithCooldown() }
                )
            }
        }
        
    }
}

// ==================== Header ====================

// ==================== Tabs ====================

@Composable
private fun FindPeopleControlsCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    reduceAnimations: Boolean,
    selectedTab: FindPeopleTab,
    onTabSelected: (FindPeopleTab) -> Unit,
    searchQuery: String,
    normalizedQuery: String,
    isLoading: Boolean,
    displayMode: AllPeopleDisplayMode,
    filterOptions: com.kyant.backdrop.catalog.network.models.FilterOptions,
    selectedCollege: String?,
    selectedBranch: String?,
    selectedGraduationYear: Int?,
    isFilterExpanded: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleFilter: () -> Unit,
    onDisplayModeSelected: (AllPeopleDisplayMode) -> Unit,
    onCollegeSelected: (String?) -> Unit,
    onBranchSelected: (String?) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onClearFilters: () -> Unit
) {
    val activeFilterCount = listOf(selectedCollege, selectedBranch, selectedGraduationYear)
        .count { it != null }
    val activeFilters = listOfNotNull(
        selectedCollege?.let { "College · $it" to { onCollegeSelected(null) } },
        selectedBranch?.let { "Branch · $it" to { onBranchSelected(null) } },
        selectedGraduationYear?.let { "Year · $it" to { onYearSelected(null) } }
    )
    val showAllControls = selectedTab == FindPeopleTab.ALL_PEOPLE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .vormexSurface(
                backdrop = backdrop,
                tone = VormexSurfaceTone.Sheet,
                cornerRadius = 20.dp,
                blurRadius = 16.dp,
                lensRadius = 0.dp,
                lensDepth = 0.dp,
                useBackdropEffects = isGlassTheme && !reduceAnimations,
                surfaceColor = when {
                    isLightTheme && isGlassTheme -> Color.White.copy(alpha = 0.72f)
                    isLightTheme -> Color(0xFFF8FAFD)
                    else -> Color.White.copy(alpha = 0.055f)
                },
                borderColor = if (isLightTheme) Color(0xFFD9E2EF) else Color.White.copy(alpha = 0.08f)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FindPeopleTabs(
            contentColor = contentColor,
            accentColor = accentColor,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )

        if (showAllControls) {
            AllPeopleSearchControls(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isLightTheme = isLightTheme,
                reduceAnimations = reduceAnimations,
                searchQuery = searchQuery,
                normalizedQuery = normalizedQuery,
                isLoading = isLoading,
                activeFilterCount = activeFilterCount,
                isFilterExpanded = isFilterExpanded,
                onSearchQueryChange = onSearchQueryChange,
                onToggleFilter = onToggleFilter
            )

            if (activeFilters.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeFilters.forEach { (label, onRemove) ->
                        ActiveFilterPill(
                            label = label,
                            accentColor = accentColor,
                            onRemove = onRemove
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(contentColor.copy(alpha = 0.06f))
                            .clickable(onClick = onClearFilters)
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                    ) {
                        BasicText(
                            "Reset",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.68f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isFilterExpanded,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(160)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(120))
            ) {
                FilterPanel(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    filterOptions = filterOptions,
                    displayMode = displayMode,
                    selectedCollege = selectedCollege,
                    selectedBranch = selectedBranch,
                    selectedGraduationYear = selectedGraduationYear,
                    isGlassTheme = isGlassTheme,
                    isLightTheme = isLightTheme,
                    reduceAnimations = reduceAnimations,
                    onDisplayModeSelected = onDisplayModeSelected,
                    onCollegeSelected = onCollegeSelected,
                    onBranchSelected = onBranchSelected,
                    onYearSelected = onYearSelected,
                    onClearFilters = onClearFilters
                )
            }
        }
    }
}

@Composable
private fun FindPeopleTabs(
    contentColor: Color,
    accentColor: Color,
    selectedTab: FindPeopleTab,
    onTabSelected: (FindPeopleTab) -> Unit
) {
    val tabs = remember {
        listOf(
            FindPeopleTabSpec(
                tab = FindPeopleTab.ALL_PEOPLE,
                label = "All",
                icon = { color -> FooterFindIcon(color = color, size = 16.dp) }
            ),
            FindPeopleTabSpec(
                tab = FindPeopleTab.SMART_MATCHES,
                label = "Smart",
                icon = { color -> ZapIcon(color = color, size = 16.dp) }
            ),
            FindPeopleTabSpec(
                tab = FindPeopleTab.FOR_YOU,
                label = "For You",
                icon = { color -> SparkleIcon(color = color, size = 16.dp) }
            ),
            FindPeopleTabSpec(
                tab = FindPeopleTab.SAME_CAMPUS,
                label = "Campus",
                icon = { color -> GraduationCapIcon(color = color, size = 16.dp) }
            ),
            FindPeopleTabSpec(
                tab = FindPeopleTab.NEARBY,
                label = "Nearby",
                icon = { color -> LocationPinIcon(color = color, size = 16.dp) }
            ),
            FindPeopleTabSpec(
                tab = FindPeopleTab.PEOPLE_YOU_KNOW,
                label = "Known",
                icon = { color -> UsersIcon(color = color, size = 16.dp) }
            )
        )
    }

    LazyRow(
        Modifier
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tabs, key = { it.tab.name }) { tabSpec ->
            FindPeopleTabItem(
                icon = tabSpec.icon,
                label = tabSpec.label,
                isSelected = selectedTab == tabSpec.tab,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = { onTabSelected(tabSpec.tab) }
            )
        }
    }
}

private data class FindPeopleTabSpec(
    val tab: FindPeopleTab,
    val label: String,
    val icon: @Composable (Color) -> Unit
)

@Composable
private fun FindPeopleVectorTabIcon(
    drawableRes: Int,
    color: Color,
    size: androidx.compose.ui.unit.Dp = 14.dp
) {
    androidx.compose.foundation.Image(
        painter = painterResource(drawableRes),
        contentDescription = null,
        modifier = Modifier.size(size),
        colorFilter = ColorFilter.tint(color)
    )
}

@Composable
private fun FindPeopleTabItem(
    icon: @Composable (Color) -> Unit,
    label: String,
    isSelected: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val foregroundColor = if (isSelected) accentColor else contentColor.copy(alpha = 0.68f)
    val iconColor = if (isSelected) accentColor else contentColor.copy(alpha = 0.52f)
    
    Box(
        Modifier
            .widthIn(min = 70.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon(iconColor)

            BasicText(
                label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = foregroundColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            )
        }
    }
}

// ==================== Shimmer/Skeleton ====================

@Composable
private fun findPeopleShimmerBrush(isLightTheme: Boolean): Brush {
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

/** Glass blur is expensive; use a flat surface when [reduceAnimations] is enabled. */
private fun Modifier.findPeopleGlassSurface(
    backdrop: LayerBackdrop,
    cornerDp: Float,
    blurDp: Float,
    isLightTheme: Boolean,
    reduceAnimations: Boolean,
    onDrawSurfaceColor: Color = Color.White.copy(alpha = 0.1f)
): Modifier {
    return this.vormexSurface(
        backdrop = backdrop,
        tone = VormexSurfaceTone.Card,
        cornerRadius = cornerDp.dp,
        blurRadius = blurDp.dp,
        lensRadius = 0.dp,
        lensDepth = 0.dp,
        useBackdropEffects = !reduceAnimations
    )
}

@Composable
private fun PersonCardSkeleton(
    shimmerBrush: Brush,
    isLightTheme: Boolean
) {
    
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isLightTheme) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.04f))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top row: Avatar + Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )
                
                Spacer(Modifier.width(8.dp))
                
                Column {
                    Box(
                        Modifier
                            .width(100.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .width(70.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }
            }
            
            // Headline
            Box(
                Modifier
                    .fillMaxWidth(0.9f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            
            // Skills row
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        Modifier
                            .width(50.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )
                }
            }
            
            // Connect button
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

@Composable
private fun SmartMatchCardSkeleton(
    shimmerBrush: Brush,
    isLightTheme: Boolean
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isLightTheme) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.04f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush)
            )
            
            Spacer(Modifier.width(10.dp))
            
            Column(Modifier.weight(1f)) {
                // Name + badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(100.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .width(35.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(shimmerBrush)
                    )
                }
                
                Spacer(Modifier.height(6.dp))
                
                // Details
                Box(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                
                Spacer(Modifier.height(6.dp))
                
                // Tags
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(2) {
                        Box(
                            Modifier
                                .width(60.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
            
            Spacer(Modifier.width(8.dp))
            
            // View button
            Box(
                Modifier
                    .width(50.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

// ==================== PersonCard Component ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonCard(
    person: PersonInfo,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean = false,
    isLightTheme: Boolean = true,
    isActionInProgress: Boolean = false,
    onConnect: () -> Unit = {},
    onCardClick: () -> Unit = {},
    reduceAnimations: Boolean = false
) {
    val imageCrossfadeMs = if (reduceAnimations) 0 else 300
    val displayName = person.name ?: person.username ?: "Unknown"
    val handle = person.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
    val statusLabel = when {
        person.isInContacts -> "Contacts"
        person.connectionStatus == "connected" -> "Connected"
        person.connectionStatus == "pending_received" -> "Reply"
        person.isOnline -> "Active now"
        else -> null
    }
    val statusBackground = when (person.connectionStatus) {
        "connected" -> contentColor.copy(alpha = 0.10f)
        "pending_sent" -> Color(0xFFF59E0B).copy(alpha = 0.18f)
        "pending_received" -> Color(0xFF22C55E).copy(alpha = 0.18f)
        else -> accentColor.copy(alpha = 0.14f)
    }
    val statusContent = when (person.connectionStatus) {
        "connected" -> contentColor.copy(alpha = 0.72f)
        "pending_sent" -> Color(0xFFD97706)
        "pending_received" -> Color(0xFF15803D)
        else -> accentColor
    }
    val subtitle = listOfNotNull(person.college, person.branch).joinToString(" · ")
    val supportingLine = handle ?: person.headline ?: subtitle.takeIf { it.isNotBlank() }
    val isConnectedSurface = person.isInContacts || person.connectionStatus == "connected"
    val cardSurfaceColor = when {
        isConnectedSurface && isLightTheme && isGlassTheme -> Color.White.copy(alpha = 0.72f)
        isConnectedSurface && isLightTheme -> Color(0xFFF2F7FF)
        isConnectedSurface -> accentColor.copy(alpha = 0.14f)
        isLightTheme && isGlassTheme -> Color.White.copy(alpha = 0.68f)
        isLightTheme -> Color(0xFFF8FAFD)
        else -> Color.White.copy(alpha = 0.06f)
    }
    val cardBorderColor = when {
        isConnectedSurface && isLightTheme -> Color(0xFFC9DAF2)
        isLightTheme -> Color(0xFFD9E2EF)
        else -> Color.White.copy(alpha = 0.08f)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .vormexSurface(
                backdrop = backdrop,
                tone = if (person.isInContacts || person.connectionStatus == "connected") {
                    VormexSurfaceTone.Selected
                } else {
                    VormexSurfaceTone.Card
                },
                cornerRadius = 18.dp,
                blurRadius = 16.dp,
                lensRadius = 0.dp,
                lensDepth = 0.dp,
                useBackdropEffects = isGlassTheme && !reduceAnimations,
                surfaceColor = cardSurfaceColor,
                borderColor = cardBorderColor
            )
            .clickable(onClick = onCardClick)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!person.profileImage.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(person.profileImage)
                                    .crossfade(imageCrossfadeMs)
                                    .build(),
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            val initials = displayName
                                .split(" ")
                                .mapNotNull { it.firstOrNull()?.uppercase() }
                                .take(2)
                                .joinToString("")
                            BasicText(
                                initials,
                                style = TextStyle(accentColor, 14.sp, FontWeight.SemiBold)
                            )
                        }
                    }

                    if (person.isOnline) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isLightTheme) Color.White else Color(0xFF15181D))
                                .padding(2.dp)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BasicText(
                            displayName,
                            style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        VerificationBadge(
                            verified = person.hasVerificationBadge(),
                            size = VerificationBadgeSize.Small
                        )
                    }

                    supportingLine?.takeIf { it.isNotBlank() }?.let { line ->
                        BasicText(
                            line,
                            style = TextStyle(contentColor.copy(alpha = 0.58f), 11.sp, FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (statusLabel != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusBackground)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        BasicText(
                            statusLabel,
                            style = TextStyle(
                                color = statusContent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            if (!person.headline.isNullOrBlank() && person.headline != supportingLine) {
                BasicText(
                    person.headline,
                    style = TextStyle(contentColor.copy(alpha = 0.70f), 12.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (subtitle.isNotBlank() && subtitle != supportingLine) {
                BasicText(
                    subtitle,
                    style = TextStyle(contentColor.copy(alpha = 0.54f), 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (person.skills.isNotEmpty() || person.interests.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    person.skills.take(2).forEach { skill ->
                        SkillChip(skill, contentColor, accentColor)
                    }
                    if (person.skills.isEmpty()) {
                        person.interests.take(2).forEach { interest ->
                            InterestChip(interest, accentColor)
                        }
                    } else if (person.skills.size > 2) {
                        SkillChip("+${person.skills.size - 2}", contentColor, accentColor)
                    } else if (person.interests.isNotEmpty()) {
                        InterestChip(person.interests.first(), accentColor)
                    }
                }
            }

            ConnectionButton(
                status = person.connectionStatus,
                contentColor = contentColor,
                accentColor = accentColor,
                isLoading = isActionInProgress,
                onConnect = onConnect
            )
        }
    }
}

@Composable
private fun SkillChip(text: String, contentColor: Color, accentColor: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        BasicText(
            text,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.72f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun InterestChip(text: String, accentColor: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        BasicText(
            text,
            style = TextStyle(accentColor, 10.sp, FontWeight.Medium)
        )
    }
}

@Composable
private fun ConnectionButton(
    status: String,
    contentColor: Color,
    accentColor: Color,
    isLoading: Boolean,
    onConnect: () -> Unit
) {
    val (text, bgColor, textColor, enabled) = when (status) {
        "connected" -> listOf("Connected", contentColor.copy(alpha = 0.10f), contentColor.copy(alpha = 0.56f), false)
        "pending_sent" -> listOf("Pending", Color(0xFFF59E0B).copy(alpha = 0.18f), Color(0xFFD97706), true)
        "pending_received" -> listOf("Accept", Color(0xFF22C55E).copy(alpha = 0.18f), Color(0xFF15803D), true)
        else -> listOf("Connect", accentColor.copy(alpha = 0.18f), accentColor, true)
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor as Color)
            .clickable(enabled = enabled as Boolean && !isLoading, onClick = onConnect)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = textColor as Color,
                strokeWidth = 2.dp
            )
        } else {
            BasicText(
                text as String,
                style = TextStyle(textColor as Color, 12.sp, FontWeight.SemiBold)
            )
        }
    }
}

// ==================== Smart Match Card ====================

@Composable
fun SmartMatchCard(
    match: SmartMatch,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean = false,
    isLightTheme: Boolean = true,
    onViewClick: () -> Unit = {},
    reduceAnimations: Boolean = false
) {
    val imageCrossfadeMs = if (reduceAnimations) 0 else 300
    val percentageColor = when {
        match.matchPercentage >= 60 -> Color(0xFF22C55E) // Green
        match.matchPercentage >= 35 -> Color(0xFF3B82F6) // Blue
        else -> Color(0xFFF97316) // Orange
    }
    val matchFitLabel = when {
        match.matchPercentage >= 60 -> "Strong fit"
        match.matchPercentage >= 35 -> "Good fit"
        else -> "Fresh fit"
    }
    val displayName = match.user.name ?: match.user.username ?: "Unknown"
    val details = buildList {
        match.user.college?.let { add(it) }
        match.user.onboarding?.primaryGoal?.let { add(mapPrimaryGoalToDisplay(it)) }
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .vormexSurface(
                backdrop = backdrop,
                tone = VormexSurfaceTone.Card,
                cornerRadius = 18.dp,
                blurRadius = 12.dp,
                lensRadius = 0.dp,
                lensDepth = 0.dp,
                useBackdropEffects = isGlassTheme && !reduceAnimations,
                surfaceColor = when {
                    isLightTheme && isGlassTheme -> Color.White.copy(alpha = 0.68f)
                    isLightTheme -> Color(0xFFF8FAFD)
                    else -> Color.White.copy(alpha = 0.055f)
                },
                borderColor = if (isLightTheme) {
                    Color(0xFFD9E2EF)
                } else {
                    Color.White.copy(alpha = 0.08f)
                }
            )
            .clickable(onClick = onViewClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (!match.user.profileImage.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(match.user.profileImage)
                            .crossfade(imageCrossfadeMs)
                            .build(),
                        contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                    )
                } else {
                    val initials = displayName
                        .split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .take(2)
                        .joinToString("")
                    BasicText(
                        initials,
                        style = TextStyle(accentColor, 15.sp, FontWeight.SemiBold)
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText(
                        displayName,
                        style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    VerificationBadge(
                        verified = match.user.hasVerificationBadge(),
                        size = VerificationBadgeSize.Small
                    )

                    Box(
                        Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(percentageColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        BasicText(
                            matchFitLabel,
                            style = TextStyle(percentageColor, 10.sp, FontWeight.SemiBold),
                            maxLines = 1
                        )
                    }
                }
                
                if (details.isNotEmpty()) {
                    BasicText(
                        details.joinToString(" · "),
                        style = TextStyle(contentColor.copy(alpha = 0.58f), 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (match.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        match.tags.take(3).forEach { tag ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(accentColor.copy(alpha = 0.08f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                BasicText(
                                    tag,
                                    style = TextStyle(contentColor.copy(alpha = 0.72f), 10.sp, FontWeight.Medium),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.width(10.dp))
            
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .clickable(onClick = onViewClick)
                    .padding(horizontal = 11.dp, vertical = 7.dp)
            ) {
                BasicText(
                    "View",
                    style = TextStyle(accentColor, 12.sp, FontWeight.SemiBold)
                )
            }
        }
    }
}

// ==================== Tab Contents ====================

@Composable
private fun SmartMatchesContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    listShimmerBrush: Brush,
    reduceAnimations: Boolean,
    matches: List<SmartMatch>,
    isLoading: Boolean,
    error: String?,
    selectedFilter: SmartMatchFilter,
    onFilterSelected: (SmartMatchFilter) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onRetry: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize()) {
        // Sub-filters
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                SmartMatchFilter.ALL to "Best Matches",
                SmartMatchFilter.SAME_CAMPUS to "Same Campus",
                SmartMatchFilter.SAME_GOAL to "Same Goal",
                SmartMatchFilter.FIND_MENTOR to "Find Mentor"
            ).forEach { (filter, label) ->
                val isSelected = selectedFilter == filter
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.2f)
                            else contentColor.copy(alpha = 0.08f)
                        )
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        label,
                        style = TextStyle(
                            if (isSelected) accentColor else contentColor.copy(alpha = 0.7f),
                            12.sp,
                            if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Content
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isLoading -> {
                    items(count = 4, key = { "smart_match_sk_$it" }) {
                        SmartMatchCardSkeleton(listShimmerBrush, isLightTheme)
                    }
                }
                error != null -> {
                    item {
                        ErrorState(
                            message = error,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            reduceAnimations = reduceAnimations,
                            onRetry = onRetry,
                            onDismiss = onDismissError
                        )
                    }
                }
                matches.isEmpty() -> {
                    item {
                        EmptyState(
                            iconRes = R.drawable.ic_search,
                            title = "No matches found",
                            subtitle = "Complete your profile and add interests to get matched",
                            backdrop = backdrop,
                            contentColor = contentColor
                        )
                    }
                }
                else -> {
                    items(matches, key = { it.user.id }) { match ->
                        SmartMatchCard(
                            match = match,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isGlassTheme = isGlassTheme,
                            isLightTheme = isLightTheme,
                            reduceAnimations = reduceAnimations,
                            onViewClick = { onNavigateToProfile(match.user.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllPeopleContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    listShimmerBrush: Brush,
    reduceAnimations: Boolean,
    people: List<PersonInfo>,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    displayMode: AllPeopleDisplayMode,
    filterOptions: com.kyant.backdrop.catalog.network.models.FilterOptions,
    selectedCollege: String?,
    selectedBranch: String?,
    selectedGraduationYear: Int?,
    isFilterExpanded: Boolean,
    onToggleFilter: () -> Unit,
    onDisplayModeSelected: (AllPeopleDisplayMode) -> Unit,
    onCollegeSelected: (String?) -> Unit,
    onBranchSelected: (String?) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onClearFilters: () -> Unit,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    connectionActionInProgress: Set<String>,
    onConnect: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onRetry: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    val normalizedQuery = searchQuery.trim().lowercase()
    val gridColumns = GridCells.Fixed(displayMode.columns)
    val displayedPeople = remember(people, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            people
        } else {
            people.filter { person ->
                person.name.orEmpty().lowercase().contains(normalizedQuery) ||
                    person.username.orEmpty().lowercase().contains(normalizedQuery) ||
                    person.headline.orEmpty().lowercase().contains(normalizedQuery) ||
                    person.bio.orEmpty().lowercase().contains(normalizedQuery) ||
                    person.college.orEmpty().lowercase().contains(normalizedQuery) ||
                    person.branch.orEmpty().lowercase().contains(normalizedQuery) ||
                    person.skills.any { it.lowercase().contains(normalizedQuery) } ||
                    person.interests.any { it.lowercase().contains(normalizedQuery) }
            }
        }
    }
    val showInitialLoading = isLoading && people.isEmpty()

    Column(Modifier.fillMaxSize()) {
        when {
            showInitialLoading -> {
                LazyVerticalGrid(
                    columns = gridColumns,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(count = 8, key = { "all_people_sk_$it" }) {
                        PersonCardSkeleton(listShimmerBrush, isLightTheme)
                    }
                }
            }
            error != null -> {
                ErrorState(
                    message = error,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    reduceAnimations = reduceAnimations,
                    onRetry = onRetry,
                    onDismiss = onDismissError
                )
            }
            displayedPeople.isEmpty() -> {
                EmptyState(
                    iconRes = R.drawable.ic_users,
                    title = "No people found",
                    subtitle = if (normalizedQuery.isNotBlank() && isLoading) {
                        "Looking for matches across the network..."
                    } else if (normalizedQuery.isNotBlank()) {
                        "Try another name, username, college, or skill."
                    } else {
                        "Try adjusting your search or filters"
                    },
                    backdrop = backdrop,
                    contentColor = contentColor
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = gridColumns,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = displayedPeople, key = { it.id }) { person ->
                        PersonCard(
                            person = person,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isGlassTheme = isGlassTheme,
                            isLightTheme = isLightTheme,
                            isActionInProgress = connectionActionInProgress.contains(person.id),
                            onConnect = { onConnect(person.id) },
                            onCardClick = { onNavigateToProfile(person.id) },
                            reduceAnimations = reduceAnimations
                        )
                    }

                    if (hasMore) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            LaunchedEffect(displayedPeople.size, hasMore, normalizedQuery) {
                                onLoadMore()
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = accentColor
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
private fun AllPeopleSearchControls(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    reduceAnimations: Boolean,
    searchQuery: String,
    normalizedQuery: String,
    isLoading: Boolean,
    activeFilterCount: Int,
    isFilterExpanded: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleFilter: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(1f)
                .vormexSurface(
                    backdrop = backdrop,
                    tone = VormexSurfaceTone.Sheet,
                    cornerRadius = 20.dp,
                    blurRadius = 16.dp,
                    lensRadius = 0.dp,
                    lensDepth = 0.dp,
                    useBackdropEffects = isGlassTheme && !reduceAnimations,
                    surfaceColor = when {
                        isLightTheme && isGlassTheme -> Color.White.copy(alpha = 0.74f)
                        isLightTheme -> Color.White
                        else -> Color.White.copy(alpha = 0.06f)
                    },
                    borderColor = if (isLightTheme) Color(0xFFD7E1EC) else Color.White.copy(alpha = 0.10f)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Search",
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.72f))
                )
                Spacer(Modifier.width(10.dp))

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(accentColor),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            BasicText(
                                "Search people, colleges, skills",
                                style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                )

                if (isLoading && normalizedQuery.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = accentColor,
                        strokeWidth = 2.dp
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                            .clickable { onSearchQueryChange("") }
                            .padding(4.dp)
                    ) {
                        BasicText(
                            "✕",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.55f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = searchQuery.isBlank()) {
            FilterToggleButton(
                contentColor = contentColor,
                accentColor = accentColor,
                isExpanded = isFilterExpanded,
                activeFilterCount = activeFilterCount,
                onClick = onToggleFilter
            )
        }
    }
}

@Composable
private fun FilterToggleButton(
    contentColor: Color,
    accentColor: Color,
    isExpanded: Boolean,
    activeFilterCount: Int,
    onClick: () -> Unit
) {
    val active = isExpanded || activeFilterCount > 0

    Box(
        Modifier
            .height(46.dp)
            .widthIn(min = 92.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (active) accentColor.copy(alpha = 0.12f)
                else contentColor.copy(alpha = 0.05f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            FilterSlidersGlyph(
                color = if (active) accentColor else contentColor.copy(alpha = 0.72f),
                modifier = Modifier.size(18.dp)
            )
            BasicText(
                "Filters",
                style = TextStyle(
                    color = if (active) accentColor else contentColor.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        if (activeFilterCount > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.92f))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                BasicText(
                    activeFilterCount.toString(),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun FilterSlidersGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val stroke = 1.8.dp.toPx()
        val knobRadius = 2.2.dp.toPx()
        val startX = 1.5.dp.toPx()
        val endX = size.width - 1.5.dp.toPx()
        val topY = size.height * 0.34f
        val bottomY = size.height * 0.68f

        drawLine(
            color = color,
            start = Offset(startX, topY),
            end = Offset(endX, topY),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(startX, bottomY),
            end = Offset(endX, bottomY),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawCircle(color = color, radius = knobRadius, center = Offset(size.width * 0.38f, topY))
        drawCircle(color = color, radius = knobRadius, center = Offset(size.width * 0.66f, bottomY))
    }
}

@Composable
private fun ActiveFilterPill(
    label: String,
    accentColor: Color,
    onRemove: () -> Unit
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .clickable(onClick = onRemove)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BasicText(
                label,
                style = TextStyle(
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            BasicText(
                "x",
                style = TextStyle(
                    color = accentColor.copy(alpha = 0.76f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun FilterPanel(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    filterOptions: com.kyant.backdrop.catalog.network.models.FilterOptions,
    displayMode: AllPeopleDisplayMode,
    selectedCollege: String?,
    selectedBranch: String?,
    selectedGraduationYear: Int?,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    reduceAnimations: Boolean,
    onDisplayModeSelected: (AllPeopleDisplayMode) -> Unit,
    onCollegeSelected: (String?) -> Unit,
    onBranchSelected: (String?) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onClearFilters: () -> Unit
) {
    val activeFilterCount = listOf(selectedCollege, selectedBranch, selectedGraduationYear).count { it != null }
    var isCollegeSheetVisible by remember { mutableStateOf(false) }
    var isBranchSheetVisible by remember { mutableStateOf(false) }
    var isYearSheetVisible by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .vormexSurface(
                backdrop = backdrop,
                tone = VormexSurfaceTone.Overlay,
                cornerRadius = 18.dp,
                blurRadius = 14.dp,
                lensRadius = 0.dp,
                lensDepth = 0.dp,
                useBackdropEffects = isGlassTheme && !reduceAnimations,
                surfaceColor = when {
                    isLightTheme && isGlassTheme -> Color.White.copy(alpha = 0.70f)
                    isLightTheme -> Color.White
                    else -> Color.White.copy(alpha = 0.05f)
                },
                borderColor = if (isLightTheme) Color(0xFFD9E2EF) else Color.White.copy(alpha = 0.08f)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                "Filters",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )

            if (activeFilterCount > 0) {
                BasicText(
                    "Clear",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.62f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(contentColor.copy(alpha = 0.06f))
                        .clickable(onClick = onClearFilters)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        FilterChipSection(
            label = "Display",
            selectedValue = when (displayMode) {
                AllPeopleDisplayMode.TWO_PER_ROW -> "2 cards"
                AllPeopleDisplayMode.ONE_PER_ROW -> "1 card"
            },
            contentColor = contentColor,
            accentColor = accentColor
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterOptionChip(
                        label = "2 cards",
                        isSelected = displayMode == AllPeopleDisplayMode.TWO_PER_ROW,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { onDisplayModeSelected(AllPeopleDisplayMode.TWO_PER_ROW) }
                    )
                }
                item {
                    FilterOptionChip(
                        label = "1 card",
                        isSelected = displayMode == AllPeopleDisplayMode.ONE_PER_ROW,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { onDisplayModeSelected(AllPeopleDisplayMode.ONE_PER_ROW) }
                    )
                }
            }
        }

        FilterSelectionCard(
            title = "College",
            value = selectedCollege,
            placeholder = "Choose your campus from a searchable list",
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = { isCollegeSheetVisible = true },
            icon = {
                GraduationCapIcon(color = accentColor, size = 18.dp)
            }
        )

        FilterSelectionCard(
            title = "Branch",
            value = selectedBranch,
            placeholder = "Pick a branch from the full list",
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = { isBranchSheetVisible = true },
            icon = {
                FindPeopleVectorTabIcon(
                    drawableRes = R.drawable.ic_list,
                    color = accentColor,
                    size = 18.dp
                )
            }
        )

        FilterSelectionCard(
            title = "Graduation year",
            value = selectedGraduationYear?.toString(),
            placeholder = "Choose the passing year",
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = { isYearSheetVisible = true },
            icon = {
                FindPeopleVectorTabIcon(
                    drawableRes = R.drawable.ic_calendar,
                    color = accentColor,
                    size = 18.dp
                )
            }
        )

        if (activeFilterCount > 0) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(contentColor.copy(alpha = 0.06f))
                    .clickable(onClick = onClearFilters)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "Reset filters",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.68f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }

    if (isCollegeSheetVisible) {
        CollegePickerSheet(
            colleges = filterOptions.colleges,
            selectedCollege = selectedCollege,
            contentColor = contentColor,
            accentColor = accentColor,
            isLightTheme = isLightTheme,
            onDismiss = { isCollegeSheetVisible = false },
            onSelected = {
                isCollegeSheetVisible = false
                onCollegeSelected(it)
            }
        )
    }

    if (isBranchSheetVisible) {
        BranchPickerSheet(
            branches = filterOptions.branches,
            selectedBranch = selectedBranch,
            contentColor = contentColor,
            accentColor = accentColor,
            isLightTheme = isLightTheme,
            onDismiss = { isBranchSheetVisible = false },
            onSelected = {
                isBranchSheetVisible = false
                onBranchSelected(it)
            }
        )
    }

    if (isYearSheetVisible) {
        GraduationYearPickerSheet(
            years = filterOptions.graduationYears,
            selectedYear = selectedGraduationYear,
            contentColor = contentColor,
            accentColor = accentColor,
            isLightTheme = isLightTheme,
            onDismiss = { isYearSheetVisible = false },
            onSelected = {
                isYearSheetVisible = false
                onYearSelected(it)
            }
        )
    }
}

@Composable
private fun FilterChipSection(
    label: String,
    selectedValue: String,
    contentColor: Color,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                label,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.58f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )

            BasicText(
                selectedValue,
                style = TextStyle(
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        content()
    }
}

@Composable
private fun FilterOptionChip(
    label: String,
    isSelected: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .widthIn(max = 180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.11f)
                else contentColor.copy(alpha = 0.045f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (isSelected) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
            BasicText(
                label,
                style = TextStyle(
                    color = if (isSelected) accentColor else contentColor.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterSelectionCard(
    title: String,
    value: String?,
    placeholder: String,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(contentColor.copy(alpha = 0.045f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        title,
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.50f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    BasicText(
                        value ?: placeholder,
                        style = TextStyle(
                            color = if (value != null) contentColor else contentColor.copy(alpha = 0.70f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            BasicText(
                if (value != null) "Edit" else "Select",
                style = TextStyle(
                    color = accentColor.copy(alpha = 0.88f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollegePickerSheet(
    colleges: List<String>,
    selectedCollege: String?,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    onDismiss: () -> Unit,
    onSelected: (String?) -> Unit
) {
    OptionPickerSheet(
        title = "Select college",
        subtitle = "Search the campus list and pick the one that fits best.",
        searchPlaceholder = "Search colleges",
        allLabel = "All colleges",
        options = colleges.distinct().sorted(),
        selectedOption = selectedCollege,
        contentColor = contentColor,
        accentColor = accentColor,
        isLightTheme = isLightTheme,
        onDismiss = onDismiss,
        onSelected = onSelected
    )
}

@Composable
private fun BranchPickerSheet(
    branches: List<String>,
    selectedBranch: String?,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    onDismiss: () -> Unit,
    onSelected: (String?) -> Unit
) {
    OptionPickerSheet(
        title = "Select branch",
        subtitle = "Browse the available branches and narrow the people list cleanly.",
        searchPlaceholder = "Search branches",
        allLabel = "All branches",
        options = branches.distinct().sorted(),
        selectedOption = selectedBranch,
        contentColor = contentColor,
        accentColor = accentColor,
        isLightTheme = isLightTheme,
        onDismiss = onDismiss,
        onSelected = onSelected
    )
}

@Composable
private fun GraduationYearPickerSheet(
    years: List<Int>,
    selectedYear: Int?,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    onDismiss: () -> Unit,
    onSelected: (Int?) -> Unit
) {
    OptionPickerSheet(
        title = "Select graduation year",
        subtitle = "Choose the year you want to focus on from the available graduating batches.",
        searchPlaceholder = "Search graduation year",
        allLabel = "All years",
        options = years.distinct().sortedDescending().map(Int::toString),
        selectedOption = selectedYear?.toString(),
        contentColor = contentColor,
        accentColor = accentColor,
        isLightTheme = isLightTheme,
        onDismiss = onDismiss,
        onSelected = { value ->
            onSelected(value?.toIntOrNull())
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionPickerSheet(
    title: String,
    subtitle: String,
    searchPlaceholder: String,
    allLabel: String,
    options: List<String>,
    selectedOption: String?,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    onDismiss: () -> Unit,
    onSelected: (String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val visibleOptions = remember(options, searchQuery) {
        val normalizedQuery = searchQuery.trim().lowercase()
        options.filter { option ->
            normalizedQuery.isBlank() || option.lowercase().contains(normalizedQuery)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isLightTheme) Color.White else Color(0xFF101114),
        contentColor = contentColor
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    title,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    subtitle,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.58f),
                        fontSize = 12.sp
                    )
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(contentColor.copy(alpha = 0.05f))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = searchPlaceholder,
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.72f))
                    )
                    Spacer(Modifier.width(10.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(contentColor, 14.sp),
                        cursorBrush = SolidColor(accentColor),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                BasicText(
                                    searchPlaceholder,
                                    style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(contentColor.copy(alpha = 0.08f))
                                .clickable { searchQuery = "" }
                                .padding(4.dp)
                        ) {
                            BasicText(
                                "x",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.55f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OptionSheetRow(
                        label = allLabel,
                        isSelected = selectedOption == null,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { onSelected(null) }
                    )
                }

                if (visibleOptions.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(contentColor.copy(alpha = 0.04f))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                "No results found for \"$searchQuery\"",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.58f),
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                } else {
                    items(visibleOptions, key = { it }) { option ->
                        OptionSheetRow(
                            label = option,
                            isSelected = option == selectedOption,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onClick = { onSelected(option) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionSheetRow(
    label: String,
    isSelected: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.10f)
                else contentColor.copy(alpha = 0.04f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                label,
                style = TextStyle(
                    color = if (isSelected) accentColor else contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
private fun ForYouContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    listShimmerBrush: Brush,
    reduceAnimations: Boolean,
    people: List<PersonInfo>,
    isLoading: Boolean,
    error: String?,
    connectionActionInProgress: Set<String>,
    onConnect: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onRetry: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    PeopleGridContent(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isGlassTheme = isGlassTheme,
        isLightTheme = isLightTheme,
        listShimmerBrush = listShimmerBrush,
        reduceAnimations = reduceAnimations,
        people = people,
        isLoading = isLoading,
        error = error,
        emptyIcon = R.drawable.ic_sparkles,
        emptyTitle = "No suggestions yet",
        emptySubtitle = "We'll suggest people based on your profile and interests",
        connectionActionInProgress = connectionActionInProgress,
        onConnect = onConnect,
        onNavigateToProfile = onNavigateToProfile,
        onRetry = onRetry,
        onDismissError = onDismissError
    )
}

@Composable
private fun SameCampusContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    listShimmerBrush: Brush,
    reduceAnimations: Boolean,
    people: List<PersonInfo>,
    isLoading: Boolean,
    error: String?,
    userCollege: String?,
    isSavingCollege: Boolean,
    collegeSuggestions: List<CollegeInfo>,
    isSearchingColleges: Boolean,
    connectionActionInProgress: Set<String>,
    onConnect: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onRetry: () -> Unit,
    onSaveCollege: (String) -> Unit,
    onCollegeSearch: (String) -> Unit,
    onDismissError: () -> Unit = {}
) {
    // If user doesn't have a college set, show the college input form
    if (userCollege == null && !isLoading) {
        CollegeInputForm(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isSaving = isSavingCollege,
            error = error,
            collegeSuggestions = collegeSuggestions,
            isSearchingColleges = isSearchingColleges,
            onSaveCollege = onSaveCollege,
            onCollegeSearch = onCollegeSearch,
            isLightTheme = isLightTheme,
            reduceAnimations = reduceAnimations
        )
    } else {
        // Show results with college name header
        Column(Modifier.fillMaxSize()) {
            // Show college header if set
            if (userCollege != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .findPeopleGlassSurface(
                            backdrop = backdrop,
                            cornerDp = 12f,
                            blurDp = 12f,
                            isLightTheme = isLightTheme,
                            reduceAnimations = reduceAnimations,
                            onDrawSurfaceColor = if (isLightTheme) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicText("🎓", style = TextStyle(fontSize = 20.sp))
                        Column(Modifier.weight(1f)) {
                            BasicText(
                                userCollege,
                                style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
                            )
                            BasicText(
                                "${people.size} campus mates found",
                                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                            )
                        }
                    }
                }
            }
            
            // Show people grid
                PeopleGridContent(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isLightTheme = isLightTheme,
                listShimmerBrush = listShimmerBrush,
                reduceAnimations = reduceAnimations,
                    people = people,
                    isLoading = isLoading,
                    error = error,
                    emptyIcon = R.drawable.ic_education,
                emptyTitle = "No campus mates found",
                emptySubtitle = "Be the first from your campus to join Vormex!",
                connectionActionInProgress = connectionActionInProgress,
                onConnect = onConnect,
                onNavigateToProfile = onNavigateToProfile,
                onRetry = onRetry,
                onDismissError = onDismissError
            )
        }
    }
}

@Composable
private fun CollegeInputForm(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isSaving: Boolean,
    error: String?,
    collegeSuggestions: List<CollegeInfo>,
    isSearchingColleges: Boolean,
    onSaveCollege: (String) -> Unit,
    onCollegeSearch: (String) -> Unit,
    isLightTheme: Boolean,
    reduceAnimations: Boolean
) {
    var collegeName by remember { mutableStateOf("") }
    val isValid = collegeName.trim().length >= 3
    var showSuggestions by remember { mutableStateOf(false) }
    
    // Trigger search when text changes
    LaunchedEffect(collegeName) {
        if (collegeName.length >= 2) {
            onCollegeSearch(collegeName)
            showSuggestions = true
        } else {
            showSuggestions = false
        }
    }
    
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        BasicText("🎓", style = TextStyle(fontSize = 64.sp))
        
        Spacer(Modifier.height(24.dp))
        
        // Title
        BasicText(
            "Find your campus mates",
            style = TextStyle(contentColor, 22.sp, FontWeight.Bold)
        )
        
        Spacer(Modifier.height(8.dp))
        
        // Subtitle
        BasicText(
            "Enter your college/university name to discover people from the same campus",
            style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Input field with suggestions
        Box(Modifier.fillMaxWidth()) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .findPeopleGlassSurface(
                            backdrop = backdrop,
                            cornerDp = 12f,
                            blurDp = 12f,
                            isLightTheme = isLightTheme,
                            reduceAnimations = reduceAnimations,
                            onDrawSurfaceColor = Color.White.copy(alpha = 0.1f)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = collegeName,
                            onValueChange = { collegeName = it },
                            textStyle = TextStyle(contentColor, 16.sp),
                            cursorBrush = SolidColor(accentColor),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (collegeName.isEmpty()) {
                                        BasicText(
                                            "e.g. Stanford University, IIT Delhi",
                                            style = TextStyle(contentColor.copy(alpha = 0.4f), 16.sp)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (isSearchingColleges) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(
                                color = contentColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                
                // Suggestions dropdown
                if (showSuggestions && collegeSuggestions.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .findPeopleGlassSurface(
                                backdrop = backdrop,
                                cornerDp = 12f,
                                blurDp = 12f,
                                isLightTheme = isLightTheme,
                                reduceAnimations = reduceAnimations,
                                onDrawSurfaceColor = Color.White.copy(alpha = 0.15f)
                            )
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            collegeSuggestions.forEach { college ->
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            collegeName = college.name
                                            showSuggestions = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BasicText(
                                            college.name,
                                            style = TextStyle(contentColor, 14.sp)
                                        )
                                        BasicText(
                                            "${college.count} ${if (college.count == 1) "member" else "members"}",
                                            style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Error message
        error?.let {
            Spacer(Modifier.height(8.dp))
            BasicText(
                it,
                style = TextStyle(Color.Red, 13.sp)
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Save button
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isValid && !isSaving) accentColor else accentColor.copy(alpha = 0.5f))
                .clickable(enabled = isValid && !isSaving) {
                    onSaveCollege(collegeName)
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                BasicText(
                    "Save & Find Campus Mates",
                    style = TextStyle(Color.White, 15.sp, FontWeight.SemiBold)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Hint
        BasicText(
            "You can change this later in your profile settings",
            style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeopleGridContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    listShimmerBrush: Brush,
    reduceAnimations: Boolean,
    people: List<PersonInfo>,
    isLoading: Boolean,
    error: String?,
    emptyIcon: Int,
    emptyTitle: String,
    emptySubtitle: String,
    connectionActionInProgress: Set<String>,
    onConnect: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onRetry: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Reset refreshing state when loading completes
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isRefreshing = false
        }
    }
    
    when {
        isLoading && !isRefreshing -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(count = 6, key = { "people_grid_sk_$it" }) {
                    PersonCardSkeleton(listShimmerBrush, isLightTheme)
                }
            }
        }
        error != null && people.isEmpty() -> {
            ErrorState(
                message = error,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                reduceAnimations = reduceAnimations,
                onRetry = onRetry,
                onDismiss = onDismissError
            )
        }
        people.isEmpty() && !isLoading -> {
            EmptyState(
                iconRes = emptyIcon,
                title = emptyTitle,
                subtitle = emptySubtitle,
                backdrop = backdrop,
                contentColor = contentColor
            )
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    onRetry()
                },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = people, key = { it.id }) { person ->
                        PersonCard(
                            person = person,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isGlassTheme = isGlassTheme,
                            isLightTheme = isLightTheme,
                            isActionInProgress = connectionActionInProgress.contains(person.id),
                            onConnect = { onConnect(person.id) },
                            onCardClick = { onNavigateToProfile(person.id) },
                            reduceAnimations = reduceAnimations
                        )
                    }
                }
            }
        }
    }
}

// ==================== Nearby Content ====================

@SuppressLint("MissingPermission")
@Composable
private fun NearbyContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    listShimmerBrush: Brush,
    reduceAnimations: Boolean,
    nearbyPeople: List<NearbyUser>,
    isLoading: Boolean,
    error: String?,
    currentLat: Double?,
    currentLng: Double?,
    currentCity: String?,
    selectedRadius: Int,
    hasLocationPermission: Boolean,
    onPermissionGranted: () -> Unit,
    onLocationUpdate: (Double, Double, Float?) -> Unit,
    onRadiusChange: (Int) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    
    // Helper function to get current location
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        // Try lastLocation first, if null request fresh location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationUpdate(location.latitude, location.longitude, location.accuracy)
            } else {
                // Request fresh location with high accuracy
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    5000L // 5 second interval
                ).setMaxUpdates(1).build()
                
                val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        result.lastLocation?.let { loc ->
                            onLocationUpdate(loc.latitude, loc.longitude, loc.accuracy)
                        }
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    android.os.Looper.getMainLooper()
                )
            }
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onPermissionGranted()
            fetchCurrentLocation()
        }
    }
    
    // Check permission when page loads
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            onPermissionGranted()
            fetchCurrentLocation()
        }
    }
    
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (!hasLocationPermission) {
            // Permission request UI (clean, no animations)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .then(
                        if (reduceAnimations) {
                            Modifier.background(
                                Brush.verticalGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                        } else {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(24f.dp) },
                                effects = {
                                    vibrancy()
                                    blur(16f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(
                                        Brush.verticalGradient(
                                            listOf(
                                                accentColor.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                }
                            )
                        }
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Static location icon
                    Box(contentAlignment = Alignment.Center) {
                        // Outer ring
                        Box(
                            Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.1f))
                        )
                        // Middle ring
                        Box(
                            Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f))
                        )
                        // Inner circle with icon
                        Box(
                            Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("📍", style = TextStyle(fontSize = 28.sp))
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    BasicText(
                        "Discover People Nearby",
                        style = TextStyle(contentColor, 22.sp, FontWeight.Bold)
                    )
                    BasicText(
                        "Enable location to find and connect with\npeople around you",
                        style = TextStyle(
                            contentColor.copy(alpha = 0.7f),
                            14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Enable button
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(accentColor)
                            .clickable { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                            .padding(horizontal = 32.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BasicText("📍", style = TextStyle(fontSize = 16.sp))
                            BasicText(
                                "Enable Location",
                                style = TextStyle(Color.White, 15.sp, FontWeight.SemiBold)
                            )
                        }
                    }
                    
                    BasicText(
                        "Your location is only visible to others\nwhen you choose to share it",
                        style = TextStyle(
                            contentColor.copy(alpha = 0.5f),
                            12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            }
        } else {
            // Location header with city name
            Box(
                Modifier
                    .fillMaxWidth()
                    .findPeopleGlassSurface(
                        backdrop = backdrop,
                        cornerDp = 20f,
                        blurDp = 12f,
                        isLightTheme = isLightTheme,
                        reduceAnimations = reduceAnimations,
                        onDrawSurfaceColor = if (isLightTheme) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Static location indicator
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText("📍", style = TextStyle(fontSize = 18.sp))
                            }
                            
                            Column {
                                BasicText(
                                    currentCity ?: "Your Location",
                                    style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
                                )
                                if (currentLat != null && currentLng != null) {
                                    BasicText(
                                        "${nearbyPeople.size} people within ${selectedRadius}km",
                                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                                    )
                                }
                            }
                        }
                        
                        // Refresh button
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f))
                                .clickable { fetchCurrentLocation() }
                                .padding(10.dp)
                        ) {
                            BasicText("🔄", style = TextStyle(fontSize = 18.sp))
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Radius selector with animated selection
                    BasicText(
                        "Search Radius",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp, FontWeight.Medium)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 25, 50, 100, 200).forEach { radius ->
                            val isSelected = selectedRadius == radius
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) 
                                            Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.7f)))
                                        else 
                                            Brush.horizontalGradient(listOf(contentColor.copy(alpha = 0.08f), contentColor.copy(alpha = 0.08f)))
                                    )
                                    .clickable { onRadiusChange(radius) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    "${radius}km",
                                    style = TextStyle(
                                        if (isSelected) Color.White else contentColor.copy(alpha = 0.7f),
                                        13.sp,
                                        FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Map View with OpenStreetMap
            if (currentLat != null && currentLng != null) {
                NearbyMapView(
                    currentLat = currentLat,
                    currentLng = currentLng,
                    nearbyPeople = nearbyPeople,
                    selectedRadius = selectedRadius,
                    backdrop = backdrop,
                    accentColor = accentColor,
                    isLightTheme = isLightTheme,
                    reduceAnimations = reduceAnimations,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                
                Spacer(Modifier.height(16.dp))
            }
            
            // Nearby people list with animation
            when {
                isLoading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(4) { index ->
                            AnimatedNearbyCardSkeleton(
                                backdrop = backdrop,
                                listShimmerBrush = listShimmerBrush,
                                isLightTheme = isLightTheme,
                                reduceAnimations = reduceAnimations,
                                delay = index * 100
                            )
                        }
                    }
                }
                error != null -> {
                    ErrorState(
                        message = error,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        reduceAnimations = reduceAnimations,
                        onRetry = onRefresh
                    )
                }
                nearbyPeople.isEmpty() -> {
                    NearbyEmptyState(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isLightTheme = isLightTheme,
                        reduceAnimations = reduceAnimations,
                        selectedRadius = selectedRadius
                    )
                }
                else -> {
                    // Section header with count badge
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            "People Near You",
                            style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
                        )
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            BasicText(
                                "${nearbyPeople.size} found",
                                style = TextStyle(accentColor, 12.sp, FontWeight.SemiBold)
                            )
                        }
                    }
                    
                    nearbyPeople.forEachIndexed { index, user ->
                        AnimatedNearbyPersonCard(
                            user = user,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isLightTheme = isLightTheme,
                            index = index,
                            reduceAnimations = reduceAnimations,
                            onCardClick = { onNavigateToProfile(user.id) }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
        
        Spacer(Modifier.height(80.dp))
    }
}

// Animated nearby card with stagger effect
@Composable
private fun AnimatedNearbyPersonCard(
    user: NearbyUser,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    index: Int,
    reduceAnimations: Boolean,
    onCardClick: () -> Unit
) {
    // Simple fade-in, no bouncing
    var visible by remember { mutableStateOf(reduceAnimations) }
    
    LaunchedEffect(Unit) {
        if (!reduceAnimations) {
            kotlinx.coroutines.delay(index * 50L)
        }
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = tween(if (reduceAnimations) 0 else 200)
        )
    ) {
        NearbyPersonCard(
            user = user,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isLightTheme = isLightTheme,
            reduceAnimations = reduceAnimations,
            onCardClick = onCardClick
        )
    }
}

// Empty state specific to nearby
@Composable
private fun NearbyEmptyState(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    reduceAnimations: Boolean,
    selectedRadius: Int
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
            .findPeopleGlassSurface(
                backdrop = backdrop,
                cornerDp = 24f,
                blurDp = 12f,
                isLightTheme = isLightTheme,
                reduceAnimations = reduceAnimations,
                onDrawSurfaceColor = Color.White.copy(alpha = 0.08f)
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BasicText("🌍", style = TextStyle(fontSize = 56.sp))
            BasicText(
                "No one nearby yet",
                style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
            )
            BasicText(
                "Try increasing the search radius above ${selectedRadius}km\nor check back later",
                style = TextStyle(
                    contentColor.copy(alpha = 0.6f),
                    14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            )
        }
    }
}

// Animated skeleton for loading
@Composable
private fun AnimatedNearbyCardSkeleton(
    backdrop: LayerBackdrop,
    listShimmerBrush: Brush,
    isLightTheme: Boolean,
    reduceAnimations: Boolean,
    delay: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, delayMillis = if (reduceAnimations) 0 else delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    val lineBrush = if (reduceAnimations) {
        listShimmerBrush
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.Gray.copy(alpha = 0.2f),
                Color.Gray.copy(alpha = 0.4f),
                Color.Gray.copy(alpha = 0.2f)
            ),
            start = Offset(shimmerOffset, 0f),
            end = Offset(shimmerOffset + 200f, 0f)
        )
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .height(90.dp)
            .findPeopleGlassSurface(
                backdrop = backdrop,
                cornerDp = 16f,
                blurDp = 8f,
                isLightTheme = isLightTheme,
                reduceAnimations = reduceAnimations,
                onDrawSurfaceColor = if (isLightTheme) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar skeleton
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(lineBrush)
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(lineBrush)
                )
                Box(
                    Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(lineBrush)
                )
            }
        }
    }
}

// ==================== Nearby Map View ====================

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun NearbyMapView(
    currentLat: Double,
    currentLng: Double,
    nearbyPeople: List<NearbyUser>,
    selectedRadius: Int,
    backdrop: LayerBackdrop,
    accentColor: Color,
    isLightTheme: Boolean,
    reduceAnimations: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember(context) {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setTilesScaledToDpi(true)
            setUseDataConnection(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE -> view.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }
    
    Box(
        modifier
            .findPeopleGlassSurface(
                backdrop = backdrop,
                cornerDp = 20f,
                blurDp = 10f,
                isLightTheme = isLightTheme,
                reduceAnimations = reduceAnimations,
                onDrawSurfaceColor = Color.White.copy(alpha = 0.08f)
            )
    ) {
        AndroidView(
            factory = { mapView },
            update = { view ->
                updateNearbyMap(
                    context = context,
                    mapView = view,
                    currentLat = currentLat,
                    currentLng = currentLng,
                    nearbyPeople = nearbyPeople,
                    selectedRadius = selectedRadius,
                    accentColor = accentColor,
                    isLightTheme = isLightTheme
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
        )

        BasicText(
            "(C) OpenStreetMap contributors",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isLightTheme) Color.White.copy(alpha = 0.82f) else Color.Black.copy(alpha = 0.58f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            style = TextStyle(
                color = if (isLightTheme) Color.Black.copy(alpha = 0.68f) else Color.White.copy(alpha = 0.78f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

private fun updateNearbyMap(
    context: Context,
    mapView: MapView,
    currentLat: Double,
    currentLng: Double,
    nearbyPeople: List<NearbyUser>,
    selectedRadius: Int,
    accentColor: Color,
    isLightTheme: Boolean
) {
    val center = GeoPoint(currentLat, currentLng)
    val density = context.resources.displayMetrics.density
    val accentArgb = accentColor.toArgb()
    val whiteArgb = android.graphics.Color.WHITE
    val peopleArgb = Color(0xFF10B981).toArgb()

    mapView.setBackgroundColor(
        if (isLightTheme) android.graphics.Color.rgb(232, 238, 242) else android.graphics.Color.rgb(26, 32, 38)
    )
    mapView.controller.setZoom(nearbyMapZoomForRadius(selectedRadius))
    mapView.controller.setCenter(center)
    mapView.overlays.clear()

    val radiusOverlay = Polygon(mapView).apply {
        setPoints(buildMapCirclePoints(currentLat, currentLng, selectedRadius * 1000.0))
        fillPaint.color = accentColor.copy(alpha = if (isLightTheme) 0.12f else 0.18f).toArgb()
        fillPaint.style = Paint.Style.FILL
        outlinePaint.color = accentColor.copy(alpha = 0.78f).toArgb()
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.strokeWidth = 2f * density
    }
    mapView.overlays.add(radiusOverlay)

    mapView.overlays.add(
        Marker(mapView).apply {
            position = center
            title = "You"
            icon = circleMarkerDrawable(
                context = context,
                fillColor = accentArgb,
                strokeColor = whiteArgb,
                sizeDp = 22f,
                strokeDp = 3f
            )
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
    )

    nearbyPeople.forEach { user ->
        val location = user.location ?: return@forEach
        mapView.overlays.add(
            Marker(mapView).apply {
                position = GeoPoint(location.lat, location.lng)
                title = user.name ?: user.username ?: "Nearby person"
                subDescription = "${formatDistance(user.distance)} away"
                icon = circleMarkerDrawable(
                    context = context,
                    fillColor = peopleArgb,
                    strokeColor = whiteArgb,
                    sizeDp = if (user.isOnline) 19f else 16f,
                    strokeDp = 2f
                )
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
        )
    }

    mapView.invalidate()
}

private fun nearbyMapZoomForRadius(radiusKm: Int): Double {
    return when {
        radiusKm <= 10 -> 12.2
        radiusKm <= 25 -> 11.1
        radiusKm <= 50 -> 10.0
        radiusKm <= 100 -> 9.0
        else -> 8.0
    }
}

private fun circleMarkerDrawable(
    context: Context,
    fillColor: Int,
    strokeColor: Int,
    sizeDp: Float,
    strokeDp: Float
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).roundToInt().coerceAtLeast(1)
    val strokePx = strokeDp * density
    val center = sizePx / 2f
    val radius = center - strokePx / 2f
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }

    canvas.drawCircle(center, center, radius, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = strokePx
    paint.color = strokeColor
    canvas.drawCircle(center, center, radius, paint)

    return BitmapDrawable(context.resources, bitmap).apply {
        setBounds(0, 0, sizePx, sizePx)
    }
}

private fun buildMapCirclePoints(
    centerLat: Double,
    centerLng: Double,
    radiusMeters: Double,
    steps: Int = 96
): List<GeoPoint> {
    val earthRadiusMeters = 6_371_000.0
    val centerLatRad = Math.toRadians(centerLat)
    val centerLngRad = Math.toRadians(centerLng)
    val angularDistance = radiusMeters / earthRadiusMeters

    return (0..steps).map { index ->
        val bearing = 2.0 * Math.PI * index / steps
        val pointLatRad = asin(
            sin(centerLatRad) * cos(angularDistance) +
                cos(centerLatRad) * sin(angularDistance) * cos(bearing)
        )
        val pointLngRad = centerLngRad + atan2(
            sin(bearing) * sin(angularDistance) * cos(centerLatRad),
            cos(angularDistance) - sin(centerLatRad) * sin(pointLatRad)
        )

        GeoPoint(Math.toDegrees(pointLatRad), normalizeMapLongitude(Math.toDegrees(pointLngRad)))
    }
}

private fun normalizeMapLongitude(longitude: Double): Double {
    var normalized = longitude
    while (normalized < -180.0) normalized += 360.0
    while (normalized > 180.0) normalized -= 360.0
    return normalized
}

@Composable
private fun NearbyPersonCard(
    user: NearbyUser,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    reduceAnimations: Boolean,
    onCardClick: () -> Unit = {}
) {
    val imageCrossfadeMs = if (reduceAnimations) 0 else 300
    val appearance = currentVormexAppearance()
    val displayName = user.name ?: user.username ?: "Unknown"
    val handle = user.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
    val locationLabel = listOfNotNull(user.location?.city, user.location?.state)
        .take(2)
        .joinToString(", ")
        .takeIf { it.isNotBlank() }
    val detailLine = user.headline ?: handle ?: locationLabel
    val supportingLine = when {
        user.headline != null && locationLabel != null -> locationLabel
        user.headline != null && handle != null -> handle
        else -> null
    }

    Box(
        Modifier
            .fillMaxWidth()
            .vormexSurface(
                backdrop = backdrop,
                tone = VormexSurfaceTone.Card,
                cornerRadius = 18.dp,
                blurRadius = 10.dp,
                lensRadius = 0.dp,
                lensDepth = 0.dp,
                useBackdropEffects = appearance.isGlassTheme && !reduceAnimations,
                surfaceColor = when {
                    isLightTheme && appearance.isGlassTheme -> Color.White.copy(alpha = 0.68f)
                    isLightTheme -> Color(0xFFF8FAFD)
                    else -> Color.White.copy(alpha = 0.07f)
                },
                borderColor = if (isLightTheme) Color(0xFFD9E2EF) else Color.White.copy(alpha = 0.08f)
            )
            .clickable(onClick = onCardClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user.profileImage.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user.profileImage)
                                .crossfade(imageCrossfadeMs)
                                .build(),
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        val initials = displayName
                            .split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .take(2)
                            .joinToString("")
                        BasicText(
                            initials,
                            style = TextStyle(accentColor, 15.sp, FontWeight.SemiBold)
                        )
                    }
                }

                if (user.isOnline) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isLightTheme) Color.White else Color(0xFF15181D))
                            .padding(2.dp)
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                    }
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText(
                        displayName,
                        style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    VerificationBadge(
                        verified = user.hasVerificationBadge(),
                        size = VerificationBadgeSize.Small
                    )

                    if (user.isOnline) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFF22C55E).copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            BasicText(
                                "Active",
                                style = TextStyle(Color(0xFF16A34A), 10.sp, FontWeight.SemiBold)
                            )
                        }
                    }
                }

                detailLine?.let { line ->
                    BasicText(
                        line,
                        style = TextStyle(contentColor.copy(alpha = 0.62f), 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    supportingLine?.let { line ->
                        Box(
                            Modifier
                                .widthIn(max = 140.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(contentColor.copy(alpha = 0.06f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            BasicText(
                                line,
                                style = TextStyle(contentColor.copy(alpha = 0.58f), 10.sp, FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    (user.skills.firstOrNull() ?: user.interests.firstOrNull())?.let { tag ->
                        Box(
                            Modifier
                                .widthIn(max = 120.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(accentColor.copy(alpha = 0.08f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            BasicText(
                                tag,
                                style = TextStyle(contentColor.copy(alpha = 0.70f), 10.sp, FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.width(10.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.11f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BasicText(
                            formatDistance(user.distance),
                            style = TextStyle(accentColor, 12.sp, FontWeight.SemiBold)
                        )
                        BasicText(
                            "away",
                            style = TextStyle(accentColor.copy(alpha = 0.66f), 9.sp, FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyPersonCardSkeleton(
    backdrop: LayerBackdrop,
    shimmerBrush: Brush,
    isLightTheme: Boolean,
    reduceAnimations: Boolean
) {
    Box(
        Modifier
            .fillMaxWidth()
            .findPeopleGlassSurface(
                backdrop = backdrop,
                cornerDp = 20f,
                blurDp = 12f,
                isLightTheme = isLightTheme,
                reduceAnimations = reduceAnimations,
                onDrawSurfaceColor = Color.White.copy(alpha = 0.1f)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .width(150.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
            
            Box(
                Modifier
                    .width(50.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

private fun formatDistance(distanceKm: Double): String {
    return when {
        distanceKm < 1 -> "${(distanceKm * 1000).toInt()}m"
        distanceKm < 10 -> String.format("%.1fkm", distanceKm)
        else -> "${distanceKm.toInt()}km"
    }
}

// ==================== Common States ====================

@Composable
private fun EmptyState(
    iconRes: Int,
    title: String,
    subtitle: String,
    backdrop: LayerBackdrop,
    contentColor: Color
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(iconRes),
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                colorFilter = ColorFilter.tint(contentColor)
            )
            BasicText(
                title,
                style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
            )
            BasicText(
                subtitle,
                style = TextStyle(contentColor.copy(alpha = 0.6f), 13.sp)
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    reduceAnimations: Boolean = false,
    onRetry: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (reduceAnimations) {
                    Modifier.background(Color.Red.copy(alpha = 0.12f))
                } else {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(16f.dp) },
                        effects = { blur(8f.dp.toPx()) },
                        onDrawSurface = { drawRect(Color.Red.copy(alpha = 0.1f)) }
                    )
                }
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = "Error",
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(accentColor)
            )
            BasicText(
                message,
                style = TextStyle(contentColor, 14.sp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dismiss button
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(contentColor.copy(alpha = 0.1f))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        "Dismiss",
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp, FontWeight.Medium)
                    )
                }
                // Retry button
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        "Retry",
                        style = TextStyle(accentColor, 13.sp, FontWeight.Medium)
                    )
                }
            }
        }
    }
}
