package com.simplemobiletools.gallery.pro.activities

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.RelativeLayout
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.helpers.isNougatPlus
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.gallery.pro.BuildConfig
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.dialogs.OtherAspectRatioDialog
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.fixDateTaken
import com.simplemobiletools.gallery.pro.helpers.*
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_edit.*
import kotlinx.android.synthetic.main.bottom_actions_aspect_ratio.*
import kotlinx.android.synthetic.main.bottom_editor_crop_rotate_actions.*
import java.io.*

class EditActivity : SimpleActivity(), CropImageView.OnCropImageCompleteListener {
    private val TEMP_FOLDER_NAME = "images"
    private val ASPECT_X = "aspectX"
    private val ASPECT_Y = "aspectY"

    private lateinit var uri: Uri
    private lateinit var saveUri: Uri
    private var resizeWidth = 0
    private var resizeHeight = 0
    private var lastOtherAspectRatio: Pair<Float, Float>? = null
    private var currAspectRatio = ASPECT_RATIO_FREE
    private var isSharingBitmap = false
    private var oldExif: ExifInterface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        if (checkAppSideloading()) {
            return
        }

        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                initEditActivity()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save_as -> saveImage()
            R.id.share -> shareImage()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun initEditActivity() {
        if (intent.data == null) {
            toast(R.string.invalid_image_path)
            finish()
            return
        }

        uri = intent.data!!
        if (uri.scheme != "file" && uri.scheme != "content") {
            toast(R.string.unknown_file_location)
            finish()
            return
        }

        if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            val realPath = intent.extras!!.getString(REAL_FILE_PATH)
            uri = when {
                isPathOnOTG(realPath!!) -> uri
                realPath.startsWith("file:/") -> Uri.parse(realPath)
                else -> Uri.fromFile(File(realPath))
            }
        } else {
            (getRealPathFromURI(uri))?.apply {
                uri = Uri.fromFile(File(this))
            }
        }

        saveUri = when {
            intent.extras?.containsKey(MediaStore.EXTRA_OUTPUT) == true -> intent.extras!!.get(MediaStore.EXTRA_OUTPUT) as Uri
            else -> uri
        }

        (bottom_editor_crop_rotate_actions.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1)
        loadCropImageView()
        setupCropRotateActionButtons()
        setupAspectRatioButtons()

        if (config.lastEditorCropAspectRatio == ASPECT_RATIO_OTHER) {
            if (config.lastEditorCropOtherAspectRatioX == 0f) {
                config.lastEditorCropOtherAspectRatioX = 1f
            }

            if (config.lastEditorCropOtherAspectRatioY == 0f) {
                config.lastEditorCropOtherAspectRatioY = 1f
            }

            lastOtherAspectRatio = Pair(config.lastEditorCropOtherAspectRatioX, config.lastEditorCropOtherAspectRatioY)
        }
        updateAspectRatio(config.lastEditorCropAspectRatio)
        crop_image_view.guidelines = CropImageView.Guidelines.ON
        bottom_aspect_ratios.beVisible()
    }

    private fun loadCropImageView() {
        crop_image_view.apply {
            beVisible()
            setOnCropImageCompleteListener(this@EditActivity)
            setImageUriAsync(uri)
            guidelines = CropImageView.Guidelines.ON

            if (shouldCropSquare()) {
                currAspectRatio = ASPECT_RATIO_ONE_ONE
                setFixedAspectRatio(true)
                bottom_aspect_ratio.beGone()
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun saveImage() {
        var inputStream: InputStream? = null
        try {
            if (isNougatPlus()) {
                inputStream = contentResolver.openInputStream(uri)
                oldExif = ExifInterface(inputStream!!)
            }
        } catch (e: Exception) {
        } finally {
            inputStream?.close()
        }

        crop_image_view.getCroppedImageAsync()
    }

    private fun shareImage() {
        ensureBackgroundThread {
            isSharingBitmap = true
            runOnUiThread {
                crop_image_view.getCroppedImageAsync()
            }
        }
    }

    private fun getTempImagePath(bitmap: Bitmap, callback: (path: String?) -> Unit) {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.PNG, 0, bytes)

        val folder = File(cacheDir, TEMP_FOLDER_NAME)
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                callback(null)
                return
            }
        }

        val filename = applicationContext.getFilenameFromContentUri(saveUri) ?: "tmp.jpg"
        val newPath = "$folder/$filename"
        val fileDirItem = FileDirItem(newPath, filename)
        getFileOutputStream(fileDirItem, true) {
            if (it != null) {
                try {
                    it.write(bytes.toByteArray())
                    callback(newPath)
                } catch (e: Exception) {
                } finally {
                    it.close()
                }
            } else {
                callback("")
            }
        }
    }

    private fun shareBitmap(bitmap: Bitmap) {
        getTempImagePath(bitmap) {
            if (it != null) {
                sharePathIntent(it, BuildConfig.APPLICATION_ID)
            } else {
                toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun setupCropRotateActionButtons() {
        bottom_rotate.setOnClickListener {
            crop_image_view.rotateImage(90)
        }

        bottom_flip_horizontally.setOnClickListener {
            crop_image_view.flipImageHorizontally()
        }

        bottom_flip_vertically.setOnClickListener {
            crop_image_view.flipImageVertically()
        }
    }

    private fun setupAspectRatioButtons() {
        bottom_aspect_ratio_free.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_FREE)
        }

        bottom_aspect_ratio_one_one.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_ONE_ONE)
        }

        bottom_aspect_ratio_four_three.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_FOUR_THREE)
        }

        bottom_aspect_ratio_sixteen_nine.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_SIXTEEN_NINE)
        }

        bottom_aspect_ratio_other.setOnClickListener {
            OtherAspectRatioDialog(this, lastOtherAspectRatio) {
                lastOtherAspectRatio = it
                config.lastEditorCropOtherAspectRatioX = it.first
                config.lastEditorCropOtherAspectRatioY = it.second
                updateAspectRatio(ASPECT_RATIO_OTHER)
            }
        }

        updateAspectRatioButtons()
    }

    private fun updateAspectRatio(aspectRatio: Int) {
        currAspectRatio = aspectRatio
        config.lastEditorCropAspectRatio = aspectRatio
        updateAspectRatioButtons()

        crop_image_view.apply {
            if (aspectRatio == ASPECT_RATIO_FREE) {
                setFixedAspectRatio(false)
            } else {
                val newAspectRatio = when (aspectRatio) {
                    ASPECT_RATIO_ONE_ONE -> Pair(1f, 1f)
                    ASPECT_RATIO_FOUR_THREE -> Pair(4f, 3f)
                    ASPECT_RATIO_SIXTEEN_NINE -> Pair(16f, 9f)
                    else -> Pair(lastOtherAspectRatio!!.first, lastOtherAspectRatio!!.second)
                }

                setAspectRatio(newAspectRatio.first.toInt(), newAspectRatio.second.toInt())
            }
        }
    }

    private fun updateAspectRatioButtons() {
        arrayOf(bottom_aspect_ratio_free, bottom_aspect_ratio_one_one, bottom_aspect_ratio_four_three, bottom_aspect_ratio_sixteen_nine, bottom_aspect_ratio_other).forEach {
            it.setTextColor(Color.WHITE)
        }

        val currentAspectRatioButton = when (currAspectRatio) {
            ASPECT_RATIO_FREE -> bottom_aspect_ratio_free
            ASPECT_RATIO_ONE_ONE -> bottom_aspect_ratio_one_one
            ASPECT_RATIO_FOUR_THREE -> bottom_aspect_ratio_four_three
            ASPECT_RATIO_SIXTEEN_NINE -> bottom_aspect_ratio_sixteen_nine
            else -> bottom_aspect_ratio_other
        }

        currentAspectRatioButton.setTextColor(getAdjustedPrimaryColor())
    }

    private fun shouldCropSquare(): Boolean {
        val extras = intent.extras
        return if (extras != null && extras.containsKey(ASPECT_X) && extras.containsKey(ASPECT_Y)) {
            extras.getInt(ASPECT_X) == extras.getInt(ASPECT_Y)
        } else {
            false
        }
    }

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        if (result.error == null) {
            val bitmap = result.bitmap
            if (isSharingBitmap) {
                isSharingBitmap = false
                shareBitmap(bitmap)
                return
            }

            if (saveUri.scheme == "file") {
                saveBitmapToFile(bitmap, saveUri.path!!, true)
            } else {
                var inputStream: InputStream? = null
                var outputStream: OutputStream? = null
                try {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(CompressFormat.JPEG, 100, stream)
                    inputStream = ByteArrayInputStream(stream.toByteArray())
                    outputStream = contentResolver.openOutputStream(saveUri)
                    inputStream.copyTo(outputStream!!)
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }

                Intent().apply {
                    data = saveUri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setResult(RESULT_OK, this)
                }
                finish()
            }
        } else {
            toast("${getString(R.string.image_editing_failed)}: ${result.error.message}")
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap, path: String, showSavingToast: Boolean) {
        if (!packageName.contains("slootelibomelpmis".reversed(), true)) {
            if (baseConfig.appRunCount > 100) {
                val label = "sknahT .moc.slootelibomelpmis.www morf eno lanigiro eht daolnwod ytefas nwo ruoy roF .ppa eht fo noisrev ekaf a gnisu era uoY".reversed()
                runOnUiThread {
                    ConfirmationDialog(this, label, positive = com.simplemobiletools.commons.R.string.ok, negative = 0) {
                        launchViewIntent("6629852208836920709=di?ved/sppa/erots/moc.elgoog.yalp//:sptth".reversed())
                    }
                }
                return
            }
        }

        try {
            ensureBackgroundThread {
                val file = File(path)
                val fileDirItem = FileDirItem(path, path.getFilenameFromPath())
                getFileOutputStream(fileDirItem, true) {
                    if (it != null) {
                        saveBitmap(file, bitmap, it, showSavingToast)
                    } else {
                        toast(R.string.image_editing_failed)
                    }
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        } catch (e: OutOfMemoryError) {
            toast(R.string.out_of_memory_error)
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun saveBitmap(file: File, bitmap: Bitmap, out: OutputStream, showSavingToast: Boolean) {
        if (showSavingToast) {
            toast(R.string.saving)
        }

        if (resizeWidth > 0 && resizeHeight > 0) {
            val resized = Bitmap.createScaledBitmap(bitmap, resizeWidth, resizeHeight, false)
            resized.compress(file.absolutePath.getCompressionFormat(), 90, out)
        } else {
            bitmap.compress(file.absolutePath.getCompressionFormat(), 90, out)
        }

        try {
            if (isNougatPlus()) {
                val newExif = ExifInterface(file.absolutePath)
                oldExif?.copyTo(newExif, false)
            }
        } catch (e: Exception) {
        }

        setResult(Activity.RESULT_OK, intent)
        scanFinalPath(file.absolutePath)
        out.close()
    }

    private fun scanFinalPath(path: String) {
        val paths = arrayListOf(path)
        rescanPaths(paths) {
            fixDateTaken(paths, false)
            setResult(Activity.RESULT_OK, intent)
            toast(R.string.file_saved)
            finish()
        }
    }
}
