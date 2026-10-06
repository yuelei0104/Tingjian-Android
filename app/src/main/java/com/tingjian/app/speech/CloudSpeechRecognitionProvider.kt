package com.tingjian.app.speech

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.tingjian.app.network.TokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

class CloudSpeechRecognitionProvider internal constructor(
    private val client: OkHttpClient,
    private val baseUrl: String,
    private val tokenStore: TokenStore
) : SpeechRecognitionProvider {
    private val main = Handler(Looper.getMainLooper())
    private val cancelled = AtomicBoolean(true)
    @Volatile private var socket: WebSocket? = null
    @Volatile private var recorder: AudioRecord? = null
    @Volatile private var audioThread: Thread? = null
    @Volatile private var generation = 0

    override fun isAvailable(): Boolean = !tokenStore.accessToken().isNullOrBlank()

    override fun start(language: String, listener: SpeechRecognitionProvider.Listener) {
        cancel()
        val token = tokenStore.accessToken()
        if (token.isNullOrBlank()) {
            listener.onFailure(cloudFailure("请先登录后再使用云端语音识别。"))
            return
        }
        val currentGeneration = ++generation
        cancelled.set(false)
        val request = Request.Builder()
            .url(cloudAsrUrl(baseUrl, language))
            .header("Authorization", "Bearer $token")
            .build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isCurrent(currentGeneration)) webSocket.cancel()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!isCurrent(currentGeneration)) return
                val root = runCatching { JSONObject(text) }.getOrNull() ?: return
                val type = root.optString("type")
                val data = root.optJSONObject("data")
                when (type) {
                    "READY" -> {
                        main.post {
                            if (isCurrent(currentGeneration)) listener.onReady()
                        }
                        startAudio(webSocket, currentGeneration, listener)
                    }
                    "PARTIAL" -> publish(currentGeneration) {
                        listener.onPartial(data?.optString("text").orEmpty())
                    }
                    "FINAL" -> {
                        val result = data?.optString("text").orEmpty()
                        cancelled.set(true)
                        stopAudio()
                        webSocket.send("FINISH")
                        publish(currentGeneration) {
                            listener.onSpeechEnded()
                            listener.onResult(result)
                        }
                    }
                    "COMPLETE" -> {
                        cancelled.set(true)
                        stopAudio()
                        webSocket.close(1000, "recognition complete")
                    }
                    "ERROR" -> fail(
                        currentGeneration,
                        listener,
                        data?.optString("message").orEmpty().ifBlank {
                            "云端语音识别暂时不可用。"
                        })
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                fail(currentGeneration, listener, "云端识别连接失败，正在切换设备识别…")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (isCurrent(currentGeneration) && !cancelled.get()) {
                    fail(currentGeneration, listener, "云端识别连接已断开，正在切换设备识别…")
                }
            }
        })
    }

    override fun cancel() {
        cancelled.set(true)
        generation++
        stopAudio()
        socket?.close(1000, "client closing")
        socket = null
    }

    override fun release() = cancel()

    private fun startAudio(
        webSocket: WebSocket,
        currentGeneration: Int,
        listener: SpeechRecognitionProvider.Listener
    ) {
        if (!isCurrent(currentGeneration) || audioThread != null) return
        val minimum = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minimum <= 0) {
            fail(currentGeneration, listener, "设备无法创建录音缓冲区。")
            return
        }
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minimum, FRAME_BYTES * 2)
            )
        } catch (_: SecurityException) {
            fail(currentGeneration, listener, "没有麦克风权限，请在系统设置中允许。")
            return
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            fail(currentGeneration, listener, "麦克风初始化失败。")
            return
        }
        recorder = record
        audioThread = Thread({
            try {
                record.startRecording()
                publish(currentGeneration) { listener.onSpeechStarted() }
                val frame = ByteArray(FRAME_BYTES)
                while (isCurrent(currentGeneration) && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val count = record.read(frame, 0, frame.size)
                    if (count > 0 && !webSocket.send(frame.toByteString(0, count))) {
                        throw IllegalStateException("audio websocket queue is full")
                    }
                }
            } catch (_: Exception) {
                if (isCurrent(currentGeneration)) {
                    fail(currentGeneration, listener, "录音传输失败，正在切换设备识别…")
                }
            } finally {
                runCatching { record.stop() }
                record.release()
                if (recorder === record) recorder = null
                audioThread = null
            }
        }, "tingjian-cloud-asr").also { it.start() }
    }

    private fun stopAudio() {
        val active = recorder
        recorder = null
        runCatching { active?.stop() }
        audioThread?.interrupt()
        audioThread = null
    }

    private fun fail(
        currentGeneration: Int,
        listener: SpeechRecognitionProvider.Listener,
        message: String
    ) {
        if (!isCurrent(currentGeneration)) return
        cancelled.set(true)
        stopAudio()
        socket?.cancel()
        socket = null
        publish(currentGeneration) { listener.onFailure(cloudFailure(message)) }
    }

    private fun publish(currentGeneration: Int, action: () -> Unit) {
        main.post { if (currentGeneration == generation) action() }
    }

    private fun isCurrent(currentGeneration: Int): Boolean =
        currentGeneration == generation && !cancelled.get()

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val FRAME_BYTES = 3_200 // 100 ms, mono PCM 16-bit at 16 kHz.
    }
}

internal fun cloudAsrUrl(baseUrl: String, language: String): String =
    baseUrl.replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://") +
        "ws/v1/speech/asr?language=" +
        URLEncoder.encode(language, StandardCharsets.UTF_8)

private fun cloudFailure(message: String) = RecognitionFailure(
    kind = RecognitionFailureKind.NETWORK,
    code = -100,
    retryDelayMillis = 1_500L,
    message = message
)
