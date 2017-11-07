package com.simplemobiletools.gallery.fragments

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.PhotoActivity
import com.simplemobiletools.gallery.activities.ViewPagerActivity
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.GlideRotateTransformation
import com.simplemobiletools.gallery.helpers.MEDIUM
import com.simplemobiletools.gallery.models.Medium
import it.sephiroth.android.library.exif2.ExifInterface
import kotlinx.android.synthetic.main.pager_photo_item.view.*
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.io.FileOutputStream

class PhotoFragment : ViewPagerFragment() {
    private var isFragmentVisible = false
    private var wasInit = false
    private var storedShowExtendedDetails = false
    private var storedExtendedDetails = 0
    private var gifDrawable: GifDrawable? = null

    lateinit var view: ViewGroup
    lateinit var medium: Medium

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        view = inflater.inflate(R.layout.pager_photo_item, container, false) as ViewGroup

        if (!isFragmentVisible && activity is PhotoActivity) {
            isFragmentVisible = true
        }

        medium = arguments!!.getSerializable(MEDIUM) as Medium
        if (medium.path.startsWith("content://")) {
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

        view.subsampling_view.setOnClickListener { photoClicked() }
        view.gif_view.setOnClickListener { photoClicked() }
        loadImage()
        checkExtendedDetails()

        wasInit = true

        return view
    }

    override fun onPause() {
        super.onPause()
        storedShowExtendedDetails = context!!.config.showExtendedDetails
        storedExtendedDetails = context!!.config.extendedDetails
    }

    override fun onResume() {
        super.onResume()
        if (wasInit && (context!!.config.showExtendedDetails != storedShowExtendedDetails || context!!.config.extendedDetails != storedExtendedDetails)) {
            checkExtendedDetails()
        }
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

    private fun gifFragmentVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            gifDrawable?.start()
        } else {
            gifDrawable?.stop()
        }
    }

    private fun photoFragmentVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            addZoomableView()
        } else {
            view.subsampling_view.apply {
                recycle()
                beGone()
                background = ColorDrawable(Color.TRANSPARENT)
            }
        }
    }

    private fun degreesForRotation(orientation: Int) = when (orientation) {
        8 -> 270
        3 -> 180
        6 -> 90
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
        if (medium.isGif()) {
            loadGif()
        } else {
            loadBitmap()
        }
    }

    private fun loadGif() {
        try {
            gifDrawable = if (medium.path.startsWith("content://") || medium.path.startsWith("file://")) {
                GifDrawable(context!!.contentResolver, Uri.parse(medium.path))
            } else {
                GifDrawable(medium.path)
            }

            if (!isFragmentVisible) {
                gifDrawable!!.stop()
            }

            view.gif_view.setImageDrawable(gifDrawable)
        } catch (e: Exception) {
            gifDrawable = null
            loadBitmap()
        }
    }

    private fun loadBitmap(degrees: Float = 0f) {
        if (degrees == 0f) {
            val targetWidth = if (ViewPagerActivity.screenWidth == 0) Target.SIZE_ORIGINAL else ViewPagerActivity.screenWidth
            val targetHeight = if (ViewPagerActivity.screenHeight == 0) Target.SIZE_ORIGINAL else ViewPagerActivity.screenHeight

            val options = RequestOptions()
                    .signature(medium.path.getFileSignature())
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(targetWidth, targetHeight)

            Glide.with(this)
                    .asBitmap()
                    .load(medium.path)
                    .apply(options)
                    .listener(object : RequestListener<Bitmap> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean) = false

                        override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            if (isFragmentVisible)
                                addZoomableView()
                            return false
                        }
                    }).into(view.gif_view)
        } else {
            val options = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .transform(GlideRotateTransformation(context!!, degrees))

            Glide.with(this)
                    .asBitmap()
                    .load(medium.path)
                    .thumbnail(0.2f)
                    .apply(options)
                    .into(view.gif_view)
        }
    }

    private fun addZoomableView() {
        if ((medium.isImage()) && isFragmentVisible && view.subsampling_view.visibility == View.GONE) {
            view.subsampling_view.apply {
                //setBitmapDecoderClass(GlideDecoder::class.java)   // causing random crashes on Android 7+
                maxScale = 10f
                beVisible()
                setImage(ImageSource.uri(medium.path))
                orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
                setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
                    override fun onImageLoaded() {
                    }

                    override fun onReady() {
                        background = ColorDrawable(if (context.config.darkBackground) Color.BLACK else context.config.backgroundColor)
                        setDoubleTapZoomScale(getDoubleTapZoomScale())
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
    }

    private fun getDoubleTapZoomScale(): Float {
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(medium.path, bitmapOptions)
        val width = bitmapOptions.outWidth
        val height = bitmapOptions.outHeight
        val bitmapAspectRatio = height / (width).toFloat()

        if (context == null)
            return 2f

        return if (context!!.portrait && bitmapAspectRatio <= 1f) {
            ViewPagerActivity.screenHeight / height.toFloat()
        } else if (!context!!.portrait && bitmapAspectRatio >= 1f) {
            ViewPagerActivity.screenWidth / width.toFloat()
        } else {
            2f
        }
    }

    fun rotateImageViewBy(degrees: Float) {
        view.subsampling_view.beGone()
        loadBitmap(degrees)
    }

    private fun checkExtendedDetails() {
        if (context!!.config.showExtendedDetails) {
            view.photo_details.apply {
                text = getMediumExtendedDetails(medium)
                setTextColor(context.config.textColor)
                beVisibleIf(text.isNotEmpty())
                onGlobalLayout {
                    if (height != 0) {
                        val smallMargin = resources.getDimension(R.dimen.small_margin)
                        y = context.usableScreenSize.y - height - if (context.navigationBarHeight == 0) smallMargin else 0f
                    }
                }
            }
        } else {
            view.photo_details.beGone()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && !activity!!.isDestroyed) {
            Glide.with(context).clear(view.gif_view)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        loadImage()
        checkExtendedDetails()
    }

    private fun photoClicked() {
        listener?.fragmentClicked()
    }

    override fun fullscreenToggled(isFullscreen: Boolean) {
        view.photo_details.apply {
            if (visibility == View.VISIBLE) {
                val smallMargin = resources.getDimension(R.dimen.small_margin)
                val fullscreenOffset = context.navigationBarHeight.toFloat() - smallMargin
                val newY = context.usableScreenSize.y - height + if (isFullscreen) fullscreenOffset else -(if (context.navigationBarHeight == 0) smallMargin else 0f)
                animate().y(newY)
            }
        }
    }
}
