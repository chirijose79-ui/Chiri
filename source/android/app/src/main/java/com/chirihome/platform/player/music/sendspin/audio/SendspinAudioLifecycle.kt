package com.chirihome.platform.player.music.sendspin.audio

interface SendspinAudioLifecycle {

    suspend fun configure(
        sampleRate: Int,
        channels: Int,
        bitDepth: Int,
        codec: String
    )

    suspend fun start()
}
