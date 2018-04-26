package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.simplemobiletools.commons.dialogs.CreateNewFolderDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.databases.GalleryDataBase
import com.simplemobiletools.gallery.dialogs.ChangeSortingDialog
import com.simplemobiletools.gallery.dialogs.FilterMediaDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.interfaces.DirectoryDao
import com.simplemobiletools.gallery.models.AlbumCover
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*

class MainActivity : SimpleActivity(), DirectoryAdapter.DirOperationsListener {
    private val PICK_MEDIA = 2
    private val PICK_WALLPAPER = 3
    private val LAST_MEDIA_CHECK_PERIOD = 3000L
    private val NEW_APP_PACKAGE = "com.simplemobiletools.clock"

    private var mIsPickImageIntent = false
    private var mIsPickVideoIntent = false
    private var mIsGetImageContentIntent = false
    private var mIsGetVideoContentIntent = false
    private var mIsGetAnyContentIntent = false
    private var mIsSetWallpaperIntent = false
    private var mAllowPickingMultiple = false
    private var mIsThirdPartyIntent = false
    private var mIsGettingDirs = false
    private var mLoadedInitialPhotos = false
    private var mIsPasswordProtectionPending = false
    private var mLatestMediaId = 0L
    private var mLatestMediaDateId = 0L
    private var mLastMediaHandler = Handler()
    private var mTempShowHiddenHandler = Handler()
    private var mZoomListener: MyRecyclerView.MyZoomListener? = null

    private var mStoredAnimateGifs = true
    private var mStoredCropThumbnails = true
    private var mStoredScrollHorizontally = true
    private var mStoredShowMediaCount = true
    private var mStoredShowInfoBubble = true
    private var mStoredTextColor = 0
    private var mStoredPrimaryColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched()

        mIsPickImageIntent = isPickImageIntent(intent)
        mIsPickVideoIntent = isPickVideoIntent(intent)
        mIsGetImageContentIntent = isGetImageContentIntent(intent)
        mIsGetVideoContentIntent = isGetVideoContentIntent(intent)
        mIsGetAnyContentIntent = isGetAnyContentIntent(intent)
        mIsSetWallpaperIntent = isSetWallpaperIntent(intent)
        mAllowPickingMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        mIsThirdPartyIntent = mIsPickImageIntent || mIsPickVideoIntent || mIsGetImageContentIntent || mIsGetVideoContentIntent ||
                mIsGetAnyContentIntent || mIsSetWallpaperIntent

        removeTempFolder()
        directories_refresh_layout.setOnRefreshListener { getDirectories() }
        storeStateVariables()
        checkWhatsNewDialog()

        directories_empty_text.setOnClickListener {
            showFilterMediaDialog()
        }

        mIsPasswordProtectionPending = config.appPasswordProtectionOn
        setupLatestMediaId()

        // notify some users about the Clock app
        /*if (System.currentTimeMillis() < 1523750400000 && !config.wasNewAppShown && config.appRunCount > 100 && config.appRunCount % 50 != 0 && !isPackageInstalled(NEW_APP_PACKAGE)) {
            config.wasNewAppShown = true
            NewAppDialog(this, NEW_APP_PACKAGE, "Simple Clock")
        }*/

        if (!config.wasOTGHandled && hasPermission(PERMISSION_WRITE_STORAGE)) {
            checkOTGInclusion()
        }
    }

    override fun onStart() {
        super.onStart()
        mTempShowHiddenHandler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        config.isThirdPartyIntent = false

        if (mStoredAnimateGifs != config.animateGifs) {
            getRecyclerAdapter()?.updateAnimateGifs(config.animateGifs)
        }

        if (mStoredCropThumbnails != config.cropThumbnails) {
            getRecyclerAdapter()?.updateCropThumbnails(config.cropThumbnails)
        }

        if (mStoredShowMediaCount != config.showMediaCount) {
            getRecyclerAdapter()?.updateShowMediaCount(config.showMediaCount)
        }

        if (mStoredScrollHorizontally != config.scrollHorizontally) {
            getRecyclerAdapter()?.updateScrollHorizontally(config.viewTypeFolders != VIEW_TYPE_LIST && config.scrollHorizontally)
            setupScrollDirection()
        }

        if (mStoredTextColor != config.textColor) {
            getRecyclerAdapter()?.updateTextColor(config.textColor)
        }

        if (mStoredPrimaryColor != config.primaryColor) {
            getRecyclerAdapter()?.updatePrimaryColor(config.primaryColor)
            directories_vertical_fastscroller.updatePrimaryColor()
            directories_horizontal_fastscroller.updatePrimaryColor()
        }

        directories_horizontal_fastscroller.updateBubbleColors()
        directories_vertical_fastscroller.updateBubbleColors()
        directories_horizontal_fastscroller.allowBubbleDisplay = config.showInfoBubble
        directories_vertical_fastscroller.allowBubbleDisplay = config.showInfoBubble
        directories_refresh_layout.isEnabled = config.enablePullToRefresh
        invalidateOptionsMenu()
        directories_empty_text_label.setTextColor(config.textColor)
        directories_empty_text.setTextColor(getAdjustedPrimaryColor())

        if (mIsPasswordProtectionPending) {
            handleAppPasswordProtection {
                if (it) {
                    mIsPasswordProtectionPending = false
                    tryLoadGallery()
                } else {
                    finish()
                }
            }
        } else {
            tryLoadGallery()
        }
    }

    override fun onPause() {
        super.onPause()
        directories_refresh_layout.isRefreshing = false
        mIsGettingDirs = false
        storeStateVariables()
        mLastMediaHandler.removeCallbacksAndMessages(null)
    }

    override fun onStop() {
        super.onStop()
        if (config.temporarilyShowHidden) {
            mTempShowHiddenHandler.postDelayed({
                config.temporarilyShowHidden = false
            }, SHOW_TEMP_HIDDEN_DURATION)
        } else {
            mTempShowHiddenHandler.removeCallbacksAndMessages(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        config.temporarilyShowHidden = false
        mTempShowHiddenHandler.removeCallbacksAndMessages(null)
        removeTempFolder()
        if (!isChangingConfigurations) {
            GalleryDataBase.destroyInstance()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (mIsThirdPartyIntent) {
            menuInflater.inflate(R.menu.menu_main_intent, menu)
        } else {
            menuInflater.inflate(R.menu.menu_main, menu)
            menu.findItem(R.id.increase_column_count).isVisible = config.viewTypeFolders == VIEW_TYPE_GRID && config.dirColumnCnt < MAX_COLUMN_COUNT
            menu.findItem(R.id.reduce_column_count).isVisible = config.viewTypeFolders == VIEW_TYPE_GRID && config.dirColumnCnt > 1
        }
        menu.findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden
        menu.findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.filter -> showFilterMediaDialog()
            R.id.open_camera -> launchCamera()
            R.id.show_all -> showAllMedia()
            R.id.change_view_type -> changeViewType()
            R.id.temporarily_show_hidden -> tryToggleTemporarilyShowHidden()
            R.id.stop_showing_hidden -> tryToggleTemporarilyShowHidden()
            R.id.create_new_folder -> createNewFolder()
            R.id.increase_column_count -> increaseColumnCount()
            R.id.reduce_column_count -> reduceColumnCount()
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun getRecyclerAdapter() = directories_grid.adapter as? DirectoryAdapter

    private fun storeStateVariables() {
        config.apply {
            mStoredAnimateGifs = animateGifs
            mStoredCropThumbnails = cropThumbnails
            mStoredScrollHorizontally = scrollHorizontally
            mStoredShowMediaCount = showMediaCount
            mStoredShowInfoBubble = showInfoBubble
            mStoredTextColor = textColor
            mStoredPrimaryColor = primaryColor
        }
    }

    private fun removeTempFolder() {
        if (config.tempFolderPath.isNotEmpty()) {
            val newFolder = File(config.tempFolderPath)
            if (newFolder.exists() && newFolder.isDirectory) {
                if (newFolder.list()?.isEmpty() == true) {
                    toast(String.format(getString(R.string.deleting_folder), config.tempFolderPath), Toast.LENGTH_LONG)
                    tryDeleteFileDirItem(newFolder.toFileDirItem(applicationContext), true)
                }
            }
            config.tempFolderPath = ""
        }
    }

    private fun checkOTGInclusion() {
        Thread {
            if (hasOTGConnected()) {
                runOnUiThread {
                    handleOTGPermission {
                        config.addIncludedFolder(OTG_PATH)
                    }
                }
                config.wasOTGHandled = true
            }
        }.start()
    }

    private fun tryLoadGallery() {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                if (config.showAll) {
                    showAllMedia()
                } else {
                    getDirectories()
                }

                setupLayoutManager()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    private fun getDirectories() {
        if (mIsGettingDirs) {
            return
        }

        mIsGettingDirs = true
        val getImagesOnly = mIsPickImageIntent || mIsGetImageContentIntent
        val getVideosOnly = mIsPickVideoIntent || mIsGetVideoContentIntent

        getCachedDirectories(getVideosOnly, getImagesOnly) {
            if (!mLoadedInitialPhotos) {
                runOnUiThread {
                    directories_refresh_layout.isRefreshing = true
                }
            }
            gotDirectories(addTempFolderIfNeeded(it))
        }
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, true, false) {
            if (config.directorySorting and SORT_BY_DATE_MODIFIED > 0 || config.directorySorting and SORT_BY_DATE_TAKEN > 0) {
                getDirectories()
            } else {
                gotDirectories(getCurrentlyDisplayedDirs())
            }
        }
    }

    private fun showFilterMediaDialog() {
        FilterMediaDialog(this) {
            mLoadedInitialPhotos = false
            directories_refresh_layout.isRefreshing = true
            getDirectories()
        }
    }

    private fun showAllMedia() {
        config.showAll = true
        Intent(this, MediaActivity::class.java).apply {
            putExtra(DIRECTORY, "/")

            if (mIsThirdPartyIntent) {
                handleMediaIntent(this)
            } else {
                startActivity(this)
                finish()
            }
        }
    }

    private fun changeViewType() {
        val items = arrayListOf(
                RadioItem(VIEW_TYPE_GRID, getString(R.string.grid)),
                RadioItem(VIEW_TYPE_LIST, getString(R.string.list)))

        RadioGroupDialog(this, items, config.viewTypeFolders) {
            config.viewTypeFolders = it as Int
            invalidateOptionsMenu()
            setupLayoutManager()
            val dirs = getCurrentlyDisplayedDirs()
            directories_grid.adapter = null
            setupAdapter(dirs)
        }
    }

    private fun tryToggleTemporarilyShowHidden() {
        if (config.temporarilyShowHidden) {
            toggleTemporarilyShowHidden(false)
        } else {
            handleHiddenFolderPasswordProtection {
                toggleTemporarilyShowHidden(true)
            }
        }
    }

    private fun toggleTemporarilyShowHidden(show: Boolean) {
        mLoadedInitialPhotos = false
        config.temporarilyShowHidden = show
        getDirectories()
        invalidateOptionsMenu()
    }

    override fun deleteFolders(folders: ArrayList<File>) {
        val fileDirItems = folders.map { FileDirItem(it.absolutePath, it.name, true) } as ArrayList<FileDirItem>
        deleteFolders(fileDirItems) {
            runOnUiThread {
                refreshItems()
            }

            Thread {
                val directoryDao = galleryDB.DirectoryDao()
                folders.filter { !it.exists() }.forEach {
                    directoryDao.deleteDirPath(it.absolutePath)
                }
            }.start()
        }
    }

    private fun setupLayoutManager() {
        if (config.viewTypeFolders == VIEW_TYPE_GRID) {
            setupGridLayoutManager()
        } else {
            setupListLayoutManager()
        }
    }

    private fun setupGridLayoutManager() {
        val layoutManager = directories_grid.layoutManager as MyGridLayoutManager
        if (config.scrollHorizontally) {
            layoutManager.orientation = GridLayoutManager.HORIZONTAL
            directories_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            layoutManager.orientation = GridLayoutManager.VERTICAL
            directories_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        layoutManager.spanCount = config.dirColumnCnt
    }

    private fun initZoomListener() {
        if (config.viewTypeFolders == VIEW_TYPE_GRID) {
            val layoutManager = directories_grid.layoutManager as MyGridLayoutManager
            mZoomListener = object : MyRecyclerView.MyZoomListener {
                override fun zoomIn() {
                    if (layoutManager.spanCount > 1) {
                        reduceColumnCount()
                        getRecyclerAdapter()?.finishActMode()
                    }
                }

                override fun zoomOut() {
                    if (layoutManager.spanCount < MAX_COLUMN_COUNT) {
                        increaseColumnCount()
                        getRecyclerAdapter()?.finishActMode()
                    }
                }
            }
        } else {
            mZoomListener = null
        }
    }

    private fun setupListLayoutManager() {
        val layoutManager = directories_grid.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = 1
        layoutManager.orientation = GridLayoutManager.VERTICAL
        directories_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        mZoomListener = null
    }

    private fun createNewFolder() {
        FilePickerDialog(this, internalStoragePath, false, config.shouldShowHidden) {
            CreateNewFolderDialog(this, it) {
                config.tempFolderPath = it
                gotDirectories(addTempFolderIfNeeded(getCurrentlyDisplayedDirs()))
            }
        }
    }

    private fun increaseColumnCount() {
        directories_vertical_fastscroller.measureRecyclerViewOnRedraw()
        directories_horizontal_fastscroller.measureRecyclerViewOnRedraw()
        config.dirColumnCnt = ++(directories_grid.layoutManager as MyGridLayoutManager).spanCount
        invalidateOptionsMenu()
        directories_grid.adapter?.notifyDataSetChanged()
    }

    private fun reduceColumnCount() {
        directories_vertical_fastscroller.measureRecyclerViewOnRedraw()
        directories_horizontal_fastscroller.measureRecyclerViewOnRedraw()
        config.dirColumnCnt = --(directories_grid.layoutManager as MyGridLayoutManager).spanCount
        invalidateOptionsMenu()
        directories_grid.adapter?.notifyDataSetChanged()
    }

    private fun isPickImageIntent(intent: Intent) = isPickIntent(intent) && (hasImageContentData(intent) || isImageType(intent))

    private fun isPickVideoIntent(intent: Intent) = isPickIntent(intent) && (hasVideoContentData(intent) || isVideoType(intent))

    private fun isPickIntent(intent: Intent) = intent.action == Intent.ACTION_PICK

    private fun isGetContentIntent(intent: Intent) = intent.action == Intent.ACTION_GET_CONTENT && intent.type != null

    private fun isGetImageContentIntent(intent: Intent) = isGetContentIntent(intent) &&
            (intent.type.startsWith("image/") || intent.type == MediaStore.Images.Media.CONTENT_TYPE)

    private fun isGetVideoContentIntent(intent: Intent) = isGetContentIntent(intent) &&
            (intent.type.startsWith("video/") || intent.type == MediaStore.Video.Media.CONTENT_TYPE)

    private fun isGetAnyContentIntent(intent: Intent) = isGetContentIntent(intent) && intent.type == "*/*"

    private fun isSetWallpaperIntent(intent: Intent?) = intent?.action == Intent.ACTION_SET_WALLPAPER

    private fun hasImageContentData(intent: Intent) = (intent.data == MediaStore.Images.Media.EXTERNAL_CONTENT_URI ||
            intent.data == MediaStore.Images.Media.INTERNAL_CONTENT_URI)

    private fun hasVideoContentData(intent: Intent) = (intent.data == MediaStore.Video.Media.EXTERNAL_CONTENT_URI ||
            intent.data == MediaStore.Video.Media.INTERNAL_CONTENT_URI)

    private fun isImageType(intent: Intent) = (intent.type?.startsWith("image/") == true || intent.type == MediaStore.Images.Media.CONTENT_TYPE)

    private fun isVideoType(intent: Intent) = (intent.type?.startsWith("video/") == true || intent.type == MediaStore.Video.Media.CONTENT_TYPE)

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_MEDIA && resultData != null) {
                val resultIntent = Intent()
                if (mIsThirdPartyIntent) {
                    when {
                        intent.extras?.containsKey(MediaStore.EXTRA_OUTPUT) == true -> fillExtraOutput(resultData)
                        resultData.extras?.containsKey(PICKED_PATHS) == true -> fillPickedPaths(resultData, resultIntent)
                        else -> fillIntentPath(resultData, resultIntent)
                    }
                }

                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else if (requestCode == PICK_WALLPAPER) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun fillExtraOutput(resultData: Intent) {
        val path = resultData.data.path
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            val output = intent.extras.get(MediaStore.EXTRA_OUTPUT) as Uri
            inputStream = FileInputStream(File(path))
            outputStream = contentResolver.openOutputStream(output)
            inputStream.copyTo(outputStream)
        } catch (e: SecurityException) {
            showErrorToast(e)
        } catch (ignored: FileNotFoundException) {
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    private fun fillPickedPaths(resultData: Intent, resultIntent: Intent) {
        val paths = resultData.extras.getStringArrayList(PICKED_PATHS)
        val uris = paths.map { getFilePublicUri(File(it), BuildConfig.APPLICATION_ID) } as ArrayList
        val clipData = ClipData("Attachment", arrayOf("image/*", "video/*"), ClipData.Item(uris.removeAt(0)))

        uris.forEach {
            clipData.addItem(ClipData.Item(it))
        }

        resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        resultIntent.clipData = clipData
    }

    private fun fillIntentPath(resultData: Intent, resultIntent: Intent) {
        val path = resultData.data.path
        val uri = getFilePublicUri(File(path), BuildConfig.APPLICATION_ID)
        val type = path.getMimeType()
        resultIntent.setDataAndTypeAndNormalize(uri, type)
        resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun itemClicked(path: String) {
        Intent(this, MediaActivity::class.java).apply {
            putExtra(DIRECTORY, path)
            handleMediaIntent(this)
        }
    }

    private fun handleMediaIntent(intent: Intent) {
        intent.apply {
            if (mIsSetWallpaperIntent) {
                putExtra(SET_WALLPAPER_INTENT, true)
                startActivityForResult(this, PICK_WALLPAPER)
            } else {
                putExtra(GET_IMAGE_INTENT, mIsPickImageIntent || mIsGetImageContentIntent)
                putExtra(GET_VIDEO_INTENT, mIsPickVideoIntent || mIsGetVideoContentIntent)
                putExtra(GET_ANY_INTENT, mIsGetAnyContentIntent)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, mAllowPickingMultiple)
                startActivityForResult(this, PICK_MEDIA)
            }
        }
    }

    private fun gotDirectories(newDirs: ArrayList<Directory>) {
        val dirs = getSortedDirectories(newDirs)
        var isPlaceholderVisible = dirs.isEmpty()

        runOnUiThread {
            checkPlaceholderVisibility(dirs)

            val allowHorizontalScroll = config.scrollHorizontally && config.viewTypeFiles == VIEW_TYPE_GRID
            directories_vertical_fastscroller.beVisibleIf(directories_grid.isVisible() && !allowHorizontalScroll)
            directories_horizontal_fastscroller.beVisibleIf(directories_grid.isVisible() && allowHorizontalScroll)
            setupAdapter(dirs)
        }

        // cached folders have been loaded, recheck folders one by one starting with the first displayed
        Thread {
            val mediaFetcher = MediaFetcher(applicationContext)
            val getImagesOnly = mIsPickImageIntent || mIsGetImageContentIntent
            val getVideosOnly = mIsPickVideoIntent || mIsGetVideoContentIntent
            val hiddenString = getString(R.string.hidden)
            val albumCovers = config.parseAlbumCovers()
            val includedFolders = config.includedFolders
            val isSortingAscending = config.directorySorting and SORT_DESCENDING == 0
            val mediumDao = galleryDB.MediumDao()
            val directoryDao = galleryDB.DirectoryDao()

            for (directory in dirs) {
                val curMedia = mediaFetcher.getFilesFrom(directory.path, getImagesOnly, getVideosOnly)
                val newDir = if (curMedia.isEmpty()) {
                    directory
                } else {
                    createDirectoryFromMedia(directory.path, curMedia, albumCovers, hiddenString, includedFolders, isSortingAscending)
                }

                // we are looping through the already displayed folders looking for changes, do not do anything if nothing changed
                if (directory == newDir) {
                    continue
                }

                directory.apply {
                    tmb = newDir.tmb
                    mediaCnt = newDir.mediaCnt
                    modified = newDir.modified
                    taken = newDir.taken
                    this@apply.size = newDir.size
                    types = newDir.types
                }

                showSortedDirs(dirs)

                // update directories and media files in the local db, delete invalid items
                updateDBDirectory(directory)
                mediumDao.insertAll(curMedia)
                getCachedMedia(directory.path, getVideosOnly, getImagesOnly) {
                    it.forEach {
                        if (!curMedia.contains(it)) {
                            mediumDao.deleteMediumPath(it.path)
                        }
                    }
                }
            }

            val foldersToScan = mediaFetcher.getFoldersToScan("")
            dirs.forEach {
                foldersToScan.remove(it.path)
            }

            for (folder in foldersToScan) {
                val newMedia = mediaFetcher.getFilesFrom(folder, getImagesOnly, getVideosOnly)
                if (newMedia.isEmpty()) {
                    continue
                }

                if (isPlaceholderVisible) {
                    isPlaceholderVisible = false
                    runOnUiThread {
                        directories_empty_text_label.beGone()
                        directories_empty_text.beGone()
                        directories_grid.beVisible()
                    }
                }

                val newDir = createDirectoryFromMedia(folder, newMedia, albumCovers, hiddenString, includedFolders, isSortingAscending)
                dirs.add(newDir)
                showSortedDirs(dirs)
                directoryDao.insert(newDir)
                mediumDao.insertAll(newMedia)
            }

            mIsGettingDirs = false
            mLoadedInitialPhotos = true
            checkLastMediaChanged()

            runOnUiThread {
                directories_refresh_layout.isRefreshing = false
                directories_vertical_fastscroller.measureRecyclerView()
                checkPlaceholderVisibility(dirs)
            }
            checkInvalidDirectories(dirs, directoryDao)
        }.start()
    }

    private fun checkPlaceholderVisibility(dirs: ArrayList<Directory>) {
        directories_empty_text_label.beVisibleIf(dirs.isEmpty() && mLoadedInitialPhotos)
        directories_empty_text.beVisibleIf(dirs.isEmpty() && mLoadedInitialPhotos)
        directories_grid.beVisibleIf(directories_empty_text_label.isGone())
    }

    private fun showSortedDirs(dirs: ArrayList<Directory>) {
        var sortedDirs = getSortedDirectories(dirs).clone() as ArrayList<Directory>
        sortedDirs = sortedDirs.distinctBy { it.path.toLowerCase() } as ArrayList<Directory>

        runOnUiThread {
            (directories_grid.adapter as DirectoryAdapter).updateDirs(sortedDirs)
        }
    }

    private fun createDirectoryFromMedia(path: String, curMedia: ArrayList<Medium>, albumCovers: ArrayList<AlbumCover>, hiddenString: String,
                                         includedFolders: MutableSet<String>, isSortingAscending: Boolean): Directory {
        var thumbnail = curMedia.firstOrNull { getDoesFilePathExist(it.path) }?.path ?: ""
        if (thumbnail.startsWith(OTG_PATH)) {
            thumbnail = thumbnail.getOTGPublicPath(applicationContext)
        }

        albumCovers.forEach {
            if (it.path == path && getDoesFilePathExist(it.tmb)) {
                thumbnail = it.tmb
            }
        }

        val mediaTypes = curMedia.getDirMediaTypes()
        val dirName = checkAppendingHidden(path, hiddenString, includedFolders)

        val firstItem = curMedia.first()
        val lastItem = curMedia.last()
        val lastModified = if (isSortingAscending) Math.min(firstItem.modified, lastItem.modified) else Math.max(firstItem.modified, lastItem.modified)
        val dateTaken = if (isSortingAscending) Math.min(firstItem.taken, lastItem.taken) else Math.max(firstItem.taken, lastItem.taken)
        val size = curMedia.sumByLong { it.size }
        return Directory(null, path, thumbnail, dirName, curMedia.size, lastModified, dateTaken, size, getPathLocation(path), mediaTypes)
    }

    private fun setupAdapter(dirs: ArrayList<Directory>) {
        val currAdapter = directories_grid.adapter
        if (currAdapter == null) {
            initZoomListener()
            val fastscroller = if (config.scrollHorizontally) directories_horizontal_fastscroller else directories_vertical_fastscroller
            DirectoryAdapter(this, dirs, this, directories_grid, isPickIntent(intent) || isGetAnyContentIntent(intent), fastscroller) {
                val path = (it as Directory).path
                if (path != config.tempFolderPath) {
                    itemClicked(path)
                }
            }.apply {
                setupZoomListener(mZoomListener)
                directories_grid.adapter = this
            }
        } else {
            (currAdapter as DirectoryAdapter).updateDirs(dirs)
        }

        setupScrollDirection()
    }

    private fun setupScrollDirection() {
        val allowHorizontalScroll = config.scrollHorizontally && config.viewTypeFolders == VIEW_TYPE_GRID

        directories_vertical_fastscroller.isHorizontal = false
        directories_vertical_fastscroller.beGoneIf(allowHorizontalScroll)

        directories_horizontal_fastscroller.isHorizontal = true
        directories_horizontal_fastscroller.beVisibleIf(allowHorizontalScroll)

        if (allowHorizontalScroll) {
            directories_horizontal_fastscroller.allowBubbleDisplay = config.showInfoBubble
            directories_horizontal_fastscroller.setViews(directories_grid, directories_refresh_layout) {
                directories_horizontal_fastscroller.updateBubbleText(getBubbleTextItem(it))
            }
        } else {
            directories_vertical_fastscroller.allowBubbleDisplay = config.showInfoBubble
            directories_vertical_fastscroller.setViews(directories_grid, directories_refresh_layout) {
                directories_vertical_fastscroller.updateBubbleText(getBubbleTextItem(it))
            }
        }
    }

    private fun checkInvalidDirectories(dirs: ArrayList<Directory>, directoryDao: DirectoryDao) {
        val invalidDirs = ArrayList<Directory>()
        dirs.forEach {
            if (!getDoesFilePathExist(it.path)) {
                invalidDirs.add(it)
            } else if (it.path != config.tempFolderPath) {
                val children = if (it.path.startsWith(OTG_PATH)) getOTGFolderChildrenNames(it.path) else File(it.path).list()?.asList()
                val hasMediaFile = children?.any { it.isImageVideoGif() } ?: false
                if (!hasMediaFile) {
                    invalidDirs.add(it)
                }
            }
        }

        if (invalidDirs.isNotEmpty()) {
            dirs.removeAll(invalidDirs)
            showSortedDirs(dirs)
            invalidDirs.forEach {
                directoryDao.deleteDirPath(it.path)
            }
        }
    }

    private fun getCurrentlyDisplayedDirs() = getRecyclerAdapter()?.dirs ?: ArrayList()

    private fun getBubbleTextItem(index: Int) = getRecyclerAdapter()?.dirs?.getOrNull(index)?.getBubbleText() ?: ""

    private fun setupLatestMediaId() {
        Thread {
            if (hasPermission(PERMISSION_READ_STORAGE)) {
                mLatestMediaId = getLatestMediaId()
                mLatestMediaDateId = getLatestMediaByDateId()
            }
        }.start()
    }

    private fun checkLastMediaChanged() {
        if (isActivityDestroyed()) {
            return
        }

        mLastMediaHandler.postDelayed({
            Thread {
                val mediaId = getLatestMediaId()
                val mediaDateId = getLatestMediaByDateId()
                if (mLatestMediaId != mediaId || mLatestMediaDateId != mediaDateId) {
                    mLatestMediaId = mediaId
                    mLatestMediaDateId = mediaDateId
                    runOnUiThread {
                        getDirectories()
                    }
                } else {
                    mLastMediaHandler.removeCallbacksAndMessages(null)
                    checkLastMediaChanged()
                }
            }.start()
        }, LAST_MEDIA_CHECK_PERIOD)
    }

    override fun refreshItems() {
        getDirectories()
    }

    override fun recheckPinnedFolders() {
        gotDirectories(movePinnedDirectoriesToFront(getCurrentlyDisplayedDirs()))
    }

    override fun updateDirectories(directories: ArrayList<Directory>) {
        Thread {
            storeDirectoryItems(directories)
            removeInvalidDBDirectories()
        }.start()
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(46, R.string.release_46))
            add(Release(47, R.string.release_47))
            add(Release(49, R.string.release_49))
            add(Release(50, R.string.release_50))
            add(Release(51, R.string.release_51))
            add(Release(52, R.string.release_52))
            add(Release(54, R.string.release_54))
            add(Release(58, R.string.release_58))
            add(Release(62, R.string.release_62))
            add(Release(65, R.string.release_65))
            add(Release(66, R.string.release_66))
            add(Release(69, R.string.release_69))
            add(Release(70, R.string.release_70))
            add(Release(72, R.string.release_72))
            add(Release(74, R.string.release_74))
            add(Release(76, R.string.release_76))
            add(Release(77, R.string.release_77))
            add(Release(83, R.string.release_83))
            add(Release(84, R.string.release_84))
            add(Release(88, R.string.release_88))
            add(Release(89, R.string.release_89))
            add(Release(93, R.string.release_93))
            add(Release(94, R.string.release_94))
            add(Release(97, R.string.release_97))
            add(Release(98, R.string.release_98))
            add(Release(108, R.string.release_108))
            add(Release(112, R.string.release_112))
            add(Release(114, R.string.release_114))
            add(Release(115, R.string.release_115))
            add(Release(118, R.string.release_118))
            add(Release(119, R.string.release_119))
            add(Release(122, R.string.release_122))
            add(Release(123, R.string.release_123))
            add(Release(125, R.string.release_125))
            add(Release(127, R.string.release_127))
            add(Release(133, R.string.release_133))
            add(Release(136, R.string.release_136))
            add(Release(137, R.string.release_137))
            add(Release(138, R.string.release_138))
            add(Release(143, R.string.release_143))
            add(Release(158, R.string.release_158))
            add(Release(159, R.string.release_159))
            add(Release(163, R.string.release_163))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
