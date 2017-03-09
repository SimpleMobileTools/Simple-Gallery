package com.simplemobiletools.gallery.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.Release
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.dialogs.ChangeSortingDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.views.MyScalableRecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.net.URLDecoder
import java.util.*

class MainActivity : SimpleActivity(), DirectoryAdapter.DirOperationsListener {
    private val STORAGE_PERMISSION = 1
    private val PICK_MEDIA = 2
    private val PICK_WALLPAPER = 3

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
        storeStoragePaths()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (mIsThirdPartyIntent)
            return false

        menuInflater.inflate(R.menu.menu_main, menu)

        menu.findItem(R.id.increase_column_count).isVisible = config.dirColumnCnt < 10
        menu.findItem(R.id.reduce_column_count).isVisible = config.dirColumnCnt > 1
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.open_camera -> launchCamera()
            R.id.show_all -> showAllMedia()
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
            mDirs.clear()
        }
        tryloadGallery()
    }

    override fun onPause() {
        super.onPause()
        mCurrAsyncTask?.shouldStop = true
        storeDirectories()
        directories_refresh_layout.isRefreshing = false
        mIsGettingDirs = false
        mStoredAnimateGifs = config.animateGifs
    }

    override fun onDestroy() {
        super.onDestroy()
        config.isFirstRun = false
    }

    private fun tryloadGallery() {
        if (hasWriteStoragePermission()) {
            if (config.showAll)
                showAllMedia()
            else
                getDirectories()
            handleZooming()
            checkWhatsNewDialog()
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
                toast(R.string.no_permissions)
                finish()
            }
        }
    }

    private fun getDirectories() {
        if (mIsGettingDirs)
            return

        mIsGettingDirs = true
        val token = object : TypeToken<List<Directory>>() {}.type
        val dirs = Gson().fromJson<ArrayList<Directory>>(config.directories, token) ?: ArrayList<Directory>(1)
        if (dirs.size == 0) {
            directories_refresh_layout.isRefreshing = true
        } else {
            gotDirectories(dirs)
        }

        mCurrAsyncTask = GetDirectoriesAsynctask(applicationContext, mIsPickVideoIntent || mIsGetVideoContentIntent, mIsPickImageIntent || mIsGetImageContentIntent) {
            gotDirectories(it)
        }
        mCurrAsyncTask!!.execute()
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

    private fun checkIfColorChanged() {
        if (DirectoryAdapter.foregroundColor != config.primaryColor) {
            DirectoryAdapter.foregroundColor = config.primaryColor
            setupAdapter()
        }
    }

    override fun deleteFiles(paths: ArrayList<String>) {
        val updatedFiles = ArrayList<File>()
        for (delPath in paths) {
            val dir = File(delPath)
            if (dir.exists()) {
                val files = dir.listFiles() ?: continue
                files.forEach {
                    if (it.isFile && it.isImageVideoGif()) {
                        updatedFiles.add(it)
                        deleteItem(it)
                    }
                }
                updatedFiles.add(dir)
                if (dir.listFiles().isEmpty())
                    deleteItem(dir)
            }
        }

        scanFiles(updatedFiles) {}
    }

    private fun deleteItem(file: File) {
        if (isShowingPermDialog(file)) {
            return
        }

        Thread({
            if (!file.delete()) {
                val document = getFileDocument(file.absolutePath, config.treeUri) ?: return@Thread

                // double check we have the uri to the proper file path, not some parent folder
                val uri = URLDecoder.decode(document.uri.toString(), "UTF-8")
                val filename = URLDecoder.decode(file.absolutePath.getFilenameFromPath(), "UTF-8")
                if (uri.endsWith(filename)) {
                    document.delete()
                }
            }
        }).start()
    }

    private fun handleZooming() {
        val layoutManager = directories_grid.layoutManager as GridLayoutManager
        layoutManager.spanCount = config.dirColumnCnt
        MyScalableRecyclerView.mListener = object : MyScalableRecyclerView.ZoomListener {
            override fun zoomIn() {
                if (layoutManager.spanCount > 1) {
                    reduceColumnCount()
                    DirectoryAdapter.actMode?.finish()
                }
            }

            override fun zoomOut() {
                if (layoutManager.spanCount < 10) {
                    increaseColumnCount()
                    DirectoryAdapter.actMode?.finish()
                }
            }
        }
    }

    private fun increaseColumnCount() {
        config.dirColumnCnt = ++(directories_grid.layoutManager as GridLayoutManager).spanCount
        invalidateOptionsMenu()
    }

    private fun reduceColumnCount() {
        config.dirColumnCnt = --(directories_grid.layoutManager as GridLayoutManager).spanCount
        invalidateOptionsMenu()
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

    private fun hasImageContentData(intent: Intent) = intent.data == MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    private fun hasVideoContentData(intent: Intent) = intent.data == MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    private fun isImageType(intent: Intent) = (intent.type?.startsWith("image/") == true || intent.type == MediaStore.Images.Media.CONTENT_TYPE)

    private fun isVideoType(intent: Intent) = (intent.type?.startsWith("video/") == true || intent.type == MediaStore.Video.Media.CONTENT_TYPE)

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_MEDIA && resultData != null) {
                Intent().apply {
                    val path = resultData.data.path
                    val uri = getFileUri(File(path))
                    if (mIsGetImageContentIntent || mIsGetVideoContentIntent || mIsGetAnyContentIntent) {
                        val type = File(path).getMimeType("image/jpeg")
                        setDataAndTypeAndNormalize(uri, type)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
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
        directories_refresh_layout.isRefreshing = false
        mIsGettingDirs = false

        if (dirs.hashCode() == mDirs.hashCode())
            return

        mDirs = dirs

        setupAdapter()
        storeDirectories()
    }

    private fun storeDirectories() {
        val directories = Gson().toJson(mDirs)
        config.directories = directories
    }

    private fun setupAdapter() {
        val adapter = DirectoryAdapter(this, mDirs, this) {
            itemClicked(it.path)
        }

        directories_grid.adapter = adapter
        directories_fastscroller.setViews(directories_grid, directories_refresh_layout)
    }

    override fun refreshItems() {
        getDirectories()
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
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
