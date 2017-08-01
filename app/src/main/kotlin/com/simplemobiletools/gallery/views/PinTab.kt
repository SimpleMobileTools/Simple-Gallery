package com.simplemobiletools.gallery.views

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.tab_pin.view.*

class PinTab(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    var pin = ""

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

    }

    private fun updatePinCode() {
        pin_lock_current_pin.text = "*".repeat(pin.length)
    }
}
