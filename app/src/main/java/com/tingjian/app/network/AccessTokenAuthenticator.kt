package com.tingjian.app.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

internal class AccessTokenAuthenticator(
    private val tokenStore: TokenStore,
    private val refreshApi: TokenRefreshApi
) : Authenticator {
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2 || response.request.url.encodedPath.startsWith("/api/auth/")) {
            return null
        }

        synchronized(refreshLock) {
            val failedHeader = response.request.header("Authorization")
            val currentAccessToken = tokenStore.accessToken()
            if (!currentAccessToken.isNullOrBlank() && failedHeader != "Bearer $currentAccessToken") {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            val refreshToken = tokenStore.refreshToken() ?: return null
            val refreshed = runCatching {
                refreshApi.refresh(RefreshTokenRequest(refreshToken)).execute()
            }.getOrNull()
            val tokens = refreshed?.takeIf { it.isSuccessful }?.body()?.data
            if (tokens == null) {
                tokenStore.clear()
                return null
            }

            tokenStore.save(tokens)
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${tokens.accessToken}")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
