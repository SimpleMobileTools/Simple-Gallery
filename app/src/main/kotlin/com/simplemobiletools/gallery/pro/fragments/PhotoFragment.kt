package com.simplemobiletools.gallery.pro.fragments

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.media.ExifInterface.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.State
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.davemorrissey.labs.subscaleview.DecoderFactory
import com.davemorrissey.labs.subscaleview.ImageDecoder
import com.davemorrissey.labs.subscaleview.ImageRegionDecoder
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.OTG_PATH
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.PanoramaPhotoActivity
import com.simplemobiletools.gallery.pro.activities.PhotoActivity
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.models.Medium
import com.simplemobiletools.gallery.pro.svg.SvgSoftwareLayerSetter
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.sephiroth.android.library.exif2.ExifInterface
import kotlinx.android.synthetic.main.pager_photo_item.view.*
import org.apache.sanselan.common.byteSources.ByteSourceInputStream
import org.apache.sanselan.formats.jpeg.JpegImageParser
import pl.droidsonroids.gif.InputSource
import java.io.File
import java.io.FileOutputStream
import java.util.*

class PhotoFragment : ViewPagerFragment() {
    private val DEFAULT_DOUBLE_TAP_ZOOM = 2f
    private val ZOOMABLE_VIEW_LOAD_DELAY = 150L
    private val SAME_ASPECT_RATIO_THRESHOLD = 0.01

    // devices with good displays, but the rest of the hardware not good enough for them
    private val WEIRD_DEVICES = arrayListOf(
            "motorola xt1685",
            "google nexus 5x"
    )

    var mCurrentRotationDegrees = 0
    private var mIsFragmentVisible = false
    private var mIsFullscreen = false
    private var mWasInit = false
    private var mIsPanorama = false
    private var mIsSubsamplingVisible = false    // checking view.visibility is unreliable, use an extra variable for it
    private var mImageOrientation = -1
    private var mOriginalSubsamplingScale = 0f
    private var mLoadZoomableViewHandler = Handler()
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mCurrentGestureViewZoom = 1f

    private var mStoredShowExtendedDetails = false
    private var mStoredHideExtendedDetails = false
    private var mStoredAllowDeepZoomableImages = false
    private var mStoredShowHighestQuality = false
    private var mStoredAllowOneFingerZoom = false
    private var mStoredExtendedDetails = 0

    private lateinit var mView: ViewGroup
    private lateinit var mMedium: Medium

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mView = (inflater.inflate(R.layout.pager_photo_item, container, false) as ViewGroup).apply {
            subsampling_view.setOnClickListener { photoClicked() }
            gestures_view.setOnClickListener { photoClicked() }
            gif_view.setOnClickListener { photoClicked() }
            instant_prev_item.setOnClickListener { listener?.goToPrevItem() }
            instant_next_item.setOnClickListener { listener?.goToNextItem() }
            panorama_outline.setOnClickListener { openPanorama() }

            instant_prev_item.parentView = container
            instant_next_item.parentView = container

            photo_brightness_controller.initialize(activity!!, slide_info, true, container) { x, y ->
                mView.apply {
                    if (subsampling_view.isVisible()) {
                        subsampling_view.sendFakeClick(x, y)
                    } else {
                        gestures_view.sendFakeClick(x, y)
                    }
                }
            }

            if (context.config.allowDownGesture) {
                gif_view.setOnTouchListener { v, event ->
                    if (gif_view_frame.controller.state.zoom == 1f) {
                        handleEvent(event)
                    }
                    false
                }

                gestures_view.controller.addOnStateChangeListener(object : GestureController.OnStateChangeListener {
                    override fun onStateChanged(state: State) {
                        mCurrentGestureViewZoom = state.zoom
                    }
                })

                gestures_view.setOnTouchListener { v, event ->
                    if (mCurrentGestureViewZoom == 1f) {
                        handleEvent(event)
                    }
                    false
                }

                subsampling_view.setOnTouchListener { v, event ->
                    if (subsampling_view.scale == mOriginalSubsamplingScale) {
                        handleEvent(event)
                    }
                    false
                }
            }
        }

        checkScreenDimensions()
        storeStateVariables()
        if (!mIsFragmentVisible && activity is PhotoActivity) {
            mIsFragmentVisible = true
        }

        mMedium = arguments!!.getSerializable(MEDIUM) as Medium
        if (mMedium.path.startsWith("content://") && !mMedium.path.startsWith("content://mms/")) {
            val originalPath = mMedium.path
            mMedium.path = context!!.getRealPathFromURI(Uri.parse(originalPath)) ?: mMedium.path

            if (mMedium.path.isEmpty()) {
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
                    mMedium.path = file.absolutePath
                } catch (e: Exception) {
                    activity!!.toast(R.string.unknown_error_occurred)
                    return mView
                } finally {
                    out?.close()
                }
            }
        }

        mIsFullscreen = activity!!.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
        loadImage()
        initExtendedDetails()
        mWasInit = true
        checkIfPanorama()
        updateInstantSwitchWidths()

        return mView
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onResume() {
        super.onResume()
        val config = context!!.config
        if (mWasInit && (config.showExtendedDetails != mStoredShowExtendedDetails || config.extendedDetails != mStoredExtendedDetails)) {
            initExtendedDetails()
        }

        if (mWasInit) {
            if (config.allowZoomingImages != mStoredAllowDeepZoomableImages || config.showHighestQuality != mStoredShowHighestQuality ||
                    config.oneFingerZoom != mStoredAllowOneFingerZoom) {
                mIsSubsamplingVisible = false
                mView.subsampling_view.beGone()
                loadImage()
            } else if (mMedium.isGIF()) {
                loadGif()
            }
        }

        val allowPhotoGestures = config.allowPhotoGestures
        val allowInstantChange = config.allowInstantChange

        mView.apply {
            photo_brightness_controller.beVisibleIf(allowPhotoGestures)
            instant_prev_item.beVisibleIf(allowInstantChange)
            instant_next_item.beVisibleIf(allowInstantChange)
        }

        storeStateVariables()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (activity?.isDestroyed == false) {
            mView.subsampling_view.recycle()
        }

        mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
        if (mCurrentRotationDegrees != 0) {
            Thread {
                val path = mMedium.path
                (activity as? BaseSimpleActivity)?.saveRotatedImageToFile(path, path, mCurrentRotationDegrees, false) {}
            }.start()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // avoid GIFs being skewed, played in wrong aspect ratio
        if (mMedium.isGIF()) {
            mView.onGlobalLayout {
                measureScreen()
                Handler().postDelayed({
                    loadGif()
                }, 50)
            }
        } else {
            hideZoomableView()
            loadImage()
        }

        initExtendedDetails()
        updateInstantSwitchWidths()
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        mIsFragmentVisible = menuVisible
        if (mWasInit) {
            if (!mMedium.isGIF()) {
                photoFragmentVisibilityChanged(menuVisible)
            }
        }
    }

    private fun storeStateVariables() {
        context!!.config.apply {
            mStoredShowExtendedDetails = showExtendedDetails
            mStoredHideExtendedDetails = hideExtendedDetails
            mStoredAllowDeepZoomableImages = allowZoomingImages
            mStoredShowHighestQuality = showHighestQuality
            mStoredAllowOneFingerZoom = oneFingerZoom
            mStoredExtendedDetails = extendedDetails
        }
    }

    private fun checkScreenDimensions() {
        if (mScreenWidth == 0 || mScreenHeight == 0) {
            measureScreen()
        }
    }

    private fun measureScreen() {
        val metrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getRealMetrics(metrics)
        mScreenWidth = metrics.widthPixels
        mScreenHeight = metrics.heightPixels
    }

    private fun photoFragmentVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            scheduleZoomableView()
        } else {
            hideZoomableView()
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
        checkScreenDimensions()
        mImageOrientation = getImageOrientation()
        when {
            mMedium.isGIF() -> loadGif()
            mMedium.isSVG() -> loadSVG()
            else -> loadBitmap()
        }
    }

    private fun loadGif() {
        try {
            val pathToLoad = getPathToLoad(mMedium)
            val source = if (pathToLoad.startsWith("content://") || pathToLoad.startsWith("file://")) {
                InputSource.UriSource(context!!.contentResolver, Uri.parse(pathToLoad))
            } else {
                InputSource.FileSource(pathToLoad)
            }

            mView.apply {
                gestures_view.beGone()
                gif_view.setInputSource(source)
                gif_view_frame.beVisible()
            }
        } catch (e: Exception) {
            loadBitmap()
        } catch (e: OutOfMemoryError) {
            loadBitmap()
        }
    }

    private fun loadSVG() {
        Glide.with(context!!)
                .`as`(PictureDrawable::class.java)
                .listener(SvgSoftwareLayerSetter())
                .load(mMedium.path)
                .into(mView.gestures_view)
    }

    private fun loadBitmap(degrees: Int = mCurrentRotationDegrees) {
        val options = RequestOptions()
                .signature(mMedium.path.getFileSignature())
                .format(DecodeFormat.PREFER_ARGB_8888)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .fitCenter()

        if (degrees != 0) {
            options.transform(GlideRotateTransformation(degrees))
            options.diskCacheStrategy(DiskCacheStrategy.NONE)
        }

        Glide.with(context!!)
                .load(getPathToLoad(mMedium))
                .apply(options)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        if (activity != null) {
                            tryLoadingWithPicasso(degrees)
                        }
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        if (mIsFragmentVisible && degrees == 0) {
                            scheduleZoomableView()
                        }
                        return false
                    }
                }).into(mView.gestures_view)
    }

    private fun tryLoadingWithPicasso(degrees: Int = 0) {
        var pathToLoad = if (mMedium.path.startsWith("content://")) mMedium.path else "file://${mMedium.path}"
        pathToLoad = pathToLoad.replace("%", "%25").replace("#", "%23")

        try {
            val picasso = Picasso.get()
                    .load(pathToLoad)
                    .centerInside()
                    .stableKey(mMedium.path.getFileKey())
                    .resize(mScreenWidth, mScreenHeight)

            if (degrees != 0) {
                picasso.rotate(degrees.toFloat())
            } else {
                degreesForRotation(mImageOrientation).toFloat()
            }

            picasso.into(mView.gestures_view, object : Callback {
                override fun onSuccess() {
                    mView.gestures_view.controller.settings.isZoomEnabled = degrees != 0 || context?.config?.allowZoomingImages == false
                    if (mIsFragmentVisible && degrees == 0) {
                        scheduleZoomableView()
                    }
                }

                override fun onError(e: Exception) {}
            })
        } catch (ignored: Exception) {
        }
    }

    private fun openPanorama() {
        Intent(context, PanoramaPhotoActivity::class.java).apply {
            putExtra(PATH, mMedium.path)
            startActivity(this)
        }
    }

    private fun scheduleZoomableView() {
        mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
        mLoadZoomableViewHandler.postDelayed({
            if (mIsFragmentVisible && context?.config?.allowZoomingImages == true && mMedium.isImage() && !mIsSubsamplingVisible && mCurrentRotationDegrees == 0) {
                addZoomableView()
            }
        }, ZOOMABLE_VIEW_LOAD_DELAY)
    }

    private fun addZoomableView() {
        val rotation = degreesForRotation(mImageOrientation)
        val path = getPathToLoad(mMedium)
        mIsSubsamplingVisible = true

        val bitmapDecoder = object : DecoderFactory<ImageDecoder> {
            override fun make() = PicassoDecoder(path, Picasso.get(), rotation)
        }

        val regionDecoder = object : DecoderFactory<ImageRegionDecoder> {
            override fun make() = PicassoRegionDecoder()
        }

        val config = context!!.config
        mView.subsampling_view.apply {
            setMaxTileSize(if (config.showHighestQuality) Integer.MAX_VALUE else 4096)
            setMinimumTileDpi(if (config.showHighestQuality) -1 else getMinTileDpi())
            background = ColorDrawable(Color.TRANSPARENT)
            bitmapDecoderFactory = bitmapDecoder
            regionDecoderFactory = regionDecoder
            maxScale = 10f
            beVisible()
            isQuickScaleEnabled = config.oneFingerZoom
            isOneToOneZoomEnabled = config.allowOneToOneZoom
            orientation = rotation
            setImage(path)
            onImageEventListener = object : SubsamplingScaleImageView.OnImageEventListener {
                override fun onReady() {
                    background = ColorDrawable(if (config.blackBackground) Color.BLACK else config.backgroundColor)
                    val useWidth = if (mImageOrientation == ORIENTATION_ROTATE_90 || mImageOrientation == ORIENTATION_ROTATE_270) sHeight else sWidth
                    val useHeight = if (mImageOrientation == ORIENTATION_ROTATE_90 || mImageOrientation == ORIENTATION_ROTATE_270) sWidth else sHeight
                    doubleTapZoomScale = getDoubleTapZoomScale(useWidth, useHeight)
                    mOriginalSubsamplingScale = scale
                }

                override fun onImageLoadError(e: Exception) {
                    mView.gestures_view.controller.settings.isZoomEnabled = true
                    background = ColorDrawable(Color.TRANSPARENT)
                    mIsSubsamplingVisible = false
                    beGone()
                }
            }
        }
    }

    private fun getMinTileDpi(): Int {
        val metrics = resources.displayMetrics
        val averageDpi = (metrics.xdpi + metrics.ydpi) / 2
        val device = "${Build.BRAND} ${Build.MODEL}".toLowerCase()
        return when {
            WEIRD_DEVICES.contains(device) -> 240
            averageDpi > 400 -> 280
            averageDpi > 300 -> 220
            else -> 160
        }
    }

    private fun checkIfPanorama() {
        mIsPanorama = try {
            val inputStream = if (mMedium.path.startsWith("content:/")) context!!.contentResolver.openInputStream(Uri.parse(mMedium.path)) else File(mMedium.path).inputStream()
            val imageParser = JpegImageParser().getXmpXml(ByteSourceInputStream(inputStream, mMedium.name), HashMap<String, Any>())
            imageParser.contains("GPano:UsePanoramaViewer=\"True\"", true) || imageParser.contains("<GPano:UsePanoramaViewer>True</GPano:UsePanoramaViewer>", true)
        } catch (e: Exception) {
            false
        } catch (e: OutOfMemoryError) {
            false
        }

        mView.panorama_outline.beVisibleIf(mIsPanorama)
    }

    private fun getImageOrientation(): Int {
        val defaultOrientation = -1
        var orient = defaultOrientation

        try {
            val pathToLoad = getPathToLoad(mMedium)
            val exif = android.media.ExifInterface(pathToLoad)
            orient = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, defaultOrientation)

            if (orient == defaultOrientation || mMedium.path.startsWith(OTG_PATH)) {
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
        val screenAspectRatio = mScreenHeight / mScreenWidth.toFloat()

        return if (context == null || Math.abs(bitmapAspectRatio - screenAspectRatio) < SAME_ASPECT_RATIO_THRESHOLD) {
            DEFAULT_DOUBLE_TAP_ZOOM
        } else if (context!!.portrait && bitmapAspectRatio <= screenAspectRatio) {
            mScreenHeight / height.toFloat()
        } else if (context!!.portrait && bitmapAspectRatio > screenAspectRatio) {
            mScreenWidth / width.toFloat()
        } else if (!context!!.portrait && bitmapAspectRatio >= screenAspectRatio) {
            mScreenWidth / width.toFloat()
        } else if (!context!!.portrait && bitmapAspectRatio < screenAspectRatio) {
            mScreenHeight / height.toFloat()
        } else {
            DEFAULT_DOUBLE_TAP_ZOOM
        }
    }

    fun rotateImageViewBy(degrees: Int) {
        mCurrentRotationDegrees = degrees
        mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
        mView.subsampling_view.beGone()
        mIsSubsamplingVisible = false
        loadBitmap(degrees)
    }

    private fun initExtendedDetails() {
        if (context!!.config.showExtendedDetails) {
            mView.photo_details.apply {
                beInvisible()   // make it invisible so we can measure it, but not show yet
                text = getMediumExtendedDetails(mMedium)
                onGlobalLayout {
                    if (isAdded) {
                        val realY = getExtendedDetailsY(height)
                        if (realY > 0) {
                            y = realY
                            beVisibleIf(text.isNotEmpty())
                            alpha = if (!context!!.config.hideExtendedDetails || !mIsFullscreen) 1f else 0f
                        }
                    }
                }
            }
        } else {
            mView.photo_details.beGone()
        }
    }

    private fun hideZoomableView() {
        if (context?.config?.allowZoomingImages == true) {
            mIsSubsamplingVisible = false
            mView.subsampling_view.recycle()
            mView.subsampling_view.beGone()
            mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun photoClicked() {
        listener?.fragmentClicked()
    }

    private fun updateInstantSwitchWidths() {
        val newWidth = resources.getDimension(R.dimen.instant_change_bar_width) + if (activity?.portrait == false) activity!!.navigationBarWidth else 0
        mView.instant_prev_item.layoutParams.width = newWidth.toInt()
        mView.instant_next_item.layoutParams.width = newWidth.toInt()
    }

    override fun fullscreenToggled(isFullscreen: Boolean) {
        this.mIsFullscreen = isFullscreen
        mView.photo_details.apply {
            if (mStoredShowExtendedDetails && isVisible()) {
                animate().y(getExtendedDetailsY(height))

                if (mStoredHideExtendedDetails) {
                    animate().alpha(if (isFullscreen) 0f else 1f).start()
                }
            }
        }
    }

    private fun getExtendedDetailsY(height: Int): Float {
        val smallMargin = resources.getDimension(R.dimen.small_margin)
        val fullscreenOffset = smallMargin + if (mIsFullscreen) 0 else context!!.navigationBarHeight
        val actionsHeight = if (context!!.config.bottomActions && !mIsFullscreen) resources.getDimension(R.dimen.bottom_actions_height) else 0f
        return context!!.realScreenSize.y - height - actionsHeight - fullscreenOffset
    }
}
