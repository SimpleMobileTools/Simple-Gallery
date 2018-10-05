package com.simplemobiletools.gallery.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.interfaces.FilterAdapterListener
import com.simplemobiletools.gallery.models.FilterItem
import kotlinx.android.synthetic.main.editor_filter_item.view.*
import java.util.*

class FiltersAdapter(val context: Context, val filterItems: ArrayList<FilterItem>, val itemClick: (Int) -> Unit) : RecyclerView.Adapter<FiltersAdapter.ViewHolder>(),
        FilterAdapterListener {

    private var currentSelection = filterItems.first()
    private var strokeBackground = context.resources.getDrawable(R.drawable.stroke_background)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(filterItems[position], strokeBackground)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.editor_filter_item, parent, false)
        return ViewHolder(view, this)
    }

    override fun getItemCount() = filterItems.size

    override fun getCurrentFilter() = currentSelection

    override fun setCurrentFilter(position: Int) {
        val filterItem = filterItems.getOrNull(position) ?: return
        if (currentSelection != filterItem) {
            currentSelection = filterItem
            notifyDataSetChanged()
            itemClick.invoke(position)
        }
    }

    class ViewHolder(view: View, val filterAdapterListener: FilterAdapterListener) : RecyclerView.ViewHolder(view) {
        fun bindView(filterItem: FilterItem, strokeBackground: Drawable): View {
            itemView.apply {
                editor_filter_item_label.text = filterItem.filter.name
                editor_filter_item_thumbnail.setImageBitmap(filterItem.bitmap)
                editor_filter_item_thumbnail.background = if (filterAdapterListener.getCurrentFilter() == filterItem) {
                    strokeBackground
                } else {
                    null
                }

                setOnClickListener {
                    filterAdapterListener.setCurrentFilter(adapterPosition)
                }
            }
            return itemView
        }
    }
}
