package com.kyant.backdrop.catalog

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.kyant.backdrop.catalog.ai.VormexAiBootstrap
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.ChatSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.kyant.backdrop.catalog.location.CrossedPathsForegroundPresenceCoordinator

class VormexApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val crossedPathsPresence by lazy { CrossedPathsForegroundPresenceCoordinator(this, appScope) }

    override fun onCreate() {
        super.onCreate()
        VormexAiBootstrap.initialize(this)
        registerChatSocketWarmup()
    }

    private fun registerChatSocketWarmup() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                crossedPathsPresence.onForeground()
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
            override fun onActivityPaused(activity: Activity) { crossedPathsPresence.onBackground() }
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
