package com.kyant.backdrop.catalog.linkedin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MentionSearchPolicyTest {
    @Test
    fun `normalizes query before searching`() {
        assertEquals("alex", MentionSearchPolicy.normalize("  alex  "))
    }

    @Test
    fun `requires at least two characters after trimming`() {
        assertFalse(MentionSearchPolicy.shouldSearch("a"))
        assertFalse(MentionSearchPolicy.shouldSearch(" a "))
        assertTrue(MentionSearchPolicy.shouldSearch("al"))
        assertTrue(MentionSearchPolicy.shouldSearch(" alex "))
    }

    @Test
    fun `finds active mention immediately before cursor`() {
        val activeMention = MentionSearchPolicy.findActiveMention("Working with @alex", 18)

        assertEquals(13, activeMention?.start)
        assertEquals(18, activeMention?.end)
        assertEquals("alex", activeMention?.query)
    }

    @Test
    fun `ignores mentions embedded inside emails and words`() {
        assertEquals(null, MentionSearchPolicy.findActiveMention("hello@vormex.com", 16))
        assertEquals(null, MentionSearchPolicy.findActiveMention("hello@alex", 11))
    }

    @Test
    fun `stops active mention when whitespace or punctuation is typed`() {
        assertEquals(null, MentionSearchPolicy.findActiveMention("Working with @alex ", 19))
        assertEquals(null, MentionSearchPolicy.findActiveMention("Working with @alex,", 19))
    }
}
