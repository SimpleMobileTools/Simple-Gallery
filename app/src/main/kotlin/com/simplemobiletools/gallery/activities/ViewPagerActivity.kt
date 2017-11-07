package com.simplemobiletools.gallery.activities

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.hardware.SensorManager
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.support.v4.view.ViewPager
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.DecelerateInterpolator
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.IS_FROM_GALLERY
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.REQUEST_EDIT_IMAGE
import com.simplemobiletools.commons.helpers.REQUEST_SET_AS
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.MediaActivity.Companion.mMedia
import com.simplemobiletools.gallery.adapters.MyPagerAdapter
import com.simplemobiletools.gallery.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.dialogs.DeleteWithRememberDialog
import com.simplemobiletools.gallery.dialogs.SaveAsDialog
import com.simplemobiletools.gallery.dialogs.SlideshowDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.fragments.PhotoFragment
import com.simplemobiletools.gallery.fragments.VideoFragment
import com.simplemobiletools.gallery.fragments.ViewPagerFragment
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.activity_medium.*
import java.io.*
import java.util.*

class ViewPagerActivity : SimpleActivity(), ViewPager.OnPageChangeListener, ViewPagerFragment.FragmentListener {
    private var mOrientationEventListener: OrientationEventListener? = null
    private var mPath = ""
    private var mDirectory = ""

    private var mIsFullScreen = false
    private var mPos = -1
    private var mShowAll = false
    private var mIsSlideshowActive = false
    private var mSkipConfirmationDialog = false
    private var mRotationDegrees = 0f
    private var mLastHandledOrientation = 0
    private var mPrevHashcode = 0

    private var mSlideshowHandler = Handler()
    private var mSlideshowInterval = SLIDESHOW_DEFAULT_INTERVAL
    private var mSlideshowMoveBackwards = false
    private var mSlideshowMedia = mutableListOf<Medium>()
    private var mAreSlideShowMediaVisible = false

    companion object {
        var screenWidth = 0
        var screenHeight = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medium)

        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                initViewPager()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    private fun initViewPager() {
        setupOrientationEventListener()
        measureScreen()
        val uri = intent.data
        if (uri != null) {
            var cursor: Cursor? = null
            try {
                val proj = arrayOf(MediaStore.Images.Media.DATA)
                cursor = contentResolver.query(uri, proj, null, null, null)
                if (cursor?.moveToFirst() == true) {
                    mPath = cursor.getStringValue(MediaStore.Images.Media.DATA)
                }
            } finally {
                cursor?.close()
            }
        } else {
            try {
                mPath = intent.getStringExtra(PATH)
                mShowAll = config.showAll
            } catch (e: Exception) {
                showErrorToast(e)
                finish()
                return
            }
        }

        if (mPath.isEmpty()) {
            toast(R.string.unknown_error_occurred)
            finish()
            return
        }

        if (intent.extras?.containsKey(IS_VIEW_INTENT) == true) {
            if (isShowHiddenFlagNeeded()) {
                if (!config.isPasswordProtectionOn) {
                    config.temporarilyShowHidden = true
                }
            }

            config.isThirdPartyIntent = true
        }

        showSystemUI()

        mDirectory = File(mPath).parent
        title = mPath.getFilenameFromPath()

        view_pager.onGlobalLayout {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !isDestroyed) {
                if (mMedia.isNotEmpty()) {
                    gotMedia(mMedia)
                }
            }
        }

        reloadViewPager()
        scanPath(mPath) {}

        if (config.darkBackground)
            view_pager.background = ColorDrawable(Color.BLACK)

        if (config.hideSystemUI)
            fragmentClicked()

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            mIsFullScreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            view_pager.adapter?.let {
                (it as MyPagerAdapter).toggleFullscreen(mIsFullScreen)
                checkSystemUI()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (intent.extras?.containsKey(IS_VIEW_INTENT) == true) {
            config.temporarilyShowHidden = false
        }

        if (config.isThirdPartyIntent) {
            config.isThirdPartyIntent = false

            if (intent.extras == null || !intent.getBooleanExtra(IS_FROM_GALLERY, false)) {
                mMedia.clear()
            }
        }
    }

    private fun setupOrientationEventListener() {
        mOrientationEventListener = object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                val currOrient = when (orientation) {
                    in 75..134 -> ORIENT_LANDSCAPE_RIGHT
                    in 225..289 -> ORIENT_LANDSCAPE_LEFT
                    else -> ORIENT_PORTRAIT
                }

                if (mLastHandledOrientation != currOrient) {
                    mLastHandledOrientation = currOrient

                    requestedOrientation = when (currOrient) {
                        ORIENT_LANDSCAPE_LEFT -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        ORIENT_LANDSCAPE_RIGHT -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasPermission(PERMISSION_WRITE_STORAGE)) {
            finish()
            return
        }
        supportActionBar?.setBackgroundDrawable(resources.getDrawable(R.drawable.actionbar_gradient_background))

        if (config.maxBrightness) {
            val attributes = window.attributes
            attributes.screenBrightness = 1f
            window.attributes = attributes
        }

        if (config.screenRotation == ROTATE_BY_DEVICE_ROTATION && mOrientationEventListener?.canDetectOrientation() == true) {
            mOrientationEventListener?.enable()
        } else if (config.screenRotation == ROTATE_BY_SYSTEM_SETTING) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        mOrientationEventListener?.disable()
        stopSlideshow()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_viewpager, menu)
        val currentMedium = getCurrentMedium() ?: return true

        menu.apply {
            findItem(R.id.menu_share_1).isVisible = !config.replaceShare
            findItem(R.id.menu_share_2).isVisible = config.replaceShare
            findItem(R.id.menu_rotate).isVisible = currentMedium.isImage()
            findItem(R.id.menu_save_as).isVisible = mRotationDegrees != 0f
            findItem(R.id.menu_hide).isVisible = !currentMedium.name.startsWith('.')
            findItem(R.id.menu_unhide).isVisible = currentMedium.name.startsWith('.')
            findItem(R.id.menu_rotate).setShowAsAction(
                    if (mRotationDegrees != 0f) {
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                    } else {
                        MenuItem.SHOW_AS_ACTION_IF_ROOM
                    })
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (getCurrentMedium() == null)
            return true

        when (item.itemId) {
            R.id.menu_set_as -> setAs(Uri.fromFile(getCurrentFile()))
            R.id.slideshow -> initSlideshow()
            R.id.menu_copy_to -> copyMoveTo(true)
            R.id.menu_move_to -> copyMoveTo(false)
            R.id.menu_open_with -> openFile(Uri.fromFile(getCurrentFile()), true)
            R.id.menu_hide -> toggleFileVisibility(true)
            R.id.menu_unhide -> toggleFileVisibility(false)
            R.id.menu_share_1 -> shareMedium(getCurrentMedium()!!)
            R.id.menu_share_2 -> shareMedium(getCurrentMedium()!!)
            R.id.menu_delete -> checkDeleteConfirmation()
            R.id.menu_rename -> renameFile()
            R.id.menu_edit -> openEditor(Uri.fromFile(getCurrentFile()))
            R.id.menu_properties -> showProperties()
            R.id.show_on_map -> showOnMap()
            R.id.menu_rotate -> rotateImage()
            R.id.menu_save_as -> saveImageAs()
            R.id.settings -> launchSettings()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun updatePagerItems(media: MutableList<Medium>) {
        val pagerAdapter = MyPagerAdapter(this, supportFragmentManager, media)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !isDestroyed) {
            view_pager.apply {
                adapter = pagerAdapter
                currentItem = mPos
                addOnPageChangeListener(this@ViewPagerActivity)
            }
        }
    }

    private fun initSlideshow() {
        SlideshowDialog(this) {
            startSlideshow()
        }
    }

    private fun startSlideshow() {
        if (getMediaForSlideshow()) {
            view_pager.onGlobalLayout {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !isDestroyed) {
                    hideSystemUI()
                    mSlideshowInterval = config.slideshowInterval
                    mSlideshowMoveBackwards = config.slideshowMoveBackwards
                    mIsSlideshowActive = true
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    scheduleSwipe()
                }
            }
        }
    }

    private fun animatePagerTransition(forward: Boolean) {
        val oldPosition = view_pager.currentItem
        val animator = ValueAnimator.ofInt(0, view_pager.width)
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                view_pager.endFakeDrag()

                if (view_pager.currentItem == oldPosition) {
                    slideshowEnded(forward)
                }
            }

            override fun onAnimationCancel(animation: Animator?) {
                view_pager.endFakeDrag()
            }

            override fun onAnimationStart(animation: Animator?) {
            }
        })

        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            var oldDragPosition = 0
            override fun onAnimationUpdate(animation: ValueAnimator) {
                if (view_pager?.isFakeDragging == true) {
                    val dragPosition = animation.animatedValue as Int
                    val dragOffset = dragPosition - oldDragPosition
                    oldDragPosition = dragPosition
                    view_pager.fakeDragBy(dragOffset * (if (forward) 1f else -1f))
                }
            }
        })

        animator.duration = SLIDESHOW_SCROLL_DURATION
        view_pager.beginFakeDrag()
        animator.start()
    }

    private fun slideshowEnded(forward: Boolean) {
        if (config.loopSlideshow) {
            if (forward) {
                view_pager.setCurrentItem(0, false)
            } else {
                view_pager.setCurrentItem(view_pager.adapter!!.count - 1, false)
            }
        } else {
            stopSlideshow()
            toast(R.string.slideshow_ended)
        }
    }

    private fun stopSlideshow() {
        if (mIsSlideshowActive) {
            showSystemUI()
            mIsSlideshowActive = false
            mSlideshowHandler.removeCallbacksAndMessages(null)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun scheduleSwipe() {
        mSlideshowHandler.removeCallbacksAndMessages(null)
        if (mIsSlideshowActive) {
            if (getCurrentMedium()!!.isImage() || getCurrentMedium()!!.isGif()) {
                mSlideshowHandler.postDelayed({
                    if (mIsSlideshowActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && !isDestroyed) {
                        swipeToNextMedium()
                    }
                }, mSlideshowInterval * 1000L)
            } else {
                (getCurrentFragment() as? VideoFragment)!!.playVideo()
            }
        }
    }

    private fun swipeToNextMedium() {
        animatePagerTransition(!mSlideshowMoveBackwards)
    }

    private fun getMediaForSlideshow(): Boolean {
        mSlideshowMedia = mMedia.toMutableList()
        if (!config.slideshowIncludePhotos) {
            mSlideshowMedia = mSlideshowMedia.filter { !it.isImage() } as MutableList
        }

        if (!config.slideshowIncludeVideos) {
            mSlideshowMedia = mSlideshowMedia.filter { it.isImage() || it.isGif() } as MutableList
        }

        if (!config.slideshowIncludeGIFs) {
            mSlideshowMedia = mSlideshowMedia.filter { !it.isGif() } as MutableList
        }

        if (config.slideshowRandomOrder) {
            Collections.shuffle(mSlideshowMedia)
            mPos = 0
        } else {
            mPath = getCurrentPath()
            mPos = getPositionInList(mSlideshowMedia)
        }

        return if (mSlideshowMedia.isEmpty()) {
            toast(R.string.no_media_for_slideshow)
            false
        } else {
            updatePagerItems(mSlideshowMedia)
            mAreSlideShowMediaVisible = true
            true
        }
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val files = ArrayList<File>(1).apply { add(getCurrentFile()) }
        tryCopyMoveFilesTo(files, isCopyOperation) {
            config.tempFolderPath = ""
            if (!isCopyOperation) {
                reloadViewPager()
            }
        }
    }

    private fun toggleFileVisibility(hide: Boolean) {
        toggleFileVisibility(getCurrentFile(), hide) {
            val newFileName = it.absolutePath.getFilenameFromPath()
            title = newFileName

            getCurrentMedium()!!.apply {
                name = newFileName
                path = it.absolutePath
                getCurrentMedia()[mPos] = this
            }
            invalidateOptionsMenu()
        }
    }

    private fun rotateImage() {
        mRotationDegrees = (mRotationDegrees + 90) % 360
        getCurrentFragment()?.let {
            (it as? PhotoFragment)?.rotateImageViewBy(mRotationDegrees)
        }
        supportInvalidateOptionsMenu()
    }

    private fun saveImageAs() {
        val currPath = getCurrentPath()
        SaveAsDialog(this, currPath, false) {
            Thread({
                toast(R.string.saving)
                val selectedFile = File(it)
                handleSAFDialog(selectedFile) {
                    val tmpFile = File(filesDir, ".tmp_${it.getFilenameFromPath()}")
                    try {
                        val bitmap = BitmapFactory.decodeFile(currPath)
                        getFileOutputStream(tmpFile) {
                            if (it == null) {
                                toast(R.string.unknown_error_occurred)
                                return@getFileOutputStream
                            }

                            if (currPath.isJpg()) {
                                saveRotation(getCurrentFile(), tmpFile)
                            } else {
                                saveFile(tmpFile, bitmap, it as FileOutputStream)
                            }

                            if (tmpFile.length() > 0 && selectedFile.exists()) {
                                deleteFile(selectedFile) {}
                            }
                            copyFile(tmpFile, selectedFile)
                            scanPath(selectedFile.absolutePath) {}
                            toast(R.string.file_saved)

                            it.flush()
                            it.close()
                            mRotationDegrees = 0f
                            invalidateOptionsMenu()
                        }
                    } catch (e: OutOfMemoryError) {
                        toast(R.string.out_of_memory_error)
                    } catch (e: Exception) {
                        showErrorToast(e)
                    } finally {
                        deleteFile(tmpFile) {}
                    }
                }
            }).start()
        }
    }

    private fun copyFile(source: File, destination: File) {
        var inputStream: InputStream? = null
        var out: OutputStream? = null
        try {
            out = getFileOutputStreamSync(destination.absolutePath, source.getMimeType(), getFileDocument(destination.parent))
            inputStream = FileInputStream(source)
            inputStream.copyTo(out!!)
        } finally {
            inputStream?.close()
            out?.close()
        }
    }

    private fun saveFile(file: File, bitmap: Bitmap, out: FileOutputStream) {
        val matrix = Matrix()
        matrix.postRotate(mRotationDegrees)
        val bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bmp.compress(file.getCompressionFormat(), 90, out)
    }

    private fun saveRotation(source: File, destination: File) {
        copyFile(source, destination)
        val exif = ExifInterface(destination.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val orientationDegrees = (degreesForRotation(orientation) + mRotationDegrees) % 360
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, rotationFromDegrees(orientationDegrees))
        exif.saveAttributes()
    }

    private fun degreesForRotation(orientation: Int) = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        else -> 0f
    }

    private fun rotationFromDegrees(degrees: Float) = when (degrees) {
        270f -> ExifInterface.ORIENTATION_ROTATE_270
        180f -> ExifInterface.ORIENTATION_ROTATE_180
        90f -> ExifInterface.ORIENTATION_ROTATE_90
        else -> ExifInterface.ORIENTATION_NORMAL
    }.toString()

    private fun isShowHiddenFlagNeeded(): Boolean {
        val file = File(mPath)
        if (file.isHidden)
            return true

        var parent = file.parentFile ?: return false
        while (true) {
            if (parent.isHidden || parent.listFiles()?.contains(File(NOMEDIA)) == true) {
                return true
            }

            if (parent.absolutePath == "/") {
                break
            }
            parent = parent.parentFile ?: return false
        }

        return false
    }

    private fun getCurrentFragment() = (view_pager.adapter as MyPagerAdapter).getCurrentFragment(view_pager.currentItem)

    private fun showProperties() {
        if (getCurrentMedium() != null) {
            PropertiesDialog(this, getCurrentPath(), false)
        }
    }

    private fun showOnMap() {
        val exif = ExifInterface(getCurrentPath())
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
        } else if (requestCode == REQUEST_SET_AS) {
            if (resultCode == Activity.RESULT_OK) {
                toast(R.string.wallpaper_set_successfully)
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun checkDeleteConfirmation() {
        if (mSkipConfirmationDialog) {
            deleteConfirmed()
        } else {
            askConfirmDelete()
        }
    }

    private fun askConfirmDelete() {
        DeleteWithRememberDialog(this) {
            mSkipConfirmationDialog = it
            deleteConfirmed()
        }
    }

    private fun deleteConfirmed() {
        deleteFileBg(File(getCurrentMedia()[mPos].path)) {
            reloadViewPager()
        }
    }

    private fun isDirEmpty(media: ArrayList<Medium>): Boolean {
        return if (media.isEmpty()) {
            deleteDirectoryIfEmpty()
            finish()
            true
        } else {
            false
        }
    }

    private fun renameFile() {
        RenameItemDialog(this, getCurrentPath()) {
            getCurrentMedia()[mPos].path = it
            updateActionbarTitle()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        measureScreen()
    }

    private fun measureScreen() {
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            windowManager.defaultDisplay.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
        } else {
            windowManager.defaultDisplay.getMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
        }
    }

    private fun reloadViewPager() {
        GetMediaAsynctask(applicationContext, mDirectory, false, false, mShowAll) {
            gotMedia(it)
        }.execute()
    }

    private fun gotMedia(media: ArrayList<Medium>) {
        if (isDirEmpty(media) || media.hashCode() == mPrevHashcode) {
            return
        }

        mPrevHashcode = media.hashCode()
        mMedia = media
        mPos = if (mPos == -1) {
            getPositionInList(media)
        } else {
            Math.min(mPos, mMedia.size - 1)
        }

        updateActionbarTitle()
        updatePagerItems(mMedia.toMutableList())
        invalidateOptionsMenu()
        checkOrientation()
    }

    private fun getPositionInList(items: MutableList<Medium>): Int {
        mPos = 0
        for ((i, medium) in items.withIndex()) {
            if (medium.path == mPath) {
                return i
            }
        }
        return mPos
    }

    private fun deleteDirectoryIfEmpty() {
        val file = File(mDirectory)
        if (config.deleteEmptyFolders && !file.isDownloadsFolder() && file.isDirectory && file.listFiles()?.isEmpty() == true) {
            deleteFile(file, true) {}
        }

        scanPath(mDirectory) {}
    }

    private fun checkOrientation() {
        if (config.screenRotation == ROTATE_BY_ASPECT_RATIO) {
            val res = getCurrentFile().getResolution()
            if (res.x > res.y) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else if (res.x < res.y) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    override fun fragmentClicked() {
        mIsFullScreen = !mIsFullScreen
        checkSystemUI()
    }

    override fun videoEnded(): Boolean {
        if (mIsSlideshowActive)
            swipeToNextMedium()
        return mIsSlideshowActive
    }

    private fun checkSystemUI() {
        if (mIsFullScreen) {
            hideSystemUI()
        } else {
            stopSlideshow()
            showSystemUI()
        }
    }

    private fun updateActionbarTitle() {
        runOnUiThread {
            if (mPos < getCurrentMedia().size) {
                title = getCurrentMedia()[mPos].path.getFilenameFromPath()
            }
        }
    }

    private fun getCurrentMedium(): Medium? {
        return if (getCurrentMedia().isEmpty() || mPos == -1) {
            null
        } else {
            getCurrentMedia()[Math.min(mPos, getCurrentMedia().size - 1)]
        }
    }

    private fun getCurrentMedia() = if (mAreSlideShowMediaVisible) mSlideshowMedia else mMedia

    private fun getCurrentPath() = getCurrentMedium()!!.path

    private fun getCurrentFile() = File(getCurrentPath())

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        if (view_pager.offscreenPageLimit == 1) {
            view_pager.offscreenPageLimit = 2
        }
        mPos = position
        updateActionbarTitle()
        mRotationDegrees = 0f
        supportInvalidateOptionsMenu()
        scheduleSwipe()
    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state == ViewPager.SCROLL_STATE_IDLE && getCurrentMedium() != null) {
            checkOrientation()
        }
    }
}
