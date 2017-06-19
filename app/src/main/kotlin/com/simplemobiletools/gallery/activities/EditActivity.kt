package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.getCompressionFormat
import com.simplemobiletools.commons.extensions.getFileOutputStream
import com.simplemobiletools.commons.extensions.scanPath
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.dialogs.ResizeDialog
import com.simplemobiletools.gallery.dialogs.SaveAsDialog
import com.simplemobiletools.gallery.extensions.getRealPathFromURI
import com.simplemobiletools.gallery.extensions.openEditor
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.view_crop_image.*
import java.io.*

class EditActivity : SimpleActivity(), CropImageView.OnCropImageCompleteListener {
    val TAG = EditActivity::class.java.simpleName
    val ASPECT_X = "aspectX"
    val ASPECT_Y = "aspectY"
    val CROP = "crop"

    lateinit var uri: Uri
    var resizeWidth = 0
    var resizeHeight = 0
    var isCropIntent = false
    var isEditingWithThirdParty = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_crop_image)

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

        isCropIntent = intent.extras?.get(CROP) == "true"

        crop_image_view.apply {
            setOnCropImageCompleteListener(this@EditActivity)
            setImageUriAsync(intent.data)

            if (isCropIntent && shouldCropSquare())
                setFixedAspectRatio(true)
        }
    }

    override fun onResume() {
        super.onResume()
        isEditingWithThirdParty = false
    }

    override fun onStop() {
        super.onStop()
        if (isEditingWithThirdParty)
            finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        menu.findItem(R.id.resize).isVisible = !isCropIntent
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save_as -> crop_image_view.getCroppedImageAsync()
            R.id.rotate -> crop_image_view.rotateImage(90)
            R.id.resize -> resizeImage()
            R.id.flip_horizontally -> flipImage(true)
            R.id.flip_vertically -> flipImage(false)
            R.id.edit -> editWith()
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

    private fun shouldCropSquare(): Boolean {
        val extras = intent.extras
        return if (extras != null && extras.containsKey(ASPECT_X) && extras.containsKey(ASPECT_Y)) {
            extras.getInt(ASPECT_X) == extras.getInt(ASPECT_Y)
        } else {
            false
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
            if (isCropIntent && intent.extras?.containsKey(MediaStore.EXTRA_OUTPUT) == true) {
                val targetUri = intent.extras!!.get(MediaStore.EXTRA_OUTPUT) as Uri
                var inputStream: InputStream? = null
                var outputStream: OutputStream? = null
                try {
                    val stream = ByteArrayOutputStream()
                    result.bitmap.compress(CompressFormat.JPEG, 100, stream)
                    inputStream = ByteArrayInputStream(stream.toByteArray())
                    outputStream = contentResolver.openOutputStream(targetUri)
                    inputStream.copyTo(outputStream)
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }
                setResult(RESULT_OK)
                finish()
            } else if (uri.scheme == "file") {
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

        try {
            getFileOutputStream(file) {
                saveBitmap(file, bitmap, it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Crop compressing failed $path $e")
            toast(R.string.image_editing_failed)
            finish()
        } catch (e: OutOfMemoryError) {
            toast(R.string.out_of_memory_error)
        }
    }

    private fun saveBitmap(file: File, bitmap: Bitmap, out: OutputStream) {
        if (resizeWidth > 0 && resizeHeight > 0) {
            val resized = Bitmap.createScaledBitmap(bitmap, resizeWidth, resizeHeight, false)
            resized.compress(file.getCompressionFormat(), 90, out)
        } else {
            bitmap.compress(file.getCompressionFormat(), 90, out)
        }
        setResult(Activity.RESULT_OK, intent)
        scanFinalPath(file.absolutePath)
        out.close()
    }

    private fun flipImage(horizontally: Boolean) {
        if (horizontally)
            crop_image_view.flipImageHorizontally()
        else
            crop_image_view.flipImageVertically()
    }

    private fun editWith() {
        openEditor(uri, true)
        isEditingWithThirdParty = true
    }

    private fun scanFinalPath(path: String) {
        scanPath(path) {
            setResult(Activity.RESULT_OK, intent)
            toast(R.string.file_saved)
            finish()
        }
    }
}
