package com.chirihome.platform.player.music.sendspin

class DelegatingSendspinMessageSender : SendspinMessageSender {

    private var delegate: SendspinMessageSender? = null

    fun setDelegate(delegate: SendspinMessageSender) {
        check(this.delegate == null) {
            "Sendspin message sender delegate is already initialized"
        }

        this.delegate = delegate
    }

    override suspend fun sendEncrypted(message: String) {
        val currentDelegate = delegate
            ?: error("Sendspin message sender delegate is not initialized")

        currentDelegate.sendEncrypted(message)
    }
}
