package com.chirihome.platform.storage

interface SendspinCredentialStorage {

    suspend fun saveStaticPrivateKey(key: ByteArray)

    suspend fun getStaticPrivateKey(): ByteArray?

    suspend fun savePairingPsk(psk: ByteArray)

    suspend fun getPairingPsk(): ByteArray?

    suspend fun saveLongTermPsk(
        serverId: String,
        psk: ByteArray
    )

    suspend fun getLongTermPsk(
        serverId: String
    ): ByteArray?

    suspend fun saveServerStaticPublicKey(key: ByteArray)

    suspend fun getServerStaticPublicKey(): ByteArray?

    suspend fun clearCredentials()
}