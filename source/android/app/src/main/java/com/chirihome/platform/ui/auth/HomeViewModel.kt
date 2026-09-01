package com.chirihome.platform.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirihome.platform.domain.auth.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun logout(
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isLoggingOut) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoggingOut = true,
                logoutError = null
            )

            try {
                logoutUseCase()
                onSuccess()
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    logoutError = "No se pudo cerrar sesión."
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    isLoggingOut = false
                )
            }
        }
    }
}