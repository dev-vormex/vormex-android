package com.kyant.backdrop.catalog.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VormexDeepLinksTest {
    @Test
    fun `extracts group invite codes from web links`() {
        assertEquals("abc123", VormexDeepLinks.extractGroupInviteCode("https://vormex.in/groups/invite/abc123"))
        assertEquals("abc123", VormexDeepLinks.extractGroupInviteCode("https://www.vormex.in/groups/invite/abc123"))
    }

    @Test
    fun `extracts group invite codes from app links`() {
        assertEquals("abc123", VormexDeepLinks.extractGroupInviteCode("vormex://group-invite/abc123"))
        assertEquals("abc123", VormexDeepLinks.extractGroupInviteCode("vormex://group-invite?code=abc123"))
    }

    @Test
    fun `ignores non invite links`() {
        assertNull(VormexDeepLinks.extractGroupInviteCode("https://vormex.in/post/abc123"))
        assertNull(VormexDeepLinks.extractGroupInviteCode("https://example.com/groups/invite/abc123"))
    }
}
