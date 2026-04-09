package com.kyant.backdrop.catalog.linkedin.groups

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.network.GroupsApiService
import com.kyant.backdrop.catalog.network.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ==================== Circles UI State ====================

enum class CirclesTab {
    MY_CIRCLES,
    DISCOVER
}

data class CirclesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Hub state
    val activeTab: CirclesTab = CirclesTab.DISCOVER,
    val myCircles: List<Circle> = emptyList(),
    val discoverCircles: List<Circle> = emptyList(),
    
    // Filters
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    
    // Create modal
    val showCreateModal: Boolean = false,
    val isCreatingCircle: Boolean = false,
    
    // Detail state
    val selectedCircle: Circle? = null,
    val circleMembers: List<CircleMember> = emptyList(),
    val circlePosts: List<CirclePost> = emptyList(),
    
    // Actions
    val joiningCircleIds: Set<String> = emptySet(),
    val leavingCircleIds: Set<String> = emptySet(),
    
    // Free plan limit
    val showUpgradePrompt: Boolean = false,
    val circlesJoinedCount: Int = 0,
    val maxFreeCircles: Int = 3
) {
    // Computed properties for UI
    val maxCircles: Int get() = maxFreeCircles
    val canCreateMore: Boolean get() = circlesJoinedCount < maxFreeCircles
    val isCreating: Boolean get() = isCreatingCircle
    val isJoining: Boolean get() = joiningCircleIds.isNotEmpty()
    
    val filteredMyCircles: List<Circle> get() = if (searchQuery.isBlank()) {
        myCircles
    } else {
        myCircles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    
    val filteredDiscoverCircles: List<Circle> get() = if (searchQuery.isBlank()) {
        discoverCircles
    } else {
        discoverCircles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    
    val currentCircle: Circle? get() = selectedCircle
}

// ==================== Circles ViewModel ====================

class CirclesViewModel(private val context: Context) : ViewModel() {
    companion object {
        private const val TAG = "CirclesViewModel"
    }
    
    private val _uiState = MutableStateFlow(CirclesUiState())
    val uiState: StateFlow<CirclesUiState> = _uiState.asStateFlow()
    
    init {
        loadMyCircles()
        loadDiscoverCircles()
    }
    
    fun setActiveTab(tab: CirclesTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab, error = null)
        when (tab) {
            CirclesTab.MY_CIRCLES -> loadMyCircles()
            CirclesTab.DISCOVER -> loadDiscoverCircles()
        }
    }
    
    fun loadMyCircles(refresh: Boolean = false) {
        if (_uiState.value.isLoading && !refresh) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            GroupsApiService.getMyCircles(context).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        myCircles = response.circles,
                        circlesJoinedCount = response.circles.size
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
    
    fun loadDiscoverCircles(refresh: Boolean = false) {
        if (_uiState.value.isLoading && !refresh) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            GroupsApiService.discoverCircles(
                context = context,
                category = _uiState.value.selectedCategory,
                search = _uiState.value.searchQuery.takeIf { it.isNotBlank() }
            ).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        discoverCircles = response.circles
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
    
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadDiscoverCircles(refresh = true)
    }
    
    fun setSelectedCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadDiscoverCircles(refresh = true)
    }
    
    fun joinCircle(circleId: String) {
        // Check free plan limit
        if (_uiState.value.circlesJoinedCount >= _uiState.value.maxFreeCircles) {
            _uiState.value = _uiState.value.copy(showUpgradePrompt = true)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                joiningCircleIds = _uiState.value.joiningCircleIds + circleId
            )
            
            GroupsApiService.joinCircle(context, circleId).fold(
                onSuccess = { response ->
                    if (response.requiresUpgrade) {
                        _uiState.value = _uiState.value.copy(
                            showUpgradePrompt = true,
                            joiningCircleIds = _uiState.value.joiningCircleIds - circleId
                        )
                    } else {
                        loadMyCircles(refresh = true)
                        loadDiscoverCircles(refresh = true)
                        
                        // Update selected circle if viewing
                        _uiState.value.selectedCircle?.let { circle ->
                            if (circle.id == circleId) {
                                _uiState.value = _uiState.value.copy(
                                    selectedCircle = circle.copy(
                                        isMember = true,
                                        memberCount = circle.memberCount + 1
                                    )
                                )
                            }
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            joiningCircleIds = _uiState.value.joiningCircleIds - circleId
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        joiningCircleIds = _uiState.value.joiningCircleIds - circleId,
                        error = e.message
                    )
                }
            )
        }
    }
    
    fun leaveCircle(circleId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                leavingCircleIds = _uiState.value.leavingCircleIds + circleId
            )
            
            GroupsApiService.leaveCircle(context, circleId).fold(
                onSuccess = {
                    loadMyCircles(refresh = true)
                    loadDiscoverCircles(refresh = true)
                    
                    _uiState.value.selectedCircle?.let { circle ->
                        if (circle.id == circleId) {
                            _uiState.value = _uiState.value.copy(
                                selectedCircle = circle.copy(
                                    isMember = false,
                                    memberCount = (circle.memberCount - 1).coerceAtLeast(0)
                                )
                            )
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        leavingCircleIds = _uiState.value.leavingCircleIds - circleId
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        leavingCircleIds = _uiState.value.leavingCircleIds - circleId,
                        error = e.message
                    )
                }
            )
        }
    }
    
    fun showCreateModal(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCreateModal = show)
    }
    
    fun createCircle(
        name: String,
        description: String?,
        category: String?,
        emoji: String?,
        tags: List<String>,
        isPrivate: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingCircle = true, error = null)
            
            GroupsApiService.createCircle(
                context = context,
                name = name,
                description = description,
                category = category,
                emoji = emoji,
                tags = tags,
                isPrivate = isPrivate
            ).fold(
                onSuccess = { circle ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingCircle = false,
                        showCreateModal = false
                    )
                    loadMyCircles(refresh = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingCircle = false,
                        error = e.message
                    )
                }
            )
        }
    }
    
    fun loadCircleDetail(slug: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            GroupsApiService.getCircle(context, slug).fold(
                onSuccess = { circle ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        selectedCircle = circle
                    )
                    loadCirclePosts(circle.id)
                    loadCircleMembers(circle.id)
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
    
    fun loadCircleMembers(circleId: String) {
        viewModelScope.launch {
            GroupsApiService.getCircleMembers(context, circleId).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(circleMembers = response.members)
                },
                onFailure = { /* ignore */ }
            )
        }
    }
    
    fun loadCirclePosts(circleId: String) {
        viewModelScope.launch {
            GroupsApiService.getCirclePosts(context, circleId).fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(circlePosts = response.posts)
                },
                onFailure = { /* ignore */ }
            )
        }
    }
    
    fun createCirclePost(content: String, mediaUrls: List<String> = emptyList()) {
        val circleId = _uiState.value.selectedCircle?.id ?: return
        
        viewModelScope.launch {
            GroupsApiService.createCirclePost(
                context = context,
                circleId = circleId,
                content = content,
                mediaUrls = mediaUrls
            ).fold(
                onSuccess = { post ->
                    _uiState.value = _uiState.value.copy(
                        circlePosts = listOf(post) + _uiState.value.circlePosts
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            )
        }
    }
    
    fun clearSelectedCircle() {
        _uiState.value = _uiState.value.copy(
            selectedCircle = null,
            circleMembers = emptyList(),
            circlePosts = emptyList()
        )
    }
    
    fun dismissUpgradePrompt() {
        _uiState.value = _uiState.value.copy(showUpgradePrompt = false)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    // Factory
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CirclesViewModel(context) as T
        }
    }
}
