package com.tingjian.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MessageClientIdTest {
    @Test
    fun clientMessageIdIsStableForRetries() {
        assertEquals("android-1700000000000-3", messageClientId(1700000000000L, 2))
        assertEquals(
            messageClientId(1700000000000L, 2),
            messageClientId(1700000000000L, 2)
        )
    }

    @Test
    fun eachTranscriptPositionGetsDifferentId() {
        assertNotEquals(
            messageClientId(1700000000000L, 1),
            messageClientId(1700000000000L, 2)
        )
    }
}
