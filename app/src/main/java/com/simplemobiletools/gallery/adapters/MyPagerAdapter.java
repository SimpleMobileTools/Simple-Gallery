package com.simplemobiletools.gallery.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

import com.simplemobiletools.gallery.ViewPagerFragment;

public class MyPagerAdapter extends FragmentStatePagerAdapter {
    private List<String> paths;

    public MyPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void setPaths(List<String> paths) {
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
}
