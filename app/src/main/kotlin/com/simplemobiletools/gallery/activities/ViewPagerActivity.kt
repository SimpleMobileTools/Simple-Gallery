package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.view.ViewPager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.simplemobiletools.commons.asynctasks.CopyMoveTask
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.adapters.MyPagerAdapter
import com.simplemobiletools.gallery.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.dialogs.CopyDialog
import com.simplemobiletools.gallery.dialogs.RenameFileDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.fragments.ViewPagerFragment
import com.simplemobiletools.gallery.helpers.MEDIUM
import com.simplemobiletools.gallery.helpers.REQUEST_EDIT_IMAGE
import com.simplemobiletools.gallery.helpers.REQUEST_SET_WALLPAPER
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.activity_medium.*
import java.io.File
import java.util.*

class ViewPagerActivity : SimpleActivity(), ViewPager.OnPageChangeListener, ViewPagerFragment.FragmentListener {
    private var mMedia = ArrayList<Medium>()
    private var mPath = ""
    private var mDirectory = ""

    private var mIsFullScreen = false
    private var mPos = -1
    private var mShowAll = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medium)

        if (!hasWriteStoragePermission()) {
            finish()
            return
        }

        val uri = intent.data
        if (uri != null) {
            var cursor: Cursor? = null
            try {
                val proj = arrayOf(MediaStore.Images.Media.DATA)
                cursor = contentResolver.query(uri, proj, null, null, null)
                if (cursor != null) {
                    val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    cursor.moveToFirst()
                    mPath = cursor.getString(dataIndex)
                }
            } finally {
                cursor?.close()
            }
        } else {
            mPath = intent.getStringExtra(MEDIUM)
            mShowAll = config.showAll
        }

        if (mPath.isEmpty()) {
            toast(R.string.unknown_error_occurred)
            finish()
            return
        }

        mMedia = ArrayList<Medium>()
        showSystemUI()

        mDirectory = File(mPath).parent
        title = mPath.getFilenameFromPath()
        reloadViewPager()
        scanPath(mPath) {}
    }

    override fun onResume() {
        super.onResume()
        if (!hasWriteStoragePermission()) {
            finish()
        }
        supportActionBar?.setBackgroundDrawable(resources.getDrawable(R.drawable.actionbar_gradient_background))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.viewpager_menu, menu)

        menu.findItem(R.id.menu_set_as_wallpaper).isVisible = getCurrentMedium()?.isImage() == true
        menu.findItem(R.id.menu_edit).isVisible = getCurrentMedium()?.isImage() == true

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_set_as_wallpaper -> setAsWallpaper(getCurrentFile())
            R.id.menu_copy_move -> displayCopyDialog()
            R.id.menu_open_with -> openWith(getCurrentFile())
            R.id.menu_share -> shareMedium(getCurrentMedium()!!)
            R.id.menu_delete -> askConfirmDelete()
            R.id.menu_rename -> renameFile()
            R.id.menu_edit -> openEditor(getCurrentFile())
            R.id.menu_properties -> showProperties()
            R.id.show_on_map -> showOnMap()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun updatePagerItems() {
        val pagerAdapter = MyPagerAdapter(this, supportFragmentManager, mMedia)
        view_pager.apply {
            adapter = pagerAdapter
            currentItem = mPos
            addOnPageChangeListener(this@ViewPagerActivity)
        }
    }

    private fun displayCopyDialog() {
        val files = ArrayList<File>()
        files.add(getCurrentFile())
        CopyDialog(this, files, object : CopyMoveTask.CopyMoveListener {
            override fun copySucceeded(deleted: Boolean, copiedAll: Boolean) {
                if (deleted) {
                    reloadViewPager()
                    toast(if (copiedAll) R.string.moving_success else R.string.moving_success_partial)
                } else {
                    toast(if (copiedAll) R.string.copying_success else R.string.copying_success_partial)
                }
            }

            override fun copyFailed() {
                toast(R.string.copy_move_failed)
            }
        })
    }

    private fun showProperties() {
        if (getCurrentMedium() != null)
            PropertiesDialog(this, getCurrentMedium()!!.path, false)
    }

    private fun showOnMap() {
        val exif = ExifInterface(getCurrentMedium()?.path)
        val lat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
        val lat_ref = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
        val lon = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
        val lon_ref = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)

        if (lat == null || lat_ref == null || lon == null || lon_ref == null) {
            toast(R.string.unknown_location)
        } else {
            val geoLat = if (lat_ref == "N") {
                convertToDegree(lat)
            } else {
                0 - convertToDegree(lat)
            }

            val geoLon = if (lon_ref == "E") {
                convertToDegree(lon)
            } else {
                0 - convertToDegree(lon)
            }

            val uriBegin = "geo:$geoLat,$geoLon"
            val query = "$geoLat, $geoLon"
            val encodedQuery = Uri.encode(query)
            val uriString = "$uriBegin?q=$encodedQuery&z=16"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
            val packageManager = packageManager
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                toast(R.string.no_map_application)
            }
        }
    }

    private fun convertToDegree(stringDMS: String): Float {
        val dms = stringDMS.split(",".toRegex(), 3).toTypedArray()

        val stringD = dms[0].split("/".toRegex(), 2).toTypedArray()
        val d0 = stringD[0].toDouble()
        val d1 = stringD[1].toDouble()
        val floatD = d0 / d1

        val stringM = dms[1].split("/".toRegex(), 2).toTypedArray()
        val m0 = stringM[0].toDouble()
        val m1 = stringM[1].toDouble()
        val floatM = m0 / m1

        val stringS = dms[2].split("/".toRegex(), 2).toTypedArray()
        val s0 = stringS[0].toDouble()
        val s1 = stringS[1].toDouble()
        val floatS = s0 / s1

        return (floatD + floatM / 60 + floatS / 3600).toFloat()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == REQUEST_EDIT_IMAGE) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                mPos = -1
                reloadViewPager()
            }
        } else if (requestCode == REQUEST_SET_WALLPAPER) {
            if (resultCode == Activity.RESULT_OK) {
                toast(R.string.wallpaper_set_successfully)
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(this) {
            deleteFile()
        }
    }

    private fun deleteFile() {
        val file = File(mMedia[mPos].path)
        if (isShowingPermDialog(file))
            return

        if (needsStupidWritePermissions(mPath)) {
            if (!isShowingPermDialog(file)) {
                val document = getFileDocument(mPath, config.treeUri)
                if (document.uri.toString().endsWith(file.absolutePath.getFilenameFromPath()) && !document.isDirectory)
                    document.delete()
            }
        } else {
            file.delete()
        }

        if (deleteFromMediaStore(file)) {
            reloadViewPager()
        } else {
            scanFile(file) {
                reloadViewPager()
            }
        }
    }

    private fun isDirEmpty(): Boolean {
        return if (mMedia.size <= 0) {
            deleteDirectoryIfEmpty()
            finish()
            true
        } else
            false
    }

    private fun renameFile() {
        RenameFileDialog(this, getCurrentFile()) {
            mMedia[mPos].path = it.absolutePath
            updateActionbarTitle()
        }
    }

    private fun reloadViewPager() {
        GetMediaAsynctask(applicationContext, mDirectory, false, false, mShowAll) {
            mMedia = it
            if (isDirEmpty())
                return@GetMediaAsynctask

            if (mPos == -1) {
                mPos = getProperPosition()
            } else {
                mPos = Math.min(mPos, mMedia.size - 1)
            }

            updateActionbarTitle()
            updatePagerItems()
            invalidateOptionsMenu()
        }.execute()
    }

    private fun getProperPosition(): Int {
        mPos = 0
        var i = 0
        for (medium in mMedia) {
            if (medium.path == mPath) {
                return i
            }
            i++
        }
        return mPos
    }

    private fun deleteDirectoryIfEmpty() {
        val file = File(mDirectory)
        if (file.isDirectory && file.listFiles()?.isEmpty() == true) {
            file.delete()
        }

        scanPath(mDirectory) {}
    }

    override fun fragmentClicked() {
        mIsFullScreen = !mIsFullScreen
        if (mIsFullScreen) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    override fun systemUiVisibilityChanged(visibility: Int) {
        if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
            mIsFullScreen = false
            showSystemUI()
        } else {
            mIsFullScreen = true
        }
    }

    private fun updateActionbarTitle() {
        runOnUiThread {
            title = mMedia[mPos].path.getFilenameFromPath()
        }
    }

    private fun getCurrentMedium(): Medium? {
        return if (mMedia.isEmpty())
            null
        else
            mMedia[Math.min(mPos, mMedia.size - 1)]
    }

    private fun getCurrentFile() = File(getCurrentMedium()!!.path)

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        mPos = position
        updateActionbarTitle()
        supportInvalidateOptionsMenu()
    }

    override fun onPageScrollStateChanged(state: Int) {
    }
}
