package com.chirihome.platform.network

import com.chirihome.platform.storage.SessionStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthAuthenticatorTest {

    private val oldAccessToken = "old-access-token"
    private val oldRefreshToken = "old-refresh-token"

    private val newAccessToken = "new-access-token"
    private val newRefreshToken = "new-refresh-token"

    @Test
    fun `401 con refresh exitoso devuelve request con nuevo access token`() {

        val storage = FakeSessionStorage(
            accessToken = oldAccessToken,
            refreshToken = oldRefreshToken
        )

        val refreshApi = FakeAuthApi(
            refreshResponse = RefreshResponse(
                access_token = newAccessToken,
                refresh_token = newRefreshToken,
                token_type = "bearer",
                session_id = "test-session",
                user_id = "test-user",
                expires_at = "2099-01-01T00:00:00Z"
            )
        )

        val authenticator = AuthAuthenticator(
            sessionStorage = storage,
            refreshApi = refreshApi
        )

        val response = unauthorizedResponse(
            authorization = "Bearer $oldAccessToken"
        )

        val result = authenticator.authenticate(
            route = null,
            response = response
        )

        assertNotNull(result)

        assertEquals(
            "Bearer $newAccessToken",
            result?.header("Authorization")
        )

        assertEquals(
            newAccessToken,
            runBlocking {
                storage.getAccessToken()
            }
        )

        assertEquals(
            newRefreshToken,
            runBlocking {
                storage.getRefreshToken()
            }
        )

        assertEquals(
            1,
            refreshApi.refreshCalls
        )
    }

    @Test
    fun `401 sin refresh token devuelve null`() {

        val storage = FakeSessionStorage(
            accessToken = oldAccessToken,
            refreshToken = null
        )

        val refreshApi = FakeAuthApi()

        val authenticator = AuthAuthenticator(
            sessionStorage = storage,
            refreshApi = refreshApi
        )

        val response = unauthorizedResponse(
            authorization = "Bearer $oldAccessToken"
        )

        val result = authenticator.authenticate(
            route = null,
            response = response
        )

        assertNull(result)

        assertEquals(
            0,
            refreshApi.refreshCalls
        )
    }

    @Test
    fun `refresh fallido limpia la sesion y devuelve null`() {

        val storage = FakeSessionStorage(
            accessToken = oldAccessToken,
            refreshToken = oldRefreshToken
        )

        val refreshApi = FakeAuthApi(
            refreshException = RuntimeException("refresh failed")
        )

        val authenticator = AuthAuthenticator(
            sessionStorage = storage,
            refreshApi = refreshApi
        )

        val response = unauthorizedResponse(
            authorization = "Bearer $oldAccessToken"
        )

        val result = authenticator.authenticate(
            route = null,
            response = response
        )

        assertNull(result)

        assertNull(
            runBlocking {
                storage.getAccessToken()
            }
        )

        assertNull(
            runBlocking {
                storage.getRefreshToken()
            }
        )

        assertEquals(
            1,
            refreshApi.refreshCalls
        )
    }

    @Test
    fun `si el access token ya cambio reutiliza el nuevo token sin hacer refresh`() {

        val storage = FakeSessionStorage(
            accessToken = newAccessToken,
            refreshToken = oldRefreshToken
        )

        val refreshApi = FakeAuthApi(
            refreshResponse = RefreshResponse(
                access_token = "should-not-be-used",
                refresh_token = "should-not-be-used",
                token_type = "bearer",
                session_id = "should-not-be-used",
                user_id = "should-not-be-used",
                expires_at = "2099-01-01T00:00:00Z"
            )
        )

        val authenticator = AuthAuthenticator(
            sessionStorage = storage,
            refreshApi = refreshApi
        )

        val response = unauthorizedResponse(
            authorization = "Bearer $oldAccessToken"
        )

        val result = authenticator.authenticate(
            route = null,
            response = response
        )

        assertNotNull(result)

        assertEquals(
            "Bearer $newAccessToken",
            result?.header("Authorization")
        )

        assertEquals(
            0,
            refreshApi.refreshCalls
        )
    }

    @Test
    fun `segundo intento devuelve null`() {

        val storage = FakeSessionStorage(
            accessToken = oldAccessToken,
            refreshToken = oldRefreshToken
        )

        val refreshApi = FakeAuthApi()

        val authenticator = AuthAuthenticator(
            sessionStorage = storage,
            refreshApi = refreshApi
        )

        val firstResponse = unauthorizedResponse(
            authorization = "Bearer $oldAccessToken"
        )

        val secondResponse = unauthorizedResponse(
            authorization = "Bearer $oldAccessToken",
            priorResponse = firstResponse
        )

        val result = authenticator.authenticate(
            route = null,
            response = secondResponse
        )

        assertNull(result)

        assertEquals(
            0,
            refreshApi.refreshCalls
        )
    }

    private fun unauthorizedResponse(
        authorization: String,
        priorResponse: Response? = null
    ): Response {

        val request = Request.Builder()
            .url("http://localhost/auth/me")
            .header("Authorization", authorization)
            .build()

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody(null))
            .apply {
                if (priorResponse != null) {
                    priorResponse(priorResponse)
                }
            }
            .build()
    }

    private class FakeSessionStorage(
        private var accessToken: String?,
        private var refreshToken: String?
    ) : SessionStorage {

        override suspend fun saveAccessToken(token: String) {
            accessToken = token
        }

        override suspend fun getAccessToken(): String? {
            return accessToken
        }

        override suspend fun saveRefreshToken(token: String) {
            refreshToken = token
        }

        override suspend fun getRefreshToken(): String? {
            return refreshToken
        }

        override suspend fun clearSession() {
            accessToken = null
            refreshToken = null
        }
    }

    private class FakeAuthApi(
        private val refreshResponse: RefreshResponse? = null,
        private val refreshException: Exception? = null
    ) : AuthApi {

        var refreshCalls: Int = 0

        override suspend fun login(
            request: LoginRequest
        ): LoginResponse {
            throw UnsupportedOperationException()
        }

        override suspend fun me(): CurrentUserResponse {
            throw UnsupportedOperationException()
        }

        override suspend fun refresh(
            request: RefreshRequest
        ): RefreshResponse {

            refreshCalls++

            refreshException?.let {
                throw it
            }

            return refreshResponse
                ?: throw IllegalStateException(
                    "FakeAuthApi.refresh no configurado"
                )
        }

        override suspend fun logout(): LogoutResponse {
            throw UnsupportedOperationException()
        }
    }
}