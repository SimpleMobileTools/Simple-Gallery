package com.simplemobiletools.gallery.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.WindowManager
import com.simplemobiletools.filepicker.dialogs.FilePickerDialog
import com.simplemobiletools.filepicker.extensions.*
import com.simplemobiletools.gallery.R
import kotlinx.android.synthetic.main.rename_file.view.*
import java.io.File

class SaveAsDialog(val activity: Activity, val path: String, val callback: (savePath: String) -> Unit) {

    init {
        var realPath = File(path).parent.trimEnd('/')
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_save_as, null)
        view.apply {
            file_path.text = activity.humanizePath(realPath)
            file_name.setText(path.getFilenameFromPath())

            file_path.setOnClickListener {
                FilePickerDialog(activity, realPath, false, false, listener = object : FilePickerDialog.OnFilePickerListener {
                    override fun onSuccess(pickedPath: String) {
                        file_path.text = activity.humanizePath(pickedPath)
                        realPath = pickedPath
                    }

                    override fun onFail(error: FilePickerDialog.FilePickerResult) {
                    }
                })
            }
        }

        AlertDialog.Builder(activity)
                .setTitle(activity.resources.getString(R.string.save_as))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            setCanceledOnTouchOutside(true)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                val filename = view.file_name.value

                if (filename.isEmpty()) {
                    context.toast(R.string.filename_cannot_be_empty)
                    return@setOnClickListener
                }

                if (!filename.isAValidFilename()) {
                    context.toast(R.string.filename_invalid_characters)
                    return@setOnClickListener
                }

                val newPath = File(realPath, filename).absolutePath
                callback.invoke(newPath)
                dismiss()
            })
        }
    }
}
