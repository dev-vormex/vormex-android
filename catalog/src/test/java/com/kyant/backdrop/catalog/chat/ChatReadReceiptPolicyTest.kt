package com.kyant.backdrop.catalog.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatReadReceiptPolicyTest {
    @Test
    fun `immediate read receipts skip debounce`() {
        assertEquals(0L, ChatReadReceiptPolicy.delayMillis(immediate = true))
    }

    @Test
    fun `normal read receipts are debounced`() {
        assertEquals(
            ChatReadReceiptPolicy.DebounceMillis,
            ChatReadReceiptPolicy.delayMillis(immediate = false)
        )
    }
}
