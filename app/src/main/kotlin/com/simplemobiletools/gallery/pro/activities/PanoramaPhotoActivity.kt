package com.simplemobiletools.gallery.pro.activities

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.RelativeLayout
import com.google.vr.sdk.widgets.pano.VrPanoramaEventListener
import com.google.vr.sdk.widgets.pano.VrPanoramaView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.databinding.ActivityPanoramaPhotoBinding
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.hideSystemUI
import com.simplemobiletools.gallery.pro.extensions.showSystemUI
import com.simplemobiletools.gallery.pro.helpers.PATH

open class PanoramaPhotoActivity : SimpleActivity() {
    private val CARDBOARD_DISPLAY_MODE = 3

    private lateinit var  binding: ActivityPanoramaPhotoBinding

    private var isFullscreen = false
    private var isExploreEnabled = true
    private var isRendering = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)

        binding = ActivityPanoramaPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        checkNotchSupport()
        setupButtonMargins()

        binding.cardboard.setOnClickListener {
            binding.panoramaView.displayMode = CARDBOARD_DISPLAY_MODE
        }

        binding.explore.setOnClickListener {
            isExploreEnabled = !isExploreEnabled
            binding.panoramaView.setPureTouchTracking(isExploreEnabled)
            binding.explore.setImageResource(if (isExploreEnabled) R.drawable.ic_explore_vector
            else R.drawable.ic_explore_off_vector)
        }

        checkIntent()
    }

    override fun onResume() {
        super.onResume()
        binding.panoramaView.resumeRendering()
        isRendering = true
        if (config.blackBackground) {
            updateStatusbarColor(Color.BLACK)
        }

        window.statusBarColor = resources.getColor(R.color.circle_black_background)
    }

    override fun onPause() {
        super.onPause()
        binding.panoramaView.pauseRendering()
        isRendering = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRendering) {
            binding.panoramaView.shutdown()
        }
    }

    private fun checkIntent() {
        val path = intent.getStringExtra(PATH)
        if (path == null) {
            toast(R.string.invalid_image_path)
            finish()
            return
        }

        intent.removeExtra(PATH)

        try {
            val options = VrPanoramaView.Options()
            options.inputType = VrPanoramaView.Options.TYPE_MONO
            ensureBackgroundThread {
                val bitmap = getBitmapToLoad(path)
                runOnUiThread {
                    binding.panoramaView.apply {
                        beVisible()
                        loadImageFromBitmap(bitmap, options)
                        setFlingingEnabled(true)
                        setPureTouchTracking(true)

                        // add custom buttons so we can position them and toggle visibility as desired
                        setFullscreenButtonEnabled(false)
                        setInfoButtonEnabled(false)
                        setTransitionViewEnabled(false)
                        setStereoModeButtonEnabled(false)

                        setOnClickListener {
                            handleClick()
                        }

                        setEventListener(object : VrPanoramaEventListener() {
                            override fun onClick() {
                                handleClick()
                            }
                        })
                    }
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            isFullscreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            toggleButtonVisibility()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupButtonMargins()
    }

    private fun getBitmapToLoad(path: String): Bitmap? {
        val options = BitmapFactory.Options()
        options.inSampleSize = 1
        var bitmap: Bitmap? = null

        for (i in 0..10) {
            try {
                bitmap = if (path.startsWith("content://")) {
                    val inputStream = contentResolver.openInputStream(Uri.parse(path))
                    BitmapFactory.decodeStream(inputStream)
                } else {
                    BitmapFactory.decodeFile(path, options)
                }
                break
            } catch (e: OutOfMemoryError) {
                options.inSampleSize *= 2
            }
        }

        return bitmap
    }

    private fun setupButtonMargins() {
        val navBarHeight = navigationBarHeight
        (binding.cardboard.layoutParams as RelativeLayout.LayoutParams).apply {
            bottomMargin = navBarHeight
            rightMargin = navigationBarWidth
        }

        (binding.explore.layoutParams as RelativeLayout.LayoutParams).bottomMargin = navigationBarHeight

        binding.cardboard.onGlobalLayout {
            binding.panoramaGradientBackground.layoutParams.height = navBarHeight + binding.cardboard.height
        }
    }

    private fun toggleButtonVisibility() {
        arrayOf(binding.cardboard, binding.explore, binding.panoramaGradientBackground).forEach {
            it.animate().alpha(if (isFullscreen) 0f else 1f)
            it.isClickable = !isFullscreen
        }
    }

    private fun handleClick() {
        isFullscreen = !isFullscreen
        toggleButtonVisibility()
        if (isFullscreen) {
            hideSystemUI(false)
        } else {
            showSystemUI(false)
        }
    }
}
