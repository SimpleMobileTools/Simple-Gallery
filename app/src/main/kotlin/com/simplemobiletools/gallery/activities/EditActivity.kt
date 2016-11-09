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
import com.simplemobiletools.gallery.Utils
import com.simplemobiletools.gallery.dialogs.SaveAsDialog
import com.simplemobiletools.gallery.extensions.toast
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_edit.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

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
        return when (item.itemId) {
            R.id.save_as -> {
                crop_image_view.getCroppedImageAsync()
                true
            }
            R.id.rotate -> {
                crop_image_view.rotateImage(90)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        if (result.error == null) {
            if (uri.scheme == "file") {
                SaveAsDialog(this, uri.path, object : SaveAsDialog.OnSaveAsListener {
                    override fun onSaveAsSuccess(filename: String) {
                        val parent = File(uri.path).parent
                        val path = File(parent, filename).absolutePath
                        saveBitmapToFile(result.bitmap, path)
                    }
                })
            } else if (uri.scheme == "content") {
                val newPath = Utils.getRealPathFromURI(applicationContext, uri) ?: ""
                if (!newPath.isEmpty()) {
                    SaveAsDialog(this, newPath, object : SaveAsDialog.OnSaveAsListener {
                        override fun onSaveAsSuccess(filename: String) {
                            val parent = File(uri.path).parent
                            val path = File(parent, filename).absolutePath
                            saveBitmapToFile(result.bitmap, path)
                        }
                    })
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
            if (Utils.needsStupidWritePermissions(this, path)) {
                if (Utils.isShowingWritePermissions(this, file))
                    return

                var document = Utils.getFileDocument(this, path)
                if (!file.exists()) {
                    document = document.createFile("", file.name)
                }
                out = contentResolver.openOutputStream(document.uri)
            } else {
                out = FileOutputStream(file)
            }

            bitmap.compress(getCompressionFormat(file), 90, out)
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

        MediaScannerConnection.scanFile(applicationContext, arrayOf(path), null, { path: String, uri: Uri ->
            setResult(Activity.RESULT_OK, intent)
            runOnUiThread {
                toast(R.string.file_saved)
            }

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
}
