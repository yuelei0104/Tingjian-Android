package com.tingjian.app.data

import com.tingjian.app.ChatLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveSessionDraftStoreTest {
    @Test
    fun draftRoundTripKeepsChineseTranscriptAndRecoveryCursor() {
        val draft = LiveSessionDraft(
            ownerKey = "demo@example.com",
            startedAt = 1_700_000_000_000L,
            scene = "课堂",
            serverId = "session-1",
            lines = listOf(
                ChatLine("请继续讲解。", true),
                ChatLine("好的。", false)
            ),
            lastSequence = 9L,
            updatedAt = 1_700_000_001_000L
        )

        val restored = decodeLiveSessionDraft(encodeLiveSessionDraft(draft))

        assertEquals(draft, restored)
    }

    @Test
    fun invalidDraftIsIgnored() {
        assertNull(decodeLiveSessionDraft("{}"))
        assertNull(decodeLiveSessionDraft("not-json"))
    }

    @Test
    fun ownerKeyIsStableAndSeparatesLocalMode() {
        assertEquals("demo@example.com", draftOwnerKey(" Demo@Example.com "))
        assertEquals("__local__", draftOwnerKey(null))
        assertEquals("__local__", draftOwnerKey(""))
    }
}
