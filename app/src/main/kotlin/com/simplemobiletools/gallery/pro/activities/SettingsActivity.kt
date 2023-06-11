package com.simplemobiletools.gallery.pro.activities

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.dialogs.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.dialogs.*
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.helpers.FavoritesProxy.Companion.getFavoriteFromPath
import com.simplemobiletools.gallery.pro.models.AlbumCover
import kotlinx.android.synthetic.main.activity_settings.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private val PICK_IMPORT_SOURCE_INTENT = 1
    private val SELECT_EXPORT_FAVORITES_FILE_INTENT = 2
    private val SELECT_IMPORT_FAVORITES_FILE_INTENT = 3
    private var mRecycleBinContentSize = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        updateMaterialActivityViews(settings_coordinator, settings_holder, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(settings_nested_scrollview, settings_toolbar)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(settings_toolbar, NavigationIcon.Arrow)
        setupSettingItems()
    }

    private fun setupSettingItems() {
        setupCustomizeColors()
        setupUseEnglish()
        setupLanguage()
        setupChangeDateTimeFormat()
        setupFileLoadingPriority()
        setupManageIncludedFolders()
        setupManageExcludedFolders()
        setupManageHiddenFolders()
        setupSearchAllFiles()
        setupShowHiddenItems()
        setupAutoplayVideos()
        setupRememberLastVideo()
        setupLoopVideos()
        setupOpenVideosOnSeparateScreen()
        setupMaxBrightness()
        setupCropThumbnails()
        setupDarkBackground()
        setupScrollHorizontally()
        setupScreenRotation()
        setupHideSystemUI()
        setupHiddenItemPasswordProtection()
        setupExcludedItemPasswordProtection()
        setupAppPasswordProtection()
        setupFileDeletionPasswordProtection()
        setupDeleteEmptyFolders()
        setupAllowPhotoGestures()
        setupAllowVideoGestures()
        setupAllowDownGesture()
        setupAllowRotatingWithGestures()
        setupShowNotch()
        setupBottomActions()
        setupFileThumbnailStyle()
        setupFolderThumbnailStyle()
        setupKeepLastModified()
        setupEnablePullToRefresh()
        setupAllowZoomingImages()
        setupShowHighestQuality()
        setupAllowOneToOneZoom()
        setupAllowInstantChange()
        setupShowExtendedDetails()
        setupHideExtendedDetails()
        setupManageExtendedDetails()
        setupSkipDeleteConfirmation()
        setupManageBottomActions()
        setupUseRecycleBin()
        setupShowRecycleBin()
        setupShowRecycleBinLast()
        setupEmptyRecycleBin()
        updateTextColors(settings_holder)
        setupClearCache()
        setupExportFavorites()
        setupImportFavorites()
        setupExportSettings()
        setupImportSettings()

        arrayOf(
            settings_color_customization_section_label,
            settings_general_settings_label,
            settings_videos_label,
            settings_thumbnails_label,
            settings_scrolling_label,
            settings_fullscreen_media_label,
            settings_deep_zoomable_images_label,
            settings_extended_details_label,
            settings_security_label,
            settings_file_operations_label,
            settings_bottom_actions_label,
            settings_recycle_bin_label,
            settings_migrating_label
        ).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val inputStream = contentResolver.openInputStream(resultData.data!!)
            parseFile(inputStream)
        } else if (requestCode == SELECT_EXPORT_FAVORITES_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            exportFavoritesTo(outputStream)
        } else if (requestCode == SELECT_IMPORT_FAVORITES_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val inputStream = contentResolver.openInputStream(resultData.data!!)
            importFavorites(inputStream)
        }
    }

    private fun setupCustomizeColors() {
        settings_color_customization_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        settings_use_english.isChecked = config.useEnglish
        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() {
        settings_language.text = Locale.getDefault().displayLanguage
        settings_language_holder.beVisibleIf(isTiramisuPlus())
        settings_language_holder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun setupChangeDateTimeFormat() {
        settings_change_date_time_format_holder.setOnClickListener {
            ChangeDateTimeFormatDialog(this) {}
        }
    }

    private fun setupFileLoadingPriority() {
        settings_file_loading_priority_holder.beGoneIf(isRPlus() && !isExternalStorageManager())
        settings_file_loading_priority.text = getFileLoadingPriorityText()
        settings_file_loading_priority_holder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(PRIORITY_SPEED, getString(R.string.speed)),
                RadioItem(PRIORITY_COMPROMISE, getString(R.string.compromise)),
                RadioItem(PRIORITY_VALIDITY, getString(R.string.avoid_showing_invalid_files))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fileLoadingPriority) {
                config.fileLoadingPriority = it as Int
                settings_file_loading_priority.text = getFileLoadingPriorityText()
            }
        }
    }

    private fun getFileLoadingPriorityText() = getString(
        when (config.fileLoadingPriority) {
            PRIORITY_SPEED -> R.string.speed
            PRIORITY_COMPROMISE -> R.string.compromise
            else -> R.string.avoid_showing_invalid_files
        }
    )

    private fun setupManageIncludedFolders() {
        if (isRPlus() && !isExternalStorageManager()) {
            settings_manage_included_folders.text = "${getString(R.string.manage_included_folders)} (${getString(R.string.no_permission)})"
        } else {
            settings_manage_included_folders.setText(R.string.manage_included_folders)
        }

        settings_manage_included_folders_holder.setOnClickListener {
            if (isRPlus() && !isExternalStorageManager()) {
                GrantAllFilesDialog(this)
            } else {
                startActivity(Intent(this, IncludedFoldersActivity::class.java))
            }
        }
    }

    private fun setupManageExcludedFolders() {
        settings_manage_excluded_folders_holder.setOnClickListener {
            handleExcludedFolderPasswordProtection {
                startActivity(Intent(this, ExcludedFoldersActivity::class.java))
            }
        }
    }

    private fun setupManageHiddenFolders() {
        settings_manage_hidden_folders_holder.beGoneIf(isQPlus())
        settings_manage_hidden_folders_holder.setOnClickListener {
            handleHiddenFolderPasswordProtection {
                startActivity(Intent(this, HiddenFoldersActivity::class.java))
            }
        }
    }

    private fun setupShowHiddenItems() {
        if (isRPlus() && !isExternalStorageManager()) {
            settings_show_hidden_items.text = "${getString(R.string.show_hidden_items)} (${getString(R.string.no_permission)})"
        } else {
            settings_show_hidden_items.setText(R.string.show_hidden_items)
        }

        settings_show_hidden_items.isChecked = config.showHiddenMedia
        settings_show_hidden_items_holder.setOnClickListener {
            if (isRPlus() && !isExternalStorageManager()) {
                GrantAllFilesDialog(this)
            } else if (config.showHiddenMedia) {
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

    private fun setupSearchAllFiles() {
        settings_search_all_files.isChecked = config.searchAllFilesByDefault
        settings_search_all_files_holder.setOnClickListener {
            settings_search_all_files.toggle()
            config.searchAllFilesByDefault = settings_search_all_files.isChecked
        }
    }

    private fun setupAutoplayVideos() {
        settings_autoplay_videos.isChecked = config.autoplayVideos
        settings_autoplay_videos_holder.setOnClickListener {
            settings_autoplay_videos.toggle()
            config.autoplayVideos = settings_autoplay_videos.isChecked
        }
    }

    private fun setupRememberLastVideo() {
        settings_remember_last_video_position.isChecked = config.rememberLastVideoPosition
        settings_remember_last_video_position_holder.setOnClickListener {
            settings_remember_last_video_position.toggle()
            config.rememberLastVideoPosition = settings_remember_last_video_position.isChecked
        }
    }

    private fun setupLoopVideos() {
        settings_loop_videos.isChecked = config.loopVideos
        settings_loop_videos_holder.setOnClickListener {
            settings_loop_videos.toggle()
            config.loopVideos = settings_loop_videos.isChecked
        }
    }

    private fun setupOpenVideosOnSeparateScreen() {
        settings_open_videos_on_separate_screen.isChecked = config.openVideosOnSeparateScreen
        settings_open_videos_on_separate_screen_holder.setOnClickListener {
            settings_open_videos_on_separate_screen.toggle()
            config.openVideosOnSeparateScreen = settings_open_videos_on_separate_screen.isChecked
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

    private fun setupHiddenItemPasswordProtection() {
        settings_hidden_item_password_protection_holder.beGoneIf(isRPlus() && !isExternalStorageManager())
        settings_hidden_item_password_protection.isChecked = config.isHiddenPasswordProtectionOn
        settings_hidden_item_password_protection_holder.setOnClickListener {
            val tabToShow = if (config.isHiddenPasswordProtectionOn) config.hiddenProtectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.hiddenPasswordHash, tabToShow) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isHiddenPasswordProtectionOn
                    settings_hidden_item_password_protection.isChecked = !hasPasswordProtection
                    config.isHiddenPasswordProtectionOn = !hasPasswordProtection
                    config.hiddenPasswordHash = if (hasPasswordProtection) "" else hash
                    config.hiddenProtectionType = type

                    if (config.isHiddenPasswordProtectionOn) {
                        val confirmationTextId = if (config.hiddenProtectionType == PROTECTION_FINGERPRINT)
                            R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                        ConfirmationDialog(this, "", confirmationTextId, R.string.ok, 0) { }
                    }
                }
            }
        }
    }

    private fun setupExcludedItemPasswordProtection() {
        settings_excluded_item_password_protection_holder.beGoneIf(settings_hidden_item_password_protection_holder.isVisible())
        settings_excluded_item_password_protection.isChecked = config.isExcludedPasswordProtectionOn
        settings_excluded_item_password_protection_holder.setOnClickListener {
            val tabToShow = if (config.isExcludedPasswordProtectionOn) config.excludedProtectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.excludedPasswordHash, tabToShow) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isExcludedPasswordProtectionOn
                    settings_excluded_item_password_protection.isChecked = !hasPasswordProtection
                    config.isExcludedPasswordProtectionOn = !hasPasswordProtection
                    config.excludedPasswordHash = if (hasPasswordProtection) "" else hash
                    config.excludedProtectionType = type

                    if (config.isExcludedPasswordProtectionOn) {
                        val confirmationTextId = if (config.excludedProtectionType == PROTECTION_FINGERPRINT)
                            R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                        ConfirmationDialog(this, "", confirmationTextId, R.string.ok, 0) { }
                    }
                }
            }
        }
    }

    private fun setupAppPasswordProtection() {
        settings_app_password_protection.isChecked = config.isAppPasswordProtectionOn
        settings_app_password_protection_holder.setOnClickListener {
            val tabToShow = if (config.isAppPasswordProtectionOn) config.appProtectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.appPasswordHash, tabToShow) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isAppPasswordProtectionOn
                    settings_app_password_protection.isChecked = !hasPasswordProtection
                    config.isAppPasswordProtectionOn = !hasPasswordProtection
                    config.appPasswordHash = if (hasPasswordProtection) "" else hash
                    config.appProtectionType = type

                    if (config.isAppPasswordProtectionOn) {
                        val confirmationTextId = if (config.appProtectionType == PROTECTION_FINGERPRINT)
                            R.string.fingerprint_setup_successfully else R.string.protection_setup_successfully
                        ConfirmationDialog(this, "", confirmationTextId, R.string.ok, 0) { }
                    }
                }
            }
        }
    }

    private fun setupFileDeletionPasswordProtection() {
        settings_file_deletion_password_protection.isChecked = config.isDeletePasswordProtectionOn
        settings_file_deletion_password_protection_holder.setOnClickListener {
            val tabToShow = if (config.isDeletePasswordProtectionOn) config.deleteProtectionType else SHOW_ALL_TABS
            SecurityDialog(this, config.deletePasswordHash, tabToShow) { hash, type, success ->
                if (success) {
                    val hasPasswordProtection = config.isDeletePasswordProtectionOn
                    settings_file_deletion_password_protection.isChecked = !hasPasswordProtection
                    config.isDeletePasswordProtectionOn = !hasPasswordProtection
                    config.deletePasswordHash = if (hasPasswordProtection) "" else hash
                    config.deleteProtectionType = type

                    if (config.isDeletePasswordProtectionOn) {
                        val confirmationTextId = if (config.deleteProtectionType == PROTECTION_FINGERPRINT)
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

    private fun setupAllowDownGesture() {
        settings_allow_down_gesture.isChecked = config.allowDownGesture
        settings_allow_down_gesture_holder.setOnClickListener {
            settings_allow_down_gesture.toggle()
            config.allowDownGesture = settings_allow_down_gesture.isChecked
        }
    }

    private fun setupAllowRotatingWithGestures() {
        settings_allow_rotating_with_gestures.isChecked = config.allowRotatingWithGestures
        settings_allow_rotating_with_gestures_holder.setOnClickListener {
            settings_allow_rotating_with_gestures.toggle()
            config.allowRotatingWithGestures = settings_allow_rotating_with_gestures.isChecked
        }
    }

    private fun setupShowNotch() {
        settings_show_notch_holder.beVisibleIf(isPiePlus())
        settings_show_notch.isChecked = config.showNotch
        settings_show_notch_holder.setOnClickListener {
            settings_show_notch.toggle()
            config.showNotch = settings_show_notch.isChecked
        }
    }

    private fun setupFileThumbnailStyle() {
        settings_file_thumbnail_style_holder.setOnClickListener {
            ChangeFileThumbnailStyleDialog(this)
        }
    }

    private fun setupFolderThumbnailStyle() {
        settings_folder_thumbnail_style.text = getFolderStyleText()
        settings_folder_thumbnail_style_holder.setOnClickListener {
            ChangeFolderThumbnailStyleDialog(this) {
                settings_folder_thumbnail_style.text = getFolderStyleText()
            }
        }
    }

    private fun getFolderStyleText() = getString(
        when (config.folderStyle) {
            FOLDER_STYLE_SQUARE -> R.string.square
            else -> R.string.rounded_corners
        }
    )

    private fun setupKeepLastModified() {
        settings_keep_last_modified.isChecked = config.keepLastModified
        settings_keep_last_modified_holder.setOnClickListener {
            handleMediaManagementPrompt {
                settings_keep_last_modified.toggle()
                config.keepLastModified = settings_keep_last_modified.isChecked
            }
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
        settings_allow_zooming_images.isChecked = config.allowZoomingImages
        updateDeepZoomToggleButtons()
        settings_allow_zooming_images_holder.setOnClickListener {
            settings_allow_zooming_images.toggle()
            config.allowZoomingImages = settings_allow_zooming_images.isChecked
            updateDeepZoomToggleButtons()
        }
    }

    private fun updateDeepZoomToggleButtons() {
        settings_allow_rotating_with_gestures_holder.beVisibleIf(config.allowZoomingImages)
        settings_show_highest_quality_holder.beVisibleIf(config.allowZoomingImages)
        settings_allow_one_to_one_zoom_holder.beVisibleIf(config.allowZoomingImages)
    }

    private fun setupShowHighestQuality() {
        settings_show_highest_quality.isChecked = config.showHighestQuality
        settings_show_highest_quality_holder.setOnClickListener {
            settings_show_highest_quality.toggle()
            config.showHighestQuality = settings_show_highest_quality.isChecked
        }
    }

    private fun setupAllowOneToOneZoom() {
        settings_allow_one_to_one_zoom.isChecked = config.allowOneToOneZoom
        settings_allow_one_to_one_zoom_holder.setOnClickListener {
            settings_allow_one_to_one_zoom.toggle()
            config.allowOneToOneZoom = settings_allow_one_to_one_zoom.isChecked
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
        updateExtendedDetailsButtons()
        settings_show_extended_details_holder.setOnClickListener {
            settings_show_extended_details.toggle()
            config.showExtendedDetails = settings_show_extended_details.isChecked
            updateExtendedDetailsButtons()
        }
    }

    private fun setupHideExtendedDetails() {
        settings_hide_extended_details.isChecked = config.hideExtendedDetails
        settings_hide_extended_details_holder.setOnClickListener {
            settings_hide_extended_details.toggle()
            config.hideExtendedDetails = settings_hide_extended_details.isChecked
        }
    }

    private fun setupManageExtendedDetails() {
        settings_manage_extended_details_holder.setOnClickListener {
            ManageExtendedDetailsDialog(this) {
                if (config.extendedDetails == 0) {
                    settings_show_extended_details_holder.callOnClick()
                }
            }
        }
    }

    private fun updateExtendedDetailsButtons() {
        settings_manage_extended_details_holder.beVisibleIf(config.showExtendedDetails)
        settings_hide_extended_details_holder.beVisibleIf(config.showExtendedDetails)
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
                RadioItem(ROTATE_BY_SYSTEM_SETTING, getString(R.string.screen_rotation_system_setting)),
                RadioItem(ROTATE_BY_DEVICE_ROTATION, getString(R.string.screen_rotation_device_rotation)),
                RadioItem(ROTATE_BY_ASPECT_RATIO, getString(R.string.screen_rotation_aspect_ratio))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.screenRotation) {
                config.screenRotation = it as Int
                settings_screen_rotation.text = getScreenRotationText()
            }
        }
    }

    private fun getScreenRotationText() = getString(
        when (config.screenRotation) {
            ROTATE_BY_SYSTEM_SETTING -> R.string.screen_rotation_system_setting
            ROTATE_BY_DEVICE_ROTATION -> R.string.screen_rotation_device_rotation
            else -> R.string.screen_rotation_aspect_ratio
        }
    )

    private fun setupBottomActions() {
        settings_bottom_actions_checkbox.isChecked = config.bottomActions
        settings_manage_bottom_actions_holder.beVisibleIf(config.bottomActions)
        settings_bottom_actions_checkbox_holder.setOnClickListener {
            settings_bottom_actions_checkbox.toggle()
            config.bottomActions = settings_bottom_actions_checkbox.isChecked
            settings_manage_bottom_actions_holder.beVisibleIf(config.bottomActions)
        }
    }

    private fun setupManageBottomActions() {
        settings_manage_bottom_actions_holder.setOnClickListener {
            ManageBottomActionsDialog(this) {
                if (config.visibleBottomActions == 0) {
                    settings_bottom_actions_checkbox_holder.callOnClick()
                    config.bottomActions = false
                    config.visibleBottomActions = DEFAULT_BOTTOM_ACTIONS
                }
            }
        }
    }

    private fun setupUseRecycleBin() {
        updateRecycleBinButtons()
        settings_use_recycle_bin.isChecked = config.useRecycleBin
        settings_use_recycle_bin_holder.setOnClickListener {
            settings_use_recycle_bin.toggle()
            config.useRecycleBin = settings_use_recycle_bin.isChecked
            updateRecycleBinButtons()
        }
    }

    private fun setupShowRecycleBin() {
        settings_show_recycle_bin.isChecked = config.showRecycleBinAtFolders
        settings_show_recycle_bin_holder.setOnClickListener {
            settings_show_recycle_bin.toggle()
            config.showRecycleBinAtFolders = settings_show_recycle_bin.isChecked
            updateRecycleBinButtons()
        }
    }

    private fun setupShowRecycleBinLast() {
        settings_show_recycle_bin_last.isChecked = config.showRecycleBinLast
        settings_show_recycle_bin_last_holder.setOnClickListener {
            settings_show_recycle_bin_last.toggle()
            config.showRecycleBinLast = settings_show_recycle_bin_last.isChecked
            if (config.showRecycleBinLast) {
                config.removePinnedFolders(setOf(RECYCLE_BIN))
            }
        }
    }

    private fun updateRecycleBinButtons() {
        settings_show_recycle_bin_last_holder.beVisibleIf(config.useRecycleBin && config.showRecycleBinAtFolders)
        settings_empty_recycle_bin_holder.beVisibleIf(config.useRecycleBin)
        settings_show_recycle_bin_holder.beVisibleIf(config.useRecycleBin)
    }

    private fun setupEmptyRecycleBin() {
        ensureBackgroundThread {
            try {
                mRecycleBinContentSize = mediaDB.getDeletedMedia().sumByLong { medium ->
                    val size = medium.size
                    if (size == 0L) {
                        val path = medium.path.removePrefix(RECYCLE_BIN).prependIndent(recycleBinPath)
                        File(path).length()
                    } else {
                        size
                    }
                }
            } catch (ignored: Exception) {
            }

            runOnUiThread {
                settings_empty_recycle_bin_size.text = mRecycleBinContentSize.formatSize()
            }
        }

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

    private fun setupClearCache() {
        ensureBackgroundThread {
            val size = cacheDir.getProperSize(true).formatSize()
            runOnUiThread {
                settings_clear_cache_size.text = size
            }
        }

        settings_clear_cache_holder.setOnClickListener {
            ensureBackgroundThread {
                cacheDir.deleteRecursively()
                runOnUiThread {
                    settings_clear_cache_size.text = cacheDir.getProperSize(true).formatSize()
                }
            }
        }
    }

    private fun setupExportFavorites() {
        settings_export_favorites_holder.setOnClickListener {
            if (isQPlus()) {
                ExportFavoritesDialog(this, getExportFavoritesFilename(), true) { path, filename ->
                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TITLE, filename)
                        addCategory(Intent.CATEGORY_OPENABLE)

                        try {
                            startActivityForResult(this, SELECT_EXPORT_FAVORITES_FILE_INTENT)
                        } catch (e: ActivityNotFoundException) {
                            toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                        } catch (e: Exception) {
                            showErrorToast(e)
                        }
                    }
                }
            } else {
                handlePermission(PERMISSION_WRITE_STORAGE) {
                    if (it) {
                        ExportFavoritesDialog(this, getExportFavoritesFilename(), false) { path, filename ->
                            val file = File(path)
                            getFileOutputStream(file.toFileDirItem(this), true) {
                                exportFavoritesTo(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun exportFavoritesTo(outputStream: OutputStream?) {
        if (outputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        ensureBackgroundThread {
            val favoritePaths = favoritesDB.getValidFavoritePaths()
            if (favoritePaths.isNotEmpty()) {
                outputStream.bufferedWriter().use { out ->
                    favoritePaths.forEach { path ->
                        out.writeLn(path)
                    }
                }

                toast(R.string.exporting_successful)
            } else {
                toast(R.string.no_items_found)
            }
        }
    }

    private fun getExportFavoritesFilename(): String {
        val appName = baseConfig.appId.removeSuffix(".debug").removeSuffix(".pro").removePrefix("com.simplemobiletools.")
        return "$appName-favorites_${getCurrentFormattedDateTime()}"
    }

    private fun setupImportFavorites() {
        settings_import_favorites_holder.setOnClickListener {
            if (isQPlus()) {
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"
                    startActivityForResult(this, SELECT_IMPORT_FAVORITES_FILE_INTENT)
                }
            } else {
                handlePermission(PERMISSION_READ_STORAGE) {
                    if (it) {
                        FilePickerDialog(this) {
                            ensureBackgroundThread {
                                importFavorites(File(it).inputStream())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun importFavorites(inputStream: InputStream?) {
        if (inputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        ensureBackgroundThread {
            var importedItems = 0
            inputStream.bufferedReader().use {
                while (true) {
                    try {
                        val line = it.readLine() ?: break
                        if (getDoesFilePathExist(line)) {
                            val favorite = getFavoriteFromPath(line)
                            favoritesDB.insert(favorite)
                            importedItems++
                        }
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
                }
            }

            toast(if (importedItems > 0) R.string.importing_successful else R.string.no_entries_for_importing)
        }
    }

    private fun setupExportSettings() {
        settings_export_holder.setOnClickListener {
            val configItems = LinkedHashMap<String, Any>().apply {
                put(IS_USING_SHARED_THEME, config.isUsingSharedTheme)
                put(TEXT_COLOR, config.textColor)
                put(BACKGROUND_COLOR, config.backgroundColor)
                put(PRIMARY_COLOR, config.primaryColor)
                put(ACCENT_COLOR, config.accentColor)
                put(APP_ICON_COLOR, config.appIconColor)
                put(USE_ENGLISH, config.useEnglish)
                put(WAS_USE_ENGLISH_TOGGLED, config.wasUseEnglishToggled)
                put(WIDGET_BG_COLOR, config.widgetBgColor)
                put(WIDGET_TEXT_COLOR, config.widgetTextColor)
                put(DATE_FORMAT, config.dateFormat)
                put(USE_24_HOUR_FORMAT, config.use24HourFormat)
                put(INCLUDED_FOLDERS, TextUtils.join(",", config.includedFolders))
                put(EXCLUDED_FOLDERS, TextUtils.join(",", config.excludedFolders))
                put(SHOW_HIDDEN_MEDIA, config.showHiddenMedia)
                put(FILE_LOADING_PRIORITY, config.fileLoadingPriority)
                put(AUTOPLAY_VIDEOS, config.autoplayVideos)
                put(REMEMBER_LAST_VIDEO_POSITION, config.rememberLastVideoPosition)
                put(LOOP_VIDEOS, config.loopVideos)
                put(OPEN_VIDEOS_ON_SEPARATE_SCREEN, config.openVideosOnSeparateScreen)
                put(ALLOW_VIDEO_GESTURES, config.allowVideoGestures)
                put(ANIMATE_GIFS, config.animateGifs)
                put(CROP_THUMBNAILS, config.cropThumbnails)
                put(SHOW_THUMBNAIL_VIDEO_DURATION, config.showThumbnailVideoDuration)
                put(SHOW_THUMBNAIL_FILE_TYPES, config.showThumbnailFileTypes)
                put(MARK_FAVORITE_ITEMS, config.markFavoriteItems)
                put(SCROLL_HORIZONTALLY, config.scrollHorizontally)
                put(ENABLE_PULL_TO_REFRESH, config.enablePullToRefresh)
                put(MAX_BRIGHTNESS, config.maxBrightness)
                put(BLACK_BACKGROUND, config.blackBackground)
                put(HIDE_SYSTEM_UI, config.hideSystemUI)
                put(ALLOW_INSTANT_CHANGE, config.allowInstantChange)
                put(ALLOW_PHOTO_GESTURES, config.allowPhotoGestures)
                put(ALLOW_DOWN_GESTURE, config.allowDownGesture)
                put(ALLOW_ROTATING_WITH_GESTURES, config.allowRotatingWithGestures)
                put(SHOW_NOTCH, config.showNotch)
                put(SCREEN_ROTATION, config.screenRotation)
                put(ALLOW_ZOOMING_IMAGES, config.allowZoomingImages)
                put(SHOW_HIGHEST_QUALITY, config.showHighestQuality)
                put(ALLOW_ONE_TO_ONE_ZOOM, config.allowOneToOneZoom)
                put(SHOW_EXTENDED_DETAILS, config.showExtendedDetails)
                put(HIDE_EXTENDED_DETAILS, config.hideExtendedDetails)
                put(EXTENDED_DETAILS, config.extendedDetails)
                put(DELETE_EMPTY_FOLDERS, config.deleteEmptyFolders)
                put(KEEP_LAST_MODIFIED, config.keepLastModified)
                put(SKIP_DELETE_CONFIRMATION, config.skipDeleteConfirmation)
                put(BOTTOM_ACTIONS, config.bottomActions)
                put(VISIBLE_BOTTOM_ACTIONS, config.visibleBottomActions)
                put(USE_RECYCLE_BIN, config.useRecycleBin)
                put(SHOW_RECYCLE_BIN_AT_FOLDERS, config.showRecycleBinAtFolders)
                put(SHOW_RECYCLE_BIN_LAST, config.showRecycleBinLast)
                put(SORT_ORDER, config.sorting)
                put(DIRECTORY_SORT_ORDER, config.directorySorting)
                put(GROUP_BY, config.groupBy)
                put(GROUP_DIRECT_SUBFOLDERS, config.groupDirectSubfolders)
                put(PINNED_FOLDERS, TextUtils.join(",", config.pinnedFolders))
                put(DISPLAY_FILE_NAMES, config.displayFileNames)
                put(FILTER_MEDIA, config.filterMedia)
                put(DIR_COLUMN_CNT, config.dirColumnCnt)
                put(MEDIA_COLUMN_CNT, config.mediaColumnCnt)
                put(SHOW_ALL, config.showAll)
                put(SHOW_WIDGET_FOLDER_NAME, config.showWidgetFolderName)
                put(VIEW_TYPE_FILES, config.viewTypeFiles)
                put(VIEW_TYPE_FOLDERS, config.viewTypeFolders)
                put(SLIDESHOW_INTERVAL, config.slideshowInterval)
                put(SLIDESHOW_INCLUDE_VIDEOS, config.slideshowIncludeVideos)
                put(SLIDESHOW_INCLUDE_GIFS, config.slideshowIncludeGIFs)
                put(SLIDESHOW_RANDOM_ORDER, config.slideshowRandomOrder)
                put(SLIDESHOW_MOVE_BACKWARDS, config.slideshowMoveBackwards)
                put(SLIDESHOW_LOOP, config.loopSlideshow)
                put(LAST_EDITOR_CROP_ASPECT_RATIO, config.lastEditorCropAspectRatio)
                put(LAST_EDITOR_CROP_OTHER_ASPECT_RATIO_X, config.lastEditorCropOtherAspectRatioX)
                put(LAST_EDITOR_CROP_OTHER_ASPECT_RATIO_Y, config.lastEditorCropOtherAspectRatioY)
                put(LAST_CONFLICT_RESOLUTION, config.lastConflictResolution)
                put(LAST_CONFLICT_APPLY_TO_ALL, config.lastConflictApplyToAll)
                put(EDITOR_BRUSH_COLOR, config.editorBrushColor)
                put(EDITOR_BRUSH_HARDNESS, config.editorBrushHardness)
                put(EDITOR_BRUSH_SIZE, config.editorBrushSize)
                put(ALBUM_COVERS, config.albumCovers)
                put(FOLDER_THUMBNAIL_STYLE, config.folderStyle)
                put(FOLDER_MEDIA_COUNT, config.showFolderMediaCount)
                put(LIMIT_FOLDER_TITLE, config.limitFolderTitle)
                put(THUMBNAIL_SPACING, config.thumbnailSpacing)
                put(FILE_ROUNDED_CORNERS, config.fileRoundedCorners)
                put(SEARCH_ALL_FILES_BY_DEFAULT, config.searchAllFilesByDefault)
            }

            exportSettings(configItems)
        }
    }

    private fun setupImportSettings() {
        settings_import_holder.setOnClickListener {
            if (isQPlus()) {
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"
                    startActivityForResult(this, PICK_IMPORT_SOURCE_INTENT)
                }
            } else {
                handlePermission(PERMISSION_READ_STORAGE) {
                    if (it) {
                        FilePickerDialog(this) {
                            ensureBackgroundThread {
                                parseFile(File(it).inputStream())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseFile(inputStream: InputStream?) {
        if (inputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        var importedItems = 0
        val configValues = LinkedHashMap<String, Any>()
        inputStream.bufferedReader().use {
            while (true) {
                try {
                    val line = it.readLine() ?: break
                    val split = line.split("=".toRegex(), 2)
                    if (split.size == 2) {
                        configValues[split[0]] = split[1]
                    }
                    importedItems++
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }

        for ((key, value) in configValues) {
            when (key) {
                IS_USING_SHARED_THEME -> config.isUsingSharedTheme = value.toBoolean()
                TEXT_COLOR -> config.textColor = value.toInt()
                BACKGROUND_COLOR -> config.backgroundColor = value.toInt()
                PRIMARY_COLOR -> config.primaryColor = value.toInt()
                ACCENT_COLOR -> config.accentColor = value.toInt()
                APP_ICON_COLOR -> {
                    if (getAppIconColors().contains(value.toInt())) {
                        config.appIconColor = value.toInt()
                        checkAppIconColor()
                    }
                }
                USE_ENGLISH -> config.useEnglish = value.toBoolean()
                WAS_USE_ENGLISH_TOGGLED -> config.wasUseEnglishToggled = value.toBoolean()
                WIDGET_BG_COLOR -> config.widgetBgColor = value.toInt()
                WIDGET_TEXT_COLOR -> config.widgetTextColor = value.toInt()
                DATE_FORMAT -> config.dateFormat = value.toString()
                USE_24_HOUR_FORMAT -> config.use24HourFormat = value.toBoolean()
                INCLUDED_FOLDERS -> config.addIncludedFolders(value.toStringSet())
                EXCLUDED_FOLDERS -> config.addExcludedFolders(value.toStringSet())
                SHOW_HIDDEN_MEDIA -> config.showHiddenMedia = value.toBoolean()
                FILE_LOADING_PRIORITY -> config.fileLoadingPriority = value.toInt()
                AUTOPLAY_VIDEOS -> config.autoplayVideos = value.toBoolean()
                REMEMBER_LAST_VIDEO_POSITION -> config.rememberLastVideoPosition = value.toBoolean()
                LOOP_VIDEOS -> config.loopVideos = value.toBoolean()
                OPEN_VIDEOS_ON_SEPARATE_SCREEN -> config.openVideosOnSeparateScreen = value.toBoolean()
                ALLOW_VIDEO_GESTURES -> config.allowVideoGestures = value.toBoolean()
                ANIMATE_GIFS -> config.animateGifs = value.toBoolean()
                CROP_THUMBNAILS -> config.cropThumbnails = value.toBoolean()
                SHOW_THUMBNAIL_VIDEO_DURATION -> config.showThumbnailVideoDuration = value.toBoolean()
                SHOW_THUMBNAIL_FILE_TYPES -> config.showThumbnailFileTypes = value.toBoolean()
                MARK_FAVORITE_ITEMS -> config.markFavoriteItems = value.toBoolean()
                SCROLL_HORIZONTALLY -> config.scrollHorizontally = value.toBoolean()
                ENABLE_PULL_TO_REFRESH -> config.enablePullToRefresh = value.toBoolean()
                MAX_BRIGHTNESS -> config.maxBrightness = value.toBoolean()
                BLACK_BACKGROUND -> config.blackBackground = value.toBoolean()
                HIDE_SYSTEM_UI -> config.hideSystemUI = value.toBoolean()
                ALLOW_INSTANT_CHANGE -> config.allowInstantChange = value.toBoolean()
                ALLOW_PHOTO_GESTURES -> config.allowPhotoGestures = value.toBoolean()
                ALLOW_DOWN_GESTURE -> config.allowDownGesture = value.toBoolean()
                ALLOW_ROTATING_WITH_GESTURES -> config.allowRotatingWithGestures = value.toBoolean()
                SHOW_NOTCH -> config.showNotch = value.toBoolean()
                SCREEN_ROTATION -> config.screenRotation = value.toInt()
                ALLOW_ZOOMING_IMAGES -> config.allowZoomingImages = value.toBoolean()
                SHOW_HIGHEST_QUALITY -> config.showHighestQuality = value.toBoolean()
                ALLOW_ONE_TO_ONE_ZOOM -> config.allowOneToOneZoom = value.toBoolean()
                SHOW_EXTENDED_DETAILS -> config.showExtendedDetails = value.toBoolean()
                HIDE_EXTENDED_DETAILS -> config.hideExtendedDetails = value.toBoolean()
                EXTENDED_DETAILS -> config.extendedDetails = value.toInt()
                DELETE_EMPTY_FOLDERS -> config.deleteEmptyFolders = value.toBoolean()
                KEEP_LAST_MODIFIED -> config.keepLastModified = value.toBoolean()
                SKIP_DELETE_CONFIRMATION -> config.skipDeleteConfirmation = value.toBoolean()
                BOTTOM_ACTIONS -> config.bottomActions = value.toBoolean()
                VISIBLE_BOTTOM_ACTIONS -> config.visibleBottomActions = value.toInt()
                USE_RECYCLE_BIN -> config.useRecycleBin = value.toBoolean()
                SHOW_RECYCLE_BIN_AT_FOLDERS -> config.showRecycleBinAtFolders = value.toBoolean()
                SHOW_RECYCLE_BIN_LAST -> config.showRecycleBinLast = value.toBoolean()
                SORT_ORDER -> config.sorting = value.toInt()
                DIRECTORY_SORT_ORDER -> config.directorySorting = value.toInt()
                GROUP_BY -> config.groupBy = value.toInt()
                GROUP_DIRECT_SUBFOLDERS -> config.groupDirectSubfolders = value.toBoolean()
                PINNED_FOLDERS -> config.addPinnedFolders(value.toStringSet())
                DISPLAY_FILE_NAMES -> config.displayFileNames = value.toBoolean()
                FILTER_MEDIA -> config.filterMedia = value.toInt()
                DIR_COLUMN_CNT -> config.dirColumnCnt = value.toInt()
                MEDIA_COLUMN_CNT -> config.mediaColumnCnt = value.toInt()
                SHOW_ALL -> config.showAll = value.toBoolean()
                SHOW_WIDGET_FOLDER_NAME -> config.showWidgetFolderName = value.toBoolean()
                VIEW_TYPE_FILES -> config.viewTypeFiles = value.toInt()
                VIEW_TYPE_FOLDERS -> config.viewTypeFolders = value.toInt()
                SLIDESHOW_INTERVAL -> config.slideshowInterval = value.toInt()
                SLIDESHOW_INCLUDE_VIDEOS -> config.slideshowIncludeVideos = value.toBoolean()
                SLIDESHOW_INCLUDE_GIFS -> config.slideshowIncludeGIFs = value.toBoolean()
                SLIDESHOW_RANDOM_ORDER -> config.slideshowRandomOrder = value.toBoolean()
                SLIDESHOW_MOVE_BACKWARDS -> config.slideshowMoveBackwards = value.toBoolean()
                SLIDESHOW_LOOP -> config.loopSlideshow = value.toBoolean()
                LAST_EDITOR_CROP_ASPECT_RATIO -> config.lastEditorCropAspectRatio = value.toInt()
                LAST_EDITOR_CROP_OTHER_ASPECT_RATIO_X -> config.lastEditorCropOtherAspectRatioX = value.toString().toFloat()
                LAST_EDITOR_CROP_OTHER_ASPECT_RATIO_Y -> config.lastEditorCropOtherAspectRatioY = value.toString().toFloat()
                LAST_CONFLICT_RESOLUTION -> config.lastConflictResolution = value.toInt()
                LAST_CONFLICT_APPLY_TO_ALL -> config.lastConflictApplyToAll = value.toBoolean()
                EDITOR_BRUSH_COLOR -> config.editorBrushColor = value.toInt()
                EDITOR_BRUSH_HARDNESS -> config.editorBrushHardness = value.toString().toFloat()
                EDITOR_BRUSH_SIZE -> config.editorBrushSize = value.toString().toFloat()
                FOLDER_THUMBNAIL_STYLE -> config.folderStyle = value.toInt()
                FOLDER_MEDIA_COUNT -> config.showFolderMediaCount = value.toInt()
                LIMIT_FOLDER_TITLE -> config.limitFolderTitle = value.toBoolean()
                THUMBNAIL_SPACING -> config.thumbnailSpacing = value.toInt()
                FILE_ROUNDED_CORNERS -> config.fileRoundedCorners = value.toBoolean()
                SEARCH_ALL_FILES_BY_DEFAULT -> config.searchAllFilesByDefault = value.toBoolean()
                ALBUM_COVERS -> {
                    val existingCovers = config.parseAlbumCovers()
                    val existingCoverPaths = existingCovers.map { it.path }.toMutableList() as ArrayList<String>

                    val listType = object : TypeToken<List<AlbumCover>>() {}.type
                    val covers = Gson().fromJson<ArrayList<AlbumCover>>(value.toString(), listType) ?: ArrayList(1)
                    covers.filter { !existingCoverPaths.contains(it.path) && getDoesFilePathExist(it.tmb) }.forEach {
                        existingCovers.add(it)
                    }

                    config.albumCovers = Gson().toJson(existingCovers)
                }
            }
        }

        toast(if (configValues.size > 0) R.string.settings_imported_successfully else R.string.no_entries_for_importing)
        runOnUiThread {
            setupSettingItems()
        }
    }
}
