package com.kyant.backdrop.catalog.linkedin

internal object MentionSearchPolicy {
    const val MinQueryLength = 2
    private const val MaxQueryLength = 30
    private val mentionBreakCharacters = setOf(',', ';', ':', '!', '?', '(', ')', '[', ']', '{', '}')

    data class ActiveMention(
        val start: Int,
        val end: Int,
        val query: String
    )

    fun normalize(query: String): String = query.trim()

    fun shouldSearch(query: String): Boolean = normalize(query).length >= MinQueryLength

    fun findActiveMention(text: String, cursor: Int): ActiveMention? {
        val safeCursor = cursor.coerceIn(0, text.length)
        val beforeCursor = text.substring(0, safeCursor)
        val atIndex = beforeCursor.lastIndexOf('@')
        if (atIndex < 0) return null

        if (atIndex > 0) {
            val previous = text[atIndex - 1]
            if (previous.isLetterOrDigit() || previous == '_' || previous == '.') return null
        }

        val query = beforeCursor.substring(atIndex + 1)
        if (query.length > MaxQueryLength) return null
        if (query.any { it.isWhitespace() || it in mentionBreakCharacters }) return null

        return ActiveMention(atIndex, safeCursor, query)
    }
}
