package com.chirihome.platform.player.music.sendspin.protocol

interface SendspinPskResolver {

    suspend fun resolve(
        pskId: String,
        pskCategory: String
    ): ByteArray?
}
