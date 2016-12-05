package com.simplemobiletools.gallery.models

import com.simplemobiletools.gallery.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.gallery.helpers.SORT_BY_NAME
import com.simplemobiletools.gallery.helpers.SORT_DESCENDING

class Directory(val path: String, val thumbnail: String, val name: String, var mediaCnt: Int, val date_modified: Long, val date_taken: Long,
                var size: Long) : Comparable<Directory> {
    companion object {
        var sorting: Int = 0
    }

    fun addSize(bytes: Long) {
        size += bytes
    }

    override fun compareTo(other: Directory): Int {
        var res: Int
        if (sorting and SORT_BY_NAME != 0) {
            res = name.toLowerCase().compareTo(other.name.toLowerCase())
        } else if (sorting and SORT_BY_DATE_MODIFIED != 0) {
            res = if (date_modified == other.date_modified)
                0
            else if (date_modified > other.date_modified)
                1
            else
                -1
        } else {
            res = if (size == other.size)
                0
            else if (size > other.size)
                1
            else
                -1
        }

        if (sorting and SORT_DESCENDING != 0) {
            res *= -1
        }
        return res
    }

    override fun toString() = "Directory {path=$path, thumbnail=$thumbnail, name=$name, mediaCnt=$mediaCnt, date_modified=$date_modified, " +
            "date_taken=$date_taken, size $size}"
}
