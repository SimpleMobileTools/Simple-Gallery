package com.simplemobiletools.gallery.pro.extensions

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Point
import android.graphics.drawable.PictureDrawable
import android.media.AudioManager
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.SettingsActivity
import com.simplemobiletools.gallery.pro.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.pro.databases.GalleryDatabase
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.interfaces.DirectoryDao
import com.simplemobiletools.gallery.pro.interfaces.MediumDao
import com.simplemobiletools.gallery.pro.interfaces.WidgetsDao
import com.simplemobiletools.gallery.pro.models.AlbumCover
import com.simplemobiletools.gallery.pro.models.Directory
import com.simplemobiletools.gallery.pro.models.Medium
import com.simplemobiletools.gallery.pro.models.ThumbnailItem
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

val Context.portrait get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
val Context.audioManager get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager
val Context.windowManager: WindowManager get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager
val Context.navigationBarRight: Boolean get() = usableScreenSize.x < realScreenSize.x
val Context.navigationBarBottom: Boolean get() = usableScreenSize.y < realScreenSize.y
val Context.navigationBarHeight: Int get() = if (navigationBarBottom) navigationBarSize.y else 0
val Context.navigationBarWidth: Int get() = if (navigationBarRight) navigationBarSize.x else 0

internal val Context.navigationBarSize: Point
    get() = when {
        navigationBarRight -> Point(newNavigationBarHeight, usableScreenSize.y)
        navigationBarBottom -> Point(usableScreenSize.x, newNavigationBarHeight)
        else -> Point()
    }

internal val Context.newNavigationBarHeight: Int
    get() {
        var navigationBarHeight = 0
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            navigationBarHeight = resources.getDimensionPixelSize(resourceId)
        }
        return navigationBarHeight
    }

internal val Context.statusBarHeight: Int
    get() {
        var statusBarHeight = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

internal val Context.actionBarHeight: Int
    get() {
        val styledAttributes = theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val actionBarHeight = styledAttributes.getDimension(0, 0f)
        styledAttributes.recycle()
        return actionBarHeight.toInt()
    }


val Context.usableScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        return size
    }

val Context.realScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)
        return size
    }

fun Context.getHumanizedFilename(path: String): String {
    val humanized = humanizePath(path)
    return humanized.substring(humanized.lastIndexOf("/") + 1)
}

fun Context.launchSettings() {
    startActivity(Intent(applicationContext, SettingsActivity::class.java))
}

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.galleryDB: GalleryDatabase get() = GalleryDatabase.getInstance(applicationContext)

val Context.widgetsDB: WidgetsDao get() = GalleryDatabase.getInstance(applicationContext).WidgetsDao()

val Context.directoryDB: DirectoryDao get() = GalleryDatabase.getInstance(applicationContext).DirectoryDao()

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

    if (config.useRecycleBin && config.showRecycleBinAtFolders && config.showRecycleBinLast) {
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
                    AlphanumericComparator().compare(o1.name.toLowerCase(), o2.name.toLowerCase())
                } else {
                    o1.name.toLowerCase().compareTo(o2.name.toLowerCase())
                }
            }
            sorting and SORT_BY_PATH != 0 -> {
                if (sorting and SORT_USE_NUMERIC_VALUE != 0) {
                    AlphanumericComparator().compare(o1.path.toLowerCase(), o2.path.toLowerCase())
                } else {
                    o1.path.toLowerCase().compareTo(o2.path.toLowerCase())
                }
            }
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
            if (!folders.contains(parent) && dirs.none { it.path == parent }) {
                currentPaths.add(parent)
                val isSortingAscending = config.sorting and SORT_DESCENDING == 0
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
                            mediaTypes)

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
    Thread {
        val folders = ArrayList<String>()

        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? AND ${MediaStore.Files.FileColumns.TITLE} LIKE ?"
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString(), "%$NOMEDIA%")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            if (cursor?.moveToFirst() == true) {
                do {
                    val path = cursor.getStringValue(MediaStore.Files.FileColumns.DATA) ?: continue
                    val noMediaFile = File(path)
                    if (noMediaFile.exists() && noMediaFile.name == NOMEDIA) {
                        folders.add("${noMediaFile.parent}/")
                    }
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }

        callback(folders)
    }.start()
}

fun Context.rescanFolderMedia(path: String) {
    Thread {
        rescanFolderMediaSync(path)
    }.start()
}

fun Context.rescanFolderMediaSync(path: String) {
    getCachedMedia(path) {
        val cached = it
        GetMediaAsynctask(applicationContext, path, false, false, false) {
            Thread {
                val newMedia = it
                val mediumDao = galleryDB.MediumDao()
                val media = newMedia.filter { it is Medium } as ArrayList<Medium>
                mediumDao.insertAll(media)

                cached.forEach {
                    if (!newMedia.contains(it)) {
                        val mediumPath = (it as? Medium)?.path
                        if (mediumPath != null) {
                            deleteDBPath(mediumDao, mediumPath)
                        }
                    }
                }
            }.start()
        }.execute()
    }
}

fun Context.storeDirectoryItems(items: ArrayList<Directory>, directoryDao: DirectoryDao) {
    Thread {
        directoryDao.insertAll(items)
    }.start()
}

fun Context.checkAppendingHidden(path: String, hidden: String, includedFolders: MutableSet<String>): String {
    val dirName = getFolderNameFromPath(path)
    return if (File(path).doesThisOrParentHaveNoMedia() && !path.isThisOrParentIncluded(includedFolders)) {
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
                      skipMemoryCacheAtPaths: ArrayList<String>? = null) {
    target.isHorizontalScrolling = horizontalScroll
    if (type == TYPE_IMAGES || type == TYPE_VIDEOS || type == TYPE_RAWS) {
        if (type == TYPE_IMAGES && path.isPng()) {
            loadPng(path, target, cropThumbnails, skipMemoryCacheAtPaths)
        } else {
            loadJpg(path, target, cropThumbnails, skipMemoryCacheAtPaths)
        }
    } else if (type == TYPE_GIFS) {
        try {
            val gifDrawable = GifDrawable(path)
            target.setImageDrawable(gifDrawable)
            if (animateGifs) {
                gifDrawable.start()
            } else {
                gifDrawable.stop()
            }

            target.scaleType = if (cropThumbnails) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
        } catch (e: Exception) {
            loadJpg(path, target, cropThumbnails, skipMemoryCacheAtPaths)
        } catch (e: OutOfMemoryError) {
            loadJpg(path, target, cropThumbnails, skipMemoryCacheAtPaths)
        }
    } else if (type == TYPE_SVGS) {
        loadSVG(path, target, cropThumbnails)
    }
}

fun Context.addTempFolderIfNeeded(dirs: ArrayList<Directory>): ArrayList<Directory> {
    val tempFolderPath = config.tempFolderPath
    return if (tempFolderPath.isNotEmpty()) {
        val directories = ArrayList<Directory>()
        val newFolder = Directory(null, tempFolderPath, "", tempFolderPath.getFilenameFromPath(), 0, 0, 0, 0L, getPathLocation(tempFolderPath), 0)
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

fun Context.loadPng(path: String, target: MySquareImageView, cropThumbnails: Boolean, skipMemoryCacheAtPaths: ArrayList<String>? = null) {
    val options = RequestOptions()
            .signature(path.getFileSignature())
            .skipMemoryCache(skipMemoryCacheAtPaths?.contains(path) == true)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .priority(Priority.LOW)
            .format(DecodeFormat.PREFER_ARGB_8888)

    val builder = Glide.with(applicationContext)
            .asBitmap()
            .load(path)

    if (cropThumbnails) options.centerCrop() else options.fitCenter()
    builder.apply(options).into(target)
}

fun Context.loadJpg(path: String, target: MySquareImageView, cropThumbnails: Boolean, skipMemoryCacheAtPaths: ArrayList<String>? = null) {
    val options = RequestOptions()
            .signature(path.getFileSignature())
            .skipMemoryCache(skipMemoryCacheAtPaths?.contains(path) == true)
            .priority(Priority.LOW)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

    val builder = Glide.with(applicationContext)
            .load(path)

    if (cropThumbnails) options.centerCrop() else options.fitCenter()
    builder.apply(options)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(target)
}

fun Context.loadSVG(path: String, target: MySquareImageView, cropThumbnails: Boolean) {
    target.scaleType = if (cropThumbnails) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER

    val options = RequestOptions().signature(path.getFileSignature())
    Glide.with(applicationContext)
            .`as`(PictureDrawable::class.java)
            .listener(SvgSoftwareLayerSetter())
            .load(path)
            .apply(options)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(target)
}

fun Context.getCachedDirectories(getVideosOnly: Boolean = false, getImagesOnly: Boolean = false, directoryDao: DirectoryDao = galleryDB.DirectoryDao(), forceShowHidden: Boolean = false, callback: (ArrayList<Directory>) -> Unit) {
    Thread {
        val directories = try {
            directoryDao.getAll() as ArrayList<Directory>
        } catch (e: Exception) {
            ArrayList<Directory>()
        }

        if (!config.showRecycleBinAtFolders || !config.useRecycleBin) {
            directories.removeAll { it.isRecycleBin() }
        }

        val shouldShowHidden = config.shouldShowHidden || forceShowHidden
        val excludedPaths = config.excludedFolders
        val includedPaths = config.includedFolders
        var filteredDirectories = directories.filter { it.path.shouldFolderBeVisible(excludedPaths, includedPaths, shouldShowHidden) } as ArrayList<Directory>
        val filterMedia = config.filterMedia

        filteredDirectories = (when {
            getVideosOnly -> filteredDirectories.filter { it.types and TYPE_VIDEOS != 0 }
            getImagesOnly -> filteredDirectories.filter { it.types and TYPE_IMAGES != 0 }
            else -> filteredDirectories.filter {
                (filterMedia and TYPE_IMAGES != 0 && it.types and TYPE_IMAGES != 0) ||
                        (filterMedia and TYPE_VIDEOS != 0 && it.types and TYPE_VIDEOS != 0) ||
                        (filterMedia and TYPE_GIFS != 0 && it.types and TYPE_GIFS != 0) ||
                        (filterMedia and TYPE_RAWS != 0 && it.types and TYPE_RAWS != 0) ||
                        (filterMedia and TYPE_SVGS != 0 && it.types and TYPE_SVGS != 0)
            }
        }) as ArrayList<Directory>

        val hiddenString = resources.getString(R.string.hidden)
        filteredDirectories.forEach {
            it.name = if (File(it.path).doesThisOrParentHaveNoMedia() && !it.path.isThisOrParentIncluded(includedPaths)) {
                "${it.name.removeSuffix(hiddenString).trim()} $hiddenString"
            } else {
                it.name
            }
        }

        val clone = filteredDirectories.clone() as ArrayList<Directory>
        callback(clone.distinctBy { it.path.getDistinctPath() } as ArrayList<Directory>)

        removeInvalidDBDirectories(filteredDirectories, directoryDao)
    }.start()
}

fun Context.getCachedMedia(path: String, getVideosOnly: Boolean = false, getImagesOnly: Boolean = false, mediumDao: MediumDao = galleryDB.MediumDao(),
                           callback: (ArrayList<ThumbnailItem>) -> Unit) {
    Thread {
        val mediaFetcher = MediaFetcher(this)
        val foldersToScan = if (path.isEmpty()) mediaFetcher.getFoldersToScan() else arrayListOf(path)
        var media = ArrayList<Medium>()
        if (path == FAVORITES) {
            media.addAll(mediumDao.getFavorites())
        }

        if (path == RECYCLE_BIN) {
            media.addAll(getUpdatedDeletedMedia(mediumDao))
        }

        val shouldShowHidden = config.shouldShowHidden
        foldersToScan.forEach {
            try {
                val currMedia = mediumDao.getMediaFromPath(it)
                media.addAll(currMedia)
            } catch (ignored: IllegalStateException) {
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
                        (filterMedia and TYPE_SVGS != 0 && it.type == TYPE_SVGS)
            }
        }) as ArrayList<Medium>

        val pathToUse = if (path.isEmpty()) SHOW_ALL else path
        mediaFetcher.sortMedia(media, config.getFileSorting(pathToUse))
        val grouped = mediaFetcher.groupMedia(media, pathToUse)
        callback(grouped.clone() as ArrayList<ThumbnailItem>)

        val mediaToDelete = ArrayList<Medium>()
        media.filter { !File(it.path).exists() }.forEach {
            if (it.path.startsWith(recycleBinPath)) {
                deleteDBPath(mediumDao, it.path)
            } else {
                mediaToDelete.add(it)
            }
        }

        try {
            if (mediaToDelete.isNotEmpty()) {
                mediumDao.deleteMedia(*mediaToDelete.toTypedArray())
            }
        } catch (ignored: Exception) {
        }
    }.start()
}

fun Context.removeInvalidDBDirectories(dirs: ArrayList<Directory>? = null, directoryDao: DirectoryDao = galleryDB.DirectoryDao()) {
    val dirsToCheck = dirs ?: directoryDao.getAll()
    dirsToCheck.filter { !it.areFavorites() && !it.isRecycleBin() && !File(it.path).exists() && it.path != config.tempFolderPath }.forEach {
        directoryDao.deleteDirPath(it.path)
    }
}

fun Context.updateDBMediaPath(oldPath: String, newPath: String) {
    val newFilename = newPath.getFilenameFromPath()
    val newParentPath = newPath.getParentPath()
    try {
        galleryDB.MediumDao().updateMedium(oldPath, newParentPath, newFilename, newPath)
    } catch (ignored: Exception) {
    }
}

fun Context.updateDBDirectory(directory: Directory, directoryDao: DirectoryDao) {
    directoryDao.updateDirectory(directory.path, directory.tmb, directory.mediaCnt, directory.modified, directory.taken, directory.size, directory.types)
}

fun Context.getFavoritePaths(): ArrayList<String> {
    return try {
        galleryDB.MediumDao().getFavoritePaths() as ArrayList<String>
    } catch (e: Exception) {
        ArrayList()
    }
}

// remove the "recycle_bin" from the file path prefix, replace it with real bin path /data/user...
fun Context.getUpdatedDeletedMedia(mediumDao: MediumDao): ArrayList<Medium> {
    val media = try {
        mediumDao.getDeletedMedia() as ArrayList<Medium>
    } catch (ignored: Exception) {
        ArrayList<Medium>()
    }

    media.forEach {
        it.path = File(recycleBinPath, it.path.removePrefix(RECYCLE_BIN)).toString()
    }
    return media
}

fun Context.deleteDBPath(mediumDao: MediumDao, path: String) {
    try {
        mediumDao.deleteMediumPath(path.replaceFirst(recycleBinPath, RECYCLE_BIN))
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
                while (true) {
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
    Thread {
        if (!File(path).exists()) {
            return@Thread
        }

        val type = when {
            path.isVideoFast() -> TYPE_VIDEOS
            path.isGif() -> TYPE_GIFS
            path.isRawFast() -> TYPE_RAWS
            path.isSvg() -> TYPE_SVGS
            else -> TYPE_IMAGES
        }

        val videoDuration = if (type == TYPE_VIDEOS) path.getVideoDuration() else 0
        val medium = Medium(null, path.getFilenameFromPath(), path, path.getParentPath(), System.currentTimeMillis(), System.currentTimeMillis(),
                File(path).length(), type, videoDuration, false, 0L)
        try {
            galleryDB.MediumDao().insert(medium)
        } catch (ignored: Exception) {
        }
    }.start()
}

fun Context.createDirectoryFromMedia(path: String, curMedia: ArrayList<Medium>, albumCovers: ArrayList<AlbumCover>, hiddenString: String,
                                     includedFolders: MutableSet<String>, isSortingAscending: Boolean, getProperFileSize: Boolean): Directory {
    var thumbnail = curMedia.firstOrNull { File(it.path).exists() }?.path ?: ""
    albumCovers.forEach {
        if (it.path == path && File(it.tmb).exists()) {
            thumbnail = it.tmb
        }
    }

    val defaultMedium = Medium(0, "", "", "", 0L, 0L, 0L, 0, 0, false, 0L)
    val firstItem = curMedia.firstOrNull() ?: defaultMedium
    val lastItem = curMedia.lastOrNull() ?: defaultMedium
    val dirName = checkAppendingHidden(path, hiddenString, includedFolders)
    val lastModified = if (isSortingAscending) Math.min(firstItem.modified, lastItem.modified) else Math.max(firstItem.modified, lastItem.modified)
    val dateTaken = if (isSortingAscending) Math.min(firstItem.taken, lastItem.taken) else Math.max(firstItem.taken, lastItem.taken)
    val size = if (getProperFileSize) curMedia.sumByLong { it.size } else 0L
    val mediaTypes = curMedia.getDirMediaTypes()
    return Directory(null, path, thumbnail, dirName, curMedia.size, lastModified, dateTaken, size, getPathLocation(path), mediaTypes)
}

fun Context.updateDirectoryPath(path: String) {
    val mediaFetcher = MediaFetcher(applicationContext)
    val getImagesOnly = false
    val getVideosOnly = false
    val hiddenString = getString(R.string.hidden)
    val albumCovers = config.parseAlbumCovers()
    val includedFolders = config.includedFolders
    val isSortingAscending = config.directorySorting and SORT_DESCENDING == 0
    val getProperDateTaken = config.directorySorting and SORT_BY_DATE_TAKEN != 0
    val getProperFileSize = config.directorySorting and SORT_BY_SIZE != 0
    val favoritePaths = getFavoritePaths()
    val curMedia = mediaFetcher.getFilesFrom(path, getImagesOnly, getVideosOnly, getProperDateTaken, getProperFileSize, favoritePaths, false)
    val directory = createDirectoryFromMedia(path, curMedia, albumCovers, hiddenString, includedFolders, isSortingAscending, getProperFileSize)
    updateDBDirectory(directory, galleryDB.DirectoryDao())
}
