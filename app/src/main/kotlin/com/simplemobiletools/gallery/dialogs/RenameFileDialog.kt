package com.simplemobiletools.gallery.dialogs

import android.app.Activity
import android.media.MediaScannerConnection
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.WindowManager
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.Utils
import com.simplemobiletools.gallery.extensions.toast
import com.simplemobiletools.gallery.extensions.value
import kotlinx.android.synthetic.main.rename_file.view.*
import java.io.File

class RenameFileDialog(val activity: Activity, val file: File, val listener: OnRenameFileListener) {

    init {
        val context = activity
        val view = LayoutInflater.from(context).inflate(R.layout.rename_file, null)
        val fullName = file.name
        val dotAt = fullName.lastIndexOf(".")
        var name = fullName

        if (dotAt > 0) {
            name = fullName.substring(0, dotAt)
            val extension = fullName.substring(dotAt + 1)
            view.file_extension.setText(extension)
        }

        view.file_name.setText(name)
        view.file_path.text = "${file.parent}/"

        AlertDialog.Builder(context)
                .setTitle(context.resources.getString(R.string.rename_file))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                val fileName = view.file_name.value
                val extension = view.file_extension.value

                if (fileName.isEmpty() || extension.isEmpty()) {
                    context.toast(R.string.filename_cannot_be_empty)
                    return@setOnClickListener
                }

                val newFile = File(file.parent, "$fileName.$extension")

                if (Utils.needsStupidWritePermissions(context, file.absolutePath)) {
                    if (Utils.isShowingWritePermissions(activity, file))
                        return@setOnClickListener

                    val document = Utils.Companion.getFileDocument(context, file.absolutePath)
                    if (document.canWrite())
                        document.renameTo(newFile.name)
                    sendSuccess(file, newFile)
                    dismiss()
                } else if (file.renameTo(newFile)) {
                    sendSuccess(file, newFile)
                    dismiss()
                } else {
                    context.toast(R.string.rename_file_error)
                }
            })
        }
    }

    private fun sendSuccess(currFile: File, newFile: File) {
        val changedFiles = arrayOf(currFile.absolutePath, newFile.absolutePath)
        MediaScannerConnection.scanFile(activity.applicationContext, changedFiles, null, null)
        listener.onRenameFileSuccess(newFile)
    }

    interface OnRenameFileListener {
        fun onRenameFileSuccess(newFile: File)
    }
}
