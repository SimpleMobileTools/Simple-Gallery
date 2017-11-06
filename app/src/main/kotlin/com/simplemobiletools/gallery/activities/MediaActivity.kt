package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.REQUEST_EDIT_IMAGE
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.views.MyScalableRecyclerView
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.adapters.MediaAdapter
import com.simplemobiletools.gallery.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.dialogs.ChangeSortingDialog
import com.simplemobiletools.gallery.dialogs.ExcludeFolderDialog
import com.simplemobiletools.gallery.dialogs.FilterMediaDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.activity_media.*
import java.io.File
import java.io.IOException

class MediaActivity : SimpleActivity(), MediaAdapter.MediaOperationsListener {
    private val SAVE_MEDIA_CNT = 100
    private val LAST_MEDIA_CHECK_PERIOD = 3000L

    private var mPath = ""
    private var mIsGetImageIntent = false
    private var mIsGetVideoIntent = false
    private var mIsGetAnyIntent = false
    private var mIsGettingMedia = false
    private var mAllowPickingMultiple = false
    private var mShowAll = false
    private var mLoadedInitialPhotos = false
    private var mStoredAnimateGifs = true
    private var mStoredCropThumbnails = true
    private var mStoredScrollHorizontally = true
    private var mStoredTextColor = 0
    private var mLastDrawnHashCode = 0
    private var mLatestMediaId = 0L
    private var mLastMediaHandler = Handler()
    private var mCurrAsyncTask: GetMediaAsynctask? = null

    companion object {
        var mMedia = ArrayList<Medium>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)
        intent.apply {
            mIsGetImageIntent = getBooleanExtra(GET_IMAGE_INTENT, false)
            mIsGetVideoIntent = getBooleanExtra(GET_VIDEO_INTENT, false)
            mIsGetAnyIntent = getBooleanExtra(GET_ANY_INTENT, false)
            mAllowPickingMultiple = getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }

        media_refresh_layout.setOnRefreshListener({ getMedia() })
        mPath = intent.getStringExtra(DIRECTORY)
        storeStateVariables()
        if (mShowAll)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)

        media_empty_text.setOnClickListener {
            showFilterMediaDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mStoredAnimateGifs != config.animateGifs) {
            getMediaAdapter()?.updateAnimateGifs(config.animateGifs)
        }

        if (mStoredCropThumbnails != config.cropThumbnails) {
            getMediaAdapter()?.updateCropThumbnails(config.cropThumbnails)
        }

        if (mStoredScrollHorizontally != config.scrollHorizontally) {
            getMediaAdapter()?.updateScrollHorizontally(config.viewTypeFiles != VIEW_TYPE_LIST || !config.scrollHorizontally)
            setupScrollDirection()
        }

        if (mStoredTextColor != config.textColor) {
            getMediaAdapter()?.updateTextColor(config.textColor)
        }

        tryloadGallery()
        invalidateOptionsMenu()
        media_empty_text_label.setTextColor(config.textColor)
        media_empty_text.setTextColor(config.primaryColor)
    }

    override fun onPause() {
        super.onPause()
        mIsGettingMedia = false
        media_refresh_layout.isRefreshing = false
        storeStateVariables()
        media_grid.listener = null
        mLastMediaHandler.removeCallbacksAndMessages(null)

        if (!mMedia.isEmpty()) {
            mCurrAsyncTask?.stopFetching()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (config.showAll)
            config.temporarilyShowHidden = false
        mMedia.clear()
    }

    private fun storeStateVariables() {
        config.apply {
            mStoredAnimateGifs = animateGifs
            mStoredCropThumbnails = cropThumbnails
            mStoredScrollHorizontally = scrollHorizontally
            mStoredTextColor = textColor
            mShowAll = showAll
        }
    }

    private fun tryloadGallery() {
        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                val dirName = getHumanizedFilename(mPath)
                title = if (mShowAll) resources.getString(R.string.all_folders) else dirName
                getMedia()
                setupLayoutManager()
                checkIfColorChanged()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    private fun getMediaAdapter() = media_grid.adapter as? MediaAdapter

    private fun checkIfColorChanged() {
        if (media_grid.adapter != null && getRecyclerAdapter().primaryColor != config.primaryColor) {
            getRecyclerAdapter().primaryColor = config.primaryColor
            media_horizontal_fastscroller.updateHandleColor()
            media_vertical_fastscroller.updateHandleColor()
        }
    }

    private fun setupAdapter() {
        if (isDirEmpty())
            return

        val currAdapter = media_grid.adapter
        if (currAdapter == null) {
            media_grid.adapter = MediaAdapter(this, mMedia, this, mIsGetImageIntent || mIsGetVideoIntent || mIsGetAnyIntent, mAllowPickingMultiple) {
                itemClicked(it.path)
            }
        } else {
            (currAdapter as MediaAdapter).updateMedia(mMedia)
        }
        setupScrollDirection()
    }

    private fun setupScrollDirection() {
        val allowHorizontalScroll = config.scrollHorizontally && config.viewTypeFiles == VIEW_TYPE_GRID
        media_refresh_layout.isEnabled = !config.scrollHorizontally

        media_vertical_fastscroller.isHorizontal = false
        media_vertical_fastscroller.beGoneIf(allowHorizontalScroll)

        media_horizontal_fastscroller.isHorizontal = true
        media_horizontal_fastscroller.beVisibleIf(allowHorizontalScroll)

        if (allowHorizontalScroll) {
            media_horizontal_fastscroller.setViews(media_grid, media_refresh_layout)
        } else {
            media_vertical_fastscroller.setViews(media_grid, media_refresh_layout)
        }
    }

    private fun checkLastMediaChanged() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed)
            return

        mLastMediaHandler.removeCallbacksAndMessages(null)
        mLastMediaHandler.postDelayed({
            Thread({
                val mediaId = getLatestMediaId()
                if (mLatestMediaId != mediaId) {
                    mLatestMediaId = mediaId
                    runOnUiThread {
                        getMedia()
                    }
                } else {
                    checkLastMediaChanged()
                }
            }).start()
        }, LAST_MEDIA_CHECK_PERIOD)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_media, menu)

        val isFolderHidden = File(mPath).containsNoMedia()
        menu.apply {
            findItem(R.id.hide_folder).isVisible = !isFolderHidden && !mShowAll
            findItem(R.id.unhide_folder).isVisible = isFolderHidden && !mShowAll

            findItem(R.id.folder_view).isVisible = mShowAll
            findItem(R.id.open_camera).isVisible = mShowAll
            findItem(R.id.about).isVisible = mShowAll

            findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden
            findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden

            findItem(R.id.increase_column_count).isVisible = config.viewTypeFiles == VIEW_TYPE_GRID && config.mediaColumnCnt < MAX_COLUMN_COUNT
            findItem(R.id.reduce_column_count).isVisible = config.viewTypeFiles == VIEW_TYPE_GRID && config.mediaColumnCnt > 1

            findItem(R.id.toggle_filename).isVisible = config.viewTypeFiles == VIEW_TYPE_GRID
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.filter -> showFilterMediaDialog()
            R.id.toggle_filename -> toggleFilenameVisibility()
            R.id.open_camera -> launchCamera()
            R.id.folder_view -> switchToFolderView()
            R.id.change_view_type -> changeViewType()
            R.id.hide_folder -> tryHideFolder()
            R.id.unhide_folder -> unhideFolder()
            R.id.exclude_folder -> tryExcludeFolder()
            R.id.temporarily_show_hidden -> tryToggleTemporarilyShowHidden()
            R.id.stop_showing_hidden -> tryToggleTemporarilyShowHidden()
            R.id.increase_column_count -> increaseColumnCount()
            R.id.reduce_column_count -> reduceColumnCount()
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, false, !config.showAll, mPath) {
            getMedia()
        }
    }

    private fun showFilterMediaDialog() {
        FilterMediaDialog(this) {
            media_refresh_layout.isRefreshing = true
            getMedia()
        }
    }

    private fun toggleFilenameVisibility() {
        config.displayFileNames = !config.displayFileNames
        if (media_grid.adapter != null)
            getRecyclerAdapter().updateDisplayFilenames(config.displayFileNames)
    }

    private fun switchToFolderView() {
        config.showAll = false
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun changeViewType() {
        val items = arrayListOf(
                RadioItem(VIEW_TYPE_GRID, getString(R.string.grid)),
                RadioItem(VIEW_TYPE_LIST, getString(R.string.list)))

        RadioGroupDialog(this, items, config.viewTypeFiles) {
            config.viewTypeFiles = it as Int
            invalidateOptionsMenu()
            setupLayoutManager()
            media_grid.adapter = null
            setupAdapter()
        }
    }

    private fun tryHideFolder() {
        if (config.wasHideFolderTooltipShown) {
            hideFolder()
        } else {
            ConfirmationDialog(this, getString(R.string.hide_folder_description)) {
                config.wasHideFolderTooltipShown = true
                hideFolder()
            }
        }
    }

    private fun hideFolder() {
        addNoMedia(mPath) {
            runOnUiThread {
                if (!config.shouldShowHidden)
                    finish()
                else
                    invalidateOptionsMenu()
            }
        }
    }

    private fun unhideFolder() {
        removeNoMedia(mPath) {
            runOnUiThread {
                invalidateOptionsMenu()
            }
        }
    }

    private fun tryExcludeFolder() {
        ExcludeFolderDialog(this, arrayListOf(mPath)) {
            finish()
        }
    }

    private fun deleteDirectoryIfEmpty() {
        val file = File(mPath)
        if (config.deleteEmptyFolders && !file.isDownloadsFolder() && file.isDirectory && file.listFiles()?.isEmpty() == true) {
            deleteFile(file, true) {}
        }
    }

    private fun getMedia() {
        if (mIsGettingMedia)
            return

        mIsGettingMedia = true
        val media = getCachedMedia(mPath)
        if (media.isNotEmpty() && !mLoadedInitialPhotos) {
            gotMedia(media, true)
        } else {
            media_refresh_layout.isRefreshing = true
        }

        mLoadedInitialPhotos = true
        mCurrAsyncTask = GetMediaAsynctask(applicationContext, mPath, mIsGetVideoIntent, mIsGetImageIntent, mShowAll) {
            gotMedia(it)
        }
        mCurrAsyncTask!!.execute()
    }

    private fun isDirEmpty(): Boolean {
        return if (mMedia.size <= 0 && config.filterMedia > 0) {
            deleteDirectoryIfEmpty()
            finish()
            true
        } else
            false
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
        config.temporarilyShowHidden = show
        getMedia()
        invalidateOptionsMenu()
    }

    private fun getRecyclerAdapter() = (media_grid.adapter as MediaAdapter)

    private fun setupLayoutManager() {
        if (config.viewTypeFiles == VIEW_TYPE_GRID)
            setupGridLayoutManager()
        else
            setupListLayoutManager()
    }

    private fun setupGridLayoutManager() {
        val layoutManager = media_grid.layoutManager as GridLayoutManager
        if (config.scrollHorizontally) {
            layoutManager.orientation = GridLayoutManager.HORIZONTAL
            media_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            layoutManager.orientation = GridLayoutManager.VERTICAL
            media_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        media_grid.isDragSelectionEnabled = true
        media_grid.isZoomingEnabled = true
        layoutManager.spanCount = config.mediaColumnCnt
        media_grid.listener = object : MyScalableRecyclerView.MyScalableRecyclerViewListener {
            override fun zoomIn() {
                if (layoutManager.spanCount > 1) {
                    reduceColumnCount()
                    getRecyclerAdapter().actMode?.finish()
                }
            }

            override fun zoomOut() {
                if (layoutManager.spanCount < MAX_COLUMN_COUNT) {
                    increaseColumnCount()
                    getRecyclerAdapter().actMode?.finish()
                }
            }

            override fun selectItem(position: Int) {
                getRecyclerAdapter().selectItem(position)
            }

            override fun selectRange(initialSelection: Int, lastDraggedIndex: Int, minReached: Int, maxReached: Int) {
                getRecyclerAdapter().selectRange(initialSelection, lastDraggedIndex, minReached, maxReached)
            }
        }
    }

    private fun setupListLayoutManager() {
        media_grid.isDragSelectionEnabled = true
        media_grid.isZoomingEnabled = false

        val layoutManager = media_grid.layoutManager as GridLayoutManager
        layoutManager.spanCount = 1
        layoutManager.orientation = GridLayoutManager.VERTICAL
        media_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun increaseColumnCount() {
        config.mediaColumnCnt = ++(media_grid.layoutManager as GridLayoutManager).spanCount
        invalidateOptionsMenu()
        media_grid.adapter?.notifyDataSetChanged()
    }

    private fun reduceColumnCount() {
        config.mediaColumnCnt = --(media_grid.layoutManager as GridLayoutManager).spanCount
        invalidateOptionsMenu()
        media_grid.adapter?.notifyDataSetChanged()
    }

    private fun isSetWallpaperIntent() = intent.getBooleanExtra(SET_WALLPAPER_INTENT, false)

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == REQUEST_EDIT_IMAGE) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                mMedia.clear()
                refreshItems()
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun itemClicked(path: String) {
        if (isSetWallpaperIntent()) {
            toast(R.string.setting_wallpaper)

            val wantedWidth = wallpaperDesiredMinimumWidth
            val wantedHeight = wallpaperDesiredMinimumHeight
            val ratio = wantedWidth.toFloat() / wantedHeight

            val options = RequestOptions()
                    .override((wantedWidth * ratio).toInt(), wantedHeight)
                    .fitCenter()

            Glide.with(this)
                    .asBitmap()
                    .load(File(path))
                    .apply(options)
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                            try {
                                WallpaperManager.getInstance(applicationContext).setBitmap(resource)
                                setResult(Activity.RESULT_OK)
                            } catch (ignored: IOException) {

                            }

                            finish()
                        }
                    })
        } else if (mIsGetImageIntent || mIsGetVideoIntent || mIsGetAnyIntent) {
            Intent().apply {
                data = Uri.parse(path)
                setResult(Activity.RESULT_OK, this)
            }
            finish()
        } else {
            val file = File(path)
            val isVideo = file.isVideoFast()
            if (isVideo) {
                openFile(Uri.fromFile(file), false)
            } else {
                Intent(this, ViewPagerActivity::class.java).apply {
                    putExtra(PATH, path)
                    putExtra(SHOW_ALL, mShowAll)
                    startActivity(this)
                }
            }
        }
    }

    private fun gotMedia(media: ArrayList<Medium>, isFromCache: Boolean = false) {
        mLatestMediaId = getLatestMediaId()
        mIsGettingMedia = false
        media_refresh_layout.isRefreshing = false

        media_empty_text_label.beVisibleIf(media.isEmpty() && !isFromCache)
        media_empty_text.beVisibleIf(media.isEmpty() && !isFromCache)

        checkLastMediaChanged()
        if (mLastDrawnHashCode == 0)
            mLastDrawnHashCode = media.hashCode()

        if (media.hashCode() == mMedia.hashCode() && media.hashCode() == mLastDrawnHashCode)
            return

        mLastDrawnHashCode = media.hashCode()
        mMedia = media
        runOnUiThread {
            setupAdapter()
        }
        storeFolder()
    }

    private fun storeFolder() {
        if (!config.temporarilyShowHidden) {
            val subList = mMedia.subList(0, Math.min(SAVE_MEDIA_CNT, mMedia.size))
            val json = Gson().toJson(subList)
            config.saveFolderMedia(mPath, json)
        }
    }

    override fun deleteFiles(files: ArrayList<File>) {
        val filtered = files.filter { it.isImageVideoGif() } as ArrayList
        deleteFiles(filtered) {
            if (!it) {
                toast(R.string.unknown_error_occurred)
            } else if (mMedia.isEmpty()) {
                deleteDirectoryIfEmpty()
                finish()
            }
        }
    }

    override fun refreshItems() {
        getMedia()
        Handler().postDelayed({
            getMedia()
        }, 1000)
    }

    override fun itemLongClicked(position: Int) {
        media_grid.setDragSelectActive(position)
    }

    override fun selectedPaths(paths: ArrayList<String>) {
        Intent().apply {
            putExtra(PICKED_PATHS, paths)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }
}
