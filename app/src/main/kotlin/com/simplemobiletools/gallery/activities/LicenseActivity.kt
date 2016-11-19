package com.simplemobiletools.gallery.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.simplemobiletools.gallery.R
import kotlinx.android.synthetic.main.activity_license.*

class LicenseActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)

        license_subsampling_title.setOnClickListener { openUrl(R.string.subsampling_url) }
        license_glide_title.setOnClickListener { openUrl(R.string.glide_url) }
        license_cropper_title.setOnClickListener { openUrl(R.string.cropper_url) }
        license_filepicker_title.setOnClickListener { openUrl(R.string.filepicker_url) }
        license_fileproperties_title.setOnClickListener { openUrl(R.string.fileproperties_url) }
    }

    private fun openUrl(id: Int) {
        val url = resources.getString(id)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }
}
