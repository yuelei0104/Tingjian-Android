package com.tingjian.app.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class ApiErrorMapperTest {
    @Test
    fun timeoutIsRetryable() {
        val error = ApiErrorMapper.from(SocketTimeoutException())

        assertEquals(ApiErrorKind.TIMEOUT, error.kind)
        assertTrue(error.retryable)
    }

    @Test
    fun unknownHostIsReportedAsOffline() {
        val error = ApiErrorMapper.from(UnknownHostException())

        assertEquals(ApiErrorKind.OFFLINE, error.kind)
        assertTrue(error.retryable)
    }

    @Test
    fun tlsFailureHasDedicatedMessage() {
        val error = ApiErrorMapper.from(SSLHandshakeException("certificate"))

        assertEquals(ApiErrorKind.TLS, error.kind)
        assertTrue(error.message.contains("安全连接"))
    }

    @Test
    fun genericIoFailureCanRetry() {
        val error = ApiErrorMapper.from(IOException("closed"))

        assertEquals(ApiErrorKind.OFFLINE, error.kind)
        assertTrue(error.retryable)
    }

    @Test
    fun quotaErrorHasUserFriendlyMessage() {
        val error = ApiErrorMapper.from(httpError(
            429, "USAGE_QUOTA_EXCEEDED", "本周期可用额度不足"))

        assertEquals(ApiErrorKind.QUOTA, error.kind)
        assertEquals("USAGE_QUOTA_EXCEEDED", error.serverCode)
        assertTrue(error.message.contains("额度已用完"))
    }

    @Test
    fun usageServiceFailureCanRetry() {
        val error = ApiErrorMapper.from(httpError(
            503, "USAGE_SERVICE_UNAVAILABLE", "用量服务暂时不可用"))

        assertEquals(ApiErrorKind.SERVICE_UNAVAILABLE, error.kind)
        assertTrue(error.retryable)
    }

    private fun httpError(status: Int, code: String, message: String): HttpException {
        val body = """{"requestId":"test","code":"$code","message":"$message"}"""
            .toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Unit>(status, body))
    }
}
