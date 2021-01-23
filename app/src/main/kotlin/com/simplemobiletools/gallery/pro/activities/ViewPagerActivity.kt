package com.simplemobiletools.gallery.pro.activities

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore.Images
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.print.PrintHelper
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.dialogs.RenameItemDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.gallery.pro.BuildConfig
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.adapters.MyPagerAdapter
import com.simplemobiletools.gallery.pro.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.pro.dialogs.DeleteWithRememberDialog
import com.simplemobiletools.gallery.pro.dialogs.ResizeWithPathDialog
import com.simplemobiletools.gallery.pro.dialogs.SaveAsDialog
import com.simplemobiletools.gallery.pro.dialogs.SlideshowDialog
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.fragments.PhotoFragment
import com.simplemobiletools.gallery.pro.fragments.VideoFragment
import com.simplemobiletools.gallery.pro.fragments.ViewPagerFragment
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.models.Medium
import com.simplemobiletools.gallery.pro.models.ThumbnailItem
import kotlinx.android.synthetic.main.activity_medium.*
import kotlinx.android.synthetic.main.bottom_actions.*
import java.io.File
import java.io.OutputStream
import java.util.*

class ViewPagerActivity : SimpleActivity(), ViewPager.OnPageChangeListener, ViewPagerFragment.FragmentListener {
    private val REQUEST_VIEW_VIDEO = 1

    private var mPath = ""
    private var mDirectory = ""
    private var mIsFullScreen = false
    private var mPos = -1
    private var mShowAll = false
    private var mIsSlideshowActive = false
    private var mPrevHashcode = 0

    private var mSlideshowHandler = Handler()
    private var mSlideshowInterval = SLIDESHOW_DEFAULT_INTERVAL
    private var mSlideshowMoveBackwards = false
    private var mSlideshowMedia = mutableListOf<Medium>()
    private var mAreSlideShowMediaVisible = false

    private var mIsOrientationLocked = false

    private var mMediaFiles = ArrayList<Medium>()
    private var mFavoritePaths = ArrayList<String>()
    private var mIgnoredPaths = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medium)

        window.decorView.setBackgroundColor(config.backgroundColor)
        top_shadow.layoutParams.height = statusBarHeight + actionBarHeight
        checkNotchSupport()
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

    override fun onResume() {
        super.onResume()
        if (!hasPermission(PERMISSION_WRITE_STORAGE)) {
            finish()
            return
        }

        if (config.bottomActions) {
            window.navigationBarColor = Color.TRANSPARENT
        } else {
            setTranslucentNavigation()
        }

        initBottomActions()

        if (config.maxBrightness) {
            val attributes = window.attributes
            attributes.screenBrightness = 1f
            window.attributes = attributes
        }

        setupOrientation()
        invalidateOptionsMenu()

        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val filename = getCurrentMedium()?.name ?: mPath.getFilenameFromPath()
        supportActionBar?.title = filename
        window.statusBarColor = Color.TRANSPARENT
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_viewpager, menu)
        val currentMedium = getCurrentMedium() ?: return true
        currentMedium.isFavorite = mFavoritePaths.contains(currentMedium.path)
        val visibleBottomActions = if (config.bottomActions) config.visibleBottomActions else 0

        val rotationDegrees = getCurrentPhotoFragment()?.mCurrentRotationDegrees ?: 0
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
            findItem(R.id.menu_copy_to).isVisible = visibleBottomActions and BOTTOM_ACTION_COPY == 0
            findItem(R.id.menu_move_to).isVisible = visibleBottomActions and BOTTOM_ACTION_MOVE == 0
            findItem(R.id.menu_save_as).isVisible = rotationDegrees != 0
            findItem(R.id.menu_print).isVisible = currentMedium.isImage() || currentMedium.isRaw()
            findItem(R.id.menu_resize).isVisible = visibleBottomActions and BOTTOM_ACTION_RESIZE == 0 && currentMedium.isImage()
            findItem(R.id.menu_hide).isVisible = !currentMedium.isHidden() && visibleBottomActions and BOTTOM_ACTION_TOGGLE_VISIBILITY == 0 && !currentMedium.getIsInRecycleBin()
            findItem(R.id.menu_unhide).isVisible = currentMedium.isHidden() && visibleBottomActions and BOTTOM_ACTION_TOGGLE_VISIBILITY == 0 && !currentMedium.getIsInRecycleBin()
            findItem(R.id.menu_add_to_favorites).isVisible = !currentMedium.isFavorite && visibleBottomActions and BOTTOM_ACTION_TOGGLE_FAVORITE == 0 && !currentMedium.getIsInRecycleBin()
            findItem(R.id.menu_remove_from_favorites).isVisible = currentMedium.isFavorite && visibleBottomActions and BOTTOM_ACTION_TOGGLE_FAVORITE == 0 && !currentMedium.getIsInRecycleBin()
            findItem(R.id.menu_restore_file).isVisible = currentMedium.path.startsWith(recycleBinPath)
            findItem(R.id.menu_create_shortcut).isVisible = isOreoPlus()
            findItem(R.id.menu_change_orientation).isVisible = rotationDegrees == 0 && visibleBottomActions and BOTTOM_ACTION_CHANGE_ORIENTATION == 0
            findItem(R.id.menu_change_orientation).icon = resources.getDrawable(getChangeOrientationIcon())
            findItem(R.id.menu_rotate).setShowAsAction(
                if (rotationDegrees != 0) {
                    MenuItem.SHOW_AS_ACTION_ALWAYS
                } else {
                    MenuItem.SHOW_AS_ACTION_IF_ROOM
                })
        }

        if (visibleBottomActions != 0) {
            updateBottomActionIcons(currentMedium)
        }

        updateMenuItemColors(menu, baseColor = Color.BLACK)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (getCurrentMedium() == null)
            return true

        when (item.itemId) {
            R.id.menu_set_as -> setAs(getCurrentPath())
            R.id.menu_slideshow -> initSlideshow()
            R.id.menu_copy_to -> copyMoveTo(true)
            R.id.menu_move_to -> moveFileTo()
            R.id.menu_open_with -> openPath(getCurrentPath(), true)
            R.id.menu_hide -> toggleFileVisibility(true)
            R.id.menu_unhide -> toggleFileVisibility(false)
            R.id.menu_share -> shareMediumPath(getCurrentPath())
            R.id.menu_delete -> checkDeleteConfirmation()
            R.id.menu_rename -> renameFile()
            R.id.menu_print -> printFile()
            R.id.menu_edit -> openEditor(getCurrentPath())
            R.id.menu_properties -> showProperties()
            R.id.menu_show_on_map -> showFileOnMap(getCurrentPath())
            R.id.menu_rotate_right -> rotateImage(90)
            R.id.menu_rotate_left -> rotateImage(-90)
            R.id.menu_rotate_one_eighty -> rotateImage(180)
            R.id.menu_add_to_favorites -> toggleFavorite()
            R.id.menu_remove_from_favorites -> toggleFavorite()
            R.id.menu_restore_file -> restoreFile()
            R.id.menu_force_portrait -> toggleOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            R.id.menu_force_landscape -> toggleOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            R.id.menu_default_orientation -> toggleOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            R.id.menu_save_as -> saveImageAs()
            R.id.menu_create_shortcut -> createShortcut()
            R.id.menu_resize -> resizeImage()
            R.id.menu_settings -> launchSettings()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == REQUEST_EDIT_IMAGE && resultCode == Activity.RESULT_OK && resultData != null) {
            mPos = -1
            mPrevHashcode = 0
            refreshViewPager()
        } else if (requestCode == REQUEST_SET_AS && resultCode == Activity.RESULT_OK) {
            toast(R.string.wallpaper_set_successfully)
        } else if (requestCode == REQUEST_VIEW_VIDEO && resultCode == Activity.RESULT_OK && resultData != null) {
            if (resultData.getBooleanExtra(GO_TO_NEXT_ITEM, false)) {
                goToNextItem()
            } else if (resultData.getBooleanExtra(GO_TO_PREV_ITEM, false)) {
                goToPrevItem()
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun initViewPager() {
        val uri = intent.data
        if (uri != null) {
            var cursor: Cursor? = null
            try {
                val proj = arrayOf(Images.Media.DATA)
                cursor = contentResolver.query(uri, proj, null, null, null)
                if (cursor?.moveToFirst() == true) {
                    mPath = cursor.getStringValue(Images.Media.DATA)
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
            mPath = intent.extras!!.getString(REAL_FILE_PATH)!!
        }

        if (mPath.isEmpty()) {
            toast(R.string.unknown_error_occurred)
            finish()
            return
        }

        if (mPath.isPortrait() && getPortraitPath() == "") {
            val newIntent = Intent(this, ViewPagerActivity::class.java)
            newIntent.putExtras(intent!!.extras!!)
            newIntent.putExtra(PORTRAIT_PATH, mPath)
            newIntent.putExtra(PATH, "${mPath.getParentPath().getParentPath()}/${mPath.getFilenameFromPath()}")

            startActivity(newIntent)
            finish()
            return
        }

        if (!getDoesFilePathExist(mPath) && getPortraitPath() == "") {
            finish()
            return
        }

        showSystemUI(true)

        if (intent.getBooleanExtra(SKIP_AUTHENTICATION, false)) {
            initContinue()
        } else {
            handleLockedFolderOpening(mPath.getParentPath()) { success ->
                if (success) {
                    initContinue()
                } else {
                    finish()
                }
            }
        }
    }

    private fun initContinue() {
        if (intent.extras?.containsKey(IS_VIEW_INTENT) == true) {
            if (isShowHiddenFlagNeeded()) {
                if (!config.isHiddenPasswordProtectionOn) {
                    config.temporarilyShowHidden = true
                }
            }

            config.isThirdPartyIntent = true
        }

        val isShowingFavorites = intent.getBooleanExtra(SHOW_FAVORITES, false)
        val isShowingRecycleBin = intent.getBooleanExtra(SHOW_RECYCLE_BIN, false)
        mDirectory = when {
            isShowingFavorites -> FAVORITES
            isShowingRecycleBin -> RECYCLE_BIN
            else -> mPath.getParentPath()
        }
        supportActionBar?.title = mPath.getFilenameFromPath()

        view_pager.onGlobalLayout {
            if (!isDestroyed) {
                if (mMediaFiles.isNotEmpty()) {
                    gotMedia(mMediaFiles as ArrayList<ThumbnailItem>)
                    checkSlideshowOnEnter()
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
                }, HIDE_SYSTEM_UI_DELAY)
            }
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            mIsFullScreen = if (visibility and View.SYSTEM_UI_FLAG_LOW_PROFILE == 0) {
                false
            } else {
                visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            }

            checkSystemUI()
            fullscreenToggled()
        }

        if (intent.action == "com.android.camera.action.REVIEW") {
            ensureBackgroundThread {
                if (mediaDB.getMediaFromPath(mPath).isEmpty()) {
                    val type = when {
                        mPath.isVideoFast() -> TYPE_VIDEOS
                        mPath.isGif() -> TYPE_GIFS
                        mPath.isSvg() -> TYPE_SVGS
                        mPath.isRawFast() -> TYPE_RAWS
                        mPath.isPortrait() -> TYPE_PORTRAITS
                        else -> TYPE_IMAGES
                    }

                    val isFavorite = favoritesDB.isFavorite(mPath)
                    val duration = if (type == TYPE_VIDEOS) getDuration(mPath) ?: 0 else 0
                    val ts = System.currentTimeMillis()
                    val medium = Medium(null, mPath.getFilenameFromPath(), mPath, mPath.getParentPath(), ts, ts, File(mPath).length(), type, duration, isFavorite, 0)
                    mediaDB.insert(medium)
                }
            }
        }
    }

    private fun initBottomActions() {
        initBottomActionButtons()
        initBottomActionsLayout()
    }

    private fun initFavorites() {
        ensureBackgroundThread {
            mFavoritePaths = getFavoritePaths()
        }
    }

    private fun setupOrientation() {
        if (!mIsOrientationLocked) {
            if (config.screenRotation == ROTATE_BY_DEVICE_ROTATION) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else if (config.screenRotation == ROTATE_BY_SYSTEM_SETTING) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    private fun updatePagerItems(media: MutableList<Medium>) {
        val pagerAdapter = MyPagerAdapter(this, supportFragmentManager, media)
        if (!isDestroyed) {
            pagerAdapter.shouldInitFragment = mPos < 5
            view_pager.apply {
                adapter = pagerAdapter
                pagerAdapter.shouldInitFragment = true
                currentItem = mPos
                removeOnPageChangeListener(this@ViewPagerActivity)
                addOnPageChangeListener(this@ViewPagerActivity)
            }
        }
    }

    private fun checkSlideshowOnEnter() {
        if (intent.getBooleanExtra(SLIDESHOW_START_ON_ENTER, false)) {
            initSlideshow()
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
                if (!isDestroyed) {
                    if (config.slideshowAnimation == SLIDESHOW_ANIMATION_FADE) {
                        view_pager.setPageTransformer(false, FadePageTransformer())
                    }

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

    private fun goToNextMedium(forward: Boolean) {
        val oldPosition = view_pager.currentItem
        val newPosition = if (forward) oldPosition + 1 else oldPosition - 1
        if (newPosition == -1 || newPosition > view_pager.adapter!!.count - 1) {
            slideshowEnded(forward)
        } else {
            view_pager.setCurrentItem(newPosition, false)
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

        if (config.slideshowAnimation == SLIDESHOW_ANIMATION_SLIDE) {
            animator.interpolator = DecelerateInterpolator()
            animator.duration = SLIDESHOW_SLIDE_DURATION
        } else {
            animator.duration = SLIDESHOW_FADE_DURATION
        }

        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            var oldDragPosition = 0
            override fun onAnimationUpdate(animation: ValueAnimator) {
                if (view_pager?.isFakeDragging == true) {
                    val dragPosition = animation.animatedValue as Int
                    val dragOffset = dragPosition - oldDragPosition
                    oldDragPosition = dragPosition
                    try {
                        view_pager.fakeDragBy(dragOffset * (if (forward) -1f else 1f))
                    } catch (e: Exception) {
                        stopSlideshow()
                    }
                }
            }
        })

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
            view_pager.setPageTransformer(false, DefaultPageTransformer())
            mIsSlideshowActive = false
            showSystemUI(true)
            mSlideshowHandler.removeCallbacksAndMessages(null)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun scheduleSwipe() {
        mSlideshowHandler.removeCallbacksAndMessages(null)
        if (mIsSlideshowActive) {
            if (getCurrentMedium()!!.isImage() || getCurrentMedium()!!.isGIF() || getCurrentMedium()!!.isPortrait()) {
                mSlideshowHandler.postDelayed({
                    if (mIsSlideshowActive && !isDestroyed) {
                        swipeToNextMedium()
                    }
                }, mSlideshowInterval * 1000L)
            } else {
                (getCurrentFragment() as? VideoFragment)!!.playVideo()
            }
        }
    }

    private fun swipeToNextMedium() {
        if (config.slideshowAnimation == SLIDESHOW_ANIMATION_NONE) {
            goToNextMedium(!mSlideshowMoveBackwards)
        } else {
            animatePagerTransition(!mSlideshowMoveBackwards)
        }
    }

    private fun getMediaForSlideshow(): Boolean {
        mSlideshowMedia = mMediaFiles.filter {
            it.isImage() || it.isPortrait() || (config.slideshowIncludeVideos && it.isVideo() || (config.slideshowIncludeGIFs && it.isGIF()))
        }.toMutableList()

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

    private fun moveFileTo() {
        handleDeletePasswordProtection {
            copyMoveTo(false)
        }
    }

    private fun copyMoveTo(isCopyOperation: Boolean) {
        val currPath = getCurrentPath()
        if (!isCopyOperation && currPath.startsWith(recycleBinPath)) {
            toast(R.string.moving_recycle_bin_items_disabled, Toast.LENGTH_LONG)
            return
        }

        val fileDirItems = arrayListOf(FileDirItem(currPath, currPath.getFilenameFromPath()))
        tryCopyMoveFilesTo(fileDirItems, isCopyOperation) {
            val newPath = "$it/${currPath.getFilenameFromPath()}"
            rescanPaths(arrayListOf(newPath)) {
                fixDateTaken(arrayListOf(newPath), false)
            }

            config.tempFolderPath = ""
            if (!isCopyOperation) {
                refreshViewPager()
                updateFavoritePaths(fileDirItems, it)
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
        val currentPath = getCurrentPath()
        if (needsStupidWritePermissions(currentPath)) {
            handleSAFDialog(currentPath) {
                if (it) {
                    rotateBy(degrees)
                }
            }
        } else {
            rotateBy(degrees)
        }
    }

    private fun rotateBy(degrees: Int) {
        getCurrentPhotoFragment()?.rotateImageViewBy(degrees)
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
                R.drawable.ic_orientation_portrait_vector
            } else {
                R.drawable.ic_orientation_landscape_vector
            }
        } else {
            R.drawable.ic_orientation_auto_vector
        }
    }

    private fun saveImageAs() {
        val currPath = getCurrentPath()
        SaveAsDialog(this, currPath, false) {
            val newPath = it
            handleSAFDialog(it) {
                if (!it) {
                    return@handleSAFDialog
                }

                toast(R.string.saving)
                ensureBackgroundThread {
                    val photoFragment = getCurrentPhotoFragment() ?: return@ensureBackgroundThread
                    saveRotatedImageToFile(currPath, newPath, photoFragment.mCurrentRotationDegrees, true) {
                        toast(R.string.file_saved)
                        getCurrentPhotoFragment()?.mCurrentRotationDegrees = 0
                        invalidateOptionsMenu()
                    }
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private fun createShortcut() {
        val manager = getSystemService(ShortcutManager::class.java)
        if (manager.isRequestPinShortcutSupported) {
            val medium = getCurrentMedium() ?: return
            val path = medium.path
            val drawable = resources.getDrawable(R.drawable.shortcut_image).mutate()
            getShortcutImage(path, drawable) {
                val intent = Intent(this, ViewPagerActivity::class.java).apply {
                    putExtra(PATH, path)
                    putExtra(SHOW_ALL, config.showAll)
                    putExtra(SHOW_FAVORITES, path == FAVORITES)
                    putExtra(SHOW_RECYCLE_BIN, path == RECYCLE_BIN)
                    action = Intent.ACTION_VIEW
                    flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                val shortcut = ShortcutInfo.Builder(this, path)
                    .setShortLabel(medium.name)
                    .setIcon(Icon.createWithBitmap(drawable.convertToBitmap()))
                    .setIntent(intent)
                    .build()

                manager.requestPinShortcut(shortcut, null)
            }
        }
    }

    private fun getCurrentPhotoFragment() = getCurrentFragment() as? PhotoFragment

    private fun getPortraitPath() = intent.getStringExtra(PORTRAIT_PATH) ?: ""

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

    private fun getCurrentFragment() = (view_pager.adapter as? MyPagerAdapter)?.getCurrentFragment(view_pager.currentItem)

    private fun showProperties() {
        if (getCurrentMedium() != null) {
            PropertiesDialog(this, getCurrentPath(), false)
        }
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
        val currentMedium = getCurrentMedium()
        val visibleBottomActions = if (config.bottomActions) config.visibleBottomActions else 0
        bottom_favorite.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_TOGGLE_FAVORITE != 0 && currentMedium?.getIsInRecycleBin() == false)
        bottom_favorite.setOnClickListener {
            toggleFavorite()
        }

        bottom_edit.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_EDIT != 0 && currentMedium?.isSVG() == false)
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
            updateBottomActionIcons(currentMedium)
        }

        bottom_slideshow.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_SLIDESHOW != 0)
        bottom_slideshow.setOnClickListener {
            initSlideshow()
        }

        bottom_show_on_map.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_SHOW_ON_MAP != 0)
        bottom_show_on_map.setOnClickListener {
            showFileOnMap(getCurrentPath())
        }

        bottom_toggle_file_visibility.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_TOGGLE_VISIBILITY != 0)
        bottom_toggle_file_visibility.setOnClickListener {
            currentMedium?.apply {
                toggleFileVisibility(!isHidden()) {
                    updateBottomActionIcons(currentMedium)
                }
            }
        }

        bottom_rename.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_RENAME != 0 && currentMedium?.getIsInRecycleBin() == false)
        bottom_rename.setOnClickListener {
            renameFile()
        }

        bottom_set_as.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_SET_AS != 0)
        bottom_set_as.setOnClickListener {
            setAs(getCurrentPath())
        }

        bottom_copy.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_COPY != 0)
        bottom_copy.setOnClickListener {
            copyMoveTo(true)
        }

        bottom_move.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_MOVE != 0)
        bottom_move.setOnClickListener {
            moveFileTo()
        }

        bottom_resize.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_RESIZE != 0 && currentMedium?.isImage() == true)
        bottom_resize.setOnClickListener {
            resizeImage()
        }
    }

    private fun updateBottomActionIcons(medium: Medium?) {
        if (medium == null) {
            return
        }

        val favoriteIcon = if (medium.isFavorite) R.drawable.ic_star_on_vector else R.drawable.ic_star_off_vector
        bottom_favorite.setImageResource(favoriteIcon)

        val hideIcon = if (medium.isHidden()) R.drawable.ic_unhide_vector else R.drawable.ic_hide
        bottom_toggle_file_visibility.setImageResource(hideIcon)

        bottom_rotate.beVisibleIf(config.visibleBottomActions and BOTTOM_ACTION_ROTATE != 0 && getCurrentMedium()?.isImage() == true)
        bottom_change_orientation.setImageResource(getChangeOrientationIcon())
    }

    private fun toggleFavorite() {
        val medium = getCurrentMedium() ?: return
        medium.isFavorite = !medium.isFavorite
        ensureBackgroundThread {
            updateFavorite(medium.path, medium.isFavorite)
            if (medium.isFavorite) {
                mFavoritePaths.add(medium.path)
            } else {
                mFavoritePaths.remove(medium.path)
            }
            invalidateOptionsMenu()
        }
    }

    private fun printFile() {
        sendPrintIntent(getCurrentPath())
    }

    private fun sendPrintIntent(path: String) {
        val printHelper = PrintHelper(this)
        printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
        printHelper.orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        try {
            val resolution = path.getImageResolution()
            if (resolution == null) {
                toast(R.string.unknown_error_occurred)
                return
            }

            var requestedWidth = resolution.x
            var requestedHeight = resolution.y

            if (requestedWidth >= MAX_PRINT_SIDE_SIZE) {
                requestedHeight = (requestedHeight / (requestedWidth / MAX_PRINT_SIDE_SIZE.toFloat())).toInt()
                requestedWidth = MAX_PRINT_SIDE_SIZE
            } else if (requestedHeight >= MAX_PRINT_SIDE_SIZE) {
                requestedWidth = (requestedWidth / (requestedHeight / MAX_PRINT_SIDE_SIZE.toFloat())).toInt()
                requestedHeight = MAX_PRINT_SIDE_SIZE
            }

            val options = RequestOptions()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)

            Glide.with(this)
                .asBitmap()
                .load(path)
                .apply(options)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                        showErrorToast(e?.localizedMessage ?: "")
                        return false
                    }

                    override fun onResourceReady(bitmap: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        if (bitmap != null) {
                            printHelper.printBitmap(path.getFilenameFromPath(), bitmap)
                        }

                        return false
                    }
                }).submit(requestedWidth, requestedHeight)
        } catch (e: Exception) {
        }
    }

    private fun restoreFile() {
        restoreRecycleBinPath(getCurrentPath()) {
            refreshViewPager()
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun resizeImage() {
        val oldPath = getCurrentPath()
        val originalSize = oldPath.getImageResolution() ?: return
        ResizeWithPathDialog(this, originalSize, oldPath) { newSize, newPath ->
            ensureBackgroundThread {
                try {
                    var oldExif: ExifInterface? = null
                    if (isNougatPlus()) {
                        val inputStream = contentResolver.openInputStream(Uri.fromFile(File(oldPath)))
                        oldExif = ExifInterface(inputStream!!)
                    }

                    val newBitmap = Glide.with(applicationContext).asBitmap().load(oldPath).submit(newSize.x, newSize.y).get()

                    val newFile = File(newPath)
                    val newFileDirItem = FileDirItem(newPath, newPath.getFilenameFromPath())
                    getFileOutputStream(newFileDirItem, true) {
                        if (it != null) {
                            saveBitmap(newFile, newBitmap, it, oldExif, File(oldPath).lastModified())
                        } else {
                            toast(R.string.image_editing_failed)
                        }
                    }
                } catch (e: OutOfMemoryError) {
                    toast(R.string.out_of_memory_error)
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun saveBitmap(file: File, bitmap: Bitmap, out: OutputStream, oldExif: ExifInterface?, lastModified: Long) {
        try {
            bitmap.compress(file.absolutePath.getCompressionFormat(), 90, out)

            if (isNougatPlus()) {
                val newExif = ExifInterface(file.absolutePath)
                oldExif?.copyNonDimensionAttributesTo(newExif)
            }
        } catch (e: Exception) {
        }

        toast(R.string.file_saved)
        val paths = arrayListOf(file.absolutePath)
        rescanPaths(paths) {
            fixDateTaken(paths, false)

            if (config.keepLastModified) {
                File(file.absolutePath).setLastModified(lastModified)
                updateLastModified(file.absolutePath, lastModified)
            }
        }
        out.close()
    }

    private fun checkDeleteConfirmation() {
        if (getCurrentMedium() == null) {
            return
        }

        if (config.isDeletePasswordProtectionOn) {
            handleDeletePasswordProtection {
                deleteConfirmed()
            }
        } else if (config.tempSkipDeleteConfirmation || config.skipDeleteConfirmation) {
            deleteConfirmed()
        } else {
            askConfirmDelete()
        }
    }

    private fun askConfirmDelete() {
        val filename = "\"${getCurrentPath().getFilenameFromPath()}\""

        val baseString = if (config.useRecycleBin && !getCurrentMedium()!!.getIsInRecycleBin()) {
            R.string.move_to_recycle_bin_confirmation
        } else {
            R.string.deletion_confirmation
        }

        val message = String.format(resources.getString(baseString), filename)
        DeleteWithRememberDialog(this, message) {
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
            mIgnoredPaths.add(fileDirItem.path)
            val media = mMediaFiles.filter { !mIgnoredPaths.contains(it.path) } as ArrayList<ThumbnailItem>
            runOnUiThread {
                gotMedia(media, true)
            }

            movePathsInRecycleBin(arrayListOf(path)) {
                if (it) {
                    tryDeleteFileDirItem(fileDirItem, false, false) {
                        mIgnoredPaths.remove(fileDirItem.path)
                        deleteDirectoryIfEmpty()
                    }
                } else {
                    toast(R.string.unknown_error_occurred)
                }
            }
        } else {
            handleDeletion(fileDirItem)
        }
    }

    private fun handleDeletion(fileDirItem: FileDirItem) {
        mIgnoredPaths.add(fileDirItem.path)
        val media = mMediaFiles.filter { !mIgnoredPaths.contains(it.path) } as ArrayList<ThumbnailItem>
        runOnUiThread {
            gotMedia(media, true)
        }

        tryDeleteFileDirItem(fileDirItem, false, true) {
            mIgnoredPaths.remove(fileDirItem.path)
            deleteDirectoryIfEmpty()
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

            ensureBackgroundThread {
                updateDBMediaPath(oldPath, it)
            }
            updateActionbarTitle()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        initBottomActionsLayout()
    }

    private fun refreshViewPager() {
        if (config.getFolderSorting(mDirectory) and SORT_BY_RANDOM == 0) {
            GetMediaAsynctask(applicationContext, mDirectory, false, false, mShowAll) {
                gotMedia(it)
            }.execute()
        }
    }

    private fun gotMedia(thumbnailItems: ArrayList<ThumbnailItem>, ignorePlayingVideos: Boolean = false) {
        val media = thumbnailItems.asSequence().filter { it is Medium && !mIgnoredPaths.contains(it.path) }.map { it as Medium }.toMutableList() as ArrayList<Medium>
        if (isDirEmpty(media) || media.hashCode() == mPrevHashcode) {
            return
        }

        if (!ignorePlayingVideos && (getCurrentFragment() as? VideoFragment)?.mIsPlaying == true) {
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
            val portraitPath = getPortraitPath()
            if (portraitPath != "") {
                val portraitPaths = File(portraitPath).parentFile?.list()
                if (portraitPaths != null) {
                    for (path in portraitPaths) {
                        if (medium.name == path) {
                            return i
                        }
                    }
                }
            } else if (medium.path == mPath) {
                return i
            }
        }
        return mPos
    }

    private fun deleteDirectoryIfEmpty() {
        val fileDirItem = FileDirItem(mDirectory, mDirectory.getFilenameFromPath(), File(mDirectory).isDirectory)
        if (config.deleteEmptyFolders && !fileDirItem.isDownloadsFolder() && fileDirItem.isDirectory && fileDirItem.getProperFileCount(this, true) == 0) {
            tryDeleteFileDirItem(fileDirItem, true, true)
            scanPathRecursively(mDirectory)
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun checkOrientation() {
        if (!mIsOrientationLocked && config.screenRotation == ROTATE_BY_ASPECT_RATIO) {
            var flipSides = false
            try {
                val pathToLoad = getCurrentPath()
                val exif = ExifInterface(pathToLoad)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
                flipSides = orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270
            } catch (e: Exception) {
            }
            val resolution = applicationContext.getResolution(getCurrentPath()) ?: return
            val width = if (flipSides) resolution.y else resolution.x
            val height = if (flipSides) resolution.x else resolution.y
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
        fullscreenToggled()
    }

    override fun videoEnded(): Boolean {
        if (mIsSlideshowActive) {
            swipeToNextMedium()
        }
        return mIsSlideshowActive
    }

    override fun isSlideShowActive() = mIsSlideshowActive

    override fun goToPrevItem() {
        view_pager.setCurrentItem(view_pager.currentItem - 1, false)
        checkOrientation()
    }

    override fun goToNextItem() {
        view_pager.setCurrentItem(view_pager.currentItem + 1, false)
        checkOrientation()
    }

    override fun launchViewVideoIntent(path: String) {
        ensureBackgroundThread {
            val newUri = getFinalUriFromPath(path, BuildConfig.APPLICATION_ID) ?: return@ensureBackgroundThread
            val mimeType = getUriMimeType(path, newUri)
            Intent().apply {
                action = Intent.ACTION_VIEW
                setDataAndType(newUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(IS_FROM_GALLERY, true)
                putExtra(REAL_FILE_PATH, path)
                putExtra(SHOW_PREV_ITEM, view_pager.currentItem != 0)
                putExtra(SHOW_NEXT_ITEM, view_pager.currentItem != mMediaFiles.size - 1)

                if (resolveActivity(packageManager) != null) {
                    try {
                        startActivityForResult(this, REQUEST_VIEW_VIDEO)
                    } catch (e: NullPointerException) {
                        showErrorToast(e)
                    }
                } else {
                    if (!tryGenericMimeType(this, mimeType, newUri)) {
                        toast(R.string.no_app_found)
                    }
                }
            }
        }
    }

    private fun checkSystemUI() {
        if (mIsFullScreen) {
            hideSystemUI(true)
        } else {
            stopSlideshow()
            showSystemUI(true)
        }
    }

    private fun fullscreenToggled() {
        view_pager.adapter?.let {
            (it as MyPagerAdapter).toggleFullscreen(mIsFullScreen)
            val newAlpha = if (mIsFullScreen) 0f else 1f
            top_shadow.animate().alpha(newAlpha).start()
            if (bottom_actions.isVisible()) {
                bottom_actions.animate().alpha(newAlpha).start()
                arrayOf(bottom_favorite, bottom_edit, bottom_share, bottom_delete, bottom_rotate, bottom_properties, bottom_change_orientation,
                    bottom_slideshow, bottom_show_on_map, bottom_toggle_file_visibility, bottom_rename, bottom_set_as, bottom_copy, bottom_move,
                    bottom_resize).forEach {
                    it.isClickable = !mIsFullScreen
                }
            }
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

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        if (mPos != position) {
            mPos = position
            updateActionbarTitle()
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
