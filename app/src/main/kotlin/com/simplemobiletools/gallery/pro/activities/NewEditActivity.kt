package com.simplemobiletools.gallery.pro.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.util.Pair
import com.simplemobiletools.commons.asynctasks.CopyMoveTask
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.CONFLICT_OVERWRITE
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
import com.simplemobiletools.commons.interfaces.CopyMoveListener
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.dialogs.SaveAsDialog
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.fixDateTaken
import ly.img.android.pesdk.assets.filter.basic.FilterPackBasic
import ly.img.android.pesdk.assets.font.basic.FontPackBasic
import ly.img.android.pesdk.backend.model.state.EditorLoadSettings
import ly.img.android.pesdk.backend.model.state.EditorSaveSettings
import ly.img.android.pesdk.backend.model.state.manager.SettingsList
import ly.img.android.pesdk.ui.activity.PhotoEditorBuilder
import ly.img.android.pesdk.ui.model.state.UiConfigFilter
import ly.img.android.pesdk.ui.model.state.UiConfigMainMenu
import ly.img.android.pesdk.ui.model.state.UiConfigText
import ly.img.android.pesdk.ui.model.state.UiConfigTheme
import ly.img.android.pesdk.ui.panels.item.ToolItem
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.collections.set

class NewEditActivity : SimpleActivity() {
    private val PESDK_EDIT_IMAGE = 1
    private val SOURCE_IMAGE_PATH = "SOURCE_IMAGE_PATH"
    private val RESULT_IMAGE_PATH = "RESULT_IMAGE_PATH"
    private var sourceFileLastModified = 0L
    private var destinationFilePath = ""
    private var imagePathFromEditor = ""    // delete the file stored at the internal app storage (the editor saves it there) in case moving to the selected location fails

    private lateinit var uri: Uri
    private lateinit var saveUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_edit)

        if (checkAppSideloading()) {
            return
        }

        handlePermission(PERMISSION_WRITE_STORAGE) {
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
            toast(R.string.invalid_image_path)
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
        if (requestCode == PESDK_EDIT_IMAGE) {
            val extras = resultData?.extras
            val source = extras?.getString(SOURCE_IMAGE_PATH, "") ?: ""
            imagePathFromEditor = extras?.getString(RESULT_IMAGE_PATH, "") ?: ""

            if (resultCode != Activity.RESULT_OK || source.isEmpty() || imagePathFromEditor.isEmpty() || source == imagePathFromEditor) {
                finish()
            } else {
                // the image is stored at the internal app storage first, for example /data/user/0/com.simplemobiletools.gallery.pro/files/editor/IMG_20191207_183023.jpg
                // first we rename it to the desired name, then move
                SaveAsDialog(this, source, true, cancelCallback = {
                    toast(R.string.image_editing_failed)
                    finish()
                }, callback = {
                    destinationFilePath = it
                    handleSAFDialog(destinationFilePath) {
                        if (it) {
                            sourceFileLastModified = File(source).lastModified()
                            val newFile = File("${imagePathFromEditor.getParentPath()}/${destinationFilePath.getFilenameFromPath()}")
                            File(imagePathFromEditor).renameTo(newFile)
                            val sourceFile = FileDirItem(newFile.absolutePath, newFile.name)

                            val conflictResolutions = LinkedHashMap<String, Int>()
                            conflictResolutions[destinationFilePath] = CONFLICT_OVERWRITE

                            val pair = Pair(arrayListOf(sourceFile), destinationFilePath.getParentPath())
                            CopyMoveTask(this, false, true, conflictResolutions, editCopyMoveListener, true).execute(pair)
                        } else {
                            toast(R.string.image_editing_failed)
                            File(imagePathFromEditor).delete()
                            finish()
                        }
                    }
                })
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private val editCopyMoveListener = object : CopyMoveListener {
        override fun copySucceeded(copyOnly: Boolean, copiedAll: Boolean, destinationPath: String) {
            if (config.keepLastModified) {
                // add 1 s to the last modified time to properly update the thumbnail
                updateLastModified(destinationFilePath, sourceFileLastModified + 1000)
            }

            val paths = arrayListOf(destinationFilePath)
            rescanPaths(paths) {
                fixDateTaken(paths, false)
            }

            setResult(Activity.RESULT_OK, intent)
            toast(R.string.file_edited_successfully)
            finish()
        }

        override fun copyFailed() {
            toast(R.string.unknown_error_occurred)
            File(imagePathFromEditor).delete()
            finish()
        }
    }

    private fun openEditor(inputImage: Uri) {
        val filename = inputImage.toString().getFilenameFromPath()
        val settingsList = createPesdkSettingsList(filename)

        settingsList.getSettingsModel(EditorLoadSettings::class.java).imageSource = inputImage

        PhotoEditorBuilder(this)
                .setSettingsList(settingsList)
                .startActivityForResult(this, PESDK_EDIT_IMAGE)
    }

    private fun createPesdkSettingsList(filename: String): SettingsList {
        val settingsList = SettingsList()
        settingsList.getSettingsModel(UiConfigFilter::class.java).setFilterList(
                FilterPackBasic.getFilterPack()
        )

        settingsList.getSettingsModel(UiConfigText::class.java).setFontList(
                FontPackBasic.getFontPack()
        )

        // do not use Text Design, it takes up too much space
        val tools = settingsList.getSettingsModel(UiConfigMainMenu::class.java).toolList
        val newTools = tools.filterNot {
            it.name!!.isEmpty()
        }.toMutableList() as ArrayList<ToolItem>

        settingsList.getSettingsModel(UiConfigMainMenu::class.java).setToolList(newTools)

        settingsList.getSettingsModel(UiConfigTheme::class.java).theme = R.style.Imgly_Theme_NoFullscreen

        settingsList.getSettingsModel(EditorSaveSettings::class.java)
                .setOutputFilePath("$filesDir/editor/$filename")
                .savePolicy = EditorSaveSettings.SavePolicy.RETURN_SOURCE_OR_CREATE_OUTPUT_IF_NECESSARY

        return settingsList
    }
}
