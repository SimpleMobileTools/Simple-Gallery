package com.simplemobiletools.gallery.pro.dialogs

import android.view.View
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.launchGrantAllFilesIntent
import kotlinx.android.synthetic.main.dialog_grant_all_files.view.*

class GrantAllFilesDialog(val activity: BaseSimpleActivity) {
    init {
        val view: View = activity.layoutInflater.inflate(R.layout.dialog_grant_all_files, null)
        view.grant_all_files_image.applyColorFilter(activity.getProperTextColor())

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> activity.launchGrantAllFilesIntent() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, cancelOnTouchOutside = false) { alertDialog -> }
            }
    }
}
