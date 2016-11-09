package com.simplemobiletools.gallery.dialogs

import android.app.Activity
import android.support.v4.util.Pair
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.WindowManager
import com.simplemobiletools.filepicker.extensions.humanizePath
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.Utils
import com.simplemobiletools.gallery.asynctasks.CopyTask
import com.simplemobiletools.gallery.extensions.toast
import kotlinx.android.synthetic.main.copy_item.view.*
import java.io.File

class CopyDialog(val activity: Activity, val files: List<File>, val copyListener: CopyTask.CopyDoneListener, val listener: OnCopyListener) {

    init {
        val context = activity
        val view = LayoutInflater.from(context).inflate(R.layout.copy_item, null)
        val sourcePath = files[0].parent.trimEnd('/')
        var destinationPath = ""

        view.source.text = context.humanizePath(sourcePath)

        view.destination.setOnClickListener {
            PickAlbumDialog(activity, object : PickAlbumDialog.OnPickAlbumListener {
                override fun onSuccess(path: String) {
                    destinationPath = path
                    view.destination.text = context.humanizePath(path)
                }
            })
        }

        AlertDialog.Builder(context)
                .setTitle(context.resources.getString(if (files.size == 1) R.string.copy_item else R.string.copy_items))
                .setView(view)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            show()
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                if (destinationPath == context.resources.getString(R.string.select_destination) || destinationPath.isEmpty()) {
                    context.toast(R.string.please_select_destination)
                    return@setOnClickListener
                }

                if (view.source.text.trimEnd('/') == view.destination.text.trimEnd('/')) {
                    context.toast(R.string.source_and_destination_same)
                    return@setOnClickListener
                }

                val destinationDir = File(destinationPath)
                if (!destinationDir.exists()) {
                    context.toast(R.string.invalid_destination)
                    return@setOnClickListener
                }

                if (files.size == 1) {
                    val newFile = File(files[0].path)
                    if (File(destinationPath, newFile.name).exists()) {
                        context.toast(R.string.already_exists)
                        return@setOnClickListener
                    }
                }

                if (Utils.isShowingWritePermissions(context, destinationDir)) {
                    return@setOnClickListener
                }

                //if (view.dialog_radio_group.checkedRadioButtonId == R.id.dialog_radio_copy) {
                    context.toast(R.string.copying)
                    val pair = Pair<List<File>, File>(files, destinationDir)
                    CopyTask(copyListener, context).execute(pair)
                    dismiss()
                /*} else {
                    if (Utils.isPathOnSD(context, sourcePath) && Utils.isPathOnSD(context, destinationPath)) {
                        val paths = ArrayList<String>()
                        for (f in files) {
                            val destination = File(destinationDir, f.name)
                            f.renameTo(destination)
                            paths.add(destination.absolutePath)
                        }

                        context.scanFile(paths.toTypedArray())

                        dismiss()
                        listener.onSuccess()
                    } else {
                        val pair = Pair<List<File>, File>(files, destinationDir)
                        CopyTask(copyListener, context).execute(pair)
                        dismiss()
                    }
                }*/
            })
        }
    }

    interface OnCopyListener {
        fun onSuccess()
    }
}
