package org.fossify.gallery.adapters

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
import org.fossify.commons.extensions.getFileKey
import org.fossify.gallery.R
import org.fossify.gallery.databinding.PortraitPhotoItemBinding

class PortraitPhotosAdapter(val context: Context, val photos: ArrayList<String>, val sideElementWidth: Int, val itemClick: (Int, Int) -> Unit) :
    RecyclerView.Adapter<PortraitPhotosAdapter.ViewHolder>() {

    var currentSelectionIndex = -1
    var views = HashMap<Int, View>()
    private var strokeBackground = context.resources.getDrawable(R.drawable.stroke_background)
    private val itemWidth = context.resources.getDimension(R.dimen.portrait_photos_stripe_height).toInt()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(photos[position], position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PortraitPhotoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding.root)
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

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(photo: String, position: Int): View {
            PortraitPhotoItemBinding.bind(itemView).apply {
                portraitPhotoItemThumbnail.layoutParams.width = if (position == 0 || position == photos.lastIndex) {
                    sideElementWidth
                } else {
                    itemWidth
                }

                portraitPhotoItemThumbnail.background = if (photo.isEmpty() || position != currentSelectionIndex) {
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
                    .into(portraitPhotoItemThumbnail)

                if (photo.isNotEmpty()) {
                    root.isClickable = true
                    views[position] = root
                    root.setOnClickListener {
                        itemClick(position, root.x.toInt())
                        setCurrentPhoto(position)
                    }
                } else {
                    root.isClickable = false
                }
            }
            return itemView
        }
    }
}
