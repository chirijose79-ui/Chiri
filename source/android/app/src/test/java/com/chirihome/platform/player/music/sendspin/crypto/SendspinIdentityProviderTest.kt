package com.chirihome.platform.player.music.sendspin.crypto

import com.chirihome.platform.storage.SendspinCredentialStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SendspinIdentityProviderTest {

    private class InMemoryCredentialStorage :
        SendspinCredentialStorage {

        private var staticPrivateKey: ByteArray? = null
        private var pairingPsk: ByteArray? = null
        private var serverStaticPublicKey: ByteArray? = null

        private val longTermPsks =
            mutableMapOf<String, ByteArray>()

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

    private val crypto =
        JdkNoiseCrypto()

    @Test
    fun createsAndPersistsIdentityWhenNoKeyExists() =
        runBlocking {
            val storage =
                InMemoryCredentialStorage()

            val provider =
                SendspinIdentityProvider(
                    storage = storage,
                    crypto = crypto
                )

            assertNull(
                storage.getStaticPrivateKey()
            )

            val identity =
                provider.getOrCreate()

            val storedKey =
                storage.getStaticPrivateKey()

            assertNotNull(storedKey)

            assertEquals(
                identity.staticPrivateKey.toList(),
                storedKey!!.toList()
            )
        }

    @Test
    fun returnsSameIdentityFromPersistedPrivateKey() =
        runBlocking {
            val storage =
                InMemoryCredentialStorage()

            val provider =
                SendspinIdentityProvider(
                    storage = storage,
                    crypto = crypto
                )

            val first =
                provider.getOrCreate()

            val second =
                provider.getOrCreate()

            assertEquals(
                first.staticPrivateKey.toList(),
                second.staticPrivateKey.toList()
            )

            assertEquals(
                first.staticPublicKey.toList(),
                second.staticPublicKey.toList()
            )

            assertEquals(
                first.clientId,
                second.clientId
            )
        }

    @Test
    fun persistedIdentityProducesStableClientId() =
        runBlocking {
            val storage =
                InMemoryCredentialStorage()

            val provider =
                SendspinIdentityProvider(
                    storage = storage,
                    crypto = crypto
                )

            val first =
                provider.getOrCreate()

            val storedKey =
                storage.getStaticPrivateKey()

            assertNotNull(storedKey)

            storage.clearCredentials()

            storage.saveStaticPrivateKey(
                storedKey!!
            )

            val second =
                provider.getOrCreate()

            assertEquals(
                first.clientId,
                second.clientId
            )
        }
}