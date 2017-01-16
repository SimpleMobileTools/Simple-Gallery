package com.simplemobiletools.gallery.dialogs

import android.support.v4.util.Pair
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import com.simplemobiletools.commons.asynctasks.CopyMoveTask
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.extensions.config
import kotlinx.android.synthetic.main.dialog_copy_move.view.*
import java.io.File
import java.util.*

class CopyDialog(val activity: SimpleActivity, val files: ArrayList<File>, val copyMoveListener: CopyMoveTask.CopyMoveListener) {
    companion object {
        lateinit var view: View
    }

    init {
        view = LayoutInflater.from(activity).inflate(R.layout.dialog_copy_move, null)
        val sourcePath = files[0].parent.trimEnd('/')
        var destinationPath = ""

        view.destination.setOnClickListener {
            PickAlbumDialog(activity) {
                destinationPath = it
                view.destination.text = activity.humanizePath(it)
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.copy_move)
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                if (destinationPath == context.resources.getString(R.string.select_destination) || destinationPath.isEmpty()) {
                    context.toast(R.string.please_select_destination)
                    return@setOnClickListener
                }

                if (sourcePath.trimEnd('/') == destinationPath.trimEnd('/')) {
                    context.toast(R.string.source_and_destination_same)
                    return@setOnClickListener
                }

                val destinationDir = File(destinationPath)
                if (!destinationDir.exists()) {
                    context.toast(R.string.invalid_destination)
                    return@setOnClickListener
                }

                if (files.size == 1) {
                    if (File(destinationPath, files[0].name).exists()) {
                        context.toast(R.string.file_exists)
                        return@setOnClickListener
                    }
                }

                if (activity.isShowingPermDialog(destinationDir)) {
                    return@setOnClickListener
                }

                if (view.dialog_radio_group.checkedRadioButtonId == R.id.dialog_radio_copy) {
                    context.toast(R.string.copying)
                    val pair = Pair<ArrayList<File>, File>(files, destinationDir)
                    CopyMoveTask(context, false, context.config.treeUri, true, copyMoveListener).execute(pair)
                    dismiss()
                } else {
                    if (context.isPathOnSD(sourcePath) || context.isPathOnSD(destinationPath)) {
                        if (activity.isShowingPermDialog(files[0])) {
                            return@setOnClickListener
                        }

                        context.toast(R.string.moving)
                        val pair = Pair<ArrayList<File>, File>(files, destinationDir)
                        CopyMoveTask(context, true, context.config.treeUri, true, copyMoveListener).execute(pair)
                        dismiss()
                    } else {
                        val updatedFiles = ArrayList<File>(files.size * 2)
                        updatedFiles.addAll(files)
                        for (file in files) {
                            val destination = File(destinationDir, file.name)
                            if (file.renameTo(destination))
                                updatedFiles.add(destination)
                        }
                        context.scanFiles(updatedFiles) {
                            activity.runOnUiThread {
                                copyMoveListener.copySucceeded(true, files.size * 2 == updatedFiles.size)
                                dismiss()
                            }
                        }
                    }
                }
            })
        }
    }
}
