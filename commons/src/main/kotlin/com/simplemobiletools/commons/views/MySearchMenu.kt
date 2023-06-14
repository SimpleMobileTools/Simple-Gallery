package com.simplemobiletools.commons.views

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import com.google.android.material.appbar.AppBarLayout
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.LOWER_ALPHA
import com.simplemobiletools.commons.helpers.MEDIUM_ALPHA
import kotlinx.android.synthetic.main.menu_search.view.*

class MySearchMenu(context: Context, attrs: AttributeSet) : AppBarLayout(context, attrs) {
    var isSearchOpen = false
    var useArrowIcon = false
    var onSearchOpenListener: (() -> Unit)? = null
    var onSearchClosedListener: (() -> Unit)? = null
    var onSearchTextChangedListener: ((text: String) -> Unit)? = null
    var onNavigateBackClickListener: (() -> Unit)? = null

    init {
        inflate(context, R.layout.menu_search, this)
    }

    fun getToolbar() = top_toolbar

    fun setupMenu() {
        top_toolbar_search_icon.setOnClickListener {
            if (isSearchOpen) {
                closeSearch()
            } else if (useArrowIcon && onNavigateBackClickListener != null) {
                onNavigateBackClickListener!!()
            } else {
                top_toolbar_search.requestFocus()
                (context as? Activity)?.showKeyboard(top_toolbar_search)
            }
        }

        post {
            top_toolbar_search.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    openSearch()
                }
            }
        }

        top_toolbar_search.onTextChangeListener { text ->
            onSearchTextChangedListener?.invoke(text)
        }
    }

    fun focusView() {
        top_toolbar_search.requestFocus()
    }

    private fun openSearch() {
        isSearchOpen = true
        onSearchOpenListener?.invoke()
        top_toolbar_search_icon.setImageResource(R.drawable.ic_arrow_left_vector)
    }

    fun closeSearch() {
        isSearchOpen = false
        onSearchClosedListener?.invoke()
        top_toolbar_search.setText("")
        if (!useArrowIcon) {
            top_toolbar_search_icon.setImageResource(R.drawable.ic_search_vector)
        }
        (context as? Activity)?.hideKeyboard()
    }

    fun getCurrentQuery() = top_toolbar_search.text.toString()

    fun updateHintText(text: String) {
        top_toolbar_search.hint = text
    }

    fun toggleHideOnScroll(hideOnScroll: Boolean) {
        val params = top_app_bar_layout.layoutParams as LayoutParams
        if (hideOnScroll) {
            params.scrollFlags = LayoutParams.SCROLL_FLAG_SCROLL or LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        } else {
            params.scrollFlags = params.scrollFlags.removeBit(LayoutParams.SCROLL_FLAG_SCROLL or LayoutParams.SCROLL_FLAG_ENTER_ALWAYS)
        }
    }

    fun toggleForceArrowBackIcon(useArrowBack: Boolean) {
        this.useArrowIcon = useArrowBack
        val icon = if (useArrowBack) {
            R.drawable.ic_arrow_left_vector
        } else {
            R.drawable.ic_search_vector
        }

        top_toolbar_search_icon.setImageResource(icon)
    }

    fun updateColors() {
        val backgroundColor = context.getProperBackgroundColor()
        val contrastColor = backgroundColor.getContrastColor()

        setBackgroundColor(backgroundColor)
        top_app_bar_layout.setBackgroundColor(backgroundColor)
        top_toolbar_search_icon.applyColorFilter(contrastColor)
        top_toolbar_holder.background?.applyColorFilter(context.getProperPrimaryColor().adjustAlpha(LOWER_ALPHA))
        top_toolbar_search.setTextColor(contrastColor)
        top_toolbar_search.setHintTextColor(contrastColor.adjustAlpha(MEDIUM_ALPHA))
        (context as? BaseSimpleActivity)?.updateTopBarColors(top_toolbar, backgroundColor)
    }
}
