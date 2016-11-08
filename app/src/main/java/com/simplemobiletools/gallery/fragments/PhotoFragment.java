package com.simplemobiletools.gallery.fragments;

import android.net.Uri;
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
import com.simplemobiletools.gallery.Utils;
import com.simplemobiletools.gallery.models.Medium;

public class PhotoFragment extends ViewPagerFragment implements View.OnClickListener {
    private SubsamplingScaleImageView mSubsamplingView;
    private Medium mMedium;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pager_photo_item, container, false);

        mMedium = (Medium) getArguments().getSerializable(Constants.MEDIUM);
        if (mMedium.getPath().startsWith("content://"))
            mMedium.setPath(Utils.Companion.getRealPathFromURI(getContext(), Uri.parse(mMedium.getPath())));

        if (mMedium == null)
            return view;

        mSubsamplingView = (SubsamplingScaleImageView) view.findViewById(R.id.photo_view);
        if (mMedium.isGif()) {
            mSubsamplingView.setVisibility(View.GONE);
            final ImageView imageView = (ImageView) view.findViewById(R.id.gif_view);
            imageView.setVisibility(View.VISIBLE);
            Glide.with(getContext()).load(mMedium.getPath()).asGif().diskCacheStrategy(DiskCacheStrategy.NONE).into(imageView);
            imageView.setOnClickListener(this);
        } else {
            mSubsamplingView.setDoubleTapZoomScale(1.2f);
            mSubsamplingView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
            mSubsamplingView.setImage(ImageSource.uri(mMedium.getPath()));
            mSubsamplingView.setMaxScale(4f);
            mSubsamplingView.setMinimumTileDpi(200);
            mSubsamplingView.setOnClickListener(this);
        }

        return view;
    }

    @Override
    public void itemDragged() {

    }

    @Override
    public void systemUiVisibilityChanged(boolean toFullscreen) {

    }

    @Override
    public void updateItem() {
        mSubsamplingView.setImage(ImageSource.uri(mMedium.getPath()));
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
