package com.simplemobiletools.gallery.pro.adapters

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.simplemobiletools.commons.views.MySquareImageView
import com.simplemobiletools.gallery.pro.databinding.PhotoItemGridBinding
import com.simplemobiletools.gallery.pro.databinding.PhotoItemListBinding
import com.simplemobiletools.gallery.pro.databinding.VideoItemGridBinding
import com.simplemobiletools.gallery.pro.databinding.VideoItemListBinding

interface MediaItemBinding {
    val root: ViewGroup
    val mediaItemHolder: ViewGroup
    val favorite: ImageView
    val playPortraitOutline: ImageView?
    val fileType: TextView?
    val mediumName: TextView
    val videoDuration: TextView?
    val mediumCheck: ImageView
    val mediumThumbnail: MySquareImageView
}

class PhotoListMediaItemBinding(val binding: PhotoItemListBinding) : MediaItemBinding {
    override val root: ViewGroup = binding.root
    override val mediaItemHolder: ViewGroup = binding.mediaItemHolder
    override val favorite: ImageView = binding.favorite
    override val playPortraitOutline: ImageView? = null
    override val fileType: TextView = binding.fileType
    override val mediumName: TextView = binding.mediumName
    override val videoDuration: TextView? = null
    override val mediumCheck: ImageView = binding.mediumCheck
    override val mediumThumbnail: MySquareImageView = binding.mediumThumbnail
}

fun PhotoItemListBinding.toMediaItemBinding() = PhotoListMediaItemBinding(this)

class PhotoGridMediaItemBinding(val binding: PhotoItemGridBinding) : MediaItemBinding {
    override val root: ViewGroup = binding.root
    override val mediaItemHolder: ViewGroup = binding.mediaItemHolder
    override val favorite: ImageView = binding.favorite
    override val playPortraitOutline: ImageView? = null
    override val fileType: TextView = binding.fileType
    override val mediumName: TextView = binding.mediumName
    override val videoDuration: TextView? = null
    override val mediumCheck: ImageView = binding.mediumCheck
    override val mediumThumbnail: MySquareImageView = binding.mediumThumbnail
}

fun PhotoItemGridBinding.toMediaItemBinding() = PhotoGridMediaItemBinding(this)

class VideoListMediaItemBinding(val binding: VideoItemListBinding) : MediaItemBinding {
    override val root: ViewGroup = binding.root
    override val mediaItemHolder: ViewGroup = binding.mediaItemHolder
    override val favorite: ImageView = binding.favorite
    override val playPortraitOutline: ImageView? = binding.playPortraitOutline
    override val fileType: TextView? = null
    override val mediumName: TextView = binding.mediumName
    override val videoDuration: TextView = binding.videoDuration
    override val mediumCheck: ImageView = binding.mediumCheck
    override val mediumThumbnail: MySquareImageView = binding.mediumThumbnail
}

fun VideoItemListBinding.toMediaItemBinding() = VideoListMediaItemBinding(this)

class VideoGridMediaItemBinding(val binding: VideoItemGridBinding) : MediaItemBinding {
    override val root: ViewGroup = binding.root
    override val mediaItemHolder: ViewGroup = binding.mediaItemHolder
    override val favorite: ImageView = binding.favorite
    override val playPortraitOutline: ImageView = binding.playPortraitOutline
    override val fileType: TextView? = null
    override val mediumName: TextView = binding.mediumName
    override val videoDuration: TextView = binding.videoDuration
    override val mediumCheck: ImageView = binding.mediumCheck
    override val mediumThumbnail: MySquareImageView = binding.mediumThumbnail
}

fun VideoItemGridBinding.toMediaItemBinding() = VideoGridMediaItemBinding(this)
