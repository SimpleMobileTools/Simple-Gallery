package com.simplemobiletools.gallery.activities;

import android.os.Bundle;

public class VideoActivity extends PhotoVideoActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mIsVideo = true;
        super.onCreate(savedInstanceState);
    }
}
