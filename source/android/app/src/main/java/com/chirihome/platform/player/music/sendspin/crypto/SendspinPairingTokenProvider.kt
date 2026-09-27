package com.chirihome.platform.player.music.sendspin.crypto

import com.chirihome.platform.storage.SendspinCredentialStorage

class SendspinPairingTokenProvider(
    private val storage: SendspinCredentialStorage,
    private val crypto: NoiseCrypto
) {
    private val identityProvider =
        SendspinIdentityProvider(
            storage = storage,
            crypto = crypto
        )

    suspend fun getOrCreateToken(): String {
        val identity = identityProvider.getOrCreate()

        val pairingPsk =
            storage.getPairingPsk()
                ?: SendspinPsk.generatePairingPsk(crypto).also {
                    storage.savePairingPsk(it)
                }

        return SendspinPsk.createToken(
            identity = identity,
            pairingPsk = pairingPsk
        )
    }
}
