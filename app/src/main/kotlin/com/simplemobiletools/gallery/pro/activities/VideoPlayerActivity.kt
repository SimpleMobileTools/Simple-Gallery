package com.simplemobiletools.gallery.pro.activities

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import com.simplemobiletools.commons.extensions.getFilenameFromUri
import com.simplemobiletools.commons.extensions.getParentPath
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.isPiePlus
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.fragments.VideoFragment
import com.simplemobiletools.gallery.pro.fragments.ViewPagerFragment
import com.simplemobiletools.gallery.pro.helpers.MEDIUM
import com.simplemobiletools.gallery.pro.helpers.TYPE_VIDEOS
import com.simplemobiletools.gallery.pro.models.Medium
import kotlinx.android.synthetic.main.activity_video_player.*
import java.io.File

open class VideoPlayerActivity : SimpleActivity(), ViewPagerFragment.FragmentListener {

    private var mMedium: Medium? = null
    private var mIsFullScreen = false
    private var mCurrentOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    private var mFragment: ViewPagerFragment? = null
    private var mUri: Uri? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

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
        top_shadow.layoutParams.height = statusBarHeight + actionBarHeight
        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    private fun checkIntent(savedInstanceState: Bundle? = null) {
        mUri = intent.data ?: return
        val filename = getFilenameFromUri(mUri!!)

        if (isPiePlus()) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }

        showSystemUI(true)
        val bundle = Bundle()
        val file = File(mUri.toString())
        val type = TYPE_VIDEOS

        mMedium = Medium(null, filename, mUri.toString(), mUri!!.path.getParentPath(), 0, 0, file.length(), type, 0, false, 0L)
        supportActionBar?.title = mMedium!!.name
        bundle.putSerializable(MEDIUM, mMedium)

        if (savedInstanceState == null) {
            mFragment = VideoFragment()
            mFragment!!.listener = this
            mFragment!!.arguments = bundle
            supportFragmentManager.beginTransaction().replace(R.id.fragment_placeholder, mFragment!!).commit()
        }

        if (config.blackBackground) {
            fragment_holder.background = ColorDrawable(Color.BLACK)
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            val isFullscreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            mFragment?.fullscreenToggled(isFullscreen)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_video_player, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mMedium == null || mUri == null) {
            return true
        }

        when (item.itemId) {
            R.id.menu_force_portrait -> forceOrientation(true)
            R.id.menu_force_landscape -> forceOrientation(false)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun forceOrientation(portrait: Boolean) {

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
    }

    override fun videoEnded() = false

    override fun goToPrevItem() {}

    override fun goToNextItem() {}
}
