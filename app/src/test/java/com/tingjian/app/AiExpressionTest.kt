package com.tingjian.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiExpressionTest {
    @Test
    fun contextKeepsLastEightMessagesAndClipsLongContent() {
        val lines = (0 until 10).map { index ->
            ChatLine("message-$index-" + "a".repeat(300), index % 2 == 0)
        }

        val context = buildAiContext(lines)

        assertEquals(8, context.size)
        assertTrue(context.first().content.startsWith("message-2"))
        assertEquals(240, context.last().content.length)
        assertEquals("OTHER", context.last().speaker)
    }

    @Test
    fun localFallbackCreatesReplyWithoutSendingAnything() {
        val context = buildAiContext(listOf(ChatLine("明天可以提交吗？", false)))

        val suggestion = localExpressionSuggestion("", AiAction.REPLY, context)

        assertTrue(suggestion.contains("回复"))
    }

    @Test
    fun politeFallbackUsesRespectfulPronoun() {
        assertEquals(
            "您稍等 谢谢。",
            localExpressionSuggestion("你稍等", AiAction.POLITE, emptyList())
        )
    }
}
