package com.tingjian.app.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TingjianApi {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiEnvelope<AuthTokenResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiEnvelope<AuthTokenResponse>

    @POST("api/auth/email-verification/request")
    suspend fun requestRegistrationCode(
        @Body request: EmailVerificationRequest
    ): ApiEnvelope<VerificationChallengeResponse>

    @POST("api/auth/password/forgot")
    suspend fun requestPasswordReset(
        @Body request: EmailVerificationRequest
    ): ApiEnvelope<VerificationChallengeResponse>

    @POST("api/auth/password/reset")
    suspend fun resetPassword(@Body request: PasswordResetRequest): ApiEnvelope<Unit>

    @POST("api/auth/password/forgot/sms")
    suspend fun requestSmsPasswordReset(
        @Body request: PhoneVerificationRequest
    ): ApiEnvelope<VerificationChallengeResponse>

    @POST("api/auth/password/reset/sms")
    suspend fun resetPasswordBySms(@Body request: SmsPasswordResetRequest): ApiEnvelope<Unit>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): ApiEnvelope<AuthTokenResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequest): ApiEnvelope<Unit>

    @HTTP(method = "DELETE", path = "api/v1/account", hasBody = true)
    suspend fun deleteAccount(@Body request: AccountDeleteRequest): ApiEnvelope<Unit>

    @GET("api/v1/account")
    suspend fun accountProfile(): ApiEnvelope<AuthUserResponse>

    @PATCH("api/v1/account")
    suspend fun updateAccountProfile(
        @Body request: AccountProfileUpdateRequest
    ): ApiEnvelope<AuthUserResponse>

    @PUT("api/v1/account/password")
    suspend fun changeAccountPassword(
        @Body request: AccountPasswordChangeRequest
    ): ApiEnvelope<Unit>

    @GET("api/v1/account/sessions")
    suspend fun accountSessions(): ApiEnvelope<List<AccountSessionResponse>>

    @DELETE("api/v1/account/sessions/{id}")
    suspend fun revokeAccountSession(@Path("id") id: String): ApiEnvelope<Unit>

    @DELETE("api/v1/account/sessions")
    suspend fun revokeOtherAccountSessions(): ApiEnvelope<Unit>

    @GET("api/v1/account/phone")
    suspend fun accountPhone(): ApiEnvelope<PhoneBindingResponse>

    @POST("api/v1/account/phone/verification")
    suspend fun requestPhoneBindingCode(
        @Body request: PhoneVerificationRequest
    ): ApiEnvelope<VerificationChallengeResponse>

    @PUT("api/v1/account/phone")
    suspend fun bindPhone(@Body request: PhoneBindingRequest): ApiEnvelope<PhoneBindingResponse>

    @HTTP(method = "DELETE", path = "api/v1/account/phone", hasBody = true)
    suspend fun unbindPhone(@Body request: AccountDeleteRequest): ApiEnvelope<Unit>

    @GET("api/v1/home")
    suspend fun home(@Query("recentSize") recentSize: Int = 3): ApiEnvelope<HomeResponse>

    @GET("api/v1/usage")
    suspend fun usage(): ApiEnvelope<UsageResponse>

    @POST("api/v1/sessions")
    suspend fun createSession(@Body request: SessionCreateRequest): ApiEnvelope<SessionResponse>

    @GET("api/v1/sessions")
    suspend fun sessions(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiEnvelope<List<SessionResponse>>

    @GET("api/v1/sessions/active")
    suspend fun activeSession(): ApiEnvelope<ActiveSessionResponse>

    @GET("api/v1/sessions/{id}")
    suspend fun session(@Path("id") id: String): ApiEnvelope<SessionDetailResponse>

    @PATCH("api/v1/sessions/{id}")
    suspend fun renameSession(
        @Path("id") id: String,
        @Body request: SessionRenameRequest
    ): ApiEnvelope<SessionResponse>

    @POST("api/v1/sessions/{id}/messages")
    suspend fun addMessage(
        @Path("id") id: String,
        @Body request: SessionMessageRequest
    ): ApiEnvelope<SessionMessageResponse>

    @GET("api/v1/sessions/{id}/messages")
    suspend fun sessionMessages(
        @Path("id") id: String,
        @Query("afterSequence") afterSequence: Long = 0,
        @Query("size") size: Int = 50
    ): ApiEnvelope<SessionMessagePageResponse>

    @POST("api/v1/sessions/{id}/end")
    suspend fun endSession(@Path("id") id: String): ApiEnvelope<SessionResponse>

    @POST("api/v1/ai/suggestions")
    suspend fun aiSuggestion(
        @Body request: AiSuggestionRequest
    ): ApiEnvelope<AiSuggestionResponse>

    @GET("api/v1/history")
    suspend fun history(
        @Query("keyword") keyword: String = "",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiEnvelope<HistoryListResponse>

    @GET("api/v1/history/{id}")
    suspend fun historyDetail(@Path("id") id: String): ApiEnvelope<SessionDetailResponse>

    @DELETE("api/v1/history/{id}")
    suspend fun deleteHistory(@Path("id") id: String): ApiEnvelope<Unit>

    @POST("api/v1/history/{id}/summary")
    suspend fun summarizeHistory(@Path("id") id: String): ApiEnvelope<HistorySummaryResponse>

    @GET("api/v1/keywords")
    suspend fun keywords(): ApiEnvelope<List<KeywordResponse>>

    @POST("api/v1/keywords")
    suspend fun createKeyword(@Body request: KeywordUpsertRequest): ApiEnvelope<KeywordResponse>

    @PUT("api/v1/keywords/{id}")
    suspend fun updateKeyword(
        @Path("id") id: String,
        @Body request: KeywordUpsertRequest
    ): ApiEnvelope<KeywordResponse>

    @DELETE("api/v1/keywords/{id}")
    suspend fun deleteKeyword(@Path("id") id: String): ApiEnvelope<Unit>

    @GET("api/v1/glossary")
    suspend fun glossary(): ApiEnvelope<List<GlossaryResponse>>

    @POST("api/v1/glossary")
    suspend fun createGlossary(@Body request: GlossaryUpsertRequest): ApiEnvelope<GlossaryResponse>

    @PUT("api/v1/glossary/{id}")
    suspend fun updateGlossary(
        @Path("id") id: String,
        @Body request: GlossaryUpsertRequest
    ): ApiEnvelope<GlossaryResponse>

    @DELETE("api/v1/glossary/{id}")
    suspend fun deleteGlossary(@Path("id") id: String): ApiEnvelope<Unit>

    @GET("api/v1/quick-phrases")
    suspend fun quickPhrases(): ApiEnvelope<List<QuickPhraseResponse>>

    @POST("api/v1/quick-phrases")
    suspend fun createQuickPhrase(
        @Body request: QuickPhraseUpsertRequest
    ): ApiEnvelope<QuickPhraseResponse>

    @PUT("api/v1/quick-phrases/{id}")
    suspend fun updateQuickPhrase(
        @Path("id") id: String,
        @Body request: QuickPhraseUpsertRequest
    ): ApiEnvelope<QuickPhraseResponse>

    @DELETE("api/v1/quick-phrases/{id}")
    suspend fun deleteQuickPhrase(@Path("id") id: String): ApiEnvelope<Unit>

    @GET("api/v1/preferences")
    suspend fun preferences(): ApiEnvelope<UserPreferenceResponse>

    @PUT("api/v1/preferences")
    suspend fun updatePreferences(
        @Body request: UserPreferenceUpdateRequest
    ): ApiEnvelope<UserPreferenceResponse>

    @GET("api/v1/accessibility/preferences")
    suspend fun accessibilityPreferences(): ApiEnvelope<AccessibilityPreferenceResponse>

    @PUT("api/v1/accessibility/preferences")
    suspend fun updateAccessibilityPreferences(
        @Body request: AccessibilityPreferenceUpdateRequest
    ): ApiEnvelope<AccessibilityPreferenceResponse>

    @DELETE("api/v1/privacy/history")
    suspend fun clearHistory(): ApiEnvelope<PrivacyDeleteResponse>

    @DELETE("api/v1/privacy/personalization")
    suspend fun clearPersonalization(): ApiEnvelope<PrivacyDeleteResponse>

    @DELETE("api/v1/privacy/all-data")
    suspend fun clearAllData(): ApiEnvelope<PrivacyDeleteResponse>

    @GET("api/v1/privacy/export")
    suspend fun exportData(): ApiEnvelope<PrivacyExportResponse>
}

internal interface TokenRefreshApi {
    @POST("api/auth/refresh")
    fun refresh(@Body request: RefreshTokenRequest): Call<ApiEnvelope<AuthTokenResponse>>
}
