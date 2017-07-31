package com.simplemobiletools.gallery.views

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.tab_pin.view.*

class PinTab(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    override fun onFinishInflate() {
        super.onFinishInflate()
        context.updateTextColors(pin_lock_holder)
    }
}
