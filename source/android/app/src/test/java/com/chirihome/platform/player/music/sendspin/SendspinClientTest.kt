package com.chirihome.platform.player.music.sendspin

import com.chirihome.platform.player.music.sendspin.crypto.HandshakeState
import com.chirihome.platform.player.music.sendspin.crypto.JdkNoiseCrypto
import com.chirihome.platform.player.music.sendspin.crypto.NoiseRole
import com.chirihome.platform.player.music.sendspin.crypto.NoiseTransport
import com.chirihome.platform.player.music.sendspin.crypto.X25519KeyPair
import com.chirihome.platform.player.music.sendspin.protocol.SendspinHandshake
import com.chirihome.platform.player.music.sendspin.session.SendspinProtocolSession
import com.chirihome.platform.player.music.sendspin.session.SendspinSessionEvent
import com.chirihome.platform.player.music.sendspin.transport.InboundTransportEvent
import com.chirihome.platform.player.music.sendspin.transport.SendspinTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SendspinClientTest {

    private class FakeSendspinTransport : SendspinTransport {

        private val _events =
            MutableSharedFlow<InboundTransportEvent>(
                extraBufferCapacity = 32
            )

        override val events: Flow<InboundTransportEvent> =
            _events

        override var isConnected: Boolean = false
            private set

        var connectCalls = 0
            private set

        var disconnectCalls = 0
            private set

        val sentMessages = mutableListOf<String>()

        val sentBinaryMessages = mutableListOf<ByteArray>()

        override suspend fun connect() {
            connectCalls++
            isConnected = true

            _events.emit(
                InboundTransportEvent.Connected
            )
        }

        override suspend fun send(message: String) {
            check(isConnected) {
                "Fake transport is not connected"
            }

            sentMessages += message
        }

        override suspend fun sendBinary(data: ByteArray) {
            check(isConnected) {
                "Fake transport is not connected"
            }

            sentBinaryMessages += data.copyOf()
        }

        override suspend fun disconnect() {
            disconnectCalls++
            isConnected = false

            _events.emit(
                InboundTransportEvent.Disconnected()
            )
        }

        suspend fun emitTextMessage(message: String) {
            _events.emit(
                InboundTransportEvent.TextMessage(message)
            )
        }

        suspend fun emitBinaryMessage(data: ByteArray) {
            _events.emit(
                InboundTransportEvent.BinaryMessage(
                    data.copyOf()
                )
            )
        }
    }

    private class FakeSendspinHandshake(
        private val clientInit: String = """{"type":"client/init"}""",
        override val noiseTransport: NoiseTransport? = null
    ) : SendspinHandshake {

        var createClientInitCalls = 0
            private set

        var receiveServerInitCalls = 0
            private set

        var receivedServerInit: String? = null
            private set

        var receiveNoiseMessage1Calls = 0
            private set

        var receivedNoiseMessage1: String? = null
            private set

        override suspend fun createClientInit(): String {
            createClientInitCalls++
            return clientInit
        }

        override fun receiveServerInit(rawMessage: String) {
            receiveServerInitCalls++
            receivedServerInit = rawMessage
        }

        override suspend fun receiveNoiseMessage1(
            rawMessage: String
        ): String {
            receiveNoiseMessage1Calls++
            receivedNoiseMessage1 = rawMessage

            return "noise-message-2"
        }
    }

    @Test
    fun initiallyNotConnected() {
        val transport = FakeSendspinTransport()

        val client = SendspinClient(
            transport = transport,
            handshake = createHandshake(),
            session = createSession(),
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        assertFalse(client.isConnected)
    }

    @Test
    fun connectDelegatesToTransport() = runBlocking {
        val transport = FakeSendspinTransport()

        val client = SendspinClient(
            transport = transport,
            handshake = createHandshake(),
            session = createSession(),
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        client.connect()

        assertTrue(client.isConnected)
        assertTrue(transport.isConnected)
        assertEquals(1, transport.connectCalls)
    }

    @Test
    fun connectDoesNotConnectTwice() = runBlocking {
        val transport = FakeSendspinTransport()

        val client = SendspinClient(
            transport = transport,
            handshake = createHandshake(),
            session = createSession(),
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        client.connect()
        client.connect()

        assertTrue(client.isConnected)
        assertEquals(1, transport.connectCalls)
    }

    @Test
    fun connectedSendsClientInit() = runBlocking {
        val transport = FakeSendspinTransport()

        val clientInit =
            """{"type":"client/init"}"""

        val handshake =
            createHandshake(clientInit)

        val client = SendspinClient(
            transport = transport,
            handshake = handshake,
            session = createSession(),
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        client.connect()

        assertEquals(
            1,
            handshake.createClientInitCalls
        )

        assertEquals(
            listOf(clientInit),
            transport.sentMessages
        )
    }

    @Test
    fun serverInitTextMessageIsDelegatedToHandshake() = runBlocking {
        val transport = FakeSendspinTransport()
        val handshake = createHandshake()

        val client = SendspinClient(
            transport = transport,
            handshake = handshake,
            session = createSession(),
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        val serverInit =
            """{"type":"server/init","payload":{"server_id":"abc","version":1}}"""

        client.connect()
        transport.emitTextMessage(serverInit)

        assertEquals(
            1,
            handshake.receiveServerInitCalls
        )

        assertEquals(
            serverInit,
            handshake.receivedServerInit
        )
    }

    @Test
    fun noiseHandshakeMessageIsProcessedAndMessage2IsSentAsText() =
        runBlocking {
            val transport = FakeSendspinTransport()
            val handshake = createHandshake()

            val client = SendspinClient(
                transport = transport,
                handshake = handshake,
                session = createSession(),
                scope = CoroutineScope(Dispatchers.Unconfined)
            )

            val noiseMessage1 =
                """{"type":"noise/handshake","payload":{"data":"test-noise-data"}}"""

            client.connect()
            transport.emitTextMessage(noiseMessage1)

            assertEquals(
                1,
                handshake.receiveNoiseMessage1Calls
            )

            assertEquals(
                noiseMessage1,
                handshake.receivedNoiseMessage1
            )

            assertEquals(
                "noise-message-2",
                transport.sentMessages.last()
            )

            assertTrue(
                transport.sentBinaryMessages.isEmpty()
            )
        }

    @Test
    fun encryptedBinaryMessageIsDeliveredToSession() =
        runBlocking {
            val (initiatorTransport, responderTransport) =
                createTestNoiseTransports()

            val transport = FakeSendspinTransport()
            val handshake =
                createHandshake(
                    noiseTransport = responderTransport
                )

            val session =
                FakeSendspinProtocolSession()

            val client = SendspinClient(
                transport = transport,
                handshake = handshake,
                session = session,
                scope = CoroutineScope(Dispatchers.Unconfined)
            )

            val serverInit =
                """{"type":"server/init","payload":{"server_id":"abc","version":1}}"""

            val noiseMessage1 =
                """{"type":"noise/handshake","payload":{"data":"test-noise-data"}}"""

            val plaintext =
                """{"type":"server/hello","payload":{"name":"Music Assistant"}}"""

            val plaintextBytes =
                plaintext.toByteArray(Charsets.UTF_8)

            val framedPlaintext =
                ByteArray(1 + plaintextBytes.size)

            framedPlaintext[0] = 0x00

            plaintextBytes.copyInto(
                destination = framedPlaintext,
                destinationOffset = 1
            )

            client.connect()

            transport.emitTextMessage(serverInit)
            transport.emitTextMessage(noiseMessage1)

            val encrypted =
                initiatorTransport.encrypt(
                    framedPlaintext
                )

            transport.emitBinaryMessage(encrypted)

            assertEquals(
                listOf(plaintext),
                session.receivedMessages
            )
        }

    @Test
    fun sendTextDelegatesToTransport() = runBlocking {
        val transport = FakeSendspinTransport()

        val client = SendspinClient(
            transport = transport,
            handshake = createHandshake(),
            session = createSession(),
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        client.connect()
        client.send("test-message")

        assertTrue(
            transport.sentMessages.contains("test-message")
        )
    }

    @Test
    fun sendBinaryDelegatesToTransport() = runBlocking {
        val transport = FakeSendspinTransport()

        val client = SendspinClient(
            transport = transport,
            handshake = createHandshake(),
            session = createSession(),
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        client.connect()

        val data = byteArrayOf(
            0x01,
            0x02,
            0x03,
            0x04
        )

        client.sendBinary(data)

        assertEquals(
            1,
            transport.sentBinaryMessages.size
        )

        assertArrayEquals(
            data,
            transport.sentBinaryMessages.single()
        )
    }

    @Test
    fun sendEncryptedEncryptsAndSendsBinary() = runBlocking {
        val (initiatorTransport, responderTransport) =
            createTestNoiseTransports()

        val transport = FakeSendspinTransport()

        val client = SendspinClient(
            transport = transport,
            handshake = createHandshake(
                noiseTransport = initiatorTransport
            ),
            session = createSession(),
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        client.connect()

        val serverInit =
            """{"type":"server/init","payload":{"server_id":"abc","version":1}}"""

        val noiseMessage1 =
            """{"type":"noise/handshake","payload":{"data":"test-noise-data"}}"""

        transport.emitTextMessage(serverInit)
        transport.emitTextMessage(noiseMessage1)

        val plaintext =
            """{"type":"client/hello","payload":{"name":"Chiri Test"}}"""

        client.sendEncrypted(plaintext)

        assertEquals(
            1,
            transport.sentBinaryMessages.size
        )

        val encrypted =
            transport.sentBinaryMessages.single()

        val decrypted =
            responderTransport.decrypt(encrypted)

        assertTrue(
            decrypted.isNotEmpty()
        )

        assertEquals(
            0x00,
            decrypted[0].toInt()
        )

        assertEquals(
            plaintext,
            decrypted
                .copyOfRange(1, decrypted.size)
                .toString(Charsets.UTF_8)
        )
    }

    @Test
    fun disconnectDelegatesToTransport() = runBlocking {
        val transport = FakeSendspinTransport()

        val client = SendspinClient(
            transport = transport,
            handshake = createHandshake(),
            session = createSession(),
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        client.connect()
        client.disconnect()

        assertFalse(client.isConnected)
        assertFalse(transport.isConnected)
        assertEquals(1, transport.disconnectCalls)
    }

    @Test
    fun sendTextWithoutConnectionFails() = runBlocking {
        val transport = FakeSendspinTransport()

        val client = SendspinClient(
            transport = transport,
            handshake = createHandshake(),
            session = createSession(),
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        var failed = false

        try {
            client.send("test-message")
        } catch (exception: IllegalStateException) {
            failed = true
        }

        assertTrue(failed)
    }

    @Test
    fun sendBinaryWithoutConnectionFails() = runBlocking {
        val transport = FakeSendspinTransport()

        val client = SendspinClient(
            transport = transport,
            handshake = createHandshake(),
            session = createSession(),
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        var failed = false

        try {
            client.sendBinary(
                byteArrayOf(
                    0x01,
                    0x02
                )
            )
        } catch (exception: IllegalStateException) {
            failed = true
        }

        assertTrue(failed)
    }

    private fun createHandshake(
        clientInit: String = """{"type":"client/init"}""",
        noiseTransport: NoiseTransport? = null
    ): FakeSendspinHandshake {
        return FakeSendspinHandshake(
            clientInit = clientInit,
            noiseTransport = noiseTransport
        )
    }

    private fun createSession(): SendspinProtocolSession {
        return FakeSendspinProtocolSession()
    }

    private fun createTestNoiseTransports():
            Pair<NoiseTransport, NoiseTransport> {

        val crypto = JdkNoiseCrypto()

        val initiatorPrivate =
            crypto.generateX25519PrivateKey()

        val initiatorStatic =
            X25519KeyPair(
                privateKey = initiatorPrivate,
                publicKey =
                    crypto.x25519PublicKey(
                        initiatorPrivate
                    )
            )

        val responderPrivate =
            crypto.generateX25519PrivateKey()

        val responderStatic =
            X25519KeyPair(
                privateKey = responderPrivate,
                publicKey =
                    crypto.x25519PublicKey(
                        responderPrivate
                    )
            )

        val psk =
            crypto.randomBytes(32)

        val prologue =
            "sendspin-test"
                .toByteArray(Charsets.UTF_8)

        val initiatorHandshake =
            HandshakeState.createKkPsk2(
                crypto = crypto,
                role = NoiseRole.INITIATOR,
                prologue = prologue,
                localStatic = initiatorStatic,
                remoteStaticPublic =
                    responderStatic.publicKey,
                psk = psk
            )

        val responderHandshake =
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
            initiatorHandshake.writeMessage(
                "message1"
                    .toByteArray(Charsets.UTF_8)
            )

        responderHandshake.readMessage(
            message1
        )

        val message2 =
            responderHandshake.writeMessage(
                "{}"
                    .toByteArray(Charsets.UTF_8)
            )

        initiatorHandshake.readMessage(
            message2
        )

        assertNotNull(
            initiatorHandshake.result
        )

        assertNotNull(
            responderHandshake.result
        )

        return Pair(
            initiatorHandshake.result!!.transport,
            responderHandshake.result!!.transport
        )
    }

    private class FakeSendspinProtocolSession :
        SendspinProtocolSession {

        override val events:
                Flow<SendspinSessionEvent> =
            emptyFlow()

        override val isActive: Boolean =
            true

        val receivedMessages =
            mutableListOf<String>()

        override suspend fun start() {
        }

        override suspend fun handleTransportEvent(
            event: InboundTransportEvent
        ) {
        }

        override suspend fun handleMessage(
            message: String
        ) {
            receivedMessages += message
        }

        override suspend fun send(
            message: String
        ) {
        }

        override suspend fun stop() {
        }
    }
}
