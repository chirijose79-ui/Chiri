package com.chirihome.platform.player.music.sendspin.protocol

import com.chirihome.platform.player.music.sendspin.crypto.NoiseCrypto
import com.chirihome.platform.player.music.sendspin.crypto.SendspinBase64
import com.chirihome.platform.storage.SendspinCredentialStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SendspinPairingHandler(
    private val storage: SendspinCredentialStorage,
    private val crypto: NoiseCrypto
) : SendspinPairingFinalizer {

    private val json = Json {
        encodeDefaults = true
    }

    private var pendingServerId: String? = null
    private var pendingLongTermPsk: ByteArray? = null

    override suspend fun createPairFinalize(
        serverId: String
    ): String {

        require(serverId.isNotBlank()) {
            "Server id must not be blank"
        }

        val longTermPsk =
            crypto.randomBytes(PSK_SIZE)

        require(longTermPsk.size == PSK_SIZE) {
            "Generated Long-Term PSK must be 32 bytes"
        }

        pendingServerId = serverId
        pendingLongTermPsk = longTermPsk.copyOf()

        val message =
            ClientPairFinalizeMessage(
                payload =
                    ClientPairFinalizePayload(
                        long_term_psk =
                            SendspinBase64.encodeUrlSafe(
                                longTermPsk
                            )
                    )
            )

        return json.encodeToString(message)
    }

    override suspend fun confirmPairFinalize(
        serverId: String
    ) {
        require(serverId.isNotBlank()) {
            "Server id must not be blank"
        }

        require(pendingServerId == serverId) {
            "No pending pairing finalize for server"
        }

        val longTermPsk =
            pendingLongTermPsk
                ?: throw IllegalStateException(
                    "No pending Long-Term PSK"
                )

        storage.saveLongTermPsk(
            serverId = serverId,
            psk = longTermPsk.copyOf()
        )

        pendingServerId = null
        pendingLongTermPsk = null
    }

    @Serializable
    private data class ClientPairFinalizeMessage(
        val type: String = "client/pair-finalize",
        val payload: ClientPairFinalizePayload
    )

    @Serializable
    private data class ClientPairFinalizePayload(
        val long_term_psk: String
    )

    companion object {
        private const val PSK_SIZE = 32
    }
}