package com.tingjian.app.speech

class FallbackSpeechRecognitionProvider(
    private val cloud: SpeechRecognitionProvider,
    private val device: SpeechRecognitionProvider,
    private val clockMillis: () -> Long = System::currentTimeMillis
) : SpeechRecognitionProvider {
    @Volatile private var active: SpeechRecognitionProvider? = null
    @Volatile private var generation = 0
    @Volatile private var cloudRetryAfter = 0L

    override fun isAvailable(): Boolean = cloud.isAvailable() || device.isAvailable()

    override fun start(language: String, listener: SpeechRecognitionProvider.Listener) {
        cancel()
        val currentGeneration = ++generation
        if (cloud.isAvailable() && clockMillis() >= cloudRetryAfter) {
            active = cloud
            cloud.start(language, forwardingListener(
                currentGeneration, language, listener, fallbackOnFailure = true))
        } else {
            startDevice(currentGeneration, language, listener)
        }
    }

    override fun cancel() {
        generation++
        active?.cancel()
        active = null
    }

    override fun release() {
        generation++
        cloud.release()
        device.release()
        active = null
    }

    private fun startDevice(
        currentGeneration: Int,
        language: String,
        listener: SpeechRecognitionProvider.Listener
    ) {
        if (currentGeneration != generation) return
        if (!device.isAvailable()) {
            listener.onFailure(RecognitionFailure(
                RecognitionFailureKind.OTHER, -101, 1_500L,
                "云端和设备语音识别均不可用。"))
            return
        }
        active = device
        device.start(language, forwardingListener(
            currentGeneration, language, listener, fallbackOnFailure = false))
    }

    private fun forwardingListener(
        currentGeneration: Int,
        language: String,
        target: SpeechRecognitionProvider.Listener,
        fallbackOnFailure: Boolean
    ) = object : SpeechRecognitionProvider.Listener {
        override fun onReady() {
            if (currentGeneration == generation) target.onReady()
        }

        override fun onSpeechStarted() {
            if (currentGeneration == generation) target.onSpeechStarted()
        }

        override fun onSpeechEnded() {
            if (currentGeneration == generation) target.onSpeechEnded()
        }

        override fun onPartial(text: String) {
            if (currentGeneration == generation) target.onPartial(text)
        }

        override fun onResult(text: String) {
            if (currentGeneration == generation) target.onResult(text)
        }

        override fun onFailure(failure: RecognitionFailure) {
            if (currentGeneration != generation) return
            if (fallbackOnFailure) {
                cloudRetryAfter = clockMillis() +
                    maxOf(CLOUD_COOLDOWN_MILLIS, failure.retryDelayMillis)
                target.onFallback(failure)
                cloud.cancel()
                startDevice(currentGeneration, language, target)
            } else {
                target.onFailure(failure)
            }
        }
    }

    private companion object {
        const val CLOUD_COOLDOWN_MILLIS = 60_000L
    }
}
