package com.simplemobiletools.gallery.dialogs

import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.view.View
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import kotlinx.android.synthetic.main.dialog_change_sorting.view.*

class ChangeSortingDialog(val activity: BaseSimpleActivity, val isDirectorySorting: Boolean, showFolderCheckbox: Boolean,
                          val path: String = "", val callback: () -> Unit) :
        DialogInterface.OnClickListener {
    private var currSorting = 0
    private var config = activity.config
    private var view: View

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_change_sorting, null).apply {
            use_for_this_folder_divider.beVisibleIf(showFolderCheckbox)
            sorting_dialog_use_for_this_folder.beVisibleIf(showFolderCheckbox)
            sorting_dialog_use_for_this_folder.isChecked = config.hasCustomSorting(path)
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.sort_by)
                }

        currSorting = if (isDirectorySorting) config.directorySorting else config.getFileSorting(path)
        setupSortRadio()
        setupOrderRadio()
    }

    private fun setupSortRadio() {
        val sortingRadio = view.sorting_dialog_radio_sorting

        val sortBtn = when {
            currSorting and SORT_BY_PATH != 0 -> sortingRadio.sorting_dialog_radio_path
            currSorting and SORT_BY_SIZE != 0 -> sortingRadio.sorting_dialog_radio_size
            currSorting and SORT_BY_DATE_MODIFIED != 0 -> sortingRadio.sorting_dialog_radio_last_modified
            else -> sortingRadio.sorting_dialog_radio_name
        }
        sortBtn.isChecked = true
    }

    private fun setupOrderRadio() {
        val orderRadio = view.sorting_dialog_radio_order
        var orderBtn = orderRadio.sorting_dialog_radio_ascending

        if (currSorting and SORT_DESCENDING != 0) {
            orderBtn = orderRadio.sorting_dialog_radio_descending
        }
        orderBtn.isChecked = true
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val sortingRadio = view.sorting_dialog_radio_sorting
        var sorting = when (sortingRadio.checkedRadioButtonId) {
            R.id.sorting_dialog_radio_name -> SORT_BY_NAME
            R.id.sorting_dialog_radio_path -> SORT_BY_PATH
            R.id.sorting_dialog_radio_size -> SORT_BY_SIZE
            else -> SORT_BY_DATE_MODIFIED
        }

        if (view.sorting_dialog_radio_order.checkedRadioButtonId == R.id.sorting_dialog_radio_descending) {
            sorting = sorting or SORT_DESCENDING
        }

        if (isDirectorySorting) {
            config.directorySorting = sorting
        } else {
            if (view.sorting_dialog_use_for_this_folder.isChecked) {
                config.saveFileSorting(path, sorting)
            } else {
                config.removeFileSorting(path)
                config.sorting = sorting
            }
        }
        callback()
    }
}
