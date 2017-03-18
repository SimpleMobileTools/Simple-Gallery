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
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.beVisible
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.ViewPagerActivity
import com.simplemobiletools.gallery.extensions.getRealPathFromURI
import com.simplemobiletools.gallery.helpers.MEDIUM
import com.simplemobiletools.gallery.models.Medium
import com.squareup.picasso.Picasso
import it.sephiroth.android.library.exif2.ExifInterface
import kotlinx.android.synthetic.main.pager_photo_item.view.*
import uk.co.senab.photoview.PhotoViewAttacher
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PhotoFragment : ViewPagerFragment() {
    lateinit var medium: Medium
    lateinit var view: ViewGroup
    private var isFragmentVisible = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        view = inflater.inflate(R.layout.pager_photo_item, container, false) as ViewGroup

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

        view.gif_holder.setOnClickListener { photoClicked() }
        view.photo_view.setOnPhotoTapListener(object : PhotoViewAttacher.OnPhotoTapListener {
            override fun onPhotoTap(view: View?, x: Float, y: Float) {
                photoClicked()
            }

            override fun onOutsidePhotoTap() {
                photoClicked()
            }
        })
        loadImage()

        activity.window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            listener?.systemUiVisibilityChanged(visibility)
        }

        return view
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        isFragmentVisible = menuVisible
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

    private fun loadImage() {
        if (medium.isGif()) {
            view.photo_view.beGone()
            view.gif_holder.beVisible()
            Glide.with(this)
                    .load(medium.path)
                    .asGif()
                    .crossFade()
                    .priority(if (isFragmentVisible) Priority.IMMEDIATE else Priority.LOW)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(view.gif_holder)
        } else {
            loadBitmap()
        }
    }

    private fun loadBitmap(degrees: Float = 0f) {
        val densiy = ViewPagerActivity.screenDensity
        Picasso.with(activity)
                .load("file:${medium.path}")
                .resize((ViewPagerActivity.screenWidth * densiy).toInt(), (ViewPagerActivity.screenHeight * densiy).toInt())
                .priority(if (isFragmentVisible) Picasso.Priority.HIGH else Picasso.Priority.LOW)
                .rotate(degrees)
                .centerInside()
                .into(view.photo_view)
    }

    fun rotateImageViewBy(degrees: Float) {
        loadBitmap(degrees)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Glide.clear(view.gif_holder)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        loadImage()
    }

    private fun photoClicked() {
        listener?.fragmentClicked()
    }
}
