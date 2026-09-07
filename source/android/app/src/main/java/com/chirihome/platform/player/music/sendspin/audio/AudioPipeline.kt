package com.chirihome.platform.player.music.sendspin.audio

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AudioPipeline(
    private val decoder: AudioDecoder,
    private val player: MediaPlayerControllerAndroid
) {

    private val mutex = Mutex()

    private var configured = false
    private var playing = false

    suspend fun configure(
        sampleRate: Int,
        channels: Int,
        bitDepth: Int
    ) {
        mutex.withLock {
            decoder.configure(
                sampleRate = sampleRate,
                channels = channels,
                bitDepth = bitDepth
            )

            player.configure(
                sampleRate = sampleRate,
                channels = channels,
                bitDepth = bitDepth
            )

            configured = true
            playing = false
        }
    }

    suspend fun processAudioPacket(encodedData: ByteArray) {
        mutex.withLock {
            check(configured) {
                "AudioPipeline is not configured"
            }

            if (encodedData.isEmpty()) {
                return
            }

            val pcmData = decoder.decode(encodedData)

            if (pcmData.isNotEmpty()) {
                player.write(pcmData)
            }
        }
    }

    suspend fun play() {
        mutex.withLock {
            check(configured) {
                "AudioPipeline is not configured"
            }

            player.play()
            playing = true
        }
    }

    suspend fun pause() {
        mutex.withLock {
            if (!configured) {
                return
            }

            player.pause()
            playing = false
        }
    }

    suspend fun stop() {
        mutex.withLock {
            if (!configured) {
                return
            }

            player.stop()
            playing = false
        }
    }

    suspend fun flush() {
        mutex.withLock {
            if (!configured) {
                return
            }

            player.flush()
        }
    }

    suspend fun reset() {
        mutex.withLock {
            if (!configured) {
                return
            }

            player.pause()
            player.flush()
            decoder.reset()
            playing = false
        }
    }

    suspend fun release() {
        mutex.withLock {
            player.release()
            decoder.release()

            configured = false
            playing = false
        }
    }

    fun isConfigured(): Boolean = configured

    fun isPlaying(): Boolean = playing

    fun getInputCodec(): String = decoder.getInputCodec()

    fun getOutputBitDepth(): Int = decoder.getOutputBitDepth()

    fun getSampleRate(): Int = player.getSampleRate()

    fun getChannels(): Int = player.getChannels()
}
