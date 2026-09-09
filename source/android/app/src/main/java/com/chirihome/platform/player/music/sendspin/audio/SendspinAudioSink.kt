package com.chirihome.platform.player.music.sendspin.audio

interface SendspinAudioSink {

    suspend fun processFrame(
        encodedData: ByteArray,
        serverTimestampMicros: Long
    )
}
