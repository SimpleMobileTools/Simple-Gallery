package com.simplemobiletools.commons.interfaces

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter

open class ItemMoveCallback(private val mAdapter: ItemTouchHelperContract, private val allowHorizontalDrag: Boolean = false) : ItemTouchHelper.Callback() {
    override fun isLongPressDragEnabled() = false

    override fun isItemViewSwipeEnabled() = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {}

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        var dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        if (allowHorizontalDrag) {
            dragFlags = dragFlags or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        }
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        mAdapter.onRowMoved(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder is MyRecyclerViewAdapter.ViewHolder) {
                mAdapter.onRowSelected(viewHolder)
            }
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (viewHolder is MyRecyclerViewAdapter.ViewHolder) {
            mAdapter.onRowClear(viewHolder)
        }
    }
}
