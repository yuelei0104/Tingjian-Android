package com.tingjian.app.network

import okhttp3.Interceptor
import okhttp3.Response

internal class AuthHeaderInterceptor(
    private val tokenProvider: TokenProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider.accessToken()
        if (token.isNullOrBlank() || request.url.encodedPath.startsWith("/api/auth/")) {
            return chain.proceed(request)
        }
        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }
}
