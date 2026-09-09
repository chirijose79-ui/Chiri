package com.chirihome.platform.player.music.sendspin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockSynchronizerTest {

    @Test
    fun firstMeasurementEstablishesOffsetButDoesNotSynchronize() {
        val synchronizer = ClockSynchronizer()

        synchronizer.update(
            t1LocalMicros = 1_000_000L,
            t2ServerMicros = 1_100_000L,
            t3ServerMicros = 1_100_000L,
            t4LocalMicros = 1_010_000L
        )

        assertEquals(
            95_000L,
            synchronizer.getOffsetMicros()
        )
        assertFalse(synchronizer.isSynchronized())
    }

    @Test
    fun secondMeasurementEstablishesSynchronization() {
        val synchronizer = ClockSynchronizer()

        synchronizer.update(
            t1LocalMicros = 1_000_000L,
            t2ServerMicros = 1_100_000L,
            t3ServerMicros = 1_100_000L,
            t4LocalMicros = 1_010_000L
        )

        synchronizer.update(
            t1LocalMicros = 2_000_000L,
            t2ServerMicros = 2_100_000L,
            t3ServerMicros = 2_100_000L,
            t4LocalMicros = 2_010_000L
        )

        assertTrue(synchronizer.isSynchronized())
    }

    @Test
    fun offsetUsesAllFourNtpTimestamps() {
        val synchronizer = ClockSynchronizer()

        /*
         * T1 = 1,000,000
         * T2 = 1,105,000
         * T3 = 1,106,000
         * T4 = 1,011,000
         *
         * RTT =
         *   (T4 - T1) - (T3 - T2)
         * = 11,000 - 1,000
         * = 10,000 µs
         *
         * Offset =
         *   ((T2 - T1) + (T3 - T4)) / 2
         * = (105,000 + 95,000) / 2
         * = 100,000 µs
         */
        synchronizer.update(
            t1LocalMicros = 1_000_000L,
            t2ServerMicros = 1_105_000L,
            t3ServerMicros = 1_106_000L,
            t4LocalMicros = 1_011_000L
        )

        assertEquals(
            100_000L,
            synchronizer.getOffsetMicros()
        )

        assertEquals(
            10_000L,
            synchronizer.getRoundTripTimeMicros()
        )
    }

    @Test
    fun roundTripTimeIsCalculatedUsingServerProcessingTime() {
        val synchronizer = ClockSynchronizer()

        /*
         * Client elapsed time = 20,000 µs
         * Server processing time = 5,000 µs
         *
         * RTT = 20,000 - 5,000 = 15,000 µs
         */
        synchronizer.update(
            t1LocalMicros = 1_000_000L,
            t2ServerMicros = 1_100_000L,
            t3ServerMicros = 1_105_000L,
            t4LocalMicros = 1_020_000L
        )

        assertEquals(
            15_000L,
            synchronizer.getRoundTripTimeMicros()
        )
    }

    @Test
    fun invalidRoundTripIsIgnored() {
        val synchronizer = ClockSynchronizer()

        synchronizer.update(
            t1LocalMicros = 2_000_000L,
            t2ServerMicros = 2_100_000L,
            t3ServerMicros = 2_100_000L,
            t4LocalMicros = 1_000_000L
        )

        assertFalse(synchronizer.isSynchronized())
        assertEquals(
            0L,
            synchronizer.getOffsetMicros()
        )
        assertEquals(
            0L,
            synchronizer.getRoundTripTimeMicros()
        )
    }

    @Test
    fun serverTimeCanBeConvertedToLocalTime() {
        val synchronizer = ClockSynchronizer()

        synchronizer.update(
            t1LocalMicros = 1_000_000L,
            t2ServerMicros = 1_100_000L,
            t3ServerMicros = 1_100_000L,
            t4LocalMicros = 1_010_000L
        )

        assertEquals(
            1_005_000L,
            synchronizer.serverTimeToLocalMicros(
                1_100_000L
            )
        )
    }

    @Test
    fun synchronizedClockConvertsBothDirectionsUsingSameOffset() {
        val synchronizer = ClockSynchronizer()

        synchronizer.update(
            t1LocalMicros = 1_000_000L,
            t2ServerMicros = 1_100_000L,
            t3ServerMicros = 1_100_000L,
            t4LocalMicros = 1_010_000L
        )

        synchronizer.update(
            t1LocalMicros = 2_000_000L,
            t2ServerMicros = 2_100_000L,
            t3ServerMicros = 2_100_000L,
            t4LocalMicros = 2_010_000L
        )

        assertTrue(
            synchronizer.isSynchronized()
        )

        assertEquals(
            2_005_000L,
            synchronizer.serverTimeToLocalMicros(
                2_100_000L
            )
        )

        assertEquals(
            2_100_000L,
            synchronizer.localTimeToServerMicros(
                2_005_000L
            )
        )
    }

    @Test
    fun significantDriftIsAppliedToFutureServerTime() {
        val synchronizer = ClockSynchronizer()

        /*
         * First measurement:
         * offset = 1,000 µs
         */
        synchronizer.update(
            t1LocalMicros = 1_000_000L,
            t2ServerMicros = 1_001_000L,
            t3ServerMicros = 1_001_000L,
            t4LocalMicros = 1_000_000L
        )

        /*
         * Second measurement:
         * offset = 2,000 µs
         *
         * This initializes the drift:
         *
         * drift = (2,000 - 1,000) / 1,000,000
         *       = 0.001
         */
        synchronizer.update(
            t1LocalMicros = 2_000_000L,
            t2ServerMicros = 2_002_000L,
            t3ServerMicros = 2_002_000L,
            t4LocalMicros = 2_000_000L
        )

        /*
         * Third measurement keeps the same clock-rate
         * relationship and forces the Kalman update.
         */
        synchronizer.update(
            t1LocalMicros = 3_000_000L,
            t2ServerMicros = 3_003_000L,
            t3ServerMicros = 3_003_000L,
            t4LocalMicros = 3_000_000L
        )

        assertTrue(
            synchronizer.isSynchronized()
        )

        /*
         * The important assertion is that the future
         * server timestamp reflects the detected drift.
         */
        val serverTime =
            synchronizer.localTimeToServerMicros(
                4_000_000L
            )

        assertTrue(
            serverTime > 4_003_000L
        )
    }

    @Test
    fun smallDriftIsIgnoredWhenItIsNotStatisticallySignificant() {
        val synchronizer = ClockSynchronizer()

        /*
         * Use a non-zero RTT so the drift has uncertainty.
         *
         * First:
         * offset = 1,500 µs
         *
         * Second:
         * offset = 1,502 µs
         *
         * The resulting drift is intentionally too small
         * compared with its covariance and should not be
         * applied to future timestamps.
         */
        synchronizer.update(
            t1LocalMicros = 1_000_000L,
            t2ServerMicros = 1_505_000L,
            t3ServerMicros = 1_506_000L,
            t4LocalMicros = 1_008_000L
        )

        synchronizer.update(
            t1LocalMicros = 2_000_000L,
            t2ServerMicros = 2_005_002L,
            t3ServerMicros = 2_006_002L,
            t4LocalMicros = 2_008_000L
        )

        assertTrue(
            synchronizer.isSynchronized()
        )

        /*
         * If drift were applied, the result would be
         * slightly larger. The reference filter should
         * keep the drift disabled here.
         */
        assertEquals(
            3_001_502L,
            synchronizer.localTimeToServerMicros(
                3_000_000L
            )
        )
    }

    @Test
    fun repeatedStableMeasurementsKeepOffsetStable() {
        val synchronizer = ClockSynchronizer()

        repeat(20) { index ->
            val timestamp =
                (index + 1) * 1_000_000L

            synchronizer.update(
                t1LocalMicros = timestamp,
                t2ServerMicros =
                    timestamp + 50_000L,
                t3ServerMicros =
                    timestamp + 50_000L,
                t4LocalMicros =
                    timestamp + 10_000L
            )
        }

        assertTrue(
            synchronizer.isSynchronized()
        )

        assertEquals(
            45_000L,
            synchronizer.getOffsetMicros()
        )

        assertEquals(
            10_000L,
            synchronizer.getRoundTripTimeMicros()
        )

        assertEquals(
            2_035_000L,
            synchronizer.serverTimeToLocalMicros(
                2_080_000L
            )
        )
    }

    @Test
    fun nonMonotonicMeasurementsDoNotChangeSynchronization() {
        val synchronizer = ClockSynchronizer()

        synchronizer.update(
            t1LocalMicros = 1_000_000L,
            t2ServerMicros = 1_100_000L,
            t3ServerMicros = 1_100_000L,
            t4LocalMicros = 1_010_000L
        )

        synchronizer.update(
            t1LocalMicros = 2_000_000L,
            t2ServerMicros = 2_100_000L,
            t3ServerMicros = 2_100_000L,
            t4LocalMicros = 2_010_000L
        )

        val offsetBefore =
            synchronizer.getOffsetMicros()

        val rttBefore =
            synchronizer.getRoundTripTimeMicros()

        synchronizer.update(
            t1LocalMicros = 1_500_000L,
            t2ServerMicros = 1_700_000L,
            t3ServerMicros = 1_700_000L,
            t4LocalMicros = 1_510_000L
        )

        assertTrue(
            synchronizer.isSynchronized()
        )

        assertEquals(
            offsetBefore,
            synchronizer.getOffsetMicros()
        )

        assertEquals(
            rttBefore,
            synchronizer.getRoundTripTimeMicros()
        )
    }

    @Test
    fun resetClearsSynchronizationState() {
        val synchronizer = ClockSynchronizer()

        synchronizer.update(
            t1LocalMicros = 1_000_000L,
            t2ServerMicros = 1_100_000L,
            t3ServerMicros = 1_100_000L,
            t4LocalMicros = 1_010_000L
        )

        synchronizer.update(
            t1LocalMicros = 2_000_000L,
            t2ServerMicros = 2_100_000L,
            t3ServerMicros = 2_100_000L,
            t4LocalMicros = 2_010_000L
        )

        assertTrue(
            synchronizer.isSynchronized()
        )

        synchronizer.reset()

        assertFalse(
            synchronizer.isSynchronized()
        )

        assertEquals(
            0L,
            synchronizer.getOffsetMicros()
        )

        assertEquals(
            0L,
            synchronizer.getRoundTripTimeMicros()
        )
    }
}
