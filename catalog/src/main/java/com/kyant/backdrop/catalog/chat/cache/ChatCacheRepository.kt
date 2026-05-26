package com.kyant.backdrop.catalog.chat.cache

import android.content.Context
import androidx.room.withTransaction
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.ChatUser
import com.kyant.backdrop.catalog.network.models.Conversation
import com.kyant.backdrop.catalog.network.models.ConversationLastMessage
import com.kyant.backdrop.catalog.network.models.ConversationsResponse
import com.kyant.backdrop.catalog.network.models.Message
import com.kyant.backdrop.catalog.network.models.MessageReaction
import com.kyant.backdrop.catalog.network.models.MessagesResponse
import com.kyant.backdrop.catalog.network.models.ReplyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val DEFAULT_MESSAGE_PAGE_LIMIT = 50
private const val PREFETCH_MESSAGE_PAGE_LIMIT = 80
private const val MAX_CACHED_MESSAGES_PER_CONVERSATION = 1_000

class ChatCacheRepository(
    context: Context,
    private val database: ChatCacheDatabase = ChatCacheDatabase.getInstance(context)
) {
    private val appContext = context.applicationContext
    private val dao = database.chatCacheDao()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    suspend fun getCachedConversations(cacheOwnerId: String): List<Conversation> = withContext(Dispatchers.IO) {
        dao.getConversations(cacheOwnerId)
            .mapNotNull { entity ->
                runCatching { entityToConversation(entity) }.getOrNull()
            }
    }

    suspend fun getCachedConversation(
        cacheOwnerId: String,
        conversationId: String
    ): Conversation? = withContext(Dispatchers.IO) {
        dao.getConversation(cacheOwnerId, conversationId)?.let { entity ->
            runCatching { entityToConversation(entity) }.getOrNull()
        }
    }

    suspend fun getCachedMessagesSnapshot(
        cacheOwnerId: String,
        conversationId: String
    ): CachedMessagesSnapshot? = withContext(Dispatchers.IO) {
        val conversation = dao.getConversation(cacheOwnerId, conversationId) ?: return@withContext null
        val messages = dao.getMessages(cacheOwnerId, conversationId)
            .mapNotNull { entity ->
                runCatching { entityToMessage(entity) }.getOrNull()
            }
        if (messages.isEmpty() && conversation.messagesCachedAt <= 0L) {
            return@withContext null
        }

        CachedMessagesSnapshot(
            messages = messages,
            nextCursor = conversation.cachedNextCursor,
            hasMore = conversation.hasMoreMessages,
            cachedAt = conversation.messagesCachedAt
        )
    }

    suspend fun upsertConversation(
        cacheOwnerId: String,
        conversation: Conversation
    ) = withContext(Dispatchers.IO) {
        cacheConversations(cacheOwnerId, listOf(conversation), replaceAll = false)
    }

    suspend fun refreshConversations(
        cacheOwnerId: String,
        limit: Int = 30,
        cursor: String? = null
    ): Result<ConversationsResponse> = withContext(Dispatchers.IO) {
        ApiClient.getConversations(appContext, limit, cursor)
            .onSuccess { response ->
                cacheConversations(
                    cacheOwnerId = cacheOwnerId,
                    conversations = response.conversations,
                    replaceAll = cursor == null
                )
            }
    }

    suspend fun refreshMessages(
        cacheOwnerId: String,
        conversationId: String,
        limit: Int = DEFAULT_MESSAGE_PAGE_LIMIT,
        cursor: String? = null
    ): Result<MessagesResponse> = withContext(Dispatchers.IO) {
        ApiClient.getMessages(appContext, conversationId, limit, cursor)
            .onSuccess { response ->
                cacheMessagesResponse(
                    cacheOwnerId = cacheOwnerId,
                    conversationId = conversationId,
                    response = response,
                    cursor = cursor
                )
            }
    }

    suspend fun prefetchRecentMessages(
        cacheOwnerId: String,
        conversations: List<Conversation>,
        topCount: Int = 8,
        limit: Int = PREFETCH_MESSAGE_PAGE_LIMIT,
        freshnessWindowMs: Long = 2 * 60_000L
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        conversations
            .sortedByDescending(::conversationSortEpochMillis)
            .take(topCount)
            .forEach { conversation ->
                val cachedConversation = dao.getConversation(cacheOwnerId, conversation.id)
                val isFresh = cachedConversation?.messagesCachedAt?.let { cachedAt ->
                    cachedAt > 0L && now - cachedAt < freshnessWindowMs
                } ?: false
                if (!isFresh) {
                    refreshMessages(
                        cacheOwnerId = cacheOwnerId,
                        conversationId = conversation.id,
                        limit = limit,
                        cursor = null
                    )
                }
            }
    }

    suspend fun cacheConversationMessagesSnapshot(
        cacheOwnerId: String,
        conversation: Conversation?,
        messages: List<Message>,
        nextCursor: String?,
        hasMore: Boolean
    ) = withContext(Dispatchers.IO) {
        val conversationId = conversation?.id ?: messages.firstOrNull()?.conversationId ?: return@withContext
        val mergedMessages = dedupeAndSort(messages).takeLast(MAX_CACHED_MESSAGES_PER_CONVERSATION)

        database.withTransaction {
            val existingConversation = dao.getConversation(cacheOwnerId, conversationId)
            val effectiveNextCursor = if (messages.size > MAX_CACHED_MESSAGES_PER_CONVERSATION) {
                existingConversation?.cachedNextCursor
            } else {
                nextCursor
            }
            val effectiveHasMore = if (messages.size > MAX_CACHED_MESSAGES_PER_CONVERSATION) {
                existingConversation?.hasMoreMessages ?: hasMore
            } else {
                hasMore
            }
            val conversationEntity = conversation?.let {
                val latestMessage = mergedMessages.lastOrNull()
                val conversationWithFreshSummary = it.copy(
                    lastMessage = latestMessage?.toConversationLastMessage() ?: it.lastMessage,
                    lastMessageAt = latestMessage?.createdAt ?: it.lastMessageAt,
                    updatedAt = latestMessage?.updatedAt ?: it.updatedAt
                )
                conversationToEntity(
                    cacheOwnerId = cacheOwnerId,
                    conversation = conversationWithFreshSummary,
                    existing = existingConversation,
                    messagesCachedAt = System.currentTimeMillis(),
                    cachedNextCursor = effectiveNextCursor,
                    hasMoreMessages = effectiveHasMore
                )
            } ?: existingConversation?.copy(
                messagesCachedAt = System.currentTimeMillis(),
                cachedNextCursor = effectiveNextCursor,
                hasMoreMessages = effectiveHasMore
            )

            conversationEntity?.let { dao.upsertConversations(listOf(it)) }
            dao.deleteMessagesForConversation(cacheOwnerId, conversationId)
            if (mergedMessages.isNotEmpty()) {
                dao.upsertMessages(mergedMessages.map { message -> messageToEntity(cacheOwnerId, message) })
                dao.trimMessages(cacheOwnerId, conversationId, MAX_CACHED_MESSAGES_PER_CONVERSATION)
            }
        }
    }

    suspend fun upsertIncomingMessage(
        cacheOwnerId: String,
        conversationId: String,
        message: Message,
        currentUserId: String?,
        incrementUnread: Boolean
    ) = withContext(Dispatchers.IO) {
        database.withTransaction {
            dao.upsertMessages(listOf(messageToEntity(cacheOwnerId, message)))

            val shouldIncrementUnread = incrementUnread && message.senderId != currentUserId
            val cachedConversation = dao.getConversation(cacheOwnerId, conversationId)
                ?: conversationEntityFromMessage(
                    cacheOwnerId = cacheOwnerId,
                    conversationId = conversationId,
                    message = message,
                    currentUserId = currentUserId
                )
            dao.upsertConversations(
                listOf(
                    cachedConversation.copy(
                        lastMessageJson = json.encodeToString(message.toConversationLastMessage()),
                        lastMessageAt = message.createdAt,
                        lastMessageAtEpochMillis = parseEpochMillis(message.createdAt),
                        unreadCount = if (shouldIncrementUnread) cachedConversation.unreadCount + 1 else cachedConversation.unreadCount,
                        updatedAt = message.updatedAt,
                        updatedAtEpochMillis = parseEpochMillis(message.updatedAt)
                    )
                )
            )
            dao.trimMessages(cacheOwnerId, conversationId, MAX_CACHED_MESSAGES_PER_CONVERSATION)
        }
    }

    suspend fun markConversationRead(
        cacheOwnerId: String,
        conversationId: String
    ) = withContext(Dispatchers.IO) {
        dao.getConversation(cacheOwnerId, conversationId)?.let { conversation ->
            dao.upsertConversations(listOf(conversation.copy(unreadCount = 0)))
        }
    }

    suspend fun markOwnMessagesAsReadByPeer(
        cacheOwnerId: String,
        conversationId: String,
        currentUserId: String
    ) = withContext(Dispatchers.IO) {
        val messages = dao.getMessages(cacheOwnerId, conversationId)
        val updatedMessages = messages.map { entity ->
            if (entity.senderId == currentUserId && entity.status != "READ") {
                entity.copy(status = "READ", updatedAtEpochMillis = System.currentTimeMillis())
            } else {
                entity
            }
        }
        if (updatedMessages.isNotEmpty()) {
            dao.upsertMessages(updatedMessages)
        }
    }

    suspend fun updateCachedMessageContent(
        cacheOwnerId: String,
        conversationId: String,
        messageId: String,
        content: String
    ) = withContext(Dispatchers.IO) {
        val messages = dao.getMessages(cacheOwnerId, conversationId)
        val target = messages.firstOrNull { it.messageId == messageId } ?: return@withContext
        dao.upsertMessages(
            listOf(
                target.copy(
                    content = content,
                    updatedAt = formatNowIso(),
                    updatedAtEpochMillis = System.currentTimeMillis()
                )
            )
        )

        dao.getConversation(cacheOwnerId, conversationId)?.let { conversation ->
            val lastMessage = conversation.lastMessageJson
                ?.let { runCatching { json.decodeFromString<ConversationLastMessage>(it) }.getOrNull() }
            if (lastMessage?.id == messageId) {
                dao.upsertConversations(
                    listOf(
                        conversation.copy(
                            lastMessageJson = json.encodeToString(lastMessage.copy(content = content)),
                            updatedAt = formatNowIso(),
                            updatedAtEpochMillis = System.currentTimeMillis()
                        )
                    )
                )
            }
        }
    }

    suspend fun updateCachedMessageReactions(
        cacheOwnerId: String,
        conversationId: String,
        messageId: String,
        reactions: List<MessageReaction>
    ) = withContext(Dispatchers.IO) {
        val message = dao.getMessages(cacheOwnerId, conversationId)
            .firstOrNull { it.messageId == messageId }
            ?: return@withContext
        dao.upsertMessages(
            listOf(
                message.copy(
                    reactionsJson = if (reactions.isEmpty()) null else json.encodeToString(reactions),
                    updatedAtEpochMillis = System.currentTimeMillis()
                )
            )
        )
    }

    suspend fun deleteCachedMessage(
        cacheOwnerId: String,
        conversationId: String,
        messageId: String
    ) = withContext(Dispatchers.IO) {
        dao.deleteMessage(cacheOwnerId, conversationId, messageId)
    }

    suspend fun deleteConversation(
        cacheOwnerId: String,
        conversationId: String
    ) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                dao.deleteMessagesForConversation(cacheOwnerId, conversationId)
                dao.deleteConversation(cacheOwnerId, conversationId)
            }
        }
    }

    suspend fun clearAll(cacheOwnerId: String) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                dao.deleteAllMessages(cacheOwnerId)
                dao.deleteAllConversations(cacheOwnerId)
            }
        }
    }

    private suspend fun cacheConversations(
        cacheOwnerId: String,
        conversations: List<Conversation>,
        replaceAll: Boolean
    ) {
        val existingById = dao.getConversations(cacheOwnerId).associateBy { it.conversationId }
        val entities = conversations.map { conversation ->
            conversationToEntity(
                cacheOwnerId = cacheOwnerId,
                conversation = conversation,
                existing = existingById[conversation.id]
            )
        }
        database.withTransaction {
            dao.upsertConversations(entities)
            if (replaceAll) {
                if (conversations.isEmpty()) {
                    dao.deleteAllMessages(cacheOwnerId)
                    dao.deleteAllConversations(cacheOwnerId)
                }
            }
        }
    }

    private suspend fun cacheMessagesResponse(
        cacheOwnerId: String,
        conversationId: String,
        response: MessagesResponse,
        cursor: String?
    ) {
        val existingConversation = dao.getConversation(cacheOwnerId, conversationId)
        val existingMessages = dao.getMessages(cacheOwnerId, conversationId).map(::entityToMessage)
        val mergedMessages = dedupeAndSort(existingMessages + response.messages)
            .takeLast(MAX_CACHED_MESSAGES_PER_CONVERSATION)
        val hasCachedOlderHistory =
            cursor == null &&
                existingMessages.size > response.messages.size &&
                !existingConversation?.cachedNextCursor.isNullOrBlank()
        val effectiveNextCursor = if (hasCachedOlderHistory) {
            existingConversation.cachedNextCursor
        } else {
            response.nextCursor
        }
        val effectiveHasMore = if (hasCachedOlderHistory) {
            existingConversation.hasMoreMessages
        } else {
            response.hasMore
        }

        database.withTransaction {
            existingConversation?.let {
                dao.upsertConversations(
                    listOf(
                        it.copy(
                            messagesCachedAt = System.currentTimeMillis(),
                            cachedNextCursor = effectiveNextCursor,
                            hasMoreMessages = effectiveHasMore
                        )
                    )
                )
            }
            dao.deleteMessagesForConversation(cacheOwnerId, conversationId)
            if (mergedMessages.isNotEmpty()) {
                dao.upsertMessages(mergedMessages.map { message -> messageToEntity(cacheOwnerId, message) })
                dao.trimMessages(cacheOwnerId, conversationId, MAX_CACHED_MESSAGES_PER_CONVERSATION)
            }
        }
    }

    private fun conversationToEntity(
        cacheOwnerId: String,
        conversation: Conversation,
        existing: CachedConversationEntity? = null,
        messagesCachedAt: Long = existing?.messagesCachedAt ?: 0L,
        cachedNextCursor: String? = existing?.cachedNextCursor,
        hasMoreMessages: Boolean = existing?.hasMoreMessages ?: false
    ): CachedConversationEntity {
        return CachedConversationEntity(
            cacheOwnerId = cacheOwnerId,
            conversationId = conversation.id,
            participant1Id = conversation.participant1Id,
            participant2Id = conversation.participant2Id,
            participant1Json = json.encodeToString(conversation.participant1),
            participant2Json = json.encodeToString(conversation.participant2),
            otherParticipantJson = json.encodeToString(conversation.otherParticipant),
            lastMessageJson = conversation.lastMessage?.let(json::encodeToString),
            lastMessageAt = conversation.lastMessageAt,
            lastMessageAtEpochMillis = parseEpochMillis(conversation.lastMessageAt ?: conversation.lastMessage?.createdAt),
            unreadCount = conversation.unreadCount,
            createdAt = conversation.createdAt,
            updatedAt = conversation.updatedAt,
            updatedAtEpochMillis = parseEpochMillis(conversation.updatedAt),
            isMessageRequest = conversation.isMessageRequest,
            messagesCachedAt = messagesCachedAt,
            cachedNextCursor = cachedNextCursor,
            hasMoreMessages = hasMoreMessages
        )
    }

    private fun messageToEntity(
        cacheOwnerId: String,
        message: Message
    ): CachedMessageEntity {
        return CachedMessageEntity(
            cacheOwnerId = cacheOwnerId,
            conversationId = message.conversationId,
            messageId = message.id,
            senderId = message.senderId,
            receiverId = message.receiverId,
            content = message.content,
            contentType = message.contentType,
            mediaUrl = message.mediaUrl,
            mediaType = message.mediaType,
            fileName = message.fileName,
            fileSize = message.fileSize,
            status = message.status,
            deliveredAt = message.deliveredAt,
            readAt = message.readAt,
            isDeleted = message.isDeleted,
            replyToId = message.replyToId,
            replyToJson = message.replyTo?.let(json::encodeToString),
            senderJson = message.sender?.let(json::encodeToString),
            reactionsJson = if (message.reactions.isEmpty()) null else json.encodeToString(message.reactions),
            createdAt = message.createdAt,
            createdAtEpochMillis = parseEpochMillis(message.createdAt),
            updatedAt = message.updatedAt,
            updatedAtEpochMillis = parseEpochMillis(message.updatedAt)
        )
    }

    private fun conversationEntityFromMessage(
        cacheOwnerId: String,
        conversationId: String,
        message: Message,
        currentUserId: String?
    ): CachedConversationEntity {
        val ownerId = currentUserId?.takeIf { it.isNotBlank() } ?: cacheOwnerId
        val otherId = if (message.senderId == ownerId) message.receiverId else message.senderId
        val sender = message.sender ?: ChatUser(id = message.senderId)
        val owner = if (sender.id == ownerId) sender else ChatUser(id = ownerId)
        val other = if (sender.id == otherId) sender else ChatUser(id = otherId)

        return CachedConversationEntity(
            cacheOwnerId = cacheOwnerId,
            conversationId = conversationId,
            participant1Id = ownerId,
            participant2Id = otherId,
            participant1Json = json.encodeToString(owner),
            participant2Json = json.encodeToString(other),
            otherParticipantJson = json.encodeToString(other),
            createdAt = message.createdAt,
            updatedAt = message.updatedAt,
            updatedAtEpochMillis = parseEpochMillis(message.updatedAt)
        )
    }

    private fun entityToConversation(entity: CachedConversationEntity): Conversation {
        val participant1 = json.decodeFromString<ChatUser>(entity.participant1Json)
        val participant2 = json.decodeFromString<ChatUser>(entity.participant2Json)
        return Conversation(
            id = entity.conversationId,
            participant1Id = entity.participant1Id,
            participant2Id = entity.participant2Id,
            participant1 = participant1,
            participant2 = participant2,
            otherParticipant = json.decodeFromString<ChatUser>(entity.otherParticipantJson),
            lastMessage = entity.lastMessageJson?.let { json.decodeFromString<ConversationLastMessage>(it) },
            lastMessageAt = entity.lastMessageAt,
            unreadCount = entity.unreadCount,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isMessageRequest = entity.isMessageRequest
        )
    }

    private fun entityToMessage(entity: CachedMessageEntity): Message {
        return Message(
            id = entity.messageId,
            conversationId = entity.conversationId,
            senderId = entity.senderId,
            receiverId = entity.receiverId,
            content = entity.content,
            contentType = entity.contentType,
            mediaUrl = entity.mediaUrl,
            mediaType = entity.mediaType,
            fileName = entity.fileName,
            fileSize = entity.fileSize,
            status = entity.status,
            deliveredAt = entity.deliveredAt,
            readAt = entity.readAt,
            isDeleted = entity.isDeleted,
            replyToId = entity.replyToId,
            replyTo = entity.replyToJson?.let { json.decodeFromString<ReplyTo>(it) },
            sender = entity.senderJson?.let { json.decodeFromString<ChatUser>(it) },
            reactions = entity.reactionsJson?.let { json.decodeFromString<List<MessageReaction>>(it) }.orEmpty(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private fun dedupeAndSort(messages: List<Message>): List<Message> {
        return messages
            .groupBy { it.id }
            .map { (_, group) -> group.last() }
            .sortedWith(compareBy<Message> { parseEpochMillis(it.createdAt) }.thenBy { it.createdAt })
    }

    private fun conversationSortEpochMillis(conversation: Conversation): Long {
        return parseEpochMillis(conversation.lastMessageAt ?: conversation.updatedAt.ifBlank { conversation.createdAt })
    }

    private fun parseEpochMillis(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        patterns.forEach { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(value)?.time
            }.getOrNull()?.let { return it }
        }
        return 0L
    }

    private fun formatNowIso(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    private fun Message.toConversationLastMessage(): ConversationLastMessage {
        return ConversationLastMessage(
            id = id,
            content = content,
            contentType = contentType,
            senderId = senderId,
            status = status,
            createdAt = createdAt
        )
    }
}
