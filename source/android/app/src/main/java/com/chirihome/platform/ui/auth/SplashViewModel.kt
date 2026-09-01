package com.chirihome.platform.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirihome.platform.domain.auth.ValidateSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val validateSessionUseCase: ValidateSessionUseCase
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<SplashUiState>(SplashUiState.Loading)

    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        validateSession()
    }

    private fun validateSession() {
        viewModelScope.launch {
            val sessionIsValid = try {
                validateSessionUseCase()
            } catch (exception: Exception) {
                false
            }

            _uiState.value = if (sessionIsValid) {
                SplashUiState.Authenticated
            } else {
                SplashUiState.Unauthenticated
            }
        }
    }
}