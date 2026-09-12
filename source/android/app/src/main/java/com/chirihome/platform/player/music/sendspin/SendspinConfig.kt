package com.chirihome.platform.player.music.sendspin

/**
 * Configuración del reproductor Sendspin de Chiri.
 *
 * Fase 1:
 * - Conexión WebSocket pública mediante Cloudflare Tunnel.
 * - Transporte WSS.
 * - Sin WebRTC.
 * - Sin cifrado Noise a nivel de transporte público.
 * - Sin proxy HTTP de Chiri.
 */
data class SendspinConfig(
    val clientId: String,
    val deviceName: String,
    val serverHost: String = "sendspin.chirihome.com",
    val serverPort: Int = 443,
    val serverPath: String = "/sendspin",
    val useTls: Boolean = true,
    val enabled: Boolean = true,
    val codecPreference: String = "opus",
    val bufferCapacityBytes: Int = 512 * 1024,
    val authToken: String? = null
) {
    /**
     * URL WebSocket utilizada por el cliente Sendspin.
     *
     * Producción:
     * wss://sendspin.chirihome.com/sendspin
     */
    val webSocketUrl: String
        get() {
            val scheme = if (useTls) "wss" else "ws"
            val defaultPort = if (useTls) 443 else 80
            val portSuffix = if (serverPort == defaultPort) "" else ":$serverPort"
            return "$scheme://$serverHost$portSuffix$serverPath"
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
