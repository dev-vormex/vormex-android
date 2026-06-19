package com.kyant.backdrop.catalog.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRichTextTest {
    @Test
    fun `renders bold markers without leaking markdown`() {
        val text = parseChatRichText(
            "**Career & growth:** resume improvements",
            Color.Black
        )

        assertEquals("Career & growth: resume improvements", text.text)
        assertNotNull(text.styleFor("Career & growth:") { fontWeight == FontWeight.Bold })
    }

    @Test
    fun `renders italic and strikethrough markers`() {
        val text = parseChatRichText(
            "I *can* help and ~~can't~~ perform app actions",
            Color.Black
        )

        assertEquals("I can help and can't perform app actions", text.text)
        assertNotNull(text.styleFor("can") { fontStyle == FontStyle.Italic })
        assertNotNull(text.styleFor("can't") { textDecoration == TextDecoration.LineThrough })
    }

    @Test
    fun `renders color underline and code markers`() {
        val text = parseChatRichText(
            "[color:#E53935]red[/color] __under__ `code`",
            Color.Black
        )

        assertEquals("red under code", text.text)
        assertNotNull(text.styleFor("red") { color == Color(0xFFE53935) })
        assertNotNull(text.styleFor("under") { textDecoration == TextDecoration.Underline })
        assertNotNull(text.styleFor("code") { fontFamily == FontFamily.Monospace })
    }

    @Test
    fun `keeps unmatched markers visible`() {
        val text = parseChatRichText(
            "2 * 3 and [color:#E53935]raw",
            Color.Black
        )

        assertEquals("2 * 3 and [color:#E53935]raw", text.text)
        assertTrue(text.spanStyles.isEmpty())
    }

    @Test
    fun `supports escaped rich text markers`() {
        val text = parseChatRichText(
            "\\*literal\\* and \\[color:#fff]",
            Color.Black
        )

        assertEquals("*literal* and [color:#fff]", text.text)
        assertTrue(text.spanStyles.isEmpty())
    }

    private fun AnnotatedString.styleFor(
        segment: String,
        predicate: SpanStyle.() -> Boolean
    ): AnnotatedString.Range<SpanStyle>? {
        val start = text.indexOf(segment)
        val end = start + segment.length
        return spanStyles.firstOrNull { range ->
            start >= 0 && range.start <= start && range.end >= end && range.item.predicate()
        }
    }
}
