package com.chirihome.platform.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chirihome.platform.ui.screens.HomeScreen
import com.chirihome.platform.ui.screens.LoginScreen
import com.chirihome.platform.ui.screens.SplashScreen

@Composable
fun ChiriNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen()
        }

        composable(Routes.LOGIN) {
            LoginScreen()
        }

        composable(Routes.HOME) {
            HomeScreen()
        }
    }
}