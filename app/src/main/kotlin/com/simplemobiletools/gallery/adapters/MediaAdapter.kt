package com.simplemobiletools.gallery.adapters

import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.*
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.bumptech.glide.Glide
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.dialogs.DeleteWithRememberDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.VIEW_TYPE_LIST
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.photo_video_item_grid.view.*
import java.io.File
import java.util.*

class MediaAdapter(val activity: SimpleActivity, var media: MutableList<Medium>, val listener: MediaOperationsListener?, val isAGetIntent: Boolean,
                   val allowMultiplePicks: Boolean, val itemClick: (Medium) -> Unit) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {

    private val config = activity.config
    var actMode: ActionMode? = null
    var primaryColor = config.primaryColor

    private val multiSelector = MultiSelector()
    private val isListViewType = config.viewTypeFiles == VIEW_TYPE_LIST
    private var skipConfirmationDialog = false

    private var itemViews = SparseArray<View>()
    private val selectedPositions = HashSet<Int>()
    private var scrollHorizontally = config.scrollHorizontally
    private var animateGifs = config.animateGifs
    private var cropThumbnails = config.cropThumbnails
    private var textColor = config.textColor
    private var displayFilenames = config.displayFileNames

    fun toggleItemSelection(select: Boolean, pos: Int) {
        if (select) {
            if (itemViews[pos] != null) {
                itemViews[pos].medium_check?.background?.setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
                selectedPositions.add(pos)
            }
        } else {
            selectedPositions.remove(pos)
        }

        itemViews[pos]?.medium_check?.beVisibleIf(select)

        if (selectedPositions.isEmpty()) {
            actMode?.finish()
            return
        }

        updateTitle(selectedPositions.size)
    }

    private fun updateTitle(cnt: Int) {
        actMode?.title = "$cnt / ${media.size}"
        actMode?.invalidate()
    }

    private val adapterListener = object : MyAdapterListener {
        override fun toggleItemSelectionAdapter(select: Boolean, position: Int) {
            toggleItemSelection(select, position)
        }

        override fun getSelectedPositions(): HashSet<Int> = selectedPositions
    }

    private val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
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
                else -> return false
            }
            return true
        }

        override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
            super.onCreateActionMode(actionMode, menu)
            actMode = actionMode
            activity.menuInflater.inflate(R.menu.cab_media, menu)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu): Boolean {
            menu.findItem(R.id.cab_rename).isVisible = selectedPositions.size == 1
            menu.findItem(R.id.cab_open_with).isVisible = selectedPositions.size == 1
            menu.findItem(R.id.cab_confirm_selection).isVisible = isAGetIntent && allowMultiplePicks && selectedPositions.size > 0

            checkHideBtnVisibility(menu)

            return true
        }

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            selectedPositions.forEach {
                itemViews[it]?.medium_check?.beGone()
            }
            selectedPositions.clear()
            actMode = null
        }

        fun checkHideBtnVisibility(menu: Menu) {
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
                actMode?.finish()
            }
        }
    }

    private fun editFile() {
        activity.openEditor(Uri.fromFile(getCurrentFile()))
        actMode?.finish()
    }

    private fun toggleFileVisibility(hide: Boolean) {
        Thread({
            getSelectedMedia().forEach {
                val oldFile = File(it.path)
                activity.toggleFileVisibility(oldFile, hide) {}
            }
            activity.runOnUiThread {
                listener?.refreshItems()
                actMode?.finish()
            }
        }).start()
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
            actMode?.finish()
        }
    }

    fun selectAll() {
        val cnt = media.size
        for (i in 0 until cnt) {
            selectedPositions.add(i)
            multiSelector.setSelected(i, 0, true)
            notifyItemChanged(i)
        }
        updateTitle(cnt)
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
            actMode?.finish()
            return
        }

        activity.handleSAFDialog(File(media[selectedPositions.first()].path)) {
            selectedPositions.sortedDescending().forEach {
                val medium = media[it]
                files.add(File(medium.path))
                removeMedia.add(medium)
                notifyItemRemoved(it)
                itemViews.put(it, null)
            }

            media.removeAll(removeMedia)
            selectedPositions.clear()
            listener?.deleteFiles(files)

            val newItems = SparseArray<View>()
            (0 until itemViews.size())
                    .filter { itemViews[it] != null }
                    .forEachIndexed { curIndex, i -> newItems.put(curIndex, itemViews[i]) }

            itemViews = newItems
            actMode?.finish()
        }
    }

    private fun getSelectedMedia(): List<Medium> {
        val selectedMedia = ArrayList<Medium>(selectedPositions.size)
        selectedPositions.forEach { selectedMedia.add(media[it]) }
        return selectedMedia
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val layoutType = if (isListViewType) R.layout.photo_video_item_list else R.layout.photo_video_item_grid
        val view = LayoutInflater.from(parent?.context).inflate(layoutType, parent, false)
        return ViewHolder(view, adapterListener, activity, multiSelectorMode, multiSelector, listener, allowMultiplePicks || !isAGetIntent, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        itemViews.put(position, holder.bindView(media[position], displayFilenames, scrollHorizontally, isListViewType, textColor, animateGifs, cropThumbnails))
        toggleItemSelection(selectedPositions.contains(position), position)
        holder.itemView.tag = holder
    }

    override fun onViewRecycled(holder: ViewHolder?) {
        super.onViewRecycled(holder)
        holder?.stopLoad()
    }

    override fun getItemCount() = media.size

    fun updateMedia(newMedia: ArrayList<Medium>) {
        media = newMedia
        notifyDataSetChanged()
        actMode?.finish()
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

    fun updateTextColor(textColor: Int) {
        this.textColor = textColor
        notifyDataSetChanged()
    }

    fun selectItem(pos: Int) {
        toggleItemSelection(true, pos)
    }

    fun selectRange(from: Int, to: Int, min: Int, max: Int) {
        if (from == to) {
            (min..max).filter { it != from }
                    .forEach { toggleItemSelection(false, it) }
            return
        }

        if (to < from) {
            for (i in to..from)
                toggleItemSelection(true, i)

            if (min > -1 && min < to) {
                (min until to).filter { it != from }
                        .forEach { toggleItemSelection(false, it) }
            }
            if (max > -1) {
                for (i in from + 1..max)
                    toggleItemSelection(false, i)
            }
        } else {
            for (i in from..to)
                toggleItemSelection(true, i)

            if (max > -1 && max > to) {
                (to + 1..max).filter { it != from }
                        .forEach { toggleItemSelection(false, it) }
            }

            if (min > -1) {
                for (i in min until from)
                    toggleItemSelection(false, i)
            }
        }
    }

    class ViewHolder(val view: View, val adapterListener: MyAdapterListener, val activity: SimpleActivity, val multiSelectorCallback: ModalMultiSelectorCallback,
                     val multiSelector: MultiSelector, val listener: MediaOperationsListener?, val allowMultiplePicks: Boolean,
                     val itemClick: (Medium) -> (Unit)) :
            SwappingHolder(view, MultiSelector()) {
        fun bindView(medium: Medium, displayFilenames: Boolean, scrollHorizontally: Boolean, isListViewType: Boolean, textColor: Int,
                     animateGifs: Boolean, cropThumbnails: Boolean): View {
            itemView.apply {
                play_outline.visibility = if (medium.video) View.VISIBLE else View.GONE
                photo_name.beVisibleIf(displayFilenames || isListViewType)
                photo_name.text = medium.name
                activity.loadImage(medium.path, medium_thumbnail, scrollHorizontally, animateGifs, cropThumbnails)

                if (isListViewType) {
                    photo_name.setTextColor(textColor)
                    play_outline.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
                }

                setOnClickListener { viewClicked(medium) }
                setOnLongClickListener { if (allowMultiplePicks) viewLongClicked() else viewClicked(medium); true }
            }
            return itemView
        }

        private fun viewClicked(medium: Medium) {
            if (multiSelector.isSelectable) {
                val isSelected = adapterListener.getSelectedPositions().contains(adapterPosition)
                adapterListener.toggleItemSelectionAdapter(!isSelected, adapterPosition)
            } else {
                itemClick(medium)
            }
        }

        private fun viewLongClicked() {
            if (listener != null) {
                if (!multiSelector.isSelectable) {
                    activity.startSupportActionMode(multiSelectorCallback)
                    adapterListener.toggleItemSelectionAdapter(true, adapterPosition)
                }

                listener.itemLongClicked(adapterPosition)
            }
        }

        fun stopLoad() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !activity.isDestroyed)
                Glide.with(activity).clear(view.medium_thumbnail)
        }
    }

    interface MyAdapterListener {
        fun toggleItemSelectionAdapter(select: Boolean, position: Int)

        fun getSelectedPositions(): HashSet<Int>
    }

    interface MediaOperationsListener {
        fun refreshItems()

        fun deleteFiles(files: ArrayList<File>)

        fun itemLongClicked(position: Int)

        fun selectedPaths(paths: ArrayList<String>)
    }
}
