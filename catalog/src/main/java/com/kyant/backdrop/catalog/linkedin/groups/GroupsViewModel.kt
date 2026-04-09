package com.kyant.backdrop.catalog.linkedin.groups

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.GroupsApiService
import com.kyant.backdrop.catalog.network.GroupSocketManager
import com.kyant.backdrop.catalog.network.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    
    // Detail page state
    val selectedGroup: Group? = null,
    val detailTab: GroupDetailTab = GroupDetailTab.POSTS,
    val groupPosts: List<GroupPost> = emptyList(),
    val groupMembers: List<GroupMember> = emptyList(),
    
    // Actions in progress
    val joiningGroupIds: Set<String> = emptySet(),
    val leavingGroupIds: Set<String> = emptySet(),
    
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
        if (_uiState.value.isLoading && !refresh) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                currentPage = if (refresh) 1 else _uiState.value.currentPage
            )
            
            GroupsApiService.getMyGroups(context, page = 1).fold(
                onSuccess = { response ->
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
                        error = e.message
                    )
                }
            )
        }
    }
    
    fun loadDiscoverGroups(refresh: Boolean = false) {
        if (_uiState.value.isLoading && !refresh) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            GroupsApiService.discoverGroups(
                context = context,
                search = _uiState.value.searchQuery.takeIf { it.isNotBlank() },
                category = _uiState.value.selectedCategory,
                page = 1
            ).fold(
                onSuccess = { response ->
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
                        error = e.message
                    )
                }
            )
        }
    }
    
    fun loadPendingInvites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            GroupsApiService.getPendingInvites(context).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        pendingInvites = response.invites
                    )
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
    
    fun loadCategories() {
        viewModelScope.launch {
            GroupsApiService.getCategories(context).fold(
                onSuccess = { response ->
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
                                    memberCount = (group.memberCount - 1).coerceAtLeast(0)
                                )
                            )
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        leavingGroupIds = _uiState.value.leavingGroupIds - groupId
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
                        showCreateModal = false
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
    
    fun clearSelectedGroup() {
        _uiState.value = _uiState.value.copy(
            selectedGroup = null,
            groupPosts = emptyList(),
            groupMembers = emptyList()
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    // ==================== Chat Functions ====================
    
    fun loadGroupChat(groupId: String) {
        viewModelScope.launch {
            _chatState.value = GroupChatUiState(isLoading = true)
            
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
            replyToId = _chatState.value.replyingTo?.id,
            createdAt = java.time.Instant.now().toString()
        )
        
        _chatState.value = _chatState.value.copy(
            messages = _chatState.value.messages + tempMessage,
            isSending = true,
            replyingTo = null
        )
        
        // Send via socket
        GroupSocketManager.sendMessage(
            groupId = groupId,
            content = content,
            tempId = tempId,
            contentType = contentType,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            replyToId = _chatState.value.replyingTo?.id
        )
        
        // Also send via REST as fallback
        viewModelScope.launch {
            GroupsApiService.sendGroupMessage(
                context = context,
                groupId = groupId,
                content = content,
                contentType = contentType,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                replyToId = _chatState.value.replyingTo?.id
            ).fold(
                onSuccess = { message ->
                    // Replace temp message with real one
                    _chatState.value = _chatState.value.copy(
                        isSending = false,
                        messages = _chatState.value.messages.map {
                            if (it.id == tempId) message else it
                        }
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
