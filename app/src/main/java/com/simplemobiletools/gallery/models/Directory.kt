package com.simplemobiletools.gallery.models

import com.simplemobiletools.gallery.Constants

class Directory(val path: String, val thumbnail: String, val name: String, var mediaCnt: Int, val timestamp: Long, var size: Long) : Comparable<Directory> {
    fun addSize(bytes: Long) {
        size += bytes
    }

    override fun compareTo(other: Directory): Int {
        var res: Int
        if (mSorting and Constants.SORT_BY_NAME != 0) {
            res = path.compareTo(other.path)
        } else if (mSorting and Constants.SORT_BY_DATE != 0) {
            res = if (timestamp > other.timestamp) 1 else -1
        } else {
            res = if (size > other.size) 1 else -1
        }

        if (mSorting and Constants.SORT_DESCENDING != 0) {
            res *= -1
        }
        return res
    }

    override fun toString(): String {
        return "Directory {path=$path, thumbnail=$thumbnail, name=$name, timestamp=$timestamp, mediaCnt=$mediaCnt}"
    }

    companion object {
        var mSorting: Int = 0
    }
}
