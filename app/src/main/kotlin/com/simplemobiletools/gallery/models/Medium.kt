package com.simplemobiletools.gallery.models

import com.simplemobiletools.gallery.Constants

import java.io.Serializable

class Medium(val name: String, var path: String, val isVideo: Boolean, val timestamp: Long, val size: Long) : Serializable, Comparable<Medium> {
    val isGif: Boolean
        get() = path.toLowerCase().endsWith(".gif")

    val isImage: Boolean
        get() = !isGif && !isVideo

    override fun compareTo(other: Medium): Int {
        var res: Int
        if (sorting and Constants.SORT_BY_NAME != 0) {
            res = path.compareTo(other.path)
        } else if (sorting and Constants.SORT_BY_DATE != 0) {
            res = if (timestamp > other.timestamp) 1 else -1
        } else {
            res = if (size > other.size) 1 else -1
        }

        if (sorting and Constants.SORT_DESCENDING != 0) {
            res *= -1
        }
        return res
    }

    override fun toString() = "Medium {name=$name, path=$path, isVideo=$isVideo, timestamp=$timestamp, size=$size}"

    companion object {
        private val serialVersionUID = -6543139465975455L
        var sorting: Int = 0
    }
}
