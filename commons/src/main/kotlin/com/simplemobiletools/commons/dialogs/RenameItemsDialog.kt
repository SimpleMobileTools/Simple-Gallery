package com.simplemobiletools.commons.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_rename_items.view.*

// used at renaming folders
class RenameItemsDialog(val activity: BaseSimpleActivity, val paths: ArrayList<String>, val callback: () -> Unit) {
    init {
        var ignoreClicks = false
        val view = activity.layoutInflater.inflate(R.layout.dialog_rename_items, null)

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.rename) { alertDialog ->
                    alertDialog.showKeyboard(view.rename_items_value)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (ignoreClicks) {
                            return@setOnClickListener
                        }

                        val valueToAdd = view.rename_items_value.text.toString()
                        val append = view.rename_items_radio_group.checkedRadioButtonId == view.rename_items_radio_append.id

                        if (valueToAdd.isEmpty()) {
                            callback()
                            alertDialog.dismiss()
                            return@setOnClickListener
                        }

                        if (!valueToAdd.isAValidFilename()) {
                            activity.toast(R.string.invalid_name)
                            return@setOnClickListener
                        }

                        val validPaths = paths.filter { activity.getDoesFilePathExist(it) }
                        val sdFilePath = validPaths.firstOrNull { activity.isPathOnSD(it) } ?: validPaths.firstOrNull()
                        if (sdFilePath == null) {
                            activity.toast(R.string.unknown_error_occurred)
                            alertDialog.dismiss()
                            return@setOnClickListener
                        }

                        activity.handleSAFDialog(sdFilePath) {
                            if (!it) {
                                return@handleSAFDialog
                            }

                            ignoreClicks = true
                            var pathsCnt = validPaths.size
                            for (path in validPaths) {
                                val fullName = path.getFilenameFromPath()
                                var dotAt = fullName.lastIndexOf(".")
                                if (dotAt == -1) {
                                    dotAt = fullName.length
                                }

                                val name = fullName.substring(0, dotAt)
                                val extension = if (fullName.contains(".")) ".${fullName.getFilenameExtension()}" else ""

                                val newName = if (append) {
                                    "$name$valueToAdd$extension"
                                } else {
                                    "$valueToAdd$fullName"
                                }

                                val newPath = "${path.getParentPath()}/$newName"

                                if (activity.getDoesFilePathExist(newPath)) {
                                    continue
                                }

                                activity.renameFile(path, newPath, true) { success, _ ->
                                    if (success) {
                                        pathsCnt--
                                        if (pathsCnt == 0) {
                                            callback()
                                            alertDialog.dismiss()
                                        }
                                    } else {
                                        ignoreClicks = false
                                        alertDialog.dismiss()
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}
