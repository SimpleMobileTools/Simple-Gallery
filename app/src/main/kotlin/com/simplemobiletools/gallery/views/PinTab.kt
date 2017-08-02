package com.simplemobiletools.gallery.views

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.helpers.PROTECTION_PIN
import com.simplemobiletools.gallery.interfaces.HashListener
import com.simplemobiletools.gallery.interfaces.SecurityTab
import kotlinx.android.synthetic.main.tab_pin.view.*
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

class PinTab(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs), SecurityTab {
    private var hash = ""
    private var requiredHash = ""
    private var pin = ""
    lateinit var hashListener: HashListener

    override fun onFinishInflate() {
        super.onFinishInflate()
        context.updateTextColors(pin_lock_holder)

        pin_0.setOnClickListener { addNumber("0") }
        pin_1.setOnClickListener { addNumber("1") }
        pin_2.setOnClickListener { addNumber("2") }
        pin_3.setOnClickListener { addNumber("3") }
        pin_4.setOnClickListener { addNumber("4") }
        pin_5.setOnClickListener { addNumber("5") }
        pin_6.setOnClickListener { addNumber("6") }
        pin_7.setOnClickListener { addNumber("7") }
        pin_8.setOnClickListener { addNumber("8") }
        pin_9.setOnClickListener { addNumber("9") }
        pin_c.setOnClickListener { clear() }
        pin_ok.setOnClickListener { confirmPIN() }
    }

    override fun initTab(requiredHash: String, listener: HashListener) {
        this.requiredHash = requiredHash
        hash = requiredHash
        hashListener = listener
    }

    private fun addNumber(number: String) {
        if (pin.length < 10) {
            pin += number
            updatePinCode()
        }
    }

    private fun clear() {
        if (pin.isNotEmpty()) {
            pin = pin.substring(0, pin.length - 1)
            updatePinCode()
        }
    }

    private fun confirmPIN() {
        val newHash = getHashedPin()
        if (pin.isEmpty()) {
            context.toast(R.string.please_enter_pin)
        } else if (hash.isEmpty()) {
            hash = newHash
            resetPin()
            pin_lock_title.setText(R.string.repeat_pin)
        } else if (hash == newHash) {
            hashListener.receivedHash(hash, PROTECTION_PIN)
        } else {
            resetPin()
            context.toast(R.string.wrong_pin)
            if (requiredHash.isEmpty()) {
                hash = ""
                pin_lock_title.setText(R.string.enter_pin)
            }
        }
    }

    private fun resetPin() {
        pin = ""
        pin_lock_current_pin.text = ""
    }

    private fun updatePinCode() {
        pin_lock_current_pin.text = "*".repeat(pin.length)
    }

    private fun getHashedPin(): String {
        val messageDigest = MessageDigest.getInstance("SHA-1")
        messageDigest.update(pin.toByteArray(charset("UTF-8")))
        val digest = messageDigest.digest()
        val bigInteger = BigInteger(1, digest)
        return String.format(Locale.getDefault(), "%0${digest.size * 2}x", bigInteger).toLowerCase()
    }
}
