package com.simplemobiletools.gallery.dialogs

import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.adapters.MediaAdapter
import com.simplemobiletools.gallery.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.getCachedMedia
import com.simplemobiletools.gallery.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.dialog_medium_picker.view.*

class PickMediumDialog(val activity: BaseSimpleActivity, val path: String, val callback: (path: String) -> Unit) {
    var dialog: AlertDialog
    var shownMedia = ArrayList<Medium>()
    val view = activity.layoutInflater.inflate(R.layout.dialog_medium_picker, null)
    var isGridViewType = activity.config.viewTypeFiles == VIEW_TYPE_GRID

    init {
        (view.media_grid.layoutManager as MyGridLayoutManager).apply {
            orientation = if (activity.config.scrollHorizontally && isGridViewType) GridLayoutManager.HORIZONTAL else GridLayoutManager.VERTICAL
            spanCount = if (isGridViewType) activity.config.mediaColumnCnt else 1
        }

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.other_folder, { dialogInterface, i -> showOtherFolder() })
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.select_photo)
                }

        activity.getCachedMedia(path) {
            val media = it.filter { !it.isVideo() } as ArrayList
            if (media.isNotEmpty()) {
                activity.runOnUiThread {
                    gotMedia(media)
                }
            }
        }

        GetMediaAsynctask(activity, path, true, false, false) {
            gotMedia(it)
        }.execute()
    }

    private fun showOtherFolder() {
        PickDirectoryDialog(activity, path) {
            callback(it)
            dialog.dismiss()
        }
    }

    private fun gotMedia(media: ArrayList<Medium>) {
        if (media.hashCode() == shownMedia.hashCode())
            return

        shownMedia = media
        val adapter = MediaAdapter(activity, media, null, true, false, view.media_grid) {
            callback((it as Medium).path)
            dialog.dismiss()
        }

        val scrollHorizontally = activity.config.scrollHorizontally && isGridViewType
        view.apply {
            media_grid.adapter = adapter

            media_vertical_fastscroller.isHorizontal = false
            media_vertical_fastscroller.beGoneIf(scrollHorizontally)

            media_horizontal_fastscroller.isHorizontal = true
            media_horizontal_fastscroller.beVisibleIf(scrollHorizontally)

            if (scrollHorizontally) {
                media_horizontal_fastscroller.allowBubbleDisplay = activity.config.showInfoBubble
                media_horizontal_fastscroller.setViews(media_grid) {
                    media_horizontal_fastscroller.updateBubbleText(media[it].getBubbleText())
                }
            } else {
                media_vertical_fastscroller.allowBubbleDisplay = activity.config.showInfoBubble
                media_vertical_fastscroller.setViews(media_grid) {
                    media_vertical_fastscroller.updateBubbleText(media[it].getBubbleText())
                }
            }
        }
    }
}
