package com.simplemobiletools.gallery.models

import com.simplemobiletools.commons.extensions.formatDate
import com.simplemobiletools.commons.extensions.formatSize
import com.simplemobiletools.commons.helpers.*
import java.io.Serializable

data class Directory(var path: String, var tmb: String, var name: String, var mediaCnt: Int, val modified: Long, val taken: Long,
                     val size: Long, val isOnSDCard: Boolean) : Serializable, Comparable<Directory> {
    companion object {
        private val serialVersionUID = -6553345863555455L
        var sorting: Int = 0
    }

    override fun compareTo(other: Directory): Int {
        var result: Int
        when {
            sorting and SORT_BY_NAME != 0 -> result = AlphanumericComparator().compare(name.toLowerCase(), other.name.toLowerCase())
            sorting and SORT_BY_PATH != 0 -> result = AlphanumericComparator().compare(path.toLowerCase(), other.path.toLowerCase())
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

    fun getBubbleText() = when {
        sorting and SORT_BY_NAME != 0 -> name
        sorting and SORT_BY_PATH != 0 -> path
        sorting and SORT_BY_SIZE != 0 -> size.formatSize()
        sorting and SORT_BY_DATE_MODIFIED != 0 -> modified.formatDate()
        else -> taken.formatDate()
    }
}
