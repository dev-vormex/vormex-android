package com.kyant.backdrop.catalog

import android.app.Application
import com.kyant.backdrop.catalog.ai.VormexAiBootstrap

class VormexApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        VormexAiBootstrap.initialize(this)
    }
}
