package com.tingjian.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class PendingRealtimeMessage(
    val ownerEmail: String,
    val sessionId: String,
    val clientMessageId: String,
    val speaker: String,
    val content: String
)

internal class PendingMessageStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "tingjian_realtime_outbox", Context.MODE_PRIVATE
    )

    @Synchronized
    fun all(ownerEmail: String): List<PendingRealtimeMessage> =
        read().filter { it.ownerEmail == ownerEmail }

    @Synchronized
    fun enqueue(message: PendingRealtimeMessage) {
        val messages = read().filterNot {
            it.ownerEmail == message.ownerEmail &&
                it.clientMessageId == message.clientMessageId
        } + message
        write(messages)
    }

    @Synchronized
    fun remove(ownerEmail: String, clientMessageId: String) {
        write(read().filterNot {
            it.ownerEmail == ownerEmail && it.clientMessageId == clientMessageId
        })
    }

    @Synchronized
    fun removeSession(sessionId: String) {
        write(read().filterNot { it.sessionId == sessionId })
    }

    @Synchronized
    fun clear(ownerEmail: String) {
        write(read().filterNot { it.ownerEmail == ownerEmail })
    }

    private fun read(): List<PendingRealtimeMessage> = runCatching {
        val array = JSONArray(preferences.getString(KEY_MESSAGES, "[]"))
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val sessionId = item.optString("sessionId")
            val clientId = item.optString("clientMessageId")
            val content = item.optString("content")
            val ownerEmail = item.optString("ownerEmail")
            if (ownerEmail.isBlank() || sessionId.isBlank() ||
                clientId.isBlank() || content.isBlank()) null
            else PendingRealtimeMessage(
                ownerEmail, sessionId, clientId,
                item.optString("speaker", "OTHER"), content
            )
        }
    }.getOrDefault(emptyList())

    private fun write(messages: List<PendingRealtimeMessage>) {
        val array = JSONArray()
        messages.forEach { message ->
            array.put(JSONObject()
                .put("ownerEmail", message.ownerEmail)
                .put("sessionId", message.sessionId)
                .put("clientMessageId", message.clientMessageId)
                .put("speaker", message.speaker)
                .put("content", message.content))
        }
        preferences.edit().putString(KEY_MESSAGES, array.toString()).apply()
    }

    private companion object {
        const val KEY_MESSAGES = "messages"
    }
}
