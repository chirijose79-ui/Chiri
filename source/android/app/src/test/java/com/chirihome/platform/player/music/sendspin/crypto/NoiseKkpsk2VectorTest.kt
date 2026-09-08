package com.chirihome.platform.player.music.sendspin.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class NoiseKkpsk2VectorTest {

    private val crypto = JdkNoiseCrypto()

    @Test
    fun vectorMaterial_hasExpectedLengths() {
        val prologue = hex(
            "4a6f686e2047616c74"
        )

        val psk = hex(
            "54686973206973206d7920417573747269616e20706572737065637469766521"
        )

        val initiatorStaticPrivate = hex(
            "e61ef9919cde45dd5f82166404bd08e38bceb5dfdfded0a34c8df7ed542214d1"
        )

        val initiatorEphemeralPrivate = hex(
            "893e28b9dc6ca8d611ab664754b8ceb7bac5117349a4439a6b0569da977c464a"
        )

        val initiatorRemoteStaticPublic = hex(
            "31e0303fd6418d2f8c0e78b91f22e8caed0fbe48656dcf4767e4834f701b8f62"
        )

        val responderStaticPrivate = hex(
            "4a3acbfdb163dec651dfa3194dece676d437029c62a408b4c5ea9114246e4893"
        )

        val responderEphemeralPrivate = hex(
            "bbdb4cdbd309f1a1f2e1456967fe288cadd6f712d65dc7b7793d5e63da6b375b"
        )

        val responderRemoteStaticPublic = hex(
            "6bc3822a2aa7f4e6981d6538692b3cdf3e6df9eea6ed269eb41d93c22757b75a"
        )

        assertEquals(9, prologue.size)
        assertEquals(32, psk.size)

        assertEquals(32, initiatorStaticPrivate.size)
        assertEquals(32, initiatorEphemeralPrivate.size)
        assertEquals(32, initiatorRemoteStaticPublic.size)

        assertEquals(32, responderStaticPrivate.size)
        assertEquals(32, responderEphemeralPrivate.size)
        assertEquals(32, responderRemoteStaticPublic.size)

        val initiatorStaticPublic =
            crypto.x25519PublicKey(
                initiatorStaticPrivate
            )

        val responderStaticPublic =
            crypto.x25519PublicKey(
                responderStaticPrivate
            )

        assertEquals(32, initiatorStaticPublic.size)
        assertEquals(32, responderStaticPublic.size)

        assertArrayEquals(
            initiatorRemoteStaticPublic,
            responderStaticPublic
        )

        assertArrayEquals(
            responderRemoteStaticPublic,
            initiatorStaticPublic
        )
    }

    @Test
    fun kkpsk2_bothSidesReachSameState() {
        val prologue = hex(
            "4a6f686e2047616c74"
        )

        val psk = hex(
            "54686973206973206d7920417573747269616e20706572737065637469766521"
        )

        val initiatorEphemeralPrivate = hex(
            "893e28b9dc6ca8d611ab664754b8ceb7bac5117349a4439a6b0569da977c464a"
        )

        val responderEphemeralPrivate = hex(
            "bbdb4cdbd309f1a1f2e1456967fe288cadd6f712d65dc7b7793d5e63da6b375b"
        )

        val initiatorStaticPrivate = hex(
            "e61ef9919cde45dd5f82166404bd08e38bceb5dfdfded0a34c8df7ed542214d1"
        )

        val responderStaticPrivate = hex(
            "4a3acbfdb163dec651dfa3194dece676d437029c62a408b4c5ea9114246e4893"
        )

        val initiator =
            NoiseProtocol.HandshakeState(crypto)

        val responder =
            NoiseProtocol.HandshakeState(crypto)

        val initiatorStaticPublic =
            crypto.x25519PublicKey(
                initiatorStaticPrivate
            )

        val responderStaticPublic =
            crypto.x25519PublicKey(
                responderStaticPrivate
            )

        initiator.initialize(
            localStaticPrivateKey =
                initiatorStaticPrivate,
            remoteStaticPublicKey =
                responderStaticPublic,
            prologue = prologue,
            initiator = true,
            psk = psk
        )

        responder.initialize(
            localStaticPrivateKey =
                responderStaticPrivate,
            remoteStaticPublicKey =
                initiatorStaticPublic,
            prologue = prologue,
            initiator = false,
            psk = psk
        )

        /*
         * Message 1:
         *
         *   -> e, es, ss
         */
        initiator.setEphemeralPrivateKey(
            initiatorEphemeralPrivate
        )

        val message1 =
            initiator.writeMessage1()

        responder.readMessage1(
            message1
        )

        assertArrayEquals(
            initiator.handshakeHash(),
            responder.handshakeHash()
        )

        /*
         * Message 2:
         *
         *   <- e, ee, se, psk
         */
        responder.setEphemeralPrivateKey(
            responderEphemeralPrivate
        )

        val message2 =
            responder.writeMessage2()

        initiator.readMessage2(
            message2
        )

        val expectedHandshakeHash = hex(
            "7f3c5fdcdd3767e2835473a2683971490339f5bbeee82c3690bc606e14db70ed"
        )

        assertArrayEquals(
            expectedHandshakeHash,
            initiator.handshakeHash()
        )

        assertArrayEquals(
            expectedHandshakeHash,
            responder.handshakeHash()
        )
    }

    private fun hex(value: String): ByteArray {
        require(value.length % 2 == 0)

        return ByteArray(value.length / 2) { index ->
            value.substring(
                index * 2,
                index * 2 + 2
            ).toInt(16).toByte()
        }
    }
}