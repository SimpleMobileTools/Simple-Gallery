package com.simplemobiletools.gallery.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.StringSignature
import com.simplemobiletools.gallery.Config
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.beVisibleIf
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.photo_video_item.view.*
import kotlinx.android.synthetic.main.photo_video_tmb.view.*

class MediaAdapter(private val context: Context, private val media: MutableList<Medium>) : BaseAdapter() {
    private val mInflater: LayoutInflater
    var displayFilenames = false

    init {
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        displayFilenames = Config.newInstance(context).displayFileNames
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        val medium = media[position]
        val viewHolder: ViewHolder
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.photo_video_item, parent, false)
            viewHolder = ViewHolder(convertView)
            convertView!!.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        viewHolder.playOutline.visibility = if (medium.isVideo) View.VISIBLE else View.GONE

        viewHolder.fileName.beVisibleIf(displayFilenames)
        if (displayFilenames)
            viewHolder.fileName.text = medium.name
        val path = medium.path
        val timestampSignature = StringSignature(medium.timestamp.toString())
        if (medium.isGif) {
            Glide.with(context).load(path).asGif().diskCacheStrategy(DiskCacheStrategy.NONE).signature(timestampSignature).into(viewHolder.photoThumbnail)
        } else {
            Glide.with(context).load(path).diskCacheStrategy(DiskCacheStrategy.NONE).signature(timestampSignature)
                    .placeholder(R.color.tmb_background).centerCrop().crossFade().into(viewHolder.photoThumbnail)
        }

        return convertView
    }

    fun updateDisplayFilenames(display: Boolean) {
        displayFilenames = display
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return media.size
    }

    override fun getItem(position: Int): Any {
        return media[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    fun updateItems(newPhotos: List<Medium>) {
        media.clear()
        media.addAll(newPhotos)
        notifyDataSetChanged()
    }

    internal class ViewHolder(view: View) {
        val photoThumbnail: ImageView = view.medium_thumbnail
        val playOutline: View = view.play_outline
        val fileName: TextView = view.file_name
    }
}
