package com.chirihome.platform.player.music.sendspin.protocol

import com.chirihome.platform.player.music.sendspin.crypto.NoiseTransport

interface SendspinHandshake {

    suspend fun createClientInit(): String

    fun receiveServerInit(rawMessage: String)

    suspend fun receiveNoiseMessage1(
        rawMessage: String
    ): String

    val noiseTransport: NoiseTransport?

}