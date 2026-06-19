package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.HackathonsApiService
import com.kyant.backdrop.catalog.network.models.ApplyHackathonTeamRequest
import com.kyant.backdrop.catalog.network.models.CollegeCommunity
import com.kyant.backdrop.catalog.network.models.CreateCollegeCommunityRequest
import com.kyant.backdrop.catalog.network.models.CreateHackathonRequest
import com.kyant.backdrop.catalog.network.models.FormHackathonTeamRequest
import com.kyant.backdrop.catalog.network.models.Hackathon
import com.kyant.backdrop.catalog.network.models.HackathonsResponse
import com.kyant.backdrop.catalog.network.models.HackathonTeam
import com.kyant.backdrop.catalog.network.models.User
import com.kyant.backdrop.catalog.network.models.VerifyCollegeStudentRequest
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.ui.BasicText
import com.kyant.backdrop.catalog.ui.BasicTextField
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private enum class HackathonBoardTab {
    BOARD,
    MY_TEAMS,
    COMMUNITIES
}

private val RetroHackCream = Color(0xFFF3EFE3)
private val RetroHackInk = Color(0xFF111111)
private val RetroHackYellow = Color(0xFFFFD414)
private val RetroHackRed = Color(0xFFFF3B30)
private val RetroHackBlue = Color(0xFF3F6FFF)
private val RetroHackGreen = Color(0xFF28D17C)

private data class HackathonSourceFilter(
    val label: String,
    val source: String?
)

private data class HackathonSortOption(
    val label: String,
    val value: String
)

private val HackathonSourceFilters = listOf(
    HackathonSourceFilter("All", null),
    HackathonSourceFilter("Devpost", "devpost"),
    HackathonSourceFilter("HackerEarth", "hackerearth"),
    HackathonSourceFilter("MLH", "mlh"),
    HackathonSourceFilter("Devfolio", "devfolio"),
    HackathonSourceFilter("College", "college_fest"),
    HackathonSourceFilter("Custom", "custom")
)

private val HackathonSortOptions = listOf(
    HackathonSortOption("Relevant", "relevant"),
    HackathonSortOption("Deadline", "deadline"),
    HackathonSortOption("Starts soon", "starts"),
    HackathonSortOption("Prize", "prize")
)

data class HackathonBoardUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val actionInProgress: Boolean = false,
    val error: String? = null,
    val currentUser: User? = null,
    val status: String = "open",
    val source: String? = null,
    val sort: String = "relevant",
    val search: String = "",
    val hackathons: List<Hackathon> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val hasMore: Boolean = false,
    val expandedHackathonId: String? = null,
    val formingTeamHackathonId: String? = null,
    val teamsByHackathon: Map<String, List<HackathonTeam>> = emptyMap(),
    val myTeams: List<HackathonTeam> = emptyList(),
    val communities: List<CollegeCommunity> = emptyList()
)

class HackathonBoardViewModel(private val context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(HackathonBoardUiState())
    val uiState: StateFlow<HackathonBoardUiState> = _uiState.asStateFlow()

    fun loadInitial() {
        viewModelScope.launch {
            ApiClient.getCurrentUser(appContext).onSuccess { user ->
                _uiState.value = _uiState.value.copy(currentUser = user)
            }
            loadHackathons()
            loadMyTeams()
            loadCommunities()
        }
    }

    fun setStatus(status: String) {
        _uiState.value = _uiState.value.copy(status = status, page = 1, hasMore = false)
        loadHackathons()
    }

    fun setSource(source: String?) {
        _uiState.value = _uiState.value.copy(source = source, page = 1, hasMore = false)
        loadHackathons()
    }

    fun setSort(sort: String) {
        _uiState.value = _uiState.value.copy(sort = sort)
    }

    fun setSearch(search: String) {
        _uiState.value = _uiState.value.copy(search = search)
    }

    fun loadHackathons(page: Int = 1, append: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            if (append && (state.isLoading || state.isLoadingMore || !state.hasMore)) return@launch
            _uiState.value = if (append) {
                state.copy(isLoadingMore = true, error = null)
            } else {
                state.copy(isLoading = state.hackathons.isEmpty(), isLoadingMore = false, error = null)
            }
            HackathonsApiService.getHackathons(
                context = appContext,
                status = state.status,
                search = state.search.takeIf { it.isNotBlank() },
                source = state.source,
                page = page,
                limit = 50
            ).fold(
                onSuccess = { response ->
                    val effectiveResponse = if (!append && state.source == null && response.hackathons.isEmpty()) {
                        loadMergedExternalHackathons(state) ?: response
                    } else {
                        response
                    }
                    val mergedHackathons = if (append) {
                        (state.hackathons + effectiveResponse.hackathons).distinctBy { it.id }
                    } else {
                        effectiveResponse.hackathons
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        hackathons = mergedHackathons,
                        page = effectiveResponse.page,
                        total = effectiveResponse.total,
                        hasMore = effectiveResponse.hasMore,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoadingMore = false, error = error.message)
                }
            )
        }
    }

    fun loadMoreHackathons() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        loadHackathons(page = state.page + 1, append = true)
    }

    private suspend fun loadMergedExternalHackathons(state: HackathonBoardUiState): HackathonsResponse? {
        val responses = listOf("devpost", "hackerearth", "devfolio", "mlh").mapNotNull { source ->
            HackathonsApiService.getHackathons(
                context = appContext,
                status = state.status,
                search = state.search.takeIf { it.isNotBlank() },
                source = source,
                page = 1,
                limit = 50
            ).getOrNull()
        }
        val hackathons = responses.flatMap { it.hackathons }.distinctBy { it.id }
        if (hackathons.isEmpty()) return null
        return HackathonsResponse(
            hackathons = hackathons,
            page = 1,
            total = responses.sumOf { it.total }.coerceAtLeast(hackathons.size),
            totalPages = 1,
            hasMore = responses.any { it.hasMore }
        )
    }

    fun toggleTeams(hackathonId: String) {
        val expanded = _uiState.value.expandedHackathonId
        if (expanded == hackathonId) {
            _uiState.value = _uiState.value.copy(expandedHackathonId = null)
            return
        }
        _uiState.value = _uiState.value.copy(expandedHackathonId = hackathonId)
        if (_uiState.value.teamsByHackathon[hackathonId] == null) {
            loadTeams(hackathonId)
        }
    }

    private fun loadTeams(hackathonId: String) {
        viewModelScope.launch {
            HackathonsApiService.getTeams(appContext, hackathonId).onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    teamsByHackathon = _uiState.value.teamsByHackathon + (hackathonId to response.teams)
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }

    fun formTeam(hackathon: Hackathon) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                actionInProgress = true,
                formingTeamHackathonId = hackathon.id,
                error = null
            )
            HackathonsApiService.formTeam(
                appContext,
                hackathon.id,
                FormHackathonTeamRequest(
                    name = "${_uiState.value.currentUser?.name ?: "My"} ${hackathon.title} team",
                    requiredSkills = hackathon.skills.take(6),
                    maxMembers = hackathon.teamMax
                )
            ).fold(
                onSuccess = {
                    Toast.makeText(appContext, "Team chat created", Toast.LENGTH_SHORT).show()
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = false,
                        formingTeamHackathonId = null
                    )
                    loadHackathons()
                    loadMyTeams()
                    loadTeams(hackathon.id)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        actionInProgress = false,
                        formingTeamHackathonId = null,
                        error = error.message
                    )
                }
            )
        }
    }

    fun applyToTeam(team: HackathonTeam) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true, error = null)
            HackathonsApiService.applyToTeam(
                appContext,
                team.id,
                ApplyHackathonTeamRequest(
                    role = team.lookingForRoles.firstOrNull() ?: "Teammate",
                    message = "I am interested in joining this hackathon team.",
                    skills = team.requiredSkills.take(5)
                )
            ).fold(
                onSuccess = {
                    Toast.makeText(appContext, "Application sent", Toast.LENGTH_SHORT).show()
                    _uiState.value = _uiState.value.copy(actionInProgress = false)
                    loadTeams(team.hackathonId)
                    loadMyTeams()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(actionInProgress = false, error = error.message)
                }
            )
        }
    }

    fun toggleSave(hackathon: Hackathon) {
        viewModelScope.launch {
            val request = if (hackathon.isSaved) {
                HackathonsApiService.unsaveHackathon(appContext, hackathon.id)
            } else {
                HackathonsApiService.saveHackathon(appContext, hackathon.id)
            }
            request.onSuccess { loadHackathons() }
                .onFailure { error -> _uiState.value = _uiState.value.copy(error = error.message) }
        }
    }

    fun createHackathon(request: CreateHackathonRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true, error = null)
            HackathonsApiService.createHackathon(appContext, request).fold(
                onSuccess = {
                    Toast.makeText(appContext, "Hackathon posted", Toast.LENGTH_SHORT).show()
                    _uiState.value = _uiState.value.copy(actionInProgress = false, status = "open", source = null)
                    loadHackathons()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(actionInProgress = false, error = error.message)
                }
            )
        }
    }

    fun loadMyTeams() {
        viewModelScope.launch {
            HackathonsApiService.getMyTeams(appContext).onSuccess { response ->
                _uiState.value = _uiState.value.copy(myTeams = response.teams)
            }
        }
    }

    fun loadCommunities() {
        viewModelScope.launch {
            HackathonsApiService.getCollegeCommunities(appContext).fold(
                onSuccess = { response -> _uiState.value = _uiState.value.copy(communities = response.communities) },
                onFailure = { error -> _uiState.value = _uiState.value.copy(error = error.message) }
            )
        }
    }

    fun createCommunityForMyCollege() {
        val college = _uiState.value.currentUser?.college?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true, error = null)
            HackathonsApiService.createCollegeCommunity(
                appContext,
                CreateCollegeCommunityRequest(college = college)
            ).fold(
                onSuccess = {
                    Toast.makeText(appContext, "College community ready", Toast.LENGTH_SHORT).show()
                    _uiState.value = _uiState.value.copy(actionInProgress = false)
                    loadCommunities()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(actionInProgress = false, error = error.message)
                }
            )
        }
    }

    fun verifyAndJoinCommunity(community: CollegeCommunity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actionInProgress = true, error = null)
            HackathonsApiService.verifyCollegeStudent(
                appContext,
                VerifyCollegeStudentRequest(college = community.college)
            )
            HackathonsApiService.joinCollegeCommunity(appContext, community.id).fold(
                onSuccess = {
                    Toast.makeText(appContext, "Joined ${community.college}", Toast.LENGTH_SHORT).show()
                    _uiState.value = _uiState.value.copy(actionInProgress = false)
                    loadCommunities()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(actionInProgress = false, error = error.message)
                }
            )
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HackathonBoardViewModel(context) as T
        }
    }
}

@Composable
fun HackathonBoardScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    onOpenGroupChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: HackathonBoardViewModel = viewModel(factory = HackathonBoardViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(HackathonBoardTab.BOARD) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var selectedHackathonId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedHackathon = remember(uiState.hackathons, selectedHackathonId) {
        uiState.hackathons.firstOrNull { it.id == selectedHackathonId }
    }

    LaunchedEffect(Unit) {
        viewModel.loadInitial()
    }

    if (showCreateDialog) {
        CreateHackathonDialog(
            currentUser = uiState.currentUser,
            onDismiss = { showCreateDialog = false },
            onCreate = {
                showCreateDialog = false
                viewModel.createHackathon(it)
            }
        )
    }

    selectedHackathon?.let { hackathon ->
        HackathonDetailDialog(
            hackathon = hackathon,
            expanded = uiState.expandedHackathonId == hackathon.id,
            isFormingTeam = uiState.formingTeamHackathonId == hackathon.id,
            teams = uiState.teamsByHackathon[hackathon.id].orEmpty(),
            contentColor = contentColor,
            accentColor = accentColor,
            onDismiss = { selectedHackathonId = null },
            onToggleSave = { viewModel.toggleSave(hackathon) },
            onFormTeam = { viewModel.formTeam(hackathon) },
            onToggleTeams = { viewModel.toggleTeams(hackathon.id) },
            onApplyToTeam = viewModel::applyToTeam,
            onOpenGroupChat = onOpenGroupChat
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RetroHackCream)
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        HackathonHeader(
            title = "Hackathon Board",
            subtitle = "Events and teams",
            contentColor = contentColor,
            accentColor = accentColor,
            onBack = onNavigateBack,
            onCreate = { showCreateDialog = true }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HackathonBoardTab.values().forEach { tab ->
                val label = when (tab) {
                    HackathonBoardTab.BOARD -> "BOARD"
                    HackathonBoardTab.MY_TEAMS -> "MY TEAMS"
                    HackathonBoardTab.COMMUNITIES -> "COLLEGES"
                }
                RetroHackPill(
                    label = label,
                    background = if (selectedTab == tab) RetroHackYellow else Color.White,
                    onClick = { selectedTab = tab },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        uiState.error?.let { error ->
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .retroHackPanel(RetroHackRed)
                    .padding(10.dp)
            ) {
                BasicText(
                    text = error.uppercase(Locale.US),
                    style = TextStyle(Color.White, 10.sp, FontWeight.Black, letterSpacing = 0.8.sp)
                )
            }
        }

        when (selectedTab) {
            HackathonBoardTab.BOARD -> HackathonBoardList(
                uiState = uiState,
                contentColor = contentColor,
                accentColor = accentColor,
                onStatusChange = viewModel::setStatus,
                onSourceChange = viewModel::setSource,
                onSortChange = viewModel::setSort,
                onSearchChange = viewModel::setSearch,
                onSearchSubmit = viewModel::loadHackathons,
                onLoadMore = viewModel::loadMoreHackathons,
                onOpenHackathon = { selectedHackathonId = it.id },
                onToggleSave = viewModel::toggleSave
            )
            HackathonBoardTab.MY_TEAMS -> MyHackathonTeamsList(
                teams = uiState.myTeams,
                contentColor = contentColor,
                accentColor = accentColor,
                onOpenGroupChat = onOpenGroupChat
            )
            HackathonBoardTab.COMMUNITIES -> CollegeCommunitiesList(
                uiState = uiState,
                contentColor = contentColor,
                accentColor = accentColor,
                onCreateMyCommunity = viewModel::createCommunityForMyCollege,
                onJoinCommunity = viewModel::verifyAndJoinCommunity,
                onOpenGroupChat = onOpenGroupChat
            )
        }
    }
}

@Composable
private fun HackathonHeader(
    title: String,
    subtitle: String,
    contentColor: Color,
    accentColor: Color,
    onBack: () -> Unit,
    onCreate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .retroHackPanel(RetroHackYellow)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = RetroHackInk)
        }
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                "VORMEX / ${title.uppercase(Locale.US)}",
                style = TextStyle(RetroHackInk, 18.sp, FontWeight.Black, letterSpacing = 0.8.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                "// ${subtitle.uppercase(Locale.US)}",
                style = TextStyle(RetroHackInk.copy(alpha = 0.72f), 10.sp, FontWeight.Black, letterSpacing = 1.2.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onCreate) {
            Icon(Icons.Outlined.Add, contentDescription = "Post hackathon", tint = RetroHackInk)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HackathonBoardList(
    uiState: HackathonBoardUiState,
    contentColor: Color,
    accentColor: Color,
    onStatusChange: (String) -> Unit,
    onSourceChange: (String?) -> Unit,
    onSortChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenHackathon: (Hackathon) -> Unit,
    onToggleSave: (Hackathon) -> Unit,
) {
    val listState = rememberLazyListState()
    val visibleHackathons = remember(uiState.hackathons, uiState.sort) {
        sortHackathons(uiState.hackathons, uiState.sort)
    }

    LaunchedEffect(listState, uiState.hasMore, uiState.isLoadingMore, visibleHackathons.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            totalItems > 0 && lastVisible >= totalItems - 4
        }.distinctUntilChanged().collect { shouldLoad ->
            if (shouldLoad && uiState.hasMore && !uiState.isLoadingMore) {
                onLoadMore()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)
    ) {
        item {
            HackathonBoardControls(
                uiState = uiState,
                visibleCount = visibleHackathons.size,
                contentColor = contentColor,
                accentColor = accentColor,
                onStatusChange = onStatusChange,
                onSourceChange = onSourceChange,
                onSortChange = onSortChange,
                onSearchChange = onSearchChange,
                onSearchSubmit = onSearchSubmit
            )
        }

        if (uiState.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            }
        }

        if (!uiState.isLoading && visibleHackathons.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No hackathons match these filters",
                    body = "Switch source, status, or search terms.",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }

        items(visibleHackathons, key = { it.id }) { hackathon ->
            HackathonCard(
                hackathon = hackathon,
                contentColor = contentColor,
                accentColor = accentColor,
                onOpen = { onOpenHackathon(hackathon) },
                onToggleSave = { onToggleSave(hackathon) },
            )
        }

        if (uiState.isLoadingMore) {
            item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                }
            }
        } else if (uiState.hasMore && visibleHackathons.isNotEmpty()) {
            item {
                RetroHackButton(
                    label = "LOAD MORE HACKATHONS",
                    background = RetroHackInk,
                    content = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onLoadMore
                )
            }
        }
    }
}

@Composable
private fun HackathonBoardControls(
    uiState: HackathonBoardUiState,
    visibleCount: Int,
    contentColor: Color,
    accentColor: Color,
    onStatusChange: (String) -> Unit,
    onSourceChange: (String?) -> Unit,
    onSortChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .retroHackPanel(Color.White)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RetroHackSearchField(
            value = uiState.search,
            onValueChange = onSearchChange,
            onSubmit = onSearchSubmit,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RetroHackPill("$visibleCount FOUND", RetroHackYellow)

            listOf("open", "active", "upcoming", "past").forEach { status ->
                RetroHackPill(
                    label = status.uppercase(Locale.US),
                    background = if (uiState.status == status) RetroHackYellow else Color.White,
                    onClick = { onStatusChange(status) }
                )
            }

            HackathonSourceFilters.forEach { filter ->
                RetroHackPill(
                    label = filter.label.uppercase(Locale.US),
                    background = if (uiState.source == filter.source) RetroHackGreen else Color.White,
                    onClick = { onSourceChange(filter.source) }
                )
            }

            HackathonSortOptions.forEach { option ->
                RetroHackPill(
                    label = option.label.uppercase(Locale.US),
                    background = if (uiState.sort == option.value) RetroHackBlue else Color.White,
                    content = if (uiState.sort == option.value) Color.White else RetroHackInk,
                    onClick = { onSortChange(option.value) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HackathonCard(
    hackathon: Hackathon,
    contentColor: Color,
    accentColor: Color,
    onOpen: () -> Unit,
    onToggleSave: () -> Unit
) {
    val context = LocalContext.current
    val sourceTint = sourceAccent(hackathon.source, accentColor)
    val dateRange = formatHackathonDateRange(hackathon)
    val timeLeft = hackathonTimeLeft(hackathon)
    val keywords = remember(hackathon.tags, hackathon.skills) {
        (hackathon.tags + hackathon.skills).filter { it.isNotBlank() }.distinctBy { it.lowercase() }.take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .retroHackPanel(Color.White.copy(alpha = 0.72f))
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RetroHackPill(sourceLabel(hackathon.source), sourceTint)
            Spacer(Modifier.width(8.dp))
            RetroHackPill(timeLeft, Color.White)
            Spacer(Modifier.weight(1f))
            BasicText(
                hackathon.status.uppercase(Locale.US),
                style = TextStyle(RetroHackRed, 11.sp, FontWeight.Black, letterSpacing = 0.8.sp)
            )
        }

        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(96.dp)
                    .background(RetroHackYellow)
                    .border(2.dp, RetroHackInk, RoundedCornerShape(0.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!hackathon.bannerUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(hackathon.bannerUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = hackathon.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Outlined.Public, contentDescription = null, tint = RetroHackInk, modifier = Modifier.size(32.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BasicText(
                            hackathon.title.uppercase(Locale.US),
                            style = TextStyle(RetroHackInk, 16.sp, FontWeight.Black, lineHeight = 18.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        BasicText(
                            listOfNotNull(hackathon.organizer, hackathon.college).joinToString(" - ").uppercase(Locale.US),
                            style = TextStyle(RetroHackInk.copy(alpha = 0.62f), 10.sp, FontWeight.Black, letterSpacing = 0.5.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onToggleSave, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (hackathon.isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = RetroHackInk
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HackathonMetric(Icons.Outlined.CalendarMonth, dateRange, RetroHackInk)
                    HackathonMetric(Icons.Outlined.LocationOn, hackathon.location ?: if (hackathon.isOnline) "Online" else "Venue TBA", RetroHackInk)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    hackathon.prizeSummary?.takeIf { it.isNotBlank() }?.let {
                        HackathonMetric(Icons.Outlined.EmojiEvents, it, RetroHackInk)
                    }
                    HackathonMetric(Icons.Outlined.Groups, "${hackathon.teamMin}-${hackathon.teamMax} members", RetroHackInk)
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HackathonTinyChip(hackathon.status, RetroHackYellow, RetroHackInk)
            HackathonTinyChip(timeLeft, Color.White, RetroHackInk)
            keywords.forEach { keyword ->
                HackathonTinyChip(keyword, sourceTint, RetroHackInk)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HackathonDetailDialog(
    hackathon: Hackathon,
    expanded: Boolean,
    isFormingTeam: Boolean,
    teams: List<HackathonTeam>,
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onToggleSave: () -> Unit,
    onFormTeam: () -> Unit,
    onToggleTeams: () -> Unit,
    onApplyToTeam: (HackathonTeam) -> Unit,
    onOpenGroupChat: (String) -> Unit
) {
    val context = LocalContext.current
    val sourceTint = sourceAccent(hackathon.source, accentColor)
    val keywords = remember(hackathon.tags, hackathon.skills) {
        (hackathon.tags + hackathon.skills).filter { it.isNotBlank() }.distinctBy { it.lowercase() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .retroHackPanel(RetroHackCream),
                shape = RoundedCornerShape(0.dp),
                color = RetroHackCream
            ) {
                Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(
                            "// HACKATHON DETAILS",
                            style = TextStyle(RetroHackInk, 15.sp, FontWeight.Black, letterSpacing = 1.2.sp)
                        )
                        BasicText(
                            sourceLabel(hackathon.source).uppercase(Locale.US),
                            style = TextStyle(sourceTint, 10.sp, FontWeight.Black, letterSpacing = 0.8.sp)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = RetroHackInk)
                    }
                }
                HorizontalDivider(color = RetroHackInk, thickness = 2.dp)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp)
                                .background(RetroHackYellow)
                                .border(2.dp, RetroHackInk, RoundedCornerShape(0.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!hackathon.bannerUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(hackathon.bannerUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = hackathon.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(168.dp)
                                )
                            } else {
                                Icon(Icons.Outlined.Public, contentDescription = null, tint = RetroHackInk, modifier = Modifier.size(42.dp))
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                RetroHackPill(sourceLabel(hackathon.source), sourceTint)
                                RetroHackPill(hackathonTimeLeft(hackathon), Color.White)
                                RetroHackPill(hackathon.status, RetroHackGreen)
                            }
                            BasicText(
                                hackathon.title.uppercase(Locale.US),
                                style = TextStyle(RetroHackInk, 22.sp, FontWeight.Black, lineHeight = 24.sp)
                            )
                            listOfNotNull(hackathon.organizer, hackathon.college, hackathon.theme).joinToString(" - ").takeIf { it.isNotBlank() }?.let {
                                BasicText(
                                    it.uppercase(Locale.US),
                                    style = TextStyle(RetroHackInk.copy(alpha = 0.62f), 11.sp, FontWeight.Black, letterSpacing = 0.7.sp)
                                )
                            }
                        }
                    }

                    item {
                        HackathonDetailSection(title = "Overview", contentColor = contentColor) {
                            BasicText(
                                hackathon.description.ifBlank { "No description provided." },
                                style = TextStyle(RetroHackInk.copy(alpha = 0.82f), 13.sp, FontWeight.Medium, lineHeight = 19.sp)
                            )
                        }
                    }

                    item {
                        HackathonDetailSection(title = "Details", contentColor = contentColor) {
                            HackathonInfoRow("Deadline", formatHackathonDateTime(hackathon.registrationDeadline ?: hackathon.endsAt), contentColor)
                            HackathonInfoRow("Schedule", formatHackathonFullDateRange(hackathon), contentColor)
                            HackathonInfoRow("Location", hackathon.location ?: if (hackathon.isOnline) "Online" else "Venue TBA", contentColor)
                            HackathonInfoRow("Prize", hackathon.prizeSummary ?: "Not listed", contentColor)
                            HackathonInfoRow("Team size", "${hackathon.teamMin}-${hackathon.teamMax} members", contentColor)
                            HackathonInfoRow("Teams", "${hackathon.teamsCount} listed", contentColor)
                            HackathonInfoRow("Saved by", "${hackathon.savesCount} students", contentColor)
                            HackathonInfoRow("Source", sourceLabel(hackathon.source), contentColor)
                            HackathonInfoRow("Source ID", hackathon.sourceId ?: "Not listed", contentColor)
                            HackathonInfoRow("Posted", formatHackathonDateTime(hackathon.createdAt), contentColor)
                            HackathonInfoRow("Updated", formatHackathonDateTime(hackathon.updatedAt), contentColor)
                        }
                    }

                    if (keywords.isNotEmpty()) {
                        item {
                            HackathonDetailSection(title = "Tags and skills", contentColor = contentColor) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    keywords.forEach { keyword ->
                                        HackathonTinyChip(keyword, sourceTint, contentColor)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        HackathonDetailSection(title = "Actions", contentColor = contentColor) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RetroHackButton(
                                    label = when {
                                        isFormingTeam -> "FORMING..."
                                        hackathon.myTeam != null -> "TEAM READY"
                                        else -> "FORM TEAM"
                                    },
                                    background = if (hackathon.myTeam != null) RetroHackGreen else RetroHackRed,
                                    content = Color.White,
                                    modifier = Modifier.weight(1f),
                                    enabled = hackathon.myTeam == null && !isFormingTeam,
                                    onClick = onFormTeam
                                )
                                RetroHackButton(
                                    label = if (hackathon.isSaved) "SAVED" else "SAVE",
                                    background = if (hackathon.isSaved) RetroHackYellow else Color.White,
                                    content = RetroHackInk,
                                    modifier = Modifier.weight(1f),
                                    onClick = onToggleSave
                                )
                            }
                            RetroHackButton(
                                label = "OPEN SOURCE PAGE",
                                background = if (hackathon.sourceUrl.isNullOrBlank()) Color(0xFFE8E2D4) else RetroHackBlue,
                                content = if (hackathon.sourceUrl.isNullOrBlank()) RetroHackInk.copy(alpha = 0.45f) else Color.White,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { openExternalHackathon(context, hackathon.sourceUrl) },
                                enabled = !hackathon.sourceUrl.isNullOrBlank()
                            )
                            hackathon.sourceUrl?.let {
                                BasicText(
                                    it.uppercase(Locale.US),
                                    style = TextStyle(RetroHackInk.copy(alpha = 0.58f), 9.sp, FontWeight.Black, letterSpacing = 0.5.sp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            hackathon.myTeam?.groupId?.let { groupId ->
                                RetroHackButton(
                                    label = "OPEN TEAM CHAT",
                                    background = RetroHackYellow,
                                    content = RetroHackInk,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onOpenGroupChat(groupId) }
                                )
                            }
                        }
                    }

                    item {
                        HackathonDetailSection(title = "Teams", contentColor = contentColor) {
                            RetroHackButton(
                                label = if (expanded) "HIDE TEAMS" else "VIEW OPEN TEAMS",
                                background = RetroHackInk,
                                content = Color.White,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onToggleTeams
                            )
                            if (expanded) {
                                if (teams.isEmpty()) {
                                    BasicText(
                                        "NO OPEN TEAMS YET.",
                                        style = TextStyle(RetroHackInk.copy(alpha = 0.62f), 11.sp, FontWeight.Black, letterSpacing = 0.8.sp)
                                    )
                                } else {
                                    teams.forEach { team ->
                                        HackathonTeamRow(
                                            team = team,
                                            contentColor = contentColor,
                                            accentColor = accentColor,
                                            onApply = { onApplyToTeam(team) },
                                            onOpenGroupChat = onOpenGroupChat
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            if (isFormingTeam) {
                HackathonTeamLoadingOverlay(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun RetroHackPill(
    label: String,
    background: Color = Color.White,
    content: Color = RetroHackInk,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .background(background)
            .border(2.dp, RetroHackInk, RoundedCornerShape(0.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 9.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label.uppercase(Locale.US),
            style = TextStyle(content, 10.sp, FontWeight.Black, letterSpacing = 0.4.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RetroHackButton(
    label: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .retroHackPanel(background)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label.uppercase(Locale.US),
            style = TextStyle(content, 12.sp, FontWeight.Black, letterSpacing = 0.5.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun Modifier.retroHackPanel(background: Color): Modifier =
    this
        .drawBehind {
            val shadowOffset = 4.dp.toPx()
            drawRect(
                color = RetroHackInk,
                topLeft = Offset(shadowOffset, shadowOffset),
                size = size
            )
        }
        .background(background)
        .border(2.dp, RetroHackInk, RoundedCornerShape(0.dp))

@Composable
private fun RetroHackSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .background(Color.White)
            .border(2.dp, RetroHackInk, RoundedCornerShape(0.dp))
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = RetroHackInk, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            textStyle = TextStyle(RetroHackInk, 13.sp, FontWeight.Black, letterSpacing = 0.4.sp),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        BasicText(
                            "SEARCH HACKATHONS",
                            style = TextStyle(RetroHackInk.copy(alpha = 0.42f), 12.sp, FontWeight.Black, letterSpacing = 0.5.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .height(36.dp)
                .width(52.dp)
                .background(RetroHackInk)
                .clickable { onSubmit() },
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                "GO",
                style = TextStyle(Color.White, 12.sp, FontWeight.Black, letterSpacing = 0.6.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HackathonTeamLoadingOverlay(
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.hackathon_team_loading)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = modifier
            .widthIn(min = 230.dp, max = 300.dp)
            .retroHackPanel(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(140.dp)
        )
        BasicText(
            "FORMING TEAM",
            style = TextStyle(RetroHackInk, 18.sp, FontWeight.Black, letterSpacing = 1.sp, textAlign = TextAlign.Center)
        )
        BasicText(
            "CREATING CHAT + TEAM SPACE",
            style = TextStyle(RetroHackInk.copy(alpha = 0.62f), 10.sp, FontWeight.Black, letterSpacing = 0.6.sp, textAlign = TextAlign.Center)
        )
    }
}

@Composable
private fun HackathonDetailSection(
    title: String,
    contentColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .retroHackPanel(Color.White.copy(alpha = 0.72f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicText(
            "// ${title.uppercase(Locale.US)}",
            style = TextStyle(RetroHackInk, 12.sp, FontWeight.Black, letterSpacing = 1.2.sp)
        )
        content()
    }
}

@Composable
private fun HackathonInfoRow(label: String, value: String, contentColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(
            label.uppercase(Locale.US),
            style = TextStyle(RetroHackInk.copy(alpha = 0.58f), 9.sp, FontWeight.Black, letterSpacing = 0.7.sp),
            modifier = Modifier.width(92.dp)
        )
        BasicText(
            value.uppercase(Locale.US),
            style = TextStyle(RetroHackInk.copy(alpha = 0.82f), 10.sp, FontWeight.Black, lineHeight = 14.sp),
            modifier = Modifier.weight(1f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HackathonSourcePill(label: String, tint: Color, contentColor: Color) {
    RetroHackPill(label = label, background = tint, content = contentColor)
}

@Composable
private fun HackathonMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(max = 190.dp)
    ) {
        Icon(icon, contentDescription = null, tint = RetroHackInk.copy(alpha = 0.72f), modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        BasicText(
            label.uppercase(Locale.US),
            style = TextStyle(RetroHackInk.copy(alpha = 0.74f), 9.sp, FontWeight.Black, letterSpacing = 0.5.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HackathonTeamRow(
    team: HackathonTeam,
    contentColor: Color,
    accentColor: Color,
    onApply: () -> Unit,
    onOpenGroupChat: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .retroHackPanel(Color.White.copy(alpha = 0.72f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    team.name.uppercase(Locale.US),
                    style = TextStyle(RetroHackInk, 13.sp, FontWeight.Black),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    "${team.memberCount}/${team.maxMembers} MEMBERS - ${team.status.uppercase(Locale.US)}",
                    style = TextStyle(RetroHackInk.copy(alpha = 0.58f), 9.sp, FontWeight.Black, letterSpacing = 0.7.sp)
                )
            }
            if (team.groupId != null && team.members.any { it.status == "accepted" }) {
                IconButton(onClick = { onOpenGroupChat(team.groupId) }) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "Chat", tint = RetroHackInk)
                }
            }
        }
        if (!team.pitch.isNullOrBlank()) {
            BasicText(
                team.pitch.uppercase(Locale.US),
                style = TextStyle(RetroHackInk.copy(alpha = 0.74f), 10.sp, FontWeight.Black, lineHeight = 14.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            team.requiredSkills.take(3).forEach { skill ->
                HackathonTinyChip(skill, RetroHackYellow, RetroHackInk)
            }
        }
        if (team.myApplication?.status == "pending") {
            BasicText(
                "APPLICATION PENDING",
                style = TextStyle(RetroHackRed, 10.sp, FontWeight.Black, letterSpacing = 0.7.sp)
            )
        } else if (team.status == "open") {
            RetroHackButton(
                label = "APPLY TO JOIN",
                background = RetroHackYellow,
                content = RetroHackInk,
                modifier = Modifier.fillMaxWidth(),
                onClick = onApply
            )
        }
    }
}

@Composable
private fun MyHackathonTeamsList(
    teams: List<HackathonTeam>,
    contentColor: Color,
    accentColor: Color,
    onOpenGroupChat: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp)
    ) {
        if (teams.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No hackathon teams yet",
                    body = "Form a team from a hackathon to unlock team chat.",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }
        items(teams, key = { it.id }) { team ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .retroHackPanel(Color.White.copy(alpha = 0.72f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RetroHackPill("Team", RetroHackYellow)
                BasicText(
                    team.name.uppercase(Locale.US),
                    style = TextStyle(RetroHackInk, 15.sp, FontWeight.Black),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    "${team.memberCount}/${team.maxMembers} MEMBERS",
                    style = TextStyle(RetroHackInk.copy(alpha = 0.62f), 10.sp, FontWeight.Black, letterSpacing = 0.7.sp)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    team.requiredSkills.take(5).forEach { HackathonTinyChip(it, RetroHackYellow, RetroHackInk) }
                }
                team.groupId?.let { groupId ->
                    RetroHackButton(
                        label = "OPEN TEAM CHAT",
                        background = RetroHackBlue,
                        content = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenGroupChat(groupId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollegeCommunitiesList(
    uiState: HackathonBoardUiState,
    contentColor: Color,
    accentColor: Color,
    onCreateMyCommunity: () -> Unit,
    onJoinCommunity: (CollegeCommunity) -> Unit,
    onOpenGroupChat: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .retroHackPanel(RetroHackYellow)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.School, contentDescription = null, tint = RetroHackInk)
                    Spacer(Modifier.width(8.dp))
                    BasicText("COLLEGE COMMUNITIES", style = TextStyle(RetroHackInk, 14.sp, FontWeight.Black, letterSpacing = 0.8.sp))
                }
                BasicText(
                    (uiState.currentUser?.college ?: "Add your college on profile to create a community.").uppercase(Locale.US),
                    style = TextStyle(RetroHackInk.copy(alpha = 0.72f), 11.sp, FontWeight.Black, lineHeight = 15.sp)
                )
                RetroHackButton(
                    label = "CREATE COLLEGE COMMUNITY",
                    background = if (!uiState.currentUser?.college.isNullOrBlank()) RetroHackInk else Color(0xFFE8E2D4),
                    content = if (!uiState.currentUser?.college.isNullOrBlank()) Color.White else RetroHackInk.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.currentUser?.college.isNullOrBlank(),
                    onClick = onCreateMyCommunity
                )
            }
        }
        items(uiState.communities, key = { it.id }) { community ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .retroHackPanel(Color.White.copy(alpha = 0.72f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (community.canJoin || community.isMember) Icons.Outlined.Verified else Icons.Outlined.School,
                        contentDescription = null,
                        tint = RetroHackInk
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            community.college.uppercase(Locale.US),
                            style = TextStyle(RetroHackInk, 14.sp, FontWeight.Black),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        BasicText(
                            "${community.memberCount} MEMBERS",
                            style = TextStyle(RetroHackInk.copy(alpha = 0.62f), 10.sp, FontWeight.Black, letterSpacing = 0.7.sp)
                        )
                    }
                }
                community.description?.let {
                    BasicText(
                        it.uppercase(Locale.US),
                        style = TextStyle(RetroHackInk.copy(alpha = 0.72f), 11.sp, FontWeight.Black, lineHeight = 15.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (community.isMember) {
                    RetroHackButton(
                        label = "OPEN COMMUNITY CHAT",
                        background = RetroHackBlue,
                        content = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenGroupChat(community.groupId) }
                    )
                } else {
                    RetroHackButton(
                        label = if (community.verificationStatus == "verified") "JOIN COMMUNITY" else "VERIFY + JOIN",
                        background = RetroHackYellow,
                        content = RetroHackInk,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onJoinCommunity(community) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    body: String,
    contentColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .retroHackPanel(Color.White.copy(alpha = 0.72f))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicText(
            title.uppercase(Locale.US),
            style = TextStyle(RetroHackInk, 12.sp, FontWeight.Black, letterSpacing = 0.8.sp)
        )
        BasicText(
            body.uppercase(Locale.US),
            style = TextStyle(RetroHackInk.copy(alpha = 0.62f), 10.sp, FontWeight.Black, lineHeight = 14.sp)
        )
    }
}

@Composable
private fun HackathonTinyChip(label: String, tint: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .widthIn(max = 180.dp)
            .background(tint)
            .border(2.dp, RetroHackInk, RoundedCornerShape(0.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        BasicText(
            label.uppercase(Locale.US),
            style = TextStyle(contentColor, 10.sp, FontWeight.Black, letterSpacing = 0.4.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CreateHackathonDialog(
    currentUser: User?,
    onDismiss: () -> Unit,
    onCreate: (CreateHackathonRequest) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var organizer by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("college_fest") }
    var sourceUrl by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    val startsAt = remember { Instant.now().plus(7, ChronoUnit.DAYS).toString() }
    val endsAt = remember { Instant.now().plus(9, ChronoUnit.DAYS).toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post hackathon") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = organizer, onValueChange = { organizer = it }, label = { Text("Organizer") }, singleLine = true)
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 3)
                OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source") }, singleLine = true)
                OutlinedTextField(value = sourceUrl, onValueChange = { sourceUrl = it }, label = { Text("Source URL") }, singleLine = true)
                OutlinedTextField(value = skills, onValueChange = { skills = it }, label = { Text("Skills, comma separated") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        CreateHackathonRequest(
                            title = title,
                            organizer = organizer.takeIf { it.isNotBlank() },
                            description = description,
                            source = source,
                            sourceUrl = sourceUrl.takeIf { it.isNotBlank() },
                            college = currentUser?.college,
                            startsAt = startsAt,
                            endsAt = endsAt,
                            skills = skills.split(',').map { it.trim() }.filter { it.isNotBlank() },
                            tags = listOfNotNull(currentUser?.college, source).filter { it.isNotBlank() }
                        )
                    )
                },
                enabled = title.isNotBlank() && description.isNotBlank()
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun sourceLabel(source: String): String {
    return when (source.lowercase()) {
        "devfolio" -> "Devfolio"
        "mlh" -> "MLH"
        "devpost" -> "Devpost"
        "hackerearth" -> "HackerEarth"
        "college_fest" -> "College fest"
        "custom" -> "Custom"
        else -> "Hackathon"
    }
}

private val HackathonDateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("MMM d")
    .withZone(ZoneId.systemDefault())

private val HackathonDateTimeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("MMM d, yyyy h:mm a")
    .withZone(ZoneId.systemDefault())

private fun sortHackathons(hackathons: List<Hackathon>, sort: String): List<Hackathon> {
    return when (sort) {
        "deadline" -> hackathons.sortedBy { parseHackathonInstant(it.registrationDeadline ?: it.endsAt) ?: Instant.MAX }
        "starts" -> hackathons.sortedBy { parseHackathonInstant(it.startsAt) ?: Instant.MAX }
        "prize" -> hackathons.sortedByDescending { prizeValue(it.prizeSummary) }
        else -> hackathons
    }
}

private fun parseHackathonInstant(value: String?): Instant? {
    return runCatching {
        value?.takeIf { it.isNotBlank() }?.let { Instant.parse(it) }
    }.getOrNull()
}

private fun formatHackathonDateRange(hackathon: Hackathon): String {
    val startsAt = parseHackathonInstant(hackathon.startsAt)
    val endsAt = parseHackathonInstant(hackathon.endsAt)
    if (startsAt == null && endsAt == null) return "Dates TBA"
    val startText = startsAt?.let { HackathonDateFormatter.format(it) }
    val endText = endsAt?.let { HackathonDateFormatter.format(it) }
    return listOfNotNull(startText, endText).distinct().joinToString(" - ")
}

private fun formatHackathonFullDateRange(hackathon: Hackathon): String {
    val startsAt = parseHackathonInstant(hackathon.startsAt)
    val endsAt = parseHackathonInstant(hackathon.endsAt)
    if (startsAt == null && endsAt == null) return "Dates TBA"
    val startText = startsAt?.let { HackathonDateTimeFormatter.format(it) }
    val endText = endsAt?.let { HackathonDateTimeFormatter.format(it) }
    return listOfNotNull(startText, endText).distinct().joinToString(" - ")
}

private fun formatHackathonDateTime(value: String?): String {
    return parseHackathonInstant(value)?.let { HackathonDateTimeFormatter.format(it) } ?: "Not listed"
}

private fun hackathonTimeLeft(hackathon: Hackathon): String {
    val target = parseHackathonInstant(hackathon.registrationDeadline ?: hackathon.endsAt) ?: return hackathon.status
    val now = Instant.now()
    if (target.isBefore(now)) return "Ended"
    val days = Duration.between(now, target).toDays().coerceAtLeast(0)
    return when {
        days == 0L -> "Today"
        days == 1L -> "1 day left"
        days < 45L -> "$days days left"
        else -> "${((days + 29L) / 30L)} months left"
    }
}

private fun prizeValue(prizeSummary: String?): Long {
    return prizeSummary
        ?.replace(Regex("[^0-9]"), "")
        ?.toLongOrNull()
        ?: 0L
}

private fun sourceAccent(source: String, fallback: Color): Color {
    return when (source.lowercase()) {
        "devpost" -> Color(0xFF2563EB)
        "hackerearth" -> Color(0xFFF59E0B)
        "devfolio" -> Color(0xFF7C3AED)
        "mlh" -> Color(0xFFEF4444)
        "college_fest" -> Color(0xFF10B981)
        else -> fallback
    }
}

private fun openExternalHackathon(context: Context, sourceUrl: String?) {
    val url = sourceUrl?.takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
