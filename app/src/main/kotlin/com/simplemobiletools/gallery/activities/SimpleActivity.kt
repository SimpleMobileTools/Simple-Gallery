package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.simplemobiletools.gallery.Config
import com.simplemobiletools.gallery.Constants
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.Utils

open class SimpleActivity : AppCompatActivity() {
    lateinit var mConfig: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        mConfig = Config.newInstance(applicationContext)
        var theme = if (mConfig.isDarkTheme) R.style.AppTheme_Dark else R.style.AppTheme
        if (this is ViewPagerActivity || this is PhotoActivity || this is VideoActivity) {
            theme = if (mConfig.isDarkTheme) R.style.FullScreenTheme_Dark else R.style.FullScreenTheme
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == Constants.OPEN_DOCUMENT_TREE && resultCode == Activity.RESULT_OK && resultData != null) {
            Utils.saveTreeUri(this, resultData)
        }
    }
}
