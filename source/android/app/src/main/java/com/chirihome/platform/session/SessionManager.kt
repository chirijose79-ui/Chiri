package com.chirihome.platform.session

import com.chirihome.platform.storage.SessionStorage

class SessionManager(
    private val sessionStorage: SessionStorage
) {

    suspend fun isSessionValid(): Boolean {
        return !sessionStorage.getAccessToken().isNullOrBlank()
    }

    suspend fun clearSession() {
        sessionStorage.clearSession()
    }
}