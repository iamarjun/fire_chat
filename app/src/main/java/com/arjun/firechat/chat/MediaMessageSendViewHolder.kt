package com.arjun.firechat.chat

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.arjun.firechat.GlideApp
import com.arjun.firechat.R
import com.arjun.firechat.model.Message
import kotlinx.android.synthetic.main.media_message_send.view.*
import kotlinx.android.synthetic.main.message_send.view.message_time

class MediaMessageSendViewHolder
constructor(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    private val media = itemView.media
    private val time = itemView.message_time


    fun bind(item: Message) {

        GlideApp.with(itemView)
            .load(item.mediaUrl)
            .centerCrop()
            .placeholder(R.drawable.placeholder)
            .into(media)

        time.text = item.getTime()
    }
}