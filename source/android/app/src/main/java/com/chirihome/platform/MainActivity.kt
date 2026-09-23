package com.chirihome.platform

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.chirihome.platform.player.music.sendspin.SendspinForegroundService
import com.chirihome.platform.ui.ChiriApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(
            this,
            SendspinForegroundService::class.java
        )

        ContextCompat.startForegroundService(
            this,
            serviceIntent
        )

        enableEdgeToEdge()

        setContent {
            ChiriApp()
        }
    }
}