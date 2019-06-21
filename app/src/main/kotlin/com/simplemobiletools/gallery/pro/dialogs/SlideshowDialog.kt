package com.simplemobiletools.gallery.pro.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.value
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.helpers.SLIDESHOW_ANIMATION_FADE
import com.simplemobiletools.gallery.pro.helpers.SLIDESHOW_ANIMATION_NONE
import com.simplemobiletools.gallery.pro.helpers.SLIDESHOW_ANIMATION_SLIDE
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

            animation_holder.setOnClickListener {
                val items = arrayListOf(
                        RadioItem(SLIDESHOW_ANIMATION_NONE, activity.getString(R.string.no_animation)),
                        RadioItem(SLIDESHOW_ANIMATION_SLIDE, activity.getString(R.string.slide)),
                        RadioItem(SLIDESHOW_ANIMATION_FADE, activity.getString(R.string.fade)))

                RadioGroupDialog(activity, items, activity.config.slideshowAnimation) {
                    activity.config.slideshowAnimation = it as Int
                    animation_value.text = getAnimationText()
                }
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
            animation_value.text = getAnimationText()
            include_videos.isChecked = config.slideshowIncludeVideos
            include_gifs.isChecked = config.slideshowIncludeGIFs
            random_order.isChecked = config.slideshowRandomOrder
            move_backwards.isChecked = config.slideshowMoveBackwards
            loop_slideshow.isChecked = config.loopSlideshow
        }
    }

    private fun storeValues() {
        var interval = view.interval_value.text.toString()
        if (interval.trim('0').isEmpty())
            interval = SLIDESHOW_DEFAULT_INTERVAL.toString()

        activity.config.apply {
            slideshowAnimation = getAnimationValue(view.animation_value.value)
            slideshowInterval = interval.toInt()
            slideshowIncludeVideos = view.include_videos.isChecked
            slideshowIncludeGIFs = view.include_gifs.isChecked
            slideshowRandomOrder = view.random_order.isChecked
            slideshowMoveBackwards = view.move_backwards.isChecked
            loopSlideshow = view.loop_slideshow.isChecked
        }
    }

    private fun getAnimationText(): String {
        return when (activity.config.slideshowAnimation) {
            SLIDESHOW_ANIMATION_SLIDE -> activity.getString(R.string.slide)
            SLIDESHOW_ANIMATION_FADE -> activity.getString(R.string.fade)
            else -> activity.getString(R.string.no_animation)
        }
    }

    private fun getAnimationValue(text: String): Int {
        return when (text) {
            activity.getString(R.string.slide) -> SLIDESHOW_ANIMATION_SLIDE
            activity.getString(R.string.fade) -> SLIDESHOW_ANIMATION_FADE
            else -> SLIDESHOW_ANIMATION_NONE
        }
    }
}
