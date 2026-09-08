package com.chirihome.platform.player.music.sendspin.crypto

import com.chirihome.platform.storage.SendspinCredentialStorage

class SendspinIdentityProvider(
    private val storage: SendspinCredentialStorage,
    private val crypto: NoiseCrypto
) {

    suspend fun getOrCreate(): SendspinIdentity {
        val storedPrivateKey =
            storage.getStaticPrivateKey()

        if (storedPrivateKey != null) {
            return SendspinIdentity.fromPrivateKey(
                privateKey = storedPrivateKey,
                crypto = crypto
            )
        }

        val identity =
            SendspinIdentity.generate(crypto)

        storage.saveStaticPrivateKey(
            identity.staticPrivateKey
        )

        return identity
    }
}