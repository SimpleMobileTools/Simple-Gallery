package com.simplemobiletools.commons.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.isRPlus
import kotlinx.android.synthetic.main.dialog_create_new_folder.view.*
import java.io.File

class CreateNewFolderDialog(val activity: BaseSimpleActivity, val path: String, val callback: (path: String) -> Unit) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_create_new_folder, null)
        view.folder_path.setText("${activity.humanizePath(path).trimEnd('/')}/")

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.create_new_folder) { alertDialog ->
                    alertDialog.showKeyboard(view.folder_name)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
                        val name = view.folder_name.value
                        when {
                            name.isEmpty() -> activity.toast(R.string.empty_name)
                            name.isAValidFilename() -> {
                                val file = File(path, name)
                                if (file.exists()) {
                                    activity.toast(R.string.name_taken)
                                    return@OnClickListener
                                }

                                createFolder("$path/$name", alertDialog)
                            }
                            else -> activity.toast(R.string.invalid_name)
                        }
                    })
                }
            }
    }

    private fun createFolder(path: String, alertDialog: AlertDialog) {
        try {
            when {
                activity.isRestrictedSAFOnlyRoot(path) && activity.createAndroidSAFDirectory(path) -> sendSuccess(alertDialog, path)
                activity.isAccessibleWithSAFSdk30(path) -> activity.handleSAFDialogSdk30(path) {
                    if (it && activity.createSAFDirectorySdk30(path)) {
                        sendSuccess(alertDialog, path)
                    }
                }
                activity.needsStupidWritePermissions(path) -> activity.handleSAFDialog(path) {
                    if (it) {
                        try {
                            val documentFile = activity.getDocumentFile(path.getParentPath())
                            val newDir = documentFile?.createDirectory(path.getFilenameFromPath()) ?: activity.getDocumentFile(path)
                            if (newDir != null) {
                                sendSuccess(alertDialog, path)
                            } else {
                                activity.toast(R.string.unknown_error_occurred)
                            }
                        } catch (e: SecurityException) {
                            activity.showErrorToast(e)
                        }
                    }
                }
                File(path).mkdirs() -> sendSuccess(alertDialog, path)
                isRPlus() && activity.isAStorageRootFolder(path.getParentPath()) -> activity.handleSAFCreateDocumentDialogSdk30(path) {
                    if (it) {
                        sendSuccess(alertDialog, path)
                    }
                }
                else -> activity.toast(activity.getString(R.string.could_not_create_folder, path.getFilenameFromPath()))
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
        }
    }

    private fun sendSuccess(alertDialog: AlertDialog, path: String) {
        callback(path.trimEnd('/'))
        alertDialog.dismiss()
    }
}
