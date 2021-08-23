package com.simplemobiletools.gallery.pro.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.pro.R
import kotlinx.android.synthetic.main.dialog_save_as.view.*

class SaveAsDialog(val activity: BaseSimpleActivity, val path: String, val appendFilename: Boolean,
                   val showOverwriteOption: Boolean = false, val overwriteExisting: Boolean = false, val cancelCallback: (() -> Unit)? = null,
                   val callback: (savePath: String, overwriteExisting: Boolean) -> Unit) {

    init {
        var realPath = path.getParentPath()

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

            if (showOverwriteOption) {
                if (overwriteExisting) {
                    save_as_overwrite.isChecked = true
                    save_as_new.visibility = View.GONE
                }
                save_as_overwrite.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        activity.hideKeyboard(save_as_overwrite)
                        save_as_new.visibility = View.GONE
                    } else {
                        save_as_name.setSelection(save_as_name.text.toString().length)
                        activity.showKeyboard(save_as_name)
                        save_as_new.visibility = View.VISIBLE
                    }
                }
            } else {
                save_as_overwrite.visibility = View.GONE
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel) { dialog, which -> cancelCallback?.invoke() }
                .setOnCancelListener { cancelCallback?.invoke() }
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.save_as) {
                        if (!view.save_as_overwrite.isChecked)
                            showKeyboard(view.save_as_name)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val filename = view.save_as_name.value
                            val extension = view.save_as_extension.value

                            if (filename.isEmpty()) {
                                activity.toast(R.string.filename_cannot_be_empty)
                                return@setOnClickListener
                            }

                            if (extension.isEmpty()) {
                                activity.toast(R.string.extension_cannot_be_empty)
                                return@setOnClickListener
                            }

                            if (view.save_as_overwrite.isChecked) {
                                callback(path, true)
                                dismiss()
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
                                    callback(newPath, false)
                                    dismiss()
                                }
                            } else {
                                callback(newPath, false)
                                dismiss()
                            }
                        }
                    }
                }
    }
}
