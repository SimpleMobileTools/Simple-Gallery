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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.exifinterface.media.ExifInterface.*
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.State
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.Rotate
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.davemorrissey.labs.subscaleview.DecoderFactory
import com.davemorrissey.labs.subscaleview.ImageDecoder
import com.davemorrissey.labs.subscaleview.ImageRegionDecoder
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.penfeizhou.animation.apng.APNGDrawable
import com.github.penfeizhou.animation.webp.WebPDrawable
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.helpers.isRPlus
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.PanoramaPhotoActivity
import com.simplemobiletools.gallery.pro.activities.PhotoActivity
import com.simplemobiletools.gallery.pro.activities.PhotoVideoActivity
import com.simplemobiletools.gallery.pro.activities.ViewPagerActivity
import com.simplemobiletools.gallery.pro.adapters.PortraitPhotosAdapter
import com.simplemobiletools.gallery.pro.databinding.PagerPhotoItemBinding
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.saveRotatedImageToFile
import com.simplemobiletools.gallery.pro.extensions.sendFakeClick
import com.simplemobiletools.gallery.pro.helpers.*
import com.simplemobiletools.gallery.pro.models.Medium
import com.simplemobiletools.gallery.pro.svg.SvgSoftwareLayerSetter
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.sephiroth.android.library.exif2.ExifInterface
import org.apache.sanselan.common.byteSources.ByteSourceInputStream
import org.apache.sanselan.formats.jpeg.JpegImageParser
import pl.droidsonroids.gif.InputSource
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

class PhotoFragment : ViewPagerFragment() {
    private val DEFAULT_DOUBLE_TAP_ZOOM = 2f
    private val ZOOMABLE_VIEW_LOAD_DELAY = 100L
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
    private var mShouldResetImage = false
    private var mCurrentPortraitPhotoPath = ""
    private var mOriginalPath = ""
    private var mImageOrientation = -1
    private var mLoadZoomableViewHandler = Handler()
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mCurrentGestureViewZoom = 1f

    private var mStoredShowExtendedDetails = false
    private var mStoredHideExtendedDetails = false
    private var mStoredAllowDeepZoomableImages = false
    private var mStoredShowHighestQuality = false
    private var mStoredExtendedDetails = 0

    private lateinit var mView: ViewGroup
    private lateinit var binding: PagerPhotoItemBinding
    private lateinit var mMedium: Medium

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val activity = requireActivity()
        val arguments = requireArguments()

        binding = PagerPhotoItemBinding.inflate(inflater, container, false)
        mView = binding.root
        if (!arguments.getBoolean(SHOULD_INIT_FRAGMENT, true)) {
            return mView
        }

        mMedium = arguments.getSerializable(MEDIUM) as Medium
        mOriginalPath = mMedium.path

        binding.apply {
            subsamplingView.setOnClickListener { photoClicked() }
            gesturesView.setOnClickListener { photoClicked() }
            gifView.setOnClickListener { photoClicked() }
            instantPrevItem.setOnClickListener { listener?.goToPrevItem() }
            instantNextItem.setOnClickListener { listener?.goToNextItem() }
            panoramaOutline.setOnClickListener { openPanorama() }

            instantPrevItem.parentView = container
            instantNextItem.parentView = container

            photoBrightnessController.initialize(activity, slideInfo, true, container, singleTap = { x, y ->
                mView.apply {
                    if (subsamplingView.isVisible()) {
                        subsamplingView.sendFakeClick(x, y)
                    } else {
                        gesturesView.sendFakeClick(x, y)
                    }
                }
            })

            if (context.config.allowDownGesture) {
                gifView.setOnTouchListener { v, event ->
                    if (gifViewFrame.controller.state.zoom == 1f) {
                        handleEvent(event)
                    }
                    false
                }

                gesturesView.controller.addOnStateChangeListener(object : GestureController.OnStateChangeListener {
                    override fun onStateChanged(state: State) {
                        mCurrentGestureViewZoom = state.zoom
                    }
                })

                gesturesView.setOnTouchListener { v, event ->
                    if (mCurrentGestureViewZoom == 1f) {
                        handleEvent(event)
                    }
                    false
                }

                subsamplingView.setOnTouchListener { v, event ->
                    if (subsamplingView.isZoomedOut()) {
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

        if (mMedium.path.startsWith("content://") && !mMedium.path.startsWith("content://mms/")) {
            mMedium.path = requireContext().getRealPathFromURI(Uri.parse(mOriginalPath)) ?: mMedium.path
            if (isRPlus() && !isExternalStorageManager() && mMedium.path.startsWith("/storage/") && mMedium.isHidden()) {
                mMedium.path = mOriginalPath
            }

            if (mMedium.path.isEmpty()) {
                var out: FileOutputStream? = null
                try {
                    var inputStream = requireContext().contentResolver.openInputStream(Uri.parse(mOriginalPath))
                    val exif = ExifInterface()
                    exif.readExif(inputStream, ExifInterface.Options.OPTION_ALL)
                    val tag = exif.getTag(ExifInterface.TAG_ORIENTATION)
                    val orientation = tag?.getValueAsInt(-1) ?: -1
                    inputStream = requireContext().contentResolver.openInputStream(Uri.parse(mOriginalPath))
                    val original = BitmapFactory.decodeStream(inputStream)
                    val rotated = rotateViaMatrix(original, orientation)
                    exif.setTagValue(ExifInterface.TAG_ORIENTATION, 1)
                    exif.removeCompressedThumbnail()

                    val file = File(requireContext().externalCacheDir, Uri.parse(mOriginalPath).lastPathSegment)
                    out = FileOutputStream(file)
                    rotated.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    mMedium.path = file.absolutePath
                } catch (e: Exception) {
                    requireActivity().toast(com.simplemobiletools.commons.R.string.unknown_error_occurred)
                    return mView
                } finally {
                    out?.close()
                }
            }
        }

        mIsFullscreen = requireActivity().window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
        loadImage()
        initExtendedDetails()
        mWasInit = true
        updateInstantSwitchWidths()

        ensureBackgroundThread {
            checkIfPanorama()
        }

        return mView
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onResume() {
        super.onResume()
        val config = requireContext().config
        if (mWasInit && (config.showExtendedDetails != mStoredShowExtendedDetails || config.extendedDetails != mStoredExtendedDetails)) {
            initExtendedDetails()
        }

        if (mWasInit) {
            if (config.allowZoomingImages != mStoredAllowDeepZoomableImages || config.showHighestQuality != mStoredShowHighestQuality) {
                mIsSubsamplingVisible = false
                binding.subsamplingView.beGone()
                loadImage()
            } else if (mMedium.isGIF()) {
                loadGif()
            } else if (mIsSubsamplingVisible && mShouldResetImage) {
                binding.subsamplingView.onGlobalLayout {
                    binding.subsamplingView.resetView()
                }
            }
            mShouldResetImage = false
        }

        val allowPhotoGestures = config.allowPhotoGestures
        val allowInstantChange = config.allowInstantChange

        binding.apply {
            photoBrightnessController.beVisibleIf(allowPhotoGestures)
            instantPrevItem.beVisibleIf(allowInstantChange)
            instantNextItem.beVisibleIf(allowInstantChange)
        }

        storeStateVariables()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (activity?.isDestroyed == false) {
            binding.subsamplingView.recycle()

            try {
                if (context != null) {
                    Glide.with(requireContext()).clear(binding.gesturesView)
                }
            } catch (ignored: Exception) {
            }
        }

        mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
        if (mCurrentRotationDegrees != 0) {
            ensureBackgroundThread {
                val path = mMedium.path
                (activity as? BaseSimpleActivity)?.saveRotatedImageToFile(path, path, mCurrentRotationDegrees, false) {}
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!mWasInit) {
            return
        }

        // avoid GIFs being skewed, played in wrong aspect ratio
        if (mMedium.isGIF()) {
            mView.onGlobalLayout {
                if (activity != null) {
                    measureScreen()
                    Handler().postDelayed({
                        binding.gifViewFrame.controller.resetState()
                        loadGif()
                    }, 50)
                }
            }
        } else {
            hideZoomableView()
            loadImage()
        }

        measureScreen()
        initExtendedDetails()
        updateInstantSwitchWidths()
        mShouldResetImage = true
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        mIsFragmentVisible = menuVisible
        if (mWasInit) {
            if (!mMedium.isGIF() && !mMedium.isWebP() && !mMedium.isApng()) {
                photoFragmentVisibilityChanged(menuVisible)
            }
        }
    }

    private fun storeStateVariables() {
        requireContext().config.apply {
            mStoredShowExtendedDetails = showExtendedDetails
            mStoredHideExtendedDetails = hideExtendedDetails
            mStoredAllowDeepZoomableImages = allowZoomingImages
            mStoredShowHighestQuality = showHighestQuality
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
        activity?.windowManager?.defaultDisplay?.getRealMetrics(metrics)
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
        ORIENTATION_ROTATE_270, ORIENTATION_TRANSPOSE -> 270
        ORIENTATION_ROTATE_180 -> 180
        ORIENTATION_ROTATE_90, ORIENTATION_TRANSVERSE -> 90
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

        if (mMedium.isPortrait() && context != null) {
            showPortraitStripe()
        }

        ensureBackgroundThread {
            mImageOrientation = getImageOrientation()
            activity?.runOnUiThread {
                when {
                    mMedium.isGIF() -> loadGif()
                    mMedium.isSVG() -> loadSVG()
                    mMedium.isApng() -> loadAPNG()
                    else -> loadBitmap()
                }
            }
        }
    }

    private fun loadGif() {
        try {
            val pathToLoad = getPathToLoad(mMedium)
            val source = if (pathToLoad.startsWith("content://") || pathToLoad.startsWith("file://")) {
                InputSource.UriSource(requireContext().contentResolver, Uri.parse(pathToLoad))
            } else {
                InputSource.FileSource(pathToLoad)
            }

            binding.apply {
                gesturesView.beGone()
                gifViewFrame.beVisible()
                ensureBackgroundThread {
                    gifView.setInputSource(source)
                }
            }
        } catch (e: Exception) {
            loadBitmap()
        } catch (e: OutOfMemoryError) {
            loadBitmap()
        }
    }

    private fun loadSVG() {
        if (context != null) {
            Glide.with(requireContext())
                .`as`(PictureDrawable::class.java)
                .listener(SvgSoftwareLayerSetter())
                .load(mMedium.path)
                .into(binding.gesturesView)
        }
    }

    private fun loadAPNG() {
        if (context != null) {
            val drawable = APNGDrawable.fromFile(mMedium.path)
            binding.gesturesView.setImageDrawable(drawable)
        }
    }

    private fun loadBitmap(addZoomableView: Boolean = true) {
        if (context == null) {
            return
        }

        val path = getFilePathToShow()
        if (path.isWebP()) {
            val drawable = WebPDrawable.fromFile(path)
            if (drawable.intrinsicWidth == 0) {
                loadWithGlide(path, addZoomableView)
            } else {
                binding.gesturesView.setImageDrawable(drawable)
            }
        } else {
            loadWithGlide(path, addZoomableView)
        }
    }

    private fun loadWithGlide(path: String, addZoomableView: Boolean) {
        val priority = if (mIsFragmentVisible) Priority.IMMEDIATE else Priority.NORMAL
        val options = RequestOptions()
            .signature(mMedium.getKey())
            .format(DecodeFormat.PREFER_ARGB_8888)
            .priority(priority)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .fitCenter()

        if (mCurrentRotationDegrees != 0) {
            options.transform(Rotate(mCurrentRotationDegrees))
            options.diskCacheStrategy(DiskCacheStrategy.NONE)
        }

        Glide.with(requireContext())
            .load(path)
            .apply(options)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    if (activity != null && !activity!!.isDestroyed && !activity!!.isFinishing) {
                        tryLoadingWithPicasso(addZoomableView)
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    val allowZoomingImages = context?.config?.allowZoomingImages ?: true
                    binding.gesturesView.controller.settings.isZoomEnabled = mMedium.isRaw() || mCurrentRotationDegrees != 0 || allowZoomingImages == false
                    if (mIsFragmentVisible && addZoomableView) {
                        scheduleZoomableView()
                    }
                    return false
                }
            }).into(binding.gesturesView)
    }

    private fun tryLoadingWithPicasso(addZoomableView: Boolean) {
        var pathToLoad = if (getFilePathToShow().startsWith("content://")) getFilePathToShow() else "file://${getFilePathToShow()}"
        pathToLoad = pathToLoad.replace("%", "%25").replace("#", "%23")

        try {
            val picasso = Picasso.get()
                .load(pathToLoad)
                .centerInside()
                .stableKey(mMedium.getSignature())
                .resize(mScreenWidth, mScreenHeight)

            if (mCurrentRotationDegrees != 0) {
                picasso.rotate(mCurrentRotationDegrees.toFloat())
            } else {
                degreesForRotation(mImageOrientation).toFloat()
            }

            picasso.into(binding.gesturesView, object : Callback {
                override fun onSuccess() {
                    binding.gesturesView.controller.settings.isZoomEnabled =
                        mMedium.isRaw() || mCurrentRotationDegrees != 0 || context?.config?.allowZoomingImages == false
                    if (mIsFragmentVisible && addZoomableView) {
                        scheduleZoomableView()
                    }
                }

                override fun onError(e: Exception?) {
                    if (mMedium.path != mOriginalPath) {
                        mMedium.path = mOriginalPath
                        loadImage()
                        checkIfPanorama()
                    }
                }
            })
        } catch (ignored: Exception) {
        }
    }

    private fun showPortraitStripe() {
        val files = File(mMedium.parentPath).listFiles()?.toMutableList() as? ArrayList<File>
        if (files != null) {
            val screenWidth = requireContext().realScreenSize.x
            val itemWidth =
                resources.getDimension(R.dimen.portrait_photos_stripe_height).toInt() + resources.getDimension(com.simplemobiletools.commons.R.dimen.one_dp)
                    .toInt()
            val sideWidth = screenWidth / 2 - itemWidth / 2
            val fakeItemsCnt = ceil(sideWidth / itemWidth.toDouble()).toInt()

            val paths = fillPhotoPaths(files, fakeItemsCnt)
            var curWidth = itemWidth
            while (curWidth < screenWidth) {
                curWidth += itemWidth
            }

            val sideElementWidth = curWidth - screenWidth
            val adapter = PortraitPhotosAdapter(requireContext(), paths, sideElementWidth) { position, x ->
                if (mIsFullscreen) {
                    return@PortraitPhotosAdapter
                }

                binding.photoPortraitStripe.smoothScrollBy((x + itemWidth / 2) - screenWidth / 2, 0)
                if (paths[position] != mCurrentPortraitPhotoPath) {
                    mCurrentPortraitPhotoPath = paths[position]
                    hideZoomableView()
                    loadBitmap()
                }
            }

            binding.photoPortraitStripe.adapter = adapter
            setupStripeBottomMargin()

            val coverIndex = getCoverImageIndex(paths)
            if (coverIndex != -1) {
                mCurrentPortraitPhotoPath = paths[coverIndex]
                setupStripeUpListener(adapter, screenWidth, itemWidth)

                binding.photoPortraitStripe.onGlobalLayout {
                    binding.photoPortraitStripe.scrollBy((coverIndex - fakeItemsCnt) * itemWidth, 0)
                    adapter.setCurrentPhoto(coverIndex)
                    binding.photoPortraitStripeWrapper.beVisible()
                    if (mIsFullscreen) {
                        binding.photoPortraitStripeWrapper.alpha = 0f
                    }
                }
            }
        }
    }

    private fun fillPhotoPaths(files: ArrayList<File>, fakeItemsCnt: Int): ArrayList<String> {
        val paths = ArrayList<String>()
        for (i in 0 until fakeItemsCnt) {
            paths.add("")
        }

        files.forEach {
            paths.add(it.absolutePath)
        }

        for (i in 0 until fakeItemsCnt) {
            paths.add("")
        }
        return paths
    }

    private fun setupStripeBottomMargin() {
        var bottomMargin = requireContext().navigationBarHeight + resources.getDimension(com.simplemobiletools.commons.R.dimen.normal_margin).toInt()
        if (requireContext().config.bottomActions) {
            bottomMargin += resources.getDimension(R.dimen.bottom_actions_height).toInt()
        }
        (binding.photoPortraitStripeWrapper.layoutParams as RelativeLayout.LayoutParams).bottomMargin = bottomMargin
    }

    private fun getCoverImageIndex(paths: ArrayList<String>): Int {
        var coverIndex = -1
        paths.forEachIndexed { index, path ->
            if (path.contains("cover", true)) {
                coverIndex = index
            }
        }

        if (coverIndex == -1) {
            paths.forEachIndexed { index, path ->
                if (path.isNotEmpty()) {
                    coverIndex = index
                }
            }
        }
        return coverIndex
    }

    private fun setupStripeUpListener(adapter: PortraitPhotosAdapter, screenWidth: Int, itemWidth: Int) {
        binding.photoPortraitStripe.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                var closestIndex = -1
                var closestDistance = Integer.MAX_VALUE
                val center = screenWidth / 2
                for ((key, value) in adapter.views) {
                    val distance = Math.abs(value.x.toInt() + itemWidth / 2 - center)
                    if (distance < closestDistance) {
                        closestDistance = distance
                        closestIndex = key
                    }
                }

                Handler().postDelayed({
                    adapter.performClickOn(closestIndex)
                }, 100)
            }
            false
        }
    }

    private fun getFilePathToShow() = if (mMedium.isPortrait()) mCurrentPortraitPhotoPath else getPathToLoad(mMedium)

    private fun openPanorama() {
        Intent(context, PanoramaPhotoActivity::class.java).apply {
            putExtra(PATH, mMedium.path)
            startActivity(this)
        }
    }

    private fun scheduleZoomableView() {
        mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
        mLoadZoomableViewHandler.postDelayed({
            if (mIsFragmentVisible && context?.config?.allowZoomingImages == true && (mMedium.isImage() || mMedium.isPortrait()) && !mIsSubsamplingVisible) {
                addZoomableView()
            }
        }, ZOOMABLE_VIEW_LOAD_DELAY)
    }

    private fun addZoomableView() {
        val rotation = degreesForRotation(mImageOrientation)
        mIsSubsamplingVisible = true
        val config = requireContext().config
        val showHighestQuality = config.showHighestQuality
        val minTileDpi = if (showHighestQuality) -1 else getMinTileDpi()

        val bitmapDecoder = object : DecoderFactory<ImageDecoder> {
            override fun make() = MyGlideImageDecoder(rotation, mMedium.getKey())
        }

        val regionDecoder = object : DecoderFactory<ImageRegionDecoder> {
            override fun make() = PicassoRegionDecoder(showHighestQuality, mScreenWidth, mScreenHeight, minTileDpi)
        }

        var newOrientation = (rotation + mCurrentRotationDegrees) % 360
        if (newOrientation < 0) {
            newOrientation += 360
        }

        binding.subsamplingView.apply {
            setMaxTileSize(if (showHighestQuality) Integer.MAX_VALUE else 4096)
            setMinimumTileDpi(minTileDpi)
            background = ColorDrawable(Color.TRANSPARENT)
            bitmapDecoderFactory = bitmapDecoder
            regionDecoderFactory = regionDecoder
            maxScale = 10f
            beVisible()
            rotationEnabled = config.allowRotatingWithGestures
            isOneToOneZoomEnabled = config.allowOneToOneZoom
            orientation = newOrientation
            setImage(getFilePathToShow())

            onImageEventListener = object : SubsamplingScaleImageView.OnImageEventListener {
                override fun onReady() {
                    background = ColorDrawable(
                        if (config.blackBackground) {
                            Color.BLACK
                        } else {
                            context.getProperBackgroundColor()
                        }
                    )

                    val useWidth = if (mImageOrientation == ORIENTATION_ROTATE_90 || mImageOrientation == ORIENTATION_ROTATE_270) sHeight else sWidth
                    val useHeight = if (mImageOrientation == ORIENTATION_ROTATE_90 || mImageOrientation == ORIENTATION_ROTATE_270) sWidth else sHeight
                    doubleTapZoomScale = getDoubleTapZoomScale(useWidth, useHeight)
                }

                override fun onImageLoadError(e: Exception) {
                    binding.gesturesView.controller.settings.isZoomEnabled = true
                    background = ColorDrawable(Color.TRANSPARENT)
                    mIsSubsamplingVisible = false
                    beGone()
                }

                override fun onImageRotation(degrees: Int) {
                    val fullRotation = (rotation + degrees) % 360
                    val useWidth = if (fullRotation == 90 || fullRotation == 270) sHeight else sWidth
                    val useHeight = if (fullRotation == 90 || fullRotation == 270) sWidth else sHeight
                    doubleTapZoomScale = getDoubleTapZoomScale(useWidth, useHeight)
                    mCurrentRotationDegrees = (mCurrentRotationDegrees + degrees) % 360
                    loadBitmap(false)

                    // ugly, but it works
                    (activity as? ViewPagerActivity)?.refreshMenuItems()
                    (activity as? PhotoVideoActivity)?.refreshMenuItems()
                }

                override fun onUpEvent() {
                    mShouldResetImage = false
                }
            }
        }
    }

    private fun getMinTileDpi(): Int {
        val metrics = resources.displayMetrics
        val averageDpi = (metrics.xdpi + metrics.ydpi) / 2
        val device = "${Build.BRAND} ${Build.MODEL}".toLowerCase()
        return when {
            WEIRD_DEVICES.contains(device) -> WEIRD_TILE_DPI
            averageDpi > 400 -> HIGH_TILE_DPI
            averageDpi > 300 -> NORMAL_TILE_DPI
            else -> LOW_TILE_DPI
        }
    }

    private fun checkIfPanorama() {
        mIsPanorama = try  {
            if (mMedium.path.startsWith("content:/")) {
                requireContext().contentResolver.openInputStream(Uri.parse(mMedium.path))
            } else {
                File(mMedium.path).inputStream()
            }.use {
                val imageParser = JpegImageParser().getXmpXml(ByteSourceInputStream(it, mMedium.name), HashMap<String, Any>())
                imageParser.contains("GPano:UsePanoramaViewer=\"True\"", true) ||
                    imageParser.contains("<GPano:UsePanoramaViewer>True</GPano:UsePanoramaViewer>", true) ||
                    imageParser.contains("GPano:FullPanoWidthPixels=") ||
                    imageParser.contains("GPano:ProjectionType>Equirectangular")
            }
        } catch (e: Exception) {
            false
        } catch (e: OutOfMemoryError) {
            false
        }

        activity?.runOnUiThread {
            binding.panoramaOutline.beVisibleIf(mIsPanorama)
            if (mIsFullscreen) {
                binding.panoramaOutline.alpha = 0f
            }
        }
    }

    private fun getImageOrientation(): Int {
        val defaultOrientation = -1
        var orient = defaultOrientation

        try {
            val path = getFilePathToShow()
            orient = if (path.startsWith("content:/")) {
                val inputStream = requireContext().contentResolver.openInputStream(Uri.parse(path))
                val exif = ExifInterface()
                exif.readExif(inputStream, ExifInterface.Options.OPTION_ALL)
                val tag = exif.getTag(ExifInterface.TAG_ORIENTATION)
                tag?.getValueAsInt(defaultOrientation) ?: defaultOrientation
            } else {
                val exif = androidx.exifinterface.media.ExifInterface(path)
                exif.getAttributeInt(TAG_ORIENTATION, defaultOrientation)
            }

            if (orient == defaultOrientation || requireContext().isPathOnOTG(getFilePathToShow())) {
                val uri = if (path.startsWith("content:/")) Uri.parse(path) else Uri.fromFile(File(path))
                val inputStream = requireContext().contentResolver.openInputStream(uri)
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
        } else if (requireContext().portrait && bitmapAspectRatio <= screenAspectRatio) {
            mScreenHeight / height.toFloat()
        } else if (requireContext().portrait && bitmapAspectRatio > screenAspectRatio) {
            mScreenWidth / width.toFloat()
        } else if (!requireContext().portrait && bitmapAspectRatio >= screenAspectRatio) {
            mScreenWidth / width.toFloat()
        } else if (!requireContext().portrait && bitmapAspectRatio < screenAspectRatio) {
            mScreenHeight / height.toFloat()
        } else {
            DEFAULT_DOUBLE_TAP_ZOOM
        }
    }

    fun rotateImageViewBy(degrees: Int) {
        if (mIsSubsamplingVisible) {
            binding.subsamplingView.rotateBy(degrees)
        } else {
            mCurrentRotationDegrees = (mCurrentRotationDegrees + degrees) % 360
            mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
            mIsSubsamplingVisible = false
            loadBitmap()
        }
    }

    private fun initExtendedDetails() {
        if (requireContext().config.showExtendedDetails) {
            binding.photoDetails.apply {
                beInvisible()   // make it invisible so we can measure it, but not show yet
                text = getMediumExtendedDetails(mMedium)
                onGlobalLayout {
                    if (isAdded) {
                        val realY = getExtendedDetailsY(height)
                        if (realY > 0) {
                            y = realY
                            beVisibleIf(text.isNotEmpty())
                            alpha = if (!requireContext().config.hideExtendedDetails || !mIsFullscreen) 1f else 0f
                        }
                    }
                }
            }
        } else {
            binding.photoDetails.beGone()
        }
    }

    private fun hideZoomableView() {
        if (context?.config?.allowZoomingImages == true) {
            mIsSubsamplingVisible = false
            binding.subsamplingView.recycle()
            binding.subsamplingView.beGone()
            mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun photoClicked() {
        listener?.fragmentClicked()
    }

    private fun updateInstantSwitchWidths() {
        binding.instantPrevItem.layoutParams.width = mScreenWidth / 7
        binding.instantNextItem.layoutParams.width = mScreenWidth / 7
    }

    override fun fullscreenToggled(isFullscreen: Boolean) {
        this.mIsFullscreen = isFullscreen
        binding.apply {
            photoDetails.apply {
                if (mStoredShowExtendedDetails && isVisible() && context != null && resources != null) {
                    animate().y(getExtendedDetailsY(height))

                    if (mStoredHideExtendedDetails) {
                        animate().alpha(if (isFullscreen) 0f else 1f).start()
                    }
                }
            }

            if (mIsPanorama) {
                panoramaOutline.animate().alpha(if (isFullscreen) 0f else 1f).start()
                panoramaOutline.isClickable = !isFullscreen
            }

            if (mWasInit && mMedium.isPortrait()) {
                photoPortraitStripeWrapper.animate().alpha(if (isFullscreen) 0f else 1f).start()
            }
        }
    }

    private fun getExtendedDetailsY(height: Int): Float {
        val smallMargin = context?.resources?.getDimension(com.simplemobiletools.commons.R.dimen.small_margin) ?: return 0f
        val fullscreenOffset = smallMargin + if (mIsFullscreen) 0 else requireContext().navigationBarHeight
        val actionsHeight = if (requireContext().config.bottomActions && !mIsFullscreen) resources.getDimension(R.dimen.bottom_actions_height) else 0f
        return requireContext().realScreenSize.y - height - actionsHeight - fullscreenOffset
    }
}
