package com.simplemobiletools.gallery.adapters

import android.util.SparseArray
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import kotlinx.android.synthetic.main.item_manage_folder.view.*
import java.util.*

class ManageFoldersAdapter(activity: BaseSimpleActivity, var folders: ArrayList<String>, val isShowingExcludedFolders: Boolean, val listener: RefreshRecyclerViewListener?,
                           recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    private val config = activity.config

    init {
        selectableItemCount = folders.size
    }

    override fun getActionMenuId() = R.menu.cab_delete_only

    override fun prepareActionMode(menu: Menu) {}

    override fun prepareItemSelection(view: View) {}

    override fun markItemSelection(select: Boolean, view: View?) {
        view?.manage_folder_holder?.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) = createViewHolder(R.layout.item_manage_folder, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = folders[position]
        val view = holder.bindView(folder) { itemView, layoutPosition ->
            setupView(itemView, folder)
        }
        bindViewHolder(holder, position, view)
    }

    override fun getItemCount() = folders.size

    private fun setupView(view: View, folder: String) {
        view.apply {
            manage_folder_title.apply {
                text = folder
                setTextColor(config.textColor)
            }
        }
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            deleteSelection()
        }
    }

    private fun deleteSelection() {
        val removeFolders = ArrayList<String>(selectedPositions.size)

        selectedPositions.sortedDescending().forEach {
            val folder = folders[it]
            removeFolders.add(folder)
            notifyItemRemoved(it)
            itemViews.put(it, null)
            if (isShowingExcludedFolders) {
                config.removeExcludedFolder(folder)
            } else {
                config.removeIncludedFolder(folder)
            }
        }

        folders.removeAll(removeFolders)
        selectedPositions.clear()

        val newItems = SparseArray<View>()
        (0 until itemViews.size())
                .filter { itemViews[it] != null }
                .forEachIndexed { curIndex, i -> newItems.put(curIndex, itemViews[i]) }

        itemViews = newItems
        selectableItemCount = folders.size
        finishActMode()
        if (folders.isEmpty()) {
            listener?.refreshItems()
        }
    }
}
