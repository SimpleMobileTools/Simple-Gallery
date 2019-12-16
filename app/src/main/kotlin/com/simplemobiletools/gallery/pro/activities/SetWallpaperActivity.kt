package com.simplemobiletools.gallery.pro.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.checkAppSideloading
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.helpers.isNougatPlus
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.gallery.pro.R
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_set_wallpaper.*
import kotlinx.android.synthetic.main.bottom_set_wallpaper_actions.*

class SetWallpaperActivity : SimpleActivity(), CropImageView.OnCropImageCompleteListener {
    private val PICK_IMAGE = 1
    private var isLandscapeRatio = true
    private var wallpaperFlag = -1

    lateinit var uri: Uri
    lateinit var wallpaperManager: WallpaperManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_wallpaper)

        if (checkAppSideloading()) {
            return
        }

        if (intent.data == null) {
            val pickIntent = Intent(applicationContext, MainActivity::class.java)
            pickIntent.action = Intent.ACTION_PICK
            pickIntent.type = "image/*"
            startActivityForResult(pickIntent, PICK_IMAGE)
            return
        }

        handleImage(intent)
        setupBottomActions()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_set_wallpaper, menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save -> confirmWallpaper()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun handleImage(intent: Intent) {
        uri = intent.data!!
        if (uri.scheme != "file" && uri.scheme != "content") {
            toast(R.string.unknown_file_location)
            finish()
            return
        }

        wallpaperManager = WallpaperManager.getInstance(applicationContext)
        crop_image_view.apply {
            setOnCropImageCompleteListener(this@SetWallpaperActivity)
            setImageUriAsync(uri)
        }

        setupAspectRatio()
    }

    private fun setupBottomActions() {
        bottom_set_wallpaper_aspect_ratio.setOnClickListener {
            changeAspectRatio(!isLandscapeRatio)
        }

        bottom_set_wallpaper_rotate.setOnClickListener {
            crop_image_view.rotateImage(90)
        }
    }

    private fun setupAspectRatio() {
        val wallpaperWidth = if (isLandscapeRatio) wallpaperManager.desiredMinimumWidth else wallpaperManager.desiredMinimumWidth / 2
        crop_image_view.setAspectRatio(wallpaperWidth, wallpaperManager.desiredMinimumHeight)
        bottom_set_wallpaper_aspect_ratio.setImageResource(if (isLandscapeRatio) R.drawable.ic_minimize else R.drawable.ic_maximize)
    }

    private fun changeAspectRatio(isLandscape: Boolean) {
        isLandscapeRatio = isLandscape
        setupAspectRatio()
    }

    @SuppressLint("InlinedApi")
    private fun confirmWallpaper() {
        if (isNougatPlus()) {
            val items = arrayListOf(
                    RadioItem(WallpaperManager.FLAG_SYSTEM, getString(R.string.home_screen)),
                    RadioItem(WallpaperManager.FLAG_LOCK, getString(R.string.lock_screen)),
                    RadioItem(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, getString(R.string.home_and_lock_screen)))

            RadioGroupDialog(this, items) {
                wallpaperFlag = it as Int
                crop_image_view.getCroppedImageAsync()
            }
        } else {
            crop_image_view.getCroppedImageAsync()
        }
    }

    @SuppressLint("NewApi")
    override fun onCropImageComplete(view: CropImageView?, result: CropImageView.CropResult) {
        if (isDestroyed)
            return

        if (result.error == null) {
            toast(R.string.setting_wallpaper)
            ensureBackgroundThread {
                val bitmap = result.bitmap
                val wantedHeight = wallpaperManager.desiredMinimumHeight
                val ratio = wantedHeight / bitmap.height.toFloat()
                val wantedWidth = (bitmap.width * ratio).toInt()
                try {
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, wantedWidth, wantedHeight, true)
                    if (isNougatPlus()) {
                        wallpaperManager.setBitmap(scaledBitmap, null, true, wallpaperFlag)
                    } else {
                        wallpaperManager.setBitmap(scaledBitmap)
                    }
                    setResult(Activity.RESULT_OK)
                } catch (e: OutOfMemoryError) {
                    toast(R.string.out_of_memory_error)
                    setResult(Activity.RESULT_CANCELED)
                }
                finish()
            }
        } else {
            toast("${getString(R.string.image_editing_failed)}: ${result.error.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                handleImage(resultData)
            } else {
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }
}
