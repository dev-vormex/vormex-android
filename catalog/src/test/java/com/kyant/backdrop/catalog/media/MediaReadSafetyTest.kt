package com.kyant.backdrop.catalog.media

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaReadSafetyTest {
    @Test
    fun `readBoundedBytes returns bytes under limit`() {
        val bytes = byteArrayOf(1, 2, 3, 4)

        val result = MediaReadSafety.readBoundedBytes(
            input = ByteArrayInputStream(bytes),
            maxBytes = 4,
            label = "Image"
        )

        assertArrayEquals(bytes, result)
    }

    @Test
    fun `readBoundedBytes rejects streams that exceed limit`() {
        val error = runCatching {
            MediaReadSafety.readBoundedBytes(
                input = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5)),
                maxBytes = 4,
                label = "Video"
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Video must be 4 bytes or less", error?.message)
    }

    @Test
    fun `validateKnownSize rejects files before reading`() {
        val error = runCatching {
            MediaReadSafety.validateKnownSize(
                knownSize = MediaReadSafety.MaxPostVideoBytes + 1,
                maxBytes = MediaReadSafety.MaxPostVideoBytes,
                label = "Video"
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Video must be 100 MB or less", error?.message)
    }
}
