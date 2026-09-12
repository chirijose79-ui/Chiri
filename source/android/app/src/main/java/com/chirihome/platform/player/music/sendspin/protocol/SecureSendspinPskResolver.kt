package com.chirihome.platform.player.music.sendspin.protocol

import com.chirihome.platform.player.music.sendspin.crypto.NoiseCrypto
import com.chirihome.platform.player.music.sendspin.crypto.SendspinBase64
import com.chirihome.platform.storage.SendspinCredentialStorage

class SecureSendspinPskResolver(
    private val storage: SendspinCredentialStorage,
    private val crypto: NoiseCrypto
) : SendspinPskResolver {

    override suspend fun resolve(
        pskId: String
    ): ByteArray? {

        require(pskId.isNotBlank()) {
            "PSK id must not be blank"
        }

        /*
         * Sendspin uses a well-known Sentinel PSK for clients
         * that are not yet paired.
         *
         * aiosendspin:
         *
         * SENTINEL_PSK = SHA256("sendspin-sentinel-psk-v1")
         *
         * psk_id_for(psk) = base64url(SHA256("sendspin-psk-id-v1" || psk))
         */
        val sentinelPsk =
            crypto.sha256(
                SENTINEL_PSK_SEED.toByteArray(Charsets.UTF_8)
            )

        if (calculatePskId(sentinelPsk) == pskId) {
            return sentinelPsk
        }

        /*
         * If this is not the Sentinel PSK, try the stored
         * pairing PSK.
         */
        val pairingPsk =
            storage.getPairingPsk()
                ?: return null

        require(pairingPsk.size == PSK_SIZE) {
            "Stored Sendspin PSK must be 32 bytes"
        }

        val calculatedPskId =
            calculatePskId(pairingPsk)

        if (calculatedPskId != pskId) {
            return null
        }

        return pairingPsk.copyOf()
    }

    private fun calculatePskId(
        psk: ByteArray
    ): String {
        require(psk.size == PSK_SIZE) {
            "PSK must be 32 bytes"
        }

        val label =
            PSK_ID_LABEL.toByteArray(Charsets.UTF_8)

        val digest =
            crypto.sha256(label + psk)

        return SendspinBase64.encodeUrlSafe(digest)
    }

    companion object {
        private const val PSK_ID_LABEL =
            "sendspin-psk-id-v1"

        private const val SENTINEL_PSK_SEED =
            "sendspin-sentinel-psk-v1"

        private const val PSK_SIZE = 32
    }
}
