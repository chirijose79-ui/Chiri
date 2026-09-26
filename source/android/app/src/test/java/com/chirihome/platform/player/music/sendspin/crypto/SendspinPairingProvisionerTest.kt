package com.chirihome.platform.player.music.sendspin.crypto

import com.chirihome.platform.storage.SendspinCredentialStorage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SendspinPairingProvisionerTest {

    private val crypto = JdkNoiseCrypto()

    @Test
    fun provision_validToken_savesPairingPsk() = runBlocking {
        val identity = SendspinIdentity.generate(crypto)
        val pairingPsk = SendspinPsk.generatePairingPsk(crypto)
        val token = SendspinPsk.createToken(identity, pairingPsk)
        val storage = FakeSendspinCredentialStorage()

        SendspinPairingProvisioner(storage).provision(
            token = token,
            identity = identity
        )

        assertArrayEquals(pairingPsk, storage.pairingPsk)
    }

    @Test
    fun provision_tokenForDifferentIdentity_rejects() = runBlocking {
        val tokenIdentity = SendspinIdentity.generate(crypto)
        val actualIdentity = SendspinIdentity.generate(crypto)
        val pairingPsk = SendspinPsk.generatePairingPsk(crypto)
        val token = SendspinPsk.createToken(tokenIdentity, pairingPsk)
        val storage = FakeSendspinCredentialStorage()

        val exception = try {
            SendspinPairingProvisioner(storage).provision(
                token = token,
                identity = actualIdentity
            )
            null
        } catch (exception: IllegalArgumentException) {
            exception
        }

        assertTrue(exception?.message?.contains("does not belong") == true)
        assertNull(storage.pairingPsk)
    }

    @Test
    fun provision_validToken_storesCopyOfPairingPsk() = runBlocking {
        val identity = SendspinIdentity.generate(crypto)
        val pairingPsk = SendspinPsk.generatePairingPsk(crypto)
        val token = SendspinPsk.createToken(identity, pairingPsk)
        val storage = FakeSendspinCredentialStorage()

        SendspinPairingProvisioner(storage).provision(
            token = token,
            identity = identity
        )

        assertArrayEquals(pairingPsk, storage.pairingPsk)
        assertTrue(storage.pairingPsk !== pairingPsk)
    }

    private class FakeSendspinCredentialStorage :
        SendspinCredentialStorage {

        var pairingPsk: ByteArray? = null

        override suspend fun saveStaticPrivateKey(key: ByteArray) {}

        override suspend fun getStaticPrivateKey(): ByteArray? = null

        override suspend fun savePairingPsk(psk: ByteArray) {
            pairingPsk = psk.copyOf()
        }

        override suspend fun getPairingPsk(): ByteArray? =
            pairingPsk?.copyOf()

        override suspend fun saveLongTermPsk(
            serverId: String,
            psk: ByteArray
        ) {}

        override suspend fun getLongTermPsk(
            serverId: String
        ): ByteArray? = null

        override suspend fun removeLongTermPsk(
            serverId: String
        ) {}

        override suspend fun saveServerStaticPublicKey(key: ByteArray) {}

        override suspend fun getServerStaticPublicKey(): ByteArray? = null

        override suspend fun clearCredentials() {
            pairingPsk = null
        }
    }
}
