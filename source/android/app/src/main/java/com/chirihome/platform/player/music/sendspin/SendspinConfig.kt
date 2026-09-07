package com.chirihome.platform.player.music.sendspin

/**
 * Configuración del reproductor Sendspin de Chiri.
 *
 * Fase 1:
 * - Conexión directa por WebSocket a Music Assistant.
 * - Red LAN.
 * - Sin WebRTC.
 * - Sin cifrado Noise.
 * - Sin proxy HTTP de Chiri.
 */
data class SendspinConfig(
    val clientId: String,
    val deviceName: String,
    val serverHost: String = "192.168.1.88",
    val serverPort: Int = 8927,
    val serverPath: String = "/sendspin",
    val useTls: Boolean = false,
    val enabled: Boolean = true,
    val codecPreference: String = "opus",
    val bufferCapacityBytes: Int = 512 * 1024,
    val authToken: String? = null
) {
    /**
     * URL WebSocket utilizada por el cliente Sendspin.
     *
     * Fase 1:
     * ws://192.168.1.88:8927/sendspin
     */
    val webSocketUrl: String
        get() {
            val scheme = if (useTls) "wss" else "ws"
            return "$scheme://$serverHost:$serverPort$serverPath"
        }

    /**
     * Indica si la configuración mínima necesaria está presente.
     */
    fun isValid(): Boolean {
        return enabled &&
                clientId.isNotBlank() &&
                deviceName.isNotBlank() &&
                serverHost.isNotBlank() &&
                serverPort in 1..65535 &&
                serverPath.startsWith("/")
    }
}
