package com.tingjian.app.data

enum class ApiErrorKind {
    VALIDATION,
    AUTHENTICATION,
    RATE_LIMITED,
    SERVER,
    TIMEOUT,
    OFFLINE,
    TLS,
    UNKNOWN
}

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Error(
        val message: String,
        val httpCode: Int? = null,
        val cause: Throwable? = null,
        val kind: ApiErrorKind = ApiErrorKind.UNKNOWN,
        val retryable: Boolean = false,
        val requestId: String? = null,
        val serverCode: String? = null
    ) : ApiResult<Nothing>
}
