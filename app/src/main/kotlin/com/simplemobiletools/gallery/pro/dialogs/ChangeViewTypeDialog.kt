package com.simplemobiletools.gallery.pro.dialogs

import android.view.View
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.helpers.VIEW_TYPE_LIST
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.helpers.DIRECTORY_GROUPING_DIRECT_SUBFOLDERS
import com.simplemobiletools.gallery.pro.helpers.DIRECTORY_GROUPING_FILE_STRUCTURE
import com.simplemobiletools.gallery.pro.helpers.DIRECTORY_GROUPING_NONE
import com.simplemobiletools.gallery.pro.helpers.SHOW_ALL
import kotlinx.android.synthetic.main.dialog_change_view_type.view.*

class ChangeViewTypeDialog(val activity: BaseSimpleActivity, val fromFoldersView: Boolean, val path: String = "", val callback: () -> Unit) {
    private var view: View
    private var config = activity.config
    private var pathToUse = if (path.isEmpty()) SHOW_ALL else path

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_change_view_type, null).apply {
            val viewToCheck = if (fromFoldersView) {
                if (config.viewTypeFolders == VIEW_TYPE_GRID) {
                    change_view_type_dialog_radio_grid.id
                } else {
                    change_view_type_dialog_radio_list.id
                }
            } else {
                val currViewType = config.getFolderViewType(pathToUse)
                if (currViewType == VIEW_TYPE_GRID) {
                    change_view_type_dialog_radio_grid.id
                } else {
                    change_view_type_dialog_radio_list.id
                }
            }

            change_view_type_dialog_radio.check(viewToCheck)

            val groupingToCheck = when (config.directoryGrouping) {
                DIRECTORY_GROUPING_DIRECT_SUBFOLDERS -> grouping_dialog_radio_direct_subfolders.id
                DIRECTORY_GROUPING_FILE_STRUCTURE -> grouping_dialog_radio_file_structure.id
                else -> grouping_dialog_radio_none.id
            }
            grouping_dialog_radio.apply {
                beVisibleIf(fromFoldersView)
                check(groupingToCheck)
            }

            change_view_type_dialog_use_for_this_folder.apply {
                beVisibleIf(!fromFoldersView)
                isChecked = config.hasCustomViewType(pathToUse)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        val viewType = if (view.change_view_type_dialog_radio.checkedRadioButtonId == view.change_view_type_dialog_radio_grid.id) {
            VIEW_TYPE_GRID
        } else {
            VIEW_TYPE_LIST
        }

        if (fromFoldersView) {
            config.viewTypeFolders = viewType
            config.directoryGrouping = when (view.grouping_dialog_radio.checkedRadioButtonId) {
                view.grouping_dialog_radio_direct_subfolders.id -> DIRECTORY_GROUPING_DIRECT_SUBFOLDERS
                view.grouping_dialog_radio_file_structure.id -> DIRECTORY_GROUPING_FILE_STRUCTURE
                else -> DIRECTORY_GROUPING_NONE
            }
        } else {
            if (view.change_view_type_dialog_use_for_this_folder.isChecked) {
                config.saveFolderViewType(pathToUse, viewType)
            } else {
                config.removeFolderViewType(pathToUse)
                config.viewTypeFiles = viewType
            }
        }


        callback()
    }
}
