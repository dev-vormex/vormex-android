package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.network.AgentApiService
import com.kyant.backdrop.catalog.network.AgentSocketManager
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.AgentAction
import com.kyant.backdrop.catalog.network.models.AgentGoal
import com.kyant.backdrop.catalog.network.models.AgentPendingAction
import com.kyant.backdrop.catalog.network.models.AgentSessionState
import com.kyant.backdrop.catalog.network.models.AgentTurnResponse
import com.kyant.backdrop.catalog.network.models.AgentUiIntent
import com.kyant.backdrop.catalog.network.models.AgentVoiceTurnResponse
import com.kyant.backdrop.catalog.network.models.AssistantChatHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File
import java.util.Locale

data class AgentMessage(
    val role: String,
    val content: String
)

data class AgentUiState(
    val sessionState: AgentSessionState? = null,
    val messages: List<AgentMessage> = emptyList(),
    val isLoadingSession: Boolean = false,
    val isSending: Boolean = false,
    val isVoiceSessionConnecting: Boolean = false,
    val isRecordingVoice: Boolean = false,
    val isPlayingAudio: Boolean = false,
    val isSpeakingAssistant: Boolean = false,
    val isVoiceListening: Boolean = false,
    val isVoiceThinking: Boolean = false,
    val liveUserTranscript: String = "",
    val liveAssistantTranscript: String = "",
    val isRefreshingMeta: Boolean = false,
    val isLoadingEntitlements: Boolean = false,
    val isSavingGoal: Boolean = false,
    val isResolvingApproval: Boolean = false,
    val autoRunEnabled: Boolean = false,
    val autonomyMode: String = "approval",
    val aiTier: String = "free",
    val canUseAgent: Boolean = false,
    val powerModeEligible: Boolean = false,
    val isPremium: Boolean = false,
    val assistantDailyLimit: Int? = null,
    val assistantDailyRemaining: Int? = null,
    val powerCreditsBalance: Int = 0,
    val socketConnected: Boolean = false,
    val error: String? = null,
    val liveStatus: String? = null,
    val pendingUiIntents: List<AgentUiIntent> = emptyList(),
    val activeInlineResults: AgentInlineResultsPanel? = null,
    val dismissedInlineResultIds: Set<String> = emptySet(),
    val inlineResultActionInProgress: Set<String> = emptySet(),
    val lastExecutedActions: List<AgentAction> = emptyList(),
    val lastSuggestedActions: List<AgentAction> = emptyList(),
    val pendingApprovals: List<AgentPendingAction> = emptyList(),
    val goals: List<AgentGoal> = emptyList()
)

class AgentViewModel(
    private val context: Context
) : ViewModel() {
    private val applicationContext = context.applicationContext
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var voiceRecordingFile: File? = null
    private var mediaPlayer: MediaPlayer? = null
    private var responsePulsePlayer: MediaPlayer? = null
    private var assistantTts: TextToSpeech? = null
    private var assistantTtsReady = false
    private var currentAssistantSpeechId: String? = null
    private val realtimeVoiceManager = AgentRealtimeVoiceManager()
    private var shouldStartRealtimeCapture = false
    private var shouldStartRealtimeCaptureAfterPrompt = false
    private var lastSyncedSurfaceKey: String? = null
    private var pendingNavigationTarget: String? = null
    private var pendingRealtimePrompt: String? = null
    private var entitlementsLoaded = false
    private val inlineResultsJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    init {
        initializeAssistantTts()
        observeSocket()
        viewModelScope.launch {
            val storedAutoRun = AgentApiService.getStoredAutoRunEnabled(applicationContext)
            val storedAutonomyMode = if (storedAutoRun) "power" else "approval"
            _uiState.update {
                it.copy(
                    autoRunEnabled = storedAutoRun,
                    autonomyMode = storedAutonomyMode
                )
            }
            loadAiEntitlements(silent = true)
            connectSocketIfPossible(null)
        }
    }

    private fun initializeAssistantTts() {
        assistantTts = TextToSpeech(applicationContext) { status ->
            val engine = assistantTts
            if (status == TextToSpeech.SUCCESS && engine != null) {
                configureAssistantTts(engine)
                assistantTtsReady = true
            } else {
                assistantTtsReady = false
            }
        }
    }

    private fun configureAssistantTts(engine: TextToSpeech) {
        runCatching {
            engine.language = Locale.US
            selectFeminineTtsVoice(engine)?.let { voice ->
                engine.voice = voice
            }
            engine.setPitch(1.08f)
            engine.setSpeechRate(0.96f)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                private fun markSpeechError(utteranceId: String?) {
                    if (utteranceId == currentAssistantSpeechId) {
                        _uiState.update {
                            it.copy(
                                isPlayingAudio = false,
                                isSpeakingAssistant = false,
                                liveStatus = "Voice playback failed."
                            )
                        }
                    }
                }

                override fun onStart(utteranceId: String?) {
                    if (utteranceId == currentAssistantSpeechId) {
                        _uiState.update {
                            it.copy(
                                isPlayingAudio = true,
                                isSpeakingAssistant = true,
                                liveStatus = "Speaking reply..."
                            )
                        }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId == currentAssistantSpeechId) {
                        _uiState.update {
                            it.copy(
                                isPlayingAudio = false,
                                isSpeakingAssistant = false
                            )
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    markSpeechError(utteranceId)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    markSpeechError(utteranceId)
                }
            })
        }
    }

    private fun selectFeminineTtsVoice(engine: TextToSpeech): Voice? {
        val voices = engine.voices ?: return null
        val deviceLocale = Locale.getDefault()

        return voices
            .filter { voice ->
                val language = voice.locale?.language
                language == deviceLocale.language || language == Locale.US.language
            }
            .maxByOrNull { voice ->
                val name = voice.name.lowercase(Locale.US)
                var score = 0
                if (voice.locale?.language == deviceLocale.language) score += 24
                if (voice.locale?.country == deviceLocale.country) score += 8
                if (voice.locale?.language == Locale.US.language) score += 12
                if ("female" in name || "#female" in name) score += 120
                if ("woman" in name || "feminine" in name) score += 100
                if ("shimmer" in name) score += 80
                if ("sfg" in name || "female_1" in name || "female-1" in name) score += 60
                if (!voice.isNetworkConnectionRequired) score += 8
                score + voice.quality
            }
    }

    private fun shouldSpeakAssistantReplies(): Boolean {
        val state = _uiState.value
        return state.canUseAgent || state.isPremium || state.aiTier == "premium" || state.aiTier == "creator_pro"
    }

    private fun speakAssistantReplyIfPremium(text: String) {
        val spokenText = text.trim().take(3_800)
        if (spokenText.isBlank() || !shouldSpeakAssistantReplies()) return

        val engine = assistantTts
        if (!assistantTtsReady || engine == null) {
            _uiState.update { it.copy(liveStatus = "Voice is getting ready.") }
            return
        }

        stopAssistantSpeech()
        val utteranceId = "vormex_ai_reply_${System.currentTimeMillis()}"
        currentAssistantSpeechId = utteranceId
        val result = engine.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            _uiState.update {
                it.copy(
                    isPlayingAudio = false,
                    isSpeakingAssistant = false,
                    liveStatus = "Voice playback failed."
                )
            }
        }
    }

    fun stopAssistantSpeech() {
        currentAssistantSpeechId = null
        runCatching { assistantTts?.stop() }
        _uiState.update {
            it.copy(
                isPlayingAudio = false,
                isSpeakingAssistant = false
            )
        }
    }

    fun refreshAiEntitlements(silent: Boolean = false) {
        viewModelScope.launch {
            loadAiEntitlements(silent = silent)
        }
    }

    private suspend fun loadAiEntitlements(silent: Boolean = false): Boolean {
        if (_uiState.value.isLoadingEntitlements) {
            return entitlementsLoaded
        }

        if (!silent) {
            _uiState.update { it.copy(isLoadingEntitlements = true, error = null) }
        } else {
            _uiState.update { it.copy(isLoadingEntitlements = true) }
        }

        return AgentApiService.getAiEntitlements(applicationContext).fold(
            onSuccess = { entitlements ->
                entitlementsLoaded = true
                val canUseAgent = entitlements.canUseAgent
                val shouldDowngradeAutonomy = !canUseAgent && _uiState.value.autonomyMode == "power"
                if (shouldDowngradeAutonomy) {
                    AgentApiService.setStoredAutonomyMode(applicationContext, "approval")
                }
                _uiState.update { state ->
                    val nextAutonomyMode = if (canUseAgent) state.autonomyMode else "approval"
                    val nextAutoRun = canUseAgent && nextAutonomyMode == "power"
                    state.copy(
                        isLoadingEntitlements = false,
                        aiTier = entitlements.tier.ifBlank { "free" },
                        canUseAgent = canUseAgent,
                        powerModeEligible = canUseAgent,
                        isPremium = entitlements.isPremium || entitlements.isCreatorPro || entitlements.isAdmin,
                        assistantDailyLimit = state.assistantDailyLimit,
                        assistantDailyRemaining = state.assistantDailyRemaining,
                        powerCreditsBalance = entitlements.balance,
                        autoRunEnabled = nextAutoRun,
                        autonomyMode = nextAutonomyMode,
                        sessionState = if (canUseAgent) {
                            state.sessionState?.copy(
                                powerModeEligible = true,
                                isPremium = true,
                                allowAutonomousActions = nextAutoRun,
                                requestedAutonomyMode = nextAutonomyMode,
                                effectiveAutonomyMode = nextAutonomyMode
                            )
                        } else {
                            null
                        },
                        error = if (silent) state.error else null
                    )
                }
                if (!canUseAgent) {
                    AgentApiService.clearSession(applicationContext)
                }
                true
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isLoadingEntitlements = false,
                        error = if (silent) it.error else error.message ?: "Could not load AI access."
                    )
                }
                false
            }
        )
    }

    fun ensureSession(surface: String) {
        viewModelScope.launch {
            val normalizedSurface = normalizeAgentSurface(surface)
            if (!entitlementsLoaded) {
                loadAiEntitlements(silent = true)
            }
            val autonomyMode = AgentApiService.getStoredAutonomyMode(applicationContext)
            val canUseAgent = _uiState.value.canUseAgent
            val safeAutonomyMode = if (canUseAgent) autonomyMode else "approval"
            val autoRunEnabled = canUseAgent && safeAutonomyMode == "power"
            _uiState.update { state ->
                state.copy(
                    autoRunEnabled = autoRunEnabled,
                    autonomyMode = safeAutonomyMode,
                    sessionState = state.sessionState?.copy(
                        allowAutonomousActions = autoRunEnabled,
                        requestedAutonomyMode = safeAutonomyMode,
                        effectiveAutonomyMode = if (autoRunEnabled) "power" else "approval",
                        currentSurface = normalizedSurface
                    )
                )
            }

            if (!canUseAgent) {
                if (autonomyMode == "power") {
                    AgentApiService.setStoredAutonomyMode(applicationContext, "approval")
                }
                _uiState.update {
                    it.copy(
                        isLoadingSession = false,
                        sessionState = null,
                        liveStatus = "Free assistant ready"
                    )
                }
                return@launch
            }

            val existingSession = _uiState.value.sessionState
            if (existingSession != null) {
                connectSocketIfPossible(existingSession.sessionId)
                refreshPendingActions(silent = true)
                refreshGoals(silent = true)
                return@launch
            }

            if (_uiState.value.isLoadingSession) {
                return@launch
            }

            _uiState.update { it.copy(isLoadingSession = true, error = null) }
            AgentApiService.bootstrapSession(
                context = applicationContext,
                mode = "text",
                surface = surface,
                allowAutonomousActions = autoRunEnabled,
                autonomyMode = safeAutonomyMode
            ).onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isLoadingSession = false,
                        sessionState = response.sessionState,
                        autoRunEnabled = response.sessionState.effectiveAutonomyMode == "power",
                        autonomyMode = response.sessionState.effectiveAutonomyMode,
                        canUseAgent = response.sessionState.powerModeEligible,
                        powerModeEligible = response.sessionState.powerModeEligible,
                        isPremium = response.sessionState.isPremium
                    )
                }
                connectSocketIfPossible(response.sessionState.sessionId)
                refreshPendingActions(silent = true)
                refreshGoals(silent = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingSession = false,
                        error = error.message ?: "Could not initialize the agent."
                    )
                }
            }
        }
    }

    fun syncSurface(
        surface: String,
        surfaceContext: Map<String, String> = emptyMap()
    ) {
        val normalizedSurface = normalizeAgentSurface(surface)
        val normalizedContext = surfaceContext.toSortedMap()
        val sessionId = _uiState.value.sessionState?.sessionId
        val currentAutonomyMode = _uiState.value.autonomyMode
        val currentAutoRun = currentAutonomyMode == "power"

        _uiState.update { state ->
            val updatedStatus =
                if (pendingNavigationTarget == normalizedSurface) {
                    pendingNavigationTarget = null
                    "Now on ${formatSurfaceLabel(normalizedSurface)}"
                } else {
                    state.liveStatus
                }

            state.copy(
                sessionState = state.sessionState?.copy(currentSurface = normalizedSurface),
                liveStatus = updatedStatus
            )
        }

        if (sessionId.isNullOrBlank()) {
            return
        }

        val syncKey = buildSurfaceSyncKey(sessionId, normalizedSurface, normalizedContext)
        if (syncKey == lastSyncedSurfaceKey) {
            return
        }
        lastSyncedSurfaceKey = syncKey

        viewModelScope.launch {
            AgentSocketManager.updateSurface(
                sessionId = sessionId,
                surface = normalizedSurface,
                surfaceContext = normalizedContext,
                allowAutonomousActions = currentAutoRun,
                autonomyMode = currentAutonomyMode
            )
        }
    }

    fun previewUiIntent(intent: AgentUiIntent) {
        pendingNavigationTarget = targetSurfaceForIntent(intent) ?: pendingNavigationTarget
        _uiState.update {
            it.copy(liveStatus = describeUiIntent(intent))
        }
    }

    fun startRealtimeVoice(
        surface: String,
        surfaceContext: Map<String, String> = emptyMap(),
        openingGreeting: String? = null
    ) {
        if (_uiState.value.isRecordingVoice || _uiState.value.isVoiceSessionConnecting) return

        viewModelScope.launch {
            if (!entitlementsLoaded) {
                loadAiEntitlements(silent = true)
            }
            if (!_uiState.value.canUseAgent) {
                showPremiumWall("Live voice and app actions")
                return@launch
            }

            val autoRunEnabled = _uiState.value.autoRunEnabled
            val autonomyMode = _uiState.value.autonomyMode
            pendingRealtimePrompt = openingGreeting?.trim()?.takeIf { it.isNotBlank() }
            shouldStartRealtimeCaptureAfterPrompt = pendingRealtimePrompt != null
            shouldStartRealtimeCapture = !shouldStartRealtimeCaptureAfterPrompt
            realtimeVoiceManager.preparePlayback()
            realtimeVoiceManager.setAssistantPlaybackSuppressed(false, flush = true)
            _uiState.update {
                it.copy(
                    isVoiceSessionConnecting = true,
                    isVoiceListening = false,
                    isVoiceThinking = false,
                    isPlayingAudio = false,
                    liveUserTranscript = "",
                    liveAssistantTranscript = "",
                    error = null
                )
            }

            val sessionState = ensureSessionState(
                surface = surface,
                mode = "voice",
                allowAutonomousActions = autoRunEnabled,
                autonomyMode = autonomyMode
            ).getOrElse { error ->
                shouldStartRealtimeCapture = false
                shouldStartRealtimeCaptureAfterPrompt = false
                _uiState.update {
                    it.copy(
                        isVoiceSessionConnecting = false,
                        error = error.message ?: "Could not start live voice."
                    )
                }
                return@launch
            }

            connectSocketIfPossible(sessionState.sessionId)
            AgentSocketManager.startRealtimeVoice(
                sessionId = sessionState.sessionId,
                surface = surface,
                surfaceContext = surfaceContext,
                allowAutonomousActions = autoRunEnabled,
                autonomyMode = autonomyMode
            )
        }
    }

    fun requestRealtimeVoiceGreeting() {
        queueRealtimeVoicePrompt(
            "Briefly say: I am active now. Let me know how I can help you. Then stop speaking and keep listening for the user."
        )
    }

    fun stopRealtimeVoice() {
        shouldStartRealtimeCapture = false
        shouldStartRealtimeCaptureAfterPrompt = false
        pendingRealtimePrompt = null
        realtimeVoiceManager.stopCapture()
        realtimeVoiceManager.setAssistantPlaybackSuppressed(false, flush = true)
        realtimeVoiceManager.stopAssistantPlayback()
        AgentSocketManager.stopRealtimeVoice()
        clearRealtimeVoiceState()
    }

    private suspend fun ensureSessionState(
        surface: String,
        mode: String,
        allowAutonomousActions: Boolean,
        autonomyMode: String
    ): Result<AgentSessionState> {
        if (!entitlementsLoaded) {
            loadAiEntitlements(silent = true)
        }
        if (!_uiState.value.canUseAgent) {
            return Result.failure(Exception("Power Mode is available for Premium users. Free assistant can still help with planning and drafts."))
        }

        val existingSession = _uiState.value.sessionState
        if (existingSession != null) {
            val updatedSession = existingSession.copy(
                allowAutonomousActions = allowAutonomousActions,
                requestedAutonomyMode = autonomyMode,
                effectiveAutonomyMode = autonomyMode,
                mode = mode.ifBlank { existingSession.mode }
            )
            _uiState.update { it.copy(sessionState = updatedSession) }
            return Result.success(updatedSession)
        }

        _uiState.update { it.copy(isLoadingSession = true, error = null) }
        return AgentApiService.bootstrapSession(
            context = applicationContext,
            mode = mode,
            surface = surface,
            allowAutonomousActions = allowAutonomousActions,
            autonomyMode = autonomyMode
        ).fold(
            onSuccess = { response ->
                _uiState.update {
                    it.copy(
                        isLoadingSession = false,
                        sessionState = response.sessionState,
                        autoRunEnabled = response.sessionState.effectiveAutonomyMode == "power",
                        autonomyMode = response.sessionState.effectiveAutonomyMode,
                        canUseAgent = response.sessionState.powerModeEligible,
                        powerModeEligible = response.sessionState.powerModeEligible,
                        isPremium = response.sessionState.isPremium
                    )
                }
                Result.success(response.sessionState)
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isLoadingSession = false,
                        error = error.message ?: "Could not initialize the agent."
                    )
                }
                Result.failure(error)
            }
        )
    }

    private fun beginRealtimeCapture() {
        runCatching {
            realtimeVoiceManager.startCapture { audioBase64 ->
                AgentSocketManager.sendRealtimeAudioChunk(audioBase64)
            }
            _uiState.update {
                it.copy(
                    isVoiceSessionConnecting = false,
                    isRecordingVoice = true,
                    isVoiceListening = true,
                    isVoiceThinking = false,
                    isPlayingAudio = false,
                    error = null
                )
            }
        }.onFailure { error ->
            shouldStartRealtimeCapture = false
            AgentSocketManager.stopRealtimeVoice()
            _uiState.update {
                it.copy(
                    isVoiceSessionConnecting = false,
                    isRecordingVoice = false,
                    isVoiceListening = false,
                    isVoiceThinking = false,
                    error = error.message ?: "Could not start live voice."
                )
            }
        }
    }

    private fun clearRealtimeVoiceState() {
        pendingRealtimePrompt = null
        shouldStartRealtimeCaptureAfterPrompt = false
        realtimeVoiceManager.setAssistantPlaybackSuppressed(false, flush = true)
        stopResponsePulseSound()
        _uiState.update {
            it.copy(
                isVoiceSessionConnecting = false,
                isRecordingVoice = false,
                isPlayingAudio = false,
                isVoiceListening = false,
                isVoiceThinking = false,
                liveUserTranscript = "",
                liveAssistantTranscript = ""
            )
        }
    }

    fun setAutoRunEnabled(enabled: Boolean) {
        setAutonomyMode(if (enabled) "power" else "approval")
    }

    fun setPowerModeEnabled(enabled: Boolean, powerModeEligible: Boolean = _uiState.value.canUseAgent) {
        val canEnablePower = powerModeEligible && _uiState.value.canUseAgent
        if (enabled && !canEnablePower) {
            _uiState.update {
                it.copy(
                    autoRunEnabled = false,
                    autonomyMode = "approval",
                    error = "Power mode is available for Premium users. Approval mode is active."
                )
            }
            viewModelScope.launch {
                AgentApiService.setStoredAutonomyMode(applicationContext, "approval")
            }
            return
        }
        setAutonomyMode(if (enabled) "power" else "approval")
    }

    fun setAutonomyMode(mode: String) {
        val normalizedMode = if (mode.equals("power", ignoreCase = true)) "power" else "approval"
        if (normalizedMode == "power" && !_uiState.value.canUseAgent) {
            setPowerModeEnabled(enabled = true, powerModeEligible = false)
            return
        }
        viewModelScope.launch {
            AgentApiService.setStoredAutonomyMode(applicationContext, normalizedMode)
            val enabled = normalizedMode == "power"
            _uiState.update { state ->
                state.copy(
                    autoRunEnabled = enabled,
                    autonomyMode = normalizedMode,
                    sessionState = state.sessionState?.copy(
                        allowAutonomousActions = enabled,
                        requestedAutonomyMode = normalizedMode,
                        effectiveAutonomyMode = normalizedMode
                    )
                )
            }
        }
    }

    fun showPremiumWall(featureLabel: String = "Power Mode") {
        val feature = featureLabel.trim().ifBlank { "Power Mode" }
        _uiState.update { state ->
            state.copy(
                liveStatus = "Premium unlock",
                error = null,
                messages = state.messages + AgentMessage(
                    role = "assistant",
                    content = "$feature is part of Vormex AI Power Mode for Premium and Creator Pro. Free assistant can still help you plan, draft, review, and decide the next step safely."
                )
            )
        }
    }

    fun refreshPendingActions(silent: Boolean = false) {
        viewModelScope.launch {
            if (!_uiState.value.canUseAgent) {
                _uiState.update { it.copy(isRefreshingMeta = false, pendingApprovals = emptyList()) }
                return@launch
            }
            if (!silent) {
                _uiState.update { it.copy(isRefreshingMeta = true, error = null) }
            }
            AgentApiService.getPendingActions(applicationContext)
                .onSuccess { actions ->
                    _uiState.update {
                        it.copy(
                            isRefreshingMeta = false,
                            pendingApprovals = actions
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshingMeta = false,
                            error = if (silent) it.error else error.message ?: "Could not refresh approvals."
                        )
                    }
                }
        }
    }

    fun refreshGoals(silent: Boolean = false) {
        viewModelScope.launch {
            if (!_uiState.value.canUseAgent) {
                _uiState.update { it.copy(isRefreshingMeta = false, goals = emptyList()) }
                return@launch
            }
            if (!silent) {
                _uiState.update { it.copy(isRefreshingMeta = true, error = null) }
            }
            AgentApiService.getGoals(applicationContext)
                .onSuccess { goals ->
                    _uiState.update {
                        it.copy(
                            isRefreshingMeta = false,
                            goals = goals
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshingMeta = false,
                            error = if (silent) it.error else error.message ?: "Could not refresh goals."
                        )
                    }
                }
        }
    }

    fun createGoal(goal: String, category: String? = null, priority: Int? = null) {
        val trimmedGoal = goal.trim()
        if (trimmedGoal.isEmpty() || _uiState.value.isSavingGoal) return
        if (!_uiState.value.canUseAgent) {
            showPremiumWall("AI goals")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGoal = true, error = null) }
            AgentApiService.createGoal(
                context = applicationContext,
                goal = trimmedGoal,
                category = category,
                priority = priority
            ).onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isSavingGoal = false,
                        goals = response.goals
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSavingGoal = false,
                        error = error.message ?: "Could not save goal."
                    )
                }
            }
        }
    }

    fun deleteGoal(goalId: String) {
        if (goalId.isBlank() || _uiState.value.isSavingGoal) return
        if (!_uiState.value.canUseAgent) {
            showPremiumWall("AI goals")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGoal = true, error = null) }
            AgentApiService.deleteGoal(applicationContext, goalId)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isSavingGoal = false,
                            goals = response.goals
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingGoal = false,
                            error = error.message ?: "Could not delete goal."
                        )
                    }
                }
        }
    }

    fun approvePendingAction(actionId: String) {
        if (actionId.isBlank() || _uiState.value.isResolvingApproval) return
        if (!_uiState.value.canUseAgent) {
            showPremiumWall("Agent approvals")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingApproval = true, error = null) }
            AgentApiService.approvePendingAction(applicationContext, actionId)
                .onSuccess { response ->
                    _uiState.update { state ->
                        val (navigationIntents, inlineResults, resetInlineUiState) = applyResolvedUiState(
                            current = state,
                            uiIntents = response.uiIntents
                        )

                        state.copy(
                            isResolvingApproval = false,
                            pendingApprovals = response.pendingActions,
                            pendingUiIntents = navigationIntents,
                            activeInlineResults = inlineResults,
                            dismissedInlineResultIds = if (resetInlineUiState) emptySet() else state.dismissedInlineResultIds,
                            inlineResultActionInProgress = if (resetInlineUiState) emptySet() else state.inlineResultActionInProgress,
                            lastExecutedActions = response.executedAction?.let(::listOf) ?: state.lastExecutedActions,
                            messages = response.assistantMessage?.takeIf(String::isNotBlank)?.let { assistantMessage ->
                                state.messages + AgentMessage(role = "assistant", content = assistantMessage)
                            } ?: state.messages
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isResolvingApproval = false,
                            error = error.message ?: "Could not approve action."
                        )
                    }
                }
        }
    }

    fun rejectPendingAction(actionId: String) {
        if (actionId.isBlank() || _uiState.value.isResolvingApproval) return
        if (!_uiState.value.canUseAgent) {
            showPremiumWall("Agent approvals")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isResolvingApproval = true, error = null) }
            AgentApiService.rejectPendingAction(applicationContext, actionId)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isResolvingApproval = false,
                            pendingApprovals = response.pendingActions,
                            messages = response.assistantMessage?.takeIf(String::isNotBlank)?.let { assistantMessage ->
                                it.messages + AgentMessage(role = "assistant", content = assistantMessage)
                            } ?: it.messages
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isResolvingApproval = false,
                            error = error.message ?: "Could not reject action."
                        )
                    }
                }
        }
    }

    fun sendMessage(
        message: String,
        surface: String,
        surfaceContext: Map<String, String> = emptyMap()
    ) {
        val trimmed = message.trim()
        if (trimmed.isEmpty() || _uiState.value.isSending) return

        val priorMessages = _uiState.value.messages
        stopAssistantSpeech()

        _uiState.update {
            it.copy(
                messages = it.messages + AgentMessage(role = "user", content = trimmed),
                isSending = true,
                error = null
            )
        }
        playResponsePulseSound()

        viewModelScope.launch {
            if (!entitlementsLoaded) {
                loadAiEntitlements(silent = true)
            }

            if (_uiState.value.canUseAgent) {
                val autonomyMode = _uiState.value.autonomyMode
                val autoRunEnabled = autonomyMode == "power"
                AgentApiService.sendTurn(
                    context = applicationContext,
                    inputText = trimmed,
                    surface = surface,
                    surfaceContext = surfaceContext,
                    allowAutonomousActions = autoRunEnabled,
                    autonomyMode = autonomyMode
                ).onSuccess { response ->
                    applyTurnResponse(response)
                }.onFailure { error ->
                    stopResponsePulseSound()
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            error = error.message ?: "Agent AI is unavailable right now."
                        )
                    }
                }
            } else {
                val history = priorMessages
                    .takeLast(10)
                    .mapNotNull { item ->
                        val role = when (item.role.lowercase()) {
                            "assistant" -> "assistant"
                            "user" -> "user"
                            else -> return@mapNotNull null
                        }
                        item.content.takeIf { it.isNotBlank() }?.let { content ->
                            AssistantChatHistoryItem(role = role, content = content)
                        }
                    }
                AgentApiService.sendAssistantMessage(
                    context = applicationContext,
                    message = trimmed,
                    conversationHistory = history,
                    intent = "free_assistant",
                    surface = normalizeAgentSurface(surface)
                ).onSuccess { response ->
                    stopResponsePulseSound()
                    _uiState.update { state ->
                        state.copy(
                            isSending = false,
                            aiTier = response.tier.ifBlank { state.aiTier },
                            canUseAgent = response.canUseAgent,
                            powerModeEligible = response.canUseAgent,
                            isPremium = state.isPremium || response.canUseAgent,
                            assistantDailyLimit = response.assistantDailyLimit,
                            assistantDailyRemaining = response.assistantDailyRemaining,
                            liveStatus = response.assistantDailyRemaining?.let { remaining ->
                                "$remaining free AI chats left today"
                            },
                            messages = state.messages + AgentMessage(
                                role = "assistant",
                                content = response.reply.ifBlank {
                                    "I can help plan, draft, and review. What should we improve first?"
                                }
                            )
                        )
                    }
                }.onFailure { error ->
                    stopResponsePulseSound()
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            error = error.message ?: "Vormex AI is unavailable right now."
                        )
                    }
                }
            }
        }
    }

    fun startVoiceRecording(context: Context) {
        if (_uiState.value.isRecordingVoice || _uiState.value.isSending) return
        if (!_uiState.value.canUseAgent) {
            showPremiumWall("Voice agent")
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(context.cacheDir, "agent_voice_${System.currentTimeMillis()}.m4a")
                    voiceRecordingFile = file
                    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(context)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }.apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setOutputFile(file.absolutePath)
                        prepare()
                        start()
                    }
                    mediaRecorder = recorder
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isRecordingVoice = true, error = null) }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(error = e.message ?: "Could not start voice recording.")
                        }
                    }
                }
            }
        }
    }

    fun stopVoiceRecordingAndSend(
        surface: String,
        surfaceContext: Map<String, String> = emptyMap()
    ) {
        if (!_uiState.value.isRecordingVoice) return
        if (!_uiState.value.canUseAgent) {
            showPremiumWall("Voice agent")
            cancelVoiceRecording()
            return
        }

        val autonomyMode = _uiState.value.autonomyMode
        val autoRunEnabled = autonomyMode == "power"

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    mediaRecorder?.apply {
                        stop()
                        release()
                    }
                    mediaRecorder = null

                    val file = voiceRecordingFile ?: return@withContext
                    voiceRecordingFile = null
                    val bytes = file.readBytes()
                    file.delete()

                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(isRecordingVoice = false, isSending = true, error = null)
                        }
                    }

                    val result = AgentApiService.sendVoiceTurn(
                        context = applicationContext,
                        audioBytes = bytes,
                        fileName = "agent-voice.m4a",
                        mimeType = "audio/mp4",
                        surface = surface,
                        surfaceContext = surfaceContext,
                        allowAutonomousActions = autoRunEnabled,
                        autonomyMode = autonomyMode,
                        synthesizeAudio = true
                    )

                    withContext(Dispatchers.Main) {
                        result.onSuccess { response ->
                            applyVoiceTurnResponse(response)
                        }.onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isSending = false,
                                    error = error.message ?: "Voice agent is unavailable right now."
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isRecordingVoice = false,
                                isSending = false,
                                error = e.message ?: "Could not send voice request."
                            )
                        }
                    }
                }
            }
        }
    }

    fun cancelVoiceRecording() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    mediaRecorder?.apply {
                        stop()
                        release()
                    }
                } catch (_: Exception) {
                } finally {
                    mediaRecorder = null
                    voiceRecordingFile?.delete()
                    voiceRecordingFile = null
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isRecordingVoice = false) }
                    }
                }
            }
        }
    }

    fun dismissInlineResultItem(userId: String) {
        if (userId.isBlank()) return

        _uiState.update { state ->
            val panel = state.activeInlineResults ?: return@update state
            val dismissedIds = state.dismissedInlineResultIds + userId
            val remainingPeople = panel.visiblePeople(dismissedIds)

            if (remainingPeople.isEmpty()) {
                state.copy(
                    activeInlineResults = null,
                    dismissedInlineResultIds = emptySet(),
                    inlineResultActionInProgress = emptySet()
                )
            } else {
                state.copy(dismissedInlineResultIds = dismissedIds)
            }
        }
    }

    fun dismissInlineResults() {
        _uiState.update {
            it.copy(
                activeInlineResults = null,
                dismissedInlineResultIds = emptySet(),
                inlineResultActionInProgress = emptySet()
            )
        }
    }

    fun sendInlineConnectionRequest(userId: String) {
        if (userId.isBlank()) return

        val currentPanel = _uiState.value.activeInlineResults ?: return
        val person = currentPanel.people.firstOrNull { it.id == userId } ?: return
        if (
            _uiState.value.inlineResultActionInProgress.contains(userId) ||
            person.connectionStatus == "connected" ||
            person.connectionStatus == "pending_sent"
        ) {
            return
        }

        _uiState.update {
            it.copy(inlineResultActionInProgress = it.inlineResultActionInProgress + userId)
        }

        viewModelScope.launch {
            ApiClient.sendConnectionRequest(applicationContext, userId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            activeInlineResults = state.activeInlineResults?.copy(
                                people = state.activeInlineResults.people.map { candidate ->
                                    if (candidate.id == userId) {
                                        candidate.copy(connectionStatus = "pending_sent")
                                    } else {
                                        candidate
                                    }
                                }
                            ),
                            inlineResultActionInProgress = state.inlineResultActionInProgress - userId
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            inlineResultActionInProgress = it.inlineResultActionInProgress - userId,
                            error = error.message ?: "Could not send the connection request."
                        )
                    }
                }
        }
    }

    fun consumeUiIntents() {
        _uiState.update { it.copy(pendingUiIntents = emptyList()) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearConversation() {
        stopResponsePulseSound()
        stopAssistantSpeech()
        _uiState.update {
            it.copy(
                messages = emptyList(),
                pendingUiIntents = emptyList(),
                activeInlineResults = null,
                dismissedInlineResultIds = emptySet(),
                inlineResultActionInProgress = emptySet(),
                lastExecutedActions = emptyList(),
                lastSuggestedActions = emptyList(),
                error = null,
                liveStatus = if (it.canUseAgent) "Ready" else "Free assistant ready"
            )
        }
    }

    private fun resolveUiArtifacts(uiIntents: List<AgentUiIntent>): Pair<List<AgentUiIntent>, AgentInlineResultsPanel?> {
        if (uiIntents.isEmpty()) {
            return emptyList<AgentUiIntent>() to null
        }

        val navigationIntents = mutableListOf<AgentUiIntent>()
        var inlineResults: AgentInlineResultsPanel? = null

        uiIntents.forEach { intent ->
            if (intent.type == "show_inline_results") {
                val payload = intent.payload ?: return@forEach
                val decodedPayload = runCatching {
                    inlineResultsJson.decodeFromJsonElement<AgentInlineResultsPayload>(payload)
                }.getOrNull() ?: return@forEach

                if (
                    decodedPayload.resultType.equals("people", ignoreCase = true) &&
                    decodedPayload.people.isNotEmpty()
                ) {
                    inlineResults = AgentInlineResultsPanel(
                        resultType = decodedPayload.resultType.ifBlank { "people" },
                        title = decodedPayload.title,
                        subtitle = decodedPayload.subtitle,
                        source = decodedPayload.source,
                        people = decodedPayload.people,
                        shownCount = decodedPayload.shownCount.takeIf { it > 0 }
                            ?: decodedPayload.people.size,
                        totalCount = decodedPayload.totalCount.takeIf { it > 0 }
                            ?: decodedPayload.people.size,
                        fallbackNavigationTarget = decodedPayload.fallbackNavigationTarget
                    )
                }
            } else {
                navigationIntents += intent
            }
        }

        return navigationIntents to inlineResults
    }

    private fun applyResolvedUiState(
        current: AgentUiState,
        uiIntents: List<AgentUiIntent>,
        clearInlineResultsWhenEmpty: Boolean = false
    ): Triple<List<AgentUiIntent>, AgentInlineResultsPanel?, Boolean> {
        val (navigationIntents, inlineResults) = resolveUiArtifacts(uiIntents)
        val shouldClearInlineResults =
            inlineResults == null &&
                (
                    navigationIntents.isNotEmpty() ||
                        (clearInlineResultsWhenEmpty && current.activeInlineResults != null)
                )
        val activeInlineResults = when {
            inlineResults != null -> inlineResults
            shouldClearInlineResults -> null
            else -> current.activeInlineResults
        }

        return Triple(navigationIntents, activeInlineResults, inlineResults != null || shouldClearInlineResults)
    }

    private fun applyTurnResponse(response: AgentTurnResponse) {
        stopResponsePulseSound()
        AgentSocketManager.updateSession(response.sessionState.sessionId)
        val assistantText = response.assistantMessage.ifBlank {
            "I’m here. Tell me what to do next in Vormex."
        }
        _uiState.update { state ->
            val (navigationIntents, inlineResults, resetInlineUiState) = applyResolvedUiState(
                current = state,
                uiIntents = response.uiIntents,
                clearInlineResultsWhenEmpty = true
            )

            state.copy(
                isSending = false,
                sessionState = response.sessionState,
                autoRunEnabled = response.sessionState.effectiveAutonomyMode == "power",
                autonomyMode = response.sessionState.effectiveAutonomyMode,
                aiTier = if (response.sessionState.isPremium) "premium" else state.aiTier,
                canUseAgent = response.sessionState.powerModeEligible,
                powerModeEligible = response.sessionState.powerModeEligible,
                isPremium = response.sessionState.isPremium,
                messages = state.messages + AgentMessage(
                    role = "assistant",
                    content = assistantText
                ),
                pendingUiIntents = navigationIntents,
                activeInlineResults = inlineResults,
                dismissedInlineResultIds = if (resetInlineUiState) emptySet() else state.dismissedInlineResultIds,
                inlineResultActionInProgress = if (resetInlineUiState) emptySet() else state.inlineResultActionInProgress,
                lastExecutedActions = response.executedActions,
                lastSuggestedActions = response.suggestedActions,
                pendingApprovals = response.pendingActions,
                goals = response.goals
            )
        }
        speakAssistantReplyIfPremium(assistantText)
    }

    private fun applyVoiceTurnResponse(
        response: AgentVoiceTurnResponse,
        playAudio: Boolean = true
    ) {
        stopResponsePulseSound()
        AgentSocketManager.updateSession(response.sessionState.sessionId)
        val assistantText = response.assistantMessage.ifBlank {
            "I’m here. Tell me what to do next in Vormex."
        }
        val newMessages = buildList {
            if (response.transcript.isNotBlank()) {
                add(AgentMessage(role = "user", content = response.transcript))
            }
            add(
                AgentMessage(
                    role = "assistant",
                    content = assistantText
                )
            )
        }

        _uiState.update { state ->
            val (navigationIntents, inlineResults, resetInlineUiState) = applyResolvedUiState(
                current = state,
                uiIntents = response.uiIntents,
                clearInlineResultsWhenEmpty = true
            )

            state.copy(
                isSending = false,
                sessionState = response.sessionState,
                autoRunEnabled = response.sessionState.effectiveAutonomyMode == "power",
                autonomyMode = response.sessionState.effectiveAutonomyMode,
                aiTier = if (response.sessionState.isPremium) "premium" else state.aiTier,
                canUseAgent = response.sessionState.powerModeEligible,
                powerModeEligible = response.sessionState.powerModeEligible,
                isPremium = response.sessionState.isPremium,
                messages = state.messages + newMessages,
                pendingUiIntents = navigationIntents,
                activeInlineResults = inlineResults,
                dismissedInlineResultIds = if (resetInlineUiState) emptySet() else state.dismissedInlineResultIds,
                inlineResultActionInProgress = if (resetInlineUiState) emptySet() else state.inlineResultActionInProgress,
                lastExecutedActions = response.executedActions,
                lastSuggestedActions = response.suggestedActions,
                pendingApprovals = response.pendingActions,
                goals = response.goals,
                liveUserTranscript = "",
                liveAssistantTranscript = "",
                isVoiceThinking = false
            )
        }

        if (playAudio && !response.audioBase64.isNullOrBlank()) {
            playSynthesizedAudio(response.audioBase64, response.audioMimeType)
        } else if (playAudio) {
            speakAssistantReplyIfPremium(assistantText)
        }
    }

    private fun queueRealtimeVoicePrompt(instructions: String) {
        val normalizedInstructions = instructions.trim()
        if (normalizedInstructions.isBlank()) {
            return
        }
        pendingRealtimePrompt = normalizedInstructions
        dispatchPendingRealtimePrompt()
    }

    private fun dispatchPendingRealtimePrompt(force: Boolean = false) {
        val instructions = pendingRealtimePrompt?.takeIf { it.isNotBlank() } ?: return
        val uiState = _uiState.value
        val sessionId = uiState.sessionState?.sessionId?.takeIf { it.isNotBlank() } ?: return
        val voiceLive =
            uiState.socketConnected &&
                !uiState.isVoiceSessionConnecting &&
                (force ||
                    uiState.isRecordingVoice ||
                    uiState.isVoiceListening ||
                    uiState.isVoiceThinking ||
                    uiState.isPlayingAudio)
        if (!voiceLive) {
            return
        }

        pendingRealtimePrompt = null
        AgentSocketManager.updateSession(sessionId)
        AgentSocketManager.requestRealtimeVoicePrompt(instructions)
    }

    private fun observeSocket() {
        viewModelScope.launch {
            AgentSocketManager.connectionStateFlow.collectLatest { state ->
                _uiState.update {
                    it.copy(socketConnected = state == AgentSocketManager.ConnectionState.CONNECTED)
                }
            }
        }

        viewModelScope.launch {
            AgentSocketManager.events.collectLatest { event ->
                when (event.type) {
                    "navigation_preview" -> {
                        if (!event.surface.isNullOrBlank()) {
                            pendingNavigationTarget = normalizeAgentSurface(event.surface)
                        }
                        _uiState.update {
                            it.copy(
                                liveStatus = event.message ?: it.liveStatus,
                                sessionState = if (!event.surface.isNullOrBlank()) {
                                    it.sessionState?.copy(currentSurface = normalizeAgentSurface(event.surface))
                                } else {
                                    it.sessionState
                                }
                            )
                        }
                    }
                    "pending_action_created",
                    "pending_action_resolved",
                    "pending_actions_changed",
                    "approval_executed" -> refreshPendingActions(silent = true)
                    "goals_changed" -> refreshGoals(silent = true)
                    "turn_completed" -> {
                        refreshPendingActions(silent = true)
                        refreshGoals(silent = true)
                        _uiState.update {
                            it.copy(
                                sessionState = if (!event.surface.isNullOrBlank()) {
                                    it.sessionState?.copy(currentSurface = normalizeAgentSurface(event.surface))
                                } else {
                                    it.sessionState
                                }
                            )
                        }
                        event.sessionId?.let(AgentSocketManager::updateSession)
                    }
                }
            }
        }

        viewModelScope.launch {
            AgentSocketManager.voiceEvents.collectLatest { event ->
                when (event) {
                    is AgentSocketManager.AgentVoiceSocketEvent.Ready -> {
                        stopResponsePulseSound()
                        if (shouldStartRealtimeCapture) {
                            shouldStartRealtimeCapture = false
                            beginRealtimeCapture()
                            dispatchPendingRealtimePrompt()
                        } else {
                            _uiState.update {
                                it.copy(
                                    isVoiceSessionConnecting = false,
                                    isRecordingVoice = true,
                                    isVoiceListening = false,
                                    isVoiceThinking = false,
                                    isPlayingAudio = false
                                )
                            }
                            dispatchPendingRealtimePrompt(force = true)
                        }
                    }

                    is AgentSocketManager.AgentVoiceSocketEvent.State -> {
                        when (event.state) {
                            "connecting" -> _uiState.update {
                                stopResponsePulseSound()
                                it.copy(isVoiceSessionConnecting = true, error = null)
                            }

                            "ready" -> {
                                stopResponsePulseSound()
                                _uiState.update {
                                    it.copy(
                                        isVoiceSessionConnecting = false,
                                        isVoiceThinking = false
                                    )
                                }
                                dispatchPendingRealtimePrompt()
                            }

                            "listening" -> {
                                stopResponsePulseSound()
                                realtimeVoiceManager.setAssistantPlaybackSuppressed(true)
                                realtimeVoiceManager.stopAssistantPlayback()
                                _uiState.update {
                                    it.copy(
                                        isVoiceListening = true,
                                        isVoiceThinking = false,
                                        isPlayingAudio = false,
                                        liveUserTranscript = "",
                                        liveAssistantTranscript = ""
                                    )
                                }
                                dispatchPendingRealtimePrompt()
                            }

                            "processing" -> {
                                playResponsePulseSound()
                                _uiState.update {
                                    realtimeVoiceManager.setAssistantPlaybackSuppressed(false, flush = false)
                                    it.copy(
                                        isVoiceListening = false,
                                        isVoiceThinking = true
                                    )
                                }
                            }

                            "speaking" -> {
                                stopResponsePulseSound()
                                _uiState.update {
                                    realtimeVoiceManager.setAssistantPlaybackSuppressed(false, flush = false)
                                    it.copy(
                                        isVoiceThinking = false,
                                        isPlayingAudio = true
                                    )
                                }
                            }

                            "stopped" -> {
                                stopResponsePulseSound()
                                shouldStartRealtimeCapture = false
                                realtimeVoiceManager.stopCapture()
                                realtimeVoiceManager.stopAssistantPlayback()
                                clearRealtimeVoiceState()
                            }
                        }
                    }

                    is AgentSocketManager.AgentVoiceSocketEvent.UserTranscript -> {
                        if (_uiState.value.isPlayingAudio) {
                            realtimeVoiceManager.setAssistantPlaybackSuppressed(true)
                        }
                        _uiState.update {
                            it.copy(liveUserTranscript = event.text)
                        }
                    }

                    is AgentSocketManager.AgentVoiceSocketEvent.AssistantTranscript -> {
                        _uiState.update {
                            it.copy(liveAssistantTranscript = event.text)
                        }
                    }

                    is AgentSocketManager.AgentVoiceSocketEvent.AudioDelta -> {
                        if (_uiState.value.isVoiceListening) {
                            return@collectLatest
                        }
                        realtimeVoiceManager.playAssistantChunk(event.responseId, event.audioBase64)
                        _uiState.update {
                            it.copy(isPlayingAudio = true)
                        }
                    }

                    is AgentSocketManager.AgentVoiceSocketEvent.AudioDone -> {
                        stopResponsePulseSound()
                        realtimeVoiceManager.stopAssistantPlayback(flush = false)
                        if (shouldStartRealtimeCaptureAfterPrompt) {
                            shouldStartRealtimeCaptureAfterPrompt = false
                            beginRealtimeCapture()
                        } else {
                            _uiState.update {
                                it.copy(isPlayingAudio = false)
                            }
                        }
                    }

                    is AgentSocketManager.AgentVoiceSocketEvent.TurnFinal -> {
                        applyVoiceTurnResponse(
                            response = event.response,
                            playAudio = false
                        )
                    }

                    is AgentSocketManager.AgentVoiceSocketEvent.Error -> {
                        stopResponsePulseSound()
                        shouldStartRealtimeCapture = false
                        pendingRealtimePrompt = null
                        realtimeVoiceManager.stopCapture()
                        realtimeVoiceManager.setAssistantPlaybackSuppressed(false, flush = true)
                        realtimeVoiceManager.stopAssistantPlayback()
                        _uiState.update {
                            it.copy(
                                isVoiceSessionConnecting = false,
                                isRecordingVoice = false,
                                isPlayingAudio = false,
                                isVoiceListening = false,
                                isVoiceThinking = false,
                                liveUserTranscript = "",
                                liveAssistantTranscript = "",
                                error = event.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun buildSurfaceSyncKey(
        sessionId: String,
        surface: String,
        surfaceContext: Map<String, String>
    ): String {
        val contextKey = surfaceContext.entries.joinToString("&") { "${it.key}=${it.value}" }
        return "$sessionId|$surface|$contextKey"
    }

    private fun describeUiIntent(intent: AgentUiIntent): String {
        return when (intent.type) {
            "switch_tab" -> "Opening ${formatSurfaceLabel(intent.tab)}"
            "open_profile" -> "Opening Profile"
            "open_chat" -> "Opening Chat"
            "open_group", "open_groups" -> "Opening Groups"
            "open_notifications" -> "Opening Notifications"
            "open_growth_task" -> "Opening Growth Hub"
            "show_match_stack" -> "Showing Matches"
            else -> "Navigating in Vormex"
        }
    }

    private fun targetSurfaceForIntent(intent: AgentUiIntent): String? {
        return when (intent.type) {
            "switch_tab" -> normalizeAgentSurface(intent.tab)
            "open_profile" -> "profile"
            "open_chat" -> "chat"
            "open_group", "open_groups" -> "groups"
            "open_notifications" -> "notifications"
            "open_growth_task" -> "growth_hub"
            "show_match_stack" -> "find_people"
            else -> null
        }
    }

    private fun normalizeAgentSurface(surface: String?): String {
        return when (surface?.trim()?.lowercase()) {
            null, "", "global" -> "global"
            "home" -> "feed"
            "find", "network" -> "find_people"
            "growth" -> "growth_hub"
            "talk", "talk_with_ai", "talk_with_vormex" -> "talk_with_vormex"
            else -> surface.trim().lowercase()
        }
    }

    private fun formatSurfaceLabel(surface: String?): String {
        return when (normalizeAgentSurface(surface)) {
            "feed" -> "Home"
            "find_people" -> "Find People"
            "chat" -> "Chat"
            "groups" -> "Groups"
            "profile" -> "Profile"
            "notifications" -> "Notifications"
            "growth_hub" -> "Growth Hub"
            "talk_with_vormex" -> "vormex"
            else -> "Agent"
        }
    }

    private suspend fun connectSocketIfPossible(sessionId: String?) {
        val token = ApiClient.getToken(applicationContext) ?: return
        AgentSocketManager.connect(token, sessionId)
    }

    private fun playSynthesizedAudio(audioBase64: String, mimeType: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    stopResponsePulseSound()
                    mediaPlayer?.release()
                    mediaPlayer = null

                    val bytes = Base64.decode(audioBase64, Base64.DEFAULT)
                    val extension = when {
                        mimeType?.contains("mpeg") == true || mimeType?.contains("mp3") == true -> ".mp3"
                        else -> ".audio"
                    }
                    val tempFile = File(applicationContext.cacheDir, "agent_tts_${System.currentTimeMillis()}$extension")
                    tempFile.writeBytes(bytes)

                    val player = MediaPlayer().apply {
                        setDataSource(tempFile.absolutePath)
                        prepare()
                        setOnCompletionListener {
                            release()
                            tempFile.delete()
                            mediaPlayer = null
                            _uiState.update { state -> state.copy(isPlayingAudio = false) }
                        }
                        setOnErrorListener { mp, _, _ ->
                            mp.release()
                            tempFile.delete()
                            mediaPlayer = null
                            _uiState.update { state -> state.copy(isPlayingAudio = false) }
                            true
                        }
                        start()
                    }

                    mediaPlayer = player
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isPlayingAudio = true) }
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isPlayingAudio = false) }
                    }
                }
            }
        }
    }

    private fun playResponsePulseSound() {
        if (responsePulsePlayer?.isPlaying == true) {
            return
        }

        runCatching {
            responsePulsePlayer?.release()
            responsePulsePlayer = MediaPlayer.create(
                applicationContext,
                R.raw.mixkit_mystwrious_bass_pulse_2298
            )?.apply {
                isLooping = false
                setVolume(0.8f, 0.8f)
                setOnCompletionListener { player ->
                    player.release()
                    if (responsePulsePlayer === player) {
                        responsePulsePlayer = null
                    }
                }
                setOnErrorListener { player, _, _ ->
                    player.release()
                    if (responsePulsePlayer === player) {
                        responsePulsePlayer = null
                    }
                    true
                }
                start()
            }
        }.onFailure {
            runCatching { responsePulsePlayer?.release() }
            responsePulsePlayer = null
        }
    }

    private fun stopResponsePulseSound() {
        runCatching {
            responsePulsePlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        }
        responsePulsePlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        shouldStartRealtimeCapture = false
        AgentSocketManager.stopRealtimeVoice()
        realtimeVoiceManager.release()
        stopAssistantSpeech()
        runCatching { mediaRecorder?.release() }
        runCatching { mediaPlayer?.release() }
        runCatching { responsePulsePlayer?.release() }
        runCatching { assistantTts?.shutdown() }
        mediaRecorder = null
        mediaPlayer = null
        responsePulsePlayer = null
        assistantTts = null
        voiceRecordingFile?.delete()
        voiceRecordingFile = null
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AgentViewModel(context) as T
        }
    }
}
