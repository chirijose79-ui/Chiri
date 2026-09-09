package com.chirihome.platform.player.music.sendspin

interface SendspinMessageSender {

    suspend fun sendEncrypted(message: String)
}