package com.simplemobiletools.gallery.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.simplemobiletools.gallery.Media;
import com.simplemobiletools.gallery.ViewPagerFragment;

import java.util.List;

public class MyPagerAdapter extends FragmentStatePagerAdapter {
    private List<Media> media;

    public MyPagerAdapter(FragmentManager fm, List<Media> media) {
        super(fm);
        this.media = media;
    }

    @Override
    public int getCount() {
        return media.size();
    }

    @Override
    public Fragment getItem(int position) {
        final ViewPagerFragment fragment = new ViewPagerFragment();
        fragment.setMedium(media.get(position));
        return fragment;
    }

    public void updateItems(List<Media> newPaths) {
        media.clear();
        media.addAll(newPaths);
        notifyDataSetChanged();
    }
}
