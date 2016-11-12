package com.simplemobiletools.gallery.activities

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.ActionBar
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.filepicker.extensions.getFilenameFromPath
import com.simplemobiletools.gallery.Constants
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.Utils
import com.simplemobiletools.gallery.fragments.PhotoFragment
import com.simplemobiletools.gallery.fragments.VideoFragment
import com.simplemobiletools.gallery.fragments.ViewPagerFragment
import com.simplemobiletools.gallery.models.Medium
import java.io.File

open class PhotoVideoActivity : SimpleActivity(), ViewPagerFragment.FragmentClickListener {
    companion object {
        private var mActionbar: ActionBar? = null
        private var mUri: Uri? = null
        private var mFragment: ViewPagerFragment? = null

        private var mIsFullScreen = false
        var mIsVideo = false
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_holder)

        mUri = intent.data ?: return

        mActionbar = supportActionBar
        mIsFullScreen = true
        hideSystemUI()

        val bundle = Bundle()
        val file = File(mUri!!.toString())
        val medium = Medium(file.name, mUri!!.toString(), mIsVideo, 0, file.length())
        bundle.putSerializable(Constants.MEDIUM, medium)

        if (savedInstanceState == null) {
            mFragment = if (mIsVideo) VideoFragment() else PhotoFragment()
            mFragment!!.setListener(this)
            mFragment!!.arguments = bundle
            supportFragmentManager.beginTransaction().replace(R.id.fragment_holder, mFragment).commit()
        }
        hideSystemUI()

        if (mUri!!.scheme == "content") {
            val proj = arrayOf(MediaStore.Images.Media.TITLE)
            val cursor = contentResolver.query(mUri!!, proj, null, null, null)
            if (cursor != null && cursor.count != 0) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE)
                cursor.moveToFirst()
                title = cursor.getString(columnIndex)
            }
            cursor?.close()
        } else {
            title = mUri!!.toString().getFilenameFromPath()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mFragment!!.updateItem()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.photo_video_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_share -> {
                shareMedium()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareMedium() {
        val shareTitle = resources.getString(R.string.share_via)
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, mUri)
            type = if (mIsVideo) "video/*" else "image/*"
            startActivity(Intent.createChooser(this, shareTitle))
        }
    }

    override fun fragmentClicked() {
        mIsFullScreen = !mIsFullScreen
        if (mIsFullScreen) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    private fun hideSystemUI() {
        Utils.hideSystemUI(mActionbar, window)
    }

    private fun showSystemUI() {
        Utils.showSystemUI(mActionbar, window)
    }
}
