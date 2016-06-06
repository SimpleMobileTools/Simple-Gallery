package com.simplemobiletools.gallery;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.simplemobiletools.gallery.activities.ViewPagerActivity;

public class ViewPagerFragment extends Fragment implements View.OnClickListener {
    private static final String MEDIUM = "medium";
    private Media medium;

    public void setMedium(Media medium) {
        this.medium = medium;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.pager_item, container, false);

        if (medium == null && savedInstanceState != null) {
            medium = (Media) savedInstanceState.getSerializable(MEDIUM);
        }

        if (medium != null) {
            final SubsamplingScaleImageView imageView = (SubsamplingScaleImageView) view.findViewById(R.id.photo);
            imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
            imageView.setImage(ImageSource.uri(medium.getPath()));
            imageView.setMaxScale(5f);
            imageView.setOnClickListener(this);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(MEDIUM, medium);
    }

    @Override
    public void onClick(View v) {
        ((ViewPagerActivity) getActivity()).photoClicked();
    }
}
