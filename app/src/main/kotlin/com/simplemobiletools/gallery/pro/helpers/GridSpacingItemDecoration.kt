package com.simplemobiletools.gallery.pro.helpers

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(val spanCount: Int, val spacing: Int, val isScrollingHorizontally: Boolean) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        if (isScrollingHorizontally) {
            outRect.top = column * spacing / spanCount
            outRect.bottom = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.left = spacing
            }
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing
            }
        }
    }
}
