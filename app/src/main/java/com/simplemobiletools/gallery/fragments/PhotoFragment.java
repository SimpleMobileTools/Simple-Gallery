package com.simplemobiletools.gallery.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.R;
import com.simplemobiletools.gallery.activities.ViewPagerActivity;
import com.simplemobiletools.gallery.models.Medium;

public class PhotoFragment extends ViewPagerFragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pager_photo_item, container, false);

        final Medium medium = (Medium) getArguments().getSerializable(Constants.MEDIUM);
        if (medium == null)
            return view;

        final SubsamplingScaleImageView imageView = (SubsamplingScaleImageView) view.findViewById(R.id.photo_view);
        imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
        imageView.setImage(ImageSource.uri(medium.getPath()));
        imageView.setMaxScale(5f);
        imageView.setOnClickListener(this);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        ((ViewPagerActivity) getActivity()).fragmentClicked();
    }

    @Override
    public void itemDragged() {

    }
}
