package com.simplemobiletools.gallery.adapters

import android.os.Build
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
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

class MediaAdapter(val activity: SimpleActivity, var media: MutableList<Medium>, val listener: MediaOperationsListener?, val itemClick: (Medium) -> Unit) :
        RecyclerView.Adapter<MediaAdapter.ViewHolder>() {
    val multiSelector = MultiSelector()
    val views = ArrayList<View>()
    val config = activity.config

    companion object {
        var actMode: ActionMode? = null
        var displayFilenames = false
        val markedItems = HashSet<Int>()
        var foregroundColor = 0
        var backgroundColor = 0
        var itemCnt = 0

        fun toggleItemSelection(itemView: View, select: Boolean, pos: Int = -1) {
            getProperView(itemView).isSelected = select

            if (pos == -1)
                return

            if (select)
                markedItems.add(pos)
            else
                markedItems.remove(pos)
        }

        fun getProperView(itemView: View): View {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                itemView.medium_thumbnail_holder
            else
                itemView.medium_thumbnail
        }

        fun updateTitle(cnt: Int) {
            actMode?.title = "$cnt / $itemCnt"
        }
    }

    init {
        foregroundColor = config.primaryColor
        backgroundColor = config.backgroundColor
        itemCnt = media.size
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
            val positions = multiSelector.selectedPositions
            menu.findItem(R.id.cab_rename).isVisible = positions.size <= 1
            menu.findItem(R.id.cab_edit).isVisible = positions.size == 1 && media[positions[0]].isImage()

            checkHideBtnVisibility(menu, positions)

            return true
        }

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            views.forEach { toggleItemSelection(it, false) }
            markedItems.clear()
            actMode = null
        }

        fun checkHideBtnVisibility(menu: Menu, positions: List<Int>) {
            var hiddenCnt = 0
            var unhiddenCnt = 0
            positions.map { media[it] }.forEach {
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
        val selections = multiSelector.selectedPositions
        if (selections.size <= 1) {
            PropertiesDialog(activity, media[selections[0]].path, config.shouldShowHidden)
        } else {
            val paths = ArrayList<String>()
            selections.forEach { paths.add(media[it].path) }
            PropertiesDialog(activity, paths, config.shouldShowHidden)
        }
    }

    private fun renameFile() {
        RenameItemDialog(activity, getCurrentFile().absolutePath) {
            listener?.refreshItems()
            activity.runOnUiThread {
                actMode?.finish()
            }
        }
    }

    private fun editFile() {
        activity.openEditor(getCurrentFile())
        actMode?.finish()
    }

    private fun toggleFileVisibility(hide: Boolean) {

    }

    private fun shareMedia() {
        val selections = multiSelector.selectedPositions
        if (selections.size <= 1) {
            activity.shareMedium(getSelectedMedia()[0])
        } else {
            activity.shareMedia(getSelectedMedia())
        }
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val files = ArrayList<File>()
        val positions = multiSelector.selectedPositions
        positions.forEach { files.add(File(media[it].path)) }

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
            markedItems.add(i)
            multiSelector.setSelected(i, 0, true)
            notifyItemChanged(i)
        }
        updateTitle(cnt)
        actMode?.invalidate()
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            actMode?.finish()
            deleteFiles()
        }
    }

    private fun getCurrentFile() = File(media[multiSelector.selectedPositions[0]].path)

    private fun deleteFiles() {
        val selections = multiSelector.selectedPositions
        val files = ArrayList<File>(selections.size)
        val removeMedia = ArrayList<Medium>(selections.size)

        activity.handleSAFDialog(File(media[selections[0]].path)) {
            selections.reverse()
            selections.forEach {
                val medium = media[it]
                files.add(File(medium.path))
                removeMedia.add(medium)
                notifyItemRemoved(it)
            }

            media.removeAll(removeMedia)
            markedItems.clear()
            listener?.deleteFiles(files)
            itemCnt = media.size
        }
    }

    private fun getSelectedMedia(): List<Medium> {
        val positions = multiSelector.selectedPositions
        val selectedMedia = ArrayList<Medium>(positions.size)
        positions.forEach { selectedMedia.add(media[it]) }
        return selectedMedia
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.photo_video_item, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        views.add(holder.bindView(activity, multiSelectorMode, multiSelector, media[position], position))
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

    class ViewHolder(val view: View, val itemClick: (Medium) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(activity: SimpleActivity, multiSelectorCallback: ModalMultiSelectorCallback, multiSelector: MultiSelector, medium: Medium, pos: Int): View {
            itemView.apply {
                play_outline.visibility = if (medium.video) View.VISIBLE else View.GONE
                photo_name.beVisibleIf(displayFilenames)
                photo_name.text = medium.name
                toggleItemSelection(this, markedItems.contains(pos), pos)
                activity.loadImage(medium.path, medium_thumbnail)

                setOnClickListener { viewClicked(multiSelector, medium, pos) }
                setOnLongClickListener {
                    if (!multiSelector.isSelectable) {
                        activity.startSupportActionMode(multiSelectorCallback)
                        multiSelector.setSelected(this@ViewHolder, true)
                        updateTitle(multiSelector.selectedPositions.size)
                        toggleItemSelection(this, true, pos)
                        actMode?.invalidate()
                    }
                    true
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    (getProperView(this) as FrameLayout).foreground = foregroundColor.createSelector()
                else
                    getProperView(this).foreground = foregroundColor.createSelector()
            }
            return itemView
        }

        fun viewClicked(multiSelector: MultiSelector, medium: Medium, pos: Int) {
            if (multiSelector.isSelectable) {
                val isSelected = multiSelector.selectedPositions.contains(layoutPosition)
                multiSelector.setSelected(this, !isSelected)
                toggleItemSelection(itemView, !isSelected, pos)

                val selectedCnt = multiSelector.selectedPositions.size
                if (selectedCnt == 0) {
                    actMode?.finish()
                } else {
                    updateTitle(selectedCnt)
                }
                actMode?.invalidate()
            } else {
                itemClick(medium)
            }
        }

        fun stopLoad() {
            Glide.clear(view.medium_thumbnail)
        }
    }

    interface MediaOperationsListener {
        fun refreshItems()

        fun deleteFiles(files: ArrayList<File>)
    }
}
