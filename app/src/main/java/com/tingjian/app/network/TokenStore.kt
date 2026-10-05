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
    private val cipher = TokenCipher()

    init {
        migratePlaintextToken(KEY_ACCESS_TOKEN)
        migratePlaintextToken(KEY_REFRESH_TOKEN)
    }

    override fun accessToken(): String? = readToken(KEY_ACCESS_TOKEN)

    override fun refreshToken(): String? = readToken(KEY_REFRESH_TOKEN)

    fun displayName(): String? = preferences.getString(KEY_DISPLAY_NAME, null)

    fun email(): String? = preferences.getString(KEY_EMAIL, null)

    fun isLoggedIn(): Boolean = !accessToken().isNullOrBlank() && !refreshToken().isNullOrBlank()

    fun save(response: AuthTokenResponse) {
        val encryptedAccess = cipher.encrypt(response.accessToken)
        val encryptedRefresh = cipher.encrypt(response.refreshToken)
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, encryptedAccess)
            .putString(KEY_REFRESH_TOKEN, encryptedRefresh)
            .putString(KEY_ACCESS_EXPIRES_AT, response.accessExpiresAt)
            .putString(KEY_REFRESH_EXPIRES_AT, response.refreshExpiresAt)
            .putString(KEY_USER_ID, response.user.id)
            .putString(KEY_EMAIL, response.user.email)
            .putString(KEY_DISPLAY_NAME, response.user.displayName)
            .apply()
    }

    fun updateProfile(user: AuthUserResponse) {
        preferences.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_DISPLAY_NAME, user.displayName)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun readToken(key: String): String? {
        val stored = preferences.getString(key, null) ?: return null
        return runCatching { cipher.decrypt(stored) }
            .onFailure { clear() }
            .getOrNull()
    }

    private fun migratePlaintextToken(key: String) {
        val stored = preferences.getString(key, null) ?: return
        if (stored.startsWith("v1:")) return
        runCatching { cipher.encrypt(stored) }
            .onSuccess { preferences.edit().putString(key, it).apply() }
            .onFailure { clear() }
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
