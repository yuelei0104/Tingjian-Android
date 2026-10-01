package com.tingjian.app.network

data class ApiEnvelope<T>(
    val requestId: String,
    val code: String,
    val message: String,
    val data: T?,
    val timestamp: String
)

data class RegisterRequest(val email: String, val password: String, val displayName: String)
data class LoginRequest(val email: String, val password: String)
data class RefreshTokenRequest(val refreshToken: String)
data class AccountDeleteRequest(val password: String)
data class AccountProfileUpdateRequest(val displayName: String)
data class AccountPasswordChangeRequest(val currentPassword: String, val newPassword: String)
data class AuthUserResponse(
    val id: String,
    val email: String,
    val displayName: String,
    val createdAt: String
)
data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: String,
    val refreshExpiresAt: String,
    val user: AuthUserResponse
)

data class HomeOverviewResponse(
    val conversationCount: Long,
    val messageCount: Long,
    val totalDurationSeconds: Long
)
data class HomeSceneResponse(val code: String, val name: String)
data class HomePlanResponse(
    val code: String,
    val name: String,
    val description: String,
    val purchasable: Boolean
)
data class HomeResponse(
    val overview: HomeOverviewResponse,
    val recentConversations: List<HistoryItemResponse>,
    val scenes: List<HomeSceneResponse>,
    val plan: HomePlanResponse
)

data class UsageOverviewResponse(
    val conversationCount: Long,
    val messageCount: Long,
    val textCharacterCount: Long,
    val totalDurationSeconds: Long
)
data class UsageMetricResponse(
    val code: String,
    val name: String,
    val unit: String,
    val used: Long,
    val limit: Long,
    val remaining: Long,
    val progress: Double,
    val description: String
)
data class UsageResponse(
    val planCode: String,
    val planName: String,
    val planDescription: String,
    val purchasable: Boolean,
    val periodStart: String,
    val periodEnd: String,
    val overview: UsageOverviewResponse,
    val metrics: List<UsageMetricResponse>
)

data class SessionCreateRequest(val title: String)
data class SessionRenameRequest(val title: String)
data class SessionMessageRequest(
    val clientMessageId: String,
    val speaker: String,
    val content: String
)
data class SessionResponse(
    val id: String,
    val title: String,
    val status: String,
    val startedAt: String,
    val endedAt: String?
)
data class SessionMessageResponse(
    val id: String,
    val clientMessageId: String,
    val sequence: Long,
    val speaker: String,
    val content: String,
    val createdAt: String
)
data class SessionMessagePageResponse(
    val items: List<SessionMessageResponse>,
    val nextAfterSequence: Long,
    val hasNext: Boolean
)

data class RealtimeMessageRequest(
    val type: String = "MESSAGE",
    val sessionId: String,
    val clientMessageId: String,
    val speaker: String,
    val content: String
)

data class RealtimeMessageEvent(
    val type: String,
    val sessionId: String?,
    val clientMessageId: String?,
    val message: SessionMessageResponse?,
    val code: String,
    val detail: String
)
data class SessionDetailResponse(
    val session: SessionResponse,
    val messages: List<SessionMessageResponse>
)

data class HistoryItemResponse(
    val id: String,
    val title: String,
    val status: String,
    val startedAt: String,
    val endedAt: String?,
    val messageCount: Long,
    val preview: String?
)
data class HistoryListResponse(
    val items: List<HistoryItemResponse>,
    val page: Int,
    val size: Int,
    val total: Long,
    val hasNext: Boolean
)

data class HistorySummaryResponse(
    val sessionId: String,
    val summary: String,
    val messageCount: Int,
    val generatedBy: String
)

data class AccountSessionResponse(
    val id: String,
    val createdAt: String,
    val lastActiveAt: String,
    val expiresAt: String
)

data class KeywordUpsertRequest(
    val phrase: String,
    val vibrationEnabled: Boolean,
    val priority: Int,
    val enabled: Boolean
)
data class KeywordResponse(
    val id: String,
    val phrase: String,
    val vibrationEnabled: Boolean,
    val priority: Int,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class GlossaryUpsertRequest(
    val term: String,
    val alias: String?,
    val language: String,
    val category: String,
    val priority: Int,
    val enabled: Boolean
)
data class GlossaryResponse(
    val id: String,
    val term: String,
    val alias: String?,
    val language: String,
    val category: String,
    val priority: Int,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class QuickPhraseUpsertRequest(
    val content: String,
    val category: String,
    val sortOrder: Int,
    val enabled: Boolean
)
data class QuickPhraseResponse(
    val id: String,
    val content: String,
    val category: String,
    val sortOrder: Int,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class PrivacyDeleteResponse(
    val conversations: Int,
    val messages: Int,
    val keywords: Int,
    val glossaryTerms: Int,
    val quickPhrases: Int
)

data class AccountExportResponse(
    val email: String,
    val displayName: String,
    val createdAt: String
)

data class MessageExportResponse(
    val speaker: String,
    val content: String,
    val createdAt: String
)

data class ConversationExportResponse(
    val id: String,
    val title: String,
    val status: String,
    val startedAt: String,
    val endedAt: String?,
    val messages: List<MessageExportResponse>
)

data class PrivacyExportResponse(
    val exportedAt: String,
    val account: AccountExportResponse,
    val conversations: List<ConversationExportResponse>,
    val keywords: List<KeywordResponse>,
    val glossaryTerms: List<GlossaryResponse>,
    val quickPhrases: List<QuickPhraseResponse>,
    val preferences: UserPreferenceResponse
)

data class UserPreferenceResponse(
    val configured: Boolean,
    val largeText: Boolean,
    val voiceMode: String,
    val voiceStyle: String,
    val ttsSpeed: Double,
    val recognitionLanguage: String,
    val keywordVibration: Boolean,
    val keywordHighlight: Boolean,
    val autoSummary: Boolean,
    val updatedAt: String?
)

data class UserPreferenceUpdateRequest(
    val largeText: Boolean,
    val voiceMode: String,
    val voiceStyle: String,
    val ttsSpeed: Double,
    val recognitionLanguage: String,
    val keywordVibration: Boolean,
    val keywordHighlight: Boolean,
    val autoSummary: Boolean
)
