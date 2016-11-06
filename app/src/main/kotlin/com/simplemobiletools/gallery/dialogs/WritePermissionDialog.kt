package com.simplemobiletools.gallery.dialogs

import android.content.Context
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import com.simplemobiletools.gallery.R

class WritePermissionDialog(val context: Context, val listener: OnWritePermissionListener) {
    var dialog: AlertDialog? = null

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_write_permission, null)

        dialog = AlertDialog.Builder(context)
                .setTitle(context.resources.getString(R.string.confirm_storage_access_title))
                .setView(view)
                .setPositiveButton(R.string.ok, { dialog, which -> dialogConfirmed() })
                .create()

        dialog?.show()
    }

    private fun dialogConfirmed() {
        dialog?.dismiss()
        listener.onConfirmed()
    }

    interface OnWritePermissionListener {
        fun onConfirmed()
    }
}
