package com.chirihome.platform.network

data class MusicSearchResponse(
    val items: List<MusicSearchItem>
)

data class MusicSearchItem(
    val id: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Int?,
    val uri: String
)


data class MusicPlayersResponse(
    val items: List<MusicPlayerItem>
)

data class MusicPlayerItem(
    val id: String,
    val name: String,
    val available: Boolean
)

data class MusicNowPlayingResponse(
    val player_id: String,
    val state: String,
    val track: MusicNowPlayingTrack?,
    val elapsed: Int
)

data class MusicNowPlayingTrack(
    val id: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Int?,
    val uri: String,
    val stream_url: String?
)

data class MusicQueueResponse(
    val player_id: String,
    val items: List<MusicQueueItem>
)

data class MusicQueueItem(
    val id: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Int?
)

data class MusicPlayRequest(
    val player_id: String,
    val uri: String
)

data class MusicPlayerRequest(
    val player_id: String
)

data class MusicActionResponse(
    val success: Boolean
)