package com.simplemobiletools.gallery.pro.dialogs

import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.config
import kotlinx.android.synthetic.main.dialog_change_file_thumbnail_style.view.*

class ChangeFileThumbnailStyleDialog(val activity: BaseSimpleActivity) : DialogInterface.OnClickListener {
    private var config = activity.config
    private var view: View

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_change_file_thumbnail_style, null).apply {
            dialog_file_style_animate_gifs.isChecked = config.animateGifs
            dialog_file_style_show_thumbnail_video_duration.isChecked = config.showThumbnailVideoDuration
            dialog_file_style_show_thumbnail_file_types.isChecked = config.showThumbnailFileTypes

            dialog_file_style_animate_gifs_holder.setOnClickListener { dialog_file_style_animate_gifs.toggle() }
            dialog_file_style_show_thumbnail_video_duration_holder.setOnClickListener { dialog_file_style_show_thumbnail_video_duration.toggle() }
            dialog_file_style_show_thumbnail_file_types_holder.setOnClickListener { dialog_file_style_show_thumbnail_file_types.toggle() }
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, this)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this)
            }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        config.animateGifs = view.dialog_file_style_animate_gifs.isChecked
        config.showThumbnailVideoDuration = view.dialog_file_style_show_thumbnail_video_duration.isChecked
        config.showThumbnailFileTypes = view.dialog_file_style_show_thumbnail_file_types.isChecked
    }
}
