package com.chirihome.platform.session

import com.chirihome.platform.network.CurrentUserResponse
import com.chirihome.platform.network.LoginResponse
import com.chirihome.platform.repository.auth.AuthRepository
import com.chirihome.platform.storage.SessionStorage

class SessionManager(
    private val sessionStorage: SessionStorage,
    private val authRepository: AuthRepository
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
            authRepository.getCurrentUser()
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
            val response = authRepository.refresh(refreshToken)

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
        return authRepository.login(
            identifier = identifier,
            password = password
        )
    }

    suspend fun logout() {
        try {
            authRepository.logout()
        } finally {
            sessionStorage.clearSession()
        }
    }
}