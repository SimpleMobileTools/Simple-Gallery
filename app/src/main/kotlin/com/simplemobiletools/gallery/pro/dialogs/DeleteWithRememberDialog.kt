package com.simplemobiletools.gallery.pro.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.pro.R
import kotlinx.android.synthetic.main.dialog_delete_with_remember.view.*

class DeleteWithRememberDialog(
    private val activity: Activity,
    private val message: String,
    private val showSkipRecycleBinOption: Boolean,
    private val callback: (remember: Boolean, skipRecycleBin: Boolean) -> Unit
) {

    private var dialog: AlertDialog? = null
    val view = activity.layoutInflater.inflate(R.layout.dialog_delete_with_remember, null)!!

    init {
        view.delete_remember_title.text = message
        view.skip_the_recycle_bin_checkbox.beGoneIf(!showSkipRecycleBinOption)
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
        callback(view.delete_remember_checkbox.isChecked, view.skip_the_recycle_bin_checkbox.isChecked)
    }
}
