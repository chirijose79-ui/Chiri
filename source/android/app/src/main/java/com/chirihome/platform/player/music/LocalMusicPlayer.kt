package com.chirihome.platform.player.music

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class LocalMusicPlayer(
    context: Context
) {

    private val player = ExoPlayer.Builder(context).build()

    fun play(uri: String) {
        val mediaItem = MediaItem.fromUri(uri)

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun next() {
        player.seekToNext()
    }

    fun previous() {
        player.seekToPrevious()
    }

    fun release() {
        player.release()
    }
}