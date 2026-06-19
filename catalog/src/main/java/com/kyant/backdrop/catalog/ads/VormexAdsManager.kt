package com.kyant.backdrop.catalog.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.kyant.backdrop.catalog.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object VormexAdsManager {
    private const val TAG = "VormexAdsManager"

    private val mobileAdsInitialized = AtomicBoolean(false)
    private val consentRequestStarted = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired = _privacyOptionsRequired.asStateFlow()

    fun requestConsentAndInitialize(activity: Activity) {
        if (!BuildConfig.ADS_ENABLED) {
            _canRequestAds.value = false
            _privacyOptionsRequired.value = false
            return
        }

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        updateConsentState(consentInformation)

        if (!consentRequestStarted.compareAndSet(false, true)) {
            initializeIfAllowed(activity.applicationContext, consentInformation)
            return
        }

        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                updateConsentState(consentInformation)
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form dismissed with error: ${formError.message}")
                    }
                    updateConsentState(consentInformation)
                    initializeIfAllowed(activity.applicationContext, consentInformation)
                }
                initializeIfAllowed(activity.applicationContext, consentInformation)
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.message}")
                updateConsentState(consentInformation)
                initializeIfAllowed(activity.applicationContext, consentInformation)
            }
        )
    }

    fun showPrivacyOptions(context: Context) {
        val activity = context.findActivity() ?: return
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options form dismissed with error: ${formError.message}")
            }
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            updateConsentState(consentInformation)
            initializeIfAllowed(activity.applicationContext, consentInformation)
        }
    }

    private fun initializeIfAllowed(context: Context, consentInformation: ConsentInformation) {
        updateConsentState(consentInformation)
        Log.d(
            TAG,
            "Ad init check: enabled=${BuildConfig.ADS_ENABLED}, canRequest=${consentInformation.canRequestAds()}"
        )
        if (!BuildConfig.ADS_ENABLED || !consentInformation.canRequestAds()) return
        if (!mobileAdsInitialized.compareAndSet(false, true)) return

        scope.launch {
            MobileAds.initialize(context) {
                Log.d(TAG, "Google Mobile Ads initialized")
            }
        }
    }

    private fun updateConsentState(consentInformation: ConsentInformation) {
        _canRequestAds.value = BuildConfig.ADS_ENABLED && consentInformation.canRequestAds()
        _privacyOptionsRequired.value =
            BuildConfig.ADS_ENABLED &&
                consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        Log.d(
            TAG,
            "Consent state: enabled=${BuildConfig.ADS_ENABLED}, canRequest=${_canRequestAds.value}, privacyRequired=${_privacyOptionsRequired.value}"
        )
    }
}

internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
