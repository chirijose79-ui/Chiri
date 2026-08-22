package com.chirihome.platform.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chirihome.platform.ui.navigation.ChiriNavGraph
import com.chirihome.platform.ui.theme.ChiriTheme

@Composable
fun ChiriApp() {
    ChiriTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) {
            ChiriNavGraph()
        }
    }
}