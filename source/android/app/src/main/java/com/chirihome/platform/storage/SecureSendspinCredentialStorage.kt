package com.chirihome.platform.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureSendspinCredentialStorage(
    context: Context
) : SendspinCredentialStorage {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    private val secretKey: SecretKey
        get() {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            val existingKey = keyStore.getKey(KEY_ALIAS, null)

            if (existingKey is SecretKey) {
                return existingKey
            }

            val keyGenerator = KeyGenerator.getInstance(
                android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            keyGenerator.init(
                android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                            android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(
                        android.security.keystore.KeyProperties.BLOCK_MODE_GCM
                    )
                    .setEncryptionPaddings(
                        android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
                    )
                    .build()
            )

            return keyGenerator.generateKey()
        }

    override suspend fun saveStaticPrivateKey(key: ByteArray) {
        saveEncrypted(KEY_STATIC_PRIVATE_KEY, key)
    }

    override suspend fun getStaticPrivateKey(): ByteArray? {
        return getDecrypted(KEY_STATIC_PRIVATE_KEY)
    }

    override suspend fun savePairingPsk(psk: ByteArray) {
        saveEncrypted(KEY_PAIRING_PSK, psk)
    }

    override suspend fun getPairingPsk(): ByteArray? {
        return getDecrypted(KEY_PAIRING_PSK)
    }

    override suspend fun saveLongTermPsk(
        serverId: String,
        psk: ByteArray
    ) {
        require(serverId.isNotBlank()) {
            "serverId must not be blank"
        }

        require(psk.size == 32) {
            "Long-Term PSK must be exactly 32 bytes"
        }

        saveEncrypted(
            "${KEY_LONG_TERM_PSK_PREFIX}${serverId}",
            psk
        )
    }

    override suspend fun getLongTermPsk(
        serverId: String
    ): ByteArray? {
        if (serverId.isBlank()) {
            return null
        }

        return getDecrypted(
            "${KEY_LONG_TERM_PSK_PREFIX}${serverId}"
        )
    }

    override suspend fun saveServerStaticPublicKey(key: ByteArray) {
        saveEncrypted(KEY_SERVER_STATIC_PUBLIC_KEY, key)
    }

    override suspend fun getServerStaticPublicKey(): ByteArray? {
        return getDecrypted(KEY_SERVER_STATIC_PUBLIC_KEY)
    }

    override suspend fun clearCredentials() {
        preferences.edit()
            .remove(KEY_STATIC_PRIVATE_KEY)
            .remove("${KEY_STATIC_PRIVATE_KEY}_iv")
            .remove(KEY_PAIRING_PSK)
            .remove("${KEY_PAIRING_PSK}_iv")
            .remove(KEY_SERVER_STATIC_PUBLIC_KEY)
            .remove("${KEY_SERVER_STATIC_PUBLIC_KEY}_iv")
            .apply()
    }

    private fun saveEncrypted(
        key: String,
        value: ByteArray
    ) {
        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey
        )

        val encrypted = cipher.doFinal(value)

        preferences.edit()
            .putString(
                "${key}_iv",
                Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            )
            .putString(
                key,
                Base64.encodeToString(encrypted, Base64.NO_WRAP)
            )
            .apply()
    }

    private fun getDecrypted(
        key: String
    ): ByteArray? {

        val encryptedData = preferences.getString(
            key,
            null
        ) ?: return null

        val iv = preferences.getString(
            "${key}_iv",
            null
        ) ?: return null

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)

            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(
                    GCM_TAG_LENGTH,
                    Base64.decode(iv, Base64.NO_WRAP)
                )
            )

            cipher.doFinal(
                Base64.decode(
                    encryptedData,
                    Base64.NO_WRAP
                )
            )
        } catch (exception: Exception) {
            null
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        private const val KEY_ALIAS =
            "chiri_sendspin_key"

        private const val PREFERENCES_NAME =
            "chiri_secure_sendspin"

        private const val KEY_STATIC_PRIVATE_KEY =
            "static_private_key"

        private const val KEY_PAIRING_PSK =
            "pairing_psk"

        private const val KEY_LONG_TERM_PSK_PREFIX =
            "long_term_psk_"

        private const val KEY_SERVER_STATIC_PUBLIC_KEY =
            "server_static_public_key"

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        private const val GCM_TAG_LENGTH =
            128
    }
}