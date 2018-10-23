package com.simplemobiletools.gallery.dialogs

import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.extensions.addTempFolderIfNeeded
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.getCachedDirectories
import com.simplemobiletools.gallery.extensions.getSortedDirectories
import com.simplemobiletools.gallery.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.dialog_directory_picker.view.*

class PickDirectoryDialog(val activity: BaseSimpleActivity, val sourcePath: String, val callback: (path: String) -> Unit) {
    var dialog: AlertDialog
    var shownDirectories = ArrayList<Directory>()
    var view = activity.layoutInflater.inflate(R.layout.dialog_directory_picker, null)
    var isGridViewType = activity.config.viewTypeFolders == VIEW_TYPE_GRID
    var showHidden = activity.config.shouldShowHidden

    init {
        (view.directories_grid.layoutManager as MyGridLayoutManager).apply {
            orientation = if (activity.config.scrollHorizontally && isGridViewType) RecyclerView.HORIZONTAL else RecyclerView.VERTICAL
            spanCount = if (isGridViewType) activity.config.dirColumnCnt else 1
        }

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.other_folder) { dialogInterface, i -> showOtherFolder() }
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.select_destination) {
                        view.directories_show_hidden.beVisibleIf(!context.config.shouldShowHidden)
                        view.directories_show_hidden.setOnClickListener {
                            activity.handleHiddenFolderPasswordProtection {
                                view.directories_show_hidden.beGone()
                                showHidden = true
                                fetchDirectories(true)
                            }
                        }
                    }
                }

        fetchDirectories(false)
    }

    private fun fetchDirectories(forceShowHidden: Boolean) {
        activity.getCachedDirectories(forceShowHidden = forceShowHidden) {
            if (it.isNotEmpty()) {
                activity.runOnUiThread {
                    gotDirectories(activity.addTempFolderIfNeeded(it))
                }
            }
        }
    }

    private fun showOtherFolder() {
        FilePickerDialog(activity, sourcePath, false, showHidden, true, true) {
            callback(it)
        }
    }

    private fun gotDirectories(newDirs: ArrayList<Directory>) {
        val dirs = activity.getSortedDirectories(newDirs)
        if (dirs.hashCode() == shownDirectories.hashCode())
            return

        shownDirectories = dirs
        val adapter = DirectoryAdapter(activity, dirs.clone() as ArrayList<Directory>, null, view.directories_grid, true) {
            if ((it as Directory).path.trimEnd('/') == sourcePath) {
                activity.toast(R.string.source_and_destination_same)
                return@DirectoryAdapter
            } else {
                callback(it.path)
                dialog.dismiss()
            }
        }

        val scrollHorizontally = activity.config.scrollHorizontally && isGridViewType
        val sorting = activity.config.directorySorting
        view.apply {
            directories_grid.adapter = adapter

            directories_vertical_fastscroller.isHorizontal = false
            directories_vertical_fastscroller.beGoneIf(scrollHorizontally)

            directories_horizontal_fastscroller.isHorizontal = true
            directories_horizontal_fastscroller.beVisibleIf(scrollHorizontally)

            if (scrollHorizontally) {
                directories_horizontal_fastscroller.allowBubbleDisplay = activity.config.showInfoBubble
                directories_horizontal_fastscroller.setViews(directories_grid) {
                    directories_horizontal_fastscroller.updateBubbleText(dirs[it].getBubbleText(sorting))
                }
            } else {
                directories_vertical_fastscroller.allowBubbleDisplay = activity.config.showInfoBubble
                directories_vertical_fastscroller.setViews(directories_grid) {
                    directories_vertical_fastscroller.updateBubbleText(dirs[it].getBubbleText(sorting))
                }
            }
        }
    }
}
