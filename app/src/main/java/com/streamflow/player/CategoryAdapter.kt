package com.streamflow.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val selectedCategoryId: String?,
    private val onClick: (XtreamCategory) -> Unit
) : ListAdapter<XtreamCategory, CategoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), selectedCategoryId, onClick)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvCategoryName)

        fun bind(category: XtreamCategory, selectedId: String?, onClick: (XtreamCategory) -> Unit) {
            tvName.text = category.categoryName
            tvName.isSelected = category.categoryId == selectedId
            if (category.categoryId == selectedId) {
                itemView.setBackgroundColor(0x330f3460.toInt())
            } else {
                itemView.setBackgroundColor(0x00000000.toInt())
            }
            itemView.setOnClickListener { onClick(category) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<XtreamCategory>() {
        override fun areItemsTheSame(old: XtreamCategory, new: XtreamCategory) = old.categoryId == new.categoryId
        override fun areContentsTheSame(old: XtreamCategory, new: XtreamCategory) = old == new
    }
}
