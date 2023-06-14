package com.gallery.raw.dialogs

import android.content.DialogInterface
import android.view.View
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.models.RadioItem
import com.gallery.raw.R
import com.gallery.raw.extensions.config
import kotlinx.android.synthetic.main.dialog_change_file_thumbnail_style.view.*

class ChangeFileThumbnailStyleDialog(val activity: BaseSimpleActivity) : DialogInterface.OnClickListener {
    private var config = activity.config
    private var view: View
    private var thumbnailSpacing = config.thumbnailSpacing

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_change_file_thumbnail_style, null).apply {
            dialog_file_style_rounded_corners.isChecked = config.fileRoundedCorners
            dialog_file_style_animate_gifs.isChecked = config.animateGifs
            dialog_file_style_show_thumbnail_video_duration.isChecked = config.showThumbnailVideoDuration
            dialog_file_style_show_thumbnail_file_types.isChecked = config.showThumbnailFileTypes
            dialog_file_style_mark_favorite_items.isChecked = config.markFavoriteItems

            dialog_file_style_rounded_corners_holder.setOnClickListener { dialog_file_style_rounded_corners.toggle() }
            dialog_file_style_animate_gifs_holder.setOnClickListener { dialog_file_style_animate_gifs.toggle() }
            dialog_file_style_show_thumbnail_video_duration_holder.setOnClickListener { dialog_file_style_show_thumbnail_video_duration.toggle() }
            dialog_file_style_show_thumbnail_file_types_holder.setOnClickListener { dialog_file_style_show_thumbnail_file_types.toggle() }
            dialog_file_style_mark_favorite_items_holder.setOnClickListener { dialog_file_style_mark_favorite_items.toggle() }

            dialog_file_style_spacing_holder.setOnClickListener {
                val items = arrayListOf(
                    RadioItem(0, "0x"),
                    RadioItem(1, "1x"),
                    RadioItem(2, "2x"),
                    RadioItem(4, "4x"),
                    RadioItem(8, "8x"),
                    RadioItem(16, "16x"),
                    RadioItem(32, "32x"),
                    RadioItem(64, "64x")
                )

                RadioGroupDialog(activity, items, thumbnailSpacing) {
                    thumbnailSpacing = it as Int
                    updateThumbnailSpacingText()
                }
            }
        }

        updateThumbnailSpacingText()

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, this)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this)
            }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        config.fileRoundedCorners = view.dialog_file_style_rounded_corners.isChecked
        config.animateGifs = view.dialog_file_style_animate_gifs.isChecked
        config.showThumbnailVideoDuration = view.dialog_file_style_show_thumbnail_video_duration.isChecked
        config.showThumbnailFileTypes = view.dialog_file_style_show_thumbnail_file_types.isChecked
        config.markFavoriteItems = view.dialog_file_style_mark_favorite_items.isChecked
        config.thumbnailSpacing = thumbnailSpacing
    }

    private fun updateThumbnailSpacingText() {
        view.dialog_file_style_spacing.text = "${thumbnailSpacing}x"
    }
}
