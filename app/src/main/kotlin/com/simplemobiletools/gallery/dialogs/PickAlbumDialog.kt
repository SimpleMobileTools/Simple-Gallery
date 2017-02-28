package com.simplemobiletools.gallery.dialogs

import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.extensions.config
import kotlinx.android.synthetic.main.dialog_album_picker.view.*

class PickAlbumDialog(val activity: SimpleActivity, val sourcePath: String, val callback: (path: String) -> Unit) {
    var directoriesGrid: RecyclerView

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_album_picker, null)
        directoriesGrid = view.directories_grid

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.other_folder, { dialogInterface, i -> showOtherFolder() })
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.select_destination)

            GetDirectoriesAsynctask(activity, false, false) {
                val adapter = DirectoryAdapter(activity, it, null) {
                    if (it.path.trimEnd('/') == sourcePath) {
                        activity.toast(R.string.source_and_destination_same)
                        return@DirectoryAdapter
                    } else {
                        callback.invoke(it.path)
                        dismiss()
                    }
                }
                directoriesGrid.adapter = adapter
            }.execute()
        }
    }

    fun showOtherFolder() {
        val showHidden = activity.config.showHiddenFolders
        FilePickerDialog(activity, sourcePath, false, showHidden, true) {
            callback.invoke(it)
        }
    }
}
