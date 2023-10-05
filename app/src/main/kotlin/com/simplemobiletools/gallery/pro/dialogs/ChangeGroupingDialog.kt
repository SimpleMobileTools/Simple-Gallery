package com.simplemobiletools.gallery.pro.dialogs

import android.content.DialogInterface
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.databinding.DialogChangeGroupingBinding
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.helpers.*

class ChangeGroupingDialog(val activity: BaseSimpleActivity, val path: String = "", val callback: () -> Unit) :
    DialogInterface.OnClickListener {
    private var currGrouping = 0
    private var config = activity.config
    private val pathToUse = if (path.isEmpty()) SHOW_ALL else path
    private val binding: DialogChangeGroupingBinding

    init {
        currGrouping = config.getFolderGrouping(pathToUse)
        binding = DialogChangeGroupingBinding.inflate(activity.layoutInflater).apply {
            groupingDialogUseForThisFolder.isChecked = config.hasCustomGrouping(pathToUse)
            groupingDialogRadioFolder.beVisibleIf(path.isEmpty())
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok, this)
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.group_by)
            }

        setupGroupRadio()
        setupOrderRadio()
        binding.groupingDialogShowFileCount.isChecked = currGrouping and GROUP_SHOW_FILE_COUNT != 0
    }

    private fun setupGroupRadio() {
        val groupBtn = when {
            currGrouping and GROUP_BY_NONE != 0 -> binding.groupingDialogRadioNone
            currGrouping and GROUP_BY_LAST_MODIFIED_DAILY != 0 -> binding.groupingDialogRadioLastModifiedDaily
            currGrouping and GROUP_BY_LAST_MODIFIED_MONTHLY != 0 -> binding.groupingDialogRadioLastModifiedMonthly
            currGrouping and GROUP_BY_DATE_TAKEN_DAILY != 0 -> binding.groupingDialogRadioDateTakenDaily
            currGrouping and GROUP_BY_DATE_TAKEN_MONTHLY != 0 -> binding.groupingDialogRadioDateTakenMonthly
            currGrouping and GROUP_BY_FILE_TYPE != 0 -> binding.groupingDialogRadioFileType
            currGrouping and GROUP_BY_EXTENSION != 0 -> binding.groupingDialogRadioExtension
            else -> binding.groupingDialogRadioFolder
        }
        groupBtn.isChecked = true
    }

    private fun setupOrderRadio() {
        var orderBtn = binding.groupingDialogRadioAscending

        if (currGrouping and GROUP_DESCENDING != 0) {
            orderBtn = binding.groupingDialogRadioDescending
        }
        orderBtn.isChecked = true
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val groupingRadio = binding.groupingDialogRadioGrouping
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

        if (binding.groupingDialogRadioOrder.checkedRadioButtonId == R.id.grouping_dialog_radio_descending) {
            grouping = grouping or GROUP_DESCENDING
        }

        if (binding.groupingDialogShowFileCount.isChecked) {
            grouping = grouping or GROUP_SHOW_FILE_COUNT
        }

        if (binding.groupingDialogUseForThisFolder.isChecked) {
            config.saveFolderGrouping(pathToUse, grouping)
        } else {
            config.removeFolderGrouping(pathToUse)
            config.groupBy = grouping
        }

        callback()
    }
}
