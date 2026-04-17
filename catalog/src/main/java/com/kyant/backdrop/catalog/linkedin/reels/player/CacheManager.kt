package com.kyant.backdrop.catalog.linkedin.reels.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object CacheManager {
    private const val CACHE_DIR_NAME = "reels_media_cache"
    private const val MAX_CACHE_BYTES = 500L * 1024L * 1024L

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Volatile
    private var databaseProvider: StandaloneDatabaseProvider? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        simpleCache?.let { return it }

        val appContext = context.applicationContext
        val cacheDir = File(appContext.cacheDir, CACHE_DIR_NAME)
        val provider = databaseProvider ?: StandaloneDatabaseProvider(appContext).also {
            databaseProvider = it
        }

        return SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
            provider
        ).also { cache ->
            simpleCache = cache
        }
    }

    @Synchronized
    fun release() {
        simpleCache?.release()
        simpleCache = null
        databaseProvider = null
    }
}
