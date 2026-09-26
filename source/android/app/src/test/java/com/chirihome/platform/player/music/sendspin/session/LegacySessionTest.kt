package com.chirihome.platform.player.music.sendspin.session

import com.chirihome.platform.player.music.sendspin.SendspinCapabilities
import com.chirihome.platform.player.music.sendspin.SendspinConfig
import com.chirihome.platform.player.music.sendspin.SendspinMessageSender
import com.chirihome.platform.player.music.sendspin.audio.SendspinAudioLifecycle
import com.chirihome.platform.player.music.sendspin.transport.InboundTransportEvent
import com.chirihome.platform.player.music.sendspin.transport.SendspinTransport
import com.chirihome.platform.player.music.sendspin.ClockSynchronizer
import com.chirihome.platform.player.music.sendspin.protocol.SendspinPairingState
import com.chirihome.platform.player.music.sendspin.protocol.SendspinPskType
import com.chirihome.platform.player.music.sendspin.protocol.SendspinPairingFinalizer
import com.chirihome.platform.storage.SendspinCredentialStorage
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

    private class FakeSendspinPairingState(
        override val pskType: SendspinPskType? = null,
        override val serverId: String? = null
    ) : SendspinPairingState

    private class FakeSendspinPairingFinalizer : SendspinPairingFinalizer {

        val receivedServerIds = mutableListOf<String>()

        val confirmedServerIds = mutableListOf<String>()

        override suspend fun createPairFinalize(serverId: String): String {
            receivedServerIds += serverId

            return """
        {
            "type":"client/pair-finalize",
            "payload":{
                "long_term_psk":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            }
        }
        """.trimIndent()
        }

        override suspend fun confirmPairFinalize(
            serverId: String
        ) {
            confirmedServerIds += serverId
        }
    }

    private class FakeSendspinCredentialStorage :
        SendspinCredentialStorage {

        private val longTermPsks =
            mutableMapOf<String, ByteArray>()

        val removedServerIds =
            mutableListOf<String>()

        override suspend fun saveStaticPrivateKey(
            key: ByteArray
        ) {}

        override suspend fun getStaticPrivateKey(): ByteArray? =
            null

        override suspend fun savePairingPsk(
            psk: ByteArray
        ) {}

        override suspend fun getPairingPsk(): ByteArray? =
            null

        override suspend fun saveLongTermPsk(
            serverId: String,
            psk: ByteArray
        ) {
            longTermPsks[serverId] = psk.copyOf()
        }

        override suspend fun getLongTermPsk(
            serverId: String
        ): ByteArray? {
            return longTermPsks[serverId]?.copyOf()
        }

        override suspend fun removeLongTermPsk(
            serverId: String
        ) {
            removedServerIds += serverId
            longTermPsks.remove(serverId)
        }

        override suspend fun saveServerStaticPublicKey(
            key: ByteArray
        ) {}

        override suspend fun getServerStaticPublicKey(): ByteArray? =
            null

        override suspend fun clearCredentials() {
            longTermPsks.clear()
        }
    }

    private class FakeSendspinAudioLifecycle :
        SendspinAudioLifecycle {

        val configurations = mutableListOf<AudioConfiguration>()

        var startCount = 0

        override suspend fun configure(
            sampleRate: Int,
            channels: Int,
            bitDepth: Int,
            codec: String
        ) {
            configurations += AudioConfiguration(
                sampleRate = sampleRate,
                channels = channels,
                bitDepth = bitDepth,
                codec = codec
            )
        }

        override suspend fun start() {
            startCount++
        }

        data class AudioConfiguration(
            val sampleRate: Int,
            val channels: Int,
            val bitDepth: Int,
            val codec: String
        )
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
        scope: CoroutineScope,
        pairingState: SendspinPairingState = FakeSendspinPairingState(),
        pairingFinalizer: SendspinPairingFinalizer = FakeSendspinPairingFinalizer(),
        credentialStorage: SendspinCredentialStorage =
            FakeSendspinCredentialStorage()
    ): LegacySession {
        return LegacySession(
            config = createConfig(),
            capabilities = createCapabilities(),
            transport = transport,
            messageSender = messageSender,
            clockSynchronizer = ClockSynchronizer(),
            scope = scope,
            pairingState = pairingState,
            pairingFinalizer = pairingFinalizer,
            audioLifecycle = FakeSendspinAudioLifecycle(),
            credentialStorage = credentialStorage
        )
    }

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
            clientHello.contains(
                "\"supported_pair_methods\":[{\"method\":\"pairing_psk\"}]"
            )
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
                scope = scope,
                pairingState = FakeSendspinPairingState(),
                pairingFinalizer = FakeSendspinPairingFinalizer(),
                audioLifecycle = FakeSendspinAudioLifecycle(),
                credentialStorage = FakeSendspinCredentialStorage()
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
    fun serverActivateStartsPeriodicClientTime() = runBlocking {
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
                scope = scope,
                pairingState = FakeSendspinPairingState(),
                pairingFinalizer = FakeSendspinPairingFinalizer(),
                audioLifecycle = FakeSendspinAudioLifecycle(),
                credentialStorage = FakeSendspinCredentialStorage()
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

        assertEquals(
            0,
            messageSender.encryptedMessages.count {
                it.contains("\"type\":\"client/time\"")
            }
        )

        val serverActivate =
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":[],
                "active_roles":[]
            }
        }
        """.trimIndent()

        session.handleMessage(serverActivate)

        delay(100)

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
                scope = scope,
                pairingState = FakeSendspinPairingState(),
                pairingFinalizer = FakeSendspinPairingFinalizer(),
                audioLifecycle = FakeSendspinAudioLifecycle(),
                credentialStorage = FakeSendspinCredentialStorage()
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

    @Test
    fun playerRoleActivationSendsInitialClientStateUnavailable() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val session = createSession(
            transport = transport,
            messageSender = messageSender,
            scope = scope
        )

        val serverActivate =
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":[],
                "active_roles":["player@v1"]
            }
        }
        """.trimIndent()

        session.handleMessage(serverActivate)

        delay(100)

        val clientStates =
            messageSender.encryptedMessages.filter {
                it.contains("\"type\":\"client/state\"")
            }

        assertEquals(1, clientStates.size)

        val clientState = clientStates.single()

        assertTrue(
            clientState.contains("\"available\":false")
        )

        assertTrue(
            clientState.contains("\"player@v1\"")
        )

        assertTrue(
            clientState.contains("\"output_delay_ms\":0")
        )

        assertTrue(
            clientState.contains("\"required_lead_time_ms\":250")
        )

        assertTrue(
            clientState.contains("\"min_buffer_ms\":250")
        )

        assertTrue(
            clientState.contains("\"supported_commands\"")
        )

        assertTrue(
            clientState.contains("\"volume\"")
        )

        assertTrue(
            clientState.contains("\"mute\"")
        )

        scope.cancel()
    }

    @Test
    fun pairingActivationWithPairingPskSendsClientPairFinalize() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pairingFinalizer = FakeSendspinPairingFinalizer()

        val session =
            createSession(
                transport = transport,
                messageSender = messageSender,
                scope = scope,
                pairingState =
                    FakeSendspinPairingState(
                        pskType = SendspinPskType.PAIRING,
                        serverId = "test-server-id"
                    ),
                pairingFinalizer = pairingFinalizer
            )

        val serverActivate =
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":["pairing"],
                "active_roles":[],
                "pairing":{
                    "method":"pairing_psk"
                }
            }
        }
        """.trimIndent()

        session.handleMessage(serverActivate)

        delay(100)

        assertEquals(
            listOf("test-server-id"),
            pairingFinalizer.receivedServerIds
        )

        val pairFinalizeMessages =
            messageSender.encryptedMessages.filter {
                it.contains("\"type\":\"client/pair-finalize\"")
            }

        assertEquals(1, pairFinalizeMessages.size)

        assertTrue(
            pairFinalizeMessages.single().contains(
                "\"long_term_psk\":\"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\""
            )
        )

        scope.cancel()
    }

    @Test
    fun serverPairFinalizeConfirmsPairingForActiveServer() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pairingFinalizer = FakeSendspinPairingFinalizer()

        val session =
            createSession(
                transport = transport,
                messageSender = messageSender,
                scope = scope,
                pairingState =
                    FakeSendspinPairingState(
                        pskType = SendspinPskType.PAIRING,
                        serverId = "test-server-id"
                    ),
                pairingFinalizer = pairingFinalizer
            )

        val serverActivate =
            """
    {
        "type":"server/activate",
        "payload":{
            "activities":["pairing"],
            "active_roles":[],
            "pairing":{
                "method":"pairing_psk"
            }
        }
    }
    """.trimIndent()

        session.handleMessage(serverActivate)

        delay(100)

        assertEquals(
            listOf("test-server-id"),
            pairingFinalizer.receivedServerIds
        )

        assertEquals(
            emptyList<String>(),
            pairingFinalizer.confirmedServerIds
        )

        val serverPairFinalize =
            """
    {
        "type":"server/pair-finalize",
        "payload":{}
    }
    """.trimIndent()

        session.handleMessage(serverPairFinalize)

        assertEquals(
            listOf("test-server-id"),
            pairingFinalizer.confirmedServerIds
        )

        scope.cancel()
    }

    @Test
    fun serverPairFinalizeWithoutPendingFinalizeDoesNotConfirm() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pairingFinalizer = FakeSendspinPairingFinalizer()

        val session =
            createSession(
                transport = transport,
                messageSender = messageSender,
                scope = scope,
                pairingState =
                    FakeSendspinPairingState(
                        pskType = SendspinPskType.PAIRING,
                        serverId = "test-server-id"
                    ),
                pairingFinalizer = pairingFinalizer
            )

        val serverPairFinalize =
            """
        {
            "type":"server/pair-finalize",
            "payload":{}
        }
        """.trimIndent()

        var exception: IllegalArgumentException? = null

        try {
            session.handleMessage(serverPairFinalize)
        } catch (error: IllegalArgumentException) {
            exception = error
        }

        assertEquals(
            "Received server/pair-finalize without pending client/pair-finalize",
            exception?.message
        )

        assertEquals(
            emptyList<String>(),
            pairingFinalizer.confirmedServerIds
        )

        scope.cancel()
    }

    @Test
    fun pairingActivationWithLongTermPskDoesNotSendClientPairFinalize() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pairingFinalizer = FakeSendspinPairingFinalizer()

        val session =
            createSession(
                transport = transport,
                messageSender = messageSender,
                scope = scope,
                pairingState =
                    FakeSendspinPairingState(
                        pskType = SendspinPskType.LONG_TERM,
                        serverId = "test-server-id"
                    ),
                pairingFinalizer = pairingFinalizer
            )

        val serverActivate =
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":["pairing"],
                "active_roles":[],
                "pairing":{
                    "method":"pairing_psk"
                }
            }
        }
        """.trimIndent()

        session.handleMessage(serverActivate)

        delay(100)

        assertTrue(pairingFinalizer.receivedServerIds.isEmpty())

        assertTrue(
            messageSender.encryptedMessages.none {
                it.contains("\"type\":\"client/pair-finalize\"")
            }
        )

        scope.cancel()
    }

    @Test
    fun pairingActivationWithUnsupportedPairingMethodDoesNotSendClientPairFinalize() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pairingFinalizer = FakeSendspinPairingFinalizer()

        val session =
            createSession(
                transport = transport,
                messageSender = messageSender,
                scope = scope,
                pairingState =
                    FakeSendspinPairingState(
                        pskType = SendspinPskType.PAIRING,
                        serverId = "test-server-id"
                    ),
                pairingFinalizer = pairingFinalizer
            )

        val serverActivate =
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":["pairing"],
                "active_roles":[],
                "pairing":{
                    "method":"unsupported_method"
                }
            }
        }
        """.trimIndent()

        session.handleMessage(serverActivate)

        delay(100)

        assertTrue(pairingFinalizer.receivedServerIds.isEmpty())

        assertTrue(
            messageSender.encryptedMessages.none {
                it.contains("\"type\":\"client/pair-finalize\"")
            }
        )

        scope.cancel()
    }

    @Test
    fun pairingActivationWithoutServerIdDoesNotSendClientPairFinalize() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pairingFinalizer = FakeSendspinPairingFinalizer()

        val session =
            createSession(
                transport = transport,
                messageSender = messageSender,
                scope = scope,
                pairingState =
                    FakeSendspinPairingState(
                        pskType = SendspinPskType.PAIRING,
                        serverId = null
                    ),
                pairingFinalizer = pairingFinalizer
            )

        val serverActivate =
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":["pairing"],
                "active_roles":[],
                "pairing":{
                    "method":"pairing_psk"
                }
            }
        }
        """.trimIndent()

        session.handleMessage(serverActivate)

        delay(100)

        assertTrue(pairingFinalizer.receivedServerIds.isEmpty())

        assertTrue(
            messageSender.encryptedMessages.none {
                it.contains("\"type\":\"client/pair-finalize\"")
            }
        )

        scope.cancel()
    }

    @Test
    fun repeatedPairingActivationSendsClientPairFinalizeOnlyOnce() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pairingFinalizer = FakeSendspinPairingFinalizer()

        val session =
            createSession(
                transport = transport,
                messageSender = messageSender,
                scope = scope,
                pairingState =
                    FakeSendspinPairingState(
                        pskType = SendspinPskType.PAIRING,
                        serverId = "test-server-id"
                    ),
                pairingFinalizer = pairingFinalizer
            )

        val serverActivate =
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":["pairing"],
                "active_roles":[],
                "pairing":{
                    "method":"pairing_psk"
                }
            }
        }
        """.trimIndent()

        session.handleMessage(serverActivate)
        session.handleMessage(serverActivate)

        delay(100)

        assertEquals(
            listOf("test-server-id"),
            pairingFinalizer.receivedServerIds
        )

        assertEquals(
            1,
            messageSender.encryptedMessages.count {
                it.contains("\"type\":\"client/pair-finalize\"")
            }
        )

        scope.cancel()
    }

    @Test
    fun inactivePlayerRoleDoesNotSendClientState() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val session = createSession(
            transport = transport,
            messageSender = messageSender,
            scope = scope
        )

        val serverActivate =
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":[],
                "active_roles":[]
            }
        }
        """.trimIndent()

        session.handleMessage(serverActivate)

        delay(100)

        assertEquals(
            0,
            messageSender.encryptedMessages.count {
                it.contains("\"type\":\"client/state\"")
            }
        )

        scope.cancel()
    }

    @Test
    fun synchronizedClockMakesPlayerAvailable() = runBlocking {
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
                scope = scope,
                pairingState = FakeSendspinPairingState(),
                pairingFinalizer = FakeSendspinPairingFinalizer(),
                audioLifecycle = FakeSendspinAudioLifecycle(),
                credentialStorage = FakeSendspinCredentialStorage()
            )

        val serverActivate =
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":[],
                "active_roles":["player@v1"]
            }
        }
        """.trimIndent()

        session.handleMessage(serverActivate)

        delay(100)

        assertEquals(
            1,
            messageSender.encryptedMessages.count {
                it.contains("\"type\":\"client/state\"")
            }
        )

        assertTrue(
            messageSender.encryptedMessages.any {
                it.contains("\"available\":false")
            }
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

        delay(100)

        assertEquals(
            1,
            messageSender.encryptedMessages.count {
                it.contains("\"type\":\"client/state\"")
            }
        )

        session.handleMessage(
            message = serverTime2,
            receivedAtLocalMicros = 2001000
        )

        delay(100)

        val clientStates =
            messageSender.encryptedMessages.filter {
                it.contains("\"type\":\"client/state\"")
            }

        assertEquals(
            2,
            clientStates.size
        )

        assertTrue(
            clientStates[0].contains("\"available\":false")
        )

        assertTrue(
            clientStates[1].contains("\"available\":true")
        )

        scope.cancel()
    }

    @Test
    fun synchronizedClockDoesNotSendAvailableStateAgain() = runBlocking {
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
                scope = scope,
                pairingState = FakeSendspinPairingState(),
                pairingFinalizer = FakeSendspinPairingFinalizer(),
                audioLifecycle = FakeSendspinAudioLifecycle(),
                credentialStorage = FakeSendspinCredentialStorage()
            )

        session.handleMessage(
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":[],
                "active_roles":["player@v1"]
            }
        }
        """.trimIndent()
        )

        delay(100)

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

        session.handleMessage(
            message = serverTime2,
            receivedAtLocalMicros = 2001000
        )

        delay(100)

        val availableStateCount =
            messageSender.encryptedMessages.count {
                it.contains("\"type\":\"client/state\"") &&
                        it.contains("\"available\":true")
            }

        assertEquals(
            1,
            availableStateCount
        )

        // Una tercera muestra no debe producir otro client/state.
        session.handleMessage(
            message = serverTime2,
            receivedAtLocalMicros = 2001000
        )

        delay(100)

        val availableStateCountAfterExtraSample =
            messageSender.encryptedMessages.count {
                it.contains("\"type\":\"client/state\"") &&
                        it.contains("\"available\":true")
            }

        assertEquals(
            1,
            availableStateCountAfterExtraSample
        )

        scope.cancel()
    }

    @Test
    fun serverActivateDetectsPairingPskActivity() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val session = createSession(
            transport = transport,
            messageSender = messageSender,
            scope = scope,
            pairingState = FakeSendspinPairingState(
                pskType = SendspinPskType.PAIRING
            )
        )

        val serverActivate =
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":["pairing"],
                "active_roles":[],
                "pairing":{
                    "method":"pairing_psk"
                }
            }
        }
        """.trimIndent()

        session.handleMessage(serverActivate)

        assertTrue(session.isPairingActivityActive)
        assertEquals(
            "pairing_psk",
            session.activePairingMethod
        )

        scope.cancel()
    }

    @Test
    fun serverActivateWithoutPairingActivityIsNotPairing() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val session = createSession(
            transport = transport,
            messageSender = messageSender,
            scope = scope
        )

        val serverActivate =
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":["playback"],
                "active_roles":["player@v1"]
            }
        }
        """.trimIndent()

        session.handleMessage(serverActivate)

        assertTrue(!session.isPairingActivityActive)
        assertEquals(
            null,
            session.activePairingMethod
        )

        scope.cancel()
    }

    @Test
    fun serverActivatePairingWithoutMethodKeepsMethodNull() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val session = createSession(
            transport = transport,
            messageSender = messageSender,
            scope = scope
        )

        val serverActivate =
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":["pairing"],
                "active_roles":[]
            }
        }
        """.trimIndent()

        session.handleMessage(serverActivate)

        assertTrue(session.isPairingActivityActive)
        assertEquals(
            null,
            session.activePairingMethod
        )

        scope.cancel()
    }

    @Test
    fun serverActivatePreservesUnsupportedPairingMethod() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val session = createSession(
            transport = transport,
            messageSender = messageSender,
            scope = scope
        )

        val serverActivate =
            """
        {
            "type":"server/activate",
            "payload":{
                "activities":["pairing"],
                "active_roles":[],
                "pairing":{
                    "method":"some_other_method"
                }
            }
        }
        """.trimIndent()

        session.handleMessage(serverActivate)

        assertTrue(session.isPairingActivityActive)
        assertEquals(
            "some_other_method",
            session.activePairingMethod
        )

        scope.cancel()
    }

    @Test
    fun streamStartConfiguresAndStartsAudio() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val audioLifecycle = FakeSendspinAudioLifecycle()

        val session =
            LegacySession(
                config = createConfig(),
                capabilities = createCapabilities(),
                transport = transport,
                messageSender = messageSender,
                clockSynchronizer = ClockSynchronizer(),
                scope = scope,
                pairingState = FakeSendspinPairingState(),
                pairingFinalizer = FakeSendspinPairingFinalizer(),
                audioLifecycle = audioLifecycle,
                credentialStorage = FakeSendspinCredentialStorage()
            )

        val streamStart =
            """
        {
            "type":"stream/start",
            "payload":{
                "player":{
                    "codec":"opus",
                    "sample_rate":48000,
                    "channels":2,
                    "bit_depth":16
                }
            }
        }
        """.trimIndent()

        session.handleMessage(streamStart)

        assertEquals(
            listOf(
                FakeSendspinAudioLifecycle.AudioConfiguration(
                    sampleRate = 48000,
                    channels = 2,
                    bitDepth = 16,
                    codec = "opus"
                )
            ),
            audioLifecycle.configurations
        )

        assertEquals(
            1,
            audioLifecycle.startCount
        )

        scope.cancel()
    }

    @Test
    fun serverUnpairRemovesLongTermPskSendsGoodbyeAndStopsSession() = runBlocking {
        val transport = FakeSendspinTransport()
        val messageSender = FakeSendspinMessageSender()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val credentialStorage = FakeSendspinCredentialStorage()

        val session = createSession(
            transport = transport,
            messageSender = messageSender,
            scope = scope,
            pairingState = FakeSendspinPairingState(
                serverId = "test-server-id"
            ),
            credentialStorage = credentialStorage
        )

        session.handleMessage(
            """
            {
                "type":"server/unpair",
                "payload":{}
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("test-server-id"),
            credentialStorage.removedServerIds
        )

        assertEquals(
            1,
            messageSender.encryptedMessages.count {
                it.contains("\"type\":\"client/goodbye\"")
            }
        )

        assertTrue(
            messageSender.encryptedMessages.any {
                it.contains("\"reason\":\"unpaired\"")
            }
        )

        assertTrue(
            !session.isActive
        )

        scope.cancel()
    }
}
