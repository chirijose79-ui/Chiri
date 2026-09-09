package com.chirihome.platform.player.music.sendspin.session

import com.chirihome.platform.player.music.sendspin.transport.InboundTransportEvent
import kotlinx.coroutines.flow.Flow

/**
 * Abstracción de una sesión del protocolo Sendspin.
 *
 * La sesión se encuentra por encima del transporte:
 *
 * WebSocket
 *     ↓
 * SendspinTransport
 *     ↓
 * SendspinProtocolSession
 *     ↓
 * LegacySession
 *
 * La sesión es responsable de interpretar el protocolo Sendspin,
 * mientras que el transporte solamente mueve los mensajes.
 */
interface SendspinProtocolSession {

    /**
     * Eventos de protocolo producidos por la sesión.
     */
    val events: Flow<SendspinSessionEvent>

    /**
     * Indica si la sesión está activa.
     */
    val isActive: Boolean

    /**
     * Inicia la sesión sobre el transporte proporcionado.
     */
    suspend fun start()

    /**
     * Procesa un evento recibido desde el transporte.
     */
    suspend fun handleTransportEvent(
        event: InboundTransportEvent
    )

    /**
     * Procesa un mensaje de protocolo que ya fue
     * recibido y, si corresponde, descifrado por el cliente.
     */
    suspend fun handleMessage(
        message: String
    )

    /**
     * Envía un mensaje perteneciente al protocolo Sendspin.
     */
    suspend fun send(message: String)

    /**
     * Cierra la sesión.
     */
    suspend fun stop()
}

/**
 * Eventos de alto nivel producidos por una sesión Sendspin.
 *
 * En esta etapa solamente definimos el contrato.
 * Las implementaciones añadirán posteriormente los eventos
 * específicos del handshake, stream y comandos del servidor.
 */
sealed interface SendspinSessionEvent {

    /**
     * La sesión se inició.
     */
    data object Started : SendspinSessionEvent

    /**
     * El handshake/protocolo fue aceptado.
     */
    data object Ready : SendspinSessionEvent

    /**
     * La sesión fue cerrada.
     */
    data class Stopped(
        val cause: Throwable? = null
    ) : SendspinSessionEvent

    /**
     * Se recibió un mensaje que todavía no tiene
     * una interpretación específica.
     */
    data class MessageReceived(
        val message: String
    ) : SendspinSessionEvent

    /**
     * Se produjo un error de protocolo.
     */
    data class Error(
        val cause: Throwable
    ) : SendspinSessionEvent
}
