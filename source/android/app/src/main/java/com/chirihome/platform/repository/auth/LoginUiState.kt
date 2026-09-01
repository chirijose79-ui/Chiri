package com.chirihome.platform.ui.auth

data class LoginUiState(
    val usernameOrEmail: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)