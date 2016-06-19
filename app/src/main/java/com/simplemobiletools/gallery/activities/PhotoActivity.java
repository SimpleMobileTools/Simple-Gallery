package com.simplemobiletools.gallery.activities;

import android.os.Bundle;

public class PhotoActivity extends PhotoVideoActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mIsVideo = false;
        super.onCreate(savedInstanceState);
    }
}
