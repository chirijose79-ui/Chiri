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
        val sessionIsValid = sessionManager.isSessionValid()

        val destination = if (sessionIsValid) {
            Routes.HOME
        } else {
            Routes.LOGIN
        }

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