package com.chirihome.platform.player.music.sendspin.transport

import com.chirihome.platform.player.music.sendspin.SendspinConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Transporte Sendspin basado en WebSocket.
 *
 * Fase 1:
 * ws://<Music Assistant>:8927/sendspin
 */
class WebSocketSendspinTransport(
    private val config: SendspinConfig,
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(WebSockets)
    }
) : SendspinTransport {

    private val _events = MutableSharedFlow<InboundTransportEvent>(
        extraBufferCapacity = 32
    )

    override val events: Flow<InboundTransportEvent> =
        _events.asSharedFlow()

    private val connected = AtomicBoolean(false)

    override val isConnected: Boolean
        get() = connected.get()

    private val connectionMutex = Mutex()

    private var session: DefaultClientWebSocketSession? = null

    override suspend fun connect() {
        connectionMutex.withLock {
            if (connected.get()) {
                return
            }

            try {
                httpClient.webSocket(
                    urlString = config.webSocketUrl
                ) {
                    session = this
                    connected.set(true)

                    _events.emit(
                        InboundTransportEvent.Connected
                    )

                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val message = frame.readText()

                                    println("[SendspinTransport] [RECV] WebSocket text: $message")

                                    _events.emit(
                                        InboundTransportEvent.TextMessage(
                                            message
                                        )
                                    )
                                }

                                is Frame.Binary -> {
                                    val data = frame.readBytes()

                                    _events.emit(
                                        InboundTransportEvent.BinaryMessage(
                                            data
                                        )
                                    )
                                }

                                is Frame.Close -> {
                                    println(
                                        "[SendspinTransport] [RECV] WebSocket CLOSE frame: $frame"
                                    )
                                    break
                                }

                                is Frame.Ping -> Unit
                                is Frame.Pong -> Unit
                            }
                        }
                    } finally {
                        session = null

                        if (connected.compareAndSet(true, false)) {
                            _events.emit(
                                InboundTransportEvent.Disconnected()
                            )
                        }
                    }
                }
            } catch (throwable: Throwable) {
                session = null
                connected.set(false)
                _events.emit(
                    InboundTransportEvent.Error(
                        throwable
                    )
                )
            }
        }
    }

    override suspend fun send(message: String) {
        val currentSession = session
            ?: error("Sendspin WebSocket is not connected")

        println("[SendspinTransport] [SEND] WebSocket text: $message")

        currentSession.send(
            Frame.Text(message)
        )
    }

    override suspend fun sendBinary(data: ByteArray) {
        val currentSession = session
            ?: error("Sendspin WebSocket is not connected")

        currentSession.send(
            Frame.Binary(
                fin = true,
                data = data
            )
        )
    }

    override suspend fun disconnect() {
        connectionMutex.withLock {

            val currentSession = session

            session = null
            connected.set(false)

            if (currentSession != null) {
                currentSession.close()
            }
        }
    }
}
