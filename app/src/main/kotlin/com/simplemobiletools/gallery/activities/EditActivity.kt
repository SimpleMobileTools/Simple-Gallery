package com.simplemobiletools.gallery.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.OTG_PATH
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.dialogs.ResizeDialog
import com.simplemobiletools.gallery.dialogs.SaveAsDialog
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.openEditor
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_edit.*
import kotlinx.android.synthetic.main.bottom_actions_aspect_ratio.*
import kotlinx.android.synthetic.main.bottom_editor_crop_rotate_actions.*
import kotlinx.android.synthetic.main.bottom_editor_primary_actions.*
import java.io.*

class EditActivity : SimpleActivity(), CropImageView.OnCropImageCompleteListener {
    private val ASPECT_X = "aspectX"
    private val ASPECT_Y = "aspectY"
    private val CROP = "crop"

    private val ASPECT_RATIO_FREE = 0
    private val ASPECT_RATIO_ONE_ONE = 1
    private val ASPECT_RATIO_FOUR_THREE = 2
    private val ASPECT_RATIO_SIXTEEN_NINE = 3

    // constants for bottom primary action groups
    private val PRIMARY_ACTION_NONE = 0
    private val PRIMARY_ACTION_FILTER = 1
    private val PRIMARY_ACTION_CROP_ROTATE = 2

    private val CROP_ROTATE_NONE = 0
    private val CROP_ROTATE_ASPECT_RATIO = 1

    private lateinit var uri: Uri
    private lateinit var saveUri: Uri
    private var resizeWidth = 0
    private var resizeHeight = 0
    private var currPrimaryAction = PRIMARY_ACTION_NONE
    private var currCropRotateAction = CROP_ROTATE_NONE
    private var currAspectRatio = ASPECT_RATIO_FREE
    private var isCropIntent = false
    private var isEditingWithThirdParty = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                initEditActivity()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    private fun initEditActivity() {
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

        if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            val realPath = intent.extras.getString(REAL_FILE_PATH)
            uri = when {
                realPath.startsWith(OTG_PATH) -> uri
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

        isCropIntent = intent.extras?.get(CROP) == "true"

        crop_image_view.apply {
            setOnCropImageCompleteListener(this@EditActivity)
            setImageUriAsync(uri)
            guidelines = CropImageView.Guidelines.ON

            if (isCropIntent && shouldCropSquare()) {
                currAspectRatio = ASPECT_RATIO_ONE_ONE
                setFixedAspectRatio(true)
                bottom_aspect_ratio.beGone()
            }
        }

        setupBottomActions()
    }

    override fun onResume() {
        super.onResume()
        isEditingWithThirdParty = false
    }

    override fun onStop() {
        super.onStop()
        if (isEditingWithThirdParty) {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save_as -> crop_image_view.getCroppedImageAsync()
            R.id.edit -> editWith()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun setupBottomActions() {
        setupPrimaryActionButtons()
        setupCropRotateActionButtons()
        setupAspectRatioButtons()
    }

    private fun setupPrimaryActionButtons() {
        bottom_primary_filter.setOnClickListener {
            currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_FILTER) {
                PRIMARY_ACTION_NONE
            } else {
                PRIMARY_ACTION_FILTER
            }
            updatePrimaryActionButtons()
        }

        bottom_primary_crop_rotate.setOnClickListener {
            currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE) {
                PRIMARY_ACTION_NONE
            } else {
                PRIMARY_ACTION_CROP_ROTATE
            }
            updatePrimaryActionButtons()
        }
    }

    private fun setupCropRotateActionButtons() {
        bottom_rotate.setOnClickListener {
            crop_image_view.rotateImage(90)
        }

        bottom_resize.beGoneIf(isCropIntent)
        bottom_resize.setOnClickListener {
            resizeImage()
        }

        bottom_flip_horizontally.setOnClickListener {
            crop_image_view.flipImageHorizontally()
        }

        bottom_flip_vertically.setOnClickListener {
            crop_image_view.flipImageVertically()
        }

        bottom_aspect_ratio.setOnClickListener {
            currCropRotateAction = if (currCropRotateAction == CROP_ROTATE_ASPECT_RATIO) {
                crop_image_view.guidelines = CropImageView.Guidelines.OFF
                bottom_aspect_ratios.beGone()
                CROP_ROTATE_NONE
            } else {
                crop_image_view.guidelines = CropImageView.Guidelines.ON
                bottom_aspect_ratios.beVisible()
                CROP_ROTATE_ASPECT_RATIO
            }
            updateCropRotateActionButtons()
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
        updateAspectRatioButtons()
    }

    private fun updatePrimaryActionButtons() {
        arrayOf(bottom_primary_filter, bottom_primary_crop_rotate).forEach {
            it.applyColorFilter(Color.WHITE)
        }

        val currentPrimaryActionButton = when (currPrimaryAction) {
            PRIMARY_ACTION_FILTER -> bottom_primary_filter
            PRIMARY_ACTION_CROP_ROTATE -> bottom_primary_crop_rotate
            else -> null
        }

        currentPrimaryActionButton?.applyColorFilter(config.primaryColor)
        bottom_editor_filter_actions.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_FILTER)
        bottom_editor_crop_rotate_actions.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE)

        if (currPrimaryAction != PRIMARY_ACTION_CROP_ROTATE) {
            bottom_aspect_ratios.beGone()
            currCropRotateAction = CROP_ROTATE_NONE
        }
    }

    private fun updateAspectRatio(aspectRatio: Int) {
        currAspectRatio = aspectRatio
        updateAspectRatioButtons()

        crop_image_view.apply {
            if (aspectRatio == ASPECT_RATIO_FREE) {
                setFixedAspectRatio(false)
            } else {
                val newAspectRatio = when (aspectRatio) {
                    ASPECT_RATIO_ONE_ONE -> Pair(1, 1)
                    ASPECT_RATIO_FOUR_THREE -> Pair(4, 3)
                    else -> Pair(16, 9)
                }

                setAspectRatio(newAspectRatio.first, newAspectRatio.second)
            }
        }
    }

    private fun updateAspectRatioButtons() {
        arrayOf(bottom_aspect_ratio_free, bottom_aspect_ratio_one_one, bottom_aspect_ratio_four_three, bottom_aspect_ratio_sixteen_nine).forEach {
            it.setTextColor(Color.WHITE)
        }

        val currentAspectRatioButton = when (currAspectRatio) {
            ASPECT_RATIO_FREE -> bottom_aspect_ratio_free
            ASPECT_RATIO_ONE_ONE -> bottom_aspect_ratio_one_one
            ASPECT_RATIO_FOUR_THREE -> bottom_aspect_ratio_four_three
            else -> bottom_aspect_ratio_sixteen_nine
        }

        currentAspectRatioButton.setTextColor(config.primaryColor)
    }

    private fun updateCropRotateActionButtons() {
        arrayOf(bottom_aspect_ratio).forEach {
            it.applyColorFilter(Color.WHITE)
        }

        val primaryActionView = when (currCropRotateAction) {
            CROP_ROTATE_ASPECT_RATIO -> bottom_aspect_ratio
            else -> null
        }

        primaryActionView?.applyColorFilter(config.primaryColor)
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
            if (isCropIntent) {
                if (saveUri.scheme == "file") {
                    saveBitmapToFile(result.bitmap, saveUri.path)
                } else {
                    var inputStream: InputStream? = null
                    var outputStream: OutputStream? = null
                    try {
                        val stream = ByteArrayOutputStream()
                        result.bitmap.compress(CompressFormat.JPEG, 100, stream)
                        inputStream = ByteArrayInputStream(stream.toByteArray())
                        outputStream = contentResolver.openOutputStream(saveUri)
                        inputStream.copyTo(outputStream)
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
            } else if (saveUri.scheme == "file") {
                SaveAsDialog(this, saveUri.path, true) {
                    saveBitmapToFile(result.bitmap, it)
                }
            } else if (saveUri.scheme == "content") {
                var newPath = applicationContext.getRealPathFromURI(saveUri) ?: ""
                var shouldAppendFilename = true
                if (newPath.isEmpty()) {
                    val filename = applicationContext.getFilenameFromContentUri(saveUri) ?: ""
                    if (filename.isNotEmpty()) {
                        val path = if (intent.extras?.containsKey(REAL_FILE_PATH) == true) intent.getStringExtra(REAL_FILE_PATH).getParentPath() else internalStoragePath
                        newPath = "$path/$filename"
                        shouldAppendFilename = false
                    }
                }

                if (newPath.isEmpty()) {
                    newPath = "$internalStoragePath/${getCurrentFormattedDateTime()}.${saveUri.toString().getFilenameExtension()}"
                    shouldAppendFilename = false
                }

                SaveAsDialog(this, newPath, shouldAppendFilename) {
                    saveBitmapToFile(result.bitmap, it)
                }
            } else {
                toast(R.string.unknown_file_location)
            }
        } else {
            toast("${getString(R.string.image_editing_failed)}: ${result.error.message}")
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap, path: String) {
        try {
            Thread {
                val file = File(path)
                val fileDirItem = FileDirItem(path, path.getFilenameFromPath())
                getFileOutputStream(fileDirItem, true) {
                    if (it != null) {
                        saveBitmap(file, bitmap, it)
                    } else {
                        toast(R.string.image_editing_failed)
                    }
                }
            }.start()
        } catch (e: Exception) {
            showErrorToast(e)
        } catch (e: OutOfMemoryError) {
            toast(R.string.out_of_memory_error)
        }
    }

    private fun saveBitmap(file: File, bitmap: Bitmap, out: OutputStream) {
        toast(R.string.saving)
        if (resizeWidth > 0 && resizeHeight > 0) {
            val resized = Bitmap.createScaledBitmap(bitmap, resizeWidth, resizeHeight, false)
            resized.compress(file.absolutePath.getCompressionFormat(), 90, out)
        } else {
            bitmap.compress(file.absolutePath.getCompressionFormat(), 90, out)
        }
        setResult(Activity.RESULT_OK, intent)
        scanFinalPath(file.absolutePath)
        out.close()
    }

    private fun editWith() {
        openEditor(uri.toString())
        isEditingWithThirdParty = true
    }

    private fun scanFinalPath(path: String) {
        scanPathRecursively(path) {
            setResult(Activity.RESULT_OK, intent)
            toast(R.string.file_saved)
            finish()
        }
    }
}
