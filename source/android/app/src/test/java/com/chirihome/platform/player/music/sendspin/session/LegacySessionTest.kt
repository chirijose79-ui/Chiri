package com.chirihome.platform.player.music.sendspin.session

import com.chirihome.platform.player.music.sendspin.SendspinCapabilities
import com.chirihome.platform.player.music.sendspin.SendspinConfig
import com.chirihome.platform.player.music.sendspin.SendspinMessageSender
import com.chirihome.platform.player.music.sendspin.transport.InboundTransportEvent
import com.chirihome.platform.player.music.sendspin.transport.SendspinTransport
import com.chirihome.platform.player.music.sendspin.ClockSynchronizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class LegacySessionTest {

    private class FakeSendspinTransport : SendspinTransport {

        private val _events =
            MutableSharedFlow<InboundTransportEvent>(
                extraBufferCapacity = 32
            )

        override val events: Flow<InboundTransportEvent> =
            _events

        override var isConnected: Boolean = false
            private set

        val sentMessages = mutableListOf<String>()

        val sentBinaryMessages = mutableListOf<ByteArray>()

        override suspend fun connect() {
            isConnected = true
            _events.emit(InboundTransportEvent.Connected)
        }

        override suspend fun send(message: String) {
            sentMessages += message
        }

        override suspend fun sendBinary(data: ByteArray) {
            sentBinaryMessages += data.copyOf()
        }

        override suspend fun disconnect() {
            isConnected = false
            _events.emit(InboundTransportEvent.Disconnected())
        }
    }

    private class FakeSendspinMessageSender : SendspinMessageSender {

        val encryptedMessages =
            CopyOnWriteArrayList<String>()

        override suspend fun sendEncrypted(message: String) {
            encryptedMessages += message
        }
    }

    private fun createConfig(): SendspinConfig =
        SendspinConfig(
            clientId = "test-client",
            deviceName = "Chiri Test"
        )

    private fun createCapabilities(): SendspinCapabilities =
        SendspinCapabilities(
            codecs = listOf("opus"),
            sampleRates = listOf(48000),
            bitDepths = listOf(16),
            channels = listOf(1, 2)
        )

    private fun createSession(
        transport: FakeSendspinTransport,
        messageSender: FakeSendspinMessageSender,
        scope: CoroutineScope
    ): LegacySession =
        LegacySession(
            config = createConfig(),
            capabilities = createCapabilities(),
            transport = transport,
            messageSender = messageSender,
            clockSynchronizer = ClockSynchronizer(),
            scope = scope
        )

    @Test
    fun startDoesNotSendClientHello() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val session = createSession(
            transport = transport,
            messageSender = messageSender,
            scope = scope
        )

        session.start()

        assertTrue(session.isActive)
        assertTrue(messageSender.encryptedMessages.isEmpty())
        assertTrue(transport.sentMessages.isEmpty())

        scope.cancel()
    }

    @Test
    fun connectedDoesNotSendClientHello() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val session = createSession(
            transport = transport,
            messageSender = messageSender,
            scope = scope
        )

        session.handleTransportEvent(
            InboundTransportEvent.Connected
        )

        assertTrue(session.isActive)
        assertTrue(messageSender.encryptedMessages.isEmpty())
        assertTrue(transport.sentMessages.isEmpty())

        scope.cancel()
    }

    @Test
    fun serverHelloTriggersEncryptedClientHello() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val session = createSession(
            transport = transport,
            messageSender = messageSender,
            scope = scope
        )

        val serverHello =
            """{"type":"server/hello","payload":{"name":"Music Assistant"}}"""

        session.handleMessage(serverHello)

        assertEquals(
            1,
            messageSender.encryptedMessages.size
        )

        val clientHello = messageSender.encryptedMessages.single()

        assertTrue(
            clientHello.contains("\"type\":\"client/hello\"")
        )

        assertTrue(
            clientHello.contains("\"name\":\"Chiri Test\"")
        )

        assertTrue(
            clientHello.contains("\"supported_roles\":[\"player@v1\"]")
        )

        assertTrue(
            clientHello.contains("\"player@v1_support\"")
        )

        assertTrue(
            clientHello.contains("\"supported_formats\"")
        )

        assertTrue(
            clientHello.contains("\"codec\":\"opus\"")
        )

        assertTrue(
            clientHello.contains("\"channels\":1")
        )

        assertTrue(
            clientHello.contains("\"channels\":2")
        )

        assertTrue(
            clientHello.contains("\"sample_rate\":48000")
        )

        assertTrue(
            clientHello.contains("\"bit_depth\":16")
        )

        assertTrue(
            clientHello.contains("\"buffer_capacity\":524288")
        )

        assertTrue(
            clientHello.contains("\"supported_commands\"")
        )

        assertTrue(
            clientHello.contains("\"unpaired_access\":{\"enabled\":false}")
        )

        assertTrue(
            transport.sentMessages.isEmpty()
        )

        assertTrue(
            transport.sentBinaryMessages.isEmpty()
        )

        scope.cancel()
    }

    @Test
    fun clientTimeSendsEncryptedClientTimeMessage() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val clockSynchronizer = ClockSynchronizer()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val session =
            LegacySession(
                config = createConfig(),
                capabilities = createCapabilities(),
                transport = transport,
                messageSender = messageSender,
                clockSynchronizer = clockSynchronizer,
                scope = scope
            )

        val before =
            clockSynchronizer.localTimeMicros()

        session.sendClientTime()

        val after =
            clockSynchronizer.localTimeMicros()

        assertEquals(
            1,
            messageSender.encryptedMessages.size
        )

        val clientTime =
            messageSender.encryptedMessages.single()

        assertTrue(
            clientTime.contains("\"type\":\"client/time\"")
        )

        assertTrue(
            clientTime.contains("\"client_transmitted\":")
        )

        val timestamp =
            Regex(
                "\"client_transmitted\":(\\d+)"
            )
                .find(clientTime)
                ?.groupValues
                ?.get(1)
                ?.toLong()
                ?: error("client_transmitted not found")

        assertTrue(timestamp >= before)
        assertTrue(timestamp <= after)
        scope.cancel()
    }

    @Test
    fun serverHelloStartsPeriodicClientTime() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val session =
            LegacySession(
                config = createConfig(),
                capabilities = createCapabilities(),
                transport = transport,
                messageSender = messageSender,
                clockSynchronizer = ClockSynchronizer(),
                scope = scope
            )

        val serverHello =
            """{"type":"server/hello","payload":{"name":"Music Assistant"}}"""

        session.handleMessage(serverHello)

        delay(100)

        assertEquals(
            1,
            messageSender.encryptedMessages.count {
                it.contains("\"type\":\"client/hello\"")
            }
        )

        assertTrue(
            messageSender.encryptedMessages.any {
                it.contains("\"type\":\"client/time\"")
            }
        )

        delay(1_100)

        val clientTimeCount =
            messageSender.encryptedMessages.count {
                it.contains("\"type\":\"client/time\"")
            }

        assertTrue(
            "Expected at least two client/time messages",
            clientTimeCount >= 2
        )

        session.handleTransportEvent(
            InboundTransportEvent.Disconnected(null)
        )

        val countAfterDisconnect =
            messageSender.encryptedMessages.count {
                it.contains("\"type\":\"client/time\"")
            }

        delay(1_100)

        assertEquals(
            countAfterDisconnect,
            messageSender.encryptedMessages.count {
                it.contains("\"type\":\"client/time\"")
            }
        )

        scope.cancel()
    }

    @Test
    fun duplicateServerHelloDoesNotSendClientHelloAgain() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val session = createSession(
            transport = transport,
            messageSender = messageSender,
            scope = scope
        )

        val serverHello =
            """{"type":"server/hello","payload":{"name":"Music Assistant"}}"""

        session.handleMessage(serverHello)
        session.handleMessage(serverHello)

        assertEquals(
            1,
            messageSender.encryptedMessages.count {
                it.contains("\"type\":\"client/hello\"")
            }
        )

        scope.cancel()
    }

    @Test
    fun serverTimeUpdatesClockSynchronizer() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val clockSynchronizer = ClockSynchronizer()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val session =
            LegacySession(
                config = createConfig(),
                capabilities = createCapabilities(),
                transport = transport,
                messageSender = messageSender,
                clockSynchronizer = clockSynchronizer,
                scope = scope
            )

        val serverTime1 =
            """
        {
            "type":"server/time",
            "payload":{
                "client_transmitted":1000000,
                "server_received":1005000,
                "server_transmitted":1006000
            }
        }
        """.trimIndent()

        val serverTime2 =
            """
        {
            "type":"server/time",
            "payload":{
                "client_transmitted":2000000,
                "server_received":2005000,
                "server_transmitted":2006000
            }
        }
        """.trimIndent()

        session.handleMessage(
            message = serverTime1,
            receivedAtLocalMicros = 1001000
        )

        assertTrue(
            !clockSynchronizer.isSynchronized()
        )

        session.handleMessage(
            message = serverTime2,
            receivedAtLocalMicros = 2001000
        )

        assertTrue(
            clockSynchronizer.isSynchronized()
        )

        assertEquals(
            1995000L,
            clockSynchronizer.serverTimeToLocalMicros(2000000L)
        )

        scope.cancel()
    }
}
