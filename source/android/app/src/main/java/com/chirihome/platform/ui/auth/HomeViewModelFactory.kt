package com.chirihome.platform.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chirihome.platform.domain.auth.LogoutUseCase
import com.chirihome.platform.domain.home.HomeUseCase

class HomeViewModelFactory(
    private val homeUseCase: HomeUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                homeUseCase = homeUseCase,
                logoutUseCase = logoutUseCase
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}