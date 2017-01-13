package com.simplemobiletools.gallery.dialogs

import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.*
import kotlinx.android.synthetic.main.dialog_change_sorting.view.*

class ChangeSortingDialog(val activity: SimpleActivity, val isDirectorySorting: Boolean, val callback: () -> Unit) : DialogInterface.OnClickListener {
    companion object {
        private var currSorting = 0

        lateinit var config: Config
        lateinit var view: View
    }

    init {
        config = activity.config
        view = LayoutInflater.from(activity).inflate(R.layout.dialog_change_sorting, null)

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.sort_by)
        }

        currSorting = if (isDirectorySorting) config.directorySorting else config.fileSorting
        setupSortRadio()
        setupOrderRadio()
    }

    private fun setupSortRadio() {
        val sortingRadio = view.sorting_dialog_radio_sorting
        var sortBtn = sortingRadio.sorting_dialog_radio_name

        if (currSorting and SORT_BY_SIZE != 0) {
            sortBtn = sortingRadio.sorting_dialog_radio_size
        } else if (currSorting and SORT_BY_DATE_MODIFIED != 0) {
            sortBtn = sortingRadio.sorting_dialog_radio_last_modified
        } else if (currSorting and SORT_BY_DATE_TAKEN != 0)
            sortBtn = sortingRadio.sorting_dialog_radio_date_taken
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
            R.id.sorting_dialog_radio_size -> SORT_BY_SIZE
            R.id.sorting_dialog_radio_last_modified -> SORT_BY_DATE_MODIFIED
            else -> SORT_BY_DATE_TAKEN
        }

        if (view.sorting_dialog_radio_order.checkedRadioButtonId == R.id.sorting_dialog_radio_descending) {
            sorting = sorting or SORT_DESCENDING
        }

        if (isDirectorySorting) {
            config.directorySorting = sorting
        } else {
            config.fileSorting = sorting
        }
        callback.invoke()
    }
}
