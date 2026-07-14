package com.kyant.backdrop.catalog.linkedin

import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.models.ChatUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificationBadgeTest {
    @Test
    fun `premium entitlement uses special premium badge style by default`() {
        assertEquals(
            SettingsPreferences.PROFILE_BADGE_STYLE_PREMIUM,
            resolveVerificationBadgeStyle(null, isPremium = true)
        )
        assertEquals(
            SettingsPreferences.PROFILE_BADGE_STYLE_STUDENT,
            resolveVerificationBadgeStyle(SettingsPreferences.PROFILE_BADGE_STYLE_STUDENT, isPremium = true)
        )
    }

    @Test
    fun `free users render earned badge styles but not premium entitlement style`() {
        assertNull(resolveVerificationBadgeStyle(null))
        assertEquals(
            SettingsPreferences.PROFILE_BADGE_STYLE_STUDENT,
            resolveVerificationBadgeStyle(SettingsPreferences.PROFILE_BADGE_STYLE_STUDENT)
        )
        assertEquals(
            SettingsPreferences.PROFILE_BADGE_STYLE_PROFESSIONAL,
            resolveVerificationBadgeStyle(SettingsPreferences.PROFILE_BADGE_STYLE_PROFESSIONAL)
        )
        assertNull(resolveVerificationBadgeStyle(SettingsPreferences.PROFILE_BADGE_STYLE_PREMIUM))
    }

    @Test
    fun `badge preference resolver keeps editable custom styles`() {
        assertEquals(
            SettingsPreferences.PROFILE_BADGE_STYLE_PROFESSIONAL,
            resolveProfileBadgePreferenceStyle(SettingsPreferences.PROFILE_BADGE_STYLE_PROFESSIONAL)
        )
        assertEquals(
            SettingsPreferences.PROFILE_BADGE_STYLE_STUDENT,
            resolveProfileBadgePreferenceStyle(SettingsPreferences.PROFILE_BADGE_STYLE_STUDENT)
        )
        assertNull(resolveProfileBadgePreferenceStyle(SettingsPreferences.PROFILE_BADGE_STYLE_PREMIUM))
    }

    @Test
    fun `premium chat participants render a badge from premium entitlement`() {
        val chatUser = ChatUser(id = "premium-user", name = "Premium User", isPremium = true)

        assertTrue(chatUser.hasVerificationBadge())
        assertEquals(
            SettingsPreferences.PROFILE_BADGE_STYLE_PREMIUM,
            chatUser.verificationBadgeStyle()
        )
    }
}
