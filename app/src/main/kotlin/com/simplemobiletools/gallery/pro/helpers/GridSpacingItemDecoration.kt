package com.simplemobiletools.gallery.pro.helpers

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.gallery.pro.models.Medium
import com.simplemobiletools.gallery.pro.models.ThumbnailItem

class GridSpacingItemDecoration(val spanCount: Int, val spacing: Int, val isScrollingHorizontally: Boolean, val addSideSpacing: Boolean,
                                var items: ArrayList<ThumbnailItem>, val useGridPosition: Boolean) : RecyclerView.ItemDecoration() {

    override fun toString() = "spanCount: $spanCount, spacing: $spacing, isScrollingHorizontally: $isScrollingHorizontally, addSideSpacing: $addSideSpacing, " +
            "items: ${items.hashCode()}, useGridPosition: $useGridPosition"

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (spacing <= 1) {
            return
        }

        val position = parent.getChildAdapterPosition(view)
        val medium = items.getOrNull(position) as? Medium ?: return
        val gridPositionToUse = if (useGridPosition) medium.gridPosition else position
        val column = gridPositionToUse % spanCount

        if (isScrollingHorizontally) {
            if (addSideSpacing) {
                outRect.top = spacing - column * spacing / spanCount
                outRect.bottom = (column + 1) * spacing / spanCount
                outRect.right = spacing

                if (position < spanCount) {
                    outRect.left = spacing
                }
            } else {
                outRect.top = column * spacing / spanCount
                outRect.bottom = spacing - (column + 1) * spacing / spanCount
                if (position >= spanCount) {
                    outRect.left = spacing
                }
            }
        } else {
            if (addSideSpacing) {
                outRect.left = spacing - column * spacing / spanCount
                outRect.right = (column + 1) * spacing / spanCount
                outRect.bottom = spacing

                if (position < spanCount && !useGridPosition) {
                    outRect.top = spacing
                }
            } else {
                outRect.left = column * spacing / spanCount
                outRect.right = spacing - (column + 1) * spacing / spanCount

                if (gridPositionToUse >= spanCount) {
                    outRect.top = spacing
                }
            }
        }
    }
}
