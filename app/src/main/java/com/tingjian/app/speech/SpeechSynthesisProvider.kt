package com.tingjian.app.speech

data class SpeechSynthesisRequest(
    val text: String,
    val voiceMode: String,
    val voiceStyle: String,
    val speed: Float
)

sealed interface SpeechStartResult {
    data class Started(val utteranceId: String) : SpeechStartResult
    data class Error(val message: String) : SpeechStartResult
}

interface SpeechSynthesisProvider {
    interface Listener {
        fun onStarted(utteranceId: String)
        fun onCompleted(utteranceId: String)
        fun onFailure(utteranceId: String, message: String)
    }

    fun initialize(onReady: (Boolean) -> Unit)
    fun speak(request: SpeechSynthesisRequest, listener: Listener): SpeechStartResult
    fun stop()
    fun release()
}

internal fun synthesisLanguageTag(voiceMode: String, text: String): String = when (voiceMode) {
    "中文" -> "zh-CN"
    "English" -> "en-US"
    else -> if (text.any { it in '\u4e00'..'\u9fff' }) "zh-CN" else "en-US"
}

internal fun synthesisRate(voiceStyle: String, speed: Float): Float {
    val styleRate = when (voiceStyle) {
        "清晰" -> 0.9f
        "舒缓" -> 0.78f
        else -> 1.0f
    }
    return (styleRate * speed).coerceIn(0.5f, 1.5f)
}
