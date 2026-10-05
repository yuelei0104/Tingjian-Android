package com.tingjian.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class AndroidSpeechRecognitionProvider(
    context: Context
) : SpeechRecognitionProvider {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    override fun start(language: String, listener: SpeechRecognitionProvider.Listener) {
        val service = recognizer ?: SpeechRecognizer.createSpeechRecognizer(appContext).also {
            recognizer = it
        }
        service.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = listener.onReady()
            override fun onBeginningOfSpeech() = listener.onSpeechStarted()
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = listener.onSpeechEnded()
            override fun onError(error: Int) = listener.onFailure(recognitionFailure(error))
            override fun onResults(results: Bundle?) {
                listener.onResult(firstResult(results))
            }
            override fun onPartialResults(partialResults: Bundle?) {
                listener.onPartial(firstResult(partialResults))
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        service.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            recognitionLanguageTag(language)?.let {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, it)
            }
        })
    }

    override fun cancel() {
        recognizer?.cancel()
    }

    override fun release() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun firstResult(results: Bundle?): String =
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull().orEmpty()
}

internal fun recognitionLanguageTag(language: String): String? = when (language) {
    "中文" -> "zh-CN"
    "English" -> "en-US"
    else -> null
}
