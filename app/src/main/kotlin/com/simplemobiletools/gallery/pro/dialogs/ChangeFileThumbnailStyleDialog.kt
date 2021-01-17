package com.simplemobiletools.gallery.pro.dialogs

import android.content.DialogInterface
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.config
import kotlinx.android.synthetic.main.dialog_change_file_thumbnail_style.view.*
import java.text.DecimalFormat

class ChangeFileThumbnailStyleDialog(val activity: BaseSimpleActivity) : DialogInterface.OnClickListener {
    private var config = activity.config
    private var view: View
    private var thumbnailSpacing = config.thumbnailSpacing

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_change_file_thumbnail_style, null).apply {
            dialog_file_style_animate_gifs.isChecked = config.animateGifs
            dialog_file_style_show_thumbnail_video_duration.isChecked = config.showThumbnailVideoDuration
            dialog_file_style_show_thumbnail_file_types.isChecked = config.showThumbnailFileTypes

            dialog_file_style_animate_gifs_holder.setOnClickListener { dialog_file_style_animate_gifs.toggle() }
            dialog_file_style_show_thumbnail_video_duration_holder.setOnClickListener { dialog_file_style_show_thumbnail_video_duration.toggle() }
            dialog_file_style_show_thumbnail_file_types_holder.setOnClickListener { dialog_file_style_show_thumbnail_file_types.toggle() }

            dialog_file_style_spacing_holder.setOnClickListener {
                val items = arrayListOf(
                    RadioItem(0, "0x"),
                    RadioItem(1, "0.5x"),
                    RadioItem(2, "1x"),
                    RadioItem(4, "2x"),
                    RadioItem(8, "4x"),
                    RadioItem(16, "8x"),
                    RadioItem(32, "16x"))

                RadioGroupDialog(activity, items, thumbnailSpacing) {
                    thumbnailSpacing = it as Int
                    updateThumbnailSpacingText()
                }
            }
        }
        updateThumbnailSpacingText()

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
        config.thumbnailSpacing = thumbnailSpacing
    }

    private fun updateThumbnailSpacingText() {
        val number = thumbnailSpacing * 0.5
        val format = DecimalFormat("0.#")
        view.dialog_file_style_spacing.text = "${format.format(number)}x"
    }
}
