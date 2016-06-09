package com.simplemobiletools.gallery.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.simplemobiletools.gallery.Constants;
import com.simplemobiletools.gallery.fragments.PhotoFragment;
import com.simplemobiletools.gallery.fragments.VideoFragment;
import com.simplemobiletools.gallery.fragments.ViewPagerFragment;
import com.simplemobiletools.gallery.models.Medium;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyPagerAdapter extends FragmentStatePagerAdapter {
    private List<Medium> media;
    private ViewPagerFragment fragment;
    private Map<Integer, ViewPagerFragment> fragments;

    public MyPagerAdapter(FragmentManager fm, List<Medium> media) {
        super(fm);
        this.media = media;
        fragments = new HashMap<>();
    }

    @Override
    public int getCount() {
        return media.size();
    }

    @Override
    public Fragment getItem(int position) {
        final Medium medium = media.get(position);
        final Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.MEDIUM, medium);

        if (medium.getIsVideo()) {
            fragment = new VideoFragment();
        } else {
            fragment = new PhotoFragment();
        }

        fragments.put(position, fragment);
        fragment.setArguments(bundle);
        return fragment;
    }

    public void itemDragged(int pos) {
        if (fragments.get(pos) != null) {
            fragments.get(pos).itemDragged();
        }
    }

    public void updateItems(List<Medium> newPaths) {
        media.clear();
        media.addAll(newPaths);
        notifyDataSetChanged();
    }
}
