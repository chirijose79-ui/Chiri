package com.chirihome.platform.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chirihome.platform.ChiriApplication
import com.chirihome.platform.ui.screens.HomeScreen
import com.chirihome.platform.ui.screens.LoginScreen
import com.chirihome.platform.ui.screens.MusicScreen
import com.chirihome.platform.ui.screens.MultimediaScreen
import com.chirihome.platform.ui.screens.PhotosScreen
import com.chirihome.platform.ui.screens.SplashScreen
import com.chirihome.platform.ui.screens.VideosScreen
import com.chirihome.platform.ui.music.MusicViewModel

@Composable
fun ChiriNavGraph() {
    val navController = rememberNavController()

    val application = LocalContext.current.applicationContext as ChiriApplication

    val musicViewModel = MusicViewModel(
        musicUseCase = application.musicUseCase
    )

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                navController = navController
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                navController = navController
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                navController = navController
            )
        }

        composable(Routes.MUSIC) {
            MusicScreen(
                musicViewModel = musicViewModel
            )
        }

        composable(Routes.MULTIMEDIA) {
            MultimediaScreen(
                onMusicClick = {
                    navController.navigate(Routes.MUSIC)
                },
                onVideosClick = {
                    navController.navigate(Routes.VIDEOS)
                },
                onPhotosClick = {
                    navController.navigate(Routes.PHOTOS)
                }
            )
        }

        composable(Routes.VIDEOS) {
            VideosScreen()
        }

        composable(Routes.PHOTOS) {
            PhotosScreen()
        }
    }
}