package com.kyant.backdrop.catalog.ai

enum class VormexAiSurface(val wireName: String) {
    CHAT("chat"),
    POST("post"),
    PROFILE("profile"),
    AGENT("agent")
}

enum class VormexAiOperationKind(val wireName: String) {
    SMART_REPLIES("smart_replies"),
    REWRITE("rewrite"),
    PROOFREAD("proofread"),
    SUMMARIZE("summarize")
}

enum class VormexAiRewriteStyle(
    val wireName: String,
    val promptInstruction: String
) {
    PROFESSIONAL("professional", "Rewrite the text to sound professional and polished."),
    FRIENDLY("friendly", "Rewrite the text to sound warm, friendly, and approachable."),
    SHORTER("shorter", "Rewrite the text to be shorter while keeping the same meaning."),
    CLEARER("clearer", "Rewrite the text to be clearer and easier to understand.")
}

enum class VormexAiLocalState {
    LOCAL_AVAILABLE,
    LOCAL_DOWNLOADABLE,
    LOCAL_UNAVAILABLE
}

enum class VormexAiSource {
    LOCAL,
    CLOUD
}

data class VormexAiAvailability(
    val localState: VormexAiLocalState,
    val cloudAllowed: Boolean,
    val featureEnabled: Boolean,
    val reason: String? = null
)

sealed interface VormexAiTextResult {
    data class Success(
        val text: String,
        val source: VormexAiSource
    ) : VormexAiTextResult

    data class NeedsDownload(
        val message: String = "Prepare on-device AI to use this feature offline."
    ) : VormexAiTextResult

    data class Blocked(
        val message: String,
        val canUseCloudFallback: Boolean = false
    ) : VormexAiTextResult

    data class Failure(
        val message: String
    ) : VormexAiTextResult
}

sealed interface VormexAiSuggestionsResult {
    data class Success(
        val suggestions: List<String>,
        val source: VormexAiSource
    ) : VormexAiSuggestionsResult

    data class NeedsDownload(
        val message: String = "Prepare on-device AI to generate smart replies."
    ) : VormexAiSuggestionsResult

    data class Blocked(
        val message: String,
        val canUseCloudFallback: Boolean = false
    ) : VormexAiSuggestionsResult

    data class Failure(
        val message: String
    ) : VormexAiSuggestionsResult
}

sealed interface VormexAiPrepareResult {
    data object Ready : VormexAiPrepareResult
    data class Failure(val message: String) : VormexAiPrepareResult
}
