package com.chirihome.platform.player.music.sendspin.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseProtocolTest {

    private val crypto = JdkNoiseCrypto()

    @Test
    fun cipherState_unkeyed_roundTrip() {
        val cipher = CipherState(crypto)

        val plaintext =
            "hello sendspin".toByteArray()

        val encrypted =
            cipher.encryptWithAd(
                ByteArray(0),
                plaintext
            )

        assertArrayEquals(
            plaintext,
            encrypted
        )

        val decrypted =
            cipher.decryptWithAd(
                ByteArray(0),
                encrypted
            )

        assertArrayEquals(
            plaintext,
            decrypted
        )
    }

    @Test
    fun cipherState_keyed_roundTrip() {
        val key =
            crypto.randomBytes(32)

        val encryptCipher =
            CipherState(crypto)

        val decryptCipher =
            CipherState(crypto)

        encryptCipher.initializeKey(key)
        decryptCipher.initializeKey(key)

        val plaintext =
            "Noise protocol test".toByteArray()

        val encrypted =
            encryptCipher.encryptWithAd(
                ByteArray(0),
                plaintext
            )

        assertFalse(
            encrypted.contentEquals(
                plaintext
            )
        )

        val decrypted =
            decryptCipher.decryptWithAd(
                ByteArray(0),
                encrypted
            )

        assertArrayEquals(
            plaintext,
            decrypted
        )
    }

    @Test
    fun cipherState_nonce_increments() {
        val key =
            crypto.randomBytes(32)

        val cipher =
            CipherState(crypto)

        cipher.initializeKey(key)

        val plaintext =
            "nonce test".toByteArray()

        val first =
            cipher.encryptWithAd(
                ByteArray(0),
                plaintext
            )

        val second =
            cipher.encryptWithAd(
                ByteArray(0),
                plaintext
            )

        assertFalse(
            first.contentEquals(second)
        )
    }

    @Test
    fun cipherState_wrongKey_fails() {
        val key1 =
            crypto.randomBytes(32)

        val key2 =
            crypto.randomBytes(32)

        val encryptCipher =
            CipherState(crypto)

        val decryptCipher =
            CipherState(crypto)

        encryptCipher.initializeKey(key1)
        decryptCipher.initializeKey(key2)

        val plaintext =
            "wrong key".toByteArray()

        val encrypted =
            encryptCipher.encryptWithAd(
                ByteArray(0),
                plaintext
            )

        var failed = false

        try {
            decryptCipher.decryptWithAd(
                ByteArray(0),
                encrypted
            )
        } catch (
            exception: NoiseException
        ) {
            failed = true
        }

        assertTrue(failed)
    }

    @Test
    fun symmetricState_initialize_creates32ByteHash() {
        val state =
            SymmetricState(crypto)

        state.initialize(
            "Noise_KKpsk2_25519_ChaChaPoly_SHA256"
        )

        assertEquals(
            32,
            state.handshakeHash.size
        )
    }

    @Test
    fun symmetricState_mixHash_changesHash() {
        val state =
            SymmetricState(crypto)

        state.initialize(
            "Noise_KKpsk2_25519_ChaChaPoly_SHA256"
        )

        val before =
            state.handshakeHash.copyOf()

        state.mixHash(
            "test".toByteArray()
        )

        assertFalse(
            before.contentEquals(
                state.handshakeHash
            )
        )
    }

    @Test
    fun symmetricState_encryptAndHash_roundTrip() {
        val encryptState =
            SymmetricState(crypto)

        val decryptState =
            SymmetricState(crypto)

        encryptState.initialize(
            "Noise_KKpsk2_25519_ChaChaPoly_SHA256"
        )

        decryptState.initialize(
            "Noise_KKpsk2_25519_ChaChaPoly_SHA256"
        )

        val key =
            crypto.randomBytes(32)

        encryptState.cipher.initializeKey(key)
        decryptState.cipher.initializeKey(key)

        val plaintext =
            "encrypted payload".toByteArray()

        val ciphertext =
            encryptState.encryptAndHash(
                plaintext
            )

        assertFalse(
            ciphertext.contentEquals(
                plaintext
            )
        )

        val decrypted =
            decryptState.decryptAndHash(
                ciphertext
            )

        assertArrayEquals(
            plaintext,
            decrypted
        )

        assertArrayEquals(
            encryptState.handshakeHash,
            decryptState.handshakeHash
        )
    }

    @Test
    fun x25519_keyExchange_isSymmetric() {
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

        assertArrayEquals(
            aliceShared,
            bobShared
        )
    }

    @Test
    fun kkPsk2_handshake_completes() {
        val initiatorStaticPrivate =
            crypto.generateX25519PrivateKey()

        val responderStaticPrivate =
            crypto.generateX25519PrivateKey()

        val initiatorStatic =
            X25519KeyPair(
                privateKey =
                    initiatorStaticPrivate,
                publicKey =
                    crypto.x25519PublicKey(
                        initiatorStaticPrivate
                    )
            )

        val responderStatic =
            X25519KeyPair(
                privateKey =
                    responderStaticPrivate,
                publicKey =
                    crypto.x25519PublicKey(
                        responderStaticPrivate
                    )
            )

        val psk =
            crypto.randomBytes(32)

        val prologue =
            "sendspin-test".toByteArray()

        val initiator =
            HandshakeState.createKkPsk2(
                crypto = crypto,
                role = NoiseRole.INITIATOR,
                prologue = prologue,
                localStatic = initiatorStatic,
                remoteStaticPublic =
                    responderStatic.publicKey,
                psk = psk
            )

        val responder =
            HandshakeState.createKkPsk2(
                crypto = crypto,
                role = NoiseRole.RESPONDER,
                prologue = prologue,
                localStatic = responderStatic,
                remoteStaticPublic =
                    initiatorStatic.publicKey,
                psk = psk
            )

        val message1 =
            initiator.writeMessage(
                "hello".toByteArray()
            )

        val payload1 =
            responder.readMessage(
                message1
            )

        assertArrayEquals(
            "hello".toByteArray(),
            payload1
        )

        val message2 =
            responder.writeMessage(
                "world".toByteArray()
            )

        val payload2 =
            initiator.readMessage(
                message2
            )

        assertArrayEquals(
            "world".toByteArray(),
            payload2
        )

        assertTrue(
            initiator.isComplete
        )

        assertTrue(
            responder.isComplete
        )

        assertNotNull(
            initiator.result
        )

        assertNotNull(
            responder.result
        )
    }

    @Test
    fun kkPsk2_handshake_producesMatchingHash() {
        val initiatorStaticPrivate =
            crypto.generateX25519PrivateKey()

        val responderStaticPrivate =
            crypto.generateX25519PrivateKey()

        val initiatorStatic =
            X25519KeyPair(
                privateKey =
                    initiatorStaticPrivate,
                publicKey =
                    crypto.x25519PublicKey(
                        initiatorStaticPrivate
                    )
            )

        val responderStatic =
            X25519KeyPair(
                privateKey =
                    responderStaticPrivate,
                publicKey =
                    crypto.x25519PublicKey(
                        responderStaticPrivate
                    )
            )

        val psk =
            crypto.randomBytes(32)

        val prologue =
            "sendspin-test".toByteArray()

        val initiator =
            HandshakeState.createKkPsk2(
                crypto = crypto,
                role = NoiseRole.INITIATOR,
                prologue = prologue,
                localStatic = initiatorStatic,
                remoteStaticPublic =
                    responderStatic.publicKey,
                psk = psk
            )

        val responder =
            HandshakeState.createKkPsk2(
                crypto = crypto,
                role = NoiseRole.RESPONDER,
                prologue = prologue,
                localStatic = responderStatic,
                remoteStaticPublic =
                    initiatorStatic.publicKey,
                psk = psk
            )

        val message1 =
            initiator.writeMessage(
                "one".toByteArray()
            )

        responder.readMessage(
            message1
        )

        val message2 =
            responder.writeMessage(
                "two".toByteArray()
            )

        initiator.readMessage(
            message2
        )

        assertArrayEquals(
            initiator.handshakeHash,
            responder.handshakeHash
        )
    }

    @Test
    fun kkPsk2_transport_canExchangeData() {
        val initiatorStaticPrivate =
            crypto.generateX25519PrivateKey()

        val responderStaticPrivate =
            crypto.generateX25519PrivateKey()

        val initiatorStatic =
            X25519KeyPair(
                privateKey =
                    initiatorStaticPrivate,
                publicKey =
                    crypto.x25519PublicKey(
                        initiatorStaticPrivate
                    )
            )

        val responderStatic =
            X25519KeyPair(
                privateKey =
                    responderStaticPrivate,
                publicKey =
                    crypto.x25519PublicKey(
                        responderStaticPrivate
                    )
            )

        val psk =
            crypto.randomBytes(32)

        val prologue =
            "sendspin-test".toByteArray()

        val initiator =
            HandshakeState.createKkPsk2(
                crypto = crypto,
                role = NoiseRole.INITIATOR,
                prologue = prologue,
                localStatic = initiatorStatic,
                remoteStaticPublic =
                    responderStatic.publicKey,
                psk = psk
            )

        val responder =
            HandshakeState.createKkPsk2(
                crypto = crypto,
                role = NoiseRole.RESPONDER,
                prologue = prologue,
                localStatic = responderStatic,
                remoteStaticPublic =
                    initiatorStatic.publicKey,
                psk = psk
            )

        val message1 =
            initiator.writeMessage(
                "one".toByteArray()
            )

        responder.readMessage(
            message1
        )

        val message2 =
            responder.writeMessage(
                "two".toByteArray()
            )

        initiator.readMessage(
            message2
        )

        val initiatorResult =
            initiator.result
                ?: throw AssertionError(
                    "Initiator result missing"
                )

        val responderResult =
            responder.result
                ?: throw AssertionError(
                    "Responder result missing"
                )

        val plaintext =
            "transport message".toByteArray()

        val encrypted =
            initiatorResult.transport.encrypt(
                plaintext
            )

        val decrypted =
            responderResult.transport.decrypt(
                encrypted
            )

        assertArrayEquals(
            plaintext,
            decrypted
        )
    }
}
