package com.chirihome.platform.player.music.sendspin.audio

/**
 * Interfaz común para los decodificadores de audio Sendspin.
 *
 * El decoder recibe datos comprimidos del stream y produce
 * PCM listo para ser enviado al reproductor de audio.
 *
 * Fase 1:
 * - Opus como codec principal.
 * - Salida PCM de 16 bits.
 *
 * Implementaciones futuras:
 * - OpusDecoder
 * - FlacDecoder
 * - PcmDecoder
 */
interface AudioDecoder {

    /**
     * Configura el decoder para el stream recibido.
     *
     * @param sampleRate frecuencia de muestreo en Hz.
     * @param channels número de canales.
     * @param bitDepth profundidad PCM de salida.
     */
    fun configure(
        sampleRate: Int,
        channels: Int,
        bitDepth: Int
    )

    /**
     * Decodifica un paquete de audio.
     *
     * La implementación debe devolver PCM intercalado
     * (interleaved) en formato de 16 bits cuando la
     * configuración del decoder lo permita.
     *
     * @param encodedData datos codificados recibidos.
     * @return muestras PCM de 16 bits.
     */
    fun decode(
        encodedData: ByteArray
    ): ShortArray

    /**
     * Reinicia el estado interno del decoder.
     *
     * Debe utilizarse cuando comienza un nuevo stream.
     */
    fun reset()

    /**
     * Libera los recursos utilizados por el decoder.
     */
    fun release()

    /**
     * Codec de entrada soportado por esta implementación.
     */
    fun getInputCodec(): String

    /**
     * Profundidad de bits del PCM producido.
     */
    fun getOutputBitDepth(): Int
}
