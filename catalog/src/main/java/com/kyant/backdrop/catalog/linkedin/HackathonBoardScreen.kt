package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.kyant.backdrop.catalog.network.models.HackathonTeam
import com.kyant.backdrop.catalog.network.models.User
import com.kyant.backdrop.catalog.network.models.VerifyCollegeStudentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

private enum class HackathonBoardTab {
    BOARD,
    MY_TEAMS,
    COMMUNITIES
}

data class HackathonBoardUiState(
    val isLoading: Boolean = false,
    val actionInProgress: Boolean = false,
    val error: String? = null,
    val currentUser: User? = null,
    val status: String = "active",
    val search: String = "",
    val hackathons: List<Hackathon> = emptyList(),
    val expandedHackathonId: String? = null,
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
        _uiState.value = _uiState.value.copy(status = status)
        loadHackathons()
    }

    fun setSearch(search: String) {
        _uiState.value = _uiState.value.copy(search = search)
    }

    fun loadHackathons() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isLoading = state.hackathons.isEmpty(), error = null)
            HackathonsApiService.getHackathons(
                context = appContext,
                status = state.status,
                search = state.search.takeIf { it.isNotBlank() }
            ).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hackathons = response.hackathons,
                        error = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                }
            )
        }
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
            _uiState.value = _uiState.value.copy(actionInProgress = true, error = null)
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
                    _uiState.value = _uiState.value.copy(actionInProgress = false)
                    loadHackathons()
                    loadMyTeams()
                    loadTeams(hackathon.id)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(actionInProgress = false, error = error.message)
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
                    _uiState.value = _uiState.value.copy(actionInProgress = false, status = "upcoming")
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        HackathonHeader(
            title = "Hackathon Board",
            contentColor = contentColor,
            onBack = onNavigateBack,
            onCreate = { showCreateDialog = true }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HackathonBoardTab.values().forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = {
                        Text(
                            when (tab) {
                                HackathonBoardTab.BOARD -> "Board"
                                HackathonBoardTab.MY_TEAMS -> "My teams"
                                HackathonBoardTab.COMMUNITIES -> "College"
                            }
                        )
                    }
                )
            }
        }

        uiState.error?.let { error ->
            Text(
                text = error,
                color = Color(0xFFE11D48),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        when (selectedTab) {
            HackathonBoardTab.BOARD -> HackathonBoardList(
                uiState = uiState,
                contentColor = contentColor,
                accentColor = accentColor,
                onStatusChange = viewModel::setStatus,
                onSearchChange = viewModel::setSearch,
                onSearchSubmit = viewModel::loadHackathons,
                onToggleSave = viewModel::toggleSave,
                onFormTeam = viewModel::formTeam,
                onToggleTeams = viewModel::toggleTeams,
                onApplyToTeam = viewModel::applyToTeam,
                onOpenGroupChat = onOpenGroupChat
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
    contentColor: Color,
    onBack: () -> Unit,
    onCreate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = contentColor)
        }
        Text(
            title,
            color = contentColor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onCreate) {
            Icon(Icons.Outlined.Add, contentDescription = "Post hackathon", tint = contentColor)
        }
    }
}

@Composable
private fun HackathonBoardList(
    uiState: HackathonBoardUiState,
    contentColor: Color,
    accentColor: Color,
    onStatusChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onToggleSave: (Hackathon) -> Unit,
    onFormTeam: (Hackathon) -> Unit,
    onToggleTeams: (String) -> Unit,
    onApplyToTeam: (HackathonTeam) -> Unit,
    onOpenGroupChat: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.search,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        TextButton(onClick = onSearchSubmit) { Text("Search") }
                    },
                    singleLine = true,
                    label = { Text("Search hackathons, skills, colleges") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("active", "upcoming", "past").forEach { status ->
                        FilterChip(
                            selected = uiState.status == status,
                            onClick = { onStatusChange(status) },
                            label = { Text(status.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        }

        if (uiState.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            }
        }

        if (!uiState.isLoading && uiState.hackathons.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No hackathons here yet",
                    body = "Post a college fest or check upcoming events.",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }

        items(uiState.hackathons, key = { it.id }) { hackathon ->
            HackathonCard(
                hackathon = hackathon,
                expanded = uiState.expandedHackathonId == hackathon.id,
                teams = uiState.teamsByHackathon[hackathon.id].orEmpty(),
                contentColor = contentColor,
                accentColor = accentColor,
                onToggleSave = { onToggleSave(hackathon) },
                onFormTeam = { onFormTeam(hackathon) },
                onToggleTeams = { onToggleTeams(hackathon.id) },
                onApplyToTeam = onApplyToTeam,
                onOpenGroupChat = onOpenGroupChat
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HackathonCard(
    hackathon: Hackathon,
    expanded: Boolean,
    teams: List<HackathonTeam>,
    contentColor: Color,
    accentColor: Color,
    onToggleSave: () -> Unit,
    onFormTeam: () -> Unit,
    onToggleTeams: () -> Unit,
    onApplyToTeam: (HackathonTeam) -> Unit,
    onOpenGroupChat: (String) -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(8.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Public, contentDescription = null, tint = accentColor)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        hackathon.title,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        listOfNotNull(sourceLabel(hackathon.source), hackathon.organizer, hackathon.college).joinToString(" · "),
                        color = contentColor.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onToggleSave) {
                    Icon(
                        if (hackathon.isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = accentColor
                    )
                }
            }

            Text(
                hackathon.description,
                color = contentColor.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HackathonTinyChip(hackathon.status, accentColor, contentColor)
                HackathonTinyChip("${hackathon.teamMin}-${hackathon.teamMax} per team", accentColor, contentColor)
                hackathon.skills.take(4).forEach { skill ->
                    HackathonTinyChip(skill, Color(0xFF22C55E), contentColor)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onFormTeam, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (hackathon.myTeam != null) "Team Ready" else "Form Team")
                }
                OutlinedButton(onClick = onToggleTeams, modifier = Modifier.weight(1f)) {
                    Text(if (expanded) "Hide Teams" else "View Teams")
                }
            }

            hackathon.myTeam?.groupId?.let { groupId ->
                OutlinedButton(onClick = { onOpenGroupChat(groupId) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open team chat")
                }
            }

            if (expanded) {
                if (teams.isEmpty()) {
                    Text("No open teams yet.", color = contentColor.copy(alpha = 0.62f))
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
            .clip(RoundedCornerShape(8.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(team.name, color = contentColor, fontWeight = FontWeight.SemiBold)
                Text(
                    "${team.memberCount}/${team.maxMembers} members · ${team.status}",
                    color = contentColor.copy(alpha = 0.58f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (team.groupId != null && team.members.any { it.status == "accepted" }) {
                IconButton(onClick = { onOpenGroupChat(team.groupId) }) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "Chat", tint = accentColor)
                }
            }
        }
        if (!team.pitch.isNullOrBlank()) {
            Text(team.pitch, color = contentColor.copy(alpha = 0.74f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            team.requiredSkills.take(3).forEach { skill ->
                AssistChip(onClick = {}, label = { Text(skill) })
            }
        }
        if (team.myApplication?.status == "pending") {
            Text("Application pending", color = accentColor, fontWeight = FontWeight.SemiBold)
        } else if (team.status == "open") {
            OutlinedButton(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                Text("Apply to join")
            }
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
                    body = "Form a team from the board to get a private team chat.",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }
        items(teams, key = { it.id }) { team ->
            ElevatedCard(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(team.name, color = contentColor, fontWeight = FontWeight.Bold)
                    Text("${team.memberCount}/${team.maxMembers} members", color = contentColor.copy(alpha = 0.62f))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        team.requiredSkills.take(5).forEach { HackathonTinyChip(it, accentColor, contentColor) }
                    }
                    team.groupId?.let { groupId ->
                        Button(onClick = { onOpenGroupChat(groupId) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open group chat")
                        }
                    }
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
            ElevatedCard(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.School, contentDescription = null, tint = accentColor)
                        Spacer(Modifier.width(8.dp))
                        Text("Private college spaces", color = contentColor, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        uiState.currentUser?.college ?: "Add your college on profile to create a community.",
                        color = contentColor.copy(alpha = 0.66f)
                    )
                    Button(
                        onClick = onCreateMyCommunity,
                        enabled = !uiState.currentUser?.college.isNullOrBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create my college community")
                    }
                }
            }
        }
        items(uiState.communities, key = { it.id }) { community ->
            ElevatedCard(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (community.canJoin || community.isMember) Icons.Outlined.Verified else Icons.Outlined.School,
                            contentDescription = null,
                            tint = accentColor
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(community.college, color = contentColor, fontWeight = FontWeight.Bold)
                            Text("${community.memberCount} members", color = contentColor.copy(alpha = 0.62f))
                        }
                    }
                    community.description?.let {
                        Text(it, color = contentColor.copy(alpha = 0.72f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (community.isMember) {
                        Button(onClick = { onOpenGroupChat(community.groupId) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open community chat")
                        }
                    } else {
                        OutlinedButton(onClick = { onJoinCommunity(community) }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (community.verificationStatus == "verified") "Join community" else "Verify and join")
                        }
                    }
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
    ElevatedCard(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = contentColor, fontWeight = FontWeight.Bold)
            Text(body, color = contentColor.copy(alpha = 0.66f))
        }
    }
}

@Composable
private fun HackathonTinyChip(label: String, tint: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.13f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = contentColor, style = MaterialTheme.typography.labelSmall)
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
                OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source: devfolio, mlh, college_fest") }, singleLine = true)
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
        "college_fest" -> "College fest"
        else -> "Hackathon"
    }
}
