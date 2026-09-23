package com.tingjian.app.data

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Error(
        val message: String,
        val httpCode: Int? = null,
        val cause: Throwable? = null
    ) : ApiResult<Nothing>
}
