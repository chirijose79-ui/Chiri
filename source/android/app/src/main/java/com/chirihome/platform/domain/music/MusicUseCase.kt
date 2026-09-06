package com.chirihome.platform.domain.music

import com.chirihome.platform.network.MusicActionResponse
import com.chirihome.platform.network.MusicNowPlayingResponse
import com.chirihome.platform.network.MusicPlayRequest
import com.chirihome.platform.network.MusicPlayerRequest
import com.chirihome.platform.network.MusicQueueResponse
import com.chirihome.platform.network.MusicSearchResponse
import com.chirihome.platform.repository.music.MusicRepository

class MusicUseCase(
    private val musicRepository: MusicRepository
) {

    suspend fun search(
        query: String
    ): MusicSearchResponse {
        return musicRepository.search(query)
    }

    suspend fun getNowPlaying(
        playerId: String
    ): MusicNowPlayingResponse {
        return musicRepository.getNowPlaying(playerId)
    }

    suspend fun getQueue(
        playerId: String
    ): MusicQueueResponse {
        return musicRepository.getQueue(playerId)
    }

    suspend fun play(
        request: MusicPlayRequest
    ): MusicActionResponse {
        return musicRepository.play(request)
    }

    suspend fun pause(
        request: MusicPlayerRequest
    ): MusicActionResponse {
        return musicRepository.pause(request)
    }

    suspend fun next(
        request: MusicPlayerRequest
    ): MusicActionResponse {
        return musicRepository.next(request)
    }

    suspend fun previous(
        request: MusicPlayerRequest
    ): MusicActionResponse {
        return musicRepository.previous(request)
    }
}