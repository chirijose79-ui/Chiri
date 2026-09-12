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
) {

    private val json = Json {
        encodeDefaults = true
    }

    suspend fun createPairFinalize(
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

        storage.saveLongTermPsk(
            serverId = serverId,
            psk = longTermPsk
        )

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