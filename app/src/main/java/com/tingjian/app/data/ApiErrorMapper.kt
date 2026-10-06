package com.tingjian.app.data

import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

internal object ApiErrorMapper {
    fun from(exception: Throwable): ApiResult.Error {
        if (exception is CancellationException) throw exception
        return when (exception) {
            is HttpException -> fromHttp(exception)
            is SocketTimeoutException -> ApiResult.Error(
                message = "服务器响应超时，请稍后重试",
                cause = exception,
                kind = ApiErrorKind.TIMEOUT,
                retryable = true
            )
            is UnknownHostException, is ConnectException -> ApiResult.Error(
                message = "暂时无法连接听见服务，请检查网络或服务器地址",
                cause = exception,
                kind = ApiErrorKind.OFFLINE,
                retryable = true
            )
            is SSLException -> ApiResult.Error(
                message = "安全连接失败，请检查系统时间或服务器证书",
                cause = exception,
                kind = ApiErrorKind.TLS,
                retryable = true
            )
            is IOException -> ApiResult.Error(
                message = "网络连接已中断，请稍后重试",
                cause = exception,
                kind = ApiErrorKind.OFFLINE,
                retryable = true
            )
            else -> ApiResult.Error(
                message = exception.message ?: "请求失败",
                cause = exception,
                kind = ApiErrorKind.UNKNOWN
            )
        }
    }

    private fun fromHttp(exception: HttpException): ApiResult.Error {
        val response = exception.response()
        val payload = runCatching {
            response?.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let(::JSONObject)
        }.getOrNull()
        val httpCode = exception.code()
        val message = payload?.optString("message")?.takeIf { it.isNotBlank() }
            ?: defaultHttpMessage(httpCode)
        val kind = when (httpCode) {
            400, 404, 409, 422 -> ApiErrorKind.VALIDATION
            401, 403 -> ApiErrorKind.AUTHENTICATION
            429 -> ApiErrorKind.RATE_LIMITED
            in 500..599 -> ApiErrorKind.SERVER
            else -> ApiErrorKind.UNKNOWN
        }
        return ApiResult.Error(
            message = message,
            httpCode = httpCode,
            cause = exception,
            kind = kind,
            retryable = httpCode == 408 || httpCode == 429 || httpCode >= 500,
            requestId = payload?.optString("requestId")?.takeIf { it.isNotBlank() }
                ?: response?.headers()?.get("X-Request-Id"),
            serverCode = payload?.optString("code")?.takeIf { it.isNotBlank() }
        )
    }

    private fun defaultHttpMessage(code: Int): String = when (code) {
        401 -> "登录状态已失效，请重新登录"
        403 -> "当前账号无权执行此操作"
        404 -> "请求的内容不存在"
        408 -> "服务器响应超时，请稍后重试"
        429 -> "操作过于频繁，请稍后再试"
        in 500..599 -> "听见服务暂时不可用，请稍后重试"
        else -> "请求失败（$code）"
    }
}
