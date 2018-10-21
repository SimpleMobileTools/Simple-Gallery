package com.simplemobiletools.gallery.activities

import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.RelativeLayout
import com.google.vr.sdk.widgets.video.VrVideoEventListener
import com.google.vr.sdk.widgets.video.VrVideoView
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.isPiePlus
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.hideSystemUI
import com.simplemobiletools.gallery.extensions.navigationBarHeight
import com.simplemobiletools.gallery.extensions.showSystemUI
import com.simplemobiletools.gallery.helpers.PATH
import kotlinx.android.synthetic.main.activity_panorama_video.*
import java.io.File

open class PanoramaVideoActivity : SimpleActivity() {
    private var isFullScreen = false
    private var isExploreEnabled = true
    private var isRendering = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panorama_video)
        supportActionBar?.hide()

        if (isPiePlus()) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }

        setupButtonMargins()

        explore.setOnClickListener {
            isExploreEnabled = !isExploreEnabled
            vr_video_view.setPureTouchTracking(isExploreEnabled)
            explore.setImageResource(if (isExploreEnabled) R.drawable.ic_explore else R.drawable.ic_explore_off)
        }

        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                checkIntent()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vr_video_view.resumeRendering()
        isRendering = true
        if (config.blackBackground) {
            updateStatusbarColor(Color.BLACK)
        }

        window.statusBarColor = resources.getColor(R.color.circle_black_background)
    }

    override fun onPause() {
        super.onPause()
        vr_video_view.pauseRendering()
        isRendering = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRendering) {
            vr_video_view.shutdown()
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
            val options = VrVideoView.Options()
            options.inputType = VrVideoView.Options.TYPE_MONO

            vr_video_view.apply {
                loadVideo(Uri.fromFile(File(path)), options)
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

                setEventListener(object : VrVideoEventListener() {
                    override fun onClick() {
                        handleClick()
                    }
                })
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            isFullScreen = visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            toggleButtonVisibility()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        setupButtonMargins()
    }

    private fun setupButtonMargins() {
        (explore.layoutParams as RelativeLayout.LayoutParams).bottomMargin = navigationBarHeight
    }

    private fun toggleButtonVisibility() {
        explore.animate().alpha(if (isFullScreen) 0f else 1f)
        explore.isClickable = !isFullScreen
    }

    private fun handleClick() {
        isFullScreen = !isFullScreen
        toggleButtonVisibility()
        if (isFullScreen) {
            hideSystemUI(false)
        } else {
            showSystemUI(false)
        }
    }
}
