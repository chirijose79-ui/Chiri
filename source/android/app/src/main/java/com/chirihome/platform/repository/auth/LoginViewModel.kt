package com.chirihome.platform.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirihome.platform.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameOrEmailChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            usernameOrEmail = value,
            errorMessage = null
        )
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            password = value,
            errorMessage = null
        )
    }

    fun onPasswordVisibilityChanged() {
        _uiState.value = _uiState.value.copy(
            passwordVisible = !_uiState.value.passwordVisible
        )
    }

    fun login(
        onSuccess: () -> Unit
    ) {
        val currentState = _uiState.value

        if (
            currentState.usernameOrEmail.isBlank() ||
            currentState.password.isBlank()
        ) {
            _uiState.value = currentState.copy(
                errorMessage = "Ingresa usuario y contraseña."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val response = sessionManager.login(
                    identifier = currentState.usernameOrEmail.trim(),
                    password = currentState.password
                )

                sessionManager.saveSession(
                    accessToken = response.access_token,
                    refreshToken = response.refresh_token
                )

                onSuccess()
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No se pudo iniciar sesión."
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    isLoading = false
                )
            }
        }
    }
}