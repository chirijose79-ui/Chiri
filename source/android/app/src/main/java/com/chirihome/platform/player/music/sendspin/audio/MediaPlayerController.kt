package com.chirihome.platform.player.music.sendspin.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MediaPlayerControllerAndroid {

    private val mutex = Mutex()

    private var audioTrack: AudioTrack? = null
    private var sampleRate: Int = 48000
    private var channels: Int = 2
    private var bitDepth: Int = 16

    suspend fun configure(
        sampleRate: Int,
        channels: Int,
        bitDepth: Int
    ) {
        require(sampleRate > 0) {
            "Sample rate must be greater than zero"
        }

        require(channels in 1..2) {
            "Only mono and stereo are supported"
        }

        require(bitDepth == 16) {
            "Only 16-bit PCM is supported"
        }

        mutex.withLock {
            releaseInternal()

            this.sampleRate = sampleRate
            this.channels = channels
            this.bitDepth = bitDepth

            val channelConfig = when (channels) {
                1 -> AudioFormat.CHANNEL_OUT_MONO
                2 -> AudioFormat.CHANNEL_OUT_STEREO
                else -> error("Unsupported channel count: $channels")
            }

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                channelConfig,
                AudioFormat.ENCODING_PCM_16BIT
            )

            require(minBufferSize > 0) {
                "Unable to determine AudioTrack buffer size"
            }

            val bufferSize = maxOf(
                minBufferSize,
                sampleRate * channels * 2 / 5
            )

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(channelConfig)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferSize)
                .build()

            check(audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                "AudioTrack initialization failed"
            }
        }
    }

    suspend fun play() {
        mutex.withLock {
            audioTrack?.play()
        }
    }

    suspend fun pause() {
        mutex.withLock {
            audioTrack?.pause()
        }
    }

    suspend fun write(pcmData: ShortArray) {
        require(pcmData.isNotEmpty()) {
            "PCM data must not be empty"
        }

        mutex.withLock {
            val track = audioTrack
                ?: error("AudioTrack is not configured")

            var offset = 0

            while (offset < pcmData.size) {
                val written = track.write(
                    pcmData,
                    offset,
                    pcmData.size - offset
                )

                if (written < 0) {
                    error("AudioTrack write failed: $written")
                }

                if (written == 0) {
                    break
                }

                offset += written
            }
        }
    }

    suspend fun stop() {
        mutex.withLock {
            audioTrack?.stop()
        }
    }

    suspend fun flush() {
        mutex.withLock {
            audioTrack?.flush()
        }
    }

    suspend fun release() {
        mutex.withLock {
            releaseInternal()
        }
    }

    fun isInitialized(): Boolean {
        return audioTrack?.state == AudioTrack.STATE_INITIALIZED
    }

    fun getSampleRate(): Int = sampleRate

    fun getChannels(): Int = channels

    fun getBitDepth(): Int = bitDepth

    private fun releaseInternal() {
        audioTrack?.let { track ->
            runCatching {
                track.stop()
            }

            track.release()
        }

        audioTrack = null
    }
}