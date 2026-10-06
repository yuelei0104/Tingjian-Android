package com.tingjian.app.speech

enum class RecognitionFailureKind {
    NO_MATCH,
    SPEECH_TIMEOUT,
    NETWORK,
    BUSY,
    PERMISSION,
    QUOTA,
    SERVICE_UNAVAILABLE,
    OTHER
}

data class RecognitionFailure(
    val kind: RecognitionFailureKind,
    val code: Int,
    val retryDelayMillis: Long,
    val message: String
)

interface SpeechRecognitionProvider {
    interface Listener {
        fun onReady()
        fun onSpeechStarted()
        fun onSpeechEnded()
        fun onPartial(text: String)
        fun onResult(text: String)
        fun onFallback(failure: RecognitionFailure) {}
        fun onFailure(failure: RecognitionFailure)
    }

    fun isAvailable(): Boolean
    fun start(language: String, listener: Listener)
    fun cancel()
    fun release()
}

internal fun recognitionFailure(code: Int): RecognitionFailure = when (code) {
    7 -> RecognitionFailure(
        RecognitionFailureKind.NO_MATCH, code, 250L, "持续监听中…")
    6 -> RecognitionFailure(
        RecognitionFailureKind.SPEECH_TIMEOUT, code, 250L, "持续监听中…")
    8 -> RecognitionFailure(
        RecognitionFailureKind.BUSY, code, 800L, "识别服务正忙，正在自动重试…")
    1, 2 -> RecognitionFailure(
        RecognitionFailureKind.NETWORK, code, 1500L, "网络异常，正在自动重连…")
    9 -> RecognitionFailure(
        RecognitionFailureKind.PERMISSION, code, 1000L, "没有麦克风权限，请在系统设置中允许。")
    else -> RecognitionFailure(
        RecognitionFailureKind.OTHER, code, 1000L, "识别暂停（错误 $code），正在自动恢复…")
}
