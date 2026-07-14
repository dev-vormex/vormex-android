package com.kyant.backdrop.catalog.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps the device's current FCM token synced with the backend whenever the app has
 * both notification access and an authenticated user session.
 */
object PushTokenRegistrar {
    private const val TAG = "PushTokenRegistrar"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncCurrentToken(context: Context) {
        val appContext = context.applicationContext
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "Current FCM token: $token")
            syncToken(appContext, token)
        }
    }

    fun syncToken(context: Context, token: String) {
        val appContext = context.applicationContext
        scope.launch {
            if (!SettingsPreferences.isPushNotificationDeliveryEnabled(appContext)) {
                Log.d(TAG, "Skipping FCM token sync because push notifications are disabled")
                return@launch
            }

            val authToken = ApiClient.getToken(appContext)
            if (authToken.isNullOrBlank()) {
                Log.d(TAG, "Skipping FCM token sync until the user is logged in")
                return@launch
            }

            ApiClient.registerDeviceToken(appContext, token, "android")
                .onSuccess {
                    Log.d(TAG, "FCM token synced with backend")
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to sync FCM token with backend", error)
                }
        }
    }

    fun setPushEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM token for push toggle failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            if (enabled) {
                syncToken(appContext, token)
                return@addOnCompleteListener
            }

            scope.launch {
                ApiClient.unregisterDeviceToken(appContext, token)
                    .onSuccess { Log.d(TAG, "FCM token unregistered after push was disabled") }
                    .onFailure { error -> Log.e(TAG, "Failed to unregister FCM token", error) }
            }
        }
    }
}
