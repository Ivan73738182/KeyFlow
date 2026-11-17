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
    private lateinit var apiKey: String
    private lateinit var provider: String
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)

        loadSettings()

        adapter = ChatAdapter(messagesList)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        messagesList.add(Message("Привет! Я KeyFlow ($provider). Чем могу помочь?", false))
        adapter.notifyDataSetChanged()

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                messagesList.add(Message(text, true))
                adapter.notifyDataSetChanged()
                etMessage.text.clear()
                rvMessages.scrollToPosition(messagesList.size - 1)

                // Блокируем кнопку и показываем, что идет загрузка
                btnSend.isEnabled = false
                messagesList.add(Message("Печатает...", false))
                adapter.notifyDataSetChanged()
                rvMessages.scrollToPosition(messagesList.size - 1)

                ApiClient.sendMessage(provider, apiKey, text) { response, error ->
                    mainHandler.post {
                        messagesList.removeAt(messagesList.size - 1) // Удаляем "Печатает..."
                        if (error != null) {
                            messagesList.add(Message("⚠️ $error", false))
                        } else {
                            messagesList.add(Message(response ?: "Пустой ответ", false))
                        }
                        adapter.notifyDataSetChanged()
                        rvMessages.scrollToPosition(messagesList.size - 1)
                        btnSend.isEnabled = true
                    }
                }
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
