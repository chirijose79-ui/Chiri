package com.chirihome.platform.player.music.sendspin.transport

import kotlinx.coroutines.flow.Flow

/**
 * Abstracción del transporte utilizado por Sendspin.
 */
interface SendspinTransport {

    /**
     * Eventos recibidos desde el transporte.
     */
    val events: Flow<InboundTransportEvent>

    /**
     * Indica si existe una conexión activa.
     */
    val isConnected: Boolean

    /**
     * Establece la conexión.
     */
    suspend fun connect()

    /**
     * Envía un mensaje de texto.
     */
    suspend fun send(message: String)

    /**
     * Envía un mensaje binario.
     *
     * Se utiliza para el handshake Noise y,
     * posteriormente, para los frames binarios del protocolo.
     */
    suspend fun sendBinary(data: ByteArray)

    /**
     * Cierra la conexión.
     */
    suspend fun disconnect()
}