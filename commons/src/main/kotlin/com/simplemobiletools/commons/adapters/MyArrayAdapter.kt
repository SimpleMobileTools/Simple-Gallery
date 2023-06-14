package com.simplemobiletools.commons.adapters

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class MyArrayAdapter<T>(context: Context, res: Int, items: Array<T>, val textColor: Int, val backgroundColor: Int, val padding: Int) :
        ArrayAdapter<T>(context, res, items) {
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)

        view.findViewById<TextView>(android.R.id.text1).apply {
            setTextColor(textColor)
            setPadding(padding, padding, padding, padding)
            background = ColorDrawable(backgroundColor)
        }

        return view
    }
}
