package com.chirihome.platform.repository.auth

import com.chirihome.platform.network.CurrentUserResponse
import com.chirihome.platform.network.LoginResponse
import com.chirihome.platform.network.RefreshResponse

interface AuthRepository {

    suspend fun login(
        identifier: String,
        password: String
    ): LoginResponse

    suspend fun getCurrentUser(): CurrentUserResponse

    suspend fun refresh(
        refreshToken: String
    ): RefreshResponse

    suspend fun logout()
}