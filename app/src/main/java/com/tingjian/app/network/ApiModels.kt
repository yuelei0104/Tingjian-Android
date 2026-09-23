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

data class SessionCreateRequest(val title: String)
data class SessionMessageRequest(val speaker: String, val content: String)
data class SessionResponse(
    val id: String,
    val title: String,
    val status: String,
    val startedAt: String,
    val endedAt: String?
)
data class SessionMessageResponse(
    val id: String,
    val speaker: String,
    val content: String,
    val createdAt: String
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
