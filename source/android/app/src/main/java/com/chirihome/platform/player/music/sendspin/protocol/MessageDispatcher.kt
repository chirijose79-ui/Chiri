package com.chirihome.platform.player.music.sendspin.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class MessageDispatcher {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun dispatch(message: String): DispatchResult {
        if (message.isBlank()) {
            return DispatchResult.Invalid(
                reason = "Sendspin message is empty"
            )
        }

        val payload = try {
            json.parseToJsonElement(message)
        } catch (exception: Throwable) {
            return DispatchResult.Invalid(
                reason = "Invalid Sendspin JSON message",
                cause = exception
            )
        }

        if (payload !is JsonObject) {
            return DispatchResult.Invalid(
                reason = "Sendspin message must be a JSON object"
            )
        }

        val typeElement = payload["type"]

        if (typeElement == null) {
            return DispatchResult.Invalid(
                reason = "Sendspin message does not contain a type"
            )
        }

        val type = try {
            typeElement.jsonPrimitive.content
        } catch (exception: Throwable) {
            return DispatchResult.Invalid(
                reason = "Sendspin message type is not a JSON primitive",
                cause = exception
            )
        }

        if (type.isBlank()) {
            return DispatchResult.Invalid(
                reason = "Sendspin message type is empty"
            )
        }

        return when (classify(type)) {
            MessageCategory.AUTHENTICATION ->
                DispatchResult.Authentication(
                    type = type,
                    payload = payload
                )

            MessageCategory.STREAM ->
                DispatchResult.Stream(
                    type = type,
                    payload = payload
                )

            MessageCategory.SYNCHRONIZATION ->
                DispatchResult.Synchronization(
                    type = type,
                    payload = payload
                )

            MessageCategory.PLAYER ->
                DispatchResult.Player(
                    type = type,
                    payload = payload
                )

            MessageCategory.METADATA ->
                DispatchResult.Metadata(
                    type = type,
                    payload = payload
                )

            MessageCategory.UNKNOWN ->
                DispatchResult.Unknown(
                    type = type,
                    payload = payload
                )
        }
    }

    private fun classify(type: String): MessageCategory {
        return when {
            type.startsWith("auth_") ->
                MessageCategory.AUTHENTICATION

            type.startsWith("stream/") ->
                MessageCategory.STREAM

            type.startsWith("sync/") ||
                    type == "client/time" ||
                    type == "server/time" ->
                MessageCategory.SYNCHRONIZATION

            type.startsWith("player/") ->
                MessageCategory.PLAYER

            type.startsWith("metadata/") ->
                MessageCategory.METADATA

            else ->
                MessageCategory.UNKNOWN
        }
    }

    enum class MessageCategory {
        AUTHENTICATION,
        STREAM,
        SYNCHRONIZATION,
        PLAYER,
        METADATA,
        UNKNOWN
    }

    sealed interface DispatchResult {

        data class Authentication(
            val type: String,
            val payload: JsonObject
        ) : DispatchResult

        data class Stream(
            val type: String,
            val payload: JsonObject
        ) : DispatchResult

        data class Synchronization(
            val type: String,
            val payload: JsonObject
        ) : DispatchResult

        data class Player(
            val type: String,
            val payload: JsonObject
        ) : DispatchResult

        data class Metadata(
            val type: String,
            val payload: JsonObject
        ) : DispatchResult

        data class Unknown(
            val type: String,
            val payload: JsonObject
        ) : DispatchResult

        data class Invalid(
            val reason: String,
            val cause: Throwable? = null
        ) : DispatchResult
    }
}
