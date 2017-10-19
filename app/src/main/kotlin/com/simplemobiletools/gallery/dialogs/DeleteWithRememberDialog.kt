package com.simplemobiletools.gallery.dialogs

import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.R
import kotlinx.android.synthetic.main.dialog_delete_with_remember.view.*

class DeleteWithRememberDialog(val context: Context, val callback: (Boolean) -> Unit) {
    var dialog: AlertDialog
    val view = LayoutInflater.from(context).inflate(R.layout.dialog_delete_with_remember, null)

    init {
        val builder = AlertDialog.Builder(context)
                .setPositiveButton(R.string.yes, { dialog, which -> dialogConfirmed() })
                .setNegativeButton(R.string.no, null)

        dialog = builder.create().apply {
            context.setupDialogStuff(view, this)
        }
    }

    private fun dialogConfirmed() {
        dialog.dismiss()
        callback(view.delete_remember_checkbox.isChecked)
    }
}
