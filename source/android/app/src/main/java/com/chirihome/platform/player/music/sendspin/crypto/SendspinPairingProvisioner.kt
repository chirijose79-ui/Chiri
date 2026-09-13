package com.chirihome.platform.player.music.sendspin.crypto

import com.chirihome.platform.storage.SendspinCredentialStorage

class SendspinPairingProvisioner(
    private val storage: SendspinCredentialStorage
) {

    suspend fun provision(
        token: String,
        identity: SendspinIdentity
    ) {
        val (clientKey, pairingPsk) =
            SendspinPsk.decodeToken(token)

        require(
            clientKey.contentEquals(
                identity.staticPublicKey
            )
        ) {
            "Sendspin pairing token does not belong to this identity"
        }

        require(
            pairingPsk.size == 32
        ) {
            "Pairing PSK must be exactly 32 bytes"
        }

        storage.savePairingPsk(
            pairingPsk.copyOf()
        )
    }
}
