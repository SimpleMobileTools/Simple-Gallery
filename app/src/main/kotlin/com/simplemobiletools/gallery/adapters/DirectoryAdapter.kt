package com.simplemobiletools.gallery.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.OTG_PATH
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.dialogs.ExcludeFolderDialog
import com.simplemobiletools.gallery.dialogs.PickMediumDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.interfaces.DirectoryOperationsListener
import com.simplemobiletools.gallery.models.AlbumCover
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.directory_item_list.view.*
import java.io.File

class DirectoryAdapter(activity: BaseSimpleActivity, var dirs: ArrayList<Directory>, val listener: DirectoryOperationsListener?, recyclerView: MyRecyclerView,
                       val isPickIntent: Boolean, fastScroller: FastScroller? = null, itemClick: (Any) -> Unit) :
        MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {

    private val config = activity.config
    private val isListViewType = config.viewTypeFolders == VIEW_TYPE_LIST
    private var pinnedFolders = config.pinnedFolders
    private var scrollHorizontally = config.scrollHorizontally
    private var showMediaCount = config.showMediaCount
    private var animateGifs = config.animateGifs
    private var cropThumbnails = config.cropThumbnails
    private var currentDirectoriesHash = dirs.hashCode()

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_directories

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutType = if (isListViewType) R.layout.directory_item_list else R.layout.directory_item_grid
        return createViewHolder(layoutType, parent)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val dir = dirs.getOrNull(position) ?: return
        holder.bindView(dir, true, !isPickIntent) { itemView, adapterPosition ->
            setupView(itemView, dir)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = dirs.size

    override fun prepareActionMode(menu: Menu) {
        val selectedPaths = getSelectedPaths()
        if (selectedPaths.isEmpty()) {
            return
        }

        val isOneItemSelected = isOneItemSelected()
        menu.apply {
            findItem(R.id.cab_rename).isVisible = isOneItemSelected && !selectedPaths.contains(FAVORITES) && !selectedPaths.contains(RECYCLE_BIN)
            findItem(R.id.cab_change_cover_image).isVisible = isOneItemSelected

            findItem(R.id.cab_empty_recycle_bin).isVisible = isOneItemSelected && selectedPaths.first() == RECYCLE_BIN
            findItem(R.id.cab_empty_disable_recycle_bin).isVisible = isOneItemSelected && selectedPaths.first() == RECYCLE_BIN

            checkHideBtnVisibility(this, selectedPaths)
            checkPinBtnVisibility(this, selectedPaths)
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_properties -> showProperties()
            R.id.cab_rename -> renameDir()
            R.id.cab_pin -> pinFolders(true)
            R.id.cab_unpin -> pinFolders(false)
            R.id.cab_empty_recycle_bin -> tryEmptyRecycleBin(true)
            R.id.cab_empty_disable_recycle_bin -> emptyAndDisableRecycleBin()
            R.id.cab_hide -> toggleFoldersVisibility(true)
            R.id.cab_unhide -> toggleFoldersVisibility(false)
            R.id.cab_exclude -> tryExcludeFolder()
            R.id.cab_copy_to -> copyMoveTo(true)
            R.id.cab_move_to -> copyMoveTo(false)
            R.id.cab_select_all -> selectAll()
            R.id.cab_delete -> askConfirmDelete()
            R.id.cab_select_photo -> changeAlbumCover(false)
            R.id.cab_use_default -> changeAlbumCover(true)
        }
    }

    override fun getSelectableItemCount() = dirs.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = dirs.getOrNull(position)?.path?.hashCode()

    override fun getItemKeyPosition(key: Int) = dirs.indexOfFirst { it.path.hashCode() == key }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed) {
            Glide.with(activity).clear(holder.itemView.dir_thumbnail!!)
        }
    }

    private fun checkHideBtnVisibility(menu: Menu, selectedPaths: ArrayList<String>) {
        menu.findItem(R.id.cab_hide).isVisible = selectedPaths.any { !File(it).doesThisOrParentHaveNoMedia() }
        menu.findItem(R.id.cab_unhide).isVisible = selectedPaths.any { File(it).doesThisOrParentHaveNoMedia() }
    }

    private fun checkPinBtnVisibility(menu: Menu, selectedPaths: ArrayList<String>) {
        val pinnedFolders = config.pinnedFolders
        menu.findItem(R.id.cab_pin).isVisible = selectedPaths.any { !pinnedFolders.contains(it) }
        menu.findItem(R.id.cab_unpin).isVisible = selectedPaths.any { pinnedFolders.contains(it) }
    }

    private fun showProperties() {
        if (selectedKeys.size <= 1) {
            val path = getFirstSelectedItemPath() ?: return
            if (path != FAVORITES && path != RECYCLE_BIN) {
                PropertiesDialog(activity, path, config.shouldShowHidden)
            }
        } else {
            PropertiesDialog(activity, getSelectedPaths().filter { it != FAVORITES && it != RECYCLE_BIN }.toMutableList(), config.shouldShowHidden)
        }
    }

    private fun renameDir() {
        val firstDir = getFirstSelectedItem() ?: return
        val sourcePath = firstDir.path
        val dir = File(sourcePath)
        if (activity.isAStorageRootFolder(dir.absolutePath)) {
            activity.toast(R.string.rename_folder_root)
            return
        }

        RenameItemDialog(activity, dir.absolutePath) {
            activity.runOnUiThread {
                firstDir.apply {
                    path = it
                    name = it.getFilenameFromPath()
                    tmb = File(it, tmb.getFilenameFromPath()).absolutePath
                }
                updateDirs(dirs)
                Thread {
                    activity.galleryDB.DirectoryDao().updateDirectoryAfterRename(firstDir.tmb, firstDir.name, firstDir.path, sourcePath)
                    listener?.refreshItems()
                }.start()
            }
        }
    }

    private fun toggleFoldersVisibility(hide: Boolean) {
        val selectedPaths = getSelectedPaths()
        if (hide && selectedPaths.contains(RECYCLE_BIN)) {
            config.showRecycleBinAtFolders = false
            if (selectedPaths.size == 1) {
                listener?.refreshItems()
                finishActMode()
            }
        }

        selectedPaths.filter { it != FAVORITES && it != RECYCLE_BIN }.forEach {
            val path = it
            if (hide) {
                if (config.wasHideFolderTooltipShown) {
                    hideFolder(path)
                } else {
                    config.wasHideFolderTooltipShown = true
                    ConfirmationDialog(activity, activity.getString(R.string.hide_folder_description)) {
                        hideFolder(path)
                    }
                }
            } else {
                activity.removeNoMedia(path) {
                    if (activity.config.shouldShowHidden) {
                        updateFolderNames()
                    } else {
                        activity.runOnUiThread {
                            listener?.refreshItems()
                            finishActMode()
                        }
                    }
                }
            }
        }
    }

    private fun tryEmptyRecycleBin(askConfirmation: Boolean) {
        if (askConfirmation) {
            activity.showRecycleBinEmptyingDialog {
                emptyRecycleBin()
            }
        } else {
            emptyRecycleBin()
        }
    }

    private fun emptyRecycleBin() {
        activity.emptyTheRecycleBin {
            listener?.refreshItems()
        }
    }

    private fun emptyAndDisableRecycleBin() {
        activity.showRecycleBinEmptyingDialog {
            activity.emptyAndDisableTheRecycleBin {
                listener?.refreshItems()
            }
        }
    }

    private fun updateFolderNames() {
        val includedFolders = activity.config.includedFolders
        val hidden = activity.getString(R.string.hidden)
        dirs.forEach {
            it.name = activity.checkAppendingHidden(it.path, hidden, includedFolders)
        }
        listener?.updateDirectories(dirs.toMutableList() as ArrayList)
        activity.runOnUiThread {
            updateDirs(dirs)
        }
    }

    private fun hideFolder(path: String) {
        activity.addNoMedia(path) {
            if (activity.config.shouldShowHidden) {
                updateFolderNames()
            } else {
                val affectedPositions = ArrayList<Int>()
                val includedFolders = activity.config.includedFolders
                val newDirs = dirs.filterIndexed { index, directory ->
                    val removeDir = File(directory.path).doesThisOrParentHaveNoMedia() && !includedFolders.contains(directory.path)
                    if (removeDir) {
                        affectedPositions.add(index)
                    }
                    !removeDir
                } as ArrayList<Directory>

                activity.runOnUiThread {
                    affectedPositions.sortedDescending().forEach {
                        notifyItemRemoved(it)
                    }

                    currentDirectoriesHash = newDirs.hashCode()
                    dirs = newDirs

                    finishActMode()
                    listener?.updateDirectories(newDirs)
                }
            }
        }
    }

    private fun tryExcludeFolder() {
        val selectedPaths = getSelectedPaths()
        val paths = selectedPaths.filter { it != PATH && it != RECYCLE_BIN && it != FAVORITES }.toSet()
        if (selectedPaths.contains(RECYCLE_BIN)) {
            config.showRecycleBinAtFolders = false
            if (selectedPaths.size == 1) {
                listener?.refreshItems()
                finishActMode()
            }
        }

        if (paths.size == 1) {
            ExcludeFolderDialog(activity, paths.toMutableList()) {
                listener?.refreshItems()
                finishActMode()
            }
        } else if (paths.size > 1) {
            activity.config.addExcludedFolders(paths)
            listener?.refreshItems()
            finishActMode()
        }
    }

    private fun pinFolders(pin: Boolean) {
        if (pin) {
            config.addPinnedFolders(getSelectedPaths().toHashSet())
        } else {
            config.removePinnedFolders(getSelectedPaths().toHashSet())
        }

        currentDirectoriesHash = 0
        pinnedFolders = config.pinnedFolders
        listener?.recheckPinnedFolders()
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val paths = ArrayList<String>()
        val showHidden = activity.config.shouldShowHidden
        getSelectedPaths().forEach {
            if (it.startsWith(OTG_PATH)) {
                paths.addAll(getOTGFilePaths(it, showHidden))
            } else if (it != FAVORITES) {
                File(it).listFiles()?.filter {
                    !activity.getIsPathDirectory(it.absolutePath) && it.isMediaFile() && (showHidden || !it.name.startsWith('.'))
                }?.mapTo(paths) { it.absolutePath }
            }
        }

        val fileDirItems = paths.map { FileDirItem(it, it.getFilenameFromPath()) } as ArrayList<FileDirItem>
        activity.tryCopyMoveFilesTo(fileDirItems, isCopyOperation) {
            config.tempFolderPath = ""
            listener?.refreshItems()
            finishActMode()
        }
    }

    private fun getOTGFilePaths(path: String, showHidden: Boolean): ArrayList<String> {
        val paths = ArrayList<String>()
        activity.getOTGFolderChildren(path)?.forEach {
            if (!it.isDirectory && it.name!!.isMediaFile() && (showHidden || !it.name!!.startsWith('.'))) {
                val relativePath = it.uri.path.substringAfterLast("${activity.config.OTGPartition}:")
                paths.add("$OTG_PATH$relativePath")
            }
        }
        return paths
    }

    private fun askConfirmDelete() {
        if (config.skipDeleteConfirmation) {
            deleteFolders()
        } else {
            val itemsCnt = selectedKeys.size
            val items = resources.getQuantityString(R.plurals.delete_items, itemsCnt, itemsCnt)
            val fileDirItem = getFirstSelectedItem() ?: return
            val baseString = if (!config.useRecycleBin || (isOneItemSelected() && fileDirItem.isRecycleBin()) || (isOneItemSelected() && fileDirItem.areFavorites())) {
                R.string.deletion_confirmation
            } else {
                R.string.move_to_recycle_bin_confirmation
            }

            var question = String.format(resources.getString(baseString), items)
            val warning = resources.getQuantityString(R.plurals.delete_warning, itemsCnt, itemsCnt)
            question += "\n\n$warning"
            ConfirmationDialog(activity, question) {
                deleteFolders()
            }
        }
    }

    private fun deleteFolders() {
        if (selectedKeys.isEmpty()) {
            return
        }

        var SAFPath = ""
        val selectedDirs = getSelectedItems()
        selectedDirs.forEach {
            val path = it.path
            if (activity.needsStupidWritePermissions(path) && config.treeUri.isEmpty()) {
                SAFPath = path
            }
        }

        activity.handleSAFDialog(SAFPath) {
            val foldersToDelete = ArrayList<File>(selectedKeys.size)
            selectedDirs.forEach {
                if (it.areFavorites() || it.isRecycleBin()) {
                    if (it.isRecycleBin()) {
                        tryEmptyRecycleBin(false)
                    } else {
                        Thread {
                            activity.galleryDB.MediumDao().clearFavorites()
                            listener?.refreshItems()
                        }.start()
                    }

                    if (selectedKeys.size == 1) {
                        finishActMode()
                    }
                } else {
                    foldersToDelete.add(File(it.path))
                }
            }

            listener?.deleteFolders(foldersToDelete)
        }
    }

    private fun changeAlbumCover(useDefault: Boolean) {
        if (selectedKeys.size != 1)
            return

        val path = getFirstSelectedItemPath() ?: return

        if (useDefault) {
            val albumCovers = getAlbumCoversWithout(path)
            storeCovers(albumCovers)
        } else {
            pickMediumFrom(path, path)
        }
    }

    private fun pickMediumFrom(targetFolder: String, path: String) {
        PickMediumDialog(activity, path) {
            if (File(it).isDirectory) {
                pickMediumFrom(targetFolder, it)
            } else {
                val albumCovers = getAlbumCoversWithout(path)
                val cover = AlbumCover(targetFolder, it)
                albumCovers.add(cover)
                storeCovers(albumCovers)
            }
        }
    }

    private fun getAlbumCoversWithout(path: String) = config.parseAlbumCovers().filterNot { it.path == path } as ArrayList

    private fun storeCovers(albumCovers: ArrayList<AlbumCover>) {
        activity.config.albumCovers = Gson().toJson(albumCovers)
        finishActMode()
        listener?.refreshItems()
    }

    private fun getSelectedItems() = dirs.filter { selectedKeys.contains(it.path.hashCode()) } as ArrayList<Directory>

    private fun getSelectedPaths() = getSelectedItems().map { it.path } as ArrayList<String>

    private fun getFirstSelectedItem() = getItemWithKey(selectedKeys.first())

    private fun getFirstSelectedItemPath() = getFirstSelectedItem()?.path

    private fun getItemWithKey(key: Int): Directory? = dirs.firstOrNull { it.path.hashCode() == key }

    fun updateDirs(newDirs: ArrayList<Directory>) {
        val directories = newDirs.clone() as ArrayList<Directory>
        if (directories.hashCode() != currentDirectoriesHash) {
            currentDirectoriesHash = directories.hashCode()
            dirs = directories
            notifyDataSetChanged()
            finishActMode()
        }
    }

    fun updateAnimateGifs(animateGifs: Boolean) {
        this.animateGifs = animateGifs
        notifyDataSetChanged()
    }

    fun updateCropThumbnails(cropThumbnails: Boolean) {
        this.cropThumbnails = cropThumbnails
        notifyDataSetChanged()
    }

    fun updateShowMediaCount(showMediaCount: Boolean) {
        this.showMediaCount = showMediaCount
        notifyDataSetChanged()
    }

    private fun setupView(view: View, directory: Directory) {
        val isSelected = selectedKeys.contains(directory.path.hashCode())
        view.apply {
            dir_name.text = directory.name
            dir_path?.text = "${directory.path.substringBeforeLast("/")}/"
            photo_cnt.text = directory.mediaCnt.toString()
            val thumbnailType = when {
                directory.tmb.isVideoFast() -> TYPE_VIDEOS
                directory.tmb.isGif() -> TYPE_GIFS
                directory.tmb.isRawFast() -> TYPE_RAWS
                directory.tmb.isSvg() -> TYPE_SVGS
                else -> TYPE_IMAGES
            }

            dir_check?.beVisibleIf(isSelected)
            if (isSelected) {
                dir_check.background?.applyColorFilter(primaryColor)
            }

            activity.loadImage(thumbnailType, directory.tmb, dir_thumbnail, scrollHorizontally, animateGifs, cropThumbnails)
            dir_pin.beVisibleIf(pinnedFolders.contains(directory.path))
            dir_location.beVisibleIf(directory.location != LOCAITON_INTERNAL)
            if (dir_location.isVisible()) {
                dir_location.setImageResource(if (directory.location == LOCATION_SD) R.drawable.ic_sd_card else R.drawable.ic_usb)
            }

            photo_cnt.beVisibleIf(showMediaCount)

            if (isListViewType) {
                dir_name.setTextColor(textColor)
                dir_path.setTextColor(textColor)
                photo_cnt.setTextColor(textColor)
                dir_pin.applyColorFilter(textColor)
                dir_location.applyColorFilter(textColor)
            }
        }
    }
}
