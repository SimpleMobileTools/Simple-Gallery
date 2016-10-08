package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
    lateinit var uri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

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
            if (uri.scheme == "file") {
                saveBitmapToFile(result.bitmap, uri.path)
            } else if (uri.scheme == "content") {
                val newPath = convertMediaUriToPath(uri) ?: ""
                if (!newPath.isEmpty()) {
                    saveBitmapToFile(result.bitmap, newPath)
                } else {
                    toast(R.string.image_editing_failed)
                    finish()
                }
            } else {
                toast(R.string.unknown_file_location)
                finish()
            }
        } else {
            toast("${getString(R.string.image_editing_failed)}: ${result.error.message}")
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap, path: String) {
        val file = File(path)
        if (!file.exists()) {
            toast(R.string.error_saving_file)
            finish()
            return
        }

        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(file)
            bitmap.compress(getCompressionFormat(file), 100, out)
            setResult(Activity.RESULT_OK, intent)
        } catch (e: Exception) {
            Log.e(TAG, "Crop compressing failed $e")
            toast(R.string.image_editing_failed)
            finish()
        } finally {
            try {
                out?.close()
            } catch (e: IOException) {
                Log.e(TAG, "FileOutputStream closing failed $e")
            }
        }

        MediaScannerConnection.scanFile(applicationContext, arrayOf(path), null, { path: String, uri: Uri ->
            setResult(Activity.RESULT_OK, intent)
            finish()
        })
    }

    private fun getCompressionFormat(file: File): Bitmap.CompressFormat {
        return when (file.extension.toLowerCase()) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
    }

    private fun convertMediaUriToPath(uri: Uri): String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, proj, null, null, null)
        val index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        val path = cursor.getString(index)
        cursor.close()
        return path
    }
}
