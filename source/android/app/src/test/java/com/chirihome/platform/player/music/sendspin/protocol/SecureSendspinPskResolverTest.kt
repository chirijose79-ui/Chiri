package com.chirihome.platform.player.music.sendspin.protocol

import com.chirihome.platform.player.music.sendspin.crypto.JdkNoiseCrypto
import com.chirihome.platform.player.music.sendspin.crypto.NoiseCrypto
import com.chirihome.platform.player.music.sendspin.crypto.SendspinBase64
import com.chirihome.platform.storage.SendspinCredentialStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecureSendspinPskResolverTest {

    private val crypto: NoiseCrypto = JdkNoiseCrypto()

    @Test
    fun correctPskId_returnsStoredPairingPsk() = runBlocking {
        val psk =
            ByteArray(32) { it.toByte() }

        val storage =
            FakeSendspinCredentialStorage(
                pairingPsk = psk
            )

        val resolver =
            SecureSendspinPskResolver(
                storage = storage,
                crypto = crypto
            )

        val pskId =
            calculatePskId(psk)

        val resolved =
            resolver.resolve(
                pskId = pskId,
                serverId = TEST_SERVER_ID
            )

        assertEquals(
            SendspinPskType.PAIRING,
            resolved?.type
        )

        assertArrayEquals(
            psk,
            resolved?.psk
        )
    }

    @Test
    fun sentinelPskId_returnsSentinelPsk() = runBlocking {
        val storage =
            FakeSendspinCredentialStorage(
                pairingPsk = null
            )

        val resolver =
            SecureSendspinPskResolver(
                storage = storage,
                crypto = crypto
            )

        val sentinelPsk =
            crypto.sha256(
                "sendspin-sentinel-psk-v1"
                    .toByteArray(Charsets.UTF_8)
            )

        val sentinelPskId =
            calculatePskId(sentinelPsk)

        val resolved =
            resolver.resolve(
                pskId = sentinelPskId,
                serverId = TEST_SERVER_ID
            )

        assertEquals(
            SendspinPskType.SENTINEL,
            resolved?.type
        )

        assertEquals(
            32,
            resolved?.psk?.size
        )

        assertArrayEquals(
            sentinelPsk,
            resolved?.psk
        )
    }

    @Test
    fun sentinelPskId_matchesOfficialSendspinValue() = runBlocking {
        val storage =
            FakeSendspinCredentialStorage(
                pairingPsk = null
            )

        val resolver =
            SecureSendspinPskResolver(
                storage = storage,
                crypto = crypto
            )

        val sentinelPsk =
            crypto.sha256(
                "sendspin-sentinel-psk-v1"
                    .toByteArray(Charsets.UTF_8)
            )

        val resolved =
            resolver.resolve(
                pskId =
                    "GFsV9tLaSQm9HcFWpKsgYQOr7wFTvNUtkmFwuVz3zoo",
                serverId = TEST_SERVER_ID
            )

        assertEquals(
            SendspinPskType.SENTINEL,
            resolved?.type
        )

        assertArrayEquals(
            sentinelPsk,
            resolved?.psk
        )
    }

    @Test
    fun incorrectPskId_returnsNull() = runBlocking {
        val psk =
            ByteArray(32) { it.toByte() }

        val storage =
            FakeSendspinCredentialStorage(
                pairingPsk = psk
            )

        val resolver =
            SecureSendspinPskResolver(
                storage = storage,
                crypto = crypto
            )

        val resolved =
            resolver.resolve(
                pskId = "invalid-psk-id",
                serverId = TEST_SERVER_ID
            )

        assertNull(resolved)
    }

    @Test
    fun correctPskId_returns32BytePairingPsk() = runBlocking {
        val psk =
            ByteArray(32) { (it + 1).toByte() }

        val storage =
            FakeSendspinCredentialStorage(
                pairingPsk = psk
            )

        val resolver =
            SecureSendspinPskResolver(
                storage = storage,
                crypto = crypto
            )

        val pskId =
            calculatePskId(psk)

        val resolved =
            resolver.resolve(
                pskId = pskId,
                serverId = TEST_SERVER_ID
            )

        assertEquals(
            SendspinPskType.PAIRING,
            resolved?.type
        )

        assertEquals(
            32,
            resolved?.psk?.size
        )

        assertArrayEquals(
            psk,
            resolved?.psk
        )
    }

    @Test
    fun noStoredPairingPsk_returnsNull() = runBlocking {
        val storage =
            FakeSendspinCredentialStorage(
                pairingPsk = null
            )

        val resolver =
            SecureSendspinPskResolver(
                storage = storage,
                crypto = crypto
            )

        val resolved =
            resolver.resolve(
                pskId = "any-psk-id",
                serverId = TEST_SERVER_ID
            )

        assertNull(resolved)
    }

    @Test
    fun correctPskId_returnsStoredLongTermPskForServer() =
        runBlocking {

            val psk =
                ByteArray(32) { (it + 2).toByte() }

            val storage =
                FakeSendspinCredentialStorage()

            storage.saveLongTermPsk(
                serverId = TEST_SERVER_ID,
                psk = psk
            )

            val resolver =
                SecureSendspinPskResolver(
                    storage = storage,
                    crypto = crypto
                )

            val pskId =
                calculatePskId(psk)

            val resolved =
                resolver.resolve(
                    pskId = pskId,
                    serverId = TEST_SERVER_ID
                )

            assertEquals(
                SendspinPskType.LONG_TERM,
                resolved?.type
            )

            assertArrayEquals(
                psk,
                resolved?.psk
            )
        }

    @Test
    fun longTermPskForDifferentServer_returnsNull() =
        runBlocking {

            val psk =
                ByteArray(32) { (it + 3).toByte() }

            val storage =
                FakeSendspinCredentialStorage()

            storage.saveLongTermPsk(
                serverId = TEST_SERVER_ID,
                psk = psk
            )

            val resolver =
                SecureSendspinPskResolver(
                    storage = storage,
                    crypto = crypto
                )

            val pskId =
                calculatePskId(psk)

            val resolved =
                resolver.resolve(
                    pskId = pskId,
                    serverId = "different-server"
                )

            assertNull(resolved)
        }

    @Test
    fun storedLongTermPskWithInvalidLength_returnsNull() =
        runBlocking {

            val invalidPsk =
                ByteArray(31) { it.toByte() }

            val storage =
                FakeSendspinCredentialStorage()

            storage.saveLongTermPsk(
                serverId = TEST_SERVER_ID,
                psk = invalidPsk
            )

            val resolver =
                SecureSendspinPskResolver(
                    storage = storage,
                    crypto = crypto
                )

            val pskId =
                calculatePskId(invalidPsk)

            val resolved =
                resolver.resolve(
                    pskId = pskId,
                    serverId = TEST_SERVER_ID
                )

            assertNull(resolved)
        }

    private companion object {
        const val TEST_SERVER_ID = "test-server"
    }

    private fun calculatePskId(
        psk: ByteArray
    ): String {

        val label =
            "sendspin-psk-id-v1"
                .toByteArray(Charsets.UTF_8)

        val digest =
            crypto.sha256(label + psk)

        return SendspinBase64.encodeUrlSafe(digest)
    }

    private class FakeSendspinCredentialStorage(
        private var pairingPsk: ByteArray? = null
    ) : SendspinCredentialStorage {

        private var staticPrivateKey: ByteArray? =
            null

        private var serverStaticPublicKey: ByteArray? =
            null

        private val longTermPsks =
            mutableMapOf<String, ByteArray>()

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
            serverStaticPublicKey = null
            longTermPsks.clear()
        }
    }
}
