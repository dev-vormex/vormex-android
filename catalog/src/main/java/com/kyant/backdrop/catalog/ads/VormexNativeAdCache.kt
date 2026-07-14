package com.kyant.backdrop.catalog.ads

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.kyant.backdrop.catalog.BuildConfig

object VormexNativeAdCache {
    private const val TAG = "VormexNativeAdCache"
    private const val MAX_CACHED_ADS = 12
    private const val AD_EXPIRY_MS = 55L * 60L * 1000L
    private const val FAILED_LOAD_COOLDOWN_MS = 2L * 60L * 1000L

    private data class CachedNativeAd(
        val ad: NativeAd,
        val loadedAtMs: Long
    )

    private val cachedAds = LinkedHashMap<String, CachedNativeAd>(16, 0.75f, true)
    private val loadingSlots = mutableSetOf<String>()
    private val failedSlots = mutableMapOf<String, Long>()

    @Synchronized
    fun cachedAd(slotKey: String): NativeAd? {
        val cached = cachedAds[slotKey] ?: return null
        if (isExpired(cached)) {
            cachedAds.remove(slotKey)
            cached.ad.destroy()
            return null
        }
        return cached.ad
    }

    fun load(
        context: Context,
        slotKey: String,
        adUnitId: String,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (String) -> Unit = {}
    ) {
        if (!BuildConfig.ADS_ENABLED || adUnitId.isBlank()) {
            Log.d(TAG, "Native ad skipped for $slotKey: enabled=${BuildConfig.ADS_ENABLED}, adUnitBlank=${adUnitId.isBlank()}")
            return
        }

        synchronized(this) {
            cachedAd(slotKey)?.let {
                Log.d(TAG, "Native ad cache hit for $slotKey")
                onLoaded(it)
                return
            }

            val failedAtMs = failedSlots[slotKey]
            if (failedAtMs != null && SystemClock.elapsedRealtime() - failedAtMs < FAILED_LOAD_COOLDOWN_MS) {
                Log.d(TAG, "Native ad cooldown active for $slotKey")
                return
            }

            if (!loadingSlots.add(slotKey)) return
        }

        Log.d(TAG, "Native ad request started for $slotKey using ${adUnitId.take(24)}...")
        val adLoader = AdLoader.Builder(context.applicationContext, adUnitId)
            .forNativeAd { nativeAd ->
                val oldAd = synchronized(this) {
                    loadingSlots.remove(slotKey)
                    failedSlots.remove(slotKey)
                    val old = cachedAds.put(
                        slotKey,
                        CachedNativeAd(nativeAd, SystemClock.elapsedRealtime())
                    )
                    trimLocked()
                    old?.ad
                }
                oldAd?.destroy()
                Log.d(TAG, "Native ad loaded for $slotKey")
                onLoaded(nativeAd)
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        synchronized(this@VormexNativeAdCache) {
                            loadingSlots.remove(slotKey)
                            failedSlots[slotKey] = SystemClock.elapsedRealtime()
                        }
                        Log.w(TAG, "Native ad failed for $slotKey: ${adError.message}")
                        onFailed(adError.message)
                    }
                }
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    @Synchronized
    fun destroyAll() {
        cachedAds.values.forEach { it.ad.destroy() }
        cachedAds.clear()
        loadingSlots.clear()
        failedSlots.clear()
    }

    private fun isExpired(cached: CachedNativeAd): Boolean =
        SystemClock.elapsedRealtime() - cached.loadedAtMs >= AD_EXPIRY_MS

    private fun trimLocked() {
        val iterator = cachedAds.entries.iterator()
        while (cachedAds.size > MAX_CACHED_ADS && iterator.hasNext()) {
            val entry = iterator.next()
            iterator.remove()
            entry.value.ad.destroy()
        }
    }
}
