package com.tingjian.app.network

import android.content.Context
import com.tingjian.app.BuildConfig
import com.tingjian.app.data.TingjianRepository
import com.tingjian.app.speech.AndroidSpeechRecognitionProvider
import com.tingjian.app.speech.CloudSpeechRecognitionProvider
import com.tingjian.app.speech.FallbackSpeechRecognitionProvider
import com.tingjian.app.speech.SpeechRecognitionProvider
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    @Volatile
    private var initialized = false

    lateinit var tokenStore: TokenStore
        private set

    lateinit var repository: TingjianRepository
        private set

    lateinit var realtimeClient: RealtimeMessageClient
        private set

    private lateinit var authenticatedClient: OkHttpClient
    private lateinit var apiBaseUrl: String

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            tokenStore = TokenStore(context)
            val baseUrl = normalizedBaseUrl(BuildConfig.API_BASE_URL)
            apiBaseUrl = baseUrl
            val converter = GsonConverterFactory.create()

            val refreshClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
            val refreshApi = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(refreshClient)
                .addConverterFactory(converter)
                .build()
                .create(TokenRefreshApi::class.java)

            val client = OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(RequestIdInterceptor())
                .addInterceptor(ClientInfoInterceptor(context))
                .addInterceptor(AuthHeaderInterceptor(tokenStore))
                .authenticator(AccessTokenAuthenticator(tokenStore, refreshApi))
                .apply {
                    if (BuildConfig.DEBUG) {
                        addInterceptor(HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                            redactHeader("Authorization")
                        })
                    }
                }
                .build()
            authenticatedClient = client

            val api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(converter)
                .build()
                .create(TingjianApi::class.java)

            repository = TingjianRepository(api, tokenStore)
            realtimeClient = RealtimeMessageClient(client, baseUrl, tokenStore)
            initialized = true
        }
    }

    fun speechRecognitionProvider(context: Context): SpeechRecognitionProvider {
        val device = AndroidSpeechRecognitionProvider(context)
        if (!initialized || !BuildConfig.CLOUD_ASR_ENABLED) return device
        val cloud = CloudSpeechRecognitionProvider(authenticatedClient, apiBaseUrl, tokenStore)
        return FallbackSpeechRecognitionProvider(cloud, device)
    }

    internal fun diagnostics(): BackendEndpointDiagnostics = backendEndpointDiagnostics(
        if (initialized) apiBaseUrl else normalizedBaseUrl(BuildConfig.API_BASE_URL),
        BuildConfig.CLOUD_ASR_ENABLED
    )

    private fun normalizedBaseUrl(value: String): String {
        val trimmed = value.trim()
        require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            "TINGJIAN_API_BASE_URL must start with http:// or https://"
        }
        return if (trimmed.endsWith('/')) trimmed else "$trimmed/"
    }
}
