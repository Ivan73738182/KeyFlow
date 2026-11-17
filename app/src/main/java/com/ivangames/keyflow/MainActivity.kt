package com.ivangames.keyflow

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val actProvider = findViewById<AutoCompleteTextView>(R.id.actProvider)
        val etApiKey = findViewById<TextInputEditText>(R.id.etApiKey)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)

        val providers = arrayOf("DeepSeek", "OpenAI", "OpenRouter", "Anthropic (Claude)", "Другой")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, providers)
        actProvider.setAdapter(adapter)
        
        // Если ключ уже есть, подставляем провайдера
        val savedProvider = getSavedSettings().first
        if (savedProvider != null) {
            actProvider.setText(savedProvider, false)
        } else {
            actProvider.setText("DeepSeek", false)
        }

        btnSave.setOnClickListener {
            val apiKey = etApiKey.text.toString().trim()
            val provider = actProvider.text.toString()

            if (apiKey.isEmpty()) {
                Toast.makeText(this, "Введите API ключ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            saveSettings(apiKey, provider)
            Toast.makeText(this, "Ключ ($provider) сохранен!", Toast.LENGTH_SHORT).show()
            
            // Переход в чат
            startActivity(Intent(this, ChatActivity::class.java))
            finish()
        }
    }

    private fun getSavedSettings(): Pair<String?, String?> {
        val masterKey = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs = EncryptedSharedPreferences.create(this, "keyflow_secure_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        return Pair(prefs.getString("provider", null), prefs.getString("api_key", null))
    }

    private fun saveSettings(apiKey: String, provider: String) {
        val masterKey = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs = EncryptedSharedPreferences.create(this, "keyflow_secure_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        prefs.edit().putString("api_key", apiKey).putString("provider", provider).apply()
    }
}
