package com.chirihome.platform.player.music.sendspin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SendspinForegroundService : Service() {

    private lateinit var sendspinManager: SendspinManager

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        sendspinManager = SendspinManager(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val notification = createNotification()

        startForeground(
            NOTIFICATION_ID,
            notification
        )

        sendspinManager.start()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        sendspinManager.close()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Chiri Music",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Sendspin music playback"
        }

        val notificationManager =
            getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("Chiri")
            .setContentText("Sendspin activo")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID =
            "chiri_sendspin"

        private const val NOTIFICATION_ID =
            1001
    }
}