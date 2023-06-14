package com.simplemobiletools.commons.helpers

import android.content.Context
import android.text.format.DateFormat
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.getInternalStoragePath
import com.simplemobiletools.commons.extensions.getSDCardPath
import com.simplemobiletools.commons.extensions.getSharedPrefs
import java.text.SimpleDateFormat
import java.util.*

open class BaseConfig(val context: Context) {
    protected val prefs = context.getSharedPrefs()

    companion object {
        fun newInstance(context: Context) = BaseConfig(context)
    }

    var appRunCount: Int
        get() = prefs.getInt(APP_RUN_COUNT, 0)
        set(appRunCount) = prefs.edit().putInt(APP_RUN_COUNT, appRunCount).apply()

    var lastVersion: Int
        get() = prefs.getInt(LAST_VERSION, 0)
        set(lastVersion) = prefs.edit().putInt(LAST_VERSION, lastVersion).apply()

    var primaryAndroidDataTreeUri: String
        get() = prefs.getString(PRIMARY_ANDROID_DATA_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(PRIMARY_ANDROID_DATA_TREE_URI, uri).apply()

    var sdAndroidDataTreeUri: String
        get() = prefs.getString(SD_ANDROID_DATA_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(SD_ANDROID_DATA_TREE_URI, uri).apply()

    var otgAndroidDataTreeUri: String
        get() = prefs.getString(OTG_ANDROID_DATA_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(OTG_ANDROID_DATA_TREE_URI, uri).apply()

    var primaryAndroidObbTreeUri: String
        get() = prefs.getString(PRIMARY_ANDROID_OBB_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(PRIMARY_ANDROID_OBB_TREE_URI, uri).apply()

    var sdAndroidObbTreeUri: String
        get() = prefs.getString(SD_ANDROID_OBB_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(SD_ANDROID_OBB_TREE_URI, uri).apply()

    var otgAndroidObbTreeUri: String
        get() = prefs.getString(OTG_ANDROID_OBB_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(OTG_ANDROID_OBB_TREE_URI, uri).apply()

    var sdTreeUri: String
        get() = prefs.getString(SD_TREE_URI, "")!!
        set(uri) = prefs.edit().putString(SD_TREE_URI, uri).apply()

    var OTGTreeUri: String
        get() = prefs.getString(OTG_TREE_URI, "")!!
        set(OTGTreeUri) = prefs.edit().putString(OTG_TREE_URI, OTGTreeUri).apply()

    var OTGPartition: String
        get() = prefs.getString(OTG_PARTITION, "")!!
        set(OTGPartition) = prefs.edit().putString(OTG_PARTITION, OTGPartition).apply()

    var OTGPath: String
        get() = prefs.getString(OTG_REAL_PATH, "")!!
        set(OTGPath) = prefs.edit().putString(OTG_REAL_PATH, OTGPath).apply()

    var sdCardPath: String
        get() = prefs.getString(SD_CARD_PATH, getDefaultSDCardPath())!!
        set(sdCardPath) = prefs.edit().putString(SD_CARD_PATH, sdCardPath).apply()

    private fun getDefaultSDCardPath() = if (prefs.contains(SD_CARD_PATH)) "" else context.getSDCardPath()

    var internalStoragePath: String
        get() = prefs.getString(INTERNAL_STORAGE_PATH, getDefaultInternalPath())!!
        set(internalStoragePath) = prefs.edit().putString(INTERNAL_STORAGE_PATH, internalStoragePath).apply()

    private fun getDefaultInternalPath() = if (prefs.contains(INTERNAL_STORAGE_PATH)) "" else context.getInternalStoragePath()

    var textColor: Int
        get() = prefs.getInt(TEXT_COLOR, context.resources.getColor(R.color.default_text_color))
        set(textColor) = prefs.edit().putInt(TEXT_COLOR, textColor).apply()

    var backgroundColor: Int
        get() = prefs.getInt(BACKGROUND_COLOR, context.resources.getColor(R.color.default_background_color))
        set(backgroundColor) = prefs.edit().putInt(BACKGROUND_COLOR, backgroundColor).apply()

    var primaryColor: Int
        get() = prefs.getInt(PRIMARY_COLOR, context.resources.getColor(R.color.default_primary_color))
        set(primaryColor) = prefs.edit().putInt(PRIMARY_COLOR, primaryColor).apply()

    var accentColor: Int
        get() = prefs.getInt(ACCENT_COLOR, context.resources.getColor(R.color.default_accent_color))
        set(accentColor) = prefs.edit().putInt(ACCENT_COLOR, accentColor).apply()

    var lastHandledShortcutColor: Int
        get() = prefs.getInt(LAST_HANDLED_SHORTCUT_COLOR, 1)
        set(lastHandledShortcutColor) = prefs.edit().putInt(LAST_HANDLED_SHORTCUT_COLOR, lastHandledShortcutColor).apply()

    var appIconColor: Int
        get() = prefs.getInt(APP_ICON_COLOR, context.resources.getColor(R.color.default_app_icon_color))
        set(appIconColor) {
            isUsingModifiedAppIcon = appIconColor != context.resources.getColor(R.color.color_primary)
            prefs.edit().putInt(APP_ICON_COLOR, appIconColor).apply()
        }

    var lastIconColor: Int
        get() = prefs.getInt(LAST_ICON_COLOR, context.resources.getColor(R.color.color_primary))
        set(lastIconColor) = prefs.edit().putInt(LAST_ICON_COLOR, lastIconColor).apply()

    var customTextColor: Int
        get() = prefs.getInt(CUSTOM_TEXT_COLOR, textColor)
        set(customTextColor) = prefs.edit().putInt(CUSTOM_TEXT_COLOR, customTextColor).apply()

    var customBackgroundColor: Int
        get() = prefs.getInt(CUSTOM_BACKGROUND_COLOR, backgroundColor)
        set(customBackgroundColor) = prefs.edit().putInt(CUSTOM_BACKGROUND_COLOR, customBackgroundColor).apply()

    var customPrimaryColor: Int
        get() = prefs.getInt(CUSTOM_PRIMARY_COLOR, primaryColor)
        set(customPrimaryColor) = prefs.edit().putInt(CUSTOM_PRIMARY_COLOR, customPrimaryColor).apply()

    var customAccentColor: Int
        get() = prefs.getInt(CUSTOM_ACCENT_COLOR, accentColor)
        set(customAccentColor) = prefs.edit().putInt(CUSTOM_ACCENT_COLOR, customAccentColor).apply()

    var customAppIconColor: Int
        get() = prefs.getInt(CUSTOM_APP_ICON_COLOR, appIconColor)
        set(customAppIconColor) = prefs.edit().putInt(CUSTOM_APP_ICON_COLOR, customAppIconColor).apply()

    var widgetBgColor: Int
        get() = prefs.getInt(WIDGET_BG_COLOR, context.resources.getColor(R.color.default_widget_bg_color))
        set(widgetBgColor) = prefs.edit().putInt(WIDGET_BG_COLOR, widgetBgColor).apply()

    var widgetTextColor: Int
        get() = prefs.getInt(WIDGET_TEXT_COLOR, context.resources.getColor(R.color.default_widget_text_color))
        set(widgetTextColor) = prefs.edit().putInt(WIDGET_TEXT_COLOR, widgetTextColor).apply()

    // hidden folder visibility protection
    var isHiddenPasswordProtectionOn: Boolean
        get() = prefs.getBoolean(PASSWORD_PROTECTION, false)
        set(isHiddenPasswordProtectionOn) = prefs.edit().putBoolean(PASSWORD_PROTECTION, isHiddenPasswordProtectionOn).apply()

    var hiddenPasswordHash: String
        get() = prefs.getString(PASSWORD_HASH, "")!!
        set(hiddenPasswordHash) = prefs.edit().putString(PASSWORD_HASH, hiddenPasswordHash).apply()

    var hiddenProtectionType: Int
        get() = prefs.getInt(PROTECTION_TYPE, PROTECTION_PATTERN)
        set(hiddenProtectionType) = prefs.edit().putInt(PROTECTION_TYPE, hiddenProtectionType).apply()

    // whole app launch protection
    var isAppPasswordProtectionOn: Boolean
        get() = prefs.getBoolean(APP_PASSWORD_PROTECTION, false)
        set(isAppPasswordProtectionOn) = prefs.edit().putBoolean(APP_PASSWORD_PROTECTION, isAppPasswordProtectionOn).apply()

    var appPasswordHash: String
        get() = prefs.getString(APP_PASSWORD_HASH, "")!!
        set(appPasswordHash) = prefs.edit().putString(APP_PASSWORD_HASH, appPasswordHash).apply()

    var appProtectionType: Int
        get() = prefs.getInt(APP_PROTECTION_TYPE, PROTECTION_PATTERN)
        set(appProtectionType) = prefs.edit().putInt(APP_PROTECTION_TYPE, appProtectionType).apply()

    // file delete and move protection
    var isDeletePasswordProtectionOn: Boolean
        get() = prefs.getBoolean(DELETE_PASSWORD_PROTECTION, false)
        set(isDeletePasswordProtectionOn) = prefs.edit().putBoolean(DELETE_PASSWORD_PROTECTION, isDeletePasswordProtectionOn).apply()

    var deletePasswordHash: String
        get() = prefs.getString(DELETE_PASSWORD_HASH, "")!!
        set(deletePasswordHash) = prefs.edit().putString(DELETE_PASSWORD_HASH, deletePasswordHash).apply()

    var deleteProtectionType: Int
        get() = prefs.getInt(DELETE_PROTECTION_TYPE, PROTECTION_PATTERN)
        set(deleteProtectionType) = prefs.edit().putInt(DELETE_PROTECTION_TYPE, deleteProtectionType).apply()

    // folder locking
    fun addFolderProtection(path: String, hash: String, type: Int) {
        prefs.edit()
            .putString("$PROTECTED_FOLDER_HASH$path", hash)
            .putInt("$PROTECTED_FOLDER_TYPE$path", type)
            .apply()
    }

    fun removeFolderProtection(path: String) {
        prefs.edit()
            .remove("$PROTECTED_FOLDER_HASH$path")
            .remove("$PROTECTED_FOLDER_TYPE$path")
            .apply()
    }

    fun isFolderProtected(path: String) = getFolderProtectionType(path) != PROTECTION_NONE

    fun getFolderProtectionHash(path: String) = prefs.getString("$PROTECTED_FOLDER_HASH$path", "") ?: ""

    fun getFolderProtectionType(path: String) = prefs.getInt("$PROTECTED_FOLDER_TYPE$path", PROTECTION_NONE)

    var keepLastModified: Boolean
        get() = prefs.getBoolean(KEEP_LAST_MODIFIED, true)
        set(keepLastModified) = prefs.edit().putBoolean(KEEP_LAST_MODIFIED, keepLastModified).apply()

    var useEnglish: Boolean
        get() = prefs.getBoolean(USE_ENGLISH, false)
        set(useEnglish) {
            wasUseEnglishToggled = true
            prefs.edit().putBoolean(USE_ENGLISH, useEnglish).commit()
        }

    var wasUseEnglishToggled: Boolean
        get() = prefs.getBoolean(WAS_USE_ENGLISH_TOGGLED, false)
        set(wasUseEnglishToggled) = prefs.edit().putBoolean(WAS_USE_ENGLISH_TOGGLED, wasUseEnglishToggled).apply()

    var wasSharedThemeEverActivated: Boolean
        get() = prefs.getBoolean(WAS_SHARED_THEME_EVER_ACTIVATED, false)
        set(wasSharedThemeEverActivated) = prefs.edit().putBoolean(WAS_SHARED_THEME_EVER_ACTIVATED, wasSharedThemeEverActivated).apply()

    var isUsingSharedTheme: Boolean
        get() = prefs.getBoolean(IS_USING_SHARED_THEME, false)
        set(isUsingSharedTheme) = prefs.edit().putBoolean(IS_USING_SHARED_THEME, isUsingSharedTheme).apply()

    // used by Simple Thank You, stop using shared Shared Theme if it has been changed in it
    var shouldUseSharedTheme: Boolean
        get() = prefs.getBoolean(SHOULD_USE_SHARED_THEME, false)
        set(shouldUseSharedTheme) = prefs.edit().putBoolean(SHOULD_USE_SHARED_THEME, shouldUseSharedTheme).apply()

    var isUsingAutoTheme: Boolean
        get() = prefs.getBoolean(IS_USING_AUTO_THEME, false)
        set(isUsingAutoTheme) = prefs.edit().putBoolean(IS_USING_AUTO_THEME, isUsingAutoTheme).apply()

    var isUsingSystemTheme: Boolean
        get() = prefs.getBoolean(IS_USING_SYSTEM_THEME, isSPlus())
        set(isUsingSystemTheme) = prefs.edit().putBoolean(IS_USING_SYSTEM_THEME, isUsingSystemTheme).apply()

    var wasCustomThemeSwitchDescriptionShown: Boolean
        get() = prefs.getBoolean(WAS_CUSTOM_THEME_SWITCH_DESCRIPTION_SHOWN, false)
        set(wasCustomThemeSwitchDescriptionShown) = prefs.edit().putBoolean(WAS_CUSTOM_THEME_SWITCH_DESCRIPTION_SHOWN, wasCustomThemeSwitchDescriptionShown)
            .apply()

    var wasSharedThemeForced: Boolean
        get() = prefs.getBoolean(WAS_SHARED_THEME_FORCED, false)
        set(wasSharedThemeForced) = prefs.edit().putBoolean(WAS_SHARED_THEME_FORCED, wasSharedThemeForced).apply()

    var showInfoBubble: Boolean
        get() = prefs.getBoolean(SHOW_INFO_BUBBLE, true)
        set(showInfoBubble) = prefs.edit().putBoolean(SHOW_INFO_BUBBLE, showInfoBubble).apply()

    var lastConflictApplyToAll: Boolean
        get() = prefs.getBoolean(LAST_CONFLICT_APPLY_TO_ALL, true)
        set(lastConflictApplyToAll) = prefs.edit().putBoolean(LAST_CONFLICT_APPLY_TO_ALL, lastConflictApplyToAll).apply()

    var lastConflictResolution: Int
        get() = prefs.getInt(LAST_CONFLICT_RESOLUTION, CONFLICT_SKIP)
        set(lastConflictResolution) = prefs.edit().putInt(LAST_CONFLICT_RESOLUTION, lastConflictResolution).apply()

    var sorting: Int
        get() = prefs.getInt(SORT_ORDER, context.resources.getInteger(R.integer.default_sorting))
        set(sorting) = prefs.edit().putInt(SORT_ORDER, sorting).apply()

    fun saveCustomSorting(path: String, value: Int) {
        if (path.isEmpty()) {
            sorting = value
        } else {
            prefs.edit().putInt(SORT_FOLDER_PREFIX + path.toLowerCase(), value).apply()
        }
    }

    fun getFolderSorting(path: String) = prefs.getInt(SORT_FOLDER_PREFIX + path.toLowerCase(), sorting)

    fun removeCustomSorting(path: String) {
        prefs.edit().remove(SORT_FOLDER_PREFIX + path.toLowerCase()).apply()
    }

    fun hasCustomSorting(path: String) = prefs.contains(SORT_FOLDER_PREFIX + path.toLowerCase())

    var hadThankYouInstalled: Boolean
        get() = prefs.getBoolean(HAD_THANK_YOU_INSTALLED, false)
        set(hadThankYouInstalled) = prefs.edit().putBoolean(HAD_THANK_YOU_INSTALLED, hadThankYouInstalled).apply()

    var skipDeleteConfirmation: Boolean
        get() = prefs.getBoolean(SKIP_DELETE_CONFIRMATION, false)
        set(skipDeleteConfirmation) = prefs.edit().putBoolean(SKIP_DELETE_CONFIRMATION, skipDeleteConfirmation).apply()

    var enablePullToRefresh: Boolean
        get() = prefs.getBoolean(ENABLE_PULL_TO_REFRESH, true)
        set(enablePullToRefresh) = prefs.edit().putBoolean(ENABLE_PULL_TO_REFRESH, enablePullToRefresh).apply()

    var scrollHorizontally: Boolean
        get() = prefs.getBoolean(SCROLL_HORIZONTALLY, false)
        set(scrollHorizontally) = prefs.edit().putBoolean(SCROLL_HORIZONTALLY, scrollHorizontally).apply()

    var preventPhoneFromSleeping: Boolean
        get() = prefs.getBoolean(PREVENT_PHONE_FROM_SLEEPING, true)
        set(preventPhoneFromSleeping) = prefs.edit().putBoolean(PREVENT_PHONE_FROM_SLEEPING, preventPhoneFromSleeping).apply()

    var lastUsedViewPagerPage: Int
        get() = prefs.getInt(LAST_USED_VIEW_PAGER_PAGE, context.resources.getInteger(R.integer.default_viewpager_page))
        set(lastUsedViewPagerPage) = prefs.edit().putInt(LAST_USED_VIEW_PAGER_PAGE, lastUsedViewPagerPage).apply()

    var use24HourFormat: Boolean
        get() = prefs.getBoolean(USE_24_HOUR_FORMAT, DateFormat.is24HourFormat(context))
        set(use24HourFormat) = prefs.edit().putBoolean(USE_24_HOUR_FORMAT, use24HourFormat).apply()

    var isSundayFirst: Boolean
        get() {
            val isSundayFirst = Calendar.getInstance(Locale.getDefault()).firstDayOfWeek == Calendar.SUNDAY
            return prefs.getBoolean(SUNDAY_FIRST, isSundayFirst)
        }
        set(sundayFirst) = prefs.edit().putBoolean(SUNDAY_FIRST, sundayFirst).apply()

    var wasAlarmWarningShown: Boolean
        get() = prefs.getBoolean(WAS_ALARM_WARNING_SHOWN, false)
        set(wasAlarmWarningShown) = prefs.edit().putBoolean(WAS_ALARM_WARNING_SHOWN, wasAlarmWarningShown).apply()

    var wasReminderWarningShown: Boolean
        get() = prefs.getBoolean(WAS_REMINDER_WARNING_SHOWN, false)
        set(wasReminderWarningShown) = prefs.edit().putBoolean(WAS_REMINDER_WARNING_SHOWN, wasReminderWarningShown).apply()

    var useSameSnooze: Boolean
        get() = prefs.getBoolean(USE_SAME_SNOOZE, true)
        set(useSameSnooze) = prefs.edit().putBoolean(USE_SAME_SNOOZE, useSameSnooze).apply()

    var snoozeTime: Int
        get() = prefs.getInt(SNOOZE_TIME, 10)
        set(snoozeDelay) = prefs.edit().putInt(SNOOZE_TIME, snoozeDelay).apply()

    var vibrateOnButtonPress: Boolean
        get() = prefs.getBoolean(VIBRATE_ON_BUTTON_PRESS, context.resources.getBoolean(R.bool.default_vibrate_on_press))
        set(vibrateOnButton) = prefs.edit().putBoolean(VIBRATE_ON_BUTTON_PRESS, vibrateOnButton).apply()

    var yourAlarmSounds: String
        get() = prefs.getString(YOUR_ALARM_SOUNDS, "")!!
        set(yourAlarmSounds) = prefs.edit().putString(YOUR_ALARM_SOUNDS, yourAlarmSounds).apply()

    var isUsingModifiedAppIcon: Boolean
        get() = prefs.getBoolean(IS_USING_MODIFIED_APP_ICON, false)
        set(isUsingModifiedAppIcon) = prefs.edit().putBoolean(IS_USING_MODIFIED_APP_ICON, isUsingModifiedAppIcon).apply()

    var appId: String
        get() = prefs.getString(APP_ID, "")!!
        set(appId) = prefs.edit().putString(APP_ID, appId).apply()

    var initialWidgetHeight: Int
        get() = prefs.getInt(INITIAL_WIDGET_HEIGHT, 0)
        set(initialWidgetHeight) = prefs.edit().putInt(INITIAL_WIDGET_HEIGHT, initialWidgetHeight).apply()

    var widgetIdToMeasure: Int
        get() = prefs.getInt(WIDGET_ID_TO_MEASURE, 0)
        set(widgetIdToMeasure) = prefs.edit().putInt(WIDGET_ID_TO_MEASURE, widgetIdToMeasure).apply()

    var wasOrangeIconChecked: Boolean
        get() = prefs.getBoolean(WAS_ORANGE_ICON_CHECKED, false)
        set(wasOrangeIconChecked) = prefs.edit().putBoolean(WAS_ORANGE_ICON_CHECKED, wasOrangeIconChecked).apply()

    var wasAppOnSDShown: Boolean
        get() = prefs.getBoolean(WAS_APP_ON_SD_SHOWN, false)
        set(wasAppOnSDShown) = prefs.edit().putBoolean(WAS_APP_ON_SD_SHOWN, wasAppOnSDShown).apply()

    var wasBeforeAskingShown: Boolean
        get() = prefs.getBoolean(WAS_BEFORE_ASKING_SHOWN, false)
        set(wasBeforeAskingShown) = prefs.edit().putBoolean(WAS_BEFORE_ASKING_SHOWN, wasBeforeAskingShown).apply()

    var wasBeforeRateShown: Boolean
        get() = prefs.getBoolean(WAS_BEFORE_RATE_SHOWN, false)
        set(wasBeforeRateShown) = prefs.edit().putBoolean(WAS_BEFORE_RATE_SHOWN, wasBeforeRateShown).apply()

    var wasInitialUpgradeToProShown: Boolean
        get() = prefs.getBoolean(WAS_INITIAL_UPGRADE_TO_PRO_SHOWN, false)
        set(wasInitialUpgradeToProShown) = prefs.edit().putBoolean(WAS_INITIAL_UPGRADE_TO_PRO_SHOWN, wasInitialUpgradeToProShown).apply()

    var wasAppIconCustomizationWarningShown: Boolean
        get() = prefs.getBoolean(WAS_APP_ICON_CUSTOMIZATION_WARNING_SHOWN, false)
        set(wasAppIconCustomizationWarningShown) = prefs.edit().putBoolean(WAS_APP_ICON_CUSTOMIZATION_WARNING_SHOWN, wasAppIconCustomizationWarningShown)
            .apply()

    var appSideloadingStatus: Int
        get() = prefs.getInt(APP_SIDELOADING_STATUS, SIDELOADING_UNCHECKED)
        set(appSideloadingStatus) = prefs.edit().putInt(APP_SIDELOADING_STATUS, appSideloadingStatus).apply()

    var dateFormat: String
        get() = prefs.getString(DATE_FORMAT, getDefaultDateFormat())!!
        set(dateFormat) = prefs.edit().putString(DATE_FORMAT, dateFormat).apply()

    private fun getDefaultDateFormat(): String {
        val format = DateFormat.getDateFormat(context)
        val pattern = (format as SimpleDateFormat).toLocalizedPattern()
        return when (pattern.toLowerCase().replace(" ", "")) {
            "d.M.y" -> DATE_FORMAT_ONE
            "dd/mm/y" -> DATE_FORMAT_TWO
            "mm/dd/y" -> DATE_FORMAT_THREE
            "y-mm-dd" -> DATE_FORMAT_FOUR
            "dmmmmy" -> DATE_FORMAT_FIVE
            "mmmmdy" -> DATE_FORMAT_SIX
            "mm-dd-y" -> DATE_FORMAT_SEVEN
            "dd-mm-y" -> DATE_FORMAT_EIGHT
            else -> DATE_FORMAT_ONE
        }
    }

    var wasOTGHandled: Boolean
        get() = prefs.getBoolean(WAS_OTG_HANDLED, false)
        set(wasOTGHandled) = prefs.edit().putBoolean(WAS_OTG_HANDLED, wasOTGHandled).apply()

    var wasUpgradedFromFreeShown: Boolean
        get() = prefs.getBoolean(WAS_UPGRADED_FROM_FREE_SHOWN, false)
        set(wasUpgradedFromFreeShown) = prefs.edit().putBoolean(WAS_UPGRADED_FROM_FREE_SHOWN, wasUpgradedFromFreeShown).apply()

    var wasRateUsPromptShown: Boolean
        get() = prefs.getBoolean(WAS_RATE_US_PROMPT_SHOWN, false)
        set(wasRateUsPromptShown) = prefs.edit().putBoolean(WAS_RATE_US_PROMPT_SHOWN, wasRateUsPromptShown).apply()

    var wasAppRated: Boolean
        get() = prefs.getBoolean(WAS_APP_RATED, false)
        set(wasAppRated) = prefs.edit().putBoolean(WAS_APP_RATED, wasAppRated).apply()

    var wasSortingByNumericValueAdded: Boolean
        get() = prefs.getBoolean(WAS_SORTING_BY_NUMERIC_VALUE_ADDED, false)
        set(wasSortingByNumericValueAdded) = prefs.edit().putBoolean(WAS_SORTING_BY_NUMERIC_VALUE_ADDED, wasSortingByNumericValueAdded).apply()

    var wasFolderLockingNoticeShown: Boolean
        get() = prefs.getBoolean(WAS_FOLDER_LOCKING_NOTICE_SHOWN, false)
        set(wasFolderLockingNoticeShown) = prefs.edit().putBoolean(WAS_FOLDER_LOCKING_NOTICE_SHOWN, wasFolderLockingNoticeShown).apply()

    var lastRenameUsed: Int
        get() = prefs.getInt(LAST_RENAME_USED, RENAME_SIMPLE)
        set(lastRenameUsed) = prefs.edit().putInt(LAST_RENAME_USED, lastRenameUsed).apply()

    var lastRenamePatternUsed: String
        get() = prefs.getString(LAST_RENAME_PATTERN_USED, "")!!
        set(lastRenamePatternUsed) = prefs.edit().putString(LAST_RENAME_PATTERN_USED, lastRenamePatternUsed).apply()

    var lastExportedSettingsFolder: String
        get() = prefs.getString(LAST_EXPORTED_SETTINGS_FOLDER, "")!!
        set(lastExportedSettingsFolder) = prefs.edit().putString(LAST_EXPORTED_SETTINGS_FOLDER, lastExportedSettingsFolder).apply()

    var lastBlockedNumbersExportPath: String
        get() = prefs.getString(LAST_BLOCKED_NUMBERS_EXPORT_PATH, "")!!
        set(lastBlockedNumbersExportPath) = prefs.edit().putString(LAST_BLOCKED_NUMBERS_EXPORT_PATH, lastBlockedNumbersExportPath).apply()

    var blockUnknownNumbers: Boolean
        get() = prefs.getBoolean(BLOCK_UNKNOWN_NUMBERS, false)
        set(blockUnknownNumbers) = prefs.edit().putBoolean(BLOCK_UNKNOWN_NUMBERS, blockUnknownNumbers).apply()

    var fontSize: Int
        get() = prefs.getInt(FONT_SIZE, context.resources.getInteger(R.integer.default_font_size))
        set(size) = prefs.edit().putInt(FONT_SIZE, size).apply()

    // notify the users about new SMS Messenger and Voice Recorder released
    var wasMessengerRecorderShown: Boolean
        get() = prefs.getBoolean(WAS_MESSENGER_RECORDER_SHOWN, false)
        set(wasMessengerRecorderShown) = prefs.edit().putBoolean(WAS_MESSENGER_RECORDER_SHOWN, wasMessengerRecorderShown).apply()

    var defaultTab: Int
        get() = prefs.getInt(DEFAULT_TAB, TAB_LAST_USED)
        set(defaultTab) = prefs.edit().putInt(DEFAULT_TAB, defaultTab).apply()

    var startNameWithSurname: Boolean
        get() = prefs.getBoolean(START_NAME_WITH_SURNAME, false)
        set(startNameWithSurname) = prefs.edit().putBoolean(START_NAME_WITH_SURNAME, startNameWithSurname).apply()

    var favorites: MutableSet<String>
        get() = prefs.getStringSet(FAVORITES, HashSet())!!
        set(favorites) = prefs.edit().remove(FAVORITES).putStringSet(FAVORITES, favorites).apply()

    var showCallConfirmation: Boolean
        get() = prefs.getBoolean(SHOW_CALL_CONFIRMATION, false)
        set(showCallConfirmation) = prefs.edit().putBoolean(SHOW_CALL_CONFIRMATION, showCallConfirmation).apply()

    // color picker last used colors
    var colorPickerRecentColors: LinkedList<Int>
        get(): LinkedList<Int> {
            val defaultList = arrayListOf(
                context.resources.getColor(R.color.md_red_700),
                context.resources.getColor(R.color.md_blue_700),
                context.resources.getColor(R.color.md_green_700),
                context.resources.getColor(R.color.md_yellow_700),
                context.resources.getColor(R.color.md_orange_700)
            )
            return LinkedList(prefs.getString(COLOR_PICKER_RECENT_COLORS, null)?.lines()?.map { it.toInt() } ?: defaultList)
        }
        set(recentColors) = prefs.edit().putString(COLOR_PICKER_RECENT_COLORS, recentColors.joinToString(separator = "\n")).apply()

    var ignoredContactSources: HashSet<String>
        get() = prefs.getStringSet(IGNORED_CONTACT_SOURCES, hashSetOf(".")) as HashSet
        set(ignoreContactSources) = prefs.edit().remove(IGNORED_CONTACT_SOURCES).putStringSet(IGNORED_CONTACT_SOURCES, ignoreContactSources).apply()

    var showContactThumbnails: Boolean
        get() = prefs.getBoolean(SHOW_CONTACT_THUMBNAILS, true)
        set(showContactThumbnails) = prefs.edit().putBoolean(SHOW_CONTACT_THUMBNAILS, showContactThumbnails).apply()

    var showPhoneNumbers: Boolean
        get() = prefs.getBoolean(SHOW_PHONE_NUMBERS, false)
        set(showPhoneNumbers) = prefs.edit().putBoolean(SHOW_PHONE_NUMBERS, showPhoneNumbers).apply()

    var showOnlyContactsWithNumbers: Boolean
        get() = prefs.getBoolean(SHOW_ONLY_CONTACTS_WITH_NUMBERS, false)
        set(showOnlyContactsWithNumbers) = prefs.edit().putBoolean(SHOW_ONLY_CONTACTS_WITH_NUMBERS, showOnlyContactsWithNumbers).apply()

    var lastUsedContactSource: String
        get() = prefs.getString(LAST_USED_CONTACT_SOURCE, "")!!
        set(lastUsedContactSource) = prefs.edit().putString(LAST_USED_CONTACT_SOURCE, lastUsedContactSource).apply()

    var onContactClick: Int
        get() = prefs.getInt(ON_CONTACT_CLICK, ON_CLICK_VIEW_CONTACT)
        set(onContactClick) = prefs.edit().putInt(ON_CONTACT_CLICK, onContactClick).apply()

    var showContactFields: Int
        get() = prefs.getInt(
            SHOW_CONTACT_FIELDS,
            SHOW_FIRST_NAME_FIELD or SHOW_SURNAME_FIELD or SHOW_PHONE_NUMBERS_FIELD or SHOW_EMAILS_FIELD or
                SHOW_ADDRESSES_FIELD or SHOW_EVENTS_FIELD or SHOW_NOTES_FIELD or SHOW_GROUPS_FIELD or SHOW_CONTACT_SOURCE_FIELD
        )
        set(showContactFields) = prefs.edit().putInt(SHOW_CONTACT_FIELDS, showContactFields).apply()
    var showDialpadButton: Boolean
        get() = prefs.getBoolean(SHOW_DIALPAD_BUTTON, true)
        set(showDialpadButton) = prefs.edit().putBoolean(SHOW_DIALPAD_BUTTON, showDialpadButton).apply()

    var wasLocalAccountInitialized: Boolean
        get() = prefs.getBoolean(WAS_LOCAL_ACCOUNT_INITIALIZED, false)
        set(wasLocalAccountInitialized) = prefs.edit().putBoolean(WAS_LOCAL_ACCOUNT_INITIALIZED, wasLocalAccountInitialized).apply()

    var lastExportPath: String
        get() = prefs.getString(LAST_EXPORT_PATH, "")!!
        set(lastExportPath) = prefs.edit().putString(LAST_EXPORT_PATH, lastExportPath).apply()

    var speedDial: String
        get() = prefs.getString(SPEED_DIAL, "")!!
        set(speedDial) = prefs.edit().putString(SPEED_DIAL, speedDial).apply()

    var showPrivateContacts: Boolean
        get() = prefs.getBoolean(SHOW_PRIVATE_CONTACTS, true)
        set(showPrivateContacts) = prefs.edit().putBoolean(SHOW_PRIVATE_CONTACTS, showPrivateContacts).apply()

    var mergeDuplicateContacts: Boolean
        get() = prefs.getBoolean(MERGE_DUPLICATE_CONTACTS, true)
        set(mergeDuplicateContacts) = prefs.edit().putBoolean(MERGE_DUPLICATE_CONTACTS, mergeDuplicateContacts).apply()

    var favoritesContactsOrder: String
        get() = prefs.getString(FAVORITES_CONTACTS_ORDER, "")!!
        set(order) = prefs.edit().putString(FAVORITES_CONTACTS_ORDER, order).apply()

    var isCustomOrderSelected: Boolean
        get() = prefs.getBoolean(FAVORITES_CUSTOM_ORDER_SELECTED, false)
        set(selected) = prefs.edit().putBoolean(FAVORITES_CUSTOM_ORDER_SELECTED, selected).apply()
}
