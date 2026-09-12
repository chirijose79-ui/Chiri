package com.chirihome.platform.player.music.sendspin.protocol

enum class SendspinPskType {
    SENTINEL,
    PAIRING,
    LONG_TERM
}

data class SendspinPskResolution(
    val psk: ByteArray,
    val type: SendspinPskType
)