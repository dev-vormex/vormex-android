package com.kyant.backdrop.catalog.linkedin.groups

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupRolePolicyTest {
    @Test
    fun `owner and admin roles are normalized case-insensitively`() {
        assertEquals("owner", normalizeGroupRole("OWNER"))
        assertEquals("owner", normalizeGroupRole("owner"))
        assertEquals("admin", normalizeGroupRole("ADMIN"))
        assertEquals("admin", normalizeGroupRole("admin"))

        assertTrue(canManageGroup("OWNER"))
        assertTrue(canManageGroup("owner"))
        assertTrue(canManageGroup("ADMIN"))
        assertTrue(canManageGroup("admin"))
    }

    @Test
    fun `non manager roles do not unlock owner admin tools`() {
        assertFalse(canManageGroup("MODERATOR"))
        assertFalse(canManageGroup("member"))
        assertFalse(canManageGroup(null))
    }
}
