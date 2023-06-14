package com.simplemobiletools.commons.views

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager

class MyGridLayoutManager : GridLayoutManager {
    constructor(context: Context, spanCount: Int) : super(context, spanCount)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, spanCount: Int, orientation: Int, reverseLayout: Boolean) : super(context, spanCount, orientation, reverseLayout)

    // fixes crash java.lang.IndexOutOfBoundsException: Inconsistency detected...
    // taken from https://stackoverflow.com/a/33985508/1967672
    override fun supportsPredictiveItemAnimations() = false
}
