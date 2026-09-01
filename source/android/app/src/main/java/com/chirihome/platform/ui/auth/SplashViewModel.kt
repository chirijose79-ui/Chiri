package com.chirihome.platform.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirihome.platform.domain.auth.SessionValidationResult
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

    fun validateSession() {
        viewModelScope.launch {
            when (val result = validateSessionUseCase()) {

                SessionValidationResult.Authenticated -> {
                    _uiState.value = SplashUiState.Authenticated
                }

                SessionValidationResult.Unauthenticated -> {
                    _uiState.value = SplashUiState.Unauthenticated
                }

                is SessionValidationResult.Error -> {
                    _uiState.value = SplashUiState.Error(
                        result.exception
                    )
                }
            }
        }
    }
}