package com.chirihome.platform.player.music.sendspin.transport

/**
 * Eventos recibidos desde el transporte Sendspin.
 */
sealed interface InboundTransportEvent {

    /**
     * La conexión se estableció correctamente.
     */
    data object Connected : InboundTransportEvent

    /**
     * Se recibió un mensaje de texto.
     */
    data class TextMessage(
        val message: String
    ) : InboundTransportEvent

    /**
     * Se recibió un mensaje binario.
     */
    data class BinaryMessage(
        val data: ByteArray
    ) : InboundTransportEvent

    /**
     * La conexión fue cerrada.
     */
    data class Disconnected(
        val cause: Throwable? = null
    ) : InboundTransportEvent

    /**
     * Ocurrió un error en el transporte.
     */
    data class Error(
        val cause: Throwable
    ) : InboundTransportEvent
}
