package com.kyant.backdrop.catalog.linkedin.reels

import androidx.compose.runtime.Immutable
import com.kyant.backdrop.catalog.network.models.ManagedAdPlacement
import com.kyant.backdrop.catalog.network.models.Reel

internal const val ReelsFirstNativeAdAfterItems = 5
internal const val ReelsNativeAdIntervalItems = 8
internal const val ReelsLoadMoreThresholdPages = 5

@Immutable
sealed class ReelsFeedPage {
    abstract val pageKey: String

    data class ReelItem(
        val reel: Reel,
        val reelIndex: Int
    ) : ReelsFeedPage() {
        override val pageKey: String = "reel_${reel.id}"
    }

    data class NativeAdItem(
        val sequence: Int
    ) : ReelsFeedPage() {
        val slotKey: String = "reels_native_$sequence"
        override val pageKey: String = "ad_$slotKey"
    }

    data class ManagedAdItem(
        val ad: ManagedAdPlacement
    ) : ReelsFeedPage() {
        override val pageKey: String = "managed_ad_${ad.slotKey}_${ad.campaignId}"
    }
}

internal object ReelsNativeAdPagingPolicy {
    fun isReservedAdSlotAfterReelCount(reelCount: Int): Boolean {
        return reelCount >= ReelsFirstNativeAdAfterItems &&
            (reelCount - ReelsFirstNativeAdAfterItems) % ReelsNativeAdIntervalItems == 0
    }

    fun shouldInsertAfterReelCount(
        reelCount: Int,
        includeNativeAds: Boolean
    ): Boolean {
        return includeNativeAds && isReservedAdSlotAfterReelCount(reelCount)
    }

    fun pageIndexForReelIndex(
        reelIndex: Int,
        reelCount: Int,
        includeNativeAds: Boolean
    ): Int {
        if (reelCount <= 0) return 0
        val boundedReelIndex = reelIndex.coerceIn(0, reelCount - 1)
        return boundedReelIndex + nativeAdsBeforeReelIndex(boundedReelIndex, includeNativeAds)
    }

    fun reelIndexForPageIndex(
        pageIndex: Int,
        reelCount: Int,
        includeNativeAds: Boolean
    ): Int? {
        var currentPageIndex = 0
        for (reelIndex in 0 until reelCount) {
            if (currentPageIndex == pageIndex) return reelIndex
            currentPageIndex++

            if (shouldInsertAfterReelCount(reelIndex + 1, includeNativeAds)) {
                if (currentPageIndex == pageIndex) return null
                currentPageIndex++
            }
        }
        return null
    }

    fun pageCountForReelCount(
        reelCount: Int,
        includeNativeAds: Boolean
    ): Int {
        if (reelCount <= 0) return 0
        var adCount = 0
        for (count in 1..reelCount) {
            if (shouldInsertAfterReelCount(count, includeNativeAds)) {
                adCount++
            }
        }
        return reelCount + adCount
    }

    fun shouldLoadMoreForPage(
        pageIndex: Int,
        pageCount: Int,
        thresholdPages: Int = ReelsLoadMoreThresholdPages
    ): Boolean {
        return pageCount > 0 && pageIndex >= pageCount - thresholdPages
    }

    private fun nativeAdsBeforeReelIndex(
        reelIndex: Int,
        includeNativeAds: Boolean
    ): Int {
        if (!includeNativeAds || reelIndex < ReelsFirstNativeAdAfterItems) return 0
        return 1 + ((reelIndex - ReelsFirstNativeAdAfterItems) / ReelsNativeAdIntervalItems)
    }
}

internal fun buildReelsFeedPages(
    reels: List<Reel>,
    includeNativeAds: Boolean,
    managedAdPlacements: List<ManagedAdPlacement> = emptyList(),
    readyNativeAdSequences: Set<Int>? = null,
    earliestNativeAdAfterReelCount: Int = 0
): List<ReelsFeedPage> {
    val managedAdsBySequence = managedAdPlacements
        .filter { it.placement.equals("reels", ignoreCase = true) }
        .associateBy { it.sequence }
    val includeAdSlots = includeNativeAds || managedAdsBySequence.isNotEmpty()
    val pages = ArrayList<ReelsFeedPage>(ReelsNativeAdPagingPolicy.pageCountForReelCount(reels.size, includeAdSlots))
    var nativeAdSequence = 0
    reels.forEachIndexed { index, reel ->
        pages.add(ReelsFeedPage.ReelItem(reel = reel, reelIndex = index))
        if (ReelsNativeAdPagingPolicy.shouldInsertAfterReelCount(index + 1, includeAdSlots)) {
            val managedAd = managedAdsBySequence[nativeAdSequence]
            when {
                managedAd != null -> pages.add(ReelsFeedPage.ManagedAdItem(managedAd))
                includeNativeAds &&
                    (readyNativeAdSequences == null || nativeAdSequence in readyNativeAdSequences) &&
                    index + 1 >= earliestNativeAdAfterReelCount -> pages.add(ReelsFeedPage.NativeAdItem(nativeAdSequence))
            }
            nativeAdSequence++
        }
    }
    return pages
}

internal fun managedAdPlacementsNetworkFirst(
    existing: List<ManagedAdPlacement>,
    incoming: List<ManagedAdPlacement>
): List<ManagedAdPlacement> {
    return (incoming + existing).distinctBy { it.slotKey }
}

internal fun reelsNativeAdSequencesToPreload(
    reelCount: Int,
    includeNativeAds: Boolean,
    managedAdPlacements: List<ManagedAdPlacement> = emptyList()
): List<Int> {
    if (!includeNativeAds || reelCount <= 0) return emptyList()
    val managedSequences = managedAdPlacements
        .asSequence()
        .filter { it.placement.equals("reels", ignoreCase = true) }
        .map { it.sequence }
        .toSet()
    val sequences = ArrayList<Int>()
    var sequence = 0
    for (count in 1..reelCount) {
        if (ReelsNativeAdPagingPolicy.isReservedAdSlotAfterReelCount(count)) {
            if (sequence !in managedSequences) {
                sequences.add(sequence)
            }
            sequence++
        }
    }
    return sequences
}

internal fun shouldPrefetchMoreReels(
    currentReelIndex: Int,
    reelCount: Int,
    prefetchDistance: Int = REELS_PREFETCH_DISTANCE
): Boolean {
    if (reelCount <= 0) return false
    val remainingItems = (reelCount - 1) - currentReelIndex.coerceIn(0, reelCount - 1)
    return remainingItems <= prefetchDistance
}
