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
import com.simplemobiletools.gallery.pro.databinding.PortraitPhotoItemBinding
import java.util.*

class PortraitPhotosAdapter(
    val context: Context,
    val photos: ArrayList<String>,
    val sideElementWidth: Int,
    val itemClick: (Int, Int) -> Unit
) : RecyclerView.Adapter<PortraitPhotosAdapter.ViewHolder>() {
    var currentSelectionIndex = -1
    var views = HashMap<Int, View>()
    private var strokeBackground = context.resources.getDrawable(R.drawable.stroke_background)
    private val itemWidth = context.resources.getDimension(R.dimen.portrait_photos_stripe_height).toInt()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(photos[position], position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(PortraitPhotoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount() = photos.size

    fun setCurrentPhoto(position: Int) {
        if (currentSelectionIndex != position) {
            currentSelectionIndex = position
            notifyDataSetChanged()
        }
    }

    fun performClickOn(position: Int) {
        views[position]?.performClick()
    }

    inner class ViewHolder(private val binding: PortraitPhotoItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindView(photo: String, position: Int) {
            binding.portraitPhotoItemThumbnail.layoutParams.width = if (position == 0 || position == photos.size - 1) {
                sideElementWidth
            } else {
                itemWidth
            }
            binding.portraitPhotoItemThumbnail.background = if (photo.isEmpty() || position != currentSelectionIndex) {
                null
            } else {
                strokeBackground
            }

            val options = RequestOptions()
                    .signature(ObjectKey(photo.getFileKey()))
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .centerCrop()
            Glide.with(context)
                    .load(photo)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(options)
                    .into(binding.portraitPhotoItemThumbnail)

            if (photo.isNotEmpty()) {
                binding.root.isClickable = true
                views[position] = binding.root
                binding.root.setOnClickListener {
                    itemClick(position, binding.root.x.toInt())
                    setCurrentPhoto(position)
                }
            } else {
                binding.root.isClickable = false
            }
        }
    }
}
