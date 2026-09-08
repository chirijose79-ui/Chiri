package com.chirihome.platform.player.music.sendspin

import com.chirihome.platform.player.music.sendspin.protocol.SendspinHandshake
import com.chirihome.platform.player.music.sendspin.transport.InboundTransportEvent
import com.chirihome.platform.player.music.sendspin.transport.SendspinTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    }

    private class FakeSendspinHandshake(
        private val clientInit: String = """{"type":"client/init"}"""
    ) : SendspinHandshake {

        var createClientInitCalls = 0
            private set

        var receiveServerInitCalls = 0
            private set

        var receivedServerInit: String? = null
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
            return "noise-message-2"
        }
    }

    @Test
    fun initiallyNotConnected() {
        val transport = FakeSendspinTransport()

        val client = SendspinClient(
            transport = transport,
            handshake = createHandshake(),
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
            scope = CoroutineScope(Dispatchers.Unconfined)
        )
        val serverInit = """{"type":"server/init","payload":{"server_id":"abc","version":1}}"""

        client.connect()
        transport.emitTextMessage(serverInit)

        assertEquals(1, handshake.receiveServerInitCalls)
        assertEquals(serverInit, handshake.receivedServerInit)
    }

    @Test
    fun sendTextDelegatesToTransport() = runBlocking {
        val transport = FakeSendspinTransport()

        val client = SendspinClient(
            transport = transport,
            handshake = createHandshake(),
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
    fun disconnectDelegatesToTransport() = runBlocking {
        val transport = FakeSendspinTransport()

        val client = SendspinClient(
            transport = transport,
            handshake = createHandshake(),
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
        clientInit: String = """{"type":"client/init"}"""
    ): FakeSendspinHandshake {
        return FakeSendspinHandshake(clientInit)
    }
}
