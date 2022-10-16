package com.simplemobiletools.gallery.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.pro.R
import kotlinx.android.synthetic.main.dialog_confirm_delete_folder.view.*

class AllFilesPermissionDialog(
    val activity: BaseSimpleActivity, message: String = "", val callback: (result: Boolean) -> Unit, val neutralPressed: () -> Unit
) {
    private var dialog: AlertDialog? = null

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_message, null)
        view.message.text = message

        activity.getAlertDialogBuilder().setPositiveButton(R.string.all_files) { dialog, which -> positivePressed() }
            .setNeutralButton(R.string.media_only) { dialog, which -> neutralPressed() }
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun positivePressed() {
        dialog?.dismiss()
        callback(true)
    }
}
