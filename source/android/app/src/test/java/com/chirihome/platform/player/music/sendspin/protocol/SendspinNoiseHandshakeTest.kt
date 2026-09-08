package com.chirihome.platform.player.music.sendspin.protocol

import com.chirihome.platform.player.music.sendspin.crypto.HandshakeState
import com.chirihome.platform.player.music.sendspin.crypto.JdkNoiseCrypto
import com.chirihome.platform.player.music.sendspin.crypto.NoiseCrypto
import com.chirihome.platform.player.music.sendspin.crypto.NoiseRole
import com.chirihome.platform.player.music.sendspin.crypto.SendspinBase64
import com.chirihome.platform.player.music.sendspin.crypto.SendspinIdentity
import com.chirihome.platform.player.music.sendspin.crypto.SendspinIdentityProvider
import com.chirihome.platform.player.music.sendspin.crypto.X25519KeyPair
import com.chirihome.platform.storage.SendspinCredentialStorage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SendspinNoiseHandshakeTest {

    private val crypto: NoiseCrypto = JdkNoiseCrypto()

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    @Test
    fun handshake_resolvesPskAndCreatesMessage2() = runBlocking {
        val clientStaticPrivateKey = ByteArray(32) { (it + 1).toByte() }
        val clientIdentity =
            SendspinIdentity.fromPrivateKey(
                clientStaticPrivateKey,
                crypto
            )

        val serverStaticPrivateKey = ByteArray(32) { (it + 33).toByte() }
        val serverStaticPublicKey =
            crypto.x25519PublicKey(serverStaticPrivateKey)

        val pairingPsk = ByteArray(32) { (it + 65).toByte() }

        val storage =
            FakeSendspinCredentialStorage(
                staticPrivateKey = clientStaticPrivateKey,
                pairingPsk = pairingPsk,
                serverStaticPublicKey = serverStaticPublicKey
            )

        val identityProvider =
            SendspinIdentityProvider(
                storage = storage,
                crypto = crypto
            )

        val resolver =
            SecureSendspinPskResolver(
                storage = storage,
                crypto = crypto
            )

        val responder =
            SendspinNoiseHandshake(
                identityProvider = identityProvider,
                crypto = crypto,
                pskResolver = resolver
            )

        val clientInit =
            responder.createClientInit()

        val serverInit =
            createServerInit(serverStaticPublicKey)

        responder.receiveServerInit(serverInit)

        val clientEphemeralPrivateKey =
            ByteArray(32) { (it + 97).toByte() }

        val prologue =
            clientInit.toByteArray(Charsets.UTF_8) +
                    serverInit.toByteArray(Charsets.UTF_8)

        val initiator =
            HandshakeState.createKkPsk2(
                crypto = crypto,
                role = NoiseRole.INITIATOR,
                prologue = prologue,
                localStatic =
                    X25519KeyPair(
                        privateKey = serverStaticPrivateKey,
                        publicKey = serverStaticPublicKey
                    ),
                remoteStaticPublic =
                    clientIdentity.staticPublicKey,
                psk = pairingPsk,
                localEphemeral =
                    X25519KeyPair(
                        privateKey = clientEphemeralPrivateKey,
                        publicKey =
                            crypto.x25519PublicKey(
                                clientEphemeralPrivateKey
                            )
                    )
            )

        val expectedPskId =
            calculatePskId(pairingPsk)

        val message1Payload =
            SendspinNoiseMsg1Payload(
                psk_id = expectedPskId,
                psk_category = "pr"
            )

        val message1PayloadJson =
            json.encodeToString(message1Payload)

        val noiseMessage1 =
            initiator.writeMessage(
                message1PayloadJson.toByteArray(Charsets.UTF_8)
            )

        val wrappedMessage1 =
            json.encodeToString(
                SendspinNoiseHandshakeMessage(
                    payload =
                        SendspinNoiseHandshakePayload(
                            data =
                                SendspinBase64.encodeUrlSafe(
                                    noiseMessage1
                                )
                        )
                )
            )

        val receivedPayload =
            responder.readNoiseMessage1(wrappedMessage1)

        assertEquals(
            expectedPskId,
            receivedPayload.psk_id
        )

        assertEquals(
            "pr",
            receivedPayload.psk_category
        )

        val message2 =
            responder.createNoiseMessage2()

        assertTrue(message2.isNotBlank())
        assertTrue(responder.isComplete)

        assertNotNull(responder.handshakeHash)
        assertNotNull(responder.result)

        assertArrayEquals(
            serverStaticPublicKey,
            responder.getServerStaticPublicKey()
        )

        assertEquals(
            expectedPskId,
            responder.pskId
        )

        assertEquals(
            "pr",
            responder.pskCategory
        )
    }

    private fun createServerInit(
        serverStaticPublicKey: ByteArray
    ): String {
        return json.encodeToString(
            SendspinServerInitMessage(
                payload =
                    SendspinServerInitPayload(
                        server_id =
                            SendspinBase64.encodeUrlSafe(
                                serverStaticPublicKey
                            ),
                        version = 1
                    )
            )
        )
    }

    private fun calculatePskId(
        psk: ByteArray
    ): String {
        val label =
            "sendspin-psk-id-v1"
                .toByteArray(Charsets.UTF_8)

        val digest =
            crypto.sha256(label + psk)

        return SendspinBase64.encodeUrlSafe(digest)
    }

    private class FakeSendspinCredentialStorage(
        private var staticPrivateKey: ByteArray? = null,
        private var pairingPsk: ByteArray? = null,
        private var serverStaticPublicKey: ByteArray? = null
    ) : SendspinCredentialStorage {

        override suspend fun saveStaticPrivateKey(
            key: ByteArray
        ) {
            staticPrivateKey = key.copyOf()
        }

        override suspend fun getStaticPrivateKey(): ByteArray? {
            return staticPrivateKey?.copyOf()
        }

        override suspend fun savePairingPsk(
            psk: ByteArray
        ) {
            pairingPsk = psk.copyOf()
        }

        override suspend fun getPairingPsk(): ByteArray? {
            return pairingPsk?.copyOf()
        }

        override suspend fun saveServerStaticPublicKey(
            key: ByteArray
        ) {
            serverStaticPublicKey = key.copyOf()
        }

        override suspend fun getServerStaticPublicKey(): ByteArray? {
            return serverStaticPublicKey?.copyOf()
        }

        override suspend fun clearCredentials() {
            staticPrivateKey = null
            pairingPsk = null
            serverStaticPublicKey = null
        }
    }
}