package com.tingjian.app.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class CloudSpeechRecognitionProviderTest {
    @Test
    fun buildsAuthenticatedBackendWebSocketUrl() {
        assertEquals(
            "ws://10.0.2.2:8080/ws/v1/speech/asr?language=%E4%B8%AD%E8%8B%B1%E6%B7%B7%E5%90%88",
            cloudAsrUrl("http://10.0.2.2:8080/", "中英混合")
        )
    }
}
