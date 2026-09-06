package com.chirihome.platform.repository.music

import com.chirihome.platform.network.MusicActionResponse
import com.chirihome.platform.network.MusicNowPlayingResponse
import com.chirihome.platform.network.MusicPlayRequest
import com.chirihome.platform.network.MusicPlayerRequest
import com.chirihome.platform.network.MusicPlayersResponse
import com.chirihome.platform.network.MusicQueueResponse
import com.chirihome.platform.network.MusicSearchResponse

interface MusicRepository {

    suspend fun search(
        query: String
    ): MusicSearchResponse

    suspend fun getPlayers(): MusicPlayersResponse

    suspend fun getNowPlaying(
        playerId: String
    ): MusicNowPlayingResponse

    suspend fun getQueue(
        playerId: String
    ): MusicQueueResponse

    suspend fun play(
        request: MusicPlayRequest
    ): MusicActionResponse

    suspend fun pause(
        request: MusicPlayerRequest
    ): MusicActionResponse

    suspend fun next(
        request: MusicPlayerRequest
    ): MusicActionResponse

    suspend fun previous(
        request: MusicPlayerRequest
    ): MusicActionResponse
}