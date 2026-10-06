package com.tingjian.app.network

import java.net.URI

internal enum class BackendRouteMode(val label: String) {
    GATEWAY("微服务网关"),
    DIRECT("单体直连"),
    CUSTOM("自定义入口")
}

internal data class BackendEndpointDiagnostics(
    val baseUrl: String,
    val mode: BackendRouteMode,
    val cloudAsrEnabled: Boolean
)

internal fun backendEndpointDiagnostics(
    baseUrl: String,
    cloudAsrEnabled: Boolean
): BackendEndpointDiagnostics {
    val normalized = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
    val port = runCatching { URI(normalized).port }.getOrDefault(-1)
    val mode = when (port) {
        8088 -> BackendRouteMode.GATEWAY
        8080 -> BackendRouteMode.DIRECT
        else -> BackendRouteMode.CUSTOM
    }
    return BackendEndpointDiagnostics(normalized, mode, cloudAsrEnabled)
}
