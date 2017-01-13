package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.getFileDocument
import com.simplemobiletools.commons.extensions.needsStupidWritePermissions
import com.simplemobiletools.commons.extensions.scanPath
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.dialogs.ResizeDialog
import com.simplemobiletools.gallery.dialogs.SaveAsDialog
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.getRealPathFromURI
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_edit.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class EditActivity : SimpleActivity(), CropImageView.OnCropImageCompleteListener {
    val TAG: String = EditActivity::class.java.simpleName

    lateinit var uri: Uri
    var resizeWidth = 0
    var resizeHeight = 0

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
            R.id.save_as -> crop_image_view.getCroppedImageAsync()
            R.id.rotate -> crop_image_view.rotateImage(90)
            R.id.resize -> resizeImage()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun resizeImage() {
        val point = getAreaSize()
        if (point == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        ResizeDialog(this, point) {
            resizeWidth = it.x
            resizeHeight = it.y
            crop_image_view.getCroppedImageAsync()
        }
    }

    private fun getAreaSize(): Point? {
        val rect = crop_image_view.cropRect ?: return null
        val rotation = crop_image_view.rotatedDegrees
        return if (rotation == 0 || rotation == 180) {
            Point(rect.width(), rect.height())
        } else {
            Point(rect.height(), rect.width())
        }
    }

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        if (result.error == null) {
            if (uri.scheme == "file") {
                SaveAsDialog(this, uri.path) {
                    saveBitmapToFile(result.bitmap, it)
                }
            } else if (uri.scheme == "content") {
                val newPath = applicationContext.getRealPathFromURI(uri) ?: ""
                if (!newPath.isEmpty()) {
                    SaveAsDialog(this, newPath) {
                        saveBitmapToFile(result.bitmap, it)
                    }
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

        var out: OutputStream? = null
        try {
            if (needsStupidWritePermissions(path)) {
                if (isShowingPermDialog(file))
                    return

                var document = getFileDocument(path, config.treeUri)
                if (!file.exists()) {
                    document = document.createFile("", file.name)
                }
                out = contentResolver.openOutputStream(document.uri)
            } else {
                out = FileOutputStream(file)
            }

            if (resizeWidth > 0 && resizeHeight > 0) {
                val resized = Bitmap.createScaledBitmap(bitmap, resizeWidth, resizeHeight, false)
                resized.compress(getCompressionFormat(file), 90, out)
            } else {
                bitmap.compress(getCompressionFormat(file), 90, out)
            }
            setResult(Activity.RESULT_OK, intent)
        } catch (e: Exception) {
            Log.e(TAG, "Crop compressing failed $path $e")
            toast(R.string.image_editing_failed)
            finish()
        } finally {
            try {
                out?.close()
            } catch (e: IOException) {
                Log.e(TAG, "FileOutputStream closing failed $e")
            }
        }

        scanPath(path) {
            setResult(Activity.RESULT_OK, intent)
            runOnUiThread {
                toast(R.string.file_saved)
            }

            finish()
        }
    }

    private fun getCompressionFormat(file: File): Bitmap.CompressFormat {
        return when (file.extension.toLowerCase()) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
    }
}
