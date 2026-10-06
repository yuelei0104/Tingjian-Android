package com.tingjian.app.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

internal class RequestIdInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-Request-Id", UUID.randomUUID().toString())
            .build()
        return chain.proceed(request)
    }
}
