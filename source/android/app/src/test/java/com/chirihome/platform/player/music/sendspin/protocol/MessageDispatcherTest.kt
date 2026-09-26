package com.chirihome.platform.player.music.sendspin.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageDispatcherTest {

    @Test
    fun dispatch_serverUnpair_returnsSessionControl() {
        val dispatcher = MessageDispatcher()

        val result = dispatcher.dispatch(
            """{"type":"server/unpair","payload":{}}"""
        )

        assertTrue(
            result is MessageDispatcher.DispatchResult.SessionControl
        )

        val sessionControl =
            result as MessageDispatcher.DispatchResult.SessionControl

        assertEquals(
            "server/unpair",
            sessionControl.type
        )
    }
}
