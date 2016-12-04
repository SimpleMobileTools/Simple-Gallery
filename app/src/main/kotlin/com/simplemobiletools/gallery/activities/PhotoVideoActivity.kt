package com.simplemobiletools.gallery.activities

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.gallery.MEDIUM
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.openWith
import com.simplemobiletools.gallery.extensions.setAsWallpaper
import com.simplemobiletools.gallery.extensions.shareMedium
import com.simplemobiletools.gallery.fragments.PhotoFragment
import com.simplemobiletools.gallery.fragments.VideoFragment
import com.simplemobiletools.gallery.fragments.ViewPagerFragment
import com.simplemobiletools.gallery.models.Medium
import java.io.File

open class PhotoVideoActivity : SimpleActivity(), ViewPagerFragment.FragmentClickListener {
    private var mIsFullScreen = false
    lateinit var mUri: Uri
    lateinit var mFragment: ViewPagerFragment
    lateinit var mMedium: Medium

    companion object {
        var mIsVideo = false
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_holder)

        mUri = intent.data ?: return

        if (mUri.scheme == "file") {
            Intent(this, ViewPagerActivity::class.java).apply {
                putExtra(MEDIUM, mUri.path)
                startActivity(this)
            }
            finish()
            return
        }

        val bundle = Bundle()
        val file = File(mUri.toString())
        mMedium = Medium(file.name, mUri.toString(), mIsVideo, 0, file.length())
        bundle.putSerializable(MEDIUM, mMedium)

        if (savedInstanceState == null) {
            mFragment = if (mIsVideo) VideoFragment() else PhotoFragment()
            mFragment.listener = this
            mFragment.arguments = bundle
            supportFragmentManager.beginTransaction().replace(R.id.fragment_holder, mFragment).commit()
        }

        val proj = arrayOf(MediaStore.Images.Media.TITLE)
        val cursor = contentResolver.query(mUri, proj, null, null, null)
        if (cursor != null && cursor.count != 0) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE)
            cursor.moveToFirst()
            title = cursor.getString(columnIndex)
        }
        cursor?.close()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mFragment.updateItem()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.photo_video_menu, menu)
        menu.findItem(R.id.menu_set_as_wallpaper).isVisible = mMedium.isImage()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_set_as_wallpaper -> {
                setAsWallpaper(File(mMedium.path))
                true
            }
            R.id.menu_open_with -> {
                openWith(File(mMedium.path))
                true
            }
            R.id.menu_share -> {
                shareMedium(mMedium)
                true
            }
            else -> super.onOptionsItemSelected(item)
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
}
