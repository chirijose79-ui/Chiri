package com.chirihome.platform.player.music.sendspin.protocol

interface SendspinHandshake {

    suspend fun createClientInit(): String

    fun receiveServerInit(rawMessage: String)

    suspend fun receiveNoiseMessage1(
        rawMessage: String
    ): String
}