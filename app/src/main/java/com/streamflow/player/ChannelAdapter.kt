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

class ChannelAdapter(private val onClick: (M3UChannel) -> Unit) :
    ListAdapter<M3UChannel, ChannelAdapter.ViewHolder>(DiffCallback()) {

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

        fun bind(channel: M3UChannel, onClick: (M3UChannel) -> Unit) {
            tvName.text = channel.name
            tvGroup.text = channel.group
            if (channel.logo.isNotBlank()) {
                Picasso.get().load(channel.logo).placeholder(R.drawable.ic_tv).into(ivLogo)
            } else {
                ivLogo.setImageResource(R.drawable.ic_tv)
            }
            itemView.setOnClickListener { onClick(channel) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<M3UChannel>() {
        override fun areItemsTheSame(old: M3UChannel, new: M3UChannel) = old.url == new.url
        override fun areContentsTheSame(old: M3UChannel, new: M3UChannel) = old == new
    }
}
