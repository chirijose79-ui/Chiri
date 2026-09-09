package com.chirihome.platform.player.music.sendspin.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendspinAudioFrameParserTest {

    @Test
    fun parsesAudioFrame() {
        val audio = byteArrayOf(
            0x11,
            0x22,
            0x33,
            0x44
        )

        val frame = ByteArray(13 + audio.size)

        frame[0] = 0x04

        writeLongBigEndian(
            frame,
            1,
            1_234_567_890_123L
        )

        writeIntBigEndian(
            frame,
            9,
            250
        )

        audio.copyInto(
            destination = frame,
            destinationOffset = 13
        )

        val result =
            SendspinAudioFrameParser.parse(frame)

        assertEquals(
            1_234_567_890_123L,
            result.serverTimestampMicros
        )

        assertEquals(
            250L,
            result.sendAhead
        )

        assertTrue(
            audio.contentEquals(result.encodedData)
        )
    }

    @Test
    fun parsesUnsignedSendAhead() {
        val frame = ByteArray(14)

        frame[0] = 0x04

        writeLongBigEndian(
            frame,
            1,
            100L
        )

        frame[9] = 0xFF.toByte()
        frame[10] = 0xFF.toByte()
        frame[11] = 0xFF.toByte()
        frame[12] = 0xFF.toByte()

        frame[13] = 0x55

        val result =
            SendspinAudioFrameParser.parse(frame)

        assertEquals(
            4_294_967_295L,
            result.sendAhead
        )
    }

    @Test
    fun rejectsNonAudioMessageType() {
        val frame = ByteArray(14)

        frame[0] = 0x00
        frame[13] = 0x55

        try {
            SendspinAudioFrameParser.parse(frame)
            assertTrue("Expected IllegalArgumentException", false)
        } catch (exception: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    @Test
    fun rejectsFrameShorterThanHeader() {
        val frame = ByteArray(12)

        frame[0] = 0x04

        try {
            SendspinAudioFrameParser.parse(frame)
            assertTrue("Expected IllegalArgumentException", false)
        } catch (exception: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    @Test
    fun rejectsAudioFrameWithoutPayload() {
        val frame = ByteArray(13)

        frame[0] = 0x04

        try {
            SendspinAudioFrameParser.parse(frame)
            assertTrue("Expected IllegalArgumentException", false)
        } catch (exception: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    private fun writeLongBigEndian(
        data: ByteArray,
        offset: Int,
        value: Long
    ) {
        for (index in 0 until Long.SIZE_BYTES) {
            val shift =
                (Long.SIZE_BYTES - 1 - index) * 8

            data[offset + index] =
                (value ushr shift).toByte()
        }
    }

    private fun writeIntBigEndian(
        data: ByteArray,
        offset: Int,
        value: Int
    ) {
        for (index in 0 until Int.SIZE_BYTES) {
            val shift =
                (Int.SIZE_BYTES - 1 - index) * 8

            data[offset + index] =
                (value ushr shift).toByte()
        }
    }
}
