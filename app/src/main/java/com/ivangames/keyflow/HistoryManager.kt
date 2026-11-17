package com.ivangames.keyflow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object HistoryManager {
    private const val FILE_NAME = "keyflow_history.json"

    // Получить список всех чатов (ID -> Список сообщений)
    fun loadAllChats(context: Context): MutableMap<String, MutableList<Message>> {
        val file = File(context.filesDir, FILE_NAME)
        val chats = mutableMapOf<String, MutableList<Message>>()
        if (!file.exists()) return chats

        try {
            val jsonStr = file.readText()
            val jsonObject = JSONObject(jsonStr)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val chatId = keys.next()
                val messagesArray = jsonObject.getJSONArray(chatId)
                val messagesList = mutableListOf<Message>()
                for (i in 0 until messagesArray.length()) {
                    val msgObj = messagesArray.getJSONObject(i)
                    messagesList.add(Message(msgObj.getString("text"), msgObj.getBoolean("isUser")))
                }
                chats[chatId] = messagesList
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return chats
    }

    // Сохранить конкретный чат
    fun saveChat(context: Context, chatId: String, messages: List<Message>) {
        val chats = loadAllChats(context)
        chats[chatId] = messages.toMutableList()

        val jsonObject = JSONObject()
        for ((id, msgList) in chats) {
            val messagesArray = JSONArray()
            for (msg in msgList) {
                messagesArray.put(JSONObject().apply {
                    put("text", msg.text)
                    put("isUser", msg.isUser)
                })
            }
            jsonObject.put(id, messagesArray)
        }

        File(context.filesDir, FILE_NAME).writeText(jsonObject.toString())
    }

    // Удалить чат
    fun deleteChat(context: Context, chatId: String) {
        val chats = loadAllChats(context)
        chats.remove(chatId)
        
        val jsonObject = JSONObject()
        for ((id, msgList) in chats) {
            val messagesArray = JSONArray()
            for (msg in msgList) {
                messagesArray.put(JSONObject().apply {
                    put("text", msg.text)
                    put("isUser", msg.isUser)
                })
            }
            jsonObject.put(id, messagesArray)
        }
        File(context.filesDir, FILE_NAME).writeText(jsonObject.toString())
    }
}
