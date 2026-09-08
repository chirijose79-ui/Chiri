package com.chirihome.platform.player.music.sendspin.protocol

import com.chirihome.platform.player.music.sendspin.crypto.JdkNoiseCrypto
import com.chirihome.platform.player.music.sendspin.crypto.NoiseCrypto
import com.chirihome.platform.storage.SendspinCredentialStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test

class SecureSendspinPskResolverTest {

    private val crypto: NoiseCrypto = JdkNoiseCrypto()

    @Test
    fun correctPskId_returnsStoredPairingPsk() = runBlocking {
        val psk = ByteArray(32) { it.toByte() }
        val storage = FakeSendspinCredentialStorage(
            pairingPsk = psk
        )
        val resolver = SecureSendspinPskResolver(
            storage = storage,
            crypto = crypto
        )

        val pskId = calculatePskId(psk)

        val resolved = resolver.resolve(
            pskId = pskId,
            pskCategory = "pr"
        )

        assertArrayEquals(psk, resolved)
    }

    @Test
    fun incorrectPskId_returnsNull() = runBlocking {
        val psk = ByteArray(32) { it.toByte() }
        val storage = FakeSendspinCredentialStorage(
            pairingPsk = psk
        )
        val resolver = SecureSendspinPskResolver(
            storage = storage,
            crypto = crypto
        )

        val resolved = resolver.resolve(
            pskId = "invalid-psk-id",
            pskCategory = "pr"
        )

        assertNull(resolved)
    }

    @Test
    fun unsupportedCategory_returnsNull() = runBlocking {
        val psk = ByteArray(32) { it.toByte() }
        val storage = FakeSendspinCredentialStorage(
            pairingPsk = psk
        )
        val resolver = SecureSendspinPskResolver(
            storage = storage,
            crypto = crypto
        )

        val pskId = calculatePskId(psk)

        val resolved = resolver.resolve(
            pskId = pskId,
            pskCategory = "unsupported"
        )

        assertNull(resolved)
    }

    @Test
    fun pairingCategoryUsesStoredPairingPsk() = runBlocking {
        val psk = ByteArray(32) { (it + 1).toByte() }
        val storage = FakeSendspinCredentialStorage(
            pairingPsk = psk
        )
        val resolver = SecureSendspinPskResolver(
            storage = storage,
            crypto = crypto
        )

        val pskId = calculatePskId(psk)

        val resolved = resolver.resolve(
            pskId = pskId,
            pskCategory = "pr"
        )

        assertEquals(32, resolved?.size)
        assertArrayEquals(psk, resolved)
    }

    private fun calculatePskId(psk: ByteArray): String {
        val label = "sendspin-psk-id-v1".toByteArray(Charsets.UTF_8)
        val digest = crypto.sha256(label + psk)
        return com.chirihome.platform.player.music.sendspin.crypto.SendspinBase64
            .encodeUrlSafe(digest)
    }

    private class FakeSendspinCredentialStorage(
        private var pairingPsk: ByteArray? = null
    ) : SendspinCredentialStorage {

        private var staticPrivateKey: ByteArray? = null
        private var serverStaticPublicKey: ByteArray? = null

        override suspend fun saveStaticPrivateKey(key: ByteArray) {
            staticPrivateKey = key.copyOf()
        }

        override suspend fun getStaticPrivateKey(): ByteArray? {
            return staticPrivateKey?.copyOf()
        }

        override suspend fun savePairingPsk(psk: ByteArray) {
            pairingPsk = psk.copyOf()
        }

        override suspend fun getPairingPsk(): ByteArray? {
            return pairingPsk?.copyOf()
        }

        override suspend fun saveServerStaticPublicKey(key: ByteArray) {
            serverStaticPublicKey = key.copyOf()
        }

        override suspend fun getServerStaticPublicKey(): ByteArray? {
            return serverStaticPublicKey?.copyOf()
        }

        override suspend fun clearCredentials() {
            staticPrivateKey = null
            pairingPsk = null
            serverStaticPublicKey = null
        }
    }
}