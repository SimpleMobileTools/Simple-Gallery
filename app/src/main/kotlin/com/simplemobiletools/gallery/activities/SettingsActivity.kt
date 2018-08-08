package com.simplemobiletools.gallery.activities

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.dialogs.SecurityDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PROTECTION_FINGERPRINT
import com.simplemobiletools.commons.helpers.SHOW_ALL_TABS
import com.simplemobiletools.commons.helpers.sumByLong
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.dialogs.ManageBottomActionsDialog
import com.simplemobiletools.gallery.dialogs.ManageExtendedDetailsDialog
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.emptyTheRecycleBin
import com.simplemobiletools.gallery.extensions.galleryDB
import com.simplemobiletools.gallery.extensions.showRecycleBinEmptyingDialog
import com.simplemobiletools.gallery.helpers.DEFAULT_BOTTOM_ACTIONS
import com.simplemobiletools.gallery.helpers.ROTATE_BY_ASPECT_RATIO
import com.simplemobiletools.gallery.helpers.ROTATE_BY_DEVICE_ROTATION
import com.simplemobiletools.gallery.helpers.ROTATE_BY_SYSTEM_SETTING
import kotlinx.android.synthetic.main.activity_settings.*
import java.util.*

class SettingsActivity : SimpleActivity() {
    lateinit var res: Resources
    private var mRecycleBinContentSize = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        res = resources
    }

    override fun onResume() {
        super.onResume()

        setupPurchaseThankYou()
        setupCustomizeColors()
        setupUseEnglish()
        setupAvoidWhatsNew()
        setupManageIncludedFolders()
        setupManageExcludedFolders()
        setupManageHiddenFolders()
        setupShowHiddenItems()
        setupDoExtraCheck()
        setupAutoplayVideos()
        setupLoopVideos()
        setupAnimateGifs()
        setupMaxBrightness()
        setupCropThumbnails()
        setupDarkBackground()
        setupScrollHorizontally()
        setupScreenRotation()
        setupHideSystemUI()
        setupPasswordProtection()
        setupAppPasswordProtection()
        setupDeleteEmptyFolders()
        setupAllowPhotoGestures()
        setupAllowVideoGestures()
        setupBottomActions()
        setupShowMediaCount()
        setupKeepLastModified()
        setupShowInfoBubble()
        setupEnablePullToRefresh()
        setupAllowZoomingImages()
        setupOneFingerZoom()
        setupAllowInstantChange()
        setupShowExtendedDetails()
        setupHideExtendedDetails()
        setupManageExtendedDetails()
        setupSkipDeleteConfirmation()
        setupManageBottomActions()
        setupUseRecycleBin()
        setupShowRecycleBin()
        setupEmptyRecycleBin()
        updateTextColors(settings_holder)
        setupSectionColors()
    }

    private fun setupSectionColors() {
        val adjustedPrimaryColor = getAdjustedPrimaryColor()
        arrayListOf(visibility_label, videos_label, thumbnails_label, scrolling_label, fullscreen_media_label, security_label,
                file_operations_label, extended_details_label, bottom_actions_label, recycle_bin_label).forEach {
            it.setTextColor(adjustedPrimaryColor)
        }
    }

    private fun setupPurchaseThankYou() {
        settings_purchase_thank_you_holder.beVisibleIf(config.appRunCount > 10 && !isThankYouInstalled())
        settings_purchase_thank_you_holder.setOnClickListener {
            launchPurchaseThankYouIntent()
        }
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf(config.wasUseEnglishToggled || Locale.getDefault().language != "en")
        settings_use_english.isChecked = config.useEnglish
        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            System.exit(0)
        }
    }

    private fun setupAvoidWhatsNew() {
        settings_avoid_whats_new.isChecked = config.avoidWhatsNew
        settings_avoid_whats_new_holder.setOnClickListener {
            settings_avoid_whats_new.toggle()
            config.avoidWhatsNew = settings_avoid_whats_new.isChecked
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

    private fun setupManageHiddenFolders() {
        settings_manage_hidden_folders_holder.setOnClickListener {
            handleHiddenFolderPasswordProtection {
                startActivity(Intent(this, HiddenFoldersActivity::class.java))
            }
        }
    }

    private fun setupShowHiddenItems() {
        settings_show_hidden_items.isChecked = config.showHiddenMedia
        settings_show_hidden_items_holder.setOnClickListener {
            if (config.showHiddenMedia) {
                toggleHiddenItems()
            } else {
                handleHiddenFolderPasswordProtection {
                    toggleHiddenItems()
                }
            }
        }
    }

    private fun toggleHiddenItems() {
        settings_show_hidden_items.toggle()
        config.showHiddenMedia = settings_show_hidden_items.isChecked
    }

    private fun setupDoExtraCheck() {
        settings_do_extra_check.isChecked = config.doExtraCheck
        settings_do_extra_check_holder.setOnClickListener {
            settings_do_extra_check.toggle()
            config.doExtraCheck = settings_do_extra_check.isChecked
        }
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
        settings_black_background.isChecked = config.blackBackground
        settings_black_background_holder.setOnClickListener {
            settings_black_background.toggle()
            config.blackBackground = settings_black_background.isChecked
        }
    }

    private fun setupScrollHorizontally() {
        settings_scroll_horizontally.isChecked = config.scrollHorizontally
        settings_scroll_horizontally_holder.setOnClickListener {
            settings_scroll_horizontally.toggle()
            config.scrollHorizontally = settings_scroll_horizontally.isChecked

            if (config.scrollHorizontally) {
                config.enablePullToRefresh = false
                settings_enable_pull_to_refresh.isChecked = false
            }
        }
    }

    private fun setupHideSystemUI() {
        settings_hide_system_ui.isChecked = config.hideSystemUI
        settings_hide_system_ui_holder.setOnClickListener {
            settings_hide_system_ui.toggle()
            config.hideSystemUI = settings_hide_system_ui.isChecked
        }
    }

    private fun setupPasswordProtection() {
        settings_password_protection.isChecked = config.isPasswordProtectionOn
        settings_password_protection_holder.setOnClickListener {
            val tabToShow = if (config.isPasswordProtectionOn) config.protectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.passwordHash, tabToShow) { hash, type, success ->
                if (success) {
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
    }

    private fun setupAppPasswordProtection() {
        settings_app_password_protection.isChecked = config.appPasswordProtectionOn
        settings_app_password_protection_holder.setOnClickListener {
            val tabToShow = if (config.appPasswordProtectionOn) config.appProtectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.appPasswordHash, tabToShow) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.appPasswordProtectionOn
                    settings_app_password_protection.isChecked = !hasPasswordProtection
                    config.appPasswordProtectionOn = !hasPasswordProtection
                    config.appPasswordHash = if (hasPasswordProtection) "" else hash
                    config.appProtectionType = type

                    if (config.appPasswordProtectionOn) {
                        val confirmationTextId = if (config.appProtectionType == PROTECTION_FINGERPRINT)
                            R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                        ConfirmationDialog(this, "", confirmationTextId, R.string.ok, 0) { }
                    }
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

    private fun setupAllowPhotoGestures() {
        settings_allow_photo_gestures.isChecked = config.allowPhotoGestures
        settings_allow_photo_gestures_holder.setOnClickListener {
            settings_allow_photo_gestures.toggle()
            config.allowPhotoGestures = settings_allow_photo_gestures.isChecked
        }
    }

    private fun setupAllowVideoGestures() {
        settings_allow_video_gestures.isChecked = config.allowVideoGestures
        settings_allow_video_gestures_holder.setOnClickListener {
            settings_allow_video_gestures.toggle()
            config.allowVideoGestures = settings_allow_video_gestures.isChecked
        }
    }

    private fun setupShowMediaCount() {
        settings_show_media_count.isChecked = config.showMediaCount
        settings_show_media_count_holder.setOnClickListener {
            settings_show_media_count.toggle()
            config.showMediaCount = settings_show_media_count.isChecked
        }
    }

    private fun setupKeepLastModified() {
        settings_keep_last_modified.isChecked = config.keepLastModified
        settings_keep_last_modified_holder.setOnClickListener {
            settings_keep_last_modified.toggle()
            config.keepLastModified = settings_keep_last_modified.isChecked
        }
    }

    private fun setupShowInfoBubble() {
        settings_show_info_bubble.isChecked = config.showInfoBubble
        settings_show_info_bubble_holder.setOnClickListener {
            settings_show_info_bubble.toggle()
            config.showInfoBubble = settings_show_info_bubble.isChecked
        }
    }

    private fun setupEnablePullToRefresh() {
        settings_enable_pull_to_refresh.isChecked = config.enablePullToRefresh
        settings_enable_pull_to_refresh_holder.setOnClickListener {
            settings_enable_pull_to_refresh.toggle()
            config.enablePullToRefresh = settings_enable_pull_to_refresh.isChecked
        }
    }

    private fun setupAllowZoomingImages() {
        settings_one_finger_zoom_holder.beVisibleIf(config.allowZoomingImages)
        settings_allow_zooming_images.isChecked = config.allowZoomingImages
        settings_allow_zooming_images_holder.setOnClickListener {
            settings_allow_zooming_images.toggle()
            config.allowZoomingImages = settings_allow_zooming_images.isChecked
            settings_one_finger_zoom_holder.beVisibleIf(config.allowZoomingImages)
        }
    }

    private fun setupOneFingerZoom() {
        settings_one_finger_zoom.isChecked = config.oneFingerZoom
        settings_one_finger_zoom_holder.setOnClickListener {
            settings_one_finger_zoom.toggle()
            config.oneFingerZoom = settings_one_finger_zoom.isChecked
        }
    }

    private fun setupAllowInstantChange() {
        settings_allow_instant_change.isChecked = config.allowInstantChange
        settings_allow_instant_change_holder.setOnClickListener {
            settings_allow_instant_change.toggle()
            config.allowInstantChange = settings_allow_instant_change.isChecked
        }
    }

    private fun setupShowExtendedDetails() {
        settings_show_extended_details.isChecked = config.showExtendedDetails
        settings_show_extended_details_holder.setOnClickListener {
            settings_show_extended_details.toggle()
            config.showExtendedDetails = settings_show_extended_details.isChecked
            settings_manage_extended_details_holder.beVisibleIf(config.showExtendedDetails)
            settings_hide_extended_details_holder.beVisibleIf(config.showExtendedDetails)
        }
    }

    private fun setupHideExtendedDetails() {
        settings_hide_extended_details_holder.beVisibleIf(config.showExtendedDetails)
        settings_hide_extended_details.isChecked = config.hideExtendedDetails
        settings_hide_extended_details_holder.setOnClickListener {
            settings_hide_extended_details.toggle()
            config.hideExtendedDetails = settings_hide_extended_details.isChecked
        }
    }

    private fun setupManageExtendedDetails() {
        settings_manage_extended_details_holder.beVisibleIf(config.showExtendedDetails)
        settings_manage_extended_details_holder.setOnClickListener {
            ManageExtendedDetailsDialog(this) {
                if (config.extendedDetails == 0) {
                    settings_show_extended_details_holder.callOnClick()
                }
            }
        }
    }

    private fun setupSkipDeleteConfirmation() {
        settings_skip_delete_confirmation.isChecked = config.skipDeleteConfirmation
        settings_skip_delete_confirmation_holder.setOnClickListener {
            settings_skip_delete_confirmation.toggle()
            config.skipDeleteConfirmation = settings_skip_delete_confirmation.isChecked
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

    private fun setupBottomActions() {
        settings_bottom_actions.isChecked = config.bottomActions
        settings_bottom_actions_holder.setOnClickListener {
            settings_bottom_actions.toggle()
            config.bottomActions = settings_bottom_actions.isChecked
            settings_manage_bottom_actions_holder.beVisibleIf(config.bottomActions)
        }
    }

    private fun setupManageBottomActions() {
        settings_manage_bottom_actions_holder.beVisibleIf(config.bottomActions)
        settings_manage_bottom_actions_holder.setOnClickListener {
            ManageBottomActionsDialog(this) {
                if (config.visibleBottomActions == 0) {
                    settings_bottom_actions_holder.callOnClick()
                    config.bottomActions = false
                    config.visibleBottomActions = DEFAULT_BOTTOM_ACTIONS
                }
            }
        }
    }

    private fun setupUseRecycleBin() {
        settings_empty_recycle_bin_holder.beVisibleIf(config.useRecycleBin)
        settings_show_recycle_bin_holder.beVisibleIf(config.useRecycleBin)
        settings_use_recycle_bin.isChecked = config.useRecycleBin
        settings_use_recycle_bin_holder.setOnClickListener {
            settings_use_recycle_bin.toggle()
            config.useRecycleBin = settings_use_recycle_bin.isChecked
            settings_empty_recycle_bin_holder.beVisibleIf(config.useRecycleBin)
        }
    }

    private fun setupShowRecycleBin() {
        settings_show_recycle_bin.isChecked = config.showRecycleBinAtFolders
        settings_show_recycle_bin_holder.setOnClickListener {
            settings_show_recycle_bin.toggle()
            config.showRecycleBinAtFolders = settings_show_recycle_bin.isChecked
        }
    }

    private fun setupEmptyRecycleBin() {
        Thread {
            mRecycleBinContentSize = galleryDB.MediumDao().getDeletedMedia().sumByLong { it.size }
            runOnUiThread {
                settings_empty_recycle_bin_size.text = mRecycleBinContentSize.formatSize()
            }
        }.start()

        settings_empty_recycle_bin_holder.setOnClickListener {
            if (mRecycleBinContentSize == 0L) {
                toast(R.string.recycle_bin_empty)
            } else {
                showRecycleBinEmptyingDialog {
                    emptyTheRecycleBin()
                    mRecycleBinContentSize = 0L
                    settings_empty_recycle_bin_size.text = 0L.formatSize()
                }
            }
        }
    }
}
