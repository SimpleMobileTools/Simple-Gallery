package com.simplemobiletools.gallery

import android.support.multidex.MultiDexApplication
import com.github.ajalt.reprint.core.Reprint
import com.simplemobiletools.commons.extensions.checkUseEnglish
import com.squareup.leakcanary.LeakCanary

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            if (LeakCanary.isInAnalyzerProcess(this)) {
                return
            }
            LeakCanary.install(this)
        }

        checkUseEnglish()
        Reprint.initialize(this)
    }
}
