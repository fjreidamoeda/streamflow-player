package com.streamflow.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class ContentAdapter(private val onClick: (ContentItem) -> Unit) :
    ListAdapter<ContentItem, ContentAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivLogo: ImageView = itemView.findViewById(R.id.ivChannelLogo)
        private val tvName: TextView = itemView.findViewById(R.id.tvChannelName)
        private val tvGroup: TextView = itemView.findViewById(R.id.tvChannelGroup)

        fun bind(item: ContentItem, onClick: (ContentItem) -> Unit) {
            tvName.text = item.name
            tvGroup.text = when (item) {
                is ContentItem.Live -> "ID: ${item.stream.streamId}"
                is ContentItem.Movie -> "Filme"
                is ContentItem.Series -> "Serie"
                is ContentItem.Episode -> item.ep.seriesName
            }
            if (item.icon.isNotBlank()) {
                Picasso.get().load(item.icon).placeholder(R.drawable.ic_tv).into(ivLogo)
            } else {
                ivLogo.setImageResource(R.drawable.ic_tv)
            }
            itemView.setOnClickListener { onClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ContentItem>() {
        override fun areItemsTheSame(old: ContentItem, new: ContentItem): Boolean {
            return when {
                old is ContentItem.Live && new is ContentItem.Live -> old.stream.streamId == new.stream.streamId
                old is ContentItem.Movie && new is ContentItem.Movie -> old.vod.streamId == new.vod.streamId
                old is ContentItem.Series && new is ContentItem.Series -> old.series.seriesId == new.series.seriesId
                old is ContentItem.Episode && new is ContentItem.Episode -> old.ep.episode.id == new.ep.episode.id
                else -> false
            }
        }
        override fun areContentsTheSame(old: ContentItem, new: ContentItem) = old == new
    }
}
