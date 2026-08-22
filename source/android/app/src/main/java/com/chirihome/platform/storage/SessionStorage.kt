package com.chirihome.platform.storage

interface SessionStorage {

    suspend fun saveAccessToken(token: String)

    suspend fun getAccessToken(): String?

    suspend fun saveRefreshToken(token: String)

    suspend fun getRefreshToken(): String?

    suspend fun clearSession()
}