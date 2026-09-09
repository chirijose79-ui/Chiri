package com.chirihome.platform.player.music.sendspin.audio

import com.chirihome.platform.player.music.sendspin.ClockSynchronizer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates incoming Sendspin audio frames with the audio pipeline.
 *
 * This first implementation deliberately keeps the wire-format parser
 * outside this class. The Sendspin protocol/session layer will provide
 * already-separated audio frames once the exact MA 2.6.0 wire format
 * is integrated.
 *
 * Responsibilities:
 * - configure the audio pipeline;
 * - accept decoded Sendspin audio frame metadata;
 * - apply server/local clock conversion;
 * - reject frames that are already too late;
 * - forward valid encoded audio packets to AudioPipeline;
 * - control stream lifecycle.
 */
class AudioStreamManager(
    private val audioPipeline: AudioPipeline,
    private val clockSynchronizer: ClockSynchronizer
) : SendspinAudioSink {

    private val mutex = Mutex()

    private var configured = false
    private var streaming = false

    private var sampleRate = 48000
    private var channels = 2
    private var bitDepth = 16
    private var codec = "opus"

    private var acceptedFrames = 0L
    private var droppedLateFrames = 0L
    private var processedFrames = 0L

    suspend fun configure(
        sampleRate: Int,
        channels: Int,
        bitDepth: Int,
        codec: String
    ) {
        mutex.withLock {
            require(sampleRate > 0) {
                "Sample rate must be greater than zero"
            }

            require(channels in 1..2) {
                "Only mono and stereo are supported"
            }

            require(bitDepth == 16) {
                "Only 16-bit PCM output is supported"
            }

            require(codec.equals("opus", ignoreCase = true)) {
                "Unsupported Sendspin codec: $codec"
            }

            audioPipeline.configure(
                sampleRate = sampleRate,
                channels = channels,
                bitDepth = bitDepth
            )

            this.sampleRate = sampleRate
            this.channels = channels
            this.bitDepth = bitDepth
            this.codec = codec.lowercase()

            configured = true
            streaming = false

            acceptedFrames = 0L
            droppedLateFrames = 0L
            processedFrames = 0L
        }
    }

    /**
     * Starts audio output.
     */
    suspend fun start() {
        mutex.withLock {
            check(configured) {
                "AudioStreamManager is not configured"
            }

            if (streaming) {
                return
            }

            audioPipeline.play()
            streaming = true
        }
    }

    /**
     * Temporarily pauses audio output without destroying the pipeline.
     */
    suspend fun pause() {
        mutex.withLock {
            if (!configured || !streaming) {
                return
            }

            audioPipeline.pause()
            streaming = false
        }
    }

    /**
     * Stops the current stream and clears pending audio from AudioTrack.
     */
    suspend fun stop() {
        mutex.withLock {
            if (!configured) {
                return
            }

            audioPipeline.stop()
            audioPipeline.flush()

            streaming = false
        }
    }

    /**
     * Resets decoder and audio output while keeping the stream configured.
     */
    suspend fun reset() {
        mutex.withLock {
            if (!configured) {
                return
            }

            audioPipeline.reset()
            streaming = false
        }
    }

    /**
     * Processes an incoming encoded Sendspin audio frame.
     *
     * serverTimestampMicros is expressed in the Sendspin server clock.
     * The frame is converted to the local monotonic clock using the
     * shared ClockSynchronizer.
     */
    override suspend fun processFrame(
        encodedData: ByteArray,
        serverTimestampMicros: Long
    ) {
        mutex.withLock {
            check(configured) {
                "AudioStreamManager is not configured"
            }

            if (encodedData.isEmpty()) {
                return
            }

            val localTimestampMicros =
                clockSynchronizer.serverTimeToLocalMicros(
                    serverTimestampMicros
                )

            val nowMicros = clockSynchronizer.localTimeMicros()

            val latenessMicros = nowMicros - localTimestampMicros

            if (latenessMicros > MAX_LATE_FRAME_MICROS) {
                droppedLateFrames++
                return
            }

            acceptedFrames++

            audioPipeline.processAudioPacket(encodedData)

            processedFrames++
        }
    }

    /**
     * Processes an audio frame when no server timestamp is available.
     *
     * This is intentionally provided only as a compatibility path.
     * Once the exact Sendspin 2.6.0 audio frame format is integrated,
     * timestamped frames should be preferred.
     */
    suspend fun processFrame(encodedData: ByteArray) {
        mutex.withLock {
            check(configured) {
                "AudioStreamManager is not configured"
            }

            if (encodedData.isEmpty()) {
                return
            }

            acceptedFrames++

            audioPipeline.processAudioPacket(encodedData)

            processedFrames++
        }
    }

    suspend fun release() {
        mutex.withLock {
            if (!configured) {
                return
            }

            audioPipeline.release()

            configured = false
            streaming = false

            acceptedFrames = 0L
            droppedLateFrames = 0L
            processedFrames = 0L
        }
    }

    fun isConfigured(): Boolean = configured

    fun isStreaming(): Boolean = streaming

    fun getSampleRate(): Int = sampleRate

    fun getChannels(): Int = channels

    fun getBitDepth(): Int = bitDepth

    fun getCodec(): String = codec

    fun getAcceptedFrames(): Long = acceptedFrames

    fun getDroppedLateFrames(): Long = droppedLateFrames

    fun getProcessedFrames(): Long = processedFrames

    companion object {
        /**
         * Frames arriving more than 100 ms late are discarded.
         *
         * This matches the intended first-phase Sendspin behavior:
         * late audio should not be injected into the live playback path.
         */
        private const val MAX_LATE_FRAME_MICROS = 100_000L
    }
}
