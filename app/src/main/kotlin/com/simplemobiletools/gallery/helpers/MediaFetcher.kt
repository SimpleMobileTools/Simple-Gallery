package com.simplemobiletools.gallery.helpers

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.getDistinctPath
import com.simplemobiletools.gallery.extensions.getOTGFolderChildren
import com.simplemobiletools.gallery.extensions.shouldFolderBeVisible
import com.simplemobiletools.gallery.models.Medium
import java.io.File

class MediaFetcher(val context: Context) {
    var shouldStop = false

    fun getFilesFrom(curPath: String, isPickImage: Boolean, isPickVideo: Boolean, getProperDateTaken: Boolean): ArrayList<Medium> {
        val filterMedia = context.config.filterMedia
        if (filterMedia == 0) {
            return ArrayList()
        }

        val curMedia = ArrayList<Medium>()
        if (curPath.startsWith(OTG_PATH)) {
            val newMedia = getMediaOnOTG(curPath, isPickImage, isPickVideo, filterMedia)
            curMedia.addAll(newMedia)
        } else {
            val newMedia = getMediaInFolder(curPath, isPickImage, isPickVideo, filterMedia, getProperDateTaken)
            curMedia.addAll(newMedia)
        }

        sortMedia(curMedia, context.config.getFileSorting(curPath))
        return curMedia
    }

    fun getFoldersToScan(): ArrayList<String> {
        val filterMedia = context.config.filterMedia
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val uri = MediaStore.Files.getContentUri("external")

        val selection = "${getSelectionQuery(filterMedia)} ${MediaStore.Images.ImageColumns.BUCKET_ID} IS NOT NULL) GROUP BY (${MediaStore.Images.ImageColumns.BUCKET_ID}"
        val selectionArgs = getSelectionArgsQuery(filterMedia).toTypedArray()

        return try {
            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            parseCursor(cursor)
        } catch (e: Exception) {
            ArrayList()
        }
    }

    private fun getSelectionQuery(filterMedia: Int): String {
        val query = StringBuilder()
        query.append("(")
        if (filterMedia and TYPE_IMAGES != 0) {
            photoExtensions.forEach {
                query.append("${MediaStore.Images.Media.DATA} LIKE ? OR ")
            }
        }

        if (filterMedia and TYPE_VIDEOS != 0) {
            videoExtensions.forEach {
                query.append("${MediaStore.Images.Media.DATA} LIKE ? OR ")
            }
        }

        if (filterMedia and TYPE_GIFS != 0) {
            query.append("${MediaStore.Images.Media.DATA} LIKE ?")
        }

        var selectionQuery = query.toString().trim().removeSuffix("OR")
        selectionQuery += ") AND "
        return selectionQuery
    }

    private fun getSelectionArgsQuery(filterMedia: Int): ArrayList<String> {
        val args = ArrayList<String>()
        if (filterMedia and TYPE_IMAGES != 0) {
            photoExtensions.forEach {
                args.add("%$it")
            }
        }

        if (filterMedia and TYPE_VIDEOS != 0) {
            videoExtensions.forEach {
                args.add("%$it")
            }
        }

        if (filterMedia and TYPE_GIFS != 0) {
            args.add("%.gif")
        }

        return args
    }

    private fun parseCursor(cursor: Cursor): ArrayList<String> {
        val foldersToIgnore = arrayListOf("/storage/emulated/legacy")
        val config = context.config
        val includedFolders = config.includedFolders
        var foldersToScan = ArrayList<String>()

        cursor.use {
            if (cursor.moveToFirst()) {
                do {
                    val path = cursor.getStringValue(MediaStore.Images.Media.DATA).trim()
                    val parentPath = File(path).parent?.trimEnd('/') ?: continue
                    if (!includedFolders.contains(parentPath) && !foldersToIgnore.contains(parentPath.toLowerCase())) {
                        foldersToScan.add(parentPath)
                    }
                } while (cursor.moveToNext())
            }
        }

        includedFolders.forEach {
            addFolder(foldersToScan, it)
        }

        val showHidden = config.shouldShowHidden
        val excludedFolders = config.excludedFolders
        foldersToScan = foldersToScan.filter { it.shouldFolderBeVisible(excludedFolders, includedFolders, showHidden) } as ArrayList<String>
        return foldersToScan.distinctBy { it.getDistinctPath() } as ArrayList<String>
    }

    private fun addFolder(curFolders: ArrayList<String>, folder: String) {
        curFolders.add(folder)
        if (folder.startsWith(OTG_PATH)) {
            val files = context.getOTGFolderChildren(folder) ?: return
            for (file in files) {
                if (file.isDirectory) {
                    val relativePath = file.uri.path.substringAfterLast("${context.config.OTGPartition}:")
                    addFolder(curFolders, "$OTG_PATH$relativePath")
                }
            }
        } else {
            val files = File(folder).listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    addFolder(curFolders, file.absolutePath)
                }
            }
        }
    }

    private fun getMediaInFolder(folder: String, isPickImage: Boolean, isPickVideo: Boolean, filterMedia: Int, getProperDateTaken: Boolean): ArrayList<Medium> {
        val media = ArrayList<Medium>()
        val files = File(folder).listFiles() ?: return media
        val doExtraCheck = context.config.doExtraCheck
        val showHidden = context.config.shouldShowHidden
        val dateTakens = if (getProperDateTaken) getFolderDateTakens(folder) else HashMap()

        for (file in files) {
            if (shouldStop) {
                break
            }

            val filename = file.name
            val isImage = filename.isImageFast()
            val isVideo = if (isImage) false else filename.isVideoFast()
            val isGif = if (isImage || isVideo) false else filename.isGif()

            if (!isImage && !isVideo && !isGif)
                continue

            if (isVideo && (isPickImage || filterMedia and TYPE_VIDEOS == 0))
                continue

            if (isImage && (isPickVideo || filterMedia and TYPE_IMAGES == 0))
                continue

            if (isGif && filterMedia and TYPE_GIFS == 0)
                continue

            if (!showHidden && filename.startsWith('.'))
                continue

            val size = file.length()
            if (size <= 0L || (doExtraCheck && !file.exists()))
                continue

            val lastModified = file.lastModified()
            var dateTaken = lastModified

            if (getProperDateTaken) {
                dateTaken = dateTakens.remove(filename) ?: lastModified
            }

            val type = when {
                isImage -> TYPE_IMAGES
                isVideo -> TYPE_VIDEOS
                else -> TYPE_GIFS
            }

            val medium = Medium(null, filename, file.absolutePath, folder, lastModified, dateTaken, size, type)
            media.add(medium)
        }
        return media
    }

    private fun getMediaOnOTG(folder: String, isPickImage: Boolean, isPickVideo: Boolean, filterMedia: Int): ArrayList<Medium> {
        val media = ArrayList<Medium>()
        val files = context.getDocumentFile(folder)?.listFiles() ?: return media
        val doExtraCheck = context.config.doExtraCheck
        val showHidden = context.config.shouldShowHidden

        for (file in files) {
            if (shouldStop) {
                break
            }

            val filename = file.name ?: continue
            val isImage = filename.isImageFast()
            val isVideo = if (isImage) false else filename.isVideoFast()
            val isGif = if (isImage || isVideo) false else filename.isGif()

            if (!isImage && !isVideo && !isGif)
                continue

            if (isVideo && (isPickImage || filterMedia and TYPE_VIDEOS == 0))
                continue

            if (isImage && (isPickVideo || filterMedia and TYPE_IMAGES == 0))
                continue

            if (isGif && filterMedia and TYPE_GIFS == 0)
                continue

            if (!showHidden && filename.startsWith('.'))
                continue

            val size = file.length()
            if (size <= 0L || (doExtraCheck && !file.exists()))
                continue

            val dateTaken = file.lastModified()
            val dateModified = file.lastModified()

            val type = when {
                isImage -> TYPE_IMAGES
                isVideo -> TYPE_VIDEOS
                else -> TYPE_GIFS
            }

            val path = Uri.decode(file.uri.toString().replaceFirst("${context.config.OTGTreeUri}/document/${context.config.OTGPartition}%3A", OTG_PATH))
            val medium = Medium(null, filename, path, folder, dateModified, dateTaken, size, type)
            media.add(medium)
        }

        return media
    }

    private fun getFolderDateTakens(folder: String): HashMap<String, Long> {
        val projection = arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN
        )

        val uri = MediaStore.Files.getContentUri("external")
        val selection = "${MediaStore.Images.Media.DATA} LIKE ? AND ${MediaStore.Images.Media.DATA} NOT LIKE ?"
        val selectionArgs = arrayOf("$folder/%", "$folder/%/%")

        val dateTakens = HashMap<String, Long>()
        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor.use {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        val path = cursor.getStringValue(MediaStore.Images.Media.DISPLAY_NAME)
                        val dateTaken = cursor.getLongValue(MediaStore.Images.Media.DATE_TAKEN)
                        dateTakens[path] = dateTaken
                    } catch (e: Exception) {
                    }
                } while (cursor.moveToNext())
            }
        }

        return dateTakens
    }

    fun sortMedia(media: ArrayList<Medium>, sorting: Int) {
        media.sortWith(Comparator { o1, o2 ->
            o1 as Medium
            o2 as Medium
            var result = when {
                sorting and SORT_BY_NAME != 0 -> AlphanumericComparator().compare(o1.name.toLowerCase(), o2.name.toLowerCase())
                sorting and SORT_BY_PATH != 0 -> AlphanumericComparator().compare(o1.path.toLowerCase(), o2.path.toLowerCase())
                sorting and SORT_BY_SIZE != 0 -> o1.size.compareTo(o2.size)
                sorting and SORT_BY_DATE_MODIFIED != 0 -> o1.modified.compareTo(o2.modified)
                else -> o1.taken.compareTo(o2.taken)
            }

            if (sorting and SORT_DESCENDING != 0) {
                result *= -1
            }
            result
        })
    }
}
