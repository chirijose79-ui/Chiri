package com.chirihome.platform.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.chirihome.platform.ui.auth.HomeViewModel
import com.chirihome.platform.ui.auth.HomeViewModelFactory
import com.chirihome.platform.ui.navigation.Routes

@Composable
fun HomeScreen(
    navController: NavController
) {
    val application =
        LocalContext.current.applicationContext as ChiriApplication

    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            logoutUseCase = application.logoutUseCase
        )
    )

    val uiState by viewModel.uiState.collectAsState()

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

        Button(
            onClick = {
                viewModel.logout(
                    onSuccess = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) {
                                inclusive = true
                            }
                        }
                    }
                )
            },
            enabled = !uiState.isLoggingOut
        ) {
            if (uiState.isLoggingOut) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Cerrar sesión")
            }
        }

        uiState.logoutError?.let { error ->
            Text(
                text = error
            )
        }
    }
}
