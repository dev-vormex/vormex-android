package com.kyant.backdrop.catalog.network

import org.junit.Assert.assertEquals
import org.junit.Test

class InputSecurityTest {
    @Test
    fun `pagination cursor allows signed keyset cursors longer than identifiers`() {
        val cursor = "eyJ2IjoxLCJzY29wZSI6InJlZWxzLmZlZWQiLCJpZCI6IjkyOWRiMWRhLTQ1ZTgtNDI1MC1hYjAzLTViYTE4YWRhNjc1OSIsInQiOiIyMDI2LTA2LTEwVDA0OjQyOjMyLjk0MloifQ.qjvwB2D_B_jJkgpfRPNW8Li7Njq0oMreAEY47x5aGtc"

        assertEquals(cursor, InputSecurity.optionalPaginationCursor(cursor, "cursor"))
    }

    @Test(expected = InputSecurity.InputValidationException::class)
    fun `pagination cursor still rejects unsafe characters`() {
        InputSecurity.optionalPaginationCursor("abc<script>", "cursor")
    }
}
