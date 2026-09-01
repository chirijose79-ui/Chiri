package com.chirihome.platform.ui.auth

data class HomeUiState(
    val isLoggingOut: Boolean = false,
    val logoutError: String? = null
)
