package com.chirihome.platform.player.music.sendspin.crypto

/**
 * Low-level Noise protocol state used by Sendspin.
 *
 * This file intentionally starts with CipherState and SymmetricState.
 * The KKpsk2 handshake will be added only after these primitives are
 * independently validated.
 */
object NoiseProtocol {

    /**
     * Noise CipherState.
     *
     * Maintains:
     * - encryption key k
     * - nonce counter n
     *
     * Noise uses a 64-bit little-endian nonce encoded into the
     * 12-byte nonce required by ChaCha20-Poly1305:
     *
     *   00 00 00 00 || uint64_le(n)
     */
    class CipherState(
        private val crypto: NoiseCrypto
    ) {

        private var key: ByteArray? = null
        private var nonce: Long = 0

        fun initializeKey(newKey: ByteArray?) {
            if (newKey != null) {
                require(newKey.size == 32) {
                    "Noise cipher key must be 32 bytes"
                }

                key = newKey.copyOf()
            } else {
                key = null
            }

            nonce = 0
        }

        fun hasKey(): Boolean {
            return key != null
        }

        fun getNonce(): Long {
            return nonce
        }

        fun encryptWithAd(
            aad: ByteArray,
            plaintext: ByteArray
        ): ByteArray {

            val currentKey = key

            /*
             * Noise permits encryption before a key is established.
             * In that case the payload is plaintext and the nonce
             * does not advance.
             */
            if (currentKey == null) {
                return plaintext.copyOf()
            }

            check(nonce != Long.MAX_VALUE) {
                "Noise nonce exhausted"
            }

            val encrypted = crypto.chacha20Poly1305Encrypt(
                key = currentKey,
                nonce = nonceBytes(nonce),
                plaintext = plaintext,
                aad = aad
            )

            nonce++

            return encrypted
        }

        fun decryptWithAd(
            aad: ByteArray,
            ciphertext: ByteArray
        ): ByteArray? {

            val currentKey = key

            /*
             * Before a key is established, Noise treats the payload
             * as plaintext.
             */
            if (currentKey == null) {
                return ciphertext.copyOf()
            }

            check(nonce != Long.MAX_VALUE) {
                "Noise nonce exhausted"
            }

            val decrypted = crypto.chacha20Poly1305Decrypt(
                key = currentKey,
                nonce = nonceBytes(nonce),
                ciphertext = ciphertext,
                aad = aad
            )

            if (decrypted != null) {
                nonce++
            }

            return decrypted
        }

        private fun nonceBytes(value: Long): ByteArray {
            return ByteArray(12).also { bytes ->

                /*
                 * First four bytes remain zero.
                 *
                 * Noise specification:
                 * 4 zero bytes || little-endian uint64 nonce
                 */
                for (i in 0 until 8) {
                    bytes[4 + i] =
                        (value ushr (8 * i)).toByte()
                }
            }
        }
    }

    /**
     * Noise SymmetricState.
     *
     * Contains:
     * - chaining key ck
     * - handshake hash h
     * - CipherState
     *
     * This implements the fundamental Noise hash/key operations.
     */
    class SymmetricState(
        private val crypto: NoiseCrypto,
        private val cipherState: CipherState = CipherState(crypto)
    ) {

        private lateinit var chainingKey: ByteArray
        private lateinit var handshakeHash: ByteArray

        fun initializeSymmetric(
            protocolName: ByteArray
        ) {
            require(protocolName.isNotEmpty()) {
                "Noise protocol name must not be empty"
            }

            /*
             * Noise initialization:
             *
             * If protocol name <= HASHLEN:
             *   h = protocolName padded with zeros
             *
             * Otherwise:
             *   h = HASH(protocolName)
             *
             * For SHA-256 HASHLEN = 32.
             */
            if (protocolName.size <= 32) {
                handshakeHash = ByteArray(32)

                protocolName.copyInto(
                    destination = handshakeHash,
                    destinationOffset = 0
                )
            } else {
                handshakeHash = crypto.sha256(protocolName)
            }

            chainingKey = handshakeHash.copyOf()

            cipherState.initializeKey(null)
        }

        fun mixHash(data: ByteArray) {
            handshakeHash = crypto.sha256(
                handshakeHash + data
            )
        }

        fun mixKey(inputKeyMaterial: ByteArray) {
            val output = hkdf2(
                chainingKey,
                inputKeyMaterial
            )

            chainingKey = output.first

            cipherState.initializeKey(
                output.second
            )
        }

        fun mixKeyAndHash(
            inputKeyMaterial: ByteArray
        ) {
            val output = hkdf3(
                chainingKey,
                inputKeyMaterial
            )

            chainingKey = output.first

            /*
             * Noise mixKeyAndHash:
             *
             * ck, temp_h, temp_k = HKDF(ck, input_key_material)
             *
             * h = HASH(h || temp_h)
             *
             * k = temp_k
             */

            mixHash(output.second)

            cipherState.initializeKey(
                output.third
            )
        }

        fun encryptAndHash(
            plaintext: ByteArray
        ): ByteArray {

            val ciphertext = cipherState.encryptWithAd(
                handshakeHash,
                plaintext
            )

            mixHash(ciphertext)

            return ciphertext
        }

        fun decryptAndHash(
            ciphertext: ByteArray
        ): ByteArray? {

            val plaintext = cipherState.decryptWithAd(
                handshakeHash,
                ciphertext
            )

            if (plaintext != null) {
                mixHash(ciphertext)
            }

            return plaintext
        }

        fun getHandshakeHash(): ByteArray {
            return handshakeHash.copyOf()
        }

        fun getChainingKey(): ByteArray {
            return chainingKey.copyOf()
        }

        fun getCipherState(): CipherState {
            return cipherState
        }

        /**
         * Noise HKDF with two outputs.
         *
         * HKDF:
         *
         * temp_key = HMAC(chainingKey, input)
         * output1  = HMAC(temp_key, 0x01)
         * output2  = HMAC(temp_key, output1 || 0x02)
         */
        private fun hkdf2(
            chainingKey: ByteArray,
            inputKeyMaterial: ByteArray
        ): Pair<ByteArray, ByteArray> {

            val tempKey = crypto.hmacSha256(
                chainingKey,
                inputKeyMaterial
            )

            val output1 = crypto.hmacSha256(
                tempKey,
                byteArrayOf(0x01)
            )

            val output2 = crypto.hmacSha256(
                tempKey,
                output1 + byteArrayOf(0x02)
            )

            return output1 to output2
        }

        /**
         * Noise HKDF with three outputs.
         */
        private fun hkdf3(
            chainingKey: ByteArray,
            inputKeyMaterial: ByteArray
        ): Triple<ByteArray, ByteArray, ByteArray> {

            val tempKey = crypto.hmacSha256(
                chainingKey,
                inputKeyMaterial
            )

            val output1 = crypto.hmacSha256(
                tempKey,
                byteArrayOf(0x01)
            )

            val output2 = crypto.hmacSha256(
                tempKey,
                output1 + byteArrayOf(0x02)
            )

            val output3 = crypto.hmacSha256(
                tempKey,
                output2 + byteArrayOf(0x03)
            )

            return Triple(
                output1,
                output2,
                output3
            )
        }
    }

    /**
     * Noise handshake state for Sendspin KKpsk2.
     *
     * Sendspin uses:
     *
     *   -> e, es, ss
     *   <- e, ee, se, psk
     *
     * The Sendspin server is the Noise initiator.
     * The Android client is the Noise responder.
     */
    class HandshakeState(
        private val crypto: NoiseCrypto
    ) {

        companion object {
            const val SENDSPIN_PROTOCOL_NAME =
                "Noise_KKpsk2_25519_ChaChaPoly_SHA256"
        }

        private val symmetricState =
            SymmetricState(crypto)

        private var initialized = false
        private var initiator = false

        private var localStaticPrivateKey: ByteArray? = null
        private var localStaticPublicKey: ByteArray? = null

        private var remoteStaticPublicKey: ByteArray? = null

        private var localEphemeralPrivateKey: ByteArray? = null
        private var localEphemeralPublicKey: ByteArray? = null

        private var remoteEphemeralPublicKey: ByteArray? = null

        private var psk: ByteArray? = null

        /**
         * Initializes the KKpsk2 handshake state.
         */
        fun initialize(
            localStaticPrivateKey: ByteArray,
            remoteStaticPublicKey: ByteArray,
            prologue: ByteArray,
            initiator: Boolean,
            psk: ByteArray?
        ) {
            require(localStaticPrivateKey.size == 32) {
                "Local X25519 private key must be 32 bytes"
            }

            require(remoteStaticPublicKey.size == 32) {
                "Remote X25519 public key must be 32 bytes"
            }

            if (psk != null) {
                require(psk.size == 32) {
                    "Noise PSK must be 32 bytes"
                }
            }

            this.localStaticPrivateKey =
                localStaticPrivateKey.copyOf()

            this.localStaticPublicKey =
                crypto.x25519PublicKey(
                    localStaticPrivateKey
                )

            this.remoteStaticPublicKey =
                remoteStaticPublicKey.copyOf()

            this.initiator = initiator

            this.psk = psk?.copyOf()

            symmetricState.initializeSymmetric(
                SENDSPIN_PROTOCOL_NAME
                    .toByteArray(Charsets.UTF_8)
            )

            /*
             * Noise prologue.
             *
             * Sendspin defines the prologue as the exact bytes
             * transmitted during client/init and server/init.
             */
            symmetricState.mixHash(prologue)

            /*
             * KK pre-messages.
             *
             * The initiator knows its own static key first,
             * followed by the responder's static key.
             *
             * The responder sees the same keys in the opposite
             * local/remote orientation.
             */
            if (initiator) {
                symmetricState.mixHash(
                    localStaticPublicKey
                        ?: error("Local static public key missing")
                )

                symmetricState.mixHash(
                    remoteStaticPublicKey
                        ?: error("Remote static public key missing")
                )
            } else {
                symmetricState.mixHash(
                    remoteStaticPublicKey
                        ?: error("Remote static public key missing")
                )

                symmetricState.mixHash(
                    localStaticPublicKey
                        ?: error("Local static public key missing")
                )
            }

            initialized = true
        }

        /**
         * Generates a fresh ephemeral X25519 keypair.
         */
        fun generateEphemeralKeyPair() {
            checkInitialized()

            val privateKey =
                crypto.generateX25519PrivateKey()

            val publicKey =
                crypto.x25519PublicKey(privateKey)

            localEphemeralPrivateKey =
                privateKey

            localEphemeralPublicKey =
                publicKey
        }

        fun setEphemeralPrivateKey(privateKey: ByteArray) {
            checkInitialized()

            require(privateKey.size == 32) {
                "Ephemeral X25519 private key must be 32 bytes"
            }

            localEphemeralPrivateKey =
                privateKey.copyOf()

            localEphemeralPublicKey =
                crypto.x25519PublicKey(
                    privateKey
                )
        }

        /**
         * Returns the local static public key.
         */
        fun localStaticPublicKey(): ByteArray {
            checkInitialized()

            return localStaticPublicKey!!
                .copyOf()
        }

        /**
         * Returns the local ephemeral public key.
         */
        fun localEphemeralPublicKey(): ByteArray {
            checkInitialized()

            return localEphemeralPublicKey
                ?.copyOf()
                ?: error(
                    "Ephemeral keypair has not been generated"
                )
        }

        /**
         * Stores the remote ephemeral public key.
         */
        fun setRemoteEphemeralPublicKey(
            publicKey: ByteArray
        ) {
            checkInitialized()

            require(publicKey.size == 32) {
                "Remote X25519 ephemeral public key must be 32 bytes"
            }

            remoteEphemeralPublicKey =
                publicKey.copyOf()
        }

        /**
         * Performs:
         *
         *   local static × remote static
         */
        fun dhStaticStatic() {
            checkInitialized()

            val localPrivate =
                localStaticPrivateKey
                    ?: error("Local static private key missing")

            val remotePublic =
                remoteStaticPublicKey
                    ?: error("Remote static public key missing")

            symmetricState.mixKey(
                crypto.x25519Dh(
                    localPrivate,
                    remotePublic
                )
            )
        }

        /**
         * Performs:
         *
         *   local ephemeral × remote static
         */
        fun dhEphemeralStatic() {
            checkInitialized()

            val localPrivate =
                localEphemeralPrivateKey
                    ?: error("Local ephemeral private key missing")

            val remotePublic =
                remoteStaticPublicKey
                    ?: error("Remote static public key missing")

            symmetricState.mixKey(
                crypto.x25519Dh(
                    localPrivate,
                    remotePublic
                )
            )
        }

        /**
         * Performs:
         *
         *   local ephemeral × remote ephemeral
         */
        fun dhEphemeralEphemeral() {
            checkInitialized()

            val localPrivate =
                localEphemeralPrivateKey
                    ?: error("Local ephemeral private key missing")

            val remotePublic =
                remoteEphemeralPublicKey
                    ?: error("Remote ephemeral public key missing")

            symmetricState.mixKey(
                crypto.x25519Dh(
                    localPrivate,
                    remotePublic
                )
            )
        }

        /**
         * Performs:
         *
         *   local static × remote ephemeral
         */
        fun dhStaticEphemeral() {
            checkInitialized()

            val localPrivate =
                localStaticPrivateKey
                    ?: error("Local static private key missing")

            val remotePublic =
                remoteEphemeralPublicKey
                    ?: error("Remote ephemeral public key missing")

            symmetricState.mixKey(
                crypto.x25519Dh(
                    localPrivate,
                    remotePublic
                )
            )
        }

        /**
         * Applies the KKpsk2 PSK operation.
         *
         * Noise:
         *
         *   mixKeyAndHash(psk)
         */
        fun mixPsk() {
            checkInitialized()

            val currentPsk =
                psk ?: error("No PSK configured")

            symmetricState.mixKeyAndHash(
                currentPsk
            )
        }

        /**
         * Encrypts a handshake payload.
         */
        fun encryptAndHash(
            plaintext: ByteArray
        ): ByteArray {
            checkInitialized()

            return symmetricState.encryptAndHash(
                plaintext
            )
        }

        /**
         * Decrypts a handshake payload.
         */
        fun decryptAndHash(
            ciphertext: ByteArray
        ): ByteArray? {
            checkInitialized()

            return symmetricState.decryptAndHash(
                ciphertext
            )
        }

        /**
         * Returns the current handshake hash.
         */
        fun handshakeHash(): ByteArray {
            checkInitialized()

            return symmetricState
                .getHandshakeHash()
        }

        /**
         * Returns the current chaining key.
         */
        fun chainingKey(): ByteArray {
            checkInitialized()

            return symmetricState
                .getChainingKey()
        }

        /**
         * Returns the underlying CipherState.
         */
        fun cipherState(): CipherState {
            checkInitialized()

            return symmetricState
                .getCipherState()
        }

        /**
         * KKpsk2 message 1:
         *
         *   -> e, es, ss
         *
         * The initiator:
         *
         * 1. generates e
         * 2. mixes e into h
         * 3. performs DH(e, rs)
         * 4. mixes that DH into ck
         * 5. performs DH(s, rs)
         * 6. mixes that DH into ck
         *
         * Returns the 32-byte ephemeral public key.
         */
        fun writeMessage1(): ByteArray {
            checkInitialized()

            check(initiator) {
                "writeMessage1() is only valid for the initiator"
            }

            if (localEphemeralPrivateKey == null) {
                generateEphemeralKeyPair()
            }

            val ephemeralPublic =
                localEphemeralPublicKey()

            symmetricState.mixHash(
                ephemeralPublic
            )

            dhEphemeralStatic()
            dhStaticStatic()

            return ephemeralPublic
        }

        /**
         * KKpsk2 message 1:
         *
         *   -> e, es, ss
         *
         * Responder receives the initiator ephemeral key.
         */
        fun readMessage1(
            ephemeralPublicKey: ByteArray
        ) {
            checkInitialized()

            check(!initiator) {
                "readMessage1() is only valid for the responder"
            }

            setRemoteEphemeralPublicKey(
                ephemeralPublicKey
            )

            symmetricState.mixHash(
                ephemeralPublicKey
            )

            /*
             * responder:
             *
             * es = DH(rs, ie)
             * ss = DH(rs, is)
             */
            dhStaticEphemeral()
            dhStaticStatic()
        }

        /**
         * KKpsk2 message 2:
         *
         *   <- e, ee, se, psk
         *
         * The responder:
         *
         * 1. generates e
         * 2. mixes e into h
         * 3. performs DH(e, re)
         * 4. performs DH(s, re)
         * 5. applies PSK
         *
         * Returns the responder ephemeral public key.
         */
        fun writeMessage2(): ByteArray {
            checkInitialized()

            check(!initiator) {
                "writeMessage2() is only valid for the responder"
            }

            if (localEphemeralPrivateKey == null) {
                generateEphemeralKeyPair()
            }

            val ephemeralPublic =
                localEphemeralPublicKey()

            symmetricState.mixHash(
                ephemeralPublic
            )

            /*
             * ee = DH(e, re)
             */
            dhEphemeralEphemeral()

            /*
             * se = DH(s, re)
             */
            dhStaticEphemeral()

            /*
             * psk = mixKeyAndHash(psk)
             */
            mixPsk()

            return ephemeralPublic
        }

        /**
         * KKpsk2 message 2:
         *
         *   <- e, ee, se, psk
         *
         * Initiator receives the responder ephemeral key.
         */
        fun readMessage2(
            ephemeralPublicKey: ByteArray
        ) {
            checkInitialized()

            check(initiator) {
                "readMessage2() is only valid for the initiator"
            }

            setRemoteEphemeralPublicKey(
                ephemeralPublicKey
            )

            symmetricState.mixHash(
                ephemeralPublicKey
            )

            /*
             * ee = DH(e, re)
             */
            dhEphemeralEphemeral()

            /*
             * se = DH(s, re)
             */
            dhStaticEphemeral()

            /*
             * psk = mixKeyAndHash(psk)
             */
            mixPsk()
        }

        private fun checkInitialized() {
            check(initialized) {
                "Noise handshake has not been initialized"
            }
        }
    }
}
