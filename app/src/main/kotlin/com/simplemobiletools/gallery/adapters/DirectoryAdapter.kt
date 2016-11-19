package com.simplemobiletools.gallery.adapters

import android.os.Build
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.view.*
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.StringSignature
import com.simplemobiletools.filepicker.asynctasks.CopyMoveTask
import com.simplemobiletools.filepicker.dialogs.ConfirmationDialog
import com.simplemobiletools.filepicker.extensions.isAStorageRootFolder
import com.simplemobiletools.filepicker.extensions.scanPaths
import com.simplemobiletools.filepicker.extensions.toast
import com.simplemobiletools.fileproperties.dialogs.PropertiesDialog
import com.simplemobiletools.gallery.Config
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.dialogs.CopyDialog
import com.simplemobiletools.gallery.dialogs.RenameDirectoryDialog
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.directory_item.view.*
import kotlinx.android.synthetic.main.directory_tmb.view.*
import java.io.File
import java.util.*

class DirectoryAdapter(val activity: SimpleActivity, val dirs: MutableList<Directory>, val listener: DirOperationsListener?, val itemClick: (Directory) -> Unit) :
        RecyclerView.Adapter<DirectoryAdapter.ViewHolder>() {

    val multiSelector = MultiSelector()
    val views = ArrayList<View>()
    val config = Config.newInstance(activity)

    companion object {
        var actMode: ActionMode? = null

        fun toggleItemSelection(itemView: View, select: Boolean) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                itemView.dir_frame.isActivated = select
            else
                itemView.dir_thumbnail.isActivated = select
        }
    }

    val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.cab_properties -> {
                    showProperties()
                    true
                }
                R.id.cab_edit -> {
                    editDir()
                    true
                }
                R.id.cab_hide -> {
                    hideDirs()
                    mode.finish()
                    true
                }
                R.id.cab_unhide -> {
                    unhideDir()
                    mode.finish()
                    true
                }
                R.id.cab_copy_move -> {
                    displayCopyDialog()
                    true
                }
                R.id.cab_delete -> {
                    askConfirmDelete()
                    true
                }
                else -> false
            }
        }

        override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
            super.onCreateActionMode(actionMode, menu)
            actMode = actionMode
            activity.menuInflater.inflate(R.menu.cab_directories, menu)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu): Boolean {
            val menuItem = menu.findItem(R.id.cab_edit)
            menuItem.isVisible = multiSelector.selectedPositions.size <= 1

            var hiddenCnt = 0
            var unhiddenCnt = 0
            val positions = multiSelector.selectedPositions
            for (i in positions) {
                val path = dirs[i].path
                if (config.getIsFolderHidden(path))
                    hiddenCnt++
                else
                    unhiddenCnt++
            }

            menu.findItem(R.id.cab_hide).isVisible = unhiddenCnt > 0
            menu.findItem(R.id.cab_unhide).isVisible = hiddenCnt > 0
            return true
        }

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            views.forEach { toggleItemSelection(it, false) }
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

    private fun editDir() {
        val path = dirs[multiSelector.selectedPositions[0]].path
        val dir = File(path)
        if (activity.isAStorageRootFolder(dir.absolutePath)) {
            activity.toast(R.string.rename_folder_root)
            return
        }

        RenameDirectoryDialog(activity, dir, object : RenameDirectoryDialog.OnRenameDirListener {
            override fun onRenameDirSuccess(changedPaths: ArrayList<String>) {
                activity.scanPaths(changedPaths) {
                    activity.runOnUiThread {
                        actMode?.finish()
                        listener?.refreshItems()
                        activity.toast(R.string.rename_folder_ok)
                    }
                }
            }
        })
    }

    private fun hideDirs() {
        config.addHiddenDirectories(getSelectedPaths())
        listener?.refreshItems()
    }

    private fun unhideDir() {
        config.removeHiddenDirectories(getSelectedPaths())
        listener?.refreshItems()
    }

    private fun displayCopyDialog() {
        val files = ArrayList<File>()
        val positions = multiSelector.selectedPositions
        positions.forEach { files.add(File(dirs[it].path)) }

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

    private fun askConfirmDelete() {
        ConfirmationDialog(activity, listener = object : ConfirmationDialog.OnConfirmedListener {
            override fun onConfirmed() {
                actMode?.finish()
                prepareForDeleting()
            }
        })
    }

    private fun prepareForDeleting() {
        val selections = multiSelector.selectedPositions
        val paths = ArrayList<String>(selections.size)
        selections.forEach { paths.add(dirs[it].path.toLowerCase()) }
        listener?.prepareForDeleting(paths)
    }

    private fun getSelectedPaths(): HashSet<String> {
        val positions = multiSelector.selectedPositions
        val paths = HashSet<String>()
        positions.forEach { paths.add(dirs[it].path) }
        return paths
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        views.add(holder.bindView(activity, multiSelectorMode, multiSelector, dirs[position]))
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.directory_item, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun getItemCount() = dirs.size

    class ViewHolder(view: View, val itemClick: (Directory) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(activity: SimpleActivity, multiSelectorCallback: ModalMultiSelectorCallback, multiSelector: MultiSelector, directory: Directory): View {
            itemView.dir_name.text = directory.name
            itemView.photo_cnt.text = directory.mediaCnt.toString()

            val tmb = directory.thumbnail
            val timestampSignature = StringSignature(directory.timestamp.toString())
            if (tmb.endsWith(".gif")) {
                Glide.with(activity.applicationContext).load(tmb).asGif().diskCacheStrategy(DiskCacheStrategy.NONE).signature(timestampSignature)
                        .placeholder(R.color.tmb_background).centerCrop().crossFade().into(itemView.dir_thumbnail)
            } else {
                Glide.with(activity.applicationContext).load(tmb).diskCacheStrategy(DiskCacheStrategy.RESULT).signature(timestampSignature)
                        .placeholder(R.color.tmb_background).centerCrop().crossFade().into(itemView.dir_thumbnail)
            }

            itemView.setOnClickListener { viewClicked(multiSelector, directory) }
            itemView.setOnLongClickListener {
                if (!multiSelector.isSelectable) {
                    activity.startSupportActionMode(multiSelectorCallback)
                    multiSelector.setSelected(this, true)
                    actMode?.title = multiSelector.selectedPositions.size.toString()
                    toggleItemSelection(itemView, true)
                    actMode?.invalidate()
                }
                true
            }
            return itemView
        }

        fun viewClicked(multiSelector: MultiSelector, directory: Directory) {
            if (multiSelector.isSelectable) {
                val isSelected = multiSelector.selectedPositions.contains(layoutPosition)
                multiSelector.setSelected(this, !isSelected)
                toggleItemSelection(itemView, !isSelected)

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

        fun prepareForDeleting(paths: ArrayList<String>)
    }
}
