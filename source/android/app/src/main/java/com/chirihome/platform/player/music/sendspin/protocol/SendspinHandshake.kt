package com.chirihome.platform.player.music.sendspin.protocol

interface SendspinHandshake {

    suspend fun createClientInit(): String

    fun receiveServerInit(rawMessage: String)

}