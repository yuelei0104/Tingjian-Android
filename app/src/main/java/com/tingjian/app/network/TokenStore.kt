package com.tingjian.app.network

import android.content.Context

internal interface TokenProvider {
    fun accessToken(): String?
    fun refreshToken(): String?
}

class TokenStore(context: Context) : TokenProvider {
    private val preferences = context.applicationContext.getSharedPreferences(
        "tingjian_auth", Context.MODE_PRIVATE
    )

    override fun accessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

    override fun refreshToken(): String? = preferences.getString(KEY_REFRESH_TOKEN, null)

    fun displayName(): String? = preferences.getString(KEY_DISPLAY_NAME, null)

    fun email(): String? = preferences.getString(KEY_EMAIL, null)

    fun isLoggedIn(): Boolean = !accessToken().isNullOrBlank() && !refreshToken().isNullOrBlank()

    fun save(response: AuthTokenResponse) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, response.accessToken)
            .putString(KEY_REFRESH_TOKEN, response.refreshToken)
            .putString(KEY_ACCESS_EXPIRES_AT, response.accessExpiresAt)
            .putString(KEY_REFRESH_EXPIRES_AT, response.refreshExpiresAt)
            .putString(KEY_USER_ID, response.user.id)
            .putString(KEY_EMAIL, response.user.email)
            .putString(KEY_DISPLAY_NAME, response.user.displayName)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
        const val KEY_REFRESH_EXPIRES_AT = "refresh_expires_at"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "display_name"
    }
}
