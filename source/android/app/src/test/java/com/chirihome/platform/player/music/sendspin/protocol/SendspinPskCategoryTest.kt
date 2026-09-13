package com.chirihome.platform.player.music.sendspin.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class SendspinPskCategoryTest {

    @Test
    fun parse_lt_returnsLongTerm() {
        assertEquals(
            SendspinPskType.LONG_TERM,
            parseSendspinPskCategory("lt")
        )
    }

    @Test
    fun parse_pr_returnsPairing() {
        assertEquals(
            SendspinPskType.PAIRING,
            parseSendspinPskCategory("pr")
        )
    }

    @Test
    fun parse_sn_returnsSentinel() {
        assertEquals(
            SendspinPskType.SENTINEL,
            parseSendspinPskCategory("sn")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_unknownCategory_rejects() {
        parseSendspinPskCategory("unknown")
    }
}