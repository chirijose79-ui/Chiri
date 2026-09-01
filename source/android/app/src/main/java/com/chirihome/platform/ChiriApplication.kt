package com.chirihome.platform

import android.app.Application
import com.chirihome.platform.network.ApiClient
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

        sessionManager = SessionManager(
            sessionStorage = sessionStorage,
            apiClient = apiClient
        )
    }
}