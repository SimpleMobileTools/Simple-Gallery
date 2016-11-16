package com.simplemobiletools.gallery.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.StringSignature
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.directory_item.view.*
import kotlinx.android.synthetic.main.directory_tmb.view.*

class DirectoryAdapter(val mContext: Context, val mDirs: MutableList<Directory>, val itemClick: (Directory) -> Unit) :
        RecyclerView.Adapter<DirectoryAdapter.ViewHolder>() {

    override fun getItemCount() = mDirs.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(mContext, mDirs[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.directory_item, parent, false)
        return ViewHolder(view, itemClick)
    }

    class ViewHolder(view: View, val itemClick: (Directory) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(context: Context, directory: Directory) {
            itemView.dir_name.text = directory.name
            itemView.photo_cnt.text = directory.mediaCnt.toString()

            val tmb = directory.thumbnail
            val timestampSignature = StringSignature(directory.timestamp.toString())
            if (tmb.endsWith(".gif")) {
                Glide.with(context).load(tmb).asGif().diskCacheStrategy(DiskCacheStrategy.NONE).signature(timestampSignature)
                        .placeholder(R.color.tmb_background).centerCrop().crossFade().into(itemView.dir_thumbnail)
            } else {
                Glide.with(context).load(tmb).diskCacheStrategy(DiskCacheStrategy.RESULT).signature(timestampSignature)
                        .placeholder(R.color.tmb_background).centerCrop().crossFade().into(itemView.dir_thumbnail)
            }

            itemView.setOnClickListener { itemClick(directory) }
        }
    }
}
