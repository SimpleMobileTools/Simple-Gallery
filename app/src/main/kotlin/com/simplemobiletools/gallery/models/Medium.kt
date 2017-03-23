package com.simplemobiletools.gallery.models

import com.simplemobiletools.commons.extensions.getMimeType
import com.simplemobiletools.commons.extensions.isGif
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.commons.helpers.SORT_BY_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_SIZE
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import java.io.File
import java.io.Serializable

data class Medium(val name: String, var path: String, val isVideo: Boolean, val date_modified: Long, val date_taken: Long, val size: Long) : Serializable, Comparable<Medium> {
    companion object {
        private val serialVersionUID = -6553149466975455L
        var sorting: Int = 0
    }

    fun isGif() = path.isGif()

    fun isImage() = !isGif() && !isVideo

    fun getMimeType() = File(path).getMimeType()

    override fun compareTo(other: Medium): Int {
        var res: Int
        if (sorting and SORT_BY_NAME != 0) {
            res = name.toLowerCase().compareTo(other.name.toLowerCase())
        } else if (sorting and SORT_BY_SIZE != 0) {
            res = if (size == other.size)
                0
            else if (size > other.size)
                1
            else
                -1
        } else if (sorting and SORT_BY_DATE_MODIFIED != 0) {
            res = if (date_modified == other.date_modified)
                0
            else if (date_modified > other.date_modified)
                1
            else
                -1
        } else {
            res = if (date_taken == other.date_taken)
                0
            else if (date_taken > other.date_taken)
                1
            else
                -1
        }

        if (sorting and SORT_DESCENDING != 0) {
            res *= -1
        }
        return res
    }
}
