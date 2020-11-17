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
import com.simplemobiletools.gallery.pro.databinding.ActivityWidgetConfigBinding
import com.simplemobiletools.gallery.pro.dialogs.PickDirectoryDialog
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.helpers.MyWidgetProvider
import com.simplemobiletools.gallery.pro.helpers.ROUNDED_CORNERS_NONE
import com.simplemobiletools.gallery.pro.models.Directory
import com.simplemobiletools.gallery.pro.models.Widget

class WidgetConfigureActivity : SimpleActivity() {
    private lateinit var binding: ActivityWidgetConfigBinding

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

        binding = ActivityWidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initVariables()

        mWidgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }

        binding.configSave.setOnClickListener { saveConfig() }
        binding.configBgColor.setOnClickListener { pickBackgroundColor() }
        binding.configTextColor.setOnClickListener { pickTextColor() }
        binding.folderPickerValue.setOnClickListener { changeSelectedFolder() }
        binding.configImageHolder.setOnClickListener { changeSelectedFolder() }
        binding.folderPickerShowFolderName.isChecked = config.showWidgetFolderName
        handleFolderNameDisplay()
        binding.folderPickerShowFolderNameHolder.setOnClickListener {
            binding.folderPickerShowFolderName.toggle()
            handleFolderNameDisplay()
        }

        updateTextColors(binding.folderPickerHolder)
        binding.folderPickerHolder.background = ColorDrawable(config.backgroundColor)

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
        binding.configBgSeekbar.apply {
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
        config.showWidgetFolderName = binding.folderPickerShowFolderName.isChecked
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
        binding.configSave.setBackgroundColor(mBgColor)
        binding.configImageHolder.setBackgroundColor(mBgColor)
        binding.configBgColor.setFillWithStroke(mBgColor, Color.BLACK)
    }

    private fun updateTextColor() {
        binding.configSave.setTextColor(mTextColor)
        binding.configFolderName.setTextColor(mTextColor)
        binding.configTextColor.setFillWithStroke(mTextColor, Color.BLACK)
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
            binding.folderPickerValue.text = getFolderNameFromPath(folderPath)
            binding.configFolderName.text = getFolderNameFromPath(folderPath)
        }

        ensureBackgroundThread {
            val path = directoryDao.getDirectoryThumbnail(folderPath)
            if (path != null) {
                runOnUiThread {
                    loadJpg(path, binding.configImage, config.cropThumbnails, ROUNDED_CORNERS_NONE)
                }
            }
        }
    }

    private fun handleFolderNameDisplay() {
        val showFolderName = binding.folderPickerShowFolderName.isChecked
        binding.configFolderName.beVisibleIf(showFolderName)
        (binding.configImage.layoutParams as RelativeLayout.LayoutParams).bottomMargin = if (showFolderName) 0 else resources.getDimension(R.dimen.normal_margin).toInt()
    }
}
