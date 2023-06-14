package com.simplemobiletools.commons.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_rate_stars.view.*

class RateStarsDialog(val activity: Activity) {
    private var dialog: AlertDialog? = null

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_rate_stars, null).apply {
            val primaryColor = activity.getProperPrimaryColor()
            arrayOf(rate_star_1, rate_star_2, rate_star_3, rate_star_4, rate_star_5).forEach {
                it.applyColorFilter(primaryColor)
            }

            rate_star_1.setOnClickListener { dialogCancelled(true) }
            rate_star_2.setOnClickListener { dialogCancelled(true) }
            rate_star_3.setOnClickListener { dialogCancelled(true) }
            rate_star_4.setOnClickListener { dialogCancelled(true) }
            rate_star_5.setOnClickListener {
                activity.redirectToRateUs()
                dialogCancelled(true)
            }
        }

        activity.getAlertDialogBuilder()
            .setNegativeButton(R.string.later) { dialog, which -> dialogCancelled(false) }
            .setOnCancelListener { dialogCancelled(false) }
            .apply {
                activity.setupDialogStuff(view, this, cancelOnTouchOutside = false) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun dialogCancelled(showThankYou: Boolean) {
        dialog?.dismiss()
        if (showThankYou) {
            activity.toast(R.string.thank_you)
            activity.baseConfig.wasAppRated = true
        }
    }
}
