package com.ivangames.keyflow

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<Message>) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == 1) R.layout.item_message_user else R.layout.item_message_ai
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.tvMessage.text = message.text

        // Копирование по долгому нажатию (только для ИИ)
        if (!message.isUser) {
            holder.tvMessage.setOnLongClickListener {
                val clipboard = it.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("KeyFlow", message.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(it.context, "Скопировано!", Toast.LENGTH_SHORT).show()
                true
            }
        }
    }

    override fun getItemCount() = messages.size
}
