package org.fossify.gallery.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.gallery.databinding.DialogConfirmDeleteFolderBinding

class ConfirmDeleteFolderDialog(activity: Activity, message: String, warningMessage: String, val callback: () -> Unit) {
    private var dialog: AlertDialog? = null

    init {
        val binding = DialogConfirmDeleteFolderBinding.inflate(activity.layoutInflater)
        binding.message.text = message
        binding.messageWarning.text = warningMessage

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.yes) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(org.fossify.commons.R.string.no, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun dialogConfirmed() {
        dialog?.dismiss()
        callback()
    }
}
