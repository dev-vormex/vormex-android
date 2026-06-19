@file:androidx.media3.common.util.UnstableApi

package com.kyant.backdrop.catalog.linkedin.reels.player

import android.content.Context
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import kotlin.math.min

class BunnyDataSourceFactory private constructor(context: Context) {
    private val appContext = context.applicationContext

    private val httpFactory by lazy {
        // Bunny Stream playback URLs are CDN URLs. We intentionally do not ship
        // the Bunny API key in the Android client.
        DefaultHttpDataSource.Factory()
            .setUserAgent("Vormex/Android")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
    }

    private val upstreamFactory by lazy {
        DefaultDataSource.Factory(appContext, httpFactory)
    }

    private val cacheFactory by lazy {
        CacheDataSource.Factory()
            .setCache(CacheManager.getCache(appContext))
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    val loadErrorHandlingPolicy: LoadErrorHandlingPolicy by lazy {
        ReelsLoadErrorHandlingPolicy()
    }

    fun createDataSourceFactory(): DataSource.Factory = cacheFactory

    fun createMediaSourceFactory(): DefaultMediaSourceFactory {
        return DefaultMediaSourceFactory(cacheFactory)
            .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
    }

    companion object {
        @Volatile
        private var instance: BunnyDataSourceFactory? = null

        fun getInstance(context: Context): BunnyDataSourceFactory {
            return instance ?: synchronized(this) {
                instance ?: BunnyDataSourceFactory(context).also { instance = it }
            }
        }
    }
}

private class ReelsLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy(3) {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        return when (loadErrorInfo.errorCount.coerceAtLeast(1)) {
            1 -> 1_000L
            2 -> 2_000L
            3 -> 5_000L
            else -> min(5_000L, super.getRetryDelayMsFor(loadErrorInfo).coerceAtLeast(0L))
        }
    }
}
