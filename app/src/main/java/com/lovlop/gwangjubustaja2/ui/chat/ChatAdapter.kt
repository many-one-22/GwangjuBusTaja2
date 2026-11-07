package com.lovlop.gwangjubustaja2.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lovlop.gwangjubustaja2.R
import com.lovlop.gwangjubustaja2.data.model.ChatMessage

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position]) {
            is ChatMessage.User -> TYPE_USER
            is ChatMessage.Bot, is ChatMessage.Loading -> TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            UserViewHolder(inflater.inflate(R.layout.item_chat_user, parent, false))
        } else {
            BotViewHolder(inflater.inflate(R.layout.item_chat_bot, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val msg = messages[position]) {
            is ChatMessage.User -> (holder as UserViewHolder).bind(msg)
            is ChatMessage.Bot -> (holder as BotViewHolder).bind(msg)
            is ChatMessage.Loading -> (holder as BotViewHolder).bindLoading()
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun removeLastIfLoading() {
        if (messages.isNotEmpty() && messages.last() is ChatMessage.Loading) {
            val lastIdx = messages.size - 1
            messages.removeAt(lastIdx)
            notifyItemRemoved(lastIdx)
        }
    }

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)

        fun bind(item: ChatMessage.User) {
            tvMessage.text = item.text
            tvTime.text = item.timestamp
        }
    }

    class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)

        fun bind(item: ChatMessage.Bot) {
            tvMessage.text = item.text
            tvTime.text = item.timestamp
        }

        fun bindLoading() {
            tvMessage.text = "도착 정보를 조회 중입니다..."
            tvTime.text = ""
        }
    }
}