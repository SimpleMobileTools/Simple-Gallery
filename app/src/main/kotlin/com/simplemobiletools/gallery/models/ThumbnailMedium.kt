package com.simplemobiletools.gallery.models

import com.simplemobiletools.gallery.helpers.TYPE_VIDEOS

data class ThumbnailMedium(val name: String, val path: String, val parentPath: String, val modified: Long, val taken: Long, val size: Long,
                           val type: Int, val isFavorite: Boolean) : ThumbnailItem() {
    fun isVideo() = type == TYPE_VIDEOS
}
