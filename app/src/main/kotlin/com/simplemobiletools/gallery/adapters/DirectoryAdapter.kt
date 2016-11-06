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
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.directory_item.view.*
import kotlinx.android.synthetic.main.directory_tmb.view.*

class DirectoryAdapter(private val mContext: Context, private val mDirs: MutableList<Directory>) : BaseAdapter() {
    private val mInflater: LayoutInflater

    init {
        mInflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view
        val viewHolder: ViewHolder
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.directory_item, parent, false)
            viewHolder = ViewHolder(convertView)
            convertView!!.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        val dir = mDirs[position]
        viewHolder.dirName.text = formatDirectoryName(dir)
        viewHolder.photoCnt.text = dir.mediaCnt.toString()
        val tmb = dir.thumbnail
        val timestampSignature = StringSignature(dir.timestamp.toString())
        if (tmb.endsWith(".gif")) {
            Glide.with(mContext).load(tmb).asGif().diskCacheStrategy(DiskCacheStrategy.NONE).signature(timestampSignature)
                    .placeholder(R.color.tmb_background).centerCrop().crossFade().into(viewHolder.dirThumbnail)
        } else {
            Glide.with(mContext).load(tmb).diskCacheStrategy(DiskCacheStrategy.RESULT).signature(timestampSignature)
                    .placeholder(R.color.tmb_background).centerCrop().crossFade().into(viewHolder.dirThumbnail)
        }

        return convertView
    }

    private fun formatDirectoryName(dir: Directory): String {
        return dir.name
    }

    override fun getCount(): Int {
        return mDirs.size
    }

    override fun getItem(position: Int): Any {
        return mDirs[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    internal class ViewHolder(view: View) {
        val dirName: TextView = view.dir_name
        val photoCnt: TextView = view.photo_cnt
        val dirThumbnail: ImageView = view.dir_thumbnail
    }
}
