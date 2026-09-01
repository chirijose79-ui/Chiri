package com.chirihome.platform.network

data class LoginRequest(
    val identifier: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val refresh_token: String,
    val session_id: String,
    val user_id: String,
    val expires_at: String
)

data class CurrentUserResponse(
    val user_id: String,
    val username: String,
    val email: String
)

data class RefreshRequest(
    val refresh_token: String
)

data class RefreshResponse(
    val access_token: String,
    val token_type: String,
    val refresh_token: String,
    val session_id: String,
    val user_id: String,
    val expires_at: String
)