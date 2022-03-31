package com.simplemobiletools.gallery.pro.dialogs

import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.adapters.MediaAdapter
import com.simplemobiletools.gallery.pro.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.getCachedMedia
import com.simplemobiletools.gallery.pro.helpers.GridSpacingItemDecoration
import com.simplemobiletools.gallery.pro.helpers.SHOW_ALL
import com.simplemobiletools.gallery.pro.models.Medium
import com.simplemobiletools.gallery.pro.models.ThumbnailItem
import com.simplemobiletools.gallery.pro.models.ThumbnailSection
import kotlinx.android.synthetic.main.dialog_medium_picker.view.*

class PickMediumDialog(val activity: BaseSimpleActivity, val path: String, val callback: (path: String) -> Unit) {
    var dialog: AlertDialog
    var shownMedia = ArrayList<ThumbnailItem>()
    val view = activity.layoutInflater.inflate(R.layout.dialog_medium_picker, null)
    val config = activity.config
    val viewType = config.getFolderViewType(if (config.showAll) SHOW_ALL else path)
    var isGridViewType = viewType == VIEW_TYPE_GRID

    init {
        (view.media_grid.layoutManager as MyGridLayoutManager).apply {
            orientation = if (config.scrollHorizontally && isGridViewType) RecyclerView.HORIZONTAL else RecyclerView.VERTICAL
            spanCount = if (isGridViewType) config.mediaColumnCnt else 1
        }

        view.media_fastscroller.updateColors(activity.getProperPrimaryColor())

        dialog = AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.other_folder) { dialogInterface, i -> showOtherFolder() }
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.select_photo)
            }

        activity.getCachedMedia(path) {
            val media = it.filter { it is Medium } as ArrayList
            if (media.isNotEmpty()) {
                activity.runOnUiThread {
                    gotMedia(media)
                }
            }
        }

        GetMediaAsynctask(activity, path, false, false, false) {
            gotMedia(it)
        }.execute()
    }

    private fun showOtherFolder() {
        PickDirectoryDialog(activity, path, true, true) {
            callback(it)
            dialog.dismiss()
        }
    }

    private fun gotMedia(media: ArrayList<ThumbnailItem>) {
        if (media.hashCode() == shownMedia.hashCode())
            return

        shownMedia = media
        val adapter = MediaAdapter(activity, shownMedia.clone() as ArrayList<ThumbnailItem>, null, true, false, path, view.media_grid) {
            if (it is Medium) {
                callback(it.path)
                dialog.dismiss()
            }
        }

        val scrollHorizontally = config.scrollHorizontally && isGridViewType
        view.apply {
            media_grid.adapter = adapter
            media_fastscroller.setScrollVertically(!scrollHorizontally)
        }
        handleGridSpacing(media)
    }

    private fun handleGridSpacing(media: ArrayList<ThumbnailItem>) {
        if (isGridViewType) {
            val spanCount = config.mediaColumnCnt
            val spacing = config.thumbnailSpacing
            val useGridPosition = media.firstOrNull() is ThumbnailSection

            var currentGridDecoration: GridSpacingItemDecoration? = null
            if (view.media_grid.itemDecorationCount > 0) {
                currentGridDecoration = view.media_grid.getItemDecorationAt(0) as GridSpacingItemDecoration
                currentGridDecoration.items = media
            }

            val newGridDecoration = GridSpacingItemDecoration(spanCount, spacing, config.scrollHorizontally, config.fileRoundedCorners, media, useGridPosition)
            if (currentGridDecoration.toString() != newGridDecoration.toString()) {
                if (currentGridDecoration != null) {
                    view.media_grid.removeItemDecoration(currentGridDecoration)
                }
                view.media_grid.addItemDecoration(newGridDecoration)
            }
        }
    }
}
