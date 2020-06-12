package com.simplemobiletools.gallery.pro.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView

class MySquareImageView : ImageView {
    var isHorizontalScrolling = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val spec = if (isHorizontalScrolling) heightMeasureSpec else widthMeasureSpec
        super.onMeasure(spec, spec)
    }
}
