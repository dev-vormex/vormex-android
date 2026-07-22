package com.kyant.backdrop.catalog

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.kyant.backdrop.catalog.ai.VormexAiBootstrap
import com.kyant.backdrop.catalog.chat.cache.ChatOutboxWorker
import com.kyant.backdrop.catalog.chat.ChatDeltaSyncManager
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.ChatSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.kyant.backdrop.catalog.location.CrossedPathsForegroundPresenceCoordinator
import com.kyant.backdrop.catalog.recommendation.telemetry.RecommendationTelemetryWorker

class VormexApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val crossedPathsPresence by lazy { CrossedPathsForegroundPresenceCoordinator(this, appScope) }

    override fun onCreate() {
        super.onCreate()
        VormexAiBootstrap.initialize(this)
        ChatOutboxWorker.enqueue(this)
        RecommendationTelemetryWorker.enqueue(this)
        registerChatSocketWarmup()
    }

    private fun registerChatSocketWarmup() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                crossedPathsPresence.onForeground()
                appScope.launch { ChatDeltaSyncManager.sync(this@VormexApplication) }
                appScope.launch {
                    if (ChatSocketManager.getConnectionState() == ChatSocketManager.ConnectionState.CONNECTED) {
                        return@launch
                    }
                    val token = ApiClient.getRealtimeAccessToken(this@VormexApplication)
                        ?.takeIf { it.isNotBlank() }
                        ?: return@launch
                    ChatSocketManager.currentUserId = ApiClient.getCurrentUserId(this@VormexApplication)
                    ChatSocketManager.connect(token)
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) {
                crossedPathsPresence.onBackground()
                RecommendationTelemetryWorker.enqueue(this@VormexApplication, immediate = true)
            }
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
