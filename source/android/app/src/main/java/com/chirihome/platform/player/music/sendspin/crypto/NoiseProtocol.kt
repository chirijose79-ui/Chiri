package com.chirihome.platform.player.music.sendspin.crypto

internal const val DH_LEN = 32
internal const val HASH_LEN = 32
internal const val KEY_LEN = 32
internal const val TAG_LEN = 16

class NoiseException(
    message: String
) : Exception(message)

enum class NoiseRole {
    INITIATOR,
    RESPONDER
}

data class X25519KeyPair(
    val privateKey: ByteArray,
    val publicKey: ByteArray
)

internal enum class NoiseToken {
    E,
    ES,
    SS,
    EE,
    SE,
    PSK
}

internal data class NoisePattern(
    val name: String,
    val initiatorPreSharesStatic: Boolean,
    val responderPreSharesStatic: Boolean,
    val messages: List<List<NoiseToken>>,
    val hasPsk: Boolean
) {

    companion object {

        val KK =
            NoisePattern(
                name = "KK",
                initiatorPreSharesStatic = true,
                responderPreSharesStatic = true,
                messages = listOf(
                    listOf(
                        NoiseToken.E,
                        NoiseToken.ES,
                        NoiseToken.SS
                    ),
                    listOf(
                        NoiseToken.E,
                        NoiseToken.EE,
                        NoiseToken.SE
                    )
                ),
                hasPsk = false
            )

        val KKPSK2 =
            NoisePattern(
                name = "KKpsk2",
                initiatorPreSharesStatic = true,
                responderPreSharesStatic = true,
                messages = listOf(
                    listOf(
                        NoiseToken.E,
                        NoiseToken.ES,
                        NoiseToken.SS
                    ),
                    listOf(
                        NoiseToken.E,
                        NoiseToken.EE,
                        NoiseToken.SE,
                        NoiseToken.PSK
                    )
                ),
                hasPsk = true
            )
    }
}

class CipherState(
    private val crypto: NoiseCrypto
) {

    private var key: ByteArray? = null

    internal var nonce: ULong = 0u

    val hasKey: Boolean
        get() = key != null

    internal fun initializeKey(
        newKey: ByteArray?
    ) {
        if (newKey != null) {
            require(newKey.size == KEY_LEN) {
                "Cipher key must be 32 bytes"
            }
        }

        key = newKey
        nonce = 0u
    }

    private fun checkNonce() {
        if (nonce == ULong.MAX_VALUE) {
            throw NoiseException(
                "Cipher nonce exhausted"
            )
        }
    }

    private fun nonceBytes(): ByteArray {
        val result =
            ByteArray(12)

        for (index in 0 until 8) {
            result[4 + index] =
                (
                        (nonce shr (index * 8)) and
                                0xFFu
                        ).toByte()
        }

        return result
    }

    fun encryptWithAd(
        associatedData: ByteArray,
        plaintext: ByteArray
    ): ByteArray {

        val currentKey =
            key
                ?: return plaintext

        checkNonce()

        val ciphertext =
            crypto.chacha20Poly1305Encrypt(
                key = currentKey,
                nonce = nonceBytes(),
                plaintext = plaintext,
                aad = associatedData
            )

        nonce++

        return ciphertext
    }

    fun decryptWithAd(
        associatedData: ByteArray,
        ciphertext: ByteArray
    ): ByteArray {

        val currentKey =
            key
                ?: return ciphertext

        checkNonce()

        val plaintext =
            crypto.chacha20Poly1305Decrypt(
                key = currentKey,
                nonce = nonceBytes(),
                ciphertext = ciphertext,
                aad = associatedData
            )
                ?: throw NoiseException(
                    "AEAD authentication failed"
                )

        nonce++

        return plaintext
    }
}

internal class SymmetricState(
    private val crypto: NoiseCrypto
) {

    private lateinit var chainingKey: ByteArray

    lateinit var handshakeHash: ByteArray
        private set

    val cipher =
        CipherState(crypto)

    fun initialize(
        protocolName: String
    ) {
        val protocolBytes =
            protocolName.toByteArray()

        handshakeHash =
            if (protocolBytes.size <= HASH_LEN) {
                protocolBytes.copyOf(HASH_LEN)
            } else {
                crypto.sha256(
                    protocolBytes
                )
            }

        chainingKey =
            handshakeHash.copyOf()
    }

    private fun hkdf(
        inputKeyMaterial: ByteArray,
        outputCount: Int
    ): List<ByteArray> {

        require(
            outputCount in 1..3
        ) {
            "HKDF output count must be 1..3"
        }

        val tempKey =
            crypto.hmacSha256(
                chainingKey,
                inputKeyMaterial
            )

        val output1 =
            crypto.hmacSha256(
                tempKey,
                byteArrayOf(0x01)
            )

        if (outputCount == 1) {
            return listOf(
                output1
            )
        }

        val output2 =
            crypto.hmacSha256(
                tempKey,
                output1 + byteArrayOf(0x02)
            )

        if (outputCount == 2) {
            return listOf(
                output1,
                output2
            )
        }

        val output3 =
            crypto.hmacSha256(
                tempKey,
                output2 + byteArrayOf(0x03)
            )

        return listOf(
            output1,
            output2,
            output3
        )
    }

    fun mixKey(
        inputKeyMaterial: ByteArray
    ) {
        val outputs =
            hkdf(
                inputKeyMaterial,
                2
            )

        chainingKey =
            outputs[0]

        cipher.initializeKey(
            outputs[1]
        )
    }

    fun mixHash(
        data: ByteArray
    ) {
        handshakeHash =
            crypto.sha256(
                handshakeHash + data
            )
    }

    fun mixKeyAndHash(
        inputKeyMaterial: ByteArray
    ) {
        val outputs =
            hkdf(
                inputKeyMaterial,
                3
            )

        chainingKey =
            outputs[0]

        mixHash(
            outputs[1]
        )

        cipher.initializeKey(
            outputs[2]
        )
    }

    fun encryptAndHash(
        plaintext: ByteArray
    ): ByteArray {

        val ciphertext =
            cipher.encryptWithAd(
                associatedData = handshakeHash,
                plaintext = plaintext
            )

        mixHash(
            ciphertext
        )

        return ciphertext
    }

    fun decryptAndHash(
        ciphertext: ByteArray
    ): ByteArray {

        val plaintext =
            cipher.decryptWithAd(
                associatedData = handshakeHash,
                ciphertext = ciphertext
            )

        mixHash(
            ciphertext
        )

        return plaintext
    }

    fun split(): Pair<CipherState, CipherState> {

        val outputs =
            hkdf(
                ByteArray(0),
                2
            )

        val initiatorCipher =
            CipherState(crypto)

        initiatorCipher.initializeKey(
            outputs[0]
        )

        val responderCipher =
            CipherState(crypto)

        responderCipher.initializeKey(
            outputs[1]
        )

        return Pair(
            initiatorCipher,
            responderCipher
        )
    }
}

class NoiseTransport internal constructor(
    private val sending: CipherState,
    private val receiving: CipherState,
    val handshakeHash: ByteArray
) {

    fun encrypt(
        plaintext: ByteArray
    ): ByteArray {

        return sending.encryptWithAd(
            associatedData = ByteArray(0),
            plaintext = plaintext
        )
    }

    fun decrypt(
        ciphertext: ByteArray
    ): ByteArray {

        return receiving.decryptWithAd(
            associatedData = ByteArray(0),
            ciphertext = ciphertext
        )
    }
}

class HandshakeResult internal constructor(
    val transport: NoiseTransport,
    val handshakeHash: ByteArray
)

class HandshakeState private constructor(
    private val crypto: NoiseCrypto,
    private val pattern: NoisePattern,
    private val role: NoiseRole,
    private val localStatic: X25519KeyPair,
    private val remoteStaticPublic: ByteArray,
    private var psk: ByteArray?,
    private var localEphemeral: X25519KeyPair?
) {

    private val symmetric =
        SymmetricState(
            crypto
        )

    private var remoteEphemeralPublic:
            ByteArray? = null

    private var messageIndex =
        0

    private var poisoned =
        false

    var result:
            HandshakeResult? =
        null
        private set

    val isComplete: Boolean
        get() = result != null

    val handshakeHash: ByteArray
        get() = symmetric.handshakeHash

    init {
        require(
            localStatic.privateKey.size == DH_LEN
        ) {
            "Local static private key must be 32 bytes"
        }

        require(
            localStatic.publicKey.size == DH_LEN
        ) {
            "Local static public key must be 32 bytes"
        }

        require(
            remoteStaticPublic.size == DH_LEN
        ) {
            "Remote static public key must be 32 bytes"
        }

        require(
            psk == null ||
                    psk!!.size == KEY_LEN
        ) {
            "PSK must be 32 bytes"
        }

        symmetric.initialize(
            "Noise_${pattern.name}_25519_ChaChaPoly_SHA256"
        )
    }

    companion object {

        fun createKkPsk2(
            crypto: NoiseCrypto,
            role: NoiseRole,
            prologue: ByteArray,
            localStatic: X25519KeyPair,
            remoteStaticPublic: ByteArray,
            psk: ByteArray? = null,
            localEphemeral: X25519KeyPair? = null
        ): HandshakeState {

            val state =
                HandshakeState(
                    crypto = crypto,
                    pattern = NoisePattern.KKPSK2,
                    role = role,
                    localStatic = localStatic,
                    remoteStaticPublic =
                        remoteStaticPublic,
                    psk = psk,
                    localEphemeral =
                        localEphemeral
                )

            state.symmetric.mixHash(
                prologue
            )

            if (
                state.pattern
                    .initiatorPreSharesStatic
            ) {
                state.symmetric.mixHash(
                    if (
                        role ==
                        NoiseRole.INITIATOR
                    ) {
                        localStatic.publicKey
                    } else {
                        remoteStaticPublic
                    }
                )
            }

            if (
                state.pattern
                    .responderPreSharesStatic
            ) {
                state.symmetric.mixHash(
                    if (
                        role ==
                        NoiseRole.RESPONDER
                    ) {
                        localStatic.publicKey
                    } else {
                        remoteStaticPublic
                    }
                )
            }

            return state
        }
    }

    fun providePsk(
        newPsk: ByteArray
    ) {
        require(
            newPsk.size == KEY_LEN
        ) {
            "PSK must be 32 bytes"
        }

        check(!isComplete) {
            "Handshake already complete"
        }

        psk =
            newPsk
    }

    private val writesMessage: Boolean
        get() =
            if (
                role ==
                NoiseRole.INITIATOR
            ) {
                messageIndex % 2 == 0
            } else {
                messageIndex % 2 != 0
            }

    private fun localEphemeralPrivate():
            ByteArray {

        return localEphemeral?.privateKey
            ?: throw NoiseException(
                "Missing local ephemeral key"
            )
    }

    private fun localStaticPrivate():
            ByteArray {
        return localStatic.privateKey
    }

    private fun remoteEphemeral():
            ByteArray {

        return remoteEphemeralPublic
            ?: throw NoiseException(
                "Missing remote ephemeral key"
            )
    }

    private fun remoteStatic():
            ByteArray {
        return remoteStaticPublic
    }

    private fun processDh(
        token: NoiseToken
    ) {

        val sharedSecret =
            when (token) {

                NoiseToken.EE ->
                    crypto.x25519Dh(
                        localEphemeralPrivate(),
                        remoteEphemeral()
                    )

                NoiseToken.SS ->
                    crypto.x25519Dh(
                        localStaticPrivate(),
                        remoteStatic()
                    )

                NoiseToken.ES ->
                    if (
                        role ==
                        NoiseRole.INITIATOR
                    ) {
                        crypto.x25519Dh(
                            localEphemeralPrivate(),
                            remoteStatic()
                        )
                    } else {
                        crypto.x25519Dh(
                            localStaticPrivate(),
                            remoteEphemeral()
                        )
                    }

                NoiseToken.SE ->
                    if (
                        role ==
                        NoiseRole.INITIATOR
                    ) {
                        crypto.x25519Dh(
                            localStaticPrivate(),
                            remoteEphemeral()
                        )
                    } else {
                        crypto.x25519Dh(
                            localEphemeralPrivate(),
                            remoteStatic()
                        )
                    }

                else ->
                    throw NoiseException(
                        "Invalid DH token: $token"
                    )
            }

        symmetric.mixKey(
            sharedSecret
        )
    }

    fun writeMessage(
        payload: ByteArray
    ): ByteArray {

        check(!poisoned) {
            "Handshake state is poisoned"
        }

        check(!isComplete) {
            "Handshake already complete"
        }

        check(writesMessage) {
            "It is not this side's turn to write"
        }

        return try {

            val tokens =
                pattern.messages[
                    messageIndex
                ]

            var output =
                ByteArray(0)

            for (token in tokens) {

                when (token) {

                    NoiseToken.E -> {

                        val ephemeral =
                            localEphemeral
                                ?: generateEphemeral()
                                    .also {
                                        localEphemeral =
                                            it
                                    }

                        output +=
                            ephemeral.publicKey

                        symmetric.mixHash(
                            ephemeral.publicKey
                        )

                        if (
                            pattern.hasPsk
                        ) {
                            symmetric.mixKey(
                                ephemeral.publicKey
                            )
                        }
                    }

                    NoiseToken.PSK -> {

                        symmetric.mixKeyAndHash(
                            psk
                                ?: throw NoiseException(
                                    "PSK required"
                                )
                        )
                    }

                    NoiseToken.ES,
                    NoiseToken.SS,
                    NoiseToken.EE,
                    NoiseToken.SE -> {

                        processDh(
                            token
                        )
                    }
                }
            }

            output +=
                symmetric.encryptAndHash(
                    payload
                )

            advance()

            output

        } catch (
            exception: Exception
        ) {

            poisoned = true

            throw exception
        }
    }

    fun readMessage(
        message: ByteArray
    ): ByteArray {

        check(!poisoned) {
            "Handshake state is poisoned"
        }

        check(!isComplete) {
            "Handshake already complete"
        }

        check(!writesMessage) {
            "It is not this side's turn to read"
        }

        return try {

            val tokens =
                pattern.messages[
                    messageIndex
                ]

            var offset =
                0

            for (token in tokens) {

                when (token) {

                    NoiseToken.E -> {

                        if (
                            message.size -
                            offset <
                            DH_LEN
                        ) {
                            throw NoiseException(
                                "Handshake message too short"
                            )
                        }

                        val ephemeral =
                            message.copyOfRange(
                                offset,
                                offset + DH_LEN
                            )

                        offset +=
                            DH_LEN

                        remoteEphemeralPublic =
                            ephemeral

                        symmetric.mixHash(
                            ephemeral
                        )

                        if (
                            pattern.hasPsk
                        ) {
                            symmetric.mixKey(
                                ephemeral
                            )
                        }
                    }

                    NoiseToken.PSK -> {

                        symmetric.mixKeyAndHash(
                            psk
                                ?: throw NoiseException(
                                    "PSK required"
                                )
                        )
                    }

                    NoiseToken.ES,
                    NoiseToken.SS,
                    NoiseToken.EE,
                    NoiseToken.SE -> {

                        processDh(
                            token
                        )
                    }
                }
            }

            val ciphertext =
                message.copyOfRange(
                    offset,
                    message.size
                )

            if (
                symmetric.cipher.hasKey &&
                ciphertext.size < TAG_LEN
            ) {
                throw NoiseException(
                    "Handshake ciphertext too short"
                )
            }

            val payload =
                symmetric.decryptAndHash(
                    ciphertext
                )

            advance()

            payload

        } catch (
            exception: Exception
        ) {

            poisoned = true

            throw exception
        }
    }

    private fun generateEphemeral():
            X25519KeyPair {

        val privateKey =
            crypto.generateX25519PrivateKey()

        val publicKey =
            crypto.x25519PublicKey(
                privateKey
            )

        return X25519KeyPair(
            privateKey = privateKey,
            publicKey = publicKey
        )
    }

    private fun advance() {

        messageIndex++

        if (
            messageIndex ==
            pattern.messages.size
        ) {

            val (
                initiatorCipher,
                responderCipher
            ) =
                symmetric.split()

            val transport =
                if (
                    role ==
                    NoiseRole.INITIATOR
                ) {

                    NoiseTransport(
                        sending =
                            initiatorCipher,
                        receiving =
                            responderCipher,
                        handshakeHash =
                            symmetric.handshakeHash
                    )

                } else {

                    NoiseTransport(
                        sending =
                            responderCipher,
                        receiving =
                            initiatorCipher,
                        handshakeHash =
                            symmetric.handshakeHash
                    )
                }

            result =
                HandshakeResult(
                    transport =
                        transport,
                    handshakeHash =
                        symmetric.handshakeHash
                )
        }
    }
}