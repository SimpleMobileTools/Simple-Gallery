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
import com.simplemobiletools.filepicker.dialogs.ConfirmationDialog
import com.simplemobiletools.fileproperties.dialogs.PropertiesDialog
import com.simplemobiletools.gallery.Config
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.extensions.beVisibleIf
import com.simplemobiletools.gallery.extensions.shareMedia
import com.simplemobiletools.gallery.extensions.shareMedium
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.photo_video_item.view.*
import kotlinx.android.synthetic.main.photo_video_tmb.view.*
import java.util.*

class MediaAdapter(val activity: SimpleActivity, var media: MutableList<Medium>, val listener: MediaOperationsListener?, val itemClick: (Medium) -> Unit) :
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
            return when (item.itemId) {
                R.id.cab_properties -> {
                    showProperties()
                    true
                }
                R.id.cab_share -> {
                    shareMedia()
                    return true
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
            activity.menuInflater.inflate(R.menu.cab_media, menu)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu) = true

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            views.forEach { toggleItemSelection(it, false) }
        }
    }

    private fun showProperties() {
        val selections = multiSelector.selectedPositions
        if (selections.size <= 1) {
            PropertiesDialog(activity, media[selections[0]].path, config.showHiddenFolders)
        } else {
            val paths = ArrayList<String>()
            selections.forEach { paths.add(media[it].path) }
            PropertiesDialog(activity, paths, config.showHiddenFolders)
        }
    }

    private fun shareMedia() {
        val selections = multiSelector.selectedPositions
        if (selections.size <= 1) {
            activity.shareMedium(getSelectedMedia()[0])
        } else {
            activity.shareMedia(getSelectedMedia())
        }
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
        selections.forEach { paths.add(media[it].path.toLowerCase()) }
        listener?.prepareForDeleting(paths)
    }

    private fun getSelectedMedia(): List<Medium> {
        val selections = multiSelector.selectedPositions
        val cnt = selections.size
        val selectedMedia = (0..cnt - 1)
                .map { media[selections[it]] }

        return selectedMedia
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        views.add(holder.bindView(activity, multiSelectorMode, multiSelector, media[position]))
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.photo_video_item, parent, false)
        return ViewHolder(view, itemClick)
    }

    fun updateDisplayFilenames(display: Boolean) {
        displayFilenames = display
        notifyDataSetChanged()
    }

    fun updateMedia(media: MutableList<Medium>) {
        this.media = media
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

        fun viewClicked(multiSelector: MultiSelector, medium: Medium) {
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
                itemClick(medium)
            }
        }
    }

    interface MediaOperationsListener {
        fun prepareForDeleting(paths: ArrayList<String>)
    }
}
