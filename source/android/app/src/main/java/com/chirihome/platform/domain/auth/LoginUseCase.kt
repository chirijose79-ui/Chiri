package com.chirihome.platform.domain.auth

import com.chirihome.platform.network.LoginResponse
import com.chirihome.platform.repository.auth.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(
        identifier: String,
        password: String
    ): LoginResponse {
        return authRepository.login(
            identifier = identifier,
            password = password
        )
    }
}