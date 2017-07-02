package com.simplemobiletools.gallery.adapters

import android.os.Build
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.*
import android.widget.FrameLayout
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.bumptech.glide.Glide
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.photo_video_item.view.*
import kotlinx.android.synthetic.main.photo_video_tmb.view.*
import java.io.File
import java.util.*

class MediaAdapter(val activity: SimpleActivity, var media: MutableList<Medium>, val listener: MediaOperationsListener?, val isPickIntent: Boolean,
                   val itemClick: (Medium) -> Unit) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {

    val multiSelector = MultiSelector()
    val config = activity.config

    var actMode: ActionMode? = null
    var itemViews = SparseArray<View>()
    val selectedPositions = HashSet<Int>()
    var foregroundColor = config.primaryColor
    var displayFilenames = config.displayFileNames
    var scrollVertically = !config.scrollHorizontally

    fun toggleItemSelection(select: Boolean, pos: Int) {
        if (itemViews[pos] != null)
            getProperView(itemViews[pos]!!).isSelected = select

        if (select)
            selectedPositions.add(pos)
        else
            selectedPositions.remove(pos)

        if (selectedPositions.isEmpty()) {
            actMode?.finish()
            return
        }

        updateTitle(selectedPositions.size)
    }

    fun getProperView(itemView: View): View {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            itemView.medium_thumbnail_holder
        else
            itemView.medium_thumbnail
    }

    fun updateTitle(cnt: Int) {
        actMode?.title = "$cnt / ${media.size}"
        actMode?.invalidate()
    }

    fun updatePrimaryColor(color: Int) {
        foregroundColor = color
        (0..itemViews.size() - 1).mapNotNull { itemViews[it] }
                .forEach { setupItemViewForeground(it) }
    }

    private fun setupItemViewForeground(itemView: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            (getProperView(itemView) as FrameLayout).foreground = foregroundColor.createSelector()
        else
            getProperView(itemView).foreground = foregroundColor.createSelector()
    }

    val adapterListener = object : MyAdapterListener {
        override fun toggleItemSelectionAdapter(select: Boolean, position: Int) {
            toggleItemSelection(select, position)
        }

        override fun setupItemForeground(itemView: View) {
            setupItemViewForeground(itemView)
        }

        override fun getSelectedPositions(): HashSet<Int> = selectedPositions
    }

    val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.cab_properties -> showProperties()
                R.id.cab_rename -> renameFile()
                R.id.cab_edit -> editFile()
                R.id.cab_hide -> toggleFileVisibility(true)
                R.id.cab_unhide -> toggleFileVisibility(false)
                R.id.cab_share -> shareMedia()
                R.id.cab_copy_to -> copyMoveTo(true)
                R.id.cab_move_to -> copyMoveTo(false)
                R.id.cab_select_all -> selectAll()
                R.id.cab_delete -> askConfirmDelete()
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
            menu.findItem(R.id.cab_rename).isVisible = selectedPositions.size <= 1
            menu.findItem(R.id.cab_edit).isVisible = selectedPositions.size == 1 && media.size > selectedPositions.first() && media[selectedPositions.first()].isImage()

            checkHideBtnVisibility(menu)

            return true
        }

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            selectedPositions.forEach {
                if (itemViews[it] != null)
                    getProperView(itemViews[it]!!).isSelected = false
            }
            selectedPositions.clear()
            actMode = null
        }

        fun checkHideBtnVisibility(menu: Menu) {
            var hiddenCnt = 0
            var unhiddenCnt = 0
            selectedPositions.map { media.getOrNull(it) }.filterNotNull().forEach {
                if (it.name.startsWith('.'))
                    hiddenCnt++
                else
                    unhiddenCnt++
            }

            menu.findItem(R.id.cab_hide).isVisible = unhiddenCnt > 0
            menu.findItem(R.id.cab_unhide).isVisible = hiddenCnt > 0
        }
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
        activity.openFileEditor(getCurrentFile())
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
        if (selectedPositions.size <= 1) {
            activity.shareMedium(getSelectedMedia()[0])
        } else {
            activity.shareMedia(getSelectedMedia())
        }
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val files = ArrayList<File>()
        selectedPositions.forEach { files.add(File(media[it].path)) }

        activity.tryCopyMoveFilesTo(files, isCopyOperation) {
            if (!isCopyOperation) {
                listener?.refreshItems()
            }
            actMode?.finish()
        }
    }

    fun selectAll() {
        val cnt = media.size
        for (i in 0..cnt - 1) {
            selectedPositions.add(i)
            multiSelector.setSelected(i, 0, true)
            notifyItemChanged(i)
        }
        updateTitle(cnt)
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            deleteFiles()
            actMode?.finish()
        }
    }

    private fun getCurrentFile() = File(media[selectedPositions.first()].path)

    private fun deleteFiles() {
        if (selectedPositions.isEmpty())
            return

        val files = ArrayList<File>(selectedPositions.size)
        val removeMedia = ArrayList<Medium>(selectedPositions.size)

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
            var curIndex = 0
            for (i in 0..itemViews.size() - 1) {
                if (itemViews[i] != null) {
                    newItems.put(curIndex, itemViews[i])
                    curIndex++
                }
            }

            itemViews = newItems
        }
    }

    private fun getSelectedMedia(): List<Medium> {
        val selectedMedia = ArrayList<Medium>(selectedPositions.size)
        selectedPositions.forEach { selectedMedia.add(media[it]) }
        return selectedMedia
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.photo_video_item, parent, false)
        return ViewHolder(view, adapterListener, activity, multiSelectorMode, multiSelector, listener, isPickIntent, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        itemViews.put(position, holder.bindView(media[position], displayFilenames, scrollVertically))
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
    }

    fun updateDisplayFilenames(display: Boolean) {
        displayFilenames = display
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
                (min..to - 1).filter { it != from }
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
                for (i in min..from - 1)
                    toggleItemSelection(false, i)
            }
        }
    }

    class ViewHolder(val view: View, val adapterListener: MyAdapterListener, val activity: SimpleActivity, val multiSelectorCallback: ModalMultiSelectorCallback,
                     val multiSelector: MultiSelector, val listener: MediaOperationsListener?, val isPickIntent: Boolean, val itemClick: (Medium) -> (Unit)) :
            SwappingHolder(view, MultiSelector()) {
        fun bindView(medium: Medium, displayFilenames: Boolean, scrollVertically: Boolean): View {
            itemView.apply {
                play_outline.visibility = if (medium.video) View.VISIBLE else View.GONE
                photo_name.beVisibleIf(displayFilenames)
                photo_name.text = medium.name
                activity.loadImage(medium.path, medium_thumbnail, scrollVertically)

                setOnClickListener { viewClicked(medium) }
                setOnLongClickListener { if (isPickIntent) viewClicked(medium) else viewLongClicked(); true }

                adapterListener.setupItemForeground(this)
            }
            return itemView
        }

        fun viewClicked(medium: Medium) {
            if (multiSelector.isSelectable) {
                val isSelected = adapterListener.getSelectedPositions().contains(layoutPosition)
                adapterListener.toggleItemSelectionAdapter(!isSelected, layoutPosition)
            } else {
                itemClick(medium)
            }
        }

        fun viewLongClicked() {
            if (listener != null) {
                if (!multiSelector.isSelectable) {
                    activity.startSupportActionMode(multiSelectorCallback)
                    adapterListener.toggleItemSelectionAdapter(true, layoutPosition)
                }

                listener.itemLongClicked(layoutPosition)
            }
        }

        fun stopLoad() {
            if (!activity.isDestroyed)
                Glide.with(activity).clear(view.medium_thumbnail)
        }
    }

    interface MyAdapterListener {
        fun toggleItemSelectionAdapter(select: Boolean, position: Int)

        fun setupItemForeground(itemView: View)

        fun getSelectedPositions(): HashSet<Int>
    }

    interface MediaOperationsListener {
        fun refreshItems()

        fun deleteFiles(files: ArrayList<File>)

        fun itemLongClicked(position: Int)
    }
}
