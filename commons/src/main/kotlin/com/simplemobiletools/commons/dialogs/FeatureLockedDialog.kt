package com.simplemobiletools.commons.dialogs

import android.app.Activity
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_feature_locked.view.*

class FeatureLockedDialog(val activity: Activity, val callback: () -> Unit) {
    private var dialog: AlertDialog? = null

    init {
        val view: View = activity.layoutInflater.inflate(R.layout.dialog_feature_locked, null)
        view.feature_locked_image.applyColorFilter(activity.getProperTextColor())

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.purchase, null)
            .setNegativeButton(R.string.later) { dialog, which -> dismissDialog() }
            .setOnDismissListener { dismissDialog() }
            .apply {
                activity.setupDialogStuff(view, this, cancelOnTouchOutside = false) { alertDialog ->
                    dialog = alertDialog
                    view.feature_locked_description.text = Html.fromHtml(activity.getString(R.string.features_locked))
                    view.feature_locked_description.movementMethod = LinkMovementMethod.getInstance()

                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        activity.launchPurchaseThankYouIntent()
                    }
                }
            }
    }

    fun dismissDialog() {
        dialog?.dismiss()
        callback()
    }
}
