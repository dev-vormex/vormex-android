package com.kyant.backdrop.catalog.network

import com.kyant.backdrop.catalog.network.models.AssistantChatHistoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentApiServiceTest {
    @Test
    fun `trims long assistant history instead of rejecting the next message`() {
        val longAssistantReply = "a".repeat(900)

        val history = sanitizeAssistantConversationHistory(
            listOf(AssistantChatHistoryItem(role = "assistant", content = longAssistantReply))
        )

        assertEquals(1, history.size)
        assertEquals("assistant", history.single().role)
        assertEquals(500, history.single().content.length)
    }

    @Test
    fun `keeps only the latest ten valid history messages`() {
        val history = sanitizeAssistantConversationHistory(
            (1..12).map { index ->
                AssistantChatHistoryItem(role = "user", content = "message $index")
            }
        )

        assertEquals(10, history.size)
        assertEquals("message 3", history.first().content)
        assertEquals("message 12", history.last().content)
    }

    @Test
    fun `drops invalid roles and unsafe nonessential history`() {
        val history = sanitizeAssistantConversationHistory(
            listOf(
                AssistantChatHistoryItem(role = "system", content = "hidden rules"),
                AssistantChatHistoryItem(role = "assistant", content = "<script>alert(1)</script>"),
                AssistantChatHistoryItem(role = "user", content = "Can you send requests?")
            )
        )

        assertEquals(1, history.size)
        assertEquals("user", history.single().role)
        assertEquals("Can you send requests?", history.single().content)
    }

    @Test
    fun `drops blank history after trimming`() {
        val history = sanitizeAssistantConversationHistory(
            listOf(AssistantChatHistoryItem(role = "assistant", content = "   "))
        )

        assertTrue(history.isEmpty())
    }
}
