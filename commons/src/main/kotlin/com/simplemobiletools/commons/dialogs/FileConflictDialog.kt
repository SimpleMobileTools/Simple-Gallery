package com.simplemobiletools.commons.dialogs

import android.app.Activity
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.R.id.conflict_dialog_radio_keep_both
import com.simplemobiletools.commons.R.id.conflict_dialog_radio_merge
import com.simplemobiletools.commons.R.id.conflict_dialog_radio_skip
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.CONFLICT_KEEP_BOTH
import com.simplemobiletools.commons.helpers.CONFLICT_MERGE
import com.simplemobiletools.commons.helpers.CONFLICT_OVERWRITE
import com.simplemobiletools.commons.helpers.CONFLICT_SKIP
import com.simplemobiletools.commons.models.FileDirItem
import kotlinx.android.synthetic.main.dialog_file_conflict.view.*

class FileConflictDialog(
    val activity: Activity, val fileDirItem: FileDirItem, val showApplyToAllCheckbox: Boolean,
    val callback: (resolution: Int, applyForAll: Boolean) -> Unit
) {
    val view = activity.layoutInflater.inflate(R.layout.dialog_file_conflict, null)!!

    init {
        view.apply {
            val stringBase = if (fileDirItem.isDirectory) R.string.folder_already_exists else R.string.file_already_exists
            conflict_dialog_title.text = String.format(activity.getString(stringBase), fileDirItem.name)
            conflict_dialog_apply_to_all.isChecked = activity.baseConfig.lastConflictApplyToAll
            conflict_dialog_apply_to_all.beVisibleIf(showApplyToAllCheckbox)
            conflict_dialog_divider.beVisibleIf(showApplyToAllCheckbox)
            conflict_dialog_radio_merge.beVisibleIf(fileDirItem.isDirectory)

            val resolutionButton = when (activity.baseConfig.lastConflictResolution) {
                CONFLICT_OVERWRITE -> conflict_dialog_radio_overwrite
                CONFLICT_MERGE -> conflict_dialog_radio_merge
                else -> conflict_dialog_radio_skip
            }
            resolutionButton.isChecked = true
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        val resolution = when (view.conflict_dialog_radio_group.checkedRadioButtonId) {
            conflict_dialog_radio_skip -> CONFLICT_SKIP
            conflict_dialog_radio_merge -> CONFLICT_MERGE
            conflict_dialog_radio_keep_both -> CONFLICT_KEEP_BOTH
            else -> CONFLICT_OVERWRITE
        }

        val applyToAll = view.conflict_dialog_apply_to_all.isChecked
        activity.baseConfig.apply {
            lastConflictApplyToAll = applyToAll
            lastConflictResolution = resolution
        }

        callback(resolution, applyToAll)
    }
}
