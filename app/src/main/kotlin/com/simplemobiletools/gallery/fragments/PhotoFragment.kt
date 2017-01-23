package com.simplemobiletools.gallery.fragments

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.getRealPathFromURI
import com.simplemobiletools.gallery.helpers.MEDIUM
import com.simplemobiletools.gallery.models.Medium
import it.sephiroth.android.library.exif2.ExifInterface
import kotlinx.android.synthetic.main.pager_photo_item.view.*
import uk.co.senab.photoview.PhotoView
import uk.co.senab.photoview.PhotoViewAttacher
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PhotoFragment : ViewPagerFragment() {
    lateinit var medium: Medium
    lateinit var subsamplingView: SubsamplingScaleImageView
    lateinit var glideView: PhotoView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.pager_photo_item, container, false)

        medium = arguments.getSerializable(MEDIUM) as Medium

        if (medium.path.startsWith("content://")) {
            val originalPath = medium.path
            medium.path = context.getRealPathFromURI(Uri.parse(medium.path)) ?: ""

            if (medium.path.isEmpty()) {
                var out: FileOutputStream? = null
                try {
                    var inputStream = context.contentResolver.openInputStream(Uri.parse(originalPath))
                    val exif = ExifInterface()
                    exif.readExif(inputStream, ExifInterface.Options.OPTION_ALL)
                    val tag = exif.getTag(ExifInterface.TAG_ORIENTATION)
                    val orientation = tag?.getValueAsInt(-1) ?: -1

                    inputStream = context.contentResolver.openInputStream(Uri.parse(originalPath))
                    val original = BitmapFactory.decodeStream(inputStream)
                    val rotated = rotateViaMatrix(original, orientation)
                    exif.setTagValue(ExifInterface.TAG_ORIENTATION, 1)
                    exif.removeCompressedThumbnail()

                    val file = File(context.externalCacheDir, Uri.parse(originalPath).lastPathSegment)
                    out = FileOutputStream(file)
                    rotated.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    medium.path = file.absolutePath
                } catch (e: Exception) {
                    context.toast(R.string.unknown_error_occurred)
                    return view
                } finally {
                    try {
                        out?.close()
                    } catch (e: IOException) {
                    }
                }
            }
        }

        subsamplingView = view.photo_view.apply { setOnClickListener({ photoClicked() }) }
        glideView = view.glide_view.apply {
            setOnPhotoTapListener(object : PhotoViewAttacher.OnPhotoTapListener {
                override fun onPhotoTap(view: View?, x: Float, y: Float) {
                    photoClicked()
                }

                override fun onOutsidePhotoTap() {
                    photoClicked()
                }
            })
        }
        loadImage(medium)

        activity.window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            listener?.systemUiVisibilityChanged(visibility)
        }

        return view
    }

    private fun degreesForRotation(orientation: Int): Int {
        return when (orientation) {
            8 -> 270
            3 -> 180
            6 -> 90
            else -> 0
        }
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

    private fun loadImage(medium: Medium) {
        if (medium.isGif()) {
            Glide.with(this)
                    .load(medium.path)
                    .asGif()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(glideView)
        } else {
            Glide.with(this)
                    .load(medium.path)
                    .asBitmap()
                    .format(if (medium.isPng()) DecodeFormat.PREFER_ARGB_8888 else DecodeFormat.PREFER_RGB_565)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .listener(object : RequestListener<String, Bitmap> {
                        override fun onException(e: Exception?, model: String?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                            return false
                        }

                        override fun onResourceReady(bitmap: Bitmap?, model: String?, target: Target<Bitmap>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            addZoomableView()
                            return false
                        }
                    }).into(glideView)
        }
    }

    private fun addZoomableView() {
        if (!medium.isPng()) {
            subsamplingView.apply {
                visibility = View.VISIBLE
                setDoubleTapZoomScale(1.2f)
                setImage(ImageSource.uri(medium.path))
                orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
                maxScale = 5f
                setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
                    override fun onImageLoaded() {
                    }

                    override fun onReady() {
                        glideView.visibility = View.GONE
                        glideView.setImageBitmap(null)
                    }

                    override fun onTileLoadError(p0: Exception?) {
                    }

                    override fun onPreviewReleased() {
                    }

                    override fun onImageLoadError(p0: Exception?) {
                        visibility = View.GONE
                    }

                    override fun onPreviewLoadError(p0: Exception?) {
                    }
                })
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        loadImage(medium)
    }

    private fun photoClicked() {
        listener?.fragmentClicked()
    }
}
