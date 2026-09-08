package com.chirihome.platform.player.music.sendspin.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SendspinPskTest {

    private val crypto = JdkNoiseCrypto()

    @Test
    fun createToken_hasExpectedPrefix() {
        val clientKey =
            ByteArray(32) { it.toByte() }

        val pairingPsk =
            ByteArray(32) { (it + 32).toByte() }

        val token =
            SendspinPsk.createToken(
                clientKey,
                pairingPsk
            )

        assertTrue(
            token.startsWith("SP:0")
        )
    }

    @Test
    fun createToken_hasExpectedLength() {
        val clientKey =
            ByteArray(32) { it.toByte() }

        val pairingPsk =
            ByteArray(32) { (it + 32).toByte() }

        val token =
            SendspinPsk.createToken(
                clientKey,
                pairingPsk
            )

        assertEquals(
            SendspinPsk.tokenLength(),
            token.length
        )

        assertEquals(
            107,
            token.length
        )
    }

    @Test
    fun createToken_decodeToken_roundTrip() {
        val clientKey =
            ByteArray(32) { it.toByte() }

        val pairingPsk =
            ByteArray(32) {
                (255 - it).toByte()
            }

        val token =
            SendspinPsk.createToken(
                clientKey,
                pairingPsk
            )

        val decoded =
            SendspinPsk.decodeToken(
                token
            )

        assertArrayEquals(
            clientKey,
            decoded.first
        )

        assertArrayEquals(
            pairingPsk,
            decoded.second
        )
    }

    @Test
    fun decodeToken_acceptsGeneratedToken() {
        val clientKey =
            crypto.randomBytes(32)

        val pairingPsk =
            crypto.randomBytes(32)

        val token =
            SendspinPsk.createToken(
                clientKey,
                pairingPsk
            )

        assertTrue(
            SendspinPsk.isValidToken(token)
        )

        val decoded =
            SendspinPsk.decodeToken(token)

        assertArrayEquals(
            clientKey,
            decoded.first
        )

        assertArrayEquals(
            pairingPsk,
            decoded.second
        )
    }

    @Test
    fun isValidToken_rejectsInvalidPrefix() {
        val clientKey =
            ByteArray(32)

        val pairingPsk =
            ByteArray(32)

        val token =
            SendspinPsk.createToken(
                clientKey,
                pairingPsk
            )

        val invalidToken =
            "XX:0" +
                    token.removePrefix("SP:0")

        assertFalse(
            SendspinPsk.isValidToken(
                invalidToken
            )
        )
    }

    @Test
    fun isValidToken_rejectsInvalidLength() {
        assertFalse(
            SendspinPsk.isValidToken(
                "SP:0ABC"
            )
        )
    }

    @Test
    fun decodeToken_rejectsInvalidPrefix() {
        val exception =
            org.junit.Assert.assertThrows(
                IllegalArgumentException::class.java
            ) {
                SendspinPsk.decodeToken(
                    "XX:0ABC"
                )
            }

        assertTrue(
            exception.message
                ?.contains("prefix")
                    == true
        )
    }

    @Test
    fun createToken_rejectsInvalidClientKeyLength() {
        org.junit.Assert.assertThrows(
            IllegalArgumentException::class.java
        ) {
            SendspinPsk.createToken(
                ByteArray(31),
                ByteArray(32)
            )
        }
    }

    @Test
    fun createToken_rejectsInvalidPskLength() {
        org.junit.Assert.assertThrows(
            IllegalArgumentException::class.java
        ) {
            SendspinPsk.createToken(
                ByteArray(32),
                ByteArray(31)
            )
        }
    }

    @Test
    fun generateClientKey_returns32Bytes() {
        val clientKey =
            SendspinPsk.generateClientKey(
                crypto
            )

        assertEquals(
            32,
            clientKey.size
        )
    }

    @Test
    fun generatePairingPsk_returns32Bytes() {
        val pairingPsk =
            SendspinPsk.generatePairingPsk(
                crypto
            )

        assertEquals(
            32,
            pairingPsk.size
        )
    }

    @Test
    fun generatedValues_areNotIdentical() {
        val first =
            SendspinPsk.generatePairingPsk(
                crypto
            )

        val second =
            SendspinPsk.generatePairingPsk(
                crypto
            )

        assertFalse(
            first.contentEquals(second)
        )
    }
}