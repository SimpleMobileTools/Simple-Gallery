package com.gallery.raw.dialogs

import android.view.View
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.helpers.VIEW_TYPE_LIST
import com.gallery.raw.R
import com.gallery.raw.extensions.config
import com.gallery.raw.helpers.SHOW_ALL
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
            change_view_type_dialog_group_direct_subfolders.apply {
                beVisibleIf(fromFoldersView)
                isChecked = config.groupDirectSubfolders
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
            config.groupDirectSubfolders = view.change_view_type_dialog_group_direct_subfolders.isChecked
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
