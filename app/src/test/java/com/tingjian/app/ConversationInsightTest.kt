package com.tingjian.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationInsightTest {
    @Test
    fun localInsightExtractsTasksAndTone() {
        val record = Conversation(
            title = "项目会议",
            time = "今天",
            preview = "好的",
            duration = "2 分钟",
            transcript = listOf(
                "对方" to "请明天提交项目报告",
                "我" to "好的，我会完成项目报告，谢谢"
            )
        )

        val insight = localConversationInsight(record)

        assertTrue(insight.summary.contains("2 条"))
        assertFalse(insight.highlights.isEmpty())
        assertFalse(insight.actionItems.isEmpty())
        assertFalse(insight.keywords.isEmpty())
        assertEquals("积极", insight.tone)
    }
}
