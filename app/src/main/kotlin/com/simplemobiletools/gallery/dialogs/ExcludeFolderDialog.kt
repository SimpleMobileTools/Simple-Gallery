package com.simplemobiletools.gallery.dialogs

import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import kotlinx.android.synthetic.main.dialog_exclude_folder.view.*

class ExcludeFolderDialog(val activity: SimpleActivity, val selectedPaths: HashSet<String>, val callback: () -> Unit) {
    var dialog: AlertDialog? = null

    init {
        val alternativePaths = getAlternativePaths()
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_exclude_folder, null).apply {
            exclude_folder_parent.beVisibleIf(alternativePaths.size > 1)
            exclude_folder_radio_group.beVisibleIf(alternativePaths.size > 1)
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialog, which -> dialogConfirmed() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    private fun dialogConfirmed() {

    }

    private fun getAlternativePaths(): ArrayList<String> {
        val parentsList = ArrayList<String>()
        if (selectedPaths.size > 1)
            return parentsList

        return parentsList
    }
}
