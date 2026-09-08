package com.chirihome.platform.player.music.sendspin.crypto

class SendspinIdentity(
    val staticPrivateKey: ByteArray,
    val staticPublicKey: ByteArray
) {

    val clientId: String
        get() = SendspinBase64.encodeUrlSafe(staticPublicKey)

    fun pskId(
        psk: ByteArray,
        crypto: NoiseCrypto
    ): String {
        require(psk.size == KEY_LEN) {
            "PSK must be 32 bytes"
        }

        val label =
            "sendspin-psk-id-v1"
                .toByteArray(Charsets.UTF_8)

        val digest =
            crypto.sha256(
                label + psk
            )

        return SendspinBase64.encodeUrlSafe(digest)
    }

    companion object {

        fun generate(
            crypto: NoiseCrypto
        ): SendspinIdentity {

            val privateKey =
                crypto.generateX25519PrivateKey()

            val publicKey =
                crypto.x25519PublicKey(
                    privateKey
                )

            return SendspinIdentity(
                staticPrivateKey = privateKey,
                staticPublicKey = publicKey
            )
        }

        fun fromPrivateKey(
            privateKey: ByteArray,
            crypto: NoiseCrypto
        ): SendspinIdentity {

            require(privateKey.size == DH_LEN) {
                "X25519 private key must be 32 bytes"
            }

            val publicKey =
                crypto.x25519PublicKey(
                    privateKey
                )

            return SendspinIdentity(
                staticPrivateKey = privateKey.copyOf(),
                staticPublicKey = publicKey
            )
        }
    }
}
