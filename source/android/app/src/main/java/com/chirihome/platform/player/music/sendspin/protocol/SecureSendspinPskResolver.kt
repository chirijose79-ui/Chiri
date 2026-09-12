package com.chirihome.platform.player.music.sendspin.protocol

import com.chirihome.platform.player.music.sendspin.crypto.NoiseCrypto
import com.chirihome.platform.player.music.sendspin.crypto.SendspinBase64
import com.chirihome.platform.storage.SendspinCredentialStorage

class SecureSendspinPskResolver(
    private val storage: SendspinCredentialStorage,
    private val crypto: NoiseCrypto
) : SendspinPskResolver {

    override suspend fun resolve(
        pskId: String,
        serverId: String
    ): SendspinPskResolution? {

        require(pskId.isNotBlank()) {
            "PSK id must not be blank"
        }

        require(serverId.isNotBlank()) {
            "Server id must not be blank"
        }

        /*
         * Sendspin uses a well-known Sentinel PSK for clients
         * that are not yet paired.
         *
         * aiosendspin:
         *
         * SENTINEL_PSK = SHA256("sendspin-sentinel-psk-v1")
         *
         * psk_id_for(psk) =
         * base64url(SHA256("sendspin-psk-id-v1" || psk))
         */
        val sentinelPsk =
            crypto.sha256(
                SENTINEL_PSK_SEED.toByteArray(Charsets.UTF_8)
            )

        if (calculatePskId(sentinelPsk) == pskId) {
            return SendspinPskResolution(
                psk = sentinelPsk,
                type = SendspinPskType.SENTINEL
            )
        }

        /*
         * If this is not the Sentinel PSK, try the stored
         * pairing PSK.
         */
        val pairingPsk =
            storage.getPairingPsk()

        if (pairingPsk != null) {
            require(pairingPsk.size == PSK_SIZE) {
                "Stored Sendspin PSK must be 32 bytes"
            }

            val pairingPskId =
                calculatePskId(pairingPsk)

            if (pairingPskId == pskId) {
                return SendspinPskResolution(
                    psk = pairingPsk.copyOf(),
                    type = SendspinPskType.PAIRING
                )
            }
        }

        /*
         * If this is not the Pairing PSK, try the Long-Term PSK
         * associated with the current server.
         */
        val longTermPsk =
            storage.getLongTermPsk(serverId)

        if (
            longTermPsk != null &&
            longTermPsk.size == PSK_SIZE
        ) {
            val longTermPskId =
                calculatePskId(longTermPsk)

            if (longTermPskId == pskId) {
                return SendspinPskResolution(
                    psk = longTermPsk.copyOf(),
                    type = SendspinPskType.LONG_TERM
                )
            }
        }

        return null
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
