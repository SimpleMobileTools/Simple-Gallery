package com.simplemobiletools.gallery.helpers

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.OTG_PATH
import com.simplemobiletools.commons.helpers.photoExtensions
import com.simplemobiletools.commons.helpers.videoExtensions
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.shouldFolderBeVisible
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.LinkedHashMap
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.set

class MediaFetcher(val context: Context) {
    var shouldStop = false

    fun getMediaByDirectories(isPickVideo: Boolean, isPickImage: Boolean): HashMap<String, ArrayList<Medium>> {
        val media = getFilesFrom("", isPickImage, isPickVideo)
        return groupDirectories(media)
    }

    fun getFilesFrom(curPath: String, isPickImage: Boolean, isPickVideo: Boolean): ArrayList<Medium> {
        val filterMedia = context.config.filterMedia
        if (filterMedia == 0) {
            return ArrayList()
        }

        if (curPath.startsWith(OTG_PATH)) {
            val curMedia = ArrayList<Medium>()
            getMediaOnOTG(curPath, curMedia, isPickImage, isPickVideo, filterMedia)
            return curMedia
        } else {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val uri = MediaStore.Files.getContentUri("external")

            val selection = "${getSelectionQuery(curPath, filterMedia)} ${MediaStore.Images.ImageColumns.BUCKET_ID} IS NOT NULL) GROUP BY (${MediaStore.Images.ImageColumns.BUCKET_ID}"
            val selectionArgs = getSelectionArgsQuery(curPath, filterMedia).toTypedArray()

            return try {
                val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                parseCursor(context, cursor, isPickImage, isPickVideo, curPath, filterMedia)
            } catch (e: Exception) {
                ArrayList()
            }
        }
    }

    private fun getSelectionQuery(path: String, filterMedia: Int): String {
        val query = StringBuilder()
        if (path.isNotEmpty()) {
            query.append("${MediaStore.Images.Media.DATA} LIKE ? AND ${MediaStore.Images.Media.DATA} NOT LIKE ? AND ")
        }

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

    private fun getSelectionArgsQuery(path: String, filterMedia: Int): ArrayList<String> {
        val args = ArrayList<String>()
        if (path.isNotEmpty()) {
            args.add("$path/%")
            args.add("$path/%/%")
        }

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

    private fun parseCursor(context: Context, cursor: Cursor, isPickImage: Boolean, isPickVideo: Boolean, curPath: String, filterMedia: Int): ArrayList<Medium> {
        val config = context.config
        val includedFolders = config.includedFolders
        val foldersToScan = HashSet<String>()

        cursor.use {
            if (cursor.moveToFirst()) {
                do {
                    val path = cursor.getStringValue(MediaStore.Images.Media.DATA).trim()
                    val parentPath = File(path).parent?.trimEnd('/') ?: continue
                    if (!includedFolders.contains(parentPath)) {
                        foldersToScan.add(parentPath)
                    }
                } while (cursor.moveToNext())
            }
        }

        includedFolders.forEach {
            if (curPath.isEmpty()) {
                addFolder(foldersToScan, it)
            } else if (curPath == it) {
                foldersToScan.add(it)
            }
        }

        val curMedia = ArrayList<Medium>()
        val showHidden = config.shouldShowHidden
        val excludedFolders = config.excludedFolders
        foldersToScan.filter { it.shouldFolderBeVisible(excludedFolders, includedFolders, showHidden) }.toList().forEach {
            fetchFolderContent(it, curMedia, isPickImage, isPickVideo, filterMedia)
        }

        if (config.isThirdPartyIntent && curPath.isNotEmpty() && curMedia.isEmpty()) {
            getMediaInFolder(curPath, curMedia, isPickImage, isPickVideo, filterMedia)
        }

        Medium.sorting = config.getFileSorting(curPath)
        curMedia.sort()

        return curMedia
    }

    private fun addFolder(curFolders: HashSet<String>, folder: String) {
        curFolders.add(folder)
        val files = File(folder).listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                addFolder(curFolders, file.absolutePath)
            }
        }
    }

    private fun fetchFolderContent(path: String, curMedia: ArrayList<Medium>, isPickImage: Boolean, isPickVideo: Boolean, filterMedia: Int) {
        if (path.startsWith(OTG_PATH)) {
            getMediaOnOTG(path, curMedia, isPickImage, isPickVideo, filterMedia)
        } else {
            getMediaInFolder(path, curMedia, isPickImage, isPickVideo, filterMedia)
        }
    }

    private fun groupDirectories(media: ArrayList<Medium>): HashMap<String, ArrayList<Medium>> {
        val directories = LinkedHashMap<String, ArrayList<Medium>>()
        for (medium in media) {
            if (shouldStop) {
                break
            }

            val parentDir = medium.parentPath.toLowerCase()
            if (directories.containsKey(parentDir)) {
                directories[parentDir]!!.add(medium)
            } else {
                directories[parentDir] = arrayListOf(medium)
            }
        }
        return directories
    }

    private fun getMediaInFolder(folder: String, curMedia: ArrayList<Medium>, isPickImage: Boolean, isPickVideo: Boolean, filterMedia: Int) {
        val files = File(folder).listFiles() ?: return
        val doExtraCheck = context.config.doExtraCheck
        val showHidden = context.config.shouldShowHidden

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

            val dateTaken = file.lastModified()
            val dateModified = file.lastModified()

            val type = when {
                isImage -> TYPE_IMAGES
                isVideo -> TYPE_VIDEOS
                else -> TYPE_GIFS
            }

            val medium = Medium(null, filename, file.absolutePath, folder, dateModified, dateTaken, size, type)
            curMedia.add(medium)
        }
    }

    private fun getMediaOnOTG(folder: String, curMedia: ArrayList<Medium>, isPickImage: Boolean, isPickVideo: Boolean, filterMedia: Int) {
        val files = context.getDocumentFile(folder)?.listFiles() ?: return
        val doExtraCheck = context.config.doExtraCheck
        val showHidden = context.config.shouldShowHidden

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

            val dateTaken = file.lastModified()
            val dateModified = file.lastModified()

            val type = when {
                isImage -> TYPE_IMAGES
                isVideo -> TYPE_VIDEOS
                else -> TYPE_GIFS
            }

            val path = Uri.decode(file.uri.toString().replaceFirst("${context.config.OTGBasePath}%3A", OTG_PATH))
            val medium = Medium(null, filename, path, folder, dateModified, dateTaken, size, type)
            curMedia.add(medium)
        }
    }
}
