package com.simplemobiletools.gallery.fragments;

import android.support.v4.app.Fragment;

public abstract class ViewPagerFragment extends Fragment {
    public abstract void itemDragged();

    public abstract void systemUiVisibilityChanged(boolean toFullscreen);
}
