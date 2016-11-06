package com.simplemobiletools.gallery.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.WindowManager
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.Utils
import com.simplemobiletools.gallery.extensions.toast
import com.simplemobiletools.gallery.extensions.value
import kotlinx.android.synthetic.main.rename_directory.view.*
import java.io.File
import java.util.*

class RenameDirectoryDialog(val activity: Activity, val dir: File, val listener: OnRenameDirListener) {
    val context = activity

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.rename_directory, null)

        view.directory_name.setText(dir.name)
        view.directory_path.text = "${dir.parent}/"

        AlertDialog.Builder(context)
                .setTitle(context.resources.getString(R.string.rename_folder))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                val newDirName = view.directory_name.value

                if (newDirName.isEmpty()) {
                    context.toast(R.string.rename_folder_empty)
                    return@setOnClickListener
                }

                val updatedFiles = ArrayList<String>()
                updatedFiles.add(dir.absolutePath)
                val newDir = File(dir.parent, newDirName)

                if (Utils.needsStupidWritePermissions(context, dir.absolutePath)) {
                    if (Utils.isShowingWritePermissions(activity, dir))
                        return@setOnClickListener

                    val document = Utils.Companion.getFileDocument(context, dir.absolutePath)
                    if (document.canWrite())
                        document.renameTo(newDirName)
                    sendSuccess(updatedFiles, newDir)
                    dismiss()
                } else if (dir.renameTo(newDir)) {
                    sendSuccess(updatedFiles, newDir)
                    dismiss()
                } else {
                    context.toast(R.string.rename_folder_error)
                }
            })
        }
    }

    private fun sendSuccess(updatedFiles: ArrayList<String>, newDir: File) {
        context.toast(R.string.renaming_folder)
        val files = newDir.listFiles()
        for (file in files) {
            updatedFiles.add(file.absolutePath)
        }

        updatedFiles.add(newDir.absolutePath)
        val changedFiles = updatedFiles.toTypedArray()
        listener.onRenameDirSuccess(changedFiles)
    }

    interface OnRenameDirListener {
        fun onRenameDirSuccess(changedFiles: Array<String>)
    }
}
