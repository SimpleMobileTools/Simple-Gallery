package com.simplemobiletools.gallery.dialogs

import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.GIFS
import com.simplemobiletools.gallery.helpers.IMAGES
import com.simplemobiletools.gallery.helpers.VIDEOS
import kotlinx.android.synthetic.main.dialog_filter_media.view.*

class FilterMediaDialog(val activity: SimpleActivity, val callback: (result: Int) -> Unit) {
    private var view: View = LayoutInflater.from(activity).inflate(R.layout.dialog_filter_media, null)

    init {
        val filterMedia = activity.config.filterMedia
        view.apply {
            filter_media_images.isChecked = filterMedia and IMAGES != 0
            filter_media_videos.isChecked = filterMedia and VIDEOS != 0
            filter_media_gifs.isChecked = filterMedia and GIFS != 0
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialog, which -> dialogConfirmed() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.filter_media)
        }
    }

    private fun dialogConfirmed() {
        var result = 0
        if (view.filter_media_images.isChecked)
            result += IMAGES
        if (view.filter_media_videos.isChecked)
            result += VIDEOS
        if (view.filter_media_gifs.isChecked)
            result += GIFS

        activity.config.filterMedia = result
        callback(result)
    }
}
