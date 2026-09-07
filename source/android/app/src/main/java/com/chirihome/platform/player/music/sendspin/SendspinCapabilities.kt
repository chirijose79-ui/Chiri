package com.chirihome.platform.player.music.sendspin

/**
 * Capacidades que Chiri anuncia al servidor Sendspin.
 *
 * Fase 1:
 * - Codec principal: Opus
 * - Salida PCM de 16 bits
 * - Audio estéreo/mono
 * - Reproducción sincronizada
 *
 * Estas capacidades se utilizan durante el handshake de Sendspin.
 */
data class SendspinCapabilities(
    val codecs: List<String> = listOf("opus"),
    val sampleRates: List<Int> = listOf(
        8000,
        12000,
        16000,
        24000,
        48000
    ),
    val bitDepths: List<Int> = listOf(16),
    val channels: List<Int> = listOf(1, 2),
    val supportsVolume: Boolean = true,
    val supportsMute: Boolean = true,
    val supportsPause: Boolean = true,
    val supportsSeek: Boolean = true,
    val supportsSynchronizedPlayback: Boolean = true
) {

    /**
     * Comprueba que las capacidades definidas son válidas.
     */
    fun isValid(): Boolean {
        return codecs.isNotEmpty() &&
                sampleRates.isNotEmpty() &&
                bitDepths.contains(16) &&
                channels.all { it in 1..2 }
    }

    /**
     * Indica si un codec concreto es compatible.
     */
    fun supportsCodec(codec: String): Boolean {
        return codecs.any { it.equals(codec, ignoreCase = true) }
    }

    /**
     * Indica si una frecuencia de muestreo es compatible.
     */
    fun supportsSampleRate(sampleRate: Int): Boolean {
        return sampleRates.contains(sampleRate)
    }

    /**
     * Indica si una profundidad de bits es compatible.
     */
    fun supportsBitDepth(bitDepth: Int): Boolean {
        return bitDepths.contains(bitDepth)
    }

    /**
     * Indica si una configuración de canales es compatible.
     */
    fun supportsChannels(channelCount: Int): Boolean {
        return channels.contains(channelCount)
    }
}
