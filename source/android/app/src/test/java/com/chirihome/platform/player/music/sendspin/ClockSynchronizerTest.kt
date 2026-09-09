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
            synchronizer.serverTimeToLocalMicros(1_100_000L)
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
        assertEquals(0L, synchronizer.getOffsetMicros())
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

        assertTrue(synchronizer.isSynchronized())

        synchronizer.reset()

        assertFalse(synchronizer.isSynchronized())
        assertEquals(0L, synchronizer.getOffsetMicros())
        assertEquals(0L, synchronizer.getRoundTripTimeMicros())
    }
}
