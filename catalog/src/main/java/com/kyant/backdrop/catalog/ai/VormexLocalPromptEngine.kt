package com.kyant.backdrop.catalog.ai

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.flow.collect

internal interface VormexLocalPromptEngine {
    suspend fun localState(): VormexAiLocalState
    suspend fun downloadFeature(): Result<Unit>
    suspend fun generateText(prompt: String): Result<String>
}

@RequiresApi(Build.VERSION_CODES.O)
internal class VormexMlKitPromptEngine : VormexLocalPromptEngine {
    private val model: GenerativeModel = Generation.getClient()

    override suspend fun localState(): VormexAiLocalState {
        return when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> VormexAiLocalState.LOCAL_AVAILABLE
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> VormexAiLocalState.LOCAL_DOWNLOADABLE
            else -> VormexAiLocalState.LOCAL_UNAVAILABLE
        }
    }

    override suspend fun downloadFeature(): Result<Unit> {
        var failedMessage: String? = null
        model.download().collect { status ->
            when (status) {
                is DownloadStatus.DownloadFailed -> {
                    failedMessage = status.e.message ?: "Failed to download on-device AI."
                }
                DownloadStatus.DownloadCompleted -> Unit
                else -> Unit
            }
        }
        return if (failedMessage == null) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException(failedMessage))
        }
    }

    override suspend fun generateText(prompt: String): Result<String> {
        return runCatching {
            model.generateContent(prompt)
                .candidates
                .firstOrNull()
                ?.text
                ?.trim()
                .orEmpty()
        }
    }
}
