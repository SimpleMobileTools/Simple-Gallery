package com.simplemobiletools.gallery.dialogs

import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.view.LayoutInflater
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.getCachedDirectories
import com.simplemobiletools.gallery.extensions.getSortedDirectories
import com.simplemobiletools.gallery.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.dialog_directory_picker.view.*

class PickDirectoryDialog(val activity: SimpleActivity, val sourcePath: String, val callback: (path: String) -> Unit) {
    var dialog: AlertDialog
    var shownDirectories = ArrayList<Directory>()
    var view = LayoutInflater.from(activity).inflate(R.layout.dialog_directory_picker, null)
    var isGridViewType = activity.config.viewTypeFolders == VIEW_TYPE_GRID

    init {
        (view.directories_grid.layoutManager as GridLayoutManager).apply {
            orientation = if (activity.config.scrollHorizontally && isGridViewType) GridLayoutManager.HORIZONTAL else GridLayoutManager.VERTICAL
            spanCount = if (isGridViewType) activity.config.dirColumnCnt else 1
        }

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.other_folder, { dialogInterface, i -> showOtherFolder() })
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.select_destination)

            val dirs = activity.getCachedDirectories()
            if (dirs.isNotEmpty()) {
                gotDirectories(activity.addTempFolderIfNeeded(dirs))
            }

            GetDirectoriesAsynctask(activity, false, false) {
                gotDirectories(activity.addTempFolderIfNeeded(it))
            }.execute()
        }
    }

    private fun showOtherFolder() {
        val showHidden = activity.config.shouldShowHidden
        FilePickerDialog(activity, sourcePath, false, showHidden, true) {
            callback(it)
        }
    }

    private fun gotDirectories(newDirs: ArrayList<Directory>) {
        val dirs = activity.getSortedDirectories(newDirs)
        if (dirs.hashCode() == shownDirectories.hashCode())
            return

        shownDirectories = dirs
        val adapter = DirectoryAdapter(activity, dirs, null, true) {
            if (it.path.trimEnd('/') == sourcePath) {
                activity.toast(R.string.source_and_destination_same)
                return@DirectoryAdapter
            } else {
                callback(it.path)
                dialog.dismiss()
            }
        }

        val scrollHorizontally = activity.config.scrollHorizontally && isGridViewType
        view.apply {
            directories_grid.adapter = adapter

            directories_vertical_fastscroller.isHorizontal = false
            directories_vertical_fastscroller.beGoneIf(scrollHorizontally)

            directories_horizontal_fastscroller.isHorizontal = true
            directories_horizontal_fastscroller.beVisibleIf(scrollHorizontally)

            if (scrollHorizontally) {
                directories_horizontal_fastscroller.setViews(directories_grid)
            } else {
                directories_vertical_fastscroller.setViews(directories_grid)
            }
        }
    }
}
