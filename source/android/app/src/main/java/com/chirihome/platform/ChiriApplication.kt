package com.chirihome.platform

import android.app.Application
import com.chirihome.platform.network.ApiClient
import com.chirihome.platform.repository.auth.AuthRepositoryImpl
import com.chirihome.platform.session.SessionManager
import com.chirihome.platform.storage.SecureSessionStorage

class ChiriApplication : Application() {

    lateinit var sessionManager: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()

        val sessionStorage = SecureSessionStorage(this)

        val apiClient = ApiClient(
            sessionStorage = sessionStorage
        )

        val authRepository = AuthRepositoryImpl(
            authApi = apiClient.authApi
        )

        sessionManager = SessionManager(
            sessionStorage = sessionStorage,
            authRepository = authRepository
        )
    }
}