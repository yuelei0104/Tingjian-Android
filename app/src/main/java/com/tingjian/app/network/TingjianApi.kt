package com.tingjian.app.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TingjianApi {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiEnvelope<AuthTokenResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiEnvelope<AuthTokenResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): ApiEnvelope<AuthTokenResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequest): ApiEnvelope<Unit>

    @GET("api/v1/home")
    suspend fun home(@Query("recentSize") recentSize: Int = 3): ApiEnvelope<HomeResponse>

    @POST("api/v1/sessions")
    suspend fun createSession(@Body request: SessionCreateRequest): ApiEnvelope<SessionResponse>

    @GET("api/v1/sessions")
    suspend fun sessions(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiEnvelope<List<SessionResponse>>

    @GET("api/v1/sessions/{id}")
    suspend fun session(@Path("id") id: String): ApiEnvelope<SessionDetailResponse>

    @POST("api/v1/sessions/{id}/messages")
    suspend fun addMessage(
        @Path("id") id: String,
        @Body request: SessionMessageRequest
    ): ApiEnvelope<SessionMessageResponse>

    @POST("api/v1/sessions/{id}/end")
    suspend fun endSession(@Path("id") id: String): ApiEnvelope<SessionResponse>

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

    @DELETE("api/v1/privacy/history")
    suspend fun clearHistory(): ApiEnvelope<PrivacyDeleteResponse>

    @DELETE("api/v1/privacy/personalization")
    suspend fun clearPersonalization(): ApiEnvelope<PrivacyDeleteResponse>

    @DELETE("api/v1/privacy/all-data")
    suspend fun clearAllData(): ApiEnvelope<PrivacyDeleteResponse>
}

internal interface TokenRefreshApi {
    @POST("api/auth/refresh")
    fun refresh(@Body request: RefreshTokenRequest): Call<ApiEnvelope<AuthTokenResponse>>
}
