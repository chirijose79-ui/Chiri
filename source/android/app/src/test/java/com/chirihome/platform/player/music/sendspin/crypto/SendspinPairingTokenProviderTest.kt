package com.chirihome.platform.player.music.sendspin.crypto

import com.chirihome.platform.storage.SendspinCredentialStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SendspinPairingTokenProviderTest {

    private val crypto =
        JdkNoiseCrypto()

    @Test
    fun getOrCreateToken_createsAndPersistsPairingPsk() =
        runBlocking {
            val storage =
                FakeSendspinCredentialStorage()

            val provider =
                SendspinPairingTokenProvider(
                    storage = storage,
                    crypto = crypto
                )

            assertNull(
                storage.pairingPsk
            )

            val token =
                provider.getOrCreateToken()

            assertTrue(
                token.startsWith("SP:0")
            )

            val (clientKey, pairingPsk) =
                SendspinPsk.decodeToken(token)

            assertEquals(
                32,
                clientKey.size
            )

            assertEquals(
                32,
                pairingPsk.size
            )

            assertArrayEquals(
                pairingPsk,
                storage.pairingPsk
            )
        }

    @Test
    fun getOrCreateToken_reusesPersistedPairingPskAndIdentity() =
        runBlocking {
            val storage =
                FakeSendspinCredentialStorage()

            val provider =
                SendspinPairingTokenProvider(
                    storage = storage,
                    crypto = crypto
                )

            val firstToken =
                provider.getOrCreateToken()

            val firstPairingPsk =
                storage.pairingPsk

            val secondToken =
                provider.getOrCreateToken()

            val secondPairingPsk =
                storage.pairingPsk

            assertEquals(
                firstToken,
                secondToken
            )

            assertArrayEquals(
                firstPairingPsk,
                secondPairingPsk
            )
        }

    @Test
    fun getOrCreateToken_tokenIdentityMatchesPersistedIdentity() =
        runBlocking {
            val storage =
                FakeSendspinCredentialStorage()

            val identityProvider =
                SendspinIdentityProvider(
                    storage = storage,
                    crypto = crypto
                )

            val identity =
                identityProvider.getOrCreate()

            val provider =
                SendspinPairingTokenProvider(
                    storage = storage,
                    crypto = crypto
                )

            val token =
                provider.getOrCreateToken()

            val (clientKey, _) =
                SendspinPsk.decodeToken(token)

            assertArrayEquals(
                identity.staticPublicKey,
                clientKey
            )
        }

    private class FakeSendspinCredentialStorage :
        SendspinCredentialStorage {

        private var staticPrivateKey: ByteArray? = null

        var pairingPsk: ByteArray? = null

        private val longTermPsks =
            mutableMapOf<String, ByteArray>()

        private var serverStaticPublicKey: ByteArray? = null

        override suspend fun saveStaticPrivateKey(
            key: ByteArray
        ) {
            staticPrivateKey = key.copyOf()
        }

        override suspend fun getStaticPrivateKey(): ByteArray? {
            return staticPrivateKey?.copyOf()
        }

        override suspend fun savePairingPsk(
            psk: ByteArray
        ) {
            pairingPsk = psk.copyOf()
        }

        override suspend fun getPairingPsk(): ByteArray? {
            return pairingPsk?.copyOf()
        }

        override suspend fun saveLongTermPsk(
            serverId: String,
            psk: ByteArray
        ) {
            longTermPsks[serverId] = psk.copyOf()
        }

        override suspend fun getLongTermPsk(
            serverId: String
        ): ByteArray? {
            return longTermPsks[serverId]?.copyOf()
        }

        override suspend fun removeLongTermPsk(
            serverId: String
        ) {
            longTermPsks.remove(serverId)
        }

        override suspend fun saveServerStaticPublicKey(
            key: ByteArray
        ) {
            serverStaticPublicKey = key.copyOf()
        }

        override suspend fun getServerStaticPublicKey(): ByteArray? {
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