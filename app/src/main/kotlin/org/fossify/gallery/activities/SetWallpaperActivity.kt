package org.fossify.gallery.activities

import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import com.canhub.cropper.CropImageView
import org.fossify.commons.dialogs.RadioGroupDialog
import org.fossify.commons.extensions.checkAppSideloading
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isNougatPlus
import org.fossify.commons.models.RadioItem
import org.fossify.gallery.R
import org.fossify.gallery.databinding.ActivitySetWallpaperBinding

class SetWallpaperActivity : SimpleActivity(), CropImageView.OnCropImageCompleteListener {
    private val RATIO_PORTRAIT = 0
    private val RATIO_LANDSCAPE = 1
    private val RATIO_SQUARE = 2

    private val PICK_IMAGE = 1
    private var aspectRatio = RATIO_PORTRAIT
    private var wallpaperFlag = -1

    lateinit var uri: Uri
    lateinit var wallpaperManager: WallpaperManager

    private val binding by viewBinding(ActivitySetWallpaperBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupBottomActions()

        if (checkAppSideloading()) {
            return
        }

        setupOptionsMenu()
        if (intent.data == null) {
            val pickIntent = Intent(applicationContext, MainActivity::class.java)
            pickIntent.action = Intent.ACTION_PICK
            pickIntent.type = "image/*"
            startActivityForResult(pickIntent, PICK_IMAGE)
            return
        }

        handleImage(intent)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.setWallpaperToolbar, NavigationIcon.Arrow)
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

    private fun setupOptionsMenu() {
        binding.setWallpaperToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.save -> confirmWallpaper()
                R.id.allow_changing_aspect_ratio -> binding.cropImageView.clearAspectRatio()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun handleImage(intent: Intent) {
        uri = intent.data!!
        if (uri.scheme != "file" && uri.scheme != "content") {
            toast(R.string.unknown_file_location)
            finish()
            return
        }

        wallpaperManager = WallpaperManager.getInstance(applicationContext)
        binding.cropImageView.apply {
            setOnCropImageCompleteListener(this@SetWallpaperActivity)
            setImageUriAsync(uri)
        }

        setupAspectRatio()
    }

    private fun setupBottomActions() {
        binding.bottomSetWallpaperActions.bottomSetWallpaperAspectRatio.setOnClickListener {
            changeAspectRatio()
        }

        binding.bottomSetWallpaperActions.bottomSetWallpaperRotate.setOnClickListener {
            binding.cropImageView.rotateImage(90)
        }
    }

    private fun setupAspectRatio() {
        var widthToUse = wallpaperManager.desiredMinimumWidth
        val heightToUse = wallpaperManager.desiredMinimumHeight
        if (widthToUse == heightToUse) {
            widthToUse /= 2
        }

        when (aspectRatio) {
            RATIO_PORTRAIT -> binding.cropImageView.setAspectRatio(heightToUse, widthToUse)
            RATIO_LANDSCAPE -> binding.cropImageView.setAspectRatio(widthToUse, heightToUse)
            else -> binding.cropImageView.setAspectRatio(widthToUse, widthToUse)
        }
    }

    private fun changeAspectRatio() {
        aspectRatio = ++aspectRatio % (RATIO_SQUARE + 1)
        setupAspectRatio()
    }

    private fun confirmWallpaper() {
        if (isNougatPlus()) {
            val items = arrayListOf(
                RadioItem(WallpaperManager.FLAG_SYSTEM, getString(R.string.home_screen)),
                RadioItem(WallpaperManager.FLAG_LOCK, getString(R.string.lock_screen)),
                RadioItem(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, getString(R.string.home_and_lock_screen))
            )

            RadioGroupDialog(this, items) {
                wallpaperFlag = it as Int
                binding.cropImageView.croppedImageAsync()
            }
        } else {
            binding.cropImageView.croppedImageAsync()
        }
    }

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        if (isDestroyed)
            return

        if (result.error == null && result.bitmap != null) {
            toast(R.string.setting_wallpaper)
            ensureBackgroundThread {
                val bitmap = result.bitmap!!
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
                    toast(org.fossify.commons.R.string.out_of_memory_error)
                    setResult(Activity.RESULT_CANCELED)
                }
                finish()
            }
        } else {
            toast("${getString(R.string.image_editing_failed)}: ${result.error?.message}")
        }
    }
}
