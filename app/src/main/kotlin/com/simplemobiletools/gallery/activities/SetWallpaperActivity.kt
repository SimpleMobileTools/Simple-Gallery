package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.toast
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_edit.*

class SetWallpaperActivity : SimpleActivity(), CropImageView.OnCropImageCompleteListener {
    lateinit var uri: Uri
    lateinit var wallpaperManager: WallpaperManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_wallpaper)

        if (intent.data == null) {
            toast(R.string.invalid_image_path)
            finish()
            return
        }

        uri = intent.data
        if (uri.scheme != "file" && uri.scheme != "content") {
            toast(R.string.unknown_file_location)
            finish()
            return
        }

        wallpaperManager = WallpaperManager.getInstance(applicationContext)
        crop_image_view.apply {
            guidelines = CropImageView.Guidelines.OFF
            setOnCropImageCompleteListener(this@SetWallpaperActivity)
            setImageUriAsync(intent.data)
            setAspectRatio(wallpaperManager.desiredMinimumWidth, wallpaperManager.desiredMinimumHeight)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_set_wallpaper, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save -> {
                crop_image_view.getCroppedImageAsync()
                return true
            }
            R.id.rotate -> {
                crop_image_view.rotateImage(90)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCropImageComplete(view: CropImageView?, result: CropImageView.CropResult) {
        if (result.error == null) {
            toast(R.string.setting_wallpaper)
            Thread({
                val bitmap = result.bitmap
                val wantedHeight = wallpaperManager.desiredMinimumHeight
                val ratio = wantedHeight / bitmap.height.toFloat()
                val wantedWidth = (bitmap.width * ratio).toInt()
                wallpaperManager.setBitmap(Bitmap.createScaledBitmap(bitmap, wantedWidth, wantedHeight, false))
                setResult(Activity.RESULT_OK)
                finish()
            }).start()
        } else {
            toast("${getString(R.string.image_editing_failed)}: ${result.error.message}")
        }
    }
}
