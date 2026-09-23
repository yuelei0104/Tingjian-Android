package com.tingjian.app.data

import com.tingjian.app.network.ApiEnvelope
import com.tingjian.app.network.AuthTokenResponse
import com.tingjian.app.network.GlossaryResponse
import com.tingjian.app.network.GlossaryUpsertRequest
import com.tingjian.app.network.HistoryListResponse
import com.tingjian.app.network.HomeResponse
import com.tingjian.app.network.KeywordResponse
import com.tingjian.app.network.KeywordUpsertRequest
import com.tingjian.app.network.LoginRequest
import com.tingjian.app.network.PrivacyDeleteResponse
import com.tingjian.app.network.QuickPhraseResponse
import com.tingjian.app.network.QuickPhraseUpsertRequest
import com.tingjian.app.network.RefreshTokenRequest
import com.tingjian.app.network.RegisterRequest
import com.tingjian.app.network.SessionCreateRequest
import com.tingjian.app.network.SessionDetailResponse
import com.tingjian.app.network.SessionMessageRequest
import com.tingjian.app.network.SessionMessageResponse
import com.tingjian.app.network.SessionResponse
import com.tingjian.app.network.TingjianApi
import com.tingjian.app.network.TokenStore
import retrofit2.HttpException
import java.io.IOException

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
        displayName: String
    ): ApiResult<AuthTokenResponse> = callAndSaveTokens {
        api.register(RegisterRequest(email.trim(), password, displayName.trim()))
    }

    suspend fun login(email: String, password: String): ApiResult<AuthTokenResponse> =
        callAndSaveTokens { api.login(LoginRequest(email.trim(), password)) }

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

    suspend fun home(recentSize: Int = 3): ApiResult<HomeResponse> =
        call { api.home(recentSize) }

    suspend fun createSession(title: String): ApiResult<SessionResponse> =
        call { api.createSession(SessionCreateRequest(title)) }

    suspend fun sessions(page: Int = 0, size: Int = 20): ApiResult<List<SessionResponse>> =
        call { api.sessions(page, size) }

    suspend fun session(id: String): ApiResult<SessionDetailResponse> =
        call { api.session(id) }

    suspend fun addMessage(
        sessionId: String,
        speaker: String,
        content: String
    ): ApiResult<SessionMessageResponse> =
        call { api.addMessage(sessionId, SessionMessageRequest(speaker, content)) }

    suspend fun endSession(id: String): ApiResult<SessionResponse> =
        call { api.endSession(id) }

    suspend fun history(
        keyword: String = "",
        page: Int = 0,
        size: Int = 20
    ): ApiResult<HistoryListResponse> = call { api.history(keyword, page, size) }

    suspend fun historyDetail(id: String): ApiResult<SessionDetailResponse> =
        call { api.historyDetail(id) }

    suspend fun deleteHistory(id: String): ApiResult<Unit> =
        callEmpty { api.deleteHistory(id) }

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

    suspend fun clearHistory(): ApiResult<PrivacyDeleteResponse> =
        call { api.clearHistory() }
    suspend fun clearPersonalization(): ApiResult<PrivacyDeleteResponse> =
        call { api.clearPersonalization() }
    suspend fun clearAllData(): ApiResult<PrivacyDeleteResponse> =
        call { api.clearAllData() }

    private suspend fun callAndSaveTokens(
        block: suspend () -> ApiEnvelope<AuthTokenResponse>
    ): ApiResult<AuthTokenResponse> = when (val result = call(block)) {
        is ApiResult.Success -> result.also { tokenStore.save(it.value) }
        is ApiResult.Error -> result
    }

    private suspend fun <T> call(block: suspend () -> ApiEnvelope<T>): ApiResult<T> = try {
        val envelope = block()
        val data = envelope.data
        if (data != null) ApiResult.Success(data)
        else ApiResult.Error(envelope.message.ifBlank { "服务器未返回数据" })
    } catch (exception: HttpException) {
        ApiResult.Error("请求失败（${exception.code()}）", exception.code(), exception)
    } catch (exception: IOException) {
        ApiResult.Error("无法连接服务器，请检查网络和服务器地址", cause = exception)
    } catch (exception: Exception) {
        ApiResult.Error(exception.message ?: "请求失败", cause = exception)
    }

    private suspend fun callEmpty(
        block: suspend () -> ApiEnvelope<Unit>
    ): ApiResult<Unit> = try {
        block()
        ApiResult.Success(Unit)
    } catch (exception: HttpException) {
        ApiResult.Error("请求失败（${exception.code()}）", exception.code(), exception)
    } catch (exception: IOException) {
        ApiResult.Error("无法连接服务器，请检查网络和服务器地址", cause = exception)
    } catch (exception: Exception) {
        ApiResult.Error(exception.message ?: "请求失败", cause = exception)
    }
}
