package com.kyant.backdrop.catalog.linkedin.groups

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupInvitePolicyTest {
    @Test
    fun `owner and admin can always share invite link`() {
        assertTrue(canShareGroupInviteLink("OWNER", "ADMINS"))
        assertTrue(canShareGroupInviteLink("owner", "MEMBERS"))
        assertTrue(canShareGroupInviteLink("ADMIN", "ADMINS"))
        assertTrue(canShareGroupInviteLink("admin", "MEMBERS"))
    }

    @Test
    fun `members can share only when visibility allows members`() {
        assertTrue(canShareGroupInviteLink("MEMBER", "MEMBERS"))
        assertTrue(canShareGroupInviteLink("member", "members"))
        assertFalse(canShareGroupInviteLink("MEMBER", "ADMINS"))
        assertFalse(canShareGroupInviteLink("member", null))
        assertFalse(canShareGroupInviteLink(null, "MEMBERS"))
    }

    @Test
    fun `invite visibility defaults to admins`() {
        assertEquals("ADMINS", normalizeGroupInviteVisibility(null))
        assertEquals("ADMINS", normalizeGroupInviteVisibility("unknown"))
        assertEquals("MEMBERS", normalizeGroupInviteVisibility("members"))
    }
}
