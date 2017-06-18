package com.simplemobiletools.gallery.views

import android.content.Context
import android.support.v7.widget.OrientationHelper
import android.util.AttributeSet
import android.widget.ImageView

class MySquareImageView : ImageView {
    var orientation = OrientationHelper.VERTICAL

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val spec = if (orientation == OrientationHelper.VERTICAL) measuredWidth else measuredHeight
        setMeasuredDimension(spec, spec)
    }
}
