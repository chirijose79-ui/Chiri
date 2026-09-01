package com.chirihome.platform.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chirihome.platform.domain.auth.LoginUseCase
import com.chirihome.platform.session.SessionManager

class LoginViewModelFactory(
    private val loginUseCase: LoginUseCase,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                loginUseCase = loginUseCase,
                sessionManager = sessionManager
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}