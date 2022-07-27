package com.simplemobiletools.gallery.pro.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.pro.R
import kotlinx.android.synthetic.main.dialog_confirm_delete_folder.view.*

class ConfirmDeleteFolderDialog(activity: Activity, message: String, warningMessage: String, val callback: () -> Unit) {
    private var dialog: AlertDialog? = null

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_confirm_delete_folder, null)
        view.message.text = message
        view.message_warning.text = warningMessage

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.yes) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.no, null)
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun dialogConfirmed() {
        dialog?.dismiss()
        callback()
    }
}
