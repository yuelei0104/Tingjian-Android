package com.tingjian.app.network

import android.content.Context
import android.os.Build
import com.tingjian.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

internal class ClientInfoInterceptor(context: Context) : Interceptor {
    private val preferences = context.applicationContext.getSharedPreferences(
        "tingjian_install", Context.MODE_PRIVATE
    )
    private val deviceId: String = preferences.getString(KEY_DEVICE_ID, null)
        ?: UUID.randomUUID().toString().also {
            preferences.edit().putString(KEY_DEVICE_ID, it).apply()
        }

    override fun intercept(chain: Interceptor.Chain): Response {
        val deviceName = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .take(80)
        val request = chain.request().newBuilder()
            .header("X-Device-Id", deviceId)
            .header("X-Device-Name", deviceName)
            .header("X-Platform", "Android ${Build.VERSION.RELEASE}".take(40))
            .header("X-App-Version", BuildConfig.VERSION_NAME.take(24))
            .build()
        return chain.proceed(request)
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
    }
}
