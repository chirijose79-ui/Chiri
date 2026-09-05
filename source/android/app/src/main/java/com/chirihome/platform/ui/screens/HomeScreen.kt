package com.chirihome.platform.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.chirihome.platform.ui.auth.HomeUiState
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
            homeUseCase = application.homeUseCase,
            logoutUseCase = application.logoutUseCase
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> {
            HomeLoading()
        }

        uiState.error != null -> {
            HomeError(
                message = uiState.error
                    ?: "No se pudo cargar la información."
            )
        }

        uiState.home != null -> {
            HomeContent(
                uiState = uiState,
                onLogout = {
                    viewModel.logout(
                        onSuccess = {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.HOME) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun HomeLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Cargando Chiri Home..."
        )
    }
}

@Composable
private fun HomeError(
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No se pudo cargar Home.",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = message
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onLogout: () -> Unit
) {
    val home = uiState.home ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Image(
                painter = painterResource(id = R.drawable.chiri_logo),
                contentDescription = "Chiri Platform",
                modifier = Modifier
                    .size(140.dp)
                    .fillMaxWidth()
            )
        }

        item {
            Text(
                text = "Hola, ${home.user.display_name}",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Estado del hogar",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = when (home.home.status) {
                            "operational" -> "Hogar operativo"
                            "attention" -> "Hogar requiere atención"
                            "offline" -> "Hogar fuera de línea"
                            else -> "Estado desconocido"
                        }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Acciones rápidas",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        home.quick_actions
                            .firstOrNull { it.id == "music" }
                            ?.let { action ->
                                Button(
                                    onClick = { },
                                    enabled = action.enabled,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Música")
                                }
                            }

                        home.quick_actions
                            .firstOrNull { it.id == "multimedia" }
                            ?.let { action ->
                                Button(
                                    onClick = { },
                                    enabled = action.enabled,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Multimedia")
                                }
                            }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Información básica",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Conectividad: ${
                            if (home.information.connectivity == "online") {
                                "Online"
                            } else {
                                "Offline"
                            }
                        }"
                    )

                    Text(
                        text = "Servidor: ${
                            if (home.information.server == "online") {
                                "Online"
                            } else {
                                "Offline"
                            }
                        }"
                    )
                }
            }
        }

        item {
            Button(
                onClick = onLogout,
                enabled = !uiState.isLoggingOut,
                modifier = Modifier.fillMaxWidth()
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
        }

        item {
            uiState.logoutError?.let { error ->
                Text(
                    text = error
                )
            }
        }
    }
}
