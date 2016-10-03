package com.simplemobiletools.gallery.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.models.Medium;

public class PhotoFragment extends ViewPagerFragment implements View.OnClickListener {
    private View mView;
    private SubsamplingScaleImageView mSubsamplingView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.pager_photo_item, container, false);

        final Medium medium = (Medium) getArguments().getSerializable(Constants.MEDIUM);
        if (medium == null)
            return mView;

        mSubsamplingView = (SubsamplingScaleImageView) mView.findViewById(R.id.photo_view);
        if (medium.isGif()) {
            mSubsamplingView.setVisibility(View.GONE);
            final ImageView imageView = (ImageView) mView.findViewById(R.id.gif_view);
            imageView.setVisibility(View.VISIBLE);
            Glide.with(getContext()).load(medium.getPath()).asGif().diskCacheStrategy(DiskCacheStrategy.NONE).into(imageView);
            imageView.setOnClickListener(this);
        } else {
            mSubsamplingView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
            mSubsamplingView.setImage(ImageSource.uri(medium.getPath()));
            mSubsamplingView.setMaxScale(4f);
            mSubsamplingView.setMinimumTileDpi(200);
            mSubsamplingView.setOnClickListener(this);
        }

        return mView;
    }

    @Override
    public void itemDragged() {

    }

    @Override
    public void systemUiVisibilityChanged(boolean toFullscreen) {

    }

    @Override
    public void confChanged() {

    }

    @Override
    public void onClick(View v) {
        photoClicked();
    }

    private void photoClicked() {
        if (mListener == null)
            mListener = (FragmentClickListener) getActivity();

        mListener.fragmentClicked();
    }
}
