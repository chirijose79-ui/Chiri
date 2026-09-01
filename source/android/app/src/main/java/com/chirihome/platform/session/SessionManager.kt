package com.chirihome.platform.session

import com.chirihome.platform.storage.SessionStorage

class SessionManager(
    private val sessionStorage: SessionStorage
) {

    suspend fun isSessionValid(): Boolean {
        return !sessionStorage.getAccessToken().isNullOrBlank()
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String
    ) {
        sessionStorage.saveAccessToken(accessToken)
        sessionStorage.saveRefreshToken(refreshToken)
    }

    suspend fun getAccessToken(): String? {
        return sessionStorage.getAccessToken()
    }

    suspend fun getRefreshToken(): String? {
        return sessionStorage.getRefreshToken()
    }

    suspend fun clearSession() {
        sessionStorage.clearSession()
    }
}
