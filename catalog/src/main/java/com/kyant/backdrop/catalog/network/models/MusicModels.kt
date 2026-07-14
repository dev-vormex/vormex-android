package com.kyant.backdrop.catalog.network.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicAttachment(
    val audioId: String,
    val title: String,
    val artist: String? = null,
    val albumArt: String? = null,
    val audioUrl: String? = null,
    val durationMs: Long? = null,
    val source: String? = null,
    val startTimeMs: Long? = 0L
)

@Serializable
data class AudioLibraryItem(
    val id: String,
    val title: String,
    val artist: String? = null,
    val albumName: String? = null,
    val albumArt: String? = null,
    val audioUrl: String? = null,
    val durationMs: Long? = null,
    val genre: String? = null,
    val mood: String? = null,
    val tempo: Int? = null,
    val isRoyaltyFree: Boolean = false,
    val source: String? = null,
    val licenseType: String? = null,
    val attribution: String? = null,
    val isOriginal: Boolean = false,
    val originalCreatorId: String? = null,
    val usageCount: Int = 0,
    val savesCount: Int = 0,
    val isSaved: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class AudioLibraryResponse(
    val audio: List<AudioLibraryItem> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false
)

fun AudioLibraryItem.toMusicAttachment(startTimeMs: Long = 0L): MusicAttachment = MusicAttachment(
    audioId = id,
    title = title,
    artist = artist,
    albumArt = albumArt,
    audioUrl = audioUrl,
    durationMs = durationMs,
    source = this.source,
    startTimeMs = startTimeMs
)

fun formatMusicDuration(durationMs: Long?): String {
    if (durationMs == null || durationMs <= 0L) {
        return "0:00"
    }

    val totalSeconds = durationMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
