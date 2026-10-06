package com.tingjian.app.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackSpeechRecognitionProviderTest {
    @Test
    fun quotaFailureFallsBackToDeviceAndHonorsCooldown() {
        var now = 1_000L
        val quotaFailure = RecognitionFailure(
            RecognitionFailureKind.QUOTA,
            429,
            30 * 60 * 1_000L,
            "云端额度不足，已切换设备识别"
        )
        val cloud = FakeProvider(quotaFailure)
        val device = FakeProvider()
        val provider = FallbackSpeechRecognitionProvider(cloud, device) { now }
        val listener = RecordingListener()

        provider.start("中英混合", listener)

        assertEquals(1, cloud.starts)
        assertEquals(1, device.starts)
        assertEquals(RecognitionFailureKind.QUOTA, listener.fallback?.kind)

        now += 60_000L
        provider.start("中英混合", listener)

        assertEquals(1, cloud.starts)
        assertEquals(2, device.starts)
        assertTrue(listener.failures.isEmpty())
    }

    private class FakeProvider(
        private val failure: RecognitionFailure? = null
    ) : SpeechRecognitionProvider {
        var starts = 0

        override fun isAvailable(): Boolean = true

        override fun start(language: String, listener: SpeechRecognitionProvider.Listener) {
            starts++
            if (failure == null) listener.onReady() else listener.onFailure(failure)
        }

        override fun cancel() = Unit
        override fun release() = Unit
    }

    private class RecordingListener : SpeechRecognitionProvider.Listener {
        var fallback: RecognitionFailure? = null
        val failures = mutableListOf<RecognitionFailure>()

        override fun onReady() = Unit
        override fun onSpeechStarted() = Unit
        override fun onSpeechEnded() = Unit
        override fun onPartial(text: String) = Unit
        override fun onResult(text: String) = Unit
        override fun onFallback(failure: RecognitionFailure) {
            fallback = failure
        }
        override fun onFailure(failure: RecognitionFailure) {
            failures += failure
        }
    }
}
