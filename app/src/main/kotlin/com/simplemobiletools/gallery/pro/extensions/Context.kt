package com.simplemobiletools.gallery.pro.extensions

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.drawable.PictureDrawable
import android.media.AudioManager
import android.provider.MediaStore.Files
import android.provider.MediaStore.Images
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.SettingsActivity
import com.simplemobiletools.gallery.pro.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.pro.databases.GalleryDatabase
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.interfaces.*
import com.simplemobiletools.gallery.pro.models.*
import com.simplemobiletools.gallery.pro.svg.SvgSoftwareLayerSetter
import com.simplemobiletools.gallery.pro.views.MySquareImageView
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.HashSet
import java.util.LinkedHashSet
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set

val Context.audioManager get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager

fun Context.getHumanizedFilename(path: String): String {
    val humanized = humanizePath(path)
    return humanized.substring(humanized.lastIndexOf("/") + 1)
}

fun Context.launchSettings() {
    startActivity(Intent(applicationContext, SettingsActivity::class.java))
}

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.widgetsDB: WidgetsDao get() = GalleryDatabase.getInstance(applicationContext).WidgetsDao()

val Context.mediaDB: MediumDao get() = GalleryDatabase.getInstance(applicationContext).MediumDao()

val Context.directoryDao: DirectoryDao get() = GalleryDatabase.getInstance(applicationContext).DirectoryDao()

val Context.favoritesDB: FavoritesDao get() = GalleryDatabase.getInstance(applicationContext).FavoritesDao()

val Context.dateTakensDB: DateTakensDao get() = GalleryDatabase.getInstance(applicationContext).DateTakensDao()

val Context.recycleBin: File get() = filesDir

val Context.recycleBinPath: String get() = filesDir.absolutePath

fun Context.movePinnedDirectoriesToFront(dirs: ArrayList<Directory>): ArrayList<Directory> {
    val foundFolders = ArrayList<Directory>()
    val pinnedFolders = config.pinnedFolders

    dirs.forEach {
        if (pinnedFolders.contains(it.path)) {
            foundFolders.add(it)
        }
    }

    dirs.removeAll(foundFolders)
    dirs.addAll(0, foundFolders)
    if (config.tempFolderPath.isNotEmpty()) {
        val newFolder = dirs.firstOrNull { it.path == config.tempFolderPath }
        if (newFolder != null) {
            dirs.remove(newFolder)
            dirs.add(0, newFolder)
        }
    }

    if (config.showRecycleBinAtFolders && config.showRecycleBinLast) {
        val binIndex = dirs.indexOfFirst { it.isRecycleBin() }
        if (binIndex != -1) {
            val bin = dirs.removeAt(binIndex)
            dirs.add(bin)
        }
    }
    return dirs
}

@Suppress("UNCHECKED_CAST")
fun Context.getSortedDirectories(source: ArrayList<Directory>): ArrayList<Directory> {
    val sorting = config.directorySorting
    val dirs = source.clone() as ArrayList<Directory>

    if (sorting and SORT_BY_RANDOM != 0) {
        dirs.shuffle()
        return movePinnedDirectoriesToFront(dirs)
    }

    dirs.sortWith(Comparator { o1, o2 ->
        o1 as Directory
        o2 as Directory

        var result = when {
            sorting and SORT_BY_NAME != 0 -> {
                if (sorting and SORT_USE_NUMERIC_VALUE != 0) {
                    AlphanumericComparator().compare(o1.sortValue.toLowerCase(), o2.sortValue.toLowerCase())
                } else {
                    o1.sortValue.toLowerCase().compareTo(o2.sortValue.toLowerCase())
                }
            }
            sorting and SORT_BY_PATH != 0 -> {
                if (sorting and SORT_USE_NUMERIC_VALUE != 0) {
                    AlphanumericComparator().compare(o1.sortValue.toLowerCase(), o2.sortValue.toLowerCase())
                } else {
                    o1.sortValue.toLowerCase().compareTo(o2.sortValue.toLowerCase())
                }
            }
            sorting and SORT_BY_PATH != 0 -> AlphanumericComparator().compare(o1.sortValue.toLowerCase(), o2.sortValue.toLowerCase())
            sorting and SORT_BY_SIZE != 0 -> (o1.sortValue.toLongOrNull() ?: 0).compareTo(o2.sortValue.toLongOrNull() ?: 0)
            sorting and SORT_BY_DATE_MODIFIED != 0 -> (o1.sortValue.toLongOrNull() ?: 0).compareTo(o2.sortValue.toLongOrNull() ?: 0)
            else -> (o1.sortValue.toLongOrNull() ?: 0).compareTo(o2.sortValue.toLongOrNull() ?: 0)
        }

        if (sorting and SORT_DESCENDING != 0) {
            result *= -1
        }
        result
    })

    return movePinnedDirectoriesToFront(dirs)
}

fun Context.getDirsToShow(dirs: ArrayList<Directory>, allDirs: ArrayList<Directory>, currentPathPrefix: String): ArrayList<Directory> {
    return if (config.groupDirectSubfolders) {
        dirs.forEach {
            it.subfoldersCount = 0
            it.subfoldersMediaCount = it.mediaCnt
        }

        val parentDirs = getDirectParentSubfolders(dirs, currentPathPrefix)
        updateSubfolderCounts(dirs, parentDirs)

        // show the current folder as an available option too, not just subfolders
        if (currentPathPrefix.isNotEmpty()) {
            val currentFolder = allDirs.firstOrNull { parentDirs.firstOrNull { it.path == currentPathPrefix } == null && it.path == currentPathPrefix }
            currentFolder?.apply {
                subfoldersCount = 1
                parentDirs.add(this)
            }
        }

        getSortedDirectories(parentDirs)
    } else {
        dirs.forEach { it.subfoldersMediaCount = it.mediaCnt }
        dirs
    }
}

fun Context.getDirectParentSubfolders(dirs: ArrayList<Directory>, currentPathPrefix: String): ArrayList<Directory> {
    val folders = dirs.map { it.path }.sorted().toMutableSet() as HashSet<String>
    val currentPaths = LinkedHashSet<String>()
    val foldersWithoutMediaFiles = ArrayList<String>()
    var newDirId = 1000L

    for (path in folders) {
        if (path == RECYCLE_BIN || path == FAVORITES) {
            continue
        }

        if (currentPathPrefix.isNotEmpty()) {
            if (!path.startsWith(currentPathPrefix, true)) {
                continue
            }

            if (!File(path).parent.equals(currentPathPrefix, true)) {
                continue
            }
        }

        if (currentPathPrefix.isNotEmpty() && path == currentPathPrefix || File(path).parent.equals(currentPathPrefix, true)) {
            currentPaths.add(path)
        } else if (folders.any { !it.equals(path, true) && (File(path).parent.equals(it, true) || File(it).parent.equals(File(path).parent, true)) }) {
            // if we have folders like
            // /storage/emulated/0/Pictures/Images and
            // /storage/emulated/0/Pictures/Screenshots,
            // but /storage/emulated/0/Pictures is empty, still Pictures with the first folders thumbnails and proper other info
            val parent = File(path).parent
            if (parent != null && !folders.contains(parent) && dirs.none { it.path == parent }) {
                currentPaths.add(parent)
                val isSortingAscending = config.sorting.isSortingAscending()
                val subDirs = dirs.filter { File(it.path).parent.equals(File(path).parent, true) } as ArrayList<Directory>
                if (subDirs.isNotEmpty()) {
                    val lastModified = if (isSortingAscending) {
                        subDirs.minBy { it.modified }?.modified
                    } else {
                        subDirs.maxBy { it.modified }?.modified
                    } ?: 0

                    val dateTaken = if (isSortingAscending) {
                        subDirs.minBy { it.taken }?.taken
                    } else {
                        subDirs.maxBy { it.taken }?.taken
                    } ?: 0

                    var mediaTypes = 0
                    subDirs.forEach {
                        mediaTypes = mediaTypes or it.types
                    }

                    val directory = Directory(newDirId++,
                        parent,
                        subDirs.first().tmb,
                        getFolderNameFromPath(parent),
                        subDirs.sumBy { it.mediaCnt },
                        lastModified,
                        dateTaken,
                        subDirs.sumByLong { it.size },
                        getPathLocation(parent),
                        mediaTypes,
                        "")

                    directory.containsMediaFilesDirectly = false
                    dirs.add(directory)
                    currentPaths.add(parent)
                    foldersWithoutMediaFiles.add(parent)
                }
            }
        } else {
            currentPaths.add(path)
        }
    }

    var areDirectSubfoldersAvailable = false
    currentPaths.forEach {
        val path = it
        currentPaths.forEach {
            if (!foldersWithoutMediaFiles.contains(it) && !it.equals(path, true) && File(it).parent?.equals(path, true) == true) {
                areDirectSubfoldersAvailable = true
            }
        }
    }

    if (currentPathPrefix.isEmpty() && folders.contains(RECYCLE_BIN)) {
        currentPaths.add(RECYCLE_BIN)
    }

    if (currentPathPrefix.isEmpty() && folders.contains(FAVORITES)) {
        currentPaths.add(FAVORITES)
    }

    if (folders.size == currentPaths.size) {
        return dirs.filter { currentPaths.contains(it.path) } as ArrayList<Directory>
    }

    folders.clear()
    folders.addAll(currentPaths)

    val dirsToShow = dirs.filter { folders.contains(it.path) } as ArrayList<Directory>
    return if (areDirectSubfoldersAvailable) {
        getDirectParentSubfolders(dirsToShow, currentPathPrefix)
    } else {
        dirsToShow
    }
}

fun Context.updateSubfolderCounts(children: ArrayList<Directory>, parentDirs: ArrayList<Directory>) {
    for (child in children) {
        var longestSharedPath = ""
        for (parentDir in parentDirs) {
            if (parentDir.path == child.path) {
                longestSharedPath = child.path
                continue
            }

            if (child.path.startsWith(parentDir.path, true) && parentDir.path.length > longestSharedPath.length) {
                longestSharedPath = parentDir.path
            }
        }

        // make sure we count only the proper direct subfolders, grouped the same way as on the main screen
        parentDirs.firstOrNull { it.path == longestSharedPath }?.apply {
            if (path.equals(child.path, true) || path.equals(File(child.path).parent, true) || children.any { it.path.equals(File(child.path).parent, true) }) {
                if (child.containsMediaFilesDirectly) {
                    subfoldersCount++
                }

                if (path != child.path) {
                    subfoldersMediaCount += child.mediaCnt
                }
            }
        }
    }
}

fun Context.getNoMediaFolders(callback: (folders: ArrayList<String>) -> Unit) {
    ensureBackgroundThread {
        callback(getNoMediaFoldersSync())
    }
}

fun Context.getNoMediaFoldersSync(): ArrayList<String> {
    val folders = ArrayList<String>()

    val uri = Files.getContentUri("external")
    val projection = arrayOf(Files.FileColumns.DATA)
    val selection = "${Files.FileColumns.MEDIA_TYPE} = ? AND ${Files.FileColumns.TITLE} LIKE ?"
    val selectionArgs = arrayOf(Files.FileColumns.MEDIA_TYPE_NONE.toString(), "%$NOMEDIA%")
    val sortOrder = "${Files.FileColumns.DATE_MODIFIED} DESC"
    val OTGPath = config.OTGPath

    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        if (cursor?.moveToFirst() == true) {
            do {
                val path = cursor.getStringValue(Files.FileColumns.DATA) ?: continue
                val noMediaFile = File(path)
                if (getDoesFilePathExist(noMediaFile.absolutePath, OTGPath) && noMediaFile.name == NOMEDIA) {
                    folders.add(noMediaFile.parent)
                }
            } while (cursor.moveToNext())
        }
    } catch (ignored: Exception) {
    } finally {
        cursor?.close()
    }

    return folders
}

fun Context.rescanFolderMedia(path: String) {
    ensureBackgroundThread {
        rescanFolderMediaSync(path)
    }
}

fun Context.rescanFolderMediaSync(path: String) {
    getCachedMedia(path) {
        val cached = it
        GetMediaAsynctask(applicationContext, path, false, false, false) {
            ensureBackgroundThread {
                val newMedia = it
                val media = newMedia.filter { it is Medium } as ArrayList<Medium>
                try {
                    mediaDB.insertAll(media)

                    cached.forEach {
                        if (!newMedia.contains(it)) {
                            val mediumPath = (it as? Medium)?.path
                            if (mediumPath != null) {
                                deleteDBPath(mediumPath)
                            }
                        }
                    }
                } catch (ignored: Exception) {
                }
            }
        }.execute()
    }
}

fun Context.storeDirectoryItems(items: ArrayList<Directory>) {
    ensureBackgroundThread {
        directoryDao.insertAll(items)
    }
}

fun Context.checkAppendingHidden(path: String, hidden: String, includedFolders: MutableSet<String>, noMediaFolders: ArrayList<String>): String {
    val dirName = getFolderNameFromPath(path)
    val folderNoMediaStatuses = java.util.HashMap<String, Boolean>()
    noMediaFolders.forEach { folder ->
        folderNoMediaStatuses["$folder/$NOMEDIA"] = true
    }

    return if (path.doesThisOrParentHaveNoMedia(folderNoMediaStatuses, null) && !path.isThisOrParentIncluded(includedFolders)) {
        "$dirName $hidden"
    } else {
        dirName
    }
}

fun Context.getFolderNameFromPath(path: String): String {
    return when (path) {
        internalStoragePath -> getString(R.string.internal)
        sdCardPath -> getString(R.string.sd_card)
        otgPath -> getString(R.string.usb)
        FAVORITES -> getString(R.string.favorites)
        RECYCLE_BIN -> getString(R.string.recycle_bin)
        else -> path.getFilenameFromPath()
    }
}

fun Context.loadImage(type: Int, path: String, target: MySquareImageView, horizontalScroll: Boolean, animateGifs: Boolean, cropThumbnails: Boolean,
                      roundCorners: Int, signature: ObjectKey, skipMemoryCacheAtPaths: ArrayList<String>? = null) {
    target.isHorizontalScrolling = horizontalScroll
    if (type == TYPE_IMAGES || type == TYPE_VIDEOS || type == TYPE_RAWS || type == TYPE_PORTRAITS) {
        if (type == TYPE_IMAGES && path.isPng()) {
            loadPng(path, target, cropThumbnails, roundCorners, signature, skipMemoryCacheAtPaths)
        } else {
            loadJpg(path, target, cropThumbnails, roundCorners, signature, skipMemoryCacheAtPaths)
        }
    } else if (type == TYPE_GIFS) {
        if (!animateGifs) {
            loadStaticGIF(path, target, cropThumbnails, roundCorners, signature, skipMemoryCacheAtPaths)
            return
        }

        try {
            val gifDrawable = GifDrawable(path)
            target.setImageDrawable(gifDrawable)
            gifDrawable.start()

            target.scaleType = if (cropThumbnails) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
        } catch (e: Exception) {
            loadStaticGIF(path, target, cropThumbnails, roundCorners, signature, skipMemoryCacheAtPaths)
        } catch (e: OutOfMemoryError) {
            loadStaticGIF(path, target, cropThumbnails, roundCorners, signature, skipMemoryCacheAtPaths)
        }
    } else if (type == TYPE_SVGS) {
        loadSVG(path, target, cropThumbnails, roundCorners, signature)
    }
}

fun Context.addTempFolderIfNeeded(dirs: ArrayList<Directory>): ArrayList<Directory> {
    val tempFolderPath = config.tempFolderPath
    return if (tempFolderPath.isNotEmpty()) {
        val directories = ArrayList<Directory>()
        val newFolder = Directory(null, tempFolderPath, "", tempFolderPath.getFilenameFromPath(), 0, 0, 0, 0L, getPathLocation(tempFolderPath), 0, "")
        directories.add(newFolder)
        directories.addAll(dirs)
        directories
    } else {
        dirs
    }
}

fun Context.getPathLocation(path: String): Int {
    return when {
        isPathOnSD(path) -> LOCATION_SD
        isPathOnOTG(path) -> LOCATION_OTG
        else -> LOCATION_INTERNAL
    }
}

fun Context.loadPng(path: String, target: MySquareImageView, cropThumbnails: Boolean, roundCorners: Int, signature: ObjectKey, skipMemoryCacheAtPaths: ArrayList<String>? = null) {
    val options = RequestOptions()
        .signature(signature)
        .skipMemoryCache(skipMemoryCacheAtPaths?.contains(path) == true)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        .priority(Priority.LOW)
        .format(DecodeFormat.PREFER_ARGB_8888)

    if (cropThumbnails) options.centerCrop() else options.fitCenter()
    var builder = Glide.with(applicationContext)
        .asBitmap()
        .load(path)
        .apply(options)

    if (roundCorners != ROUNDED_CORNERS_NONE) {
        val cornerSize = if (roundCorners == ROUNDED_CORNERS_SMALL) R.dimen.rounded_corner_radius_small else R.dimen.rounded_corner_radius_big
        val cornerRadius = resources.getDimension(cornerSize).toInt()
        builder = builder.transform(CenterCrop(), RoundedCorners(cornerRadius))
    }

    builder.into(target)
}

fun Context.loadJpg(path: String, target: MySquareImageView, cropThumbnails: Boolean, roundCorners: Int, signature: ObjectKey, skipMemoryCacheAtPaths: ArrayList<String>? = null) {
    val options = RequestOptions()
        .signature(signature)
        .skipMemoryCache(skipMemoryCacheAtPaths?.contains(path) == true)
        .priority(Priority.LOW)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

    if (cropThumbnails) options.centerCrop() else options.fitCenter()
    var builder = Glide.with(applicationContext)
        .load(path)
        .apply(options)
        .transition(DrawableTransitionOptions.withCrossFade())

    if (roundCorners != ROUNDED_CORNERS_NONE) {
        val cornerSize = if (roundCorners == ROUNDED_CORNERS_SMALL) R.dimen.rounded_corner_radius_small else R.dimen.rounded_corner_radius_big
        val cornerRadius = resources.getDimension(cornerSize).toInt()
        builder = builder.transform(CenterCrop(), RoundedCorners(cornerRadius))
    }

    builder.into(target)
}

fun Context.loadStaticGIF(path: String, target: MySquareImageView, cropThumbnails: Boolean, roundCorners: Int, signature: ObjectKey, skipMemoryCacheAtPaths: ArrayList<String>? = null) {
    val options = RequestOptions()
        .signature(signature)
        .skipMemoryCache(skipMemoryCacheAtPaths?.contains(path) == true)
        .priority(Priority.LOW)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

    if (cropThumbnails) options.centerCrop() else options.fitCenter()
    var builder = Glide.with(applicationContext)
        .asBitmap() // make sure the GIF wont animate
        .load(path)
        .apply(options)

    if (roundCorners != ROUNDED_CORNERS_NONE) {
        val cornerSize = if (roundCorners == ROUNDED_CORNERS_SMALL) R.dimen.rounded_corner_radius_small else R.dimen.rounded_corner_radius_big
        val cornerRadius = resources.getDimension(cornerSize).toInt()
        builder = builder.transform(CenterCrop(), RoundedCorners(cornerRadius))
    }

    builder.into(target)
}

fun Context.loadSVG(path: String, target: MySquareImageView, cropThumbnails: Boolean, roundCorners: Int, signature: ObjectKey) {
    target.scaleType = if (cropThumbnails) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER

    val options = RequestOptions().signature(signature)
    var builder = Glide.with(applicationContext)
        .`as`(PictureDrawable::class.java)
        .listener(SvgSoftwareLayerSetter())
        .load(path)
        .apply(options)
        .transition(DrawableTransitionOptions.withCrossFade())

    if (roundCorners != ROUNDED_CORNERS_NONE) {
        val cornerSize = if (roundCorners == ROUNDED_CORNERS_SMALL) R.dimen.rounded_corner_radius_small else R.dimen.rounded_corner_radius_big
        val cornerRadius = resources.getDimension(cornerSize).toInt()
        builder = builder.transform(CenterCrop(), RoundedCorners(cornerRadius))
    }

    builder.into(target)
}

fun Context.getCachedDirectories(getVideosOnly: Boolean = false, getImagesOnly: Boolean = false, forceShowHidden: Boolean = false, callback: (ArrayList<Directory>) -> Unit) {
    ensureBackgroundThread {
        val directories = try {
            directoryDao.getAll() as ArrayList<Directory>
        } catch (e: Exception) {
            ArrayList<Directory>()
        }

        if (!config.showRecycleBinAtFolders) {
            directories.removeAll { it.isRecycleBin() }
        }

        val shouldShowHidden = config.shouldShowHidden || forceShowHidden
        val excludedPaths = config.excludedFolders
        val includedPaths = config.includedFolders

        val folderNoMediaStatuses = HashMap<String, Boolean>()
        val noMediaFolders = getNoMediaFoldersSync()
        noMediaFolders.forEach { folder ->
            folderNoMediaStatuses["$folder/$NOMEDIA"] = true
        }

        var filteredDirectories = directories.filter {
            it.path.shouldFolderBeVisible(excludedPaths, includedPaths, shouldShowHidden, folderNoMediaStatuses) { path, hasNoMedia ->
                folderNoMediaStatuses[path] = hasNoMedia
            }
        } as ArrayList<Directory>
        val filterMedia = config.filterMedia

        filteredDirectories = (when {
            getVideosOnly -> filteredDirectories.filter { it.types and TYPE_VIDEOS != 0 }
            getImagesOnly -> filteredDirectories.filter { it.types and TYPE_IMAGES != 0 }
            else -> filteredDirectories.filter {
                (filterMedia and TYPE_IMAGES != 0 && it.types and TYPE_IMAGES != 0) ||
                        (filterMedia and TYPE_VIDEOS != 0 && it.types and TYPE_VIDEOS != 0) ||
                        (filterMedia and TYPE_GIFS != 0 && it.types and TYPE_GIFS != 0) ||
                        (filterMedia and TYPE_RAWS != 0 && it.types and TYPE_RAWS != 0) ||
                        (filterMedia and TYPE_SVGS != 0 && it.types and TYPE_SVGS != 0) ||
                        (filterMedia and TYPE_PORTRAITS != 0 && it.types and TYPE_PORTRAITS != 0)
            }
        }) as ArrayList<Directory>

        if (shouldShowHidden) {
            val hiddenString = resources.getString(R.string.hidden)
            filteredDirectories.forEach {
                val noMediaPath = "${it.path}/$NOMEDIA"
                val hasNoMedia = if (folderNoMediaStatuses.keys.contains(noMediaPath)) {
                    folderNoMediaStatuses[noMediaPath]!!
                } else {
                    it.path.doesThisOrParentHaveNoMedia(folderNoMediaStatuses) { path, hasNoMedia ->
                        val newPath = "$path/$NOMEDIA"
                        folderNoMediaStatuses[newPath] = hasNoMedia
                    }
                }

                it.name = if (hasNoMedia && !it.path.isThisOrParentIncluded(includedPaths)) {
                    "${it.name.removeSuffix(hiddenString).trim()} $hiddenString"
                } else {
                    it.name.removeSuffix(hiddenString).trim()
                }
            }
        }

        val clone = filteredDirectories.clone() as ArrayList<Directory>
        callback(clone.distinctBy { it.path.getDistinctPath() } as ArrayList<Directory>)
        removeInvalidDBDirectories(filteredDirectories)
    }
}

fun Context.getCachedMedia(path: String, getVideosOnly: Boolean = false, getImagesOnly: Boolean = false, callback: (ArrayList<ThumbnailItem>) -> Unit) {
    ensureBackgroundThread {
        val mediaFetcher = MediaFetcher(this)
        val foldersToScan = if (path.isEmpty()) mediaFetcher.getFoldersToScan() else arrayListOf(path)
        var media = ArrayList<Medium>()
        if (path == FAVORITES) {
            media.addAll(mediaDB.getFavorites())
        }

        if (path == RECYCLE_BIN) {
            media.addAll(getUpdatedDeletedMedia())
        }

        if (config.filterMedia and TYPE_PORTRAITS != 0) {
            val foldersToAdd = ArrayList<String>()
            for (folder in foldersToScan) {
                val allFiles = File(folder).listFiles() ?: continue
                allFiles.filter { it.name.startsWith("img_", true) && it.isDirectory }.forEach {
                    foldersToAdd.add(it.absolutePath)
                }
            }
            foldersToScan.addAll(foldersToAdd)
        }

        val shouldShowHidden = config.shouldShowHidden
        foldersToScan.filter { path.isNotEmpty() || !config.isFolderProtected(it) }.forEach {
            try {
                val currMedia = mediaDB.getMediaFromPath(it)
                media.addAll(currMedia)
            } catch (ignored: Exception) {
            }
        }

        if (!shouldShowHidden) {
            media = media.filter { !it.path.contains("/.") } as ArrayList<Medium>
        }

        val filterMedia = config.filterMedia
        media = (when {
            getVideosOnly -> media.filter { it.type == TYPE_VIDEOS }
            getImagesOnly -> media.filter { it.type == TYPE_IMAGES }
            else -> media.filter {
                (filterMedia and TYPE_IMAGES != 0 && it.type == TYPE_IMAGES) ||
                        (filterMedia and TYPE_VIDEOS != 0 && it.type == TYPE_VIDEOS) ||
                        (filterMedia and TYPE_GIFS != 0 && it.type == TYPE_GIFS) ||
                        (filterMedia and TYPE_RAWS != 0 && it.type == TYPE_RAWS) ||
                        (filterMedia and TYPE_SVGS != 0 && it.type == TYPE_SVGS) ||
                        (filterMedia and TYPE_PORTRAITS != 0 && it.type == TYPE_PORTRAITS)
            }
        }) as ArrayList<Medium>

        val pathToUse = if (path.isEmpty()) SHOW_ALL else path
        mediaFetcher.sortMedia(media, config.getFolderSorting(pathToUse))
        val grouped = mediaFetcher.groupMedia(media, pathToUse)
        callback(grouped.clone() as ArrayList<ThumbnailItem>)
        val OTGPath = config.OTGPath

        try {
            val mediaToDelete = ArrayList<Medium>()
            // creating a new thread intentionally, do not reuse the common background thread
            Thread {
                media.filter { !getDoesFilePathExist(it.path, OTGPath) }.forEach {
                    if (it.path.startsWith(recycleBinPath)) {
                        deleteDBPath(it.path)
                    } else {
                        mediaToDelete.add(it)
                    }
                }

                if (mediaToDelete.isNotEmpty()) {
                    mediaDB.deleteMedia(*mediaToDelete.toTypedArray())

                    mediaToDelete.filter { it.isFavorite }.forEach {
                        favoritesDB.deleteFavoritePath(it.path)
                    }
                }
            }.start()
        } catch (ignored: Exception) {
        }
    }
}

fun Context.removeInvalidDBDirectories(dirs: ArrayList<Directory>? = null) {
    val dirsToCheck = dirs ?: directoryDao.getAll()
    val OTGPath = config.OTGPath
    dirsToCheck.filter { !it.areFavorites() && !it.isRecycleBin() && !getDoesFilePathExist(it.path, OTGPath) && it.path != config.tempFolderPath }.forEach {
        try {
            directoryDao.deleteDirPath(it.path)
        } catch (ignored: Exception) {
        }
    }
}

fun Context.updateDBMediaPath(oldPath: String, newPath: String) {
    val newFilename = newPath.getFilenameFromPath()
    val newParentPath = newPath.getParentPath()
    try {
        mediaDB.updateMedium(newFilename, newPath, newParentPath, oldPath)
        favoritesDB.updateFavorite(newFilename, newPath, newParentPath, oldPath)
    } catch (ignored: Exception) {
    }
}

fun Context.updateDBDirectory(directory: Directory) {
    try {
        directoryDao.updateDirectory(directory.path, directory.tmb, directory.mediaCnt, directory.modified, directory.taken, directory.size, directory.types, directory.sortValue)
    } catch (ignored: Exception) {
    }
}

fun Context.getOTGFolderChildren(path: String) = getDocumentFile(path)?.listFiles()

fun Context.getOTGFolderChildrenNames(path: String) = getOTGFolderChildren(path)?.map { it.name }?.toMutableList()

fun Context.getFavoritePaths(): ArrayList<String> {
    return try {
        favoritesDB.getValidFavoritePaths() as ArrayList<String>
    } catch (e: Exception) {
        ArrayList()
    }
}

fun Context.getFavoriteFromPath(path: String) = Favorite(null, path, path.getFilenameFromPath(), path.getParentPath())

fun Context.updateFavorite(path: String, isFavorite: Boolean) {
    if (isFavorite) {
        favoritesDB.insert(getFavoriteFromPath(path))
    } else {
        favoritesDB.deleteFavoritePath(path)
    }
}

// remove the "recycle_bin" from the file path prefix, replace it with real bin path /data/user...
fun Context.getUpdatedDeletedMedia(): ArrayList<Medium> {
    val media = try {
        mediaDB.getDeletedMedia() as ArrayList<Medium>
    } catch (ignored: Exception) {
        ArrayList<Medium>()
    }

    media.forEach {
        it.path = File(recycleBinPath, it.path.removePrefix(RECYCLE_BIN)).toString()
    }
    return media
}

fun Context.deleteDBPath(path: String) {
    deleteMediumWithPath(path.replaceFirst(recycleBinPath, RECYCLE_BIN))
}

fun Context.deleteMediumWithPath(path: String) {
    try {
        mediaDB.deleteMediumPath(path)
    } catch (ignored: Exception) {
    }
}

fun Context.updateWidgets() {
    val widgetIDs = AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(ComponentName(applicationContext, MyWidgetProvider::class.java))
    if (widgetIDs.isNotEmpty()) {
        Intent(applicationContext, MyWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
            sendBroadcast(this)
        }
    }
}

// based on https://github.com/sannies/mp4parser/blob/master/examples/src/main/java/com/google/code/mp4parser/example/PrintStructure.java
fun Context.parseFileChannel(path: String, fc: FileChannel, level: Int, start: Long, end: Long, callback: () -> Unit) {
    val FILE_CHANNEL_CONTAINERS = arrayListOf("moov", "trak", "mdia", "minf", "udta", "stbl")
    try {
        var iteration = 0
        var currEnd = end
        fc.position(start)
        if (currEnd <= 0) {
            currEnd = start + fc.size()
        }

        while (currEnd - fc.position() > 8) {
            // just a check to avoid deadloop at some videos
            if (iteration++ > 50) {
                return
            }

            val begin = fc.position()
            val byteBuffer = ByteBuffer.allocate(8)
            fc.read(byteBuffer)
            byteBuffer.rewind()
            val size = IsoTypeReader.readUInt32(byteBuffer)
            val type = IsoTypeReader.read4cc(byteBuffer)
            val newEnd = begin + size

            if (type == "uuid") {
                val fis = FileInputStream(File(path))
                fis.skip(begin)

                val sb = StringBuilder()
                val buffer = ByteArray(1024)
                while (sb.length < size) {
                    val n = fis.read(buffer)
                    if (n != -1) {
                        sb.append(String(buffer, 0, n))
                    } else {
                        break
                    }
                }

                val xmlString = sb.toString().toLowerCase()
                if (xmlString.contains("gspherical:projectiontype>equirectangular") || xmlString.contains("gspherical:projectiontype=\"equirectangular\"")) {
                    callback.invoke()
                }
                return
            }

            if (FILE_CHANNEL_CONTAINERS.contains(type)) {
                parseFileChannel(path, fc, level + 1, begin + 8, newEnd, callback)
            }

            fc.position(newEnd)
        }
    } catch (ignored: Exception) {
    }
}

fun Context.addPathToDB(path: String) {
    ensureBackgroundThread {
        if (!getDoesFilePathExist(path)) {
            return@ensureBackgroundThread
        }

        val type = when {
            path.isVideoFast() -> TYPE_VIDEOS
            path.isGif() -> TYPE_GIFS
            path.isRawFast() -> TYPE_RAWS
            path.isSvg() -> TYPE_SVGS
            path.isPortrait() -> TYPE_PORTRAITS
            else -> TYPE_IMAGES
        }

        try {
            val isFavorite = favoritesDB.isFavorite(path)
            val videoDuration = if (type == TYPE_VIDEOS) getDuration(path) ?: 0 else 0
            val medium = Medium(null, path.getFilenameFromPath(), path, path.getParentPath(), System.currentTimeMillis(), System.currentTimeMillis(),
                File(path).length(), type, videoDuration, isFavorite, 0L)

            mediaDB.insert(medium)
        } catch (ignored: Exception) {
        }
    }
}

fun Context.createDirectoryFromMedia(path: String, curMedia: ArrayList<Medium>, albumCovers: ArrayList<AlbumCover>, hiddenString: String,
                                     includedFolders: MutableSet<String>, getProperFileSize: Boolean, noMediaFolders: ArrayList<String>): Directory {
    val OTGPath = config.OTGPath
    val grouped = MediaFetcher(this).groupMedia(curMedia, path)
    var thumbnail: String? = null

    albumCovers.forEach {
        if (it.path == path && getDoesFilePathExist(it.tmb, OTGPath)) {
            thumbnail = it.tmb
        }
    }

    if (thumbnail == null) {
        val sortedMedia = grouped.filter { it is Medium }.toMutableList() as ArrayList<Medium>
        thumbnail = sortedMedia.firstOrNull { getDoesFilePathExist(it.path, OTGPath) }?.path ?: ""
    }

    if (config.OTGPath.isNotEmpty() && thumbnail!!.startsWith(config.OTGPath)) {
        thumbnail = thumbnail!!.getOTGPublicPath(applicationContext)
    }

    val isSortingAscending = config.directorySorting.isSortingAscending()
    val defaultMedium = Medium(0, "", "", "", 0L, 0L, 0L, 0, 0, false, 0L)
    val firstItem = curMedia.firstOrNull() ?: defaultMedium
    val lastItem = curMedia.lastOrNull() ?: defaultMedium
    val dirName = checkAppendingHidden(path, hiddenString, includedFolders, noMediaFolders)
    val lastModified = if (isSortingAscending) Math.min(firstItem.modified, lastItem.modified) else Math.max(firstItem.modified, lastItem.modified)
    val dateTaken = if (isSortingAscending) Math.min(firstItem.taken, lastItem.taken) else Math.max(firstItem.taken, lastItem.taken)
    val size = if (getProperFileSize) curMedia.sumByLong { it.size } else 0L
    val mediaTypes = curMedia.getDirMediaTypes()
    val sortValue = getDirectorySortingValue(curMedia, path, dirName, size)
    return Directory(null, path, thumbnail!!, dirName, curMedia.size, lastModified, dateTaken, size, getPathLocation(path), mediaTypes, sortValue)
}

fun Context.getDirectorySortingValue(media: ArrayList<Medium>, path: String, name: String, size: Long): String {
    val sorting = config.directorySorting
    val sorted = when {
        sorting and SORT_BY_NAME != 0 -> return name
        sorting and SORT_BY_PATH != 0 -> return path
        sorting and SORT_BY_SIZE != 0 -> return size.toString()
        sorting and SORT_BY_DATE_MODIFIED != 0 -> media.sortedBy { it.modified }
        sorting and SORT_BY_DATE_TAKEN != 0 -> media.sortedBy { it.taken }
        else -> media
    }

    val relevantMedium = if (sorting.isSortingAscending()) {
        sorted.firstOrNull() ?: return ""
    } else {
        sorted.lastOrNull() ?: return ""
    }

    val result: Any = when {
        sorting and SORT_BY_DATE_MODIFIED != 0 -> relevantMedium.modified
        sorting and SORT_BY_DATE_TAKEN != 0 -> relevantMedium.taken
        else -> 0
    }

    return result.toString()
}

fun Context.updateDirectoryPath(path: String) {
    val mediaFetcher = MediaFetcher(applicationContext)
    val getImagesOnly = false
    val getVideosOnly = false
    val hiddenString = getString(R.string.hidden)
    val albumCovers = config.parseAlbumCovers()
    val includedFolders = config.includedFolders
    val noMediaFolders = getNoMediaFoldersSync()

    val sorting = config.getFolderSorting(path)
    val grouping = config.getFolderGrouping(path)
    val getProperDateTaken = config.directorySorting and SORT_BY_DATE_TAKEN != 0 ||
            sorting and SORT_BY_DATE_TAKEN != 0 ||
            grouping and GROUP_BY_DATE_TAKEN_DAILY != 0 ||
            grouping and GROUP_BY_DATE_TAKEN_MONTHLY != 0

    val getProperLastModified = config.directorySorting and SORT_BY_DATE_MODIFIED != 0 ||
            sorting and SORT_BY_DATE_MODIFIED != 0 ||
            grouping and GROUP_BY_LAST_MODIFIED_DAILY != 0 ||
            grouping and GROUP_BY_LAST_MODIFIED_MONTHLY != 0

    val getProperFileSize = config.directorySorting and SORT_BY_SIZE != 0

    val lastModifieds = if (getProperLastModified) mediaFetcher.getFolderLastModifieds(path) else HashMap()
    val dateTakens = mediaFetcher.getFolderDateTakens(path)
    val favoritePaths = getFavoritePaths()
    val curMedia = mediaFetcher.getFilesFrom(path, getImagesOnly, getVideosOnly, getProperDateTaken, getProperLastModified, getProperFileSize,
        favoritePaths, false, lastModifieds, dateTakens)
    val directory = createDirectoryFromMedia(path, curMedia, albumCovers, hiddenString, includedFolders, getProperFileSize, noMediaFolders)
    updateDBDirectory(directory)
}

fun Context.getFileDateTaken(path: String): Long {
    val projection = arrayOf(
        Images.Media.DATE_TAKEN
    )

    val uri = Files.getContentUri("external")
    val selection = "${Images.Media.DATA} = ?"
    val selectionArgs = arrayOf(path)

    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getLongValue(Images.Media.DATE_TAKEN)
            }
        }
    } catch (ignored: Exception) {
    }

    return 0L
}
