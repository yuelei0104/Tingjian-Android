package com.tingjian.app.network

import android.content.Context
import com.tingjian.app.BuildConfig
import com.tingjian.app.data.TingjianRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    @Volatile
    private var initialized = false

    lateinit var tokenStore: TokenStore
        private set

    lateinit var repository: TingjianRepository
        private set

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            tokenStore = TokenStore(context)
            val baseUrl = normalizedBaseUrl(BuildConfig.API_BASE_URL)
            val converter = GsonConverterFactory.create()

            val refreshClient = OkHttpClient.Builder().build()
            val refreshApi = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(refreshClient)
                .addConverterFactory(converter)
                .build()
                .create(TokenRefreshApi::class.java)

            val client = OkHttpClient.Builder()
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

            val api = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(converter)
                .build()
                .create(TingjianApi::class.java)

            repository = TingjianRepository(api, tokenStore)
            initialized = true
        }
    }

    private fun normalizedBaseUrl(value: String): String {
        val trimmed = value.trim()
        require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            "TINGJIAN_API_BASE_URL must start with http:// or https://"
        }
        return if (trimmed.endsWith('/')) trimmed else "$trimmed/"
    }
}
