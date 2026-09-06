package com.chirihome.platform.repository.music

import com.chirihome.platform.network.MusicActionResponse
import com.chirihome.platform.network.MusicApi
import com.chirihome.platform.network.MusicNowPlayingResponse
import com.chirihome.platform.network.MusicPlayRequest
import com.chirihome.platform.network.MusicPlayerRequest
import com.chirihome.platform.network.MusicPlayersResponse
import com.chirihome.platform.network.MusicQueueResponse
import com.chirihome.platform.network.MusicSearchResponse

class MusicRepositoryImpl(
    private val musicApi: MusicApi
) : MusicRepository {

    override suspend fun search(
        query: String
    ): MusicSearchResponse {
        return musicApi.search(query)
    }

    override suspend fun getPlayers(): MusicPlayersResponse {
        return musicApi.getPlayers()
    }

    override suspend fun getNowPlaying(
        playerId: String
    ): MusicNowPlayingResponse {
        return musicApi.getNowPlaying(playerId)
    }

    override suspend fun getQueue(
        playerId: String
    ): MusicQueueResponse {
        return musicApi.getQueue(playerId)
    }

    override suspend fun play(
        request: MusicPlayRequest
    ): MusicActionResponse {
        return musicApi.play(request)
    }

    override suspend fun pause(
        request: MusicPlayerRequest
    ): MusicActionResponse {
        return musicApi.pause(request)
    }

    override suspend fun next(
        request: MusicPlayerRequest
    ): MusicActionResponse {
        return musicApi.next(request)
    }

    override suspend fun previous(
        request: MusicPlayerRequest
    ): MusicActionResponse {
        return musicApi.previous(request)
    }
}