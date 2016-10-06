package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.toast
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_edit.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class EditActivity : SimpleActivity(), CropImageView.OnCropImageCompleteListener {
    val TAG: String = EditActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        if (intent.data == null) {
            toast(R.string.invalid_image_path)
            finish()
            return
        }

        Log.e("DEBUG", "uri $TAG ${intent.data}")
        crop_image_view.apply {
            guidelines = CropImageView.Guidelines.OFF
            setOnCropImageCompleteListener(this@EditActivity)
            setImageUriAsync(intent.data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
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

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        if (result.error == null) {
            val path = intent.data.path
            val file = File(path)
            var out: FileOutputStream? = null
            try {
                out = FileOutputStream(file)
                result.bitmap.compress(getFileExtension(file), 100, out)
                setResult(Activity.RESULT_OK, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Crop compressing failed $e")
            } finally {
                try {
                    out?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "FileOutputStream closing failed $e")
                }
            }

            MediaScannerConnection.scanFile(applicationContext, arrayOf(path), null, { path: String, uri: Uri ->
                finish()
            })
        } else {
            toast("${getString(R.string.image_croping_failed)} ${result.error.message}")
        }
    }

    private fun getFileExtension(file: File): Bitmap.CompressFormat {
        return when (file.extension) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
    }
}
