package com.simplemobiletools.gallery.activities

import android.os.Bundle
import com.simplemobiletools.gallery.R
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_edit.*

class EditActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        crop_image_view.apply {
            guidelines = CropImageView.Guidelines.OFF
        }
    }
}
