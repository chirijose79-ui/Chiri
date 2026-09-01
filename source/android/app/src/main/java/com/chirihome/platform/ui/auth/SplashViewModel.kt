package com.chirihome.platform.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirihome.platform.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<SplashUiState>(SplashUiState.Loading)

    val uiState: StateFlow<SplashUiState> =
        _uiState.asStateFlow()

    init {
        validateSession()
    }

    private fun validateSession() {
        viewModelScope.launch {
            val currentUser = sessionManager.getCurrentUser()

            if (currentUser != null) {
                _uiState.value = SplashUiState.Authenticated
            } else {
                sessionManager.clearSession()
                _uiState.value = SplashUiState.Unauthenticated
            }
        }
    }
}