package com.kyant.backdrop.catalog.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiMessageStyleTest {
    @Test
    fun `detects emoji only system messages`() {
        assertTrue(isSystemEmojiOnlyMessage("😂"))
        assertTrue(isSystemEmojiOnlyMessage("😂 😂"))
        assertTrue(isSystemEmojiOnlyMessage("❤️"))
        assertTrue(isSystemEmojiOnlyMessage("👍🏽"))
        assertTrue(isSystemEmojiOnlyMessage("👨‍👩‍👧‍👦"))
        assertTrue(isSystemEmojiOnlyMessage("🇮🇳"))
        assertTrue(isSystemEmojiOnlyMessage("1️⃣"))
    }

    @Test
    fun `rejects mixed text messages`() {
        assertFalse(isSystemEmojiOnlyMessage(""))
        assertFalse(isSystemEmojiOnlyMessage("hello"))
        assertFalse(isSystemEmojiOnlyMessage("hello 😂"))
        assertFalse(isSystemEmojiOnlyMessage("😂 ok"))
        assertFalse(isSystemEmojiOnlyMessage("123"))
    }

    @Test
    fun `sizes single and grouped emoji messages`() {
        assertEquals(44, systemEmojiMessageFontSizeSp("😂"))
        assertEquals(40, systemEmojiMessageFontSizeSp("😂😂"))
        assertEquals(36, systemEmojiMessageFontSizeSp("😂😂😂"))
        assertEquals(32, systemEmojiMessageFontSizeSp("😂😂😂😂"))
        assertEquals(44, systemEmojiMessageFontSizeSp("👨‍👩‍👧‍👦"))
    }
}
