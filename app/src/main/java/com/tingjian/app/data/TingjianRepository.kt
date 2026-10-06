package com.tingjian.app.data

import com.tingjian.app.network.ApiEnvelope
import com.tingjian.app.network.AccountDeleteRequest
import com.tingjian.app.network.AccountPasswordChangeRequest
import com.tingjian.app.network.AccountProfileUpdateRequest
import com.tingjian.app.network.AccountSessionResponse
import com.tingjian.app.network.AccessibilityPreferenceResponse
import com.tingjian.app.network.AccessibilityPreferenceUpdateRequest
import com.tingjian.app.network.ActiveSessionResponse
import com.tingjian.app.network.AiSuggestionRequest
import com.tingjian.app.network.AiSuggestionResponse
import com.tingjian.app.network.AuthTokenResponse
import com.tingjian.app.network.AuthUserResponse
import com.tingjian.app.network.EmailVerificationRequest
import com.tingjian.app.network.GlossaryResponse
import com.tingjian.app.network.GlossaryUpsertRequest
import com.tingjian.app.network.HistoryListResponse
import com.tingjian.app.network.HistorySummaryResponse
import com.tingjian.app.network.HomeResponse
import com.tingjian.app.network.KeywordResponse
import com.tingjian.app.network.KeywordUpsertRequest
import com.tingjian.app.network.LoginRequest
import com.tingjian.app.network.PasswordResetRequest
import com.tingjian.app.network.PhoneBindingRequest
import com.tingjian.app.network.PhoneBindingResponse
import com.tingjian.app.network.PhoneVerificationRequest
import com.tingjian.app.network.PrivacyDeleteResponse
import com.tingjian.app.network.PrivacyExportResponse
import com.tingjian.app.network.QuickPhraseResponse
import com.tingjian.app.network.QuickPhraseUpsertRequest
import com.tingjian.app.network.RefreshTokenRequest
import com.tingjian.app.network.RegisterRequest
import com.tingjian.app.network.SessionCreateRequest
import com.tingjian.app.network.SessionDetailResponse
import com.tingjian.app.network.SessionMessageRequest
import com.tingjian.app.network.SessionMessageResponse
import com.tingjian.app.network.SessionMessagePageResponse
import com.tingjian.app.network.SessionRenameRequest
import com.tingjian.app.network.SessionResponse
import com.tingjian.app.network.SmsPasswordResetRequest
import com.tingjian.app.network.TingjianApi
import com.tingjian.app.network.TokenStore
import com.tingjian.app.network.UserPreferenceResponse
import com.tingjian.app.network.UserPreferenceUpdateRequest
import com.tingjian.app.network.UsageResponse
import com.tingjian.app.network.VerificationChallengeResponse
import kotlinx.coroutines.CancellationException

class TingjianRepository internal constructor(
    private val api: TingjianApi,
    private val tokenStore: TokenStore
) {
    fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()
    fun displayName(): String? = tokenStore.displayName()
    fun email(): String? = tokenStore.email()

    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        verificationId: String,
        verificationCode: String
    ): ApiResult<AuthTokenResponse> = callAndSaveTokens {
        api.register(RegisterRequest(
            email.trim(), password, displayName.trim(), verificationId, verificationCode.trim()
        ))
    }

    suspend fun login(email: String, password: String): ApiResult<AuthTokenResponse> =
        callAndSaveTokens { api.login(LoginRequest(email.trim(), password)) }

    suspend fun requestRegistrationCode(
        email: String
    ): ApiResult<VerificationChallengeResponse> =
        call { api.requestRegistrationCode(EmailVerificationRequest(email.trim())) }

    suspend fun requestPasswordResetCode(
        email: String
    ): ApiResult<VerificationChallengeResponse> =
        call { api.requestPasswordReset(EmailVerificationRequest(email.trim())) }

    suspend fun resetPassword(
        email: String,
        verificationId: String,
        verificationCode: String,
        newPassword: String
    ): ApiResult<Unit> = callEmpty {
        api.resetPassword(PasswordResetRequest(
            email.trim(), verificationId, verificationCode.trim(), newPassword
        ))
    }

    suspend fun requestSmsPasswordResetCode(
        phone: String
    ): ApiResult<VerificationChallengeResponse> =
        call { api.requestSmsPasswordReset(PhoneVerificationRequest(phone.trim())) }

    suspend fun resetPasswordBySms(
        phone: String,
        verificationId: String,
        verificationCode: String,
        newPassword: String
    ): ApiResult<Unit> = callEmpty {
        api.resetPasswordBySms(SmsPasswordResetRequest(
            phone.trim(), verificationId, verificationCode.trim(), newPassword
        ))
    }

    suspend fun refresh(): ApiResult<AuthTokenResponse> {
        val refreshToken = tokenStore.refreshToken()
            ?: return ApiResult.Error("登录状态已失效，请重新登录")
        return callAndSaveTokens { api.refresh(RefreshTokenRequest(refreshToken)) }
    }

    suspend fun logout(): ApiResult<Unit> {
        val refreshToken = tokenStore.refreshToken()
        if (refreshToken == null) {
            tokenStore.clear()
            return ApiResult.Success(Unit)
        }
        return try {
            callEmpty { api.logout(RefreshTokenRequest(refreshToken)) }
        } finally {
            tokenStore.clear()
        }
    }

    suspend fun deleteAccount(password: String): ApiResult<Unit> {
        val result = callEmpty { api.deleteAccount(AccountDeleteRequest(password)) }
        if (result is ApiResult.Success) tokenStore.clear()
        return result
    }

    suspend fun accountProfile(): ApiResult<AuthUserResponse> =
        saveProfile(call { api.accountProfile() })

    suspend fun updateAccountProfile(displayName: String): ApiResult<AuthUserResponse> =
        saveProfile(call {
            api.updateAccountProfile(AccountProfileUpdateRequest(displayName.trim()))
        })

    suspend fun changeAccountPassword(
        currentPassword: String,
        newPassword: String
    ): ApiResult<Unit> {
        val result = callEmpty {
            api.changeAccountPassword(
                AccountPasswordChangeRequest(currentPassword, newPassword)
            )
        }
        if (result is ApiResult.Success) tokenStore.clear()
        return result
    }

    suspend fun accountSessions(): ApiResult<List<AccountSessionResponse>> =
        call { api.accountSessions() }

    suspend fun revokeAccountSession(id: String): ApiResult<Unit> =
        callEmpty { api.revokeAccountSession(id) }

    suspend fun revokeOtherAccountSessions(): ApiResult<Unit> =
        callEmpty { api.revokeOtherAccountSessions() }

    suspend fun accountPhone(): ApiResult<PhoneBindingResponse> =
        call { api.accountPhone() }

    suspend fun requestPhoneBindingCode(
        phone: String
    ): ApiResult<VerificationChallengeResponse> =
        call { api.requestPhoneBindingCode(PhoneVerificationRequest(phone.trim())) }

    suspend fun bindPhone(
        phone: String,
        verificationId: String,
        verificationCode: String
    ): ApiResult<PhoneBindingResponse> = call {
        api.bindPhone(PhoneBindingRequest(
            phone.trim(), verificationId, verificationCode.trim()
        ))
    }

    suspend fun unbindPhone(password: String): ApiResult<Unit> =
        callEmpty { api.unbindPhone(AccountDeleteRequest(password)) }

    suspend fun home(recentSize: Int = 3): ApiResult<HomeResponse> =
        call { api.home(recentSize) }

    suspend fun usage(): ApiResult<UsageResponse> = call { api.usage() }

    suspend fun createSession(title: String): ApiResult<SessionResponse> =
        call { api.createSession(SessionCreateRequest(title)) }

    suspend fun sessions(page: Int = 0, size: Int = 20): ApiResult<List<SessionResponse>> =
        call { api.sessions(page, size) }

    suspend fun activeSession(): ApiResult<ActiveSessionResponse> =
        call { api.activeSession() }

    suspend fun session(id: String): ApiResult<SessionDetailResponse> =
        call { api.session(id) }

    suspend fun renameSession(id: String, title: String): ApiResult<SessionResponse> =
        call { api.renameSession(id, SessionRenameRequest(title.trim())) }

    suspend fun addMessage(
        sessionId: String,
        clientMessageId: String,
        speaker: String,
        content: String
    ): ApiResult<SessionMessageResponse> =
        call { api.addMessage(
            sessionId, SessionMessageRequest(clientMessageId, speaker, content)
        ) }

    suspend fun sessionMessages(
        sessionId: String,
        afterSequence: Long = 0,
        size: Int = 50
    ): ApiResult<SessionMessagePageResponse> =
        call { api.sessionMessages(sessionId, afterSequence, size) }

    suspend fun endSession(id: String): ApiResult<SessionResponse> =
        call { api.endSession(id) }

    suspend fun aiSuggestion(request: AiSuggestionRequest): ApiResult<AiSuggestionResponse> =
        call { api.aiSuggestion(request) }

    suspend fun history(
        keyword: String = "",
        page: Int = 0,
        size: Int = 20
    ): ApiResult<HistoryListResponse> = call { api.history(keyword, page, size) }

    suspend fun historyDetail(id: String): ApiResult<SessionDetailResponse> =
        call { api.historyDetail(id) }

    suspend fun deleteHistory(id: String): ApiResult<Unit> =
        callEmpty { api.deleteHistory(id) }

    suspend fun summarizeHistory(id: String): ApiResult<HistorySummaryResponse> =
        call { api.summarizeHistory(id) }

    suspend fun keywords(): ApiResult<List<KeywordResponse>> = call { api.keywords() }
    suspend fun createKeyword(request: KeywordUpsertRequest): ApiResult<KeywordResponse> =
        call { api.createKeyword(request) }
    suspend fun updateKeyword(
        id: String,
        request: KeywordUpsertRequest
    ): ApiResult<KeywordResponse> = call { api.updateKeyword(id, request) }
    suspend fun deleteKeyword(id: String): ApiResult<Unit> =
        callEmpty { api.deleteKeyword(id) }

    suspend fun glossary(): ApiResult<List<GlossaryResponse>> = call { api.glossary() }
    suspend fun createGlossary(request: GlossaryUpsertRequest): ApiResult<GlossaryResponse> =
        call { api.createGlossary(request) }
    suspend fun updateGlossary(
        id: String,
        request: GlossaryUpsertRequest
    ): ApiResult<GlossaryResponse> = call { api.updateGlossary(id, request) }
    suspend fun deleteGlossary(id: String): ApiResult<Unit> =
        callEmpty { api.deleteGlossary(id) }

    suspend fun quickPhrases(): ApiResult<List<QuickPhraseResponse>> = call { api.quickPhrases() }
    suspend fun createQuickPhrase(
        request: QuickPhraseUpsertRequest
    ): ApiResult<QuickPhraseResponse> = call { api.createQuickPhrase(request) }
    suspend fun updateQuickPhrase(
        id: String,
        request: QuickPhraseUpsertRequest
    ): ApiResult<QuickPhraseResponse> = call { api.updateQuickPhrase(id, request) }
    suspend fun deleteQuickPhrase(id: String): ApiResult<Unit> =
        callEmpty { api.deleteQuickPhrase(id) }

    suspend fun preferences(): ApiResult<UserPreferenceResponse> =
        call { api.preferences() }

    suspend fun updatePreferences(
        request: UserPreferenceUpdateRequest
    ): ApiResult<UserPreferenceResponse> = call { api.updatePreferences(request) }

    suspend fun accessibilityPreferences(): ApiResult<AccessibilityPreferenceResponse> =
        call { api.accessibilityPreferences() }

    suspend fun updateAccessibilityPreferences(
        request: AccessibilityPreferenceUpdateRequest
    ): ApiResult<AccessibilityPreferenceResponse> =
        call { api.updateAccessibilityPreferences(request) }

    suspend fun clearHistory(): ApiResult<PrivacyDeleteResponse> =
        call { api.clearHistory() }
    suspend fun clearPersonalization(): ApiResult<PrivacyDeleteResponse> =
        call { api.clearPersonalization() }
    suspend fun clearAllData(): ApiResult<PrivacyDeleteResponse> =
        call { api.clearAllData() }
    suspend fun exportData(): ApiResult<PrivacyExportResponse> =
        call { api.exportData() }

    private suspend fun callAndSaveTokens(
        block: suspend () -> ApiEnvelope<AuthTokenResponse>
    ): ApiResult<AuthTokenResponse> = when (val result = call(block)) {
        is ApiResult.Success -> result.also { tokenStore.save(it.value) }
        is ApiResult.Error -> result
    }

    private fun saveProfile(result: ApiResult<AuthUserResponse>): ApiResult<AuthUserResponse> {
        if (result is ApiResult.Success) tokenStore.updateProfile(result.value)
        return result
    }

    private suspend fun <T> call(block: suspend () -> ApiEnvelope<T>): ApiResult<T> = try {
        val envelope = block()
        val data = envelope.data
        if (data != null) ApiResult.Success(data)
        else ApiResult.Error(
            message = envelope.message.ifBlank { "服务器未返回数据" },
            requestId = envelope.requestId,
            serverCode = envelope.code
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Throwable) {
        ApiErrorMapper.from(exception)
    }

    private suspend fun callEmpty(
        block: suspend () -> ApiEnvelope<Unit>
    ): ApiResult<Unit> = try {
        block()
        ApiResult.Success(Unit)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Throwable) {
        ApiErrorMapper.from(exception)
    }
}
