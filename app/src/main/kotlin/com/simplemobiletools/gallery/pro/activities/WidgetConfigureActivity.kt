package com.simplemobiletools.gallery.pro.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.RelativeLayout
import android.widget.RemoteViews
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.dialogs.PickDirectoryDialog
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.helpers.MyWidgetProvider
import com.simplemobiletools.gallery.pro.models.Directory
import com.simplemobiletools.gallery.pro.models.Widget
import kotlinx.android.synthetic.main.activity_widget_config.*

class WidgetConfigureActivity : SimpleActivity() {
    private var mBgAlpha = 0f
    private var mWidgetId = 0
    private var mBgColor = 0
    private var mBgColorWithoutTransparency = 0
    private var mTextColor = 0
    private var mFolderPath = ""
    private var mDirectories = ArrayList<Directory>()

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_widget_config)
        initVariables()

        mWidgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }

        config_save.setOnClickListener { saveConfig() }
        config_bg_color.setOnClickListener { pickBackgroundColor() }
        config_text_color.setOnClickListener { pickTextColor() }
        folder_picker_value.setOnClickListener { changeSelectedFolder() }
        config_image_holder.setOnClickListener { changeSelectedFolder() }
        folder_picker_show_folder_name.isChecked = config.showWidgetFolderName
        handleFolderNameDisplay()
        folder_picker_show_folder_name_holder.setOnClickListener {
            folder_picker_show_folder_name.toggle()
            handleFolderNameDisplay()
        }

        updateTextColors(folder_picker_holder)
        folder_picker_holder.background = ColorDrawable(config.backgroundColor)

        getCachedDirectories(false, false) {
            mDirectories = it
            val path = it.firstOrNull()?.path
            if (path != null) {
                updateFolderImage(path)
            }
        }
    }

    private fun initVariables() {
        mBgColor = config.widgetBgColor
        mBgAlpha = Color.alpha(mBgColor) / 255f

        mBgColorWithoutTransparency = Color.rgb(Color.red(mBgColor), Color.green(mBgColor), Color.blue(mBgColor))
        config_bg_seekbar.apply {
            progress = (mBgAlpha * 100).toInt()

            onSeekBarChangeListener {
                mBgAlpha = it / 100f
                updateBackgroundColor()
            }
        }
        updateBackgroundColor()

        mTextColor = config.widgetTextColor
        updateTextColor()
    }

    private fun saveConfig() {
        val views = RemoteViews(packageName, R.layout.widget)
        views.setBackgroundColor(R.id.widget_holder, mBgColor)
        AppWidgetManager.getInstance(this).updateAppWidget(mWidgetId, views)
        config.showWidgetFolderName = folder_picker_show_folder_name.isChecked
        val widget = Widget(null, mWidgetId, mFolderPath)
        ensureBackgroundThread {
            widgetsDB.insertOrUpdate(widget)
        }

        storeWidgetColors()
        requestWidgetUpdate()

        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    private fun storeWidgetColors() {
        config.apply {
            widgetBgColor = mBgColor
            widgetTextColor = mTextColor
        }
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateBackgroundColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        config_save.setBackgroundColor(mBgColor)
        config_image_holder.setBackgroundColor(mBgColor)
        config_bg_color.setFillWithStroke(mBgColor, Color.BLACK)
    }

    private fun updateTextColor() {
        config_save.setTextColor(mTextColor)
        config_folder_name.setTextColor(mTextColor)
        config_text_color.setFillWithStroke(mTextColor, Color.BLACK)
    }

    private fun pickBackgroundColor() {
        ColorPickerDialog(this, mBgColorWithoutTransparency) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                mBgColorWithoutTransparency = color
                updateBackgroundColor()
            }
        }
    }

    private fun pickTextColor() {
        ColorPickerDialog(this, mTextColor) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                mTextColor = color
                updateTextColor()
            }
        }
    }

    private fun changeSelectedFolder() {
        PickDirectoryDialog(this, "", false, true) {
            updateFolderImage(it)
        }
    }

    private fun updateFolderImage(folderPath: String) {
        mFolderPath = folderPath
        runOnUiThread {
            folder_picker_value.text = getFolderNameFromPath(folderPath)
            config_folder_name.text = getFolderNameFromPath(folderPath)
        }

        ensureBackgroundThread {
            val path = directoryDao.getDirectoryThumbnail(folderPath)
            if (path != null) {
                runOnUiThread {
                    loadJpg(path, config_image, config.cropThumbnails)
                }
            }
        }
    }

    private fun handleFolderNameDisplay() {
        val showFolderName = folder_picker_show_folder_name.isChecked
        config_folder_name.beVisibleIf(showFolderName)
        (config_image.layoutParams as RelativeLayout.LayoutParams).bottomMargin = if (showFolderName) 0 else resources.getDimension(R.dimen.normal_margin).toInt()
    }
}
