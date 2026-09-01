package com.chirihome.platform.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.chirihome.platform.ChiriApplication
import com.chirihome.platform.R
import com.chirihome.platform.ui.auth.SplashUiState
import com.chirihome.platform.ui.auth.SplashViewModel
import com.chirihome.platform.ui.auth.SplashViewModelFactory
import com.chirihome.platform.ui.navigation.Routes

@Composable
fun SplashScreen(
    navController: NavController
) {
    val application =
        LocalContext.current.applicationContext as ChiriApplication

    val viewModel: SplashViewModel = viewModel(
        factory = SplashViewModelFactory(
            validateSessionUseCase = application.validateSessionUseCase
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            SplashUiState.Loading -> Unit

            SplashUiState.Authenticated -> {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SPLASH) {
                        inclusive = true
                    }
                }
            }

            SplashUiState.Unauthenticated -> {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.SPLASH) {
                        inclusive = true
                    }
                }
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
