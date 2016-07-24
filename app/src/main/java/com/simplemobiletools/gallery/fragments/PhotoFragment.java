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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pager_photo_item, container, false);

        final Medium medium = (Medium) getArguments().getSerializable(Constants.MEDIUM);
        if (medium == null)
            return view;

        final SubsamplingScaleImageView subsamplingView = (SubsamplingScaleImageView) view.findViewById(R.id.photo_view);
        if (medium.isGif()) {
            subsamplingView.setVisibility(View.GONE);
            final ImageView imageView = (ImageView) view.findViewById(R.id.gif_view);
            imageView.setVisibility(View.VISIBLE);
            Glide.with(getContext()).load(medium.getPath()).asGif().diskCacheStrategy(DiskCacheStrategy.NONE).into(imageView);
            imageView.setOnClickListener(this);
        } else {
            subsamplingView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
            subsamplingView.setImage(ImageSource.uri(medium.getPath()));
            subsamplingView.setMaxScale(4f);
            subsamplingView.setMinimumTileDpi(200);
            subsamplingView.setOnClickListener(this);
        }

        return view;
    }

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
    public void onClick(View v) {
        photoClicked();
    }

    private void photoClicked() {
        if (mListener == null)
            mListener = (FragmentClickListener) getActivity();

        mListener.fragmentClicked();
    }
}
