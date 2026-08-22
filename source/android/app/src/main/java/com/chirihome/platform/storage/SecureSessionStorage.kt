package com.chirihome.platform.storage

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureSessionStorage(
    context: Context
) : SessionStorage {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    private val secretKey: SecretKey
        get() {
            val keyStore = java.security.KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            val existingKey = keyStore.getKey(KEY_ALIAS, null)

            if (existingKey is SecretKey) {
                return existingKey
            }

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or
                            KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_NONE
                    )
                    .build()
            )

            return keyGenerator.generateKey()
        }

    override suspend fun saveAccessToken(token: String) {
        saveEncrypted(KEY_ACCESS_TOKEN, token)
    }

    override suspend fun getAccessToken(): String? {
        return getDecrypted(KEY_ACCESS_TOKEN)
    }

    override suspend fun saveRefreshToken(token: String) {
        saveEncrypted(KEY_REFRESH_TOKEN, token)
    }

    override suspend fun getRefreshToken(): String? {
        return getDecrypted(KEY_REFRESH_TOKEN)
    }

    override suspend fun clearSession() {
        preferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    private fun saveEncrypted(
        key: String,
        value: String
    ) {
        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey
        )

        val encrypted = cipher.doFinal(
            value.toByteArray(StandardCharsets.UTF_8)
        )

        val iv = Base64.encodeToString(
            cipher.iv,
            Base64.NO_WRAP
        )

        val data = Base64.encodeToString(
            encrypted,
            Base64.NO_WRAP
        )

        preferences.edit()
            .putString("${key}_iv", iv)
            .putString(key, data)
            .apply()
    }

    private fun getDecrypted(
        key: String
    ): String? {

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

            val decrypted = cipher.doFinal(
                Base64.decode(
                    encryptedData,
                    Base64.NO_WRAP
                )
            )

            String(
                decrypted,
                StandardCharsets.UTF_8
            )
        } catch (exception: Exception) {
            null
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE =
            "AndroidKeyStore"

        private const val KEY_ALIAS =
            "chiri_session_key"

        private const val PREFERENCES_NAME =
            "chiri_secure_session"

        private const val KEY_ACCESS_TOKEN =
            "access_token"

        private const val KEY_REFRESH_TOKEN =
            "refresh_token"

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        private const val GCM_TAG_LENGTH =
            128
    }
}