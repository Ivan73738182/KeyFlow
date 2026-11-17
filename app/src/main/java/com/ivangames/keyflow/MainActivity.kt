package com.ivangames.keyflow

import android.content.Context
import android.os.Bundle
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

        val etApiKey = findViewById<TextInputEditText>(R.id.etApiKey)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)

        btnSave.setOnClickListener {
            val apiKey = etApiKey.text.toString().trim()
            if (apiKey.isEmpty()) {
                Toast.makeText(this, "Введите API ключ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            saveApiKey(apiKey)
            Toast.makeText(this, "Ключ сохранен безопасно! (Дальше будет чат)", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveApiKey(apiKey: String) {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "keyflow_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences.edit().putString("api_key", apiKey).apply()
    }
}
