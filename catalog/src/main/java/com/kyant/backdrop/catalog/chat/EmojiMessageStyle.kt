package com.kyant.backdrop.catalog.chat

internal fun isSystemEmojiOnlyMessage(content: String): Boolean =
    systemEmojiClusterCount(content) > 0

internal fun systemEmojiMessageFontSizeSp(content: String): Int =
    when (systemEmojiClusterCount(content)) {
        1 -> 44
        2 -> 40
        3 -> 36
        else -> 32
    }

internal fun systemEmojiClusterCount(content: String): Int {
    val text = content.trim()
    if (text.isEmpty()) return 0

    var index = 0
    var clusters = 0
    var sawEmoji = false
    var joinNextEmoji = false
    var pendingRegionalIndicator = false

    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        val nextIndex = index + Character.charCount(codePoint)

        if (Character.isWhitespace(codePoint)) {
            joinNextEmoji = false
            pendingRegionalIndicator = false
            index = nextIndex
            continue
        }

        val keycapEnd = keycapSequenceEnd(text, index)
        if (keycapEnd != -1) {
            clusters++
            sawEmoji = true
            joinNextEmoji = false
            pendingRegionalIndicator = false
            index = keycapEnd
            continue
        }

        if (isEmojiContinuationCodePoint(codePoint)) {
            if (!sawEmoji) return 0
            index = nextIndex
            continue
        }

        if (codePoint == ZERO_WIDTH_JOINER) {
            if (!sawEmoji) return 0
            joinNextEmoji = true
            pendingRegionalIndicator = false
            index = nextIndex
            continue
        }

        if (isRegionalIndicator(codePoint)) {
            sawEmoji = true
            if (joinNextEmoji) {
                joinNextEmoji = false
                pendingRegionalIndicator = true
            } else if (pendingRegionalIndicator) {
                pendingRegionalIndicator = false
            } else {
                clusters++
                pendingRegionalIndicator = true
            }
            index = nextIndex
            continue
        }

        if (isEmojiBaseCodePoint(codePoint)) {
            sawEmoji = true
            if (!joinNextEmoji) clusters++
            joinNextEmoji = false
            pendingRegionalIndicator = false
            index = nextIndex
            continue
        }

        return 0
    }

    return if (sawEmoji) clusters.coerceAtLeast(1) else 0
}

private fun keycapSequenceEnd(text: String, startIndex: Int): Int {
    val base = text.codePointAt(startIndex)
    if (!isKeycapBase(base)) return -1

    var index = startIndex + Character.charCount(base)
    if (index < text.length && text.codePointAt(index) == VARIATION_SELECTOR_16) {
        index += Character.charCount(VARIATION_SELECTOR_16)
    }

    if (index < text.length && text.codePointAt(index) == COMBINING_ENCLOSING_KEYCAP) {
        return index + Character.charCount(COMBINING_ENCLOSING_KEYCAP)
    }

    return -1
}

private fun isKeycapBase(codePoint: Int): Boolean =
    codePoint == '#'.code ||
        codePoint == '*'.code ||
        codePoint in '0'.code..'9'.code

private fun isEmojiContinuationCodePoint(codePoint: Int): Boolean =
    codePoint == VARIATION_SELECTOR_15 ||
        codePoint == VARIATION_SELECTOR_16 ||
        codePoint == COMBINING_ENCLOSING_KEYCAP ||
        codePoint in EMOJI_MODIFIER_RANGE ||
        codePoint in TAG_SPEC_RANGE

private fun isRegionalIndicator(codePoint: Int): Boolean =
    codePoint in REGIONAL_INDICATOR_RANGE

private fun isEmojiBaseCodePoint(codePoint: Int): Boolean =
    codePoint in 0x1F000..0x1FAFF ||
        codePoint in 0x2600..0x27BF ||
        codePoint in 0x2B05..0x2B55 ||
        codePoint in 0x2194..0x21AA ||
        codePoint in 0x23E9..0x23FA ||
        codePoint in 0x25AA..0x25FE ||
        codePoint in setOf(
            0x00A9,
            0x00AE,
            0x203C,
            0x2049,
            0x2122,
            0x2139,
            0x231A,
            0x231B,
            0x2328,
            0x23CF,
            0x24C2,
            0x2934,
            0x2935,
            0x3030,
            0x303D,
            0x3297,
            0x3299
        )

private const val ZERO_WIDTH_JOINER = 0x200D
private const val VARIATION_SELECTOR_15 = 0xFE0E
private const val VARIATION_SELECTOR_16 = 0xFE0F
private const val COMBINING_ENCLOSING_KEYCAP = 0x20E3
private val EMOJI_MODIFIER_RANGE = 0x1F3FB..0x1F3FF
private val REGIONAL_INDICATOR_RANGE = 0x1F1E6..0x1F1FF
private val TAG_SPEC_RANGE = 0xE0020..0xE007F
