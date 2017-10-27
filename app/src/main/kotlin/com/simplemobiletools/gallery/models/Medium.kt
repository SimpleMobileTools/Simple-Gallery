package com.simplemobiletools.gallery.models

import com.simplemobiletools.commons.extensions.getMimeTypeFromPath
import com.simplemobiletools.commons.extensions.isGif
import com.simplemobiletools.commons.extensions.isPng
import com.simplemobiletools.commons.helpers.*
import java.io.Serializable

data class Medium(var name: String, var path: String, val video: Boolean, val modified: Long, val taken: Long, val size: Long) : Serializable, Comparable<Medium> {
    companion object {
        private val serialVersionUID = -6553149366975455L
        var sorting: Int = 0
    }

    fun isPng() = path.isPng()

    fun isGif() = path.isGif()

    fun isJpg() = path.endsWith(".jpg", true) || path.endsWith(".jpeg", true)

    fun isImage() = !isGif() && !video

    fun getMimeType() = path.getMimeTypeFromPath()

    override fun compareTo(other: Medium): Int {
        var result: Int
        when {
            sorting and SORT_BY_NAME != 0 -> result = AlphanumericComparator().compare(name.toLowerCase(), other.name.toLowerCase())
            sorting and SORT_BY_SIZE != 0 -> result = when {
                size == other.size -> 0
                size > other.size -> 1
                else -> -1
            }
            sorting and SORT_BY_DATE_MODIFIED != 0 -> result = when {
                modified == other.modified -> 0
                modified > other.modified -> 1
                else -> -1
            }
            else -> result = when {
                taken == other.taken -> 0
                taken > other.taken -> 1
                else -> -1
            }
        }

        if (sorting and SORT_DESCENDING != 0) {
            result *= -1
        }
        return result
    }
}
