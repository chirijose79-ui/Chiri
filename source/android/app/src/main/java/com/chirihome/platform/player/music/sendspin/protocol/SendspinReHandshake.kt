package com.chirihome.platform.player.music.sendspin.protocol

import com.chirihome.platform.player.music.sendspin.crypto.NoiseTransport

data class SendspinReHandshakeResult(
    val message2: String,
    val noiseTransport: NoiseTransport
)

interface SendspinReHandshake {

    suspend fun receiveReHandshakeMessage1(
        rawMessage: String
    ): SendspinReHandshakeResult
}
