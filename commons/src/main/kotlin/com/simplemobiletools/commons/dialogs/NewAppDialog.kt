package com.simplemobiletools.commons.dialogs

import android.app.Activity
import android.text.Html
import android.text.method.LinkMovementMethod
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_textview.view.*

class NewAppDialog(val activity: Activity, val packageName: String, val title: String, val packageName2: String, val title2: String) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_textview, null).apply {
            val text = String.format(
                activity.getString(R.string.new_app),
                "https://play.google.com/store/apps/details?id=$packageName", title,
                "https://play.google.com/store/apps/details?id=$packageName2", title2
            )

            text_view.text = Html.fromHtml(text)
            text_view.movementMethod = LinkMovementMethod.getInstance()
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .apply {
                activity.setupDialogStuff(view, this, cancelOnTouchOutside = false)
            }
    }
}
