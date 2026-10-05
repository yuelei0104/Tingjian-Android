package com.tingjian.app.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class AndroidSpeechSynthesisProvider(
    context: Context
) : SpeechSynthesisProvider {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var ready = false
    private var activeListener: SpeechSynthesisProvider.Listener? = null

    override fun initialize(onReady: (Boolean) -> Unit) {
        if (engine != null) {
            onReady(ready)
            return
        }
        engine = TextToSpeech(appContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            main.post { onReady(ready) }
        }.also { tts ->
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {
                    main.post { activeListener?.onStarted(utteranceId) }
                }
                override fun onDone(utteranceId: String) {
                    main.post { activeListener?.onCompleted(utteranceId) }
                }
                override fun onError(utteranceId: String) {
                    main.post { activeListener?.onFailure(utteranceId, "播报失败，请检查系统语音引擎。") }
                }
            })
        }
    }

    override fun speak(
        request: SpeechSynthesisRequest,
        listener: SpeechSynthesisProvider.Listener
    ): SpeechStartResult {
        val tts = engine
            ?: return SpeechStartResult.Error("无法播报：系统语音引擎尚未初始化。")
        if (!ready) return SpeechStartResult.Error("无法播报：请安装并启用系统文字转语音引擎。")
        val locale = Locale.forLanguageTag(synthesisLanguageTag(request.voiceMode, request.text))
        if (tts.setLanguage(locale) < 0) {
            return SpeechStartResult.Error("当前语音引擎缺少对应语言的语音数据。")
        }
        tts.setSpeechRate(synthesisRate(request.voiceStyle, request.speed))
        tts.setPitch(when (request.voiceStyle) {
            "清晰" -> 1.03f
            "舒缓" -> 0.96f
            else -> 1.0f
        })
        activeListener = listener
        val utteranceId = UUID.randomUUID().toString()
        return if (tts.speak(
                request.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId
            ) == TextToSpeech.ERROR) {
            SpeechStartResult.Error("播报失败，请检查系统语音引擎后重试。")
        } else {
            SpeechStartResult.Started(utteranceId)
        }
    }

    override fun stop() {
        engine?.stop()
    }

    override fun release() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
        activeListener = null
    }
}
