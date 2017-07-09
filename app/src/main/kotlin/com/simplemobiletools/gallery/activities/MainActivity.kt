package com.simplemobiletools.gallery.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.gson.Gson
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.commons.views.MyScalableRecyclerView
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.dialogs.ChangeSortingDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.util.*

class MainActivity : SimpleActivity(), DirectoryAdapter.DirOperationsListener {
    private val STORAGE_PERMISSION = 1
    private val PICK_MEDIA = 2
    private val PICK_WALLPAPER = 3
    private val LAST_MEDIA_CHECK_PERIOD = 3000L

    lateinit var mDirs: ArrayList<Directory>

    private var mIsPickImageIntent = false
    private var mIsPickVideoIntent = false
    private var mIsGetImageContentIntent = false
    private var mIsGetVideoContentIntent = false
    private var mIsGetAnyContentIntent = false
    private var mIsSetWallpaperIntent = false
    private var mIsThirdPartyIntent = false
    private var mIsGettingDirs = false
    private var mStoredAnimateGifs = true
    private var mStoredCropThumbnails = true
    private var mStoredScrollHorizontally = true
    private var mLoadedInitialPhotos = false
    private var mLastMediaModified = 0
    private var mLastMediaHandler = Handler()

    private var mCurrAsyncTask: GetDirectoriesAsynctask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mIsPickImageIntent = isPickImageIntent(intent)
        mIsPickVideoIntent = isPickVideoIntent(intent)
        mIsGetImageContentIntent = isGetImageContentIntent(intent)
        mIsGetVideoContentIntent = isGetVideoContentIntent(intent)
        mIsGetAnyContentIntent = isGetAnyContentIntent(intent)
        mIsSetWallpaperIntent = isSetWallpaperIntent(intent)
        mIsThirdPartyIntent = mIsPickImageIntent || mIsPickVideoIntent || mIsGetImageContentIntent || mIsGetVideoContentIntent ||
                mIsGetAnyContentIntent || mIsSetWallpaperIntent

        directories_refresh_layout.setOnRefreshListener({ getDirectories() })
        mDirs = ArrayList<Directory>()
        mStoredAnimateGifs = config.animateGifs
        mStoredCropThumbnails = config.cropThumbnails
        mStoredScrollHorizontally = config.scrollHorizontally
        storeStoragePaths()
        checkWhatsNewDialog()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (mIsThirdPartyIntent) {
            menuInflater.inflate(R.menu.menu_main_intent, menu)
        } else {
            menuInflater.inflate(R.menu.menu_main, menu)
            menu.findItem(R.id.increase_column_count).isVisible = config.dirColumnCnt < 10
            menu.findItem(R.id.reduce_column_count).isVisible = config.dirColumnCnt > 1
        }
        menu.findItem(R.id.temporarily_show_hidden).isVisible = !config.shouldShowHidden
        menu.findItem(R.id.stop_showing_hidden).isVisible = config.temporarilyShowHidden
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.open_camera -> launchCamera()
            R.id.show_all -> showAllMedia()
            R.id.temporarily_show_hidden -> toggleTemporarilyShowHidden(true)
            R.id.stop_showing_hidden -> toggleTemporarilyShowHidden(false)
            R.id.increase_column_count -> increaseColumnCount()
            R.id.reduce_column_count -> reduceColumnCount()
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        if (mStoredAnimateGifs != config.animateGifs) {
            directories_grid.adapter?.notifyDataSetChanged()
        }

        if (mStoredCropThumbnails != config.cropThumbnails) {
            directories_grid.adapter?.notifyDataSetChanged()
        }

        if (mStoredScrollHorizontally != config.scrollHorizontally) {
            directories_grid.adapter?.let {
                (it as DirectoryAdapter).scrollVertically = !config.scrollHorizontally
                it.notifyDataSetChanged()
            }
            setupScrollDirection()
        }

        tryloadGallery()
        invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        mCurrAsyncTask?.shouldStop = true
        storeDirectories()
        directories_refresh_layout.isRefreshing = false
        mIsGettingDirs = false
        mStoredAnimateGifs = config.animateGifs
        mStoredCropThumbnails = config.cropThumbnails
        mStoredScrollHorizontally = config.scrollHorizontally
        directories_grid.listener = null
        mLastMediaHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        config.temporarilyShowHidden = false
    }

    private fun tryloadGallery() {
        if (hasWriteStoragePermission()) {
            if (config.showAll)
                showAllMedia()
            else
                getDirectories()
            setupLayoutManager()
            checkIfColorChanged()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getDirectories()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    private fun getDirectories() {
        if (mIsGettingDirs)
            return

        mIsGettingDirs = true
        val dirs = getCachedDirectories()
        if (dirs.isNotEmpty() && !mLoadedInitialPhotos) {
            gotDirectories(dirs)
        }

        if (!mLoadedInitialPhotos) {
            directories_refresh_layout.isRefreshing = true
        }

        mLoadedInitialPhotos = true
        mCurrAsyncTask = GetDirectoriesAsynctask(applicationContext, mIsPickVideoIntent || mIsGetVideoContentIntent, mIsPickImageIntent || mIsGetImageContentIntent) {
            gotDirectories(it)
        }
        mCurrAsyncTask!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, true, false) {
            getDirectories()
        }
    }

    private fun showAllMedia() {
        config.showAll = true
        Intent(this, MediaActivity::class.java).apply {
            putExtra(DIRECTORY, "/")
            startActivity(this)
        }
        finish()
    }

    private fun toggleTemporarilyShowHidden(show: Boolean) {
        config.temporarilyShowHidden = show
        getDirectories()
        invalidateOptionsMenu()
    }

    private fun checkIfColorChanged() {
        if (directories_grid.adapter != null && getRecyclerAdapter().foregroundColor != config.primaryColor) {
            getRecyclerAdapter().updatePrimaryColor(config.primaryColor)
            directories_vertical_fastscroller.updateHandleColor()
            directories_horizontal_fastscroller.updateHandleColor()
        }
    }

    override fun tryDeleteFolders(folders: ArrayList<File>) {
        for (file in folders) {
            deleteFolders(folders) {
                runOnUiThread {
                    refreshItems()
                }
            }
        }
    }

    private fun getRecyclerAdapter() = (directories_grid.adapter as DirectoryAdapter)

    private fun setupLayoutManager() {
        val layoutManager = directories_grid.layoutManager as GridLayoutManager
        if (config.scrollHorizontally) {
            layoutManager.orientation = GridLayoutManager.HORIZONTAL
            directories_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            layoutManager.orientation = GridLayoutManager.VERTICAL
            directories_refresh_layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        directories_grid.isDragSelectionEnabled = true
        directories_grid.isZoomingEnabled = true
        layoutManager.spanCount = config.dirColumnCnt
        directories_grid.listener = object : MyScalableRecyclerView.MyScalableRecyclerViewListener {
            override fun zoomIn() {
                if (layoutManager.spanCount > 1) {
                    reduceColumnCount()
                    getRecyclerAdapter().actMode?.finish()
                }
            }

            override fun zoomOut() {
                if (layoutManager.spanCount < 10) {
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

    private fun increaseColumnCount() {
        config.dirColumnCnt = ++(directories_grid.layoutManager as GridLayoutManager).spanCount
        invalidateOptionsMenu()
        directories_grid.adapter?.notifyDataSetChanged()
    }

    private fun reduceColumnCount() {
        config.dirColumnCnt = --(directories_grid.layoutManager as GridLayoutManager).spanCount
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
            if (requestCode == PICK_MEDIA && resultData?.data != null) {
                Intent().apply {
                    val path = resultData.data.path
                    val uri = Uri.fromFile(File(path))
                    if (mIsGetImageContentIntent || mIsGetVideoContentIntent || mIsGetAnyContentIntent) {
                        if (intent.extras?.containsKey(MediaStore.EXTRA_OUTPUT) == true) {
                            var inputStream: InputStream? = null
                            var outputStream: OutputStream? = null
                            try {
                                val output = intent.extras.get(MediaStore.EXTRA_OUTPUT) as Uri
                                inputStream = FileInputStream(File(path))
                                outputStream = contentResolver.openOutputStream(output)
                                inputStream.copyTo(outputStream)
                            } catch (ignored: FileNotFoundException) {
                            } finally {
                                inputStream?.close()
                                outputStream?.close()
                            }
                        } else {
                            val type = File(path).getMimeType("image/jpeg")
                            setDataAndTypeAndNormalize(uri, type)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        }
                    } else if (mIsPickImageIntent || mIsPickVideoIntent) {
                        data = uri
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }

                    setResult(Activity.RESULT_OK, this)
                }
                finish()
            } else if (requestCode == PICK_WALLPAPER) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun itemClicked(path: String) {
        Intent(this, MediaActivity::class.java).apply {
            putExtra(DIRECTORY, path)

            if (mIsSetWallpaperIntent) {
                putExtra(SET_WALLPAPER_INTENT, true)
                startActivityForResult(this, PICK_WALLPAPER)
            } else {
                putExtra(GET_IMAGE_INTENT, mIsPickImageIntent || mIsGetImageContentIntent)
                putExtra(GET_VIDEO_INTENT, mIsPickVideoIntent || mIsGetVideoContentIntent)
                putExtra(GET_ANY_INTENT, mIsGetAnyContentIntent)
                startActivityForResult(this, PICK_MEDIA)
            }
        }
    }

    private fun gotDirectories(dirs: ArrayList<Directory>) {
        mLastMediaModified = getLastMediaModified()
        directories_refresh_layout.isRefreshing = false
        mIsGettingDirs = false

        checkLastMediaChanged()
        if (dirs.hashCode() == mDirs.hashCode())
            return

        mDirs = dirs

        runOnUiThread {
            setupAdapter()
        }
        storeDirectories()
    }

    private fun storeDirectories() {
        if (!config.temporarilyShowHidden) {
            val directories = Gson().toJson(mDirs)
            config.directories = directories
        }
    }

    private fun setupAdapter() {
        val currAdapter = directories_grid.adapter
        if (currAdapter == null) {
            directories_grid.adapter = DirectoryAdapter(this, mDirs, this, isPickIntent(intent) || isGetAnyContentIntent(intent)) {
                itemClicked(it.path)
            }
        } else {
            (currAdapter as DirectoryAdapter).updateDirs(mDirs)
        }
        setupScrollDirection()
    }

    private fun setupScrollDirection() {
        directories_vertical_fastscroller.isHorizontal = false
        directories_vertical_fastscroller.beGoneIf(config.scrollHorizontally)

        directories_horizontal_fastscroller.isHorizontal = true
        directories_horizontal_fastscroller.beVisibleIf(config.scrollHorizontally)

        if (config.scrollHorizontally) {
            directories_horizontal_fastscroller.setViews(directories_grid, directories_refresh_layout)
        } else {
            directories_vertical_fastscroller.setViews(directories_grid, directories_refresh_layout)
        }
    }

    private fun checkLastMediaChanged() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed)
            return

        mLastMediaHandler.removeCallbacksAndMessages(null)
        mLastMediaHandler.postDelayed({
            Thread({
                val lastModified = getLastMediaModified()
                if (mLastMediaModified != lastModified) {
                    mLastMediaModified = lastModified
                    runOnUiThread {
                        getDirectories()
                    }
                } else {
                    checkLastMediaChanged()
                }
            }).start()
        }, LAST_MEDIA_CHECK_PERIOD)
    }

    override fun refreshItems() {
        getDirectories()
    }

    override fun itemLongClicked(position: Int) {
        directories_grid.setDragSelectActive(position)
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
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
