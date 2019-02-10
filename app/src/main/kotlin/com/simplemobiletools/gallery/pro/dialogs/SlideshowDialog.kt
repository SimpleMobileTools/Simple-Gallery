package com.simplemobiletools.gallery.pro.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.helpers.SLIDESHOW_DEFAULT_INTERVAL
import kotlinx.android.synthetic.main.dialog_slideshow.view.*

class SlideshowDialog(val activity: BaseSimpleActivity, val callback: () -> Unit) {
    val view: View

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_slideshow, null).apply {
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

            loop_slideshow_holder.setOnClickListener {
                interval_value.clearFocus()
                loop_slideshow.toggle()
            }
        }
        setupValues()

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this) {
                        hideKeyboard()
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            storeValues()
                            callback()
                            dismiss()
                        }
                    }
                }
    }

    private fun setupValues() {
        val config = activity.config
        view.apply {
            interval_value.setText(config.slideshowInterval.toString())
            include_videos.isChecked = config.slideshowIncludeVideos
            include_gifs.isChecked = config.slideshowIncludeGIFs
            random_order.isChecked = config.slideshowRandomOrder
            use_fade.isChecked = config.slideshowUseFade
            move_backwards.isChecked = config.slideshowMoveBackwards
            loop_slideshow.isChecked = config.loopSlideshow
        }
    }

    private fun storeValues() {
        var interval = view.interval_value.text.toString()
        if (interval.trim('0').isEmpty())
            interval = SLIDESHOW_DEFAULT_INTERVAL.toString()

        activity.config.apply {
            slideshowInterval = interval.toInt()
            slideshowIncludeVideos = view.include_videos.isChecked
            slideshowIncludeGIFs = view.include_gifs.isChecked
            slideshowRandomOrder = view.random_order.isChecked
            slideshowUseFade = view.use_fade.isChecked
            slideshowMoveBackwards = view.move_backwards.isChecked
            loopSlideshow = view.loop_slideshow.isChecked
        }
    }
}
