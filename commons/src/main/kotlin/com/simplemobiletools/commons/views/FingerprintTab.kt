package com.simplemobiletools.commons.views

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.provider.Settings
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.biometric.auth.AuthPromptHost
import com.github.ajalt.reprint.core.AuthenticationFailureReason
import com.github.ajalt.reprint.core.AuthenticationListener
import com.github.ajalt.reprint.core.Reprint
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PROTECTION_FINGERPRINT
import com.simplemobiletools.commons.interfaces.HashListener
import com.simplemobiletools.commons.interfaces.SecurityTab
import kotlinx.android.synthetic.main.tab_fingerprint.view.*

class FingerprintTab(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs), SecurityTab {
    private val RECHECK_PERIOD = 3000L
    private val registerHandler = Handler()

    lateinit var hashListener: HashListener

    override fun onFinishInflate() {
        super.onFinishInflate()
        val textColor = context.getProperTextColor()
        context.updateTextColors(fingerprint_lock_holder)
        fingerprint_image.applyColorFilter(textColor)

        fingerprint_settings.setOnClickListener {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    override fun initTab(
        requiredHash: String,
        listener: HashListener,
        scrollView: MyScrollView,
        biometricPromptHost: AuthPromptHost,
        showBiometricAuthentication: Boolean
    ) {
        hashListener = listener
    }

    override fun visibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            checkRegisteredFingerprints()
        } else {
            Reprint.cancelAuthentication()
        }
    }

    private fun checkRegisteredFingerprints() {
        val hasFingerprints = Reprint.hasFingerprintRegistered()
        fingerprint_settings.beGoneIf(hasFingerprints)
        fingerprint_label.text = context.getString(if (hasFingerprints) R.string.place_finger else R.string.no_fingerprints_registered)

        Reprint.authenticate(object : AuthenticationListener {
            override fun onSuccess(moduleTag: Int) {
                hashListener.receivedHash("", PROTECTION_FINGERPRINT)
            }

            override fun onFailure(failureReason: AuthenticationFailureReason?, fatal: Boolean, errorMessage: CharSequence?, moduleTag: Int, errorCode: Int) {
                when (failureReason) {
                    AuthenticationFailureReason.AUTHENTICATION_FAILED -> context.toast(R.string.authentication_failed)
                    AuthenticationFailureReason.LOCKED_OUT -> context.toast(R.string.authentication_blocked)
                    else -> {}
                }
            }
        })

        registerHandler.postDelayed({
            checkRegisteredFingerprints()
        }, RECHECK_PERIOD)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        registerHandler.removeCallbacksAndMessages(null)
        Reprint.cancelAuthentication()
    }
}
