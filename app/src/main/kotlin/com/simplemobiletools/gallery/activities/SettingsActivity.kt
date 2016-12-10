package com.simplemobiletools.gallery.activities

import android.os.Bundle
import android.support.v4.app.TaskStackBuilder
import android.view.View
import android.widget.AdapterView
import com.simplemobiletools.gallery.R
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupDarkTheme()
        setupSameSorting()
        setupShowHiddenFolders()
        setupAutoplayVideos()
        setupShowMedia()
    }

    private fun setupDarkTheme() {
        settings_dark_theme.isChecked = mConfig.isDarkTheme
        settings_dark_theme_holder.setOnClickListener {
            settings_dark_theme.toggle()
            mConfig.isDarkTheme = settings_dark_theme.isChecked
            restartActivity()
        }
    }

    private fun setupSameSorting() {
        settings_same_sorting.isChecked = mConfig.isSameSorting
        settings_same_sorting_holder.setOnClickListener {
            settings_same_sorting.toggle()
            mConfig.isSameSorting = settings_same_sorting.isChecked
        }
    }

    private fun setupShowHiddenFolders() {
        settings_show_hidden_folders.isChecked = mConfig.showHiddenFolders
        settings_show_hidden_folders_holder.setOnClickListener {
            settings_show_hidden_folders.toggle()
            mConfig.showHiddenFolders = settings_show_hidden_folders.isChecked
        }
    }

    private fun setupAutoplayVideos() {
        settings_autoplay_videos_holder.setOnClickListener {
            settings_autoplay_videos.toggle()
            mConfig.autoplayVideos = settings_autoplay_videos.isChecked
        }
    }

    private fun setupShowMedia() {
        settings_show_media.setSelection(mConfig.showMedia)
        settings_show_media.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                mConfig.showMedia = settings_show_media.selectedItemPosition
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun restartActivity() {
        TaskStackBuilder.create(applicationContext).addNextIntentWithParentStack(intent).startActivities()
    }
}
