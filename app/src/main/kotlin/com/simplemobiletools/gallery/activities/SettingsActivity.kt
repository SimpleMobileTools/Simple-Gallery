package com.simplemobiletools.gallery.activities

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.dialogs.SecurityDialog
import com.simplemobiletools.commons.extensions.handleHiddenFolderPasswordProtection
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.PROTECTION_FINGERPRINT
import com.simplemobiletools.commons.helpers.SHOW_ALL_TABS
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.ROTATE_BY_ASPECT_RATIO
import com.simplemobiletools.gallery.helpers.ROTATE_BY_DEVICE_ROTATION
import com.simplemobiletools.gallery.helpers.ROTATE_BY_SYSTEM_SETTING
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : SimpleActivity() {
    lateinit var res: Resources

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        res = resources
    }

    override fun onResume() {
        super.onResume()

        setupCustomizeColors()
        setupManageIncludedFolders()
        setupManageExcludedFolders()
        setupShowHiddenFolders()
        setupAutoplayVideos()
        setupLoopVideos()
        setupAnimateGifs()
        setupMaxBrightness()
        setupCropThumbnails()
        setupDarkBackground()
        setupScrollHorizontally()
        setupScreenRotation()
        setupHideSystemUI()
        setupReplaceShare()
        setupPasswordProtection()
        setupDeleteEmptyFolders()
        setupAllowVideoGestures()
        setupShowExtendedDetails()
        updateTextColors(settings_holder)
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupManageIncludedFolders() {
        settings_manage_included_folders_holder.setOnClickListener {
            startActivity(Intent(this, IncludedFoldersActivity::class.java))
        }
    }

    private fun setupManageExcludedFolders() {
        settings_manage_excluded_folders_holder.setOnClickListener {
            startActivity(Intent(this, ExcludedFoldersActivity::class.java))
        }
    }

    private fun setupShowHiddenFolders() {
        settings_show_hidden_folders.isChecked = config.showHiddenMedia
        settings_show_hidden_folders_holder.setOnClickListener {
            if (config.showHiddenMedia) {
                toggleHiddenFolders()
            } else {
                handleHiddenFolderPasswordProtection {
                    toggleHiddenFolders()
                }
            }
        }
    }

    private fun toggleHiddenFolders() {
        settings_show_hidden_folders.toggle()
        config.showHiddenMedia = settings_show_hidden_folders.isChecked
    }

    private fun setupAutoplayVideos() {
        settings_autoplay_videos.isChecked = config.autoplayVideos
        settings_autoplay_videos_holder.setOnClickListener {
            settings_autoplay_videos.toggle()
            config.autoplayVideos = settings_autoplay_videos.isChecked
        }
    }

    private fun setupLoopVideos() {
        settings_loop_videos.isChecked = config.loopVideos
        settings_loop_videos_holder.setOnClickListener {
            settings_loop_videos.toggle()
            config.loopVideos = settings_loop_videos.isChecked
        }
    }

    private fun setupAnimateGifs() {
        settings_animate_gifs.isChecked = config.animateGifs
        settings_animate_gifs_holder.setOnClickListener {
            settings_animate_gifs.toggle()
            config.animateGifs = settings_animate_gifs.isChecked
        }
    }

    private fun setupMaxBrightness() {
        settings_max_brightness.isChecked = config.maxBrightness
        settings_max_brightness_holder.setOnClickListener {
            settings_max_brightness.toggle()
            config.maxBrightness = settings_max_brightness.isChecked
        }
    }

    private fun setupCropThumbnails() {
        settings_crop_thumbnails.isChecked = config.cropThumbnails
        settings_crop_thumbnails_holder.setOnClickListener {
            settings_crop_thumbnails.toggle()
            config.cropThumbnails = settings_crop_thumbnails.isChecked
        }
    }

    private fun setupDarkBackground() {
        settings_dark_background.isChecked = config.darkBackground
        settings_dark_background_holder.setOnClickListener {
            settings_dark_background.toggle()
            config.darkBackground = settings_dark_background.isChecked
        }
    }

    private fun setupScrollHorizontally() {
        settings_scroll_horizontally.isChecked = config.scrollHorizontally
        settings_scroll_horizontally_holder.setOnClickListener {
            settings_scroll_horizontally.toggle()
            config.scrollHorizontally = settings_scroll_horizontally.isChecked
        }
    }

    private fun setupHideSystemUI() {
        settings_hide_system_ui.isChecked = config.hideSystemUI
        settings_hide_system_ui_holder.setOnClickListener {
            settings_hide_system_ui.toggle()
            config.hideSystemUI = settings_hide_system_ui.isChecked
        }
    }

    private fun setupReplaceShare() {
        settings_replace_share.isChecked = config.replaceShare
        settings_replace_share_holder.setOnClickListener {
            settings_replace_share.toggle()
            config.replaceShare = settings_replace_share.isChecked
        }
    }

    private fun setupPasswordProtection() {
        settings_password_protection.isChecked = config.isPasswordProtectionOn
        settings_password_protection_holder.setOnClickListener {
            val tabToShow = if (config.isPasswordProtectionOn) config.protectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.passwordHash, tabToShow) { hash, type ->
                val hasPasswordProtection = config.isPasswordProtectionOn
                settings_password_protection.isChecked = !hasPasswordProtection
                config.isPasswordProtectionOn = !hasPasswordProtection
                config.passwordHash = if (hasPasswordProtection) "" else hash
                config.protectionType = type

                if (config.isPasswordProtectionOn) {
                    val confirmationTextId = if (config.protectionType == PROTECTION_FINGERPRINT)
                        R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                    ConfirmationDialog(this, "", confirmationTextId, R.string.ok, 0) { }
                }
            }
        }
    }

    private fun setupDeleteEmptyFolders() {
        settings_delete_empty_folders.isChecked = config.deleteEmptyFolders
        settings_delete_empty_folders_holder.setOnClickListener {
            settings_delete_empty_folders.toggle()
            config.deleteEmptyFolders = settings_delete_empty_folders.isChecked
        }
    }

    private fun setupAllowVideoGestures() {
        settings_allow_video_gestures.isChecked = config.allowVideoGestures
        settings_allow_video_gestures_holder.setOnClickListener {
            settings_allow_video_gestures.toggle()
            config.allowVideoGestures = settings_allow_video_gestures.isChecked
        }
    }

    private fun setupScreenRotation() {
        settings_screen_rotation.text = getScreenRotationText()
        settings_screen_rotation_holder.setOnClickListener {
            val items = arrayListOf(
                    RadioItem(ROTATE_BY_SYSTEM_SETTING, res.getString(R.string.screen_rotation_system_setting)),
                    RadioItem(ROTATE_BY_DEVICE_ROTATION, res.getString(R.string.screen_rotation_device_rotation)),
                    RadioItem(ROTATE_BY_ASPECT_RATIO, res.getString(R.string.screen_rotation_aspect_ratio)))

            RadioGroupDialog(this@SettingsActivity, items, config.screenRotation) {
                config.screenRotation = it as Int
                settings_screen_rotation.text = getScreenRotationText()
            }
        }
    }

    private fun getScreenRotationText() = getString(when (config.screenRotation) {
        ROTATE_BY_SYSTEM_SETTING -> R.string.screen_rotation_system_setting
        ROTATE_BY_DEVICE_ROTATION -> R.string.screen_rotation_device_rotation
        else -> R.string.screen_rotation_aspect_ratio
    })

    private fun setupShowExtendedDetails() {
        settings_show_extended_details.isChecked = config.showExtendedDetails
        settings_show_extended_details_holder.setOnClickListener {
            settings_show_extended_details.toggle()
            config.showExtendedDetails = settings_show_extended_details.isChecked
        }
    }
}
