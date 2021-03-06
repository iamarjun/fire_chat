package com.arjun.firechat.chat

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.arjun.firechat.model.Message
import kotlinx.android.synthetic.main.message_send.view.*

class MessageSendViewHolder
constructor(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    private val message = itemView.message_sender
    private val time = itemView.message_time


    fun bind(item: Message) {

        message.text = item.message
        time.text = item.getTime()
    }
}