package com.tingjian.app.data

import com.tingjian.app.Conversation
import com.tingjian.app.network.HistoryItemResponse
import com.tingjian.app.network.HomeOverviewResponse
import com.tingjian.app.network.HomePlanResponse
import com.tingjian.app.network.HomeResponse
import com.tingjian.app.network.HomeSceneResponse
import com.tingjian.app.network.SessionDetailResponse
import com.tingjian.app.network.SessionMessageResponse
import com.tingjian.app.network.SessionResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteMappersTest {
    @Test
    fun homeResponseMapsDashboardContent() {
        val recent = historyItem("session-1", "课堂 · 会话", "2026-09-24T10:30:00")
        val response = HomeResponse(
            overview = HomeOverviewResponse(3, 12, 600),
            recentConversations = listOf(recent),
            scenes = listOf(HomeSceneResponse("CLASS", "课堂")),
            plan = HomePlanResponse("BETA", "内测版", "暂未开放购买", false)
        )

        val dashboard = response.toDashboard()

        assertEquals(3L, dashboard.conversationCount)
        assertEquals(12L, dashboard.messageCount)
        assertEquals(600L, dashboard.totalDurationSeconds)
        assertEquals(listOf("课堂"), dashboard.scenes)
        assertEquals("session-1", dashboard.recentConversations.single().serverId)
        assertEquals("内测版", dashboard.planName)
    }

    @Test
    fun historyItemFromTodayUsesTodayLabel() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())

        val conversation = historyItem(
            id = "session-today",
            title = "面对面会话",
            startedAt = "${today}T08:05:00"
        ).toConversation()

        assertTrue(conversation.time.startsWith("今天 08:05"))
    }

    @Test
    fun resumeIndexAcceptsMatchingCloudPrefix() {
        val local = localConversation(listOf("对方" to "你好", "我" to "你好呀"))
        val detail = sessionDetail(listOf("OTHER" to "你好"))

        assertEquals(1, detail.resumeIndexFor(local))
    }

    @Test
    fun resumeIndexRejectsDivergedCloudMessages() {
        val local = localConversation(listOf("对方" to "你好"))
        val detail = sessionDetail(listOf("OTHER" to "不同内容"))

        assertEquals(null, detail.resumeIndexFor(local))
    }

    private fun localConversation(transcript: List<Pair<String, String>>) = Conversation(
        title = "测试会话",
        time = "今天 08:05",
        preview = transcript.last().second,
        duration = "1 分钟",
        transcript = transcript,
        id = 1L,
        syncPending = true
    )

    private fun sessionDetail(messages: List<Pair<String, String>>) = SessionDetailResponse(
        session = SessionResponse("session-1", "测试会话", "ACTIVE",
            "2026-09-24T08:05:00", null),
        messages = messages.mapIndexed { index, (speaker, content) ->
            SessionMessageResponse("message-$index", speaker, content,
                "2026-09-24T08:05:0$index")
        }
    )

    private fun historyItem(id: String, title: String, startedAt: String) =
        HistoryItemResponse(
            id = id,
            title = title,
            status = "ENDED",
            startedAt = startedAt,
            endedAt = startedAt,
            messageCount = 4,
            preview = "测试内容"
        )
}
