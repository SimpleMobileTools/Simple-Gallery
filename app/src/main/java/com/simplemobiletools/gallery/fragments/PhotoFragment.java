package com.simplemobiletools.gallery.fragments;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.ViewPropertyAnimation;
import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.models.Medium;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoFragment extends ViewPagerFragment implements PhotoViewAttacher.OnPhotoTapListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pager_photo_item, container, false);

        final Medium medium = (Medium) getArguments().getSerializable(Constants.MEDIUM);
        if (medium == null)
            return view;

        final PhotoView photoView = (PhotoView) view.findViewById(R.id.photo_view);
        if (medium.isGif()) {
            Glide.with(getContext()).load(medium.getPath()).asGif().diskCacheStrategy(DiskCacheStrategy.NONE).into(photoView);
        } else {
            Glide.with(getContext()).load(medium.getPath()).asBitmap().thumbnail(0.2f).animate(fadeInAnimator).into(photoView);
        }
        new PhotoViewAttacher(photoView).setOnPhotoTapListener(this);

        return view;
    }

    ViewPropertyAnimation.Animator fadeInAnimator = new ViewPropertyAnimation.Animator() {
        @Override
        public void animate(View view) {
            view.setAlpha(0f);
            ObjectAnimator fadeAnim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
            fadeAnim.setDuration(250);
            fadeAnim.start();
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void itemDragged() {

    }

    @Override
    public void systemUiVisibilityChanged(boolean toFullscreen) {

    }

    @Override
    public void onPhotoTap(View view, float v, float v1) {
        photoClicked();
    }

    @Override
    public void onOutsidePhotoTap() {
        photoClicked();
    }

    private void photoClicked() {
        if (mListener == null)
            mListener = (FragmentClickListener) getActivity();

        mListener.fragmentClicked();
    }
}
