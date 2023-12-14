package org.fossify.gallery.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fossify.gallery.R
import org.fossify.gallery.databinding.EditorFilterItemBinding
import org.fossify.gallery.models.FilterItem

class FiltersAdapter(val context: Context, val filterItems: ArrayList<FilterItem>, val itemClick: (Int) -> Unit) :
    RecyclerView.Adapter<FiltersAdapter.ViewHolder>() {

    private var currentSelection = filterItems.first()
    private var strokeBackground = context.resources.getDrawable(R.drawable.stroke_background)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(filterItems[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = EditorFilterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
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
        fun bindView(filterItem: FilterItem): View {
            binding.apply {
                editorFilterItemLabel.text = filterItem.filter.name
                editorFilterItemThumbnail.setImageBitmap(filterItem.bitmap)
                editorFilterItemThumbnail.background = if (getCurrentFilter() == filterItem) {
                    strokeBackground
                } else {
                    null
                }

                root.setOnClickListener {
                    setCurrentFilter(adapterPosition)
                }
            }
            return itemView
        }
    }
}
