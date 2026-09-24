package com.tingjian.app.data

import com.tingjian.app.Conversation
import com.tingjian.app.network.HistoryItemResponse
import com.tingjian.app.network.SessionDetailResponse

internal fun HistoryItemResponse.toConversation(): Conversation = Conversation(
    title = title,
    time = formatServerTime(startedAt),
    preview = preview.orEmpty(),
    duration = if (status == "ACTIVE") "进行中" else "$messageCount 条消息",
    transcript = emptyList(),
    id = stableLocalId(id),
    scene = inferScene(title),
    serverId = id
)

internal fun SessionDetailResponse.toConversation(): Conversation = Conversation(
    title = session.title,
    time = formatServerTime(session.startedAt),
    preview = messages.lastOrNull()?.content.orEmpty(),
    duration = "${messages.size} 条消息",
    transcript = messages.map {
        (if (it.speaker == "SELF") "我" else "对方") to it.content
    },
    id = stableLocalId(session.id),
    scene = inferScene(session.title),
    serverId = session.id
)

private fun stableLocalId(serverId: String): Long =
    serverId.fold(1125899906842597L) { value, character -> value * 31 + character.code }

private fun formatServerTime(value: String): String {
    if (value.length < 16) return value
    return "${value.substring(5, 7).toIntOrNull() ?: value.substring(5, 7)}月" +
        "${value.substring(8, 10).toIntOrNull() ?: value.substring(8, 10)}日 " +
        value.substring(11, 16)
}

private fun inferScene(title: String): String = when {
    title.contains("课堂") -> "课堂"
    title.contains("会议") -> "会议"
    title.contains("就医") -> "就医"
    else -> "日常"
}
