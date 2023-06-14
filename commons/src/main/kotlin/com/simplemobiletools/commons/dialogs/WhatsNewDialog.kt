package com.simplemobiletools.commons.dialogs

import android.app.Activity
import android.view.LayoutInflater
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.models.Release
import kotlinx.android.synthetic.main.dialog_whats_new.view.*

class WhatsNewDialog(val activity: Activity, val releases: List<Release>) {
    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_whats_new, null)
        view.whats_new_content.text = getNewReleases()

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.whats_new, cancelOnTouchOutside = false)
            }
    }

    private fun getNewReleases(): String {
        val sb = StringBuilder()

        releases.forEach {
            val parts = activity.getString(it.textId).split("\n").map(String::trim)
            parts.forEach {
                sb.append("- $it\n")
            }
        }

        return sb.toString()
    }
}
