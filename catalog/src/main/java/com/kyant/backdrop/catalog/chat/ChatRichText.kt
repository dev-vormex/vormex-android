package com.kyant.backdrop.catalog.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

private const val ColorCloseTag = "[/color]"

private data class ChatRichTextSpanRange(
    val start: Int,
    var end: Int,
    val style: SpanStyle
)

internal fun parseChatRichText(
    source: String,
    contentColor: Color
): AnnotatedString {
    val visible = StringBuilder(source.length)
    val spans = mutableListOf<ChatRichTextSpanRange>()
    val colorStack = mutableListOf<Color>()
    var bold = false
    var italic = false
    var strike = false
    var underline = false
    var code = false
    var index = 0

    fun hasStyle(): Boolean =
        bold || italic || strike || underline || code || colorStack.isNotEmpty()

    fun currentStyle(): SpanStyle {
        val decorations = buildList {
            if (strike) add(TextDecoration.LineThrough)
            if (underline) add(TextDecoration.Underline)
        }
        return SpanStyle(
            color = colorStack.lastOrNull() ?: Color.Unspecified,
            fontWeight = if (bold) FontWeight.Bold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
            textDecoration = when (decorations.size) {
                0 -> null
                1 -> decorations.first()
                else -> TextDecoration.combine(decorations)
            },
            fontFamily = if (code) FontFamily.Monospace else null,
            background = if (code) contentColor.copy(alpha = 0.10f) else Color.Unspecified
        )
    }

    fun addStyledChar(char: Char) {
        val start = visible.length
        visible.append(char)
        if (!hasStyle()) return

        val style = currentStyle()
        val previous = spans.lastOrNull()
        if (previous != null && previous.end == start && previous.style == style) {
            previous.end = visible.length
        } else {
            spans += ChatRichTextSpanRange(start, visible.length, style)
        }
    }

    fun addLiteral(text: String) {
        text.forEach(::addStyledChar)
    }

    fun hasClosing(marker: String, fromIndex: Int): Boolean =
        source.indexOf(marker, startIndex = fromIndex, ignoreCase = false) >= 0

    fun findSingleAsteriskClosing(fromIndex: Int): Boolean {
        var searchIndex = fromIndex
        while (searchIndex < source.length) {
            val found = source.indexOf('*', startIndex = searchIndex)
            if (found < 0) return false
            val isDoubleMarker =
                source.getOrNull(found - 1) == '*' || source.getOrNull(found + 1) == '*'
            if (!isDoubleMarker) return true
            searchIndex = found + 1
        }
        return false
    }

    while (index < source.length) {
        when {
            source[index] == '\\' && index + 1 < source.length && isEscapableRichTextChar(source[index + 1]) -> {
                addStyledChar(source[index + 1])
                index += 2
            }
            source[index] == '`' -> {
                if (code) {
                    code = false
                    index++
                } else if (hasClosing("`", index + 1)) {
                    code = true
                    index++
                } else {
                    addStyledChar(source[index])
                    index++
                }
            }
            code -> {
                addStyledChar(source[index])
                index++
            }
            source.startsWith("[color:", index, ignoreCase = true) -> {
                val tokenEnd = source.indexOf(']', startIndex = index)
                val closeIndex = source.indexOf(ColorCloseTag, startIndex = (tokenEnd + 1).coerceAtLeast(index), ignoreCase = true)
                val color = if (tokenEnd > index) {
                    parseChatRichTextColor(source.substring(index + "[color:".length, tokenEnd).trim())
                } else {
                    null
                }

                if (tokenEnd > index && closeIndex >= 0 && color != null) {
                    colorStack += color
                    index = tokenEnd + 1
                } else {
                    addStyledChar(source[index])
                    index++
                }
            }
            source.startsWith(ColorCloseTag, index, ignoreCase = true) && colorStack.isNotEmpty() -> {
                colorStack.removeAt(colorStack.lastIndex)
                index += ColorCloseTag.length
            }
            source.startsWith("**", index) -> {
                if (bold) {
                    bold = false
                    index += 2
                } else if (hasClosing("**", index + 2)) {
                    bold = true
                    index += 2
                } else {
                    addLiteral("**")
                    index += 2
                }
            }
            source.startsWith("~~", index) -> {
                if (strike) {
                    strike = false
                    index += 2
                } else if (hasClosing("~~", index + 2)) {
                    strike = true
                    index += 2
                } else {
                    addLiteral("~~")
                    index += 2
                }
            }
            source.startsWith("__", index) -> {
                if (underline) {
                    underline = false
                    index += 2
                } else if (hasClosing("__", index + 2)) {
                    underline = true
                    index += 2
                } else {
                    addLiteral("__")
                    index += 2
                }
            }
            source[index] == '*' -> {
                if (italic) {
                    italic = false
                    index++
                } else if (findSingleAsteriskClosing(index + 1)) {
                    italic = true
                    index++
                } else {
                    addStyledChar(source[index])
                    index++
                }
            }
            else -> {
                addStyledChar(source[index])
                index++
            }
        }
    }

    return buildAnnotatedString {
        append(visible.toString())
        spans.forEach { span ->
            addStyle(span.style, span.start, span.end)
        }
    }
}

private fun isEscapableRichTextChar(char: Char): Boolean =
    char == '*' || char == '~' || char == '_' || char == '`' || char == '[' || char == '\\'

private fun parseChatRichTextColor(raw: String): Color? {
    val normalized = raw.trim().lowercase()
    return when (normalized) {
        "black" -> Color.Black
        "darkgray", "darkgrey" -> Color.DarkGray
        "gray", "grey" -> Color.Gray
        "lightgray", "lightgrey" -> Color.LightGray
        "white" -> Color.White
        "red" -> Color.Red
        "green" -> Color.Green
        "blue" -> Color.Blue
        "yellow" -> Color.Yellow
        "cyan" -> Color.Cyan
        "magenta" -> Color.Magenta
        "transparent" -> Color.Transparent
        else -> parseHexChatRichTextColor(normalized)
    }
}

private fun parseHexChatRichTextColor(raw: String): Color? {
    if (!raw.startsWith("#")) return null
    val hex = raw.drop(1)
    if (hex.any { it !in '0'..'9' && it !in 'a'..'f' }) return null

    val argb = when (hex.length) {
        3 -> {
            val r = "${hex[0]}${hex[0]}"
            val g = "${hex[1]}${hex[1]}"
            val b = "${hex[2]}${hex[2]}"
            "ff$r$g$b"
        }
        4 -> {
            val a = "${hex[0]}${hex[0]}"
            val r = "${hex[1]}${hex[1]}"
            val g = "${hex[2]}${hex[2]}"
            val b = "${hex[3]}${hex[3]}"
            "$a$r$g$b"
        }
        6 -> "ff$hex"
        8 -> hex
        else -> return null
    }

    return Color(argb.toLong(16).toInt())
}
