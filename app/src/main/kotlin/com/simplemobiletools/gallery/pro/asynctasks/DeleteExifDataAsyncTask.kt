package com.simplemobiletools.gallery.pro.asynctasks

import android.content.Context
import android.os.AsyncTask
import androidx.exifinterface.media.ExifInterface
import com.simplemobiletools.commons.extensions.removeValues
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.getFavoritePaths
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.models.Medium
import java.util.*

class DeleteExifDataAsyncTask(val context: Context) :
    AsyncTask<Void, Void, Unit>() {
    private val mediaFetcher = MediaFetcher(context)

    override fun doInBackground(vararg params: Void) {
        val pathToUse = SHOW_ALL
        val folderGrouping = context.config.getFolderGrouping(pathToUse)
        val fileSorting = context.config.getFolderSorting(pathToUse)
        val getProperDateTaken = fileSorting and SORT_BY_DATE_TAKEN != 0 ||
            folderGrouping and GROUP_BY_DATE_TAKEN_DAILY != 0 ||
            folderGrouping and GROUP_BY_DATE_TAKEN_MONTHLY != 0

        val getProperLastModified = fileSorting and SORT_BY_DATE_MODIFIED != 0 ||
            folderGrouping and GROUP_BY_LAST_MODIFIED_DAILY != 0 ||
            folderGrouping and GROUP_BY_LAST_MODIFIED_MONTHLY != 0

        val getProperFileSize = fileSorting and SORT_BY_SIZE != 0
        val favoritePaths = context.getFavoritePaths()
        val getVideoDurations = context.config.showThumbnailVideoDuration
        val lastModifieds = if (getProperLastModified) mediaFetcher.getLastModifieds() else HashMap()
        val dateTakens = if (getProperDateTaken) mediaFetcher.getDateTakens() else HashMap()

        val foldersToScan = mediaFetcher.getFoldersToScan().filter { it != RECYCLE_BIN }
        val media = ArrayList<Medium>()
        foldersToScan.forEach {
            val newMedia = mediaFetcher.getFilesFrom(
                it, false, false, getProperDateTaken, getProperLastModified, getProperFileSize,
                favoritePaths, getVideoDurations, lastModifieds, dateTakens.clone() as HashMap<String, Long>, null
            )
            media.addAll(newMedia)
        }
        media.forEach {
            ExifInterface(it.path).removeValues()
        }
    }
}
