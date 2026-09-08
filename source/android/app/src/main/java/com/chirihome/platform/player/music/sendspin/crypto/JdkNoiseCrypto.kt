package com.chirihome.platform.player.music.sendspin.crypto

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.NamedParameterSpec
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
        /*
         * RFC 7748 X25519 private keys are 32 random bytes.
         *
         * The X25519 implementation itself performs the required
         * clamping when the scalar is used.
         */
        return ByteArray(32).also {
            secureRandom.nextBytes(it)
        }
    }

    override fun x25519PublicKey(
        privateKey: ByteArray
    ): ByteArray {
        require(privateKey.size == 32) {
            "X25519 private key must be 32 bytes"
        }

        /*
         * Build a PKCS#8 X25519 private-key structure around
         * the raw 32-byte scalar.
         *
         * PKCS#8:
         *
         * SEQUENCE
         *   INTEGER 0
         *   SEQUENCE { OID X25519 }
         *   OCTET STRING
         *       OCTET STRING <32 raw bytes>
         */
        val encodedPrivateKey = encodeX25519PrivateKey(privateKey)

        val keyFactory = KeyFactory.getInstance("X25519")
        val privateKeyObject = keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(encodedPrivateKey)
        )

        /*
         * X25519 public key = scalar multiplication by the
         * standard base point.
         *
         * We perform this through KeyAgreement using the
         * standard base-point public key (u = 9).
         */
        val basePointPublicKey = encodeX25519PublicKey(
            ByteArray(32).also { it[0] = 9 }
        )

        val basePointObject = keyFactory.generatePublic(
            X509EncodedKeySpec(basePointPublicKey)
        )

        val agreement = KeyAgreement.getInstance("X25519")
        agreement.init(privateKeyObject)
        agreement.doPhase(basePointObject, true)

        return agreement.generateSecret().also {
            require(it.size == 32) {
                "X25519 public key must be 32 bytes"
            }
        }
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

        val keyFactory = KeyFactory.getInstance("X25519")

        val privateKeyObject = keyFactory.generatePrivate(
            PKCS8EncodedKeySpec(
                encodeX25519PrivateKey(privateKey)
            )
        )

        val publicKeyObject = keyFactory.generatePublic(
            X509EncodedKeySpec(
                encodeX25519PublicKey(publicKey)
            )
        )

        val agreement = KeyAgreement.getInstance("X25519")

        agreement.init(privateKeyObject)
        agreement.doPhase(publicKeyObject, true)

        return agreement.generateSecret().also {
            require(it.size == 32) {
                "X25519 DH output must be 32 bytes"
            }
        }
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

        val cipher = Cipher.getInstance("ChaCha20-Poly1305")

        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "ChaCha20"),
            IvParameterSpec(nonce)
        )

        cipher.updateAAD(aad)

        return cipher.doFinal(plaintext)
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
            val cipher = Cipher.getInstance("ChaCha20-Poly1305")

            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "ChaCha20"),
                IvParameterSpec(nonce)
            )

            cipher.updateAAD(aad)

            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
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
        val mac = Mac.getInstance("HmacSHA256")

        mac.init(
            SecretKeySpec(key, "HmacSHA256")
        )

        return mac.doFinal(data)
    }

    override fun randomBytes(
        size: Int
    ): ByteArray {
        require(size >= 0) {
            "size must be non-negative"
        }

        return ByteArray(size).also {
            secureRandom.nextBytes(it)
        }
    }

    private fun encodeX25519PrivateKey(
        rawPrivateKey: ByteArray
    ): ByteArray {
        /*
         * RFC 8410:
         *
         * 30 2E
         *    02 01 00
         *    30 05
         *       06 03 2B 65 6E
         *    04 22
         *       04 20
         *          <32 bytes>
         */
        return byteArrayOf(
            0x30, 0x2e,
            0x02, 0x01, 0x00,
            0x30, 0x05,
            0x06, 0x03,
            0x2b, 0x65, 0x6e,
            0x04, 0x22,
            0x04, 0x20
        ) + rawPrivateKey
    }

    private fun encodeX25519PublicKey(
        rawPublicKey: ByteArray
    ): ByteArray {
        /*
         * RFC 8410:
         *
         * 30 2A
         *    30 05
         *       06 03 2B 65 6E
         *    03 21
         *       00
         *       <32 bytes>
         */
        return byteArrayOf(
            0x30, 0x2a,
            0x30, 0x05,
            0x06, 0x03,
            0x2b, 0x65, 0x6e,
            0x03, 0x21,
            0x00
        ) + rawPublicKey
    }
}