package com.chirihome.platform.network

import com.chirihome.platform.storage.SessionStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthAuthenticator(
    private val sessionStorage: SessionStorage
) : Authenticator {

    private val refreshApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    override fun authenticate(
        route: Route?,
        response: Response
    ): Request? {

        if (responseCount(response) >= 2) {
            return null
        }

        val refreshToken = runBlocking {
            sessionStorage.getRefreshToken()
        }

        if (refreshToken.isNullOrBlank()) {
            return null
        }

        return synchronized(this) {

            val currentAccessToken = runBlocking {
                sessionStorage.getAccessToken()
            }

            val failedAccessToken =
                response.request.header("Authorization")
                    ?.removePrefix("Bearer ")
                    ?.trim()

            /*
             * Otra petición pudo haber hecho refresh mientras
             * esta petición esperaba.
             *
             * Si el token almacenado ya cambió, simplemente
             * usamos el nuevo token y NO hacemos otro refresh.
             */
            if (
                !currentAccessToken.isNullOrBlank() &&
                currentAccessToken != failedAccessToken
            ) {
                return@synchronized response.request
                    .newBuilder()
                    .header(
                        "Authorization",
                        "Bearer $currentAccessToken"
                    )
                    .build()
            }

            try {
                val refreshResponse = runBlocking {
                    refreshApi.refresh(
                        RefreshRequest(
                            refresh_token = refreshToken
                        )
                    )
                }

                runBlocking {
                    sessionStorage.saveAccessToken(
                        refreshResponse.access_token
                    )

                    sessionStorage.saveRefreshToken(
                        refreshResponse.refresh_token
                    )
                }

                response.request
                    .newBuilder()
                    .header(
                        "Authorization",
                        "Bearer ${refreshResponse.access_token}"
                    )
                    .build()

            } catch (exception: Exception) {

                runBlocking {
                    sessionStorage.clearSession()
                }

                null
            }
        }
    }

    private fun responseCount(
        response: Response
    ): Int {
        var count = 1
        var priorResponse = response.priorResponse

        while (priorResponse != null) {
            count++
            priorResponse = priorResponse.priorResponse
        }

        return count
    }

    companion object {
        private const val BASE_URL =
            "http://192.168.1.88:8000/"
    }
}