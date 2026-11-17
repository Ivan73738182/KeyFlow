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
            else -> "https://api.openai.com/v1/chat/completions"
        }
    }

    fun sendMessage(provider: String, model: String, apiKey: String, prompt: String, callback: (String?, String?) -> Unit) {
        val url = getBaseUrl(provider)

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

        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)

        // Добавляем заголовки для OpenRouter, чтобы избежать блокировок
        if (provider == "OpenRouter") {
            requestBuilder.addHeader("HTTP-Referer", "https://github.com/Ivan73738182/KeyFlow")
            requestBuilder.addHeader("X-Title", "KeyFlow")
        }

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, "Ошибка сети: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        callback(null, "Ошибка API: ${response.code} ${response.message}\n$errorBody")
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
