package com.ivangames.keyflow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.button.MaterialButton
import java.util.UUID

class ChatActivity : AppCompatActivity() {
    private var messagesList = mutableListOf<Message>()
    private lateinit var adapter: ChatAdapter
    private lateinit var rvMessages: RecyclerView
    private lateinit var apiKey: String
    private lateinit var provider: String
    private var selectedModel: String = "openai/gpt-4o-mini"
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private lateinit var currentChatId: String
    private val allChats = mutableMapOf<String, MutableList<Message>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        rvMessages = findViewById(R.id.rvMessages)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)
        val btnModelSelect = findViewById<MaterialButton>(R.id.btnModelSelect)
        val btnNewChat = findViewById<MaterialButton>(R.id.btnNewChat)

        loadSettings()
        loadHistory()

        adapter = ChatAdapter(messagesList)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        // Если чатов нет, создаем новый
        if (allChats.isEmpty()) {
            createNewChat()
        } else {
            currentChatId = allChats.keys.first()
            messagesList = allChats[currentChatId] ?: mutableListOf()
            adapter = ChatAdapter(messagesList)
            rvMessages.adapter = adapter
        }

        btnNewChat.setOnClickListener {
            createNewChat()
        }

        btnModelSelect.setOnClickListener {
            showModelSelectionDialog(btnModelSelect)
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                messagesList.add(Message(text, true))
                adapter.notifyItemInserted(messagesList.size - 1)
                etMessage.text.clear()
                scrollToBottom()
                
                // Сохраняем сообщение пользователя
                HistoryManager.saveChat(this, currentChatId, messagesList)

                btnSend.isEnabled = false
                messagesList.add(Message("⏳ Печатает...", false))
                adapter.notifyItemInserted(messagesList.size - 1)
                scrollToBottom()

                ApiClient.sendMessage(provider, selectedModel, apiKey, text) { response, error ->
                    mainHandler.post {
                        val lastIndex = messagesList.size - 1
                        if (lastIndex >= 0) {
                            messagesList.removeAt(lastIndex)
                            adapter.notifyItemRemoved(lastIndex)
                        }

                        if (error != null) {
                            messagesList.add(Message("⚠️ $error", false))
                        } else {
                            messagesList.add(Message(response ?: "Пустой ответ", false))
                        }
                        adapter.notifyItemInserted(messagesList.size - 1)
                        scrollToBottom()
                        btnSend.isEnabled = true
                        
                        // Сохраняем ответ ИИ
                        HistoryManager.saveChat(this@ChatActivity, currentChatId, messagesList)
                    }
                }
            }
        }
    }

    private fun createNewChat() {
        currentChatId = UUID.randomUUID().toString()
        messagesList = mutableListOf()
        messagesList.add(Message("Привет! Я KeyFlow ($provider). Модель: $selectedModel", false))
        
        adapter = ChatAdapter(messagesList)
        rvMessages.adapter = adapter
        
        HistoryManager.saveChat(this, currentChatId, messagesList)
        allChats[currentChatId] = messagesList
    }

    private fun loadHistory() {
        val loaded = HistoryManager.loadAllChats(this)
        allChats.putAll(loaded)
    }

    private fun showModelSelectionDialog(btnModelSelect: MaterialButton) {
        val models = if (provider == "OpenRouter") {
            arrayOf(
                "openai/gpt-4o-mini",
                "anthropic/claude-3.5-sonnet",
                "meta-llama/llama-3-8b-instruct:free",
                "google/gemini-flash-1.5"
            )
        } else if (provider == "DeepSeek") {
            arrayOf("deepseek-chat", "deepseek-coder")
        } else {
            arrayOf("gpt-4o-mini", "gpt-4o")
        }

        // ИСПРАВЛЕНИЕ: Убрали R.style.Theme_Material3_Dark_Dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите модель")
        builder.setItems(models) { dialog, which ->
            selectedModel = models[which]
            btnModelSelect.text = selectedModel.split("/").last() + " ▾"
            Toast.makeText(this, "Модель: $selectedModel", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        builder.show()
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
