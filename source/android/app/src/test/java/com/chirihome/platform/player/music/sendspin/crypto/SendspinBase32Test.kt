package com.chirihome.platform.player.music.sendspin.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SendspinBase32Test {

    @Test
    fun encode_empty_returnsEmpty() {
        assertEquals(
            "",
            SendspinBase32.encode(
                ByteArray(0)
            )
        )
    }

    @Test
    fun encode_knownValue_matchesRfc4648() {
        val input =
            "foo".toByteArray()

        assertEquals(
            "MZXW6",
            SendspinBase32.encode(input)
        )
    }

    @Test
    fun encode_withoutPadding() {
        val input =
            "foobar".toByteArray()

        assertEquals(
            "MZXW6YTBOI",
            SendspinBase32.encode(input)
        )
    }

    @Test
    fun decode_knownValue_matchesOriginal() {
        val encoded =
            "MZXW6"

        val decoded =
            SendspinBase32.decode(encoded)

        assertArrayEquals(
            "foo".toByteArray(),
            decoded
        )
    }

    @Test
    fun decode_withoutPadding_matchesOriginal() {
        val encoded =
            "MZXW6YTBOI"

        val decoded =
            SendspinBase32.decode(encoded)

        assertArrayEquals(
            "foobar".toByteArray(),
            decoded
        )
    }

    @Test
    fun encodeDecode_roundTrip() {
        val original =
            "Sendspin Base32 test".toByteArray()

        val encoded =
            SendspinBase32.encode(original)

        val decoded =
            SendspinBase32.decode(encoded)

        assertArrayEquals(
            original,
            decoded
        )
    }

    @Test
    fun decode_acceptsLowercase() {
        val decoded =
            SendspinBase32.decode(
                "mzxw6"
            )

        assertArrayEquals(
            "foo".toByteArray(),
            decoded
        )
    }

    @Test
    fun decode_acceptsWhitespaceAroundValue() {
        val decoded =
            SendspinBase32.decode(
                "  MZXW6  "
            )

        assertArrayEquals(
            "foo".toByteArray(),
            decoded
        )
    }

    @Test
    fun decode_invalidCharacter_throws() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            SendspinBase32.decode(
                "MZXW6!"
            )
        }
    }

    @Test
    fun decode_invalidAlphabetCharacter_throws() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            SendspinBase32.decode(
                "MZXW0"
            )
        }
    }
}