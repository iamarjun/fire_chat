package com.arjun.firechat.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.arjun.firechat.R
import com.arjun.firechat.model.Message

class ChatAdapter(private val currentUserId: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<Message>() {

        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }

    }
    private val differ = AsyncListDiffer(this, diffCallback)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        when (viewType) {

            R.layout.message_send -> {
                return MessageSendViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.message_send,
                        parent,
                        false
                    )
                )
            }

            R.layout.message_receive -> {

                return MessageReceiveViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.message_receive,
                        parent,
                        false
                    )
                )
            }

            else -> {
                throw IllegalStateException()
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MessageSendViewHolder -> {
                holder.bind(differ.currentList[position])
            }

            is MessageReceiveViewHolder -> {
                holder.bind(differ.currentList[position])
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = differ.currentList[position]

        return if (item.from == currentUserId)
            R.layout.message_send
        else
            R.layout.message_receive
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun submitList(list: List<Message>) {
        differ.submitList(list)
    }

}

