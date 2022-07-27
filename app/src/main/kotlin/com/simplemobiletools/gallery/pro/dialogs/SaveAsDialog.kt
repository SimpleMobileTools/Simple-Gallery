package com.simplemobiletools.gallery.pro.dialogs

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

        val view = activity.layoutInflater.inflate(R.layout.dialog_save_as, null).apply {
            folder_value.setText("${activity.humanizePath(realPath).trimEnd('/')}/")

            val fullName = path.getFilenameFromPath()
            val dotAt = fullName.lastIndexOf(".")
            var name = fullName

            if (dotAt > 0) {
                name = fullName.substring(0, dotAt)
                val extension = fullName.substring(dotAt + 1)
                extension_value.setText(extension)
            }

            if (appendFilename) {
                name += "_1"
            }

            filename_value.setText(name)
            folder_value.setOnClickListener {
                activity.hideKeyboard(folder_value)
                FilePickerDialog(activity, realPath, false, false, true, true) {
                    folder_value.setText(activity.humanizePath(it))
                    realPath = it
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel) { dialog, which -> cancelCallback?.invoke() }
            .setOnCancelListener { cancelCallback?.invoke() }
            .apply {
                activity.setupDialogStuff(view, this, R.string.save_as) { alertDialog ->
                    alertDialog.showKeyboard(view.filename_value)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = view.filename_value.value
                        val extension = view.extension_value.value

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
                                val newFile = File(newPath)
                                val isInDownloadDir = activity.isInDownloadDir(newPath)
                                val isInSubFolderInDownloadDir = activity.isInSubFolderInDownloadDir(newPath)
                                if ((isRPlus() && !isExternalStorageManager()) && isInDownloadDir && !isInSubFolderInDownloadDir && !newFile.canWrite()) {
                                    val fileDirItem = arrayListOf(File(newPath).toFileDirItem(activity))
                                    val fileUris = activity.getFileUrisFromFileDirItems(fileDirItem)
                                    activity.updateSDK30Uris(fileUris) { success ->
                                        if (success) {
                                            selectPath(alertDialog, newPath)
                                        }
                                    }
                                } else {
                                    selectPath(alertDialog, newPath)
                                }
                            }
                        } else {
                            selectPath(alertDialog, newPath)
                        }
                    }
                }
            }
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
