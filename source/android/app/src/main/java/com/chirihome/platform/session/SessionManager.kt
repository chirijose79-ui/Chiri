package com.chirihome.platform.session

import com.chirihome.platform.network.ApiClient
import com.chirihome.platform.network.CurrentUserResponse
import com.chirihome.platform.network.LoginResponse
import com.chirihome.platform.network.RefreshRequest
import com.chirihome.platform.storage.SessionStorage

class SessionManager(
    private val sessionStorage: SessionStorage,
    private val apiClient: ApiClient
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

    suspend fun getCurrentUser(): CurrentUserResponse? {
        return try {
            apiClient.authApi.me()
        } catch (exception: Exception) {
            null
        }
    }

    suspend fun clearSession() {
        sessionStorage.clearSession()
    }

    suspend fun refreshSession(): Boolean {
        val refreshToken = sessionStorage.getRefreshToken()
            ?: return false

        return try {
            val response = apiClient.authApi.refresh(
                RefreshRequest(
                    refresh_token = refreshToken
                )
            )

            saveSession(
                accessToken = response.access_token,
                refreshToken = response.refresh_token
            )

            true
        } catch (exception: Exception) {
            false
        }
    }

    suspend fun login(
        identifier: String,
        password: String
    ): LoginResponse {
        return apiClient.authApi.login(
            com.chirihome.platform.network.LoginRequest(
                identifier = identifier,
                password = password
            )
        )
    }

    suspend fun logout() {
        try {
            apiClient.authApi.logout()
        } finally {
            sessionStorage.clearSession()
        }
    }
}