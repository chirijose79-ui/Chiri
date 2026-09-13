package com.chirihome.platform.player.music.sendspin.protocol

import com.chirihome.platform.player.music.sendspin.crypto.JdkNoiseCrypto
import com.chirihome.platform.player.music.sendspin.crypto.SendspinBase64
import com.chirihome.platform.storage.SendspinCredentialStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SendspinPairingHandlerTest {

    @Test
    fun createPairFinalizeGeneratesLongTermPskWithoutPersisting() =
        runBlocking {
            val storage =
                FakeSendspinCredentialStorage()

            val crypto =
                JdkNoiseCrypto()

            val handler =
                SendspinPairingHandler(
                    storage = storage,
                    crypto = crypto
                )

            val serverId = "server-001"

            val message =
                handler.createPairFinalize(
                    serverId = serverId
                )

            assertEquals(
                null,
                storage.getLongTermPsk(serverId)
            )

            assertTrue(
                message.contains(
                    "\"type\":\"client/pair-finalize\""
                )
            )

            assertTrue(
                message.contains(
                    "\"long_term_psk\""
                )
            )

            val payloadStart =
                message.indexOf(
                    "\"long_term_psk\":\""
                )

            assertTrue(
                payloadStart >= 0
            )

            val encodedPskStart =
                payloadStart +
                        "\"long_term_psk\":\"".length

            val encodedPskEnd =
                message.indexOf(
                    "\"",
                    encodedPskStart
                )

            val encodedPsk =
                message.substring(
                    encodedPskStart,
                    encodedPskEnd
                )

            assertEquals(
                43,
                encodedPsk.length
            )
        }

    @Test
    fun confirmPairFinalizePersistsGeneratedLongTermPsk() =
        runBlocking {
            val storage =
                FakeSendspinCredentialStorage()

            val crypto =
                JdkNoiseCrypto()

            val handler =
                SendspinPairingHandler(
                    storage = storage,
                    crypto = crypto
                )

            val serverId = "server-001"

            val message =
                handler.createPairFinalize(
                    serverId = serverId
                )

            val marker =
                "\"long_term_psk\":\""

            val start =
                message.indexOf(marker)

            assertTrue(start >= 0)

            val encodedPskStart =
                start + marker.length

            val encodedPskEnd =
                message.indexOf(
                    "\"",
                    encodedPskStart
                )

            val encodedPsk =
                message.substring(
                    encodedPskStart,
                    encodedPskEnd
                )

            handler.confirmPairFinalize(
                serverId = serverId
            )

            val storedPsk =
                storage.getLongTermPsk(serverId)

            assertNotNull(storedPsk)

            assertEquals(
                32,
                storedPsk!!.size
            )

            assertEquals(
                encodedPsk,
                SendspinBase64.encodeUrlSafe(
                    storedPsk
                )
            )
        }

    @Test
    fun confirmPairFinalizeRejectsDifferentServerId() =
        runBlocking {
            val storage =
                FakeSendspinCredentialStorage()

            val crypto =
                JdkNoiseCrypto()

            val handler =
                SendspinPairingHandler(
                    storage = storage,
                    crypto = crypto
                )

            handler.createPairFinalize(
                serverId = "server-a"
            )

            var failed = false

            try {
                handler.confirmPairFinalize(
                    serverId = "server-b"
                )
            } catch (exception: IllegalArgumentException) {
                failed = true
            }

            assertTrue(failed)

            assertEquals(
                null,
                storage.getLongTermPsk("server-a")
            )

            assertEquals(
                null,
                storage.getLongTermPsk("server-b")
            )
        }

    @Test
    fun createPairFinalizeRejectsBlankServerId() =
        runBlocking {
            val storage =
                FakeSendspinCredentialStorage()

            val crypto =
                JdkNoiseCrypto()

            val handler =
                SendspinPairingHandler(
                    storage = storage,
                    crypto = crypto
                )

            var failed = false

            try {
                handler.createPairFinalize(" ")
            } catch (exception: IllegalArgumentException) {
                failed = true
            }

            assertTrue(failed)
        }

    private class FakeSendspinCredentialStorage :
        SendspinCredentialStorage {

        private var staticPrivateKey: ByteArray? =
            null

        private var pairingPsk: ByteArray? =
            null

        private val longTermPsks =
            mutableMapOf<String, ByteArray>()

        private var serverStaticPublicKey: ByteArray? =
            null

        override suspend fun saveStaticPrivateKey(
            key: ByteArray
        ) {
            staticPrivateKey =
                key.copyOf()
        }

        override suspend fun getStaticPrivateKey():
                ByteArray? {
            return staticPrivateKey?.copyOf()
        }

        override suspend fun savePairingPsk(
            psk: ByteArray
        ) {
            pairingPsk =
                psk.copyOf()
        }

        override suspend fun getPairingPsk():
                ByteArray? {
            return pairingPsk?.copyOf()
        }

        override suspend fun saveLongTermPsk(
            serverId: String,
            psk: ByteArray
        ) {
            longTermPsks[serverId] =
                psk.copyOf()
        }

        override suspend fun getLongTermPsk(
            serverId: String
        ): ByteArray? {
            return longTermPsks[serverId]?.copyOf()
        }

        override suspend fun saveServerStaticPublicKey(
            key: ByteArray
        ) {
            serverStaticPublicKey =
                key.copyOf()
        }

        override suspend fun getServerStaticPublicKey():
                ByteArray? {
            return serverStaticPublicKey?.copyOf()
        }

        override suspend fun clearCredentials() {
            staticPrivateKey = null
            pairingPsk = null
            longTermPsks.clear()
            serverStaticPublicKey = null
        }
    }
}
