package com.simplemobiletools.gallery.fragments

import android.content.res.Configuration
import android.graphics.Bitmap
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
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.getRealPathFromURI
import com.simplemobiletools.gallery.helpers.MEDIUM
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.pager_photo_item.view.*

class PhotoFragment : ViewPagerFragment() {
    lateinit var medium: Medium
    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mView = inflater.inflate(R.layout.pager_photo_item, container, false)

        medium = arguments.getSerializable(MEDIUM) as Medium
        if (medium.path.startsWith("content://"))
            medium.path = context.getRealPathFromURI(Uri.parse(medium.path)) ?: ""

        loadImage(medium)

        activity.window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            listener?.systemUiVisibilityChanged(visibility)
        }

        return mView
    }

    private fun loadImage(medium: Medium) {
        val subsamplingView = mView.photo_view.apply { setOnClickListener({ photoClicked() }) }
        val glideView = mView.glide_view.apply { setOnClickListener({ photoClicked() }) }

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

                        override fun onResourceReady(resource: Bitmap?, model: String?, target: Target<Bitmap>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            if (!medium.isPng()) {
                                subsamplingView.visibility = View.VISIBLE
                                subsamplingView.apply {
                                    setDoubleTapZoomScale(1.2f)
                                    setImage(ImageSource.uri(medium.path))
                                    orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
                                    maxScale = 5f
                                    setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
                                        override fun onImageLoaded() {
                                        }

                                        override fun onReady() {
                                            glideView.visibility = View.GONE
                                            subsamplingView.visibility = View.VISIBLE
                                        }

                                        override fun onTileLoadError(p0: Exception?) {
                                        }

                                        override fun onPreviewReleased() {
                                        }

                                        override fun onImageLoadError(p0: Exception?) {
                                        }

                                        override fun onPreviewLoadError(p0: Exception?) {
                                        }
                                    })
                                }
                            }
                            return false
                        }
                    }).into(glideView)
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
