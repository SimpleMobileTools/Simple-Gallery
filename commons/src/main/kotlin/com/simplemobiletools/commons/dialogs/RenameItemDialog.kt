package com.simplemobiletools.commons.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_rename_item.view.*

class RenameItemDialog(val activity: BaseSimpleActivity, val path: String, val callback: (newPath: String) -> Unit) {
    init {
        var ignoreClicks = false
        val fullName = path.getFilenameFromPath()
        val dotAt = fullName.lastIndexOf(".")
        var name = fullName

        val view = activity.layoutInflater.inflate(R.layout.dialog_rename_item, null).apply {
            if (dotAt > 0 && !activity.getIsPathDirectory(path)) {
                name = fullName.substring(0, dotAt)
                val extension = fullName.substring(dotAt + 1)
                rename_item_extension.setText(extension)
            } else {
                rename_item_extension_hint.beGone()
            }

            rename_item_name.setText(name)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.rename) { alertDialog ->
                    alertDialog.showKeyboard(view.rename_item_name)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (ignoreClicks) {
                            return@setOnClickListener
                        }

                        var newName = view.rename_item_name.value
                        val newExtension = view.rename_item_extension.value

                        if (newName.isEmpty()) {
                            activity.toast(R.string.empty_name)
                            return@setOnClickListener
                        }

                        if (!newName.isAValidFilename()) {
                            activity.toast(R.string.invalid_name)
                            return@setOnClickListener
                        }

                        val updatedPaths = ArrayList<String>()
                        updatedPaths.add(path)
                        if (!newExtension.isEmpty()) {
                            newName += ".$newExtension"
                        }

                        if (!activity.getDoesFilePathExist(path)) {
                            activity.toast(String.format(activity.getString(R.string.source_file_doesnt_exist), path))
                            return@setOnClickListener
                        }

                        val newPath = "${path.getParentPath()}/$newName"

                        if (path == newPath) {
                            activity.toast(R.string.name_taken)
                            return@setOnClickListener
                        }

                        if (!path.equals(newPath, ignoreCase = true) && activity.getDoesFilePathExist(newPath)) {
                            activity.toast(R.string.name_taken)
                            return@setOnClickListener
                        }

                        updatedPaths.add(newPath)
                        ignoreClicks = true
                        activity.renameFile(path, newPath, false) { success, _ ->
                            ignoreClicks = false
                            if (success) {
                                callback(newPath)
                                alertDialog.dismiss()
                            }
                        }
                    }
                }
            }
    }
}
