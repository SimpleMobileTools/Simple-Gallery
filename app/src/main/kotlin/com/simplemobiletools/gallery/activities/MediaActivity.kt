package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.simplemobiletools.filepicker.asynctasks.CopyMoveTask
import com.simplemobiletools.filepicker.extensions.scanPath
import com.simplemobiletools.fileproperties.dialogs.PropertiesDialog
import com.simplemobiletools.gallery.Constants
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.Utils
import com.simplemobiletools.gallery.adapters.MediaAdapter
import com.simplemobiletools.gallery.dialogs.ChangeSortingDialog
import com.simplemobiletools.gallery.dialogs.CopyDialog
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.activity_media.*
import java.io.File
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

class MediaActivity : SimpleActivity(), AdapterView.OnItemClickListener/*, GridView.MultiChoiceModeListener, GridView.OnTouchListener*/, SwipeRefreshLayout.OnRefreshListener {
    companion object {
        private val TAG = MediaActivity::class.java.simpleName

        private var mSnackbar: Snackbar? = null

        lateinit var mToBeDeleted: MutableList<String>
        lateinit var mMedia: MutableList<Medium>

        private var mPath = ""
        private var mIsSnackbarShown = false
        private var mIsGetImageIntent = false
        private var mIsGetVideoIntent = false
        private var mIsGetAnyIntent = false
        private var mSelectedItemsCnt = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)
        mIsGetImageIntent = intent.getBooleanExtra(Constants.GET_IMAGE_INTENT, false)
        mIsGetVideoIntent = intent.getBooleanExtra(Constants.GET_VIDEO_INTENT, false)
        mIsGetAnyIntent = intent.getBooleanExtra(Constants.GET_ANY_INTENT, false)

        media_holder.setOnRefreshListener(this)
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
        if (Utils.hasStoragePermission(applicationContext)) {
            initializeGallery()
        } else {
            finish()
        }
    }

    private fun initializeGallery() {
        val newMedia = getMedia()
        if (newMedia.toString() == mMedia.toString()) {
            return
        }

        mMedia = newMedia
        if (isDirEmpty)
            return

        val adapter = MediaAdapter(this, mMedia)
        media_grid.adapter = adapter
        media_grid.onItemClickListener = this
        /*mGridView!!.setMultiChoiceModeListener(this)
        mGridView!!.setOnTouchListener(this)*/
        mIsSnackbarShown = false

        val dirName = Utils.getFilename(this, mPath)
        title = dirName
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
        ChangeSortingDialog(this, false, object : ChangeSortingDialog.OnChangeSortingListener {
            override fun sortingChanged() {
                initializeGallery()
            }
        })
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

    private fun getMedia(): MutableList<Medium> {
        val media = ArrayList<Medium>()
        val invalidFiles = ArrayList<File>()
        for (i in 0..1) {
            if (mIsGetVideoIntent && i == 0)
                continue

            var uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            if (i == 1) {
                if (mIsGetImageIntent)
                    continue

                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val where = "${MediaStore.Images.Media.DATA} like ? "
            val args = arrayOf("$mPath%")
            val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_MODIFIED)
            val cursor = contentResolver.query(uri, columns, where, args, null)
            val pattern = "${Pattern.quote(mPath)}/[^/]*"

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    do {
                        val curPath = cursor.getString(pathIndex) ?: continue

                        if (curPath.matches(pattern.toRegex()) && !mToBeDeleted.contains(curPath)) {
                            val file = File(curPath)
                            if (file.exists()) {
                                val dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                                val timestamp = cursor.getLong(dateIndex)
                                media.add(Medium(file.name, curPath, i == 1, timestamp, file.length()))
                            } else {
                                invalidFiles.add(file)
                            }
                        }
                    } while (cursor.moveToNext())
                }
                cursor.close()
            }
        }

        Medium.sorting = mConfig.sorting
        Collections.sort(media)
        Utils.scanFiles(applicationContext, invalidFiles)

        return media
    }

    private val isDirEmpty: Boolean
        get() {
            if (mMedia.size <= 0) {
                deleteDirectoryIfEmpty()
                finish()
                return true
            }
            return false
        }

    private fun shareMedia() {
        val selectedMedia = selectedMedia
        if (selectedMedia.size <= 1) {
            Utils.shareMedium(selectedMedia[0], this)
        } else {
            shareMedia(selectedMedia)
        }
    }

    private fun shareMedia(media: List<Medium>) {
        val shareTitle = resources.getString(R.string.share_via)
        val intent = Intent()
        intent.action = Intent.ACTION_SEND_MULTIPLE
        intent.type = "image/* video/*"
        val uris = ArrayList<Uri>(media.size)
        for (medium in media) {
            val file = File(medium.path)
            uris.add(Uri.fromFile(file))
        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        startActivity(Intent.createChooser(intent, shareTitle))
    }

    private val selectedMedia: List<Medium>
        get() {
            val media = ArrayList<Medium>()
            val items = media_grid.checkedItemPositions
            val cnt = items.size()
            for (i in 0..cnt - 1) {
                if (items.valueAt(i)) {
                    val id = items.keyAt(i)
                    media.add(mMedia[id])
                }
            }
            return media
        }

    private fun prepareForDeleting() {
        if (isShowingPermDialog(File(mPath)))
            return

        Utils.showToast(this, R.string.deleting)
        val items = media_grid.checkedItemPositions
        val cnt = items.size()
        var deletedCnt = 0
        for (i in 0..cnt - 1) {
            if (items.valueAt(i)) {
                val id = items.keyAt(i)
                val path = mMedia[id].path
                mToBeDeleted.add(path)
                deletedCnt++
            }
        }

        notifyDeletion(deletedCnt)
    }

    private fun notifyDeletion(cnt: Int) {
        mMedia = getMedia()

        if (mMedia.isEmpty()) {
            deleteFiles()
        } else {
            val coordinator = findViewById(R.id.coordinator_layout) as CoordinatorLayout
            val res = resources
            val msg = res.getQuantityString(R.plurals.files_deleted, cnt, cnt)
            mSnackbar = Snackbar.make(coordinator, msg, Snackbar.LENGTH_INDEFINITE)
            mSnackbar!!.apply {
                setAction(res.getString(R.string.undo), undoDeletion)
                setActionTextColor(Color.WHITE)
                show()
            }
            mIsSnackbarShown = true
            updateGridView()
        }
    }

    private fun deleteFiles() {
        if (mToBeDeleted.isEmpty())
            return

        if (mSnackbar != null) {
            mSnackbar!!.dismiss()
        }

        mIsSnackbarShown = false
        var wereFilesDeleted = false

        for (delPath in mToBeDeleted) {
            val file = File(delPath)
            if (file.exists()) {
                if (Utils.needsStupidWritePermissions(this, delPath)) {
                    if (isShowingPermDialog(file))
                        return

                    val document = Utils.getFileDocument(this, delPath, mConfig.treeUri)
                    if (document.delete()) {
                        wereFilesDeleted = true
                    }
                } else {
                    if (file.delete())
                        wereFilesDeleted = true
                }
            }
        }

        if (wereFilesDeleted) {
            val deletedPaths = mToBeDeleted.toTypedArray()
            MediaScannerConnection.scanFile(applicationContext, deletedPaths, null) { path, uri ->
                if (mMedia.isEmpty()) {
                    finish()
                }
            }
            mToBeDeleted.clear()
        }
    }

    private val undoDeletion = View.OnClickListener {
        mSnackbar!!.dismiss()
        mIsSnackbarShown = false
        mToBeDeleted.clear()
        mMedia = getMedia()
        updateGridView()
    }

    private fun updateGridView() {
        if (!isDirEmpty) {
            val adapter = media_grid.adapter as MediaAdapter
            adapter.updateItems(mMedia)
        }
    }

    private fun showProperties() {
        val selectedMedia = selectedMedia
        if (selectedMedia.size == 1) {
            PropertiesDialog(this, selectedMedia[0].path, false)
        } else {
            val paths = ArrayList<String>(selectedMedia.size)
            for (medium in selectedMedia) {
                paths.add(medium.path)
            }
            PropertiesDialog(this, paths, false)
        }
    }

    private val isSetWallpaperIntent: Boolean
        get() = intent.getBooleanExtra(Constants.SET_WALLPAPER_INTENT, false)

    private fun displayCopyDialog() {
        val files = ArrayList<File>()

        val items = media_grid.checkedItemPositions
        val cnt = items.size()
        for (i in 0..cnt - 1) {
            if (items.valueAt(i)) {
                val id = items.keyAt(i)
                files.add(File(mMedia[id].path))
            }
        }

        CopyDialog(this, files, object : CopyMoveTask.CopyMoveListener {
            override fun copySucceeded(deleted: Boolean, copiedAll: Boolean) {
                val msgId: Int
                if (deleted) {
                    refreshDir()
                    msgId = if (copiedAll) R.string.moving_success else R.string.moving_success_partial
                } else {
                    msgId = if (copiedAll) R.string.copying_success else R.string.copying_success_partial
                }
                Utils.showToast(applicationContext, msgId)
            }

            override fun copyFailed() {
                Utils.showToast(applicationContext, R.string.copy_move_failed)
            }
        })
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val curItemPath = mMedia[position].path
        if (isSetWallpaperIntent) {
            Utils.showToast(this, R.string.setting_wallpaper)

            val wantedWidth = wallpaperDesiredMinimumWidth
            val wantedHeight = wallpaperDesiredMinimumHeight
            val ratio = wantedWidth.toFloat() / wantedHeight
            Glide.with(this).load(File(curItemPath)).asBitmap().override((wantedWidth * ratio).toInt(), wantedHeight).fitCenter().into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(bitmap: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
                    try {
                        WallpaperManager.getInstance(applicationContext).setBitmap(bitmap)
                        setResult(Activity.RESULT_OK)
                    } catch (e: IOException) {
                        Log.e(TAG, "item click " + e.message)
                    }

                    finish()
                }
            })
        } else if (mIsGetImageIntent || mIsGetVideoIntent || mIsGetAnyIntent) {
            val result = Intent()
            result.data = Uri.parse(curItemPath)
            setResult(Activity.RESULT_OK, result)
            finish()
        } else {
            val intent = Intent(this, ViewPagerActivity::class.java)
            intent.putExtra(Constants.MEDIUM, curItemPath)
            startActivity(intent)
        }
    }

    /*override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
        if (checked) {
            mSelectedItemsCnt++
        } else {
            mSelectedItemsCnt--
        }

        if (mSelectedItemsCnt > 0)
            mode.title = mSelectedItemsCnt.toString()

        mode.invalidate()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.cab_media, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.cab_properties -> {
                showProperties()
                return true
            }
            R.id.cab_share -> {
                shareMedia()
                return true
            }
            R.id.cab_delete -> {
                prepareForDeleting()
                mode.finish()
                return true
            }
            R.id.cab_copy_move -> {
                displayCopyDialog()
                return true
            }
            else -> return false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        mSelectedItemsCnt = 0
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (mIsSnackbarShown) {
            deleteFiles()
        }

        return false
    }*/

    override fun onRefresh() {
        refreshDir()
    }

    private fun refreshDir() {
        val dir = File(mPath)
        if (dir.isDirectory) {
            scanPath(mPath) {}
        }
        initializeGallery()
        media_holder.isRefreshing = false
    }
}
