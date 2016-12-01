package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.simplemobiletools.filepicker.extensions.*
import com.simplemobiletools.gallery.Constants
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.adapters.MediaAdapter
import com.simplemobiletools.gallery.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.dialogs.ChangeSortingDialog
import com.simplemobiletools.gallery.extensions.getHumanizedFilename
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.activity_media.*
import java.io.File
import java.io.IOException
import java.util.*

class MediaActivity : SimpleActivity(), MediaAdapter.MediaOperationsListener {
    companion object {
        private val TAG = MediaActivity::class.java.simpleName

        private var mSnackbar: Snackbar? = null

        lateinit var mToBeDeleted: ArrayList<String>
        lateinit var mMedia: ArrayList<Medium>

        private var mPath = ""
        private var mIsGetImageIntent = false
        private var mIsGetVideoIntent = false
        private var mIsGetAnyIntent = false
        private var mIsGettingMedia = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)
        intent.apply {
            mIsGetImageIntent = getBooleanExtra(Constants.GET_IMAGE_INTENT, false)
            mIsGetVideoIntent = getBooleanExtra(Constants.GET_VIDEO_INTENT, false)
            mIsGetAnyIntent = getBooleanExtra(Constants.GET_ANY_INTENT, false)
        }

        media_holder.setOnRefreshListener({ getMedia() })
        mPath = intent.getStringExtra(Constants.DIRECTORY)
        mToBeDeleted = ArrayList<String>()
        mMedia = ArrayList<Medium>()
    }

    override fun onResume() {
        super.onResume()
        tryloadGallery()
    }

    override fun onPause() {
        super.onPause()
        deleteFiles()
    }

    private fun tryloadGallery() {
        if (hasStoragePermission()) {
            val dirName = getHumanizedFilename(mPath)
            title = dirName
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

        media_grid.adapter = adapter
        media_grid.setOnTouchListener { view, motionEvent -> checkDelete(); false }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_media, menu)

        val isFolderHidden = mConfig.getIsFolderHidden(mPath)
        menu.findItem(R.id.hide_folder).isVisible = !isFolderHidden
        menu.findItem(R.id.unhide_folder).isVisible = isFolderHidden
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
            R.id.hide_folder -> {
                hideDirectory()
                true
            }
            R.id.unhide_folder -> {
                unhideDirectory()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleFilenameVisibility() {
        mConfig.displayFileNames = !mConfig.displayFileNames
        (media_grid.adapter as MediaAdapter).updateDisplayFilenames(mConfig.displayFileNames)
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, false) {
            getMedia()
        }
    }

    private fun hideDirectory() {
        mConfig.addHiddenDirectory(mPath)

        if (!mConfig.showHiddenFolders)
            finish()
        else
            invalidateOptionsMenu()
    }

    private fun unhideDirectory() {
        mConfig.removeHiddenDirectory(mPath)
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
        GetMediaAsynctask(applicationContext, mPath, mIsGetVideoIntent, mIsGetImageIntent, mToBeDeleted) {
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

    override fun prepareForDeleting(paths: ArrayList<String>) {
        toast(R.string.deleting)
        mToBeDeleted = paths
        val deletedCnt = mToBeDeleted.size

        if (isShowingPermDialog(File(mToBeDeleted[0])))
            return

        notifyDeletion(deletedCnt)
    }

    private fun notifyDeletion(cnt: Int) {
        getMedia()

        if (mMedia.isEmpty()) {
            deleteFiles()
        } else {
            val res = resources
            val msg = res.getQuantityString(R.plurals.files_deleted, cnt, cnt)
            mSnackbar = Snackbar.make(coordinator_layout, msg, Snackbar.LENGTH_INDEFINITE)
            mSnackbar!!.apply {
                setAction(res.getString(R.string.undo), undoDeletion)
                setActionTextColor(Color.WHITE)
                show()
            }
            updateMediaView()
        }
    }

    private fun deleteFiles() {
        if (mToBeDeleted.isEmpty())
            return

        mSnackbar?.dismiss()
        var wereFilesDeleted = false

        for (delPath in mToBeDeleted) {
            val file = File(delPath)
            if (file.exists() && file.isImageVideoGif()) {
                if (needsStupidWritePermissions(file.absolutePath)) {
                    if (isShowingPermDialog(file))
                        return

                    val document = getFileDocument(file.absolutePath, mConfig.treeUri)

                    // double check we have the uri to the proper file path, not some parent folder
                    if (document.uri.toString().endsWith(file.absolutePath.getFilenameFromPath()) && !document.isDirectory) {
                        if (document.delete()) {
                            wereFilesDeleted = true
                        }
                    }
                } else {
                    if (file.delete())
                        wereFilesDeleted = true
                }

                if (file.exists()) {
                    try {
                        file.delete()
                    } catch (ignored: Exception) {
                    }
                }
            }
        }

        if (wereFilesDeleted) {
            scanPaths(mToBeDeleted) {
                if (mMedia.isEmpty()) {
                    finish()
                }
            }
        }
        mToBeDeleted.clear()
    }

    private val undoDeletion = View.OnClickListener {
        mSnackbar!!.dismiss()
        mToBeDeleted.clear()
        updateMediaView()
    }

    private fun updateMediaView() {
        if (!isDirEmpty()) {
            getMedia()
        }
    }

    private fun isSetWallpaperIntent() = intent.getBooleanExtra(Constants.SET_WALLPAPER_INTENT, false)

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
                putExtra(Constants.MEDIUM, path)
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

    fun checkDelete() {
        if (mSnackbar?.isShown == true) {
            deleteFiles()
        }
    }

    override fun refreshItems() {
        getMedia()
    }
}
