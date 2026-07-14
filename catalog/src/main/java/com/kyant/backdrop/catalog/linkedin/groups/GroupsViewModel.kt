package com.kyant.backdrop.catalog.linkedin.groups

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.GroupSocketManager
import com.kyant.backdrop.catalog.network.GroupsApiService
import com.kyant.backdrop.catalog.network.PostsApiService
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.backdrop.catalog.notifications.MessageNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID

private val json = Json { ignoreUnknownKeys = true }

// ==================== UI State ====================

enum class GroupsTab {
    MY_GROUPS,
    DISCOVER,
    INVITES
}

enum class GroupDetailTab {
    POSTS,
    ABOUT,
    MEMBERS
}

data class GroupsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    
    // Hub state
    val activeTab: GroupsTab = GroupsTab.MY_GROUPS,
    val myGroups: List<Group> = emptyList(),
    val discoverGroups: List<Group> = emptyList(),
    val pendingInvites: List<GroupInvite> = emptyList(),
    val categories: List<GroupCategory> = emptyList(),
    
    // Search & Filters
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    
    // Create group modal
    val showCreateModal: Boolean = false,
    val isCreatingGroup: Boolean = false,
    val messageShortcutPromptGroup: Group? = null,
    
    // Detail page state
    val selectedGroup: Group? = null,
    val detailTab: GroupDetailTab = GroupDetailTab.POSTS,
    val groupPosts: List<GroupPost> = emptyList(),
    val groupMembers: List<GroupMember> = emptyList(),
    val currentInviteLink: GroupInviteLinkResponse? = null,
    val inviteLinkPreview: GroupInviteLinkResponse? = null,
    val groupJoinRequests: List<GroupJoinRequest> = emptyList(),
    val inviteUserResults: List<MentionUser> = emptyList(),
    
    // Actions in progress
    val joiningGroupIds: Set<String> = emptySet(),
    val leavingGroupIds: Set<String> = emptySet(),
    val isUpdatingGroupAppearance: Boolean = false,
    val groupAppearanceError: String? = null,
    val isSavingGroupSettings: Boolean = false,
    val groupSettingsError: String? = null,
    val deletingGroupIds: Set<String> = emptySet(),
    val updatingMemberIds: Set<String> = emptySet(),
    val removingMemberIds: Set<String> = emptySet(),
    val isLoadingInviteLink: Boolean = false,
    val isLoadingInvitePreview: Boolean = false,
    val isSearchingInviteUsers: Boolean = false,
    val invitingUserIds: Set<String> = emptySet(),
    val respondingInviteIds: Set<String> = emptySet(),
    val respondingJoinRequestIds: Set<String> = emptySet(),
    val updatingMessageShortcutIds: Set<String> = emptySet(),
    val inviteLinkError: String? = null,
    
    // Pagination
    val currentPage: Int = 1,
    val hasMoreGroups: Boolean = false
)

data class GroupChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val group: Group? = null,
    val messages: List<GroupMessage> = emptyList(),
    val typingUsers: List<GroupSocketManager.GroupTypingEvent> = emptyList(),
    val onlineCount: Int = 0,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val isSending: Boolean = false,
    val replyingTo: GroupMessage? = null,
    val connectionState: GroupSocketManager.ConnectionState = GroupSocketManager.ConnectionState.DISCONNECTED
)

// ==================== ViewModel ====================

class GroupsViewModel(private val context: Context) : ViewModel() {
    companion object {
        private const val TAG = "GroupsViewModel"
    }
    
    // Hub state
    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()
    
    // Chat state
    private val _chatState = MutableStateFlow(GroupChatUiState())
    val chatState: StateFlow<GroupChatUiState> = _chatState.asStateFlow()
    
    private var currentUserId: String? = null
    
    init {
        loadCurrentUser()
        observeSocketEvents()
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            currentUserId = ApiClient.getCurrentUserId(context)
            GroupSocketManager.currentUserId = currentUserId
        }
    }

    private suspend fun cacheOwnerId(): String? {
        currentUserId?.let { return it }
        return ApiClient.getCurrentUserId(context).also { userId ->
            currentUserId = userId
            GroupSocketManager.currentUserId = userId
        }
    }
    
    // ==================== Hub Actions ====================
    
    fun setActiveTab(tab: GroupsTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab, error = null)
        when (tab) {
            GroupsTab.MY_GROUPS -> loadMyGroups()
            GroupsTab.DISCOVER -> loadDiscoverGroups()
            GroupsTab.INVITES -> loadPendingInvites()
        }
    }
    
    fun loadMyGroups(refresh: Boolean = false) {
        if (_uiState.value.isLoading && !refresh && _uiState.value.myGroups.isNotEmpty()) return
        
        viewModelScope.launch {
            val ownerId = cacheOwnerId()
            val cached = if (!refresh) GroupsLocalCache.readMyGroups(context, ownerId) else null
            if (cached != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    myGroups = cached.groups,
                    hasMoreGroups = cached.pagination?.let { it.page < it.totalPages } ?: false,
                    currentPage = 1
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    currentPage = if (refresh) 1 else _uiState.value.currentPage
                )
            }
            
            GroupsApiService.getMyGroups(context, page = 1).fold(
                onSuccess = { response ->
                    GroupsLocalCache.writeMyGroups(context, ownerId, response)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        myGroups = response.groups,
                        hasMoreGroups = response.pagination?.let { 
                            it.page < it.totalPages 
                        } ?: false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (cached != null) null else e.message
                    )
                }
            )
        }
    }
    
    fun loadDiscoverGroups(refresh: Boolean = false) {
        if (_uiState.value.isLoading && !refresh && _uiState.value.discoverGroups.isNotEmpty()) return
        
        viewModelScope.launch {
            val ownerId = cacheOwnerId()
            val search = _uiState.value.searchQuery.takeIf { it.isNotBlank() }
            val category = _uiState.value.selectedCategory
            val cached = if (!refresh) {
                GroupsLocalCache.readDiscoverGroups(context, ownerId, search, category)
            } else {
                null
            }
            if (cached != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    discoverGroups = cached.groups,
                    hasMoreGroups = cached.pagination?.let { it.page < it.totalPages } ?: false,
                    currentPage = 1
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            GroupsApiService.discoverGroups(
                context = context,
                search = search,
                category = category,
                page = 1
            ).fold(
                onSuccess = { response ->
                    GroupsLocalCache.writeDiscoverGroups(context, ownerId, search, category, response)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        discoverGroups = response.groups,
                        hasMoreGroups = response.pagination?.let { 
                            it.page < it.totalPages 
                        } ?: false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (cached != null) null else e.message
                    )
                }
            )
        }
    }
    
    fun loadPendingInvites() {
        viewModelScope.launch {
            val ownerId = cacheOwnerId()
            val cached = GroupsLocalCache.readPendingInvites(context, ownerId)
            if (cached != null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    pendingInvites = cached.invites
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            GroupsApiService.getPendingInvites(context).fold(
                onSuccess = { response ->
                    GroupsLocalCache.writePendingInvites(context, ownerId, response)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        pendingInvites = response.invites
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (cached != null) null else e.message
                    )
                }
            )
        }
    }
    
    fun loadCategories() {
        viewModelScope.launch {
            GroupsLocalCache.readCategories(context)?.let { cached ->
                _uiState.value = _uiState.value.copy(categories = cached.categories)
            }
            GroupsApiService.getCategories(context).fold(
                onSuccess = { response ->
                    GroupsLocalCache.writeCategories(context, response)
                    _uiState.value = _uiState.value.copy(categories = response.categories)
                },
                onFailure = { /* ignore */ }
            )
        }
    }
    
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (_uiState.value.activeTab == GroupsTab.DISCOVER) {
            loadDiscoverGroups(refresh = true)
        }
    }
    
    fun setSelectedCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        if (_uiState.value.activeTab == GroupsTab.DISCOVER) {
            loadDiscoverGroups(refresh = true)
        }
    }
    
    // ==================== Group Actions ====================
    
    fun joinGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                joiningGroupIds = _uiState.value.joiningGroupIds + groupId
            )
            
            GroupsApiService.joinGroup(context, groupId).fold(
                onSuccess = { response ->
                    Log.d(TAG, "Join result: ${response.status} - ${response.message}")
                    // Refresh lists
                    loadMyGroups(refresh = true)
                    loadDiscoverGroups(refresh = true)
                    
                    // Update selected group if viewing
                    _uiState.value.selectedGroup?.let { group ->
                        if (group.id == groupId) {
                            _uiState.value = _uiState.value.copy(
                                selectedGroup = group.copy(
                                    isMember = true,
                                    memberRole = "member",
                                    memberCount = group.memberCount + 1
                                )
                            )
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        joiningGroupIds = _uiState.value.joiningGroupIds - groupId
                    )
                    showMessageShortcutPromptForGroup(groupId)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        joiningGroupIds = _uiState.value.joiningGroupIds - groupId,
                        error = e.message
                    )
                }
            )
        }
    }
    
    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                leavingGroupIds = _uiState.value.leavingGroupIds + groupId
            )
            
            GroupsApiService.leaveGroup(context, groupId).fold(
                onSuccess = {
                    loadMyGroups(refresh = true)
                    loadDiscoverGroups(refresh = true)
                    
                    _uiState.value.selectedGroup?.let { group ->
                        if (group.id == groupId) {
                            _uiState.value = _uiState.value.copy(
                                selectedGroup = group.copy(
                                    isMember = false,
                                    memberRole = null,
                                    isAddedToMessages = false,
                                    memberCount = (group.memberCount - 1).coerceAtLeast(0)
                                )
                            )
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        leavingGroupIds = _uiState.value.leavingGroupIds - groupId,
                        messageShortcutPromptGroup = _uiState.value.messageShortcutPromptGroup
                            ?.takeUnless { it.id == groupId }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        leavingGroupIds = _uiState.value.leavingGroupIds - groupId,
                        error = e.message
                    )
                }
            )
        }
    }
    
    fun showCreateModal(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCreateModal = show)
    }

    fun dismissMessageShortcutPrompt() {
        _uiState.value = _uiState.value.copy(messageShortcutPromptGroup = null)
    }

    private fun knownGroupForPrompt(groupId: String, fallback: Group? = null): Group? {
        val state = _uiState.value
        return fallback
            ?: state.selectedGroup?.takeIf { it.id == groupId }
            ?: state.myGroups.firstOrNull { it.id == groupId }
            ?: state.discoverGroups.firstOrNull { it.id == groupId }
            ?: state.pendingInvites.firstOrNull { it.group?.id == groupId }?.group
            ?: _chatState.value.group?.takeIf { it.id == groupId }
    }

    private fun showMessageShortcutPromptForGroup(groupId: String, fallback: Group? = null) {
        val knownGroup = knownGroupForPrompt(groupId, fallback)
        if (knownGroup != null) {
            val promptGroup = knownGroup.copy(
                isMember = true,
                memberRole = knownGroup.memberRole ?: "member"
            )
            if (!promptGroup.isAddedToMessages) {
                _uiState.value = _uiState.value.copy(messageShortcutPromptGroup = promptGroup)
            }
            return
        }

        viewModelScope.launch {
            GroupsApiService.getGroup(context, groupId).fold(
                onSuccess = { group ->
                    if (group.isMember && !group.isAddedToMessages) {
                        _uiState.value = _uiState.value.copy(messageShortcutPromptGroup = group)
                    }
                },
                onFailure = { /* Prompt is optional; leave the joined flow untouched. */ }
            )
        }
    }

    fun setGroupMessageShortcut(
        group: Group,
        enabled: Boolean,
        onUpdated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                updatingMessageShortcutIds = _uiState.value.updatingMessageShortcutIds + group.id,
                error = null
            )

            GroupsApiService.setMessageShortcut(context, group.id, enabled).fold(
                onSuccess = {
                    val updateGroup: (Group) -> Group = { candidate ->
                        if (candidate.id == group.id) candidate.copy(isAddedToMessages = enabled) else candidate
                    }
                    _uiState.value = _uiState.value.let { state ->
                        state.copy(
                            myGroups = state.myGroups.map(updateGroup),
                            discoverGroups = state.discoverGroups.map(updateGroup),
                            selectedGroup = state.selectedGroup?.let(updateGroup),
                            messageShortcutPromptGroup = state.messageShortcutPromptGroup
                                ?.takeUnless { prompt -> prompt.id == group.id },
                            updatingMessageShortcutIds = state.updatingMessageShortcutIds - group.id
                        )
                    }
                    _chatState.value = _chatState.value.copy(
                        group = _chatState.value.group?.let(updateGroup)
                    )
                    onUpdated()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        updatingMessageShortcutIds = _uiState.value.updatingMessageShortcutIds - group.id,
                        error = e.message
                    )
                }
            )
        }
    }
    
    fun createGroup(
        name: String,
        description: String?,
        privacy: String,
        category: String?,
        rules: List<String>
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingGroup = true, error = null)
            
            GroupsApiService.createGroup(
                context = context,
                name = name,
                description = description,
                privacy = privacy,
                category = category,
                rules = rules
            ).fold(
                onSuccess = { group ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingGroup = false,
                        showCreateModal = false,
                        messageShortcutPromptGroup = group
                    )
                    loadMyGroups(refresh = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingGroup = false,
                        error = e.message
                    )
                }
            )
        }
    }
    
    // ==================== Detail Page ====================
    
    fun loadGroupDetail(identifier: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            GroupsApiService.getGroup(context, identifier).fold(
                onSuccess = { group ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedGroup = group,
                        detailTab = GroupDetailTab.POSTS
                    )
                    // Load initial data
                    loadGroupPosts(group.id)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }
    
    fun setDetailTab(tab: GroupDetailTab) {
        _uiState.value = _uiState.value.copy(detailTab = tab)
        val groupId = _uiState.value.selectedGroup?.id ?: return
        
        when (tab) {
            GroupDetailTab.POSTS -> loadGroupPosts(groupId)
            GroupDetailTab.MEMBERS -> loadGroupMembers(groupId)
            GroupDetailTab.ABOUT -> { /* Data already in selectedGroup */ }
        }
    }
    
    fun loadGroupPosts(groupId: String) {
        viewModelScope.launch {
            GroupsApiService.getGroupPosts(context, groupId).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(groupPosts = response.posts)
                },
                onFailure = { /* ignore */ }
            )
        }
    }
    
    fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            GroupsApiService.getGroupMembers(context, groupId).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(groupMembers = response.members)
                },
                onFailure = { /* ignore */ }
            )
        }
    }

    fun loadGroupInviteLink(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingInviteLink = true, inviteLinkError = null)
            GroupsApiService.getGroupInviteLink(context, groupId).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingInviteLink = false,
                        currentInviteLink = response,
                        inviteLinkError = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingInviteLink = false,
                        currentInviteLink = null,
                        inviteLinkError = e.message
                    )
                }
            )
        }
    }

    fun loadGroupInvitePreview(inviteCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingInvitePreview = true,
                inviteLinkError = null,
                inviteLinkPreview = null
            )
            GroupsApiService.previewGroupInviteLink(context, inviteCode).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingInvitePreview = false,
                        inviteLinkPreview = response,
                        inviteLinkError = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingInvitePreview = false,
                        inviteLinkError = e.message ?: "Invite link is not available"
                    )
                }
            )
        }
    }

    fun joinGroupInviteLink(inviteCode: String, onFinished: (GroupInviteActionResponse) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingInvitePreview = true, inviteLinkError = null)
            GroupsApiService.joinGroupByInviteLink(context, inviteCode).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(isLoadingInvitePreview = false, inviteLinkError = null)
                    loadMyGroups(refresh = true)
                    loadDiscoverGroups(refresh = true)
                    response.groupId.takeIf { it.isNotBlank() }?.let { loadGroupDetail(it) }
                    if (response.status == "joined" || response.status == "already_member") {
                        response.groupId.takeIf { it.isNotBlank() }?.let { showMessageShortcutPromptForGroup(it) }
                    }
                    onFinished(response)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingInvitePreview = false,
                        inviteLinkError = e.message ?: "Could not join from this invite link"
                    )
                }
            )
        }
    }

    fun updateInviteLinkVisibility(groupId: String, visibility: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingInviteLink = true, groupSettingsError = null)
            GroupsApiService.updateGroupInviteLinkVisibility(context, groupId, visibility).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingInviteLink = false,
                        currentInviteLink = response,
                        groupSettingsError = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingInviteLink = false,
                        groupSettingsError = e.message ?: "Failed to update invite link"
                    )
                }
            )
        }
    }

    fun resetInviteLink(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingInviteLink = true, groupSettingsError = null)
            GroupsApiService.resetGroupInviteLink(context, groupId).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingInviteLink = false,
                        currentInviteLink = response,
                        groupSettingsError = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingInviteLink = false,
                        groupSettingsError = e.message ?: "Failed to reset invite link"
                    )
                }
            )
        }
    }

    fun searchInviteUsers(query: String) {
        val normalized = query.trim()
        if (normalized.length < 2) {
            _uiState.value = _uiState.value.copy(inviteUserResults = emptyList(), isSearchingInviteUsers = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingInviteUsers = true)
            PostsApiService.searchMentions(context, normalized, limit = 8).fold(
                onSuccess = { response ->
                    val memberIds = _uiState.value.groupMembers.map { it.userId }.toSet()
                    _uiState.value = _uiState.value.copy(
                        isSearchingInviteUsers = false,
                        inviteUserResults = response.users.filterNot { it.id in memberIds }
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isSearchingInviteUsers = false)
                }
            )
        }
    }

    fun createGroupInvite(groupId: String, userId: String, message: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                invitingUserIds = _uiState.value.invitingUserIds + userId,
                groupSettingsError = null
            )
            GroupsApiService.createGroupInvite(context, groupId, userId, message).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        invitingUserIds = _uiState.value.invitingUserIds - userId,
                        inviteUserResults = _uiState.value.inviteUserResults.filterNot { it.id == userId },
                        groupSettingsError = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        invitingUserIds = _uiState.value.invitingUserIds - userId,
                        groupSettingsError = e.message ?: "Failed to send invite"
                    )
                }
            )
        }
    }

    fun respondToGroupInvite(inviteId: String, action: String) {
        viewModelScope.launch {
            val invitedGroup = _uiState.value.pendingInvites.firstOrNull { it.id == inviteId }?.group
            _uiState.value = _uiState.value.copy(
                respondingInviteIds = _uiState.value.respondingInviteIds + inviteId,
                error = null
            )
            GroupsApiService.respondToGroupInvite(context, inviteId, action).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        respondingInviteIds = _uiState.value.respondingInviteIds - inviteId,
                        pendingInvites = _uiState.value.pendingInvites.filterNot { it.id == inviteId }
                    )
                    if (response.status == "joined" || response.status == "already_member") {
                        loadMyGroups(refresh = true)
                        showMessageShortcutPromptForGroup(response.groupId, invitedGroup)
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        respondingInviteIds = _uiState.value.respondingInviteIds - inviteId,
                        error = e.message
                    )
                }
            )
        }
    }

    fun loadGroupJoinRequests(groupId: String) {
        viewModelScope.launch {
            GroupsApiService.getGroupJoinRequests(context, groupId).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(groupJoinRequests = response.requests)
                },
                onFailure = { /* Member/admin-only surface; ignore in non-admin contexts. */ }
            )
        }
    }

    fun respondToGroupJoinRequest(groupId: String, requestId: String, action: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                respondingJoinRequestIds = _uiState.value.respondingJoinRequestIds + requestId,
                groupSettingsError = null
            )
            GroupsApiService.respondToGroupJoinRequest(context, groupId, requestId, action).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        respondingJoinRequestIds = _uiState.value.respondingJoinRequestIds - requestId,
                        groupJoinRequests = _uiState.value.groupJoinRequests.filterNot { it.id == requestId }
                    )
                    if (response.status == "joined" || response.status == "already_member") {
                        loadGroupMembers(groupId)
                        loadGroupDetail(groupId)
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        respondingJoinRequestIds = _uiState.value.respondingJoinRequestIds - requestId,
                        groupSettingsError = e.message ?: "Failed to update join request"
                    )
                }
            )
        }
    }

    fun updateGroupSettings(
        groupId: String,
        name: String,
        description: String?,
        privacy: String,
        category: String?,
        rules: List<String>
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingGroupSettings = true,
                groupSettingsError = null
            )

            GroupsApiService.updateGroup(
                context = context,
                groupId = groupId,
                name = name,
                description = description,
                privacy = privacy,
                category = category,
                rules = rules
            ).fold(
                onSuccess = { updatedGroup ->
                    val currentState = _uiState.value
                    _uiState.value = currentState.copy(
                        isSavingGroupSettings = false,
                        groupSettingsError = null,
                        selectedGroup = updatedGroup,
                        myGroups = currentState.myGroups.map { group ->
                            if (group.id == groupId) updatedGroup else group
                        },
                        discoverGroups = currentState.discoverGroups.map { group ->
                            if (group.id == groupId) updatedGroup else group
                        }
                    )
                    if (_chatState.value.group?.id == groupId) {
                        _chatState.value = _chatState.value.copy(group = updatedGroup)
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSavingGroupSettings = false,
                        groupSettingsError = e.message ?: "Failed to update group"
                    )
                }
            )
        }
    }

    fun updateMemberRole(groupId: String, userId: String, role: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                updatingMemberIds = _uiState.value.updatingMemberIds + userId,
                groupSettingsError = null
            )

            GroupsApiService.updateMemberRole(context, groupId, userId, role).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        updatingMemberIds = _uiState.value.updatingMemberIds - userId,
                        groupMembers = _uiState.value.groupMembers.map { member ->
                            if (member.userId == userId) member.copy(role = role.lowercase()) else member
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        updatingMemberIds = _uiState.value.updatingMemberIds - userId,
                        groupSettingsError = e.message ?: "Failed to update member role"
                    )
                }
            )
        }
    }

    fun removeMember(groupId: String, userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                removingMemberIds = _uiState.value.removingMemberIds + userId,
                groupSettingsError = null
            )

            GroupsApiService.removeMember(context, groupId, userId).fold(
                onSuccess = {
                    val currentState = _uiState.value
                    val updatedSelectedGroup = currentState.selectedGroup
                        ?.takeIf { it.id == groupId }
                        ?.let { group -> group.copy(memberCount = (group.memberCount - 1).coerceAtLeast(0)) }

                    _uiState.value = currentState.copy(
                        removingMemberIds = currentState.removingMemberIds - userId,
                        groupMembers = currentState.groupMembers.filterNot { it.userId == userId },
                        selectedGroup = updatedSelectedGroup ?: currentState.selectedGroup,
                        myGroups = currentState.myGroups.map { group ->
                            if (group.id == groupId) {
                                group.copy(memberCount = (group.memberCount - 1).coerceAtLeast(0))
                            } else {
                                group
                            }
                        },
                        discoverGroups = currentState.discoverGroups.map { group ->
                            if (group.id == groupId) {
                                group.copy(memberCount = (group.memberCount - 1).coerceAtLeast(0))
                            } else {
                                group
                            }
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        removingMemberIds = _uiState.value.removingMemberIds - userId,
                        groupSettingsError = e.message ?: "Failed to remove member"
                    )
                }
            )
        }
    }

    fun deleteGroup(groupId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                deletingGroupIds = _uiState.value.deletingGroupIds + groupId,
                groupSettingsError = null
            )

            GroupsApiService.deleteGroup(context, groupId).fold(
                onSuccess = {
                    val currentState = _uiState.value
                    _uiState.value = currentState.copy(
                        deletingGroupIds = currentState.deletingGroupIds - groupId,
                        selectedGroup = null,
                        groupPosts = emptyList(),
                        groupMembers = emptyList(),
                        myGroups = currentState.myGroups.filterNot { it.id == groupId },
                        discoverGroups = currentState.discoverGroups.filterNot { it.id == groupId }
                    )
                    onDeleted()
                    loadMyGroups(refresh = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        deletingGroupIds = _uiState.value.deletingGroupIds - groupId,
                        groupSettingsError = e.message ?: "Failed to delete group"
                    )
                }
            )
        }
    }

    fun uploadGroupIcon(
        groupId: String,
        uri: Uri,
        fileName: String,
        mimeType: String
    ) {
        uploadGroupAppearance(
            groupId = groupId,
            uri = uri,
            fileName = fileName.ifBlank { "group-icon.jpg" },
            mimeType = mimeType,
            upload = { bytes, safeName, safeMimeType ->
                GroupsApiService.uploadGroupIcon(
                    context = context,
                    groupId = groupId,
                    imageBytes = bytes,
                    filename = safeName,
                    mimeType = safeMimeType
                )
            },
            applyUrl = { group, url -> group.copy(iconImage = url) },
            extractUrl = { response -> response.iconUrl },
            defaultError = "Failed to upload group icon"
        )
    }

    fun uploadGroupCover(
        groupId: String,
        uri: Uri,
        fileName: String,
        mimeType: String
    ) {
        uploadGroupAppearance(
            groupId = groupId,
            uri = uri,
            fileName = fileName.ifBlank { "group-cover.jpg" },
            mimeType = mimeType,
            upload = { bytes, safeName, safeMimeType ->
                GroupsApiService.uploadGroupCover(
                    context = context,
                    groupId = groupId,
                    imageBytes = bytes,
                    filename = safeName,
                    mimeType = safeMimeType
                )
            },
            applyUrl = { group, url -> group.copy(coverImage = url) },
            extractUrl = { response -> response.coverUrl },
            defaultError = "Failed to upload group cover"
        )
    }

    private fun <T> uploadGroupAppearance(
        groupId: String,
        uri: Uri,
        fileName: String,
        mimeType: String,
        upload: suspend (ByteArray, String, String) -> Result<T>,
        applyUrl: (Group, String) -> Group,
        extractUrl: (T) -> String,
        defaultError: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingGroupAppearance = true,
                groupAppearanceError = null
            )

            val imageBytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes()
                }
            }

            if (imageBytes == null || imageBytes.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isUpdatingGroupAppearance = false,
                    groupAppearanceError = "Could not read the selected image"
                )
                return@launch
            }

            upload(imageBytes, fileName, mimeType).fold(
                onSuccess = { response ->
                    val updatedUrl = extractUrl(response)
                    val currentState = _uiState.value
                    val updatedSelectedGroup = currentState.selectedGroup
                        ?.takeIf { it.id == groupId }
                        ?.let { applyUrl(it, updatedUrl) }

                    _uiState.value = currentState.copy(
                        isUpdatingGroupAppearance = false,
                        groupAppearanceError = null,
                        selectedGroup = updatedSelectedGroup ?: currentState.selectedGroup,
                        myGroups = currentState.myGroups
                            .map { group -> if (group.id == groupId) applyUrl(group, updatedUrl) else group },
                        discoverGroups = currentState.discoverGroups
                            .map { group -> if (group.id == groupId) applyUrl(group, updatedUrl) else group }
                    )

                    val chatGroup = _chatState.value.group
                    if (chatGroup?.id == groupId) {
                        _chatState.value = _chatState.value.copy(group = applyUrl(chatGroup, updatedUrl))
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isUpdatingGroupAppearance = false,
                        groupAppearanceError = e.message ?: defaultError
                    )
                }
            )
        }
    }
    
    fun clearSelectedGroup() {
        _uiState.value = _uiState.value.copy(
            selectedGroup = null,
            groupPosts = emptyList(),
            groupMembers = emptyList(),
            currentInviteLink = null,
            groupJoinRequests = emptyList(),
            inviteUserResults = emptyList()
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    // ==================== Chat Functions ====================
    
    fun loadGroupChat(groupId: String) {
        viewModelScope.launch {
            _chatState.value = GroupChatUiState(isLoading = true)
            MessageNotificationManager.clearConversationNotification(
                context,
                MessageNotificationManager.groupNotificationKey(groupId)
            )
            
            // Connect socket and join room
            val token = ApiClient.getToken(context)
            if (token != null) {
                GroupSocketManager.connect(token)
                GroupSocketManager.joinGroup(groupId)
            }
            
            // Load group info
            GroupsApiService.getGroup(context, groupId).fold(
                onSuccess = { group ->
                    _chatState.value = _chatState.value.copy(group = group)
                },
                onFailure = { }
            )
            
            // Load messages
            GroupsApiService.getGroupMessages(context, groupId).fold(
                onSuccess = { messages ->
                    _chatState.value = _chatState.value.copy(
                        isLoading = false,
                        messages = messages,
                        hasMoreMessages = messages.size >= 50
                    )
                },
                onFailure = { e ->
                    _chatState.value = _chatState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }
    
    fun loadMoreMessages() {
        val state = _chatState.value
        if (state.isLoadingMore || !state.hasMoreMessages || state.messages.isEmpty()) return
        
        val groupId = state.group?.id ?: return
        val oldestMessage = state.messages.firstOrNull()?.createdAt ?: return
        
        viewModelScope.launch {
            _chatState.value = _chatState.value.copy(isLoadingMore = true)
            
            GroupsApiService.getGroupMessages(context, groupId, before = oldestMessage).fold(
                onSuccess = { messages ->
                    _chatState.value = _chatState.value.copy(
                        isLoadingMore = false,
                        messages = messages + state.messages,
                        hasMoreMessages = messages.size >= 50
                    )
                },
                onFailure = { e ->
                    _chatState.value = _chatState.value.copy(
                        isLoadingMore = false,
                        error = e.message
                    )
                }
            )
        }
    }
    
    fun sendMessage(content: String, mediaUrl: String? = null, mediaType: String? = null) {
        val groupId = _chatState.value.group?.id ?: return
        if (content.isBlank() && mediaUrl == null) return
        
        val tempId = UUID.randomUUID().toString()
        val replyToMessageId = _chatState.value.replyingTo?.id
        val contentType = when {
            mediaUrl != null && mediaType?.startsWith("image") == true -> "image"
            mediaUrl != null && mediaType?.startsWith("video") == true -> "video"
            mediaUrl != null -> "file"
            else -> "text"
        }
        
        // Optimistic update
        val tempMessage = GroupMessage(
            id = tempId,
            groupId = groupId,
            senderId = currentUserId ?: "",
            sender = GroupUser(
                id = currentUserId ?: "",
                name = "You"
            ),
            content = content,
            contentType = contentType,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            replyToId = replyToMessageId,
            createdAt = java.time.Instant.now().toString()
        )
        
        _chatState.value = _chatState.value.copy(
            messages = _chatState.value.messages + tempMessage,
            isSending = true,
            replyingTo = null
        )
        
        // Send via REST so persistence, real-time fanout, and push notifications share one path.
        viewModelScope.launch {
            GroupsApiService.sendGroupMessage(
                context = context,
                groupId = groupId,
                content = content,
                contentType = contentType,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                replyToId = replyToMessageId
            ).fold(
                onSuccess = { message ->
                    // Replace temp message with real one
                    _chatState.value = _chatState.value.copy(
                        isSending = false,
                        messages = _chatState.value.messages
                            .filterNot { it.id == tempId || it.id == message.id } + message
                    )
                },
                onFailure = { e ->
                    _chatState.value = _chatState.value.copy(
                        isSending = false,
                        error = e.message
                    )
                }
            )
        }
    }
    
    fun setReplyingTo(message: GroupMessage?) {
        _chatState.value = _chatState.value.copy(replyingTo = message)
    }
    
    fun sendTyping(isTyping: Boolean) {
        val groupId = _chatState.value.group?.id ?: return
        GroupSocketManager.sendTyping(groupId, isTyping)
    }
    
    fun leaveGroupChat() {
        val groupId = _chatState.value.group?.id
        if (groupId != null) {
            GroupSocketManager.leaveGroup(groupId)
        }
        _chatState.value = GroupChatUiState()
    }
    
    // ==================== Socket Events ====================
    
    private fun observeSocketEvents() {
        // Connection state
        viewModelScope.launch {
            GroupSocketManager.connectionStateFlow.collect { state ->
                _chatState.value = _chatState.value.copy(connectionState = state)
            }
        }
        
        // New messages
        viewModelScope.launch {
            GroupSocketManager.newMessageFlow.collect { (groupId, messageJson) ->
                if (groupId == _chatState.value.group?.id) {
                    try {
                        val message = json.decodeFromString<GroupMessage>(messageJson)
                        // Avoid duplicates
                        if (_chatState.value.messages.none { it.id == message.id }) {
                            _chatState.value = _chatState.value.copy(
                                messages = _chatState.value.messages + message
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message", e)
                    }
                }
            }
        }
        
        // Typing indicators
        viewModelScope.launch {
            GroupSocketManager.typingFlow.collect { event ->
                if (event.groupId == _chatState.value.group?.id) {
                    val current = _chatState.value.typingUsers.toMutableList()
                    if (event.isTyping) {
                        if (current.none { it.userId == event.userId }) {
                            current.add(event)
                        }
                    } else {
                        current.removeAll { it.userId == event.userId }
                    }
                    _chatState.value = _chatState.value.copy(typingUsers = current)
                }
            }
        }
        
        // Online count
        viewModelScope.launch {
            GroupSocketManager.onlineCountFlow.collect { (groupId, count) ->
                if (groupId == _chatState.value.group?.id) {
                    _chatState.value = _chatState.value.copy(onlineCount = count)
                }
            }
        }
        
        // Message deleted
        viewModelScope.launch {
            GroupSocketManager.messageDeletedFlow.collect { (groupId, _, messageId) ->
                if (groupId == _chatState.value.group?.id) {
                    _chatState.value = _chatState.value.copy(
                        messages = _chatState.value.messages.filter { it.id != messageId }
                    )
                }
            }
        }

        viewModelScope.launch {
            GroupSocketManager.allGroupChatsClearedFlow.collect {
                _chatState.value = _chatState.value.copy(
                    messages = emptyList(),
                    typingUsers = emptyList(),
                    hasMoreMessages = false,
                    isSending = false,
                    replyingTo = null,
                    error = null
                )
                MessageNotificationManager.clearAll(context)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        leaveGroupChat()
    }
    
    // Factory
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GroupsViewModel(context) as T
        }
    }
}
