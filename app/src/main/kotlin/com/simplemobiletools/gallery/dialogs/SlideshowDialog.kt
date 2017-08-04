package com.simplemobiletools.gallery.dialogs

import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.WindowManager
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import kotlinx.android.synthetic.main.dialog_slideshow.view.*


class SlideshowDialog(val activity: SimpleActivity, val callback: () -> Unit) {
    var dialog: AlertDialog

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_slideshow, null).apply {
            interval_value.setOnClickListener {
                val text = interval_value.text
                if (text.isNotEmpty()) {
                    text.replace(0, 1, text.subSequence(0, 1), 0, 1)
                    interval_value.selectAll()
                }
            }

            include_videos_holder.setOnClickListener {
                include_videos.toggle()
            }

            random_order_holder.setOnClickListener {
                random_order.toggle()
            }

            use_fade_holder.setOnClickListener {
                use_fade.toggle()
            }
        }

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialog, which -> dialogConfirmed() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            activity.setupDialogStuff(view, this)
            currentFocus?.clearFocus()
        }
    }

    private fun dialogConfirmed() {

    }
}
