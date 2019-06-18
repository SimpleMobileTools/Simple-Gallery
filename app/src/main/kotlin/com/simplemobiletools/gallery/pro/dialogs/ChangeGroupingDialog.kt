package com.simplemobiletools.gallery.pro.dialogs

import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.helpers.*
import kotlinx.android.synthetic.main.dialog_change_grouping.view.*

class ChangeGroupingDialog(val activity: BaseSimpleActivity, val path: String = "", val callback: () -> Unit) :
        DialogInterface.OnClickListener {
    private var currGrouping = 0
    private var config = activity.config
    private val pathToUse = if (path.isEmpty()) SHOW_ALL else path
    private var view: View

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_change_grouping, null).apply {
            grouping_dialog_use_for_this_folder.isChecked = config.hasCustomGrouping(pathToUse)
            grouping_dialog_radio_folder.beVisibleIf(path.isEmpty())
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.group_by)
                }

        currGrouping = config.getFolderGrouping(pathToUse)
        setupGroupRadio()
        setupOrderRadio()
    }

    private fun setupGroupRadio() {
        val groupingRadio = view.grouping_dialog_radio_grouping

        val groupBtn = when {
            currGrouping and GROUP_BY_NONE != 0 -> groupingRadio.grouping_dialog_radio_none
            currGrouping and GROUP_BY_LAST_MODIFIED_DAILY != 0 -> groupingRadio.grouping_dialog_radio_last_modified_daily
            currGrouping and GROUP_BY_LAST_MODIFIED_MONTHLY != 0 -> groupingRadio.grouping_dialog_radio_last_modified_monthly
            currGrouping and GROUP_BY_DATE_TAKEN_DAILY != 0 -> groupingRadio.grouping_dialog_radio_date_taken_daily
            currGrouping and GROUP_BY_DATE_TAKEN_MONTHLY != 0 -> groupingRadio.grouping_dialog_radio_date_taken_monthly
            currGrouping and GROUP_BY_FILE_TYPE != 0 -> groupingRadio.grouping_dialog_radio_file_type
            currGrouping and GROUP_BY_EXTENSION != 0 -> groupingRadio.grouping_dialog_radio_extension
            else -> groupingRadio.grouping_dialog_radio_folder
        }
        groupBtn.isChecked = true
    }

    private fun setupOrderRadio() {
        val orderRadio = view.grouping_dialog_radio_order
        var orderBtn = orderRadio.grouping_dialog_radio_ascending

        if (currGrouping and GROUP_DESCENDING != 0) {
            orderBtn = orderRadio.grouping_dialog_radio_descending
        }
        orderBtn.isChecked = true
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val groupingRadio = view.grouping_dialog_radio_grouping
        var grouping = when (groupingRadio.checkedRadioButtonId) {
            R.id.grouping_dialog_radio_none -> GROUP_BY_NONE
            R.id.grouping_dialog_radio_last_modified_daily -> GROUP_BY_LAST_MODIFIED_DAILY
            R.id.grouping_dialog_radio_last_modified_monthly -> GROUP_BY_LAST_MODIFIED_MONTHLY
            R.id.grouping_dialog_radio_date_taken_daily -> GROUP_BY_DATE_TAKEN_DAILY
            R.id.grouping_dialog_radio_date_taken_monthly -> GROUP_BY_DATE_TAKEN_MONTHLY
            R.id.grouping_dialog_radio_file_type -> GROUP_BY_FILE_TYPE
            R.id.grouping_dialog_radio_extension -> GROUP_BY_EXTENSION
            else -> GROUP_BY_FOLDER
        }

        if (view.grouping_dialog_radio_order.checkedRadioButtonId == R.id.grouping_dialog_radio_descending) {
            grouping = grouping or GROUP_DESCENDING
        }

        if (view.grouping_dialog_use_for_this_folder.isChecked) {
            config.saveFolderGrouping(pathToUse, grouping)
        } else {
            config.removeFolderGrouping(pathToUse)
            config.groupBy = grouping
        }
        callback()
    }
}
