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

class ChannelAdapter(private val onClick: (XtreamStream) -> Unit) :
    ListAdapter<XtreamStream, ChannelAdapter.ViewHolder>(DiffCallback()) {

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

        fun bind(stream: XtreamStream, onClick: (XtreamStream) -> Unit) {
            tvName.text = stream.name
            tvGroup.text = "ID: ${stream.streamId}"
            if (stream.streamIcon.isNotBlank()) {
                Picasso.get().load(stream.streamIcon).placeholder(R.drawable.ic_tv).into(ivLogo)
            } else {
                ivLogo.setImageResource(R.drawable.ic_tv)
            }
            itemView.setOnClickListener { onClick(stream) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<XtreamStream>() {
        override fun areItemsTheSame(old: XtreamStream, new: XtreamStream) = old.streamId == new.streamId
        override fun areContentsTheSame(old: XtreamStream, new: XtreamStream) = old == new
    }
}
