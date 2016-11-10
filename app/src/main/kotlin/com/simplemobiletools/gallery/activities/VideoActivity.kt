package com.simplemobiletools.gallery.activities

import android.os.Bundle

class VideoActivity : PhotoVideoActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        PhotoVideoActivity.mIsVideo = true
        super.onCreate(savedInstanceState)
    }
}
