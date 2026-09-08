package com.chirihome.platform.player.music.sendspin.crypto

object SendspinPsk {

    private const val TOKEN_PREFIX = "SP:0"

    private const val CLIENT_KEY_LENGTH = 32
    private const val PSK_LENGTH = 32

    private const val TOKEN_LENGTH =
        4 + ((CLIENT_KEY_LENGTH + PSK_LENGTH) * 8 + 4) / 5

    fun createToken(
        clientKey: ByteArray,
        pairingPsk: ByteArray
    ): String {

        require(
            clientKey.size == CLIENT_KEY_LENGTH
        ) {
            "Client key must be 32 bytes"
        }

        require(
            pairingPsk.size == PSK_LENGTH
        ) {
            "Pairing PSK must be 32 bytes"
        }

        val payload =
            clientKey + pairingPsk

        return TOKEN_PREFIX +
                SendspinBase32.encode(payload)
    }

    fun decodeToken(
        token: String
    ): Pair<ByteArray, ByteArray> {

        require(
            token.startsWith(TOKEN_PREFIX)
        ) {
            "Invalid Sendspin pairing token prefix"
        }

        val encoded =
            token.removePrefix(
                TOKEN_PREFIX
            )

        require(
            encoded.isNotEmpty()
        ) {
            "Empty Sendspin pairing token"
        }

        val payload =
            SendspinBase32.decode(
                encoded
            )

        require(
            payload.size ==
                    CLIENT_KEY_LENGTH + PSK_LENGTH
        ) {
            "Invalid Sendspin pairing token length"
        }

        val clientKey =
            payload.copyOfRange(
                0,
                CLIENT_KEY_LENGTH
            )

        val pairingPsk =
            payload.copyOfRange(
                CLIENT_KEY_LENGTH,
                CLIENT_KEY_LENGTH + PSK_LENGTH
            )

        return Pair(
            clientKey,
            pairingPsk
        )
    }

    fun isValidToken(
        token: String
    ): Boolean {

        return try {
            decodeToken(token)
            true
        } catch (
            exception: Exception
        ) {
            false
        }
    }

    fun generateClientKey(
        crypto: NoiseCrypto
    ): ByteArray {
        return crypto.randomBytes(
            CLIENT_KEY_LENGTH
        )
    }

    fun generatePairingPsk(
        crypto: NoiseCrypto
    ): ByteArray {
        return crypto.randomBytes(
            PSK_LENGTH
        )
    }

    fun tokenLength(): Int {
        return TOKEN_LENGTH
    }
}