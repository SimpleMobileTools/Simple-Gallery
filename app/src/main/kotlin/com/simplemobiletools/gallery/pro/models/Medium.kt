package com.simplemobiletools.gallery.pro.models

import android.content.Context
import androidx.room.*
import com.bumptech.glide.signature.ObjectKey
import com.simplemobiletools.commons.extensions.formatDate
import com.simplemobiletools.commons.extensions.formatSize
import com.simplemobiletools.commons.extensions.getFilenameExtension
import com.simplemobiletools.commons.extensions.isWebP
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.commons.helpers.SORT_BY_NAME
import com.simplemobiletools.commons.helpers.SORT_BY_PATH
import com.simplemobiletools.commons.helpers.SORT_BY_SIZE
import com.simplemobiletools.gallery.pro.helpers.*
import java.io.File
import java.io.Serializable
import java.util.*

@Entity(tableName = "media", indices = [(Index(value = ["full_path"], unique = true))])
data class Medium(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "filename") var name: String,
    @ColumnInfo(name = "full_path") var path: String,
    @ColumnInfo(name = "parent_path") var parentPath: String,
    @ColumnInfo(name = "last_modified") var modified: Long,
    @ColumnInfo(name = "date_taken") var taken: Long,
    @ColumnInfo(name = "size") var size: Long,
    @ColumnInfo(name = "type") var type: Int,
    @ColumnInfo(name = "video_duration") var videoDuration: Int,
    @ColumnInfo(name = "is_favorite") var isFavorite: Boolean,
    @ColumnInfo(name = "deleted_ts") var deletedTS: Long,

    @Ignore var gridPosition: Int = 0   // used at grid view decoration at Grouping enabled
) : Serializable, ThumbnailItem() {

    constructor() : this(null, "", "", "", 0L, 0L, 0L, 0, 0, false, 0L, 0)

    companion object {
        private const val serialVersionUID = -6553149366975655L
    }

    fun isWebP() = name.isWebP()

    fun isGIF() = type == TYPE_GIFS

    fun isImage() = type == TYPE_IMAGES

    fun isVideo() = type == TYPE_VIDEOS

    fun isRaw() = type == TYPE_RAWS

    fun isSVG() = type == TYPE_SVGS

    fun isPortrait() = type == TYPE_PORTRAITS

    fun isHidden() = name.startsWith('.')

    fun getBubbleText(sorting: Int, context: Context, dateFormat: String, timeFormat: String) = when {
        sorting and SORT_BY_NAME != 0 -> name
        sorting and SORT_BY_PATH != 0 -> path
        sorting and SORT_BY_SIZE != 0 -> size.formatSize()
        sorting and SORT_BY_DATE_MODIFIED != 0 -> modified.formatDate(context, dateFormat, timeFormat)
        else -> taken.formatDate(context)
    }

    fun getGroupingKey(groupBy: Int): String {
        return when {
            groupBy and GROUP_BY_LAST_MODIFIED_DAILY != 0 -> getDayStartTS(modified, false)
            groupBy and GROUP_BY_LAST_MODIFIED_MONTHLY != 0 -> getDayStartTS(modified, true)
            groupBy and GROUP_BY_DATE_TAKEN_DAILY != 0 -> getDayStartTS(taken, false)
            groupBy and GROUP_BY_DATE_TAKEN_MONTHLY != 0 -> getDayStartTS(taken, true)
            groupBy and GROUP_BY_FILE_TYPE != 0 -> type.toString()
            groupBy and GROUP_BY_EXTENSION != 0 -> name.getFilenameExtension().toLowerCase()
            groupBy and GROUP_BY_FOLDER != 0 -> parentPath
            else -> ""
        }
    }

    fun getIsInRecycleBin() = deletedTS != 0L

    private fun getDayStartTS(ts: Long, resetDays: Boolean): String {
        val calendar = Calendar.getInstance(Locale.ENGLISH).apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (resetDays) {
                set(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return calendar.timeInMillis.toString()
    }

    fun getSignature(): String {
        val lastModified = if (modified > 1) {
            modified
        } else {
            File(path).lastModified()
        }

        return "$path-$lastModified-$size"
    }

    fun getKey() = ObjectKey(getSignature())
}
