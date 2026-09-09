package com.chirihome.platform.player.music.sendspin

import kotlin.math.abs
import kotlin.math.max
import kotlin.time.TimeSource

/**
 * Synchronizes the local monotonic clock with the Sendspin server clock.
 *
 * T1 = client sends client/time
 * T2 = server receives client/time
 * T3 = server sends server/time
 * T4 = client receives server/time
 *
 * The internal filter is a Kotlin implementation of the
 * SendspinTimeFilter used by aiosendspin.
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

    fun getOffsetMicros(): Long =
        serverOffsetMicros

    fun getRoundTripTimeMicros(): Long =
        roundTripTimeMicros

    fun isSynchronized(): Boolean =
        synchronized

    /**
     * Returns the local monotonic clock in microseconds.
     */
    fun localTimeMicros(): Long =
        startMark.elapsedNow().inWholeMicroseconds

    /**
     * Processes one complete Sendspin server/time measurement.
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

        /*
         * NTP-style round-trip time:
         *
         * RTT = (T4 - T1) - (T3 - T2)
         */
        val roundTripMicros =
            (t4LocalMicros - t1LocalMicros) -
                    (t3ServerMicros - t2ServerMicros)

        if (roundTripMicros < 0L) {
            return
        }

        /*
         * Clock offset:
         *
         * offset = ((T2 - T1) + (T3 - T4)) / 2
         */
        val measuredOffsetMicros =
            (
                (t2ServerMicros - t1LocalMicros) +
                        (t3ServerMicros - t4LocalMicros)
                ).toDouble() / 2.0

        /*
         * aiosendspin uses RTT / 2 as max_error.
         */
        val maxErrorMicros =
            roundTripMicros.toDouble() / 2.0

        filter.update(
            measurementMicros = measuredOffsetMicros,
            maxErrorMicros = maxErrorMicros,
            timestampMicros = t4LocalMicros
        )

        serverOffsetMicros =
            filter.computeOffset(t4LocalMicros)

        roundTripTimeMicros =
            roundTripMicros

        sampleCount =
            filter.measurementCount

        synchronized =
            filter.isSynchronized()
    }

    /**
     * Converts a Sendspin server timestamp into local monotonic time.
     */
    fun serverTimeToLocalMicros(
        serverTimeMicros: Long
    ): Long {
        if (!synchronized) {
            return serverTimeMicros -
                    serverOffsetMicros
        }

        return filter.computeClientTime(
            serverTimeMicros
        )
    }

    /**
     * Converts local monotonic time into Sendspin server time.
     */
    fun localTimeToServerMicros(
        localTimeMicros: Long
    ): Long {
        if (!synchronized) {
            return localTimeMicros +
                    serverOffsetMicros
        }

        return filter.computeServerTime(
            localTimeMicros
        )
    }

    /**
     * Resets synchronization state.
     */
    @Synchronized
    fun reset() {
        serverOffsetMicros = 0L
        roundTripTimeMicros = 0L
        synchronized = false
        sampleCount = 0

        filter =
            SendspinTimeFilter()
    }

    /**
     * Kotlin implementation of aiosendspin's SendspinTimeFilter.
     *
     * State vector:
     *
     *   [ offset ]
     *   [ drift  ]
     *
     * offset:
     *   server_time - client_time
     *
     * drift:
     *   clock-rate difference between client and server.
     */
    private class SendspinTimeFilter {

        private var lastUpdateMicros: Long = 0L

        var measurementCount: Int = 0
            private set

        private var offset: Double = 0.0
        private var drift: Double = 0.0

        private var offsetCovariance =
            Double.POSITIVE_INFINITY

        private var offsetDriftCovariance =
            0.0

        private var driftCovariance =
            0.0

        private var currentTimeElement =
            TimeElement()

        /**
         * Adds one clock synchronization measurement.
         *
         * This follows the initialization, prediction,
         * Kalman update, adaptive forgetting and drift
         * significance rules of aiosendspin.
         */
        fun update(
            measurementMicros: Double,
            maxErrorMicros: Double,
            timestampMicros: Long
        ) {
            /*
             * Ignore non-monotonic timestamps.
             */
            if (timestampMicros <= lastUpdateMicros) {
                return
            }

            val dt =
                (timestampMicros -
                        lastUpdateMicros).toDouble()

            lastUpdateMicros =
                timestampMicros

            /*
             * aiosendspin:
             *
             * update_std_dev = max_error * 0.5
             * measurement_variance = update_std_dev²
             */
            val updateStdDev =
                maxErrorMicros *
                    MAX_ERROR_SCALE

            val measurementVariance =
                updateStdDev *
                    updateStdDev

            /*
             * First measurement.
             *
             * The filter starts directly from the
             * measured offset.
             */
            if (measurementCount <= 0) {
                measurementCount++

                offset =
                    measurementMicros

                offsetCovariance =
                    measurementVariance

                drift = 0.0

                currentTimeElement =
                    TimeElement(
                        lastUpdateMicros =
                            lastUpdateMicros,
                        offset = offset,
                        drift = drift,
                        useDrift = false
                    )

                return
            }

            /*
             * Second measurement.
             *
             * Initialize drift from the difference
             * between the first and second offsets.
             */
            if (measurementCount == 1) {
                measurementCount++

                drift =
                    (
                            measurementMicros.toDouble() -
                                    offset
                            ) / dt

                offset =
                    measurementMicros

                driftCovariance =
                    (
                            offsetCovariance +
                                    measurementVariance
                            ) / (dt * dt)

                offsetCovariance =
                    measurementVariance

                currentTimeElement =
                    TimeElement(
                        lastUpdateMicros =
                            lastUpdateMicros,
                        offset = offset,
                        drift = drift,
                        useDrift = false
                    )

                return
            }

            /*
             * ---------------------------------------
             * Prediction
             * ---------------------------------------
             */

            val predictedOffset =
                offset +
                        drift * dt

            val dtSquared =
                dt * dt

            /*
             * Drift process noise:
             *
             * drift_process_std_dev = 1e-11
             */
            val driftProcessVariance =
                dt *
                        DRIFT_PROCESS_VARIANCE

            val newDriftCovariance =
                driftCovariance +
                        driftProcessVariance

            val newOffsetDriftCovariance =
                offsetDriftCovariance +
                        driftCovariance * dt

            /*
             * process_std_dev = 0
             */
            val newOffsetCovariance =
                offsetCovariance +
                        2.0 *
                        offsetDriftCovariance *
                        dt +
                        driftCovariance *
                        dtSquared +
                        dt *
                        PROCESS_VARIANCE

            /*
             * ---------------------------------------
             * Innovation
             * ---------------------------------------
             */

            val residual =
                measurementMicros.toDouble() -
                    predictedOffset

            val maxResidualCutoff =
                maxErrorMicros *
                    ADAPTIVE_FORGETTING_CUTOFF

            /*
             * Adaptive forgetting is only activated
             * after the first 100 measurements.
             */
            var adjustedDriftCovariance =
                newDriftCovariance

            var adjustedOffsetDriftCovariance =
                newOffsetDriftCovariance

            var adjustedOffsetCovariance =
                newOffsetCovariance

            if (
                measurementCount <
                ADAPTIVE_FORGETTING_MIN_COUNT
            ) {
                measurementCount++
            } else if (
                abs(residual) >
                maxResidualCutoff
            ) {
                adjustedDriftCovariance *=
                    FORGET_VARIANCE_FACTOR

                adjustedOffsetDriftCovariance *=
                    FORGET_VARIANCE_FACTOR

                adjustedOffsetCovariance *=
                    FORGET_VARIANCE_FACTOR
            }

            /*
             * ---------------------------------------
             * Kalman update
             * ---------------------------------------
             */

            val uncertainty =
                1.0 /
                        max(
                            adjustedOffsetCovariance +
                                    measurementVariance,
                            MIN_DENOMINATOR
                        )

            val offsetGain =
                adjustedOffsetCovariance *
                        uncertainty

            val driftGain =
                adjustedOffsetDriftCovariance *
                        uncertainty

            offset =
                predictedOffset +
                        offsetGain *
                        residual

            drift +=
                driftGain *
                        residual

            /*
             * Covariance update.
             */
            driftCovariance =
                adjustedDriftCovariance -
                        driftGain *
                        adjustedOffsetDriftCovariance

            offsetDriftCovariance =
                adjustedOffsetDriftCovariance -
                        driftGain *
                        adjustedOffsetCovariance

            offsetCovariance =
                adjustedOffsetCovariance -
                        offsetGain *
                        adjustedOffsetCovariance

            /*
             * Drift is only used when statistically
             * significant.
             *
             * drift² > 4 * drift_covariance
             */
            val useDrift =
                drift * drift >
                        DRIFT_SIGNIFICANCE_THRESHOLD_SQUARED *
                        driftCovariance

            currentTimeElement =
                TimeElement(
                    lastUpdateMicros =
                        lastUpdateMicros,
                    offset = offset,
                    drift = drift,
                    useDrift = useDrift
                )
        }

        /**
         * Converts local/client time to server time.
         */
        fun computeServerTime(
            clientTimeMicros: Long
        ): Long {
            val element =
                currentTimeElement

            val effectiveDrift =
                if (element.useDrift) {
                    element.drift
                } else {
                    0.0
                }

            val dt =
                (
                        clientTimeMicros -
                                element.lastUpdateMicros
                        ).toDouble()

            val effectiveOffset =
                roundToLong(
                    element.offset +
                            effectiveDrift *
                            dt
                )

            return clientTimeMicros +
                    effectiveOffset
        }

        /**
         * Converts server time to client/local time.
         *
         * This is the exact inverse used by aiosendspin.
         */
        fun computeClientTime(
            serverTimeMicros: Long
        ): Long {
            val element =
                currentTimeElement

            val effectiveDrift =
                if (element.useDrift) {
                    element.drift
                } else {
                    0.0
                }

            return roundToLong(
                (
                        serverTimeMicros.toDouble() -
                                element.offset +
                                effectiveDrift *
                                element.lastUpdateMicros
                        ) /
                        (1.0 + effectiveDrift)
            )
        }

        /**
         * Returns the current effective offset at
         * the requested timestamp.
         */
        fun computeOffset(
            timestampMicros: Long
        ): Long {
            val element =
                currentTimeElement

            val effectiveDrift =
                if (element.useDrift) {
                    element.drift
                } else {
                    0.0
                }

            val dt =
                (
                        timestampMicros -
                                element.lastUpdateMicros
                        ).toDouble()

            return roundToLong(
                element.offset +
                        effectiveDrift *
                        dt
            )
        }

        fun isSynchronized(): Boolean =
            measurementCount >= 2 &&
                    !offsetCovariance.isInfinite()

        private data class TimeElement(
            val lastUpdateMicros: Long = 0L,
            val offset: Double = 0.0,
            val drift: Double = 0.0,
            val useDrift: Boolean = false
        )

        companion object {

            /*
             * Values matching aiosendspin.
             */

            private const val
                    ADAPTIVE_FORGETTING_CUTOFF =
                3.0

            private const val
                    MAX_ERROR_SCALE =
                0.5

            private const val
                    DRIFT_SIGNIFICANCE_THRESHOLD_SQUARED =
                4.0

            private const val
                    PROCESS_VARIANCE =
                0.0

            private const val
                    DRIFT_PROCESS_VARIANCE =
                1e-22

            private const val
                    FORGET_VARIANCE_FACTOR =
                4.0

            private const val
                    ADAPTIVE_FORGETTING_MIN_COUNT =
                100

            private const val
                    MIN_DENOMINATOR =
                1e-9

            private fun roundToLong(
                value: Double
            ): Long =
                kotlin.math.round(value).toLong()
        }
    }
}