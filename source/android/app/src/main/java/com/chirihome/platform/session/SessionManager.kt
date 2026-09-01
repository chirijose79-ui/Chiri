package com.chirihome.platform.session

import com.chirihome.platform.network.ApiClient
import com.chirihome.platform.network.CurrentUserResponse
import com.chirihome.platform.storage.SessionStorage

import android.util.Log

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

    suspend fun getCurrentUser(): CurrentUserResponse? {
        val accessToken = sessionStorage.getAccessToken()

        if (accessToken.isNullOrBlank()) {
            Log.e(
                "ChiriSession",
                "NO hay access token guardado"
            )
            return null
        }

        Log.e(
            "ChiriSession",
            "Access token encontrado. Longitud: ${accessToken.length}"
        )

        return try {
            val response = ApiClient.authApi.me(
                authorization = "Bearer $accessToken"
            )

            Log.e(
                "ChiriSession",
                "/auth/me exitoso: $response"
            )

            response
        } catch (exception: Exception) {
            Log.e(
                "ChiriSession",
                "Error en /auth/me",
                exception
            )
            null
        }
    }

    suspend fun clearSession() {
        sessionStorage.clearSession()
    }

    suspend fun refreshSession(): Boolean {
        val refreshToken = sessionStorage.getRefreshToken()
            ?: return false

        Log.e(
            "ChiriSession",
            "Refresh token encontrado. Longitud: ${refreshToken.length}"
        )

        return try {
            val response = ApiClient.authApi.refresh(
                com.chirihome.platform.network.RefreshRequest(
                    refresh_token = refreshToken
                )
            )

            android.util.Log.e(
                "ChiriSession",
                "Refresh exitoso. Nuevo access token: ${response.access_token.length} caracteres"
            )

            saveSession(
                accessToken = response.access_token,
                refreshToken = response.refresh_token
            )

            true
        } catch (exception: Exception) {
            android.util.Log.e(
                "ChiriSession",
                "Error durante refresh",
                exception
            )
            false
        }
    }
}