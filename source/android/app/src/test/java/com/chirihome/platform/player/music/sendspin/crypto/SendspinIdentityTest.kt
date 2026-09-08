package com.chirihome.platform.player.music.sendspin.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendspinIdentityTest {

    private val crypto =
        JdkNoiseCrypto()

    @Test
    fun generatedIdentityHasValidKeysAndClientId() {
        val identity =
            SendspinIdentity.generate(crypto)

        assertEquals(
            DH_LEN,
            identity.staticPrivateKey.size
        )

        assertEquals(
            DH_LEN,
            identity.staticPublicKey.size
        )

        assertEquals(
            43,
            identity.clientId.length
        )

        assertTrue(
            identity.clientId.all {
                it.isLetterOrDigit() ||
                        it == '-' ||
                        it == '_'
            }
        )
    }

    @Test
    fun samePrivateKeyProducesSameIdentity() {
        val privateKey =
            crypto.generateX25519PrivateKey()

        val first =
            SendspinIdentity.fromPrivateKey(
                privateKey,
                crypto
            )

        val second =
            SendspinIdentity.fromPrivateKey(
                privateKey,
                crypto
            )

        assertEquals(
            first.staticPublicKey.toList(),
            second.staticPublicKey.toList()
        )

        assertEquals(
            first.clientId,
            second.clientId
        )
    }

    @Test
    fun differentGeneratedIdentitiesHaveDifferentClientIds() {
        val first =
            SendspinIdentity.generate(crypto)

        val second =
            SendspinIdentity.generate(crypto)

        assertNotEquals(
            first.clientId,
            second.clientId
        )
    }

    @Test
    fun pskIdIsStableAndBase64UrlEncoded() {
        val identity =
            SendspinIdentity.generate(crypto)

        val psk =
            ByteArray(KEY_LEN) { index ->
                index.toByte()
            }

        val first =
            identity.pskId(
                psk = psk,
                crypto = crypto
            )

        val second =
            identity.pskId(
                psk = psk,
                crypto = crypto
            )

        assertEquals(
            first,
            second
        )

        assertEquals(
            43,
            first.length
        )

        assertTrue(
            first.all {
                it.isLetterOrDigit() ||
                        it == '-' ||
                        it == '_'
            }
        )
    }
}