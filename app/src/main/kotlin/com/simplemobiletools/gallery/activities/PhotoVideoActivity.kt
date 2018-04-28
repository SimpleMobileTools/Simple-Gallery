package com.simplemobiletools.gallery.activities

import android.content.Intent
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
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.fragments.PhotoFragment
import com.simplemobiletools.gallery.fragments.VideoFragment
import com.simplemobiletools.gallery.fragments.ViewPagerFragment
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.fragment_holder.*
import java.io.File

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
        setTranslucentNavigation()

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
        supportActionBar?.setBackgroundDrawable(resources.getDrawable(R.drawable.actionbar_gradient_background))
        if (config.blackBackground) {
            updateStatusbarColor(Color.BLACK)
        }
    }

    private fun checkIntent(savedInstanceState: Bundle? = null) {
        mUri = intent.data ?: return
        if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            val realPath = intent.extras.getString(REAL_FILE_PATH)
            sendViewPagerIntent(realPath)
            finish()
            return
        }

        mIsFromGallery = intent.getBooleanExtra(IS_FROM_GALLERY, false)
        if (mUri!!.scheme == "file") {
            scanPath(mUri!!.path)
            sendViewPagerIntent(mUri!!.path)
            finish()
            return
        } else {
            val path = applicationContext.getRealPathFromURI(mUri!!) ?: ""
            if (path != mUri.toString() && path.isNotEmpty() && mUri!!.authority != "mms") {
                scanPath(mUri!!.path)
                sendViewPagerIntent(path)
                finish()
                return
            }
        }

        showSystemUI()
        val bundle = Bundle()
        val file = File(mUri.toString())
        val type = when {
            file.isImageFast() -> TYPE_IMAGES
            file.isVideoFast() -> TYPE_VIDEOS
            else -> TYPE_GIFS
        }

        mMedium = Medium(null, getFilenameFromUri(mUri!!), mUri.toString(), mUri!!.path.getParentPath(), 0, 0, file.length(), type)
        supportActionBar?.title = mMedium!!.name
        bundle.putSerializable(MEDIUM, mMedium)

        if (savedInstanceState == null) {
            mFragment = if (mIsVideo) VideoFragment() else PhotoFragment()
            mFragment!!.listener = this
            mFragment!!.arguments = bundle
            supportFragmentManager.beginTransaction().replace(R.id.fragment_holder, mFragment).commit()
        }

        if (config.blackBackground) {
            fragment_holder.background = ColorDrawable(Color.BLACK)
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            val isFullscreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            mFragment?.fullscreenToggled(isFullscreen)
        }
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

        menu.apply {
            findItem(R.id.menu_set_as).isVisible = mMedium?.isImage() == true
            findItem(R.id.menu_edit).isVisible = mMedium?.isImage() == true && mUri?.scheme == "file"
            findItem(R.id.menu_properties).isVisible = mUri?.scheme == "file"
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mMedium == null) {
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

    override fun fragmentClicked() {
        mIsFullScreen = !mIsFullScreen
        if (mIsFullScreen) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    override fun videoEnded() = false

    override fun goToPrevItem() {}

    override fun goToNextItem() {}
}
