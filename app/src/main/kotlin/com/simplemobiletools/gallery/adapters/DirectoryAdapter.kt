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
import com.simplemobiletools.commons.extensions.isAStorageRootFolder
import com.simplemobiletools.commons.extensions.isImageVideoGif
import com.simplemobiletools.commons.extensions.needsStupidWritePermissions
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.dialogs.ExcludeFolderDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.directory_item.view.*
import kotlinx.android.synthetic.main.directory_tmb.view.*
import java.io.File
import java.util.*

class DirectoryAdapter(val activity: SimpleActivity, val dirs: MutableList<Directory>, val listener: DirOperationsListener?, val itemClick: (Directory) -> Unit) :
        RecyclerView.Adapter<DirectoryAdapter.ViewHolder>() {

    val multiSelector = MultiSelector()
    val views = ArrayList<View>()
    val config = activity.config
    var pinnedFolders = config.pinnedFolders

    companion object {
        var actMode: ActionMode? = null
        val markedItems = HashSet<Int>()
        var foregroundColor = 0
        var backgroundColor = 0
        var animateGifs = true
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
                itemView.dir_frame
            else
                itemView.dir_thumbnail
        }

        fun updateTitle(cnt: Int) {
            actMode?.title = "$cnt / $itemCnt"
        }
    }

    init {
        foregroundColor = config.primaryColor
        backgroundColor = config.backgroundColor
        animateGifs = config.animateGifs
        itemCnt = dirs.size
    }

    val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.cab_properties -> showProperties()
                R.id.cab_rename -> renameDir()
                R.id.cab_pin -> pinFolders(true)
                R.id.cab_unpin -> pinFolders(false)
                R.id.cab_hide -> toggleFoldersVisibility(true)
                R.id.cab_unhide -> toggleFoldersVisibility(false)
                R.id.cab_exclude -> tryExcludeFolder()
                R.id.cab_copy_to -> copyTo()
                R.id.cab_move_to -> moveTo()
                R.id.cab_select_all -> selectAll()
                R.id.cab_delete -> askConfirmDelete()
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
            val positions = multiSelector.selectedPositions
            menu.findItem(R.id.cab_rename).isVisible = positions.size <= 1

            checkHideBtnVisibility(menu, positions)
            checkPinBtnVisibility(menu, positions)

            return true
        }

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            views.forEach { toggleItemSelection(it, false) }
            markedItems.clear()
        }

        fun checkHideBtnVisibility(menu: Menu, positions: List<Int>) {
            var hiddenCnt = 0
            var unhiddenCnt = 0
            positions.map { dirs[it].path }.forEach {
                if (File(it).containsNoMedia())
                    hiddenCnt++
                else
                    unhiddenCnt++
            }

            menu.findItem(R.id.cab_hide).isVisible = unhiddenCnt > 0
            menu.findItem(R.id.cab_unhide).isVisible = hiddenCnt > 0
        }

        fun checkPinBtnVisibility(menu: Menu, positions: List<Int>) {
            val pinnedFolders = config.pinnedFolders
            var pinnedCnt = 0
            var unpinnedCnt = 0
            positions.map { dirs[it].path }.forEach {
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
        val selections = multiSelector.selectedPositions
        if (selections.size <= 1) {
            PropertiesDialog(activity, dirs[selections[0]].path, config.shouldShowHidden)
        } else {
            val paths = ArrayList<String>()
            selections.forEach { paths.add(dirs[it].path) }
            PropertiesDialog(activity, paths, config.shouldShowHidden)
        }
    }

    private fun renameDir() {
        val path = dirs[multiSelector.selectedPositions[0]].path
        val dir = File(path)
        if (activity.isAStorageRootFolder(dir.absolutePath)) {
            activity.toast(R.string.rename_folder_root)
            return
        }

        RenameItemDialog(activity, dir.absolutePath) {
            activity.runOnUiThread {
                actMode?.finish()
                listener?.refreshItems()
            }
        }
    }

    private fun toggleFoldersVisibility(hide: Boolean) {
        val paths = getSelectedPaths()
        for (path in paths) {
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

    private fun displayCopyDialog() {
        val files = ArrayList<File>()
        val positions = multiSelector.selectedPositions
        if (positions.isEmpty())
            return

        positions.forEach {
            val dir = File(dirs[it].path)
            files.addAll(dir.listFiles().filter { it.isFile && it.isImageVideoGif() })
        }
    }

    private fun copyTo() {

    }

    private fun moveTo() {

    }

    fun selectAll() {
        val cnt = dirs.size
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

    private fun deleteFiles() {
        val selections = multiSelector.selectedPositions
        val folders = ArrayList<File>(selections.size)
        val removeFolders = ArrayList<Directory>(selections.size)

        var needPermissionForPath = ""
        selections.forEach {
            val path = dirs[it].path
            if (activity.needsStupidWritePermissions(path) && activity.config.treeUri.isEmpty()) {
                needPermissionForPath = path
            }
        }

        activity.handleSAFDialog(File(needPermissionForPath)) {
            selections.reverse()
            selections.forEach {
                val directory = dirs[it]
                folders.add(File(directory.path))
                removeFolders.add(directory)
                notifyItemRemoved(it)
            }

            dirs.removeAll(removeFolders)
            markedItems.clear()
            listener?.tryDeleteFolders(folders)
            itemCnt = dirs.size
        }
    }

    private fun getSelectedPaths(): HashSet<String> {
        val positions = multiSelector.selectedPositions
        val paths = HashSet<String>(positions.size)
        positions.forEach { paths.add(dirs[it].path) }
        return paths
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.directory_item, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dir = dirs[position]
        views.add(holder.bindView(activity, multiSelectorMode, multiSelector, dir, position, pinnedFolders.contains(dir.path)))
    }

    override fun onViewRecycled(holder: ViewHolder?) {
        super.onViewRecycled(holder)
        holder?.stopLoad()
    }

    override fun getItemCount() = dirs.size

    class ViewHolder(val view: View, val itemClick: (Directory) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(activity: SimpleActivity, multiSelectorCallback: ModalMultiSelectorCallback, multiSelector: MultiSelector, directory: Directory, pos: Int, isPinned: Boolean)
                : View {
            itemView.apply {
                dir_name.text = directory.name
                photo_cnt.text = directory.mediaCnt.toString()
                dir_pin.visibility = if (isPinned) View.VISIBLE else View.GONE
                toggleItemSelection(this, markedItems.contains(pos), pos)
                activity.loadImage(directory.thumbnail, dir_thumbnail)

                setOnClickListener { viewClicked(multiSelector, directory, pos) }
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

        fun viewClicked(multiSelector: MultiSelector, directory: Directory, pos: Int) {
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
                itemClick(directory)
            }
        }

        fun stopLoad() {
            Glide.clear(view.dir_thumbnail)
        }
    }

    interface DirOperationsListener {
        fun refreshItems()

        fun tryDeleteFolders(folders: ArrayList<File>)
    }
}
