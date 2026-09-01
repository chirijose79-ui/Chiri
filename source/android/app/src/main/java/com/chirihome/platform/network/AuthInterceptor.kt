package com.chirihome.platform.network

import com.chirihome.platform.storage.SessionStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionStorage: SessionStorage
) : Interceptor {

    override fun intercept(
        chain: Interceptor.Chain
    ): Response {
        val originalRequest = chain.request()

        val accessToken = runBlocking {
            sessionStorage.getAccessToken()
        }

        if (accessToken.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header(
                "Authorization",
                "Bearer $accessToken"
            )
            .build()

        return chain.proceed(authenticatedRequest)
    }
}