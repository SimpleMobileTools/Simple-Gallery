package com.simplemobiletools.gallery.pro.activities

import android.content.Intent
import com.simplemobiletools.commons.activities.BaseSplashActivity

class SplashActivity : BaseSplashActivity() {
    override fun getAppPackageName() = "-1"

    override fun initActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
