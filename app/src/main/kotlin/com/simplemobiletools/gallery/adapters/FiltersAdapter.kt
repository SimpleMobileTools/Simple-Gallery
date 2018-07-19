package com.simplemobiletools.gallery.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.models.FilterItem
import kotlinx.android.synthetic.main.editor_filter_item.view.*
import java.util.*

class FiltersAdapter(val filterItems: ArrayList<FilterItem>, val itemClick: (FilterItem) -> Unit) : RecyclerView.Adapter<FiltersAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(filterItems[position], itemClick)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.editor_filter_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = filterItems.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(filterItem: FilterItem, itemClick: (FilterItem) -> Unit): View {
            itemView.apply {
                editor_filter_item_label.text = filterItem.filter.name
                editor_filter_item_thumbnail.setImageBitmap(filterItem.bitmap)

                setOnClickListener {
                    itemClick.invoke(filterItem)
                }
            }
            return itemView
        }
    }
}
