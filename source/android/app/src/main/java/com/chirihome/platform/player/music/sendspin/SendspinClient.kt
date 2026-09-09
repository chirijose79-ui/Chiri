package com.chirihome.platform.player.music.sendspin

import com.chirihome.platform.player.music.sendspin.crypto.NoiseTransport
import com.chirihome.platform.player.music.sendspin.protocol.SendspinHandshake
import com.chirihome.platform.player.music.sendspin.session.SendspinProtocolSession
import com.chirihome.platform.player.music.sendspin.transport.InboundTransportEvent
import com.chirihome.platform.player.music.sendspin.transport.SendspinTransport
import com.chirihome.platform.player.music.sendspin.audio.SendspinAudioSink
import com.chirihome.platform.player.music.sendspin.protocol.SendspinAudioFrameParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Cliente principal de Sendspin.
 *
 * Administra el ciclo de vida del transporte y coordina
 * la negociación inicial del protocolo Sendspin.
 */
class SendspinClient(
    private val transport: SendspinTransport,
    private val handshake: SendspinHandshake,
    private val session: SendspinProtocolSession,
    private val audioSink: SendspinAudioSink,
    private val scope: CoroutineScope
) : SendspinMessageSender {

    private var eventJob: Job? = null

    private var noiseTransport: NoiseTransport? = null

    /**
     * Indica si existe una conexión activa.
     */
    val isConnected: Boolean
        get() = transport.isConnected

    /**
     * Conecta con el servidor Sendspin.
     */
    suspend fun connect() {
        if (transport.isConnected) {
            return
        }

        startEventCollection()
        transport.connect()
    }

    /**
     * Desconecta del servidor Sendspin.
     */
    suspend fun disconnect() {
        eventJob?.cancel()
        eventJob = null

        if (transport.isConnected) {
            transport.disconnect()
        }
    }

    /**
     * Envía un mensaje de texto.
     */
    suspend fun send(message: String) {
        check(transport.isConnected) {
            "Sendspin client is not connected"
        }

        transport.send(message)
    }

    /**
     * Envía un mensaje binario.
     */
    suspend fun sendBinary(data: ByteArray) {
        check(transport.isConnected) {
            "Sendspin client is not connected"
        }

        transport.sendBinary(data)
    }

    /**
     * Inicia la recepción de eventos del transporte.
     */
    private fun startEventCollection() {
        if (eventJob?.isActive == true) {
            return
        }

        eventJob = scope.launch {
            transport.events.collect { event ->
                handleTransportEvent(event)
            }
        }
    }

    /**
     * Procesa los eventos recibidos desde el transporte.
     */
    private suspend fun handleTransportEvent(
        event: InboundTransportEvent
    ) {
        when (event) {
            is InboundTransportEvent.Connected -> {
                handleConnected()
            }

            is InboundTransportEvent.TextMessage -> {
                handleTextMessage(event.message)
            }

            is InboundTransportEvent.BinaryMessage -> {
                handleBinaryMessage(event.data)
            }

            is InboundTransportEvent.Disconnected -> {
                handleDisconnected(event.cause)
            }

            is InboundTransportEvent.Error -> {
                handleError(event.cause)
            }
        }
    }

    /**
     * Se invoca cuando el transporte establece la conexión.
     *
     * Primera transición del protocolo:
     *
     * Connected
     *     ↓
     * createClientInit()
     *     ↓
     * client/init
     */
    private suspend fun handleConnected() {
        val clientInit = handshake.createClientInit()

        transport.send(clientInit)
    }

    /**
     * Se invoca cuando se recibe un mensaje de texto.
     *
     * En la fase inicial del protocolo Sendspin se reciben
     * mensajes de texto JSON.
     *
     * server/init
     *     ↓
     * receiveServerInit()
     *
     * noise/handshake
     *     ↓
     * receiveNoiseMessage1()
     *     ↓
     * noise/handshake Message 2
     */
    private suspend fun handleTextMessage(message: String) {
        val type = Json
            .parseToJsonElement(message)
            .jsonObject["type"]
            ?.jsonPrimitive
            ?.content

        when (type) {
            "server/init" -> {
                handshake.receiveServerInit(message)
            }

            "noise/handshake" -> {
                val noiseMessage2 =
                    handshake.receiveNoiseMessage1(message)

                transport.send(noiseMessage2)

                noiseTransport =
                    handshake.noiseTransport
                        ?: error("Noise handshake did not produce a transport")
            }

            else -> {
                error("Unsupported Sendspin text message type: $type")
            }
        }
    }

    /**
     * Se invoca cuando se recibe un mensaje binario.
     */
    private suspend fun handleBinaryMessage(data: ByteArray) {
        val transport =
            noiseTransport
                ?: error(
                    "Received encrypted message before Noise transport was established"
                )

        val plaintext =
            transport.decrypt(data)

        check(plaintext.isNotEmpty()) {
            "Received empty Sendspin message"
        }

        val messageType = plaintext[0].toInt() and 0xFF

        when (messageType) {
            0x00 -> {
                val message =
                    plaintext
                        .copyOfRange(1, plaintext.size)
                        .toString(Charsets.UTF_8)

                session.handleMessage(message)
            }

            0x04 -> {
                val audioFrame =
                    SendspinAudioFrameParser.parse(plaintext)

                audioSink.processFrame(
                    encodedData = audioFrame.encodedData,
                    serverTimestampMicros = audioFrame.serverTimestampMicros
                )
            }

            else -> {
                error(
                    "Unsupported Sendspin message type: $messageType"
                )
            }
        }
    }

    /**
     * Se invoca cuando la conexión se cierra.
     */
    private fun handleDisconnected(cause: Throwable?) {
        eventJob?.cancel()
        eventJob = null
    }

    /**
     * Se invoca cuando ocurre un error de transporte.
     */
    private fun handleError(cause: Throwable) {
        eventJob?.cancel()
        eventJob = null
    }

    override suspend fun sendEncrypted(message: String) {
        check(transport.isConnected) {
            "Sendspin client is not connected"
        }

        val noise = noiseTransport
            ?: error("Noise transport is not established")

        val jsonBytes = message.toByteArray(Charsets.UTF_8)

        val plaintext = ByteArray(1 + jsonBytes.size)
        plaintext[0] = 0x00
        jsonBytes.copyInto(
            destination = plaintext,
            destinationOffset = 1
        )

        val encrypted = noise.encrypt(plaintext)

        transport.sendBinary(encrypted)
    }
}