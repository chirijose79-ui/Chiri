package com.chirihome.platform.storage

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureSendspinCredentialStorageTest {

    private lateinit var storage: SecureSendspinCredentialStorage

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        storage = SecureSendspinCredentialStorage(context)
        runBlockingClear()
    }

    @After
    fun tearDown() {
        runBlockingClear()
    }

    @Test
    fun credentialsCanBeSavedAndReadBack() {
        val privateKey = ByteArray(32) { it.toByte() }
        val pairingPsk = ByteArray(32) { (it + 32).toByte() }
        val serverPublicKey = ByteArray(32) { (it + 64).toByte() }

        kotlinx.coroutines.runBlocking {
            storage.saveStaticPrivateKey(privateKey)
            storage.savePairingPsk(pairingPsk)
            storage.saveServerStaticPublicKey(serverPublicKey)

            assertArrayEquals(
                privateKey,
                storage.getStaticPrivateKey()
            )

            assertArrayEquals(
                pairingPsk,
                storage.getPairingPsk()
            )

            assertArrayEquals(
                serverPublicKey,
                storage.getServerStaticPublicKey()
            )
        }
    }

    @Test
    fun credentialsSurviveNewStorageInstance() {
        val privateKey = ByteArray(32) { (it + 1).toByte() }

        kotlinx.coroutines.runBlocking {
            storage.saveStaticPrivateKey(privateKey)

            val newStorage =
                SecureSendspinCredentialStorage(context)

            assertArrayEquals(
                privateKey,
                newStorage.getStaticPrivateKey()
            )
        }
    }

    @Test
    fun clearCredentialsRemovesAllCredentials() {
        val privateKey = ByteArray(32) { it.toByte() }
        val pairingPsk = ByteArray(32) { (it + 32).toByte() }
        val serverPublicKey = ByteArray(32) { (it + 64).toByte() }

        kotlinx.coroutines.runBlocking {
            storage.saveStaticPrivateKey(privateKey)
            storage.savePairingPsk(pairingPsk)
            storage.saveServerStaticPublicKey(serverPublicKey)

            storage.clearCredentials()

            assertNull(storage.getStaticPrivateKey())
            assertNull(storage.getPairingPsk())
            assertNull(storage.getServerStaticPublicKey())
        }
    }

    @Test
    fun missingCredentialsReturnNull() {
        kotlinx.coroutines.runBlocking {
            storage.clearCredentials()

            assertNull(storage.getStaticPrivateKey())
            assertNull(storage.getPairingPsk())
            assertNull(storage.getServerStaticPublicKey())
        }
    }

    private fun runBlockingClear() {
        kotlinx.coroutines.runBlocking {
            storage.clearCredentials()
        }
    }
}