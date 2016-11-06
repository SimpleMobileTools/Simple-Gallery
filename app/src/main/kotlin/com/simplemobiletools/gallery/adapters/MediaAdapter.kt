package com.simplemobiletools.gallery.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.StringSignature
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.photo_video_item.view.*
import kotlinx.android.synthetic.main.photo_video_tmb.view.*

class MediaAdapter(private val mContext: Context, private val mMedia: MutableList<Medium>) : BaseAdapter() {
    private val mInflater: LayoutInflater

    init {
        mInflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        val medium = mMedia[position]
        val viewHolder: ViewHolder
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.photo_video_item, parent, false)
            viewHolder = ViewHolder(convertView)
            convertView!!.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        viewHolder.playOutline.visibility = if (medium.isVideo) View.VISIBLE else View.GONE

        val path = medium.path
        val timestampSignature = StringSignature(medium.timestamp.toString())
        if (medium.isGif) {
            Glide.with(mContext).load(path).asGif().diskCacheStrategy(DiskCacheStrategy.NONE).signature(timestampSignature).into(viewHolder.photoThumbnail)
        } else {
            Glide.with(mContext).load(path).diskCacheStrategy(DiskCacheStrategy.NONE).signature(timestampSignature)
                    .placeholder(R.color.tmb_background).centerCrop().crossFade().into(viewHolder.photoThumbnail)
        }

        return convertView
    }

    override fun getCount(): Int {
        return mMedia.size
    }

    override fun getItem(position: Int): Any {
        return mMedia[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    fun updateItems(newPhotos: List<Medium>) {
        mMedia.clear()
        mMedia.addAll(newPhotos)
        notifyDataSetChanged()
    }

    internal class ViewHolder(view: View) {
        val photoThumbnail: ImageView = view.medium_thumbnail
        val playOutline: View = view.play_outline
    }
}
