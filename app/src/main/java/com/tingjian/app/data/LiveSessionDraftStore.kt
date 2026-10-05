package com.tingjian.app.data

import android.content.Context
import com.google.gson.Gson
import com.tingjian.app.ChatLine

internal data class LiveSessionDraft(
    val ownerKey: String,
    val startedAt: Long,
    val scene: String,
    val serverId: String?,
    val lines: List<ChatLine>,
    val lastSequence: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

internal fun draftOwnerKey(email: String?): String =
    email?.trim()?.lowercase()?.ifBlank { LOCAL_OWNER } ?: LOCAL_OWNER

internal fun encodeLiveSessionDraft(draft: LiveSessionDraft): String {
    return draftGson.toJson(draft)
}

internal fun decodeLiveSessionDraft(value: String): LiveSessionDraft? = runCatching {
    draftGson.fromJson(value, LiveSessionDraft::class.java)
        ?.takeIf { draft ->
            draft.ownerKey.isNotBlank() && draft.startedAt > 0L
        }
        ?.let { draft ->
            draft.copy(
                scene = draft.scene.ifBlank { "日常" },
                serverId = draft.serverId?.ifBlank { null },
                lines = draft.lines.filter { line -> line.content.isNotBlank() },
                lastSequence = draft.lastSequence.coerceAtLeast(0L),
                updatedAt = if (draft.updatedAt > 0L) {
                    draft.updatedAt
                } else {
                    draft.startedAt
                }
            )
        }
}.getOrNull()

internal class LiveSessionDraftStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "tingjian_live_session", Context.MODE_PRIVATE
    )

    @Synchronized
    fun load(ownerKey: String): LiveSessionDraft? =
        decodeLiveSessionDraft(preferences.getString(KEY_DRAFT, "").orEmpty())
            ?.takeIf { it.ownerKey == ownerKey }

    @Synchronized
    fun save(draft: LiveSessionDraft) {
        preferences.edit().putString(KEY_DRAFT, encodeLiveSessionDraft(draft)).apply()
    }

    @Synchronized
    fun clear(ownerKey: String) {
        val current = decodeLiveSessionDraft(
            preferences.getString(KEY_DRAFT, "").orEmpty()
        )
        if (current == null || current.ownerKey == ownerKey) {
            preferences.edit().remove(KEY_DRAFT).apply()
        }
    }

    private companion object {
        const val KEY_DRAFT = "draft"
    }
}

private const val LOCAL_OWNER = "__local__"
private val draftGson = Gson()
