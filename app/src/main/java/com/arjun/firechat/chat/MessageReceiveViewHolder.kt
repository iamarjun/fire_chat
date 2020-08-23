package com.arjun.firechat.chat

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.arjun.firechat.model.Message
import kotlinx.android.synthetic.main.message_receive.view.*

class MessageReceiveViewHolder
constructor(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    private val message = itemView.message_receiver

    fun bind(item: Message) {

        message.text = item.message
    }
}