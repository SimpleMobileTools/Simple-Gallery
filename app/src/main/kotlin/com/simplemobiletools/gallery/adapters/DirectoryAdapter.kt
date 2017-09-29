package com.simplemobiletools.gallery.adapters

import android.graphics.PorterDuff
import android.os.Build
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.*
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.dialogs.ExcludeFolderDialog
import com.simplemobiletools.gallery.dialogs.PickMediumDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.VIEW_TYPE_LIST
import com.simplemobiletools.gallery.models.AlbumCover
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.directory_item_grid.view.*
import java.io.File
import java.util.*

class DirectoryAdapter(val activity: SimpleActivity, var dirs: MutableList<Directory>, val listener: DirOperationsListener?, val isPickIntent: Boolean,
                       val itemClick: (Directory) -> Unit) : RecyclerView.Adapter<DirectoryAdapter.ViewHolder>() {

    val multiSelector = MultiSelector()
    val config = activity.config
    val isListViewType = config.viewTypeFolders == VIEW_TYPE_LIST

    var actMode: ActionMode? = null
    var itemViews = SparseArray<View>()
    val selectedPositions = HashSet<Int>()
    var primaryColor = config.primaryColor
    var pinnedFolders = config.pinnedFolders
    var scrollVertically = !config.scrollHorizontally

    fun toggleItemSelection(select: Boolean, pos: Int) {
        if (select) {
            itemViews[pos]?.dir_check?.background?.setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
            selectedPositions.add(pos)
        } else
            selectedPositions.remove(pos)

        itemViews[pos]?.dir_check?.beVisibleIf(select)

        if (selectedPositions.isEmpty()) {
            actMode?.finish()
            return
        }

        updateTitle(selectedPositions.size)
    }

    private fun updateTitle(cnt: Int) {
        actMode?.title = "$cnt / ${dirs.size}"
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
                R.id.cab_properties -> showProperties()
                R.id.cab_rename -> renameDir()
                R.id.cab_pin -> pinFolders(true)
                R.id.cab_unpin -> pinFolders(false)
                R.id.cab_hide -> toggleFoldersVisibility(true)
                R.id.cab_unhide -> toggleFoldersVisibility(false)
                R.id.cab_exclude -> tryExcludeFolder()
                R.id.cab_copy_to -> copyMoveTo(true)
                R.id.cab_move_to -> copyMoveTo(false)
                R.id.cab_select_all -> selectAll()
                R.id.cab_delete -> askConfirmDelete()
                R.id.cab_select_photo -> changeAlbumCover(false)
                R.id.cab_use_default -> changeAlbumCover(true)
                else -> return false
            }
            return true
        }

        override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
            super.onCreateActionMode(actionMode, menu)
            actMode = actionMode
            activity.menuInflater.inflate(R.menu.cab_directories, menu)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu): Boolean {
            menu.findItem(R.id.cab_rename).isVisible = selectedPositions.size <= 1
            menu.findItem(R.id.cab_change_cover_image).isVisible = selectedPositions.size <= 1

            checkHideBtnVisibility(menu)
            checkPinBtnVisibility(menu)

            return true
        }

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            selectedPositions.forEach {
                itemViews[it]?.dir_check?.beGone()
            }
            selectedPositions.clear()
            actMode = null
        }

        fun checkHideBtnVisibility(menu: Menu) {
            var hiddenCnt = 0
            var unhiddenCnt = 0
            selectedPositions.mapNotNull { dirs.getOrNull(it)?.path }.forEach {
                if (File(it).containsNoMedia())
                    hiddenCnt++
                else
                    unhiddenCnt++
            }

            menu.findItem(R.id.cab_hide).isVisible = unhiddenCnt > 0
            menu.findItem(R.id.cab_unhide).isVisible = hiddenCnt > 0
        }

        fun checkPinBtnVisibility(menu: Menu) {
            val pinnedFolders = config.pinnedFolders
            var pinnedCnt = 0
            var unpinnedCnt = 0
            selectedPositions.mapNotNull { dirs.getOrNull(it)?.path }.forEach {
                if (pinnedFolders.contains(it))
                    pinnedCnt++
                else
                    unpinnedCnt++
            }

            menu.findItem(R.id.cab_pin).isVisible = unpinnedCnt > 0
            menu.findItem(R.id.cab_unpin).isVisible = pinnedCnt > 0
        }
    }

    private fun showProperties() {
        if (selectedPositions.size <= 1) {
            PropertiesDialog(activity, dirs[selectedPositions.first()].path, config.shouldShowHidden)
        } else {
            val paths = ArrayList<String>()
            selectedPositions.forEach { paths.add(dirs[it].path) }
            PropertiesDialog(activity, paths, config.shouldShowHidden)
        }
    }

    private fun renameDir() {
        val path = dirs[selectedPositions.first()].path
        val dir = File(path)
        if (activity.isAStorageRootFolder(dir.absolutePath)) {
            activity.toast(R.string.rename_folder_root)
            return
        }

        RenameItemDialog(activity, dir.absolutePath) {
            activity.runOnUiThread {
                listener?.refreshItems()
                actMode?.finish()
            }
        }
    }

    private fun toggleFoldersVisibility(hide: Boolean) {
        getSelectedPaths().forEach {
            if (hide) {
                if (config.wasHideFolderTooltipShown) {
                    hideFolder(it)
                } else {
                    config.wasHideFolderTooltipShown = true
                    ConfirmationDialog(activity, activity.getString(R.string.hide_folder_description)) {
                        hideFolder(it)
                    }
                }
            } else {
                activity.removeNoMedia(it) {
                    noMediaHandled()
                }
            }
        }
    }

    private fun hideFolder(path: String) {
        activity.addNoMedia(path) {
            noMediaHandled()
        }
    }

    private fun tryExcludeFolder() {
        ExcludeFolderDialog(activity, getSelectedPaths().toList()) {
            listener?.refreshItems()
            actMode?.finish()
        }
    }

    private fun noMediaHandled() {
        activity.runOnUiThread {
            listener?.refreshItems()
            actMode?.finish()
        }
    }

    private fun pinFolders(pin: Boolean) {
        if (pin)
            config.addPinnedFolders(getSelectedPaths())
        else
            config.removePinnedFolders(getSelectedPaths())

        pinnedFolders = config.pinnedFolders
        listener?.refreshItems()
        notifyDataSetChanged()
        actMode?.finish()
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        if (selectedPositions.isEmpty())
            return

        val files = ArrayList<File>()
        selectedPositions.forEach {
            val dir = File(dirs[it].path)
            files.addAll(dir.listFiles().filter { it.isFile && it.isImageVideoGif() })
        }

        activity.tryCopyMoveFilesTo(files, isCopyOperation) {
            config.tempFolderPath = ""
            listener?.refreshItems()
            actMode?.finish()
        }
    }

    fun selectAll() {
        val cnt = dirs.size
        for (i in 0 until cnt) {
            selectedPositions.add(i)
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

    private fun deleteFiles() {
        val folders = ArrayList<File>(selectedPositions.size)
        val removeFolders = ArrayList<Directory>(selectedPositions.size)

        var needPermissionForPath = ""
        selectedPositions.forEach {
            if (dirs.size > it) {
                val path = dirs[it].path
                if (activity.needsStupidWritePermissions(path) && config.treeUri.isEmpty()) {
                    needPermissionForPath = path
                }
            }
        }

        activity.handleSAFDialog(File(needPermissionForPath)) {
            selectedPositions.sortedDescending().forEach {
                if (dirs.size > it) {
                    val directory = dirs[it]
                    folders.add(File(directory.path))
                    removeFolders.add(directory)
                    notifyItemRemoved(it)
                    itemViews.put(it, null)
                }
            }

            dirs.removeAll(removeFolders)
            selectedPositions.clear()
            listener?.tryDeleteFolders(folders)

            val newItems = SparseArray<View>()
            (0 until itemViews.size())
                    .filter { itemViews[it] != null }
                    .forEachIndexed { curIndex, i -> newItems.put(curIndex, itemViews[i]) }

            itemViews = newItems
        }
    }

    private fun changeAlbumCover(useDefault: Boolean) {
        if (selectedPositions.size != 1)
            return

        val path = dirs[selectedPositions.first()].path
        var albumCovers = config.parseAlbumCovers()

        if (useDefault) {
            albumCovers = albumCovers.filterNot { it.path == path } as ArrayList
            storeCovers(albumCovers)
        } else {
            PickMediumDialog(activity, path) {
                albumCovers = albumCovers.filterNot { it.path == path } as ArrayList
                albumCovers.add(AlbumCover(path, it))
                storeCovers(albumCovers)
            }
        }
    }

    private fun storeCovers(albumCovers: ArrayList<AlbumCover>) {
        activity.config.albumCovers = Gson().toJson(albumCovers)
        actMode?.finish()
        listener?.refreshItems()
    }

    private fun getSelectedPaths(): HashSet<String> {
        val paths = HashSet<String>(selectedPositions.size)
        selectedPositions.forEach { paths.add(dirs[it].path) }
        return paths
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val layoutType = if (isListViewType) R.layout.directory_item_list else R.layout.directory_item_grid
        val view = LayoutInflater.from(parent?.context).inflate(layoutType, parent, false)
        return ViewHolder(view, adapterListener, activity, multiSelectorMode, multiSelector, listener, isPickIntent, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dir = dirs[position]
        itemViews.put(position, holder.bindView(dir, pinnedFolders.contains(dir.path), scrollVertically))
        toggleItemSelection(selectedPositions.contains(position), position)
        holder.itemView.tag = holder
    }

    override fun onViewRecycled(holder: ViewHolder?) {
        super.onViewRecycled(holder)
        holder?.stopLoad()
    }

    override fun getItemCount() = dirs.size

    fun updateDirs(newDirs: ArrayList<Directory>) {
        dirs = newDirs
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
                     val multiSelector: MultiSelector, val listener: DirOperationsListener?, val isPickIntent: Boolean, val itemClick: (Directory) -> (Unit)) :
            SwappingHolder(view, MultiSelector()) {
        fun bindView(directory: Directory, isPinned: Boolean, scrollVertically: Boolean): View {
            itemView.apply {
                dir_name.text = directory.name
                photo_cnt.text = directory.mediaCnt.toString()
                activity.loadImage(directory.tmb, dir_thumbnail, scrollVertically)
                dir_pin.beVisibleIf(isPinned)
                dir_sd_card.beVisibleIf(activity.isPathOnSD(directory.path))

                setOnClickListener { viewClicked(directory) }
                setOnLongClickListener { if (isPickIntent) viewClicked(directory) else viewLongClicked(); true }
            }
            return itemView
        }

        private fun viewClicked(directory: Directory) {
            if (multiSelector.isSelectable) {
                val isSelected = adapterListener.getSelectedPositions().contains(layoutPosition)
                adapterListener.toggleItemSelectionAdapter(!isSelected, layoutPosition)
            } else {
                itemClick(directory)
            }
        }

        private fun viewLongClicked() {
            if (listener != null) {
                if (!multiSelector.isSelectable) {
                    activity.startSupportActionMode(multiSelectorCallback)
                    adapterListener.toggleItemSelectionAdapter(true, layoutPosition)
                }

                listener.itemLongClicked(layoutPosition)
            }
        }

        fun stopLoad() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed)
                return

            Glide.with(activity).clear(view.dir_thumbnail)
        }
    }

    interface MyAdapterListener {
        fun toggleItemSelectionAdapter(select: Boolean, position: Int)

        fun getSelectedPositions(): HashSet<Int>
    }

    interface DirOperationsListener {
        fun refreshItems()

        fun tryDeleteFolders(folders: ArrayList<File>)

        fun itemLongClicked(position: Int)
    }
}
