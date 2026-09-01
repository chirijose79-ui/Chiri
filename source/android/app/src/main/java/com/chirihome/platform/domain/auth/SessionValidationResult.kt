package com.chirihome.platform.domain.auth

sealed interface SessionValidationResult {
    data object Authenticated : SessionValidationResult
    data object Unauthenticated : SessionValidationResult
    data class Error(
        val exception: Exception
    ) : SessionValidationResult
}