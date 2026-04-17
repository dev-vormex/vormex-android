package com.kyant.backdrop.catalog.ai

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

object VormexAiBootstrap {
    fun initialize(context: Context) {
        FirebaseApp.initializeApp(context)
        val providerFactory = if (com.kyant.backdrop.catalog.BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(providerFactory)
        VormexAiRemoteConfig.initialize()
    }
}
