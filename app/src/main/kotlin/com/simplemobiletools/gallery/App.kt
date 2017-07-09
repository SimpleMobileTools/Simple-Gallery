package com.simplemobiletools.gallery

import android.app.Application
import com.squareup.leakcanary.LeakCanary

class App : Application() {
    val USE_LEAK_CANARY = false
    override fun onCreate() {
        super.onCreate()
        if (USE_LEAK_CANARY) {
            if (LeakCanary.isInAnalyzerProcess(this)) {
                return
            }
            LeakCanary.install(this)
        }
    }
}
