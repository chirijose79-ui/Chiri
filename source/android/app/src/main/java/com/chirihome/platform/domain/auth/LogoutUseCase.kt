package com.chirihome.platform.domain.auth

import com.chirihome.platform.repository.auth.AuthRepository

class LogoutUseCase(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke() {
        authRepository.logout()
    }
}