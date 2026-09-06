package com.chirihome.platform.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chirihome.platform.domain.music.MusicUseCase
import com.chirihome.platform.network.MusicNowPlayingResponse
import com.chirihome.platform.network.MusicQueueResponse
import com.chirihome.platform.network.MusicSearchItem
import com.chirihome.platform.network.MusicPlayRequest
import com.chirihome.platform.network.MusicPlayerRequest
import com.chirihome.platform.network.MusicPlayerItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MusicUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isExecutingAction: Boolean = false,
    val searchResults: List<MusicSearchItem> = emptyList(),
    val players: List<MusicPlayerItem> = emptyList(),
    val selectedPlayerId: String? = null,
    val nowPlaying: MusicNowPlayingResponse? = null,
    val queue: MusicQueueResponse? = null,
    val error: String? = null
)

class MusicViewModel(
    private val musicUseCase: MusicUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                error = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSearching = true,
                error = null
            )

            try {
                val response = musicUseCase.search(query.trim())

                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchResults = response.items,
                    error = null
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = "No se pudo realizar la búsqueda."
                )
            }
        }
    }

    fun loadPlayers() {
        viewModelScope.launch {
            try {
                val response = musicUseCase.getPlayers()

                _uiState.value = _uiState.value.copy(
                    players = response.items,
                    error = null
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "No se pudieron cargar los reproductores."
                )
            }
        }
    }

    fun loadPlayer(playerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedPlayerId = playerId,
                error = null
            )

            try {
                val nowPlaying = musicUseCase.getNowPlaying(playerId)
                val queue = musicUseCase.getQueue(playerId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    nowPlaying = nowPlaying,
                    queue = queue,
                    error = null
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No se pudo cargar el reproductor."
                )
            }
        }
    }

    fun play(
        playerId: String,
        uri: String
    ) {
        executeAction {
            musicUseCase.play(
                MusicPlayRequest(
                    player_id = playerId,
                    uri = uri
                )
            )
        }
    }

    fun pause(playerId: String) {
        executeAction {
            musicUseCase.pause(
                MusicPlayerRequest(
                    player_id = playerId
                )
            )
        }
    }

    fun next(playerId: String) {
        executeAction {
            musicUseCase.next(
                MusicPlayerRequest(
                    player_id = playerId
                )
            )
        }
    }

    fun previous(playerId: String) {
        executeAction {
            musicUseCase.previous(
                MusicPlayerRequest(
                    player_id = playerId
                )
            )
        }
    }

    private fun executeAction(
        action: suspend () -> Unit
    ) {
        if (_uiState.value.isExecutingAction) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExecutingAction = true,
                error = null
            )

            try {
                action()
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "No se pudo ejecutar la acción."
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    isExecutingAction = false
                )
            }
        }
    }
}