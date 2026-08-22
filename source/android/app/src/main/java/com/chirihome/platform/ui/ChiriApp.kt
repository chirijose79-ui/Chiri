package com.chirihome.platform.ui

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.chirihome.platform.ChiriApplication
import com.chirihome.platform.ui.navigation.ChiriNavGraph
import com.chirihome.platform.ui.theme.ChiriTheme

@Composable
fun ChiriApp() {
    ChiriTheme {
        val application =
            LocalContext.current.applicationContext as ChiriApplication

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) {
            ChiriNavGraph(
                sessionManager = application.sessionManager
            )
        }
    }
}