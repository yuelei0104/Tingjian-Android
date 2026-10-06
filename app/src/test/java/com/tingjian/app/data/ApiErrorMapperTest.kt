package com.tingjian.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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
}
