package com.chirihome.platform.player.music.sendspin.crypto

interface NoiseCrypto {

    fun generateX25519PrivateKey(): ByteArray

    fun x25519PublicKey(
        privateKey: ByteArray
    ): ByteArray

    fun x25519Dh(
        privateKey: ByteArray,
        publicKey: ByteArray
    ): ByteArray

    fun chacha20Poly1305Encrypt(
        key: ByteArray,
        nonce: ByteArray,
        plaintext: ByteArray,
        aad: ByteArray = ByteArray(0)
    ): ByteArray

    fun chacha20Poly1305Decrypt(
        key: ByteArray,
        nonce: ByteArray,
        ciphertext: ByteArray,
        aad: ByteArray = ByteArray(0)
    ): ByteArray?

    fun sha256(
        data: ByteArray
    ): ByteArray

    fun hmacSha256(
        key: ByteArray,
        data: ByteArray
    ): ByteArray

    fun randomBytes(
        size: Int
    ): ByteArray
}