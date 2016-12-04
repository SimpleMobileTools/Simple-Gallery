package com.simplemobiletools.gallery.dialogs

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.WindowManager
import com.simplemobiletools.filepicker.extensions.*
import com.simplemobiletools.gallery.helpers.Config
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import kotlinx.android.synthetic.main.rename_directory.view.*
import java.io.File
import java.util.*

class RenameDirectoryDialog(val activity: SimpleActivity, val dir: File, val callback: (changedPaths: ArrayList<String>) -> Unit) {
    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.rename_directory, null)

        view.directory_name.setText(dir.name)
        view.directory_path.text = "${activity.humanizePath(dir.parent)}/"

        AlertDialog.Builder(activity)
                .setTitle(activity.resources.getString(R.string.rename_folder))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            setCanceledOnTouchOutside(true)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                val newDirName = view.directory_name.value
                if (newDirName.isEmpty()) {
                    context.toast(R.string.rename_folder_empty)
                    return@setOnClickListener
                }

                if (!newDirName.isAValidFilename()) {
                    context.toast(R.string.invalid_name)
                    return@setOnClickListener
                }

                val updatedFiles = ArrayList<String>()
                updatedFiles.add(dir.absolutePath)
                val newDir = File(dir.parent, newDirName)

                if (newDir.exists()) {
                    context.toast(R.string.rename_folder_exists)
                    return@setOnClickListener
                }

                if (context.needsStupidWritePermissions(dir.absolutePath)) {
                    if (activity.isShowingPermDialog(dir))
                        return@setOnClickListener

                    val document = context.getFileDocument(dir.absolutePath, Config.newInstance(context).treeUri)
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
        activity.toast(R.string.renaming_folder)
        val files = newDir.listFiles()
        files.mapTo(updatedFiles) { it.absolutePath }

        updatedFiles.add(newDir.absolutePath)
        callback.invoke(updatedFiles)
    }
}
