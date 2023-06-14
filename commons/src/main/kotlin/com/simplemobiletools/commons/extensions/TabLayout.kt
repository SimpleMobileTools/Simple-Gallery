package com.simplemobiletools.commons.extensions

import com.google.android.material.tabs.TabLayout

fun TabLayout.onTabSelectionChanged(
    tabUnselectedAction: ((inactiveTab: TabLayout.Tab) -> Unit)? = null,
    tabSelectedAction: ((activeTab: TabLayout.Tab) -> Unit)? = null
) = setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
    override fun onTabSelected(tab: TabLayout.Tab) {
        tabSelectedAction?.invoke(tab)
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
        tabUnselectedAction?.invoke(tab)
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        tabSelectedAction?.invoke(tab)
    }
})
