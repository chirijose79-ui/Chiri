package com.chirihome.platform.player.music.sendspin.audio

import io.github.jaredmdobson.concentus.OpusDecoder
import io.github.jaredmdobson.concentus.OpusException

/**
 * Decoder Opus para Android.
 *
 * Convierte paquetes Opus a PCM lineal de 16 bits.
 *
 * Fase 1:
 * - Codec de entrada: Opus
 * - PCM de salida: signed 16-bit
 * - Canales: mono o estéreo
 * - Sample rates: 8, 12, 16, 24 y 48 kHz
 */
class OpusDecoderAndroid : AudioDecoder {

    private var decoder: OpusDecoder? = null

    private var sampleRate: Int = 48000
    private var channels: Int = 2

    private var outputBuffer = ShortArray(0)

    private var configured = false

    override fun configure(
        sampleRate: Int,
        channels: Int,
        bitDepth: Int
    ) {
        require(sampleRate in SUPPORTED_SAMPLE_RATES) {
            "Unsupported Opus sample rate: $sampleRate"
        }

        require(channels in SUPPORTED_CHANNELS) {
            "Unsupported Opus channel count: $channels"
        }

        require(bitDepth == 16) {
            "OpusDecoderAndroid only supports 16-bit PCM output"
        }

        this.sampleRate = sampleRate
        this.channels = channels

        decoder = try {
            OpusDecoder(
                sampleRate,
                channels
            )
        } catch (exception: OpusException) {
            throw IllegalStateException(
                "Failed to create Opus decoder",
                exception
            )
        }

        outputBuffer = ShortArray(0)
        configured = true
    }

    override fun decode(
        encodedData: ByteArray
    ): ShortArray {
        check(configured) {
            "OpusDecoderAndroid is not configured"
        }

        require(encodedData.isNotEmpty()) {
            "Opus packet must not be empty"
        }

        val currentDecoder = decoder
            ?: error("Opus decoder is not initialized")

        /*
         * Opus permite hasta 120 ms de audio por paquete.
         *
         * 48 kHz * 120 ms = 5760 muestras por canal.
         */
        val maxSamplesPerChannel =
            sampleRate * MAX_PACKET_DURATION_MS / 1000

        val requiredSize =
            maxSamplesPerChannel * channels

        if (outputBuffer.size < requiredSize) {
            outputBuffer = ShortArray(requiredSize)
        }

        return try {
            val samplesPerChannel = currentDecoder.decode(
                encodedData,
                0,
                encodedData.size,
                outputBuffer,
                0,
                maxSamplesPerChannel,
                false
            )

            val outputSize =
                samplesPerChannel * channels

            outputBuffer.copyOf(outputSize)
        } catch (exception: OpusException) {
            throw IllegalStateException(
                "Failed to decode Opus packet",
                exception
            )
        }
    }

    override fun reset() {
        if (!configured) {
            return
        }

        decoder = try {
            OpusDecoder(
                sampleRate,
                channels
            )
        } catch (exception: OpusException) {
            throw IllegalStateException(
                "Failed to reset Opus decoder",
                exception
            )
        }

        outputBuffer = ShortArray(0)
    }

    override fun release() {
        decoder = null
        outputBuffer = ShortArray(0)
        configured = false
    }

    override fun getInputCodec(): String {
        return "opus"
    }

    override fun getOutputBitDepth(): Int {
        return 16
    }

    companion object {
        private const val MAX_PACKET_DURATION_MS = 120

        private val SUPPORTED_SAMPLE_RATES = setOf(
            8000,
            12000,
            16000,
            24000,
            48000
        )

        private val SUPPORTED_CHANNELS = setOf(
            1,
            2
        )
    }
}
