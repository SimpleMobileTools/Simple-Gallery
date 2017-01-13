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
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.StringSignature
import com.simplemobiletools.commons.asynctasks.CopyMoveTask
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.extensions.isAStorageRootFolder
import com.simplemobiletools.commons.extensions.isImageVideoGif
import com.simplemobiletools.commons.extensions.scanPaths
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.dialogs.CopyDialog
import com.simplemobiletools.gallery.dialogs.RenameDirectoryDialog
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.createSelector
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
    }

    init {
        foregroundColor = config.primaryColor
        backgroundColor = config.backgroundColor
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
                R.id.cab_copy_move -> displayCopyDialog()
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
                if (config.getIsFolderHidden(it))
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
            PropertiesDialog(activity, dirs[selections[0]].path, config.showHiddenFolders)
        } else {
            val paths = ArrayList<String>()
            selections.forEach { paths.add(dirs[it].path) }
            PropertiesDialog(activity, paths, config.showHiddenFolders)
        }
    }

    private fun renameDir() {
        val path = dirs[multiSelector.selectedPositions[0]].path
        val dir = File(path)
        if (activity.isAStorageRootFolder(dir.absolutePath)) {
            activity.toast(R.string.rename_folder_root)
            return
        }

        RenameDirectoryDialog(activity, dir) {
            activity.scanPaths(it) {
                activity.runOnUiThread {
                    actMode?.finish()
                    listener?.refreshItems()
                    activity.toast(R.string.rename_folder_ok)
                }
            }
        }
    }

    private fun toggleFoldersVisibility(hide: Boolean) {
        if (hide)
            config.addHiddenFolders(getSelectedPaths())
        else
            config.removeHiddenFolders(getSelectedPaths())

        listener?.refreshItems()
        actMode?.finish()
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
        positions.forEach {
            val dir = File(dirs[it].path)
            files.addAll(dir.listFiles().filter { it.isFile && it.isImageVideoGif() })
        }

        CopyDialog(activity, files, object : CopyMoveTask.CopyMoveListener {
            override fun copySucceeded(deleted: Boolean, copiedAll: Boolean) {
                if (deleted) {
                    activity.toast(if (copiedAll) R.string.moving_success else R.string.moving_success_partial)
                } else {
                    activity.toast(if (copiedAll) R.string.copying_success else R.string.copying_success_partial)
                }
                listener?.refreshItems()
                actMode?.finish()
            }

            override fun copyFailed() {
                activity.toast(R.string.copy_move_failed)
            }
        })
    }

    fun selectAll() {
        val cnt = dirs.size
        for (i in 0..cnt - 1) {
            markedItems.add(i)
            multiSelector.setSelected(i, 0, true)
            notifyItemChanged(i)
        }
        actMode?.title = cnt.toString()
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
        val paths = ArrayList<String>(selections.size)
        val removeDirs = ArrayList<Directory>(selections.size)

        selections.reverse()
        selections.forEach {
            val directory = dirs[it]
            paths.add(directory.path)
            removeDirs.add(directory)
            notifyItemRemoved(it)
        }

        dirs.removeAll(removeDirs)
        listener?.deleteFiles(paths)
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

    override fun getItemCount() = dirs.size

    class ViewHolder(view: View, val itemClick: (Directory) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(activity: SimpleActivity, multiSelectorCallback: ModalMultiSelectorCallback, multiSelector: MultiSelector, directory: Directory, pos: Int, isPinned: Boolean)
                : View {
            itemView.dir_name.text = directory.name
            itemView.photo_cnt.text = directory.mediaCnt.toString()
            itemView.dir_pin.visibility = if (isPinned) View.VISIBLE else View.GONE
            toggleItemSelection(itemView, markedItems.contains(pos), pos)

            val tmb = directory.thumbnail
            val timestampSignature = StringSignature(directory.date_modified.toString())
            if (tmb.toLowerCase().endsWith(".gif")) {
                Glide.with(activity).load(tmb).asGif().diskCacheStrategy(DiskCacheStrategy.NONE).signature(timestampSignature)
                        .placeholder(backgroundColor).centerCrop().crossFade().into(itemView.dir_thumbnail)
            } else if (tmb.toLowerCase().endsWith(".png")) {
                Glide.with(activity).load(tmb).asBitmap().format(DecodeFormat.PREFER_ARGB_8888).diskCacheStrategy(DiskCacheStrategy.RESULT)
                        .signature(timestampSignature).placeholder(backgroundColor).centerCrop().into(itemView.dir_thumbnail)
            } else {
                Glide.with(activity).load(tmb).diskCacheStrategy(DiskCacheStrategy.RESULT).signature(timestampSignature)
                        .placeholder(backgroundColor).centerCrop().crossFade().into(itemView.dir_thumbnail)
            }

            itemView.setOnClickListener { viewClicked(multiSelector, directory, pos) }
            itemView.setOnLongClickListener {
                if (!multiSelector.isSelectable) {
                    activity.startSupportActionMode(multiSelectorCallback)
                    multiSelector.setSelected(this, true)
                    actMode?.title = multiSelector.selectedPositions.size.toString()
                    toggleItemSelection(itemView, true, pos)
                    actMode?.invalidate()
                }
                true
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                (getProperView(itemView) as FrameLayout).foreground = foregroundColor.createSelector()
            else
                getProperView(itemView).foreground = foregroundColor.createSelector()
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
                    actMode?.title = selectedCnt.toString()
                }
                actMode?.invalidate()
            } else {
                itemClick(directory)
            }
        }
    }

    interface DirOperationsListener {
        fun refreshItems()

        fun deleteFiles(paths: ArrayList<String>)
    }
}
