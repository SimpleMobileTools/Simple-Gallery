package com.simplemobiletools.gallery.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.simplemobiletools.gallery.helpers.MEDIUM
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.getRealPathFromURI
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.pager_photo_item.view.*

class PhotoFragment : ViewPagerFragment(), View.OnClickListener {
    lateinit var subsamplingView: SubsamplingScaleImageView
    lateinit var medium: Medium

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.pager_photo_item, container, false)

        medium = arguments.getSerializable(MEDIUM) as Medium
        if (medium.path.startsWith("content://"))
            medium.path = context.getRealPathFromURI(Uri.parse(medium.path)) ?: ""

        subsamplingView = view.photo_view
        if (medium.isGif()) {
            subsamplingView.visibility = View.GONE
            view.gif_view.apply {
                visibility = View.VISIBLE
                Glide.with(context).load(medium.path).asGif().diskCacheStrategy(DiskCacheStrategy.NONE).into(this)
                this.setOnClickListener(this@PhotoFragment)
            }
        } else {
            subsamplingView.apply {
                setDoubleTapZoomScale(1.2f)
                orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
                setImage(ImageSource.uri(medium.path))
                maxScale = 4f
                setMinimumTileDpi(100)
                setOnClickListener(this@PhotoFragment)
            }
        }

        return view
    }

    override fun itemDragged() {
    }

    override fun systemUiVisibilityChanged(toFullscreen: Boolean) {
    }

    override fun updateItem() {
        subsamplingView.setImage(ImageSource.uri(medium.path))
    }

    override fun onClick(v: View) {
        photoClicked()
    }

    private fun photoClicked() {
        if (listener == null)
            listener = activity as ViewPagerFragment.FragmentClickListener

        listener?.fragmentClicked()
    }
}
