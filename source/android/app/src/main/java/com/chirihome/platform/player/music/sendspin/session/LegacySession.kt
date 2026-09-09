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
        val message = ClientHelloMessage(
            type = "client/hello",
            clientId = config.clientId,
            deviceName = config.deviceName,
            codecs = capabilities.codecs,
            sampleRates = capabilities.sampleRates,
            bitDepths = capabilities.bitDepths,
            channels = capabilities.channels
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
            sendClientHello()

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
        val type: String,
        val clientId: String,
        val deviceName: String,
        val codecs: List<String>,
        val sampleRates: List<Int>,
        val bitDepths: List<Int>,
        val channels: List<Int>
    )
}
