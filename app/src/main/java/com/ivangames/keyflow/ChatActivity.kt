package com.ivangames.keyflow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ChatActivity : AppCompatActivity() {
    private val messagesList = mutableListOf<Message>()
    private lateinit var adapter: ChatAdapter
    private lateinit var rvMessages: RecyclerView
    private lateinit var apiKey: String
    private lateinit var provider: String
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        rvMessages = findViewById(R.id.rvMessages)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)

        loadSettings()

        adapter = ChatAdapter(messagesList)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        messagesList.add(Message("Привет! Я KeyFlow ($provider). Чем могу помочь?", false))
        adapter.notifyItemInserted(messagesList.size - 1)

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                // Добавляем сообщение пользователя
                messagesList.add(Message(text, true))
                adapter.notifyItemInserted(messagesList.size - 1)
                etMessage.text.clear()
                scrollToBottom()

                // Показываем "Печатает..."
                btnSend.isEnabled = false
                messagesList.add(Message("⏳ Печатает...", false))
                adapter.notifyItemInserted(messagesList.size - 1)
                scrollToBottom()

                ApiClient.sendMessage(provider, apiKey, text) { response, error ->
                    mainHandler.post {
                        // Удаляем "Печатает..."
                        val lastIndex = messagesList.size - 1
                        if (lastIndex >= 0) {
                            messagesList.removeAt(lastIndex)
                            adapter.notifyItemRemoved(lastIndex)
                        }

                        // Добавляем ответ или ошибку
                        if (error != null) {
                            messagesList.add(Message("⚠️ $error", false))
                        } else {
                            messagesList.add(Message(response ?: "Пустой ответ", false))
                        }
                        adapter.notifyItemInserted(messagesList.size - 1)
                        scrollToBottom()
                        btnSend.isEnabled = true
                    }
                }
            }
        }
    }

    private fun scrollToBottom() {
        rvMessages.post {
            if (messagesList.isNotEmpty()) {
                rvMessages.smoothScrollToPosition(messagesList.size - 1)
            }
        }
    }

    private fun loadSettings() {
        val masterKey = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs = EncryptedSharedPreferences.create(this, "keyflow_secure_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        
        apiKey = prefs.getString("api_key", "") ?: ""
        provider = prefs.getString("provider", "DeepSeek") ?: "DeepSeek"
        
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Ключ не найден. Вернитесь в настройки.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
