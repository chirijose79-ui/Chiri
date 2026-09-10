package com.chirihome.platform.player.music.sendspin.crypto

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class JdkNoiseCrypto : NoiseCrypto {

    private val secureRandom = SecureRandom()

    override fun generateX25519PrivateKey(): ByteArray {
        val keyPairGenerator =
            KeyPairGenerator.getInstance("X25519")

        val keyPair =
            keyPairGenerator.generateKeyPair()

        val encoded =
            keyPair.private.encoded

        require(encoded.size >= 32) {
            "Invalid X25519 private key encoding"
        }

        return encoded.copyOfRange(
            encoded.size - 32,
            encoded.size
        )
    }

    override fun x25519PublicKey(
        privateKey: ByteArray
    ): ByteArray {
        require(privateKey.size == 32) {
            "X25519 private key must be 32 bytes"
        }

        return x25519Dh(
            privateKey = privateKey,
            publicKey = X25519_BASE_POINT
        )
    }

    override fun x25519Dh(
        privateKey: ByteArray,
        publicKey: ByteArray
    ): ByteArray {
        require(privateKey.size == 32) {
            "X25519 private key must be 32 bytes"
        }

        require(publicKey.size == 32) {
            "X25519 public key must be 32 bytes"
        }

        val privateKeyObject =
            createX25519PrivateKey(
                privateKey
            )

        val publicKeyObject =
            createX25519PublicKey(
                publicKey
            )

        val keyAgreement =
            KeyAgreement.getInstance("X25519")

        keyAgreement.init(
            privateKeyObject
        )

        keyAgreement.doPhase(
            publicKeyObject,
            true
        )

        val sharedSecret =
            keyAgreement.generateSecret()

        require(sharedSecret.size == 32) {
            "Invalid X25519 shared secret"
        }

        return sharedSecret
    }

    override fun chacha20Poly1305Encrypt(
        key: ByteArray,
        nonce: ByteArray,
        plaintext: ByteArray,
        aad: ByteArray
    ): ByteArray {
        require(key.size == 32) {
            "ChaCha20-Poly1305 key must be 32 bytes"
        }

        require(nonce.size == 12) {
            "ChaCha20-Poly1305 nonce must be 12 bytes"
        }

        val cipher =
            Cipher.getInstance(
                "ChaCha20-Poly1305"
            )

        val secretKey =
            SecretKeySpec(
                key,
                "ChaCha20"
            )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey,
            IvParameterSpec(nonce)
        )

        cipher.updateAAD(aad)

        return cipher.doFinal(
            plaintext
        )
    }

    override fun chacha20Poly1305Decrypt(
        key: ByteArray,
        nonce: ByteArray,
        ciphertext: ByteArray,
        aad: ByteArray
    ): ByteArray? {
        require(key.size == 32) {
            "ChaCha20-Poly1305 key must be 32 bytes"
        }

        require(nonce.size == 12) {
            "ChaCha20-Poly1305 nonce must be 12 bytes"
        }

        return try {
            val cipher =
                Cipher.getInstance(
                    "ChaCha20-Poly1305"
                )

            val secretKey =
                SecretKeySpec(
                    key,
                    "ChaCha20"
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                IvParameterSpec(nonce)
            )

            cipher.updateAAD(aad)

            cipher.doFinal(
                ciphertext
            )
        } catch (
            exception: Exception
        ) {
            null
        }
    }

    override fun sha256(
        data: ByteArray
    ): ByteArray {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(data)
    }

    override fun hmacSha256(
        key: ByteArray,
        data: ByteArray
    ): ByteArray {
        val mac =
            Mac.getInstance(
                "HmacSHA256"
            )

        mac.init(
            SecretKeySpec(
                key,
                "HmacSHA256"
            )
        )

        return mac.doFinal(
            data
        )
    }

    override fun randomBytes(
        size: Int
    ): ByteArray {
        require(size >= 0) {
            "Random byte count must not be negative"
        }

        return ByteArray(size).also {
            secureRandom.nextBytes(it)
        }
    }

    private fun createX25519PrivateKey(
        rawPrivateKey: ByteArray
    ): PrivateKey {
        val encoded =
            byteArrayOf(
                0x30,
                0x2e,
                0x02,
                0x01,
                0x00,
                0x30,
                0x05,
                0x06,
                0x03,
                0x2b,
                0x65,
                0x6e,
                0x04,
                0x22,
                0x04,
                0x20
            ) + rawPrivateKey

        val keyFactory =
            KeyFactory.getInstance(
                "X25519"
            )

        return keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(
                encoded
            )
        )
    }

    private fun createX25519PublicKey(
        rawPublicKey: ByteArray
    ): PublicKey {
        val encoded =
            byteArrayOf(
                0x30,
                0x2a,
                0x30,
                0x05,
                0x06,
                0x03,
                0x2b,
                0x65,
                0x6e,
                0x03,
                0x21,
                0x00
            ) + rawPublicKey

        val keyFactory =
            KeyFactory.getInstance(
                "X25519"
            )

        return keyFactory.generatePublic(
            X509EncodedKeySpec(
                encoded
            )
        )
    }

    companion object {

        private val X25519_BASE_POINT =
            ByteArray(32).apply {
                this[0] = 9
            }
    }
}