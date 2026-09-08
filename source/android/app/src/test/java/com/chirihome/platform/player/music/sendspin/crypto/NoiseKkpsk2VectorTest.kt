package com.chirihome.platform.player.music.sendspin.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class NoiseKkpsk2VectorTest {

    private val crypto = JdkNoiseCrypto()

    private fun hex(value: String): ByteArray {
        require(value.length % 2 == 0)

        return ByteArray(value.length / 2) { index ->
            value
                .substring(
                    index * 2,
                    index * 2 + 2
                )
                .toInt(16)
                .toByte()
        }
    }

    @Test
    fun official_kkPsk2_vector_matches() {

        val protocolName =
            "Noise_KKpsk2_25519_ChaChaPoly_SHA256"

        assertEquals(
            protocolName,
            "Noise_KKpsk2_25519_ChaChaPoly_SHA256"
        )

        val prologue =
            hex(
                "4a6f686e2047616c74"
            )

        val psk =
            hex(
                "54686973206973206d7920417573747269616e20706572737065637469766521"
            )

        val initiatorStaticPrivate =
            hex(
                "e61ef9919cde45dd5f82166404bd08e38bceb5dfdfded0a34c8df7ed542214d1"
            )

        val initiatorEphemeralPrivate =
            hex(
                "893e28b9dc6ca8d611ab664754b8ceb7bac5117349a4439a6b0569da977c464a"
            )

        val responderStaticPrivate =
            hex(
                "4a3acbfdb163dec651dfa3194dece676d437029c62a408b4c5ea9114246e4893"
            )

        val responderEphemeralPrivate =
            hex(
                "bbdb4cdbd309f1a1f2e1456967fe288cadd6f712d65dc7b7793d5e63da6b375b"
            )

        val initiatorStaticPublic =
            crypto.x25519PublicKey(
                initiatorStaticPrivate
            )

        val responderStaticPublic =
            crypto.x25519PublicKey(
                responderStaticPrivate
            )

        val initiatorEphemeral =
            X25519KeyPair(
                privateKey =
                    initiatorEphemeralPrivate,
                publicKey =
                    crypto.x25519PublicKey(
                        initiatorEphemeralPrivate
                    )
            )

        val responderEphemeral =
            X25519KeyPair(
                privateKey =
                    responderEphemeralPrivate,
                publicKey =
                    crypto.x25519PublicKey(
                        responderEphemeralPrivate
                    )
            )

        val initiatorStatic =
            X25519KeyPair(
                privateKey =
                    initiatorStaticPrivate,
                publicKey =
                    initiatorStaticPublic
            )

        val responderStatic =
            X25519KeyPair(
                privateKey =
                    responderStaticPrivate,
                publicKey =
                    responderStaticPublic
            )

        val initiator =
            HandshakeState.createKkPsk2(
                crypto = crypto,
                role = NoiseRole.INITIATOR,
                prologue = prologue,
                localStatic = initiatorStatic,
                remoteStaticPublic =
                    responderStaticPublic,
                psk = psk,
                localEphemeral =
                    initiatorEphemeral
            )

        val responder =
            HandshakeState.createKkPsk2(
                crypto = crypto,
                role = NoiseRole.RESPONDER,
                prologue = prologue,
                localStatic = responderStatic,
                remoteStaticPublic =
                    initiatorStaticPublic,
                psk = psk,
                localEphemeral =
                    responderEphemeral
            )

        val payload1 =
            "Ludwig von Mises".toByteArray()

        val payload2 =
            "Murray Rothbard".toByteArray()

        val expectedMessage1 =
            hex(
                "ca35def5ae56cec33dc2036731ab14896bc4c75dbb07a61f879f8e3afa4c7944babf6443250c604872e33233c3b9a29df5c6d334ae2d53f1bd7f0b265a716b37"
            )

        val expectedMessage2 =
            hex(
                "95ebc60d2b1fa672c1f46a8aa265ef51bfe38e7ccb39ec5be34069f14480884366a1f5f0d79fe93ae476bd1897a7a8ae92764898aa5d49e07b5849f35865ba"
            )

        val expectedFinalHash =
            hex(
                "7f3c5fdcdd3767e2835473a2683971490339f5bbeee82c3690bc606e14db70ed"
            )

        val message1 =
            initiator.writeMessage(
                payload1
            )

        assertArrayEquals(
            expectedMessage1,
            message1
        )

        val receivedPayload1 =
            responder.readMessage(
                message1
            )

        assertArrayEquals(
            payload1,
            receivedPayload1
        )

        val message2 =
            responder.writeMessage(
                payload2
            )

        assertArrayEquals(
            expectedMessage2,
            message2
        )

        val receivedPayload2 =
            initiator.readMessage(
                message2
            )

        assertArrayEquals(
            payload2,
            receivedPayload2
        )

        assertEquals(
            true,
            initiator.isComplete
        )

        assertEquals(
            true,
            responder.isComplete
        )

        assertArrayEquals(
            expectedFinalHash,
            initiator.handshakeHash
        )

        assertArrayEquals(
            expectedFinalHash,
            responder.handshakeHash
        )

        assertArrayEquals(
            initiator.handshakeHash,
            responder.handshakeHash
        )
    }
}