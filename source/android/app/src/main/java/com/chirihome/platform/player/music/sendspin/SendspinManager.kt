package com.chirihome.platform.player.music.sendspin

import android.content.Context
import com.chirihome.platform.player.music.sendspin.audio.AudioPipeline
import com.chirihome.platform.player.music.sendspin.audio.AudioStreamManager
import com.chirihome.platform.player.music.sendspin.audio.MediaPlayerControllerAndroid
import com.chirihome.platform.player.music.sendspin.audio.OpusDecoderAndroid
import com.chirihome.platform.player.music.sendspin.crypto.JdkNoiseCrypto
import com.chirihome.platform.player.music.sendspin.crypto.SendspinIdentityProvider
import com.chirihome.platform.player.music.sendspin.protocol.SecureSendspinPskResolver
import com.chirihome.platform.player.music.sendspin.protocol.SendspinNoiseHandshake
import com.chirihome.platform.player.music.sendspin.session.LegacySession
import com.chirihome.platform.player.music.sendspin.transport.WebSocketSendspinTransport
import com.chirihome.platform.storage.SecureSendspinCredentialStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class SendspinManager(
    context: Context
) {
    private val applicationContext = context.applicationContext

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )

    private val _state = MutableStateFlow<ConnectionState>(
        ConnectionState.Stopped
    )

    val state: StateFlow<ConnectionState> =
        _state.asStateFlow()

    private var lifecycleJob: Job? = null
    private var connectionJob: Job? = null
    private var client: SendspinClient? = null
    private var audioSink: AudioStreamManager? = null

    fun start() {
        if (lifecycleJob?.isActive == true) {
            return
        }

        lifecycleJob = scope.launch {
            initializeAndConnect()
        }
    }

    fun stop() {
        lifecycleJob?.cancel()
        lifecycleJob = null

        scope.launch {
            disconnectAndRelease()
        }
    }

    private suspend fun initializeAndConnect() {
        _state.value = ConnectionState.Starting

        android.util.Log.d(TAG, "[1] initializeAndConnect START")

        try {
            val storage = SecureSendspinCredentialStorage(applicationContext)
            android.util.Log.d(TAG, "[2] credential storage ready")

            val crypto = JdkNoiseCrypto()
            android.util.Log.d(TAG, "[3] crypto ready")

            val identityProvider =
                SendspinIdentityProvider(
                    storage = storage,
                    crypto = crypto
                )

            val identity = identityProvider.getOrCreate()
            android.util.Log.d(
                TAG,
                "[4] identity ready clientId=${identity.clientId}"
            )

            val config =
                SendspinConfig(
                    clientId = identity.clientId,
                    deviceName = "Chiri Android"
                )

            require(config.isValid()) {
                "Invalid Sendspin configuration"
            }

            android.util.Log.d(
                TAG,
                "[5] config ready url=${config.webSocketUrl}"
            )

            val clockSynchronizer = ClockSynchronizer()

            val pskResolver =
                SecureSendspinPskResolver(
                    storage = storage,
                    crypto = crypto
                )

            val handshake =
                SendspinNoiseHandshake(
                    identityProvider = identityProvider,
                    crypto = crypto,
                    pskResolver = pskResolver
                )

            android.util.Log.d(TAG, "[6] handshake ready")

            val transport =
                WebSocketSendspinTransport(
                    config = config
                )

            android.util.Log.d(TAG, "[7] transport ready")

            val audioDecoder = OpusDecoderAndroid()
            val mediaPlayer = MediaPlayerControllerAndroid()

            val audioPipeline =
                AudioPipeline(
                    decoder = audioDecoder,
                    player = mediaPlayer
                )

            val streamManager =
                AudioStreamManager(
                    audioPipeline = audioPipeline,
                    clockSynchronizer = clockSynchronizer
                )

            android.util.Log.d(TAG, "[8] audio ready")

            val messageSender =
                DelegatingSendspinMessageSender()

            val session = LegacySession(
                    config = config,
                    capabilities = SendspinCapabilities(),
                    transport = transport,
                    messageSender = messageSender,
                    clockSynchronizer = clockSynchronizer,
                    scope = scope,
                    pairingState = handshake
                )

            android.util.Log.d(TAG, "[9] session ready")

            val sendspinClient =
                SendspinClient(
                    transport = transport,
                    handshake = handshake,
                    session = session,
                    audioSink = streamManager,
                    scope = scope,
                    clockSynchronizer = clockSynchronizer
                )

            messageSender.setDelegate(sendspinClient)

            client = sendspinClient
            audioSink = streamManager

            android.util.Log.d(TAG, "[10] client ready")

            _state.value = ConnectionState.Connecting

            connectionJob?.cancel()

            connectionJob = scope.launch {
                try {
                    android.util.Log.d(TAG, "[11] SendspinClient.connect START")

                    sendspinClient.connect()

                    android.util.Log.d(TAG, "[12] SendspinClient.connect RETURNED")
                } catch (exception: CancellationException) {
                    android.util.Log.d(
                        TAG,
                        "[13] connect CANCELLED"
                    )
                    throw exception
                } catch (exception: Exception) {
                    android.util.Log.e(
                        TAG,
                        "[13] connect FAILED",
                        exception
                    )

                    _state.value =
                        ConnectionState.Error(exception)
                }
            }

            android.util.Log.d(TAG, "[14] connectionJob launched")

            scope.launch {
                sendspinClient.connectionState.collect { clientState ->
                    android.util.Log.d(
                        TAG,
                        "[STATE] SendspinClient=$clientState"
                    )

                    _state.value =
                        when (clientState) {
                            SendspinClient.ConnectionState.Disconnected ->
                                ConnectionState.Disconnected

                            SendspinClient.ConnectionState.Connecting ->
                                ConnectionState.Connecting

                            SendspinClient.ConnectionState.Connected ->
                                ConnectionState.Connected
                        }
                }
            }

            android.util.Log.d(TAG, "[15] initializeAndConnect COMPLETE")
        } catch (exception: CancellationException) {
            android.util.Log.d(
                TAG,
                "[CANCELLED] initializeAndConnect"
            )

            _state.value = ConnectionState.Stopped
            throw exception
        } catch (exception: Exception) {
            android.util.Log.e(
                TAG,
                "[FATAL] initializeAndConnect FAILED",
                exception
            )

            _state.value = ConnectionState.Error(exception)
            disconnectAndRelease()
        }
    }

    private suspend fun disconnectAndRelease() {
        connectionJob?.cancel()
        connectionJob = null

        client?.let { sendspinClient ->
            runCatching {
                sendspinClient.disconnect()
            }
        }

        client = null

        audioSink?.let { streamManager ->
            runCatching {
                streamManager.release()
            }
        }

        audioSink = null

        _state.value = ConnectionState.Stopped
    }

    fun close() {
        lifecycleJob?.cancel()
        lifecycleJob = null

        scope.launch {
            disconnectAndRelease()
            scope.cancel()
        }
    }

    sealed interface ConnectionState {
        data object Stopped : ConnectionState

        data object Starting : ConnectionState

        data object Connecting : ConnectionState

        data object Connected : ConnectionState

        data object Disconnected : ConnectionState

        data class Error(
            val cause: Throwable
        ) : ConnectionState
    }

    companion object {
        private const val TAG = "SendspinManager"
    }
}
