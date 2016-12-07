package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.bumptech.glide.Glide
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.simplemobiletools.filepicker.extensions.*
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.adapters.MediaAdapter
import com.simplemobiletools.gallery.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.dialogs.ChangeSortingDialog
import com.simplemobiletools.gallery.extensions.getHumanizedFilename
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.activity_media.*
import java.io.File
import java.io.IOException
import java.util.*

class MediaActivity : SimpleActivity(), MediaAdapter.MediaOperationsListener {
    companion object {
        private val TAG = MediaActivity::class.java.simpleName

        private var mMedia = ArrayList<Medium>()

        private var mPath = ""
        private var mIsGetImageIntent = false
        private var mIsGetVideoIntent = false
        private var mIsGetAnyIntent = false
        private var mIsGettingMedia = false
        private var mShowAll = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)
        intent.apply {
            mIsGetImageIntent = getBooleanExtra(GET_IMAGE_INTENT, false)
            mIsGetVideoIntent = getBooleanExtra(GET_VIDEO_INTENT, false)
            mIsGetAnyIntent = getBooleanExtra(GET_ANY_INTENT, false)
        }

        media_holder.setOnRefreshListener({ getMedia() })
        mPath = intent.getStringExtra(DIRECTORY)
        mMedia = ArrayList<Medium>()
        mShowAll = mConfig.showAll
        if (mShowAll)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onResume() {
        super.onResume()
        tryloadGallery()
    }

    private fun tryloadGallery() {
        if (hasStoragePermission()) {
            val dirName = getHumanizedFilename(mPath)
            title = if (mShowAll) resources.getString(R.string.all_media) else dirName
            getMedia()
        } else {
            finish()
        }
    }

    private fun initializeGallery() {
        if (isDirEmpty())
            return

        val adapter = MediaAdapter(this, mMedia, this) {
            itemClicked(it.path)
        }

        val currAdapter = media_grid.adapter
        if (currAdapter != null) {
            (currAdapter as MediaAdapter).updateMedia(mMedia)
        } else {
            media_grid.adapter = adapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_media, menu)

        val isFolderHidden = mConfig.getIsFolderHidden(mPath)
        menu.findItem(R.id.hide_folder).isVisible = !isFolderHidden && !mShowAll
        menu.findItem(R.id.unhide_folder).isVisible = isFolderHidden && !mShowAll

        menu.findItem(R.id.folder_view).isVisible = mShowAll
        menu.findItem(R.id.open_camera).isVisible = mShowAll
        menu.findItem(R.id.settings).isVisible = mShowAll
        menu.findItem(R.id.about).isVisible = mShowAll

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sort -> {
                showSortingDialog()
                true
            }
            R.id.toggle_filename -> {
                toggleFilenameVisibility()
                true
            }
            R.id.open_camera -> {
                launchCamera()
                true
            }
            R.id.folder_view -> {
                switchToFolderView()
                true
            }
            R.id.hide_folder -> {
                hideFolder()
                true
            }
            R.id.unhide_folder -> {
                unhideFolder()
                true
            }
            R.id.settings -> {
                launchSettings()
                true
            }
            R.id.about -> {
                launchAbout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleFilenameVisibility() {
        mConfig.displayFileNames = !mConfig.displayFileNames
        if (media_grid.adapter != null)
            (media_grid.adapter as MediaAdapter).updateDisplayFilenames(mConfig.displayFileNames)
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, false) {
            getMedia()
        }
    }

    private fun switchToFolderView() {
        mConfig.showAll = false
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun hideFolder() {
        mConfig.addHiddenFolder(mPath)

        if (!mConfig.showHiddenFolders)
            finish()
        else
            invalidateOptionsMenu()
    }

    private fun unhideFolder() {
        mConfig.removeHiddenFolder(mPath)
        invalidateOptionsMenu()
    }

    private fun deleteDirectoryIfEmpty() {
        val file = File(mPath)
        if (file.isDirectory && file.listFiles().isEmpty()) {
            file.delete()
        }
    }

    private fun getMedia() {
        if (mIsGettingMedia)
            return

        mIsGettingMedia = true
        GetMediaAsynctask(applicationContext, mPath, mIsGetVideoIntent, mIsGetImageIntent, mShowAll) {
            gotMedia(it)
        }.execute()
    }

    private fun isDirEmpty(): Boolean {
        return if (mMedia.size <= 0) {
            deleteDirectoryIfEmpty()
            finish()
            true
        } else
            false
    }

    override fun deleteFiles(files: ArrayList<File>) {
        for (file in files) {
            if (file.exists() && file.isImageVideoGif()) {
                if (needsStupidWritePermissions(file.absolutePath)) {
                    if (isShowingPermDialog(file))
                        return

                    val document = getFileDocument(file.absolutePath, mConfig.treeUri)

                    // double check we have the uri to the proper file path, not some parent folder
                    if (document.uri.toString().endsWith(file.absolutePath.getFilenameFromPath()) && !document.isDirectory) {
                        document.delete()
                    }
                } else {
                    file.delete()
                }
            }
        }

        scanFiles(files) {
            if (mMedia.isEmpty()) {
                finish()
            }
            updateMediaView()
        }
    }

    private fun updateMediaView() {
        if (!isDirEmpty()) {
            getMedia()
        }
    }

    private fun isSetWallpaperIntent() = intent.getBooleanExtra(SET_WALLPAPER_INTENT, false)

    fun itemClicked(path: String) {
        if (isSetWallpaperIntent()) {
            toast(R.string.setting_wallpaper)

            val wantedWidth = wallpaperDesiredMinimumWidth
            val wantedHeight = wallpaperDesiredMinimumHeight
            val ratio = wantedWidth.toFloat() / wantedHeight
            Glide.with(this)
                    .load(File(path))
                    .asBitmap()
                    .override((wantedWidth * ratio).toInt(), wantedHeight)
                    .fitCenter()
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(bitmap: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
                            try {
                                WallpaperManager.getInstance(applicationContext).setBitmap(bitmap)
                                setResult(Activity.RESULT_OK)
                            } catch (e: IOException) {
                                Log.e(TAG, "item click $e")
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
            Intent(this, ViewPagerActivity::class.java).apply {
                putExtra(MEDIUM, path)
                putExtra(SHOW_ALL, mShowAll)
                startActivity(this)
            }
        }
    }

    fun gotMedia(media: ArrayList<Medium>) {
        mIsGettingMedia = false
        media_holder.isRefreshing = false
        if (media.toString() == mMedia.toString()) {
            return
        }

        mMedia = media
        initializeGallery()
    }

    override fun refreshItems() {
        getMedia()
    }
}
