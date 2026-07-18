package com.kyant.backdrop.catalog.chat

import android.content.Context
import android.util.Log
import com.kyant.backdrop.catalog.chat.cache.ChatCacheRepository
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.ChatSyncResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ChatDeltaSyncManager {
    private const val TAG = "ChatDeltaSync"
    private val syncMutex = Mutex()
    private val _updates = MutableSharedFlow<ChatSyncResponse>(extraBufferCapacity = 16)
    val updates = _updates.asSharedFlow()

    suspend fun sync(context: Context) {
        val appContext = context.applicationContext
        syncMutex.withLock {
            val userId = ApiClient.getCurrentUserId(appContext)
                ?.takeIf { it.isNotBlank() }
                ?: return
            val repository = ChatCacheRepository(appContext)
            var cursor = ApiClient.getChatSyncCursor(appContext, userId)
            var hasMore: Boolean
            do {
                val response = ApiClient.syncChat(appContext, cursor).getOrElse { error ->
                    Log.w(TAG, "Chat delta sync failed", error)
                    return
                }
                repository.applyChatSync(userId, response)
                ApiClient.saveChatSyncCursor(appContext, userId, response.cursor)
                cursor = response.cursor
                hasMore = response.hasMore
                if (
                    response.messages.isNotEmpty() ||
                    response.statusChanges.isNotEmpty() ||
                    response.conversations.isNotEmpty()
                ) {
                    _updates.emit(response)
                }
            } while (hasMore)
        }
    }
}
