package com.chirihome.platform.player.music.sendspin.protocol

interface SendspinPairingFinalizer {
    suspend fun createPairFinalize(serverId: String): String
}