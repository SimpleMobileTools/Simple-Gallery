package com.simplemobiletools.gallery.pro.dialogs

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.isRPlus
import com.simplemobiletools.gallery.pro.R
import kotlinx.android.synthetic.main.dialog_save_as.view.*
import java.io.File

class SaveAsDialog(
    val activity: BaseSimpleActivity, val path: String, val appendFilename: Boolean, val cancelCallback: (() -> Unit)? = null,
    val callback: (savePath: String) -> Unit
) {

    init {
        var realPath = path.getParentPath()
        if (activity.isRestrictedWithSAFSdk30(realPath) && !activity.isInDownloadDir(realPath)) {
            realPath = activity.getPicturesDirectoryPath(realPath)
        }

        var alertDialogBuilder = AlertDialog.Builder(activity)
        var alertDialog: AlertDialog? = null
        val originalFullName = when {
            appendFilename -> path.getFilenameFromPath()
            else -> {
                val tempFullName = path.getFilenameFromPath()
                tempFullName.substring(0, tempFullName.lastIndexOf(".")).dropLast("_1".length) + tempFullName.substring(tempFullName.lastIndexOf("."))
            }
        }

        val view = activity.layoutInflater.inflate(R.layout.dialog_save_as, null).apply {
            save_as_path.text = "${activity.humanizePath(realPath).trimEnd('/')}/"

            val fullName = path.getFilenameFromPath()
            val dotAt = fullName.lastIndexOf(".")
            var name = fullName

            if (dotAt > 0) {
                name = fullName.substring(0, dotAt)
                val extension = fullName.substring(dotAt + 1)
                save_as_extension.setText(extension)
            }

            if (appendFilename) {
                name += "_1"
            }

            save_as_name.setText(name)
            save_as_path.setOnClickListener {
                activity.hideKeyboard(save_as_path)
                FilePickerDialog(activity, realPath, false, false, true, true) {
                    save_as_path.text = activity.humanizePath(it)
                    realPath = it
                }
            }
            replace_original.setOnCheckedChangeListener { _, isChecked ->
                run {
                    toggleViewFocusability(save_as_name)
                    toggleViewFocusability(save_as_path)
                    toggleViewFocusability(save_as_extension)
                    if (isChecked) {
                        alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.text = context.getString(R.string.pesdk_sticker_button_replace)
                    } else {
                        alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.text = context.getString(R.string.ok)
                    }
                }
            }
        }

        alertDialogBuilder
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel) { dialog, which -> cancelCallback?.invoke() }
            .setOnCancelListener { cancelCallback?.invoke() }

        alertDialog = alertDialogBuilder.create()

        alertDialog.apply {
            activity.setupDialogStuff(view, this, R.string.save_as) {
                showKeyboard(view.save_as_name)
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val filename = view.save_as_name.value
                    val extension = view.save_as_extension.value

                    if (view.replace_original.isChecked) {
                        overwriteFile("${realPath.trimEnd('/')}/$originalFullName")
                        return@setOnClickListener
                    }

                    if (filename.isEmpty()) {
                        activity.toast(R.string.filename_cannot_be_empty)
                        return@setOnClickListener
                    }

                    if (extension.isEmpty()) {
                        activity.toast(R.string.extension_cannot_be_empty)
                        return@setOnClickListener
                    }

                    val newFilename = "$filename.$extension"
                    val newPath = "${realPath.trimEnd('/')}/$newFilename"
                    if (!newFilename.isAValidFilename()) {
                        activity.toast(R.string.filename_invalid_characters)
                        return@setOnClickListener
                    }

                    if (activity.getDoesFilePathExist(newPath)) {
                        val title = String.format(activity.getString(R.string.file_already_exists_overwrite), newFilename)
                        ConfirmationDialog(activity, title) {
                            overwriteFile(newPath)
                        }
                    } else {
                        selectPath(this, newPath)
                    }
                }
            }
        }
    }

    private fun AlertDialog.overwriteFile(path: String) {
        val newFile = File(path)
        val isInDownloadDir = activity.isInDownloadDir(path)
        val isInSubFolderInDownloadDir = activity.isInSubFolderInDownloadDir(path)
        if (isRPlus() && isInDownloadDir && !isInSubFolderInDownloadDir && !newFile.canWrite()) {
            val fileDirItem = arrayListOf(File(path).toFileDirItem(activity))
            val fileUris = activity.getFileUrisFromFileDirItems(fileDirItem).second
            activity.updateSDK30Uris(fileUris) { success ->
                if (success) {
                    selectPath(this, path)
                }
            }
        } else {
            selectPath(this, path)
        }
    }

    private fun toggleViewFocusability(view: View?) {
        val viewIsEnabled = view?.isEnabled
        view?.isEnabled = !viewIsEnabled!!
        view.isFocusableInTouchMode = !viewIsEnabled
    }

    private fun selectPath(alertDialog: AlertDialog, newPath: String) {
        activity.handleSAFDialogSdk30(newPath) {
            if (!it) {
                return@handleSAFDialogSdk30
            }
            callback(newPath)
            alertDialog.dismiss()
        }
    }
}
