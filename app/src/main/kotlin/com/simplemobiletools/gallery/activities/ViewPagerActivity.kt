package com.simplemobiletools.gallery.activities

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
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
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.support.v4.view.ViewPager
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import com.bumptech.glide.Glide
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.gallery.R
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
import com.simplemobiletools.gallery.models.ThumbnailItem
import kotlinx.android.synthetic.main.activity_medium.*
import kotlinx.android.synthetic.main.bottom_actions.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class ViewPagerActivity : SimpleActivity(), ViewPager.OnPageChangeListener, ViewPagerFragment.FragmentListener {
    private var mPath = ""
    private var mDirectory = ""
    private var mIsFullScreen = false
    private var mPos = -1
    private var mShowAll = false
    private var mIsSlideshowActive = false
    private var mRotationDegrees = 0
    private var mPrevHashcode = 0

    private var mSlideshowHandler = Handler()
    private var mSlideshowInterval = SLIDESHOW_DEFAULT_INTERVAL
    private var mSlideshowMoveBackwards = false
    private var mSlideshowMedia = mutableListOf<Medium>()
    private var mAreSlideShowMediaVisible = false
    private var mIsOrientationLocked = false

    private var mMediaFiles = ArrayList<Medium>()
    private var mFavoritePaths = ArrayList<String>()

    companion object {
        var screenWidth = 0
        var screenHeight = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medium)

        top_shadow.layoutParams.height = statusBarHeight + actionBarHeight
        if (isOreoPlus()) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }

        (MediaActivity.mMedia.clone() as ArrayList<ThumbnailItem>).filter { it is Medium }.mapTo(mMediaFiles) { it as Medium }

        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                initViewPager()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }

        initFavorites()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onResume() {
        super.onResume()
        if (!hasPermission(PERMISSION_WRITE_STORAGE)) {
            finish()
            return
        }

        if (config.bottomActions) {
            if (isLollipopPlus()) {
                window.navigationBarColor = Color.TRANSPARENT
            }
        } else {
            setTranslucentNavigation()
        }

        initBottomActions()

        if (config.maxBrightness) {
            val attributes = window.attributes
            attributes.screenBrightness = 1f
            window.attributes = attributes
        }

        setupRotation()
        invalidateOptionsMenu()

        if (config.blackBackground) {
            updateStatusbarColor(Color.BLACK)
        }

        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (isLollipopPlus()) {
            window.statusBarColor = Color.TRANSPARENT
        }
    }

    override fun onPause() {
        super.onPause()
        stopSlideshow()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (intent.extras?.containsKey(IS_VIEW_INTENT) == true) {
            config.temporarilyShowHidden = false
        }

        if (config.isThirdPartyIntent) {
            config.isThirdPartyIntent = false

            if (intent.extras == null || !intent.getBooleanExtra(IS_FROM_GALLERY, false)) {
                mMediaFiles.clear()
            }
        }
    }

    private fun initViewPager() {
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

        if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            mPath = intent.extras.getString(REAL_FILE_PATH)
        }

        if (mPath.isEmpty()) {
            toast(R.string.unknown_error_occurred)
            finish()
            return
        }

        if (!getDoesFilePathExist(mPath)) {
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

        showSystemUI(true)

        val isShowingFavorites = intent.getBooleanExtra(SHOW_FAVORITES, false)
        val isShowingRecycleBin = intent.getBooleanExtra(SHOW_RECYCLE_BIN, false)
        mDirectory = when {
            isShowingFavorites -> FAVORITES
            isShowingRecycleBin -> RECYCLE_BIN
            else -> mPath.getParentPath()
        }
        if (mDirectory.startsWith(OTG_PATH.trimEnd('/'))) {
            mDirectory += "/"
        }
        supportActionBar?.title = mPath.getFilenameFromPath()

        view_pager.onGlobalLayout {
            if (!isActivityDestroyed()) {
                if (mMediaFiles.isNotEmpty()) {
                    gotMedia(mMediaFiles as ArrayList<ThumbnailItem>)
                }
            }
        }

        refreshViewPager()
        view_pager.offscreenPageLimit = 2

        if (config.blackBackground) {
            view_pager.background = ColorDrawable(Color.BLACK)
        }

        if (config.hideSystemUI) {
            view_pager.onGlobalLayout {
                Handler().postDelayed({
                    fragmentClicked()
                }, 500)
            }
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            mIsFullScreen = if (visibility and View.SYSTEM_UI_FLAG_LOW_PROFILE == 0) {
                false
            } else {
                visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            }

            view_pager.adapter?.let {
                (it as MyPagerAdapter).toggleFullscreen(mIsFullScreen)
                checkSystemUI()
                if (!bottom_actions.isGone()) {
                    val newAlpha = if (mIsFullScreen) 0f else 1f
                    bottom_actions.animate().alpha(newAlpha).start()
                    top_shadow.animate().alpha(newAlpha).start()
                    arrayOf(bottom_favorite, bottom_edit, bottom_share, bottom_delete, bottom_rotate, bottom_properties, bottom_change_orientation,
                            bottom_slideshow, bottom_show_on_map, bottom_toggle_file_visibility, bottom_rename).forEach {
                        it.isClickable = !mIsFullScreen
                    }
                }
            }
        }
    }

    private fun initBottomActions() {
        initBottomActionsLayout()
        initBottomActionButtons()
    }

    private fun initFavorites() {
        Thread {
            mFavoritePaths = getFavoritePaths()
        }.start()
    }

    private fun setupRotation() {
        if (!mIsOrientationLocked) {
            if (config.screenRotation == ROTATE_BY_DEVICE_ROTATION) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else if (config.screenRotation == ROTATE_BY_SYSTEM_SETTING) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_viewpager, menu)
        val currentMedium = getCurrentMedium() ?: return true
        currentMedium.isFavorite = mFavoritePaths.contains(currentMedium.path)
        val visibleBottomActions = if (config.bottomActions) config.visibleBottomActions else 0

        menu.apply {
            findItem(R.id.menu_show_on_map).isVisible = visibleBottomActions and BOTTOM_ACTION_SHOW_ON_MAP == 0
            findItem(R.id.menu_slideshow).isVisible = visibleBottomActions and BOTTOM_ACTION_SLIDESHOW == 0
            findItem(R.id.menu_properties).isVisible = visibleBottomActions and BOTTOM_ACTION_PROPERTIES == 0
            findItem(R.id.menu_delete).isVisible = visibleBottomActions and BOTTOM_ACTION_DELETE == 0
            findItem(R.id.menu_share).isVisible = visibleBottomActions and BOTTOM_ACTION_SHARE == 0
            findItem(R.id.menu_edit).isVisible = visibleBottomActions and BOTTOM_ACTION_EDIT == 0 && !currentMedium.isSVG()
            findItem(R.id.menu_rename).isVisible = visibleBottomActions and BOTTOM_ACTION_RENAME == 0 && !currentMedium.getIsInRecycleBin()
            findItem(R.id.menu_rotate).isVisible = currentMedium.isImage() && visibleBottomActions and BOTTOM_ACTION_ROTATE == 0
            findItem(R.id.menu_set_as).isVisible = visibleBottomActions and BOTTOM_ACTION_SET_AS == 0
            findItem(R.id.menu_save_as).isVisible = mRotationDegrees != 0
            findItem(R.id.menu_hide).isVisible = !currentMedium.isHidden() && visibleBottomActions and BOTTOM_ACTION_TOGGLE_VISIBILITY == 0 && !currentMedium.getIsInRecycleBin()
            findItem(R.id.menu_unhide).isVisible = currentMedium.isHidden() && visibleBottomActions and BOTTOM_ACTION_TOGGLE_VISIBILITY == 0 && !currentMedium.getIsInRecycleBin()
            findItem(R.id.menu_add_to_favorites).isVisible = !currentMedium.isFavorite && visibleBottomActions and BOTTOM_ACTION_TOGGLE_FAVORITE == 0
            findItem(R.id.menu_remove_from_favorites).isVisible = currentMedium.isFavorite && visibleBottomActions and BOTTOM_ACTION_TOGGLE_FAVORITE == 0
            findItem(R.id.menu_restore_file).isVisible = currentMedium.path.startsWith(filesDir.absolutePath)
            findItem(R.id.menu_change_orientation).isVisible = mRotationDegrees == 0 && visibleBottomActions and BOTTOM_ACTION_CHANGE_ORIENTATION == 0
            findItem(R.id.menu_change_orientation).icon = resources.getDrawable(getChangeOrientationIcon())
            findItem(R.id.menu_rotate).setShowAsAction(
                    if (mRotationDegrees != 0) {
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                    } else {
                        MenuItem.SHOW_AS_ACTION_IF_ROOM
                    })
        }

        if (visibleBottomActions != 0) {
            updateBottomActionIcons(currentMedium)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (getCurrentMedium() == null)
            return true

        when (item.itemId) {
            R.id.menu_set_as -> setAs(getCurrentPath())
            R.id.menu_slideshow -> initSlideshow()
            R.id.menu_copy_to -> copyMoveTo(true)
            R.id.menu_move_to -> copyMoveTo(false)
            R.id.menu_open_with -> openPath(getCurrentPath(), true)
            R.id.menu_hide -> toggleFileVisibility(true)
            R.id.menu_unhide -> toggleFileVisibility(false)
            R.id.menu_share -> shareMediumPath(getCurrentPath())
            R.id.menu_delete -> checkDeleteConfirmation()
            R.id.menu_rename -> renameFile()
            R.id.menu_edit -> openEditor(getCurrentPath())
            R.id.menu_properties -> showProperties()
            R.id.menu_show_on_map -> showOnMap()
            R.id.menu_rotate_right -> rotateImage(90)
            R.id.menu_rotate_left -> rotateImage(270)
            R.id.menu_rotate_one_eighty -> rotateImage(180)
            R.id.menu_add_to_favorites -> toggleFavorite()
            R.id.menu_remove_from_favorites -> toggleFavorite()
            R.id.menu_restore_file -> restoreFile()
            R.id.menu_force_portrait -> toggleOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            R.id.menu_force_landscape -> toggleOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            R.id.menu_default_orientation -> toggleOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            R.id.menu_save_as -> saveImageAs()
            R.id.menu_settings -> launchSettings()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun updatePagerItems(media: MutableList<Medium>) {
        val pagerAdapter = MyPagerAdapter(this, supportFragmentManager, media)
        if (!isActivityDestroyed()) {
            view_pager.apply {
                adapter = pagerAdapter
                currentItem = mPos
                removeOnPageChangeListener(this@ViewPagerActivity)
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
                if (!isActivityDestroyed()) {
                    hideSystemUI(true)
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
                if (view_pager.isFakeDragging) {
                    try {
                        view_pager.endFakeDrag()
                    } catch (ignored: Exception) {
                        stopSlideshow()
                    }

                    if (view_pager.currentItem == oldPosition) {
                        slideshowEnded(forward)
                    }
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
                    try {
                        view_pager.fakeDragBy(dragOffset * (if (forward) 1f else -1f))
                    } catch (e: Exception) {
                        stopSlideshow()
                    }
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
            mIsSlideshowActive = false
            showSystemUI(true)
            mSlideshowHandler.removeCallbacksAndMessages(null)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun scheduleSwipe() {
        mSlideshowHandler.removeCallbacksAndMessages(null)
        if (mIsSlideshowActive) {
            if (getCurrentMedium()!!.isImage() || getCurrentMedium()!!.isGIF()) {
                mSlideshowHandler.postDelayed({
                    if (mIsSlideshowActive && !isActivityDestroyed()) {
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
        mSlideshowMedia = mMediaFiles.toMutableList()
        if (!config.slideshowIncludePhotos) {
            mSlideshowMedia = mSlideshowMedia.filter { !it.isImage() } as MutableList
        }

        if (!config.slideshowIncludeVideos) {
            mSlideshowMedia = mSlideshowMedia.filter { it.isImage() || it.isGIF() } as MutableList
        }

        if (!config.slideshowIncludeGIFs) {
            mSlideshowMedia = mSlideshowMedia.filter { !it.isGIF() } as MutableList
        }

        if (config.slideshowRandomOrder) {
            mSlideshowMedia.shuffle()
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
        val currPath = getCurrentPath()
        val fileDirItems = arrayListOf(FileDirItem(currPath, currPath.getFilenameFromPath()))
        tryCopyMoveFilesTo(fileDirItems, isCopyOperation) {
            config.tempFolderPath = ""
            if (!isCopyOperation) {
                refreshViewPager()
            }
        }
    }

    private fun toggleFileVisibility(hide: Boolean, callback: (() -> Unit)? = null) {
        toggleFileVisibility(getCurrentPath(), hide) {
            val newFileName = it.getFilenameFromPath()
            supportActionBar?.title = newFileName

            getCurrentMedium()!!.apply {
                name = newFileName
                path = it
                getCurrentMedia()[mPos] = this
            }
            invalidateOptionsMenu()
            callback?.invoke()
        }
    }

    private fun rotateImage(degrees: Int) {
        mRotationDegrees = (mRotationDegrees + degrees) % 360
        getCurrentFragment()?.let {
            (it as? PhotoFragment)?.rotateImageViewBy(mRotationDegrees)
        }
        supportInvalidateOptionsMenu()
    }

    private fun toggleOrientation(orientation: Int) {
        requestedOrientation = orientation
        mIsOrientationLocked = orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        invalidateOptionsMenu()
    }

    private fun getChangeOrientationIcon(): Int {
        return if (mIsOrientationLocked) {
            if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                R.drawable.ic_orientation_portrait
            } else {
                R.drawable.ic_orientation_landscape
            }
        } else {
            R.drawable.ic_orientation_auto
        }
    }

    private fun saveImageAs() {
        val currPath = getCurrentPath()
        SaveAsDialog(this, currPath, false) {
            handleSAFDialog(it) {
                Thread {
                    saveImageToFile(currPath, it)
                }.start()
            }
        }
    }

    private fun saveImageToFile(oldPath: String, newPath: String) {
        toast(R.string.saving)
        if (oldPath == newPath && oldPath.isJpg()) {
            if (tryRotateByExif(oldPath)) {
                return
            }
        }

        val tmpPath = "${filesDir.absolutePath}/.tmp_${newPath.getFilenameFromPath()}"
        val tmpFileDirItem = FileDirItem(tmpPath, tmpPath.getFilenameFromPath())
        try {
            getFileOutputStream(tmpFileDirItem) {
                if (it == null) {
                    toast(R.string.unknown_error_occurred)
                    return@getFileOutputStream
                }

                val oldLastModified = getCurrentFile().lastModified()
                if (oldPath.isJpg()) {
                    copyFile(getCurrentPath(), tmpPath)
                    saveExifRotation(ExifInterface(tmpPath), mRotationDegrees)
                } else {
                    val inputstream = getFileInputStreamSync(oldPath)
                    val bitmap = BitmapFactory.decodeStream(inputstream)
                    saveFile(tmpPath, bitmap, it as FileOutputStream)
                }

                if (getDoesFilePathExist(newPath)) {
                    tryDeleteFileDirItem(FileDirItem(newPath, newPath.getFilenameFromPath()), false, true)
                }

                copyFile(tmpPath, newPath)
                scanPathRecursively(newPath)
                toast(R.string.file_saved)

                if (config.keepLastModified) {
                    File(newPath).setLastModified(oldLastModified)
                    updateLastModified(newPath, oldLastModified)
                }

                it.flush()
                it.close()
                mRotationDegrees = 0
                invalidateOptionsMenu()

                // we cannot refresh a specific image in Glide Cache, so just clear it all
                val glide = Glide.get(applicationContext)
                glide.clearDiskCache()
                runOnUiThread {
                    glide.clearMemory()
                }
            }
        } catch (e: OutOfMemoryError) {
            toast(R.string.out_of_memory_error)
        } catch (e: Exception) {
            showErrorToast(e)
        } finally {
            tryDeleteFileDirItem(tmpFileDirItem, false, true)
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun tryRotateByExif(path: String): Boolean {
        return try {
            if (saveImageRotation(path, mRotationDegrees)) {
                mRotationDegrees = 0
                invalidateOptionsMenu()
                toast(R.string.file_saved)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            showErrorToast(e)
            false
        }
    }

    private fun copyFile(source: String, destination: String) {
        var inputStream: InputStream? = null
        var out: OutputStream? = null
        try {
            out = getFileOutputStreamSync(destination, source.getMimeType())
            inputStream = getFileInputStreamSync(source)
            inputStream?.copyTo(out!!)
        } finally {
            inputStream?.close()
            out?.close()
        }
    }

    private fun saveFile(path: String, bitmap: Bitmap, out: FileOutputStream) {
        val matrix = Matrix()
        matrix.postRotate(mRotationDegrees.toFloat())
        val bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bmp.compress(path.getCompressionFormat(), 90, out)
    }

    private fun isShowHiddenFlagNeeded(): Boolean {
        val file = File(mPath)
        if (file.isHidden) {
            return true
        }

        var parent = file.parentFile ?: return false
        while (true) {
            if (parent.isHidden || parent.list()?.any { it.startsWith(NOMEDIA) } == true) {
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
        val exif: ExifInterface
        try {
            exif = ExifInterface(getCurrentPath())
        } catch (e: Exception) {
            showErrorToast(e)
            return
        }
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
                toast(R.string.no_app_found)
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

    private fun initBottomActionsLayout() {
        bottom_actions.layoutParams.height = resources.getDimension(R.dimen.bottom_actions_height).toInt() + navigationBarHeight
        if (config.bottomActions) {
            bottom_actions.beVisible()
        } else {
            bottom_actions.beGone()
        }
    }

    private fun initBottomActionButtons() {
        val visibleBottomActions = if (config.bottomActions) config.visibleBottomActions else 0
        bottom_favorite.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_TOGGLE_FAVORITE != 0)
        bottom_favorite.setOnClickListener {
            toggleFavorite()
        }

        bottom_edit.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_EDIT != 0 && getCurrentMedium()?.isSVG() == false)
        bottom_edit.setOnClickListener {
            openEditor(getCurrentPath())
        }

        bottom_share.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_SHARE != 0)
        bottom_share.setOnClickListener {
            shareMediumPath(getCurrentPath())
        }

        bottom_delete.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_DELETE != 0)
        bottom_delete.setOnClickListener {
            checkDeleteConfirmation()
        }

        bottom_rotate.setOnClickListener {
            rotateImage(90)
        }

        bottom_properties.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_PROPERTIES != 0)
        bottom_properties.setOnClickListener {
            showProperties()
        }

        bottom_change_orientation.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_CHANGE_ORIENTATION != 0)
        bottom_change_orientation.setOnClickListener {
            requestedOrientation = when (requestedOrientation) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            mIsOrientationLocked = requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            updateBottomActionIcons(getCurrentMedium())
        }

        bottom_slideshow.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_SLIDESHOW != 0)
        bottom_slideshow.setOnClickListener {
            initSlideshow()
        }

        bottom_show_on_map.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_SHOW_ON_MAP != 0)
        bottom_show_on_map.setOnClickListener {
            showOnMap()
        }

        bottom_toggle_file_visibility.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_TOGGLE_VISIBILITY != 0)
        bottom_toggle_file_visibility.setOnClickListener {
            getCurrentMedium()?.apply {
                toggleFileVisibility(!isHidden()) {
                    updateBottomActionIcons(getCurrentMedium())
                }
            }
        }

        bottom_rename.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_RENAME != 0 && getCurrentMedium()?.getIsInRecycleBin() == false)
        bottom_rename.setOnClickListener {
            renameFile()
        }

        bottom_set_as.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_SET_AS != 0)
        bottom_set_as.setOnClickListener {
            setAs(getCurrentPath())
        }
    }

    private fun updateBottomActionIcons(medium: Medium?) {
        if (medium == null) {
            return
        }

        val favoriteIcon = if (medium.isFavorite) R.drawable.ic_star_on else R.drawable.ic_star_off
        bottom_favorite.setImageResource(favoriteIcon)

        val hideIcon = if (medium.isHidden()) R.drawable.ic_unhide else R.drawable.ic_hide
        bottom_toggle_file_visibility.setImageResource(hideIcon)

        bottom_rotate.beVisibleIf(config.visibleBottomActions and BOTTOM_ACTION_ROTATE != 0 && getCurrentMedium()?.isImage() == true)
        bottom_change_orientation.setImageResource(getChangeOrientationIcon())
    }

    private fun toggleFavorite() {
        val medium = getCurrentMedium() ?: return
        medium.isFavorite = !medium.isFavorite
        Thread {
            galleryDB.MediumDao().updateFavorite(medium.path, medium.isFavorite)
            if (medium.isFavorite) {
                mFavoritePaths.add(medium.path)
            } else {
                mFavoritePaths.remove(medium.path)
            }
            invalidateOptionsMenu()
        }.start()
    }

    private fun restoreFile() {
        restoreRecycleBinPath(getCurrentPath()) {
            refreshViewPager()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == REQUEST_EDIT_IMAGE) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                mPos = -1
                mPrevHashcode = 0
                refreshViewPager()
            }
        } else if (requestCode == REQUEST_SET_AS) {
            if (resultCode == Activity.RESULT_OK) {
                toast(R.string.wallpaper_set_successfully)
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun checkDeleteConfirmation() {
        if (getCurrentMedium() == null) {
            return
        }

        if (config.tempSkipDeleteConfirmation || config.skipDeleteConfirmation) {
            deleteConfirmed()
        } else {
            askConfirmDelete()
        }
    }

    private fun askConfirmDelete() {
        val message = if (config.useRecycleBin && !getCurrentMedium()!!.getIsInRecycleBin()) R.string.are_you_sure_recycle_bin else R.string.are_you_sure_delete
        DeleteWithRememberDialog(this, getString(message)) {
            config.tempSkipDeleteConfirmation = it
            deleteConfirmed()
        }
    }

    private fun deleteConfirmed() {
        val path = getCurrentMedia().getOrNull(mPos)?.path ?: return
        if (getIsPathDirectory(path) || !path.isMediaFile()) {
            return
        }

        val fileDirItem = FileDirItem(path, path.getFilenameFromPath())
        if (config.useRecycleBin && !getCurrentMedium()!!.getIsInRecycleBin()) {
            movePathsInRecycleBin(arrayListOf(path)) {
                if (it) {
                    tryDeleteFileDirItem(fileDirItem, false, false) {
                        refreshViewPager()
                    }
                } else {
                    toast(R.string.unknown_error_occurred)
                }
            }
        } else {
            tryDeleteFileDirItem(fileDirItem, false, true) {
                refreshViewPager()
            }
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
        val oldPath = getCurrentPath()
        RenameItemDialog(this, oldPath) {
            getCurrentMedia()[mPos].apply {
                path = it
                name = it.getFilenameFromPath()
            }

            Thread {
                updateDBMediaPath(oldPath, it)
            }.start()
            updateActionbarTitle()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        measureScreen()
        initBottomActionsLayout()
    }

    @SuppressLint("NewApi")
    private fun measureScreen() {
        val metrics = DisplayMetrics()
        if (isJellyBean1Plus()) {
            windowManager.defaultDisplay.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
        } else {
            windowManager.defaultDisplay.getMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
        }
    }

    private fun refreshViewPager() {
        GetMediaAsynctask(applicationContext, mDirectory, false, false, mShowAll) {
            gotMedia(it)
        }.execute()
    }

    private fun gotMedia(thumbnailItems: ArrayList<ThumbnailItem>) {
        val media = thumbnailItems.filter { it is Medium }.map { it as Medium } as ArrayList<Medium>
        if (isDirEmpty(media) || media.hashCode() == mPrevHashcode) {
            return
        }

        mPrevHashcode = media.hashCode()
        mMediaFiles = media
        mPos = if (mPos == -1) {
            getPositionInList(media)
        } else {
            Math.min(mPos, mMediaFiles.size - 1)
        }

        updateActionbarTitle()
        updatePagerItems(mMediaFiles.toMutableList())
        invalidateOptionsMenu()
        checkOrientation()
        initBottomActions()
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
        val fileDirItem = FileDirItem(mDirectory, mDirectory.getFilenameFromPath(), getIsPathDirectory(mDirectory))
        if (config.deleteEmptyFolders && !fileDirItem.isDownloadsFolder() && fileDirItem.isDirectory && fileDirItem.getProperFileCount(applicationContext, true) == 0) {
            tryDeleteFileDirItem(fileDirItem, true, true)
        }

        scanPathRecursively(mDirectory)
    }

    private fun checkOrientation() {
        if (!mIsOrientationLocked && config.screenRotation == ROTATE_BY_ASPECT_RATIO) {
            var flipSides = false
            try {
                val pathToLoad = getCurrentPath()
                val exif = android.media.ExifInterface(pathToLoad)
                val orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, -1)
                flipSides = orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270
            } catch (e: Exception) {
            }
            val res = getCurrentPath().getResolution() ?: return
            val width = if (flipSides) res.y else res.x
            val height = if (flipSides) res.x else res.y
            if (width > height) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else if (width < height) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    override fun fragmentClicked() {
        mIsFullScreen = !mIsFullScreen
        checkSystemUI()
    }

    override fun videoEnded(): Boolean {
        if (mIsSlideshowActive) {
            swipeToNextMedium()
        }
        return mIsSlideshowActive
    }

    override fun goToPrevItem() {
        view_pager.setCurrentItem(view_pager.currentItem - 1, false)
        checkOrientation()
    }

    override fun goToNextItem() {
        view_pager.setCurrentItem(view_pager.currentItem + 1, false)
        checkOrientation()
    }

    private fun checkSystemUI() {
        if (mIsFullScreen) {
            hideSystemUI(true)
        } else {
            stopSlideshow()
            showSystemUI(true)
        }
    }

    private fun updateActionbarTitle() {
        runOnUiThread {
            if (mPos < getCurrentMedia().size) {
                supportActionBar?.title = getCurrentMedia()[mPos].path.getFilenameFromPath()
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

    private fun getCurrentMedia() = if (mAreSlideShowMediaVisible) mSlideshowMedia else mMediaFiles

    private fun getCurrentPath() = getCurrentMedium()?.path ?: ""

    private fun getCurrentFile() = File(getCurrentPath())

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        if (mPos != position) {
            mPos = position
            updateActionbarTitle()
            mRotationDegrees = 0
            invalidateOptionsMenu()
            scheduleSwipe()
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state == ViewPager.SCROLL_STATE_IDLE && getCurrentMedium() != null) {
            checkOrientation()
        }
    }
}
