package com.tingjian.app.data

import com.tingjian.app.network.AccountExportResponse
import com.tingjian.app.network.AccessibilityPreferenceResponse
import com.tingjian.app.network.PrivacyExportResponse
import com.tingjian.app.network.UserPreferenceResponse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataExportJsonTest {
    @Test
    fun prettyJsonKeepsChineseAccountData() {
        val export = PrivacyExportResponse(
            exportedAt = "2026-10-01T10:00:00",
            account = AccountExportResponse(
                email = "demo@example.com",
                displayName = "听见用户",
                createdAt = "2026-01-01T10:00:00"
            ),
            conversations = emptyList(),
            keywords = emptyList(),
            glossaryTerms = emptyList(),
            quickPhrases = emptyList(),
            preferences = UserPreferenceResponse(
                configured = true,
                largeText = false,
                voiceMode = "自动",
                voiceStyle = "自然",
                ttsSpeed = 1.0,
                recognitionLanguage = "中英混合",
                keywordVibration = true,
                keywordHighlight = true,
                autoSummary = false,
                updatedAt = "2026-10-01T10:00:00"
            ),
            accessibility = AccessibilityPreferenceResponse(
                configured = true,
                highContrast = false,
                visualAlerts = true,
                systemNotifications = true,
                strongVibration = false,
                captionFollow = true,
                updatedAt = "2026-10-01T10:00:00"
            )
        )

        val json = export.toPrettyJson()

        assertTrue(json.contains("听见用户"))
        assertTrue(json.contains("demo@example.com"))
        assertTrue(json.contains("\n"))
    }
}
