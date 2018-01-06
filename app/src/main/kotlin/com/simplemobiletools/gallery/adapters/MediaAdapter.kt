package com.simplemobiletools.gallery.adapters

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.isActivityDestroyed
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.dialogs.DeleteWithRememberDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.VIEW_TYPE_LIST
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.photo_video_item_grid.view.*
import java.io.File
import java.util.*

class MediaAdapter(activity: BaseSimpleActivity, var media: MutableList<Medium>, val listener: MediaOperationsListener?, val isAGetIntent: Boolean,
                   val allowMultiplePicks: Boolean, recyclerView: MyRecyclerView, fastScroller: FastScroller? = null,
                   itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {

    private val config = activity.config
    private val isListViewType = config.viewTypeFiles == VIEW_TYPE_LIST
    private var skipConfirmationDialog = false
    private var visibleItemPaths = ArrayList<String>()
    private var delayHandler = Handler(Looper.getMainLooper())

    private var scrollHorizontally = config.scrollHorizontally
    private var animateGifs = config.animateGifs
    private var cropThumbnails = config.cropThumbnails
    private var displayFilenames = config.displayFileNames

    override fun getActionMenuId() = R.menu.cab_media

    override fun prepareItemSelection(view: View) {
        view.medium_check?.background?.applyColorFilter(primaryColor)
    }

    override fun markItemSelection(select: Boolean, view: View?) {
        view?.medium_check?.beVisibleIf(select)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val layoutType = if (isListViewType) R.layout.photo_video_item_list else R.layout.photo_video_item_grid
        return createViewHolder(layoutType, parent)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val medium = media[position]
        visibleItemPaths.add(medium.path)
        val view = holder.bindView(medium, !allowMultiplePicks) { itemView, layoutPosition ->
            setupView(itemView, medium)
        }
        bindViewHolder(holder, position, view)
    }

    override fun getItemCount() = media.size

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_rename).isVisible = isOneItemSelected()
            findItem(R.id.cab_open_with).isVisible = isOneItemSelected()
            findItem(R.id.cab_confirm_selection).isVisible = isAGetIntent && allowMultiplePicks && selectedPositions.size > 0

            checkHideBtnVisibility(this)
        }
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_confirm_selection -> confirmSelection()
            R.id.cab_properties -> showProperties()
            R.id.cab_rename -> renameFile()
            R.id.cab_edit -> editFile()
            R.id.cab_hide -> toggleFileVisibility(true)
            R.id.cab_unhide -> toggleFileVisibility(false)
            R.id.cab_share -> shareMedia()
            R.id.cab_copy_to -> copyMoveTo(true)
            R.id.cab_move_to -> copyMoveTo(false)
            R.id.cab_select_all -> selectAll()
            R.id.cab_open_with -> activity.openFile(Uri.fromFile(getCurrentFile()), true)
            R.id.cab_set_as -> activity.setAs(Uri.fromFile(getCurrentFile()))
            R.id.cab_delete -> checkDeleteConfirmation()
        }
    }

    override fun getSelectableItemCount() = media.size

    override fun onViewRecycled(holder: ViewHolder?) {
        super.onViewRecycled(holder)
        if (!activity.isActivityDestroyed()) {
            val itemView = holder?.itemView
            visibleItemPaths.remove(itemView?.photo_name?.tag)
            Glide.with(activity).clear(itemView?.medium_thumbnail)
        }
    }

    private fun checkHideBtnVisibility(menu: Menu) {
        var hiddenCnt = 0
        var unhiddenCnt = 0
        selectedPositions.mapNotNull { media.getOrNull(it) }.forEach {
            if (it.name.startsWith('.')) {
                hiddenCnt++
            } else {
                unhiddenCnt++
            }
        }

        menu.findItem(R.id.cab_hide).isVisible = unhiddenCnt > 0
        menu.findItem(R.id.cab_unhide).isVisible = hiddenCnt > 0
    }

    private fun confirmSelection() {
        val paths = getSelectedMedia().map { it.path } as ArrayList<String>
        listener?.selectedPaths(paths)
    }

    private fun showProperties() {
        if (selectedPositions.size <= 1) {
            PropertiesDialog(activity, media[selectedPositions.first()].path, config.shouldShowHidden)
        } else {
            val paths = ArrayList<String>()
            selectedPositions.forEach { paths.add(media[it].path) }
            PropertiesDialog(activity, paths, config.shouldShowHidden)
        }
    }

    private fun renameFile() {
        RenameItemDialog(activity, getCurrentFile().absolutePath) {
            activity.runOnUiThread {
                listener?.refreshItems()
                finishActMode()
            }
        }
    }

    private fun editFile() {
        activity.openEditor(Uri.fromFile(getCurrentFile()))
        finishActMode()
    }

    private fun toggleFileVisibility(hide: Boolean) {
        Thread {
            getSelectedMedia().forEach {
                val oldFile = File(it.path)
                activity.toggleFileVisibility(oldFile, hide)
            }
            activity.runOnUiThread {
                listener?.refreshItems()
                finishActMode()
            }
        }.start()
    }

    private fun shareMedia() {
        if (selectedPositions.size == 1 && selectedPositions.first() != -1) {
            activity.shareMedium(getSelectedMedia()[0])
        } else if (selectedPositions.size > 1) {
            activity.shareMedia(getSelectedMedia())
        }
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val files = ArrayList<File>()
        selectedPositions.forEach { files.add(File(media[it].path)) }

        activity.tryCopyMoveFilesTo(files, isCopyOperation) {
            config.tempFolderPath = ""
            if (!isCopyOperation) {
                listener?.refreshItems()
            }
            finishActMode()
        }
    }

    private fun checkDeleteConfirmation() {
        if (skipConfirmationDialog) {
            deleteConfirmed()
        } else {
            askConfirmDelete()
        }
    }

    private fun askConfirmDelete() {
        DeleteWithRememberDialog(activity) {
            skipConfirmationDialog = it
            deleteConfirmed()
        }
    }

    private fun deleteConfirmed() {
        deleteFiles()
    }

    private fun getCurrentFile() = File(media[selectedPositions.first()].path)

    private fun deleteFiles() {
        if (selectedPositions.isEmpty()) {
            return
        }

        val files = ArrayList<File>(selectedPositions.size)
        val removeMedia = ArrayList<Medium>(selectedPositions.size)

        if (media.size <= selectedPositions.first()) {
            finishActMode()
            return
        }

        val SAFPath = media[selectedPositions.first()].path
        activity.handleSAFDialog(File(SAFPath)) {
            selectedPositions.sortedDescending().forEach {
                val medium = media[it]
                files.add(File(medium.path))
                removeMedia.add(medium)
            }

            media.removeAll(removeMedia)
            listener?.deleteFiles(files)
            removeSelectedItems()
        }
    }

    private fun getSelectedMedia(): List<Medium> {
        val selectedMedia = ArrayList<Medium>(selectedPositions.size)
        selectedPositions.forEach { selectedMedia.add(media[it]) }
        return selectedMedia
    }

    fun updateMedia(newMedia: ArrayList<Medium>) {
        media = newMedia
        notifyDataSetChanged()
        finishActMode()
    }

    fun updateDisplayFilenames(displayFilenames: Boolean) {
        this.displayFilenames = displayFilenames
        notifyDataSetChanged()
    }

    fun updateAnimateGifs(animateGifs: Boolean) {
        this.animateGifs = animateGifs
        notifyDataSetChanged()
    }

    fun updateCropThumbnails(cropThumbnails: Boolean) {
        this.cropThumbnails = cropThumbnails
        notifyDataSetChanged()
    }

    fun updateScrollHorizontally(scrollHorizontally: Boolean) {
        this.scrollHorizontally = scrollHorizontally
        notifyDataSetChanged()
    }

    private fun setupView(view: View, medium: Medium) {
        view.apply {
            play_outline.beVisibleIf(medium.video)
            photo_name.beVisibleIf(displayFilenames || isListViewType)
            photo_name.text = medium.name
            photo_name.tag = medium.path

            medium_thumbnail.isHorizontalScrolling = scrollHorizontally
            delayHandler.postDelayed({
                val isVisible = visibleItemPaths.contains(medium.path)
                if (isVisible) {
                    activity.loadImage(medium.path, medium_thumbnail, scrollHorizontally, animateGifs, cropThumbnails)
                }
            }, 200)

            if (isListViewType) {
                photo_name.setTextColor(textColor)
                play_outline.applyColorFilter(textColor)
            }
        }
    }

    interface MediaOperationsListener {
        fun refreshItems()

        fun deleteFiles(files: ArrayList<File>)

        fun selectedPaths(paths: ArrayList<String>)
    }
}
