package com.simplemobiletools.gallery;

import android.support.v4.app.Fragment;

public abstract class ViewPagerFragment extends Fragment {
    public abstract void itemDragged();

    public abstract void fragmentHidden();
}
