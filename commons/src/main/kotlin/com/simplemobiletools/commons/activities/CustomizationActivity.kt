package com.simplemobiletools.commons.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.dialogs.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.MyTheme
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.SharedTheme
import com.simplemobiletools.commons.views.MyTextView
import kotlinx.android.synthetic.main.activity_customization.*

class CustomizationActivity : BaseSimpleActivity() {
    private val THEME_LIGHT = 0
    private val THEME_DARK = 1
    private val THEME_SOLARIZED = 2
    private val THEME_DARK_RED = 3
    private val THEME_BLACK_WHITE = 4
    private val THEME_CUSTOM = 5
    private val THEME_SHARED = 6
    private val THEME_WHITE = 7
    private val THEME_AUTO = 8
    private val THEME_SYSTEM = 9    // Material You

    private var curTextColor = 0
    private var curBackgroundColor = 0
    private var curPrimaryColor = 0
    private var curAccentColor = 0
    private var curAppIconColor = 0
    private var curSelectedThemeId = 0
    private var originalAppIconColor = 0
    private var lastSavePromptTS = 0L
    private var hasUnsavedChanges = false
    private var isThankYou = false      // show "Apply colors to all Simple apps" in Simple Thank You itself even with "Hide Google relations" enabled
    private var predefinedThemes = LinkedHashMap<Int, MyTheme>()
    private var curPrimaryLineColorPicker: LineColorPickerDialog? = null
    private var storedSharedTheme: SharedTheme? = null

    override fun getAppIconIDs() = intent.getIntegerArrayListExtra(APP_ICON_IDS) ?: ArrayList()

    override fun getAppLauncherName() = intent.getStringExtra(APP_LAUNCHER_NAME) ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customization)

        setupOptionsMenu()
        refreshMenuItems()

        updateMaterialActivityViews(customization_coordinator, customization_holder, useTransparentNavigation = true, useTopSearchMenu = false)

        isThankYou = packageName.removeSuffix(".debug") == "com.simplemobiletools.thankyou"
        initColorVariables()

        if (isThankYouInstalled()) {
            val cursorLoader = getMyContentProviderCursorLoader()
            ensureBackgroundThread {
                try {
                    storedSharedTheme = getSharedThemeSync(cursorLoader)
                    if (storedSharedTheme == null) {
                        baseConfig.isUsingSharedTheme = false
                    } else {
                        baseConfig.wasSharedThemeEverActivated = true
                    }

                    runOnUiThread {
                        setupThemes()
                        val hideGoogleRelations = resources.getBoolean(R.bool.hide_google_relations) && !isThankYou
                        apply_to_all_holder.beVisibleIf(
                            storedSharedTheme == null && curSelectedThemeId != THEME_AUTO && curSelectedThemeId != THEME_SYSTEM && !hideGoogleRelations
                        )
                    }
                } catch (e: Exception) {
                    toast(R.string.update_thank_you)
                    finish()
                }
            }
        } else {
            setupThemes()
            baseConfig.isUsingSharedTheme = false
        }

        val textColor = if (baseConfig.isUsingSystemTheme) {
            getProperTextColor()
        } else {
            baseConfig.textColor
        }

        updateLabelColors(textColor)
        originalAppIconColor = baseConfig.appIconColor

        if (resources.getBoolean(R.bool.hide_google_relations) && !isThankYou) {
            apply_to_all_holder.beGone()
        }
    }

    override fun onResume() {
        super.onResume()
        setTheme(getThemeId(getCurrentPrimaryColor()))

        if (!baseConfig.isUsingSystemTheme) {
            updateBackgroundColor(getCurrentBackgroundColor())
            updateActionbarColor(getCurrentStatusBarColor())
        }

        curPrimaryLineColorPicker?.getSpecificColor()?.apply {
            updateActionbarColor(this)
            setTheme(getThemeId(this))
        }

        setupToolbar(customization_toolbar, NavigationIcon.Cross, getColoredMaterialStatusBarColor())
    }

    private fun refreshMenuItems() {
        customization_toolbar.menu.findItem(R.id.save).isVisible = hasUnsavedChanges
    }

    private fun setupOptionsMenu() {
        customization_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.save -> {
                    saveChanges(true)
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        if (hasUnsavedChanges && System.currentTimeMillis() - lastSavePromptTS > SAVE_DISCARD_PROMPT_INTERVAL) {
            promptSaveDiscard()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupThemes() {
        predefinedThemes.apply {
            if (isSPlus()) {
                put(THEME_SYSTEM, getSystemThemeColors())
            }

            put(THEME_AUTO, getAutoThemeColors())
            put(
                THEME_LIGHT,
                MyTheme(
                    getString(R.string.light_theme),
                    R.color.theme_light_text_color,
                    R.color.theme_light_background_color,
                    R.color.color_primary,
                    R.color.color_primary
                )
            )
            put(
                THEME_DARK,
                MyTheme(
                    getString(R.string.dark_theme),
                    R.color.theme_dark_text_color,
                    R.color.theme_dark_background_color,
                    R.color.color_primary,
                    R.color.color_primary
                )
            )
            put(
                THEME_DARK_RED,
                MyTheme(
                    getString(R.string.dark_red),
                    R.color.theme_dark_text_color,
                    R.color.theme_dark_background_color,
                    R.color.theme_dark_red_primary_color,
                    R.color.md_red_700
                )
            )
            put(THEME_WHITE, MyTheme(getString(R.string.white), R.color.dark_grey, android.R.color.white, android.R.color.white, R.color.color_primary))
            put(
                THEME_BLACK_WHITE,
                MyTheme(getString(R.string.black_white), android.R.color.white, android.R.color.black, android.R.color.black, R.color.md_grey_black)
            )
            put(THEME_CUSTOM, MyTheme(getString(R.string.custom), 0, 0, 0, 0))

            if (storedSharedTheme != null) {
                put(THEME_SHARED, MyTheme(getString(R.string.shared), 0, 0, 0, 0))
            }
        }
        setupThemePicker()
        setupColorsPickers()
    }

    private fun setupThemePicker() {
        curSelectedThemeId = getCurrentThemeId()
        customization_theme.text = getThemeText()
        updateAutoThemeFields()
        handleAccentColorLayout()
        customization_theme_holder.setOnClickListener {
            if (baseConfig.wasAppIconCustomizationWarningShown) {
                themePickerClicked()
            } else {
                ConfirmationDialog(this, "", R.string.app_icon_color_warning, R.string.ok, 0) {
                    baseConfig.wasAppIconCustomizationWarningShown = true
                    themePickerClicked()
                }
            }
        }

        if (customization_theme.value == getMaterialYouString()) {
            apply_to_all_holder.beGone()
        }
    }

    private fun themePickerClicked() {
        val items = arrayListOf<RadioItem>()
        for ((key, value) in predefinedThemes) {
            items.add(RadioItem(key, value.label))
        }

        RadioGroupDialog(this@CustomizationActivity, items, curSelectedThemeId) {
            if (it == THEME_SHARED && !isThankYouInstalled()) {
                PurchaseThankYouDialog(this)
                return@RadioGroupDialog
            }

            updateColorTheme(it as Int, true)
            if (it != THEME_CUSTOM && it != THEME_SHARED && it != THEME_AUTO && it != THEME_SYSTEM && !baseConfig.wasCustomThemeSwitchDescriptionShown) {
                baseConfig.wasCustomThemeSwitchDescriptionShown = true
                toast(R.string.changing_color_description)
            }

            val hideGoogleRelations = resources.getBoolean(R.bool.hide_google_relations) && !isThankYou
            apply_to_all_holder.beVisibleIf(
                curSelectedThemeId != THEME_AUTO && curSelectedThemeId != THEME_SYSTEM && curSelectedThemeId != THEME_SHARED && !hideGoogleRelations
            )

            updateMenuItemColors(customization_toolbar.menu, getCurrentStatusBarColor())
            setupToolbar(customization_toolbar, NavigationIcon.Cross, getCurrentStatusBarColor())
        }
    }

    private fun updateColorTheme(themeId: Int, useStored: Boolean = false) {
        curSelectedThemeId = themeId
        customization_theme.text = getThemeText()

        resources.apply {
            if (curSelectedThemeId == THEME_CUSTOM) {
                if (useStored) {
                    curTextColor = baseConfig.customTextColor
                    curBackgroundColor = baseConfig.customBackgroundColor
                    curPrimaryColor = baseConfig.customPrimaryColor
                    curAccentColor = baseConfig.customAccentColor
                    curAppIconColor = baseConfig.customAppIconColor
                    setTheme(getThemeId(curPrimaryColor))
                    updateMenuItemColors(customization_toolbar.menu, curPrimaryColor)
                    setupToolbar(customization_toolbar, NavigationIcon.Cross, curPrimaryColor)
                    setupColorsPickers()
                } else {
                    baseConfig.customPrimaryColor = curPrimaryColor
                    baseConfig.customAccentColor = curAccentColor
                    baseConfig.customBackgroundColor = curBackgroundColor
                    baseConfig.customTextColor = curTextColor
                    baseConfig.customAppIconColor = curAppIconColor
                }
            } else if (curSelectedThemeId == THEME_SHARED) {
                if (useStored) {
                    storedSharedTheme?.apply {
                        curTextColor = textColor
                        curBackgroundColor = backgroundColor
                        curPrimaryColor = primaryColor
                        curAccentColor = accentColor
                        curAppIconColor = appIconColor
                    }
                    setTheme(getThemeId(curPrimaryColor))
                    setupColorsPickers()
                    updateMenuItemColors(customization_toolbar.menu, curPrimaryColor)
                    setupToolbar(customization_toolbar, NavigationIcon.Cross, curPrimaryColor)
                }
            } else {
                val theme = predefinedThemes[curSelectedThemeId]!!
                curTextColor = getColor(theme.textColorId)
                curBackgroundColor = getColor(theme.backgroundColorId)

                if (curSelectedThemeId != THEME_AUTO && curSelectedThemeId != THEME_SYSTEM) {
                    curPrimaryColor = getColor(theme.primaryColorId)
                    curAccentColor = getColor(R.color.color_primary)
                    curAppIconColor = getColor(theme.appIconColorId)
                }

                setTheme(getThemeId(getCurrentPrimaryColor()))
                colorChanged()
                updateMenuItemColors(customization_toolbar.menu, getCurrentStatusBarColor())
                setupToolbar(customization_toolbar, NavigationIcon.Cross, getCurrentStatusBarColor())
            }
        }

        hasUnsavedChanges = true
        refreshMenuItems()
        updateLabelColors(getCurrentTextColor())
        updateBackgroundColor(getCurrentBackgroundColor())
        updateActionbarColor(getCurrentStatusBarColor())
        updateAutoThemeFields()
        updateApplyToAllColors(getCurrentPrimaryColor())
        handleAccentColorLayout()
    }

    private fun getAutoThemeColors(): MyTheme {
        val isUsingSystemDarkTheme = isUsingSystemDarkTheme()
        val textColor = if (isUsingSystemDarkTheme) R.color.theme_dark_text_color else R.color.theme_light_text_color
        val backgroundColor = if (isUsingSystemDarkTheme) R.color.theme_dark_background_color else R.color.theme_light_background_color
        return MyTheme(getString(R.string.auto_light_dark_theme), textColor, backgroundColor, R.color.color_primary, R.color.color_primary)
    }

    // doesn't really matter what colors we use here, everything will be taken from the system. Use the default dark theme values here.
    private fun getSystemThemeColors(): MyTheme {
        return MyTheme(
            getMaterialYouString(),
            R.color.theme_dark_text_color,
            R.color.theme_dark_background_color,
            R.color.color_primary,
            R.color.color_primary
        )
    }

    private fun getCurrentThemeId(): Int {
        if (baseConfig.isUsingSharedTheme) {
            return THEME_SHARED
        } else if ((baseConfig.isUsingSystemTheme && !hasUnsavedChanges) || curSelectedThemeId == THEME_SYSTEM) {
            return THEME_SYSTEM
        } else if (baseConfig.isUsingAutoTheme || curSelectedThemeId == THEME_AUTO) {
            return THEME_AUTO
        }

        var themeId = THEME_CUSTOM
        resources.apply {
            for ((key, value) in predefinedThemes.filter { it.key != THEME_CUSTOM && it.key != THEME_SHARED && it.key != THEME_AUTO && it.key != THEME_SYSTEM }) {
                if (curTextColor == getColor(value.textColorId) &&
                    curBackgroundColor == getColor(value.backgroundColorId) &&
                    curPrimaryColor == getColor(value.primaryColorId) &&
                    curAppIconColor == getColor(value.appIconColorId)
                ) {
                    themeId = key
                }
            }
        }

        return themeId
    }

    private fun getThemeText(): String {
        var label = getString(R.string.custom)
        for ((key, value) in predefinedThemes) {
            if (key == curSelectedThemeId) {
                label = value.label
            }
        }
        return label
    }

    private fun updateAutoThemeFields() {
        arrayOf(customization_text_color_holder, customization_background_color_holder).forEach {
            it.beVisibleIf(curSelectedThemeId != THEME_AUTO && curSelectedThemeId != THEME_SYSTEM)
        }

        customization_primary_color_holder.beVisibleIf(curSelectedThemeId != THEME_SYSTEM)
    }

    private fun promptSaveDiscard() {
        lastSavePromptTS = System.currentTimeMillis()
        ConfirmationAdvancedDialog(this, "", R.string.save_before_closing, R.string.save, R.string.discard) {
            if (it) {
                saveChanges(true)
            } else {
                resetColors()
                finish()
            }
        }
    }

    private fun saveChanges(finishAfterSave: Boolean) {
        val didAppIconColorChange = curAppIconColor != originalAppIconColor
        baseConfig.apply {
            textColor = curTextColor
            backgroundColor = curBackgroundColor
            primaryColor = curPrimaryColor
            accentColor = curAccentColor
            appIconColor = curAppIconColor
        }

        if (didAppIconColorChange) {
            checkAppIconColor()
        }

        if (curSelectedThemeId == THEME_SHARED) {
            val newSharedTheme = SharedTheme(curTextColor, curBackgroundColor, curPrimaryColor, curAppIconColor, 0, curAccentColor)
            updateSharedTheme(newSharedTheme)
            Intent().apply {
                action = MyContentProvider.SHARED_THEME_UPDATED
                sendBroadcast(this)
            }
        }

        baseConfig.isUsingSharedTheme = curSelectedThemeId == THEME_SHARED
        baseConfig.shouldUseSharedTheme = curSelectedThemeId == THEME_SHARED
        baseConfig.isUsingAutoTheme = curSelectedThemeId == THEME_AUTO
        baseConfig.isUsingSystemTheme = curSelectedThemeId == THEME_SYSTEM

        hasUnsavedChanges = false
        if (finishAfterSave) {
            finish()
        } else {
            refreshMenuItems()
        }
    }

    private fun resetColors() {
        hasUnsavedChanges = false
        initColorVariables()
        setupColorsPickers()
        updateBackgroundColor()
        updateActionbarColor()
        refreshMenuItems()
        updateLabelColors(getCurrentTextColor())
    }

    private fun initColorVariables() {
        curTextColor = baseConfig.textColor
        curBackgroundColor = baseConfig.backgroundColor
        curPrimaryColor = baseConfig.primaryColor
        curAccentColor = baseConfig.accentColor
        curAppIconColor = baseConfig.appIconColor
    }

    private fun setupColorsPickers() {
        val textColor = getCurrentTextColor()
        val backgroundColor = getCurrentBackgroundColor()
        val primaryColor = getCurrentPrimaryColor()
        customization_text_color.setFillWithStroke(textColor, backgroundColor)
        customization_primary_color.setFillWithStroke(primaryColor, backgroundColor)
        customization_accent_color.setFillWithStroke(curAccentColor, backgroundColor)
        customization_background_color.setFillWithStroke(backgroundColor, backgroundColor)
        customization_app_icon_color.setFillWithStroke(curAppIconColor, backgroundColor)
        apply_to_all.setTextColor(primaryColor.getContrastColor())

        customization_text_color_holder.setOnClickListener { pickTextColor() }
        customization_background_color_holder.setOnClickListener { pickBackgroundColor() }
        customization_primary_color_holder.setOnClickListener { pickPrimaryColor() }
        customization_accent_color_holder.setOnClickListener { pickAccentColor() }

        handleAccentColorLayout()
        apply_to_all.setOnClickListener {
            applyToAll()
        }

        customization_app_icon_color_holder.setOnClickListener {
            if (baseConfig.wasAppIconCustomizationWarningShown) {
                pickAppIconColor()
            } else {
                ConfirmationDialog(this, "", R.string.app_icon_color_warning, R.string.ok, 0) {
                    baseConfig.wasAppIconCustomizationWarningShown = true
                    pickAppIconColor()
                }
            }
        }
    }

    private fun hasColorChanged(old: Int, new: Int) = Math.abs(old - new) > 1

    private fun colorChanged() {
        hasUnsavedChanges = true
        setupColorsPickers()
        refreshMenuItems()
    }

    private fun setCurrentTextColor(color: Int) {
        curTextColor = color
        updateLabelColors(color)
    }

    private fun setCurrentBackgroundColor(color: Int) {
        curBackgroundColor = color
        updateBackgroundColor(color)
    }

    private fun setCurrentPrimaryColor(color: Int) {
        curPrimaryColor = color
        updateActionbarColor(color)
        updateApplyToAllColors(color)
    }

    private fun updateApplyToAllColors(newColor: Int) {
        if (newColor == baseConfig.primaryColor && !baseConfig.isUsingSystemTheme) {
            apply_to_all.setBackgroundResource(R.drawable.button_background_rounded)
        } else {
            val applyBackground = resources.getDrawable(R.drawable.button_background_rounded, theme) as RippleDrawable
            (applyBackground as LayerDrawable).findDrawableByLayerId(R.id.button_background_holder).applyColorFilter(newColor)
            apply_to_all.background = applyBackground
        }
    }

    private fun handleAccentColorLayout() {
        customization_accent_color_holder.beVisibleIf(curSelectedThemeId == THEME_WHITE || isCurrentWhiteTheme() || curSelectedThemeId == THEME_BLACK_WHITE || isCurrentBlackAndWhiteTheme())
        customization_accent_color_label.text = getString(
            if (curSelectedThemeId == THEME_WHITE || isCurrentWhiteTheme()) {
                R.string.accent_color_white
            } else {
                R.string.accent_color_black_and_white
            }
        )
    }

    private fun isCurrentWhiteTheme() = curTextColor == DARK_GREY && curPrimaryColor == Color.WHITE && curBackgroundColor == Color.WHITE

    private fun isCurrentBlackAndWhiteTheme() = curTextColor == Color.WHITE && curPrimaryColor == Color.BLACK && curBackgroundColor == Color.BLACK

    private fun pickTextColor() {
        ColorPickerDialog(this, curTextColor) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                if (hasColorChanged(curTextColor, color)) {
                    setCurrentTextColor(color)
                    colorChanged()
                    updateColorTheme(getUpdatedTheme())
                }
            }
        }
    }

    private fun pickBackgroundColor() {
        ColorPickerDialog(this, curBackgroundColor) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                if (hasColorChanged(curBackgroundColor, color)) {
                    setCurrentBackgroundColor(color)
                    colorChanged()
                    updateColorTheme(getUpdatedTheme())
                }
            }
        }
    }

    private fun pickPrimaryColor() {
        if (!packageName.startsWith("com.simplemobiletools.", true) && baseConfig.appRunCount > 50) {
            finish()
            return
        }

        curPrimaryLineColorPicker = LineColorPickerDialog(this, curPrimaryColor, true, toolbar = customization_toolbar) { wasPositivePressed, color ->
            curPrimaryLineColorPicker = null
            if (wasPositivePressed) {
                if (hasColorChanged(curPrimaryColor, color)) {
                    setCurrentPrimaryColor(color)
                    colorChanged()
                    updateColorTheme(getUpdatedTheme())
                    setTheme(getThemeId(color))
                }
                updateMenuItemColors(customization_toolbar.menu, color)
                setupToolbar(customization_toolbar, NavigationIcon.Cross, color)
            } else {
                updateActionbarColor(curPrimaryColor)
                setTheme(getThemeId(curPrimaryColor))
                updateMenuItemColors(customization_toolbar.menu, curPrimaryColor)
                setupToolbar(customization_toolbar, NavigationIcon.Cross, curPrimaryColor)
                updateTopBarColors(customization_toolbar, curPrimaryColor)
            }
        }
    }

    private fun pickAccentColor() {
        ColorPickerDialog(this, curAccentColor) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                if (hasColorChanged(curAccentColor, color)) {
                    curAccentColor = color
                    colorChanged()

                    if (isCurrentWhiteTheme() || isCurrentBlackAndWhiteTheme()) {
                        updateActionbarColor(getCurrentStatusBarColor())
                    }
                }
            }
        }
    }

    private fun pickAppIconColor() {
        LineColorPickerDialog(this, curAppIconColor, false, R.array.md_app_icon_colors, getAppIconIDs()) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                if (hasColorChanged(curAppIconColor, color)) {
                    curAppIconColor = color
                    colorChanged()
                    updateColorTheme(getUpdatedTheme())
                }
            }
        }
    }

    private fun getUpdatedTheme() = if (curSelectedThemeId == THEME_SHARED) THEME_SHARED else getCurrentThemeId()

    private fun applyToAll() {
        if (isThankYouInstalled()) {
            ConfirmationDialog(this, "", R.string.share_colors_success, R.string.ok, 0) {
                Intent().apply {
                    action = MyContentProvider.SHARED_THEME_ACTIVATED
                    sendBroadcast(this)
                }

                if (!predefinedThemes.containsKey(THEME_SHARED)) {
                    predefinedThemes[THEME_SHARED] = MyTheme(getString(R.string.shared), 0, 0, 0, 0)
                }

                baseConfig.wasSharedThemeEverActivated = true
                apply_to_all_holder.beGone()
                updateColorTheme(THEME_SHARED)
                saveChanges(false)
            }
        } else {
            PurchaseThankYouDialog(this)
        }
    }

    private fun updateLabelColors(textColor: Int) {
        arrayListOf<MyTextView>(
            customization_theme_label,
            customization_theme,
            customization_text_color_label,
            customization_background_color_label,
            customization_primary_color_label,
            customization_accent_color_label,
            customization_app_icon_color_label
        ).forEach {
            it.setTextColor(textColor)
        }

        val primaryColor = getCurrentPrimaryColor()
        apply_to_all.setTextColor(primaryColor.getContrastColor())
        updateApplyToAllColors(primaryColor)
    }

    private fun getCurrentTextColor() = if (customization_theme.value == getMaterialYouString()) {
        resources.getColor(R.color.you_neutral_text_color)
    } else {
        curTextColor
    }

    private fun getCurrentBackgroundColor() = if (customization_theme.value == getMaterialYouString()) {
        resources.getColor(R.color.you_background_color)
    } else {
        curBackgroundColor
    }

    private fun getCurrentPrimaryColor() = if (customization_theme.value == getMaterialYouString()) {
        resources.getColor(R.color.you_primary_color)
    } else {
        curPrimaryColor
    }

    private fun getCurrentStatusBarColor() = if (customization_theme.value == getMaterialYouString()) {
        resources.getColor(R.color.you_status_bar_color)
    } else {
        curPrimaryColor
    }

    private fun getMaterialYouString() = "${getString(R.string.system_default)} (${getString(R.string.material_you)})"
}
