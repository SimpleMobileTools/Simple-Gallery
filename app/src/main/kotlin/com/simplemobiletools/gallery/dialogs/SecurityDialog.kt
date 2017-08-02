package com.simplemobiletools.gallery.dialogs

import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.adapters.PasswordTypesAdapter
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.SHOW_ALL_TABS
import com.simplemobiletools.gallery.interfaces.HashListener
import com.simplemobiletools.gallery.views.MyDialogViewPager
import kotlinx.android.synthetic.main.dialog_security.view.*

class SecurityDialog(val activity: SimpleActivity, val requiredHash: String, val showTabIndex: Int, val callback: (hash: String, type: Int) -> Unit) : HashListener {
    var dialog: AlertDialog? = null
    val view = LayoutInflater.from(activity).inflate(R.layout.dialog_security, null)

    init {
        view.apply {
            val viewPager = findViewById(R.id.dialog_tab_view_pager) as MyDialogViewPager
            viewPager.adapter = PasswordTypesAdapter(context, requiredHash, this@SecurityDialog)
            viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    dialog_tab_layout.getTabAt(position)!!.select()
                }
            })

            if (showTabIndex == SHOW_ALL_TABS) {
                val textColor = context.config.textColor
                dialog_tab_layout.setTabTextColors(textColor, textColor)
                dialog_tab_layout.setSelectedTabIndicatorColor(context.config.primaryColor)
                dialog_tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabReselected(tab: TabLayout.Tab?) {
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab) {
                    }

                    override fun onTabSelected(tab: TabLayout.Tab) {
                        if (tab.text.toString().equals(resources.getString(R.string.pattern), true)) {
                            viewPager.currentItem = 0
                        } else {
                            viewPager.currentItem = 1
                        }
                    }
                })
            } else {
                dialog_tab_layout.beGone()
                viewPager.currentItem = showTabIndex
                viewPager.allowSwiping = false
            }
        }

        dialog = AlertDialog.Builder(activity)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    override fun receivedHash(hash: String, type: Int) {
        callback(hash, type)
        dialog!!.dismiss()
    }
}
