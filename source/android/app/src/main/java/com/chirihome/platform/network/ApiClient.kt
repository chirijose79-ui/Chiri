package com.chirihome.platform.network

import com.chirihome.platform.storage.SessionStorage
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient(
    sessionStorage: SessionStorage
) {

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(
            AuthInterceptor(sessionStorage)
        )
        .authenticator(
            AuthAuthenticator(sessionStorage)
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi =
        retrofit.create(AuthApi::class.java)

    companion object {
        private const val BASE_URL =
            "https://api.chirihome.com/api/"
    }
}