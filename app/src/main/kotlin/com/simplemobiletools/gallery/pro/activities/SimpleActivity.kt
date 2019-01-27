package com.simplemobiletools.gallery.pro.activities

import android.annotation.SuppressLint
import android.view.WindowManager
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.helpers.isPiePlus
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.config

open class SimpleActivity : BaseSimpleActivity() {
    override fun getAppIconIDs() = arrayListOf(
            R.mipmap.ic_launcher_red,
            R.mipmap.ic_launcher_pink,
            R.mipmap.ic_launcher_purple,
            R.mipmap.ic_launcher_deep_purple,
            R.mipmap.ic_launcher_indigo,
            R.mipmap.ic_launcher_blue,
            R.mipmap.ic_launcher_light_blue,
            R.mipmap.ic_launcher_cyan,
            R.mipmap.ic_launcher_teal,
            R.mipmap.ic_launcher_green,
            R.mipmap.ic_launcher_light_green,
            R.mipmap.ic_launcher_lime,
            R.mipmap.ic_launcher_yellow,
            R.mipmap.ic_launcher_amber,
            R.mipmap.ic_launcher,
            R.mipmap.ic_launcher_deep_orange,
            R.mipmap.ic_launcher_brown,
            R.mipmap.ic_launcher_blue_grey,
            R.mipmap.ic_launcher_grey_black
    )

    override fun getAppLauncherName() = getString(R.string.app_launcher_name)

    @SuppressLint("InlinedApi")
    protected fun checkNotchSupport() {
        if (isPiePlus()) {
            val cutoutMode = when {
                config.showNotch -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                else -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }

            window.attributes.layoutInDisplayCutoutMode = cutoutMode
            if (config.showNotch) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            }
        }
    }
}
