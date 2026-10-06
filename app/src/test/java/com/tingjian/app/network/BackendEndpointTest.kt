package com.tingjian.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendEndpointTest {
    @Test
    fun detectsGatewayAndNormalizesUrl() {
        val diagnostics = backendEndpointDiagnostics("http://10.0.2.2:8088", true)

        assertEquals(BackendRouteMode.GATEWAY, diagnostics.mode)
        assertEquals("http://10.0.2.2:8088/", diagnostics.baseUrl)
        assertTrue(diagnostics.cloudAsrEnabled)
    }

    @Test
    fun detectsDirectMonolithMode() {
        val diagnostics = backendEndpointDiagnostics("http://10.0.2.2:8080/", false)

        assertEquals(BackendRouteMode.DIRECT, diagnostics.mode)
    }
}
