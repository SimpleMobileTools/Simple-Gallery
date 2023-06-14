package com.simplemobiletools.commons.adapters

import android.graphics.Color
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.interfaces.MyActionModeCallback
import com.simplemobiletools.commons.views.MyRecyclerView

abstract class MyRecyclerViewAdapter(val activity: BaseSimpleActivity, val recyclerView: MyRecyclerView, val itemClick: (Any) -> Unit) :
    RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder>() {
    protected val baseConfig = activity.baseConfig
    protected val resources = activity.resources!!
    protected val layoutInflater = activity.layoutInflater
    protected var textColor = activity.getProperTextColor()
    protected var backgroundColor = activity.getProperBackgroundColor()
    protected var properPrimaryColor = activity.getProperPrimaryColor()
    protected var contrastColor = properPrimaryColor.getContrastColor()
    protected var actModeCallback: MyActionModeCallback
    protected var selectedKeys = LinkedHashSet<Int>()
    protected var positionOffset = 0
    protected var actMode: ActionMode? = null

    private var actBarTextView: TextView? = null
    private var lastLongPressedItem = -1

    abstract fun getActionMenuId(): Int

    abstract fun prepareActionMode(menu: Menu)

    abstract fun actionItemPressed(id: Int)

    abstract fun getSelectableItemCount(): Int

    abstract fun getIsItemSelectable(position: Int): Boolean

    abstract fun getItemSelectionKey(position: Int): Int?

    abstract fun getItemKeyPosition(key: Int): Int

    abstract fun onActionModeCreated()

    abstract fun onActionModeDestroyed()

    protected fun isOneItemSelected() = selectedKeys.size == 1

    init {
        actModeCallback = object : MyActionModeCallback() {
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                actionItemPressed(item.itemId)
                return true
            }

            override fun onCreateActionMode(actionMode: ActionMode, menu: Menu?): Boolean {
                if (getActionMenuId() == 0) {
                    return true
                }

                selectedKeys.clear()
                isSelectable = true
                actMode = actionMode
                actBarTextView = layoutInflater.inflate(R.layout.actionbar_title, null) as TextView
                actBarTextView!!.layoutParams = ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
                actMode!!.customView = actBarTextView
                actBarTextView!!.setOnClickListener {
                    if (getSelectableItemCount() == selectedKeys.size) {
                        finishActMode()
                    } else {
                        selectAll()
                    }
                }

                activity.menuInflater.inflate(getActionMenuId(), menu)
                val bgColor = if (baseConfig.isUsingSystemTheme) {
                    resources.getColor(R.color.you_contextual_status_bar_color, activity.theme)
                } else {
                    Color.BLACK
                }

                actBarTextView!!.setTextColor(bgColor.getContrastColor())
                activity.updateMenuItemColors(menu, baseColor = bgColor)
                onActionModeCreated()

                if (baseConfig.isUsingSystemTheme) {
                    actBarTextView?.onGlobalLayout {
                        val backArrow = activity.findViewById<ImageView>(R.id.action_mode_close_button)
                        backArrow?.applyColorFilter(bgColor.getContrastColor())
                    }
                }
                return true
            }

            override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                prepareActionMode(menu)
                return true
            }

            override fun onDestroyActionMode(actionMode: ActionMode) {
                isSelectable = false
                (selectedKeys.clone() as HashSet<Int>).forEach {
                    val position = getItemKeyPosition(it)
                    if (position != -1) {
                        toggleItemSelection(false, position, false)
                    }
                }
                updateTitle()
                selectedKeys.clear()
                actBarTextView?.text = ""
                actMode = null
                lastLongPressedItem = -1
                onActionModeDestroyed()
            }
        }
    }

    protected fun toggleItemSelection(select: Boolean, pos: Int, updateTitle: Boolean = true) {
        if (select && !getIsItemSelectable(pos)) {
            return
        }

        val itemKey = getItemSelectionKey(pos) ?: return
        if ((select && selectedKeys.contains(itemKey)) || (!select && !selectedKeys.contains(itemKey))) {
            return
        }

        if (select) {
            selectedKeys.add(itemKey)
        } else {
            selectedKeys.remove(itemKey)
        }

        notifyItemChanged(pos + positionOffset)

        if (updateTitle) {
            updateTitle()
        }

        if (selectedKeys.isEmpty()) {
            finishActMode()
        }
    }

    private fun updateTitle() {
        val selectableItemCount = getSelectableItemCount()
        val selectedCount = Math.min(selectedKeys.size, selectableItemCount)
        val oldTitle = actBarTextView?.text
        val newTitle = "$selectedCount / $selectableItemCount"
        if (oldTitle != newTitle) {
            actBarTextView?.text = newTitle
            actMode?.invalidate()
        }
    }

    fun itemLongClicked(position: Int) {
        recyclerView.setDragSelectActive(position)
        lastLongPressedItem = if (lastLongPressedItem == -1) {
            position
        } else {
            val min = Math.min(lastLongPressedItem, position)
            val max = Math.max(lastLongPressedItem, position)
            for (i in min..max) {
                toggleItemSelection(true, i, false)
            }
            updateTitle()
            position
        }
    }

    protected fun getSelectedItemPositions(sortDescending: Boolean = true): ArrayList<Int> {
        val positions = ArrayList<Int>()
        val keys = selectedKeys.toList()
        keys.forEach {
            val position = getItemKeyPosition(it)
            if (position != -1) {
                positions.add(position)
            }
        }

        if (sortDescending) {
            positions.sortDescending()
        }
        return positions
    }

    protected fun selectAll() {
        val cnt = itemCount - positionOffset
        for (i in 0 until cnt) {
            toggleItemSelection(true, i, false)
        }
        lastLongPressedItem = -1
        updateTitle()
    }

    protected fun setupDragListener(enable: Boolean) {
        if (enable) {
            recyclerView.setupDragListener(object : MyRecyclerView.MyDragListener {
                override fun selectItem(position: Int) {
                    toggleItemSelection(true, position, true)
                }

                override fun selectRange(initialSelection: Int, lastDraggedIndex: Int, minReached: Int, maxReached: Int) {
                    selectItemRange(
                        initialSelection,
                        Math.max(0, lastDraggedIndex - positionOffset),
                        Math.max(0, minReached - positionOffset),
                        maxReached - positionOffset
                    )
                    if (minReached != maxReached) {
                        lastLongPressedItem = -1
                    }
                }
            })
        } else {
            recyclerView.setupDragListener(null)
        }
    }

    protected fun selectItemRange(from: Int, to: Int, min: Int, max: Int) {
        if (from == to) {
            (min..max).filter { it != from }.forEach { toggleItemSelection(false, it, true) }
            return
        }

        if (to < from) {
            for (i in to..from) {
                toggleItemSelection(true, i, true)
            }

            if (min > -1 && min < to) {
                (min until to).filter { it != from }.forEach { toggleItemSelection(false, it, true) }
            }

            if (max > -1) {
                for (i in from + 1..max) {
                    toggleItemSelection(false, i, true)
                }
            }
        } else {
            for (i in from..to) {
                toggleItemSelection(true, i, true)
            }

            if (max > -1 && max > to) {
                (to + 1..max).filter { it != from }.forEach { toggleItemSelection(false, it, true) }
            }

            if (min > -1) {
                for (i in min until from) {
                    toggleItemSelection(false, i, true)
                }
            }
        }
    }

    fun setupZoomListener(zoomListener: MyRecyclerView.MyZoomListener?) {
        recyclerView.setupZoomListener(zoomListener)
    }

    fun addVerticalDividers(add: Boolean) {
        if (recyclerView.itemDecorationCount > 0) {
            recyclerView.removeItemDecorationAt(0)
        }

        if (add) {
            DividerItemDecoration(activity, DividerItemDecoration.VERTICAL).apply {
                setDrawable(resources.getDrawable(R.drawable.divider))
                recyclerView.addItemDecoration(this)
            }
        }
    }

    fun finishActMode() {
        actMode?.finish()
    }

    fun updateTextColor(textColor: Int) {
        this.textColor = textColor
        notifyDataSetChanged()
    }

    fun updatePrimaryColor() {
        properPrimaryColor = activity.getProperPrimaryColor()
        contrastColor = properPrimaryColor.getContrastColor()
    }

    fun updateBackgroundColor(backgroundColor: Int) {
        this.backgroundColor = backgroundColor
    }

    protected fun createViewHolder(layoutType: Int, parent: ViewGroup?): ViewHolder {
        val view = layoutInflater.inflate(layoutType, parent, false)
        return ViewHolder(view)
    }

    protected fun bindViewHolder(holder: ViewHolder) {
        holder.itemView.tag = holder
    }

    protected fun removeSelectedItems(positions: ArrayList<Int>) {
        positions.forEach {
            notifyItemRemoved(it)
        }
        finishActMode()
    }

    open inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(any: Any, allowSingleClick: Boolean, allowLongClick: Boolean, callback: (itemView: View, adapterPosition: Int) -> Unit): View {
            return itemView.apply {
                callback(this, adapterPosition)

                if (allowSingleClick) {
                    setOnClickListener { viewClicked(any) }
                    setOnLongClickListener { if (allowLongClick) viewLongClicked() else viewClicked(any); true }
                } else {
                    setOnClickListener(null)
                    setOnLongClickListener(null)
                }
            }
        }

        fun viewClicked(any: Any) {
            if (actModeCallback.isSelectable) {
                val currentPosition = adapterPosition - positionOffset
                val isSelected = selectedKeys.contains(getItemSelectionKey(currentPosition))
                toggleItemSelection(!isSelected, currentPosition, true)
            } else {
                itemClick.invoke(any)
            }
            lastLongPressedItem = -1
        }

        fun viewLongClicked() {
            val currentPosition = adapterPosition - positionOffset
            if (!actModeCallback.isSelectable) {
                activity.startActionMode(actModeCallback)
            }

            toggleItemSelection(true, currentPosition, true)
            itemLongClicked(currentPosition)
        }
    }
}
