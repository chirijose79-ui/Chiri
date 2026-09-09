package com.chirihome.platform.player.music.sendspin.protocol

data class SendspinAudioFrame(
    val serverTimestampMicros: Long,
    val sendAhead: Long,
    val encodedData: ByteArray
)

object SendspinAudioFrameParser {

    private const val AUDIO_MESSAGE_TYPE = 0x04
    private const val HEADER_SIZE = 13

    fun parse(plaintext: ByteArray): SendspinAudioFrame {
        require(plaintext.size >= HEADER_SIZE) {
            "Sendspin audio frame is too short: ${plaintext.size} bytes"
        }

        val messageType = plaintext[0].toInt() and 0xFF

        require(messageType == AUDIO_MESSAGE_TYPE) {
            "Unsupported Sendspin audio message type: $messageType"
        }

        val serverTimestampMicros = readLongBigEndian(
            plaintext,
            1
        )

        val sendAhead = readUnsignedIntBigEndian(
            plaintext,
            9
        )

        val encodedData = plaintext.copyOfRange(
            HEADER_SIZE,
            plaintext.size
        )

        require(encodedData.isNotEmpty()) {
            "Sendspin audio frame does not contain encoded audio data"
        }

        return SendspinAudioFrame(
            serverTimestampMicros = serverTimestampMicros,
            sendAhead = sendAhead,
            encodedData = encodedData
        )
    }

    private fun readLongBigEndian(
        data: ByteArray,
        offset: Int
    ): Long {
        var value = 0L

        for (index in 0 until Long.SIZE_BYTES) {
            value = (value shl 8) or
                (data[offset + index].toLong() and 0xFFL)
        }

        return value
    }

    private fun readUnsignedIntBigEndian(
        data: ByteArray,
        offset: Int
    ): Long {
        var value = 0L

        for (index in 0 until Int.SIZE_BYTES) {
            value = (value shl 8) or
                (data[offset + index].toLong() and 0xFFL)
        }

        return value
    }
}
