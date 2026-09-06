package com.chirihome.platform.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Body

interface MusicApi {

    @GET("music/search")
    suspend fun search(
        @Query("q") query: String
    ): MusicSearchResponse

    @GET("music/now-playing")
    suspend fun getNowPlaying(
        @Query("player_id") playerId: String
    ): MusicNowPlayingResponse

    @GET("music/queue")
    suspend fun getQueue(
        @Query("player_id") playerId: String
    ): MusicQueueResponse

    @POST("music/play")
    suspend fun play(
        @Body request: MusicPlayRequest
    ): MusicActionResponse

    @POST("music/pause")
    suspend fun pause(
        @Body request: MusicPlayerRequest
    ): MusicActionResponse

    @POST("music/next")
    suspend fun next(
        @Body request: MusicPlayerRequest
    ): MusicActionResponse

    @POST("music/previous")
    suspend fun previous(
        @Body request: MusicPlayerRequest
    ): MusicActionResponse
}