package com.chirihome.platform.player.music.sendspin.protocol

import com.chirihome.platform.player.music.sendspin.crypto.HandshakeState
import com.chirihome.platform.player.music.sendspin.crypto.NoiseCrypto
import com.chirihome.platform.player.music.sendspin.crypto.NoiseRole
import com.chirihome.platform.player.music.sendspin.crypto.NoiseTransport
import com.chirihome.platform.player.music.sendspin.crypto.SendspinBase64
import com.chirihome.platform.player.music.sendspin.crypto.SendspinIdentity
import com.chirihome.platform.player.music.sendspin.crypto.SendspinIdentityProvider
import com.chirihome.platform.player.music.sendspin.crypto.X25519KeyPair
import kotlinx.serialization.json.Json

class SendspinNoiseHandshake(
    private val identityProvider: SendspinIdentityProvider,
    private val crypto: NoiseCrypto,
    private val pskResolver: SendspinPskResolver
) : SendspinHandshake {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    private var identity: SendspinIdentity? = null
    private var handshakeState: HandshakeState? = null

    private var clientInitRaw: String? = null
    private var serverInitRaw: String? = null

    private var serverStaticPublicKey: ByteArray? = null

    private var resolvedPsk: ByteArray? = null
    private var resolvedPskId: String? = null

    override suspend fun createClientInit(): String {
        val currentIdentity = identityProvider.getOrCreate()
        identity = currentIdentity

        val message = SendspinClientInitMessage(
            payload = SendspinClientInitPayload(
                client_id = currentIdentity.clientId,
                version = PROTOCOL_VERSION,
                suite = NOISE_SUITE
            )
        )

        return json.encodeToString(message).also {
            clientInitRaw = it
        }
    }

    override fun receiveServerInit(rawMessage: String) {
        require(rawMessage.isNotBlank()) {
            "Empty server/init message"
        }

        val message =
            json.decodeFromString<SendspinServerInitMessage>(rawMessage)

        require(message.type == SERVER_INIT_TYPE) {
            "Unexpected server message type: ${message.type}"
        }

        require(message.payload.version == PROTOCOL_VERSION) {
            "Unsupported Sendspin protocol version: ${message.payload.version}"
        }

        val serverId = message.payload.server_id

        require(serverId.isNotBlank()) {
            "Empty server_id in server/init"
        }

        val decodedServerId =
            SendspinBase64.decodeUrlSafe(serverId)

        require(decodedServerId.size == X25519_KEY_SIZE) {
            "server_id must decode to 32 bytes"
        }

        val clientInit =
            clientInitRaw
                ?: error("client/init must be created before server/init")

        val currentIdentity =
            identity
                ?: error("Sendspin identity is not initialized")

        serverInitRaw = rawMessage
        serverStaticPublicKey = decodedServerId.copyOf()

        val prologue =
            clientInit.toByteArray(Charsets.UTF_8) +
                    rawMessage.toByteArray(Charsets.UTF_8)

        val localStatic = X25519KeyPair(
            privateKey = currentIdentity.staticPrivateKey,
            publicKey = currentIdentity.staticPublicKey
        )

        handshakeState = HandshakeState.createKkPsk2(
            crypto = crypto,
            role = NoiseRole.RESPONDER,
            prologue = prologue,
            localStatic = localStatic,
            remoteStaticPublic = decodedServerId,
            psk = null
        )

        resolvedPsk = null
        resolvedPskId = null
    }

    override suspend fun receiveNoiseMessage1(
        rawMessage: String
    ): String {
        readNoiseMessage1(rawMessage)
        return createNoiseMessage2()
    }

    suspend fun readNoiseMessage1(
        rawMessage: String
    ): SendspinNoiseMsg1Payload {
        require(rawMessage.isNotBlank()) {
            "Empty noise/handshake message"
        }

        val message =
            json.decodeFromString<SendspinNoiseHandshakeMessage>(rawMessage)

        require(message.type == NOISE_HANDSHAKE_TYPE) {
            "Unexpected Noise message type: ${message.type}"
        }

        val encodedData = message.payload.data

        require(encodedData.isNotBlank()) {
            "Empty Noise handshake data"
        }

        val handshakeMessage =
            SendspinBase64.decodeUrlSafe(encodedData)

        val state =
            handshakeState
                ?: error("Handshake state has not been initialized")

        require(!state.isComplete) {
            "Noise handshake is already complete"
        }

        val decryptedPayload =
            state.readMessage(handshakeMessage)

        val payloadJson =
            decryptedPayload.toString(Charsets.UTF_8)

        val pskPayload =
            json.decodeFromString<SendspinNoiseMsg1Payload>(payloadJson)

        require(pskPayload.psk_id.isNotBlank()) {
            "Empty psk_id in Noise message 1"
        }

        val currentServerStaticPublicKey =
            serverStaticPublicKey
                ?: error("Server static public key has not been initialized")

        val currentServerId =
            SendspinBase64.encodeUrlSafe(
                currentServerStaticPublicKey
            )

        val psk =
            pskResolver.resolve(
                pskId = pskPayload.psk_id,
                serverId = currentServerId
            )
                ?: error(
                    "Unable to resolve Sendspin PSK: " +
                            "id=${pskPayload.psk_id}"
                )

        require(psk.size == PSK_SIZE) {
            "Resolved Sendspin PSK must be 32 bytes"
        }

        state.providePsk(psk)

        resolvedPsk = psk.copyOf()
        resolvedPskId = pskPayload.psk_id

        return pskPayload
    }

    fun createNoiseMessage2(): String {
        val state =
            handshakeState
                ?: error("Handshake state has not been initialized")

        require(resolvedPsk != null) {
            "PSK must be resolved before creating Noise message 2"
        }

        require(!state.isComplete) {
            "Noise handshake is already complete"
        }

        val payload =
            SendspinNoiseMsg2Payload()

        val payloadJson =
            json.encodeToString(payload)

        require(payloadJson == "{}") {
            "Sendspin Noise message 2 payload must be {}"
        }

        val handshakeMessage =
            state.writeMessage(
                payloadJson.toByteArray(Charsets.UTF_8)
            )

        val message =
            SendspinNoiseHandshakeMessage(
                payload = SendspinNoiseHandshakePayload(
                    data = SendspinBase64.encodeUrlSafe(handshakeMessage)
                )
            )

        return json.encodeToString(message)
    }

    val isComplete: Boolean
        get() = handshakeState?.isComplete == true

    val handshakeHash: ByteArray?
        get() =
            handshakeState
                ?.takeIf { it.isComplete }
                ?.handshakeHash

    override val noiseTransport: NoiseTransport?
        get() = handshakeState?.result?.transport

    fun getClientInitRaw(): String? = clientInitRaw

    fun getServerInitRaw(): String? = serverInitRaw

    fun getServerStaticPublicKey(): ByteArray? =
        serverStaticPublicKey?.copyOf()

    val pskId: String?
        get() = resolvedPskId

    companion object {
        private const val PROTOCOL_VERSION = 1
        private const val NOISE_SUITE = "25519_ChaChaPoly_SHA256"

        private const val SERVER_INIT_TYPE = "server/init"
        private const val NOISE_HANDSHAKE_TYPE = "noise/handshake"

        private const val X25519_KEY_SIZE = 32
        private const val PSK_SIZE = 32
    }
}
