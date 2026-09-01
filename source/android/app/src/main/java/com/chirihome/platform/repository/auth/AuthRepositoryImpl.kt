package com.chirihome.platform.repository.auth

import com.chirihome.platform.network.AuthApi
import com.chirihome.platform.network.CurrentUserResponse
import com.chirihome.platform.network.LoginRequest
import com.chirihome.platform.network.LoginResponse
import com.chirihome.platform.network.RefreshRequest
import com.chirihome.platform.network.RefreshResponse

class AuthRepositoryImpl(
    private val authApi: AuthApi
) : AuthRepository {

    override suspend fun login(
        identifier: String,
        password: String
    ): LoginResponse {
        return authApi.login(
            LoginRequest(
                identifier = identifier,
                password = password
            )
        )
    }

    override suspend fun getCurrentUser(): CurrentUserResponse {
        return authApi.me()
    }

    override suspend fun refresh(
        refreshToken: String
    ): RefreshResponse {
        return authApi.refresh(
            RefreshRequest(
                refresh_token = refreshToken
            )
        )
    }

    override suspend fun logout() {
        authApi.logout()
    }
}