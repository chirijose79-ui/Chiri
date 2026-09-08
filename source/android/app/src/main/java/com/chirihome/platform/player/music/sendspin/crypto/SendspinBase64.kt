package com.chirihome.platform.player.music.sendspin.crypto

import java.util.Base64

object SendspinBase64 {

    fun encode(
        data: ByteArray
    ): String {
        return Base64
            .getEncoder()
            .encodeToString(data)
    }

    fun decode(
        value: String
    ): ByteArray {
        return Base64
            .getDecoder()
            .decode(value)
    }

    fun encodeUrlSafe(
        data: ByteArray
    ): String {
        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(data)
    }

    fun decodeUrlSafe(
        value: String
    ): ByteArray {
        return Base64
            .getUrlDecoder()
            .decode(value)
    }
}