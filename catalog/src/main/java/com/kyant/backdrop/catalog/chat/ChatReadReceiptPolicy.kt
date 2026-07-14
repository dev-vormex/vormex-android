package com.kyant.backdrop.catalog.chat

internal object ChatReadReceiptPolicy {
    const val DebounceMillis = 750L

    fun delayMillis(immediate: Boolean): Long = if (immediate) 0L else DebounceMillis
}
