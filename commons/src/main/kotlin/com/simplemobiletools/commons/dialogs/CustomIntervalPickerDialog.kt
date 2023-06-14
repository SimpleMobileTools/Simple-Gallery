package com.simplemobiletools.commons.dialogs

import android.app.Activity
import android.content.DialogInterface
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.DAY_SECONDS
import com.simplemobiletools.commons.helpers.HOUR_SECONDS
import com.simplemobiletools.commons.helpers.MINUTE_SECONDS
import kotlinx.android.synthetic.main.dialog_custom_interval_picker.view.*

class CustomIntervalPickerDialog(val activity: Activity, val selectedSeconds: Int = 0, val showSeconds: Boolean = false, val callback: (minutes: Int) -> Unit) {
    private var dialog: AlertDialog? = null
    private var view = (activity.layoutInflater.inflate(R.layout.dialog_custom_interval_picker, null) as ViewGroup)

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialogInterface, i -> confirmReminder() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.showKeyboard(view.findViewById(R.id.dialog_custom_interval_value))
                }
            }

        view.apply {
            dialog_radio_seconds.beVisibleIf(showSeconds)
            when {
                selectedSeconds == 0 -> dialog_radio_view.check(R.id.dialog_radio_minutes)
                selectedSeconds % DAY_SECONDS == 0 -> {
                    dialog_radio_view.check(R.id.dialog_radio_days)
                    dialog_custom_interval_value.setText((selectedSeconds / DAY_SECONDS).toString())
                }
                selectedSeconds % HOUR_SECONDS == 0 -> {
                    dialog_radio_view.check(R.id.dialog_radio_hours)
                    dialog_custom_interval_value.setText((selectedSeconds / HOUR_SECONDS).toString())
                }
                selectedSeconds % MINUTE_SECONDS == 0 -> {
                    dialog_radio_view.check(R.id.dialog_radio_minutes)
                    dialog_custom_interval_value.setText((selectedSeconds / MINUTE_SECONDS).toString())
                }
                else -> {
                    dialog_radio_view.check(R.id.dialog_radio_seconds)
                    dialog_custom_interval_value.setText(selectedSeconds.toString())
                }
            }

            dialog_custom_interval_value.setOnKeyListener(object : View.OnKeyListener {
                override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                        dialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.performClick()
                        return true
                    }

                    return false
                }
            })
        }
    }

    private fun confirmReminder() {
        val value = view.dialog_custom_interval_value.value
        val multiplier = getMultiplier(view.dialog_radio_view.checkedRadioButtonId)
        val minutes = Integer.valueOf(if (value.isEmpty()) "0" else value)
        callback(minutes * multiplier)
        activity.hideKeyboard()
        dialog?.dismiss()
    }

    private fun getMultiplier(id: Int) = when (id) {
        R.id.dialog_radio_days -> DAY_SECONDS
        R.id.dialog_radio_hours -> HOUR_SECONDS
        R.id.dialog_radio_minutes -> MINUTE_SECONDS
        else -> 1
    }
}
