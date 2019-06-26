package com.simplemobiletools.gallery.pro.activities

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import com.simplemobiletools.gallery.pro.R

class SearchActivity : SimpleActivity() {
    private var mIsSearchOpen = false
    private var mSearchMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
    }

    override fun onStop() {
        super.onStop()
        mSearchMenuItem?.collapseActionView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        setupSearch(menu)
        return true
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        mSearchMenuItem = menu.findItem(R.id.search)
        (mSearchMenuItem?.actionView as? SearchView)?.apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (mIsSearchOpen) {
                        textChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                mIsSearchOpen = true
                return true
            }

            // this triggers on device rotation too, avoid doing anything
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                if (mIsSearchOpen) {
                    mIsSearchOpen = false
                    textChanged("")
                }
                return true
            }
        })
        mSearchMenuItem?.expandActionView()
    }

    private fun textChanged(text: String) {

    }
}
