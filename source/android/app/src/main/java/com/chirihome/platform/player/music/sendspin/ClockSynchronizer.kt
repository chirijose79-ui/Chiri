package com.chirihome.platform.player.music.sendspin

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.time.TimeSource

/**
 * Sincroniza el reloj monotónico local del cliente con el reloj del servidor
 * Sendspin.
 *
 * Implementa el modelo offset + drift utilizado por aiosendspin.
 *
 * T1 = cliente transmite client/time
 * T2 = servidor recibe client/time
 * T3 = servidor transmite server/time
 * T4 = cliente recibe server/time
 */
class ClockSynchronizer {

    private val timeSource = TimeSource.Monotonic
    private val startMark = timeSource.markNow()

    @Volatile
    private var serverOffsetMicros: Long = 0L

    @Volatile
    private var roundTripTimeMicros: Long = 0L

    @Volatile
    private var synchronized: Boolean = false

    @Volatile
    private var sampleCount: Int = 0

    private var filter = SendspinTimeFilter()

    fun getOffsetMicros(): Long = serverOffsetMicros

    fun getRoundTripTimeMicros(): Long = roundTripTimeMicros

    fun isSynchronized(): Boolean = synchronized

    fun localTimeMicros(): Long =
        startMark.elapsedNow().inWholeMicroseconds

    /**
     * Procesa una medición completa T1/T2/T3/T4.
     */
    @Synchronized
    fun update(
        t1LocalMicros: Long,
        t2ServerMicros: Long,
        t3ServerMicros: Long,
        t4LocalMicros: Long
    ) {
        if (t4LocalMicros < t1LocalMicros) {
            return
        }

        if (t3ServerMicros < t2ServerMicros) {
            return
        }

        val roundTripMicros =
            (t4LocalMicros - t1LocalMicros) -
                    (t3ServerMicros - t2ServerMicros)

        if (roundTripMicros < 0L) {
            return
        }

        val measuredOffsetMicros =
            (
                    (t2ServerMicros - t1LocalMicros) +
                            (t3ServerMicros - t4LocalMicros)
                    ) / 2L

        /*
         * aiosendspin utiliza la mitad del RTT efectivo como
         * incertidumbre de la medición.
         */
        val maxErrorMicros =
            roundTripMicros / 2L

        if (sampleCount == 0) {
            filter.reset(
                offsetMicros = measuredOffsetMicros,
                timestampMicros = t4LocalMicros
            )

            serverOffsetMicros = measuredOffsetMicros
            roundTripTimeMicros = roundTripMicros
            sampleCount = 1
            synchronized = false
            return
        }

        filter.update(
            measurementMicros = measuredOffsetMicros,
            maxErrorMicros = maxErrorMicros,
            timestampMicros = t4LocalMicros
        )

        serverOffsetMicros =
            filter.computeOffset(
                timestampMicros = t4LocalMicros
            )

        roundTripTimeMicros =
            roundTripMicros

        sampleCount++

        synchronized =
            filter.isSynchronized()
    }

    fun serverTimeToLocalMicros(
        serverTimeMicros: Long
    ): Long {
        if (!synchronized) {
            return serverTimeMicros - serverOffsetMicros
        }

        return filter.computeClientTime(
            serverTimeMicros
        )
    }

    fun localTimeToServerMicros(
        localTimeMicros: Long
    ): Long {
        if (!synchronized) {
            return localTimeMicros + serverOffsetMicros
        }

        return filter.computeServerTime(
            localTimeMicros
        )
    }

    @Synchronized
    fun reset() {
        serverOffsetMicros = 0L
        roundTripTimeMicros = 0L
        synchronized = false
        sampleCount = 0
        filter = SendspinTimeFilter()
    }

    /**
     * Filtro 2D:
     *
     * x[0] = offset
     * x[1] = drift
     *
     * El estado evoluciona con el tiempo:
     *
     * offset(t) = offset0 + drift * dt
     */
    private class SendspinTimeFilter {

        private var offsetMicros = 0.0
        private var drift = 0.0

        private var covariance00 = INITIAL_OFFSET_VARIANCE
        private var covariance01 = 0.0
        private var covariance10 = 0.0
        private var covariance11 = INITIAL_DRIFT_VARIANCE

        private var lastTimestampMicros = 0L
        private var measurementCount = 0

        fun reset(
            offsetMicros: Long,
            timestampMicros: Long
        ) {
            this.offsetMicros = offsetMicros.toDouble()
            this.drift = 0.0

            covariance00 = INITIAL_OFFSET_VARIANCE
            covariance01 = 0.0
            covariance10 = 0.0
            covariance11 = INITIAL_DRIFT_VARIANCE

            lastTimestampMicros = timestampMicros
            measurementCount = 1
        }

        fun update(
            measurementMicros: Long,
            maxErrorMicros: Long,
            timestampMicros: Long
        ) {
            val deltaSeconds =
                (timestampMicros - lastTimestampMicros)
                    .coerceAtLeast(0L) / 1_000_000.0

            /*
             * Prediction.
             */
            offsetMicros += drift * deltaSeconds

            covariance00 +=
                deltaSeconds *
                        (covariance10 + covariance01) +
                        deltaSeconds *
                        deltaSeconds *
                        covariance11

            covariance01 +=
                deltaSeconds * covariance11

            covariance10 = covariance01

            covariance11 +=
                DRIFT_PROCESS_VARIANCE *
                        deltaSeconds

            /*
             * Measurement uncertainty.
             */
            val boundedError =
                max(
                    MIN_MEASUREMENT_ERROR_MICROS,
                    maxErrorMicros.toDouble()
                )

            val measurementVariance =
                boundedError * boundedError

            /*
             * Kalman update.
             *
             * La medición observa directamente el offset.
             */
            val innovation =
                measurementMicros.toDouble() -
                        offsetMicros

            val innovationVariance =
                covariance00 +
                        measurementVariance

            if (innovationVariance <= 0.0) {
                return
            }

            val gain0 =
                covariance00 /
                        innovationVariance

            val gain1 =
                covariance10 /
                        innovationVariance

            offsetMicros +=
                gain0 * innovation

            drift +=
                gain1 * innovation

            val oldCovariance00 = covariance00
            val oldCovariance01 = covariance01

            covariance00 =
                (1.0 - gain0) *
                        oldCovariance00

            covariance01 =
                (1.0 - gain0) *
                        oldCovariance01

            covariance10 =
                covariance10 -
                        gain1 * oldCovariance00

            covariance11 =
                covariance11 -
                        gain1 * oldCovariance01

            /*
             * Adaptive forgetting.
             *
             * Una medición muy alejada de la predicción indica que
             * la estimación anterior perdió relevancia.
             */
            if (
                abs(innovation) >
                ADAPTIVE_FORGETTING_CUTOFF *
                boundedError
            ) {
                covariance00 =
                    max(
                        covariance00,
                        measurementVariance
                    )
            }

            lastTimestampMicros =
                timestampMicros

            measurementCount++
        }

        fun computeClientTime(
            serverTimeMicros: Long
        ): Long {
            /*
             * Aproximación inversa:
             *
             * server = client + offset(client)
             *
             * Por tanto:
             *
             * client ≈ server - offset.
             */
            val elapsedSeconds =
                (
                        serverTimeMicros -
                                lastTimestampMicros
                        ) / 1_000_000.0

            val predictedOffset =
                offsetMicros +
                        drift * elapsedSeconds

            return (
                    serverTimeMicros -
                            predictedOffset
                    ).toLong()
        }

        fun computeServerTime(
            clientTimeMicros: Long
        ): Long {
            val elapsedSeconds =
                (
                        clientTimeMicros -
                                lastTimestampMicros
                        ) / 1_000_000.0

            val predictedOffset =
                offsetMicros +
                        drift * elapsedSeconds

            return (
                    clientTimeMicros +
                            predictedOffset
                    ).toLong()
        }

        fun computeOffset(
            timestampMicros: Long
        ): Long {
            val elapsedSeconds =
                (
                        timestampMicros -
                                lastTimestampMicros
                        ) / 1_000_000.0

            return (
                    offsetMicros +
                            drift * elapsedSeconds
                    ).toLong()
        }

        fun isSynchronized(): Boolean =
            measurementCount >= 2 &&
                    covariance00.isFinite() &&
                    covariance11.isFinite()

        companion object {
            private const val INITIAL_OFFSET_VARIANCE =
                1_000_000_000_000.0

            private const val INITIAL_DRIFT_VARIANCE =
                1e-12

            private const val DRIFT_PROCESS_VARIANCE =
                1e-22

            private const val MIN_MEASUREMENT_ERROR_MICROS =
                1.0

            private const val ADAPTIVE_FORGETTING_CUTOFF =
                3.0
        }
    }
}