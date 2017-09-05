package com.simplemobiletools.gallery.dialogs

import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.SLIDESHOW_DEFAULT_INTERVAL
import kotlinx.android.synthetic.main.dialog_slideshow.view.*


class SlideshowDialog(val activity: SimpleActivity, val callback: () -> Unit) {
    val dialog: AlertDialog
    val view: View

    init {
        view = LayoutInflater.from(activity).inflate(R.layout.dialog_slideshow, null).apply {
            interval_value.setOnClickListener {
                val text = interval_value.text
                if (text.isNotEmpty()) {
                    text.replace(0, 1, text.subSequence(0, 1), 0, 1)
                    interval_value.selectAll()
                }
            }

            interval_value.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus)
                    activity.hideKeyboard(v)
            }

            include_photos_holder.setOnClickListener {
                interval_value.clearFocus()
                include_photos.toggle()
            }

            include_videos_holder.setOnClickListener {
                interval_value.clearFocus()
                include_videos.toggle()
            }

            include_gifs_holder.setOnClickListener {
                interval_value.clearFocus()
                include_gifs.toggle()
            }

            random_order_holder.setOnClickListener {
                interval_value.clearFocus()
                random_order.toggle()
            }

            use_fade_holder.setOnClickListener {
                interval_value.clearFocus()
                use_fade.toggle()
            }

            move_backwards_holder.setOnClickListener {
                interval_value.clearFocus()
                move_backwards.toggle()
            }
        }
        setupValues()

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialog, which -> dialogConfirmed() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            activity.setupDialogStuff(view, this)
        }
    }

    private fun setupValues() {
        val config = activity.config
        view.apply {
            interval_value.setText(config.slideshowInterval.toString())
            include_photos.isChecked = config.slideshowIncludePhotos
            include_videos.isChecked = config.slideshowIncludeVideos
            include_gifs.isChecked = config.slideshowIncludeGIFs
            random_order.isChecked = config.slideshowRandomOrder
            use_fade.isChecked = config.slideshowUseFade
            move_backwards.isChecked = config.slideshowMoveBackwards
        }
    }

    private fun dialogConfirmed() {
        var interval = view.interval_value.text.toString()
        if (interval.trim('0').isEmpty())
            interval = SLIDESHOW_DEFAULT_INTERVAL.toString()

        activity.config.apply {
            slideshowInterval = interval.toInt()
            slideshowIncludePhotos = view.include_photos.isChecked
            slideshowIncludeVideos = view.include_videos.isChecked
            slideshowIncludeGIFs = view.include_gifs.isChecked
            slideshowRandomOrder = view.random_order.isChecked
            slideshowUseFade = view.use_fade.isChecked
            slideshowMoveBackwards = view.move_backwards.isChecked
        }
        dialog.dismiss()
        callback()
    }
}
