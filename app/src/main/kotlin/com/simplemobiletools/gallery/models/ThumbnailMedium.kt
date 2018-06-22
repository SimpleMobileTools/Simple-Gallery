package com.simplemobiletools.gallery.models

import com.simplemobiletools.commons.extensions.formatDate
import com.simplemobiletools.commons.extensions.formatSize
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.commons.helpers.SORT_BY_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_PATH
import com.simplemobiletools.commons.helpers.SORT_BY_SIZE
import com.simplemobiletools.gallery.helpers.TYPE_VIDEOS

data class ThumbnailMedium(val name: String, val path: String, val parentPath: String, val modified: Long, val taken: Long, val size: Long,
                           val type: Int, var isFavorite: Boolean) : ThumbnailItem() {
    fun isVideo() = type == TYPE_VIDEOS

    fun getBubbleText(sorting: Int) = when {
        sorting and SORT_BY_NAME != 0 -> name
        sorting and SORT_BY_PATH != 0 -> path
        sorting and SORT_BY_SIZE != 0 -> size.formatSize()
        sorting and SORT_BY_DATE_MODIFIED != 0 -> modified.formatDate()
        else -> taken.formatDate()
    }
}
