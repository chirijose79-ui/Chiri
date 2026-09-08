package com.chirihome.platform.player.music.sendspin

import com.chirihome.platform.player.music.sendspin.transport.InboundTransportEvent
import com.chirihome.platform.player.music.sendspin.transport.SendspinTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Cliente principal de Sendspin.
 *
 * Esta primera implementación administra el ciclo de vida
 * del transporte y centraliza la recepción de eventos.
 *
 * La negociación Noise y el protocolo Sendspin se integrarán
 * progresivamente sobre esta base.
 */
class SendspinClient(
    private val transport: SendspinTransport,
    private val scope: CoroutineScope
) {

    private var eventJob: Job? = null

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
     *
     * Se utilizará para el handshake Noise y posteriormente
     * para los frames binarios del protocolo Sendspin.
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
    private fun handleTransportEvent(
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
     */
    private fun handleConnected() {
        // Sendspin protocol initialization will be added next.
    }

    /**
     * Se invoca cuando se recibe un mensaje de texto.
     */
    private fun handleTextMessage(message: String) {
        // Sendspin protocol message handling will be added next.
    }

    /**
     * Se invoca cuando se recibe un mensaje binario.
     */
    private fun handleBinaryMessage(data: ByteArray) {
        // Noise handshake handling will be added next.
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
}