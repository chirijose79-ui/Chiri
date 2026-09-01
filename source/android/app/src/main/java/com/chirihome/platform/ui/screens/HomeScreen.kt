package com.chirihome.platform.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chirihome.platform.R
import com.chirihome.platform.session.SessionManager

@Composable
fun HomeScreen(
    sessionManager: SessionManager
) {
    LaunchedEffect(Unit) {
        Log.e("ChiriHome", "HOME EJECUTADO")

        val refreshOk = sessionManager.refreshSession()

        Log.e(
            "ChiriHome",
            "Resultado refreshSession() = $refreshOk"
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.chiri_logo),
            contentDescription = "Chiri Platform",
            modifier = Modifier.size(220.dp)
        )

        Text(
            text = "Chiri Home"
        )
    }
}