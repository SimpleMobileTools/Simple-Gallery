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
import com.simplemobiletools.gallery.extensions.getCachedDirectories
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.dialog_album_picker.view.*

class PickDirectoryDialog(val activity: SimpleActivity, val sourcePath: String, val callback: (path: String) -> Unit) {
    var dialog: AlertDialog
    var directoriesGrid: RecyclerView
    var shownDirectories: ArrayList<Directory> = ArrayList()

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_album_picker, null)
        directoriesGrid = view.directories_grid

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.other_folder, { dialogInterface, i -> showOtherFolder() })
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.select_destination)

            val dirs = activity.getCachedDirectories()
            if (dirs.isNotEmpty()) {
                gotDirectories(dirs)
            }

            GetDirectoriesAsynctask(activity, false, false) {
                gotDirectories(it)
            }.execute()
        }
    }

    fun showOtherFolder() {
        val showHidden = activity.config.shouldShowHidden
        FilePickerDialog(activity, sourcePath, false, showHidden, true) {
            callback.invoke(it)
        }
    }

    private fun gotDirectories(directories: ArrayList<Directory>) {
        if (directories.hashCode() == shownDirectories.hashCode())
            return

        shownDirectories = directories
        val adapter = DirectoryAdapter(activity, directories, null) {
            if (it.path.trimEnd('/') == sourcePath) {
                activity.toast(R.string.source_and_destination_same)
                return@DirectoryAdapter
            } else {
                callback.invoke(it.path)
                dialog.dismiss()
            }
        }
        directoriesGrid.adapter = adapter
    }
}
