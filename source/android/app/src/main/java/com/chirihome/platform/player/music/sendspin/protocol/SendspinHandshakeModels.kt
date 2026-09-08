package com.chirihome.platform.player.music.sendspin.protocol

import kotlinx.serialization.Serializable

@Serializable
data class SendspinClientInitPayload(
    val client_id: String,
    val version: Int,
    val suite: String
)

@Serializable
data class SendspinClientInitMessage(
    val payload: SendspinClientInitPayload,
    val type: String = "client/init"
)

@Serializable
data class SendspinServerInitPayload(
    val server_id: String,
    val version: Int
)

@Serializable
data class SendspinServerInitMessage(
    val payload: SendspinServerInitPayload,
    val type: String = "server/init"
)

@Serializable
data class SendspinNoiseHandshakePayload(
    val data: String
)

@Serializable
data class SendspinNoiseHandshakeMessage(
    val payload: SendspinNoiseHandshakePayload,
    val type: String = "noise/handshake"
)

@Serializable
data class SendspinNoiseMsg1Payload(
    val psk_id: String,
    val psk_category: String
)

@Serializable
class SendspinNoiseMsg2Payload