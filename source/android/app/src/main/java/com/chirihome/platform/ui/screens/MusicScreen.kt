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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chirihome.platform.player.music.LocalMusicPlayer
import com.chirihome.platform.network.MusicPlayerItem
import com.chirihome.platform.ui.music.MusicViewModel
import android.util.Log

private const val LOCAL_PLAYER_ID = "local"

@Composable
fun MusicScreen(
    musicViewModel: MusicViewModel
) {
    val uiState by musicViewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val localMusicPlayer = remember {
        LocalMusicPlayer(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            localMusicPlayer.release()
        }
    }

    LaunchedEffect(uiState.nowPlaying?.track?.stream_url) {
        if (uiState.selectedPlayerId == LOCAL_PLAYER_ID) {
            uiState.nowPlaying?.track?.stream_url?.let { streamUrl ->
                localMusicPlayer.play(streamUrl)
            }
        }
    }

    var query by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {
        musicViewModel.loadPlayers()
    }

    val localPlayer = MusicPlayerItem(
        id = LOCAL_PLAYER_ID,
        name = "Este celular",
        available = true
    )

    val players = listOf(localPlayer) + uiState.players

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

        Text(
            text = "Reproducir en"
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        players.forEach { player ->

            Button(
                onClick = {
                    musicViewModel.loadPlayer(player.id)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = player.available
            ) {
                Text(
                    text = if (uiState.selectedPlayerId == player.id) {
                        "✓ ${player.name}"
                    } else {
                        player.name
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
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

        if (uiState.isLoading) {
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
                            Log.d("MusicScreen", "CLICK Reproducir - playerId=${uiState.selectedPlayerId}")
                            uiState.selectedPlayerId?.let { playerId ->
                                if (playerId == LOCAL_PLAYER_ID) {
                                    musicViewModel.playLocal(
                                        playerId = playerId,
                                        uri = item.uri
                                    )
                                } else {
                                    musicViewModel.play(
                                        playerId = playerId,
                                        uri = item.uri
                                    )
                                }
                            }
                        },
                        enabled = uiState.selectedPlayerId != null &&
                                !uiState.isExecutingAction
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

            uiState.selectedPlayerId?.let { playerId ->

                if (playerId != LOCAL_PLAYER_ID) {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        Button(
                            onClick = {
                                musicViewModel.previous(playerId)
                            }
                        ) {
                            Text("Anterior")
                        }

                        Button(
                            onClick = {
                                musicViewModel.pause(playerId)
                            }
                        ) {
                            Text("Pausa")
                        }

                        Button(
                            onClick = {
                                musicViewModel.next(playerId)
                            }
                        ) {
                            Text("Siguiente")
                        }
                    }
                }
            }
        }
    }
}