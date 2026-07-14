package com.kyant.backdrop.catalog.ai

import android.content.Context
import android.util.Log
import com.kyant.backdrop.catalog.BuildConfig
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlin.random.Random

object VormexAiRemoteConfig {
    private const val TAG = "VormexAiRemoteConfig"

    const val KEY_KILL_SWITCH = "vormex_ai_kill_switch"
    const val KEY_ROLLOUT_PERCENT = "vormex_ai_rollout_percent"
    const val KEY_LOCAL_ENABLED = "vormex_ai_local_enabled"
    const val KEY_CLOUD_ENABLED = "vormex_ai_cloud_enabled"
    const val KEY_CHAT_ENABLED = "vormex_ai_chat_enabled"
    const val KEY_POST_ENABLED = "vormex_ai_post_enabled"
    const val KEY_PROFILE_ENABLED = "vormex_ai_profile_enabled"
    const val KEY_AGENT_ENABLED = "vormex_ai_agent_enabled"
    const val KEY_CLOUD_TEXT_MODEL = "vormex_ai_cloud_text_model"
    const val KEY_CLOUD_AGENT_MODEL = "vormex_ai_cloud_agent_model"
    const val KEY_MAX_OUTPUT_TOKENS = "vormex_ai_max_output_tokens"

    private const val PREFS_NAME = "vormex_ai_flags"
    private const val PREF_ROLLOUT_BUCKET = "rollout_bucket"

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance()
    }

    private val defaults = mapOf(
        KEY_KILL_SWITCH to false,
        KEY_ROLLOUT_PERCENT to 100L,
        KEY_LOCAL_ENABLED to true,
        KEY_CLOUD_ENABLED to true,
        KEY_CHAT_ENABLED to true,
        KEY_POST_ENABLED to true,
        KEY_PROFILE_ENABLED to true,
        KEY_AGENT_ENABLED to true,
        KEY_CLOUD_TEXT_MODEL to "gemini-2.5-flash-lite",
        KEY_CLOUD_AGENT_MODEL to "gemini-2.5-flash",
        KEY_MAX_OUTPUT_TOKENS to 160L
    )

    fun initialize() {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
            .build()
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(defaults)
        remoteConfig.fetchAndActivate()
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Log.d(TAG, "Remote Config updated: ${configUpdate.updatedKeys}")
                remoteConfig.activate()
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.w(TAG, "Remote Config realtime update failed: ${error.code}", error)
            }
        })
    }

    fun isLocalEnabled(context: Context, surface: VormexAiSurface): Boolean {
        return isFeatureEnabled(context, surface) && remoteConfig.getBoolean(KEY_LOCAL_ENABLED)
    }

    fun isCloudEnabled(context: Context, surface: VormexAiSurface): Boolean {
        return isFeatureEnabled(context, surface) && remoteConfig.getBoolean(KEY_CLOUD_ENABLED)
    }

    fun cloudModel(surface: VormexAiSurface): String {
        return if (surface == VormexAiSurface.AGENT) {
            remoteConfig.getString(KEY_CLOUD_AGENT_MODEL)
        } else {
            remoteConfig.getString(KEY_CLOUD_TEXT_MODEL)
        }
    }

    fun maxOutputTokens(): Int {
        return remoteConfig.getLong(KEY_MAX_OUTPUT_TOKENS).toInt().coerceIn(64, 512)
    }

    fun disabledReason(context: Context, surface: VormexAiSurface): String? {
        return when {
            remoteConfig.getBoolean(KEY_KILL_SWITCH) -> "AI is temporarily disabled."
            !isWithinRollout(context) -> "AI is still rolling out."
            !surfaceEnabled(surface) -> "AI is disabled for this screen."
            else -> null
        }
    }

    private fun isFeatureEnabled(context: Context, surface: VormexAiSurface): Boolean {
        return disabledReason(context, surface) == null
    }

    private fun surfaceEnabled(surface: VormexAiSurface): Boolean {
        val key = when (surface) {
            VormexAiSurface.CHAT -> KEY_CHAT_ENABLED
            VormexAiSurface.POST -> KEY_POST_ENABLED
            VormexAiSurface.PROFILE -> KEY_PROFILE_ENABLED
            VormexAiSurface.AGENT -> KEY_AGENT_ENABLED
        }
        return remoteConfig.getBoolean(key)
    }

    private fun isWithinRollout(context: Context): Boolean {
        val rolloutPercent = remoteConfig.getLong(KEY_ROLLOUT_PERCENT).toInt().coerceIn(0, 100)
        if (rolloutPercent >= 100) return true
        if (rolloutPercent <= 0) return false
        return rolloutBucket(context) < rolloutPercent
    }

    private fun rolloutBucket(context: Context): Int {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getInt(PREF_ROLLOUT_BUCKET, -1)
        if (stored in 0..99) return stored
        val generated = Random.nextInt(100)
        prefs.edit().putInt(PREF_ROLLOUT_BUCKET, generated).apply()
        return generated
    }
}
