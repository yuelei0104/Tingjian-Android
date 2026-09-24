package com.tingjian.app.data

import com.tingjian.app.network.HistoryItemResponse
import com.tingjian.app.network.HomeOverviewResponse
import com.tingjian.app.network.HomePlanResponse
import com.tingjian.app.network.HomeResponse
import com.tingjian.app.network.HomeSceneResponse
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
