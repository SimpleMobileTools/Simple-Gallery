package com.simplemobiletools.gallery.models

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import com.simplemobiletools.commons.extensions.formatDate
import com.simplemobiletools.commons.extensions.formatSize
import com.simplemobiletools.commons.extensions.isDng
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.gallery.helpers.TYPE_GIFS
import com.simplemobiletools.gallery.helpers.TYPE_IMAGES
import com.simplemobiletools.gallery.helpers.TYPE_VIDEOS
import java.io.Serializable

@Entity(tableName = "media", indices = [(Index(value = "full_path", unique = true))])
data class Medium(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "filename") var name: String,
        @ColumnInfo(name = "full_path") var path: String,
        @ColumnInfo(name = "parent_path") var parentPath: String,
        @ColumnInfo(name = "last_modified") val modified: Long,
        @ColumnInfo(name = "date_taken") val taken: Long,
        @ColumnInfo(name = "size") val size: Long,
        @ColumnInfo(name = "type") val type: Int) : Serializable, Comparable<Medium> {

    companion object {
        private const val serialVersionUID = -6553149366975455L
        var sorting: Int = 0
    }

    fun isGif() = type == TYPE_GIFS

    fun isImage() = type == TYPE_IMAGES

    fun isVideo() = type == TYPE_VIDEOS

    fun isDng() = path.isDng()

    override fun compareTo(other: Medium): Int {
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
