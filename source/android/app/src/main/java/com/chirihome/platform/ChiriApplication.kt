package com.chirihome.platform

import android.app.Application
import com.chirihome.platform.domain.auth.LoginUseCase
import com.chirihome.platform.domain.auth.LogoutUseCase
import com.chirihome.platform.domain.auth.ValidateSessionUseCase
import com.chirihome.platform.domain.home.HomeUseCase
import com.chirihome.platform.domain.music.MusicUseCase
import com.chirihome.platform.network.ApiClient
import com.chirihome.platform.player.music.sendspin.SendspinManager
import com.chirihome.platform.repository.auth.AuthRepositoryImpl
import com.chirihome.platform.repository.home.HomeRepositoryImpl
import com.chirihome.platform.repository.music.MusicRepositoryImpl
import com.chirihome.platform.session.SessionManager
import com.chirihome.platform.storage.SecureSessionStorage

class ChiriApplication : Application() {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var loginUseCase: LoginUseCase
        private set

    lateinit var validateSessionUseCase: ValidateSessionUseCase
        private set

    lateinit var logoutUseCase: LogoutUseCase
        private set

    lateinit var homeUseCase: HomeUseCase
        private set

    lateinit var musicUseCase: MusicUseCase
        private set

    lateinit var sendspinManager: SendspinManager
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

        val homeRepository = HomeRepositoryImpl(
            homeApi = apiClient.homeApi
        )

        val musicRepository = MusicRepositoryImpl(
            musicApi = apiClient.musicApi
        )

        sessionManager = SessionManager(
            sessionStorage = sessionStorage
        )

        loginUseCase = LoginUseCase(
            authRepository = authRepository
        )

        validateSessionUseCase = ValidateSessionUseCase(
            authRepository = authRepository
        )

        logoutUseCase = LogoutUseCase(
            authRepository = authRepository
        )

        homeUseCase = HomeUseCase(
            homeRepository = homeRepository
        )

        musicUseCase = MusicUseCase(
            musicRepository = musicRepository
        )

        sendspinManager = SendspinManager(this)
        sendspinManager.start()
    }
}