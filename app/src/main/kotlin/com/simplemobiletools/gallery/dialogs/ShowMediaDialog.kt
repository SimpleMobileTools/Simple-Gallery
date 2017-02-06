package com.simplemobiletools.gallery.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.RadioGroup
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.IMAGES
import com.simplemobiletools.gallery.helpers.IMAGES_AND_VIDEOS
import com.simplemobiletools.gallery.helpers.VIDEOS
import kotlinx.android.synthetic.main.dialog_show_media.view.*

class ShowMediaDialog(val activity: Activity, val callback: (newValue: Int) -> Unit) : AlertDialog.Builder(activity), RadioGroup.OnCheckedChangeListener {
    val dialog: AlertDialog?

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_show_media, null).dialog_radio_view.apply {
            check(getSavedItem())
            setOnCheckedChangeListener(this@ShowMediaDialog)
        }

        dialog = AlertDialog.Builder(activity)
                .create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        callback.invoke(getNewValue(checkedId))
        dialog?.dismiss()
    }

    fun getNewValue(id: Int) = when (id) {
        R.id.dialog_radio_images -> IMAGES
        R.id.dialog_radio_videos -> VIDEOS
        else -> IMAGES_AND_VIDEOS
    }

    fun getSavedItem() = when (activity.config.showMedia) {
        IMAGES -> R.id.dialog_radio_images
        VIDEOS -> R.id.dialog_radio_videos
        else -> R.id.dialog_radio_images_and_videos
    }
}
