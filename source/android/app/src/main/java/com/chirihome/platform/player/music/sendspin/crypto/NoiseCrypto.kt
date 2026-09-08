package com.chirihome.platform.player.music.sendspin.crypto

interface NoiseCrypto {

    /**
     * Generates a new random X25519 private key.
     *
     * The returned value must contain exactly 32 raw bytes.
     */
    fun generateX25519PrivateKey(): ByteArray

    /**
     * Derives the raw 32-byte X25519 public key from a raw private key.
     */
    fun x25519PublicKey(
        privateKey: ByteArray
    ): ByteArray

    /**
     * Performs X25519 Diffie-Hellman.
     *
     * Returns the raw 32-byte shared secret.
     */
    fun x25519Dh(
        privateKey: ByteArray,
        publicKey: ByteArray
    ): ByteArray

    /**
     * ChaCha20-Poly1305 encryption.
     *
     * nonce must contain exactly 12 bytes.
     * aad may be empty.
     *
     * The returned ciphertext includes the 16-byte authentication tag.
     */
    fun chacha20Poly1305Encrypt(
        key: ByteArray,
        nonce: ByteArray,
        plaintext: ByteArray,
        aad: ByteArray = ByteArray(0)
    ): ByteArray

    /**
     * ChaCha20-Poly1305 decryption.
     *
     * nonce must contain exactly 12 bytes.
     * Returns null when authentication fails.
     */
    fun chacha20Poly1305Decrypt(
        key: ByteArray,
        nonce: ByteArray,
        ciphertext: ByteArray,
        aad: ByteArray = ByteArray(0)
    ): ByteArray?

    /**
     * SHA-256 digest.
     */
    fun sha256(
        data: ByteArray
    ): ByteArray

    /**
     * HMAC-SHA-256.
     */
    fun hmacSha256(
        key: ByteArray,
        data: ByteArray
    ): ByteArray

    /**
     * Cryptographically secure random bytes.
     */
    fun randomBytes(
        size: Int
    ): ByteArray
}