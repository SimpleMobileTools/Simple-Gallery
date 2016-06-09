package com.simplemobiletools.gallery.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.Medium;
import com.simplemobiletools.gallery.PhotoFragment;
import com.simplemobiletools.gallery.VideoFragment;
import com.simplemobiletools.gallery.ViewPagerFragment;

import java.util.List;

public class MyPagerAdapter extends FragmentStatePagerAdapter {
    private List<Medium> media;
    private ViewPagerFragment fragment;

    public MyPagerAdapter(FragmentManager fm, List<Medium> media) {
        super(fm);
        this.media = media;
    }

    @Override
    public int getCount() {
        return media.size();
    }

    @Override
    public Fragment getItem(int position) {
        if (fragment != null) {
            fragment.fragmentHidden();
        }

        final Medium medium = media.get(position);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.MEDIUM, medium);

        if (medium.getIsVideo()) {
            fragment = new VideoFragment();
        } else {
            fragment = new PhotoFragment();
        }

        fragment.setArguments(bundle);
        return fragment;
    }

    public void itemDragged() {
        if (fragment != null) {
            fragment.itemDragged();
        }
    }

    public void updateItems(List<Medium> newPaths) {
        media.clear();
        media.addAll(newPaths);
        notifyDataSetChanged();
    }
}
