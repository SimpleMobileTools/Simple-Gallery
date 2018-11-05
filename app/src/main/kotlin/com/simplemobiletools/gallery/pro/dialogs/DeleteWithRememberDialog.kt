package com.simplemobiletools.gallery.pro.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.pro.R
import kotlinx.android.synthetic.main.dialog_delete_with_remember.view.*

class DeleteWithRememberDialog(val activity: Activity, val message: String, val callback: (remember: Boolean) -> Unit) {
    private var dialog: AlertDialog
    val view = activity.layoutInflater.inflate(R.layout.dialog_delete_with_remember, null)!!

    init {
        view.delete_remember_title.text = message
        val builder = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.yes) { dialog, which -> dialogConfirmed() }
                .setNegativeButton(R.string.no, null)

        dialog = builder.create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    private fun dialogConfirmed() {
        dialog.dismiss()
        callback(view.delete_remember_checkbox.isChecked)
    }
}
