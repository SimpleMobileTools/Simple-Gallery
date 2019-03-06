package com.simplemobiletools.gallery.pro.activities

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.IS_FROM_GALLERY
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
import com.simplemobiletools.commons.helpers.SIDELOADING_TRUE
import com.simplemobiletools.gallery.pro.BuildConfig
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.fragments.PhotoFragment
import com.simplemobiletools.gallery.pro.fragments.VideoFragment
import com.simplemobiletools.gallery.pro.fragments.ViewPagerFragment
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.models.Medium
import kotlinx.android.synthetic.main.bottom_actions.*
import kotlinx.android.synthetic.main.fragment_holder.*
import java.io.File
import java.io.FileInputStream

open class PhotoVideoActivity : SimpleActivity(), ViewPagerFragment.FragmentListener {
    private var mMedium: Medium? = null
    private var mIsFullScreen = false
    private var mIsFromGallery = false
    private var mFragment: ViewPagerFragment? = null
    private var mUri: Uri? = null

    var mIsVideo = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_holder)

        if (config.appSideloadingStatus == SIDELOADING_TRUE) {
            showSideloadingDialog()
            return
        }

        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                checkIntent(savedInstanceState)
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.statusBarColor = Color.TRANSPARENT

        if (config.bottomActions) {
            window.navigationBarColor = Color.TRANSPARENT
        } else {
            setTranslucentNavigation()
        }

        if (config.blackBackground) {
            updateStatusbarColor(Color.BLACK)
        }
    }

    private fun checkIntent(savedInstanceState: Bundle? = null) {
        mUri = intent.data ?: return
        val uri = mUri.toString()
        if (uri.startsWith("content:/") && uri.contains("/storage/")) {
            val guessedPath = uri.substring(uri.indexOf("/storage/"))
            if (File(guessedPath).exists()) {
                val extras = intent.extras ?: Bundle()
                extras.apply {
                    putString(REAL_FILE_PATH, guessedPath)
                    intent.putExtras(this)
                }
            }
        }

        var filename = getFilenameFromUri(mUri!!)
        mIsFromGallery = intent.getBooleanExtra(IS_FROM_GALLERY, false)
        if (mIsFromGallery && filename.isVideoFast() && config.openVideosOnSeparateScreen) {
            launchVideoPlayer()
            return
        }

        if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            val realPath = intent.extras!!.getString(REAL_FILE_PATH)
            if (realPath != null) {
                if (realPath.getFilenameFromPath().contains('.') || filename.contains('.')) {
                    sendViewPagerIntent(realPath)
                    finish()
                    return
                } else {
                    filename = realPath.getFilenameFromPath()
                }
            }
        }

        if (mUri!!.scheme == "file") {
            if (filename.contains('.')) {
                scanPathRecursively(mUri!!.path)
                sendViewPagerIntent(mUri!!.path)
                finish()
                return
            }
        } else {
            val path = applicationContext.getRealPathFromURI(mUri!!) ?: ""
            if (path != mUri.toString() && path.isNotEmpty() && mUri!!.authority != "mms" && filename.contains('.')) {
                scanPathRecursively(mUri!!.path)
                sendViewPagerIntent(path)
                finish()
                return
            }
        }

        checkNotchSupport()
        showSystemUI(true)
        val bundle = Bundle()
        val file = File(mUri.toString())
        val type = when {
            filename.isVideoFast() -> TYPE_VIDEOS
            filename.isGif() -> TYPE_GIFS
            filename.isRawFast() -> TYPE_RAWS
            filename.isSvg() -> TYPE_SVGS
            else -> TYPE_IMAGES
        }

        mIsVideo = type == TYPE_VIDEOS
        mMedium = Medium(null, filename, mUri.toString(), mUri!!.path.getParentPath(), 0, 0, file.length(), type, 0, false, 0L)
        supportActionBar?.title = mMedium!!.name
        bundle.putSerializable(MEDIUM, mMedium)

        if (savedInstanceState == null) {
            mFragment = if (mIsVideo) VideoFragment() else PhotoFragment()
            mFragment!!.listener = this
            mFragment!!.arguments = bundle
            supportFragmentManager.beginTransaction().replace(R.id.fragment_placeholder, mFragment!!).commit()
        }

        if (config.blackBackground) {
            fragment_holder.background = ColorDrawable(Color.BLACK)
        }

        if (config.maxBrightness) {
            val attributes = window.attributes
            attributes.screenBrightness = 1f
            window.attributes = attributes
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            val isFullscreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            mFragment?.fullscreenToggled(isFullscreen)
        }

        initBottomActions()
    }

    private fun launchVideoPlayer() {
        val newUri = getFinalUriFromPath(mUri.toString(), BuildConfig.APPLICATION_ID)
        if (newUri == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        var isPanorama = false
        val realPath = intent?.extras?.getString(REAL_FILE_PATH) ?: ""
        try {
            if (realPath.isNotEmpty()) {
                val fis = FileInputStream(File(realPath))
                parseFileChannel(realPath, fis.channel, 0, 0, 0) {
                    isPanorama = true
                }
            }
        } catch (ignored: Exception) {
        } catch (ignored: OutOfMemoryError) {
        }

        if (isPanorama) {
            Intent(applicationContext, PanoramaVideoActivity::class.java).apply {
                putExtra(PATH, realPath)
                startActivity(this)
            }
        } else {
            val mimeType = getUriMimeType(mUri.toString(), newUri)
            Intent(applicationContext, VideoPlayerActivity::class.java).apply {
                setDataAndType(newUri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                if (intent.extras != null) {
                    putExtras(intent.extras!!)
                }

                startActivity(this)
            }
        }
        finish()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        initBottomActionsLayout()
    }

    private fun sendViewPagerIntent(path: String) {
        Intent(this, ViewPagerActivity::class.java).apply {
            putExtra(IS_VIEW_INTENT, true)
            putExtra(IS_FROM_GALLERY, mIsFromGallery)
            putExtra(PATH, path)
            startActivity(this)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.photo_video_menu, menu)
        val visibleBottomActions = if (config.bottomActions) config.visibleBottomActions else 0

        menu.apply {
            findItem(R.id.menu_set_as).isVisible = mMedium?.isImage() == true && visibleBottomActions and BOTTOM_ACTION_SET_AS == 0
            findItem(R.id.menu_edit).isVisible = mMedium?.isImage() == true && mUri?.scheme == "file" && visibleBottomActions and BOTTOM_ACTION_EDIT == 0
            findItem(R.id.menu_properties).isVisible = mUri?.scheme == "file" && visibleBottomActions and BOTTOM_ACTION_PROPERTIES == 0
            findItem(R.id.menu_share).isVisible = visibleBottomActions and BOTTOM_ACTION_SHARE == 0
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mMedium == null || mUri == null) {
            return true
        }

        when (item.itemId) {
            R.id.menu_set_as -> setAs(mUri!!.toString())
            R.id.menu_open_with -> openPath(mUri!!.toString(), true)
            R.id.menu_share -> sharePath(mUri!!.toString())
            R.id.menu_edit -> openEditor(mUri!!.toString())
            R.id.menu_properties -> showProperties()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showProperties() {
        PropertiesDialog(this, mUri!!.path)
    }

    private fun initBottomActions() {
        initBottomActionsLayout()
        initBottomActionButtons()
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
        arrayListOf(bottom_favorite, bottom_delete, bottom_rotate, bottom_properties, bottom_change_orientation, bottom_slideshow, bottom_show_on_map,
                bottom_toggle_file_visibility, bottom_rename, bottom_copy, bottom_move).forEach {
            it.beGone()
        }

        val visibleBottomActions = if (config.bottomActions) config.visibleBottomActions else 0
        bottom_edit.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_EDIT != 0 && mMedium?.isImage() == true)
        bottom_edit.setOnClickListener {
            if (mUri != null && bottom_actions.alpha == 1f) {
                openEditor(mUri!!.toString())
            }
        }

        bottom_share.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_SHARE != 0)
        bottom_share.setOnClickListener {
            if (mUri != null && bottom_actions.alpha == 1f) {
                sharePath(mUri!!.toString())
            }
        }

        bottom_set_as.beVisibleIf(visibleBottomActions and BOTTOM_ACTION_SET_AS != 0 && mMedium?.isImage() == true)
        bottom_set_as.setOnClickListener {
            setAs(mUri!!.toString())
        }
    }

    override fun fragmentClicked() {
        mIsFullScreen = !mIsFullScreen
        if (mIsFullScreen) {
            hideSystemUI(true)
        } else {
            showSystemUI(true)
        }

        val newAlpha = if (mIsFullScreen) 0f else 1f
        top_shadow.animate().alpha(newAlpha).start()
        if (!bottom_actions.isGone()) {
            bottom_actions.animate().alpha(newAlpha).start()
        }
    }

    override fun videoEnded() = false

    override fun goToPrevItem() {}

    override fun goToNextItem() {}

    override fun launchViewVideoIntent(path: String) {}
}
