package com.kyant.backdrop.catalog.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.launch

// ==================== Onboarding Experience Entry ====================

data class OnboardingExperienceEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val company: String = "",
    val type: String = "Internship",
    val location: String = "",
    val startDate: String = "", // "2024-01"
    val endDate: String = "",
    val isCurrent: Boolean = false,
    val description: String = "",
    val skills: List<String> = emptyList()
)

// ==================== Onboarding Education Entry ====================

data class OnboardingEducationEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val school: String = "",
    val degree: String = "",
    val fieldOfStudy: String = "",
    val startDate: String = "", // "2024-01"
    val endDate: String = "",
    val isCurrent: Boolean = false,
    val grade: String = "",
    val activities: String = "",
    val description: String = ""
)

// ==================== ViewModel ====================

data class ProfileSetupUiState(
    val currentStep: Int = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    // Step 0 data
    val college: String = "",
    val primaryGoal: String = "",
    val lookingFor: List<String> = emptyList(),
    // Step 1 data (Experience) - client-side only
    val onboardingExperiences: List<OnboardingExperienceEntry> = emptyList(),
    val editingExperience: OnboardingExperienceEntry? = null,
    val showExperienceForm: Boolean = false,
    val savingExperience: Boolean = false,
    // Step 2 data (Education) - client-side only
    val onboardingEducation: List<OnboardingEducationEntry> = emptyList(),
    val editingEducation: OnboardingEducationEntry? = null,
    val showEducationForm: Boolean = false,
    val savingEducation: Boolean = false,
    // Step 3 data (Interests)
    val selectedInterests: List<String> = emptyList(),
    val canTeach: List<String> = emptyList(),
    val interestSearch: String = "",
    val customInterest: String = "",
    // Step 4 data (Matches)
    val matches: List<OnboardingMatch> = emptyList(),
    val isLoadingMatches: Boolean = false,
    // Completion
    val isCompleted: Boolean = false
)

class ProfileSetupViewModel(private val context: android.content.Context) : ViewModel() {
    
    private val _uiState = mutableStateOf(ProfileSetupUiState())
    val uiState: State<ProfileSetupUiState> = _uiState
    
    init {
        loadOnboardingData()
    }
    
    private fun loadOnboardingData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            ApiClient.getOnboarding(context)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentStep = response.onboarding.currentStep,
                        primaryGoal = response.onboarding.primaryGoal ?: "",
                        lookingFor = response.onboarding.lookingFor,
                        selectedInterests = response.onboarding.wantToLearn,
                        canTeach = response.onboarding.canTeach,
                        isCompleted = response.onboarding.isCompleted
                    )
                    // Load college from user profile if needed
                    loadUserCollege()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }
    
    private fun loadUserCollege() {
        viewModelScope.launch {
            ApiClient.getCurrentUser(context)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        college = user.college ?: ""
                    )
                }
        }
    }
    
    fun updateCollege(value: String) {
        _uiState.value = _uiState.value.copy(college = value)
    }
    
    fun selectPrimaryGoal(goalId: String) {
        _uiState.value = _uiState.value.copy(primaryGoal = goalId)
    }
    
    fun toggleLookingFor(id: String) {
        val current = _uiState.value.lookingFor.toMutableList()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _uiState.value = _uiState.value.copy(lookingFor = current)
    }
    
    fun toggleInterest(interest: String) {
        val current = _uiState.value.selectedInterests.toMutableList()
        val teach = _uiState.value.canTeach.toMutableList()
        if (current.contains(interest)) {
            current.remove(interest)
            teach.remove(interest)
        } else {
            current.add(interest)
        }
        _uiState.value = _uiState.value.copy(
            selectedInterests = current,
            canTeach = teach
        )
    }
    
    fun toggleCanTeach(interest: String) {
        if (!_uiState.value.selectedInterests.contains(interest)) return
        val current = _uiState.value.canTeach.toMutableList()
        if (current.contains(interest)) {
            current.remove(interest)
        } else {
            current.add(interest)
        }
        _uiState.value = _uiState.value.copy(canTeach = current)
    }
    
    fun updateInterestSearch(value: String) {
        _uiState.value = _uiState.value.copy(interestSearch = value)
    }
    
    fun updateCustomInterest(value: String) {
        _uiState.value = _uiState.value.copy(customInterest = value)
    }
    
    fun addCustomInterest() {
        val custom = _uiState.value.customInterest.trim()
            .replaceFirstChar { it.uppercase() }
        if (custom.isBlank()) return
        if (_uiState.value.selectedInterests.any { it.equals(custom, ignoreCase = true) }) {
            _uiState.value = _uiState.value.copy(customInterest = "")
            return
        }
        _uiState.value = _uiState.value.copy(
            selectedInterests = _uiState.value.selectedInterests + custom,
            customInterest = ""
        )
    }
    
    fun submitStep0(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            
            val data = mapOf(
                "college" to _uiState.value.college,
                "primaryGoal" to _uiState.value.primaryGoal,
                "lookingFor" to _uiState.value.lookingFor,
                "secondaryGoals" to emptyList<String>()
            )
            
            ApiClient.updateOnboardingStep(context, 0, data)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        currentStep = 1 // Go to Experience step (local)
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message
                    )
                }
        }
    }
    
    // ==================== Experience Step Methods ====================
    
    fun showAddExperience() {
        _uiState.value = _uiState.value.copy(
            showExperienceForm = true,
            editingExperience = OnboardingExperienceEntry()
        )
    }
    
    fun editExperience(experience: OnboardingExperienceEntry) {
        _uiState.value = _uiState.value.copy(
            showExperienceForm = true,
            editingExperience = experience
        )
    }
    
    fun dismissExperienceForm() {
        _uiState.value = _uiState.value.copy(
            showExperienceForm = false,
            editingExperience = null
        )
    }
    
    fun saveExperience(experience: OnboardingExperienceEntry) {
        val current = _uiState.value.onboardingExperiences.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == experience.id }
        if (existingIndex >= 0) {
            current[existingIndex] = experience
        } else {
            current.add(experience)
        }
        _uiState.value = _uiState.value.copy(
            onboardingExperiences = current,
            showExperienceForm = false,
            editingExperience = null
        )
    }
    
    fun deleteExperience(experienceId: String) {
        _uiState.value = _uiState.value.copy(
            onboardingExperiences = _uiState.value.onboardingExperiences.filter { it.id != experienceId },
            showExperienceForm = false,
            editingExperience = null
        )
    }
    
    fun submitExperienceStep(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(savingExperience = true, error = null)
            
            // Save each experience to the actual API
            val experiences = _uiState.value.onboardingExperiences
            var allSuccess = true
            
            for (exp in experiences) {
                val input = ExperienceInput(
                    title = exp.title,
                    company = exp.company,
                    type = exp.type,
                    location = exp.location.ifBlank { null },
                    startDate = exp.startDate,
                    endDate = exp.endDate.ifBlank { null },
                    isCurrent = exp.isCurrent,
                    description = exp.description.ifBlank { null },
                    skills = exp.skills.ifEmpty { null },
                    logo = null
                )
                ApiClient.createExperience(context, input).onFailure {
                    allSuccess = false
                }
            }
            
            if (allSuccess || experiences.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    savingExperience = false,
                    currentStep = 2 // Go to Education step
                )
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    savingExperience = false,
                    error = "Failed to save some experiences"
                )
            }
        }
    }
    
    fun skipExperienceStep() {
        _uiState.value = _uiState.value.copy(currentStep = 2) // Skip to Education
    }
    
    // ==================== Education Step Methods ====================
    
    fun showAddEducation() {
        _uiState.value = _uiState.value.copy(
            showEducationForm = true,
            editingEducation = OnboardingEducationEntry()
        )
    }
    
    fun editEducation(education: OnboardingEducationEntry) {
        _uiState.value = _uiState.value.copy(
            showEducationForm = true,
            editingEducation = education
        )
    }
    
    fun dismissEducationForm() {
        _uiState.value = _uiState.value.copy(
            showEducationForm = false,
            editingEducation = null
        )
    }
    
    fun saveEducation(education: OnboardingEducationEntry) {
        val current = _uiState.value.onboardingEducation.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == education.id }
        if (existingIndex >= 0) {
            current[existingIndex] = education
        } else {
            current.add(education)
        }
        _uiState.value = _uiState.value.copy(
            onboardingEducation = current,
            showEducationForm = false,
            editingEducation = null
        )
    }
    
    fun deleteOnboardingEducation(educationId: String) {
        _uiState.value = _uiState.value.copy(
            onboardingEducation = _uiState.value.onboardingEducation.filter { it.id != educationId },
            showEducationForm = false,
            editingEducation = null
        )
    }
    
    fun submitEducationStep(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(savingEducation = true, error = null)
            
            // Save each education entry to the actual API
            val educationList = _uiState.value.onboardingEducation
            var allSuccess = true
            
            for (edu in educationList) {
                val input = EducationInput(
                    school = edu.school,
                    degree = edu.degree,
                    fieldOfStudy = edu.fieldOfStudy,
                    startDate = edu.startDate,
                    endDate = edu.endDate.ifBlank { null },
                    isCurrent = edu.isCurrent,
                    grade = edu.grade.ifBlank { null },
                    activities = null,
                    description = null
                )
                ApiClient.createEducation(context, input).onFailure {
                    allSuccess = false
                }
            }
            
            if (allSuccess || educationList.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    savingEducation = false,
                    currentStep = 3 // Go to Interests step
                )
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    savingEducation = false,
                    error = "Failed to save education"
                )
            }
        }
    }
    
    fun skipEducationStep() {
        _uiState.value = _uiState.value.copy(currentStep = 3) // Skip to Interests
    }
    
    fun submitStep1(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            
            val data = mapOf(
                "interests" to _uiState.value.selectedInterests,
                "canTeach" to _uiState.value.canTeach,
                "wantToLearn" to _uiState.value.selectedInterests
            )
            
            ApiClient.updateOnboardingStep(context, 1, data)
                .onSuccess { _ ->
                    // Complete onboarding after interests step
                    ApiClient.completeOnboarding(context)
                        .onSuccess {
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                currentStep = 4 // Go to Matches step
                            )
                            loadMatches()
                            onSuccess()
                        }
                        .onFailure { e ->
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                error = e.message
                            )
                        }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message
                    )
                }
        }
    }
    
    private fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMatches = true)
            ApiClient.getOnboardingMatches(context)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        matches = response.matches.take(5),
                        isLoadingMatches = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingMatches = false)
                }
        }
    }
    
    fun goBack() {
        if (_uiState.value.currentStep > 0) {
            _uiState.value = _uiState.value.copy(
                currentStep = _uiState.value.currentStep - 1
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    class Factory(private val context: android.content.Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileSetupViewModel(context) as T
        }
    }
}

// ==================== Main Wizard Screen ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupWizard(
    onComplete: () -> Unit,
    onSkip: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel: ProfileSetupViewModel = viewModel(
        factory = ProfileSetupViewModel.Factory(context)
    )
    val state by viewModel.uiState
    
    // Glass theme setup
    val backdrop = rememberLayerBackdrop()
    val contentColor = Color.White
    val accentColor = Color(0xFF3B82F6)
    
    // Wrap callbacks to set onboarding preference
    val handleComplete: () -> Unit = {
        coroutineScope.launch {
            com.kyant.backdrop.catalog.data.OnboardingPreferences.setHasSeenOnboarding(context, true)
            onComplete()
        }
    }
    val handleSkip: () -> Unit = {
        coroutineScope.launch {
            com.kyant.backdrop.catalog.data.OnboardingPreferences.setHasSeenOnboarding(context, true)
            onSkip()
        }
    }
    
    val steps = listOf(
        "About You" to "Help us find your people",
        "Your Experience" to "Add your background",
        "Your Education" to "Where you study (or studied)",
        "Your Interests" to "What are you into?",
        "Your Circle" to "People matched for you"
    )
    
    val progress = ((state.currentStep + 1).toFloat() / steps.size) * 100f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400),
        label = "progress"
    )
    
    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = accentColor)
        }
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1117),
                        Color(0xFF161B22),
                        Color(0xFF0D1117)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        // Header with glass effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(0.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.03f))
                    }
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Vormex logo
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(accentColor, Color(0xFF8B5CF6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "V",
                            style = TextStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    BasicText(
                        "Step ${state.currentStep + 1} of ${steps.size}",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    )
                }
                
                if (state.currentStep > 0 && state.currentStep < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.goBack() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = contentColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            BasicText(
                                "Back",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                } else if (state.currentStep == 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { handleSkip() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        BasicText(
                            "Skip",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
        
        // Progress bar with glass effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress / 100f)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(accentColor, Color(0xFF8B5CF6))
                        )
                    )
            )
        }
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Step header
            BasicText(
                steps[state.currentStep].first,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            BasicText(
                steps[state.currentStep].second,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            
            // Error message with glass effect
            state.error?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(12.dp) },
                            effects = {
                                vibrancy()
                                blur(10f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.Red.copy(alpha = 0.15f))
                            }
                        )
                        .padding(12.dp)
                ) {
                    BasicText(
                        error,
                        style = TextStyle(
                            color = Color(0xFFFF6B6B),
                            fontSize = 13.sp
                        )
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            
            // Step content
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "step_content"
            ) { step ->
                when (step) {
                    0 -> StepProfile(
                        state = state,
                        viewModel = viewModel,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onContinue = { viewModel.submitStep0 { } }
                    )
                    1 -> StepExperience(
                        state = state,
                        viewModel = viewModel,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onContinue = { viewModel.submitExperienceStep { } },
                        onSkip = { viewModel.skipExperienceStep() }
                    )
                    2 -> StepEducation(
                        state = state,
                        viewModel = viewModel,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onContinue = { viewModel.submitEducationStep { } },
                        onSkip = { viewModel.skipEducationStep() }
                    )
                    3 -> StepInterests(
                        state = state,
                        viewModel = viewModel,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onContinue = { viewModel.submitStep1 { } }
                    )
                    4 -> StepMatches(
                        state = state,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onFinish = handleComplete
                    )
                }
            }
        }
    }
}

// ==================== Step 0: Profile ====================

@Composable
private fun StepProfile(
    state: ProfileSetupUiState,
    viewModel: ProfileSetupViewModel,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onContinue: () -> Unit
) {
    val canContinue = state.college.trim().length >= 2 && state.primaryGoal.isNotEmpty()
    
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // College input with glass effect
            item {
                Column {
                    BasicText(
                        "Your College / University *",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    // Glass text field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(12.dp) },
                                effects = {
                                    vibrancy()
                                    blur(10f.dp.toPx())
                                    lens(4f.dp.toPx(), 8f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.08f))
                                }
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = contentColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = state.college,
                                onValueChange = { viewModel.updateCollege(it) },
                                textStyle = TextStyle(
                                    color = contentColor,
                                    fontSize = 15.sp
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (state.college.isEmpty()) {
                                            BasicText(
                                                "e.g. NIAT, VIT, IIT Bombay, JNTU...",
                                                style = TextStyle(
                                                    color = contentColor.copy(alpha = 0.3f),
                                                    fontSize = 15.sp
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                    
                    BasicText(
                        "We'll match you with students from your campus first",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                    )
                }
            }
            
            // Primary goal selection
            item {
                Column {
                    BasicText(
                        "What's your main focus right now? *",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(320.dp)
                ) {
                    items(GOALS_WITH_ICONS) { goal ->
                        val isSelected = state.primaryGoal == goal.id
                        Box(
                            modifier = Modifier
                                .height(80.dp)
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedRectangle(12.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(10f.dp.toPx())
                                        lens(4f.dp.toPx(), 8f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        if (isSelected) {
                                            drawRect(accentColor.copy(alpha = 0.3f))
                                        } else {
                                            drawRect(Color.White.copy(alpha = 0.06f))
                                        }
                                    }
                                )
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            1.5.dp,
                                            accentColor.copy(alpha = 0.6f),
                                            RoundedCornerShape(12.dp)
                                        )
                                    } else {
                                        Modifier.border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(12.dp)
                                        )
                                    }
                                )
                                .clickable { viewModel.selectPrimaryGoal(goal.id) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    goal.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) accentColor else contentColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                BasicText(
                                    goal.label,
                                    style = TextStyle(
                                        color = if (isSelected) contentColor else contentColor.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    ),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
            
            // Looking for (optional)
            item {
                Column {
                    BasicText(
                        "I'm looking for (optional)",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LOOKING_FOR.forEach { option ->
                            val isSelected = state.lookingFor.contains(option.id)
                            Box(
                                modifier = Modifier
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedRectangle(20.dp) },
                                        effects = {
                                            vibrancy()
                                            blur(8f.dp.toPx())
                                        },
                                        onDrawSurface = {
                                            if (isSelected) {
                                                drawRect(accentColor.copy(alpha = 0.25f))
                                            } else {
                                                drawRect(Color.White.copy(alpha = 0.06f))
                                            }
                                        }
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                        } else {
                                            Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                        }
                                    )
                                    .clickable { viewModel.toggleLookingFor(option.id) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                BasicText(
                                    option.label,
                                    style = TextStyle(
                                        color = if (isSelected) contentColor else contentColor.copy(alpha = 0.6f),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Continue button with glass effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(52.dp)
                .then(
                    if (canContinue && !state.isSaving) {
                        Modifier
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(accentColor, Color(0xFF8B5CF6))
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onContinue() }
                    } else {
                        Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(12.dp) },
                                effects = {
                                    vibrancy()
                                    blur(8f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.1f))
                                }
                            )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                BasicText(
                    "Continue",
                    style = TextStyle(
                        color = if (canContinue) Color.White else contentColor.copy(alpha = 0.4f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

// ==================== Step 1: Experience ====================

private val EXPERIENCE_TYPES = listOf(
    "Internship" to Icons.Default.WorkOutline,
    "Part-time" to Icons.Default.Schedule,
    "Full-time" to Icons.Default.Work,
    "Freelance" to Icons.Default.Laptop,
    "Contract" to Icons.Default.Description,
    "Volunteer" to Icons.Default.VolunteerActivism
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun StepExperience(
    state: ProfileSetupUiState,
    viewModel: ProfileSetupViewModel,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val maxExperiences = 2
    val canAddMore = state.onboardingExperiences.size < maxExperiences
    
    // Show inline form if adding/editing
    if (state.showExperienceForm && state.editingExperience != null) {
        ExperienceInlineForm(
            experience = state.editingExperience,
            viewModel = viewModel,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            onSave = { viewModel.saveExperience(it) },
            onCancel = { viewModel.dismissExperienceForm() },
            onDelete = if (state.onboardingExperiences.any { it.id == state.editingExperience.id }) {
                { viewModel.deleteExperience(state.editingExperience.id) }
            } else null
        )
    } else {
        // Main experience list
        Column(modifier = Modifier.fillMaxSize()) {
            BasicText(
                "Add up to $maxExperiences experiences. Internships, jobs, volunteering – all count!",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            )
            
            Spacer(Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Existing experiences
                items(state.onboardingExperiences) { exp ->
                    OnboardingExperienceCard(
                        experience = exp,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { viewModel.editExperience(exp) }
                    )
                }
                
                // Add new experience button
                if (canAddMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedRectangle(12.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(10f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.06f))
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = accentColor.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.showAddExperience() }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                BasicText(
                                    "Add Experience",
                                    style = TextStyle(
                                        color = accentColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Empty state
                if (state.onboardingExperiences.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.WorkOutline,
                                    contentDescription = null,
                                    tint = contentColor.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                BasicText(
                                    "No experience yet?",
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.6f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                BasicText(
                                    "That's okay! You can skip this step.",
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.4f),
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Skip button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(12.dp) },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.08f))
                            }
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { onSkip() },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "Skip",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                // Continue button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .then(
                            if (!state.savingExperience) {
                                Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(accentColor, Color(0xFF8B5CF6))
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onContinue() }
                            } else {
                                Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(accentColor.copy(alpha = 0.5f), Color(0xFF8B5CF6).copy(alpha = 0.5f))
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.savingExperience) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        BasicText(
                            if (state.onboardingExperiences.isEmpty()) "Continue" else "Save & Continue",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingExperienceCard(
    experience: OnboardingExperienceEntry,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val typeIcon = EXPERIENCE_TYPES.find { it.first == experience.type }?.second ?: Icons.Default.Work
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(12.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.06f))
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Type icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    typeIcon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    experience.title.ifBlank { "Untitled Role" },
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                BasicText(
                    experience.company.ifBlank { "Organization" },
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        experience.type,
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 11.sp
                        )
                    )
                    if (experience.startDate.isNotBlank()) {
                        BasicText(
                            " · ${experience.startDate}${if (experience.isCurrent) " - Present" else if (experience.endDate.isNotBlank()) " - ${experience.endDate}" else ""}",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
            
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                tint = contentColor.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ExperienceInlineForm(
    experience: OnboardingExperienceEntry,
    viewModel: ProfileSetupViewModel,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onSave: (OnboardingExperienceEntry) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(experience.title) }
    var company by remember { mutableStateOf(experience.company) }
    var type by remember { mutableStateOf(experience.type) }
    var location by remember { mutableStateOf(experience.location) }
    var startDate by remember { mutableStateOf(experience.startDate) }
    var endDate by remember { mutableStateOf(experience.endDate) }
    var isCurrent by remember { mutableStateOf(experience.isCurrent) }
    var description by remember { mutableStateOf(experience.description) }
    var skills by remember { mutableStateOf(experience.skills) }
    var skillInput by remember { mutableStateOf("") }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val canSave = title.isNotBlank() && company.isNotBlank() && startDate.isNotBlank()
    
    // Date pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = java.util.Calendar.getInstance().apply {
                                timeInMillis = millis
                            }
                            startDate = "%d-%02d".format(
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH) + 1
                            )
                        }
                        showStartDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = java.util.Calendar.getInstance().apply {
                                timeInMillis = millis
                            }
                            endDate = "%d-%02d".format(
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH) + 1
                            )
                        }
                        showEndDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Experience?") },
            text = { Text("This will remove this experience entry.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF6B6B))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                if (experience.title.isBlank()) "Add Experience" else "Edit Experience",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDeleteConfirm = true }
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Role/Title field
            item {
                GlassTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Your role or position *",
                    icon = Icons.Default.Badge,
                    backdrop = backdrop,
                    contentColor = contentColor
                )
            }
            
            // Company/Organization field
            item {
                GlassTextField(
                    value = company,
                    onValueChange = { company = it },
                    placeholder = "Organization name *",
                    icon = Icons.Default.Business,
                    backdrop = backdrop,
                    contentColor = contentColor
                )
            }
            
            // Experience type chips
            item {
                Column {
                    BasicText(
                        "Type",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EXPERIENCE_TYPES.forEach { (typeLabel, icon) ->
                            val isSelected = type == typeLabel
                            Box(
                                modifier = Modifier
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedRectangle(20.dp) },
                                        effects = {
                                            vibrancy()
                                            blur(8f.dp.toPx())
                                        },
                                        onDrawSurface = {
                                            if (isSelected) {
                                                drawRect(accentColor.copy(alpha = 0.25f))
                                            } else {
                                                drawRect(Color.White.copy(alpha = 0.06f))
                                            }
                                        }
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                        } else {
                                            Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                        }
                                    )
                                    .clickable { type = typeLabel }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (isSelected) accentColor else contentColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    BasicText(
                                        typeLabel,
                                        style = TextStyle(
                                            color = if (isSelected) contentColor else contentColor.copy(alpha = 0.6f),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Location (optional)
            item {
                GlassTextField(
                    value = location,
                    onValueChange = { location = it },
                    placeholder = "Location (optional)",
                    icon = Icons.Default.LocationOn,
                    backdrop = backdrop,
                    contentColor = contentColor
                )
            }
            
            // Date fields
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start date
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(12.dp) },
                                effects = {
                                    vibrancy()
                                    blur(10f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.08f))
                                }
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .clickable { showStartDatePicker = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = contentColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            BasicText(
                                startDate.ifBlank { "Start date *" },
                                style = TextStyle(
                                    color = if (startDate.isBlank()) contentColor.copy(alpha = 0.3f) else contentColor,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                    
                    // End date
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(12.dp) },
                                effects = {
                                    vibrancy()
                                    blur(10f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = if (isCurrent) 0.04f else 0.08f))
                                }
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .then(
                                if (!isCurrent) Modifier.clickable { showEndDatePicker = true }
                                else Modifier
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                tint = contentColor.copy(alpha = if (isCurrent) 0.3f else 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            BasicText(
                                if (isCurrent) "Present" else endDate.ifBlank { "End date" },
                                style = TextStyle(
                                    color = if (isCurrent) accentColor else if (endDate.isBlank()) contentColor.copy(alpha = 0.3f) else contentColor,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
            
            // Currently working checkbox
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            isCurrent = !isCurrent
                            if (isCurrent) endDate = ""
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isCurrent,
                        onCheckedChange = { 
                            isCurrent = it
                            if (it) endDate = ""
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColor,
                            uncheckedColor = contentColor.copy(alpha = 0.4f)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        "I currently work here",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    )
                }
            }
            
            // Description (optional)
            item {
                Column {
                    BasicText(
                        "Description (optional)",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(12.dp) },
                                effects = {
                                    vibrancy()
                                    blur(10f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.08f))
                                }
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        BasicTextField(
                            value = description,
                            onValueChange = { if (it.length <= 300) description = it },
                            textStyle = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.fillMaxSize(),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (description.isEmpty()) {
                                        BasicText(
                                            "What did you do? What did you learn?",
                                            style = TextStyle(
                                                color = contentColor.copy(alpha = 0.3f),
                                                fontSize = 14.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                    BasicText(
                        "${description.length}/300",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }
            
            // Skills (optional)
            item {
                Column {
                    BasicText(
                        "Skills (optional)",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    // Skill input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedRectangle(10.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(8f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.06f))
                                    }
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = skillInput,
                                onValueChange = { skillInput = it },
                                textStyle = TextStyle(
                                    color = contentColor,
                                    fontSize = 13.sp
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (skillInput.isEmpty()) {
                                            BasicText(
                                                "Add a skill...",
                                                style = TextStyle(
                                                    color = contentColor.copy(alpha = 0.3f),
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .then(
                                    if (skillInput.isNotBlank() && skills.size < 5) {
                                        Modifier
                                            .background(accentColor, RoundedCornerShape(10.dp))
                                            .clickable {
                                                val trimmed = skillInput.trim()
                                                if (trimmed.isNotBlank() && !skills.contains(trimmed)) {
                                                    skills = skills + trimmed
                                                    skillInput = ""
                                                }
                                            }
                                    } else {
                                        Modifier
                                            .drawBackdrop(
                                                backdrop = backdrop,
                                                shape = { RoundedRectangle(10.dp) },
                                                effects = {
                                                    vibrancy()
                                                    blur(8f.dp.toPx())
                                                },
                                                onDrawSurface = {
                                                    drawRect(Color.White.copy(alpha = 0.05f))
                                                }
                                            )
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            BasicText(
                                "Add",
                                style = TextStyle(
                                    color = if (skillInput.isNotBlank() && skills.size < 5) Color.White else contentColor.copy(alpha = 0.3f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                    
                    // Skill chips
                    if (skills.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            skills.forEach { skill ->
                                Box(
                                    modifier = Modifier
                                        .drawBackdrop(
                                            backdrop = backdrop,
                                            shape = { RoundedRectangle(16.dp) },
                                            effects = {
                                                vibrancy()
                                                blur(8f.dp.toPx())
                                            },
                                            onDrawSurface = {
                                                drawRect(accentColor.copy(alpha = 0.2f))
                                            }
                                        )
                                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                        .clickable { skills = skills - skill }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BasicText(
                                            skill,
                                            style = TextStyle(
                                                color = contentColor,
                                                fontSize = 12.sp
                                            )
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = contentColor.copy(alpha = 0.5f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    BasicText(
                        "Up to 5 skills",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            
            // Bottom spacing to prevent content from being hidden under footer
            item {
                Spacer(Modifier.height(80.dp))
            }
        }
        
        // Bottom buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(12.dp) },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.08f))
                        }
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "Cancel",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            // Save button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .then(
                        if (canSave) {
                            Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(accentColor, Color(0xFF8B5CF6))
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSave(
                                        experience.copy(
                                            title = title.trim(),
                                            company = company.trim(),
                                            type = type,
                                            location = location.trim(),
                                            startDate = startDate,
                                            endDate = endDate,
                                            isCurrent = isCurrent,
                                            description = description.trim(),
                                            skills = skills
                                        )
                                    )
                                }
                        } else {
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedRectangle(12.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(8f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.1f))
                                    }
                                )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "Save",
                    style = TextStyle(
                        color = if (canSave) Color.White else contentColor.copy(alpha = 0.4f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    backdrop: LayerBackdrop,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(12.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.08f))
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = contentColor,
                    fontSize = 15.sp
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            BasicText(
                                placeholder,
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.3f),
                                    fontSize = 15.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

// ==================== Step 2: Education ====================

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun StepEducation(
    state: ProfileSetupUiState,
    viewModel: ProfileSetupViewModel,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val maxEducation = 2
    val canAddMore = state.onboardingEducation.size < maxEducation
    
    // Show inline form if adding/editing
    if (state.showEducationForm && state.editingEducation != null) {
        EducationInlineForm(
            education = state.editingEducation,
            viewModel = viewModel,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            onSave = { viewModel.saveEducation(it) },
            onCancel = { viewModel.dismissEducationForm() },
            onDelete = if (state.onboardingEducation.any { it.id == state.editingEducation.id }) {
                { viewModel.deleteOnboardingEducation(state.editingEducation.id) }
            } else null
        )
    } else {
        // Main education list
        Column(modifier = Modifier.fillMaxSize()) {
            BasicText(
                "Add up to $maxEducation education entries. Schools, colleges, courses – all welcome!",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            )
            
            Spacer(Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Existing education entries
                items(state.onboardingEducation) { edu ->
                    OnboardingEducationCard(
                        education = edu,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { viewModel.editEducation(edu) }
                    )
                }
                
                // Add new education button
                if (canAddMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedRectangle(12.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(10f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.06f))
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = accentColor.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.showAddEducation() }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                BasicText(
                                    "Add Education",
                                    style = TextStyle(
                                        color = accentColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Empty state
                if (state.onboardingEducation.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = contentColor.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                BasicText(
                                    "No education yet?",
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.6f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                BasicText(
                                    "That's okay! You can skip this step.",
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.4f),
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Skip button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(12.dp) },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.08f))
                            }
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { onSkip() },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "Skip",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                // Continue button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .then(
                            if (!state.savingEducation) {
                                Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(accentColor, Color(0xFF8B5CF6))
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onContinue() }
                            } else {
                                Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(accentColor.copy(alpha = 0.5f), Color(0xFF8B5CF6).copy(alpha = 0.5f))
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.savingEducation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        BasicText(
                            if (state.onboardingEducation.isEmpty()) "Continue" else "Save & Continue",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingEducationCard(
    education: OnboardingEducationEntry,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(12.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.06f))
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // School icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    education.degree.ifBlank { "Degree / Diploma" },
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                BasicText(
                    education.school.ifBlank { "School / College" },
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
                if (education.fieldOfStudy.isNotBlank()) {
                    BasicText(
                        education.fieldOfStudy,
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (education.startDate.isNotBlank()) {
                        BasicText(
                            "${education.startDate}${if (education.isCurrent) " - Present" else if (education.endDate.isNotBlank()) " - ${education.endDate}" else ""}",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        )
                    }
                    if (education.grade.isNotBlank()) {
                        BasicText(
                            " · Grade: ${education.grade}",
                            style = TextStyle(
                                color = accentColor.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
            
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                tint = contentColor.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EducationInlineForm(
    education: OnboardingEducationEntry,
    viewModel: ProfileSetupViewModel,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onSave: (OnboardingEducationEntry) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var school by remember { mutableStateOf(education.school) }
    var degree by remember { mutableStateOf(education.degree) }
    var fieldOfStudy by remember { mutableStateOf(education.fieldOfStudy) }
    var startDate by remember { mutableStateOf(education.startDate) }
    var endDate by remember { mutableStateOf(education.endDate) }
    var isCurrent by remember { mutableStateOf(education.isCurrent) }
    var grade by remember { mutableStateOf(education.grade) }
    var activities by remember { mutableStateOf(education.activities) }
    var description by remember { mutableStateOf(education.description) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val canSave = school.length >= 2 && degree.length >= 2 && fieldOfStudy.length >= 2 && startDate.isNotBlank()
    
    // Date pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = java.util.Calendar.getInstance().apply {
                                timeInMillis = millis
                            }
                            startDate = "%d-%02d".format(
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH) + 1
                            )
                        }
                        showStartDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = java.util.Calendar.getInstance().apply {
                                timeInMillis = millis
                            }
                            endDate = "%d-%02d".format(
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH) + 1
                            )
                        }
                        showEndDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Education?") },
            text = { Text("This will remove this education entry.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF6B6B))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                if (education.school.isBlank()) "Add Education" else "Edit Education",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDeleteConfirm = true }
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // School field
            item {
                GlassTextField(
                    value = school,
                    onValueChange = { if (it.length <= 100) school = it },
                    placeholder = "School / College / University *",
                    icon = Icons.Default.School,
                    backdrop = backdrop,
                    contentColor = contentColor
                )
            }
            
            // Degree field
            item {
                GlassTextField(
                    value = degree,
                    onValueChange = { if (it.length <= 100) degree = it },
                    placeholder = "Degree / Diploma / Certificate *",
                    icon = Icons.Default.WorkspacePremium,
                    backdrop = backdrop,
                    contentColor = contentColor
                )
            }
            
            // Field of Study
            item {
                GlassTextField(
                    value = fieldOfStudy,
                    onValueChange = { if (it.length <= 100) fieldOfStudy = it },
                    placeholder = "Field of Study / Major / Course *",
                    icon = Icons.Default.MenuBook,
                    backdrop = backdrop,
                    contentColor = contentColor
                )
            }
            
            // Date fields
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start date
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(12.dp) },
                                effects = {
                                    vibrancy()
                                    blur(10f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.08f))
                                }
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .clickable { showStartDatePicker = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = contentColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            BasicText(
                                startDate.ifBlank { "Start date *" },
                                style = TextStyle(
                                    color = if (startDate.isBlank()) contentColor.copy(alpha = 0.3f) else contentColor,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                    
                    // End date
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(12.dp) },
                                effects = {
                                    vibrancy()
                                    blur(10f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = if (isCurrent) 0.04f else 0.08f))
                                }
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .then(
                                if (!isCurrent) Modifier.clickable { showEndDatePicker = true }
                                else Modifier
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                tint = contentColor.copy(alpha = if (isCurrent) 0.3f else 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            BasicText(
                                if (isCurrent) "Present" else endDate.ifBlank { "End date" },
                                style = TextStyle(
                                    color = if (isCurrent) accentColor else if (endDate.isBlank()) contentColor.copy(alpha = 0.3f) else contentColor,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
            
            // Currently studying checkbox
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            isCurrent = !isCurrent
                            if (isCurrent) endDate = ""
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isCurrent,
                        onCheckedChange = { 
                            isCurrent = it
                            if (it) endDate = ""
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColor,
                            uncheckedColor = contentColor.copy(alpha = 0.4f)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        "I am currently studying here",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    )
                }
            }
            
            // Grade (optional)
            item {
                GlassTextField(
                    value = grade,
                    onValueChange = { if (it.length <= 50) grade = it },
                    placeholder = "Grade / GPA (optional)",
                    icon = Icons.Default.Grade,
                    backdrop = backdrop,
                    contentColor = contentColor
                )
            }
            
            // Activities (optional)
            item {
                Column {
                    BasicText(
                        "Activities & Societies (optional)",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(12.dp) },
                                effects = {
                                    vibrancy()
                                    blur(10f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.08f))
                                }
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        BasicTextField(
                            value = activities,
                            onValueChange = { if (it.length <= 500) activities = it },
                            textStyle = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.fillMaxSize(),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (activities.isEmpty()) {
                                        BasicText(
                                            "Clubs, teams, societies...",
                                            style = TextStyle(
                                                color = contentColor.copy(alpha = 0.3f),
                                                fontSize = 14.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                    BasicText(
                        "${activities.length}/500",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }
            
            // Description (optional)
            item {
                Column {
                    BasicText(
                        "Description (optional)",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(12.dp) },
                                effects = {
                                    vibrancy()
                                    blur(10f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.08f))
                                }
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        BasicTextField(
                            value = description,
                            onValueChange = { if (it.length <= 500) description = it },
                            textStyle = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.fillMaxSize(),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (description.isEmpty()) {
                                        BasicText(
                                            "Notable achievements, projects...",
                                            style = TextStyle(
                                                color = contentColor.copy(alpha = 0.3f),
                                                fontSize = 14.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                    BasicText(
                        "${description.length}/500",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }
            
            // Bottom spacing
            item {
                Spacer(Modifier.height(80.dp))
            }
        }
        
        // Bottom buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(12.dp) },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.08f))
                        }
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "Cancel",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            // Save button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .then(
                        if (canSave) {
                            Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(accentColor, Color(0xFF8B5CF6))
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSave(
                                        education.copy(
                                            school = school.trim(),
                                            degree = degree.trim(),
                                            fieldOfStudy = fieldOfStudy.trim(),
                                            startDate = startDate,
                                            endDate = endDate,
                                            isCurrent = isCurrent,
                                            grade = grade.trim(),
                                            activities = activities.trim(),
                                            description = description.trim()
                                        )
                                    )
                                }
                        } else {
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedRectangle(12.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(8f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.1f))
                                    }
                                )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "Save",
                    style = TextStyle(
                        color = if (canSave) Color.White else contentColor.copy(alpha = 0.4f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

// ==================== Step 3: Interests ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepInterests(
    state: ProfileSetupUiState,
    viewModel: ProfileSetupViewModel,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onContinue: () -> Unit
) {
    val canContinue = state.selectedInterests.size >= 2
    
    val allItems = INTEREST_GROUPS.flatMap { it.items }
    val filteredItems = if (state.interestSearch.isNotBlank()) {
        allItems.filter { it.contains(state.interestSearch, ignoreCase = true) }
    } else null
    
    Column(modifier = Modifier.fillMaxSize()) {
        BasicText(
            "Pick at least 2 interests. Tap ★ to mark what you can teach.",
            style = TextStyle(
                color = contentColor.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        )
        
        Spacer(Modifier.height(10.dp))
        
        // Compact search and add row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search field - compact
            Box(
                modifier = Modifier
                    .weight(1f)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(10.dp) },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.06f))
                        }
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = state.interestSearch,
                        onValueChange = { viewModel.updateInterestSearch(it) },
                        textStyle = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box {
                                if (state.interestSearch.isEmpty()) {
                                    BasicText(
                                        "Search...",
                                        style = TextStyle(
                                            color = contentColor.copy(alpha = 0.3f),
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Add custom interest - compact single row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(10.dp) },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.06f))
                        }
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = state.customInterest,
                        onValueChange = { viewModel.updateCustomInterest(it) },
                        textStyle = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box {
                                if (state.customInterest.isEmpty()) {
                                    BasicText(
                                        "Add custom...",
                                        style = TextStyle(
                                            color = contentColor.copy(alpha = 0.3f),
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
            
            // Add button - compact
            Box(
                modifier = Modifier
                    .then(
                        if (state.customInterest.isNotBlank()) {
                            Modifier
                                .background(accentColor, RoundedCornerShape(10.dp))
                                .clickable { viewModel.addCustomInterest() }
                        } else {
                            Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedRectangle(10.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(8f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.05f))
                                    }
                                )
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                BasicText(
                    "Add",
                    style = TextStyle(
                        color = if (state.customInterest.isNotBlank()) Color.White else contentColor.copy(alpha = 0.3f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
        
        Spacer(Modifier.height(10.dp))
        
        // Selected interests - compact chips
        if (state.selectedInterests.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                state.selectedInterests.forEach { interest ->
                    val isTeach = state.canTeach.contains(interest)
                    Box(
                        modifier = Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(16.dp) },
                                effects = {
                                    vibrancy()
                                    blur(8f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(accentColor.copy(alpha = 0.2f))
                                }
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { viewModel.toggleInterest(interest) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isTeach) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            BasicText(
                                interest,
                                style = TextStyle(
                                    color = contentColor,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = contentColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Interest groups
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredItems != null) {
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredItems.forEach { item ->
                            GlassInterestChip(
                                label = item,
                                isSelected = state.selectedInterests.contains(item),
                                canTeach = state.canTeach.contains(item),
                                backdrop = backdrop,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                onToggle = { viewModel.toggleInterest(item) },
                                onToggleTeach = { viewModel.toggleCanTeach(item) }
                            )
                        }
                    }
                }
            } else {
                items(INTEREST_GROUPS) { group ->
                    Column {
                        BasicText(
                            group.label,
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            group.items.forEach { item ->
                                GlassInterestChip(
                                    label = item,
                                    isSelected = state.selectedInterests.contains(item),
                                    canTeach = state.canTeach.contains(item),
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    onToggle = { viewModel.toggleInterest(item) },
                                    onToggleTeach = { viewModel.toggleCanTeach(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Continue button with glass effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(48.dp)
                .then(
                    if (canContinue && !state.isSaving) {
                        Modifier
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(accentColor, Color(0xFF8B5CF6))
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onContinue() }
                    } else {
                        Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(12.dp) },
                                effects = {
                                    vibrancy()
                                    blur(8f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.1f))
                                }
                            )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                BasicText(
                    "Continue",
                    style = TextStyle(
                        color = if (canContinue) Color.White else contentColor.copy(alpha = 0.4f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlassInterestChip(
    label: String,
    isSelected: Boolean,
    canTeach: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onToggle: () -> Unit,
    onToggleTeach: () -> Unit
) {
    Box {
        Box(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(16.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                    },
                    onDrawSurface = {
                        if (isSelected) {
                            drawRect(accentColor.copy(alpha = 0.15f))
                        } else {
                            drawRect(Color.White.copy(alpha = 0.05f))
                        }
                    }
                )
                .then(
                    if (isSelected) {
                        Modifier.border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    } else {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                    }
                )
                .clickable { onToggle() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            BasicText(
                label,
                style = TextStyle(
                    color = if (isSelected) contentColor else contentColor.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (canTeach) Color(0xFFFFD700)
                        else Color.White.copy(alpha = 0.2f)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable { onToggleTeach() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = if (canTeach) Color.Black else contentColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterestChip(
    label: String,
    isSelected: Boolean,
    canTeach: Boolean,
    onToggle: () -> Unit,
    onToggleTeach: () -> Unit
) {
    Box {
        FilterChip(
            selected = isSelected,
            onClick = onToggle,
            label = { Text(label, style = MaterialTheme.typography.labelMedium) }
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (canTeach) Color(0xFFFFD700)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onToggleTeach() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "★",
                    fontSize = 10.sp,
                    color = if (canTeach) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== Step 4: Matches ====================

@Composable
private fun StepMatches(
    state: ProfileSetupUiState,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onFinish: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (state.isLoadingMatches) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = accentColor)
                    Spacer(Modifier.height(16.dp))
                    BasicText(
                        "Finding people like you...",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    )
                }
            }
        } else if (state.matches.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    BasicText(
                        "You're one of the first from your campus!",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "As more students join, we'll match you with the right people. Meanwhile, explore and start connecting.",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        } else {
            BasicText(
                "Found ${state.matches.size} matches based on your interests and campus.",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            )
            Spacer(Modifier.height(12.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.matches) { match ->
                    GlassMatchCard(
                        match = match,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                }
            }
        }
        
        // Finish button with glass effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(52.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(accentColor, Color(0xFF8B5CF6))
                    ),
                    RoundedCornerShape(12.dp)
                )
                .clickable { onFinish() },
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                if (state.matches.isEmpty()) "Explore Vormex" else "Start Connecting",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun GlassMatchCard(
    match: OnboardingMatch,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(12.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.06f))
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with match percentage
            Box {
                if (match.user.profileImage != null) {
                    AsyncImage(
                        model = match.user.profileImage,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            match.user.name.firstOrNull()?.toString() ?: "?",
                            style = TextStyle(
                                color = accentColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                
                // Match percentage badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (match.matchPercentage >= 60) Color(0xFF22C55E)
                            else accentColor
                        )
                        .border(2.dp, Color(0xFF161B22), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "${match.matchPercentage}%",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    match.user.name,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                BasicText(
                    match.user.college ?: match.user.headline ?: "",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
                if (match.reasons.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        match.reasons.take(2).forEach { reason ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                BasicText(
                                    reason,
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.5f),
                                        fontSize = 10.sp
                                    )
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
private fun MatchCard(match: OnboardingMatch) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with match percentage
            Box {
                if (match.user.profileImage != null) {
                    AsyncImage(
                        model = match.user.profileImage,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            match.user.name.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Match percentage badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (match.matchPercentage >= 60) Color(0xFF22C55E)
                            else Color(0xFF3B82F6)
                        )
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${match.matchPercentage}%",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    match.user.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    match.user.college ?: match.user.headline ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (match.reasons.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        match.reasons.take(2).forEach { reason ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    reason,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== FlowRow for older Compose versions ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
