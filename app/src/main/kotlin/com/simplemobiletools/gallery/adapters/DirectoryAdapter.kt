package com.simplemobiletools.gallery.adapters

import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.view.*
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.StringSignature
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.directory_item.view.*
import kotlinx.android.synthetic.main.directory_tmb.view.*
import java.util.*

class DirectoryAdapter(val activity: SimpleActivity, val dirs: MutableList<Directory>, val itemClick: (Directory) -> Unit) :
        RecyclerView.Adapter<DirectoryAdapter.ViewHolder>() {

    val multiSelector = MultiSelector()
    val views = ArrayList<View>()

    companion object {
        var actMode: ActionMode? = null
    }

    val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.cab_edit -> {
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

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
            val menuItem = menu?.findItem(R.id.cab_edit)
            menuItem?.isVisible = multiSelector.selectedPositions.size == 1
            return true
        }

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            views.forEach { it.dir_thumbnail.isSelected = false }
        }
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
                    itemView.dir_thumbnail.isSelected = true
                }
                true
            }
            return itemView
        }

        fun viewClicked(multiSelector: MultiSelector, directory: Directory) {
            if (multiSelector.isSelectable) {
                val isSelected = multiSelector.selectedPositions.contains(layoutPosition)
                multiSelector.setSelected(this, !isSelected)
                itemView.dir_thumbnail.isSelected = !isSelected

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
}
