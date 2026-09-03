package com.chirihome.platform.network

import android.util.Log
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

        Log.d(TAG, "========== AUTHENTICATOR ==========")
        Log.d(TAG, "401 recibido")
        Log.d(TAG, "URL: ${response.request.url}")
        Log.d(TAG, "Intento: ${responseCount(response)}")

        if (responseCount(response) >= 2) {
            Log.d(
                TAG,
                "Máximo de reintentos alcanzado"
            )
            return null
        }

        return synchronized(this) {

            Log.d(
                TAG,
                "Entrando en sección sincronizada de refresh"
            )

            val refreshToken = runBlocking {
                sessionStorage.getRefreshToken()
            }

            Log.d(
                TAG,
                "Refresh Token disponible: ${!refreshToken.isNullOrBlank()}"
            )

            if (refreshToken.isNullOrBlank()) {
                Log.d(
                    TAG,
                    "No existe Refresh Token"
                )
                return@synchronized null
            }

            val currentAccessToken = runBlocking {
                sessionStorage.getAccessToken()
            }

            val failedAccessToken =
                response.request.header("Authorization")
                    ?.removePrefix("Bearer ")
                    ?.trim()

            Log.d(
                TAG,
                "Access Token actual disponible: ${
                    !currentAccessToken.isNullOrBlank()
                }"
            )

            Log.d(
                TAG,
                "Access Token fallido disponible: ${
                    !failedAccessToken.isNullOrBlank()
                }"
            )

            /*
             * Otra petición pudo haber hecho refresh mientras
             * esta petición esperaba.
             *
             * Si el token almacenado ya cambió, reutilizamos
             * el token actualizado y evitamos otro refresh.
             */
            if (
                !currentAccessToken.isNullOrBlank() &&
                currentAccessToken != failedAccessToken
            ) {

                Log.d(
                    TAG,
                    "El Access Token ya cambió; reutilizando token actualizado"
                )

                return@synchronized response.request
                    .newBuilder()
                    .header(
                        "Authorization",
                        "Bearer $currentAccessToken"
                    )
                    .build()
            }

            try {

                Log.d(
                    TAG,
                    "Ejecutando POST /auth/refresh"
                )

                val refreshResponse = runBlocking {
                    refreshApi.refresh(
                        RefreshRequest(
                            refresh_token = refreshToken
                        )
                    )
                }

                Log.d(
                    TAG,
                    "Refresh exitoso"
                )

                runBlocking {
                    sessionStorage.saveAccessToken(
                        refreshResponse.access_token
                    )

                    sessionStorage.saveRefreshToken(
                        refreshResponse.refresh_token
                    )
                }

                Log.d(
                    TAG,
                    "Tokens actualizados en SessionStorage"
                )

                val retryRequest = response.request
                    .newBuilder()
                    .header(
                        "Authorization",
                        "Bearer ${refreshResponse.access_token}"
                    )
                    .build()

                Log.d(
                    TAG,
                    "Retry construido con nuevo Access Token"
                )

                retryRequest

            } catch (exception: Exception) {

                Log.e(
                    TAG,
                    "Refresh fallido: ${
                        exception::class.simpleName
                    }: ${exception.message}"
                )

                runBlocking {
                    sessionStorage.clearSession()
                }

                Log.d(
                    TAG,
                    "Sesión limpiada"
                )

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
        private const val TAG = "ChiriAuth"

        private const val BASE_URL =
            "http://192.168.1.88:8000/"
    }
}