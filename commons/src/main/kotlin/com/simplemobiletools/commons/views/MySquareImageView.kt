package com.simplemobiletools.commons.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class MySquareImageView : AppCompatImageView {
    var isHorizontalScrolling = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val spec = if (isHorizontalScrolling) heightMeasureSpec else widthMeasureSpec
        super.onMeasure(spec, spec)
    }
}
