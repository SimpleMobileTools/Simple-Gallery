package com.gallery.raw.activities

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FileDirItem
import com.gallery.raw.R
import com.gallery.raw.dialogs.SaveAsDialog
import com.gallery.raw.extensions.config
import com.gallery.raw.extensions.fixDateTaken
import com.gallery.raw.extensions.tryDeleteFileDirItem
import com.gallery.raw.helpers.getPermissionToRequest
import ly.img.android.pesdk.VideoEditorSettingsList
import ly.img.android.pesdk.assets.filter.basic.FilterPackBasic
import ly.img.android.pesdk.assets.font.basic.FontPackBasic
import ly.img.android.pesdk.assets.overlay.basic.OverlayPackBasic
import ly.img.android.pesdk.assets.sticker.animated.StickerPackAnimated
import ly.img.android.pesdk.assets.sticker.emoticons.StickerPackEmoticons
import ly.img.android.pesdk.assets.sticker.shapes.StickerPackShapes
import ly.img.android.pesdk.backend.model.config.CropAspectAsset
import ly.img.android.pesdk.backend.model.constant.OutputMode
import ly.img.android.pesdk.backend.model.state.BrushSettings
import ly.img.android.pesdk.backend.model.state.LoadSettings
import ly.img.android.pesdk.backend.model.state.VideoEditorSaveSettings
import ly.img.android.pesdk.backend.model.state.manager.SettingsList
import ly.img.android.pesdk.ui.activity.VideoEditorBuilder
import ly.img.android.pesdk.ui.model.state.*
import ly.img.android.pesdk.ui.panels.item.CropAspectItem
import ly.img.android.pesdk.ui.panels.item.PersonalStickerAddItem
import ly.img.android.pesdk.ui.panels.item.ToggleAspectItem
import java.io.File
import java.io.InputStream

class NewVideoEditActivity : SimpleActivity() {
    private val VESDK_EDIT_VIDEO = 1
    private val SETTINGS_LIST = "SETTINGS_LIST"
    private val SOURCE_URI = "SOURCE_URI"
    private val RESULT_URI = "RESULT_URI"
    private var sourceFileLastModified = 0L
    private var oldExif: ExifInterface? = null

    private lateinit var uri: Uri
    private lateinit var saveUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_video_edit)

        if (checkAppSideloading()) {
            return
        }

        handlePermission(getPermissionToRequest()) {
            if (it) {
                initEditActivity()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    private fun initEditActivity() {
        if (intent.data == null) {
            toast(R.string.invalid_video_path)
            finish()
            return
        }

        uri = intent.data!!
        if (uri.scheme != "file" && uri.scheme != "content") {
            toast(R.string.unknown_file_location)
            finish()
            return
        }

        if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            val realPath = intent.extras!!.getString(REAL_FILE_PATH)
            uri = when {
                isPathOnOTG(realPath!!) -> uri
                realPath.startsWith("file:/") -> Uri.parse(realPath)
                else -> Uri.fromFile(File(realPath))
            }
        } else {
            (getRealPathFromURI(uri))?.apply {
                uri = Uri.fromFile(File(this))
            }
        }

        saveUri = when {
            intent.extras?.containsKey(MediaStore.EXTRA_OUTPUT) == true -> intent.extras!!.get(MediaStore.EXTRA_OUTPUT) as Uri
            else -> uri
        }

        openEditor(uri)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == VESDK_EDIT_VIDEO) {
            val extras = resultData?.extras
            val resultPath = extras?.get(RESULT_URI)?.toString() ?: ""
            val sourcePath = Uri.decode(extras?.get(SOURCE_URI)?.toString() ?: "")

            val settings = extras?.getParcelable<SettingsList>(SETTINGS_LIST)
            if (settings != null) {
                val brush = settings.getSettingsModel(BrushSettings::class.java)
                config.editorBrushColor = brush.brushColor
                config.editorBrushHardness = brush.brushHardness
                config.editorBrushSize = brush.brushSize
            }

            if (resultCode != Activity.RESULT_OK || resultPath.isEmpty()) {
                toast(R.string.video_editing_cancelled)
                finish()
            } else {
                val source = if (sourcePath.isEmpty() || sourcePath.startsWith("content")) {
                    internalStoragePath
                } else {
                    sourcePath.substringAfter("file://")
                }

                SaveAsDialog(this, source, true, cancelCallback = {
                    toast(R.string.video_editing_failed)
                    finish()
                }, callback = {
                    val destinationFilePath = it
                    handleSAFDialog(destinationFilePath) {
                        if (it) {
                            ensureBackgroundThread {
                                storeOldExif(source)
                                sourceFileLastModified = File(source).lastModified()

                                handleFileOverwriting(destinationFilePath) {
                                    try {
                                        val inputStream = contentResolver.openInputStream(Uri.parse(resultPath))
                                        val outputStream = getFileOutputStreamSync(destinationFilePath, destinationFilePath.getMimeType())
                                        inputStream?.use {
                                            outputStream?.use {
                                                inputStream.copyTo(outputStream)
                                            }
                                        }

                                        if (config.keepLastModified) {
                                            // add 1 s to the last modified time to properly update the thumbnail
                                            updateLastModified(destinationFilePath, sourceFileLastModified + 1000)
                                        }

                                        val paths = arrayListOf(destinationFilePath)
                                        rescanPaths(paths) {
                                            fixDateTaken(paths, false)

                                            setResult(Activity.RESULT_OK)
                                            toast(R.string.file_edited_successfully)
                                            finish()
                                        }
                                    } catch (e: Exception) {
                                        showErrorToast(e)
                                    }
                                }
                            }
                        } else {
                            toast(R.string.video_editing_failed)
                            finish()
                        }
                    }
                })
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun storeOldExif(sourcePath: String) {
        var inputStream: InputStream? = null
        try {
            if (isNougatPlus()) {
                inputStream = contentResolver.openInputStream(Uri.fromFile(File(sourcePath)))
                oldExif = ExifInterface(inputStream!!)
            }
        } catch (ignored: Exception) {
        } finally {
            inputStream?.close()
        }
    }

    // In case the user wants to overwrite the original file and it is on an SD card, delete it manually first. Else the system just appends (1)
    private fun handleFileOverwriting(path: String, callback: () -> Unit) {
        if (!isRPlus() && getDoesFilePathExist(path) && isPathOnSD(path)) {
            val fileDirItem = FileDirItem(path, path.getFilenameFromPath())
            tryDeleteFileDirItem(fileDirItem, false, true) { success ->
                if (success) {
                    callback()
                } else {
                    toast(R.string.unknown_error_occurred)
                    finish()
                }
            }
        } else {
            callback()
        }
    }

    private fun openEditor(inputVideo: Uri) {
        val settingsList = createVesdkSettingsList()

        settingsList.configure<LoadSettings> {
            it.source = inputVideo
        }

        settingsList[LoadSettings::class].source = inputVideo

        VideoEditorBuilder(this)
            .setSettingsList(settingsList)
            .startActivityForResult(this, VESDK_EDIT_VIDEO)

        settingsList.release()
    }

    private fun createVesdkSettingsList(): VideoEditorSettingsList {
        val settingsList = VideoEditorSettingsList(false).apply {
            configure<UiConfigFilter> {
                it.setFilterList(FilterPackBasic.getFilterPack())
            }

            configure<UiConfigText> {
                it.setFontList(FontPackBasic.getFontPack())
            }

            config.getAssetMap(CropAspectAsset::class.java).apply {
                add(CropAspectAsset("my_crop_1_2", 1, 2, false))
                add(CropAspectAsset("my_crop_2_1", 2, 1, false))
                add(CropAspectAsset("my_crop_19_9", 19, 9, false))
                add(CropAspectAsset("my_crop_9_19", 9, 19, false))
                add(CropAspectAsset("my_crop_5_4", 5, 4, false))
                add(CropAspectAsset("my_crop_4_5", 4, 5, false))
                add(CropAspectAsset("my_crop_37_18", 37, 18, false))
                add(CropAspectAsset("my_crop_18_37", 18, 37, false))
                add(CropAspectAsset("my_crop_16_10", 16, 10, false))
                add(CropAspectAsset("my_crop_10_16", 10, 16, false))
            }

            getSettingsModel(UiConfigAspect::class.java).aspectList.apply {
                add(ToggleAspectItem(CropAspectItem("my_crop_2_1"), CropAspectItem("my_crop_1_2")))
                add(ToggleAspectItem(CropAspectItem("my_crop_19_9"), CropAspectItem("my_crop_9_19")))
                add(ToggleAspectItem(CropAspectItem("my_crop_5_4"), CropAspectItem("my_crop_4_5")))
                add(ToggleAspectItem(CropAspectItem("my_crop_37_18"), CropAspectItem("my_crop_18_37")))
                add(ToggleAspectItem(CropAspectItem("my_crop_16_10"), CropAspectItem("my_crop_10_16")))
            }

            getSettingsModel(BrushSettings::class.java).apply {
                brushColor = applicationContext.config.editorBrushColor
                brushHardness = applicationContext.config.editorBrushHardness
                brushSize = applicationContext.config.editorBrushSize
            }

            configure<UiConfigOverlay> {
                it.setOverlayList(OverlayPackBasic.getOverlayPack())
            }

            configure<UiConfigSticker> {
                it.setStickerLists(
                    PersonalStickerAddItem(),
                    StickerPackEmoticons.getStickerCategory(),
                    StickerPackShapes.getStickerCategory(),
                    StickerPackAnimated.getStickerCategory()
                )
            }

            val theme = if (isUsingSystemDarkTheme()) {
                R.style.Theme_Imgly_NoFullscreen
            } else {
                R.style.Theme_Imgly_Light_NoFullscreen
            }

            getSettingsModel(UiConfigTheme::class.java).theme = theme

            configure<VideoEditorSaveSettings> {
                it.allowOrientationMatrixMetadata = true
                it.setOutputToTemp()
                it.outputMode = OutputMode.EXPORT_IF_NECESSARY
            }
        }

        return settingsList
    }
}
