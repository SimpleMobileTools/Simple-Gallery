package com.simplemobiletools.gallery.pro.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.databinding.EditorFilterItemBinding
import com.simplemobiletools.gallery.pro.models.FilterItem
import java.util.*

class FiltersAdapter(
    val context: Context,
    private val filterItems: ArrayList<FilterItem>,
    private val itemClick: (Int) -> Unit
) : RecyclerView.Adapter<FiltersAdapter.ViewHolder>() {
    private var currentSelection = filterItems.first()
    private var strokeBackground = context.resources.getDrawable(R.drawable.stroke_background)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(filterItems[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(EditorFilterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount() = filterItems.size

    fun getCurrentFilter() = currentSelection

    private fun setCurrentFilter(position: Int) {
        val filterItem = filterItems.getOrNull(position) ?: return
        if (currentSelection != filterItem) {
            currentSelection = filterItem
            notifyDataSetChanged()
            itemClick.invoke(position)
        }
    }

    inner class ViewHolder(private val binding: EditorFilterItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindView(filterItem: FilterItem) {
            binding.editorFilterItemLabel.text = filterItem.filter.name
            binding.editorFilterItemThumbnail.setImageBitmap(filterItem.bitmap)
            binding.editorFilterItemThumbnail.background = if (getCurrentFilter() == filterItem) strokeBackground else null
            binding.root.setOnClickListener {
                setCurrentFilter(adapterPosition)
            }
        }
    }
}
