package com.tingjian.app.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechPolicyTest {
    @Test
    fun recognitionErrorsHaveStableRecoveryPolicy() {
        assertEquals(250L, recognitionFailure(7).retryDelayMillis)
        assertEquals(RecognitionFailureKind.NETWORK, recognitionFailure(2).kind)
        assertEquals(RecognitionFailureKind.PERMISSION, recognitionFailure(9).kind)
    }

    @Test
    fun mixedVoiceModeChoosesLanguageFromText() {
        assertEquals("zh-CN", synthesisLanguageTag("中英混合", "你好"))
        assertEquals("en-US", synthesisLanguageTag("中英混合", "hello"))
        assertTrue(synthesisRate("舒缓", 1.0f) < synthesisRate("自然", 1.0f))
    }
}
