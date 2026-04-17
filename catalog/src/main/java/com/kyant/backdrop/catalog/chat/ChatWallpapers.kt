package com.kyant.backdrop.catalog.chat

import android.content.Context
import androidx.annotation.DrawableRes
import com.kyant.backdrop.catalog.R

internal data class ChatWallpaper(
    val key: String,
    val title: String,
    @param:DrawableRes val drawableRes: Int
)

internal val chatWallpapers = listOf(
    ChatWallpaper("chat_wallpaper_001", "Wallpaper 1", R.drawable.chat_wallpaper_001),
    ChatWallpaper("chat_wallpaper_002", "Wallpaper 2", R.drawable.chat_wallpaper_002),
    ChatWallpaper("chat_wallpaper_003", "Wallpaper 3", R.drawable.chat_wallpaper_003),
    ChatWallpaper("chat_wallpaper_004", "Wallpaper 4", R.drawable.chat_wallpaper_004),
    ChatWallpaper("chat_wallpaper_005", "Wallpaper 5", R.drawable.chat_wallpaper_005),
    ChatWallpaper("chat_wallpaper_006", "Wallpaper 6", R.drawable.chat_wallpaper_006),
    ChatWallpaper("chat_wallpaper_007", "Wallpaper 7", R.drawable.chat_wallpaper_007),
    ChatWallpaper("chat_wallpaper_008", "Wallpaper 8", R.drawable.chat_wallpaper_008),
    ChatWallpaper("chat_wallpaper_009", "Wallpaper 9", R.drawable.chat_wallpaper_009),
    ChatWallpaper("chat_wallpaper_010", "Wallpaper 10", R.drawable.chat_wallpaper_010),
    ChatWallpaper("chat_wallpaper_011", "Wallpaper 11", R.drawable.chat_wallpaper_011),
    ChatWallpaper("chat_wallpaper_012", "Wallpaper 12", R.drawable.chat_wallpaper_012),
    ChatWallpaper("chat_wallpaper_013", "Wallpaper 13", R.drawable.chat_wallpaper_013),
    ChatWallpaper("chat_wallpaper_014", "Wallpaper 14", R.drawable.chat_wallpaper_014),
    ChatWallpaper("chat_wallpaper_015", "Wallpaper 15", R.drawable.chat_wallpaper_015),
    ChatWallpaper("chat_wallpaper_016", "Wallpaper 16", R.drawable.chat_wallpaper_016),
    ChatWallpaper("chat_wallpaper_017", "Wallpaper 17", R.drawable.chat_wallpaper_017),
    ChatWallpaper("chat_wallpaper_018", "Wallpaper 18", R.drawable.chat_wallpaper_018),
    ChatWallpaper("chat_wallpaper_019", "Wallpaper 19", R.drawable.chat_wallpaper_019),
    ChatWallpaper("chat_wallpaper_020", "Wallpaper 20", R.drawable.chat_wallpaper_020),
    ChatWallpaper("chat_wallpaper_021", "Wallpaper 21", R.drawable.chat_wallpaper_021),
    ChatWallpaper("chat_wallpaper_022", "Wallpaper 22", R.drawable.chat_wallpaper_022),
    ChatWallpaper("chat_wallpaper_023", "Wallpaper 23", R.drawable.chat_wallpaper_023),
    ChatWallpaper("chat_wallpaper_024", "Wallpaper 24", R.drawable.chat_wallpaper_024),
    ChatWallpaper("chat_wallpaper_025", "Wallpaper 25", R.drawable.chat_wallpaper_025),
    ChatWallpaper("chat_wallpaper_026", "Wallpaper 26", R.drawable.chat_wallpaper_026),
    ChatWallpaper("chat_wallpaper_027", "Wallpaper 27", R.drawable.chat_wallpaper_027),
    ChatWallpaper("chat_wallpaper_028", "Wallpaper 28", R.drawable.chat_wallpaper_028),
    ChatWallpaper("chat_wallpaper_029", "Wallpaper 29", R.drawable.chat_wallpaper_029),
    ChatWallpaper("chat_wallpaper_030", "Wallpaper 30", R.drawable.chat_wallpaper_030),
    ChatWallpaper("chat_wallpaper_031", "Wallpaper 31", R.drawable.chat_wallpaper_031),
    ChatWallpaper("chat_wallpaper_032", "Wallpaper 32", R.drawable.chat_wallpaper_032),
    ChatWallpaper("chat_wallpaper_033", "Wallpaper 33", R.drawable.chat_wallpaper_033),
    ChatWallpaper("chat_wallpaper_034", "Wallpaper 34", R.drawable.chat_wallpaper_034),
    ChatWallpaper("chat_wallpaper_035", "Wallpaper 35", R.drawable.chat_wallpaper_035),
    ChatWallpaper("chat_wallpaper_036", "Wallpaper 36", R.drawable.chat_wallpaper_036),
    ChatWallpaper("chat_wallpaper_037", "Wallpaper 37", R.drawable.chat_wallpaper_037),
    ChatWallpaper("chat_wallpaper_038", "Wallpaper 38", R.drawable.chat_wallpaper_038)
)

internal object ChatWallpaperPreferences {
    private const val PREFS_NAME = "chat_wallpaper_preferences"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(conversationId: String) = "wallpaper_$conversationId"

    fun getSelectedKey(context: Context, conversationId: String): String? {
        if (conversationId.isBlank()) return null
        return prefs(context).getString(key(conversationId), null)
    }

    fun setSelectedKey(context: Context, conversationId: String, wallpaperKey: String?) {
        if (conversationId.isBlank()) return
        prefs(context).edit().apply {
            if (wallpaperKey.isNullOrBlank()) {
                remove(key(conversationId))
            } else {
                putString(key(conversationId), wallpaperKey)
            }
        }.apply()
    }
}
