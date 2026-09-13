package com.chirihome.platform.player.music.sendspin.protocol

internal fun parseSendspinPskCategory(
    category: String
): SendspinPskType =
    when (category) {
        "lt" -> SendspinPskType.LONG_TERM
        "pr" -> SendspinPskType.PAIRING
        "sn" -> SendspinPskType.SENTINEL
        else -> throw IllegalArgumentException(
            "Unsupported Sendspin PSK category: $category"
        )
    }