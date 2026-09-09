package com.chirihome.platform.player.music.sendspin.session

import com.chirihome.platform.player.music.sendspin.SendspinCapabilities
import com.chirihome.platform.player.music.sendspin.SendspinConfig
import com.chirihome.platform.player.music.sendspin.protocol.MessageDispatcher
import com.chirihome.platform.player.music.sendspin.transport.InboundTransportEvent
import com.chirihome.platform.player.music.sendspin.transport.SendspinTransport
import com.chirihome.platform.player.music.sendspin.SendspinMessageSender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    private val messageSender: SendspinMessageSender
) : SendspinProtocolSession {

    private val _events = MutableSharedFlow<SendspinSessionEvent>(
        extraBufferCapacity = 32
    )

    override val events: Flow<SendspinSessionEvent> =
        _events.asSharedFlow()

    private val active = AtomicBoolean(false)

    private var clientHelloSent = false

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
                active.set(false)

                _events.emit(
                    SendspinSessionEvent.Stopped(
                        cause = event.cause
                    )
                )
            }

            is InboundTransportEvent.Error -> {
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
        message: String
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
        val unpaired_access: UnpairedAccess = UnpairedAccess()
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
}
