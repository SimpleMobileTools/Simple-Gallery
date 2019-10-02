package com.simplemobiletools.gallery.pro.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.simplemobiletools.commons.extensions.getFileKey
import com.simplemobiletools.gallery.pro.R
import kotlinx.android.synthetic.main.portrait_photo_item.view.*
import java.util.*

class PortraitPhotosAdapter(val context: Context, val photos: ArrayList<String>, val itemClick: (Int) -> Unit) : RecyclerView.Adapter<PortraitPhotosAdapter.ViewHolder>() {

    private var currentSelection = photos.first()
    private var strokeBackground = context.resources.getDrawable(R.drawable.stroke_background)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(photos[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.portrait_photo_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = photos.size

    fun getCurrentPhoto() = currentSelection

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(photo: String): View {
            itemView.apply {
                portrait_photo_item_thumbnail.background = if (getCurrentPhoto() == photo) {
                    strokeBackground
                } else {
                    null
                }

                val options = RequestOptions()
                        .signature(ObjectKey(photo.getFileKey()))
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .centerCrop()

                Glide.with(context)
                        .load(photo)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .apply(options)
                        .into(portrait_photo_item_thumbnail)
            }
            return itemView
        }
    }
}
