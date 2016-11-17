package com.simplemobiletools.gallery.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.WindowManager
import com.simplemobiletools.filepicker.extensions.getFilenameFromPath
import com.simplemobiletools.filepicker.extensions.isAValidFilename
import com.simplemobiletools.filepicker.extensions.toast
import com.simplemobiletools.filepicker.extensions.value
import com.simplemobiletools.gallery.R
import kotlinx.android.synthetic.main.rename_file.view.*

class SaveAsDialog(val activity: Activity, val path: String, val listener: OnSaveAsListener) {

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_save_as, null)
        view.file_name.setText(path.getFilenameFromPath())

        AlertDialog.Builder(activity)
                .setTitle(activity.resources.getString(R.string.save_as))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
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

                listener.onSaveAsSuccess(filename)
                dismiss()
            })
        }
    }

    interface OnSaveAsListener {
        fun onSaveAsSuccess(filename: String)
    }
}
