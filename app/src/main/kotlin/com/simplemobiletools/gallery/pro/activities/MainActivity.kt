package com.simplemobiletools.gallery.pro.activities

import android.app.Activity
import android.app.SearchManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.CreateNewFolderDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.pro.BuildConfig
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.pro.asynctasks.ProgressNotificationAsyncTask
import com.simplemobiletools.gallery.pro.databases.GalleryDatabase
import com.simplemobiletools.gallery.pro.dialogs.ChangeSortingDialog
import com.simplemobiletools.gallery.pro.dialogs.ChangeViewTypeDialog
import com.simplemobiletools.gallery.pro.dialogs.FilterMediaDialog
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.interfaces.DirectoryDao
import com.simplemobiletools.gallery.pro.interfaces.DirectoryOperationsListener
import com.simplemobiletools.gallery.pro.interfaces.MediumDao
import com.simplemobiletools.gallery.pro.jobs.NewPhotoFetcher
import com.simplemobiletools.gallery.pro.models.Directory
import com.simplemobiletools.gallery.pro.models.Medium
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : SimpleActivity(), DirectoryOperationsListener, ProgressNotificationAsyncTask.Listener {
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
    private var mWasProtectionHandled = false
    private var mShouldStopFetching = false
    private var mIsSearchOpen = false
    private var mLatestMediaId = 0L
    private var mLatestMediaDateId = 0L
    private var mCurrentPathPrefix = ""                 // used at "Group direct subfolders" for navigation
    private var mOpenedSubfolders = arrayListOf("")     // used at "Group direct subfolders" for navigating Up with the back button
    private var mLastMediaHandler = Handler()
    private var mTempShowHiddenHandler = Handler()
    private var mZoomListener: MyRecyclerView.MyZoomListener? = null
    private var mSearchMenuItem: MenuItem? = null
    private var mDirs = ArrayList<Directory>()

    private var mStoredAnimateGifs = true
    private var mStoredCropThumbnails = true
    private var mStoredScrollHorizontally = true
    private var mStoredShowMediaCount = true
    private var mStoredShowInfoBubble = true
    private var mStoredTextColor = 0
    private var mStoredPrimaryColor = 0

    private lateinit var mMediumDao: MediumDao
    private lateinit var mDirectoryDao: DirectoryDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)

        mMediumDao = galleryDB.MediumDao()
        mDirectoryDao = galleryDB.DirectoryDao()

        if (savedInstanceState == null) {
            config.temporarilyShowHidden = false
            config.tempSkipDeleteConfirmation = false
            removeTempFolder()
            checkRecycleBinItems()
            startNewPhotoFetcher()
        }

        mIsPickImageIntent = isPickImageIntent(intent)
        mIsPickVideoIntent = isPickVideoIntent(intent)
        mIsGetImageContentIntent = isGetImageContentIntent(intent)
        mIsGetVideoContentIntent = isGetVideoContentIntent(intent)
        mIsGetAnyContentIntent = isGetAnyContentIntent(intent)
        mIsSetWallpaperIntent = isSetWallpaperIntent(intent)
        mAllowPickingMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        mIsThirdPartyIntent = mIsPickImageIntent || mIsPickVideoIntent || mIsGetImageContentIntent || mIsGetVideoContentIntent ||
                mIsGetAnyContentIntent || mIsSetWallpaperIntent

        directories_refresh_layout.setOnRefreshListener { getDirectories() }
        storeStateVariables()
        checkWhatsNewDialog()

        mIsPasswordProtectionPending = config.isAppPasswordProtectionOn
        setupLatestMediaId()

        // notify some users about the Clock app
        /*if (System.currentTimeMillis() < 1523750400000 && !config.wasNewAppShown && config.appRunCount > 100 && config.appRunCount % 50 != 0 && !isPackageInstalled(NEW_APP_PACKAGE)) {
            config.wasNewAppShown = true
            NewAppDialog(this, NEW_APP_PACKAGE, "Simple Clock")
        }*/

        if (!config.wereFavoritesPinned) {
            config.addPinnedFolders(hashSetOf(FAVORITES))
            config.wereFavoritesPinned = true
        }

        if (!config.wasRecycleBinPinned) {
            config.addPinnedFolders(hashSetOf(RECYCLE_BIN))
            config.wasRecycleBinPinned = true
            config.saveFolderGrouping(SHOW_ALL, GROUP_BY_DATE_TAKEN_DAILY or GROUP_DESCENDING)
        }

        if (!config.wasSVGShowingHandled) {
            config.wasSVGShowingHandled = true
            if (config.filterMedia and TYPE_SVGS == 0) {
                config.filterMedia += TYPE_SVGS
            }
        }

        if (!config.wasSortingByNumericValueAdded) {
            config.wasSortingByNumericValueAdded = true
            config.sorting = config.sorting or SORT_USE_NUMERIC_VALUE
        }

        updateWidgets()
        registerFileUpdateListener()

        directories_switch_searching.setOnClickListener {
            launchSearchActivity()
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
            mLoadedInitialPhotos = false
            directories_grid.adapter = null
            getDirectories()
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
        directories_switch_searching.setTextColor(getAdjustedPrimaryColor())
        directories_switch_searching.underlineText()

        if (mIsPasswordProtectionPending && !mWasProtectionHandled) {
            handleAppPasswordProtection {
                mWasProtectionHandled = it
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
        mSearchMenuItem?.collapseActionView()

        if (config.temporarilyShowHidden || config.tempSkipDeleteConfirmation) {
            mTempShowHiddenHandler.postDelayed({
                config.temporarilyShowHidden = false
                config.tempSkipDeleteConfirmation = false
            }, SHOW_TEMP_HIDDEN_DURATION)
        } else {
            mTempShowHiddenHandler.removeCallbacksAndMessages(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            config.temporarilyShowHidden = false
            config.tempSkipDeleteConfirmation = false
            mTempShowHiddenHandler.removeCallbacksAndMessages(null)
            removeTempFolder()
            unregisterFileUpdateListener()

            if (!config.showAll) {
                GalleryDatabase.destroyInstance()
            }
        }
    }

    override fun onBackPressed() {
        if (config.groupDirectSubfolders) {
            if (mCurrentPathPrefix.isEmpty()) {
                super.onBackPressed()
            } else {
                mOpenedSubfolders.removeAt(mOpenedSubfolders.size - 1)
                mCurrentPathPrefix = mOpenedSubfolders.last()
                setupAdapter(mDirs)
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (mIsThirdPartyIntent) {
            menuInflater.inflate(R.menu.menu_main_intent, menu)
        } else {
            menuInflater.inflate(R.menu.menu_main, menu)
            val useBin = config.useRecycleBin
            menu.apply {
                findItem(R.id.increase_column_count).isVisible = config.viewTypeFolders == VIEW_TYPE_GRID && config.dirColumnCnt < MAX_COLUMN_COUNT
                findItem(R.id.reduce_column_count).isVisible = config.viewTypeFolders == VIEW_TYPE_GRID && config.dirColumnCnt > 1
                findItem(R.id.hide_the_recycle_bin).isVisible = useBin && config.showRecycleBinAtFolders
                findItem(R.id.show_the_recycle_bin).isVisible = useBin && !config.showRecycleBinAtFolders
                setupSearch(this)
            }
        }

        menu.findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden
        menu.findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden

        updateMenuItemColors(menu)
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
            R.id.show_the_recycle_bin -> toggleRecycleBin(true)
            R.id.hide_the_recycle_bin -> toggleRecycleBin(false)
            R.id.increase_column_count -> increaseColumnCount()
            R.id.reduce_column_count -> reduceColumnCount()
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(WAS_PROTECTION_HANDLED, mWasProtectionHandled)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mWasProtectionHandled = savedInstanceState.getBoolean(WAS_PROTECTION_HANDLED, false)
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

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        mSearchMenuItem = menu.findItem(R.id.search)
        (mSearchMenuItem?.actionView as? SearchView)?.apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (mIsSearchOpen) {
                        setupAdapter(mDirs, newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                directories_switch_searching.beVisible()
                mIsSearchOpen = true
                directories_refresh_layout.isEnabled = false
                return true
            }

            // this triggers on device rotation too, avoid doing anything
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                if (mIsSearchOpen) {
                    directories_switch_searching.beGone()
                    mIsSearchOpen = false
                    directories_refresh_layout.isEnabled = config.enablePullToRefresh
                    setupAdapter(mDirs, "")
                }
                return true
            }
        })
    }

    private fun startNewPhotoFetcher() {
        if (isNougatPlus()) {
            val photoFetcher = NewPhotoFetcher()
            if (!photoFetcher.isScheduled(applicationContext)) {
                photoFetcher.scheduleJob(applicationContext)
            }
        }
    }

    private fun removeTempFolder() {
        if (config.tempFolderPath.isNotEmpty()) {
            val newFolder = File(config.tempFolderPath)
            if (newFolder.exists() && newFolder.isDirectory) {
                if (newFolder.list()?.isEmpty() == true && newFolder.getProperSize(true) == 0L && newFolder.getFileCount(true) == 0) {
                    toast(String.format(getString(R.string.deleting_folder), config.tempFolderPath), Toast.LENGTH_LONG)
                    tryDeleteFileDirItem(newFolder.toFileDirItem(), true, true)
                }
            }
            config.tempFolderPath = ""
        }
    }

    private fun checkOTGPath() {
        ensureBackgroundThread {
            if (!config.wasOTGHandled && hasPermission(PERMISSION_WRITE_STORAGE) && hasOTGConnected() && config.OTGPath.isEmpty()) {
                getStorageDirectories().firstOrNull { it.trimEnd('/') != internalStoragePath && it.trimEnd('/') != sdCardPath }?.apply {
                    config.wasOTGHandled = true
                    val otgPath = trimEnd('/')
                    config.OTGPath = otgPath
                    config.addIncludedFolder(otgPath)
                }

                if (config.OTGPath.isEmpty()) {
                    runOnUiThread {
                        ConfirmationDialog(this, getString(R.string.usb_detected), positive = R.string.ok, negative = 0) {
                            config.wasOTGHandled = true
                            showOTGPermissionDialog()
                        }
                    }
                }
            }
        }
    }

    private fun checkDefaultSpamFolders() {
        if (!config.spamFoldersChecked) {
            val spamFolders = arrayListOf(
                    "/storage/emulated/0/Android/data/com.facebook.orca/files/stickers"
            )

            spamFolders.forEach {
                if (File(it).exists()) {
                    config.addExcludedFolder(it)
                }
            }
            config.spamFoldersChecked = true
        }
    }

    private fun tryLoadGallery() {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                if (!config.wasUpgradedFromFreeShown && isPackageInstalled("com.simplemobiletools.gallery")) {
                    ConfirmationDialog(this, "", R.string.upgraded_from_free, R.string.ok, 0) {}
                    config.wasUpgradedFromFreeShown = true
                }

                checkOTGPath()
                checkDefaultSpamFolders()

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

        mShouldStopFetching = true
        mIsGettingDirs = true
        val getImagesOnly = mIsPickImageIntent || mIsGetImageContentIntent
        val getVideosOnly = mIsPickVideoIntent || mIsGetVideoContentIntent

        getCachedDirectories(getVideosOnly, getImagesOnly, mDirectoryDao) {
            gotDirectories(addTempFolderIfNeeded(it))
        }
    }

    private fun launchSearchActivity() {
        Intent(this, SearchActivity::class.java).apply {
            startActivity(this)
        }
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, true, false) {
            directories_grid.adapter = null
            if (config.directorySorting and SORT_BY_DATE_MODIFIED > 0 || config.directorySorting and SORT_BY_DATE_TAKEN > 0) {
                getDirectories()
            } else {
                ensureBackgroundThread {
                    gotDirectories(getCurrentlyDisplayedDirs())
                }
            }
        }
    }

    private fun showFilterMediaDialog() {
        FilterMediaDialog(this) {
            mShouldStopFetching = true
            directories_refresh_layout.isRefreshing = true
            directories_grid.adapter = null
            getDirectories()
        }
    }

    private fun showAllMedia() {
        config.showAll = true
        Intent(this, MediaActivity::class.java).apply {
            putExtra(DIRECTORY, "")

            if (mIsThirdPartyIntent) {
                handleMediaIntent(this)
            } else {
                startActivity(this)
                finish()
            }
        }
    }

    private fun changeViewType() {
        ChangeViewTypeDialog(this, true) {
            invalidateOptionsMenu()
            setupLayoutManager()
            directories_grid.adapter = null
            setupAdapter(mDirs)
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
        directories_grid.adapter = null
        getDirectories()
        invalidateOptionsMenu()
    }

    override fun deleteFolders(folders: ArrayList<File>) {
        val fileDirItems = folders.asSequence().filter { it.isDirectory }.map { FileDirItem(it.absolutePath, it.name, true) }.toMutableList() as ArrayList<FileDirItem>
        when {
            fileDirItems.isEmpty() -> return
            fileDirItems.size == 1 -> toast(String.format(getString(R.string.deleting_folder), fileDirItems.first().name))
            else -> {
                val baseString = if (config.useRecycleBin) R.plurals.moving_items_into_bin else R.plurals.delete_items
                val deletingItems = resources.getQuantityString(baseString, fileDirItems.size, fileDirItems.size)
                toast(deletingItems)
            }
        }

        val itemsToDelete = ArrayList<FileDirItem>()
        val filter = config.filterMedia
        val showHidden = config.shouldShowHidden
        fileDirItems.filter { it.isDirectory }.forEach {
            val files = File(it.path).listFiles()
            files?.filter {
                it.absolutePath.isMediaFile() && (showHidden || !it.name.startsWith('.')) &&
                        ((it.isImageFast() && filter and TYPE_IMAGES != 0) ||
                                (it.isVideoFast() && filter and TYPE_VIDEOS != 0) ||
                                (it.isGif() && filter and TYPE_GIFS != 0) ||
                                (it.isRawFast() && filter and TYPE_RAWS != 0) ||
                                (it.isSvg() && filter and TYPE_SVGS != 0))
            }?.mapTo(itemsToDelete) { it.toFileDirItem() }
        }

        if (config.useRecycleBin) {
            val pathsToDelete = ArrayList<String>()
            itemsToDelete.mapTo(pathsToDelete) { it.path }

            movePathsInRecycleBin(pathsToDelete, mMediumDao) {
                if (it) {
                    deleteFilteredFileDirItems(itemsToDelete, folders)
                } else {
                    toast(R.string.unknown_error_occurred)
                }
            }
        } else {
            deleteFilteredFileDirItems(itemsToDelete, folders)
        }
    }

    private fun deleteFilteredFileDirItems(fileDirItems: ArrayList<FileDirItem>, folders: ArrayList<File>) {
        ProgressNotificationAsyncTask(this, fileDirItems, folders, mDirectoryDao, this).execute()
    }

    override fun refresh() {
        refreshItems()
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
            layoutManager.orientation = RecyclerView.HORIZONTAL
            directories_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            layoutManager.orientation = RecyclerView.VERTICAL
            directories_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        layoutManager.spanCount = config.dirColumnCnt
    }

    private fun measureRecyclerViewContent(directories: ArrayList<Directory>) {
        directories_grid.onGlobalLayout {
            if (config.scrollHorizontally) {
                calculateContentWidth(directories)
            } else {
                calculateContentHeight(directories)
            }
        }
    }

    private fun calculateContentWidth(directories: ArrayList<Directory>) {
        val layoutManager = directories_grid.layoutManager as MyGridLayoutManager
        val thumbnailWidth = layoutManager.getChildAt(0)?.width ?: 0
        val fullWidth = ((directories.size - 1) / layoutManager.spanCount + 1) * thumbnailWidth
        directories_horizontal_fastscroller.setContentWidth(fullWidth)
        directories_horizontal_fastscroller.setScrollToX(directories_grid.computeHorizontalScrollOffset())
    }

    private fun calculateContentHeight(directories: ArrayList<Directory>) {
        val layoutManager = directories_grid.layoutManager as MyGridLayoutManager
        val thumbnailHeight = layoutManager.getChildAt(0)?.height ?: 0
        val fullHeight = ((directories.size - 1) / layoutManager.spanCount + 1) * thumbnailHeight
        directories_vertical_fastscroller.setContentHeight(fullHeight)
        directories_vertical_fastscroller.setScrollToY(directories_grid.computeVerticalScrollOffset())
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
        layoutManager.orientation = RecyclerView.VERTICAL
        directories_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        mZoomListener = null
    }

    private fun toggleRecycleBin(show: Boolean) {
        config.showRecycleBinAtFolders = show
        invalidateOptionsMenu()
        ensureBackgroundThread {
            var dirs = getCurrentlyDisplayedDirs()
            if (!show) {
                dirs = dirs.filter { it.path != RECYCLE_BIN } as ArrayList<Directory>
            }
            gotDirectories(dirs)
        }
    }

    private fun createNewFolder() {
        FilePickerDialog(this, internalStoragePath, false, config.shouldShowHidden, false, true) {
            CreateNewFolderDialog(this, it) {
                config.tempFolderPath = it
                ensureBackgroundThread {
                    gotDirectories(addTempFolderIfNeeded(getCurrentlyDisplayedDirs()))
                }
            }
        }
    }

    private fun increaseColumnCount() {
        config.dirColumnCnt = ++(directories_grid.layoutManager as MyGridLayoutManager).spanCount
        columnCountChanged()
    }

    private fun reduceColumnCount() {
        config.dirColumnCnt = --(directories_grid.layoutManager as MyGridLayoutManager).spanCount
        columnCountChanged()
    }

    private fun columnCountChanged() {
        invalidateOptionsMenu()
        directories_grid.adapter?.notifyDataSetChanged()
        getRecyclerAdapter()?.dirs?.apply {
            measureRecyclerViewContent(this)
        }
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
                var resultUri: Uri? = null
                if (mIsThirdPartyIntent) {
                    when {
                        intent.extras?.containsKey(MediaStore.EXTRA_OUTPUT) == true && intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0 -> {
                            resultUri = fillExtraOutput(resultData)
                        }
                        resultData.extras?.containsKey(PICKED_PATHS) == true -> fillPickedPaths(resultData, resultIntent)
                        else -> fillIntentPath(resultData, resultIntent)
                    }
                }

                if (resultUri != null) {
                    resultIntent.data = resultUri
                    resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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

    private fun fillExtraOutput(resultData: Intent): Uri? {
        val file = File(resultData.data.path)
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            val output = intent.extras.get(MediaStore.EXTRA_OUTPUT) as Uri
            inputStream = FileInputStream(file)
            outputStream = contentResolver.openOutputStream(output)
            inputStream.copyTo(outputStream)
        } catch (e: SecurityException) {
            showErrorToast(e)
        } catch (ignored: FileNotFoundException) {
            return getFilePublicUri(file, BuildConfig.APPLICATION_ID)
        } finally {
            inputStream?.close()
            outputStream?.close()
        }

        return null
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
        val data = resultData.data
        val path = if (data.toString().startsWith("/")) data.toString() else data.path
        val uri = getFilePublicUri(File(path), BuildConfig.APPLICATION_ID)
        val type = path.getMimeType()
        resultIntent.setDataAndTypeAndNormalize(uri, type)
        resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun itemClicked(path: String) {
        handleLockedFolderOpening(path) { success ->
            if (success) {
                Intent(this, MediaActivity::class.java).apply {
                    putExtra(DIRECTORY, path)
                    handleMediaIntent(this)
                }
            }
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
        mIsGettingDirs = false
        mShouldStopFetching = false

        // if hidden item showing is disabled but all Favorite items are hidden, hide the Favorites folder
        if (!config.shouldShowHidden) {
            val favoritesFolder = newDirs.firstOrNull { it.areFavorites() }
            if (favoritesFolder != null && favoritesFolder.tmb.getFilenameFromPath().startsWith('.')) {
                newDirs.remove(favoritesFolder)
            }
        }

        val dirs = getSortedDirectories(newDirs)
        var isPlaceholderVisible = dirs.isEmpty()

        runOnUiThread {
            checkPlaceholderVisibility(dirs)

            val allowHorizontalScroll = config.scrollHorizontally && config.viewTypeFolders == VIEW_TYPE_GRID
            directories_vertical_fastscroller.beVisibleIf(directories_grid.isVisible() && !allowHorizontalScroll)
            directories_horizontal_fastscroller.beVisibleIf(directories_grid.isVisible() && allowHorizontalScroll)
            setupAdapter(dirs.clone() as ArrayList<Directory>)
        }

        // cached folders have been loaded, recheck folders one by one starting with the first displayed
        val mediaFetcher = MediaFetcher(applicationContext)
        val getImagesOnly = mIsPickImageIntent || mIsGetImageContentIntent
        val getVideosOnly = mIsPickVideoIntent || mIsGetVideoContentIntent
        val hiddenString = getString(R.string.hidden)
        val albumCovers = config.parseAlbumCovers()
        val includedFolders = config.includedFolders
        val tempFolderPath = config.tempFolderPath
        val isSortingAscending = config.directorySorting and SORT_DESCENDING == 0
        val getProperDateTaken = config.directorySorting and SORT_BY_DATE_TAKEN != 0
        val getProperFileSize = config.directorySorting and SORT_BY_SIZE != 0
        val favoritePaths = getFavoritePaths()
        val dirPathsToRemove = ArrayList<String>()

        try {
            for (directory in dirs) {
                if (mShouldStopFetching) {
                    return
                }

                val curMedia = mediaFetcher.getFilesFrom(directory.path, getImagesOnly, getVideosOnly, getProperDateTaken, getProperFileSize, favoritePaths, false)
                val newDir = if (curMedia.isEmpty()) {
                    if (directory.path != tempFolderPath) {
                        dirPathsToRemove.add(directory.path)
                    }
                    directory
                } else {
                    createDirectoryFromMedia(directory.path, curMedia, albumCovers, hiddenString, includedFolders, isSortingAscending, getProperFileSize)
                }

                // we are looping through the already displayed folders looking for changes, do not do anything if nothing changed
                if (directory.copy(subfoldersCount = 0, subfoldersMediaCount = 0) == newDir) {
                    continue
                }

                directory.apply {
                    tmb = newDir.tmb
                    name = newDir.name
                    mediaCnt = newDir.mediaCnt
                    modified = newDir.modified
                    taken = newDir.taken
                    this@apply.size = newDir.size
                    types = newDir.types
                }

                setupAdapter(dirs)

                // update directories and media files in the local db, delete invalid items
                updateDBDirectory(directory, mDirectoryDao)
                if (!directory.isRecycleBin()) {
                    mMediumDao.insertAll(curMedia)
                }
                getCachedMedia(directory.path, getVideosOnly, getImagesOnly, mMediumDao) {
                    it.forEach {
                        if (!curMedia.contains(it)) {
                            val path = (it as? Medium)?.path
                            if (path != null) {
                                deleteDBPath(mMediumDao, path)
                            }
                        }
                    }
                }
            }

            if (dirPathsToRemove.isNotEmpty()) {
                val dirsToRemove = dirs.filter { dirPathsToRemove.contains(it.path) }
                dirsToRemove.forEach {
                    mDirectoryDao.deleteDirPath(it.path)
                }
                dirs.removeAll(dirsToRemove)
                setupAdapter(dirs)
            }
        } catch (ignored: Exception) {
        }

        val foldersToScan = mediaFetcher.getFoldersToScan()
        foldersToScan.add(FAVORITES)
        if (config.useRecycleBin && config.showRecycleBinAtFolders) {
            foldersToScan.add(RECYCLE_BIN)
        } else {
            foldersToScan.remove(RECYCLE_BIN)
        }

        dirs.forEach {
            foldersToScan.remove(it.path)
        }

        // check the remaining folders which were not cached at all yet
        for (folder in foldersToScan) {
            if (mShouldStopFetching) {
                return
            }

            val newMedia = mediaFetcher.getFilesFrom(folder, getImagesOnly, getVideosOnly, getProperDateTaken, getProperFileSize, favoritePaths, false)
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

            val newDir = createDirectoryFromMedia(folder, newMedia, albumCovers, hiddenString, includedFolders, isSortingAscending, getProperFileSize)
            dirs.add(newDir)
            setupAdapter(dirs)
            try {
                mDirectoryDao.insert(newDir)
                if (folder != RECYCLE_BIN) {
                    mMediumDao.insertAll(newMedia)
                }
            } catch (ignored: Exception) {
            }
        }

        mLoadedInitialPhotos = true
        checkLastMediaChanged()

        runOnUiThread {
            directories_refresh_layout.isRefreshing = false
            checkPlaceholderVisibility(dirs)
        }
        checkInvalidDirectories(dirs)

        val everShownFolders = config.everShownFolders as HashSet
        dirs.mapTo(everShownFolders) { it.path }

        try {
            config.everShownFolders = everShownFolders
        } catch (e: Exception) {
            config.everShownFolders = HashSet()
        }
        mDirs = dirs.clone() as ArrayList<Directory>

        if (mDirs.size > 55) {
            excludeSpamFolders()
        }
    }

    private fun checkPlaceholderVisibility(dirs: ArrayList<Directory>) {
        directories_empty_text_label.beVisibleIf(dirs.isEmpty() && mLoadedInitialPhotos)
        directories_empty_text.beVisibleIf(dirs.isEmpty() && mLoadedInitialPhotos)

        if (mIsSearchOpen) {
            directories_empty_text_label.text = getString(R.string.no_items_found)
            directories_empty_text.beGone()
        } else if (dirs.isEmpty() && config.filterMedia == TYPE_DEFAULT_FILTER) {
            directories_empty_text_label.text = getString(R.string.no_media_add_included)
            directories_empty_text.text = getString(R.string.add_folder)

            directories_empty_text.setOnClickListener {
                showAddIncludedFolderDialog {
                    refreshItems()
                }
            }
        } else {
            directories_empty_text_label.text = getString(R.string.no_media_with_filters)
            directories_empty_text.text = getString(R.string.change_filters_underlined)

            directories_empty_text.setOnClickListener {
                showFilterMediaDialog()
            }
        }

        directories_empty_text.underlineText()
        directories_grid.beVisibleIf(directories_empty_text_label.isGone())
    }

    private fun setupAdapter(dirs: ArrayList<Directory>, textToSearch: String = "") {
        val currAdapter = directories_grid.adapter
        val distinctDirs = dirs.distinctBy { it.path.getDistinctPath() }.toMutableList() as ArrayList<Directory>
        val sortedDirs = getSortedDirectories(distinctDirs)
        var dirsToShow = getDirsToShow(sortedDirs, mDirs, mCurrentPathPrefix).clone() as ArrayList<Directory>

        if (currAdapter == null) {
            initZoomListener()
            val fastscroller = if (config.scrollHorizontally) directories_horizontal_fastscroller else directories_vertical_fastscroller
            DirectoryAdapter(this, dirsToShow, this, directories_grid, isPickIntent(intent) || isGetAnyContentIntent(intent), fastscroller) {
                val clickedDir = it as Directory
                val path = clickedDir.path
                if (clickedDir.subfoldersCount == 1 || !config.groupDirectSubfolders) {
                    if (path != config.tempFolderPath) {
                        itemClicked(path)
                    }
                } else {
                    mCurrentPathPrefix = path
                    mOpenedSubfolders.add(path)
                    setupAdapter(mDirs, "")
                }
            }.apply {
                setupZoomListener(mZoomListener)
                runOnUiThread {
                    directories_grid.adapter = this
                    setupScrollDirection()
                }
            }
            measureRecyclerViewContent(dirsToShow)
        } else {
            runOnUiThread {
                if (textToSearch.isNotEmpty()) {
                    dirsToShow = dirsToShow.filter { it.name.contains(textToSearch, true) }.sortedBy { !it.name.startsWith(textToSearch, true) }.toMutableList() as ArrayList
                }
                checkPlaceholderVisibility(dirsToShow)

                (directories_grid.adapter as? DirectoryAdapter)?.updateDirs(dirsToShow)
                measureRecyclerViewContent(dirsToShow)
            }
        }

        // recyclerview sometimes becomes empty at init/update, triggering an invisible refresh like this seems to work fine
        directories_grid.postDelayed({
            directories_grid.scrollBy(0, 0)
        }, 500)
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

    private fun checkInvalidDirectories(dirs: ArrayList<Directory>) {
        val invalidDirs = ArrayList<Directory>()
        dirs.filter { !it.areFavorites() && !it.isRecycleBin() }.forEach {
            if (!File(it.path).exists()) {
                invalidDirs.add(it)
            } else if (it.path != config.tempFolderPath) {
                val children = File(it.path).list()?.asList()
                val hasMediaFile = children?.any { it?.isMediaFile() == true } ?: false
                if (!hasMediaFile) {
                    invalidDirs.add(it)
                }
            }
        }

        if (getFavoritePaths().isEmpty()) {
            val favoritesFolder = dirs.firstOrNull { it.areFavorites() }
            if (favoritesFolder != null) {
                invalidDirs.add(favoritesFolder)
            }
        }

        if (config.useRecycleBin) {
            try {
                val binFolder = dirs.firstOrNull { it.path == RECYCLE_BIN }
                if (binFolder != null && mMediumDao.getDeletedMedia().isEmpty()) {
                    invalidDirs.add(binFolder)
                }
            } catch (ignored: Exception) {
            }
        }

        if (invalidDirs.isNotEmpty()) {
            dirs.removeAll(invalidDirs)
            setupAdapter(dirs)
            invalidDirs.forEach {
                try {
                    mDirectoryDao.deleteDirPath(it.path)
                } catch (ignored: Exception) {
                }
            }
        }
    }

    private fun getCurrentlyDisplayedDirs() = getRecyclerAdapter()?.dirs ?: ArrayList()

    private fun getBubbleTextItem(index: Int) = getRecyclerAdapter()?.dirs?.getOrNull(index)?.getBubbleText(config.directorySorting, this)
            ?: ""

    private fun setupLatestMediaId() {
        ensureBackgroundThread {
            if (hasPermission(PERMISSION_READ_STORAGE)) {
                mLatestMediaId = getLatestMediaId()
                mLatestMediaDateId = getLatestMediaByDateId()
            }
        }
    }

    private fun checkLastMediaChanged() {
        if (isDestroyed) {
            return
        }

        mLastMediaHandler.postDelayed({
            ensureBackgroundThread {
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
            }
        }, LAST_MEDIA_CHECK_PERIOD)
    }

    private fun checkRecycleBinItems() {
        if (config.useRecycleBin && config.lastBinCheck < System.currentTimeMillis() - DAY_SECONDS * 1000) {
            config.lastBinCheck = System.currentTimeMillis()
            Handler().postDelayed({
                ensureBackgroundThread {
                    try {
                        mMediumDao.deleteOldRecycleBinItems(System.currentTimeMillis() - MONTH_MILLISECONDS)
                    } catch (e: Exception) {
                    }
                }
            }, 3000L)
        }
    }

    // exclude probably unwanted folders, for example facebook stickers are split between hundreds of separate folders like
    // /storage/emulated/0/Android/data/com.facebook.orca/files/stickers/175139712676531/209575122566323
    // /storage/emulated/0/Android/data/com.facebook.orca/files/stickers/497837993632037/499671223448714
    private fun excludeSpamFolders() {
        ensureBackgroundThread {
            try {
                val internalPath = internalStoragePath
                val checkedPaths = ArrayList<String>()
                val oftenRepeatedPaths = ArrayList<String>()
                val paths = mDirs.map { it.path.removePrefix(internalPath) }.toMutableList() as ArrayList<String>
                paths.forEach {
                    val parts = it.split("/")
                    var currentString = ""
                    for (i in 0 until parts.size) {
                        currentString += "${parts[i]}/"

                        if (!checkedPaths.contains(currentString)) {
                            val cnt = paths.count { it.startsWith(currentString) }
                            if (cnt > 50 && currentString.startsWith("/Android/data", true)) {
                                oftenRepeatedPaths.add(currentString)
                            }
                        }

                        checkedPaths.add(currentString)
                    }
                }

                val substringToRemove = oftenRepeatedPaths.filter {
                    val path = it
                    it == "/" || oftenRepeatedPaths.any { it != path && it.startsWith(path) }
                }

                oftenRepeatedPaths.removeAll(substringToRemove)
                oftenRepeatedPaths.forEach {
                    val file = File("$internalPath/$it")
                    if (file.exists()) {
                        config.addExcludedFolder(file.absolutePath)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    override fun refreshItems() {
        getDirectories()
    }

    override fun recheckPinnedFolders() {
        ensureBackgroundThread {
            gotDirectories(movePinnedDirectoriesToFront(getCurrentlyDisplayedDirs()))
        }
    }

    override fun updateDirectories(directories: ArrayList<Directory>) {
        ensureBackgroundThread {
            storeDirectoryItems(directories, mDirectoryDao)
            removeInvalidDBDirectories()
        }
    }

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(213, R.string.release_213))
            add(Release(217, R.string.release_217))
            add(Release(220, R.string.release_220))
            add(Release(221, R.string.release_221))
            add(Release(225, R.string.release_225))
            add(Release(258, R.string.release_258))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
