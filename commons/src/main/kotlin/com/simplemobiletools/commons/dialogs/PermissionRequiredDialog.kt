package com.simplemobiletools.commons.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.openNotificationSettings
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_message.view.*

class PermissionRequiredDialog(val activity: Activity, textId: Int) {
    private var dialog: AlertDialog? = null

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_message, null)
        view.message.text = activity.getString(textId)

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.grant_permission) { dialog, which -> activity.openNotificationSettings() }
            .setNegativeButton(R.string.cancel, null).apply {
                val title = activity.getString(R.string.permission_required)
                activity.setupDialogStuff(view, this, titleText = title) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }
}
