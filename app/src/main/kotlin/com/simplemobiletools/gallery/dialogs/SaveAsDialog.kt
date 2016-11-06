package com.simplemobiletools.gallery.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.WindowManager
import com.simplemobiletools.filepicker.extensions.getFilenameFromPath
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.isNameValid
import com.simplemobiletools.gallery.extensions.toast
import com.simplemobiletools.gallery.extensions.value
import kotlinx.android.synthetic.main.rename_file.view.*

class SaveAsDialog(val activity: Activity, val path: String, val listener: OnSaveAsListener) {

    init {
        val context = activity
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_save_as, null)
        view.file_name.setText(path.getFilenameFromPath())

        AlertDialog.Builder(context)
                .setTitle(context.resources.getString(R.string.save_as))
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

                if (!filename.isNameValid()) {
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
