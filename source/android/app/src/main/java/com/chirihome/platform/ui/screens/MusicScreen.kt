package com.chirihome.platform.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chirihome.platform.ChiriApplication
import com.chirihome.platform.ui.music.MusicViewModel

private const val DEFAULT_PLAYER_ID = "up2024ca64"

@Composable
fun MusicScreen(
    musicViewModel: MusicViewModel
) {
    val uiState by musicViewModel.uiState.collectAsStateWithLifecycle()

    var query by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Música"
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Buscar música")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Button(
            onClick = {
                musicViewModel.search(query)
            },
            enabled = query.isNotBlank() && !uiState.isSearching
        ) {
            Text("Buscar")
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (uiState.isSearching) {
            CircularProgressIndicator(
                modifier = Modifier.align(
                    Alignment.CenterHorizontally
                )
            )
        }

        uiState.error?.let { error ->
            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = error
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(uiState.searchResults) { item ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = item.title
                    )

                    item.artist?.let {
                        Text(text = it)
                    }

                    item.album?.let {
                        Text(text = it)
                    }

                    Button(
                        onClick = {
                            musicViewModel.play(
                                playerId = DEFAULT_PLAYER_ID,
                                uri = item.uri
                            )
                        }
                    ) {
                        Text("Reproducir")
                    }
                }
            }
        }

        uiState.nowPlaying?.let { nowPlaying ->

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = "Reproduciendo"
            )

            nowPlaying.track?.let { track ->

                Text(
                    text = track.title
                )

                track.artist?.let {
                    Text(text = it)
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = {
                        musicViewModel.previous(
                            DEFAULT_PLAYER_ID
                        )
                    }
                ) {
                    Text("Anterior")
                }

                Button(
                    onClick = {
                        musicViewModel.pause(
                            DEFAULT_PLAYER_ID
                        )
                    }
                ) {
                    Text("Pausa")
                }

                Button(
                    onClick = {
                        musicViewModel.next(
                            DEFAULT_PLAYER_ID
                        )
                    }
                ) {
                    Text("Siguiente")
                }
            }
        }
    }
}