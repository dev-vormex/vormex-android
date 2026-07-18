package com.kyant.backdrop.catalog.chat.cache

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.ChatSocketManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

private const val MAX_AUTO_OUTBOX_ATTEMPTS = 5
private const val CHAT_OUTBOX_WORK_NAME = "direct-chat-outbox"

class ChatOutboxWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    private val repository = ChatCacheRepository(appContext)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun doWork(): Result {
        val ownerId = ApiClient.getCurrentUserId(applicationContext)
            ?.takeIf { it.isNotBlank() }
            ?: return Result.success()
        val entries = repository.getOutboxEntries(ownerId)
            .filter { it.attempts < MAX_AUTO_OUTBOX_ATTEMPTS }
        if (entries.isEmpty()) return Result.success()

        var retryNeeded = false
        entries.forEach { originalEntry ->
            var entry = originalEntry.copy(
                status = "sending",
                attempts = originalEntry.attempts + 1,
                nextAttemptAtEpochMillis = 0L
            )
            repository.upsertOutboxEntry(entry)

            val uploadResult = if (entry.mediaUrl.isNullOrBlank() && !entry.localFileUri.isNullOrBlank()) {
                val mimeType = entry.mimeType ?: "application/octet-stream"
                ApiClient.uploadChatMedia(
                    context = applicationContext,
                    uri = Uri.parse(entry.localFileUri),
                    fileName = entry.fileName ?: "attachment",
                    mimeType = mimeType,
                    fileSize = entry.fileSize?.toLong(),
                    durationMs = entry.durationMs
                ).map { upload ->
                    entry = entry.copy(
                        mediaUrl = upload.mediaUrl,
                        mediaType = upload.mediaType,
                        fileName = upload.fileName,
                        fileSize = upload.fileSize
                    )
                    entry
                }
            } else {
                kotlin.Result.success(entry)
            }

            uploadResult.fold(
                onSuccess = { uploadedEntry ->
                    repository.upsertOutboxEntry(uploadedEntry)
                    ApiClient.sendMessage(
                        context = applicationContext,
                        conversationId = uploadedEntry.conversationId,
                        content = uploadedEntry.content,
                        contentType = uploadedEntry.contentType,
                        mediaUrl = uploadedEntry.mediaUrl,
                        mediaType = uploadedEntry.mediaType,
                        fileName = uploadedEntry.fileName,
                        fileSize = uploadedEntry.fileSize,
                        replyToId = uploadedEntry.replyToId,
                        clientMessageId = uploadedEntry.clientMessageId
                    ).fold(
                        onSuccess = { message ->
                            repository.completeOutboxMessage(ownerId, uploadedEntry.clientMessageId, message)
                            ChatSocketManager.emitExternalIncomingMessage(
                                uploadedEntry.conversationId,
                                json.encodeToString(message),
                                source = "chat-outbox-worker"
                            )
                        },
                        onFailure = {
                            val failed = uploadedEntry.copy(
                                status = "failed",
                                nextAttemptAtEpochMillis = System.currentTimeMillis()
                            )
                            repository.upsertOutboxEntry(failed)
                            retryNeeded = retryNeeded || failed.attempts < MAX_AUTO_OUTBOX_ATTEMPTS
                        }
                    )
                },
                onFailure = {
                    val failed = entry.copy(
                        status = "failed",
                        nextAttemptAtEpochMillis = System.currentTimeMillis()
                    )
                    repository.upsertOutboxEntry(failed)
                    retryNeeded = retryNeeded || failed.attempts < MAX_AUTO_OUTBOX_ATTEMPTS
                }
            )
        }

        return if (retryNeeded) Result.retry() else Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<ChatOutboxWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                CHAT_OUTBOX_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }
    }
}
