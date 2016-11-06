package com.simplemobiletools.gallery.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem

import com.simplemobiletools.gallery.Config
import com.simplemobiletools.gallery.R

open class SimpleActivity : AppCompatActivity() {
    lateinit var config: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        config = Config.newInstance(applicationContext)
        var theme = if (config.isDarkTheme) R.style.AppTheme_Dark else R.style.AppTheme
        if (this is ViewPagerActivity || this is PhotoActivity || this is VideoActivity) {
            theme = if (config.isDarkTheme) R.style.FullScreenTheme_Dark else R.style.FullScreenTheme
        }
        setTheme(theme)
        super.onCreate(savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
