package com.chirihome.platform.player.music.sendspin.session

import com.chirihome.platform.player.music.sendspin.SendspinCapabilities
import com.chirihome.platform.player.music.sendspin.SendspinConfig
import com.chirihome.platform.player.music.sendspin.protocol.MessageDispatcher
import com.chirihome.platform.player.music.sendspin.protocol.SendspinPairingState
import com.chirihome.platform.player.music.sendspin.transport.InboundTransportEvent
import com.chirihome.platform.player.music.sendspin.transport.SendspinTransport
import com.chirihome.platform.player.music.sendspin.SendspinMessageSender
import com.chirihome.platform.player.music.sendspin.ClockSynchronizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Implementación de la sesión Legacy de Sendspin.
 *
 * Fase 1:
 * - Transporte WebSocket.
 * - Conexión LAN directa.
 * - Handshake inicial.
 * - Sin WebRTC.
 * - Sin Noise.
 * - Sin reproducción de audio.
 *
 * La sesión interpreta el protocolo que llega desde el transporte.
 */
class LegacySession(
    private val config: SendspinConfig,
    private val capabilities: SendspinCapabilities,
    private val transport: SendspinTransport,
    private val messageSender: SendspinMessageSender,
    private val clockSynchronizer: ClockSynchronizer,
    private val scope: CoroutineScope,
    private val pairingState: SendspinPairingState
) : SendspinProtocolSession {

    private val _events = MutableSharedFlow<SendspinSessionEvent>(
        extraBufferCapacity = 32
    )

    override val events: Flow<SendspinSessionEvent> =
        _events.asSharedFlow()

    private val active = AtomicBoolean(false)

    private var clockSyncJob: Job? = null

    private var clientHelloSent = false

    private var playerRoleActive = false

    private var clientStateAvailable: Boolean? = null

    override val isActive: Boolean
        get() = active.get()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val messageDispatcher = MessageDispatcher()

    override suspend fun start() {
        if (active.get()) {
            return
        }

        active.set(true)

        _events.emit(
            SendspinSessionEvent.Started
        )
    }

    override suspend fun handleTransportEvent(
        event: InboundTransportEvent
    ) {
        when (event) {

            InboundTransportEvent.Connected -> {
                if (!active.get()) {
                    active.set(true)

                    _events.emit(
                        SendspinSessionEvent.Started
                    )
                }
            }

            is InboundTransportEvent.TextMessage -> {
                handleTextMessage(event.message)
            }

            is InboundTransportEvent.BinaryMessage -> {
                // Los frames binarios de audio serán procesados
                // por AudioStreamManager en una fase posterior.
            }

            is InboundTransportEvent.Disconnected -> {
                clockSyncJob?.cancel()
                clockSyncJob = null

                active.set(false)

                _events.emit(
                    SendspinSessionEvent.Stopped(
                        cause = event.cause
                    )
                )
            }

            is InboundTransportEvent.Error -> {
                clockSyncJob?.cancel()
                clockSyncJob = null

                _events.emit(
                    SendspinSessionEvent.Error(
                        cause = event.cause
                    )
                )
            }
        }
    }

    override suspend fun send(message: String) {
        transport.send(message)
    }

    override suspend fun stop() {
        if (!active.compareAndSet(true, false)) {
            return
        }

        transport.disconnect()

        _events.emit(
            SendspinSessionEvent.Stopped()
        )
    }

    override suspend fun handleMessage(
        message: String
    ) {
        handleTextMessage(message)
    }

    override suspend fun handleMessage(
        message: String,
        receivedAtLocalMicros: Long
    ) {
        handleTextMessage(
            message = message,
            receivedAtLocalMicros = receivedAtLocalMicros
        )
    }

    /**
     * Envía el mensaje inicial del cliente.
     *
     * Este mensaje identifica al reproductor Chiri y anuncia
     * las capacidades que soporta.
     */
    private suspend fun sendClientHello() {
        val supportedFormats = capabilities.codecs.flatMap { codec ->
            capabilities.channels.flatMap { channels ->
                capabilities.sampleRates.map { sampleRate ->
                    SupportedAudioFormat(
                        codec = codec,
                        channels = channels,
                        sample_rate = sampleRate,
                        bit_depth = capabilities.bitDepths.first()
                    )
                }
            }
        }

        val supportedCommands = buildList {
            if (capabilities.supportsVolume) {
                add("volume")
            }

            if (capabilities.supportsMute) {
                add("mute")
            }
        }

        val message = ClientHelloMessage(
            payload = ClientHelloPayload(
                name = config.deviceName,
                supported_roles = listOf("player@v1"),
                device_info = DeviceInfo(
                    product_name = "Chiri Android",
                    manufacturer = "Chiri",
                    software_version = "1.0"
                ),
                playerSupport = PlayerSupport(
                    supported_formats = supportedFormats,
                    buffer_capacity = 512 * 1024,
                    supported_commands = supportedCommands
                ),
                unpaired_access = UnpairedAccess(
                    enabled = false
                )
            )
        )

        messageSender.sendEncrypted(
            json.encodeToString(message)
        )
    }

    /**
     * Procesa mensajes JSON recibidos desde Music Assistant.
     *
     * MessageDispatcher se encarga de identificar la categoría
     * del mensaje. En esta fase todavía conservamos el mensaje
     * original para no asumir un payload concreto del protocolo.
     */
    private suspend fun handleTextMessage(
        message: String,
        receivedAtLocalMicros: Long? = null
    ) {
        val type = runCatching {
            json.parseToJsonElement(message)
                .jsonObject["type"]
                ?.jsonPrimitive
                ?.content
        }.getOrNull()

        if (type == "server/hello") {
            if (!clientHelloSent) {
                sendClientHello()
                clientHelloSent = true
            }

            _events.emit(
                SendspinSessionEvent.MessageReceived(
                    message
                )
            )

            return
        }

        if (type == "server/activate") {
            processServerActivate(message)

            startClockSync()

            _events.emit(
                SendspinSessionEvent.MessageReceived(
                    message
                )
            )

            return
        }

        when (val result = messageDispatcher.dispatch(message)) {

            is MessageDispatcher.DispatchResult.Authentication -> {
                if (result.type == "auth_ok") {
                    _events.emit(
                        SendspinSessionEvent.Ready
                    )
                } else {
                    _events.emit(
                        SendspinSessionEvent.MessageReceived(
                            message
                        )
                    )
                }
            }

            is MessageDispatcher.DispatchResult.Stream -> {
                _events.emit(
                    SendspinSessionEvent.MessageReceived(
                        message
                    )
                )
            }

            is MessageDispatcher.DispatchResult.Synchronization -> {
                if (
                    result.type == "server/time" &&
                    receivedAtLocalMicros != null
                ) {
                    processServerTime(
                        result.payload,
                        receivedAtLocalMicros
                    )
                }

                _events.emit(
                    SendspinSessionEvent.MessageReceived(
                        message
                    )
                )
            }

            is MessageDispatcher.DispatchResult.Player -> {
                _events.emit(
                    SendspinSessionEvent.MessageReceived(
                        message
                    )
                )
            }

            is MessageDispatcher.DispatchResult.Metadata -> {
                _events.emit(
                    SendspinSessionEvent.MessageReceived(
                        message
                    )
                )
            }

            is MessageDispatcher.DispatchResult.Unknown -> {
                _events.emit(
                    SendspinSessionEvent.MessageReceived(
                        message
                    )
                )
            }

            is MessageDispatcher.DispatchResult.Invalid -> {
                _events.emit(
                    SendspinSessionEvent.Error(
                        result.cause
                            ?: IllegalArgumentException(
                                result.reason
                            )
                    )
                )
            }
        }
    }

    private suspend fun processServerActivate(
        message: String
    ) {
        val payload = json.parseToJsonElement(message)
            .jsonObject["payload"]
            ?.jsonObject
            ?: throw IllegalArgumentException(
                "server/activate message does not contain a payload"
            )

        val activeRoles = payload["active_roles"]
            ?.jsonArray
            ?.mapNotNull { element ->
                element.jsonPrimitive.contentOrNull
            }
            ?: emptyList()

        val playerActive = "player@v1" in activeRoles

        if (playerActive != playerRoleActive) {
            playerRoleActive = playerActive

            if (playerRoleActive) {
                sendClientState(
                    available = false
                )
            } else {
                clientStateAvailable = null
            }
        }
    }

    private suspend fun sendClientState(
        available: Boolean
    ) {
        val supportedCommands = buildList {
            if (capabilities.supportsVolume) {
                add("volume")
            }

            if (capabilities.supportsMute) {
                add("mute")
            }
        }

        val message = ClientStateMessage(
            payload = ClientStatePayload(
                available = available,
                player = PlayerState(
                    volume = if (capabilities.supportsVolume) 100 else null,
                    muted = if (capabilities.supportsMute) false else null,
                    output_delay_ms = 0,
                    required_lead_time_ms = 250,
                    min_buffer_ms = 250,
                    supported_commands = supportedCommands
                )
            )
        )

        if (clientStateAvailable == available) {
            return
        }

        messageSender.sendEncrypted(
            json.encodeToString(message)
        )

        clientStateAvailable = available
    }

    private fun processServerTime(
        message: kotlinx.serialization.json.JsonObject,
        receivedAtLocalMicros: Long
    ) {
        val payload = message["payload"]?.jsonObject
            ?: throw IllegalArgumentException(
                "server/time message does not contain a payload"
            )

        val clientTransmitted = payload["client_transmitted"]
            ?.jsonPrimitive
            ?.content
            ?.toLongOrNull()
            ?: throw IllegalArgumentException(
                "server/time payload does not contain client_transmitted"
            )

        val serverReceived = payload["server_received"]
            ?.jsonPrimitive
            ?.content
            ?.toLongOrNull()
            ?: throw IllegalArgumentException(
                "server/time payload does not contain server_received"
            )

        val serverTransmitted = payload["server_transmitted"]
            ?.jsonPrimitive
            ?.content
            ?.toLongOrNull()
            ?: throw IllegalArgumentException(
                "server/time payload does not contain server_transmitted"
            )

        clockSynchronizer.update(
            t1LocalMicros = clientTransmitted,
            t2ServerMicros = serverReceived,
            t3ServerMicros = serverTransmitted,
            t4LocalMicros = receivedAtLocalMicros
        )

        if (
            playerRoleActive &&
            clockSynchronizer.isSynchronized() &&
            clientStateAvailable != true
        ) {
            scope.launch {
                sendClientState(
                    available = true
                )
            }
        }
    }

    /**
     * Envía una muestra de sincronización del reloj.
     *
     * T1 = instante local en que el cliente transmite client/time.
     */
    internal suspend fun sendClientTime() {
        val clientTransmitted =
            clockSynchronizer.localTimeMicros()

        val message = ClientTimeMessage(
            payload = ClientTimePayload(
                clientTransmitted = clientTransmitted
            )
        )

        messageSender.sendEncrypted(
            json.encodeToString(message)
        )
    }

    private fun startClockSync() {
        clockSyncJob?.cancel()

        clockSyncJob = scope.launch {
            while (isActive) {
                try {
                    sendClientTime()
                    delay(1_000)
                } catch (exception: IllegalStateException) {
                    break
                } catch (exception: Exception) {
                    break
                }
            }
        }
    }

    @Serializable
    private data class ClientHelloMessage(
        val type: String = "client/hello",
        val payload: ClientHelloPayload
    )

    @Serializable
    private data class ClientHelloPayload(
        val name: String,
        val supported_roles: List<String>,
        val device_info: DeviceInfo? = null,
        @SerialName("player@v1_support")
        val playerSupport: PlayerSupport? = null,
        val unpaired_access: UnpairedAccess = UnpairedAccess(),
        val supported_pair_methods: List<String> = listOf("pairing_psk")
    )

    @Serializable
    private data class DeviceInfo(
        val product_name: String? = null,
        val manufacturer: String? = null,
        val software_version: String? = null
    )

    @Serializable
    private data class PlayerSupport(
        val supported_formats: List<SupportedAudioFormat>,
        val buffer_capacity: Int,
        val supported_commands: List<String>
    )

    @Serializable
    private data class SupportedAudioFormat(
        val codec: String,
        val channels: Int,
        val sample_rate: Int,
        val bit_depth: Int
    )

    @Serializable
    private data class UnpairedAccess(
        val enabled: Boolean = false
    )

    @Serializable
    private data class ClientTimeMessage(
        val type: String = "client/time",
        val payload: ClientTimePayload
    )

    @Serializable
    private data class ClientTimePayload(
        @SerialName("client_transmitted")
        val clientTransmitted: Long
    )

    @Serializable
    private data class ClientStateMessage(
        val type: String = "client/state",
        val payload: ClientStatePayload
    )

    @Serializable
    private data class ClientStatePayload(
        val available: Boolean,
        @SerialName("player@v1")
        val player: PlayerState? = null
    )

    @Serializable
    private data class PlayerState(
        val volume: Int? = null,
        val muted: Boolean? = null,
        val output_delay_ms: Int,
        val required_lead_time_ms: Int,
        val min_buffer_ms: Int,
        val supported_commands: List<String>
    )
}
