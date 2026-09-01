package com.chirihome.platform.domain.auth

import com.chirihome.platform.repository.auth.AuthRepository

class ValidateSessionUseCase(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(): Boolean {
        return try {
            authRepository.getCurrentUser()
            true
        } catch (exception: Exception) {
            false
        }
    }
}