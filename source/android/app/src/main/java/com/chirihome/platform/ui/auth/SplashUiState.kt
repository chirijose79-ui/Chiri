package com.chirihome.platform.ui.auth

sealed interface SplashUiState {

    data object Loading : SplashUiState

    data object Authenticated : SplashUiState

    data object Unauthenticated : SplashUiState
}