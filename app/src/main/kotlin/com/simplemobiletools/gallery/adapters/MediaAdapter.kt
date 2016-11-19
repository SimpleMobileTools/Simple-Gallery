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
import com.simplemobiletools.gallery.Config
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.extensions.beVisibleIf
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.photo_video_item.view.*
import kotlinx.android.synthetic.main.photo_video_tmb.view.*
import java.util.*

class MediaAdapter(val activity: SimpleActivity, val media: MutableList<Medium>, val listener: MediaAdapter.MediaOperationsListener?, val itemClick: (Medium) -> Unit) :
        RecyclerView.Adapter<MediaAdapter.ViewHolder>() {
    val multiSelector = MultiSelector()
    val views = ArrayList<View>()
    val config = Config.newInstance(activity)

    companion object {
        var actMode: ActionMode? = null
        var displayFilenames = false

        fun toggleItemSelection(itemView: View, select: Boolean) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                itemView.medium_thumbnail_holder.isActivated = select
            else
                itemView.medium_thumbnail.isActivated = select
        }
    }

    val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return false
        }

        override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
            super.onCreateActionMode(actionMode, menu)
            DirectoryAdapter.actMode = actionMode
            activity.menuInflater.inflate(R.menu.cab_media, menu)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu): Boolean {
            val menuItem = menu.findItem(R.id.cab_edit)
            menuItem.isVisible = multiSelector.selectedPositions.size <= 1
            return true
        }

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            views.forEach { MediaAdapter.toggleItemSelection(it, false) }
        }
    }

    override fun onBindViewHolder(holder: MediaAdapter.ViewHolder, position: Int) {
        views.add(holder.bindView(activity, multiSelectorMode, multiSelector, media[position]))
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MediaAdapter.ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.photo_video_item, parent, false)
        return MediaAdapter.ViewHolder(view, itemClick)
    }

    fun updateDisplayFilenames(display: Boolean) {
        displayFilenames = display
        notifyDataSetChanged()
    }

    override fun getItemCount() = media.size

    class ViewHolder(view: View, val itemClick: (Medium) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(activity: SimpleActivity, multiSelectorCallback: ModalMultiSelectorCallback, multiSelector: MultiSelector, medium: Medium): View {
            itemView.play_outline.visibility = if (medium.isVideo) View.VISIBLE else View.GONE
            itemView.file_name.beVisibleIf(displayFilenames)
            itemView.file_name.text = medium.name

            val path = medium.path
            val timestampSignature = StringSignature(medium.timestamp.toString())
            if (medium.isGif) {
                Glide.with(activity).load(path).asGif().diskCacheStrategy(DiskCacheStrategy.NONE).signature(timestampSignature).into(itemView.medium_thumbnail)
            } else {
                Glide.with(activity).load(path).diskCacheStrategy(DiskCacheStrategy.NONE).signature(timestampSignature)
                        .placeholder(R.color.tmb_background).centerCrop().crossFade().into(itemView.medium_thumbnail)
            }

            itemView.setOnClickListener { viewClicked(multiSelector, medium) }

            return itemView
        }

        fun viewClicked(multiSelector: MultiSelector, medium: Medium) {
            if (multiSelector.isSelectable) {

            } else {
                itemClick(medium)
            }
        }
    }

    interface MediaOperationsListener {

    }
}
