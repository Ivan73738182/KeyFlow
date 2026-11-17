package com.ivangames.keyflow

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatActivity : AppCompatActivity() {
    private val messagesList = mutableListOf<Message>()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)

        adapter = ChatAdapter(messagesList)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        // Приветственное сообщение
        messagesList.add(Message("Привет! Я KeyFlow. Чем могу помочь?", false))
        adapter.notifyDataSetChanged()

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                messagesList.add(Message(text, true))
                adapter.notifyDataSetChanged()
                etMessage.text.clear()
                rvMessages.scrollToPosition(messagesList.size - 1)

                // Заглушка ответа ИИ для теста
                messagesList.add(Message("Это тестовый ответ. Скоро здесь будет нейросеть!", false))
                adapter.notifyDataSetChanged()
                rvMessages.scrollToPosition(messagesList.size - 1)
            }
        }
    }
}
