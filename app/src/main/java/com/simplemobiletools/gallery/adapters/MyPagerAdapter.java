package com.simplemobiletools.gallery.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.simplemobiletools.gallery.ViewPagerFragment;

import java.util.List;

public class MyPagerAdapter extends FragmentStatePagerAdapter {
    private List<String> paths;

    public MyPagerAdapter(FragmentManager fm, List<String> paths) {
        super(fm);
        this.paths = paths;
    }

    @Override
    public int getCount() {
        return paths.size();
    }

    @Override
    public Fragment getItem(int position) {
        final ViewPagerFragment fragment = new ViewPagerFragment();
        fragment.setPath(paths.get(position));
        return fragment;
    }

    public void updateItems(List<String> newPaths) {
        paths.clear();
        paths.addAll(newPaths);
        notifyDataSetChanged();
    }
}
