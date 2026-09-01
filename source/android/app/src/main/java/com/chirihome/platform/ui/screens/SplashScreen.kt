package com.chirihome.platform.ui.screens

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
import android.util.Log
import androidx.navigation.NavController
import com.chirihome.platform.R
import com.chirihome.platform.session.SessionManager
import com.chirihome.platform.ui.navigation.Routes

@Composable
fun SplashScreen(
    navController: NavController,
    sessionManager: SessionManager
) {
    LaunchedEffect(Unit) {
        Log.e("ChiriSplash", "SPLASH EJECUTADO")

        val sessionIsValid = try {
            Log.e("ChiriSplash", "Consultando /auth/me")

            val currentUser = sessionManager.getCurrentUser()

            Log.e(
                "ChiriSplash",
                "Respuesta /auth/me: $currentUser"
            )

            currentUser != null
        } catch (exception: Exception) {
            Log.e(
                "ChiriSplash",
                "Error validando sesión",
                exception
            )
            false
        }

        Log.e(
            "ChiriSplash",
            "sessionIsValid = $sessionIsValid"
        )

        val destination = if (sessionIsValid) {
            Routes.HOME
        } else {
            sessionManager.clearSession()
            Routes.LOGIN
        }

        Log.e(
            "ChiriSplash",
            "Destino = $destination"
        )

        navController.navigate(destination) {
            popUpTo(Routes.SPLASH) {
                inclusive = true
            }
        }
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
            text = "Inicializando..."
        )
    }
}