package com.simplemobiletools.commons.dialogs

import android.app.Activity
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_textview.view.*

class FolderLockingNoticeDialog(val activity: Activity, val callback: () -> Unit) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_textview, null).apply {
            text_view.text = activity.getString(R.string.lock_folder_notice)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.disclaimer)
            }
    }

    private fun dialogConfirmed() {
        activity.baseConfig.wasFolderLockingNoticeShown = true
        callback()
    }
}
