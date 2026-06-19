package com.kyant.backdrop.catalog.ads

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.kyant.backdrop.catalog.BuildConfig
import kotlin.math.roundToInt

private const val TAG_AD_ATTRIBUTION = "vormex_native_ad_attribution"
private const val TAG = "VormexNativeAdViews"
private const val TAG_HEADLINE = "vormex_native_ad_headline"
private const val TAG_BODY = "vormex_native_ad_body"
private const val TAG_ICON = "vormex_native_ad_icon"
private const val TAG_ADVERTISER = "vormex_native_ad_advertiser"
private const val TAG_CTA = "vormex_native_ad_cta"
private const val TAG_MEDIA = "vormex_native_ad_media"

@Composable
fun VormexNativeFeedAd(
    slotKey: String,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    modifier: Modifier = Modifier
) {
    VormexNativeAdSlot(
        slotKey = slotKey,
        adUnitId = BuildConfig.ADMOB_NATIVE_FEED_AD_UNIT_ID,
        fullScreen = false,
        contentColor = contentColor,
        accentColor = accentColor,
        isLightTheme = isLightTheme,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .heightIn(min = 220.dp)
    )
}

@Composable
fun VormexNativeReelsAdPage(
    slotKey: String,
    modifier: Modifier = Modifier
) {
    VormexNativeAdSlot(
        slotKey = slotKey,
        adUnitId = BuildConfig.ADMOB_NATIVE_REELS_AD_UNIT_ID,
        fullScreen = true,
        contentColor = Color.White,
        accentColor = Color(0xFF0A84FF),
        isLightTheme = false,
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun VormexNativeAdSlot(
    slotKey: String,
    adUnitId: String,
    fullScreen: Boolean,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val canRequestAds by VormexAdsManager.canRequestAds.collectAsState()
    var nativeAd by remember(slotKey) {
        mutableStateOf(VormexNativeAdCache.cachedAd(slotKey))
    }

    LaunchedEffect(canRequestAds, slotKey, adUnitId) {
        if (canRequestAds && nativeAd == null) {
            VormexNativeAdCache.load(
                context = context,
                slotKey = slotKey,
                adUnitId = adUnitId,
                onLoaded = { nativeAd = it }
            )
        }
    }

    if (!BuildConfig.ADS_ENABLED || !canRequestAds) return

    val ad = nativeAd
    if (ad == null) {
        if (fullScreen) {
            Log.w(TAG, "Native fullscreen ad page composed without a loaded ad for $slotKey")
        }
        return
    }

    val accentArgb = accentColor.toArgb()
    if (fullScreen) {
        AndroidView(
            factory = { ctx ->
                createReelsNativeAdView(ctx, accentArgb)
            },
            update = { adView ->
                bindNativeAd(adView, ad)
            },
            modifier = modifier
        )
    } else {
        val contentArgb = contentColor.toArgb()
        AndroidView(
            factory = { ctx ->
                createFeedNativeAdView(
                    context = ctx,
                    contentColor = contentArgb,
                    accentColor = accentArgb,
                    isLightTheme = isLightTheme
                )
            },
            update = { adView ->
                bindNativeAd(adView, ad)
            },
            modifier = modifier
        )
    }
}

private fun createFeedNativeAdView(
    context: Context,
    contentColor: Int,
    accentColor: Int,
    isLightTheme: Boolean
): NativeAdView {
    val cornerRadius = context.dp(22)
    val backgroundColor = if (isLightTheme) 0xF7FFFFFF.toInt() else 0xCC171717.toInt()
    val borderColor = if (isLightTheme) 0x1F000000 else 0x26FFFFFF

    return NativeAdView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(backgroundColor)
            setStroke(context.dp(1), borderColor)
            setCornerRadius(cornerRadius.toFloat())
        }
        setPadding(context.dp(14), context.dp(14), context.dp(14), context.dp(14))

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        content.addView(createFeedHeader(context, contentColor, accentColor))
        content.addView(
            TextView(context).apply {
                tag = TAG_BODY
                setTextColor(withAlpha(contentColor, 0.74f))
                textSize = 13f
                maxLines = 2
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = context.dp(8)
                }
            }
        )
        content.addView(
            SquareMediaContainer(context, isLightTheme).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = context.dp(12)
                }
            }
        )
        content.addView(
            Button(context).apply {
                tag = TAG_CTA
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(0xFFFFFFFF.toInt())
                backgroundTintList = ColorStateList.valueOf(accentColor)
                minHeight = 0
                minimumHeight = 0
                minWidth = 0
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    context.dp(42)
                ).apply {
                    topMargin = context.dp(12)
                }
            }
        )

        addView(content)
    }
}

private fun createFeedHeader(
    context: Context,
    contentColor: Int,
    accentColor: Int
): LinearLayout {
    return LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        addView(
            ImageView(context).apply {
                tag = TAG_ICON
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(withAlpha(accentColor, 0.16f))
                }
                layoutParams = LinearLayout.LayoutParams(context.dp(42), context.dp(42))
            }
        )

        addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = context.dp(10)
                    rightMargin = context.dp(10)
                }

                addView(
                    TextView(context).apply {
                        tag = TAG_HEADLINE
                        setTextColor(contentColor)
                        textSize = 15f
                        typeface = Typeface.DEFAULT_BOLD
                        maxLines = 1
                        includeFontPadding = false
                    }
                )
                addView(
                    TextView(context).apply {
                        tag = TAG_ADVERTISER
                        setTextColor(withAlpha(contentColor, 0.56f))
                        textSize = 12f
                        maxLines = 1
                        includeFontPadding = false
                    }
                )
            }
        )

        addView(createAttributionBadge(context, accentColor))
    }
}

private fun createReelsNativeAdView(
    context: Context,
    accentColor: Int
): NativeAdView {
    return NativeAdView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(0xFF000000.toInt())

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(
            FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                setBackgroundColor(0xFF050505.toInt())

                addView(
                    MediaView(context).apply {
                        tag = TAG_MEDIA
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                )

                addView(
                    createAttributionBadge(context, accentColor).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.TOP or Gravity.END
                        ).apply {
                            topMargin = context.dp(54)
                            rightMargin = context.dp(16)
                        }
                    }
                )
            }
        )

        root.addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(context.dp(18), context.dp(18), context.dp(18), context.dp(34))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                addView(
                    TextView(context).apply {
                        tag = TAG_HEADLINE
                        setTextColor(0xFFFFFFFF.toInt())
                        textSize = 22f
                        typeface = Typeface.DEFAULT_BOLD
                        maxLines = 2
                        includeFontPadding = false
                    }
                )
                addView(
                    TextView(context).apply {
                        tag = TAG_BODY
                        setTextColor(0xCCFFFFFF.toInt())
                        textSize = 14f
                        maxLines = 3
                        includeFontPadding = false
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = context.dp(8)
                        }
                    }
                )
                addView(
                    TextView(context).apply {
                        tag = TAG_ADVERTISER
                        setTextColor(0x99FFFFFF.toInt())
                        textSize = 12f
                        maxLines = 1
                        includeFontPadding = false
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = context.dp(8)
                        }
                    }
                )
                addView(
                    Button(context).apply {
                        tag = TAG_CTA
                        textSize = 15f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(0xFFFFFFFF.toInt())
                        backgroundTintList = ColorStateList.valueOf(accentColor)
                        minHeight = 0
                        minimumHeight = 0
                        minWidth = 0
                        includeFontPadding = false
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            context.dp(48)
                        ).apply {
                            topMargin = context.dp(16)
                        }
                    }
                )
            }
        )

        addView(root)
    }
}

private fun createAttributionBadge(context: Context, accentColor: Int): TextView {
    return TextView(context).apply {
        tag = TAG_AD_ATTRIBUTION
        text = "Ad"
        textSize = 11f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(accentColor)
        gravity = Gravity.CENTER
        includeFontPadding = false
        setPadding(context.dp(2), context.dp(2), context.dp(2), context.dp(2))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}

private class SquareMediaContainer(
    context: Context,
    isLightTheme: Boolean
) : FrameLayout(context) {
    init {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(if (isLightTheme) 0x11000000 else 0x18FFFFFF)
            setCornerRadius(context.dp(16).toFloat())
        }
        clipToOutline = true
        addView(
            MediaView(context).apply {
                tag = TAG_MEDIA
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val squareSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, squareSpec)
    }
}

private fun bindNativeAd(adView: NativeAdView, nativeAd: NativeAd) {
    if (adView.tag === nativeAd) return

    val headlineView = adView.findTaggedView<TextView>(TAG_HEADLINE)
    val bodyView = adView.findTaggedView<TextView>(TAG_BODY)
    val iconView = adView.findTaggedView<ImageView>(TAG_ICON)
    val advertiserView = adView.findTaggedView<TextView>(TAG_ADVERTISER)
    val ctaView = adView.findTaggedView<Button>(TAG_CTA)
    val mediaView = adView.findTaggedView<MediaView>(TAG_MEDIA)

    headlineView?.text = nativeAd.headline.orEmpty()
    bodyView?.bindOptionalText(nativeAd.body)
    advertiserView?.bindOptionalText(nativeAd.advertiser)
    ctaView?.bindOptionalText(nativeAd.callToAction)

    if (iconView != null) {
        val icon = nativeAd.icon
        if (icon?.drawable != null) {
            iconView.setImageDrawable(icon.drawable)
            iconView.visibility = View.VISIBLE
        } else {
            iconView.visibility = View.GONE
        }
    }

    if (mediaView != null) {
        val mediaContent = nativeAd.mediaContent
        mediaView.mediaContent = mediaContent
        mediaView.visibility = if (mediaContent != null) View.VISIBLE else View.GONE
    }

    adView.headlineView = headlineView
    adView.bodyView = bodyView
    adView.iconView = iconView
    adView.advertiserView = advertiserView
    adView.callToActionView = ctaView
    adView.mediaView = mediaView
    adView.setNativeAd(nativeAd)
    adView.tag = nativeAd
}

private inline fun <reified T : View> View.findTaggedView(tag: String): T? {
    return findTaggedView(tag, T::class.java) as? T
}

private fun View.findTaggedView(tag: String, viewClass: Class<out View>): View? {
    if (this.tag == tag && viewClass.isInstance(this)) return this
    if (this is ViewGroup) {
        for (index in 0 until childCount) {
            val result = getChildAt(index).findTaggedView(tag, viewClass)
            if (result != null) return result
        }
    }
    return null
}

private fun TextView.bindOptionalText(value: String?) {
    val safeValue = value?.trim().orEmpty()
    if (safeValue.isBlank()) {
        visibility = View.GONE
    } else {
        visibility = View.VISIBLE
        text = safeValue
    }
}

private fun Context.dp(value: Int): Int =
    (value * resources.displayMetrics.density).roundToInt()

private fun withAlpha(color: Int, alpha: Float): Int {
    val boundedAlpha = (alpha.coerceIn(0f, 1f) * 255).roundToInt()
    return (color and 0x00FFFFFF) or (boundedAlpha shl 24)
}
