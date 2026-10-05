package com.tingjian.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityFeedbackTest {
    @Test
    fun strongKeywordPatternIsLongerThanDefaultPattern() {
        val normal = vibrationPattern(AccessibilityEvent.KEYWORD, strong = false)
        val strong = vibrationPattern(AccessibilityEvent.KEYWORD, strong = true)

        assertTrue(strong.size > normal.size)
        assertTrue(strong.sum() > normal.sum())
    }

    @Test
    fun connectionPatternIsDistinctFromKeywordPattern() {
        assertArrayEquals(
            longArrayOf(0L, 160L, 100L, 160L),
            vibrationPattern(AccessibilityEvent.CONNECTION_LOST, strong = false)
        )
    }
}
