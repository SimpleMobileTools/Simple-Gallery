package com.simplemobiletools.commons.views.bottomactionmenu

import android.content.Context
import android.util.AttributeSet
import android.util.Xml
import android.view.MenuItem
import android.view.View
import com.simplemobiletools.commons.R
import org.xmlpull.v1.XmlPullParser

internal class BottomActionMenuParser(private val context: Context) {
    companion object {
        private const val NO_TEXT = ""
        private const val MENU_TAG = "menu"
        private const val MENU_ITEM_TAG = "item"
    }

    fun inflate(menuId: Int): List<BottomActionMenuItem> {
        val parser = context.resources.getLayout(menuId)
        parser.use {
            val attrs = Xml.asAttributeSet(parser)
            return readContextItems(parser, attrs)
        }
    }

    private fun readContextItems(parser: XmlPullParser, attrs: AttributeSet): List<BottomActionMenuItem> {
        val items = mutableListOf<BottomActionMenuItem>()
        var eventType = parser.eventType
        var tagName: String

        // This loop will skip to the menu start tag
        do {
            if (eventType == XmlPullParser.START_TAG) {
                tagName = parser.name

                if (tagName == MENU_TAG) {
                    // Go to next tag
                    eventType = parser.next()
                    break
                }
                throw RuntimeException("Expecting menu, got $tagName")
            }
            eventType = parser.next()
        } while (eventType != XmlPullParser.END_DOCUMENT)

        var reachedEndOfMenu = false
        while (!reachedEndOfMenu) {
            tagName = parser.name

            if (eventType == XmlPullParser.END_TAG) {
                if (tagName == MENU_TAG) {
                    reachedEndOfMenu = true
                }
            }

            if (eventType == XmlPullParser.START_TAG) {
                when (tagName) {
                    MENU_ITEM_TAG -> items.add(readBottomActionMenuItem(parser, attrs))
                }
            }

            eventType = parser.next()
        }

        return items
    }

    private fun readBottomActionMenuItem(parser: XmlPullParser, attrs: AttributeSet): BottomActionMenuItem {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BottomActionMenuItem)
        val id = typedArray.getResourceId(R.styleable.BottomActionMenuItem_android_id, View.NO_ID)
        val text = typedArray.getString(R.styleable.BottomActionMenuItem_android_title) ?: NO_TEXT
        val iconId = typedArray.getResourceId(R.styleable.BottomActionMenuItem_android_icon, View.NO_ID)
        val showAsAction = typedArray.getInt(R.styleable.BottomActionMenuItem_showAsAction, -1)
        val visible = typedArray.getBoolean(R.styleable.BottomActionMenuItem_android_visible, true)
        typedArray.recycle()
        parser.require(XmlPullParser.START_TAG, null, MENU_ITEM_TAG)
        return BottomActionMenuItem(
            id,
            text,
            iconId,
            showAsAction == MenuItem.SHOW_AS_ACTION_ALWAYS || showAsAction == MenuItem.SHOW_AS_ACTION_IF_ROOM,
            visible
        )
    }
}
