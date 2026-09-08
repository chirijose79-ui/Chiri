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
        pskCategory: String
    ): ByteArray? {

        require(pskId.isNotBlank()) {
            "PSK id must not be blank"
        }

        require(pskCategory.isNotBlank()) {
            "PSK category must not be blank"
        }

        val psk = when (pskCategory) {
            CATEGORY_PAIRING -> storage.getPairingPsk()
            CATEGORY_LONG_TERM -> null
            CATEGORY_SENTINEL -> null
            else -> null
        } ?: return null

        val calculatedPskId =
            calculatePskId(psk)

        if (calculatedPskId != pskId) {
            return null
        }

        return psk.copyOf()
    }

    private fun calculatePskId(psk: ByteArray): String {
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
        private const val CATEGORY_LONG_TERM = "lt"
        private const val CATEGORY_PAIRING = "pr"
        private const val CATEGORY_SENTINEL = "sn"

        private const val PSK_ID_LABEL = "sendspin-psk-id-v1"
        private const val PSK_SIZE = 32
    }
}
