package com.chirihome.platform.player.music.sendspin.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JdkNoiseCryptoInstrumentedTest {

    @Test
    fun x25519DiffieHellmanProducesSameSharedSecret() {
        val crypto = JdkNoiseCrypto()

        val privateKeyA =
            crypto.generateX25519PrivateKey()

        val privateKeyB =
            crypto.generateX25519PrivateKey()

        assertEquals(
            32,
            privateKeyA.size
        )

        assertEquals(
            32,
            privateKeyB.size
        )

        assertNotEquals(
            privateKeyA.toList(),
            privateKeyB.toList()
        )

        val publicKeyA =
            crypto.x25519PublicKey(
                privateKeyA
            )

        val publicKeyB =
            crypto.x25519PublicKey(
                privateKeyB
            )

        assertEquals(
            32,
            publicKeyA.size
        )

        assertEquals(
            32,
            publicKeyB.size
        )

        val sharedSecretA =
            crypto.x25519Dh(
                privateKey = privateKeyA,
                publicKey = publicKeyB
            )

        val sharedSecretB =
            crypto.x25519Dh(
                privateKey = privateKeyB,
                publicKey = publicKeyA
            )

        assertEquals(
            32,
            sharedSecretA.size
        )

        assertEquals(
            32,
            sharedSecretB.size
        )

        assertArrayEquals(
            sharedSecretA,
            sharedSecretB
        )
    }
}
