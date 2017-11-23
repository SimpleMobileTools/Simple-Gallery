package com.simplemobiletools.gallery

import android.support.multidex.MultiDexApplication
import com.github.ajalt.reprint.core.Reprint
import com.simplemobiletools.gallery.extensions.config
import java.util.*

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        /*if (USE_LEAK_CANARY) {
            if (LeakCanary.isInAnalyzerProcess(this)) {
                return
            }
            LeakCanary.install(this)
        }*/

        if (config.useEnglish) {
            val conf = resources.configuration
            conf.locale = Locale.ENGLISH
            resources.updateConfiguration(conf, resources.displayMetrics)
        }
        Reprint.initialize(this)
    }
}
