package com.ivangames.keyflow

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object ApiClient {
    private val client = OkHttpClient()

    fun getBaseUrl(provider: String): String {
        return when (provider) {
            "OpenAI" -> "https://api.openai.com/v1/chat/completions"
            "DeepSeek" -> "https://api.deepseek.com/chat/completions"
            "OpenRouter" -> "https://openrouter.ai/api/v1/chat/completions"
            "Anthropic (Claude)" -> "https://api.anthropic.com/v1/messages" // Пока не поддерживается в этом формате
            else -> "https://api.openai.com/v1/chat/completions"
        }
    }

    fun getModel(provider: String): String {
        return when (provider) {
            "OpenAI" -> "gpt-4o-mini"
            "DeepSeek" -> "deepseek-chat"
            "OpenRouter" -> "openai/gpt-4o-mini" // Можно поменять на любую другую
            "Anthropic (Claude)" -> "claude-3-haiku-20240307"
            else -> "gpt-4o-mini"
        }
    }

    fun sendMessage(provider: String, apiKey: String, prompt: String, callback: (String?, String?) -> Unit) {
        val url = getBaseUrl(provider)
        val model = getModel(provider)

        // Собираем JSON запрос
        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val jsonBody = JSONObject().apply {
            put("model", model)
            put("messages", messagesArray)
        }.toString()

        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, "Ошибка сети: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        callback(null, "Ошибка API: ${response.code} ${response.message}")
                        return
                    }
                    val responseBody = response.body?.string()
                    try {
                        val jsonObject = JSONObject(responseBody ?: "")
                        val choices = jsonObject.getJSONArray("choices")
                        val message = choices.getJSONObject(0).getJSONObject("message")
                        val content = message.getString("content")
                        callback(content, null)
                    } catch (e: Exception) {
                        callback(null, "Ошибка парсинга: ${e.message}")
                    }
                }
            }
        })
    }
}
