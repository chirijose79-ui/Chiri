package com.chirihome.platform.player.music.sendspin.protocol

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendspinHandshakeModelsTest {

    private val json = Json {
        encodeDefaults = true
    }

    @Test
    fun clientInit_serializesCorrectly() {
        val message = SendspinClientInitMessage(
            payload = SendspinClientInitPayload(
                client_id = "test-client-id",
                version = 1,
                suite = "25519_ChaChaPoly_SHA256"
            )
        )

        val encoded = json.encodeToString(message)

        assertEquals(
            """{"payload":{"client_id":"test-client-id","version":1,"suite":"25519_ChaChaPoly_SHA256"},"type":"client/init"}""",
            encoded
        )
    }

    @Test
    fun serverInit_deserializesCorrectly() {
        val raw =
            """{"payload":{"server_id":"test-server-id","version":1},"type":"server/init"}"""

        val message = json.decodeFromString<SendspinServerInitMessage>(raw)

        assertEquals("test-server-id", message.payload.server_id)
        assertEquals(1, message.payload.version)
        assertEquals("server/init", message.type)
    }

    @Test
    fun noiseHandshake_serializesCorrectly() {
        val message = SendspinNoiseHandshakeMessage(
            payload = SendspinNoiseHandshakePayload(
                data = "AQIDBA=="
            )
        )

        val encoded = json.encodeToString(message)

        assertEquals(
            """{"payload":{"data":"AQIDBA=="},"type":"noise/handshake"}""",
            encoded
        )
    }

    @Test
    fun noiseMsg1_serializesCorrectly() {
        val message = SendspinNoiseMsg1Payload(
            psk_id = "test-psk-id"
        )

        val encoded = json.encodeToString(message)

        assertEquals(
            """{"psk_id":"test-psk-id"}""",
            encoded
        )
    }

    @Test
    fun noiseMsg2_serializesAsEmptyObject() {
        val message = SendspinNoiseMsg2Payload()

        val encoded = json.encodeToString(message)

        assertEquals("{}", encoded)
    }

    @Test
    fun clientInit_roundTrips() {
        val original = SendspinClientInitMessage(
            payload = SendspinClientInitPayload(
                client_id = "client-123",
                version = 1,
                suite = "25519_ChaChaPoly_SHA256"
            )
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SendspinClientInitMessage>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun serverInit_roundTrips() {
        val original = SendspinServerInitMessage(
            payload = SendspinServerInitPayload(
                server_id = "server-123",
                version = 1
            )
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<SendspinServerInitMessage>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun handshakeTypeValues_areCorrect() {
        assertTrue(
            SendspinClientInitMessage(
                payload = SendspinClientInitPayload(
                    client_id = "client",
                    version = 1,
                    suite = "25519_ChaChaPoly_SHA256"
                )
            ).type == "client/init"
        )

        assertTrue(
            SendspinServerInitMessage(
                payload = SendspinServerInitPayload(
                    server_id = "server",
                    version = 1
                )
            ).type == "server/init"
        )

        assertTrue(
            SendspinNoiseHandshakeMessage(
                payload = SendspinNoiseHandshakePayload(
                    data = "data"
                )
            ).type == "noise/handshake"
        )
    }
}
