package com.kyant.backdrop.catalog.ai

import android.content.Context
import android.os.Build
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.GenerationConfig
import com.kyant.backdrop.catalog.network.ApiClient
import java.util.ArrayDeque

class VormexAiGateway(context: Context) {
    private val appContext = context.applicationContext
    private val localEngine: VormexLocalPromptEngine? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        VormexMlKitPromptEngine()
    } else {
        null
    }

    suspend fun availability(surface: VormexAiSurface): VormexAiAvailability {
        val disabledReason = VormexAiRemoteConfig.disabledReason(appContext, surface)
        if (disabledReason != null) {
            return VormexAiAvailability(
                localState = VormexAiLocalState.LOCAL_UNAVAILABLE,
                cloudAllowed = false,
                featureEnabled = false,
                reason = disabledReason
            )
        }

        val localState = if (!VormexAiRemoteConfig.isLocalEnabled(appContext, surface)) {
            VormexAiLocalState.LOCAL_UNAVAILABLE
        } else {
            readLocalState()
        }

        return VormexAiAvailability(
            localState = localState,
            cloudAllowed = VormexAiRemoteConfig.isCloudEnabled(appContext, surface),
            featureEnabled = true
        )
    }

    suspend fun prepareOnDevice(
        surface: VormexAiSurface,
        operation: VormexAiOperationKind
    ): VormexAiPrepareResult {
        val availability = availability(surface)
        if (!availability.featureEnabled) {
            return VormexAiPrepareResult.Failure(availability.reason ?: "AI is disabled.")
        }
        val engine = localEngine ?: return VormexAiPrepareResult.Failure(
            "On-device AI needs Android 8.0 or newer."
        )
        when (availability.localState) {
            VormexAiLocalState.LOCAL_AVAILABLE -> return VormexAiPrepareResult.Ready
            VormexAiLocalState.LOCAL_UNAVAILABLE -> {
                VormexAiTelemetry.localUnsupported(appContext, surface, operation, "unavailable")
                return VormexAiPrepareResult.Failure("On-device AI is unavailable on this device.")
            }
            VormexAiLocalState.LOCAL_DOWNLOADABLE -> Unit
        }

        VormexAiTelemetry.prepareStarted(appContext, surface, operation)
        val downloadResult = engine.downloadFeature()
        return if (downloadResult.isSuccess) {
            VormexAiTelemetry.prepareCompleted(appContext, surface, operation)
            VormexAiPrepareResult.Ready
        } else {
            VormexAiPrepareResult.Failure(
                downloadResult.exceptionOrNull()?.message ?: "Failed to download on-device AI."
            )
        }
    }

    suspend fun smartReplies(
        lastIncomingMessage: String,
        surface: VormexAiSurface,
        allowCloudFallback: Boolean
    ): VormexAiSuggestionsResult {
        val availability = availability(surface)
        if (!availability.featureEnabled) {
            return VormexAiSuggestionsResult.Blocked(availability.reason ?: "AI is disabled.")
        }
        val prompt = """
            You create reply suggestions for a chat app.
            Return exactly 3 short replies.
            Rules:
            - one reply per line
            - no numbering
            - no quotes
            - keep each reply under 10 words
            Message:
            $lastIncomingMessage
        """.trimIndent()

        return when (val result = generateText(prompt, surface, VormexAiOperationKind.SMART_REPLIES, allowCloudFallback)) {
            is VormexAiTextResult.Success -> {
                val suggestions = parseSuggestions(result.text, lastIncomingMessage)
                VormexAiSuggestionsResult.Success(suggestions, result.source)
            }
            is VormexAiTextResult.NeedsDownload -> VormexAiSuggestionsResult.NeedsDownload(result.message)
            is VormexAiTextResult.Blocked -> {
                VormexAiSuggestionsResult.Blocked(result.message, result.canUseCloudFallback)
            }
            is VormexAiTextResult.Failure -> VormexAiSuggestionsResult.Failure(result.message)
        }
    }

    suspend fun rewrite(
        text: String,
        style: VormexAiRewriteStyle,
        surface: VormexAiSurface,
        allowCloudFallback: Boolean
    ): VormexAiTextResult {
        val prompt = """
            ${style.promptInstruction}
            Keep the original meaning.
            Return only the rewritten text.
            Text:
            ${sanitizeInput(text)}
        """.trimIndent()
        return generateText(prompt, surface, VormexAiOperationKind.REWRITE, allowCloudFallback)
    }

    suspend fun proofread(
        text: String,
        surface: VormexAiSurface,
        allowCloudFallback: Boolean
    ): VormexAiTextResult {
        val prompt = """
            Proofread the following text.
            Fix grammar, spelling, and punctuation.
            Keep the tone and meaning the same.
            Return only the corrected text.
            Text:
            ${sanitizeInput(text)}
        """.trimIndent()
        return generateText(prompt, surface, VormexAiOperationKind.PROOFREAD, allowCloudFallback)
    }

    suspend fun summarize(
        text: String,
        surface: VormexAiSurface,
        allowCloudFallback: Boolean,
        asBullets: Boolean = true
    ): VormexAiTextResult {
        val outputFormat = if (asBullets) {
            "Return at most 2 short bullet points."
        } else {
            "Return one concise summary paragraph."
        }
        val prompt = """
            Summarize the following text.
            $outputFormat
            Do not add any intro.
            Text:
            ${sanitizeInput(text)}
        """.trimIndent()
        return generateText(prompt, surface, VormexAiOperationKind.SUMMARIZE, allowCloudFallback)
    }

    private suspend fun generateText(
        prompt: String,
        surface: VormexAiSurface,
        operation: VormexAiOperationKind,
        allowCloudFallback: Boolean
    ): VormexAiTextResult {
        val availability = availability(surface)
        if (!availability.featureEnabled) {
            return VormexAiTextResult.Blocked(availability.reason ?: "AI is disabled.")
        }

        return when (availability.localState) {
            VormexAiLocalState.LOCAL_AVAILABLE -> {
                runLocalPrompt(prompt, surface, operation, availability, allowCloudFallback)
            }
            VormexAiLocalState.LOCAL_DOWNLOADABLE -> {
                if (allowCloudFallback && availability.cloudAllowed) {
                    runCloudPrompt(prompt, surface, operation)
                } else {
                    VormexAiTextResult.NeedsDownload()
                }
            }
            VormexAiLocalState.LOCAL_UNAVAILABLE -> {
                VormexAiTelemetry.localUnsupported(appContext, surface, operation, "unavailable")
                if (allowCloudFallback && availability.cloudAllowed) {
                    runCloudPrompt(prompt, surface, operation)
                } else {
                    if (availability.cloudAllowed) {
                        VormexAiTelemetry.cloudBlocked(appContext, surface, operation, "privacy_rule")
                    }
                    VormexAiTextResult.Blocked(
                        message = "On-device AI is unavailable on this device.",
                        canUseCloudFallback = availability.cloudAllowed
                    )
                }
            }
        }
    }

    private suspend fun runLocalPrompt(
        prompt: String,
        surface: VormexAiSurface,
        operation: VormexAiOperationKind,
        availability: VormexAiAvailability,
        allowCloudFallback: Boolean
    ): VormexAiTextResult {
        val engine = localEngine ?: return VormexAiTextResult.Blocked(
            message = "On-device AI needs Android 8.0 or newer.",
            canUseCloudFallback = availability.cloudAllowed
        )
        return try {
            val text = engine.generateText(prompt).getOrThrow()
            if (text.isBlank()) {
                VormexAiTextResult.Failure("AI did not return any text.")
            } else {
                VormexAiTelemetry.localSuccess(appContext, surface, operation)
                VormexAiTextResult.Success(text = text, source = VormexAiSource.LOCAL)
            }
        } catch (error: Exception) {
            val message = error.message.orEmpty()
            if (isBusyError(message)) {
                VormexAiTelemetry.localBusy(appContext, surface, operation, "busy_or_quota")
            }
            if (allowCloudFallback && availability.cloudAllowed) {
                runCloudPrompt(prompt, surface, operation)
            } else {
                VormexAiTextResult.Failure(
                    if (message.isBlank()) "On-device AI failed." else message
                )
            }
        }
    }

    private suspend fun runCloudPrompt(
        prompt: String,
        surface: VormexAiSurface,
        operation: VormexAiOperationKind
    ): VormexAiTextResult {
        val throttleKey = "${ApiClient.getCurrentUserId(appContext) ?: "anon"}:${surface.wireName}:${operation.wireName}"
        if (!CloudThrottle.allow(throttleKey)) {
            VormexAiTelemetry.cloudBlocked(appContext, surface, operation, "rate_limited")
            return VormexAiTextResult.Blocked("Cloud fallback is cooling down. Try again in a few minutes.")
        }
        return try {
            val generationConfig = GenerationConfig.builder()
                .setTemperature(0.2f)
                .setMaxOutputTokens(VormexAiRemoteConfig.maxOutputTokens())
                .build()
            val model = FirebaseAI.getInstance(backend = GenerativeBackend.googleAI())
                .generativeModel(
                    modelName = VormexAiRemoteConfig.cloudModel(surface),
                    generationConfig = generationConfig
                )
            val response = model.generateContent(prompt)
            val text = response.text?.trim().orEmpty()
            if (text.isBlank()) {
                VormexAiTextResult.Failure("Cloud AI did not return any text.")
            } else {
                VormexAiTelemetry.cloudUsed(appContext, surface, operation)
                VormexAiTextResult.Success(text = text, source = VormexAiSource.CLOUD)
            }
        } catch (error: Exception) {
            VormexAiTextResult.Failure(error.message ?: "Cloud fallback failed.")
        }
    }

    private suspend fun readLocalState(): VormexAiLocalState {
        val engine = localEngine ?: return VormexAiLocalState.LOCAL_UNAVAILABLE
        return engine.localState()
    }

    private fun sanitizeInput(text: String): String {
        return text.trim().take(3_500)
    }

    private fun parseSuggestions(raw: String, lastIncomingMessage: String): List<String> {
        val parsed = raw
            .lines()
            .map { line ->
                line
                    .replace(Regex("^\\s*[\\-•]+\\s*"), "")
                    .replace(Regex("^\\s*\\d+[\\.)-]\\s*"), "")
                    .trim()
                    .trim('"')
            }
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)
        return if (parsed.isNotEmpty()) parsed else fallbackReplies(lastIncomingMessage)
    }

    private fun fallbackReplies(lastIncomingMessage: String): List<String> {
        return when {
            lastIncomingMessage.contains("?", ignoreCase = true) -> {
                listOf("Yes, that works.", "Let me check.", "I’ll get back to you.")
            }
            lastIncomingMessage.contains("thank", ignoreCase = true) -> {
                listOf("You’re welcome.", "Happy to help.", "Anytime.")
            }
            else -> {
                listOf("Sounds good.", "Got it.", "Let’s do it.")
            }
        }
    }

    private fun isBusyError(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("busy") ||
            lower.contains("quota") ||
            lower.contains("resource_exhausted")
    }

    private object CloudThrottle {
        private const val WINDOW_MS = 10 * 60 * 1000L
        private const val MAX_REQUESTS = 8
        private val historyByKey = mutableMapOf<String, ArrayDeque<Long>>()

        fun allow(key: String): Boolean {
            val now = System.currentTimeMillis()
            synchronized(historyByKey) {
                val history = historyByKey.getOrPut(key) { ArrayDeque() }
                while (history.isNotEmpty() && now - history.first() > WINDOW_MS) {
                    history.removeFirst()
                }
                if (history.size >= MAX_REQUESTS) {
                    return false
                }
                history.addLast(now)
                return true
            }
        }
    }
}
