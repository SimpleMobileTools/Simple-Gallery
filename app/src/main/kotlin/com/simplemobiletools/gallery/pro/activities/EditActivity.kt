package com.simplemobiletools.gallery.pro.activities

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.graphics.Point
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.RelativeLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.REAL_FILE_PATH
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.helpers.isNougatPlus
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.gallery.pro.BuildConfig
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.adapters.FiltersAdapter
import com.simplemobiletools.gallery.pro.databinding.*
import com.simplemobiletools.gallery.pro.dialogs.OtherAspectRatioDialog
import com.simplemobiletools.gallery.pro.dialogs.ResizeDialog
import com.simplemobiletools.gallery.pro.dialogs.SaveAsDialog
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.copyNonDimensionAttributesTo
import com.simplemobiletools.gallery.pro.extensions.fixDateTaken
import com.simplemobiletools.gallery.pro.extensions.openEditor
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.models.FilterItem
import com.theartofdev.edmodo.cropper.CropImageView
import com.zomato.photofilters.FilterPack
import com.zomato.photofilters.imageprocessors.Filter
import java.io.*

class EditActivity : SimpleActivity(), CropImageView.OnCropImageCompleteListener {
    companion object {
        init {
            System.loadLibrary("NativeImageProcessor")
        }
    }

    private lateinit var activityEditBinding: ActivityEditBinding
    private lateinit var drawActionsBinding: BottomEditorDrawActionsBinding
    private lateinit var primaryActionsBinding: BottomEditorPrimaryActionsBinding
    private lateinit var aspectRatiosBinding: BottomActionsAspectRatioBinding
    private lateinit var rotateActionsBinding: BottomEditorCropRotateActionsBinding
    private lateinit var filterActionsBinding: BottomEditorActionsFilterBinding

    private val TEMP_FOLDER_NAME = "images"
    private val ASPECT_X = "aspectX"
    private val ASPECT_Y = "aspectY"
    private val CROP = "crop"

    // constants for bottom primary action groups
    private val PRIMARY_ACTION_NONE = 0
    private val PRIMARY_ACTION_FILTER = 1
    private val PRIMARY_ACTION_CROP_ROTATE = 2
    private val PRIMARY_ACTION_DRAW = 3

    private val CROP_ROTATE_NONE = 0
    private val CROP_ROTATE_ASPECT_RATIO = 1

    private lateinit var saveUri: Uri
    private var uri: Uri? = null
    private var resizeWidth = 0
    private var resizeHeight = 0
    private var drawColor = 0
    private var lastOtherAspectRatio: Pair<Float, Float>? = null
    private var currPrimaryAction = PRIMARY_ACTION_NONE
    private var currCropRotateAction = CROP_ROTATE_ASPECT_RATIO
    private var currAspectRatio = ASPECT_RATIO_FREE
    private var isCropIntent = false
    private var isEditingWithThirdParty = false
    private var isSharingBitmap = false
    private var wasDrawCanvasPositioned = false
    private var oldExif: ExifInterface? = null
    private var filterInitialBitmap: Bitmap? = null
    private var originalUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityEditBinding = ActivityEditBinding.inflate(layoutInflater)
        drawActionsBinding = activityEditBinding.bottomEditorDrawActions
        primaryActionsBinding = activityEditBinding.bottomEditorPrimaryActions
        aspectRatiosBinding = activityEditBinding.bottomAspectRatios
        rotateActionsBinding = activityEditBinding.bottomEditorCropRotateActions
        filterActionsBinding = activityEditBinding.bottomEditorFilterActions
        setContentView(activityEditBinding.root)

        if (checkAppSideloading()) {
            return
        }

        initEditActivity()
    }

    override fun onResume() {
        super.onResume()
        isEditingWithThirdParty = false
        drawActionsBinding.bottomDrawWidth.setColors(config.textColor, getAdjustedPrimaryColor(), config.backgroundColor)
    }

    override fun onStop() {
        super.onStop()
        if (isEditingWithThirdParty) {
            finish()
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
            R.id.edit -> editWith()
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
        originalUri = uri
        if (uri!!.scheme != "file" && uri!!.scheme != "content") {
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
            (getRealPathFromURI(uri!!))?.apply {
                uri = Uri.fromFile(File(this))
            }
        }

        saveUri = when {
            intent.extras?.containsKey(MediaStore.EXTRA_OUTPUT) == true -> intent.extras!!.get(MediaStore.EXTRA_OUTPUT) as Uri
            else -> uri!!
        }

        isCropIntent = intent.extras?.get(CROP) == "true"
        if (isCropIntent) {
            primaryActionsBinding.root.beGone()
            (rotateActionsBinding.root.layoutParams as RelativeLayout.LayoutParams)
                    .addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1)
        }

        loadDefaultImageView()
        setupBottomActions()

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
        activityEditBinding.cropImageView.guidelines = CropImageView.Guidelines.ON
        aspectRatiosBinding.root.beVisible()
    }

    private fun loadDefaultImageView() {
        activityEditBinding.defaultImageView.beVisible()
        activityEditBinding.cropImageView.beGone()
        activityEditBinding.editorDrawCanvas.beGone()

        val options = RequestOptions()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)

        Glide.with(this)
            .asBitmap()
            .load(uri)
            .apply(options)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                    if (uri != originalUri) {
                        uri = originalUri
                        Handler().post {
                            loadDefaultImageView()
                        }
                    }
                    return false
                }

                override fun onResourceReady(bitmap: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    val currentFilter = getFiltersAdapter()?.getCurrentFilter()
                    if (filterInitialBitmap == null) {
                        loadCropImageView()
                        bottomCropRotateClicked()
                    }

                    if (filterInitialBitmap != null && currentFilter != null && currentFilter.filter.name != getString(R.string.none)) {
                        activityEditBinding.defaultImageView.onGlobalLayout {
                            applyFilter(currentFilter)
                        }
                    } else {
                        filterInitialBitmap = bitmap
                    }

                    if (isCropIntent) {
                        primaryActionsBinding.bottomPrimaryFilter.beGone()
                        primaryActionsBinding.bottomPrimaryDraw.beGone()
                    }

                    return false
                }
            }).into(activityEditBinding.defaultImageView)
    }

    private fun loadCropImageView() {
        activityEditBinding.defaultImageView.beGone()
        activityEditBinding.editorDrawCanvas.beGone()
        activityEditBinding.cropImageView.apply {
            beVisible()
            setOnCropImageCompleteListener(this@EditActivity)
            setImageUriAsync(uri)
            guidelines = CropImageView.Guidelines.ON

            if (isCropIntent && shouldCropSquare()) {
                currAspectRatio = ASPECT_RATIO_ONE_ONE
                setFixedAspectRatio(true)
                aspectRatiosBinding.root.beGone()
            }
        }
    }

    private fun loadDrawCanvas() {
        activityEditBinding.defaultImageView.beGone()
        activityEditBinding.cropImageView.beGone()
        activityEditBinding.editorDrawCanvas.beVisible()

        if (!wasDrawCanvasPositioned) {
            wasDrawCanvasPositioned = true
            activityEditBinding.editorDrawCanvas.onGlobalLayout {
                ensureBackgroundThread {
                    fillCanvasBackground()
                }
            }
        }
    }

    private fun fillCanvasBackground() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val options = RequestOptions()
            .format(DecodeFormat.PREFER_ARGB_8888)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .fitCenter()

        try {
            val builder = Glide.with(applicationContext)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(activityEditBinding.editorDrawCanvas.width, activityEditBinding.editorDrawCanvas.height)

            val bitmap = builder.get()
            runOnUiThread {
                activityEditBinding.editorDrawCanvas.apply {
                    updateBackgroundBitmap(bitmap)
                    layoutParams.width = bitmap.width
                    layoutParams.height = bitmap.height
                    y = (height - bitmap.height) / 2f
                    requestLayout()
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun saveImage() {
        setOldExif()

        if (activityEditBinding.cropImageView.isVisible()) {
            activityEditBinding.cropImageView.getCroppedImageAsync()
        } else if (activityEditBinding.editorDrawCanvas.isVisible()) {
            val bitmap = activityEditBinding.editorDrawCanvas.getBitmap()
            if (saveUri.scheme == "file") {
                SaveAsDialog(this, saveUri.path!!, true) {
                    saveBitmapToFile(bitmap, it, true)
                }
            } else if (saveUri.scheme == "content") {
                val filePathGetter = getNewFilePath()
                SaveAsDialog(this, filePathGetter.first, filePathGetter.second) {
                    saveBitmapToFile(bitmap, it, true)
                }
            }
        } else {
            val currentFilter = getFiltersAdapter()?.getCurrentFilter() ?: return
            val filePathGetter = getNewFilePath()
            SaveAsDialog(this, filePathGetter.first, filePathGetter.second) {
                toast(R.string.saving)

                // clean up everything to free as much memory as possible
                activityEditBinding.defaultImageView.setImageResource(0)
                activityEditBinding.cropImageView.setImageBitmap(null)
                filterActionsBinding.bottomActionsFilterList.adapter = null
                filterActionsBinding.bottomActionsFilterList.beGone()

                ensureBackgroundThread {
                    try {
                        val originalBitmap = Glide.with(applicationContext).asBitmap().load(uri).submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get()
                        currentFilter.filter.processFilter(originalBitmap)
                        saveBitmapToFile(originalBitmap, it, false)
                    } catch (e: OutOfMemoryError) {
                        toast(R.string.out_of_memory_error)
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun setOldExif() {
        var inputStream: InputStream? = null
        try {
            if (isNougatPlus()) {
                inputStream = contentResolver.openInputStream(uri!!)
                oldExif = ExifInterface(inputStream!!)
            }
        } catch (e: Exception) {
        } finally {
            inputStream?.close()
        }
    }

    private fun shareImage() {
        ensureBackgroundThread {
            when {
                activityEditBinding.defaultImageView.isVisible() -> {
                    val currentFilter = getFiltersAdapter()?.getCurrentFilter()
                    if (currentFilter == null) {
                        toast(R.string.unknown_error_occurred)
                        return@ensureBackgroundThread
                    }

                    val originalBitmap = Glide.with(applicationContext).asBitmap().load(uri).submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get()
                    currentFilter.filter.processFilter(originalBitmap)
                    shareBitmap(originalBitmap)
                }
                activityEditBinding.cropImageView.isVisible() -> {
                    isSharingBitmap = true
                    runOnUiThread {
                        activityEditBinding.cropImageView.getCroppedImageAsync()
                    }
                }
                activityEditBinding.editorDrawCanvas.isVisible() -> shareBitmap(activityEditBinding.editorDrawCanvas.getBitmap())
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

    private fun getFiltersAdapter() = filterActionsBinding.bottomActionsFilterList.adapter as? FiltersAdapter

    private fun setupBottomActions() {
        setupPrimaryActionButtons()
        setupCropRotateActionButtons()
        setupAspectRatioButtons()
        setupDrawButtons()
    }

    private fun setupPrimaryActionButtons() {
        primaryActionsBinding.bottomPrimaryFilter.setOnClickListener {
            bottomFilterClicked()
        }

        primaryActionsBinding.bottomPrimaryCropRotate.setOnClickListener {
            bottomCropRotateClicked()
        }

        primaryActionsBinding.bottomPrimaryDraw.setOnClickListener {
            bottomDrawClicked()
        }
    }

    private fun bottomFilterClicked() {
        currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_FILTER) {
            PRIMARY_ACTION_NONE
        } else {
            PRIMARY_ACTION_FILTER
        }
        updatePrimaryActionButtons()
    }

    private fun bottomCropRotateClicked() {
        currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE) {
            PRIMARY_ACTION_NONE
        } else {
            PRIMARY_ACTION_CROP_ROTATE
        }
        updatePrimaryActionButtons()
    }

    private fun bottomDrawClicked() {
        currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_DRAW) {
            PRIMARY_ACTION_NONE
        } else {
            PRIMARY_ACTION_DRAW
        }
        updatePrimaryActionButtons()
    }

    private fun setupCropRotateActionButtons() {
        rotateActionsBinding.bottomRotate.setOnClickListener {
            activityEditBinding.cropImageView.rotateImage(90)
        }

        rotateActionsBinding.bottomResize.beGoneIf(isCropIntent)
        rotateActionsBinding.bottomResize.setOnClickListener {
            resizeImage()
        }

        rotateActionsBinding.bottomFlipHorizontally.setOnClickListener {
            activityEditBinding.cropImageView.flipImageHorizontally()
        }

        rotateActionsBinding.bottomFlipVertically.setOnClickListener {
            activityEditBinding.cropImageView.flipImageVertically()
        }

        rotateActionsBinding.bottomAspectRatio.setOnClickListener {
            currCropRotateAction = if (currCropRotateAction == CROP_ROTATE_ASPECT_RATIO) {
                activityEditBinding.cropImageView.guidelines = CropImageView.Guidelines.OFF
                aspectRatiosBinding.root.beGone()
                CROP_ROTATE_NONE
            } else {
                activityEditBinding.cropImageView.guidelines = CropImageView.Guidelines.ON
                aspectRatiosBinding.root.beVisible()
                CROP_ROTATE_ASPECT_RATIO
            }
            updateCropRotateActionButtons()
        }
    }

    private fun setupAspectRatioButtons() {
        aspectRatiosBinding.bottomAspectRatioFree.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_FREE)
        }

        aspectRatiosBinding.bottomAspectRatioOneOne.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_ONE_ONE)
        }

        aspectRatiosBinding.bottomAspectRatioFourThree.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_FOUR_THREE)
        }

        aspectRatiosBinding.bottomAspectRatioSixteenNine.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_SIXTEEN_NINE)
        }

        aspectRatiosBinding.bottomAspectRatioOther.setOnClickListener {
            OtherAspectRatioDialog(this, lastOtherAspectRatio) {
                lastOtherAspectRatio = it
                config.lastEditorCropOtherAspectRatioX = it.first
                config.lastEditorCropOtherAspectRatioY = it.second
                updateAspectRatio(ASPECT_RATIO_OTHER)
            }
        }

        updateAspectRatioButtons()
    }

    private fun setupDrawButtons() {
        updateDrawColor(config.lastEditorDrawColor)
        drawActionsBinding.bottomDrawWidth.progress = config.lastEditorBrushSize
        updateBrushSize(config.lastEditorBrushSize)

        drawActionsBinding.bottomDrawColorClickable.setOnClickListener {
            ColorPickerDialog(this, drawColor) { wasPositivePressed, color ->
                if (wasPositivePressed) {
                    updateDrawColor(color)
                }
            }
        }

        drawActionsBinding.bottomDrawWidth.onSeekBarChangeListener {
            config.lastEditorBrushSize = it
            updateBrushSize(it)
        }

        drawActionsBinding.bottomDrawUndo.setOnClickListener {
            activityEditBinding.editorDrawCanvas.undo()
        }
    }

    private fun updateBrushSize(percent: Int) {
        activityEditBinding.editorDrawCanvas.updateBrushSize(percent)
        val scale = 0.03f.coerceAtLeast(percent / 100f)
        drawActionsBinding.bottomDrawColor.scaleX = scale
        drawActionsBinding.bottomDrawColor.scaleY = scale
    }

    private fun updatePrimaryActionButtons() {
        if (activityEditBinding.cropImageView.isGone() && currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE) {
            loadCropImageView()
        } else if (activityEditBinding.defaultImageView.isGone() && currPrimaryAction == PRIMARY_ACTION_FILTER) {
            loadDefaultImageView()
        } else if (activityEditBinding.editorDrawCanvas.isGone() && currPrimaryAction == PRIMARY_ACTION_DRAW) {
            loadDrawCanvas()
        }

        arrayOf(primaryActionsBinding.bottomPrimaryFilter, primaryActionsBinding.bottomPrimaryCropRotate,
                primaryActionsBinding.bottomPrimaryDraw).forEach {
            it.applyColorFilter(Color.WHITE)
        }

        val currentPrimaryActionButton = when (currPrimaryAction) {
            PRIMARY_ACTION_FILTER -> primaryActionsBinding.bottomPrimaryFilter
            PRIMARY_ACTION_CROP_ROTATE -> primaryActionsBinding.bottomPrimaryCropRotate
            PRIMARY_ACTION_DRAW -> primaryActionsBinding.bottomPrimaryDraw
            else -> null
        }

        currentPrimaryActionButton?.applyColorFilter(getAdjustedPrimaryColor())
        filterActionsBinding.root.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_FILTER)
        rotateActionsBinding.root.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE)
        drawActionsBinding.root.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_DRAW)

        if (currPrimaryAction == PRIMARY_ACTION_FILTER && filterActionsBinding.bottomActionsFilterList.adapter == null) {
            ensureBackgroundThread {
                val thumbnailSize = resources.getDimension(R.dimen.bottom_filters_thumbnail_size).toInt()

                val bitmap = try {
                    Glide.with(this)
                        .asBitmap()
                        .load(uri).listener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                                showErrorToast(e.toString())
                                return false
                            }

                            override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean) = false
                        })
                        .submit(thumbnailSize, thumbnailSize)
                        .get()
                } catch (e: GlideException) {
                    showErrorToast(e)
                    finish()
                    return@ensureBackgroundThread
                }

                runOnUiThread {
                    val filterThumbnailsManager = FilterThumbnailsManager()
                    filterThumbnailsManager.clearThumbs()

                    val noFilter = Filter(getString(R.string.none))
                    filterThumbnailsManager.addThumb(FilterItem(bitmap, noFilter))

                    FilterPack.getFilterPack(this).forEach {
                        val filterItem = FilterItem(bitmap, it)
                        filterThumbnailsManager.addThumb(filterItem)
                    }

                    val filterItems = filterThumbnailsManager.processThumbs()
                    val adapter = FiltersAdapter(applicationContext, filterItems) {
                        val layoutManager = filterActionsBinding.bottomActionsFilterList.layoutManager as LinearLayoutManager
                        applyFilter(filterItems[it])

                        if (it == layoutManager.findLastCompletelyVisibleItemPosition() || it == layoutManager.findLastVisibleItemPosition()) {
                            filterActionsBinding.bottomActionsFilterList.smoothScrollBy(thumbnailSize, 0)
                        } else if (it == layoutManager.findFirstCompletelyVisibleItemPosition() || it == layoutManager.findFirstVisibleItemPosition()) {
                            filterActionsBinding.bottomActionsFilterList.smoothScrollBy(-thumbnailSize, 0)
                        }
                    }

                    filterActionsBinding.bottomActionsFilterList.adapter = adapter
                    adapter.notifyDataSetChanged()
                }
            }
        }

        if (currPrimaryAction != PRIMARY_ACTION_CROP_ROTATE) {
            aspectRatiosBinding.root.beGone()
            currCropRotateAction = CROP_ROTATE_NONE
        }
        updateCropRotateActionButtons()
    }

    private fun applyFilter(filterItem: FilterItem) {
        val newBitmap = Bitmap.createBitmap(filterInitialBitmap!!)
        activityEditBinding.defaultImageView.setImageBitmap(filterItem.filter.processFilter(newBitmap))
    }

    private fun updateAspectRatio(aspectRatio: Int) {
        currAspectRatio = aspectRatio
        config.lastEditorCropAspectRatio = aspectRatio
        updateAspectRatioButtons()

        activityEditBinding.cropImageView.apply {
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
        arrayOf(aspectRatiosBinding.bottomAspectRatioFree, aspectRatiosBinding.bottomAspectRatioOneOne,
                aspectRatiosBinding.bottomAspectRatioFourThree, aspectRatiosBinding.bottomAspectRatioSixteenNine,
                aspectRatiosBinding.bottomAspectRatioOther).forEach {
            it.setTextColor(Color.WHITE)
        }

        val currentAspectRatioButton = when (currAspectRatio) {
            ASPECT_RATIO_FREE -> aspectRatiosBinding.bottomAspectRatioFree
            ASPECT_RATIO_ONE_ONE -> aspectRatiosBinding.bottomAspectRatioOneOne
            ASPECT_RATIO_FOUR_THREE -> aspectRatiosBinding.bottomAspectRatioFourThree
            ASPECT_RATIO_SIXTEEN_NINE -> aspectRatiosBinding.bottomAspectRatioSixteenNine
            else -> aspectRatiosBinding.bottomAspectRatioOther
        }

        currentAspectRatioButton.setTextColor(getAdjustedPrimaryColor())
    }

    private fun updateCropRotateActionButtons() {
        arrayOf(rotateActionsBinding.bottomAspectRatio).forEach {
            it.applyColorFilter(Color.WHITE)
        }

        val primaryActionView = when (currCropRotateAction) {
            CROP_ROTATE_ASPECT_RATIO -> rotateActionsBinding.bottomAspectRatio
            else -> null
        }

        primaryActionView?.applyColorFilter(getAdjustedPrimaryColor())
    }

    private fun updateDrawColor(color: Int) {
        drawColor = color
        drawActionsBinding.bottomDrawColor.applyColorFilter(color)
        config.lastEditorDrawColor = color
        activityEditBinding.editorDrawCanvas.updateColor(color)
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
            activityEditBinding.cropImageView.getCroppedImageAsync()
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
        val rect = activityEditBinding.cropImageView.cropRect ?: return null
        val rotation = activityEditBinding.cropImageView.rotatedDegrees
        return if (rotation == 0 || rotation == 180) {
            Point(rect.width(), rect.height())
        } else {
            Point(rect.height(), rect.width())
        }
    }

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        if (result.error == null) {
            setOldExif()

            val bitmap = result.bitmap
            if (isSharingBitmap) {
                isSharingBitmap = false
                shareBitmap(bitmap)
                return
            }

            if (isCropIntent) {
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
            } else if (saveUri.scheme == "file") {
                SaveAsDialog(this, saveUri.path!!, true) {
                    saveBitmapToFile(bitmap, it, true)
                }
            } else if (saveUri.scheme == "content") {
                val filePathGetter = getNewFilePath()
                SaveAsDialog(this, filePathGetter.first, filePathGetter.second) {
                    saveBitmapToFile(bitmap, it, true)
                }
            } else {
                toast(R.string.unknown_file_location)
            }
        } else {
            toast("${getString(R.string.image_editing_failed)}: ${result.error.message}")
        }
    }

    private fun getNewFilePath(): Pair<String, Boolean> {
        var newPath = applicationContext.getRealPathFromURI(saveUri) ?: ""
        if (newPath.startsWith("/mnt/")) {
            newPath = ""
        }

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

        return Pair(newPath, shouldAppendFilename)
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
                oldExif?.copyNonDimensionAttributesTo(newExif)
            }
        } catch (e: Exception) {
        }

        setResult(Activity.RESULT_OK, intent)
        scanFinalPath(file.absolutePath)
        out.close()
    }

    private fun editWith() {
        openEditor(uri.toString(), true)
        isEditingWithThirdParty = true
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
