package com.chirihome.platform.ui.auth

import com.chirihome.platform.network.HomeResponse

data class HomeUiState(
    val isLoading: Boolean = true,
    val home: HomeResponse? = null,
    val error: String? = null,
    val isLoggingOut: Boolean = false,
    val logoutError: String? = null
)