package com.simplemobiletools.gallery.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import com.simplemobiletools.gallery.*
import com.simplemobiletools.gallery.helpers.*
import kotlinx.android.synthetic.main.dialog_change_sorting.view.*

class ChangeSortingDialog(val activity: Activity, val isDirectorySorting: Boolean, val callback: () -> Unit) : DialogInterface.OnClickListener {
    companion object {
        private var currSorting = 0

        lateinit var config: Config
        lateinit var view: View
    }

    init {
        config = Config.newInstance(activity)
        view = LayoutInflater.from(activity).inflate(R.layout.dialog_change_sorting, null)

        val dialog = AlertDialog.Builder(activity)
                .setTitle(activity.resources.getString(R.string.sort_by))
                .setView(view)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, null)
                .create()

        dialog.setCanceledOnTouchOutside(true)
        dialog.show()

        currSorting = if (isDirectorySorting) config.directorySorting else config.sorting
        setupSortRadio()
        setupOrderRadio()
    }

    private fun setupSortRadio() {
        val sortingRadio = view.sorting_dialog_radio_sorting
        var sortBtn = sortingRadio.sorting_dialog_radio_name

        if (currSorting and SORT_BY_DATE != 0) {
            sortBtn = sortingRadio.sorting_dialog_radio_date
        } else if (currSorting and SORT_BY_SIZE != 0) {
            sortBtn = sortingRadio.sorting_dialog_radio_size
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
            R.id.sorting_dialog_radio_date -> SORT_BY_DATE
            else -> SORT_BY_SIZE
        }

        if (view.sorting_dialog_radio_order.checkedRadioButtonId == R.id.sorting_dialog_radio_descending) {
            sorting = sorting or SORT_DESCENDING
        }

        if (isDirectorySorting) {
            config.directorySorting = sorting
        } else {
            config.sorting = sorting
        }
        callback.invoke()
    }
}
