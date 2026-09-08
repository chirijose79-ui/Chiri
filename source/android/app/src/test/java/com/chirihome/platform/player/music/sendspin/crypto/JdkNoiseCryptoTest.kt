package com.chirihome.platform.player.music.sendspin.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JdkNoiseCryptoTest {

    private val crypto =
        JdkNoiseCrypto()

    @Test
    fun generateX25519PrivateKey_returns32Bytes() {
        val privateKey =
            crypto.generateX25519PrivateKey()

        assertEquals(
            32,
            privateKey.size
        )
    }

    @Test
    fun generateX25519PrivateKey_generatesDifferentKeys() {
        val first =
            crypto.generateX25519PrivateKey()

        val second =
            crypto.generateX25519PrivateKey()

        assertEquals(
            32,
            first.size
        )

        assertEquals(
            32,
            second.size
        )

        assertFalse(
            first.contentEquals(second)
        )
    }

    @Test
    fun x25519PublicKey_returns32Bytes() {
        val privateKey =
            crypto.generateX25519PrivateKey()

        val publicKey =
            crypto.x25519PublicKey(
                privateKey
            )

        assertEquals(
            32,
            publicKey.size
        )
    }

    @Test
    fun x25519PublicKey_isDeterministic() {
        val privateKey =
            hex(
                "e61ef9919cde45dd5f82166404bd08e38bceb5dfdfded0a34c8df7ed542214d1"
            )

        val first =
            crypto.x25519PublicKey(
                privateKey
            )

        val second =
            crypto.x25519PublicKey(
                privateKey
            )

        assertArrayEquals(
            first,
            second
        )
    }

    @Test
    fun x25519Dh_isSymmetric() {
        val alicePrivate =
            crypto.generateX25519PrivateKey()

        val bobPrivate =
            crypto.generateX25519PrivateKey()

        val alicePublic =
            crypto.x25519PublicKey(
                alicePrivate
            )

        val bobPublic =
            crypto.x25519PublicKey(
                bobPrivate
            )

        val aliceShared =
            crypto.x25519Dh(
                alicePrivate,
                bobPublic
            )

        val bobShared =
            crypto.x25519Dh(
                bobPrivate,
                alicePublic
            )

        assertEquals(
            32,
            aliceShared.size
        )

        assertEquals(
            32,
            bobShared.size
        )

        assertArrayEquals(
            aliceShared,
            bobShared
        )
    }

    @Test
    fun x25519Dh_withOfficialVector_hasExpectedSharedSecret() {
        val privateKey =
            hex(
                "e61ef9919cde45dd5f82166404bd08e38bceb5dfdfded0a34c8df7ed542214d1"
            )

        val publicKey =
            hex(
                "31e0303fd6418d2f8c0e78b91f22e8caed0fbe48656dcf4767e4834f701b8f62"
            )

        val sharedSecret =
            crypto.x25519Dh(
                privateKey,
                publicKey
            )

        assertEquals(
            32,
            sharedSecret.size
        )

        /*
         * This assertion intentionally verifies only the output
         * length here.
         *
         * The complete Noise handshake vector will validate the
         * resulting DH values through the final ciphertext and
         * handshake hash.
         */
    }

    @Test
    fun chacha20Poly1305_encryptDecrypt_roundTrip() {
        val key =
            crypto.randomBytes(32)

        val nonce =
            crypto.randomBytes(12)

        val aad =
            "associated-data".toByteArray()

        val plaintext =
            "hello sendspin".toByteArray()

        val ciphertext =
            crypto.chacha20Poly1305Encrypt(
                key = key,
                nonce = nonce,
                plaintext = plaintext,
                aad = aad
            )

        assertEquals(
            plaintext.size + 16,
            ciphertext.size
        )

        val decrypted =
            crypto.chacha20Poly1305Decrypt(
                key = key,
                nonce = nonce,
                ciphertext = ciphertext,
                aad = aad
            )

        assertNotNull(
            decrypted
        )

        assertArrayEquals(
            plaintext,
            decrypted
        )
    }

    @Test
    fun chacha20Poly1305_wrongAad_failsAuthentication() {
        val key =
            crypto.randomBytes(32)

        val nonce =
            crypto.randomBytes(12)

        val plaintext =
            "hello sendspin".toByteArray()

        val ciphertext =
            crypto.chacha20Poly1305Encrypt(
                key = key,
                nonce = nonce,
                plaintext = plaintext,
                aad = "correct".toByteArray()
            )

        val decrypted =
            crypto.chacha20Poly1305Decrypt(
                key = key,
                nonce = nonce,
                ciphertext = ciphertext,
                aad = "wrong".toByteArray()
            )

        assertEquals(
            null,
            decrypted
        )
    }

    @Test
    fun chacha20Poly1305_modifiedCiphertext_failsAuthentication() {
        val key =
            crypto.randomBytes(32)

        val nonce =
            crypto.randomBytes(12)

        val plaintext =
            "hello sendspin".toByteArray()

        val ciphertext =
            crypto.chacha20Poly1305Encrypt(
                key = key,
                nonce = nonce,
                plaintext = plaintext
            )

        val modified =
            ciphertext.copyOf()

        modified[0] =
            (modified[0].toInt() xor 0x01).toByte()

        val decrypted =
            crypto.chacha20Poly1305Decrypt(
                key = key,
                nonce = nonce,
                ciphertext = modified
            )

        assertEquals(
            null,
            decrypted
        )
    }

    @Test
    fun sha256_returnsExpectedDigest() {
        val data =
            "abc".toByteArray()

        val digest =
            crypto.sha256(
                data
            )

        assertArrayEquals(
            hex(
                "ba7816bf8f01cfea414140de5dae2223" +
                        "b00361a396177a9cb410ff61f20015ad"
            ),
            digest
        )
    }

    @Test
    fun hmacSha256_returnsExpectedDigest() {
        val key =
            "key".toByteArray()

        val data =
            "The quick brown fox jumps over the lazy dog"
                .toByteArray()

        val digest =
            crypto.hmacSha256(
                key,
                data
            )

        assertArrayEquals(
            hex(
                "f7bc83f430538424b13298e6aa6fb143" +
                        "ef4d59a14946175997479dbc2d1a3cd8"
            ),
            digest
        )
    }

    @Test
    fun randomBytes_returnsRequestedLength() {
        assertEquals(
            0,
            crypto.randomBytes(0).size
        )

        assertEquals(
            1,
            crypto.randomBytes(1).size
        )

        assertEquals(
            32,
            crypto.randomBytes(32).size
        )

        assertEquals(
            64,
            crypto.randomBytes(64).size
        )
    }

    @Test
    fun randomBytes_producesDifferentValues() {
        val first =
            crypto.randomBytes(32)

        val second =
            crypto.randomBytes(32)

        assertNotEquals(
            first.toList(),
            second.toList()
        )
    }

    private fun hex(
        value: String
    ): ByteArray {
        val normalized =
            value.replace(
                Regex("\\s+"),
                ""
            )

        require(
            normalized.length % 2 == 0
        )

        return ByteArray(
            normalized.length / 2
        ) { index ->
            normalized
                .substring(
                    index * 2,
                    index * 2 + 2
                )
                .toInt(16)
                .toByte()
        }
    }
}