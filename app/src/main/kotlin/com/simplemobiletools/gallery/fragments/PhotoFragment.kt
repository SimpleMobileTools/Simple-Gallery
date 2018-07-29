package com.simplemobiletools.gallery.fragments

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface.*
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.OTG_PATH
import com.simplemobiletools.commons.helpers.isLollipopPlus
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.PanoramaActivity
import com.simplemobiletools.gallery.activities.PhotoActivity
import com.simplemobiletools.gallery.activities.ViewPagerActivity
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.GlideRotateTransformation
import com.simplemobiletools.gallery.helpers.MEDIUM
import com.simplemobiletools.gallery.helpers.PATH
import com.simplemobiletools.gallery.helpers.ROTATE_BY_ASPECT_RATIO
import com.simplemobiletools.gallery.models.Medium
import it.sephiroth.android.library.exif2.ExifInterface
import kotlinx.android.synthetic.main.pager_photo_item.view.*
import org.apache.sanselan.common.byteSources.ByteSourceInputStream
import org.apache.sanselan.formats.jpeg.JpegImageParser
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.io.FileOutputStream

class PhotoFragment : ViewPagerFragment() {
    private val DEFAULT_DOUBLE_TAP_ZOOM = 2f
    private val ZOOMABLE_VIEW_LOAD_DELAY = 1500L

    private var isFragmentVisible = false
    private var isFullscreen = false
    private var wasInit = false
    private var useHalfResolution = false
    private var isPanorama = false
    private var imageOrientation = -1
    private var gifDrawable: GifDrawable? = null
    private var loadZoomableViewHandler = Handler()

    private var storedShowExtendedDetails = false
    private var storedHideExtendedDetails = false
    private var storedExtendedDetails = 0

    lateinit var view: ViewGroup
    lateinit var medium: Medium

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        view = (inflater.inflate(R.layout.pager_photo_item, container, false) as ViewGroup).apply {
            subsampling_view.setOnClickListener { photoClicked() }
            photo_view.setOnClickListener { photoClicked() }
            instant_prev_item.setOnClickListener { listener?.goToPrevItem() }
            instant_next_item.setOnClickListener { listener?.goToNextItem() }
            panorama_outline.setOnClickListener { openPanorama() }

            instant_prev_item.parentView = container
            instant_next_item.parentView = container

            photo_brightness_controller.initialize(activity!!, slide_info, true, container) { x, y ->
                view.apply {
                    if (subsampling_view.isVisible()) {
                        subsampling_view.sendFakeClick(x, y)
                    } else {
                        photo_view.sendFakeClick(x, y)
                    }
                }
            }
        }

        storeStateVariables()
        if (!isFragmentVisible && activity is PhotoActivity) {
            isFragmentVisible = true
        }

        medium = arguments!!.getSerializable(MEDIUM) as Medium
        if (medium.path.startsWith("content://") && !medium.path.startsWith("content://mms/")) {
            val originalPath = medium.path
            medium.path = context!!.getRealPathFromURI(Uri.parse(originalPath)) ?: medium.path

            if (medium.path.isEmpty()) {
                var out: FileOutputStream? = null
                try {
                    var inputStream = context!!.contentResolver.openInputStream(Uri.parse(originalPath))
                    val exif = ExifInterface()
                    exif.readExif(inputStream, ExifInterface.Options.OPTION_ALL)
                    val tag = exif.getTag(ExifInterface.TAG_ORIENTATION)
                    val orientation = tag?.getValueAsInt(-1) ?: -1
                    inputStream = context!!.contentResolver.openInputStream(Uri.parse(originalPath))
                    val original = BitmapFactory.decodeStream(inputStream)
                    val rotated = rotateViaMatrix(original, orientation)
                    exif.setTagValue(ExifInterface.TAG_ORIENTATION, 1)
                    exif.removeCompressedThumbnail()

                    val file = File(context!!.externalCacheDir, Uri.parse(originalPath).lastPathSegment)
                    out = FileOutputStream(file)
                    rotated.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    medium.path = file.absolutePath
                } catch (e: Exception) {
                    activity!!.toast(R.string.unknown_error_occurred)
                    return view
                } finally {
                    out?.close()
                }
            }
        }

        isFullscreen = activity!!.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
        loadImage()
        initExtendedDetails()
        wasInit = true
        checkIfPanorama()

        return view
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onResume() {
        super.onResume()
        if (wasInit && (context!!.config.showExtendedDetails != storedShowExtendedDetails || context!!.config.extendedDetails != storedExtendedDetails)) {
            initExtendedDetails()
        }

        val allowPhotoGestures = context!!.config.allowPhotoGestures
        val allowInstantChange = context!!.config.allowInstantChange

        view.apply {
            photo_brightness_controller.beVisibleIf(allowPhotoGestures)
            instant_prev_item.beVisibleIf(allowInstantChange)
            instant_next_item.beVisibleIf(allowInstantChange)
        }

        storeStateVariables()
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        isFragmentVisible = menuVisible
        if (wasInit) {
            if (medium.isGif()) {
                gifFragmentVisibilityChanged(menuVisible)
            } else {
                photoFragmentVisibilityChanged(menuVisible)
            }
        }
    }

    private fun storeStateVariables() {
        context!!.config.apply {
            storedShowExtendedDetails = showExtendedDetails
            storedHideExtendedDetails = hideExtendedDetails
            storedExtendedDetails = extendedDetails
        }
    }

    private fun gifFragmentVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            gifDrawable?.start()
        } else {
            gifDrawable?.stop()
        }
    }

    private fun photoFragmentVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            scheduleZoomableView()
        } else {
            loadZoomableViewHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun degreesForRotation(orientation: Int) = when (orientation) {
        ORIENTATION_ROTATE_270 -> 270
        ORIENTATION_ROTATE_180 -> 180
        ORIENTATION_ROTATE_90 -> 90
        else -> 0
    }

    private fun rotateViaMatrix(original: Bitmap, orientation: Int): Bitmap {
        val degrees = degreesForRotation(orientation).toFloat()
        return if (degrees == 0f) {
            original
        } else {
            val matrix = Matrix()
            matrix.setRotate(degrees)
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        }
    }

    private fun loadImage() {
        imageOrientation = getImageOrientation()
        if (medium.isGif()) {
            loadGif()
        } else {
            loadBitmap()
        }
    }

    private fun loadGif() {
        try {
            val pathToLoad = getPathToLoad(medium)
            gifDrawable = if (pathToLoad.startsWith("content://") || pathToLoad.startsWith("file://")) {
                GifDrawable(context!!.contentResolver, Uri.parse(pathToLoad))
            } else {
                GifDrawable(pathToLoad)
            }

            if (!isFragmentVisible) {
                gifDrawable!!.stop()
            }

            view.photo_view.setImageDrawable(gifDrawable)
        } catch (e: Exception) {
            gifDrawable = null
            loadBitmap()
        } catch (e: OutOfMemoryError) {
            gifDrawable = null
            loadBitmap()
        }
    }

    private fun loadBitmap(degrees: Int = 0) {
        if (degrees == 0) {
            var targetWidth = if (ViewPagerActivity.screenWidth == 0) Target.SIZE_ORIGINAL else ViewPagerActivity.screenWidth
            var targetHeight = if (ViewPagerActivity.screenHeight == 0) Target.SIZE_ORIGINAL else ViewPagerActivity.screenHeight
            if (useHalfResolution) {
                targetWidth /= 2
                targetHeight /= 2
            }

            if (imageOrientation == ORIENTATION_ROTATE_90) {
                targetWidth = targetHeight
                targetHeight = Target.SIZE_ORIGINAL
            }

            val options = RequestOptions()
                    .signature(medium.path.getFileSignature())
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(targetWidth, targetHeight)

            Glide.with(this)
                    .asBitmap()
                    .load(getPathToLoad(medium))
                    .apply(options)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                            if (!useHalfResolution && e?.rootCauses?.firstOrNull() is OutOfMemoryError) {
                                useHalfResolution = true
                                Handler().post {
                                    if (activity?.isActivityDestroyed() == false) {
                                        loadBitmap(degrees)
                                    }
                                }
                            }
                            return false
                        }

                        override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            if (isFragmentVisible) {
                                scheduleZoomableView()
                            }
                            return false
                        }
                    }).into(view.photo_view)
        } else {
            val options = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .transform(GlideRotateTransformation(degrees))

            Glide.with(this)
                    .asBitmap()
                    .load(getPathToLoad(medium))
                    .thumbnail(0.2f)
                    .apply(options)
                    .into(view.photo_view)
        }
    }

    private fun openPanorama() {
        Intent(context, PanoramaActivity::class.java).apply {
            putExtra(PATH, medium.path)
            startActivity(this)
        }
    }

    private fun scheduleZoomableView() {
        loadZoomableViewHandler.removeCallbacksAndMessages(null)
        loadZoomableViewHandler.postDelayed({
            if (isFragmentVisible && !context!!.config.replaceZoomableImages && medium.isImage() && view.subsampling_view.isGone()) {
                addZoomableView()
            }
        }, ZOOMABLE_VIEW_LOAD_DELAY)
    }

    private fun addZoomableView() {
        ViewPagerActivity.wasDecodedByGlide = false
        view.subsampling_view.apply {
            maxScale = 10f
            beVisible()
            isQuickScaleEnabled = context.config.oneFingerZoom
            setResetScaleOnSizeChange(context.config.screenRotation != ROTATE_BY_ASPECT_RATIO)
            setImage(ImageSource.uri(getPathToLoad(medium)))
            orientation = if (imageOrientation == -1) SubsamplingScaleImageView.ORIENTATION_USE_EXIF else degreesForRotation(imageOrientation)
            setEagerLoadingEnabled(false)
            setExecutor(AsyncTask.SERIAL_EXECUTOR)
            setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
                override fun onImageLoaded() {
                }

                override fun onReady() {
                    background = ColorDrawable(if (context.config.blackBackground) Color.BLACK else context.config.backgroundColor)
                    val useWidth = if (imageOrientation == ORIENTATION_ROTATE_90 || imageOrientation == ORIENTATION_ROTATE_270) sHeight else sWidth
                    val useHeight = if (imageOrientation == ORIENTATION_ROTATE_90 || imageOrientation == ORIENTATION_ROTATE_270) sWidth else sHeight
                    setDoubleTapZoomScale(getDoubleTapZoomScale(useWidth, useHeight))
                }

                override fun onTileLoadError(e: Exception?) {
                }

                override fun onPreviewReleased() {
                }

                override fun onImageLoadError(e: Exception) {
                    background = ColorDrawable(Color.TRANSPARENT)
                    beGone()
                }

                override fun onPreviewLoadError(e: Exception?) {
                    background = ColorDrawable(Color.TRANSPARENT)
                    beGone()
                }
            })
        }
    }

    private fun checkIfPanorama() {
        isPanorama = try {
            val inputStream = if (medium.path.startsWith("content:/")) context!!.contentResolver.openInputStream(Uri.parse(medium.path)) else File(medium.path).inputStream()
            val imageParser = JpegImageParser().getXmpXml(ByteSourceInputStream(inputStream, medium.name), HashMap<String, Any>())
            imageParser.contains("GPano:UsePanoramaViewer=\"True\"", true) || imageParser.contains("<GPano:UsePanoramaViewer>True</GPano:UsePanoramaViewer>", true)
        } catch (e: Exception) {
            false
        } catch (e: OutOfMemoryError) {
            false
        }

        view.panorama_outline.beVisibleIf(isPanorama && isLollipopPlus())
    }

    private fun getImageOrientation(): Int {
        val defaultOrientation = -1
        var orient = defaultOrientation

        try {
            val pathToLoad = getPathToLoad(medium)
            val exif = android.media.ExifInterface(pathToLoad)
            orient = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, defaultOrientation)

            if (orient == defaultOrientation || medium.path.startsWith(OTG_PATH)) {
                val uri = if (pathToLoad.startsWith("content:/")) Uri.parse(pathToLoad) else Uri.fromFile(File(pathToLoad))
                val inputStream = context!!.contentResolver.openInputStream(uri)
                val exif2 = ExifInterface()
                exif2.readExif(inputStream, ExifInterface.Options.OPTION_ALL)
                orient = exif2.getTag(ExifInterface.TAG_ORIENTATION)?.getValueAsInt(defaultOrientation) ?: defaultOrientation
            }
        } catch (ignored: Exception) {
        } catch (ignored: OutOfMemoryError) {
        }
        return orient
    }

    private fun getDoubleTapZoomScale(width: Int, height: Int): Float {
        val bitmapAspectRatio = height / width.toFloat()
        val screenAspectRatio = ViewPagerActivity.screenHeight / ViewPagerActivity.screenWidth.toFloat()

        return if (context == null || bitmapAspectRatio == screenAspectRatio) {
            DEFAULT_DOUBLE_TAP_ZOOM
        } else if (ViewPagerActivity.wasDecodedByGlide) {
            1f
        } else if (context!!.portrait && bitmapAspectRatio <= screenAspectRatio) {
            ViewPagerActivity.screenHeight / height.toFloat()
        } else if (context!!.portrait && bitmapAspectRatio > screenAspectRatio) {
            ViewPagerActivity.screenWidth / width.toFloat()
        } else if (!context!!.portrait && bitmapAspectRatio >= screenAspectRatio) {
            ViewPagerActivity.screenWidth / width.toFloat()
        } else if (!context!!.portrait && bitmapAspectRatio < screenAspectRatio) {
            ViewPagerActivity.screenHeight / height.toFloat()
        } else {
            DEFAULT_DOUBLE_TAP_ZOOM
        }
    }

    fun rotateImageViewBy(degrees: Int) {
        view.subsampling_view.beGone()
        loadBitmap(degrees)
    }

    private fun initExtendedDetails() {
        if (context!!.config.showExtendedDetails) {
            view.photo_details.apply {
                beInvisible()   // make it invisible so we can measure it, but not show yet
                text = getMediumExtendedDetails(medium)
                onGlobalLayout {
                    if (isAdded) {
                        val realY = getExtendedDetailsY(height)
                        if (realY > 0) {
                            y = realY
                            beVisibleIf(text.isNotEmpty())
                            alpha = if (!context!!.config.hideExtendedDetails || !isFullscreen) 1f else 0f
                        }
                    }
                }
            }
        } else {
            view.photo_details.beGone()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (activity?.isActivityDestroyed() == false) {
            Glide.with(context!!).clear(view.photo_view)
            view.subsampling_view.recycle()
        }
        loadZoomableViewHandler.removeCallbacksAndMessages(null)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        loadImage()
        initExtendedDetails()
    }

    private fun photoClicked() {
        listener?.fragmentClicked()
    }

    override fun fullscreenToggled(isFullscreen: Boolean) {
        this.isFullscreen = isFullscreen
        view.photo_details.apply {
            if (storedShowExtendedDetails && isVisible()) {
                animate().y(getExtendedDetailsY(height))

                if (storedHideExtendedDetails) {
                    animate().alpha(if (isFullscreen) 0f else 1f).start()
                }
            }
        }
    }

    private fun getExtendedDetailsY(height: Int): Float {
        val smallMargin = resources.getDimension(R.dimen.small_margin)
        val fullscreenOffset = context!!.navigationBarHeight.toFloat() - smallMargin
        val actionsHeight = if (context!!.config.bottomActions && !isFullscreen) resources.getDimension(R.dimen.bottom_actions_height) else 0f
        return context!!.usableScreenSize.y - height - actionsHeight + if (isFullscreen) fullscreenOffset else -smallMargin
    }
}
