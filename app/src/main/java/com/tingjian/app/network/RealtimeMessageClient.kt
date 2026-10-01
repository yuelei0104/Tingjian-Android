package com.tingjian.app.network

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicBoolean

class RealtimeMessageClient internal constructor(
    private val client: OkHttpClient,
    baseUrl: String,
    private val tokenStore: TokenStore
) {
    interface Listener {
        fun onStateChanged(state: State)
        fun onEvent(event: RealtimeMessageEvent)
    }

    enum class State { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING }

    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val webSocketUrl = baseUrl
        .replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://") + "ws/realtime"
    private val opening = AtomicBoolean(false)
    @Volatile private var socket: WebSocket? = null
    @Volatile private var listener: Listener? = null
    private var reconnectAttempt = 0
    @Volatile private var manuallyClosed = true
    @Volatile private var generation = 0

    fun connect(listener: Listener) {
        this.listener = listener
        manuallyClosed = false
        generation++
        mainHandler.removeCallbacksAndMessages(RECONNECT_TOKEN)
        open(reconnecting = reconnectAttempt > 0, generation = generation)
    }

    fun reconnect() {
        manuallyClosed = false
        generation++
        mainHandler.removeCallbacksAndMessages(RECONNECT_TOKEN)
        socket?.cancel()
        socket = null
        opening.set(false)
        open(reconnecting = true, generation = generation)
    }

    fun disconnect() {
        manuallyClosed = true
        generation++
        mainHandler.removeCallbacksAndMessages(RECONNECT_TOKEN)
        socket?.close(1000, "client closing")
        socket = null
        opening.set(false)
        publishState(State.DISCONNECTED)
    }

    fun send(request: RealtimeMessageRequest): Boolean {
        val current = socket ?: return false
        return current.send(gson.toJson(request))
    }

    private fun open(reconnecting: Boolean, generation: Int) {
        if (manuallyClosed || generation != this.generation || socket != null ||
            !opening.compareAndSet(false, true)) return
        val token = tokenStore.accessToken()
        if (token.isNullOrBlank()) {
            opening.set(false)
            publishState(State.DISCONNECTED)
            return
        }
        publishState(if (reconnecting) State.RECONNECTING else State.CONNECTING)
        val request = Request.Builder()
            .url(webSocketUrl)
            .header("Authorization", "Bearer $token")
            .build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (generation != this@RealtimeMessageClient.generation) {
                    webSocket.cancel()
                    return
                }
                socket = webSocket
                opening.set(false)
                reconnectAttempt = 0
                mainHandler.removeCallbacksAndMessages(RECONNECT_TOKEN)
                publishState(State.CONNECTED)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (generation != this@RealtimeMessageClient.generation) return
                runCatching { gson.fromJson(text, RealtimeMessageEvent::class.java) }
                    .getOrNull()?.let { event ->
                        mainHandler.post {
                            if (generation == this@RealtimeMessageClient.generation) {
                                listener?.onEvent(event)
                            }
                        }
                    }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (generation != this@RealtimeMessageClient.generation) return
                socket = null
                opening.set(false)
                if (manuallyClosed) publishState(State.DISCONNECTED)
                else scheduleReconnect(generation)
            }

            override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) {
                if (generation != this@RealtimeMessageClient.generation) return
                socket = null
                opening.set(false)
                if (!manuallyClosed) scheduleReconnect(generation)
            }
        })
    }

    private fun scheduleReconnect(generation: Int) {
        publishState(State.RECONNECTING)
        val delays = longArrayOf(1_000L, 2_000L, 5_000L, 10_000L, 30_000L)
        val delay = delays[reconnectAttempt.coerceAtMost(delays.lastIndex)]
        reconnectAttempt++
        mainHandler.removeCallbacksAndMessages(RECONNECT_TOKEN)
        mainHandler.postAtTime(
            { open(reconnecting = true, generation = generation) },
            RECONNECT_TOKEN,
            android.os.SystemClock.uptimeMillis() + delay
        )
    }

    private fun publishState(state: State) {
        mainHandler.post { listener?.onStateChanged(state) }
    }

    private companion object {
        val RECONNECT_TOKEN = Any()
    }
}
