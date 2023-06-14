package com.simplemobiletools.commons.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.models.FileDirItem
import kotlinx.android.synthetic.main.item_breadcrumb.view.*

class Breadcrumbs(context: Context, attrs: AttributeSet) : HorizontalScrollView(context, attrs) {
    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val itemsLayout: LinearLayout
    private var textColor = context.getProperTextColor()
    private var fontSize = resources.getDimension(R.dimen.bigger_text_size)
    private var lastPath = ""
    private var isLayoutDirty = true
    private var isScrollToSelectedItemPending = false
    private var isFirstScroll = true
    private var stickyRootInitialLeft = 0
    private var rootStartPadding = 0

    private val textColorStateList: ColorStateList
        get() = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_activated), intArrayOf()),
            intArrayOf(
                textColor,
                textColor.adjustAlpha(0.6f)
            )
        )

    var listener: BreadcrumbsListener? = null
    var isShownInDialog = false

    init {
        isHorizontalScrollBarEnabled = false
        itemsLayout = LinearLayout(context)
        itemsLayout.orientation = LinearLayout.HORIZONTAL
        rootStartPadding = paddingStart
        itemsLayout.setPaddingRelative(0, paddingTop, paddingEnd, paddingBottom)
        setPaddingRelative(0, 0, 0, 0)
        addView(itemsLayout, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        onGlobalLayout {
            stickyRootInitialLeft = if (itemsLayout.childCount > 0) {
                itemsLayout.getChildAt(0).left
            } else {
                0
            }
        }
    }

    private fun recomputeStickyRootLocation(left: Int) {
        stickyRootInitialLeft = left
        handleRootStickiness(scrollX)
    }

    private fun handleRootStickiness(scrollX: Int) {
        if (scrollX > stickyRootInitialLeft) {
            stickRoot(scrollX - stickyRootInitialLeft)
        } else {
            freeRoot()
        }
    }

    private fun freeRoot() {
        if (itemsLayout.childCount > 0) {
            itemsLayout.getChildAt(0).translationX = 0f
        }
    }

    private fun stickRoot(translationX: Int) {
        if (itemsLayout.childCount > 0) {
            val root = itemsLayout.getChildAt(0)
            root.translationX = translationX.toFloat()
            ViewCompat.setTranslationZ(root, translationZ)
        }
    }

    override fun onScrollChanged(scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        super.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY)
        handleRootStickiness(scrollX)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        isLayoutDirty = false
        if (isScrollToSelectedItemPending) {
            scrollToSelectedItem()
            isScrollToSelectedItemPending = false
        }

        recomputeStickyRootLocation(left)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var heightMeasureSpec = heightMeasureSpec
        if (heightMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.AT_MOST) {
            var height = context.resources.getDimensionPixelSize(R.dimen.breadcrumbs_layout_height)
            if (heightMode == MeasureSpec.AT_MOST) {
                height = height.coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
            }
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun scrollToSelectedItem() {
        if (isLayoutDirty) {
            isScrollToSelectedItemPending = true
            return
        }

        var selectedIndex = itemsLayout.childCount - 1
        val cnt = itemsLayout.childCount
        for (i in 0 until cnt) {
            val child = itemsLayout.getChildAt(i)
            if ((child.tag as? FileDirItem)?.path?.trimEnd('/') == lastPath.trimEnd('/')) {
                selectedIndex = i
                break
            }
        }

        val selectedItemView = itemsLayout.getChildAt(selectedIndex)
        val scrollX = if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
            selectedItemView.left - itemsLayout.paddingStart
        } else {
            selectedItemView.right - width + itemsLayout.paddingStart
        }

        if (!isFirstScroll && isShown) {
            smoothScrollTo(scrollX, 0)
        } else {
            scrollTo(scrollX, 0)
        }

        isFirstScroll = false
    }

    override fun requestLayout() {
        isLayoutDirty = true
        super.requestLayout()
    }

    fun setBreadcrumb(fullPath: String) {
        lastPath = fullPath
        val basePath = fullPath.getBasePath(context)
        var currPath = basePath
        val tempPath = context.humanizePath(fullPath)

        itemsLayout.removeAllViews()
        val dirs = tempPath.split("/").dropLastWhile(String::isEmpty)
        for (i in dirs.indices) {
            val dir = dirs[i]
            if (i > 0) {
                currPath += dir + "/"
            }

            if (dir.isEmpty()) {
                continue
            }

            currPath = "${currPath.trimEnd('/')}/"
            val item = FileDirItem(currPath, dir, true, 0, 0, 0)
            addBreadcrumb(item, i, i > 0)
            scrollToSelectedItem()
        }
    }

    private fun addBreadcrumb(item: FileDirItem, index: Int, addPrefix: Boolean) {
        if (itemsLayout.childCount == 0) {
            val firstItemBgColor = if (isShownInDialog && context.baseConfig.isUsingSystemTheme) {
                resources.getColor(R.color.you_dialog_background_color, context.theme)
            } else {
                context.getProperBackgroundColor()
            }

            inflater.inflate(R.layout.item_breadcrumb_first, itemsLayout, false).apply {
                resources.apply {
                    breadcrumb_text.background = ContextCompat.getDrawable(context, R.drawable.button_background)
                    breadcrumb_text.background.applyColorFilter(textColor)
                    elevation = 1f
                    background = ColorDrawable(firstItemBgColor)
                    val medium = getDimension(R.dimen.medium_margin).toInt()
                    breadcrumb_text.setPadding(medium, medium, medium, medium)
                    setPadding(rootStartPadding, 0, 0, 0)
                }

                isActivated = item.path.trimEnd('/') == lastPath.trimEnd('/')
                breadcrumb_text.text = item.name
                breadcrumb_text.setTextColor(textColorStateList)
                breadcrumb_text.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

                itemsLayout.addView(this)

                breadcrumb_text.setOnClickListener {
                    if (itemsLayout.getChildAt(index) != null) {
                        listener?.breadcrumbClicked(index)
                    }
                }

                tag = item
            }
        } else {
            inflater.inflate(R.layout.item_breadcrumb, itemsLayout, false).apply {
                var textToAdd = item.name
                if (addPrefix) {
                    textToAdd = "> $textToAdd"
                }

                isActivated = item.path.trimEnd('/') == lastPath.trimEnd('/')

                breadcrumb_text.text = textToAdd
                breadcrumb_text.setTextColor(textColorStateList)
                breadcrumb_text.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

                itemsLayout.addView(this)

                setOnClickListener { v ->
                    if (itemsLayout.getChildAt(index) != null && itemsLayout.getChildAt(index) == v) {
                        if ((v.tag as? FileDirItem)?.path?.trimEnd('/') == lastPath.trimEnd('/')) {
                            scrollToSelectedItem()
                        } else {
                            listener?.breadcrumbClicked(index)
                        }
                    }
                }

                tag = item
            }
        }
    }

    fun updateColor(color: Int) {
        textColor = color
        setBreadcrumb(lastPath)
    }

    fun updateFontSize(size: Float, updateTexts: Boolean) {
        fontSize = size
        if (updateTexts) {
            setBreadcrumb(lastPath)
        }
    }

    fun removeBreadcrumb() {
        itemsLayout.removeView(itemsLayout.getChildAt(itemsLayout.childCount - 1))
    }

    fun getItem(index: Int) = itemsLayout.getChildAt(index).tag as FileDirItem

    fun getLastItem() = itemsLayout.getChildAt(itemsLayout.childCount - 1).tag as FileDirItem

    fun getItemCount() = itemsLayout.childCount

    interface BreadcrumbsListener {
        fun breadcrumbClicked(id: Int)
    }
}
