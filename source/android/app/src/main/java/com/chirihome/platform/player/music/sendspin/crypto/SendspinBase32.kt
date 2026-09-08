package com.chirihome.platform.player.music.sendspin.crypto

object SendspinBase32 {

    private const val ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun encode(
        data: ByteArray
    ): String {
        if (data.isEmpty()) {
            return ""
        }

        val result = StringBuilder()

        var buffer = 0
        var bitsInBuffer = 0

        for (byte in data) {
            buffer =
                (buffer shl 8) or
                        (byte.toInt() and 0xFF)

            bitsInBuffer += 8

            while (bitsInBuffer >= 5) {
                bitsInBuffer -= 5

                val index =
                    (buffer shr bitsInBuffer) and 0x1F

                result.append(
                    ALPHABET[index]
                )
            }
        }

        if (bitsInBuffer > 0) {
            val index =
                (buffer shl (5 - bitsInBuffer)) and 0x1F

            result.append(
                ALPHABET[index]
            )
        }

        return result.toString()
    }

    fun decode(
        value: String
    ): ByteArray {
        if (value.isEmpty()) {
            return ByteArray(0)
        }

        val normalized =
            value
                .trim()
                .uppercase()

        var buffer = 0
        var bitsInBuffer = 0

        val result =
            ArrayList<Byte>()

        for (character in normalized) {
            val index =
                ALPHABET.indexOf(character)

            require(index >= 0) {
                "Invalid Base32 character: $character"
            }

            buffer =
                (buffer shl 5) or index

            bitsInBuffer += 5

            if (bitsInBuffer >= 8) {
                bitsInBuffer -= 8

                result.add(
                    ((buffer shr bitsInBuffer) and 0xFF)
                        .toByte()
                )
            }
        }

        return result.toByteArray()
    }
}